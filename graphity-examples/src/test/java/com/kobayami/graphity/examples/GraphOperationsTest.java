package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

class GraphOperationsTest {

  @Test
  void findCycleReturnsModelAndStorageClosedByStorageToModel() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    IntList cycle = GraphOperations.cycleInDependencies();
    assertThat(cycle.toString()).isEqualTo("[4, 5]");
    assertThat(Names.of(cycle, names)).containsExactly("model", "storage");
    assertThat(dependencies.hasOutNode(cycle.getInt(1), cycle.getInt(0))).isTrue();
  }

  @Test
  void adjacencyListOfListsTheWholeControlFlowOfLoad() {
    assertThat(GraphOperations.cfgAsAdjacencyList()).isEqualTo("""
        0 -> [1]
        1 -> [2, 5]
        2 -> [3, 4]
        3 -> [4]
        4 -> [1]
        5 -> []
        """);
  }
}
