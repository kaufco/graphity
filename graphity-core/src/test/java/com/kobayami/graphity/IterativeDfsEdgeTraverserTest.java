package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.IterativeDfsEdgeTraverser.EdgeAction;
import com.kobayami.graphity.IterativeDfsEdgeTraverser.TargetState;
import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.RecordingIterativeDfsTraverser;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IterativeDfsEdgeTraverserTest {

  /** Records every callback; descends by default, or answers from {@link #action}. */
  private static class Recorder extends IterativeDfsEdgeTraverser {
    final IntArrayList preOrder = new IntArrayList();
    final IntArrayList postOrder = new IntArrayList();
    final IntArrayList edges = new IntArrayList();
    final List<TargetState> states = new ArrayList<>();

    Recorder(Graph graph) {
      super(graph);
    }

    @Override protected boolean enterNode(int node) {
      preOrder.add(node);
      return true;
    }

    @Override protected void leaveNode(int node) {
      postOrder.add(node);
    }

    @Override protected EdgeAction visitEdge(int source, int target, int edgeIndex,
                                             TargetState state) {
      assertThat(graph.edgeSource(edgeIndex)).isEqualTo(source);
      assertThat(graph.edgeTarget(edgeIndex)).isEqualTo(target);
      assertThat(isOnStack(source)).isTrue();
      edges.add(edgeIndex);
      states.add(state);
      return action(source, target, edgeIndex, state);
    }

    EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
      return super.visitEdge(source, target, edgeIndex, state);
    }
  }

  // ---- Default behaviour ----

  @Test
  void defaultOrderMatchesNodeCentricTraverserOnRandomGraphs() {
    for (long seed = 0; seed < 300; seed++) {
      RandomGraphs.LabeledGraph lg = RandomGraphs.anyGraph(seed);
      Graph g = lg.graph();
      var nodeCentric = new RecordingIterativeDfsTraverser(g);
      nodeCentric.run();
      var edgeCentric = new Recorder(g);
      edgeCentric.run();
      assertThat(edgeCentric.preOrder.toIntArray()).as("preorder (%s)", lg.describe())
          .isEqualTo(nodeCentric.preOrder.toIntArray());
      assertThat(edgeCentric.postOrder.toIntArray()).as("postorder (%s)", lg.describe())
          .isEqualTo(nodeCentric.postOrder.toIntArray());
      assertThat(edgeCentric.edges.size()).as("every edge once (%s)", lg.describe())
          .isEqualTo(g.edgeCount());
    }
  }

  @Test
  void targetStatesAgreeWithEdgeTypesOnRandomGraphs() {
    for (long seed = 0; seed < 300; seed++) {
      RandomGraphs.LabeledGraph lg = RandomGraphs.anyGraph(seed);
      Graph g = lg.graph();
      var types = EdgeTypes.of(g);
      var t = new Recorder(g);
      t.run();
      for (int k = 0; k < t.edges.size(); k++) {
        EdgeType type = types.typeOf(t.edges.getInt(k));
        TargetState expected = switch (type) {
          case TREE -> TargetState.UNVISITED;
          case BACK -> TargetState.ON_STACK;
          case FORWARD, CROSS -> TargetState.FINISHED;
        };
        assertThat(t.states.get(k)).as("edge %d (%s)", t.edges.getInt(k), lg.describe())
            .isEqualTo(expected);
      }
    }
  }

  @Test
  void selfLoopTargetIsOnStack() {
    var t = new Recorder(TestGraphs.of(1, 0, 0));
    t.run();
    assertThat(t.states).containsExactly(TargetState.ON_STACK);
  }

  @Test
  void enterNodeDefaultsToDescending() {
    var left = new IntArrayList();
    var t = new IterativeDfsEdgeTraverser(TestGraphs.path(3)) {
      @Override protected void leaveNode(int node) {
        left.add(node);
      }
    };
    t.run();
    assertThat(left.toIntArray()).containsExactly(2, 1, 0);
  }

  // ---- Actions ----

  @Test
  void skippingAnUnvisitedTargetLeavesItForLaterEdgesAndRoots() {
    // 0 -> 1, 0 -> 2, 2 -> 1, 3 isolated. Skip 0 -> 1: node 1 is then reached via 2.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 2, 1);
    var t = new Recorder(g) {
      @Override EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
        return source == 0 && target == 1 ? EdgeAction.SKIP
            : super.action(source, target, edgeIndex, state);
      }
    };
    t.run(0);
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 2, 1);
    assertThat(t.states).containsExactly(
        TargetState.UNVISITED, TargetState.UNVISITED, TargetState.UNVISITED);

    var confined = new Recorder(g) {
      @Override EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
        return target == 1 ? EdgeAction.SKIP : super.action(source, target, edgeIndex, state);
      }
    };
    confined.run(0);
    assertThat(confined.isVisited(1)).isFalse();
    confined.run();
    assertThat(confined.preOrder.toIntArray()).containsExactly(0, 2, 1, 3);
  }

  @Test
  void descendIntoVisitedTargetActsLikeSkip() {
    var t = new Recorder(TestGraphs.cycle(3)) {
      @Override EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
        return EdgeAction.DESCEND;
      }
    };
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.postOrder.toIntArray()).containsExactly(2, 1, 0);
  }

  @Test
  void stopEndsTheRunWithoutLeavingThePath() {
    // Path 0 -> 1 -> 2 -> 3; stop on the edge 1 -> 2.
    var t = new Recorder(TestGraphs.path(4)) {
      @Override EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
        return source == 1 ? EdgeAction.STOP : super.action(source, target, edgeIndex, state);
      }
    };
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1);
    assertThat(t.postOrder.isEmpty()).isTrue();
    assertThat(t.isVisited(0)).isTrue();
    assertThat(t.isOnStack(0)).isFalse();
    assertThat(t.isOnStack(1)).isFalse();
    assertThat(t.isVisited(2)).isFalse();
  }

  @Test
  void laterRunContinuesAfterStopAndSeesStoppedPathAsFinished() {
    // 0 -> 1, 1 -> 2, 2 -> 0; stop at the first edge, then continue from 2.
    var g = TestGraphs.cycle(3);
    var stopOnce = new Recorder(g) {
      boolean stopped;
      @Override EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
        if (!stopped) {
          stopped = true;
          return EdgeAction.STOP;
        }
        return super.action(source, target, edgeIndex, state);
      }
    };
    stopOnce.run(0);
    stopOnce.run(2);
    assertThat(stopOnce.preOrder.toIntArray()).containsExactly(0, 2);
    assertThat(stopOnce.states).containsExactly(TargetState.UNVISITED, TargetState.FINISHED);
    assertThat(stopOnce.postOrder.toIntArray()).containsExactly(2);
  }

  @Test
  void stopEndsTheWholeRunFromNodesCall() {
    var g = TestGraphs.of(4, 0, 1, 2, 3);
    var t = new Recorder(g) {
      @Override EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
        return EdgeAction.STOP;
      }
    };
    t.runFromNodes(IntList.of(0, 2));
    assertThat(t.preOrder.toIntArray()).containsExactly(0);
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void nullActionIsRejected() {
    var t = new IterativeDfsEdgeTraverser(TestGraphs.path(2)) {
      @Override protected EdgeAction visitEdge(int source, int target, int edgeIndex,
                                               TargetState state) {
        return null;
      }
    };
    assertThatThrownBy(t::run).isInstanceOf(NullPointerException.class);
  }

  // ---- Pruning ----

  @Test
  void prunedNodeReportsNoEdgesButIsLeft() {
    // 0 -> 1, 0 -> 2, 1 -> 2: prune 1, so 1 -> 2 is never reported.
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    var t = new Recorder(g) {
      @Override protected boolean enterNode(int node) {
        super.enterNode(node);
        return node != 1;
      }
    };
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2);
    assertThat(t.postOrder.toIntArray()).containsExactly(1, 2, 0);
    assertThat(t.edges.size()).isEqualTo(2);
  }

  @Test
  void prunedRootReportsNoEdgesButIsLeft() {
    var t = new Recorder(TestGraphs.path(3)) {
      @Override protected boolean enterNode(int node) {
        super.enterNode(node);
        return false;
      }
    };
    t.run(0);
    assertThat(t.preOrder.toIntArray()).containsExactly(0);
    assertThat(t.postOrder.toIntArray()).containsExactly(0);
    assertThat(t.edges.isEmpty()).isTrue();
    assertThat(t.isOnStack(0)).isFalse();
    assertThat(t.isVisited(1)).isFalse();
  }

  // ---- Run contract ----

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void runFromNodeOutOfRangeThrows(int startNode) {
    var t = new Recorder(TestGraphs.isolated(4));
    assertThatThrownBy(() -> t.run(startNode)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void everyEntryPointSkipsVisitedNodes() {
    var t = new Recorder(TestGraphs.of(4, 0, 1));
    t.run(0);
    t.run(1);
    t.runFromNodes(IntList.of(0, 2, 2));
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void runOnEmptyGraphIsNoOp() {
    var t = new Recorder(TestGraphs.empty());
    t.run();
    assertThat(t.preOrder.isEmpty()).isTrue();
  }

  @Test
  void deepPathDoesNotOverflowStack() {
    int deep = 1_000_000;
    var counter = new IterativeDfsEdgeTraverser(TestGraphs.path(deep)) {
      int edges;
      int left;
      @Override protected EdgeAction visitEdge(int source, int target, int edgeIndex,
                                               TargetState state) {
        edges++;
        return super.visitEdge(source, target, edgeIndex, state);
      }
      @Override protected void leaveNode(int node) {
        left++;
      }
    };
    counter.run(0);
    assertThat(counter.edges).isEqualTo(deep - 1);
    assertThat(counter.left).isEqualTo(deep);
  }

  // ---- stop() ----

  @Test
  void stopFromEveryHookEndsTheRun() {
    // Path 0 -> 1 -> 2 -> 3; stop in enterNode(1), visitEdge(1 -> 2) or leaveNode(3).
    for (int hook = 0; hook < 3; hook++) {
      int stopIn = hook;
      var t = new Recorder(TestGraphs.path(4)) {
        @Override protected boolean enterNode(int node) {
          super.enterNode(node);
          if (stopIn == 0 && node == 1) {
            stop();
          }
          return true;
        }
        @Override protected void leaveNode(int node) {
          super.leaveNode(node);
          if (stopIn == 2 && node == 3) {
            stop();
          }
        }
        @Override EdgeAction action(int source, int target, int edgeIndex, TargetState state) {
          if (stopIn == 1 && source == 1) {
            stop();
          }
          return super.action(source, target, edgeIndex, state);
        }
      };
      t.run();
      int[][] expectedEnter = {{0, 1}, {0, 1}, {0, 1, 2, 3}};
      int[][] expectedLeave = {{}, {}, {3}};
      int[][] expectedEdges = {{0}, {0, 1}, {0, 1, 2}};
      assertThat(t.edges.toIntArray()).as("hook %d", hook).containsExactly(expectedEdges[hook]);
      assertThat(t.preOrder.toIntArray()).as("hook %d", hook).containsExactly(expectedEnter[hook]);
      assertThat(t.postOrder.toIntArray()).as("hook %d", hook).containsExactly(expectedLeave[hook]);
      for (int v = 0; v < 4; v++) {
        assertThat(t.isOnStack(v)).as("hook %d, node %d on stack", hook, v).isFalse();
      }
    }
  }

  /** Records like {@link Recorder} and stops in the hook and at the node it is told to. */
  private static class StoppingRecorder extends Recorder {
    int stopOnEnter = -1;
    int stopOnLeave = -1;
    int pruneNode = -1;

    StoppingRecorder(Graph graph) {
      super(graph);
    }

    @Override protected boolean enterNode(int node) {
      super.enterNode(node);
      if (node == stopOnEnter) stop();
      return node != pruneNode;
    }

    @Override protected void leaveNode(int node) {
      super.leaveNode(node);
      if (node == stopOnLeave) stop();
    }
  }

  @Test
  void stopInEnterNodeOfTheRootReportsNothingElse() {
    var t = new StoppingRecorder(TestGraphs.path(3));
    t.stopOnEnter = 0;
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0);
    assertThat(t.postOrder.isEmpty()).isTrue();
    assertThat(t.edges.isEmpty()).isTrue();
    assertThat(t.isOnStack(0)).isFalse();
  }

  @Test
  void stopInLeaveNodeOfAPrunedRootEndsTheRun() {
    var t = new StoppingRecorder(TestGraphs.isolated(3));
    t.pruneNode = 0;
    t.stopOnLeave = 0;
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0);
    assertThat(t.postOrder.toIntArray()).containsExactly(0);
  }

  @Test
  void stopInLeaveNodeOfAPrunedChildEndsTheRun() {
    // 0 -> 1, 0 -> 2: node 1 is pruned and stops the run when it is left.
    var t = new StoppingRecorder(TestGraphs.of(3, 0, 1, 0, 2));
    t.pruneNode = 1;
    t.stopOnLeave = 1;
    t.run();
    assertThat(t.preOrder.toIntArray()).containsExactly(0, 1);
    assertThat(t.postOrder.toIntArray()).containsExactly(1);
    assertThat(t.edges.toIntArray()).containsExactly(0);
    assertThat(t.isOnStack(0)).isFalse();
  }

  @Test
  void everyRunVariantClearsAnEarlierStop() {
    // 1 -> 2: after a stop at node 0, each variant must still descend from 1 to 2.
    for (int variant = 0; variant < 3; variant++) {
      var t = new StoppingRecorder(TestGraphs.of(3, 1, 2));
      t.stopOnEnter = 0;
      t.run(0);
      t.stopOnEnter = -1;
      switch (variant) {
        case 0 -> t.run(1);
        case 1 -> t.runFromNodes(IntList.of(1));
        default -> t.run();
      }
      assertThat(t.preOrder.toIntArray()).as("variant %d", variant).containsExactly(0, 1, 2);
    }
  }

  // ---- Equivalent of the recursion-to-hooks example in docs/tutorial.md ----

  static class SubtreeSizes extends IterativeDfsEdgeTraverser {
    final int[] parent;
    final int[] size;

    SubtreeSizes(Graph tree) {
      super(tree);
      parent = new int[tree.nodeCount()];
      size = new int[tree.nodeCount()];
      java.util.Arrays.fill(parent, -1);
    }

    @Override protected EdgeAction visitEdge(int source, int target, int edgeIndex,
        TargetState state) {
      if (state == TargetState.UNVISITED) parent[target] = source;
      return EdgeAction.DESCEND;
    }

    @Override protected void leaveNode(int node) {
      size[node]++;
      if (parent[node] >= 0) size[parent[node]] += size[node];
    }
  }

  @Test
  void tutorialSubtreeSizesOnATree() {
    // 0 -> 1, 0 -> 2, 1 -> 3, 1 -> 4, 2 -> 5
    var sizes = new SubtreeSizes(TestGraphs.of(6, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5));
    sizes.run();
    assertThat(sizes.size).containsExactly(6, 3, 2, 1, 1, 1);
  }

  @Test
  void tutorialSubtreeSizesOnADeepPath() {
    int n = 1_000_000;
    var sizes = new SubtreeSizes(TestGraphs.path(n));
    sizes.run();
    assertThat(sizes.size[0]).isEqualTo(n);
    assertThat(sizes.size[n - 1]).isEqualTo(1);
  }
}
