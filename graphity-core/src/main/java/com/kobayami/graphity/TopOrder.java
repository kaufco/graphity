package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

/**
 * The topological order of a DAG (directed acyclic graph), in which every node comes before its
 * out-nodes.
 * <p>
 * Uses a
 * <a href="https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search">depth-first
 * search</a>, without recursion: {@link #of(Graph)} returns its reverse postorder. The search
 * starts at the nodes in ascending order and takes the out-nodes of each node in ascending order,
 * as {@link DfsTraverser#run()} does, so a graph always gets the same order.
 * {@link TopOrderAndLevels} returns another topological order, together with a level per node.
 * <p>
 * A graph with a cycle has no topological order, and {@link #of(Graph)} rejects it;
 * {@link Graphs#isDag(Graph)} tests for a cycle, and {@link Graphs#findCycle(Graph)} returns one.
 * On a graph with cycles, {@link Components#sccsOf(Graph)} puts each cycle into one SCC (strongly
 * connected component) and numbers the SCCs in reverse topological order.
 */
public final class TopOrder {

  private TopOrder() {}

  /**
   * Returns the nodes of {@code graph} in topological order: for every edge {@code u -> v},
   * {@code u} comes before {@code v}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x nodeCount bytes for the result; while it runs, 2 bits per node
   * and up to 8 x nodeCount bytes for the stack, which grows with the deepest path.
   *
   * @param graph the DAG to sort
   * @return the node IDs in topological order; unmodifiable
   * @throws IllegalArgumentException if {@code graph} has a cycle, or if a depth-first path is
   *                                  longer than 2<sup>30</sup> - 5 nodes
   */
  public static IntList of(Graph graph) {
    var traverser = new TopOrderTraverser(graph);
    traverser.run();
    if (traverser.hasCycle) {
      throw new IllegalArgumentException("graph contains a cycle");
    }
    return IntLists.unmodifiable(IntArrayList.wrap(traverser.order));
  }

  // The depth-first search without recursion, writing each node into order from the back when it
  // leaves it, and stopping at the first edge to a node on its path, which closes a cycle. Both
  // flags of a node share one int, 16 nodes per int (bit 0 visited, bit 1 on the path), so that a
  // check of a node reads one int instead of two bit sets.
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
      int[] out = Csr.outAdjacencyStart(graph);
      int[] adj = Csr.outAdjacency(graph);
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
          // The scan keeps i and end in registers and touches the stack only when it
          // descends, not once per edge.
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
