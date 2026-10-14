package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link NodePartition#condense()} against a naive reference built from
 * per-group sorted sets.
 *
 * <p>The hand-written unit tests in {@code NodePartitionTest} cover partitions with two or
 * three groups, where every group ID lives in the first word of the internal dedup bit set.
 * Any bug in the multi-word case — a stale mark leaking into the next group, or a reset that
 * clears too much — is invisible there, and it is also invisible to structural checks: a
 * wrongly deduplicated condensation still has one node per group and is still acyclic.
 * Random graphs here reach up to 500 groups, so several words are in play.
 */
class CondenseFuzzTest {

  private static final int ITERATIONS = 100;
  private static final long BASE_SEED = 0xC0DEDA7AL;

  @Test
  void condenseAgreesWithNaiveReference() {
    int maxGroups = 0;

    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      Graph g = labeled.graph();
      Sccs sccs = Components.sccsOf(g);
      Graph condensed = sccs.condense();

      maxGroups = Math.max(maxGroups, sccs.groupCount());

      assertThat(condensed.nodeCount())
          .as("condensation node count for %s", labeled.describe())
          .isEqualTo(sccs.groupCount());

      List<SortedSet<Integer>> expected = naiveCondensation(g, sccs);
      for (int group = 0; group < sccs.groupCount(); group++) {
        assertThat(condensed.outNodes(group).toIntArray())
            .as("condensation adjacency of group %d for %s", group, labeled.describe())
            .containsExactly(expected.get(group).stream().mapToInt(Integer::intValue).toArray());
      }
    }

    assertThat(maxGroups)
        .as("test would not exercise the multi-word dedup path")
        .isGreaterThan(64);
  }

  /**
   * Partitions chosen at random rather than by an algorithm: nodes in no group, empty groups and
   * groups that are not connected at all, as {@link NodePartition#of} allows them. Checks the
   * members, the condensation and the edge provenance from {@link Graphs#edgePartition}.
   */
  @Test
  void callerPartitionsAgreeWithNaiveReference() {
    int partialSeen = 0;
    int crossEdgesSeen = 0;
    int maxGroups = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + 7919L * i);
      Graph g = labeled.graph();
      int n = g.nodeCount();
      var rnd = new Random(BASE_SEED + i);
      int groupCount = rnd.nextInt(Math.max(1, n / 2) + 2);
      maxGroups = Math.max(maxGroups, groupCount);
      int[] groupOf = new int[n];
      for (int v = 0; v < n; v++) {
        groupOf[v] = groupCount == 0 || rnd.nextInt(5) == 0 ? -1 : rnd.nextInt(groupCount);
      }
      var partition = NodePartition.of(g, groupOf, groupCount);
      String describe = labeled.describe() + ", groups=" + groupCount;
      if (partition.isPartial()) partialSeen++;

      for (int group = 0; group < groupCount; group++) {
        var expectedMembers = new ArrayList<Integer>();
        for (int v = 0; v < n; v++) if (groupOf[v] == group) expectedMembers.add(v);
        assertThat(partition.nodesOf(group).toIntArray())
            .as("members of group %d for %s", group, describe)
            .containsExactly(expectedMembers.stream().mapToInt(Integer::intValue).toArray());
      }

      Graph condensed = partition.condense();
      assertThat(condensed.nodeCount()).as("node count for %s", describe).isEqualTo(groupCount);
      List<SortedSet<Integer>> expected = naiveCondensation(g, partition);
      for (int group = 0; group < groupCount; group++) {
        assertThat(condensed.outNodes(group).toIntArray())
            .as("condensation adjacency of group %d for %s", group, describe)
            .containsExactly(expected.get(group).stream().mapToInt(Integer::intValue).toArray());
      }

      var provenance = Graphs.edgePartition(condensed, g, partition);
      for (int u = 0; u < n; u++) {
        for (int e = g.outEdgeStart(u); e < g.outEdgeEnd(u); e++) {
          int su = groupOf[u];
          int sv = groupOf[g.edgeTarget(e)];
          int mapped = provenance.get(e);
          if (su < 0 || sv < 0 || su == sv) {
            assertThat(mapped).as("edge %d unmapped for %s", e, describe).isEqualTo(-1);
          } else {
            crossEdgesSeen++;
            assertThat(condensed.edgeSource(mapped)).isEqualTo(su);
            assertThat(condensed.edgeTarget(mapped)).isEqualTo(sv);
          }
        }
      }
    }
    assertThat(partialSeen).as("partitions with nodes in no group").isGreaterThan(50);
    assertThat(crossEdgesSeen).as("edges between groups").isGreaterThan(1000);
    assertThat(maxGroups).as("test would not exercise the multi-word dedup path").isGreaterThan(64);
  }

  private static List<SortedSet<Integer>> naiveCondensation(Graph graph, NodePartition partition) {
    var targets = new ArrayList<SortedSet<Integer>>(partition.groupCount());
    for (int group = 0; group < partition.groupCount(); group++) {
      targets.add(new TreeSet<>());
    }
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int sourceGroup = partition.get(u);
        int targetGroup = partition.get(outs.getInt(i));
        if (sourceGroup >= 0 && targetGroup >= 0 && sourceGroup != targetGroup) {
          targets.get(sourceGroup).add(targetGroup);
        }
      }
    }
    return targets;
  }
}
