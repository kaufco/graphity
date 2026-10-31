package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.examples.SmallProgram;

/** All reachability between the methods at once, as a graph. */
final class TransitiveClosure {

  private TransitiveClosure() {}

  /** What the example reads from the closure. */
  record Read(Graph closure, int edgeCount, boolean mainReachesWrite,
      SortedIntView fromRun, boolean parseOnCycle, boolean loadOnCycle) {}

  /** Returns the transitive closure of the calls and what the example reads from it. */
  static Read closure() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int main = SmallProgram.nodeOf(names, "main");
    int write = SmallProgram.nodeOf(names, "write");
    int run = SmallProgram.nodeOf(names, "run");
    int parse = SmallProgram.nodeOf(names, "parse");
    int load = SmallProgram.nodeOf(names, "load");
    // @start region="closure"
    Graph closure = Graphs.transitiveClosure(calls);
    // 23 - 12 calls and 11 new edges
    int edgeCount = closure.edgeCount();
    boolean mainReachesWrite = closure.hasOutNode(main, write);  // true
    SortedIntView fromRun = closure.outNodes(run);
    // fromRun: [3, 4, 5, 6, 7, 8, 9] - load, save, show, parse, format, read and write

    // true - parse calls itself
    boolean parseOnCycle = closure.containsSelfLoop(parse);
    boolean loadOnCycle = closure.containsSelfLoop(load);        // false
    // @end
    return new Read(closure, edgeCount, mainReachesWrite, fromRun, parseOnCycle,
        loadOnCycle);
  }
}
