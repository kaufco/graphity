package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graphs;
import org.junit.jupiter.api.Test;

class CondensingCyclesTest {

  @Test
  void theCondensedRulesAreAcyclicAndNamedInBuildOrder() {
    CondensingCycles.Condensed condensed = CondensingCycles.condense();
    assertThat(condensed.acyclic()).isTrue();
    assertThat(condensed.groupNames()).containsExactly(
        "name", "number", "expression/term/factor", "assignment",
        "statement/block/ifStatement", "program");
  }

  // The guide draws the condensed graph; its edges must be the ones drawn there.
  @Test
  void theCondensedGraphHasTheEdgesOfTheDiagram() {
    assertThat(Graphs.adjacencyListOf(CondensingCycles.condense().graph())).isEqualTo("""
        0 -> []
        1 -> []
        2 -> [0, 1]
        3 -> [0, 2]
        4 -> [2, 3]
        5 -> [4]
        """);
  }

  @Test
  void theStatementsUseTheExpressionsAndAssignment() {
    assertThat(CondensingCycles.condense().usedByStatements().toString()).isEqualTo("[2, 3]");
  }
}
