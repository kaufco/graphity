package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Edges;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConnectivityTest {

  @Test
  void sccsOfGivesThePackagesInBuildOrder() {
    assertThat(Connectivity.buildOrder()).containsExactly(
        List.of("util"), List.of("cli"), List.of("json"), List.of("model", "storage"),
        List.of("service"), List.of("ui"), List.of("app"));
  }

  @Test
  void biconnectivityOfLoadHasTheEdgesOutsideTheLoopAsBridgesAndHeaderAsArticulationPoint() {
    String[] names = SmallProgram.blockNames();
    Connectivity.Splitters splitters = Connectivity.bridgesOfLoad();
    assertThat(edgesOf(splitters.edges(), names))
        .containsExactly("entry -> header", "header -> exit");
    assertThat(splitters.nodes().toString()).isEqualTo("[1]");
    assertThat(Names.of(splitters.nodes(), names)).containsExactly("header");
  }

  @Test
  void strongConnectivityOfLoadSparesOnlyTheBranchThroughThen() {
    String[] names = SmallProgram.blockNames();
    Connectivity.Splitters splitters = Connectivity.strongBridgesOfLoad();
    assertThat(edgesOf(splitters.edges(), names)).containsExactly(
        "header -> body", "body -> then", "then -> next", "next -> header");
    assertThat(splitters.nodes().toString()).isEqualTo("[1, 2, 4]");
    assertThat(Names.of(splitters.nodes(), names)).containsExactly("header", "body", "next");
  }

  // The edges as the comments of the examples write them, in their order.
  private static List<String> edgesOf(Edges edges, String[] names) {
    List<String> result = new ArrayList<>();
    for (int p = 0; p < edges.count(); p++) {
      result.add(names[edges.sourceNodes().getInt(p)] + " -> "
          + names[edges.targetNodes().getInt(p)]);
    }
    return result;
  }
}
