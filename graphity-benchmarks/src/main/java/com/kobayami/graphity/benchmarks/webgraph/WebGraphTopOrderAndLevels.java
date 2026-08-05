package com.kobayami.graphity.benchmarks.webgraph;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;

/**
 * Port of Graphity's Kahn implementation ({@code com.kobayami.graphity.TopOrderAndLevels})
 * onto WebGraph's data model, computing the topological order <em>and</em> the level of every
 * node — {@code level(v) = max(level(predecessors)) + 1}.
 *
 * <p>WebGraph node IDs are dense, so the algorithm state is the same primitive arrays Graphity
 * uses and the comparison comes down to how the two libraries reach the edges.
 *
 * <p>The in-degree pass is where a BVGraph pays for storing out-adjacency only: there are no
 * incoming edges to count, so every arc in the graph is decoded once to accumulate in-degrees,
 * an O(edgeCount) prologue before Kahn's loop can start. This is also the cheapest way to get
 * there — building the transpose would cost more than the scan it saves — so the measurement is
 * of the best available WebGraph path, not of a strawman.
 */
public final class WebGraphTopOrderAndLevels {

  private final int[] order;
  private final int[] levels;
  private final int levelCount;

  private WebGraphTopOrderAndLevels(int[] order, int[] levels, int levelCount) {
    this.order = order;
    this.levels = levels;
    this.levelCount = levelCount;
  }

  public static WebGraphTopOrderAndLevels of(ImmutableGraph graph) {
    int n = graph.numNodes();
    if (n == 0) {
      return new WebGraphTopOrderAndLevels(new int[0], new int[0], 0);
    }

    int[] inDegree = new int[n];
    NodeIterator nodes = graph.nodeIterator();
    while (nodes.hasNext()) {
      nodes.nextInt();
      LazyIntIterator successors = nodes.successors();
      for (int target; (target = successors.nextInt()) != -1; ) {
        inDegree[target]++;
      }
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
        LazyIntIterator successors = graph.successors(node);
        for (int target; (target = successors.nextInt()) != -1; ) {
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
    return new WebGraphTopOrderAndLevels(order, levels, level);
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
