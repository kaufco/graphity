package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.JGraphtAdapter;
import com.kobayami.graphity.testutils.RandomGraphs;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.cycle.CycleDetector;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates shape predicates in {@link Graphs} against independent references.
 * <p>
 * The directed predicates: {@code isDag} against JGraphT's {@link CycleDetector};
 * {@code isForest}, {@code isTree}, {@code isPath} against their structural definitions computed
 * from scratch via plain arrays.
 * <p>
 * The undirected predicates against JGraphT's {@link GraphTests} and
 * {@link ConnectivityInspector}, applied to {@link JGraphtAdapter#toUndirectedJgrapht(Graph)}.
 * That adapter builds a {@code SimpleGraph} and thereby enforces the undirected reading
 * structurally - reciprocal pairs collapse, self-loops are rejected - and JGraphT decides
 * bipartiteness by coloring and connectivity over vertex sets, so neither oracle shares an
 * approach with the implementation under test.
 * <p>
 * One limit follows from that adapter: since it drops self-loops, JGraphT cannot express the one
 * rule where {@code isBipartite} does not ignore them. Graphs holding a self-loop are therefore
 * excluded from that comparison and covered by {@code GraphsTest} instead.
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
  void isTreeAgreesWithDefinition() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isTree(g);
      boolean expected = isTreeReference(g);

      assertThat(ours)
          .as("isTree mismatch for %s", labeled.describe())
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
        assertThat(Graphs.isTree(g))
            .as("isPath ⇒ isTree violated for %s", labeled.describe()).isTrue();
      }
      if (Graphs.isTree(g)) {
        assertThat(Graphs.isForest(g))
            .as("isTree ⇒ isForest violated for %s", labeled.describe()).isTrue();
      }
      if (Graphs.isForest(g)) {
        assertThat(Graphs.isDag(g))
            .as("isForest ⇒ isDag violated for %s", labeled.describe()).isTrue();
      }
    }
  }

  // ---- Undirected predicates ----

  @Test
  void isBipartiteAgreesWithJgrapht() {
    int bipartite = 0;
    int notBipartite = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyUndirectedShape(BASE_SEED + i);
      var g = labeled.graph();
      if (containsSelfLoop(g)) {
        continue; // the oracle cannot represent self-loops, see class comment
      }
      boolean ours = Graphs.isBipartite(g);
      boolean theirs = GraphTests.isBipartite(JGraphtAdapter.toUndirectedJgrapht(g));

      assertThat(ours)
          .as("isBipartite mismatch vs JGraphT for %s", labeled.describe())
          .isEqualTo(theirs);

      if (ours) bipartite++;
      else notBipartite++;
    }
    // Without both outcomes the comparison above would pass on agreement about nothing.
    assertThat(bipartite).as("no bipartite graph was generated").isPositive();
    assertThat(notBipartite).as("no non-bipartite graph was generated").isPositive();
  }

  @Test
  void isUndirectedTreeAgreesWithJgrapht() {
    int trees = 0;
    int nonTrees = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyUndirectedShape(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isUndirectedTree(g);
      boolean theirs = GraphTests.isTree(JGraphtAdapter.toUndirectedJgrapht(g));

      assertThat(ours)
          .as("isUndirectedTree mismatch vs JGraphT for %s", labeled.describe())
          .isEqualTo(theirs);

      if (ours) trees++;
      else nonTrees++;
    }
    assertThat(trees).as("no undirected tree was generated").isPositive();
    assertThat(nonTrees).as("no non-tree was generated").isPositive();
  }

  @Test
  void isUndirectedForestAgreesWithJgrapht() {
    int forests = 0;
    int nonForests = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyUndirectedShape(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isUndirectedForest(g);
      boolean theirs = GraphTests.isForest(JGraphtAdapter.toUndirectedJgrapht(g));

      assertThat(ours)
          .as("isUndirectedForest mismatch vs JGraphT for %s", labeled.describe())
          .isEqualTo(theirs);

      if (ours) forests++;
      else nonForests++;
    }
    assertThat(forests).as("no undirected forest was generated").isPositive();
    assertThat(nonForests).as("no graph with an undirected cycle was generated").isPositive();
  }

  @Test
  void isWeaklyConnectedAgreesWithJgrapht() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyUndirectedShape(BASE_SEED + i);
      var g = labeled.graph();
      boolean ours = Graphs.isWeaklyConnected(g);
      boolean theirs =
          new ConnectivityInspector<>(JGraphtAdapter.toUndirectedJgrapht(g)).isConnected();

      assertThat(ours)
          .as("isWeaklyConnected mismatch vs JGraphT for %s", labeled.describe())
          .isEqualTo(theirs);
    }
  }

  @Test
  void isWeaklyConnectedAgreesWithWeaklyConnectedComponents() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();

      assertThat(Graphs.isWeaklyConnected(g))
          .as("isWeaklyConnected disagrees with wccsOf for %s", labeled.describe())
          .isEqualTo(Components.wccsOf(g).groupCount() <= 1);
    }
  }

  @Test
  void undirectedShapeHierarchyHolds() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyUndirectedShape(BASE_SEED + i);
      var g = labeled.graph();
      if (Graphs.isTree(g)) {
        assertThat(Graphs.isUndirectedTree(g))
            .as("isTree ⇒ isUndirectedTree violated for %s", labeled.describe()).isTrue();
      }
      if (Graphs.isForest(g)) {
        assertThat(Graphs.isUndirectedForest(g))
            .as("isForest ⇒ isUndirectedForest violated for %s", labeled.describe()).isTrue();
      }
      if (Graphs.isUndirectedTree(g)) {
        assertThat(Graphs.isUndirectedForest(g))
            .as("isUndirectedTree ⇒ isUndirectedForest violated for %s", labeled.describe())
            .isTrue();
        assertThat(Graphs.isWeaklyConnected(g))
            .as("isUndirectedTree ⇒ isWeaklyConnected violated for %s", labeled.describe())
            .isTrue();
        if (!containsSelfLoop(g)) {
          // Holds only without self-loops: the tree ignores them, isBipartite does not. That
          // restriction is the single observable consequence of the split rule.
          assertThat(Graphs.isBipartite(g))
              .as("isUndirectedTree ⇒ isBipartite violated for %s", labeled.describe()).isTrue();
        }
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

  /** A tree: forest with exactly one root (in-degree == 0), or an empty graph. */
  private static boolean isTreeReference(Graph graph) {
    if (!isForestReference(graph)) return false;
    if (graph.nodeCount() == 0) return true;
    int roots = 0;
    int[] inDeg = inDegrees(graph);
    for (int v = 0; v < graph.nodeCount(); v++) {
      if (inDeg[v] == 0) roots++;
    }
    return roots == 1;
  }

  /** A path: tree where every node has out-degree ≤ 1. */
  private static boolean isPathReference(Graph graph) {
    if (!isTreeReference(graph)) return false;
    for (int v = 0; v < graph.nodeCount(); v++) {
      int outDeg = graph.outEdgeEnd(v) - graph.outEdgeStart(v);
      if (outDeg > 1) return false;
    }
    return true;
  }

  private static boolean containsSelfLoop(Graph graph) {
    for (int v = 0; v < graph.nodeCount(); v++) {
      if (graph.containsSelfLoop(v)) return true;
    }
    return false;
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
