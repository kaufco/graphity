package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.ControlFlowGraphs;
import com.kobayami.graphity.testutils.NaiveDominators;
import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link IteratedDominanceFrontier} against the fixpoint over the dominance
 * frontiers that {@link NaiveDominators} derives from the definition of dominance.
 */
class IteratedDominanceFrontierFuzzTest {

  private static final int ITERATIONS = 100;
  private static final int QUERIES_PER_GRAPH = 20;

  @Test
  void frontierOfMatchesTheFixpoint() {
    int nonEmpty = 0;
    for (long seed = 0; seed < ITERATIONS; seed++) {
      Graph graph;
      IntList entries;
      String describe;
      if (seed % 2 == 0) {
        var cfg = ControlFlowGraphs.anyControlFlowGraph(seed);
        graph = cfg.graph();
        entries = IntList.of(0);
        describe = cfg.describe();
      } else {
        var labeled = RandomGraphs.anyGraph(seed);
        graph = labeled.graph();
        var r = new Random(seed);
        entries = IntList.of(r.nextInt(graph.nodeCount()), r.nextInt(graph.nodeCount()));
        describe = labeled.describe() + ", entries=" + entries;
      }
      int n = graph.nodeCount();
      var oracle = new NaiveDominators(graph, entries);
      var frontiers = oracle.frontiers();
      var idf = IteratedDominanceFrontier.of(Dominators.of(graph.biAdjacentGraph(), entries));

      var rnd = new Random(seed * 13 + 7);
      for (int q = 0; q < QUERIES_PER_GRAPH; q++) {
        var nodes = new IntArrayList();
        int count = 1 + rnd.nextInt(Math.min(8, n));
        for (int i = 0; i < count; i++) nodes.add(rnd.nextInt(n));

        var expected = oracle.iteratedFrontier(frontiers, nodes);
        var expectedList = new IntArrayList();
        for (int m = 0; m < n; m++) {
          if (expected[m]) expectedList.add(m);
        }
        if (!expectedList.isEmpty()) nonEmpty++;
        assertThat(idf.frontierOf(nodes).toIntArray())
            .as("IDF(%s), %s", nodes, describe)
            .isEqualTo(expectedList.toIntArray());
      }
    }
    assertThat(nonEmpty).as("queries with a non-empty frontier").isGreaterThan(300);
  }
}
