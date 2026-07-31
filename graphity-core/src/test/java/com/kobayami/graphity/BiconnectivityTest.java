package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

class BiconnectivityTest {

  /** Two triangles sharing node 0, plus a pendant chain 5 - 6 hanging off node 3. */
  private static Graph bowtieWithTail() {
    return TestGraphs.of(7, 0, 1, 1, 2, 2, 0, 0, 3, 3, 4, 4, 0, 3, 5, 5, 6);
  }

  @Test
  void everyEdgeOfAPathIsABridge() {
    var b = Components.biconnectivityOf(TestGraphs.path(4));
    assertThat(b.bridges().count()).isEqualTo(3);
    assertThat(b.bridges().sourceNodes().toIntArray()).containsExactly(0, 1, 2);
    assertThat(b.bridges().targetNodes().toIntArray()).containsExactly(1, 2, 3);
  }

  @Test
  void innerNodesOfAPathAreArticulationPoints() {
    var b = Components.biconnectivityOf(TestGraphs.path(4));
    assertThat(b.articulationPoints().toIntArray()).containsExactly(1, 2);
  }

  @Test
  void everyEdgeOfAPathIsItsOwnBlock() {
    var b = Components.biconnectivityOf(TestGraphs.path(4));
    assertThat(b.blocks().groupCount()).isEqualTo(3);
    for (int block = 0; block < 3; block++) {
      assertThat(b.blocks().sizeOf(block)).isEqualTo(1);
    }
  }

  @Test
  void aCycleHasNoBridgeAndNoArticulationPoint() {
    var b = Components.biconnectivityOf(TestGraphs.cycle(5));
    assertThat(b.bridges().isEmpty()).isTrue();
    assertThat(b.articulationPoints().isEmpty()).isTrue();
  }

  @Test
  void aCycleIsOneBlock() {
    var b = Components.biconnectivityOf(TestGraphs.cycle(5));
    assertThat(b.blocks().groupCount()).isEqualTo(1);
    assertThat(b.blocks().edgesOf(0).toIntArray()).containsExactly(0, 1, 2, 3, 4);
  }

  @Test
  void aReciprocalPairIsOneUndirectedEdgeAndThusABridge() {
    // Storing an undirected edge in both directions must not turn it into a 2-cycle.
    var b = Components.biconnectivityOf(TestGraphs.of(2, 0, 1, 1, 0));
    assertThat(b.bridges().count()).isEqualTo(1);
    assertThat(b.bridges().sourceNodes().getInt(0)).isZero();
    assertThat(b.bridges().targetNodes().getInt(0)).isEqualTo(1);
  }

  @Test
  void bothEdgeIndicesOfAReciprocalPairShareABlock() {
    var b = Components.biconnectivityOf(TestGraphs.of(2, 0, 1, 1, 0));
    assertThat(b.blocks().groupCount()).isEqualTo(1);
    assertThat(b.blocks().edgesOf(0).toIntArray()).containsExactly(0, 1);
  }

  @Test
  void symmetricStorageYieldsTheSameResultAsSingleDirection() {
    var single = Components.biconnectivityOf(TestGraphs.path(4));
    // The same path with every edge stored in both directions.
    var both = Components.biconnectivityOf(
        TestGraphs.of(4, 0, 1, 1, 0, 1, 2, 2, 1, 2, 3, 3, 2));
    assertThat(both.bridges().sourceNodes().toIntArray())
        .containsExactly(single.bridges().sourceNodes().toIntArray());
    assertThat(both.bridges().targetNodes().toIntArray())
        .containsExactly(single.bridges().targetNodes().toIntArray());
    assertThat(both.articulationPoints().toIntArray())
        .containsExactly(single.articulationPoints().toIntArray());
    assertThat(both.blocks().groupCount()).isEqualTo(single.blocks().groupCount());
  }

  @Test
  void articulationPointJoinsTwoTriangles() {
    // Two triangles sharing node 0, nothing else.
    var b = Components.biconnectivityOf(TestGraphs.of(5, 0, 1, 1, 2, 2, 0, 0, 3, 3, 4, 4, 0));
    assertThat(b.articulationPoints().toIntArray()).containsExactly(0);
    assertThat(b.bridges().isEmpty()).isTrue();
    assertThat(b.blocks().groupCount()).isEqualTo(2);
    assertThat(b.blocks().sizeOf(0)).isEqualTo(3);
    assertThat(b.blocks().sizeOf(1)).isEqualTo(3);
  }

  @Test
  void bridgeAndBlocksCoexist() {
    var b = Components.biconnectivityOf(bowtieWithTail());
    assertThat(b.bridges().sourceNodes().toIntArray()).containsExactly(3, 5);
    assertThat(b.bridges().targetNodes().toIntArray()).containsExactly(5, 6);
    assertThat(b.articulationPoints().toIntArray()).containsExactly(0, 3, 5);
    // Two triangles plus the two bridges.
    assertThat(b.blocks().groupCount()).isEqualTo(4);
  }

  @Test
  void everyBridgeLiesInABlockOfItsOwn() {
    var b = Components.biconnectivityOf(bowtieWithTail());
    var graph = bowtieWithTail();
    for (int i = 0; i < b.bridges().count(); i++) {
      int source = b.bridges().sourceNodes().getInt(i);
      int target = b.bridges().targetNodes().getInt(i);
      int edgeIndex = edgeIndexOf(graph, source, target);
      assertThat(b.blocks().sizeOf(b.blocks().get(edgeIndex))).isEqualTo(1);
    }
  }

  @Test
  void selfLoopsAreUnmapped() {
    // Triangle 0-1-2 plus a self-loop at node 1.
    var g = TestGraphs.of(3, 0, 1, 1, 1, 1, 2, 2, 0);
    var b = Components.biconnectivityOf(g);
    assertThat(b.blocks().isPartial()).isTrue();
    assertThat(b.blocks().get(1)).isEqualTo(-1);
    assertThat(b.blocks().size()).isEqualTo(3);
    assertThat(b.bridges().isEmpty()).isTrue();
    assertThat(b.articulationPoints().isEmpty()).isTrue();
  }

  @Test
  void blocksAreTotalWithoutSelfLoops() {
    var b = Components.biconnectivityOf(TestGraphs.cycle(5));
    assertThat(b.blocks().isPartial()).isFalse();
    assertThat(b.blocks().size()).isEqualTo(5);
  }

  @Test
  void aGraphOfOnlySelfLoopsHasNothing() {
    var b = Components.biconnectivityOf(TestGraphs.of(3, 0, 0, 1, 1, 2, 2));
    assertThat(b.bridges().isEmpty()).isTrue();
    assertThat(b.articulationPoints().isEmpty()).isTrue();
    assertThat(b.blocks().groupCount()).isZero();
    assertThat(b.blocks().size()).isZero();
  }

  @Test
  void isolatedNodesAreNeitherArticulationPointsNorBlocks() {
    var b = Components.biconnectivityOf(TestGraphs.isolated(4));
    assertThat(b.bridges().isEmpty()).isTrue();
    assertThat(b.articulationPoints().isEmpty()).isTrue();
    assertThat(b.blocks().groupCount()).isZero();
  }

  @Test
  void theEmptyGraphHasNothing() {
    var b = Components.biconnectivityOf(TestGraphs.empty());
    assertThat(b.bridges().isEmpty()).isTrue();
    assertThat(b.articulationPoints().isEmpty()).isTrue();
    assertThat(b.blocks().groupCount()).isZero();
    assertThat(b.blocks().edgeCount()).isZero();
  }

  @Test
  void aCliqueHasNoArticulationPoint() {
    var b = Components.biconnectivityOf(TestGraphs.clique(5));
    assertThat(b.articulationPoints().isEmpty()).isTrue();
    assertThat(b.bridges().isEmpty()).isTrue();
    assertThat(b.blocks().groupCount()).isEqualTo(1);
  }

  @Test
  void separateComponentsAreHandledIndependently() {
    // Path 0-1 and triangle 2-3-4.
    var b = Components.biconnectivityOf(TestGraphs.of(5, 0, 1, 2, 3, 3, 4, 4, 2));
    assertThat(b.bridges().sourceNodes().toIntArray()).containsExactly(0);
    assertThat(b.bridges().targetNodes().toIntArray()).containsExactly(1);
    assertThat(b.articulationPoints().isEmpty()).isTrue();
    assertThat(b.blocks().groupCount()).isEqualTo(2);
  }

  @Test
  void resultIsIdenticalForBiAdjacentInput() {
    var g = bowtieWithTail();
    var fromUniAdjacent = Components.biconnectivityOf(g);
    var fromBiAdjacent = Components.biconnectivityOf(g.biAdjacentGraph());
    assertThat(fromBiAdjacent.bridges().sourceNodes().toIntArray())
        .containsExactly(fromUniAdjacent.bridges().sourceNodes().toIntArray());
    assertThat(fromBiAdjacent.articulationPoints().toIntArray())
        .containsExactly(fromUniAdjacent.articulationPoints().toIntArray());
    for (int edgeIndex = 0; edgeIndex < g.edgeCount(); edgeIndex++) {
      assertThat(fromBiAdjacent.blocks().get(edgeIndex))
          .isEqualTo(fromUniAdjacent.blocks().get(edgeIndex));
    }
  }

  /** Edge index of {@code source → target}, or of the reverse edge if only that one exists. */
  private static int edgeIndexOf(Graph graph, int source, int target) {
    for (int edgeIndex = graph.outEdgeStart(source); edgeIndex < graph.outEdgeEnd(source); edgeIndex++) {
      if (graph.edgeTarget(edgeIndex) == target) return edgeIndex;
    }
    for (int edgeIndex = graph.outEdgeStart(target); edgeIndex < graph.outEdgeEnd(target); edgeIndex++) {
      if (graph.edgeTarget(edgeIndex) == source) return edgeIndex;
    }
    throw new IllegalArgumentException("no edge between " + source + " and " + target);
  }
}
