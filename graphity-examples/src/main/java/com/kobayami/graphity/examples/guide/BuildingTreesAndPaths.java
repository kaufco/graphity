package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;

/** Trees, paths and the edges of one node, each added in a few calls. */
final class BuildingTreesAndPaths {

  private BuildingTreesAndPaths() {}

  /** Returns the family tree, built person by person. */
  static Graph familyTree() {
    // @start region="tree"
    // A family tree: an edge from each parent to each child. addChild adds a new node and
    // the edge to it, and returns the new node ID.
    var builder = new GraphBuilder();
    int ada = builder.addNode();         // 0
    int ben = builder.addChild(ada);     // 1
    int cleo = builder.addChild(ada);    // 2
    int dora = builder.addChild(ben);    // 3
    builder.addChild(ben);               // 4 - Emil
    int finn = builder.addChild(cleo);   // 5
    builder.addChildren(dora, 2);        // returns 6 - Greta, then Hugo as 7
    builder.addChildren(finn, 2);        // returns 8 - Ida, then Jonas as 9
    Graph family = builder.build();
    // @end
    return family;
  }

  /**
   * Returns the steps that build and publish the program, from paths and batches of
   * edges.
   */
  static Graph steps() {
    // @start region="path"
    // The steps that build and publish a program, as the nodes 0 to 8: generate, compile,
    // lint, compileTests, docs, test, package, sign and publish. An edge a -> b means
    // that step b may start only after step a.
    var builder = new GraphBuilder();
    builder.addNodes(9);
    builder.addPath(0, 1, 3, 5);     // generate -> compile -> compileTests -> test
    builder.addOutEdges(1, 4, 6);    // compile -> docs, compile -> package
    // lint -> package, test -> package: the target comes first
    builder.addInEdges(6, 2, 5);
    builder.addPath(6, 7, 8);        // package -> sign -> publish
    builder.addEdge(4, 8);           // docs -> publish
    Graph steps = builder.build();
    // steps: 0 -> [1], 1 -> [3, 4, 6], 2 -> [6], 3 -> [5], 4 -> [8], 5 -> [6], 6 -> [7],
    //        7 -> [8], 8 -> []
    // @end
    return steps;
  }
}
