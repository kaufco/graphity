package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates the two ways to translate an edge index across a transpose:
 * {@link Graphs#transposedEdgeMapping(Graph)}, a single counting-sort pass, against
 * {@link BiAdjacentGraph#transposedEdgeIndex(int)}, two binary searches. Both are checked
 * against a structural oracle that follows neither: the translated edge must have source and
 * target swapped, and translating twice - once on the graph, once on its transpose - must
 * return the original index.
 */
class TransposedEdgeMappingFuzzTest {

  private static final int ITERATIONS = 300;

  @Test
  void translatedEdgeHasEndpointsSwapped() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      var g = labeled.graph();
      var transposed = g.transposed();
      var mapping = Graphs.transposedEdgeMapping(g);

      assertThat(mapping.isPartial())
          .as("mapping is total for %s", labeled.describe())
          .isFalse();
      assertThat(mapping.size())
          .as("mapping covers every edge for %s", labeled.describe())
          .isEqualTo(g.edgeCount());

      for (int q = 0; q < transposed.edgeCount(); q++) {
        int p = mapping.get(q);
        assertThat(g.edgeSource(p))
            .as("source of edge %d for %s", q, labeled.describe())
            .isEqualTo(transposed.edgeTarget(q));
        assertThat(g.edgeTarget(p))
            .as("target of edge %d for %s", q, labeled.describe())
            .isEqualTo(transposed.edgeSource(q));
      }
    }
  }

  @Test
  void countingSortAndBinarySearchAgree() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      var g = labeled.graph();
      var biAdjacent = g.biAdjacentGraph();
      var mapping = Graphs.transposedEdgeMapping(g);

      for (int p = 0; p < g.edgeCount(); p++) {
        int q = biAdjacent.transposedEdgeIndex(p);
        assertThat(mapping.get(q))
            .as("edge %d for %s", p, labeled.describe())
            .isEqualTo(p);
      }
    }
  }

  @Test
  void translatingTwiceReturnsTheOriginalIndex() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      var biAdjacent = labeled.graph().biAdjacentGraph();
      var transposed = biAdjacent.transposed();

      for (int p = 0; p < biAdjacent.edgeCount(); p++) {
        assertThat(transposed.transposedEdgeIndex(biAdjacent.transposedEdgeIndex(p)))
            .as("edge %d for %s", p, labeled.describe())
            .isEqualTo(p);
      }
    }
  }

  @Test
  void uniAndBiAdjacentInputYieldTheSameMapping() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      var g = labeled.graph();
      var fromUniAdjacent = Graphs.transposedEdgeMapping(g);
      var fromBiAdjacent = Graphs.transposedEdgeMapping(g.biAdjacentGraph());

      for (int q = 0; q < g.edgeCount(); q++) {
        assertThat(fromBiAdjacent.get(q))
            .as("edge %d for %s", q, labeled.describe())
            .isEqualTo(fromUniAdjacent.get(q));
      }
    }
  }
}
