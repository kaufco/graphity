package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;

/** The build steps without the edges that other paths make redundant. */
final class TransitiveReduction {

  private TransitiveReduction() {}

  /** The reduction, its edge count and the edges it leaves out. */
  record Reduced(Graph reduction, int edgeCount, Edges leftOut) {}

  /** Returns the transitive reduction of the build steps. */
  static Reduced reduction() {
    Graph steps = SmallProgram.buildSteps();
    // @start region="reduction"
    // steps has no cycles.

    // Leave out every edge that a longer path makes redundant.
    Graph reduction = Graphs.transitiveReduction(steps);
    int edgeCount = reduction.edgeCount();   // 9 - one fewer than the 10 of steps

    // Which edges are redundant? Those the reduction leaves out.
    Edges leftOut = Graphs.diff(steps, reduction).removedEdges();
    // leftOut: compile -> package, edge 3 of steps -
    // compile -> compileTests -> test -> package
    // @end
    return new Reduced(reduction, edgeCount, leftOut);
  }
}
