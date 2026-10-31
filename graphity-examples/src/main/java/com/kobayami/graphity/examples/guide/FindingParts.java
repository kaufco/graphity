package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.ArrayList;
import java.util.List;

/**
 * The weakly connected components of the calls between methods: the groups of methods
 * that calls connect when their direction does not matter.
 */
final class FindingParts {

  private FindingParts() {}

  /** The components, by name, and two checks. */
  record Wccs(Graph rest, int componentCount, List<List<String>> components,
      boolean callsConnected, boolean restConnected) {}

  /** Returns the calls without those made by main and run. */
  static Graph callsBelowRun() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int main = SmallProgram.nodeOf(names, "main");
    int run = SmallProgram.nodeOf(names, "run");
    // @start region="rest"
    // Leave out the calls of main and run, which call into every area of the program.
    var builder = new GraphBuilder();
    builder.addNodes(calls.nodeCount());
    calls.forEachEdge((caller, callee, edge) -> {
      if (caller != main && caller != run) {
        builder.addEdge(caller, callee);
      }
    });
    Graph rest = builder.build();
    // @end
    return rest;
  }

  /** Returns the weakly connected components of the calls without main and run. */
  static Wccs wccs() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    Graph rest = callsBelowRun();
    // @start region="wccsOf"
    NodePartition components = Components.wccsOf(rest);
    int componentCount = components.groupCount();   // 5
    List<List<String>> byName = new ArrayList<>();
    for (int component = 0; component < componentCount; component++) {
      byName.add(
          components.nodesOf(component).intStream().mapToObj(m -> names[m]).toList());
    }
    // byName: [[main], [parseArgs], [run], [load, parse, read],
    //          [save, show, format, write]]

    boolean callsConnected = Graphs.isWeaklyConnected(calls);   // true
    boolean restConnected = Graphs.isWeaklyConnected(rest);     // false
    // @end
    return new Wccs(rest, componentCount, byName, callsConnected, restConnected);
  }
}
