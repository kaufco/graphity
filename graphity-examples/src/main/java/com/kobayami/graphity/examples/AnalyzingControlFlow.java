package com.kobayami.graphity.examples;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.controlflow.ControlFlow;
import com.kobayami.graphity.controlflow.IteratedDominanceFrontier;
import com.kobayami.graphity.controlflow.Loops;
import it.unimi.dsi.fastutil.ints.IntList;

/** The loops, the phi-nodes and the control dependence of the method {@code load}. */
final class AnalyzingControlFlow {

  private AnalyzingControlFlow() {}

  /** The loop around a block, with what the example reads from it. */
  record LoopOfBlock(Loops loops, int loop, int header, IntList blocks) {}

  /** Returns the loop around the block {@code then} of {@code load}. */
  static LoopOfBlock loopOfThen() {
    Graph cfg = SmallProgram.cfg();
    String[] names = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(names, "entry");
    int then = SmallProgram.nodeOf(names, "then");
    // @start region="loops"
    // The basic blocks of load are the nodes 0..5 of cfg: entry, header, body, then,
    // next, exit.

    // Find the loops of load, from its entry block.
    Loops loops = Loops.of(BiAdjacentGraph.of(cfg), entry);

    // Find the loop around the block then, with its header and its body.
    int loop = loops.innermostLoopOf(then);   // 0 - the for loop, the only loop
    int header = loops.headerOf(loop);        // 1 - the block header
    // [1, 2, 3, 4] - header, body, then and next
    IntList blocks = loops.bodyOf(loop);
    // @end
    return new LoopOfBlock(loops, loop, header, blocks);
  }

  /** Returns the blocks of {@code load} where the variable {@code i} needs a phi-node. */
  static SortedIntView phiBlocksOfI() {
    Graph cfg = SmallProgram.cfg();
    String[] names = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(names, "entry");
    int next = SmallProgram.nodeOf(names, "next");
    // @start region="frontierOf"
    Dominators dominators = Dominators.of(BiAdjacentGraph.of(cfg), entry);
    IteratedDominanceFrontier phis = IteratedDominanceFrontier.of(dominators);
    // i is assigned in entry (i = 0) and in next (i++)
    SortedIntView phiBlocks = phis.frontierOf(IntList.of(entry, next));
    // phiBlocks: [1] - header
    // @end
    return phiBlocks;
  }

  /** Returns the control dependence between the blocks of {@code load}. */
  static Graph controlDependenceOfLoad() {
    Graph cfg = SmallProgram.cfg();
    int exit = SmallProgram.nodeOf(SmallProgram.blockNames(), "exit");
    // @start region="controlDependence"
    Dominators postDominators = Dominators.of(BiAdjacentGraph.of(cfg).transposed(), exit);
    Graph controlDependence =
        BiAdjacentGraph.of(ControlFlow.dominanceFrontiers(postDominators)).transposed();
    // header -> [header, body, next]: the loop condition decides whether they run
    // body -> [then]: the if decides whether then runs
    // @end
    return controlDependence;
  }
}
