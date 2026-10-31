package com.kobayami.graphity;

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
 * Edges backEdges = Edges.of(et, EdgeType.BACK);  // all back-edges, one scan per call
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
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   */
  public static EdgeTypes of(Graph graph) {
    if (graph.edgeCount() == 0) {
      return new EdgeTypes(graph, new byte[0]);
    }
    var traverser = new EdgeClassifier(graph);
    traverser.run();
    return new EdgeTypes(graph, traverser.types);
  }

  /**
   * Returns the type of the edge at the given index.
   * <p>
   * Edge indices correspond to slots in the CSR adjacency array.
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

  // The body of Edges.of(EdgeTypes, EdgeType). Counts the edges of the type first, so that the
  // arrays of the result get their exact length, then fills them in one scan over all edges.
  Edges collect(EdgeType type) {
    byte ordinal = (byte) type.ordinal();
    int count = 0;
    for (byte t : types) {
      if (t == ordinal) count++;
    }
    if (count == 0) {
      return Edges.empty();
    }
    var sources = new int[count];
    var targets = new int[count];
    var edgeIndices = new int[count];

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);
    int n = graph.nodeCount();
    int p = 0;

    for (int node = 0; node < n; node++) {
      int start = adjacencyStart[node];
      int end = adjacencyStart[node + 1];
      for (int e = start; e < end; e++) {
        if (types[e] == ordinal) {
          sources[p] = node;
          targets[p] = adjacency[e];
          edgeIndices[p] = e;
          p++;
        }
      }
    }
    return Edges.wrap(sources, targets, edgeIndices);
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

    EdgeClassifier(Graph graph) {
      int n = graph.nodeCount();
      this.graph = graph;
      this.state = new int[(n >> 4) + 1];
      this.disc = new int[n];
      this.types = new byte[graph.edgeCount()];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = Csr.outAdjacencyStart(graph);
      int[] adj = Csr.outAdjacency(graph);
      int[] state = this.state;
      int[] disc = this.disc;
      byte[] types = this.types;
      byte tree = (byte) EdgeType.TREE.ordinal();
      byte back = (byte) EdgeType.BACK.ordinal();
      byte forward = (byte) EdgeType.FORWARD.ordinal();
      byte cross = (byte) EdgeType.CROSS.ordinal();
      // Each frame is two interleaved ints, the node and its resume position in the
      // adjacency array: both share a cache line, and the array grows on demand.
      int[] frames = DfsFrames.initial(n);
      int time = 0;

      for (int root = 0; root < n; root++) {
        int rShift = (root & 0xF) << 1;
        if (((state[root >> 4] >>> rShift) & VISITED) != 0) {
          continue;
        }
        state[root >> 4] |= (VISITED | ON_STACK) << rShift;
        disc[root] = ++time;
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
            int e = i++;
            int v = adj[e];
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;
            if ((flags & VISITED) == 0) {
              types[e] = tree;
              frames[2 * top + 1] = i;
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              disc[v] = ++time;
              if (++top == frames.length >> 1) {
                frames = DfsFrames.grow(frames, n);
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            } else if ((flags & ON_STACK) != 0) {
              types[e] = back;
            } else if (disc[u] < disc[v]) {
              types[e] = forward;
            } else {
              types[e] = cross;
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
          top--;
        }
      }
    }
  }
}
