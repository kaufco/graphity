package com.kobayami.graphity.benchmarks.jgrapht;

import com.kobayami.graphity.SortedIntView;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

/**
 * Conversion helpers between a Graphity {@link com.kobayami.graphity.Graph} and a
 * structurally equivalent JGraphT {@link Graph}.
 *
 * <p>All peer-library fixtures use a Graphity graph (produced by
 * {@link com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs}) as the
 * <em>source of truth</em> and convert from it. This guarantees that both libraries
 * see byte-identical graph structure (same nodes, same edges, same edge order).
 *
 * <p>The JGraphT graphs are configured to match Graphity's semantics:
 * directed, self-loops allowed (Graphity permits them), no parallel edges
 * (Graphity deduplicates during {@code build()}).
 */
public final class JGraphTGraphs {

  private JGraphTGraphs() {}

  /** Creates an empty directed JGraphT graph with the same semantics as a Graphity graph. */
  public static Graph<Integer, DefaultEdge> emptyDirected() {
    return GraphTypeBuilder.<Integer, DefaultEdge>directed()
        .allowingSelfLoops(true)
        .allowingMultipleEdges(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();
  }

  /**
   * Converts a Graphity graph into a structurally equivalent JGraphT graph.
   * Vertex identity is the integer node id.
   */
  public static Graph<Integer, DefaultEdge> fromGraphity(com.kobayami.graphity.Graph g) {
    Graph<Integer, DefaultEdge> out = emptyDirected();
    int n = g.nodeCount();
    for (int v = 0; v < n; v++) {
      out.addVertex(v);
    }
    for (int v = 0; v < n; v++) {
      SortedIntView outs = g.outNodes(v);
      int deg = outs.size();
      for (int i = 0; i < deg; i++) {
        out.addEdge(v, outs.getInt(i));
      }
    }
    return out;
  }
}
