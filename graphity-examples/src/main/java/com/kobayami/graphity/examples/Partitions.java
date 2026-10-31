package com.kobayami.graphity.examples;

import com.kobayami.graphity.EdgePartition;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.SortedIntView;

/** Grouping the nodes or the edges of a graph, and the graph between the groups. */
final class Partitions {

  private Partitions() {}

  /** Returns the calls grouped by the package of each method. */
  static NodePartition byPackage() {
    return NodePartition.of(SmallProgram.calls(), SmallProgram.packageOf(),
        SmallProgram.dependencies().nodeCount());
  }

  /** Returns the calls between packages, condensed from the calls between methods. */
  static Graph packageCalls() {
    Graph calls = SmallProgram.calls();
    Graph dependencies = SmallProgram.dependencies();
    int[] packageOf = SmallProgram.packageOf();
    // @start region="of"
    NodePartition byPackage =
        NodePartition.of(calls, packageOf, dependencies.nodeCount());
    Graph packageCalls = byPackage.condense();
    // packageCalls: app -> [cli, ui, service], ui -> [model], service -> [model, storage]
    // @end
    return packageCalls;
  }

  /** Returns the calls from methods of {@code service} to methods of {@code model}. */
  static SortedIntView callsFromServiceToModel() {
    NodePartition byPackage = byPackage();
    Graph packageCalls = byPackage.condense();
    String[] packageNames = SmallProgram.packageNames();
    int service = SmallProgram.nodeOf(packageNames, "service");
    int model = SmallProgram.nodeOf(packageNames, "model");
    // @start region="edgesOf"
    EdgePartition byPackageCall = Graphs.edgePartition(packageCalls, byPackage);
    int serviceToModel = packageCalls.edgeIndexOf(service, model);
    SortedIntView behind = byPackageCall.edgesOf(serviceToModel);
    // behind: [5, 7] - the calls load -> parse and save -> format
    // @end
    return behind;
  }

  /** Returns the number of call sites behind each call between packages. */
  static int[] callSitesPerPackageCall() {
    NodePartition byPackage = byPackage();
    EdgePartition byPackageCall = Graphs.edgePartition(byPackage.condense(), byPackage);
    int[] siteCount = EdgeData.callSitesPerEdge();
    // @start region="reduce"
    int[] packageSiteCount = byPackageCall.reduce(siteCount, 0, Integer::sum);
    // 2 for app -> ui, app -> service, service -> model and service -> storage,
    // 1 for app -> cli and ui -> model
    // @end
    return packageSiteCount;
  }
}
