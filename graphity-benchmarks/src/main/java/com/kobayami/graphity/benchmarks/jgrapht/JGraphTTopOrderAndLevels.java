package com.kobayami.graphity.benchmarks.jgrapht;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Port of Graphity's Kahn implementation ({@code com.kobayami.graphity.TopOrderAndLevels})
 * onto JGraphT's data model, computing the topological order <em>and</em> the level of every
 * node — {@code level(v) = max(level(predecessors)) + 1}.
 *
 * <p>JGraphT's own {@link org.jgrapht.traverse.TopologicalOrderIterator} is not used here
 * because it yields an order and nothing else; charging Graphity for the levels while the peer
 * skips them would measure the ports, not the libraries.
 *
 * <p>Node IDs are dense, so the algorithm state is the same primitive arrays Graphity uses.
 * Two things differ, and they are what the benchmark is about: in-degrees come from
 * {@link Graph#inDegreeOf} in O(1) per node, because a JGraphT graph keeps incoming edges
 * alongside outgoing ones, and adjacency is read through {@link Graph#outgoingEdgesOf} plus
 * {@link Graph#getEdgeTarget} instead of a CSR array.
 */
public final class JGraphTTopOrderAndLevels {

  private final int[] order;
  private final int[] levels;
  private final int levelCount;

  private JGraphTTopOrderAndLevels(int[] order, int[] levels, int levelCount) {
    this.order = order;
    this.levels = levels;
    this.levelCount = levelCount;
  }

  public static JGraphTTopOrderAndLevels of(Graph<Integer, DefaultEdge> graph) {
    int n = graph.vertexSet().size();
    if (n == 0) {
      return new JGraphTTopOrderAndLevels(new int[0], new int[0], 0);
    }

    int[] inDegree = new int[n];
    for (int node = 0; node < n; node++) {
      inDegree[node] = graph.inDegreeOf(node);
    }

    int[] queue = new int[n];
    int queueHead = 0;
    int queueTail = 0;
    for (int node = 0; node < n; node++) {
      if (inDegree[node] == 0) {
        queue[queueTail++] = node;
      }
    }

    int[] order = new int[n];
    int[] levels = new int[n];
    int orderPos = 0;
    int level = 0;

    while (queueHead < queueTail) {
      int levelEnd = queueTail;
      while (queueHead < levelEnd) {
        int node = queue[queueHead++];
        order[orderPos++] = node;
        levels[node] = level;
        for (DefaultEdge edge : graph.outgoingEdgesOf(node)) {
          int target = graph.getEdgeTarget(edge);
          if (--inDegree[target] == 0) {
            queue[queueTail++] = target;
          }
        }
      }
      level++;
    }

    if (orderPos != n) {
      throw new IllegalArgumentException("Graph contains a cycle");
    }
    return new JGraphTTopOrderAndLevels(order, levels, level);
  }

  public int[] order() {
    return order;
  }

  public int[] levels() {
    return levels;
  }

  public int levelCount() {
    return levelCount;
  }
}
