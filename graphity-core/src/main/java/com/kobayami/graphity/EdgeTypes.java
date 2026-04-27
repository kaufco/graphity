package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.BitSet;

/**
 * Full edge-type classification of a directed {@link Graph}.
 * <p>
 * Classifies every edge into one of the four DFS edge types: {@link EdgeType#TREE},
 * {@link EdgeType#BACK}, {@link EdgeType#FORWARD}, {@link EdgeType#CROSS}.
 * Uses a single DFS pass with discovery and finish time tracking.
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
    var traverser = new Traverser(graph, m);
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
   * @throws ArrayIndexOutOfBoundsException if {@code edgeIndex} is out of range
   */
  public EdgeType typeOf(int edgeIndex) {
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

  // ---- DFS traverser for edge classification ----

  /**
   * DFS traverser that classifies all edges using discovery times and an on-stack BitSet.
   * <p>
   * Classification rules for edge (u → v):
   * <ul>
   *   <li>v unvisited → TREE (triggers recursive visit)</li>
   *   <li>v on DFS stack → BACK (ancestor)</li>
   *   <li>v visited, disc[u] < disc[v] → FORWARD (descendant, already finished)</li>
   *   <li>v visited, disc[u] > disc[v] → CROSS (different subtree, already finished)</li>
   * </ul>
   */
  private static final class Traverser extends DfsTraverser {

    private final int[] disc;
    private final BitSet onStack;
    final byte[] types;
    private int time;

    Traverser(Graph graph, int edgeCount) {
      super(graph);
      int n = graph.nodeCount();
      this.disc = new int[n];
      this.onStack = new BitSet(n);
      this.types = new byte[edgeCount];
    }

    @Override
    protected void visitNode(int node) {
      disc[node] = ++time;
      onStack.set(node);

      var adjacency = graph.outAdjacency;
      var adjacencyStart = graph.outAdjacencyStart;
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        int target = adjacency[i];
        if (visit(target)) {
          types[i] = (byte) EdgeType.TREE.ordinal();
        } else if (onStack.get(target)) {
          types[i] = (byte) EdgeType.BACK.ordinal();
        } else if (disc[node] < disc[target]) {
          types[i] = (byte) EdgeType.FORWARD.ordinal();
        } else {
          types[i] = (byte) EdgeType.CROSS.ordinal();
        }
      }

      onStack.clear(node);
    }
  }
}
