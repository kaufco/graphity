package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Csr;
import com.kobayami.graphity.DfsEdgeTraverser;
import com.kobayami.graphity.DfsTraverser;
import com.kobayami.graphity.FrontierTraverser;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallFamily;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.ArrayList;
import java.util.List;

/** Searches of one's own, on the traversers of Graphity and on the arrays of a graph. */
final class WritingSearches {

  private WritingSearches() {}

  /** Returns the number of descendants of every person, computed bottom-up. */
  static int[] descendants() {
    Graph family = SmallFamily.tree();
    int ada = SmallFamily.personOf("Ada");
    // @start region="dfs"
    // graph, a field of DfsTraverser, is the graph it searches.
    class Descendants extends DfsTraverser {
      final int[] count;

      Descendants(Graph family) {
        super(family);
        count = new int[family.nodeCount()];
      }

      @Override
      protected boolean enterNode(int person) {
        return true;   // search from the children of every person
      }

      @Override
      protected void leaveNode(int person) {
        // Called after the whole subtree of the person: the counts of the children are
        // complete.
        for (int child : graph.outNodes(person)) {
          count[person] += 1 + count[child];
        }
      }
    }
    var descendants = new Descendants(family);
    descendants.run(ada);
    // descendants.count: [9, 4, 3, 2, 0, 2, 0, 0, 0, 0] - Ada has 9, Ben 4, Cleo 3, ...
    // @end
    return descendants.count;
  }

  /**
   * The methods reached without the storage package, and the recursive calls on the way.
   */
  record WithoutStorage(List<String> reached, List<String> recursive) {}

  /** Returns what main reaches without calling into storage. */
  static WithoutStorage withoutStorage() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int[] packageOf = SmallProgram.packageOf();
    int storage = SmallProgram.nodeOf(SmallProgram.packageNames(), "storage");
    int main = SmallProgram.nodeOf(names, "main");
    // @start region="dfsEdges"
    class AvoidingStorage extends DfsEdgeTraverser {
      final List<String> reached = new ArrayList<>();
      final List<String> recursive = new ArrayList<>();

      AvoidingStorage(Graph calls) {
        super(calls);
      }

      @Override
      protected boolean enterNode(int method) {
        reached.add(names[method]);
        return true;
      }

      @Override
      protected EdgeAction visitEdge(
          int caller, int callee, int edge, TargetState state) {
        if (state == TargetState.ON_STACK) {
          recursive.add(names[caller] + " -> " + names[callee]);   // closes a cycle
        }
        if (packageOf[callee] == storage) {
          return EdgeAction.SKIP;   // do not follow calls into storage
        }
        return super.visitEdge(caller, callee, edge, state);
      }
    }
    var search = new AvoidingStorage(calls);
    search.run(main);
    // search.reached: [main, parseArgs, run, load, parse, save, format, show]
    // search.recursive: [parse -> parse, format -> format]
    // @end
    return new WithoutStorage(search.reached, search.recursive);
  }

  /** Returns the methods by call depth from main. */
  static List<List<String>> byDepth() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int main = SmallProgram.nodeOf(names, "main");
    // @start region="frontier"
    class ByDepth extends FrontierTraverser {
      final List<List<String>> levels = new ArrayList<>();
      List<String> current = new ArrayList<>();

      ByDepth(Graph calls) {
        super(calls);
      }

      @Override
      protected void visitNode(int method) {
        current.add(names[method]);
        enqueueChildren(method);   // the out-nodes form the next level
      }

      @Override
      protected void leaveLevel(int level) {
        levels.add(current);       // after the last node of each level
        current = new ArrayList<>();
      }
    }
    var byDepth = new ByDepth(calls);
    byDepth.run(main);
    // byDepth.levels: [[main], [parseArgs, run], [load, save, show],
    //                  [parse, read, format, write]]
    // @end
    return byDepth.levels;
  }

  /** Per-method sums and counts, computed on the arrays of the graph. */
  record FromArrays(int[] sitesIn, int[] callerCount, int[] sitesFromOthers) {}

  /**
   * Returns the call sites in each method, its callers, and where other methods call it.
   */
  static FromArrays fromArrays() {
    Graph calls = SmallProgram.calls();
    int[] siteCount = TransformingGraphs.siteCount();
    // @start region="arrays"
    int[] start = Csr.outAdjacencyStart(calls);
    int[] callees = Csr.outAdjacency(calls);
    int[] sitesIn = new int[calls.nodeCount()];
    int[] callerCount = new int[calls.nodeCount()];
    for (int method = 0; method < calls.nodeCount(); method++) {
      // positions = edges
      for (int edge = start[method]; edge < start[method + 1]; edge++) {
        sitesIn[method] += siteCount[edge];
        callerCount[callees[edge]]++;
      }
    }
    // sitesIn: [2, 0, 4, 2, 2, 1, 2, 1, 0, 0] - run calls from 4 places in its code
    // callerCount: [0, 1, 1, 1, 1, 1, 2, 3, 1, 1] - format has 3 callers, itself included

    // The in-nodes of a BiAdjacentGraph have the same layout, but a position there is an
    // edge index of the transposed graph: the edge data has to follow first.
    BiAdjacentGraph both = BiAdjacentGraph.of(calls);
    int[] inStart = Csr.inAdjacencyStart(both);
    int[] callers = Csr.inAdjacency(both);
    int[] calledBySiteCount = Graphs.transposedEdgeMapping(calls).gather(siteCount);
    int[] sitesFromOthers = new int[calls.nodeCount()];
    for (int method = 0; method < calls.nodeCount(); method++) {
      for (int edge = inStart[method]; edge < inStart[method + 1]; edge++) {
        if (callers[edge] != method) {
          sitesFromOthers[method] += calledBySiteCount[edge];
        }
      }
    }
    // sitesFromOthers: [0, 1, 1, 1, 1, 2, 1, 2, 1, 1] - show is called from 2 places
    // @end
    return new FromArrays(sitesIn, callerCount, sitesFromOthers);
  }
}
