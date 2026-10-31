package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BfsDistances;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.examples.SmallProgram;
import it.unimi.dsi.fastutil.ints.IntList;

/** Shortest chains of calls, from one method and from the nearest of several. */
final class ShortestPaths {

  private ShortestPaths() {}

  /** What the example reads from one source. */
  record FromMain(int distanceToWrite, IntList pathToWrite, boolean reachesParseArgs) {}

  /** What the example reads from two sources. */
  record FromTwo(BfsDistances fromEither, int distanceToFormat, IntList pathToFormat,
      boolean reachesMain) {}

  /** Returns a shortest chain of calls from main to write. */
  static FromMain fromMain() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int main = SmallProgram.nodeOf(names, "main");
    int write = SmallProgram.nodeOf(names, "write");
    int parseArgs = SmallProgram.nodeOf(names, "parseArgs");
    // @start region="pathTo"
    BfsDistances fromMain = BfsDistances.of(calls, main);
    int distance = fromMain.distanceOf(write);          // 3 - three calls away
    // [0, 2, 4, 9] - main, run, save, write
    IntList path = fromMain.pathTo(write);
    boolean reached = fromMain.isReachable(parseArgs);  // true
    // @end
    return new FromMain(distance, path, reached);
  }

  /** Returns how many calls away from main the farthest method is. */
  static int farthestCall() {
    Graph calls = SmallProgram.calls();
    int main = SmallProgram.nodeOf(SmallProgram.methodNames(), "main");
    // @start region="maxDistance"
    // How many calls away from main is the farthest method?
    BfsDistances fromMain = BfsDistances.of(calls, main);
    // 3 - every method is at most 3 calls away from main
    int farthest = fromMain.maxDistance();
    // @end
    return farthest;
  }

  /** Returns the distances from the nearer of load and save. */
  static FromTwo fromTwo() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int load = SmallProgram.nodeOf(names, "load");
    int save = SmallProgram.nodeOf(names, "save");
    int format = SmallProgram.nodeOf(names, "format");
    int main = SmallProgram.nodeOf(names, "main");
    // @start region="nearest"
    // The search starts at both load and save.
    BfsDistances fromEither = BfsDistances.of(calls, IntList.of(load, save));
    int distance = fromEither.distanceOf(format);       // 1 - save calls it
    IntList path = fromEither.pathTo(format);           // [4, 7] - save, format
    boolean reached = fromEither.isReachable(main);     // false
    // @end
    return new FromTwo(fromEither, distance, path, reached);
  }
}
