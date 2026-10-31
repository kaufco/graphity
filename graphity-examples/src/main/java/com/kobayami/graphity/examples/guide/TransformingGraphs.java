package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.EdgeMapping;
import com.kobayami.graphity.EdgePartition;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodeMapping;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.TopOrder;
import com.kobayami.graphity.examples.SmallProgram;
import com.kobayami.graphity.examples.SmallProgram.CallSite;
import java.util.ArrayList;
import java.util.List;

/** Subgraphs, renumbered and transposed graphs, groups of one's own, and their data. */
final class TransformingGraphs {

  private TransformingGraphs() {}

  /** The number of call sites of each call, by edge index of the calls. */
  static int[] siteCount() {
    Graph calls = SmallProgram.calls();
    int[] siteCount = new int[calls.edgeCount()];
    for (CallSite site : SmallProgram.callSites()) {
      siteCount[calls.edgeIndexOf(site.caller(), site.callee())]++;
    }
    return siteCount;
  }

  /** A subgraph with its mapping and data. */
  record Subgraph(Graph loading, NodeMapping newToOld, String[] loadingNames,
      Graph savingAndShowing) {}

  /**
   * Returns the subgraph of the methods that load reaches, and that of four given
   * methods.
   */
  static Subgraph subgraph() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int load = SmallProgram.nodeOf(names, "load");
    // @start region="subgraph"
    // The subgraph of the methods that load reaches:
    NodeMapping newToOld = NodeMapping.forSubgraph(Graphs.reachableFrom(calls, load));
    Graph loading = Graphs.remapped(calls, newToOld);
    // loading: 0 -> [1, 2], 1 -> [1], 2 -> [] - load -> [parse, read], parse -> [parse]

    // The names come along through the mapping, from new to old node IDs.
    String[] loadingNames = new String[loading.nodeCount()];
    newToOld.gather(names, loadingNames);   // [load, parse, read]

    // Or a subgraph of nodes given by their node IDs: save, show, format and write.
    Graph savingAndShowing = Graphs.remapped(calls, NodeMapping.forSubgraph(4, 5, 7, 9));
    // savingAndShowing: 0 -> [2, 3], 1 -> [2], 2 -> [2], 3 -> [] - save, show, format,
    // write
    // @end
    return new Subgraph(loading, newToOld, loadingNames, savingAndShowing);
  }

  /** Renumbered graphs. */
  record Renumbered(Graph inSearchOrder, NodeMapping searchOrder, Graph inRunOrder,
      NodeMapping runOrder) {}

  /**
   * Returns the calls numbered in depth-first order, and the steps in topological order.
   */
  static Renumbered renumbered() {
    Graph calls = SmallProgram.calls();
    Graph steps = SmallProgram.buildSteps();
    // @start region="renumber"
    // In the order of a depth-first search: nodes visited one after another get adjacent
    // IDs.
    NodeMapping searchOrder = NodeMapping.reindex(calls);
    Graph inSearchOrder = Graphs.remapped(calls, searchOrder);
    // searchOrder, new to old: [0, 1, 2, 3, 6, 8, 4, 7, 9, 5] - parse and read follow
    // load

    // In an order of your own, here a topological one: every edge then goes to a higher
    // ID.
    NodeMapping runOrder = NodeMapping.forPermutation(TopOrder.of(steps));
    Graph inRunOrder = Graphs.remapped(steps, runOrder);
    // runOrder, new to old: [2, 0, 1, 4, 3, 5, 6, 7, 8] - lint, generate, compile, docs,
    // ...
    // @end
    return new Renumbered(inSearchOrder, searchOrder, inRunOrder, runOrder);
  }

  /** Node data carried over, and node IDs translated. */
  record NodeData(int[] loadingLines, int newIdOfParse, boolean mainKept, boolean partial,
      int mappedCount, int indexCount, NodeMapping secondToOriginal, int[] calleeLines) {}

  /**
   * Returns node data of the subgraph of load and translations between old and new IDs.
   */
  static NodeData nodeData() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int load = SmallProgram.nodeOf(names, "load");
    int parse = SmallProgram.nodeOf(names, "parse");
    int main = SmallProgram.nodeOf(names, "main");
    NodeMapping newToOld = NodeMapping.forSubgraph(Graphs.reachableFrom(calls, load));
    Graph loading = Graphs.remapped(calls, newToOld);
    // @start region="nodeData"
    // The lines of code of each method:
    int[] linesOf = {12, 30, 20, 25, 18, 15, 60, 40, 10, 10};
    int[] loadingLines = newToOld.gather(linesOf);   // [25, 60, 10] - load, parse, read

    // From old to new node IDs: the inverse mapping. Not every old node has a new one.
    NodeMapping oldToNew = newToOld.inverted(calls.nodeCount());
    int newIdOfParse = oldToNew.get(parse);          // 1 - parse is node 1 of loading
    boolean mainKept = oldToNew.isMapped(main);      // false - main is not in loading
    // true - 7 of 10 old nodes are not kept
    boolean partial = oldToNew.isPartial();
    int mappedCount = oldToNew.mappedCount();        // 3 - load, parse, read
    int indexCount = oldToNew.indexCount();          // 10 - the methods of calls

    // Transformed twice - loading, then its subgraph of the out-nodes of its node 0,
    // load: compose the two mappings, and gather from the data of calls in one step.
    NodeMapping secondToFirst = NodeMapping.forSubgraph(loading.outNodes(0));
    NodeMapping secondToOriginal = newToOld.compose(secondToFirst);
    int[] calleeLines = secondToOriginal.gather(linesOf);   // [60, 10] - parse, read
    // @end
    return new NodeData(loadingLines, newIdOfParse, mainKept, partial, mappedCount,
        indexCount, secondToOriginal, calleeLines);
  }

  /** Returns the call sites of each call of the subgraph of load. */
  static int[] edgeData() {
    Graph calls = SmallProgram.calls();
    int load = SmallProgram.nodeOf(SmallProgram.methodNames(), "load");
    int[] siteCount = siteCount();
    NodeMapping newToOld = NodeMapping.forSubgraph(Graphs.reachableFrom(calls, load));
    Graph loading = Graphs.remapped(calls, newToOld);
    // @start region="edgeData"
    EdgeMapping edges = Graphs.edgeMapping(loading, calls, newToOld);
    int[] loadingSiteCount = edges.gather(siteCount);
    // loadingSiteCount: [1, 1, 2] - load -> parse, load -> read, parse -> parse
    // @end
    return loadingSiteCount;
  }

  /** The transposed graph with its edge data. */
  record Transposed(BiAdjacentGraph calledBy, int transposedIndexOfLoadToRead,
      List<String> callersOfFormat) {}

  /**
   * Returns the callers of format with their call sites, through the transposed graph.
   */
  static Transposed transposed() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int format = SmallProgram.nodeOf(names, "format");
    int[] siteCount = siteCount();
    int loadToRead = calls.edgeIndexOf(SmallProgram.nodeOf(names, "load"),
        SmallProgram.nodeOf(names, "read"));
    // @start region="transposed"
    BiAdjacentGraph both = BiAdjacentGraph.of(calls);
    BiAdjacentGraph calledBy = both.transposed();   // without copying
    int transposedIndex = both.transposedEdgeIndex(loadToRead);   // 10 - read -> load

    // The edge data, by edge index of the transposed graph:
    int[] calledBySiteCount = Graphs.transposedEdgeMapping(calls).gather(siteCount);
    List<String> callersOfFormat = new ArrayList<>();
    for (int edge = calledBy.outEdgeStart(format);
        edge < calledBy.outEdgeEnd(format);
        edge++) {
      callersOfFormat.add(
          names[calledBy.edgeTarget(edge)] + ": " + calledBySiteCount[edge]);
    }
    // callersOfFormat: [save: 1, show: 1, format: 1]
    // @end
    return new Transposed(calledBy, transposedIndex, callersOfFormat);
  }

  /** Groups of the caller's own and the condensed graph. */
  record OwnGroups(NodePartition byPackage, Graph packageCalls, int groupCount,
      int sizeOfService, SortedIntView methodsOfService) {}

  /** Returns the calls grouped by package and condensed. */
  static OwnGroups ownGroups() {
    Graph calls = SmallProgram.calls();
    int[] packageOf = SmallProgram.packageOf();
    int service = SmallProgram.nodeOf(SmallProgram.packageNames(), "service");
    // @start region="ownGroups"
    NodePartition byPackage = NodePartition.of(calls, packageOf, 8);
    // 8 - json and util are empty
    int groupCount = byPackage.groupCount();
    int sizeOfService = byPackage.sizeOf(service);               // 2
    // [3, 4] - load and save
    SortedIntView methodsOfService = byPackage.nodesOf(service);

    // The calls between packages: an edge wherever a method of one calls a method of
    // another.
    Graph packageCalls = byPackage.condense();
    // packageCalls: app -> [cli, ui, service], ui -> [model], service -> [model, storage]
    // @end
    return new OwnGroups(byPackage, packageCalls, groupCount, sizeOfService,
        methodsOfService);
  }

  /** Data combined per group. */
  record Combined(int[] linesPerPackage, int[] sitesPerPackageCall,
      SortedIntView behind) {}

  /** Returns node and edge data combined per package. */
  static Combined combined() {
    Graph calls = SmallProgram.calls();
    NodePartition byPackage = NodePartition.of(calls, SmallProgram.packageOf(), 8);
    Graph packageCalls = byPackage.condense();
    String[] packages = SmallProgram.packageNames();
    int service = SmallProgram.nodeOf(packages, "service");
    int model = SmallProgram.nodeOf(packages, "model");
    int[] siteCount = siteCount();
    // @start region="reduce"
    // The lines of code of each method:
    int[] linesOf = {12, 30, 20, 25, 18, 15, 60, 40, 10, 10};
    int[] linesPerPackage = byPackage.reduce(linesOf, 0, Integer::sum);
    // linesPerPackage: [32, 30, 15, 43, 100, 20, 0, 0] - model has 100, json and util
    // none

    // Edge data: which calls are behind each call between packages, and their sum.
    EdgePartition byPackageCall = Graphs.edgePartition(packageCalls, byPackage);
    SortedIntView behind =
        byPackageCall.edgesOf(packageCalls.edgeIndexOf(service, model));
    // behind: [5, 7] - load -> parse and save -> format
    int[] sitesPerPackageCall = byPackageCall.reduce(siteCount, 0, Integer::sum);
    // sitesPerPackageCall: [1, 2, 2, 1, 2, 2] - app -> cli has 1, service -> model 2
    // @end
    return new Combined(linesPerPackage, sitesPerPackageCall, behind);
  }
}
