package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.examples.SmallProgram;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * The methods a method may end up calling, and the methods that may end up calling it.
 */
final class FindingReachable {

  private FindingReachable() {}

  /** The nodes reached forward. */
  record Forward(SortedIntView fromLoad, SortedIntView fromLoadOrSave) {}

  /** Returns the methods that load, and load or save, may end up calling. */
  static Forward forward() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int load = SmallProgram.nodeOf(names, "load");
    int save = SmallProgram.nodeOf(names, "save");
    // @start region="reachableFrom"
    SortedIntView fromLoad = Graphs.reachableFrom(calls, load);
    // fromLoad: [3, 6, 8] - load, parse and read
    SortedIntView fromLoadOrSave = Graphs.reachableFrom(calls, IntList.of(load, save));
    // fromLoadOrSave: [3, 4, 6, 7, 8, 9] - load, save, parse, format, read and write
    // @end
    return new Forward(fromLoad, fromLoadOrSave);
  }

  /** Returns the methods that may end up calling format. */
  static SortedIntView callersOfFormat() {
    Graph calls = SmallProgram.calls();
    int format = SmallProgram.nodeOf(SmallProgram.methodNames(), "format");
    // @start region="reaching"
    // Turn every call around, to search from format to the methods that may end up
    // calling it.
    BiAdjacentGraph calledBy = BiAdjacentGraph.of(calls).transposed();
    SortedIntView affected = Graphs.reachableFrom(calledBy, format);
    // affected: [0, 2, 4, 5, 7] - main, run, save, show and format itself
    // @end
    return affected;
  }
}
