package com.kobayami.graphity.examples;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.examples.SmallProgram.CallSite;
import java.io.PrintStream;
import java.util.List;

/** Building a graph with {@link GraphBuilder}, and reading its edges in bulk. */
final class BuildingAndReading {

  private BuildingAndReading() {}

  /** Builds the calls between the methods from the call sites, one edge per call site. */
  static Graph callsFromCallSites() {
    String[] names = SmallProgram.methodNames();
    List<CallSite> callSites = SmallProgram.callSites();
    // @start region="build"
    var builder = new GraphBuilder();
    builder.addNodes(names.length);    // one node per method, numbered as in names
    for (CallSite site : callSites) {
      builder.addEdge(site.caller(), site.callee());
    }
    Graph calls = builder.build();
    // 14 call sites, 12 edges: run -> show and parse -> parse were added twice
    // @end
    return calls;
  }

  /**
   * Returns the dependencies of a next version of the program, in which ui also uses
   * storage.
   */
  static Graph nextVersion() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    int ui = SmallProgram.nodeOf(names, "ui");
    int storage = SmallProgram.nodeOf(names, "storage");
    // @start region="addGraph"
    var builder = new GraphBuilder();
    // into an empty builder: the node IDs stay the same
    builder.addGraph(dependencies);
    builder.addEdge(ui, storage);
    Graph nextVersion = builder.build();
    // @end
    return nextVersion;
  }

  /** Returns the number of callers of each method, by node ID of {@code calls}. */
  static int[] callerCounts() {
    Graph calls = SmallProgram.calls();
    // @start region="loop"
    int[] start = Csr.outAdjacencyStart(calls);
    int[] callees = Csr.outAdjacency(calls);
    int[] callerCount = new int[calls.nodeCount()];
    for (int method = 0; method < calls.nodeCount(); method++) {
      for (int edge = start[method], end = start[method + 1]; edge < end; edge++) {
        callerCount[callees[edge]]++;
      }
    }
    // callerCount: 3 for format, 2 for parse, 0 for main, 1 for every other method
    // @end
    return callerCount;
  }

  /** Prints the recursive calls, with the number of call sites of each. */
  static void recursiveCalls(PrintStream out) {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int[] siteCount = EdgeData.callSitesPerEdge();
    // @start region="edges"
    Edges recursive = BackEdges.of(calls);
    for (int p = 0; p < recursive.count(); p++) {
      int caller = recursive.sourceNodes().getInt(p);
      int callee = recursive.targetNodes().getInt(p);
      int edge = recursive.edgeIndices().getInt(p);
      out.println(names[caller] + " -> " + names[callee] + ": " + siteCount[edge]);
    }
    // parse -> parse: 2
    // format -> format: 1
    // @end
  }
}
