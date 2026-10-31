package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import it.unimi.dsi.fastutil.ints.IntList;

/** Operations on whole graphs: a cycle, and the graph as text. */
final class GraphOperations {

  private GraphOperations() {}

  /** Returns the cycle in the dependencies between the packages. */
  static IntList cycleInDependencies() {
    Graph dependencies = SmallProgram.dependencies();
    // @start region="findCycle"
    IntList cycle = Graphs.findCycle(dependencies);
    // cycle: [4, 5] - model and storage, closed by the edge storage -> model
    // @end
    return cycle;
  }

  /** Returns the whole control flow of {@code load} as text. */
  static String cfgAsAdjacencyList() {
    Graph cfg = SmallProgram.cfg();
    // @start region="adjacencyListOf"
    String text = Graphs.adjacencyListOf(cfg);
    // 0 -> [1]
    // 1 -> [2, 5]
    // 2 -> [3, 4]
    // 3 -> [4]
    // 4 -> [1]
    // 5 -> []
    // @end
    return text;
  }
}
