package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.JGraphtAdapter;
import com.kobayami.graphity.testutils.RandomGraphs;
import org.jgrapht.alg.cycle.CycleDetector;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates shape predicates in {@link Graphs} against independent references:
 * {@code isDag} against JGraphT's {@link CycleDetector}; {@code isForest},
 * {@code isOutTree}, {@code isPath} against their structural definitions computed
 * from scratch via plain arrays.
 */
class GraphsFuzzTest {

  private static final int ITERATIONS = 100;
  private static final long BASE_SEED = 0x15DA6EDL;

  @Test
  void isDagAgreesWithJgraphtCycleDetector() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isDag(g);
      boolean theirs = !new CycleDetector<>(JGraphtAdapter.toJgrapht(g)).detectCycles();

      assertThat(ours)
          .as("isDag mismatch vs JGraphT for %s", labeled.describe())
          .isEqualTo(theirs);
    }
  }

  @Test
  void isForestAgreesWithDefinition() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isForest(g);
      boolean expected = isForestReference(g);

      assertThat(ours)
          .as("isForest mismatch for %s", labeled.describe())
          .isEqualTo(expected);
    }
  }

  @Test
  void isOutTreeAgreesWithDefinition() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isOutTree(g);
      boolean expected = isOutTreeReference(g);

      assertThat(ours)
          .as("isOutTree mismatch for %s", labeled.describe())
          .isEqualTo(expected);
    }
  }

  @Test
  void isPathAgreesWithDefinition() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isPath(g);
      boolean expected = isPathReference(g);

      assertThat(ours)
          .as("isPath mismatch for %s", labeled.describe())
          .isEqualTo(expected);
    }
  }

  @Test
  void shapeHierarchyHolds() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      if (Graphs.isPath(g)) {
        assertThat(Graphs.isOutTree(g))
            .as("isPath ⇒ isOutTree violated for %s", labeled.describe()).isTrue();
      }
      if (Graphs.isOutTree(g)) {
        assertThat(Graphs.isForest(g))
            .as("isOutTree ⇒ isForest violated for %s", labeled.describe()).isTrue();
      }
      if (Graphs.isForest(g)) {
        assertThat(Graphs.isDag(g))
            .as("isForest ⇒ isDag violated for %s", labeled.describe()).isTrue();
      }
    }
  }

  /** A forest: DAG where every node has in-degree ≤ 1. */
  private static boolean isForestReference(Graph graph) {
    if (!Graphs.isDag(graph)) return false;
    int[] inDeg = inDegrees(graph);
    for (int v = 0; v < graph.nodeCount(); v++) {
      if (inDeg[v] > 1) return false;
    }
    return true;
  }

  /** An out-tree: forest with exactly one root (in-degree == 0), or an empty graph. */
  private static boolean isOutTreeReference(Graph graph) {
    if (!isForestReference(graph)) return false;
    if (graph.nodeCount() == 0) return true;
    int roots = 0;
    int[] inDeg = inDegrees(graph);
    for (int v = 0; v < graph.nodeCount(); v++) {
      if (inDeg[v] == 0) roots++;
    }
    return roots == 1;
  }

  /** A path: out-tree where every node has out-degree ≤ 1. */
  private static boolean isPathReference(Graph graph) {
    if (!isOutTreeReference(graph)) return false;
    for (int v = 0; v < graph.nodeCount(); v++) {
      int outDeg = graph.outEdgeEnd(v) - graph.outEdgeStart(v);
      if (outDeg > 1) return false;
    }
    return true;
  }

  private static int[] inDegrees(Graph graph) {
    int n = graph.nodeCount();
    int[] inDeg = new int[n];
    for (int u = 0; u < n; u++) {
      int start = graph.outEdgeStart(u);
      int end = graph.outEdgeEnd(u);
      for (int ei = start; ei < end; ei++) inDeg[graph.edgeTarget(ei)]++;
    }
    return inDeg;
  }
}
