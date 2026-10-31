package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.examples.SmallProgram;

/** The build steps that need no other step: where a run starts. */
final class FindingRoots {

  private FindingRoots() {}

  /** The root nodes, and a test of a single node. */
  record Roots(SortedIntView roots, boolean compileIsRoot) {}

  /** Returns the steps without an edge into them. */
  static Roots roots() {
    String[] names = SmallProgram.stepNames();
    int compile = SmallProgram.nodeOf(names, "compile");
    // @start region="rootNodes"
    // Root nodes have no in-nodes, so the graph needs its in-nodes.
    BiAdjacentGraph steps = BiAdjacentGraph.of(SmallProgram.buildSteps());
    SortedIntView roots = Graphs.rootNodes(steps);    // [0, 2] - generate and lint
    boolean compileIsRoot = steps.isRootNode(compile);  // false - it waits for generate
    // @end
    return new Roots(roots, compileIsRoot);
  }
}
