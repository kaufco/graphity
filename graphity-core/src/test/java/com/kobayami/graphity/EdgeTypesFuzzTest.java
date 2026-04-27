package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.NaiveEdgeClassifier;
import com.kobayami.graphity.testutils.RandomGraphs;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link EdgeTypes#of(Graph)} against the independent
 * {@link NaiveEdgeClassifier} oracle on random graphs.
 */
class EdgeTypesFuzzTest {

  private static final int ITERATIONS = 100;
  private static final long BASE_SEED = 0xFEEDFACEL;

  @Test
  void edgeTypesMatchNaiveClassifier() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();

      var theirs = EdgeTypes.of(g);
      var expected = NaiveEdgeClassifier.classify(g);

      for (int ei = 0; ei < g.edgeCount(); ei++) {
        assertThat(theirs.typeOf(ei))
            .as("edge %d (source=%d, target=%d) classification mismatch for %s",
                ei, sourceOf(g, ei), g.edgeTarget(ei), labeled.describe())
            .isEqualTo(expected[ei]);
      }
    }
  }

  @Test
  void typeCountsSumToEdgeCount() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      var et = EdgeTypes.of(g);

      int total = 0;
      for (EdgeType t : EdgeType.values()) total += et.edgesOfType(t).count();
      assertThat(total)
          .as("sum of type counts must equal edge count for %s", labeled.describe())
          .isEqualTo(g.edgeCount());
    }
  }

  @Test
  void edgesOfTypeAgreeWithTypeOf() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      var et = EdgeTypes.of(g);

      Map<EdgeType, Set<Long>> byType = new EnumMap<>(EdgeType.class);
      for (EdgeType t : EdgeType.values()) byType.put(t, new HashSet<>());
      for (int u = 0; u < g.nodeCount(); u++) {
        int start = g.outEdgeStart(u);
        int end = g.outEdgeEnd(u);
        for (int ei = start; ei < end; ei++) {
          byType.get(et.typeOf(ei)).add(pair(u, g.edgeTarget(ei)));
        }
      }

      for (EdgeType t : EdgeType.values()) {
        var edges = et.edgesOfType(t);
        var collected = new HashSet<Long>();
        for (int k = 0; k < edges.count(); k++) {
          collected.add(pair(edges.sourceNodes().getInt(k), edges.targetNodes().getInt(k)));
        }
        assertThat(collected)
            .as("edgesOfType(%s) mismatches typeOf scan for %s", t, labeled.describe())
            .isEqualTo(byType.get(t));
      }
    }
  }

  @Test
  void backEdgesSubsetMatchesBackEdgesClass() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      var fromEdgeTypes = EdgeTypes.of(g).edgesOfType(EdgeType.BACK);
      var fromBackEdges = BackEdges.of(g);

      assertThat(toPairSet(fromEdgeTypes))
          .as("EdgeTypes.BACK must match BackEdges.of for %s", labeled.describe())
          .isEqualTo(toPairSet(fromBackEdges));
    }
  }

  private static Set<Long> toPairSet(Edges edges) {
    var set = new HashSet<Long>();
    for (int i = 0; i < edges.count(); i++) {
      set.add(pair(edges.sourceNodes().getInt(i), edges.targetNodes().getInt(i)));
    }
    return set;
  }

  private static long pair(int src, int tgt) {
    return (((long) src) << 32) | (tgt & 0xFFFFFFFFL);
  }

  /** Recovers the source node of edge index {@code ei} via CSR scan (for diagnostics). */
  private static int sourceOf(Graph graph, int ei) {
    for (int u = 0; u < graph.nodeCount(); u++) {
      if (graph.outEdgeStart(u) <= ei && ei < graph.outEdgeEnd(u)) return u;
    }
    return -1;
  }
}
