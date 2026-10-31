package com.kobayami.graphity.examples;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.dot.DotWriter;

/** Writing the dependencies between packages in DOT (the language of Graphviz). */
final class WritingDot {

  private WritingDot() {}

  /** Returns the DOT text of the packages that form a cycle. */
  static String cycle() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    Sccs sccs = Components.sccsOf(dependencies);
    // @start region="cycle"
    // The packages are the nodes of dependencies, names[node] is the name of a package,
    // and sccs holds the strongly connected components of dependencies.

    // Write the packages that lie on a cycle in DOT, each labeled with its name.
    String dot = DotWriter.of(dependencies)
        .nodeLabel(node -> names[node])
        .nodeFilter(node -> sccs.containsCycle(sccs.sccOf(node)))
        .writeToString();
    // digraph {
    //   4 [label="model"];
    //   5 [label="storage"];
    //   4 -> 5;
    //   5 -> 4;
    // }
    // @end
    return dot;
  }

  /** Returns the DOT text of all packages, with the packages of the cycle in red. */
  static String cycleInRed() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    Sccs sccs = Components.sccsOf(dependencies);
    // @start region="nodeAttributes"
    String dot = DotWriter.of(dependencies)
        .nodeLabel(node -> names[node])
        .nodeAttributes((node, attributes) -> {
          if (sccs.containsCycle(sccs.sccOf(node))) {
            attributes.put("color", "red");
          }
        })
        .writeToString();
    // model and storage, the cycle, in red:
    //   4 [label="model", color="red"];
    //   5 [label="storage", color="red"];
    // @end
    return dot;
  }
}
