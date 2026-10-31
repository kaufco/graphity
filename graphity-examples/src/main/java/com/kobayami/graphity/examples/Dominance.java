package com.kobayami.graphity.examples;

import com.kobayami.graphity.Ancestry;
import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.PreorderAncestry;
import it.unimi.dsi.fastutil.ints.IntList;

/** Dominators, and ancestor queries on the dominator tree. */
final class Dominance {

  private Dominance() {}

  /**
   * The immediate dominator of {@code next}, and whether {@code body} and {@code then}
   * do.
   */
  record DominatorsOfNext(int idom, boolean bodyDominatesNext,
      boolean thenDominatesNext) {}

  /**
   * Returns what the dominators of {@code load} say about its basic block {@code next}.
   */
  static DominatorsOfNext dominatorsOfNext() {
    Graph cfg = SmallProgram.cfg();
    String[] names = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(names, "entry");
    int body = SmallProgram.nodeOf(names, "body");
    int then = SmallProgram.nodeOf(names, "then");
    int next = SmallProgram.nodeOf(names, "next");
    // @start region="dominators"
    Dominators dominators = Dominators.of(BiAdjacentGraph.of(cfg), entry);
    // 2 - the block body
    int idom = dominators.immediateDominatorOf(next);
    Ancestry dominance = Ancestry.of(dominators);
    boolean bodyDominatesNext = dominance.isAncestorOf(body, next);   // true
    boolean thenDominatesNext = dominance.isAncestorOf(then, next);   // false
    // @end
    return new DominatorsOfNext(idom, bodyDominatesNext, thenDominatesNext);
  }

  /**
   * Returns the immediate post-dominator of {@code body} in the control flow of
   * {@code load}.
   */
  static int postDominatorOfBody() {
    Graph cfg = SmallProgram.cfg();
    String[] names = SmallProgram.blockNames();
    int body = SmallProgram.nodeOf(names, "body");
    int exit = SmallProgram.nodeOf(names, "exit");
    // @start region="postDominators"
    Dominators postDominators = Dominators.of(BiAdjacentGraph.of(cfg).transposed(), exit);
    int join = postDominators.immediateDominatorOf(body);   // 4 - the block next
    // @end
    return join;
  }

  /** Returns the basic blocks of {@code load} that {@code body} dominates. */
  static IntList dominatedByBody() {
    Graph cfg = SmallProgram.cfg();
    String[] names = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(names, "entry");
    int body = SmallProgram.nodeOf(names, "body");
    Dominators dominators = Dominators.of(BiAdjacentGraph.of(cfg), entry);
    // @start region="subtreeOf"
    PreorderAncestry dominance = PreorderAncestry.of(Ancestry.of(dominators));
    // [2, 3, 4] - body, then and next
    IntList dominatedByBody = dominance.subtreeOf(body);
    // @end
    return dominatedByBody;
  }
}
