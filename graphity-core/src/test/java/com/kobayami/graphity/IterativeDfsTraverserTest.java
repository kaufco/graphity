package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.RecordingDfsTraverser;
import com.kobayami.graphity.testutils.RecordingIterativeDfsTraverser;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IterativeDfsTraverserTest {

  // ---- Basic order (mirrors DfsTraverserTest) ----

  @Test
  void runVisitsSinglePathInPreOrder() {
    var t = new RecordingIterativeDfsTraverser(TestGraphs.path(4));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
    assertThat(t.postOrder.toIntArray()).containsExactly(3, 2, 1, 0);
  }

  @Test
  void runVisitsBinaryTreeInSortedPreOrder() {
    // Binary tree: 0 -> 1, 2; 1 -> 3, 4; 2 -> 5, 6.
    var g = TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 2, 6);
    var t = new RecordingIterativeDfsTraverser(g);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 3, 4, 2, 5, 6);
    assertThat(t.postOrder.toIntArray()).containsExactly(3, 4, 1, 5, 6, 2, 0);
  }

  @Test
  void runOnEmptyGraphIsNoOp() {
    var t = new RecordingIterativeDfsTraverser(TestGraphs.empty());
    t.run();
    assertThat(t.preOrder.isEmpty()).isTrue();
    assertThat(t.postOrder.isEmpty()).isTrue();
  }

  @Test
  void runTerminatesOnCycle() {
    var t = new RecordingIterativeDfsTraverser(TestGraphs.cycle(5));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3, 4);
    assertThat(t.postOrder.toIntArray()).containsExactly(4, 3, 2, 1, 0);
  }

  @Test
  void runOnCliqueVisitsAllNodes() {
    var t = new RecordingIterativeDfsTraverser(TestGraphs.clique(4));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  // ---- Run contract ----

  @Test
  void runFromSingleNodeVisitsReachableOnly() {
    var g = TestGraphs.of(5, 0, 1, 1, 2, 3, 4);
    var t = new RecordingIterativeDfsTraverser(g);
    t.run(0);
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void runFromNodeOutOfRangeThrows(int startNode) {
    var t = new RecordingIterativeDfsTraverser(TestGraphs.isolated(4));
    assertThatThrownBy(() -> t.run(startNode)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void runFromNodesVisitsInListOrder() {
    // Disjoint: {0->1}, {2->3}, {4->5}.
    var g = TestGraphs.of(6, 0, 1, 2, 3, 4, 5);
    var t = new RecordingIterativeDfsTraverser(g);
    t.runFromNodes(IntList.of(4, 0, 2));
    assertThat(t.preOrder.toIntArray()).containsExactly(4, 5, 0, 1, 2, 3);
  }

  @Test
  void runFromNodesSkipsAlreadyVisitedStartNodes() {
    var g = TestGraphs.path(4);
    var t = new RecordingIterativeDfsTraverser(g);
    t.run(0);  // visits 0, 1, 2, 3
    t.runFromNodes(IntList.of(1, 2));  // all already visited
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runFromEmptyNodeListIsNoOp() {
    var t = new RecordingIterativeDfsTraverser(TestGraphs.path(3));
    t.runFromNodes(new IntArrayList());
    assertThat(t.preOrder.isEmpty()).isTrue();
  }

  @Test
  void multipleRunCallsDoNotRevisit() {
    var t = new RecordingIterativeDfsTraverser(TestGraphs.path(4));
    t.run(0);
    t.run(0);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runThenRunCombinesTraversals() {
    // Two components.
    var g = TestGraphs.of(5, 0, 1, 2, 3, 3, 4);
    var t = new RecordingIterativeDfsTraverser(g);
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
    var t = new IterativeDfsTraverser(g) {
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
    var t = new IterativeDfsTraverser(TestGraphs.path(3)) {
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
    var t = new IterativeDfsTraverser(g) {
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
    var t = new IterativeDfsTraverser(g) {
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

  // ---- Order parity with the recursive DfsTraverser ----

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
    var recursive = new RecordingDfsTraverser(g);
    recursive.run();
    var iterative = new RecordingIterativeDfsTraverser(g);
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
    var counter = new IterativeDfsTraverser(path) {
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
}
