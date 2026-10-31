package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WritingDotTest {

  private static final String[] NAMES = SmallProgram.packageNames();

  @Test
  void theCycleIsModelAndStorageWithTheirNodeIds() {
    assertThat(NAMES[4]).isEqualTo("model");
    assertThat(NAMES[5]).isEqualTo("storage");
    assertThat(WritingDot.cycle()).isEqualTo("""
        digraph {
          4 [label="model"];
          5 [label="storage"];
          4 -> 5;
          5 -> 4;
        }
        """);
  }

  @Test
  void onlyModelAndStorageAreRed() {
    String dot = WritingDot.cycleInRed();
    assertThat(dot).contains(
        "  4 [label=\"model\", color=\"red\"];\n",
        "  5 [label=\"storage\", color=\"red\"];\n");
    assertThat(dot.lines().filter(line -> line.contains("color"))).hasSize(2);
  }
}
