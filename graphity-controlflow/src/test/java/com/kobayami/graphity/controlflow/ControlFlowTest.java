package com.kobayami.graphity.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.testutils.ControlFlowGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

class ControlFlowTest {

  private static BiAdjacentGraph bi(int n, int... edges) {
    return BiAdjacentGraph.of(TestGraphs.of(n, edges));
  }

  @Test
  void emptyGraph() {
    var d = Dominators.of(BiAdjacentGraph.of(TestGraphs.empty()), IntList.of());
    assertThat(ControlFlow.dominanceFrontiers(d).nodeCount()).isZero();
  }

  @Test
  void dominanceFrontiersOfALoop() {
    // 0 -> 1 -> 2 -> 1, 2 -> 3: the loop header is in its own frontier and in its body's.
    var d = Dominators.of(bi(4, 0, 1, 1, 2, 2, 1, 2, 3), 0);
    var df = ControlFlow.dominanceFrontiers(d);
    assertThat(df.outNodes(0).toIntArray()).isEmpty();
    assertThat(df.outNodes(1).toIntArray()).containsExactly(1);
    assertThat(df.outNodes(2).toIntArray()).containsExactly(1);
    assertThat(df.outNodes(3).toIntArray()).isEmpty();
  }

  @Test
  void dominanceFrontiersOfADiamond() {
    var d = Dominators.of(bi(4, 0, 1, 0, 2, 1, 3, 2, 3), 0);
    var df = ControlFlow.dominanceFrontiers(d);
    assertThat(df.outNodes(1).toIntArray()).containsExactly(3);
    assertThat(df.outNodes(2).toIntArray()).containsExactly(3);
    assertThat(df.edgeCount()).isEqualTo(2);
  }

  @Test
  void unreachableNodesHaveEmptyFrontiersAndAppearInNone() {
    // 3 is unreachable and has an edge into the join point 2.
    var d = Dominators.of(bi(4, 0, 1, 1, 2, 0, 2, 3, 2), 0);
    var df = ControlFlow.dominanceFrontiers(d);
    assertThat(df.outNodes(3).toIntArray()).isEmpty();
    assertThat(df.outNodes(1).toIntArray()).containsExactly(2);
    assertThat(df.edgeCount()).isEqualTo(1);
  }

  @Test
  void ladderFrontierGrowsQuadratically() {
    int rungs = 50;
    var df = ControlFlow.dominanceFrontiers(
        Dominators.of(BiAdjacentGraph.of(ControlFlowGraphs.ladder(rungs)), 0));
    // b_i (= rungs + i) is in the frontier of every a_j with 1 <= j <= i, plus the exit.
    assertThat(df.edgeCount()).isGreaterThan(rungs * (rungs - 1) / 2);
  }

  @Test
  void controlDependenceIsTheTransposedFrontierOfThePostDominators() {
    // if (0) { 1 } ; 2   -  0 -> 1, 0 -> 2, 1 -> 2, exit 2.
    var cfg = bi(3, 0, 1, 0, 2, 1, 2);
    var postDominators = Dominators.of(cfg.transposed(), 2);
    var controlDependence =
        BiAdjacentGraph.of(ControlFlow.dominanceFrontiers(postDominators)).transposed();
    assertThat(controlDependence.edgeCount()).isEqualTo(1);
    assertThat(controlDependence.hasOutNode(0, 1)).isTrue();
  }

  @Test
  void deepChainDoesNotOverflowStack() {
    int deep = 1_000_000;
    var dominators = Dominators.of(BiAdjacentGraph.of(TestGraphs.path(deep)), 0);
    assertThat(ControlFlow.dominanceFrontiers(dominators).edgeCount()).isZero();

    // One back edge from the end of the chain to its middle h: h is in the frontier of every
    // node from h to the end.
    var builder = new GraphBuilder();
    builder.addNodes(deep);
    for (int i = 0; i + 1 < deep; i++) builder.addEdge(i, i + 1);
    builder.addEdge(deep - 1, deep / 2);
    var looped = Dominators.of(BiAdjacentGraph.of(builder.build()), 0);
    var df = ControlFlow.dominanceFrontiers(looped);
    assertThat(df.outNodes(deep - 1).toIntArray()).containsExactly(deep / 2);
    assertThat(df.edgeCount()).isEqualTo(deep - deep / 2);
  }
}
