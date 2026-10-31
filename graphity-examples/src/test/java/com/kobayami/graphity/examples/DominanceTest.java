package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DominanceTest {

  @Test
  void bodyIsTheImmediateDominatorOfNextAndDominatesItWhileThenDoesNot() {
    String[] names = SmallProgram.blockNames();
    Dominance.DominatorsOfNext result = Dominance.dominatorsOfNext();
    assertThat(result.idom()).isEqualTo(2);
    assertThat(names[result.idom()]).isEqualTo("body");
    assertThat(result.bodyDominatesNext()).isTrue();
    assertThat(result.thenDominatesNext()).isFalse();
  }

  @Test
  void nextIsTheImmediatePostDominatorOfBody() {
    assertThat(Dominance.postDominatorOfBody()).isEqualTo(4);
    assertThat(SmallProgram.blockNames()[Dominance.postDominatorOfBody()]).isEqualTo("next");
  }

  @Test
  void bodyDominatesItselfThenAndNext() {
    assertThat(Dominance.dominatedByBody().toString()).isEqualTo("[2, 3, 4]");
    assertThat(Names.of(Dominance.dominatedByBody(), SmallProgram.blockNames()))
        .containsExactly("body", "then", "next");
  }
}
