package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MappingsTest {

  @Test
  void subgraphKeepsTheMethodsThatLoadReachesWithTheirNames() {
    Mappings.Loading loading = Mappings.loading();
    assertThat(loading.names()).containsExactly("load", "parse", "read");
    assertThat(edgesByName(loading.graph(), loading.names()))
        .containsExactly("load -> parse", "load -> read", "parse -> parse");
  }

  @Test
  void edgeMappingCarriesTheCallSitesOverToTheSubgraph() {
    Mappings.Loading loading = Mappings.loading();
    Graph graph = loading.graph();
    String[] names = loading.names();
    assertThat(loading.siteCount()).hasSize(graph.edgeCount());
    List<String> counted = new ArrayList<>();
    graph.forEachEdge((caller, callee, edge) -> counted.add(
        names[caller] + " -> " + names[callee] + ": " + loading.siteCount()[edge]));
    assertThat(counted).containsExactlyInAnyOrder(
        "load -> parse: 1", "load -> read: 1", "parse -> parse: 2");
  }

  @Test
  void transposedEdgeMappingPrintsTheCallersOfParseWithTheirCallSites() {
    var bytes = new ByteArrayOutputStream();
    Mappings.callersOfParse(new PrintStream(bytes, true, StandardCharsets.UTF_8));
    assertThat(bytes.toString(StandardCharsets.UTF_8).lines())
        .containsExactly("load: 1", "parse: 2");
  }

  // The edges of a graph in edge index order, each as "source -> target" with the names.
  static List<String> edgesByName(Graph graph, String[] names) {
    List<String> edges = new ArrayList<>();
    graph.forEachEdge((source, target, edge) -> edges.add(names[source] + " -> " + names[target]));
    return edges;
  }
}
