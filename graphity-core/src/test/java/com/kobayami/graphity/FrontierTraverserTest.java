package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RecordingFrontierTraverser;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FrontierTraverserTest {

  @Test
  void runOnPathGivesBfsOrder() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(4));
    t.run(0);
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runOnBinaryTreeGivesLevelOrder() {
    // 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6.
    var g = TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6);
    var t = new RecordingFrontierTraverser(g);
    t.run(0);
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3, 4, 5, 6);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 1, 1, 2, 2, 2, 2);
  }

  @Test
  void runCoversDisconnectedForestInNodeOrder() {
    // Two components: 0->1 and 2->3.
    var g = TestGraphs.of(4, 0, 1, 2, 3);
    var t = new RecordingFrontierTraverser(g);
    t.run();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runOnEmptyGraphIsNoOp() {
    var t = new RecordingFrontierTraverser(TestGraphs.empty());
    t.run();
    assertThat(t.visitOrder.isEmpty()).isTrue();
    assertThat(t.completedLevels.isEmpty()).isTrue();
  }

  @Test
  void runOnCycleTerminates() {
    var t = new RecordingFrontierTraverser(TestGraphs.cycle(5));
    t.run(0);
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3, 4);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 1, 2, 3, 4);
  }

  @Test
  void runOnOutStarGivesRootAtLevelZero() {
    var t = new RecordingFrontierTraverser(TestGraphs.outStar(5));
    t.run(0);
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3, 4);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 1, 1, 1, 1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void runFromNodeOutOfRangeThrows(int startNode) {
    var t = new RecordingFrontierTraverser(TestGraphs.isolated(4));
    assertThatThrownBy(() -> t.run(startNode))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void runFromNodesAllSeedsAtLevelZero() {
    // Disjoint: 0->1, 2->3, 4->5.
    var g = TestGraphs.of(6, 0, 1, 2, 3, 4, 5);
    var t = new RecordingFrontierTraverser(g);
    t.runFromNodes(IntList.of(4, 0, 2));
    assertThat(t.visitOrder.toIntArray()).containsExactly(4, 0, 2, 5, 1, 3);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 0, 0, 1, 1, 1);
  }

  @Test
  void runFromNodesSkipsAlreadyVisited() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(4));
    t.run(0);
    t.runFromNodes(IntList.of(1, 2));
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromEmptyNodeListIsNoOp() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(3));
    t.runFromNodes(new IntArrayList());
    assertThat(t.visitOrder.isEmpty()).isTrue();
  }

  @Test
  void multipleRunCallsDoNotRevisit() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(3));
    t.run(0);
    t.run(0);
    t.run();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void preMarkedNodesAreExcludedFromTraversal() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(5));
    t.visited.set(2);
    t.visited.set(3);
    t.run(0);
    // From 0: visits 0, 1; stops at pre-marked 2.
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1);
  }

  @Test
  void currentLevelIsAvailableDuringVisit() {
    // Path 0->1->2->3. Check currentLevel at each visit.
    var g = TestGraphs.path(4);
    var levels = new IntArrayList();
    var t = new FrontierTraverser(g) {
      @Override protected void visitNode(int node) {
        levels.add(currentLevel());
        enqueueChildren(node);
      }
    };
    t.run(0);
    assertThat(levels.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void onLevelCompleteFiresForEachLevel() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(4));
    t.run(0);
    assertThat(t.completedLevels.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void onLevelCompleteFiresOncePerTreeInForest() {
    // Two components: 0->1 (2 levels) and 2->3->4 (3 levels).
    var g = TestGraphs.of(5, 0, 1, 2, 3, 3, 4);
    var t = new RecordingFrontierTraverser(g);
    t.run();
    // First tree: levels 0, 1. Second tree: levels 0, 1, 2.
    assertThat(t.completedLevels.toIntArray()).containsExactly(0, 1, 0, 1, 2);
  }

  @Test
  void onLevelCompleteNotFiredForEmptyGraph() {
    var t = new RecordingFrontierTraverser(TestGraphs.empty());
    t.run();
    assertThat(t.completedLevels.isEmpty()).isTrue();
  }

  @Test
  void pruningByOmittingEnqueueChildren() {
    // Binary tree: 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6. Prune at node 1.
    var g = TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6);
    var seen = new IntArrayList();
    var t = new FrontierTraverser(g) {
      @Override protected void visitNode(int node) {
        seen.add(node);
        if (node != 1) enqueueChildren(node);
      }
    };
    t.run(0);
    assertThat(seen.toIntArray()).containsExactly(0, 1, 2, 5, 6);
  }

  @Test
  void enqueueReturnsTrueForNewNodesFalseForVisited() {
    var g = TestGraphs.isolated(3);
    var t = new ExposedEnqueueFrontier(g);
    assertThat(t.callEnqueue(1)).isTrue();
    assertThat(t.callEnqueue(1)).isFalse();
  }

  @Test
  void enqueueSchedulesCustomFrontier() {
    // Disjoint: 0->1, 2->3, 4->5. Use enqueue() to schedule 2 and 4 explicitly.
    var g = TestGraphs.of(6, 0, 1, 2, 3, 4, 5);
    var visits = new IntArrayList();
    var t = new FrontierTraverser(g) {
      @Override protected void visitNode(int node) {
        visits.add(node);
        if (node == 0) {
          enqueue(2);
          enqueue(4);
        } else {
          enqueueChildren(node);
        }
      }
    };
    t.run(0);
    assertThat(visits.toIntArray()).containsExactly(0, 2, 4, 3, 5);
  }

  @Test
  void isVisitedReflectsEnqueuedState() {
    var g = TestGraphs.of(3, 0, 1, 0, 2);
    var allVisitedAfterRoot = new boolean[1];
    var t = new FrontierTraverser(g) {
      @Override protected void visitNode(int node) {
        if (node == 0) {
          enqueueChildren(node);
          // After enqueueChildren, 1 and 2 are marked visited even before visitNode fires.
          allVisitedAfterRoot[0] = isVisited(1) && isVisited(2);
        }
      }
    };
    t.run(0);
    assertThat(allVisitedAfterRoot[0]).isTrue();
  }

  @Test
  void runOnDiamondVisitsTargetOnce() {
    // Diamond 0->1, 0->2, 1->3, 2->3. Node 3 enqueued by 1, skipped by 2.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var t = new RecordingFrontierTraverser(g);
    t.run(0);
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 1, 1, 2);
  }

  private static final class ExposedEnqueueFrontier extends FrontierTraverser {
    ExposedEnqueueFrontier(Graph graph) { super(graph); }
    boolean callEnqueue(int node) { return enqueue(node); }
    @Override protected void visitNode(int node) {}
  }
}
