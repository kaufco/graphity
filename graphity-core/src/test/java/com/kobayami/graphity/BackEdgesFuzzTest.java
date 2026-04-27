package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.NaiveEdgeClassifier;
import com.kobayami.graphity.testutils.RandomGraphs;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link BackEdges#of(Graph)} against the independent
 * {@link NaiveEdgeClassifier} oracle and against the structural invariant
 * "removing all back-edges yields a DAG".
 */
class BackEdgesFuzzTest {

  private static final int ITERATIONS = 100;
  private static final long BASE_SEED = 0xBEEF0BEEFL;

  @Test
  void backEdgesMatchNaiveClassifierOnRandomGraphs() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();

      var expected = naiveBackEdgePairs(g);
      var actual = graphityBackEdgePairs(BackEdges.of(g));

      assertThat(actual)
          .as("back-edges mismatch vs naive oracle for %s", labeled.describe())
          .isEqualTo(expected);
    }
  }

  @Test
  void removingBackEdgesYieldsDagOnRandomGraphs() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      var backEdges = BackEdges.of(g);
      var reduced = removeBackEdges(g, backEdges);

      assertThat(Graphs.isDag(reduced))
          .as("removing back-edges must yield a DAG for %s", labeled.describe())
          .isTrue();
    }
  }

  @Test
  void backEdgeCountZeroIffDag() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      boolean isDag = Graphs.isDag(g);
      boolean noBackEdges = BackEdges.of(g).count() == 0;

      assertThat(noBackEdges)
          .as("|BackEdges| == 0 must be equivalent to isDag for %s", labeled.describe())
          .isEqualTo(isDag);
    }
  }

  private static Set<Long> naiveBackEdgePairs(Graph graph) {
    var types = NaiveEdgeClassifier.classify(graph);
    var pairs = new HashSet<Long>();
    for (int u = 0; u < graph.nodeCount(); u++) {
      int start = graph.outEdgeStart(u);
      int end = graph.outEdgeEnd(u);
      for (int ei = start; ei < end; ei++) {
        if (types[ei] == EdgeType.BACK) pairs.add(pair(u, graph.edgeTarget(ei)));
      }
    }
    return pairs;
  }

  private static Set<Long> graphityBackEdgePairs(Edges edges) {
    var pairs = new HashSet<Long>();
    for (int i = 0; i < edges.count(); i++) {
      pairs.add(pair(edges.sourceNodes().getInt(i), edges.targetNodes().getInt(i)));
    }
    return pairs;
  }

  private static long pair(int src, int tgt) {
    return (((long) src) << 32) | (tgt & 0xFFFFFFFFL);
  }

  private static Graph removeBackEdges(Graph graph, Edges backEdges) {
    var backSet = new HashSet<Long>();
    for (int i = 0; i < backEdges.count(); i++) {
      backSet.add(pair(backEdges.sourceNodes().getInt(i), backEdges.targetNodes().getInt(i)));
    }

    var builder = new GraphBuilder();
    builder.addNodes(graph.nodeCount());
    for (int u = 0; u < graph.nodeCount(); u++) {
      int start = graph.outEdgeStart(u);
      int end = graph.outEdgeEnd(u);
      for (int ei = start; ei < end; ei++) {
        int v = graph.edgeTarget(ei);
        if (!backSet.contains(pair(u, v))) builder.addEdge(u, v);
      }
    }
    return builder.build();
  }
}
