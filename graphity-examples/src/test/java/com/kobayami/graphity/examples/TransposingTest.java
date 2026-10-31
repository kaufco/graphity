package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TransposingTest {

  @Test
  void transposedReachesTheDependentsOfModel() {
    assertThat(SmallProgram.packageNames()[4]).isEqualTo("model");
    assertThat(Transposing.affectedByModel().toString()).isEqualTo("[0, 2, 3, 4, 5]");
    assertThat(Names.of(Transposing.affectedByModel(), SmallProgram.packageNames()))
        .containsExactly("app", "ui", "service", "model", "storage");
  }
}
