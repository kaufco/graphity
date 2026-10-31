package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.RecordingDfsTraverser;
import com.kobayami.graphity.testutils.RecursiveDfsOrder;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DfsTraverserTest {

  // ---- Basic order ----

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
    var g = TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6);
    var t = new RecordingDfsTraverser(g);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 3, 4, 2, 5, 6);
    assertThat(t.postOrder.toIntArray()).containsExactly(3, 4, 1, 5, 6, 2, 0);
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
    assertThat(t.postOrder.toIntArray()).containsExactly(4, 3, 2, 1, 0);
  }

  @Test
  void runOnCliqueVisitsAllNodes() {
    var t = new RecordingDfsTraverser(TestGraphs.clique(4));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  // ---- Run contract ----

  @Test
  void runFromSingleNodeVisitsReachableOnly() {
    var g = TestGraphs.of(5, 0, 1, 1, 2, 3, 4);
    var t = new RecordingDfsTraverser(g);
    t.run(0);
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void runFromNodeOutOfRangeThrows(int startNode) {
    var t = new RecordingDfsTraverser(TestGraphs.isolated(4));
    assertThatThrownBy(() -> t.run(startNode)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 15, 16, 1000})
  void runFromNodeListOutOfRangeThrowsBeforeAnyHook(int startNode) {
    // 4 and 15 still fall into the packed state word of the valid nodes.
    var t = new RecordingDfsTraverser(TestGraphs.isolated(4));
    assertThatThrownBy(() -> t.run(IntList.of(0, startNode)))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(t.preOrder.isEmpty()).isTrue();
    assertThat(t.isVisited(0)).isFalse();
  }

  @Test
  void runFromNodeListVisitsInListOrder() {
    // Disjoint: {0->1}, {2->3}, {4->5}.
    var g = TestGraphs.of(6, 0, 1, 2, 3, 4, 5);
    var t = new RecordingDfsTraverser(g);
    t.run(IntList.of(4, 0, 2));
    assertThat(t.preOrder.toIntArray()).containsExactly(4, 5, 0, 1, 2, 3);
  }

  @Test
  void runFromNodeListSkipsAlreadyVisitedStartNodes() {
    var g = TestGraphs.path(4);
    var t = new RecordingDfsTraverser(g);
    t.run(0);  // visits 0, 1, 2, 3
    t.run(IntList.of(1, 2));  // all already visited
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void everyEntryPointSkipsVisitedNodes() {
    // 0 -> 1, plus isolated 2 and 3; duplicates and repeated starts must not revisit.
    var t = new RecordingDfsTraverser(TestGraphs.of(4, 0, 1));
    t.run(0);
    t.run(1);
    t.run(IntList.of(0, 2, 2));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromEmptyNodeListIsNoOp() {
    var t = new RecordingDfsTraverser(TestGraphs.path(3));
    t.run(new IntArrayList());
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

  // ---- Pruning via enterNode returning false ----

  @Test
  void pruneSkipsSubtreeButStillLeavesNode() {
    // Binary tree: 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6. Prune at node 1 → subtree 3, 4 skipped.
    var g = TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6);
    var enter = new IntArrayList();
    var leave = new IntArrayList();
    var t = new DfsTraverser(g) {
      @Override protected boolean enterNode(int node) {
        enter.add(node);
        return node != 1;  // cut subtree below node 1
      }
      @Override protected void leaveNode(int node) {
        leave.add(node);
      }
    };
    t.run(0);
    // Node 1 is entered (and left) but its children 3, 4 are never discovered.
    assertThat(enter.toIntArray()).containsExactly(0, 1, 2, 5, 6);
    // leaveNode fires for every entered node, pruned node 1 included.
    assertThat(leave.toIntArray()).containsExactly(1, 5, 6, 2, 0);
  }

  @Test
  void pruningRootLeavesItImmediately() {
    var enter = new IntArrayList();
    var leave = new IntArrayList();
    var t = new DfsTraverser(TestGraphs.path(3)) {
      @Override protected boolean enterNode(int node) {
        enter.add(node);
        return false;  // prune everything at the root
      }
      @Override protected void leaveNode(int node) {
        leave.add(node);
      }
    };
    t.run(0);
    assertThat(enter.toIntArray()).containsExactly(0);
    assertThat(leave.toIntArray()).containsExactly(0);
  }

  // ---- isOnStack / isVisited ----

  @Test
  void ancestorsAreOnStackDuringEnterNode() {
    // Path 0 -> 1 -> 2 -> 3: when node k is entered, exactly 0..k are on the stack.
    var g = TestGraphs.path(4);
    var t = new DfsTraverser(g) {
      @Override protected boolean enterNode(int node) {
        for (int i = 0; i <= node; i++) {
          assertThat(isOnStack(i)).as("node %d on stack when entering %d", i, node).isTrue();
        }
        for (int i = node + 1; i < 4; i++) {
          assertThat(isOnStack(i)).as("node %d not on stack when entering %d", i, node).isFalse();
        }
        return true;
      }
    };
    t.run(0);
  }

  @Test
  void nodeIsOffStackAfterLeave() {
    // Diamond 0 -> 1, 2; 1 -> 3; 2 -> 3. After leaving 1, it must be off-stack before 2 is entered.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var t = new DfsTraverser(g) {
      @Override protected boolean enterNode(int node) {
        if (node == 2) {
          assertThat(isOnStack(1)).as("sibling 1 already left").isFalse();
          assertThat(isVisited(1)).as("sibling 1 already visited").isTrue();
          assertThat(isOnStack(0)).as("ancestor 0 still on stack").isTrue();
        }
        return true;
      }
    };
    t.run(0);
  }

  // ---- Order parity with a recursive depth-first search ----

  @Test
  void orderMatchesRecursiveOnDeterministicShapes() {
    Graph[] shapes = {
      TestGraphs.path(6),
      TestGraphs.cycle(6),
      TestGraphs.clique(5),
      TestGraphs.outStar(6),
      TestGraphs.inStar(6),
      TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6),
      TestGraphs.of(6, 0, 1, 0, 2, 1, 3, 2, 3, 3, 4, 4, 5),  // diamond + tail
      TestGraphs.isolated(4),
    };
    for (Graph g : shapes) {
      assertSameOrderAsRecursive(g, "deterministic shape");
    }
  }

  @Test
  void orderMatchesRecursiveOnRandomGraphs() {
    for (long seed = 0; seed < 300; seed++) {
      RandomGraphs.LabeledGraph lg = RandomGraphs.anyGraph(seed);
      assertSameOrderAsRecursive(lg.graph(), lg.describe());
    }
  }

  private static void assertSameOrderAsRecursive(Graph g, String desc) {
    var recursive = new RecursiveDfsOrder(g);
    recursive.run();
    var iterative = new RecordingDfsTraverser(g);
    iterative.run();
    assertThat(iterative.preOrder.toIntArray())
        .as("preorder parity (%s)", desc)
        .isEqualTo(recursive.preOrder.toIntArray());
    assertThat(iterative.postOrder.toIntArray())
        .as("postorder parity (%s)", desc)
        .isEqualTo(recursive.postOrder.toIntArray());
  }

  // ---- Stack safety (regression guard, mirrors DeepGraphStackSafetyTest) ----

  @Test
  void deepPathDoesNotOverflowStack() {
    int deep = 1_000_000;
    Graph path = TestGraphs.path(deep);
    var counter = new DfsTraverser(path) {
      int entered;
      int left;
      @Override protected boolean enterNode(int node) {
        entered++;
        return true;
      }
      @Override protected void leaveNode(int node) {
        left++;
      }
    };
    assertThatCode(counter::run).doesNotThrowAnyException();
    assertThat(counter.entered).isEqualTo(deep);
    assertThat(counter.left).isEqualTo(deep);
  }

  // ---- stop() ----

  /** Records pre- and postorder and stops when entering or leaving the given node. */
  private static class StoppingDfs extends DfsTraverser {
    final IntArrayList enter = new IntArrayList();
    final IntArrayList leave = new IntArrayList();
    int stopOnEnter = -1;
    int stopOnLeave = -1;
    boolean prune;
    int pruneNode = -1;

    StoppingDfs(Graph graph) {
      super(graph);
    }

    @Override protected boolean enterNode(int node) {
      enter.add(node);
      if (node == stopOnEnter) {
        stop();
      }
      return !prune && node != pruneNode;
    }

    @Override protected void leaveNode(int node) {
      leave.add(node);
      if (node == stopOnLeave) {
        stop();
      }
    }

    void stopNow() {
      stop();
    }
  }

  @Test
  void stopInEnterNodeEndsTheRunWithoutLeavingThePath() {
    var t = new StoppingDfs(TestGraphs.path(5));
    t.stopOnEnter = 2;
    t.run();
    assertThat(t.enter.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.leave.isEmpty()).isTrue();
    for (int v = 0; v < 5; v++) {
      assertThat(t.isOnStack(v)).as("node %d on stack", v).isFalse();
    }
    assertThat(t.isVisited(2)).isTrue();
    assertThat(t.isVisited(3)).isFalse();

    t.stopOnEnter = -1;
    t.run();
    assertThat(t.enter.toIntArray()).containsExactly(0, 1, 2, 3, 4);
    assertThat(t.leave.toIntArray()).containsExactly(4, 3);
  }

  @Test
  void stopInLeaveNodeEndsTheRun() {
    // Binary tree: 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6.
    var t = new StoppingDfs(TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6));
    t.stopOnLeave = 3;
    t.run();
    assertThat(t.enter.toIntArray()).containsExactly(0, 1, 3);
    assertThat(t.leave.toIntArray()).containsExactly(3);
    assertThat(t.isOnStack(0)).isFalse();
    assertThat(t.isOnStack(1)).isFalse();
  }

  @Test
  void stopOnAPrunedNodeSkipsItsLeaveNode() {
    var t = new StoppingDfs(TestGraphs.isolated(3));
    t.prune = true;
    t.stopOnEnter = 1;
    t.run();
    assertThat(t.enter.toIntArray()).containsExactly(0, 1);
    assertThat(t.leave.toIntArray()).containsExactly(0);
  }

  @Test
  void stopEndsTheWholeRunFromANodeList() {
    var t = new StoppingDfs(TestGraphs.isolated(4));
    t.stopOnLeave = 3;
    t.run(IntList.of(3, 1, 2));
    assertThat(t.enter.toIntArray()).containsExactly(3);
  }

  @Test
  void stopOutsideARunHasNoEffect() {
    var t = new StoppingDfs(TestGraphs.path(3));
    t.stopNow();
    t.run(0);
    assertThat(t.enter.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void stopInLeaveNodeOfAPrunedRootEndsTheRun() {
    var t = new StoppingDfs(TestGraphs.isolated(3));
    t.prune = true;
    t.stopOnLeave = 0;
    t.run();
    assertThat(t.enter.toIntArray()).containsExactly(0);
    assertThat(t.leave.toIntArray()).containsExactly(0);
  }

  @Test
  void stopInLeaveNodeOfAPrunedChildEndsTheRun() {
    // 0 -> 1, 0 -> 2: node 1 is pruned and stops the run when it is left.
    var t = new StoppingDfs(TestGraphs.of(3, 0, 1, 0, 2));
    t.pruneNode = 1;
    t.stopOnLeave = 1;
    t.run();
    assertThat(t.enter.toIntArray()).containsExactly(0, 1);
    assertThat(t.leave.toIntArray()).containsExactly(1);
    assertThat(t.isOnStack(0)).isFalse();
    assertThat(t.isVisited(2)).isFalse();
  }

  @Test
  void runFromNodeListClearsAnEarlierStop() {
    var t = new StoppingDfs(TestGraphs.isolated(3));
    t.stopOnEnter = 0;
    t.run(IntList.of(0, 1));
    assertThat(t.enter.toIntArray()).containsExactly(0);
    t.stopOnEnter = -1;
    t.run(IntList.of(1, 2));
    assertThat(t.enter.toIntArray()).containsExactly(0, 1, 2);
  }

  // ---- run from a hook ----

  /**
   * Calls the run method {@link #variant} of its own from the hook {@link #nestIn} of node 1 while
   * {@link #nest} is set.
   */
  private static final class NestingDfs extends DfsTraverser {
    final IntArrayList enter = new IntArrayList();
    int nestIn;  // 0: enterNode, 1: leaveNode
    int variant;  // 0: run(), 1: run(int), 2: run(IntList)
    boolean nest = true;

    NestingDfs(Graph graph) {
      super(graph);
    }

    @Override protected boolean enterNode(int node) {
      enter.add(node);
      if (nestIn == 0 && nest && node == 1) runVariant(3);
      return true;
    }

    @Override protected void leaveNode(int node) {
      if (nestIn == 1 && nest && node == 1) runVariant(3);
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
        var t = new NestingDfs(TestGraphs.of(4, 0, 1, 2, 3));
        t.nestIn = hook;
        t.variant = variant;
        assertThatThrownBy(() -> t.runVariant(0)).as("hook %d, variant %d", hook, variant)
            .isInstanceOf(IllegalStateException.class);
        assertThat(t.isVisited(3)).as("hook %d, variant %d", hook, variant).isFalse();

        t.nest = false;
        t.run(2);
        assertThat(t.enter.toIntArray()).as("hook %d, variant %d", hook, variant)
            .containsExactly(0, 1, 2, 3);
      }
    }
  }

  @Test
  void runFromEnterNodeOfTheRootThrows() {
    var t = new DfsTraverser(TestGraphs.of(2, 0, 1)) {
      @Override protected boolean enterNode(int node) {
        if (node == 0) run(1);
        return true;
      }
    };
    assertThatThrownBy(() -> t.run(0)).isInstanceOf(IllegalStateException.class);
  }
}
