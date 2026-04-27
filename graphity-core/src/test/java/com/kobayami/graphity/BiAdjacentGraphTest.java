package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BiAdjacentGraphTest {

  /** Standard test fixture: diamond 0->1, 0->2, 1->3, 2->3. */
  private static BiAdjacentGraph diamond() {
    return TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3).biAdjacentGraph();
  }

  @Test
  void inDegreeReportsIncomingEdges() {
    var g = diamond();
    assertThat(g.inDegree(0)).isZero();
    assertThat(g.inDegree(1)).isEqualTo(1);
    assertThat(g.inDegree(2)).isEqualTo(1);
    assertThat(g.inDegree(3)).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void inDegreeOutOfRangeThrows(int node) {
    var g = diamond();
    assertThatThrownBy(() -> g.inDegree(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void degreeIsSumOfInAndOut() {
    var g = diamond();
    for (int i = 0; i < g.nodeCount(); i++) {
      assertThat(g.degree(i)).isEqualTo(g.inDegree(i) + g.outDegree(i));
    }
  }

  @Test
  void degreeCountsSelfLoopTwice() {
    // Self-loop: 0->0. Contributes to in-degree and out-degree independently.
    var g = TestGraphs.of(1, 0, 0).biAdjacentGraph();
    assertThat(g.inDegree(0)).isEqualTo(1);
    assertThat(g.outDegree(0)).isEqualTo(1);
    assertThat(g.degree(0)).isEqualTo(2);
  }

  @Test
  void inNodesReturnsSortedView() {
    // Node 3 is the sink of all three others; in-list must come out sorted [1, 2].
    var g = diamond();
    assertThat(g.inNodes(3).toIntArray()).containsExactly(1, 2);
    assertThat(g.inNodes(0).toIntArray()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void inNodesOutOfRangeThrows(int node) {
    var g = diamond();
    assertThatThrownBy(() -> g.inNodes(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void hasInNodeDetectsIncomingEdges() {
    var g = diamond();
    assertThat(g.hasInNode(3, 1)).isTrue();
    assertThat(g.hasInNode(3, 2)).isTrue();
    assertThat(g.hasInNode(3, 0)).isFalse();
    assertThat(g.hasInNode(0, 1)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void hasInNodeOutOfRangeNodeThrows(int node) {
    var g = diamond();
    assertThatThrownBy(() -> g.hasInNode(node, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void biAdjacentGraphReturnsSelf() {
    var g = diamond();
    assertThatObject(g.biAdjacentGraph()).isSameAs(g);
  }

  @Test
  void uniAdjacentGraphReturnsEqualUniGraph() {
    var bi = diamond();
    var uni = bi.uniAdjacentGraph();
    assertThat(uni).isEqualTo(bi);
    assertThatObject(uni).isNotInstanceOf(BiAdjacentGraph.class);
  }

  @Test
  void transposedSwapsInAndOutEdges() {
    var g = diamond();
    var t = g.transposed();
    // Original out-edges become in-edges of transposed, and vice versa.
    for (int i = 0; i < g.nodeCount(); i++) {
      assertThat(t.outNodes(i).toIntArray()).containsExactly(g.inNodes(i).toIntArray());
      assertThat(t.inNodes(i).toIntArray()).containsExactly(g.outNodes(i).toIntArray());
    }
  }

  @Test
  void transposedIsBiAdjacent() {
    var g = diamond();
    assertThatObject(g.transposed()).isInstanceOf(BiAdjacentGraph.class);
  }

  @Test
  void transposedTwiceEqualsOriginal() {
    var g = diamond();
    assertThat(g.transposed().transposed()).isEqualTo(g);
  }

  @Test
  void neighborsMergesInAndOutSorted() {
    // Node 2 has out-neighbor {3} and in-neighbor {0}; expect [0, 3].
    var g = diamond();
    assertThat(g.neighbors(0).toIntArray()).containsExactly(1, 2);
    assertThat(g.neighbors(2).toIntArray()).containsExactly(0, 3);
    assertThat(g.neighbors(3).toIntArray()).containsExactly(1, 2);
  }

  @Test
  void neighborsDeduplicatesOverlapBetweenInAndOut() {
    // Bidirectional edge 0<->1: appears in both in-list and out-list of each.
    var g = TestGraphs.of(2, 0, 1, 1, 0).biAdjacentGraph();
    assertThat(g.neighbors(0).toIntArray()).containsExactly(1);
    assertThat(g.neighbors(1).toIntArray()).containsExactly(0);
  }

  @Test
  void neighborsIncludesSelfOnSelfLoop() {
    var g = TestGraphs.of(2, 0, 0, 0, 1).biAdjacentGraph();
    assertThat(g.neighbors(0).toIntArray()).containsExactly(0, 1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void neighborsOutOfRangeThrows(int node) {
    var g = diamond();
    assertThatThrownBy(() -> g.neighbors(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void hasNeighborDetectsEitherDirection() {
    var g = diamond();
    assertThat(g.hasNeighbor(0, 1)).isTrue();
    assertThat(g.hasNeighbor(1, 0)).isTrue();
    assertThat(g.hasNeighbor(3, 1)).isTrue();
    assertThat(g.hasNeighbor(1, 3)).isTrue();
    assertThat(g.hasNeighbor(1, 2)).isFalse();
  }

  @Test
  void isRootNodeTrueForSources() {
    var g = diamond();
    assertThat(g.isRootNode(0)).isTrue();
    assertThat(g.isRootNode(1)).isFalse();
    assertThat(g.isRootNode(2)).isFalse();
    assertThat(g.isRootNode(3)).isFalse();
  }

  @Test
  void isRootNodeFalseOnSelfLoop() {
    var g = TestGraphs.of(1, 0, 0).biAdjacentGraph();
    assertThat(g.isRootNode(0)).isFalse();
  }

  @Test
  void rootNodesReturnsAllSources() {
    var g = diamond();
    assertThat(g.rootNodes().toIntArray()).containsExactly(0);
  }

  @Test
  void rootNodesOnIsolatedGraphReturnsAllNodes() {
    var g = TestGraphs.isolated(3).biAdjacentGraph();
    assertThat(g.rootNodes().toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void rootNodesOnCycleIsEmpty() {
    var g = TestGraphs.cycle(4).biAdjacentGraph();
    assertThat(g.rootNodes().isEmpty()).isTrue();
  }

  @Test
  void rootNodesOnEmptyGraphIsEmpty() {
    assertThat(TestGraphs.empty().biAdjacentGraph().rootNodes().isEmpty()).isTrue();
  }
}
