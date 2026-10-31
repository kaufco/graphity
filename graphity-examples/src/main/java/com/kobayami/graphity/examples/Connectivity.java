package com.kobayami.graphity.examples;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Biconnectivity;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.StrongConnectivity;
import java.util.ArrayList;
import java.util.List;

/** The components of a graph, and the edges and nodes that keep them connected. */
final class Connectivity {

  private Connectivity() {}

  /** The edges and nodes whose removal splits a component. */
  record Splitters(Edges edges, SortedIntView nodes) {}

  /**
   * Returns the packages grouped by strongly connected component, in the order of their
   * IDs.
   */
  static List<List<String>> buildOrder() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    // @start region="sccsOf"
    // The SCCs in reverse topological order, each as the names of its packages.
    Sccs sccs = Components.sccsOf(dependencies);
    List<List<String>> buildOrder = new ArrayList<>();
    for (int scc = 0; scc < sccs.sccCount(); scc++) {
      buildOrder.add(
          sccs.nodesOf(scc).intStream().mapToObj(node -> names[node]).toList());
    }
    // buildOrder: [[util], [cli], [json], [model, storage], [service], [ui], [app]]
    // @end
    return buildOrder;
  }

  /** Returns the bridges and articulation points of the control flow of {@code load}. */
  static Splitters bridgesOfLoad() {
    Graph cfg = SmallProgram.cfg();
    // @start region="biconnectivity"
    Biconnectivity biconnectivity = Components.biconnectivityOf(BiAdjacentGraph.of(cfg));
    Edges bridges = biconnectivity.bridges();
    // bridges: entry -> header, header -> exit
    SortedIntView articulationPoints = biconnectivity.articulationPoints();
    // articulationPoints: [1] - header
    // @end
    return new Splitters(bridges, articulationPoints);
  }

  /**
   * Returns the strong bridges and strong articulation points of the control flow of
   * load.
   */
  static Splitters strongBridgesOfLoad() {
    Graph cfg = SmallProgram.cfg();
    // @start region="strongConnectivity"
    StrongConnectivity strong = Components.strongConnectivityOf(BiAdjacentGraph.of(cfg));
    Edges strongBridges = strong.strongBridges();
    // strongBridges: header -> body, body -> then, then -> next, next -> header
    SortedIntView strongArticulationPoints = strong.strongArticulationPoints();
    // strongArticulationPoints: [1, 2, 4] - header, body and next
    // @end
    return new Splitters(strongBridges, strongArticulationPoints);
  }
}
