package com.kobayami.graphity;

/**
 * The edges added and removed between two graphs over the same nodes, as
 * {@link Graphs#diff(Graph, Graph)} returns them.
 * <p>
 * Each edge comes with its edge index in the graph it belongs to: an added edge with its edge
 * index in the newer graph, a removed edge with its edge index in the older one. With these edge
 * indices, the caller can look up data on the edges of either graph in arrays indexed by edge
 * index. Both lists are in edge index order. To see whether a graph changed without keeping the
 * older one, compare hashes from {@link Graphs#topologyHash(Graph)} instead.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 12 bytes per added or removed edge.
 */
public final class GraphDiff {

  private final Edges addedEdges;
  private final Edges removedEdges;

  GraphDiff(Edges addedEdges, Edges removedEdges) {
    this.addedEdges = addedEdges;
    this.removedEdges = removedEdges;
  }

  /**
   * Returns the edges of the newer graph that the older one does not have, with their edge
   * indices in the newer graph, in edge index order.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the added edges
   */
  public Edges addedEdges() {
    return addedEdges;
  }

  /**
   * Returns the edges of the older graph that the newer one does not have, with their edge
   * indices in the older graph, in edge index order.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the removed edges
   */
  public Edges removedEdges() {
    return removedEdges;
  }

  /**
   * Returns whether no edge was added or removed, that is, whether the two graphs are equal.
   * Equivalent to {@code addedEdges().isEmpty() && removedEdges().isEmpty()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if the graphs are equal
   */
  public boolean isEmpty() {
    return addedEdges.isEmpty() && removedEdges.isEmpty();
  }

  /**
   * Returns the numbers of added and removed edges, for debuggers and logs.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public String toString() {
    return "GraphDiff[added=" + addedEdges.count() + ", removed=" + removedEdges.count() + "]";
  }
}
