package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.BitSet;

/**
 * Topological sort of a directed acyclic graph (DAG).
 * <p>
 * Uses a single DFS pass (reverse postorder) for maximum performance.
 * For topological order with level information, use {@link TopOrderAndLevels#of(Graph)}.
 *
 * <pre>{@code
 * IntList order = TopOrder.of(graph);
 * // order[0] has no predecessors in the order,
 * // order[last] has no successors in the order.
 * }</pre>
 *
 * @see TopOrderAndLevels
 * @see Graphs#isDag(Graph)
 */
public final class TopOrder {

  private TopOrder() {}

  /**
   * Computes a topological ordering of the given graph via DFS (reverse postorder).
   * <p>
   * The returned list contains all node IDs in an order such that for every directed
   * edge u → v, u appears before v.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to sort (must be a DAG)
   * @return an unmodifiable list of node IDs in topological order
   * @throws IllegalArgumentException if the graph contains a cycle
   */
  public static IntList of(Graph graph) {
    var traverser = new Traverser(graph);
    traverser.run();
    if (traverser.hasCycle) {
      throw new IllegalArgumentException("Graph contains a cycle");
    }
    return IntLists.unmodifiable(IntArrayList.wrap(traverser.order));
  }

  /**
   * DFS traverser that records reverse postorder (= topological order) and detects
   * back-edges (cycles) via an {@code onStack} BitSet.
   */
  private static final class Traverser extends DfsTraverser {

    private final BitSet onStack;
    final int[] order;
    int orderPos;
    boolean hasCycle;

    Traverser(Graph graph) {
      super(graph);
      int n = graph.nodeCount();
      this.onStack = new BitSet(n);
      this.order = new int[n];
      this.orderPos = n;
    }

    @Override
    protected void visitNode(int node) {
      if (hasCycle) return;
      onStack.set(node);

      var adjacency = graph.outAdjacency;
      var adjacencyStart = graph.outAdjacencyStart;
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        int target = adjacency[i];
        if (onStack.get(target)) {
          hasCycle = true;
          return;
        }
        visit(target);
        if (hasCycle) return;
      }

      onStack.clear(node);
      order[--orderPos] = node;
    }
  }
}
