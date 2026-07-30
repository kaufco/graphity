package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.Arrays;
import java.util.Objects;

/**
 * Full edge-type classification of a directed {@link Graph}.
 * <p>
 * Classifies every edge into one of the four DFS edge types: {@link EdgeType#TREE},
 * {@link EdgeType#BACK}, {@link EdgeType#FORWARD}, {@link EdgeType#CROSS}.
 * Uses a single DFS pass with discovery-time and on-stack state tracking.
 * <p>
 * Edges are numbered in the order they appear in the CSR adjacency arrays: edge 0 is
 * the first out-edge of node 0, edge 1 the second, etc. This is the same order as
 * iterating over all nodes and their out-edges sequentially.
 *
 * <pre>{@code
 * EdgeTypes et = EdgeTypes.of(graph);
 * EdgeType type = et.typeOf(0);                    // type of edge 0
 * Edges backEdges = et.edgesOfType(EdgeType.BACK);  // all back-edges
 * }</pre>
 *
 * <strong>Time:</strong> O(nodeCount + edgeCount) for computation.<br>
 * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result.
 *
 * @see EdgeType
 * @see BackEdges
 */
public final class EdgeTypes {

  private static final EdgeType[] EDGE_TYPE_VALUES = EdgeType.values();

  /** Reference to the original graph, for resolving edge indices to source/target pairs. */
  private final Graph graph;

  /** Edge type per edge, indexed by edge position in the CSR adjacency array. */
  private final byte[] types;

  private EdgeTypes(Graph graph, byte[] types) {
    this.graph = graph;
    this.types = types;
  }

  /**
   * Computes the edge-type classification of the given graph via a single DFS pass.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount).
   *
   * @param graph the graph to classify
   * @return the edge-type classification
   */
  public static EdgeTypes of(Graph graph) {
    int m = graph.edgeCount();
    if (m == 0) {
      return new EdgeTypes(graph, new byte[0]);
    }
    var traverser = new EdgeClassifier(graph, m);
    traverser.run();
    return new EdgeTypes(graph, traverser.types);
  }

  /**
   * Returns the type of the edge at the given index.
   * <p>
   * Edge indices correspond to positions in the CSR adjacency array.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param edgeIndex edge index in {@code 0..edgeCount-1}
   * @return the edge type
   * @throws IndexOutOfBoundsException if {@code edgeIndex} is out of range
   */
  public EdgeType typeOf(int edgeIndex) {
    Objects.checkIndex(edgeIndex, types.length);
    return EDGE_TYPE_VALUES[types[edgeIndex]];
  }

  /**
   * Returns all edges of the given type as an {@link Edges} collection.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(matchingEdgeCount).
   *
   * @param type the edge type to filter by
   * @return edges of the given type
   */
  public Edges edgesOfType(EdgeType type) {
    byte ordinal = (byte) type.ordinal();
    var sources = new IntArrayList();
    var targets = new IntArrayList();

    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;
    int n = graph.nodeCount();

    for (int node = 0; node < n; node++) {
      int start = adjacencyStart[node];
      int end = adjacencyStart[node + 1];
      for (int ei = start; ei < end; ei++) {
        if (types[ei] == ordinal) {
          sources.add(node);
          targets.add(adjacency[ei]);
        }
      }
    }

    if (sources.isEmpty()) {
      return Edges.empty();
    }
    return new Edges(IntLists.unmodifiable(sources), IntLists.unmodifiable(targets));
  }

  /**
   * Iterative DFS (explicit stack, no recursion) that classifies all edges using discovery
   * times and a packed visited/on-stack state.
   * <p>
   * The visited and on-stack flags are packed 2 bits per node into a single {@code int[]}
   * (bit 0 = visited, bit 1 = on-stack, 16 nodes per int); discovery times stay in a
   * separate {@code int[] disc}.
   * <p>
   * Classification rules for edge (u → v):
   * <ul>
   *   <li>v unvisited → TREE (triggers descent)</li>
   *   <li>v on DFS stack → BACK (ancestor)</li>
   *   <li>v visited, disc[u] < disc[v] → FORWARD (descendant, already finished)</li>
   *   <li>v visited, disc[u] > disc[v] → CROSS (different subtree, already finished)</li>
   * </ul>
   */
  private static final class EdgeClassifier {

    private static final int VISITED = 0b01;
    private static final int ON_STACK = 0b10;

    private final Graph graph;
    private final int[] state;
    private final int[] disc;
    final byte[] types;

    EdgeClassifier(Graph graph, int edgeCount) {
      int n = graph.nodeCount();
      this.graph = graph;
      this.state = new int[(n >> 4) + 1];
      this.disc = new int[n];
      this.types = new byte[edgeCount];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = graph.outAdjacencyStart;
      int[] adj = graph.outAdjacency;
      int[] state = this.state;
      int[] disc = this.disc;
      byte[] types = this.types;
      byte tree = (byte) EdgeType.TREE.ordinal();
      byte back = (byte) EdgeType.BACK.ordinal();
      byte forward = (byte) EdgeType.FORWARD.ordinal();
      byte cross = (byte) EdgeType.CROSS.ordinal();
      // Each frame packs (node << 32 | resume cursor) into one long: node and cursor share a
      // cache line, and the array grows on demand (one allocation per resize instead of two).
      long[] frames = new long[Math.max(16, Math.min(n, 1024))];
      int time = 0;

      for (int root = 0; root < n; root++) {
        int rShift = (root & 0xF) << 1;
        if (((state[root >> 4] >>> rShift) & VISITED) != 0) {
          continue;
        }
        state[root >> 4] |= (VISITED | ON_STACK) << rShift;
        disc[root] = ++time;
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
            int c = i++;
            int v = adj[c];
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;
            if ((flags & VISITED) == 0) {
              types[c] = tree;
              frames[top] = (frame & 0xFFFFFFFF00000000L) | (i & 0xFFFFFFFFL);
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              disc[v] = ++time;
              if (++top == frames.length) {
                frames = Arrays.copyOf(frames, top << 1);
              }
              frames[top] = ((long) v << 32) | (out[v] & 0xFFFFFFFFL);
              continue outer;
            } else if ((flags & ON_STACK) != 0) {
              types[c] = back;
            } else if (disc[u] < disc[v]) {
              types[c] = forward;
            } else {
              types[c] = cross;
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
          top--;
        }
      }
    }
  }
}
