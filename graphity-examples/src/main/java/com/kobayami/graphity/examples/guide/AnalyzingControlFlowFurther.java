package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.controlflow.ControlFlow;
import com.kobayami.graphity.controlflow.IteratedDominanceFrontier;
import com.kobayami.graphity.controlflow.Loops;
import com.kobayami.graphity.examples.SmallProgram;
import it.unimi.dsi.fastutil.ints.IntList;

/** Irreducible loops, dominance frontiers, phi-nodes and control dependence. */
final class AnalyzingControlFlowFurther {

  private AnalyzingControlFlowFurther() {}

  /** What the example reads from the loops of parseArgs. */
  record Irreducible(boolean reducible, int loopCount, SortedIntView entries,
      int header) {}

  /** Returns the loop of parseArgs, which can be entered in two places. */
  static Irreducible irreducible() {
    Graph cfg = SmallProgram.cfgOfParseArgs();
    int entry = SmallProgram.nodeOf(SmallProgram.blockNamesOfParseArgs(), "entry");
    // @start region="irreducible"
    Loops loops = Loops.of(BiAdjacentGraph.of(cfg), entry);
    boolean reducible = loops.isReducible();       // false
    // 1 - option and value, entered at both
    int loopCount = loops.loopCount();
    SortedIntView entries = loops.entriesOf(0);    // [1, 2] - option and value
    // 1 - option, reached first by the search
    int header = loops.headerOf(0);
    // @end
    return new Irreducible(reducible, loopCount, entries, header);
  }

  /** Returns the dominance frontiers of the control flow of load. */
  static Graph dominanceFrontiers() {
    Graph cfg = SmallProgram.cfg();
    int entry = SmallProgram.nodeOf(SmallProgram.blockNames(), "entry");
    // @start region="dominanceFrontiers"
    Dominators dominators = Dominators.of(BiAdjacentGraph.of(cfg), entry);
    Graph frontiers = ControlFlow.dominanceFrontiers(dominators);
    // frontiers: header -> header, body -> header, then -> next, next -> header
    // @end
    return frontiers;
  }

  /** The blocks that need a phi-node for each of two variables. */
  record Phis(SortedIntView forI, SortedIntView forLast) {}

  /** Returns the blocks of load that need a phi-node for i and for last. */
  static Phis phiBlocks() {
    Graph cfg = SmallProgram.cfg();
    String[] blocks = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(blocks, "entry");
    int then = SmallProgram.nodeOf(blocks, "then");
    int next = SmallProgram.nodeOf(blocks, "next");
    // @start region="phi"
    // The loop counter i is assigned in entry (i = 0) and in next (i++), the variable
    // last in entry (last = null) and in then (last = parse(line)).
    Dominators dominators = Dominators.of(BiAdjacentGraph.of(cfg), entry);
    IteratedDominanceFrontier frontier = IteratedDominanceFrontier.of(dominators);
    SortedIntView forI = frontier.frontierOf(IntList.of(entry, next));   // [1] - header
    SortedIntView forLast = frontier.frontierOf(IntList.of(entry, then));
    // forLast: [1, 4] - header and next
    // @end
    return new Phis(forI, forLast);
  }

  /** Returns the control dependence between the blocks of load. */
  static Graph controlDependence() {
    Graph cfg = SmallProgram.cfg();
    int exit = SmallProgram.nodeOf(SmallProgram.blockNames(), "exit");
    // @start region="controlDependence"
    // Control dependence is the transposed graph of the dominance frontiers of the
    // post-dominators.
    Dominators postDominators = Dominators.of(BiAdjacentGraph.of(cfg).transposed(), exit);
    Graph dependence =
        BiAdjacentGraph.of(ControlFlow.dominanceFrontiers(postDominators)).transposed();
    // dependence: header -> header, header -> body, header -> next, body -> then
    // @end
    return dependence;
  }
}
