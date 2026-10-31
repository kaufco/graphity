package com.kobayami.graphity.examples;

import com.kobayami.graphity.BfsDistances;
import com.kobayami.graphity.DfsEdgeTraverser;
import com.kobayami.graphity.DfsTraverser;
import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.EdgeTypes;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.FrontierTraverser;
import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/** Traversals of your own, and what the built-in searches find. */
final class Traversals {

  private Traversals() {}

  /** The type of an edge, and the edges of another type. */
  record Classified(EdgeType loopEdge, Edges forwardEdges) {}

  /** A distance, and a shortest path of that length. */
  record Chain(int depth, IntList chain) {}

  /** Returns the methods that {@code main} reaches, each after the methods it calls. */
  static IntList calleesFirst() {
    Graph calls = SmallProgram.calls();
    int main = SmallProgram.nodeOf(SmallProgram.methodNames(), "main");
    // @start region="leaveNode"
    class CalleesFirst extends DfsTraverser {
      final IntArrayList order = new IntArrayList();

      CalleesFirst(Graph calls) { super(calls); }

      @Override protected boolean enterNode(int method) { return true; }

      @Override protected void leaveNode(int method) { order.add(method); }
    }
    var calleesFirst = new CalleesFirst(calls);
    calleesFirst.run(main);
    // calleesFirst.order: [1, 6, 8, 3, 7, 9, 4, 5, 2, 0] - parseArgs, parse, read, load,
    // format, write, save, show, run and main
    // @end
    return calleesFirst.order;
  }

  /**
   * Returns the edge index of the dependency that closes the first cycle the search
   * finds.
   */
  static int firstCycleEdge() {
    Graph dependencies = SmallProgram.dependencies();
    // @start region="visitEdge"
    class FirstCycle extends DfsEdgeTraverser {
      int closingEdge = -1;

      FirstCycle(Graph dependencies) { super(dependencies); }

      @Override
      protected EdgeAction visitEdge(
          int source, int target, int edgeIndex, TargetState state) {
        if (state == TargetState.ON_STACK) {
          closingEdge = edgeIndex;
          return EdgeAction.STOP;
        }
        return super.visitEdge(source, target, edgeIndex, state);
      }
    }
    var firstCycle = new FirstCycle(dependencies);
    firstCycle.run();
    // firstCycle.closingEdge: 10 - the edge storage -> model
    // @end
    return firstCycle.closingEdge;
  }

  /**
   * Returns the methods that {@code main} reaches with at most two calls, nearest first.
   */
  static IntList withinTwoCalls() {
    Graph calls = SmallProgram.calls();
    int main = SmallProgram.nodeOf(SmallProgram.methodNames(), "main");
    // @start region="currentLevel"
    class WithinTwoCalls extends FrontierTraverser {
      final IntArrayList reached = new IntArrayList();

      WithinTwoCalls(Graph calls) { super(calls); }

      @Override
      protected void visitNode(int method) {
        reached.add(method);
        if (currentLevel() < 2) {
          enqueueChildren(method);
        }
      }
    }
    var withinTwoCalls = new WithinTwoCalls(calls);
    withinTwoCalls.run(main);
    // withinTwoCalls.reached: [0, 1, 2, 3, 4, 5] - main, parseArgs, run, load, save and
    // show
    // @end
    return withinTwoCalls.reached;
  }

  /**
   * Returns the type of the edge that closes the loop of {@code load}, and its forward
   * edges.
   */
  static Classified edgeTypesOfLoad() {
    Graph cfg = SmallProgram.cfg();
    String[] names = SmallProgram.blockNames();
    int header = SmallProgram.nodeOf(names, "header");
    int next = SmallProgram.nodeOf(names, "next");
    // @start region="edgeTypes"
    EdgeTypes types = EdgeTypes.of(cfg);
    EdgeType loopEdge = types.typeOf(cfg.edgeIndexOf(next, header));
    Edges forwardEdges = Edges.of(types, EdgeType.FORWARD);
    // loopEdge: BACK
    // forwardEdges: body -> next
    // @end
    return new Classified(loopEdge, forwardEdges);
  }

  /**
   * Returns the length of a shortest chain of calls from {@code main} to {@code parse}.
   */
  static Chain callChainToParse() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int main = SmallProgram.nodeOf(names, "main");
    int parse = SmallProgram.nodeOf(names, "parse");
    // @start region="pathTo"
    BfsDistances fromMain = BfsDistances.of(calls, main);
    int depth = fromMain.distanceOf(parse);
    IntList chain = fromMain.pathTo(parse);
    // depth: 3
    // chain: [0, 2, 3, 6] - main -> run -> load -> parse
    // @end
    return new Chain(depth, chain);
  }
}
