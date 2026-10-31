package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;

/** Whether every node of a graph reaches every other. */
final class CheckingStrongConnectivity {

  private CheckingStrongConnectivity() {}

  /** The two answers of the example. */
  record Answers(boolean cfgStronglyConnected, boolean loadWellFormed) {}

  /**
   * Returns whether the control flow of load is strongly connected, without and with an
   * edge from its exit back to its entry.
   */
  static Answers check() {
    Graph cfg = SmallProgram.cfg();
    String[] blocks = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(blocks, "entry");
    int exit = SmallProgram.nodeOf(blocks, "exit");
    // @start region="stronglyConnected"
    // No edge leads to entry, so no other block reaches it.
    boolean cfgStronglyConnected = Graphs.isStronglyConnected(cfg);   // false

    // With an edge from exit back to entry, the graph is strongly connected exactly when
    // every block can be reached from entry and can reach exit.
    var builder = new GraphBuilder();
    builder.addGraph(cfg);
    builder.addEdge(exit, entry);
    boolean loadWellFormed = Graphs.isStronglyConnected(builder.build());   // true
    // @end
    return new Answers(cfgStronglyConnected, loadWellFormed);
  }
}
