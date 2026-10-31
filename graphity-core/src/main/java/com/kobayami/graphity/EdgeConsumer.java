package com.kobayami.graphity;

/**
 * An action on an edge, given its source, target and edge index: the callback of
 * {@link Graph#forEachEdge(EdgeConsumer)}.
 * <p>
 * The parameters come in the order that
 * {@link DfsEdgeTraverser#visitEdge(int, int, int, DfsEdgeTraverser.TargetState)} and the lists
 * of {@link Edges} use as well.
 */
@FunctionalInterface
public interface EdgeConsumer {

  /**
   * Performs this action on edge {@code edgeIndex}, which is {@code source -> target}.
   *
   * @param source    node ID of the source, in {@code 0..nodeCount-1}
   * @param target    node ID of the target, in {@code 0..nodeCount-1}
   * @param edgeIndex edge index in {@code 0..edgeCount-1}
   */
  void accept(int source, int target, int edgeIndex);
}
