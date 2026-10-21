package com.kobayami.graphity;

/**
 * An action on one edge of a graph, given by its two end nodes and its edge index - the callback
 * of {@link Graph#forEachEdge(EdgeConsumer)}. The parameter order is that of
 * {@link IterativeDfsEdgeTraverser#visitEdge(int, int, int, IterativeDfsEdgeTraverser.TargetState)}
 * and of the lists of {@link Edges}.
 *
 * @see Graph#forEachEdge(EdgeConsumer)
 */
@FunctionalInterface
public interface EdgeConsumer {

  /**
   * Performs the action on one edge.
   *
   * @param source    source node ID in {@code 0..nodeCount()-1} of the graph
   * @param target    target node ID in {@code 0..nodeCount()-1} of the graph
   * @param edgeIndex edge index in {@code 0..edgeCount()-1} of the graph
   */
  void accept(int source, int target, int edgeIndex);
}
