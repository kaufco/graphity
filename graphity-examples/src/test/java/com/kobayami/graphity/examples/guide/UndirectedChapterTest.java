package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Connectivity when direction does not matter". */
class UndirectedChapterTest {

  @Test
  void withoutMainAndRunTheCallsFallIntoFiveComponents() {
    FindingParts.Wccs wccs = FindingParts.wccs();
    assertThat(Graphs.adjacencyListOf(wccs.rest())).isEqualTo("""
        0 -> []
        1 -> []
        2 -> []
        3 -> [6, 8]
        4 -> [7, 9]
        5 -> [7]
        6 -> [6]
        7 -> [7]
        8 -> []
        9 -> []
        """);
    assertThat(wccs.componentCount()).isEqualTo(5);
    assertThat(wccs.components()).containsExactly(List.of("main"), List.of("parseArgs"),
        List.of("run"), List.of("load", "parse", "read"),
        List.of("save", "show", "format", "write"));
    assertThat(wccs.callsConnected()).isTrue();
    assertThat(wccs.restConnected()).isFalse();
  }

  @Test
  void bridgesAndArticulationPointsOfTheCalls() {
    FindingBridges.Splitters splitters = FindingBridges.splitters();
    assertThat(OrderChapterTest.edges(splitters.bridges(), SmallProgram.methodNames()))
        .containsExactly("main -> parseArgs", "main -> run", "run -> load", "load -> parse",
            "load -> read", "save -> write");
    assertThat(splitters.articulationPoints().toString()).isEqualTo("[0, 2, 3, 4]");
  }

  @Test
  void blocksOfTheCalls() {
    FindingBridges.Blocks blocks = FindingBridges.blocks();
    assertThat(blocks.blockCount()).isEqualTo(7);
    assertThat(blocks.largeBlock())
        .containsExactly("run -> save", "run -> show", "save -> format", "show -> format");
    assertThat(blocks.inNoBlock()).containsExactly("parse -> parse", "format -> format");
  }

  @Test
  void bipartite() {
    CheckingUndirectedShapes.Bipartite bipartite = CheckingUndirectedShapes.bipartite();
    assertThat(bipartite.family()).isTrue();
    assertThat(bipartite.dependencies()).isFalse();
  }

  @Test
  void treesAndForests() {
    CheckingUndirectedShapes.Shapes shapes = CheckingUndirectedShapes.shapes();
    assertThat(shapes.familyTree()).isTrue();
    assertThat(shapes.restForest()).isTrue();
    assertThat(shapes.restTree()).isFalse();
    assertThat(shapes.callsForest()).isFalse();
  }
}
