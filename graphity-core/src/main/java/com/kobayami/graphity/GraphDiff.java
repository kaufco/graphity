package com.kobayami.graphity;

/**
 * The edges that differ between two graphs over the same nodes, as computed by
 * {@link Graphs#diff(Graph, Graph)}.
 * <p>
 * {@link #addedEdges()} names each edge by its edge index in the newer graph,
 * {@link #removedEdges()} by its edge index in the older one, so that edge data of either version
 * stays reachable. Both lists are in edge index order - by source and then target.
 *
 * <pre>{@code
 * GraphDiff diff = Graphs.diff(before, after);
 * if (!diff.isEmpty()) {
 *     Edges added = diff.addedEdges();       // edge indices of `after`
 *     Edges removed = diff.removedEdges();   // edge indices of `before`
 * }
 * }</pre>
 *
 * <strong>Memory:</strong> 12 bytes per added or removed edge.
 *
 * @see Graphs#diff(Graph, Graph)
 * @see Graphs#topologyHash(Graph)
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
   * Returns whether the two graphs have the same edges - then they are equal.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if no edge was added or removed
   */
  public boolean isEmpty() {
    return addedEdges.isEmpty() && removedEdges.isEmpty();
  }

  /**
   * Returns the number of added and removed edges.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return a short description of this diff
   */
  @Override
  public String toString() {
    return "GraphDiff[added=" + addedEdges.count() + ", removed=" + removedEdges.count() + "]";
  }
}
