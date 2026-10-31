package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WritingMermaidTest {

  private static final String[] NAMES = SmallProgram.packageNames();

  @Test
  void theCycleIsModelAndStorageWithTheirNodeIds() {
    assertThat(NAMES[4]).isEqualTo("model");
    assertThat(NAMES[5]).isEqualTo("storage");
    assertThat(WritingMermaid.cycle()).isEqualTo("""
        flowchart TD
          n4["model"]
          n5["storage"]
          n4 --> n5
          n5 --> n4
        """);
  }

  @Test
  void onlyModelAndStorageAreInTheRedClass() {
    String mermaid = WritingMermaid.cycleInRed();
    assertThat(mermaid).contains(
        "  classDef cycle stroke:red\n",
        "  class n4 cycle\n",
        "  class n5 cycle\n");
    assertThat(mermaid.lines().filter(line -> line.startsWith("  class "))).hasSize(2);
  }
}
