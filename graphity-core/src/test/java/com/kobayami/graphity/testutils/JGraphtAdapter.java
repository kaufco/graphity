package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Converts a Graphity {@link Graph} into an equivalent JGraphT
 * {@link DefaultDirectedGraph} for cross-validation tests.
 * <p>
 * Vertices are Integer objects equal to Graphity node IDs, so results from JGraphT
 * algorithms can be compared directly against Graphity results.
 */
public final class JGraphtAdapter {

  private JGraphtAdapter() {}

  /**
   * Builds a JGraphT directed graph with the same nodes and edges as the given
   * Graphity graph. Self-loops are preserved. Parallel edges do not exist in Graphity
   * (deduplicated on build) and are therefore not produced here either.
   */
  public static DefaultDirectedGraph<Integer, DefaultEdge> toJgrapht(Graph graph) {
    var g = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
    int n = graph.nodeCount();
    for (int i = 0; i < n; i++) g.addVertex(i);
    for (int u = 0; u < n; u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        g.addEdge(u, v);
      }
    }
    return g;
  }
}
