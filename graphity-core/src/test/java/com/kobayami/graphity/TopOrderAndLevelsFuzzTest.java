package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import org.junit.jupiter.api.Test;

/**
 * Fuzz-tests {@link TopOrderAndLevels#of(Graph)} on random DAGs. Levels are computed
 * independently (naive Kahn via predecessor max) and compared against Graphity's output.
 */
class TopOrderAndLevelsFuzzTest {

  private static final int ITERATIONS = 100;
  private static final long BASE_SEED = 0xDECAFBADL;

  @Test
  void topOrderAndLevelsAgreeWithReferenceOnDags() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyDag(BASE_SEED + i);
      var g = labeled.graph();
      var tol = TopOrderAndLevels.of(g);

      assertThat(tol.order().size())
          .as("order size mismatch for %s", labeled.describe())
          .isEqualTo(g.nodeCount());

      assertTopologicalInvariant(labeled, g, tol);
      assertLevelInvariant(labeled, g, tol);

      int[] reference = referenceLevels(g);
      for (int v = 0; v < g.nodeCount(); v++) {
        assertThat(tol.levelOf(v))
            .as("level(%d) mismatch for %s", v, labeled.describe())
            .isEqualTo(reference[v]);
      }
    }
  }

  private static void assertTopologicalInvariant(
      RandomGraphs.LabeledGraph labeled, Graph graph, TopOrderAndLevels tol) {
    int[] position = new int[graph.nodeCount()];
    var order = tol.order();
    for (int i = 0; i < order.size(); i++) position[order.getInt(i)] = i;
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        assertThat(position[u])
            .as("edge %d -> %d violates topological order for %s",
                u, v, labeled.describe())
            .isLessThan(position[v]);
      }
    }
  }

  private static void assertLevelInvariant(
      RandomGraphs.LabeledGraph labeled, Graph graph, TopOrderAndLevels tol) {
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        assertThat(tol.levelOf(v))
            .as("edge %d (level %d) -> %d (level %d) violates level invariant for %s",
                u, tol.levelOf(u), v, tol.levelOf(v), labeled.describe())
            .isGreaterThan(tol.levelOf(u));
      }
    }
  }

  /**
   * Independent Kahn-based level computation: for each node in topological order,
   * level(v) = max(level(predecessors)) + 1, or 0 if no predecessors.
   */
  private static int[] referenceLevels(Graph graph) {
    int n = graph.nodeCount();
    int[] inDegree = new int[n];
    int[] predMaxLevel = new int[n];
    int[] levels = new int[n];
    int[] queue = new int[n];
    int head = 0;
    int tail = 0;

    for (int u = 0; u < n; u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) inDegree[outs.getInt(i)]++;
    }
    for (int v = 0; v < n; v++) {
      if (inDegree[v] == 0) {
        queue[tail++] = v;
        levels[v] = 0;
      } else {
        predMaxLevel[v] = -1; // sentinel for "no predecessor seen yet"
      }
    }
    while (head < tail) {
      int u = queue[head++];
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        if (levels[u] > predMaxLevel[v]) predMaxLevel[v] = levels[u];
        if (--inDegree[v] == 0) {
          levels[v] = predMaxLevel[v] + 1;
          queue[tail++] = v;
        }
      }
    }
    return levels;
  }
}
