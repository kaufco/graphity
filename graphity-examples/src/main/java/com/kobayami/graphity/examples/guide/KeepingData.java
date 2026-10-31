package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.examples.SmallProgram;
import com.kobayami.graphity.examples.SmallProgram.CallSite;
import java.util.ArrayList;
import java.util.List;

/** Data on the nodes and edges of a graph, in arrays of the caller's own. */
final class KeepingData {

  private KeepingData() {}

  /** The data the example keeps and reads. */
  record Data(String[] packageOf, int[] siteCount, List<String> lines) {}

  /**
   * Returns the package of each method and the call sites of each call, and prints the
   * calls.
   */
  static Data callData() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    List<CallSite> callSites = SmallProgram.callSites();
    // @start region="data"
    // Data on nodes: one array per kind of data, indexed by node ID.
    String[] packageOf = {"app", "cli", "app", "service", "service", "ui", "model",
        "model", "storage", "storage"};

    // Data on edges: one array per kind of data, indexed by edge index.
    int[] siteCount = new int[calls.edgeCount()];
    for (CallSite site : callSites) {
      siteCount[calls.edgeIndexOf(site.caller(), site.callee())]++;
    }
    // @end
    // @start region="forEachEdge"
    // The calls between packages, each with its number of call sites. The edge index
    // finds the data of an edge in its arrays.
    List<String> lines = new ArrayList<>();
    calls.forEachEdge((caller, callee, edge) -> {
      if (!packageOf[caller].equals(packageOf[callee])) {
        lines.add(names[caller] + " -> " + names[callee] + ": " + siteCount[edge]);
      }
    });
    // lines: [main -> parseArgs: 1, run -> load: 1, run -> save: 1, run -> show: 2,
    //         load -> parse: 1, load -> read: 1, save -> format: 1, save -> write: 1,
    //         show -> format: 1]
    // @end
    return new Data(packageOf, siteCount, lines);
  }
}
