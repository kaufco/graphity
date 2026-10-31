package com.kobayami.graphity.examples;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.mermaid.MermaidWriter;

/** Writing the dependencies between packages as a Mermaid flowchart. */
final class WritingMermaid {

  private WritingMermaid() {}

  /** Returns the flowchart of the packages that form a cycle. */
  static String cycle() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    Sccs sccs = Components.sccsOf(dependencies);
    // @start region="cycle"
    // The packages are the nodes of dependencies, names[node] is the name of a package,
    // and sccs holds the strongly connected components of dependencies.

    // Write the packages that lie on a cycle as a Mermaid flowchart, each labeled with
    // its name.
    String mermaid = MermaidWriter.of(dependencies)
        .nodeLabel(node -> names[node])
        .nodeFilter(node -> sccs.containsCycle(sccs.sccOf(node)))
        .writeToString();
    // flowchart TD
    //   n4["model"]
    //   n5["storage"]
    //   n4 --> n5
    //   n5 --> n4
    // @end
    return mermaid;
  }

  /** Returns the flowchart of all packages, with the packages of the cycle in a class. */
  static String cycleInRed() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    Sccs sccs = Components.sccsOf(dependencies);
    // @start region="nodeClass"
    String mermaid = MermaidWriter.of(dependencies)
        .nodeLabel(node -> names[node])
        .classDef("cycle", "stroke:red")
        .nodeClass(node -> sccs.containsCycle(sccs.sccOf(node)) ? "cycle" : null)
        .writeToString();
    // model and storage, the cycle, in red:
    //   classDef cycle stroke:red
    //   class n4 cycle
    //   class n5 cycle
    // @end
    return mermaid;
  }
}
