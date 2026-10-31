package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.examples.SmallProgram;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Strongly connected components" added after the pilot. */
class SccChapterTest {

  @Test
  void buildOrderOfThePackages() {
    OrderingDespiteCycles.Order order = OrderingDespiteCycles.buildOrder();
    assertThat(order.buildOrder()).containsExactly(
        List.of("util"), List.of("cli"), List.of("json"), List.of("model", "storage"),
        List.of("service"), List.of("ui"), List.of("app"));
    assertThat(order.jsonBeforeUi()).isTrue();
  }

  @Test
  void theControlFlowOfLoadIsStronglyConnectedOnlyWithTheEdgeFromExitToEntry() {
    CheckingStrongConnectivity.Answers answers = CheckingStrongConnectivity.check();
    assertThat(answers.cfgStronglyConnected()).isFalse();
    assertThat(answers.loadWellFormed()).isTrue();
  }

  @Test
  void strongBridgesAndArticulationPointsOfLoad() {
    SplittingGroups.Splitters splitters = SplittingGroups.ofLoad();
    assertThat(OrderChapterTest.edges(splitters.strongBridges(), SmallProgram.blockNames()))
        .containsExactly("header -> body", "body -> then", "then -> next", "next -> header");
    assertThat(splitters.strongArticulationPoints().toString()).isEqualTo("[1, 2, 4]");
  }
}
