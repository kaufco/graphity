package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TopOrderAndLevelsTest {

  @Test
  void ofEmptyGraphHasZeroLevels() {
    var tol = TopOrderAndLevels.of(TestGraphs.empty());
    assertThat(tol.order().isEmpty()).isTrue();
    assertThat(tol.levels().isEmpty()).isTrue();
    assertThat(tol.levelCount()).isZero();
  }

  @Test
  void ofIsolatedNodesPutsAllAtLevelZero() {
    var tol = TopOrderAndLevels.of(TestGraphs.isolated(3));
    assertThat(tol.order().size()).isEqualTo(3);
    assertThat(tol.levelCount()).isEqualTo(1);
    for (int i = 0; i < 3; i++) {
      assertThat(tol.levelOf(i)).isZero();
    }
  }

  @Test
  void ofPathAssignsIncrementingLevels() {
    var tol = TopOrderAndLevels.of(TestGraphs.path(5));
    for (int i = 0; i < 5; i++) {
      assertThat(tol.levelOf(i)).isEqualTo(i);
    }
    assertThat(tol.levelCount()).isEqualTo(5);
  }

  @Test
  void ofDiamondAssignsSharedLevels() {
    // 0 -> 1, 0 -> 2, 1 -> 3, 2 -> 3.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var tol = TopOrderAndLevels.of(g);
    assertThat(tol.levelOf(0)).isZero();
    assertThat(tol.levelOf(1)).isEqualTo(1);
    assertThat(tol.levelOf(2)).isEqualTo(1);
    assertThat(tol.levelOf(3)).isEqualTo(2);
    assertThat(tol.levelCount()).isEqualTo(3);
  }

  @Test
  void ofLevelIsMaxPredecessorPlusOne() {
    // 0 -> 1, 0 -> 3, 1 -> 2, 2 -> 3.
    // Level 0: 0. Level 1: 1. Level 2: 2. Level 3: 3 (max of 0-via-0=1 and 2-via-2=3).
    var g = TestGraphs.of(4, 0, 1, 0, 3, 1, 2, 2, 3);
    var tol = TopOrderAndLevels.of(g);
    assertThat(tol.levelOf(0)).isZero();
    assertThat(tol.levelOf(1)).isEqualTo(1);
    assertThat(tol.levelOf(2)).isEqualTo(2);
    assertThat(tol.levelOf(3)).isEqualTo(3);
  }

  @Test
  void ofDisconnectedDagsAssignsIndependentLevels() {
    // {0 -> 1} and {2 -> 3 -> 4}.
    var g = TestGraphs.of(5, 0, 1, 2, 3, 3, 4);
    var tol = TopOrderAndLevels.of(g);
    assertThat(tol.levelOf(0)).isZero();
    assertThat(tol.levelOf(1)).isEqualTo(1);
    assertThat(tol.levelOf(2)).isZero();
    assertThat(tol.levelOf(3)).isEqualTo(1);
    assertThat(tol.levelOf(4)).isEqualTo(2);
    assertThat(tol.levelCount()).isEqualTo(3);
  }

  @Test
  void ofOrderSatisfiesTopologicalInvariant() {
    var g = TestGraphs.of(6, 0, 1, 0, 2, 1, 3, 2, 3, 3, 4, 3, 5);
    var tol = TopOrderAndLevels.of(g);
    assertInvariantSourceBeforeTarget(g, tol.order());
  }

  @Test
  void ofLevelsAndLevelOfAgreeForAllNodes() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var tol = TopOrderAndLevels.of(g);
    for (int i = 0; i < g.nodeCount(); i++) {
      assertThat(tol.levels().getInt(i)).isEqualTo(tol.levelOf(i));
    }
  }

  @Test
  void ofGraphWithCycleThrows() {
    assertThatThrownBy(() -> TopOrderAndLevels.of(TestGraphs.cycle(3)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");
  }

  @Test
  void ofGraphWithSelfLoopThrows() {
    var g = TestGraphs.of(2, 0, 0, 0, 1);
    assertThatThrownBy(() -> TopOrderAndLevels.of(g))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofReturnsUnmodifiableLists() {
    var tol = TopOrderAndLevels.of(TestGraphs.path(3));
    assertThatThrownBy(() -> tol.order().set(0, 99))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> tol.levels().set(0, 99))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void ofUsesBiAdjacentFastPathConsistently() {
    // Same graph computed via Graph and via BiAdjacentGraph — must produce identical results.
    var g = TestGraphs.of(5, 0, 1, 0, 2, 1, 3, 2, 3, 3, 4);
    var bi = g.biAdjacentGraph();
    var tolUni = TopOrderAndLevels.of(g);
    var tolBi = TopOrderAndLevels.of(bi);
    assertThat(tolUni.order().toIntArray()).containsExactly(tolBi.order().toIntArray());
    assertThat(tolUni.levels().toIntArray()).containsExactly(tolBi.levels().toIntArray());
    assertThat(tolUni.levelCount()).isEqualTo(tolBi.levelCount());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void levelOfOutOfRangeThrows(int node) {
    var tol = TopOrderAndLevels.of(TestGraphs.path(4));
    assertThatThrownBy(() -> tol.levelOf(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  private static void assertInvariantSourceBeforeTarget(Graph graph, IntList order) {
    int[] position = new int[graph.nodeCount()];
    for (int i = 0; i < order.size(); i++) position[order.getInt(i)] = i;
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        assertThat(position[u]).isLessThan(position[v]);
      }
    }
  }
}
