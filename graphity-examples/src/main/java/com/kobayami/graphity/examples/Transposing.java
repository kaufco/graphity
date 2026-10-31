package com.kobayami.graphity.examples;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.SortedIntView;

/** Following edges against their direction. */
final class Transposing {

  private Transposing() {}

  /** Returns {@code model} and the packages that depend on it, directly or indirectly. */
  static SortedIntView affectedByModel() {
    Graph dependencies = SmallProgram.dependencies();
    // @start region="transposed"
    int model = 4;
    BiAdjacentGraph usedBy = BiAdjacentGraph.of(dependencies).transposed();
    SortedIntView affected = Graphs.reachableFrom(usedBy, model);
    // affected: [0, 2, 3, 4, 5] - app, ui, service, model and storage
    // @end
    return affected;
  }
}
