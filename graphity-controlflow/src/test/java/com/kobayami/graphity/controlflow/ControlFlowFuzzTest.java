package com.kobayami.graphity.controlflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.testutils.ControlFlowGraphs;
import com.kobayami.graphity.testutils.NaiveDominators;
import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link ControlFlow#dominanceFrontiers} against {@link NaiveDominators}, which
 * decides dominance by removing a node and checking reachability. Inputs are control-flow shapes
 * with a single entry and arbitrary random graphs with up to three random entries, duplicates
 * and the empty list included.
 */
class ControlFlowFuzzTest {

  private static final int ITERATIONS = 120;

  private record Case(String describe, Graph graph, IntList entries) {}

  private static Case caseFor(long seed) {
    if (seed % 2 == 0) {
      var cfg = ControlFlowGraphs.anyControlFlowGraph(seed);
      return new Case(cfg.describe(), cfg.graph(), IntList.of(0));
    }
    var labeled = RandomGraphs.anyGraph(seed);
    int n = labeled.graph().nodeCount();
    var rnd = new Random(seed * 17 + 5);
    var entries = new IntArrayList();
    int count = rnd.nextInt(10) == 0 ? 0 : 1 + rnd.nextInt(3);
    for (int i = 0; i < count; i++) entries.add(rnd.nextInt(n));
    return new Case(labeled.describe() + ", entries=" + entries, labeled.graph(), entries);
  }

  @Test
  void dominanceFrontiersMatchTheDefinition() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var c = caseFor(seed);
      int n = c.graph().nodeCount();
      var expected = new NaiveDominators(c.graph(), c.entries()).frontiers();
      var df = ControlFlow.dominanceFrontiers(
          Dominators.of(BiAdjacentGraph.of(c.graph()), c.entries()));
      for (int x = 0; x < n; x++) {
        for (int m = 0; m < n; m++) {
          assertThat(df.hasOutNode(x, m))
              .as("%d in DF(%d), %s", m, x, c.describe())
              .isEqualTo(expected[x][m]);
        }
      }
    }
  }
}
