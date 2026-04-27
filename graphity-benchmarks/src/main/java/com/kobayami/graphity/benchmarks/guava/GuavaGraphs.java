package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.kobayami.graphity.SortedIntView;

/**
 * Conversion helpers between a Graphity {@link com.kobayami.graphity.Graph} and a
 * structurally equivalent Guava {@link Graph}.
 *
 * <p>All peer-library fixtures use a Graphity graph (produced by
 * {@link com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs}) as the
 * <em>source of truth</em> and convert from it, guaranteeing byte-identical
 * graph structure across libraries.
 *
 * <p>The Guava graphs are configured to match Graphity's semantics:
 * directed, self-loops allowed, no parallel edges (Guava's simple {@link Graph}
 * never has parallel edges by construction).
 */
public final class GuavaGraphs {

  private GuavaGraphs() {}

  /** Creates an empty directed Guava {@link MutableGraph} with Graphity-compatible semantics. */
  public static MutableGraph<Integer> emptyDirected() {
    return GraphBuilder.directed().allowsSelfLoops(true).build();
  }

  /**
   * Converts a Graphity graph into a structurally equivalent Guava graph.
   * Node identity is the boxed integer node id.
   */
  public static Graph<Integer> fromGraphity(com.kobayami.graphity.Graph g) {
    MutableGraph<Integer> out = emptyDirected();
    int n = g.nodeCount();
    for (int v = 0; v < n; v++) {
      out.addNode(v);
    }
    for (int v = 0; v < n; v++) {
      SortedIntView outs = g.outNodes(v);
      int deg = outs.size();
      for (int i = 0; i < deg; i++) {
        out.putEdge(v, outs.getInt(i));
      }
    }
    return out;
  }
}
