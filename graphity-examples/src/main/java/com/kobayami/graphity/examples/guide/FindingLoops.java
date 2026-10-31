package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.controlflow.Loops;
import com.kobayami.graphity.examples.SmallProgram;
import it.unimi.dsi.fastutil.ints.IntList;

/** The loops of the method {@code show} and how they nest. */
final class FindingLoops {

  private FindingLoops() {}

  /** What the example reads from the loops of {@code show}. */
  record Nesting(int loopCount, int notesLoop, int linesLoop, int header, IntList body,
      int parent, int depth) {}

  /** Returns the loops of {@code show} and how they nest. */
  static Nesting loopsOfShow() {
    Graph cfg = SmallProgram.cfgOfShow();
    String[] names = SmallProgram.blockNamesOfShow();
    int entry = SmallProgram.nodeOf(names, "entry");
    int format = SmallProgram.nodeOf(names, "format");
    int print = SmallProgram.nodeOf(names, "print");
    // @start region="loops"
    // Find the loops of show, from its entry block.
    Loops loops = Loops.of(BiAdjacentGraph.of(cfg), entry);
    int loopCount = loops.loopCount();               // 2

    // The innermost loop of format is the loop over the notes; that of print, over the
    // lines.
    int notesLoop = loops.innermostLoopOf(format);   // 0
    int linesLoop = loops.innermostLoopOf(print);    // 1
    int header = loops.headerOf(linesLoop);          // 3 - nextLine
    // [1, 2, 3, 4] - all but entry and exit
    IntList body = loops.bodyOf(notesLoop);

    // How the loops nest.
    int parent = loops.parentOf(linesLoop);          // 0 - the loop over the notes
    int depth = loops.loopDepthOf(print);            // 2 - print runs in both loops
    // @end
    return new Nesting(loopCount, notesLoop, linesLoop, header, body, parent, depth);
  }
}
