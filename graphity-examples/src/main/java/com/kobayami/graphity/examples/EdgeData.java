package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.examples.SmallProgram.CallSite;
import java.io.PrintStream;
import java.util.List;

/** Data on edges, in arrays indexed by edge index. */
final class EdgeData {

  private EdgeData() {}

  /** Returns the number of call sites of each call, by edge index of {@code calls}. */
  static int[] callSitesPerEdge() {
    Graph calls = SmallProgram.calls();
    List<CallSite> callSites = SmallProgram.callSites();
    // @start region="edgeIndexOf"
    int[] siteCount = new int[calls.edgeCount()];
    for (CallSite site : callSites) {
      siteCount[calls.edgeIndexOf(site.caller(), site.callee())]++;
    }
    // 2 for run -> show and for parse -> parse, 1 for every other edge
    // @end
    return siteCount;
  }

  /** Prints each method that {@code run} calls, with the number of call sites. */
  static void calleesOfRun(PrintStream out) {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int run = SmallProgram.nodeOf(names, "run");
    int[] siteCount = callSitesPerEdge();
    // @start region="outEdges"
    for (int edge = calls.outEdgeStart(run); edge < calls.outEdgeEnd(run); edge++) {
      out.println(names[calls.edgeTarget(edge)] + ": " + siteCount[edge]);
    }
    // load: 1
    // save: 1
    // show: 2
    // @end
  }
}
