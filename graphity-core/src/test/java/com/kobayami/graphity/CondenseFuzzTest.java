package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import java.util.ArrayList;
import java.util.List;
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

  private static List<SortedSet<Integer>> naiveCondensation(Graph graph, Sccs sccs) {
    var targets = new ArrayList<SortedSet<Integer>>(sccs.groupCount());
    for (int group = 0; group < sccs.groupCount(); group++) {
      targets.add(new TreeSet<>());
    }
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int sourceGroup = sccs.get(u);
        int targetGroup = sccs.get(outs.getInt(i));
        if (sourceGroup != targetGroup) {
          targets.get(sourceGroup).add(targetGroup);
        }
      }
    }
    return targets;
  }
}
