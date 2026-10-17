package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RecordingBiAdjacentDfsTraverser;
import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

class BiAdjacentDfsTraverserTest {

  @Test
  void runFromRootsVisitsEntireDag() {
    // DAG with single root 0: 0 -> 1, 2; 1 -> 3; 2 -> 3.
    var g = BiAdjacentGraph.of(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 3, 2);
    assertThat(t.postOrder.toIntArray()).containsExactly(3, 1, 2, 0);
  }

  @Test
  void runFromRootsStartsAtEachRoot() {
    // Two roots (0 and 3), each reaching one sink: 0 -> 1 -> 2, 3 -> 2.
    var g = BiAdjacentGraph.of(TestGraphs.of(4, 0, 1, 1, 2, 3, 2));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromRootsVisitsRootsInAscendingOrder() {
    // Three isolated nodes as roots.
    var g = BiAdjacentGraph.of(TestGraphs.isolated(3));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void runFromRootsSkipsNodesInCycleWithoutExternalEntry() {
    // Pure cycle: no root. runFromRoots visits nothing.
    var g = BiAdjacentGraph.of(TestGraphs.cycle(4));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    assertThat(t.preOrder.isEmpty()).isTrue();
  }

  @Test
  void runFromRootsVisitsCycleReachableFromExternalRoot() {
    // Root 0 -> 1 -> 2 -> 3 -> 1 (cycle via 1, 2, 3; 0 is root).
    var g = BiAdjacentGraph.of(TestGraphs.of(4, 0, 1, 1, 2, 2, 3, 3, 1));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromRootsOnEmptyGraphIsNoOp() {
    var g = BiAdjacentGraph.of(TestGraphs.empty());
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    assertThat(t.preOrder.isEmpty()).isTrue();
  }

  @Test
  void runFromRootsSkipsAlreadyVisitedRoots() {
    // Two separate roots 0 and 2: 0 -> 1, 2 -> 3.
    var g = BiAdjacentGraph.of(TestGraphs.of(4, 0, 1, 2, 3));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.run(0);
    t.runFromRoots();
    // First run visited {0, 1}. runFromRoots then only starts from unvisited root 2.
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromRootsCombinedWithRunCoversAllNodes() {
    // DAG reachable from roots plus a pure cycle {3, 4, 5}.
    var g = BiAdjacentGraph.of(TestGraphs.of(6,
        0, 1, 1, 2,
        3, 4, 4, 5, 5, 3));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    // Only {0, 1, 2} reachable from roots.
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
    t.run();
    // run() picks up the cycle as a forest.
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3, 4, 5);
  }

  @Test
  void runFromRootsMultipleCallsDoNotRevisit() {
    var g = BiAdjacentGraph.of(TestGraphs.path(3));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    t.runFromRoots();
    t.runFromRoots();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void biAdjacentGraphFieldIsSameInstanceAsGraph() {
    var g = BiAdjacentGraph.of(TestGraphs.path(3));
    var t = new RecordingBiAdjacentDfsTraverser(g);
    assertThat(t.biAdjacentGraph).isSameAs(t.graph);
  }
}
