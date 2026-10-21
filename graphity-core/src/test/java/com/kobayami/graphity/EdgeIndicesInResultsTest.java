package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

/**
 * Every result that lists edges names each by its edge index, and that index is the edge at the
 * same position: same source, same target.
 */
class EdgeIndicesInResultsTest {

  private static final int ITERATIONS = 200;

  private static void assertConsistent(Graph graph, Edges edges, String describe) {
    assertThat(edges.edgeIndices().size()).as(describe).isEqualTo(edges.count());
    for (int p = 0; p < edges.count(); p++) {
      int source = edges.sourceNodes().getInt(p);
      int target = edges.targetNodes().getInt(p);
      int edgeIndex = edges.edgeIndices().getInt(p);
      assertThat(edgeIndex).as("%s, position %d", describe, p)
          .isEqualTo(graph.edgeIndexOf(source, target))
          .isNotNegative();
    }
  }

  private static void assertAscending(Edges edges, String describe) {
    for (int p = 1; p < edges.count(); p++) {
      assertThat(edges.edgeIndices().getInt(p)).as(describe)
          .isGreaterThan(edges.edgeIndices().getInt(p - 1));
    }
  }

  @Test
  void everyProducerReportsTheEdgeAtEachPosition() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      Graph graph = labeled.graph();
      var bi = BiAdjacentGraph.of(graph);
      String describe = labeled.describe();

      assertConsistent(graph, BackEdges.of(graph), "back edges, " + describe);

      var types = EdgeTypes.of(graph);
      for (EdgeType type : EdgeType.values()) {
        var edges = Edges.of(types, type);
        assertConsistent(graph, edges, type + ", " + describe);
        assertAscending(edges, type + ", " + describe);
      }

      var bridges = Components.biconnectivityOf(bi).bridges();
      assertConsistent(graph, bridges, "bridges, " + describe);
      assertAscending(bridges, "bridges, " + describe);

      var strongBridges = Components.strongConnectivityOf(bi).strongBridges();
      assertConsistent(graph, strongBridges, "strong bridges, " + describe);
      assertAscending(strongBridges, "strong bridges, " + describe);
    }
  }

  @Test
  void backEdgeIndicesReachEdgeData() {
    // 0 -> 1 -> 2 -> 0, 2 -> 1: two back edges.
    Graph graph = TestGraphs.of(3, 0, 1, 1, 2, 2, 0, 2, 1);
    double[] weights = {1.0, 2.0, 3.0, 4.0};   // by edge index: 0->1, 1->2, 2->0, 2->1
    var back = BackEdges.of(graph);
    double total = 0;
    for (int p = 0; p < back.count(); p++) total += weights[back.edgeIndices().getInt(p)];
    assertThat(total).isEqualTo(3.0 + 4.0);
  }

  @Test
  void aBridgeStoredInBothDirectionsIsReportedFromTheLowerNode() {
    // 0 <-> 1 is a bridge stored twice; 1 -> 2 -> 3 -> 1 is a cycle.
    var graph = BiAdjacentGraph.of(TestGraphs.of(4, 0, 1, 1, 0, 1, 2, 2, 3, 3, 1));
    var bridges = Components.biconnectivityOf(graph).bridges();
    assertThat(bridges.count()).isEqualTo(1);
    assertThat(bridges.sourceNodes().getInt(0)).isZero();
    assertThat(bridges.targetNodes().getInt(0)).isEqualTo(1);
    assertThat(bridges.edgeIndices().getInt(0)).isEqualTo(graph.edgeIndexOf(0, 1));
  }

  @Test
  void aBridgeStoredOnlyFromTheHigherNodeKeepsItsDirection() {
    // 2 -> 0 and 2 -> 1: both bridges, both stored from the higher node.
    var graph = BiAdjacentGraph.of(TestGraphs.of(3, 2, 0, 2, 1));
    var bridges = Components.biconnectivityOf(graph).bridges();
    assertThat(bridges.sourceNodes().toIntArray()).containsExactly(2, 2);
    assertThat(bridges.targetNodes().toIntArray()).containsExactly(0, 1);
    assertThat(bridges.edgeIndices().toIntArray()).containsExactly(0, 1);
  }
}
