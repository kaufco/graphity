package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodeMapping;
import com.kobayami.graphity.examples.SmallProgram.CallSite;
import java.util.Arrays;
import java.util.List;

/**
 * Data on nodes and edges in arrays of your own, one per attribute, and how a subgraph
 * takes it.
 */
final class DataOnNodesAndEdges {

  private DataOnNodesAndEdges() {}

  /** The data on the calls, and the subgraph that {@code load} reaches with its data. */
  record CallData(
      String[] names,
      String[] packages,
      int[] siteCount,
      Graph loading,
      String[] loadingNames,
      int[] loadingSiteCount) {}

  /**
   * Returns a record per method and the call sites per call, for the calls and their
   * subgraph.
   */
  static CallData callData() {
    Graph calls = SmallProgram.calls();
    List<CallSite> callSites = SmallProgram.callSites();
    int load = SmallProgram.nodeOf(SmallProgram.methodNames(), "load");
    // @start region="columns"
    record Method(String name, String pkg) {}
    Method[] methods = {                           // methods[node]
        new Method("main", "app"), new Method("parseArgs", "cli"),
        new Method("run", "app"), new Method("load", "service"),
        new Method("save", "service"), new Method("show", "ui"),
        new Method("parse", "model"), new Method("format", "model"),
        new Method("read", "storage"), new Method("write", "storage")};

    int[] siteCount = new int[calls.edgeCount()];  // siteCount[edgeIndex]
    for (CallSite site : callSites) {
      siteCount[calls.edgeIndexOf(site.caller(), site.callee())]++;
    }
    // siteCount: 2 for run -> show and for parse -> parse, 1 for every other call
    // @end
    // @start region="carryAlong"
    NodeMapping newToOld = NodeMapping.forSubgraph(Graphs.reachableFrom(calls, load));
    Graph loading = Graphs.remapped(calls, newToOld);
    // loading: load -> parse, load -> read, parse -> parse

    Method[] loadingMethods = new Method[loading.nodeCount()];
    newToOld.gather(methods, loadingMethods);
    // loadingMethods: load, parse, read

    int[] loadingSiteCount =
        Graphs.edgeMapping(loading, calls, newToOld).gather(siteCount);
    // loadingSiteCount: 1 for load -> parse and for load -> read, 2 for parse -> parse
    // @end
    return new CallData(
        Arrays.stream(methods).map(Method::name).toArray(String[]::new),
        Arrays.stream(methods).map(Method::pkg).toArray(String[]::new),
        siteCount,
        loading,
        Arrays.stream(loadingMethods).map(Method::name).toArray(String[]::new),
        loadingSiteCount);
  }
}
