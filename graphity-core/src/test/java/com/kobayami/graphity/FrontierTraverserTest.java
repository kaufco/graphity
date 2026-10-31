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

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void runFromNodeListOutOfRangeThrowsBeforeAnySeed(int startNode) {
    var t = new RecordingFrontierTraverser(TestGraphs.isolated(4));
    assertThatThrownBy(() -> t.run(IntList.of(0, startNode)))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(t.visitOrder.isEmpty()).isTrue();
    assertThat(t.isVisited(0)).as("valid seed before the bad one").isFalse();
  }

  @Test
  void runFromNodeListPutsAllSeedsAtLevelZero() {
    // Disjoint: 0->1, 2->3, 4->5.
    var g = TestGraphs.of(6, 0, 1, 2, 3, 4, 5);
    var t = new RecordingFrontierTraverser(g);
    t.run(IntList.of(4, 0, 2));
    assertThat(t.visitOrder.toIntArray()).containsExactly(4, 0, 2, 5, 1, 3);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 0, 0, 1, 1, 1);
    assertThat(t.completedLevels.toIntArray()).containsExactly(0, 1);
  }

  @Test
  void runFromTheRootNodesTreatsAllRootsAsLevelZero() {
    // Roots 0 and 3: 0 -> 1 -> 2, 3 -> 2.
    var g = BiAdjacentGraph.of(TestGraphs.of(4, 0, 1, 1, 2, 3, 2));
    var t = new RecordingFrontierTraverser(g);
    t.run(Graphs.rootNodes(g));
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 3, 1, 2);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 0, 1, 1);
    assertThat(t.completedLevels.toIntArray()).containsExactly(0, 1);
  }

  @Test
  void runFromNodeListSkipsAlreadyVisited() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(4));
    t.run(0);
    t.run(IntList.of(1, 2));
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void everyEntryPointSkipsVisitedNodes() {
    // 0 -> 1, plus isolated 2 and 3; duplicates and repeated starts must not revisit.
    var t = new RecordingFrontierTraverser(TestGraphs.of(4, 0, 1));
    t.run(0);
    t.run(1);
    t.run(IntList.of(0, 2, 2));
    t.run();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1, 2, 3);
    assertThat(t.completedLevels.toIntArray()).containsExactly(0, 1, 0, 0);
  }

  @Test
  void runFromEmptyNodeListIsNoOp() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(3));
    t.run(new IntArrayList());
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
  void leaveLevelFiresForEachLevel() {
    var t = new RecordingFrontierTraverser(TestGraphs.path(4));
    t.run(0);
    assertThat(t.completedLevels.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void leaveLevelFiresOncePerTreeInForest() {
    // Two components: 0->1 (2 levels) and 2->3->4 (3 levels).
    var g = TestGraphs.of(5, 0, 1, 2, 3, 3, 4);
    var t = new RecordingFrontierTraverser(g);
    t.run();
    // First tree: levels 0, 1. Second tree: levels 0, 1, 2.
    assertThat(t.completedLevels.toIntArray()).containsExactly(0, 1, 0, 1, 2);
  }

  @Test
  void leaveLevelNotFiredForEmptyGraph() {
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

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 1000})
  void enqueueOutOfRangeThrows(int node) {
    var t = new ExposedEnqueueFrontier(TestGraphs.isolated(3));
    assertThatThrownBy(() -> t.callEnqueue(node)).isInstanceOf(IndexOutOfBoundsException.class);
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

  // ---- enqueue outside a run ----

  @Test
  void nodesEnqueuedBeforeARunFormItsLevelZeroInEveryRunVariant() {
    // 0 -> 1 and 3 -> 2. After run(0), the 3 is enqueued outside a run; the start 0 of run(0)
    // and run(IntList.of(0)) is visited already.
    for (int variant = 0; variant < 3; variant++) {
      var t = new RecordingFrontierTraverser(TestGraphs.of(4, 0, 1, 3, 2));
      t.run(0);
      t.enqueue(3);
      switch (variant) {
        case 0 -> t.run();
        case 1 -> t.run(0);
        default -> t.run(IntList.of(0));
      }
      assertThat(t.visitOrder.toIntArray()).as("variant %d", variant).containsExactly(0, 1, 3, 2);
      assertThat(t.levelAtVisit.toIntArray()).as("variant %d", variant)
          .containsExactly(0, 1, 0, 1);
      assertThat(t.completedLevels.toIntArray()).as("variant %d", variant)
          .containsExactly(0, 1, 0, 1);
    }
  }

  @Test
  void runVisitsANodeEnqueuedBeforeItEvenWithNoRootLeft() {
    var t = new RecordingFrontierTraverser(TestGraphs.isolated(2));
    t.run(0);
    t.enqueue(1);
    t.run();
    assertThat(t.visitOrder.toIntArray()).containsExactly(0, 1);
  }

  @Test
  void nodesEnqueuedBeforeARunComeAheadOfItsStartNode() {
    // 0 -> 1 and 3 -> 2: the 3 joins the start 0 in level 0, ahead of it.
    var t = new RecordingFrontierTraverser(TestGraphs.of(4, 0, 1, 3, 2));
    t.enqueue(3);
    t.run(0);
    assertThat(t.visitOrder.toIntArray()).containsExactly(3, 0, 2, 1);
    assertThat(t.levelAtVisit.toIntArray()).containsExactly(0, 0, 1, 1);
  }

  // ---- stop() ----

  @Test
  void stopAtTheFirstSeedDropsTheOtherSeedsOfTheSameLevel() {
    var t = new StoppingFrontier(TestGraphs.of(4, 0, 1, 2, 3));
    t.stopOnVisit = 0;
    t.run(IntList.of(0, 2));
    assertThat(t.visits.toIntArray()).containsExactly(0);
    assertThat(t.levels.toIntArray()).isEmpty();
  }

  /**
   * Records visits, their levels and completed levels; stops when visiting a node or completing a
   * level, and enqueues a node when completing a level.
   */
  private static class StoppingFrontier extends FrontierTraverser {
    final IntArrayList visits = new IntArrayList();
    final IntArrayList visitLevels = new IntArrayList();
    final IntArrayList levels = new IntArrayList();
    int stopOnVisit = -1;
    int stopOnLevel = -1;
    int enqueueOnLevel = -1;
    int nodeToEnqueue = -1;

    StoppingFrontier(Graph graph) {
      super(graph);
    }

    @Override protected void visitNode(int node) {
      visits.add(node);
      visitLevels.add(currentLevel());
      enqueueChildren(node);
      if (node == stopOnVisit) {
        stop();
      }
    }

    @Override protected void leaveLevel(int level) {
      levels.add(level);
      if (level == enqueueOnLevel) {
        enqueue(nodeToEnqueue);
      }
      if (level == stopOnLevel) {
        stop();
      }
    }
  }

  @Test
  void nodesEnqueuedWhenTheLastLevelCompletesFormTheNextLevel() {
    // 0 -> 1, plus isolated 2 and 3; level 1 is the last level of run(0) and enqueues 2.
    var t = new StoppingFrontier(TestGraphs.of(4, 0, 1));
    t.enqueueOnLevel = 1;
    t.nodeToEnqueue = 2;
    t.run(0);
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.visitLevels.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.levels.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void nodesEnqueuedWhenAnInnerLevelCompletesJoinTheNextLevel() {
    // 0 -> 1, plus isolated 2 and 3; level 0 enqueues 2 next to the child 1.
    var t = new StoppingFrontier(TestGraphs.of(4, 0, 1));
    t.enqueueOnLevel = 0;
    t.nodeToEnqueue = 2;
    t.run(0);
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.visitLevels.toIntArray()).containsExactly(0, 1, 1);
    assertThat(t.levels.toIntArray()).containsExactly(0, 1);
  }

  @Test
  void stopWhenTheLastLevelCompletesDropsTheNodesItEnqueued() {
    var t = new StoppingFrontier(TestGraphs.of(4, 0, 1));
    t.enqueueOnLevel = 1;
    t.nodeToEnqueue = 2;
    t.stopOnLevel = 1;
    t.run(0);
    assertThat(t.visits.toIntArray()).containsExactly(0, 1);
    assertThat(t.isVisited(2)).as("enqueued before the stop").isTrue();

    t.enqueueOnLevel = -1;
    t.stopOnLevel = -1;
    t.run();
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 3);
    assertThat(t.visitLevels.toIntArray()).containsExactly(0, 1, 0);
  }

  @Test
  void stopInVisitNodeDropsTheRestOfTheFrontier() {
    // 0 -> 1, 0 -> 2, 1 -> 3; stop while visiting 1: 2 and 3 are enqueued but dropped.
    var t = new StoppingFrontier(TestGraphs.of(5, 0, 1, 0, 2, 1, 3));
    t.stopOnVisit = 1;
    t.run();
    assertThat(t.visits.toIntArray()).containsExactly(0, 1);
    assertThat(t.levels.toIntArray()).containsExactly(0);
    assertThat(t.isVisited(2)).as("enqueued before the stop").isTrue();
    assertThat(t.isVisited(4)).isFalse();

    t.stopOnVisit = -1;
    t.run();
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 4);
  }

  @Test
  void stopInLeaveLevelEndsTheRun() {
    // Binary tree: 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6.
    var t = new StoppingFrontier(TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6));
    t.stopOnLevel = 1;
    t.run(0);
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.levels.toIntArray()).containsExactly(0, 1);
  }

  @Test
  void stopEndsTheWholeRunOverAllRoots() {
    var t = new StoppingFrontier(TestGraphs.isolated(3));
    t.stopOnLevel = 0;
    t.run();
    assertThat(t.visits.toIntArray()).containsExactly(0);
    t.stopOnLevel = -1;
    t.run();
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void runOfOneNodeClearsAnEarlierStop() {
    var t = new StoppingFrontier(TestGraphs.of(3, 1, 2));
    t.stopOnVisit = 0;
    t.run(0);
    t.stopOnVisit = -1;
    t.run(1);
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void runFromNodeListClearsAnEarlierStop() {
    var t = new StoppingFrontier(TestGraphs.of(3, 1, 2));
    t.stopOnVisit = 0;
    t.run(0);
    t.stopOnVisit = -1;
    t.run(IntList.of(1));
    assertThat(t.visits.toIntArray()).containsExactly(0, 1, 2);
  }

  // ---- run from a hook ----

  /**
   * Calls the run method {@link #variant} of its own from the hook {@link #nestIn} while
   * {@link #nest} is set.
   */
  private static final class NestingFrontier extends FrontierTraverser {
    final IntArrayList visits = new IntArrayList();
    int nestIn;  // 0: visitNode, 1: leaveLevel
    int variant;  // 0: run(), 1: run(int), 2: run(IntList)
    boolean nest = true;

    NestingFrontier(Graph graph) {
      super(graph);
    }

    @Override protected void visitNode(int node) {
      visits.add(node);
      enqueueChildren(node);
      if (nestIn == 0 && nest) runVariant(3);
    }

    @Override protected void leaveLevel(int level) {
      if (nestIn == 1 && nest) runVariant(3);
    }

    void runVariant(int node) {
      switch (variant) {
        case 0 -> run();
        case 1 -> run(node);
        default -> run(IntList.of(node));
      }
    }
  }

  @Test
  void runFromAHookThrowsAndALaterRunStillStarts() {
    // 0 -> 1, 2 -> 3: a nested run during a run from 0 is rejected, then run(2) works as usual.
    for (int hook = 0; hook < 2; hook++) {
      for (int variant = 0; variant < 3; variant++) {
        var t = new NestingFrontier(TestGraphs.of(4, 0, 1, 2, 3));
        t.nestIn = hook;
        t.variant = variant;
        assertThatThrownBy(() -> t.runVariant(0)).as("hook %d, variant %d", hook, variant)
            .isInstanceOf(IllegalStateException.class);
        assertThat(t.isVisited(3)).as("hook %d, variant %d", hook, variant).isFalse();

        t.nest = false;
        t.run(2);
        assertThat(t.visits.toIntArray()).as("hook %d, variant %d", hook, variant)
            .contains(2, 3);
      }
    }
  }
}
