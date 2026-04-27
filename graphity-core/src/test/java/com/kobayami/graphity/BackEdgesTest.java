package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

class BackEdgesTest {

  @Test
  void ofEmptyGraphIsEmpty() {
    assertThat(BackEdges.of(TestGraphs.empty()).count()).isZero();
  }

  @Test
  void ofDagHasNoBackEdges() {
    // 0 -> 1, 0 -> 2, 1 -> 3, 2 -> 3.
    assertThat(BackEdges.of(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3)).count()).isZero();
  }

  @Test
  void ofPathHasNoBackEdges() {
    assertThat(BackEdges.of(TestGraphs.path(5)).count()).isZero();
  }

  @Test
  void ofCycleHasOneBackEdge() {
    // 0 -> 1 -> 2 -> 3 -> 0: DFS from 0 descends to 3, edge 3 -> 0 is a back-edge.
    var be = BackEdges.of(TestGraphs.cycle(4));
    assertThat(be.count()).isEqualTo(1);
    assertThat(be.sourceNodes().toIntArray()).containsExactly(3);
    assertThat(be.targetNodes().toIntArray()).containsExactly(0);
  }

  @Test
  void ofSelfLoopIsBackEdge() {
    var be = BackEdges.of(TestGraphs.of(1, 0, 0));
    assertThat(be.count()).isEqualTo(1);
    assertThat(be.sourceNodes().toIntArray()).containsExactly(0);
    assertThat(be.targetNodes().toIntArray()).containsExactly(0);
  }

  @Test
  void removingBackEdgesYieldsDag() {
    // Cycle {0,1,2} + bridge 2 -> 3 + cycle {3, 4, 5}.
    var g = TestGraphs.of(6,
        0, 1, 1, 2, 2, 0,
        2, 3,
        3, 4, 4, 5, 5, 3);
    var be = BackEdges.of(g);
    assertThat(be.count()).isPositive();
    // Build the DAG: same nodes, same edges minus the back-edges.
    var backEdgeSet = new java.util.HashSet<Long>();
    for (int i = 0; i < be.count(); i++) {
      backEdgeSet.add(((long) be.sourceNodes().getInt(i) << 32) | be.targetNodes().getInt(i));
    }
    var builder = new GraphBuilder();
    builder.addNodes(g.nodeCount());
    for (int u = 0; u < g.nodeCount(); u++) {
      var outs = g.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        if (!backEdgeSet.contains(((long) u << 32) | v)) {
          builder.addEdge(u, v);
        }
      }
    }
    assertThat(Graphs.isDag(builder.build())).isTrue();
  }

  @Test
  void ofMultipleDisjointCyclesFindsBackEdgePerCycle() {
    // Two disjoint cycles.
    var g = TestGraphs.of(6,
        0, 1, 1, 2, 2, 0,
        3, 4, 4, 5, 5, 3);
    var be = BackEdges.of(g);
    assertThat(be.count()).isEqualTo(2);
  }

  @Test
  void ofGraphWithForwardEdgeHasNoBackEdge() {
    // 0 -> 1, 0 -> 2, 1 -> 2: 0 -> 2 is a forward edge, not back.
    var be = BackEdges.of(TestGraphs.of(3, 0, 1, 0, 2, 1, 2));
    assertThat(be.count()).isZero();
  }

  @Test
  void ofGraphWithCrossEdgeHasNoBackEdge() {
    // Cross-edge scenario: 0 -> 1, 0 -> 2, 2 -> 1 (after DFS finishes node 1, visit 2 with edge to already-finished 1).
    var be = BackEdges.of(TestGraphs.of(3, 0, 1, 0, 2, 2, 1));
    assertThat(be.count()).isZero();
  }
}
