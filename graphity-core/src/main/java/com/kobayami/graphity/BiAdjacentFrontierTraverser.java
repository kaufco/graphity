package com.kobayami.graphity;

/**
 * A {@link FrontierTraverser} for {@link BiAdjacentGraph} instances, adding root-aware traversal.
 * <p>
 * Inherits all traversal methods from {@link FrontierTraverser} ({@link #run()}, {@link #run(int)},
 * {@link #runFromNodes(it.unimi.dsi.fastutil.ints.IntList)}) and adds {@link #runFromRoots()},
 * which starts traversal from root nodes (in-degree 0) only.
 *
 * @see FrontierTraverser
 * @see BiAdjacentGraph
 */
public abstract class BiAdjacentFrontierTraverser extends FrontierTraverser {

  /** The graph as {@link BiAdjacentGraph}, for in-adjacency access. Same instance as {@link #graph}. */
  protected final BiAdjacentGraph biAdjacentGraph;

  /**
   * Creates a traverser for the given bi-adjacent graph.
   *
   * @param graph the bi-adjacent graph to traverse
   */
  protected BiAdjacentFrontierTraverser(BiAdjacentGraph graph) {
    super(graph);
    this.biAdjacentGraph = graph;
  }

  /**
   * Runs frontier traversal starting from root nodes (nodes with in-degree 0), in ascending
   * node order. Only visits nodes reachable from root nodes that have not been visited yet.
   * In a DAG, this covers all nodes. In cyclic graphs, nodes in strongly connected
   * components with no incoming edges from outside are not visited.
   * <p>
   * All root nodes form level 0.
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(nodeCount + reachable edges).
   */
  public final void runFromRoots() {
    runFromNodes(biAdjacentGraph.rootNodes());
  }
}
