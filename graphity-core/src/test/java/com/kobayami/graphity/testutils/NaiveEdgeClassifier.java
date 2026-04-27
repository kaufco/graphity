package com.kobayami.graphity.testutils;

import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.Graph;

/**
 * Independent, textbook-clean DFS-based edge classifier, used as an oracle for
 * cross-validation against {@link com.kobayami.graphity.EdgeTypes} and
 * {@link com.kobayami.graphity.BackEdges}.
 * <p>
 * Uses plain primitive arrays ({@code boolean[]}, {@code int[]}) and direct CSR
 * access — deliberately not built on {@link com.kobayami.graphity.DfsTraverser}
 * so that integration bugs (visited-set handling, BitSet semantics, visitor dispatch)
 * are not masked.
 * <p>
 * Traversal order matches Graphity's convention: starts from node 0, continues with
 * next-unvisited in ascending order, within each node visits out-edges in CSR
 * (ascending neighbor) order. Together with the DFS classification rules this makes
 * the produced edge types deterministic and identical to what
 * {@link com.kobayami.graphity.EdgeTypes} must produce for the same input.
 */
public final class NaiveEdgeClassifier {

  private NaiveEdgeClassifier() {}

  /**
   * Returns an array {@code types} where {@code types[ei]} is the {@link EdgeType}
   * of edge at CSR position {@code ei} in {@link Graph#outAdjacency}.
   */
  public static EdgeType[] classify(Graph graph) {
    int n = graph.nodeCount();
    int m = graph.edgeCount();
    var types = new EdgeType[m];
    if (n == 0) return types;

    boolean[] visited = new boolean[n];
    boolean[] onStack = new boolean[n];
    int[] disc = new int[n];
    int[] time = {0};

    for (int start = 0; start < n; start++) {
      if (!visited[start]) {
        dfs(start, graph, visited, onStack, disc, time, types);
      }
    }
    return types;
  }

  private static void dfs(int u, Graph graph, boolean[] visited, boolean[] onStack,
                          int[] disc, int[] time, EdgeType[] types) {
    visited[u] = true;
    onStack[u] = true;
    disc[u] = ++time[0];

    int start = graph.outEdgeStart(u);
    int end = graph.outEdgeEnd(u);
    for (int ei = start; ei < end; ei++) {
      int v = graph.edgeTarget(ei);
      if (!visited[v]) {
        types[ei] = EdgeType.TREE;
        dfs(v, graph, visited, onStack, disc, time, types);
      } else if (onStack[v]) {
        types[ei] = EdgeType.BACK;
      } else if (disc[u] < disc[v]) {
        types[ei] = EdgeType.FORWARD;
      } else {
        types[ei] = EdgeType.CROSS;
      }
    }

    onStack[u] = false;
  }
}
