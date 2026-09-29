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
 * Cross-validates {@link Dominators}, {@link Graphs#dominatorTree}, {@link
 * Graphs#dominanceFrontiers} and {@link Ancestry#of(Dominators)} against {@link
 * NaiveDominators}, which decides dominance by removing a node and checking reachability. Inputs
 * are control-flow shapes with a single entry and arbitrary random graphs with up to three
 * random entries, duplicates and the empty list included.
 */
class DominatorsFuzzTest {

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
  void immediateDominatorsMatchTheOracle() {
    int nontrivial = 0;
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var c = caseFor(seed);
      var oracle = new NaiveDominators(c.graph(), c.entries());
      var dominators = Dominators.of(c.graph().biAdjacentGraph(), c.entries());
      for (int node = 0; node < c.graph().nodeCount(); node++) {
        int expected = oracle.immediateDominatorOf(node);
        if (expected >= 0 && expected != 0) nontrivial++;
        assertThat(dominators.immediateDominatorOf(node))
            .as("idom(%d), %s", node, c.describe())
            .isEqualTo(expected);
        assertThat(dominators.isReachable(node))
            .as("reachable(%d), %s", node, c.describe())
            .isEqualTo(oracle.reachable[node]);
      }
    }
    // Guards against a vacuous run: sparse random graphs reach little from their entries.
    assertThat(nontrivial).as("nodes with an immediate dominator other than node 0")
        .isGreaterThan(3000);
  }

  @Test
  void dominatorTreeAndAncestryAnswerDominance() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var c = caseFor(seed);
      int n = c.graph().nodeCount();
      var oracle = new NaiveDominators(c.graph(), c.entries());
      var dominators = Dominators.of(c.graph().biAdjacentGraph(), c.entries());

      var tree = Graphs.dominatorTree(dominators);
      int treeEdges = 0;
      for (int node = 0; node < n; node++) {
        int idom = dominators.immediateDominatorOf(node);
        if (idom >= 0) {
          treeEdges++;
          assertThat(tree.hasOutNode(idom, node)).as("tree edge to %d, %s", node, c.describe())
              .isTrue();
        }
      }
      assertThat(tree.edgeCount()).as("tree edges, %s", c.describe()).isEqualTo(treeEdges);

      var index = Ancestry.of(dominators);
      var preorder = PreorderAncestry.of(index);
      var rnd = new Random(seed);
      for (int q = 0; q < 400; q++) {
        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n);
        boolean expected = oracle.reachable[b] ? oracle.dominates[a][b] : a == b;
        assertThat(index.isAncestorOf(a, b))
            .as("%d dominates %d, %s", a, b, c.describe())
            .isEqualTo(expected);
        assertThat(preorder.isAncestorOf(a, b))
            .as("%d dominates %d (preorder), %s", a, b, c.describe())
            .isEqualTo(expected);
      }
    }
  }

  @Test
  void dominanceFrontiersMatchTheDefinition() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var c = caseFor(seed);
      int n = c.graph().nodeCount();
      var expected = new NaiveDominators(c.graph(), c.entries()).frontiers();
      var df = Graphs.dominanceFrontiers(
          Dominators.of(c.graph().biAdjacentGraph(), c.entries()));
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
