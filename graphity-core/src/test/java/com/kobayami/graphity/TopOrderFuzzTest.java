package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.JGraphtAdapter;
import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link TopOrder#of(Graph)} against JGraphT's
 * {@link TopologicalOrderIterator}. Both are valid topological orders (not necessarily
 * the same one) — the test verifies that Graphity's order satisfies the topological
 * invariant, and additionally that JGraphT's independent order satisfies it too
 * (sanity check of the adapter).
 */
class TopOrderFuzzTest {

  private static final int ITERATIONS = 100;
  private static final long BASE_SEED = 0xBEEFFACEL;

  @Test
  void topOrderIsValidOnRandomDags() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyDag(BASE_SEED + i);
      var g = labeled.graph();

      var ourOrder = TopOrder.of(g);
      assertThat(ourOrder.size())
          .as("topological order must cover all nodes for %s", labeled.describe())
          .isEqualTo(g.nodeCount());
      assertValidTopologicalOrder(labeled, g, ourOrder);

      var jg = JGraphtAdapter.toJgrapht(g);
      var theirOrder = new ArrayList<Integer>();
      new TopologicalOrderIterator<>(jg).forEachRemaining(theirOrder::add);
      assertThat(theirOrder.size())
          .as("JGraphT topological order size mismatch for %s", labeled.describe())
          .isEqualTo(g.nodeCount());
      assertValidTopologicalOrderInteger(labeled, g, theirOrder);
    }
  }

  private static void assertValidTopologicalOrder(
      RandomGraphs.LabeledGraph labeled, Graph graph, IntList order) {
    int[] position = new int[graph.nodeCount()];
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

  private static void assertValidTopologicalOrderInteger(
      RandomGraphs.LabeledGraph labeled, Graph graph, List<Integer> order) {
    int[] position = new int[graph.nodeCount()];
    for (int i = 0; i < order.size(); i++) position[order.get(i)] = i;
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        assertThat(position[u])
            .as("(JGraphT) edge %d -> %d violates topological order for %s",
                u, v, labeled.describe())
            .isLessThan(position[v]);
      }
    }
  }
}
