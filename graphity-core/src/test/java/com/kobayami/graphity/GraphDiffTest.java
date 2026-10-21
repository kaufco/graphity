package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GraphDiffTest {

  private static Set<Long> edgeSet(Graph graph) {
    var set = new HashSet<Long>();
    graph.forEachEdge((source, target, edgeIndex) -> set.add((long) source << 32 | target));
    return set;
  }

  private static List<Long> keys(Edges edges) {
    var list = new ArrayList<Long>();
    for (int p = 0; p < edges.count(); p++) {
      list.add((long) edges.sourceNodes().getInt(p) << 32 | edges.targetNodes().getInt(p));
    }
    return list;
  }

  /** {@code graph} with some edges dropped and some added, over the same nodes. */
  private static Graph edited(Graph graph, long seed) {
    var random = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(graph.nodeCount());
    graph.forEachEdge((source, target, edgeIndex) -> {
      if (random.nextInt(5) != 0) b.addEdge(source, target);
    });
    int n = graph.nodeCount();
    for (int i = 0; i < 1 + random.nextInt(10); i++) {
      b.addEdge(random.nextInt(n), random.nextInt(n));
    }
    return b.build();
  }

  @Test
  void diffMatchesTheSetDifferenceAndNamesEachEdgeInItsGraph() {
    for (long seed = 0; seed < 200; seed++) {
      Graph before = RandomGraphs.anyGraph(seed).graph();
      Graph after = edited(before, seed);
      var diff = Graphs.diff(before, after);

      var expectedAdded = new ArrayList<>(edgeSet(after));
      expectedAdded.removeAll(edgeSet(before));
      expectedAdded.sort(null);
      var expectedRemoved = new ArrayList<>(edgeSet(before));
      expectedRemoved.removeAll(edgeSet(after));
      expectedRemoved.sort(null);

      assertThat(keys(diff.addedEdges())).as("added, seed %d", seed).isEqualTo(expectedAdded);
      assertThat(keys(diff.removedEdges())).as("removed, seed %d", seed).isEqualTo(expectedRemoved);
      assertIndices(after, diff.addedEdges());
      assertIndices(before, diff.removedEdges());
      assertThat(diff.isEmpty()).isEqualTo(before.equals(after));
    }
  }

  private static void assertIndices(Graph graph, Edges edges) {
    for (int p = 0; p < edges.count(); p++) {
      assertThat(edges.edgeIndices().getInt(p)).isEqualTo(
          graph.edgeIndexOf(edges.sourceNodes().getInt(p), edges.targetNodes().getInt(p)));
    }
  }

  @Test
  void equalGraphsHaveAnEmptyDiff() {
    Graph graph = RandomGraphs.gnp(40, 0.1, 3);
    var diff = Graphs.diff(graph, BiAdjacentGraph.of(graph));
    assertThat(diff.isEmpty()).isTrue();
    assertThat(diff.addedEdges().count()).isZero();
    assertThat(diff.removedEdges().count()).isZero();
    assertThat(diff.toString()).isEqualTo("GraphDiff[added=0, removed=0]");
  }

  @Test
  void diffOfASmallChange() {
    Graph before = TestGraphs.of(3, 0, 1, 1, 2);
    Graph after = TestGraphs.of(3, 0, 1, 0, 2);
    var diff = Graphs.diff(before, after);
    assertThat(diff.addedEdges().sourceNodes().toIntArray()).containsExactly(0);
    assertThat(diff.addedEdges().targetNodes().toIntArray()).containsExactly(2);
    assertThat(diff.addedEdges().edgeIndices().toIntArray()).containsExactly(1);
    assertThat(diff.removedEdges().sourceNodes().toIntArray()).containsExactly(1);
    assertThat(diff.removedEdges().targetNodes().toIntArray()).containsExactly(2);
    assertThat(diff.removedEdges().edgeIndices().toIntArray()).containsExactly(1);
    assertThat(diff.toString()).isEqualTo("GraphDiff[added=1, removed=1]");
  }

  @Test
  void diffAgainstAGraphWithoutEdgesListsEveryEdge() {
    Graph graph = RandomGraphs.gnp(30, 0.1, 9);
    Graph empty = TestGraphs.isolated(30);
    int[] all = IntStream.range(0, graph.edgeCount()).toArray();
    assertThat(Graphs.diff(empty, graph).addedEdges().edgeIndices().toIntArray())
        .containsExactly(all);
    assertThat(Graphs.diff(empty, graph).removedEdges().isEmpty()).isTrue();
    assertThat(Graphs.diff(graph, empty).removedEdges().edgeIndices().toIntArray())
        .containsExactly(all);
    assertThat(Graphs.diff(TestGraphs.empty(), TestGraphs.empty()).isEmpty()).isTrue();
  }

  @Test
  void aTransposedViewEqualsTheBuiltTranspose() {
    Graph graph = RandomGraphs.gnp(25, 0.15, 4);
    var view = BiAdjacentGraph.of(graph).transposed();
    var b = new GraphBuilder();
    b.addNodes(graph.nodeCount());
    graph.forEachEdge((source, target, edgeIndex) -> b.addEdge(target, source));
    Graph built = b.build();
    assertThat(Graphs.diff(view, built).isEmpty()).isTrue();
    assertThat(Graphs.topologyHash(view)).isEqualTo(Graphs.topologyHash(built));
  }

  @Test
  void diffRejectsDifferentNodeCounts() {
    assertThatThrownBy(() -> Graphs.diff(TestGraphs.path(3), TestGraphs.path(4)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("same nodes");
  }

  @Test
  void topologyHashIsConsistentWithEquals() {
    for (long seed = 0; seed < 50; seed++) {
      Graph graph = RandomGraphs.anyGraph(seed).graph();
      Graph copy = Graph.of(Csr.outAdjacencyStart(graph), Csr.outAdjacency(graph));
      assertThat(Graphs.topologyHash(copy)).isEqualTo(Graphs.topologyHash(graph));
      assertThat(Graphs.topologyHash(BiAdjacentGraph.of(graph)))
          .isEqualTo(Graphs.topologyHash(graph));
    }
  }

  @Test
  void topologyHashSeparatesDistinctGraphs() {
    var hashes = new HashSet<Long>();
    var graphs = new HashSet<Graph>();
    for (long seed = 0; seed < 2000; seed++) {
      Graph graph = RandomGraphs.gnp(1 + (int) (seed % 12), 0.3, seed);
      if (graphs.add(graph)) {
        assertThat(hashes.add(Graphs.topologyHash(graph))).as("collision at seed %d", seed).isTrue();
      }
    }
    assertThat(graphs.size()).isGreaterThan(1000);
  }

  @Test
  void topologyHashTellsApartGraphsThatDifferOnlyInWhereAnEdgeStarts() {
    // Both have targets [0, 1] and two nodes and edges; degrees 2/0 against 1/1.
    assertThat(Graphs.topologyHash(TestGraphs.of(2, 0, 0, 0, 1)))
        .isNotEqualTo(Graphs.topologyHash(TestGraphs.of(2, 0, 0, 1, 1)));
    assertThat(Graphs.topologyHash(TestGraphs.isolated(3)))
        .isNotEqualTo(Graphs.topologyHash(TestGraphs.isolated(4)));
  }
}
