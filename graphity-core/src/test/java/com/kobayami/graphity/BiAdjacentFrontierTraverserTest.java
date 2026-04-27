package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RecordingBiAdjacentFrontierTraverser;
import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

class BiAdjacentFrontierTraverserTest {

  @Test
  void runFromRootsVisitsDagInBfsOrder() {
    // DAG with single root 0: 0 -> 1, 2; 1 -> 3; 2 -> 3.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 1, 1, 2);
  }

  @Test
  void runFromRootsPutsAllRootsAtLevelZero() {
    // Two roots (0 and 3), each reaching one sink.
    var g = TestGraphs.of(4, 0, 1, 1, 2, 3, 2).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 3, 1, 2);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 0, 1, 1);
  }

  @Test
  void runFromRootsOnIsolatedGraphStartsAllAtLevelZero() {
    var g = TestGraphs.isolated(3).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 0, 0);
  }

  @Test
  void runFromRootsOnPureCycleVisitsNothing() {
    var g = TestGraphs.cycle(4).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.visitOrder.isEmpty()).isTrue();
    assertThat(t.completedLevels.isEmpty()).isTrue();
  }

  @Test
  void runFromRootsVisitsCycleReachableFromExternalRoot() {
    // 0 -> 1; cycle among {1, 2, 3}.
    var g = TestGraphs.of(4, 0, 1, 1, 2, 2, 3, 3, 1).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromRootsOnEmptyGraphIsNoOp() {
    var g = TestGraphs.empty().biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.visitOrder.isEmpty()).isTrue();
    assertThat(t.completedLevels.isEmpty()).isTrue();
  }

  @Test
  void runFromRootsSkipsAlreadyVisitedRoots() {
    // Two roots 0 and 2: 0 -> 1, 2 -> 3.
    var g = TestGraphs.of(4, 0, 1, 2, 3).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.run(0);
    t.runFromRoots();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromRootsCombinedWithRunCoversAllNodes() {
    // DAG 0->1->2 plus pure cycle {3,4,5}.
    var g = TestGraphs.of(6,
        0, 1, 1, 2,
        3, 4, 4, 5, 5, 3).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2);
    t.run();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3, 4, 5);
  }

  @Test
  void runFromRootsMultipleCallsDoNotRevisit() {
    var g = TestGraphs.path(3).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    t.runFromRoots();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void runFromRootsFiresLevelCompleteForRootLevel() {
    var g = TestGraphs.isolated(3).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    t.runFromRoots();
    assertThat(t.completedLevels.toIntArray()).containsExactly(0);
  }

  @Test
  void biAdjacentGraphFieldIsSameInstanceAsGraph() {
    var g = TestGraphs.path(3).biAdjacentGraph();
    var t = new RecordingBiAdjacentFrontierTraverser(g);
    assertThat(t.biAdjacentGraph).isSameAs(t.graph);
  }
}
