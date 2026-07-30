package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.Arrays;

/**
 * Back-edge detection for directed graphs.
 * <p>
 * Back-edges are edges that point from a node to one of its ancestors in the DFS tree
 * (including self-loops). Removing all back-edges from a graph makes it a DAG - they
 * form a <em>feedback arc set</em> (not necessarily minimal, but practical for most
 * dependency-analysis scenarios).
 * <p>
 * Uses a single DFS pass. See {@link #of(Graph)} for time and memory complexity.
 *
 * <pre>{@code
 * Edges backEdges = BackEdges.of(graph);
 * // backEdges.count() == 0  ⟺  graph is a DAG
 * for (int i = 0; i < backEdges.count(); i++) {
 *     int src = backEdges.sourceNodes().getInt(i);
 *     int tgt = backEdges.targetNodes().getInt(i);
 *     // src → tgt is a back-edge (cycle-causing)
 * }
 * }</pre>
 *
 * @see Edges
 * @see EdgeTypes
 * @see Graphs#isDag(Graph)
 */
public final class BackEdges {

  private BackEdges() {}

  /**
   * Computes all back-edges of the given graph via a single DFS pass.
   * <p>
   * Back-edges point from a node to an ancestor in the DFS tree (or to itself = self-loop).
   * Their removal yields a DAG.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount) + O(backEdgeCount) for the result.
   *
   * @param graph the graph to analyze
   * @return the back-edges as an {@link Edges} collection
   */
  public static Edges of(Graph graph) {
    if (graph.nodeCount() == 0) {
      return Edges.empty();
    }
    var traverser = new BackEdgeCollector(graph);
    traverser.run();
    return new Edges(
        IntLists.unmodifiable(traverser.sources),
        IntLists.unmodifiable(traverser.targets));
  }

  /**
   * Iterative DFS (explicit stack, no recursion) that collects back-edges. A back-edge is an
   * edge to a node that is currently on the DFS stack.
   * <p>
   * The visited and on-stack flags are packed 2 bits per node into a single {@code int[]}
   * (bit 0 = visited, bit 1 = on-stack, 16 nodes per int). Every edge inspects both flags of
   * its target; a single packed word gives one memory touch per node instead of two separate
   * bit-set lookups - the shift/mask ALU work is free next to the cache miss it avoids.
   */
  private static final class BackEdgeCollector {

    private static final int VISITED = 0b01;
    private static final int ON_STACK = 0b10;

    private final Graph graph;
    private final int[] state;
    final IntArrayList sources = new IntArrayList();
    final IntArrayList targets = new IntArrayList();

    BackEdgeCollector(Graph graph) {
      this.graph = graph;
      this.state = new int[(graph.nodeCount() >> 4) + 1];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = graph.outAdjacencyStart;
      int[] adj = graph.outAdjacency;
      int[] state = this.state;
      IntArrayList sources = this.sources;
      IntArrayList targets = this.targets;
      // Each frame packs (node << 32 | resume cursor) into one long: node and cursor share a
      // cache line, and the array grows on demand (one allocation per resize instead of two).
      long[] frames = new long[Math.max(16, Math.min(n, 1024))];

      for (int root = 0; root < n; root++) {
        int rShift = (root & 0xF) << 1;
        if (((state[root >> 4] >>> rShift) & VISITED) != 0) {
          continue;
        }
        state[root >> 4] |= (VISITED | ON_STACK) << rShift;
        frames[0] = ((long) root << 32) | (out[root] & 0xFFFFFFFFL);
        int top = 0;

        outer:
        while (top >= 0) {
          long frame = frames[top];
          int u = (int) (frame >>> 32);
          int end = out[u + 1];
          // Tight neighbor scan: i and end stay in registers; the frame list is
          // touched only when we actually descend, not once per edge.
          int i = (int) frame;
          while (i < end) {
            int v = adj[i++];
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;      // one load yields both of v's flags
            if ((flags & VISITED) == 0) {
              frames[top] = (frame & 0xFFFFFFFF00000000L) | (i & 0xFFFFFFFFL);
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              if (++top == frames.length) {
                frames = Arrays.copyOf(frames, top << 1);
              }
              frames[top] = ((long) v << 32) | (out[v] & 0xFFFFFFFFL);
              continue outer;
            } else if ((flags & ON_STACK) != 0) {
              sources.add(u);
              targets.add(v);
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));   // postorder: leave the DFS stack
          top--;
        }
      }
    }
  }
}
