package com.kobayami.graphity;

import java.util.Objects;

/**
 * The type of every edge of a graph in a depth-first search: tree, back, forward or cross edge, as
 * {@link EdgeType} defines them.
 * <p>
 * The types depend on the search. Uses a
 * <a href="https://en.wikipedia.org/wiki/Depth-first_search#Output_of_a_depth-first_search">
 * depth-first search</a>, without recursion. The search starts at the nodes in ascending order
 * and takes the out-nodes of each node in ascending order, as {@link DfsTraverser#run()} does.
 * {@link #typeOf(int)} returns the type of one edge, and {@link Edges#of(EdgeTypes, EdgeType)}
 * collects the edges of one type. Here, {@code cfg} is the control flow of a method with a loop
 * and a branch: the edge {@code next -> header} closes the loop, and {@code body -> next} skips
 * {@code then}:
 * {@snippet file="com/kobayami/graphity/examples/Traversals.java" region="edgeTypes"}
 * <p>
 * A graph is a DAG (directed acyclic graph) exactly if it has no back edge. For the back edges
 * alone, {@link BackEdges#of(Graph)} saves the classification of the other edges.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 1 byte per edge; an instance also keeps a reference to its graph.
 */
public final class EdgeTypes {

  private static final EdgeType[] EDGE_TYPE_VALUES = EdgeType.values();

  // The graph, for the sources and targets in collect.
  private final Graph graph;

  // The ordinal of the type of each edge, by edge index.
  private final byte[] types;

  private EdgeTypes(Graph graph, byte[] types) {
    this.graph = graph;
    this.types = types;
  }

  /**
   * Returns the type of every edge of {@code graph}, from one depth-first search.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> edgeCount bytes for the result; while it runs, 4 bytes and 2 bits
   * per node, and up to 8 x nodeCount bytes for the stack, which grows with the deepest path.
   *
   * @param graph the graph to classify
   * @return the edge types
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
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
   * Returns the type of edge {@code edgeIndex}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param edgeIndex edge index in {@code 0..edgeCount-1}
   * @return edge type
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

  // The depth-first search without recursion. It classifies the edge u -> v by what it finds at
  // v: unvisited is a tree edge, on the path a back edge, and a node already left a forward edge
  // if u was discovered before v (v is a descendant of u), else a cross edge. Both flags of a
  // node share one int, 16 nodes per int (bit 0 visited, bit 1 on the path); the discovery times
  // stay in disc.
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
          // The scan keeps i and end in registers and touches the stack only when it
          // descends, not once per edge.
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
