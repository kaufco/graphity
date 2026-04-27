package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RecordingDfsTraverser;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DfsTraverserTest {

  @Test
  void runVisitsSinglePathInPreOrder() {
    var t = new RecordingDfsTraverser(TestGraphs.path(4));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
    assertThat(t.postOrder.toIntArray()).containsExactly(3, 2, 1, 0);
  }

  @Test
  void runVisitsBinaryTreeInSortedPreOrder() {
    // Binary tree: 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6.
    var g = TestGraphs.of(7,
        0, 1, 0, 2,
        1, 3, 1, 4,
        2, 5, 2, 6);
    var t = new RecordingDfsTraverser(g);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 3, 4, 2, 5, 6);
    assertThat(t.postOrder.toIntArray()).containsExactly(3, 4, 1, 5, 6, 2, 0);
  }

  @Test
  void runCoversDisconnectedForestInNodeOrder() {
    // Two components: {0,1} as 0->1; {2,3} as 2->3.
    var g = TestGraphs.of(4, 0, 1, 2, 3);
    var t = new RecordingDfsTraverser(g);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runOnIsolatedGraphVisitsAllNodesOnce() {
    var t = new RecordingDfsTraverser(TestGraphs.isolated(3));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.postOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void runOnEmptyGraphIsNoOp() {
    var t = new RecordingDfsTraverser(TestGraphs.empty());
    t.run();
    assertThat(t.preOrder.isEmpty()).isTrue();
    assertThat(t.postOrder.isEmpty()).isTrue();
  }

  @Test
  void runTerminatesOnCycle() {
    var t = new RecordingDfsTraverser(TestGraphs.cycle(5));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3, 4);
  }

  @Test
  void runFromSingleNodeVisitsReachableOnly() {
    // Two components: {0->1->2} and {3->4}.
    var g = TestGraphs.of(5, 0, 1, 1, 2, 3, 4);
    var t = new RecordingDfsTraverser(g);
    t.run(0);
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void runFromSingleNodeInCycleVisitsWholeCycle() {
    var t = new RecordingDfsTraverser(TestGraphs.cycle(4));
    t.run(2);
    assertThat(t.preOrder.toIntArray()).containsExactly(2, 3, 0, 1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void runFromNodeOutOfRangeThrows(int startNode) {
    var t = new RecordingDfsTraverser(TestGraphs.isolated(4));
    assertThatThrownBy(() -> t.run(startNode))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void runFromNodesVisitsInListOrder() {
    // Disjoint: {0->1}, {2->3}, {4->5}.
    var g = TestGraphs.of(6, 0, 1, 2, 3, 4, 5);
    var t = new RecordingDfsTraverser(g);
    t.runFromNodes(IntList.of(4, 0, 2));
    assertThat(t.preOrder.toIntArray()).containsExactly(4, 5, 0, 1, 2, 3);
  }

  @Test
  void runFromNodesSkipsAlreadyVisitedStartNodes() {
    var g = TestGraphs.path(4);
    var t = new RecordingDfsTraverser(g);
    t.run(0);  // visits 0, 1, 2, 3
    t.runFromNodes(IntList.of(1, 2));  // all already visited
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromEmptyNodeListIsNoOp() {
    var t = new RecordingDfsTraverser(TestGraphs.path(3));
    t.runFromNodes(new IntArrayList());
    assertThat(t.preOrder.isEmpty()).isTrue();
  }

  @Test
  void multipleRunCallsDoNotRevisit() {
    var t = new RecordingDfsTraverser(TestGraphs.path(4));
    t.run(0);
    t.run(0);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runThenRunCombinesTraversals() {
    // Two components.
    var g = TestGraphs.of(5, 0, 1, 2, 3, 3, 4);
    var t = new RecordingDfsTraverser(g);
    t.run(2);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(2, 3, 4, 0, 1);
  }

  @Test
  void preMarkedNodesAreExcludedFromTraversal() {
    var t = new RecordingDfsTraverser(TestGraphs.path(5));
    t.visited.set(2);
    t.visited.set(3);
    t.run();
    // Traversal descends 0 -> 1, stops at pre-marked 2, then picks 4 as next root.
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 4);
  }

  @Test
  void cutPrunesSubtreeByOmittingVisitChildren() {
    // Binary tree: 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6. Cut at node 1: subtree 1, 3, 4 pruned below 1.
    var g = TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6);
    var seen = new IntArrayList();
    var cut = new DfsTraverser(g) {
      @Override protected void visitNode(int node) {
        seen.add(node);
        if (node != 1) visitChildren(node);
      }
    };
    cut.run(0);
    assertThat(seen.toIntArray()).containsExactly(0, 1, 2, 5, 6);
  }

  @Test
  void visitChildrenSkipsAlreadyVisitedNeighbors() {
    // Diamond: 0->1, 0->2, 1->3, 2->3. Node 3 visited via node 1; node 2 does not revisit it.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var t = new RecordingDfsTraverser(g);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 3, 2);
    assertThat(t.postOrder.toIntArray()).containsExactly(3, 1, 2, 0);
  }

  @Test
  void visitReturnsTrueFirstTimeFalseAfter() {
    var g = TestGraphs.isolated(3);
    var t = new ExposedVisitDfs(g);
    assertThat(t.callVisit(1)).isTrue();
    assertThat(t.callVisit(1)).isFalse();
    assertThat(t.visitedNodes.toIntArray()).containsExactly(1);
  }

  @Test
  void isVisitedReflectsCurrentTraversalState() {
    var g = TestGraphs.path(3);
    var observed = new IntArrayList();
    var t = new DfsTraverser(g) {
      @Override protected void visitNode(int node) {
        if (isVisited(node)) observed.add(node);
        visitChildren(node);
      }
    };
    t.run();
    // All nodes are marked visited before visitNode is called.
    assertThat(observed.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void manualEdgeIterationViaVisitAndIsVisited() {
    // DAG 0 -> 1, 2; 1 -> 3; 2 -> 3. Node 3 has two predecessors; inspect all edges.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var inspected = new IntArrayList();
    var traverser = new DfsTraverser(g) {
      @Override protected void visitNode(int node) {
        var outs = graph.outNodes(node);
        for (int i = 0; i < outs.size(); i++) {
          int child = outs.getInt(i);
          inspected.add(node);
          inspected.add(child);
          visit(child);
        }
      }
    };
    traverser.run(0);
    // Pairs: (0,1), (0,2), (1,3), (2,3) — all four edges inspected.
    assertThat(inspected.size()).isEqualTo(8);
    assertThat(inspected.getInt(6)).isEqualTo(2);
    assertThat(inspected.getInt(7)).isEqualTo(3);
  }

  @Test
  void runOnCliqueVisitsAllNodes() {
    var t = new RecordingDfsTraverser(TestGraphs.clique(4));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runOnOutStarVisitsCenterFirst() {
    // out-star: 0 -> 1, 2, 3.
    var t = new RecordingDfsTraverser(TestGraphs.outStar(4));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  private static final class ExposedVisitDfs extends DfsTraverser {
    final IntArrayList visitedNodes = new IntArrayList();
    ExposedVisitDfs(Graph graph) { super(graph); }
    boolean callVisit(int node) { return visit(node); }
    @Override protected void visitNode(int node) { visitedNodes.add(node); }
  }
}
