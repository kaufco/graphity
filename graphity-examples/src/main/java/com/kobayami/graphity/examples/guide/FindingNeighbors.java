package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.examples.SmallProgram;

/**
 * The out-nodes and in-nodes of single nodes: the methods a method calls, and its
 * callers.
 */
final class FindingNeighbors {

  private FindingNeighbors() {}

  /** What the example reads from the out-nodes. */
  record Out(SortedIntView calledByRun, int outDegree, boolean runCallsSave,
      boolean recursive) {}

  /** What the example reads from the in-nodes. */
  record In(SortedIntView callersOfFormat, int inDegree, boolean saveCallsFormat,
      SortedIntView neighborsOfSave, int degreeOfSave, boolean mainIsRoot) {}

  /** Returns what {@code run} calls. */
  static Out outNodes() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int run = SmallProgram.nodeOf(names, "run");
    int save = SmallProgram.nodeOf(names, "save");
    int parse = SmallProgram.nodeOf(names, "parse");
    // @start region="outNodes"
    // [3, 4, 5] - load, save and show
    SortedIntView calledByRun = calls.outNodes(run);
    int outDegree = calls.outDegree(run);                // 3
    boolean runCallsSave = calls.hasOutNode(run, save);  // true
    boolean recursive = calls.containsSelfLoop(parse);   // true - parse calls itself
    // @end
    return new Out(calledByRun, outDegree, runCallsSave, recursive);
  }

  /** Returns who calls {@code format}. */
  static In inNodes() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int format = SmallProgram.nodeOf(names, "format");
    int save = SmallProgram.nodeOf(names, "save");
    int main = SmallProgram.nodeOf(names, "main");
    // @start region="inNodes"
    // A Graph stores the out-nodes of every node; BiAdjacentGraph adds the in-nodes.
    // Build it once and keep it.
    BiAdjacentGraph both = BiAdjacentGraph.of(calls);
    // [4, 5, 7] - save, show and format
    SortedIntView callers = both.inNodes(format);
    int inDegree = both.inDegree(format);                      // 3
    boolean saveCallsFormat = both.hasInNode(format, save);    // true
    // [2, 7, 9] - run, format, write
    SortedIntView neighbors = both.neighbors(save);
    // 3 - 1 in-node, 2 out-nodes
    int degree = both.degree(save);
    boolean mainIsRoot = both.isRootNode(main);                // true - nobody calls main
    // @end
    return new In(callers, inDegree, saveCallsFormat, neighbors, degree, mainIsRoot);
  }
}
