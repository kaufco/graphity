package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;

/**
 * Deterministic mini-graph generators for unit tests.
 * <p>
 * These are not intended for fuzzing or benchmarking — they produce small, named,
 * hand-checkable topologies. Random generators for the integration / fuzzing layer
 * will live elsewhere.
 */
public final class TestGraphs {

  private TestGraphs() {}

  /** Empty graph with no nodes and no edges. */
  public static Graph empty() {
    return new GraphBuilder().build();
  }

  /** Graph with {@code n} isolated nodes and no edges. */
  public static Graph isolated(int n) {
    var b = new GraphBuilder();
    b.addNodes(n);
    return b.build();
  }

  /**
   * Directed path: {@code 0 -> 1 -> 2 -> ... -> n-1}.
   * Acyclic. {@code n == 0} returns the empty graph; {@code n == 1} returns a single isolated node.
   */
  public static Graph path(int n) {
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i + 1 < n; i++) {
      b.addEdge(i, i + 1);
    }
    return b.build();
  }

  /**
   * Directed cycle: {@code 0 -> 1 -> ... -> n-1 -> 0}.
   * For {@code n == 1}: single self-loop.
   */
  public static Graph cycle(int n) {
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i < n; i++) {
      b.addEdge(i, (i + 1) % n);
    }
    return b.build();
  }

  /**
   * Complete directed graph on {@code n} nodes: every ordered pair {@code (u, v)} with
   * {@code u != v} has an edge. No self-loops. {@code n * (n-1)} edges total.
   */
  public static Graph clique(int n) {
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int u = 0; u < n; u++) {
      for (int v = 0; v < n; v++) {
        if (u != v) b.addEdge(u, v);
      }
    }
    return b.build();
  }

  /**
   * Out-star: node 0 is the center, edges {@code 0 -> 1, 0 -> 2, ..., 0 -> n-1}.
   * Requires {@code n >= 1}. Acyclic tree.
   */
  public static Graph outStar(int n) {
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 1; i < n; i++) {
      b.addEdge(0, i);
    }
    return b.build();
  }

  /**
   * In-star: node 0 is the center, edges {@code 1 -> 0, 2 -> 0, ..., n-1 -> 0}.
   * Requires {@code n >= 1}. Acyclic.
   */
  public static Graph inStar(int n) {
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 1; i < n; i++) {
      b.addEdge(i, 0);
    }
    return b.build();
  }

  /**
   * Builds a graph with {@code n} nodes from a flat list of edge endpoints:
   * {@code of(3, 0, 1, 1, 2)} yields {@code 0 -> 1} and {@code 1 -> 2}.
   *
   * @throws IllegalArgumentException if the endpoint list has odd length
   */
  public static Graph of(int n, int... edges) {
    if ((edges.length & 1) != 0) {
      throw new IllegalArgumentException("edges must come in (source, target) pairs");
    }
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i < edges.length; i += 2) {
      b.addEdge(edges[i], edges[i + 1]);
    }
    return b.build();
  }
}
