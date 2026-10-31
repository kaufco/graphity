package com.kobayami.graphity;

/**
 * Access to the arrays in which a graph keeps its edges, for algorithm code that loops over all
 * edges. These arrays are called the CSR (compressed sparse row) layout of the graph.
 * <p>
 * Each method returns the graph's own array, not a copy. The out-nodes of node {@code v} are
 * {@code outAdjacency[outAdjacencyStart[v]..outAdjacencyStart[v + 1] - 1]}, in ascending order, and
 * the position of an out-node in {@code outAdjacency} is the edge index of its edge.
 * {@code outAdjacencyStart} has {@code nodeCount + 1} values, from {@code 0} to {@code edgeCount}.
 * A loop loads the two arrays into local variables once and reads them without a method call or an
 * argument check per edge. Here, {@code calls} holds the calls between the methods of a program,
 * and the loop counts the callers of each method:
 * {@snippet file="com/kobayami/graphity/examples/BuildingAndReading.java" region="loop"}
 * A {@link BiAdjacentGraph} also holds the in-nodes of every node, called its in-adjacency. The
 * in-adjacency has the same layout, in {@link #inAdjacencyStart(BiAdjacentGraph)} and
 * {@link #inAdjacency(BiAdjacentGraph)}. A position in {@code inAdjacency} is not an edge index of
 * the graph, though, but of its transposed graph; {@link Graphs#transposedEdgeMapping(Graph)}
 * translates all of them in one pass.
 * <p>
 * The arrays must not be modified. Nothing prevents a write, but it changes the graph for
 * everyone who holds it and breaks what every algorithm relies on: that the graph is immutable
 * and safe to share between threads, and that the out-nodes of each node are sorted. The
 * algorithms of this library read the arrays the same way. Code that reads the out-nodes of a
 * few nodes is better served by {@link Graph#outNodes(int)} and the edge index methods of
 * {@link Graph}, which check their arguments.
 */
public final class Csr {

  private Csr() {}

  /**
   * Returns where the out-nodes of each node start in {@link #outAdjacency(Graph)}, followed by
   * {@code edgeCount}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own array, of length {@code nodeCount + 1}; not to be modified
   */
  public static int[] outAdjacencyStart(Graph graph) {
    return graph.outAdjacencyStart();
  }

  /**
   * Returns the out-nodes of all nodes, node after node, at the positions of their edge indices.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own array, of length {@code edgeCount}; not to be modified
   */
  public static int[] outAdjacency(Graph graph) {
    return graph.outAdjacency();
  }

  /**
   * Returns where the in-nodes of each node start in {@link #inAdjacency(BiAdjacentGraph)},
   * followed by {@code edgeCount}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own array, of length {@code nodeCount + 1}; not to be modified
   */
  public static int[] inAdjacencyStart(BiAdjacentGraph graph) {
    return graph.inAdjacencyStart();
  }

  /**
   * Returns the in-nodes of all nodes, node after node.
   * <p>
   * A position in this array is the edge index of the edge in the transposed graph, not in
   * {@code graph}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph
   * @return the graph's own array, of length {@code edgeCount}; not to be modified
   */
  public static int[] inAdjacency(BiAdjacentGraph graph) {
    return graph.inAdjacency();
  }
}
