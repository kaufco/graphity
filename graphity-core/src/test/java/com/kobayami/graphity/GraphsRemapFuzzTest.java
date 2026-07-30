package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link Graphs#remapped(Graph, NodeMapping)} against a set-based oracle: the
 * relabeled edge set must equal the original edge set translated through the mapping. Covers the
 * total-mapping path (permutations, no edge can be dropped) and the filtering path (subgraphs,
 * where edges leaving the selection disappear) separately, since they are distinct
 * implementations.
 */
class GraphsRemapFuzzTest {

  private static final int ITERATIONS = 300;

  @Test
  void permutationPreservesTranslatedEdgeSet() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      var g = labeled.graph();
      int n = g.nodeCount();

      int[] newToOld = randomPermutation(n, seed);
      var remapped = Graphs.remapped(g, NodeMapping.forPermutation(new IntArrayList(newToOld)));

      assertThat(remapped.nodeCount())
          .as("node count for %s", labeled.describe())
          .isEqualTo(n);
      assertThat(remapped.edgeCount())
          .as("permutations drop no edge for %s", labeled.describe())
          .isEqualTo(g.edgeCount());
      assertThat(translatedEdges(remapped, newToOld))
          .as("translated edge set for %s", labeled.describe())
          .isEqualTo(edges(g));
      assertSortedAdjacency(remapped, labeled.describe());
    }
  }

  @Test
  void subgraphKeepsExactlyTheInducedEdges() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      var g = labeled.graph();
      int n = g.nodeCount();

      var rnd = new Random(seed * 31 + 7);
      var selected = new BitSet();
      for (int node = 0; node < n; node++) {
        if (rnd.nextBoolean()) selected.set(node);
      }

      var nm = NodeMapping.forSubgraph(selected);
      var sub = Graphs.remapped(g, nm);

      int[] newToOld = new int[nm.size()];
      for (int newId = 0; newId < nm.size(); newId++) newToOld[newId] = nm.get(newId);

      assertThat(translatedEdges(sub, newToOld))
          .as("induced edge set for %s", labeled.describe())
          .isEqualTo(inducedEdges(g, selected));
      assertSortedAdjacency(sub, labeled.describe());
    }
  }

  private static int[] randomPermutation(int n, long seed) {
    int[] perm = new int[n];
    for (int i = 0; i < n; i++) perm[i] = i;
    var rnd = new Random(seed);
    for (int i = n - 1; i > 0; i--) {
      int j = rnd.nextInt(i + 1);
      int tmp = perm[i];
      perm[i] = perm[j];
      perm[j] = tmp;
    }
    return perm;
  }

  private static LongOpenHashSet edges(Graph g) {
    var set = new LongOpenHashSet();
    for (int source = 0; source < g.nodeCount(); source++) {
      var targets = g.outNodes(source);
      for (int i = 0; i < targets.size(); i++) {
        set.add(((long) source << 32) | targets.getInt(i));
      }
    }
    return set;
  }

  private static LongOpenHashSet inducedEdges(Graph g, BitSet selected) {
    var set = new LongOpenHashSet();
    for (int source = selected.nextSetBit(0); source >= 0; source = selected.nextSetBit(source + 1)) {
      var targets = g.outNodes(source);
      for (int i = 0; i < targets.size(); i++) {
        int target = targets.getInt(i);
        if (selected.get(target)) set.add(((long) source << 32) | target);
      }
    }
    return set;
  }

  /** Edge set of {@code g}, with both endpoints translated back into the original ID space. */
  private static LongOpenHashSet translatedEdges(Graph g, int[] newToOld) {
    var set = new LongOpenHashSet();
    for (int source = 0; source < g.nodeCount(); source++) {
      var targets = g.outNodes(source);
      for (int i = 0; i < targets.size(); i++) {
        set.add(((long) newToOld[source] << 32) | newToOld[targets.getInt(i)]);
      }
    }
    return set;
  }

  private static void assertSortedAdjacency(Graph g, String description) {
    for (int source = 0; source < g.nodeCount(); source++) {
      var targets = g.outNodes(source);
      for (int i = 1; i < targets.size(); i++) {
        assertThat(targets.getInt(i))
            .as("adjacency of node %d must stay sorted for %s", source, description)
            .isGreaterThan(targets.getInt(i - 1));
      }
    }
  }
}
