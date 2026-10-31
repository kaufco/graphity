package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;
import it.unimi.dsi.fastutil.ints.IntList;

/** Whether a graph has a cycle, and one of its cycles. */
final class FindingCycles {

  private FindingCycles() {}

  /** What the example finds. */
  record Found(boolean dependenciesAcyclic, IntList cycle) {}

  /** Returns whether the packages have a cycle, and a cycle. */
  static Found cycles() {
    Graph dependencies = SmallProgram.dependencies();
    // @start region="findCycle"
    boolean dependenciesAcyclic = Graphs.isDag(dependencies);   // false
    // [4, 5] - model, storage
    IntList cycle = Graphs.findCycle(dependencies);
    // @end
    return new Found(dependenciesAcyclic, cycle);
  }
}
