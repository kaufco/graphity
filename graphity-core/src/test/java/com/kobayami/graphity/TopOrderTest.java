package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

class TopOrderTest {

  @Test
  void ofEmptyGraphIsEmpty() {
    assertThat(TopOrder.of(TestGraphs.empty()).isEmpty()).isTrue();
  }

  @Test
  void ofIsolatedNodesContainsAllNodes() {
    var order = TopOrder.of(TestGraphs.isolated(3));
    assertThat(order.size()).isEqualTo(3);
    assertThat(order.toIntArray()).containsExactlyInAnyOrder(0, 1, 2);
  }

  @Test
  void ofPathReturnsNodesInForwardOrder() {
    assertThat(TopOrder.of(TestGraphs.path(5)).toIntArray())
        .containsExactly(0, 1, 2, 3, 4);
  }

  @Test
  void ofDiamondDagPlacesSourceBeforeAllSuccessors() {
    // 0 -> 1, 0 -> 2, 1 -> 3, 2 -> 3.
    var order = TopOrder.of(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3));
    assertInvariantSourceBeforeTarget(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3), order);
    // Source is first, sink is last.
    assertThat(order.getInt(0)).isZero();
    assertThat(order.getInt(3)).isEqualTo(3);
  }

  @Test
  void ofBinaryTreePlacesRootFirst() {
    // Tree: 0 -> 1, 0 -> 2; 1 -> 3, 1 -> 4; 2 -> 5, 2 -> 6.
    var g = TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6);
    var order = TopOrder.of(g);
    assertThat(order.getInt(0)).isZero();
    assertInvariantSourceBeforeTarget(g, order);
  }

  @Test
  void ofDisconnectedDagsIncludesAllNodes() {
    // Two disjoint DAGs: {0 -> 1} and {2 -> 3 -> 4}.
    var g = TestGraphs.of(5, 0, 1, 2, 3, 3, 4);
    var order = TopOrder.of(g);
    assertThat(order.size()).isEqualTo(5);
    assertThat(order.toIntArray()).containsExactlyInAnyOrder(0, 1, 2, 3, 4);
    assertInvariantSourceBeforeTarget(g, order);
  }

  @Test
  void ofGraphWithCycleThrows() {
    assertThatThrownBy(() -> TopOrder.of(TestGraphs.cycle(3)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");
  }

  @Test
  void ofGraphWithSelfLoopThrows() {
    var g = TestGraphs.of(2, 0, 0, 0, 1);
    assertThatThrownBy(() -> TopOrder.of(g)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofGraphWithCycleInSubcomponentThrows() {
    // DAG component + cycle component.
    var g = TestGraphs.of(5,
        0, 1,
        2, 3, 3, 4, 4, 2);
    assertThatThrownBy(() -> TopOrder.of(g)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofReturnsUnmodifiableList() {
    var order = TopOrder.of(TestGraphs.path(3));
    assertThatThrownBy(() -> order.set(0, 99))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  /** Verifies the topological invariant: for every edge u -> v, u precedes v in the order. */
  private static void assertInvariantSourceBeforeTarget(Graph graph, IntList order) {
    int[] position = new int[graph.nodeCount()];
    for (int i = 0; i < order.size(); i++) position[order.getInt(i)] = i;
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        assertThat(position[u])
            .as("edge %d -> %d: position[%d]=%d must precede position[%d]=%d",
                u, v, u, position[u], v, position[v])
            .isLessThan(position[v]);
      }
    }
  }
}
