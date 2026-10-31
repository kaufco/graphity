package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Ancestry;
import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Dominators, the dominator tree, post-dominators and dominators from several entries.
 */
final class FindingDominators {

  private FindingDominators() {}

  /** Immediate dominators in the control flow of load. */
  record Immediate(int immediateDominatorOfNext, int immediateDominatorOfExit,
      boolean exitReachable, IntList dominatorsOfNext) {}

  /** The dominator tree and questions answered by it. */
  record Tree(Graph tree, boolean bodyDominatesNext, boolean thenDominatesNext,
      int lowestCommonDominator) {}

  /** Immediate post-dominators and the post-dominator tree. */
  record Post(int joinOfBody, int joinOfHeader, Graph tree) {}

  /** Dominators from two entries, and their dominator forest. */
  record TwoEntries(int immediateDominatorOfLoad, int immediateDominatorOfParse,
      int immediateDominatorOfFormat, Graph forest) {}

  /** Returns immediate dominators of the control flow of load. */
  static Immediate immediate() {
    Graph cfg = SmallProgram.cfg();
    String[] blocks = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(blocks, "entry");
    int next = SmallProgram.nodeOf(blocks, "next");
    int exit = SmallProgram.nodeOf(blocks, "exit");
    // @start region="dominators"
    // The analysis follows edges against their direction too, so it takes a
    // BiAdjacentGraph.
    Dominators dominators = Dominators.of(BiAdjacentGraph.of(cfg), entry);
    int immediateDominatorOfNext = dominators.immediateDominatorOf(next);   // 2 - body
    int immediateDominatorOfExit = dominators.immediateDominatorOf(exit);   // 1 - header
    boolean exitReachable = dominators.isReachable(exit);                   // true

    // All blocks that dominate next: next, its immediate dominator, that one's, and so
    // on.
    IntList dominatorsOfNext = new IntArrayList();
    for (int d = next; d != -1; d = dominators.immediateDominatorOf(d)) {
      dominatorsOfNext.add(d);
    }
    // dominatorsOfNext: [4, 2, 1, 0] - next, body, header, entry
    // @end
    return new Immediate(immediateDominatorOfNext, immediateDominatorOfExit,
        exitReachable, dominatorsOfNext);
  }

  /** Returns the dominator tree of the control flow of load and what it answers. */
  static Tree tree() {
    Graph cfg = SmallProgram.cfg();
    String[] blocks = SmallProgram.blockNames();
    int entry = SmallProgram.nodeOf(blocks, "entry");
    int body = SmallProgram.nodeOf(blocks, "body");
    int then = SmallProgram.nodeOf(blocks, "then");
    int next = SmallProgram.nodeOf(blocks, "next");
    // @start region="dominatorTree"
    // Dominators takes a BiAdjacentGraph, which also holds the in-nodes.
    Dominators dominators = Dominators.of(BiAdjacentGraph.of(cfg), entry);
    Graph tree = Graphs.dominatorTree(dominators);
    // tree: entry -> header, header -> body, header -> exit, body -> then, body -> next

    // Does one block dominate another? An ancestor question on the dominator tree.
    Ancestry dominance = Ancestry.of(dominators);
    boolean bodyDominatesNext = dominance.isAncestorOf(body, next);   // true
    boolean thenDominatesNext = dominance.isAncestorOf(then, next);   // false
    // 2 - body
    int lowestCommonDominator = dominance.lowestCommonAncestorOf(then, next);
    // @end
    return new Tree(tree, bodyDominatesNext, thenDominatesNext, lowestCommonDominator);
  }

  /**
   * Returns immediate post-dominators of the control flow of load, and its post-dominator
   * tree.
   */
  static Post post() {
    Graph cfg = SmallProgram.cfg();
    String[] blocks = SmallProgram.blockNames();
    int body = SmallProgram.nodeOf(blocks, "body");
    int header = SmallProgram.nodeOf(blocks, "header");
    int exit = SmallProgram.nodeOf(blocks, "exit");
    // @start region="postDominators"
    // The post-dominators are the dominators of the transposed graph, from the exit.
    // Dominators takes a BiAdjacentGraph, which also holds the in-nodes.
    Dominators postDominators = Dominators.of(BiAdjacentGraph.of(cfg).transposed(), exit);
    int joinOfBody = postDominators.immediateDominatorOf(body);       // 4 - next
    int joinOfHeader = postDominators.immediateDominatorOf(header);   // 5 - exit

    // The post-dominator tree: an edge from each block to the blocks it immediately
    // post-dominates.
    Graph tree = Graphs.dominatorTree(postDominators);
    // tree: header -> entry, header -> next, next -> body, next -> then, exit -> header
    // @end
    return new Post(joinOfBody, joinOfHeader, tree);
  }

  /**
   * Returns dominators of the calls from main and from load, and their dominator forest.
   */
  static TwoEntries twoEntries() {
    Graph calls = SmallProgram.calls();
    String[] methods = SmallProgram.methodNames();
    int main = SmallProgram.nodeOf(methods, "main");
    int load = SmallProgram.nodeOf(methods, "load");
    int parse = SmallProgram.nodeOf(methods, "parse");
    int format = SmallProgram.nodeOf(methods, "format");
    // @start region="entries"
    // Besides main, a background job calls load directly.
    Dominators dominators =
        Dominators.of(BiAdjacentGraph.of(calls), IntList.of(main, load));
    // -1 - an entry
    int immediateDominatorOfLoad = dominators.immediateDominatorOf(load);
    // 3 - load
    int immediateDominatorOfParse = dominators.immediateDominatorOf(parse);
    int immediateDominatorOfFormat = dominators.immediateDominatorOf(format);   // 2 - run

    // The dominator forest: here a tree from main and one from load.
    Graph forest = Graphs.dominatorTree(dominators);
    // forest: main -> parseArgs, main -> run, run -> save, run -> show, run -> format,
    //         load -> parse, load -> read, save -> write
    // @end
    return new TwoEntries(immediateDominatorOfLoad, immediateDominatorOfParse,
        immediateDominatorOfFormat, forest);
  }
}
