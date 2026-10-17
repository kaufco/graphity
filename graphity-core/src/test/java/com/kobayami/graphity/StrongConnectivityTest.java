package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class StrongConnectivityTest {

  private static StrongConnectivity of(Graph graph) {
    return Components.strongConnectivityOf(BiAdjacentGraph.of(graph));
  }

  private static List<String> bridges(StrongConnectivity result) {
    var bridges = result.strongBridges();
    var list = new ArrayList<String>();
    for (int i = 0; i < bridges.count(); i++) {
      list.add(bridges.sourceNodes().getInt(i) + "->" + bridges.targetNodes().getInt(i));
    }
    return list;
  }

  private static int[] points(StrongConnectivity result) {
    return result.strongArticulationPoints().toIntArray();
  }

  // ---- Definitions ----

  @Test
  void cycleWithoutChordsHasOnlyStrongBridgesAndArticulationPoints() {
    var result = of(TestGraphs.cycle(5));
    assertThat(bridges(result)).containsExactly("0->1", "1->2", "2->3", "3->4", "4->0");
    assertThat(points(result)).containsExactly(0, 1, 2, 3, 4);
  }

  @Test
  void twoNodeCycleHasTwoStrongBridgesButNoArticulationPoint() {
    var result = of(TestGraphs.cycle(2));
    assertThat(bridges(result)).containsExactly("0->1", "1->0");
    assertThat(points(result)).isEmpty();
  }

  @Test
  void redundantPathsLeaveNothing() {
    var result = of(TestGraphs.clique(4));
    assertThat(bridges(result)).isEmpty();
    assertThat(points(result)).isEmpty();
  }

  @Test
  void edgeBetweenTwoTanglesIsNotAStrongBridge() {
    // Cycles {0, 1, 2} and {3, 4, 5}, joined by 2 -> 3.
    var g = TestGraphs.of(6, 0, 1, 1, 2, 2, 0, 3, 4, 4, 5, 5, 3, 2, 3);
    var result = of(g);
    assertThat(bridges(result))
        .containsExactly("0->1", "1->2", "2->0", "3->4", "4->5", "5->3");
    assertThat(points(result)).containsExactly(0, 1, 2, 3, 4, 5);
  }

  @Test
  void hubOfTwoCyclesIsTheOnlyArticulationPoint() {
    // Cycles 0 -> 1 -> 0 and 0 -> 2 -> 0 share node 0.
    var result = of(TestGraphs.of(3, 0, 1, 1, 0, 0, 2, 2, 0));
    assertThat(points(result)).containsExactly(0);
    assertThat(bridges(result)).containsExactly("0->1", "0->2", "1->0", "2->0");
  }

  @Test
  void chordIsNoStrongBridge() {
    // 0 -> 1 -> 2 -> 0 plus the chord 0 -> 2, which the cycle bypasses; node 1 is not needed
    // for 0 and 2 to reach each other.
    var result = of(TestGraphs.of(3, 0, 1, 1, 2, 2, 0, 0, 2));
    assertThat(bridges(result)).containsExactly("0->1", "1->2", "2->0");
    assertThat(points(result)).containsExactly(0, 2);
  }

  @Test
  void selfLoopIsNeverAStrongBridge() {
    assertThat(bridges(of(TestGraphs.of(1, 0, 0)))).isEmpty();
    assertThat(bridges(of(TestGraphs.of(3, 0, 1, 1, 2, 2, 0, 1, 1))))
        .containsExactly("0->1", "1->2", "2->0");
  }

  @Test
  void acyclicAndEmptyGraphsHaveNothing() {
    for (Graph g : new Graph[] {
        TestGraphs.empty(), TestGraphs.isolated(3), TestGraphs.path(5), TestGraphs.outStar(4),
        RandomGraphs.randomDag(40, 0.2, 7), RandomGraphs.randomTree(40, 7)}) {
      var result = of(g);
      assertThat(bridges(result)).isEmpty();
      assertThat(points(result)).isEmpty();
    }
  }

  @Test
  void entryOfItsComponentIsFoundToo() {
    // Node 0, the lowest of its component and so the entry the computation starts from, is found
    // by the separate check for entries. Cycles 0 -> 1 -> 2 -> 0 and 0 -> 3 -> 4 -> 0 share
    // node 0; node 5 hangs off as a sink, and the edge 4 -> 5 between components changes nothing.
    var g = TestGraphs.of(6, 0, 1, 1, 2, 2, 0, 0, 3, 3, 4, 4, 0, 4, 5);
    assertThat(points(of(g))).containsExactly(0, 1, 2, 3, 4);
  }

  // ---- Against removal and recount ----

  @Test
  void matchesRemovalOnRandomGraphs() {
    var random = new Random(31);
    for (int iteration = 0; iteration < 400; iteration++) {
      int n = 1 + random.nextInt(12);
      double p = 0.05 + random.nextDouble() * 0.4;
      assertMatchesRemoval(RandomGraphs.gnp(n, p, random.nextLong()), "gnp " + iteration);
    }
    for (long seed = 0; seed < 150; seed++) {
      RandomGraphs.LabeledGraph lg = RandomGraphs.anyGraph(seed);
      if (lg.graph().nodeCount() <= 60) assertMatchesRemoval(lg.graph(), lg.describe());
    }
  }

  private static void assertMatchesRemoval(Graph g, String description) {
    int n = g.nodeCount();
    var sccs = Components.sccsOf(g);
    int count = sccs.groupCount();

    var expectedBridges = new ArrayList<String>();
    for (int e = 0; e < g.edgeCount(); e++) {
      if (Components.sccsOf(without(g, e, -1)).groupCount() > count) {
        String key = g.edgeSource(e) + "->" + g.edgeTarget(e);
        if (!expectedBridges.contains(key)) expectedBridges.add(key);
      }
    }
    var expectedPoints = new IntArrayList();
    for (int v = 0; v < n; v++) {
      // v stays as an isolated node, which adds one component of its own.
      int remaining = Components.sccsOf(without(g, -1, v)).groupCount() - 1;
      int before = count - (sccs.sizeOf(sccs.get(v)) == 1 ? 1 : 0);
      if (remaining > before) expectedPoints.add(v);
    }

    var result = of(g);
    assertThat(bridges(result)).as("strong bridges of %s", description)
        .containsExactlyElementsOf(expectedBridges);
    assertThat(points(result)).as("strong articulation points of %s", description)
        .containsExactly(expectedPoints.toIntArray());
  }

  /** The graph without edge index {@code edge} and without the edges at node {@code node}. */
  private static Graph without(Graph g, int edge, int node) {
    var edges = new IntArrayList();
    for (int e = 0; e < g.edgeCount(); e++) {
      int source = g.edgeSource(e);
      int target = g.edgeTarget(e);
      if (e == edge || source == node || target == node) continue;
      edges.add(source);
      edges.add(target);
    }
    return TestGraphs.of(g.nodeCount(), edges.toIntArray());
  }
}
