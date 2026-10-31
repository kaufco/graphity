package com.kobayami.graphity.examples;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodeMapping;
import java.io.PrintStream;

/**
 * Renumbering the nodes of a graph, and carrying the data on its nodes and edges along.
 */
final class Mappings {

  private Mappings() {}

  /**
   * The methods that {@code load} reaches, with their names and the call sites of each
   * call.
   */
  record Loading(Graph graph, String[] names, int[] siteCount) {}

  /** Returns the subgraph of the calls that {@code load} reaches, with its data. */
  static Loading loading() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int[] siteCount = EdgeData.callSitesPerEdge();
    int load = SmallProgram.nodeOf(names, "load");
    // @start region="subgraph"
    NodeMapping newToOld = NodeMapping.forSubgraph(Graphs.reachableFrom(calls, load));
    Graph loading = Graphs.remapped(calls, newToOld);
    String[] loadingNames = new String[loading.nodeCount()];
    newToOld.gather(names, loadingNames);
    // loadingNames: [load, parse, read]
    // loading: load -> parse, load -> read, parse -> parse
    // @end
    // @start region="edgeMapping"
    int[] loadingSiteCount =
        Graphs.edgeMapping(loading, calls, newToOld).gather(siteCount);
    // 1 for load -> parse and for load -> read, 2 for parse -> parse
    // @end
    return new Loading(loading, loadingNames, loadingSiteCount);
  }

  /** Prints each method that calls {@code parse}, with the number of call sites. */
  static void callersOfParse(PrintStream out) {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int[] siteCount = EdgeData.callSitesPerEdge();
    int parse = SmallProgram.nodeOf(names, "parse");
    // @start region="transposedEdges"
    BiAdjacentGraph calledBy = BiAdjacentGraph.of(calls).transposed();
    int[] calledBySiteCount = Graphs.transposedEdgeMapping(calls).gather(siteCount);
    for (int edge = calledBy.outEdgeStart(parse);
        edge < calledBy.outEdgeEnd(parse);
        edge++) {
      out.println(names[calledBy.edgeTarget(edge)] + ": " + calledBySiteCount[edge]);
    }
    // load: 1
    // parse: 2
    // @end
  }
}
