package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

class EdgeTypesTest {

  @Test
  void ofEmptyGraphHasNoEdgeTypes() {
    var et = EdgeTypes.of(TestGraphs.empty());
    for (EdgeType t : EdgeType.values()) {
      assertThat(et.edgesOfType(t).count()).isZero();
    }
  }

  @Test
  void ofPathClassifiesAllEdgesAsTree() {
    var g = TestGraphs.path(5);
    var et = EdgeTypes.of(g);
    for (int i = 0; i < g.edgeCount(); i++) {
      assertThat(et.typeOf(i)).isEqualTo(EdgeType.TREE);
    }
    assertThat(et.edgesOfType(EdgeType.TREE).count()).isEqualTo(4);
  }

  @Test
  void ofDagWithForwardEdge() {
    // 0 -> 1, 0 -> 2, 1 -> 2. DFS from 0: tree 0->1, tree 1->2, then 0->2 is forward (2 already finished).
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    var et = EdgeTypes.of(g);
    var sources = et.edgesOfType(EdgeType.FORWARD).sourceNodes();
    var targets = et.edgesOfType(EdgeType.FORWARD).targetNodes();
    assertThat(sources.toIntArray()).containsExactly(0);
    assertThat(targets.toIntArray()).containsExactly(2);
  }

  @Test
  void ofDagWithCrossEdge() {
    // 0 -> 1, 0 -> 2, 2 -> 1. DFS from 0: tree 0->1, finish 1, tree 0->2, then 2->1 — 1 is finished, not on stack, disc[2] > disc[1] → cross.
    var g = TestGraphs.of(3, 0, 1, 0, 2, 2, 1);
    var et = EdgeTypes.of(g);
    var crossSources = et.edgesOfType(EdgeType.CROSS).sourceNodes();
    var crossTargets = et.edgesOfType(EdgeType.CROSS).targetNodes();
    assertThat(crossSources.toIntArray()).containsExactly(2);
    assertThat(crossTargets.toIntArray()).containsExactly(1);
  }

  @Test
  void ofCycleHasOneBackEdge() {
    var g = TestGraphs.cycle(4);
    var et = EdgeTypes.of(g);
    var back = et.edgesOfType(EdgeType.BACK);
    assertThat(back.count()).isEqualTo(1);
    assertThat(back.sourceNodes().getInt(0)).isEqualTo(3);
    assertThat(back.targetNodes().getInt(0)).isZero();
    assertThat(et.edgesOfType(EdgeType.TREE).count()).isEqualTo(3);
  }

  @Test
  void ofSelfLoopIsBackEdge() {
    var g = TestGraphs.of(1, 0, 0);
    var et = EdgeTypes.of(g);
    assertThat(et.typeOf(0)).isEqualTo(EdgeType.BACK);
  }

  @Test
  void edgeTypesPartitionAllEdges() {
    // Mixed graph with all four types.
    // 0 -> 1, 0 -> 2, 0 -> 3 (tree 0->1, tree 0->2 after return, forward 0->3 after return)
    // 1 -> 3 (tree 1->3)
    // 2 -> 3 (cross or forward? after 1's subtree finished, 3 is discovered via 1; visiting 2 with edge to 3: 3 finished, disc[2] > disc[3] → cross)
    // 3 -> 0 (back: 0 is on stack when descending to 3)
    var g = TestGraphs.of(4,
        0, 1, 0, 2, 0, 3,
        1, 3,
        2, 3,
        3, 0);
    var et = EdgeTypes.of(g);
    int total = 0;
    for (EdgeType t : EdgeType.values()) {
      total += et.edgesOfType(t).count();
    }
    assertThat(total).isEqualTo(g.edgeCount());
    // Every edge reachable via typeOf.
    for (int i = 0; i < g.edgeCount(); i++) {
      assertThat(et.typeOf(i)).isNotNull();
    }
  }

  @Test
  void typeOfAgreesWithEdgesOfType() {
    // Any mix of edges.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 2, 1, 3, 2, 3, 3, 0);
    var et = EdgeTypes.of(g);
    // Cross-check: for each edge type, gather (source, target) via edgesOfType, then verify
    // that typeOf(edgeIndex) returns the same type when looking up that edge.
    for (EdgeType type : EdgeType.values()) {
      var edges = et.edgesOfType(type);
      for (int i = 0; i < edges.count(); i++) {
        int src = edges.sourceNodes().getInt(i);
        int tgt = edges.targetNodes().getInt(i);
        int start = g.outEdgeStart(src);
        int end = g.outEdgeEnd(src);
        int foundIndex = -1;
        for (int j = start; j < end; j++) {
          if (g.edgeTarget(j) == tgt) { foundIndex = j; break; }
        }
        assertThat(foundIndex).isNotEqualTo(-1);
        assertThat(et.typeOf(foundIndex)).isEqualTo(type);
      }
    }
  }

  @Test
  void backEdgesAgreeWithBackEdgesClass() {
    // BackEdges.of(g) and EdgeTypes.of(g).edgesOfType(BACK) must agree on the set of back-edges.
    var g = TestGraphs.of(5,
        0, 1, 1, 2, 2, 0,
        2, 3,
        3, 4, 4, 3);
    var viaEt = EdgeTypes.of(g).edgesOfType(EdgeType.BACK);
    var viaBe = BackEdges.of(g);
    assertThat(viaEt.count()).isEqualTo(viaBe.count());
    // Compare as multisets of (src, tgt) pairs.
    var etPairs = new java.util.HashSet<Long>();
    for (int i = 0; i < viaEt.count(); i++) {
      etPairs.add(((long) viaEt.sourceNodes().getInt(i) << 32) | viaEt.targetNodes().getInt(i));
    }
    var bePairs = new java.util.HashSet<Long>();
    for (int i = 0; i < viaBe.count(); i++) {
      bePairs.add(((long) viaBe.sourceNodes().getInt(i) << 32) | viaBe.targetNodes().getInt(i));
    }
    assertThat(etPairs).isEqualTo(bePairs);
  }
}
