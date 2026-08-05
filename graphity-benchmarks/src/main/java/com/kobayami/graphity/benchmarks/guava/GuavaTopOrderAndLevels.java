package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.Graph;

/**
 * Port of Graphity's Kahn implementation ({@code com.kobayami.graphity.TopOrderAndLevels})
 * onto Guava's data model, computing the topological order <em>and</em> the level of every
 * node — {@code level(v) = max(level(predecessors)) + 1}.
 *
 * <p>Node IDs are dense, so the algorithm state is the same primitive arrays Graphity uses.
 * Two things differ, and they are what the benchmark is about: in-degrees come from
 * {@link Graph#inDegree} in O(1) per node, because a Guava graph keeps predecessors alongside
 * successors, and adjacency is read through {@link Graph#successors} instead of a CSR array.
 */
public final class GuavaTopOrderAndLevels {

  private final int[] order;
  private final int[] levels;
  private final int levelCount;

  private GuavaTopOrderAndLevels(int[] order, int[] levels, int levelCount) {
    this.order = order;
    this.levels = levels;
    this.levelCount = levelCount;
  }

  public static GuavaTopOrderAndLevels of(Graph<Integer> graph) {
    int n = graph.nodes().size();
    if (n == 0) {
      return new GuavaTopOrderAndLevels(new int[0], new int[0], 0);
    }

    int[] inDegree = new int[n];
    for (int node = 0; node < n; node++) {
      inDegree[node] = graph.inDegree(node);
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
        for (int target : graph.successors(node)) {
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
    return new GuavaTopOrderAndLevels(order, levels, level);
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
