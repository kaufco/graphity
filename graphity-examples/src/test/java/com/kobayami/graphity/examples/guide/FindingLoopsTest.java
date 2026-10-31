package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FindingLoopsTest {

  @Test
  void theLoopOverTheLinesLiesInTheLoopOverTheNotes() {
    FindingLoops.Nesting nesting = FindingLoops.loopsOfShow();
    assertThat(nesting.loopCount()).isEqualTo(2);
    assertThat(nesting.notesLoop()).isEqualTo(0);
    assertThat(nesting.linesLoop()).isEqualTo(1);
    assertThat(nesting.header()).isEqualTo(3);
    assertThat(nesting.body().toString()).isEqualTo("[1, 2, 3, 4]");
    assertThat(nesting.parent()).isEqualTo(0);
    assertThat(nesting.depth()).isEqualTo(2);
  }
}
