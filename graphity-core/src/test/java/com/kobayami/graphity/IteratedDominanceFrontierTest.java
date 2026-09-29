package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.ControlFlowGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

class IteratedDominanceFrontierTest {

  private static IteratedDominanceFrontier of(Graph graph) {
    return IteratedDominanceFrontier.of(Dominators.of(graph.biAdjacentGraph(), 0));
  }

  @Test
  void definitionInOneBranchOfADiamondNeedsAPhiAtTheJoin() {
    var idf = of(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3));
    assertThat(idf.frontierOf(IntList.of(1)).toIntArray()).containsExactly(3);
    assertThat(idf.frontierOf(IntList.of(1, 2)).toIntArray()).containsExactly(3);
    assertThat(idf.frontierOf(IntList.of(0)).toIntArray()).isEmpty();
  }

  @Test
  void definitionInALoopBodyNeedsAPhiAtTheHeader() {
    // 0 -> 1 -> 2 -> 1, 2 -> 3
    var idf = of(TestGraphs.of(4, 0, 1, 1, 2, 2, 1, 2, 3));
    assertThat(idf.frontierOf(IntList.of(2)).toIntArray()).containsExactly(1);
    assertThat(idf.frontierOf(IntList.of(1)).toIntArray()).containsExactly(1);
  }

  @Test
  void iterationFollowsFrontiersOfFrontiers() {
    // Nested diamonds: 0 -> {1, 2}, 1 -> {3, 4}, {3, 4} -> 5, {5, 2} -> 6.
    // DF(3) = {5}, DF(5) = {6}: one definition in 3 needs phis at 5 and 6.
    var idf = of(TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 3, 5, 4, 5, 5, 6, 2, 6));
    assertThat(idf.frontierOf(IntList.of(3)).toIntArray()).containsExactly(5, 6);
  }

  @Test
  void ladderDefinitionReachesEveryJoinBelowIt() {
    int rungs = 10;
    var idf = of(ControlFlowGraphs.ladder(rungs));
    // A definition in a_1 needs phis at b_1 .. b_9 and at the exit.
    var phis = idf.frontierOf(IntList.of(1)).toIntArray();
    assertThat(phis).hasSize(rungs);
    assertThat(phis[0]).isEqualTo(rungs + 1);
    assertThat(phis[rungs - 1]).isEqualTo(2 * rungs);
  }

  @Test
  void unreachableAndDuplicateNodesAreHarmless() {
    var idf = of(TestGraphs.of(5, 0, 1, 0, 2, 1, 3, 2, 3));
    assertThat(idf.frontierOf(IntList.of(4)).toIntArray()).isEmpty();
    assertThat(idf.frontierOf(IntList.of(1, 1, 4)).toIntArray()).containsExactly(3);
  }

  @Test
  void emptyInputGivesAnEmptyFrontier() {
    var idf = of(TestGraphs.of(2, 0, 1));
    assertThat(idf.frontierOf(IntList.of()).toIntArray()).isEmpty();
  }

  @Test
  void outOfRangeNodesAreRejected() {
    var idf = of(TestGraphs.of(2, 0, 1));
    assertThatThrownBy(() -> idf.frontierOf(IntList.of(2)))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> idf.frontierOf(IntList.of(-1)))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void severalEntries() {
    // Entries 0 and 3 both lead to 1; a definition in 0 needs a phi at 1.
    var idf = IteratedDominanceFrontier.of(
        Dominators.of(TestGraphs.of(4, 0, 1, 3, 1, 1, 2).biAdjacentGraph(), IntList.of(0, 3)));
    assertThat(idf.frontierOf(IntList.of(0)).toIntArray()).containsExactly(1);
  }
}
