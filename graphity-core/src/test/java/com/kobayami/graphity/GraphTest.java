package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GraphTest {

  @Test
  void emptyGraphReportsZero() {
    var g = TestGraphs.empty();
    assertThat(g.nodeCount()).isZero();
    assertThat(g.edgeCount()).isZero();
    assertThat(g.isEmpty()).isTrue();
  }

  @Test
  void isolatedGraphHasNodesNoEdges() {
    var g = TestGraphs.isolated(5);
    assertThat(g.nodeCount()).isEqualTo(5);
    assertThat(g.edgeCount()).isZero();
    assertThat(g.isEmpty()).isFalse();
  }

  @Test
  void nodeAndEdgeCountsMatchGraphBuilder() {
    // 0->1, 0->2, 1->3, 2->3 (diamond).
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    assertThat(g.nodeCount()).isEqualTo(4);
    assertThat(g.edgeCount()).isEqualTo(4);
  }

  @Test
  void outDegreeReportsOutEdges() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    assertThat(g.outDegree(0)).isEqualTo(2);
    assertThat(g.outDegree(1)).isEqualTo(1);
    assertThat(g.outDegree(2)).isEqualTo(1);
    assertThat(g.outDegree(3)).isZero();
  }

  @Test
  void degreeOnUniAdjacentGraphEqualsOutDegree() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    for (int i = 0; i < g.nodeCount(); i++) {
      assertThat(g.degree(i)).isEqualTo(g.outDegree(i));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void outDegreeOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(4);
    assertThatThrownBy(() -> g.outDegree(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void outNodesReturnsSortedView() {
    var g = TestGraphs.of(4, 0, 3, 0, 1, 0, 2);
    assertThat(g.outNodes(0).toIntArray()).containsExactly(1, 2, 3);
  }

  @Test
  void outNodesForNodeWithoutOutEdgesIsEmpty() {
    var g = TestGraphs.of(2, 0, 1);
    assertThat(g.outNodes(1).isEmpty()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void outNodesOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(4);
    assertThatThrownBy(() -> g.outNodes(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void outEdgeStartAndEndFrameOutNodes() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    int s = g.outEdgeStart(0);
    int e = g.outEdgeEnd(0);
    assertThat(e - s).isEqualTo(g.outDegree(0));
    int[] targets = new int[e - s];
    for (int i = s; i < e; i++) targets[i - s] = g.edgeTarget(i);
    assertThat(targets).containsExactly(1, 2);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void outEdgeStartOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(3);
    assertThatThrownBy(() -> g.outEdgeStart(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> g.outEdgeEnd(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void edgeTargetReturnsTargetByEdgeIndex() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    assertThat(g.edgeTarget(0)).isEqualTo(1);
    assertThat(g.edgeTarget(1)).isEqualTo(2);
    assertThat(g.edgeTarget(2)).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void edgeTargetOutOfRangeThrows(int edgeIndex) {
    var g = TestGraphs.of(2, 0, 1, 1, 0, 0, 0);
    assertThatThrownBy(() -> g.edgeTarget(edgeIndex))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void edgeSourceInvertsEdgeTargetMapping() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    for (int e = 0; e < g.edgeCount(); e++) {
      int source = g.edgeSource(e);
      int target = g.edgeTarget(e);
      assertThat(g.hasOutNode(source, target)).isTrue();
      assertThat(e).isBetween(g.outEdgeStart(source), g.outEdgeEnd(source) - 1);
    }
  }

  @Test
  void edgeSourceReturnsCorrectSourceAcrossAllEdges() {
    var g = TestGraphs.of(5,
        0, 2, 0, 4,
        2, 3,
        3, 1, 3, 4);
    assertThat(g.edgeSource(0)).isZero();
    assertThat(g.edgeSource(1)).isZero();
    assertThat(g.edgeSource(2)).isEqualTo(2);
    assertThat(g.edgeSource(3)).isEqualTo(3);
    assertThat(g.edgeSource(4)).isEqualTo(3);
  }

  @Test
  void edgeSourceHandlesNodesWithNoOutEdges() {
    // Node 0 has no out-edges; edge 0 belongs to node 1.
    var g = TestGraphs.of(3, 1, 2);
    assertThat(g.edgeSource(0)).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 100})
  void edgeSourceOutOfRangeThrows(int edgeIndex) {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2, 2, 0);
    assertThatThrownBy(() -> g.edgeSource(edgeIndex))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void hasOutNodeDetectsExistingEdges() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    assertThat(g.hasOutNode(0, 1)).isTrue();
    assertThat(g.hasOutNode(0, 2)).isTrue();
    assertThat(g.hasOutNode(1, 2)).isTrue();
  }

  @Test
  void hasOutNodeRejectsMissingEdges() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    assertThat(g.hasOutNode(0, 0)).isFalse();
    assertThat(g.hasOutNode(1, 0)).isFalse();
    assertThat(g.hasOutNode(2, 0)).isFalse();
    assertThat(g.hasOutNode(0, 999)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 1000})
  void hasOutNodeOutOfRangeSourceThrows(int node) {
    var g = TestGraphs.isolated(3);
    assertThatThrownBy(() -> g.hasOutNode(node, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void containsSelfLoopDetectsSelfLoops() {
    var g = TestGraphs.of(3, 0, 0, 1, 2, 2, 2);
    assertThat(g.containsSelfLoop(0)).isTrue();
    assertThat(g.containsSelfLoop(1)).isFalse();
    assertThat(g.containsSelfLoop(2)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 1000})
  void containsSelfLoopOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(3);
    assertThatThrownBy(() -> g.containsSelfLoop(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void biAdjacentGraphComputesCorrectInEdges() {
    // Diamond 0->1, 0->2, 1->3, 2->3. In-edges: 1<-{0}, 2<-{0}, 3<-{1,2}.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var bi = g.biAdjacentGraph();
    assertThat(bi.inNodes(0).toIntArray()).isEmpty();
    assertThat(bi.inNodes(1).toIntArray()).containsExactly(0);
    assertThat(bi.inNodes(2).toIntArray()).containsExactly(0);
    assertThat(bi.inNodes(3).toIntArray()).containsExactly(1, 2);
  }

  @Test
  void biAdjacentGraphPreservesOutAdjacency() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var bi = g.biAdjacentGraph();
    for (int i = 0; i < g.nodeCount(); i++) {
      assertThat(bi.outNodes(i).toIntArray()).containsExactly(g.outNodes(i).toIntArray());
    }
  }

  @Test
  void biAdjacentGraphOnEmptyGraphIsEmpty() {
    var bi = TestGraphs.empty().biAdjacentGraph();
    assertThat(bi.nodeCount()).isZero();
    assertThat(bi.edgeCount()).isZero();
  }

  @Test
  void biAdjacentGraphInAdjacencyIsSorted() {
    // Node 3 receives edges from 0, 2, 1 — must end up sorted as [0, 1, 2].
    var g = TestGraphs.of(4, 0, 3, 1, 3, 2, 3);
    assertThat(g.biAdjacentGraph().inNodes(3).toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void uniAdjacentGraphOnPlainGraphReturnsSelf() {
    var g = TestGraphs.path(3);
    assertThatObject(g.uniAdjacentGraph()).isSameAs(g);
  }

  @Test
  void transposedReversesAllEdges() {
    var g = TestGraphs.of(3, 0, 1, 1, 2);
    var t = g.transposed();
    assertThat(t.outNodes(0).toIntArray()).isEmpty();
    assertThat(t.outNodes(1).toIntArray()).containsExactly(0);
    assertThat(t.outNodes(2).toIntArray()).containsExactly(1);
  }

  @Test
  void transposedTwiceEqualsOriginal() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    assertThat(g.transposed().transposed()).isEqualTo(g);
  }

  @Test
  void transposedOnSelfLoopKeepsSelfLoop() {
    var g = TestGraphs.of(2, 0, 0, 0, 1);
    var t = g.transposed();
    assertThat(t.containsSelfLoop(0)).isTrue();
    assertThat(t.outNodes(1).toIntArray()).containsExactly(0);
  }

  @Test
  void equalsBasedOnStructure() {
    var a = TestGraphs.of(3, 0, 1, 1, 2);
    var b = TestGraphs.of(3, 0, 1, 1, 2);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equalsWithDifferentStructureFalse() {
    var a = TestGraphs.of(3, 0, 1, 1, 2);
    var b = TestGraphs.of(3, 0, 2, 1, 2);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void equalsWithDifferentNodeCountFalse() {
    assertThat(TestGraphs.isolated(3)).isNotEqualTo(TestGraphs.isolated(4));
  }

  @Test
  void equalsIgnoresBiVsUniAdjacentType() {
    var uni = TestGraphs.of(3, 0, 1, 1, 2);
    var bi = uni.biAdjacentGraph();
    assertThat(uni).isEqualTo(bi);
    assertThat(bi).isEqualTo(uni);
    assertThat(uni.hashCode()).isEqualTo(bi.hashCode());
  }

  @Test
  void equalsWithNullOrOtherTypeFalse() {
    var g = TestGraphs.path(3);
    assertThat(g.equals(null)).isFalse();
    assertThat(g.equals("not a graph")).isFalse();
  }

  @Test
  void equalsSameInstanceReturnsTrue() {
    var g = TestGraphs.path(3);
    assertThat(g.equals(g)).isTrue();
  }
}
