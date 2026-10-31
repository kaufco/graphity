package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.EdgeTypes;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.dot.DotWriter;
import com.kobayami.graphity.examples.SmallGrammar;
import com.kobayami.graphity.examples.SmallProgram;
import com.kobayami.graphity.mermaid.MermaidWriter;
import com.kobayami.graphity.mermaid.MermaidWriter.Direction;
import com.kobayami.graphity.mermaid.MermaidWriter.Shape;
import java.io.IOException;
import java.io.StringWriter;

/** Graphs written as text, in DOT for Graphviz and as Mermaid flowcharts. */
final class ShowingGraphs {

  private ShowingGraphs() {}

  /** The two text forms of a graph. */
  record Text(String full, String brief) {}

  /** Returns the steps of the build as an adjacency list and in their short form. */
  static Text text() {
    Graph steps = SmallProgram.buildSteps();
    // @start region="text"
    String full = Graphs.adjacencyListOf(steps);   // every node, one line each:
    // 0 -> [1]
    // 1 -> [3, 4, 6]
    // 2 -> [6]
    // 3 -> [5]
    // 4 -> [8]
    // 5 -> [6]
    // 6 -> [7]
    // 7 -> [8]
    // 8 -> []

    String brief = steps.toString();               // one line, for logs and debuggers:
    // Graph[nodes=9, edges=10] {0 -> [1], 1 -> [3, 4, 6], 2 -> [6], 3 -> [5], 4 -> [8],
    //   5 -> [6], 6 -> [7], 7 -> [8], 8 -> []}
    // @end
    return new Text(full, brief);
  }

  /** Returns the packages in DOT, labeled, from left to right. */
  static String dot() throws IOException {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    // @start region="dot"
    DotWriter writer = DotWriter.of(dependencies)
        .graphAttribute("rankdir", "LR")
        .nodeLabel(node -> names[node]);
    StringWriter out = new StringWriter();   // or a file, through a Writer
    writer.write(out);
    String dot = out.toString();
    // digraph {
    //   rankdir="LR";
    //   0 [label="app"];
    //   ...
    //   0 -> 1;
    //   ...
    // }
    // @end
    return dot;
  }

  /** Returns the packages in DOT, with the cycle framed and its back edge dashed. */
  static String dotStyled() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    // @start region="dotStyled"
    // Frame the SCCs with a cycle: a partition in which every other package is in no
    // group (-1).
    Sccs sccs = Components.sccsOf(dependencies);
    int[] frameOf = new int[dependencies.nodeCount()];
    for (int node = 0; node < frameOf.length; node++) {
      frameOf[node] = sccs.containsCycle(sccs.sccOf(node)) ? sccs.sccOf(node) : -1;
    }
    NodePartition frames = NodePartition.of(dependencies, frameOf, sccs.sccCount());
    EdgeTypes edgeTypes = EdgeTypes.of(dependencies);
    String dot = DotWriter.of(dependencies)
        .nodeLabel(node -> names[node])
        .clusters(frames, scc -> "SCC " + scc)
        // Draw the edges that close a cycle dashed and red.
        .edgeAttributes((edge, attributes) -> {
          if (edgeTypes.typeOf(edge) == EdgeType.BACK) {
            attributes.put("style", "dashed");
            attributes.put("color", "red");
          }
        })
        // Leave out util, the package of helpers: four packages use it, and its edges
        // only crowd the picture.
        .nodeFilter(node -> !names[node].equals("util"))
        .writeToString();
    // @end
    return dot;
  }

  /** Returns the steps of the build as a Mermaid flowchart. */
  static String mermaid() {
    Graph steps = SmallProgram.buildSteps();
    String[] names = SmallProgram.stepNames();
    // @start region="mermaid"
    String mermaid = MermaidWriter.of(steps)
        .direction(Direction.LEFT_RIGHT)
        .nodeLabel(step -> names[step])
        .nodeShape(step -> names[step].equals("publish") ? Shape.STADIUM : null)
        .writeToString();
    // flowchart LR
    //   n0["generate"]
    //   ...
    //   n8(["publish"])
    //   n0 --> n1
    //   ...
    // @end
    return mermaid;
  }

  /** Returns the rules of the grammar as a Mermaid flowchart with their SCCs framed. */
  static String mermaidStyled() {
    Graph rules = SmallGrammar.rules();
    String[] names = SmallGrammar.ruleNames();
    // @start region="mermaidStyled"
    // Frame the SCCs with a cycle: a partition in which every other rule is in no group
    // (-1).
    Sccs sccs = Components.sccsOf(rules);
    int[] frameOf = new int[rules.nodeCount()];
    for (int rule = 0; rule < frameOf.length; rule++) {
      frameOf[rule] = sccs.containsCycle(sccs.sccOf(rule)) ? sccs.sccOf(rule) : -1;
    }
    NodePartition frames = NodePartition.of(rules, frameOf, sccs.sccCount());
    String mermaid = MermaidWriter.of(rules)
        .nodeLabel(rule -> rule + ": " + names[rule])
        .clusters(frames, scc -> "SCC " + scc)
        // Outline program, the rule where a parser starts.
        .classDef("focus", "stroke:#8b5cf6,stroke-width:3px")
        .nodeClass(rule -> names[rule].equals("program") ? "focus" : null)
        // Draw the edges within an SCC, the edges that lie on a cycle, in orange.
        .edgeStyle(edge ->
            sccs.sccOf(rules.edgeSource(edge)) == sccs.sccOf(rules.edgeTarget(edge))
                ? "stroke:#f59e0b,stroke-width:3px" : null)
        .writeToString();
    // @end
    return mermaid;
  }
}
