package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Oracle for dominance, straight from the definition: {@code d} dominates {@code v} if
 * {@code v} is reachable from the entries, but no longer once {@code d} is removed. Shares no
 * code and no idea with the algorithms under test. O(nodeCount x (nodeCount + edgeCount)) time
 * and nodeCount^2 bits; meant for graphs of a few hundred nodes.
 */
public final class NaiveDominators {

  /** {@code dominates[d][v]}: {@code d} dominates {@code v}. A node dominates itself. */
  public final boolean[][] dominates;

  /** Reachability from the entries. */
  public final boolean[] reachable;

  private final Graph graph;

  public NaiveDominators(Graph graph, IntList entries) {
    this.graph = graph;
    int n = graph.nodeCount();
    reachable = reach(graph, entries, -1);
    dominates = new boolean[n][];
    for (int d = 0; d < n; d++) {
      dominates[d] = new boolean[n];
      if (!reachable[d]) continue;
      var without = reach(graph, entries, d);
      for (int v = 0; v < n; v++) {
        dominates[d][v] = reachable[v] && !without[v];
      }
    }
  }

  /**
   * The strict dominator of {@code v} that all other strict dominators dominate, or {@code -1}
   * if there is none or {@code v} is unreachable. The dominators of a node form a chain, so it
   * is the one with the most dominators of its own.
   */
  public int immediateDominatorOf(int v) {
    if (!reachable[v]) return -1;
    int n = graph.nodeCount();
    int best = -1;
    int bestCount = -1;
    for (int d = 0; d < n; d++) {
      if (d == v || !dominates[d][v]) continue;
      int count = 0;
      for (int x = 0; x < n; x++) {
        if (dominates[x][d]) count++;
      }
      if (count > bestCount) {
        best = d;
        bestCount = count;
      }
    }
    return best;
  }

  /**
   * All dominance frontiers: {@code frontier[x][m]} if {@code x} dominates a predecessor of
   * {@code m} and does not strictly dominate {@code m}.
   */
  public boolean[][] frontiers() {
    int n = graph.nodeCount();
    var frontier = new boolean[n][n];
    for (int x = 0; x < n; x++) {
      if (!reachable[x]) continue;
      for (int p = 0; p < n; p++) {
        if (!dominates[x][p]) continue;
        var outs = graph.outNodes(p);
        for (int i = 0; i < outs.size(); i++) {
          int m = outs.getInt(i);
          if (!(dominates[x][m] && x != m)) frontier[x][m] = true;
        }
      }
    }
    return frontier;
  }

  /** Breadth-first reachability from the entries, with {@code removed} taken out of the graph. */
  private static boolean[] reach(Graph graph, IntList entries, int removed) {
    int n = graph.nodeCount();
    var seen = new boolean[n];
    var queue = new int[n];
    int head = 0;
    int tail = 0;
    for (int i = 0; i < entries.size(); i++) {
      int e = entries.getInt(i);
      if (e != removed && !seen[e]) {
        seen[e] = true;
        queue[tail++] = e;
      }
    }
    while (head < tail) {
      int u = queue[head++];
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        if (v != removed && !seen[v]) {
          seen[v] = true;
          queue[tail++] = v;
        }
      }
    }
    return seen;
  }
}
