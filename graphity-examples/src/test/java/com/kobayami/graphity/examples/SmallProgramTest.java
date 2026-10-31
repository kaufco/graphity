package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.Sccs;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins the graphs of the example domain, on which every example comment depends. */
class SmallProgramTest {

  @Test
  void dependenciesHaveOneCycleBetweenModelAndStorage() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    assertThat(names).containsExactly(
        "app", "cli", "ui", "service", "model", "storage", "json", "util");
    assertThat(dependencies.nodeCount()).isEqualTo(names.length);
    assertThat(dependencies.edgeCount()).isEqualTo(14);
    assertThat(Graphs.adjacencyListOf(dependencies)).isEqualTo("""
        0 -> [1, 2, 3]
        1 -> [7]
        2 -> [3, 4, 7]
        3 -> [4, 5]
        4 -> [5]
        5 -> [4, 6, 7]
        6 -> [7]
        7 -> []
        """);

    assertThat(cyclesOf(dependencies, names)).containsExactly(List.of("model", "storage"));
  }

  @Test
  void callsMergeTheCallSitesAndHaveTwoRecursiveMethods() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    assertThat(names).containsExactly(
        "main", "parseArgs", "run", "load", "save", "show", "parse", "format", "read", "write");
    assertThat(calls.nodeCount()).isEqualTo(names.length);
    assertThat(calls.edgeCount()).isEqualTo(12);
    assertThat(SmallProgram.callSites()).hasSize(14);
    assertThat(Graphs.adjacencyListOf(calls)).isEqualTo("""
        0 -> [1, 2]
        1 -> []
        2 -> [3, 4, 5]
        3 -> [6, 8]
        4 -> [7, 9]
        5 -> [7]
        6 -> [6]
        7 -> [7]
        8 -> []
        9 -> []
        """);
    for (SmallProgram.CallSite site : SmallProgram.callSites()) {
      assertThat(calls.hasOutNode(site.caller(), site.callee())).isTrue();
    }
    assertThat(cyclesOf(calls, names))
        .containsExactlyInAnyOrder(List.of("parse"), List.of("format"));
  }

  @Test
  void cfgIsALoopWithABranchFromEntryToExit() {
    Graph cfg = SmallProgram.cfg();
    String[] names = SmallProgram.blockNames();
    assertThat(names).containsExactly("entry", "header", "body", "then", "next", "exit");
    assertThat(cfg.nodeCount()).isEqualTo(names.length);
    assertThat(Graphs.adjacencyListOf(cfg)).isEqualTo("""
        0 -> [1]
        1 -> [2, 5]
        2 -> [3, 4]
        3 -> [4]
        4 -> [1]
        5 -> []
        """);
    assertThat(Names.of(Graphs.rootNodes(BiAdjacentGraph.of(cfg)), names))
        .containsExactly("entry");
    assertThat(cyclesOf(cfg, names)).containsExactly(List.of("header", "body", "then", "next"));
  }

  @Test
  void packageOfPutsEveryCallBetweenTwoPackagesOnADependency() {
    Graph calls = SmallProgram.calls();
    Graph dependencies = SmallProgram.dependencies();
    String[] packageNames = SmallProgram.packageNames();
    int[] packageOf = SmallProgram.packageOf();
    assertThat(packageOf).hasSize(calls.nodeCount());
    assertThat(Names.of(IntList.of(packageOf), packageNames)).containsExactly(
        "app", "cli", "app", "service", "service", "ui", "model", "model", "storage", "storage");
    calls.forEachEdge((caller, callee, edge) -> {
      if (packageOf[caller] != packageOf[callee]) {
        assertThat(dependencies.hasOutNode(packageOf[caller], packageOf[callee])).isTrue();
      }
    });
  }

  // The strongly connected components that contain a cycle, each as the names of its nodes.
  private static List<List<String>> cyclesOf(Graph graph, String[] names) {
    Sccs sccs = Components.sccsOf(graph);
    List<List<String>> cycles = new ArrayList<>();
    for (int scc = 0; scc < sccs.sccCount(); scc++) {
      if (sccs.containsCycle(scc)) {
        cycles.add(Names.of(sccs.nodesOf(scc), names));
      }
    }
    return cycles;
  }

  @Test
  void namesReturnCopiesAndLookUpNodes() {
    String[] names = SmallProgram.packageNames();
    names[0] = "changed";
    assertThat(SmallProgram.packageNames()[0]).isEqualTo("app");
    assertThat(SmallProgram.nodeOf(SmallProgram.blockNames(), "exit")).isEqualTo(5);
    assertThatThrownBy(() -> SmallProgram.nodeOf(SmallProgram.blockNames(), "loop"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
