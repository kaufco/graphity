package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallFamily;
import com.kobayami.graphity.examples.SmallProgram;

/**
 * Whether graphs are trees, forests or paths, with their edges pointing away from the
 * root.
 */
final class CheckingTrees {

  private CheckingTrees() {}

  /** The answers of the example, and the line of descent it builds. */
  record Shapes(boolean familyTree, boolean familyPath, Graph line, boolean lineTree,
      boolean linePath, boolean dependenciesForest) {}

  /**
   * Returns the shapes of the family, of one line of descent in it, and of the packages.
   */
  static Shapes shapes() {
    Graph family = SmallFamily.tree();
    Graph dependencies = SmallProgram.dependencies();
    // @start region="isTree"
    // true - one root node, one parent per person
    boolean familyTree = Graphs.isTree(family);
    boolean familyPath = Graphs.isPath(family);   // false - Ada has two children

    // One line of descent, Ada, Cleo, Finn and Ida, as a graph of its own: the nodes
    // 0 to 3.
    var builder = new GraphBuilder();
    builder.addNodes(4);
    builder.addPath(0, 1, 2, 3);
    Graph line = builder.build();
    boolean lineTree = Graphs.isTree(line);       // true
    boolean linePath = Graphs.isPath(line);       // true - one child per person at most

    // service has two in-nodes: app and ui. model and storage lie on a cycle.
    boolean dependenciesForest = Graphs.isForest(dependencies);   // false
    // @end
    return new Shapes(familyTree, familyPath, line, lineTree, linePath,
        dependenciesForest);
  }
}
