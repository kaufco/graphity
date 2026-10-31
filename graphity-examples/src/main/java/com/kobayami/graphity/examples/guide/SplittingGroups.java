package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.StrongConnectivity;
import com.kobayami.graphity.examples.SmallProgram;

/** The edges and nodes whose removal splits the loop of the method load. */
final class SplittingGroups {

  private SplittingGroups() {}

  /** The strong bridges and strong articulation points. */
  record Splitters(Edges strongBridges, SortedIntView strongArticulationPoints) {}

  /**
   * Returns the strong bridges and strong articulation points of the control flow of
   * load.
   */
  static Splitters ofLoad() {
    Graph cfg = SmallProgram.cfg();
    // @start region="strongConnectivity"
    // The loop of cfg - header, body, then, next - is a strongly connected component. The
    // analysis follows edges against their direction too.
    StrongConnectivity strong = Components.strongConnectivityOf(BiAdjacentGraph.of(cfg));
    Edges strongBridges = strong.strongBridges();
    // strongBridges: header -> body, body -> then, then -> next, next -> header
    SortedIntView strongArticulationPoints = strong.strongArticulationPoints();
    // strongArticulationPoints: [1, 2, 4] - header, body and next
    // @end
    return new Splitters(strongBridges, strongArticulationPoints);
  }
}
