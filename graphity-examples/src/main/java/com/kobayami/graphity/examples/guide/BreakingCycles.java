package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;

/** The packages without the edges that close their cycles. */
final class BreakingCycles {

  private BreakingCycles() {}

  /** The edges removed and the graph without them. */
  record Broken(Edges backEdges, Graph acyclic, boolean isDag) {}

  /** Returns the packages without their back edges. */
  static Broken withoutBackEdges() {
    Graph dependencies = SmallProgram.dependencies();
    // @start region="backEdges"
    Edges backEdges = BackEdges.of(dependencies);   // storage -> model, edge 10

    // Build the graph again, without the back edges.
    boolean[] isBack = new boolean[dependencies.edgeCount()];
    for (int edge : backEdges.edgeIndices()) {
      isBack[edge] = true;
    }
    var builder = new GraphBuilder();
    builder.addNodes(dependencies.nodeCount());
    dependencies.forEachEdge((source, target, edge) -> {
      if (!isBack[edge]) {
        builder.addEdge(source, target);
      }
    });
    Graph acyclic = builder.build();
    boolean isDag = Graphs.isDag(acyclic);   // true
    // @end
    return new Broken(backEdges, acyclic, isDag);
  }
}
