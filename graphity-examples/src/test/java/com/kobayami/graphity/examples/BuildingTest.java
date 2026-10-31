package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BuildingTest {

  @Test
  void ofBuildsTheControlFlowOfLoad() {
    assertThat(Building.cfgFromArrays()).isEqualTo(SmallProgram.cfg());
  }

  @Test
  void toStringGivesTheTextInTheComment() {
    assertThat(Building.cfgAsText()).isEqualTo(
        "Graph[nodes=6, edges=7] {0 -> [1], 1 -> [2, 5], 2 -> [3, 4], 3 -> [4], 4 -> [1], 5 -> []}");
  }
}
