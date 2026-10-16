package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

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
   * @throws IllegalArgumentException if the graph contains a cycle, or if a depth-first path is
   *     longer than 2^30 - 5 nodes, the most frames an array can hold
   */
  public static IntList of(Graph graph) {
    var traverser = new TopOrderTraverser(graph);
    traverser.run();
    if (traverser.hasCycle) {
      throw new IllegalArgumentException("Graph contains a cycle");
    }
    return IntLists.unmodifiable(IntArrayList.wrap(traverser.order));
  }

  /**
   * Iterative DFS (explicit stack, no recursion) that records reverse postorder
   * (= topological order) and detects back-edges (cycles).
   * <p>
   * visited and on-stack flags are packed 2 bits per node into a single {@code int[]}
   * (bit 0 = visited, bit 1 = on-stack, 16 nodes per int): one memory touch per node check
   * instead of two separate bit-set lookups.
   */
  private static final class TopOrderTraverser {

    private static final int VISITED = 0b01;
    private static final int ON_STACK = 0b10;

    private final Graph graph;
    private final int[] state;
    final int[] order;
    int orderPos;
    boolean hasCycle;

    TopOrderTraverser(Graph graph) {
      int n = graph.nodeCount();
      this.graph = graph;
      this.state = new int[(n >> 4) + 1];
      this.order = new int[n];
      this.orderPos = n;
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = graph.outAdjacencyStart;
      int[] adj = graph.outAdjacency;
      int[] state = this.state;
      int[] order = this.order;
      // Each frame is two interleaved ints, the node and its resume position in the
      // adjacency array: both share a cache line, and the array grows on demand.
      int[] frames = DfsFrames.initial(n);

      for (int root = 0; root < n; root++) {
        int rShift = (root & 0xF) << 1;
        if (((state[root >> 4] >>> rShift) & VISITED) != 0) {
          continue;
        }
        state[root >> 4] |= (VISITED | ON_STACK) << rShift;
        frames[0] = root;
        frames[1] = out[root];
        int top = 0;

        outer:
        while (top >= 0) {
          int u = frames[2 * top];
          int end = out[u + 1];
          // Tight neighbor scan: i and end stay in registers; the frame list is
          // touched only when we actually descend, not once per edge.
          int i = frames[2 * top + 1];
          while (i < end) {
            int v = adj[i++];
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;
            if ((flags & ON_STACK) != 0) {
              hasCycle = true;
              return;
            }
            if ((flags & VISITED) == 0) {
              frames[2 * top + 1] = i;
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              if (++top == frames.length >> 1) {
                frames = DfsFrames.grow(frames, n);
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
          order[--orderPos] = u;
          top--;
        }
      }
    }
  }
}
