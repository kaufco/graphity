package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.examples.AnalyzingControlFlow.LoopOfBlock;
import org.junit.jupiter.api.Test;

class AnalyzingControlFlowTest {

  private static final String[] NAMES = SmallProgram.blockNames();

  @Test
  void loopsFindTheForLoopWithItsHeaderAndBody() {
    LoopOfBlock found = AnalyzingControlFlow.loopOfThen();
    assertThat(found.loops().loopCount()).as("the only loop").isEqualTo(1);
    assertThat(found.loop()).isZero();
    assertThat(found.header()).isEqualTo(1);
    assertThat(NAMES[found.header()]).isEqualTo("header");
    assertThat(found.blocks().toString()).isEqualTo("[1, 2, 3, 4]");
    assertThat(Names.of(found.blocks(), NAMES)).containsExactly("header", "body", "then", "next");
  }

  @Test
  void theVariableIOfTheLoopNeedsAPhiNodeInTheHeader() {
    assertThat(AnalyzingControlFlow.phiBlocksOfI().toString()).isEqualTo("[1]");
    assertThat(Names.of(AnalyzingControlFlow.phiBlocksOfI(), NAMES)).containsExactly("header");
  }

  @Test
  void theLoopConditionAndTheIfDecideWhichBlocksRun() {
    Graph controlDependence = AnalyzingControlFlow.controlDependenceOfLoad();
    assertThat(controlDependence.nodeCount()).isEqualTo(NAMES.length);
    for (int block = 0; block < NAMES.length; block++) {
      var decides = Names.of(controlDependence.outNodes(block), NAMES);
      switch (NAMES[block]) {
        case "header" -> assertThat(decides).containsExactly("header", "body", "next");
        case "body" -> assertThat(decides).containsExactly("then");
        default -> assertThat(decides).as(NAMES[block]).isEmpty();
      }
    }
  }
}
