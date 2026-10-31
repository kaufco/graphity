package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallFamily;
import com.kobayami.graphity.examples.SmallProgram;

/** Bipartite graphs, trees and forests, all read as undirected. */
final class CheckingUndirectedShapes {

  private CheckingUndirectedShapes() {}

  /** Whether two graphs are bipartite. */
  record Bipartite(boolean family, boolean dependencies) {}

  /** Whether graphs are trees or forests when read as undirected. */
  record Shapes(boolean familyTree, boolean restForest, boolean restTree,
      boolean callsForest) {}

  /** Returns whether the family and the packages are bipartite. */
  static Bipartite bipartite() {
    Graph family = SmallFamily.tree();
    Graph dependencies = SmallProgram.dependencies();
    // @start region="isBipartite"
    // Every edge joins a parent and a child, whose depths differ by one: one even, one
    // odd.
    boolean familyBipartite = Graphs.isBipartite(family);           // true

    // Read as undirected, app, ui and service form a cycle of three edges.
    boolean packagesBipartite = Graphs.isBipartite(dependencies);   // false
    // @end
    return new Bipartite(familyBipartite, packagesBipartite);
  }

  /**
   * Returns whether the family, the calls and the rest of the calls are trees or forests.
   */
  static Shapes shapes() {
    Graph family = SmallFamily.tree();
    Graph calls = SmallProgram.calls();
    Graph rest = FindingParts.callsBelowRun();
    // @start region="undirectedTree"
    boolean familyTree = Graphs.isUndirectedTree(family);     // true

    // The self-loops of parse and format do not count as cycles here.
    boolean restForest = Graphs.isUndirectedForest(rest);     // true - five trees
    boolean restTree = Graphs.isUndirectedTree(rest);         // false - not connected

    // Read as undirected, run, save, format and show lie on a cycle.
    boolean callsForest = Graphs.isUndirectedForest(calls);   // false
    // @end
    return new Shapes(familyTree, restForest, restTree, callsForest);
  }
}
