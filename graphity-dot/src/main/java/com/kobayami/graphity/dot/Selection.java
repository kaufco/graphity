package com.kobayami.graphity.dot;

import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.NodePartition;
import java.io.IOException;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import org.jspecify.annotations.Nullable;

/**
 * The graph, the node filter, the undirected reading and the clusters of a {@link DotWriter}, and
 * the order in which edges are written. {@code graphity-mermaid} keeps its own copy for
 * {@code MermaidWriter}: a module has no access to the package-private classes of another.
 */
final class Selection {

  final Graph graph;
  private final int[] outAdjacencyStart;
  private final int[] outAdjacency;
  @Nullable IntPredicate nodeFilter;
  boolean undirected;
  @Nullable Clusters clusters;

  Selection(Graph graph) {
    this.graph = graph;
    this.outAdjacencyStart = Csr.outAdjacencyStart(graph);
    this.outAdjacency = Csr.outAdjacency(graph);
  }

  /**
   * Sets the groups drawn as clusters.
   *
   * @throws IllegalArgumentException if the partition covers a different number of nodes
   */
  void clusters(NodePartition partition, IntFunction<@Nullable String> label) {
    if (partition.nodeCount() != graph.nodeCount()) {
      throw new IllegalArgumentException("the partition covers " + partition.nodeCount()
          + " nodes, the graph has " + graph.nodeCount());
    }
    clusters = new Clusters(partition, label);
  }

  boolean isIncluded(int node) {
    return nodeFilter == null || nodeFilter.test(node);
  }

  /** The cluster of a node, {@code -1} for none. */
  int clusterOf(int node) {
    return clusters == null ? -1 : clusters.partition().get(node);
  }

  /**
   * Whether the edge {@code source -> target} of an included source is written: its target
   * included, and in the undirected reading only one edge per reciprocal pair, the one from the
   * lower node ID.
   */
  private boolean isWritten(int source, int target) {
    if (!isIncluded(target)) return false;
    return !undirected || source <= target || !graph.hasOutNode(target, source);
  }

  /** Calls {@code action} for every written edge, in edge index order. */
  void forEachWrittenEdge(EdgeAction action) throws IOException {
    int n = graph.nodeCount();
    for (int source = 0; source < n; source++) {
      if (!isIncluded(source)) continue;
      for (int e = outAdjacencyStart[source], end = outAdjacencyStart[source + 1]; e < end; e++) {
        int target = outAdjacency[e];
        if (isWritten(source, target)) action.accept(source, target, e);
      }
    }
  }

  @FunctionalInterface
  interface EdgeAction {
    void accept(int source, int target, int edgeIndex) throws IOException;
  }

  /** The groups drawn as clusters, with the label of each; set together, so held together. */
  record Clusters(NodePartition partition, IntFunction<@Nullable String> label) {}

  /** Whether {@code name} is a plain identifier: a letter or underscore, then also digits. */
  static boolean isIdentifier(String name) {
    if (name.isEmpty()) return false;
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
      boolean digit = c >= '0' && c <= '9';
      if (!(letter || (i > 0 && digit))) return false;
    }
    return true;
  }
}
