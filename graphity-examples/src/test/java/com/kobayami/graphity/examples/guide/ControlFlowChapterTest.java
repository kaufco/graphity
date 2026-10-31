package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graphs;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Control-flow analysis" added after the pilot. */
class ControlFlowChapterTest {

  @Test
  void theLoopOfParseArgsHasTwoEntries() {
    AnalyzingControlFlowFurther.Irreducible irreducible = AnalyzingControlFlowFurther.irreducible();
    assertThat(irreducible.reducible()).isFalse();
    assertThat(irreducible.loopCount()).isEqualTo(1);
    assertThat(irreducible.entries().toString()).isEqualTo("[1, 2]");
    assertThat(irreducible.header()).isEqualTo(1);
  }

  @Test
  void dominanceFrontiersOfLoad() {
    assertThat(Graphs.adjacencyListOf(AnalyzingControlFlowFurther.dominanceFrontiers()))
        .isEqualTo("""
            0 -> []
            1 -> [1]
            2 -> [1]
            3 -> [4]
            4 -> [1]
            5 -> []
            """);
  }

  @Test
  void phiBlocksOfCount() {
    AnalyzingControlFlowFurther.Phis phis = AnalyzingControlFlowFurther.phiBlocks();
    assertThat(phis.forI().toString()).isEqualTo("[1]");
    assertThat(phis.forLast().toString()).isEqualTo("[1, 4]");
  }

  @Test
  void controlDependenceOfLoad() {
    assertThat(Graphs.adjacencyListOf(AnalyzingControlFlowFurther.controlDependence()))
        .isEqualTo("""
            0 -> []
            1 -> [1, 2, 4]
            2 -> [3]
            3 -> []
            4 -> []
            5 -> []
            """);
  }
}
