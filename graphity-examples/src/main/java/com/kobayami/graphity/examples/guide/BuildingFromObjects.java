package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;

/**
 * A graph built from objects of the caller's own: packages that name the packages they
 * use.
 */
final class BuildingFromObjects {

  private BuildingFromObjects() {}

  /** A package of a program, with the names of the packages it uses. */
  record Package(String name, List<String> uses) {}

  /** The graph and the package of each node. */
  record Built(Graph dependencies, Package[] packageOf, String usedByApp) {}

  /** Returns the dependencies between packages given as objects. */
  static Built fromObjects() {
    // @start region="packages"
    // The packages of a program as objects.
    List<Package> packages = List.of(
        new Package("app", List.of("cli", "ui", "service")),
        new Package("cli", List.of("util")),
        new Package("ui", List.of("service", "model", "util")),
        new Package("service", List.of("model", "storage")),
        new Package("model", List.of("storage")),
        new Package("storage", List.of("model", "json", "util")),
        new Package("json", List.of("util")),
        new Package("util", List.of()));
    // @end
    // @start region="fromObjects"
    // Give each package a node ID, its position in the list, and keep the package of each
    // node ID in an array. A map from name to int, from the fastutil library that
    // Graphity uses, avoids boxing the node IDs; an unknown name gives -1, which the
    // builder rejects.
    Package[] packageOf = packages.toArray(new Package[0]);
    Object2IntMap<String> nodeOf = new Object2IntOpenHashMap<>();
    nodeOf.defaultReturnValue(-1);
    for (int node = 0; node < packageOf.length; node++) {
      nodeOf.put(packageOf[node].name(), node);
    }

    // Add an edge for each package another package uses.
    var builder = new GraphBuilder();
    builder.addNodes(packages.size());
    for (Package p : packages) {
      for (String used : p.uses()) {
        builder.addEdge(nodeOf.getInt(p.name()), nodeOf.getInt(used));
      }
    }
    Graph dependencies = builder.build();
    String usedByApp = packageOf[dependencies.outNodes(0).getInt(1)].name();   // ui
    // @end
    return new Built(dependencies, packageOf, usedByApp);
  }

  /**
   * Returns a graph built from edges whose node IDs come from elsewhere, without a node
   * count.
   */
  static Graph fromNumberedInput() {
    // @start region="andNodes"
    // Edges between nodes numbered elsewhere, for example by a database: no node count
    // needed.
    int[] endpoints = {0, 3, 3, 4, 4, 0, 7, 3};   // 0 -> 3, 3 -> 4, 4 -> 0, 7 -> 3
    var builder = new GraphBuilder();
    builder.addEdgePairsAndNodes(endpoints);
    Graph graph = builder.build();
    // 8 - the nodes 0 to 7; 1, 2, 5 and 6 have no edges
    int nodeCount = graph.nodeCount();
    // @end
    return graph;
  }
}
