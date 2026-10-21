package com.kobayami.graphity;

/**
 * Read access to the arrays of a graph's CSR layout, for algorithm code that must loop over them
 * without a call or a bounds check per edge.
 * <p>
 * Each method returns the graph's own array, not a copy, in constant time. The layout:
 * <ul>
 *   <li>{@code outAdjacencyStart} has length {@code nodeCount + 1}, starts with {@code 0}, ends
 *       with {@code edgeCount} and never decreases; the out-nodes of node {@code v} are
 *       {@code outAdjacency[outAdjacencyStart[v]..outAdjacencyStart[v + 1] - 1]}, sorted
 *       ascending without duplicates, and the position of an entry in {@code outAdjacency} is
 *       the index of that edge.</li>
 *   <li>{@code inAdjacencyStart} and {@code inAdjacency} hold the in-nodes of a
 *       {@link BiAdjacentGraph} in the same layout. Positions in {@code inAdjacency} are not edge
 *       indices: {@link Graphs#transposedEdgeMapping(Graph)} maps every position to its edge index
 *       in one pass, {@link BiAdjacentGraph#transposedEdgeIndex(int)} relates a single edge.</li>
 * </ul>
 * The typical loop loads the arrays into local variables once:
 * <pre>{@code
 * int[] start = Csr.outAdjacencyStart(graph);
 * int[] targets = Csr.outAdjacency(graph);
 * for (int v = 0; v < graph.nodeCount(); v++) {
 *     for (int e = start[v], end = start[v + 1]; e < end; e++) {
 *         int w = targets[e];   // edge e: v -> w
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>The arrays are read-only by contract.</strong> Nothing prevents writing to them, and
 * writing breaks every guarantee of the graph - its immutability, the sorted adjacency every
 * algorithm relies on, and safe sharing between threads - for every holder of the graph. The
 * library's own algorithms read the arrays through this class as well; there is no faster or more
 * privileged access. Code that only needs a node's neighbors is better served by
 * {@link Graph#outNodes(int)} and the edge-index methods on {@link Graph}, which check their
 * arguments.
 *
 * @see Graph
 * @see BiAdjacentGraph
 */
public final class Csr {

  private Csr() {}

  /**
   * Returns the out-adjacency offsets of the graph, length {@code nodeCount + 1}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own offset array; read-only by contract
   */
  public static int[] outAdjacencyStart(Graph graph) {
    return graph.outAdjacencyStart();
  }

  /**
   * Returns the out-adjacency targets of the graph, length {@code edgeCount}; the position of an
   * entry is the index of its edge.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own target array; read-only by contract
   */
  public static int[] outAdjacency(Graph graph) {
    return graph.outAdjacency();
  }

  /**
   * Returns the in-adjacency offsets of the graph, length {@code nodeCount + 1}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own offset array; read-only by contract
   */
  public static int[] inAdjacencyStart(BiAdjacentGraph graph) {
    return graph.inAdjacencyStart();
  }

  /**
   * Returns the in-adjacency sources of the graph, length {@code edgeCount}. Positions are not
   * edge indices.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own source array; read-only by contract
   */
  public static int[] inAdjacency(BiAdjacentGraph graph) {
    return graph.inAdjacency();
  }
}
