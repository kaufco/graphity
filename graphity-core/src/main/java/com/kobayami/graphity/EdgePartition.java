package com.kobayami.graphity;

/**
 * Bidirectional partition of edges into disjoint groups, with efficient reverse lookup.
 * <p>
 * Used wherever several edges belong to one group. In a condensation the group is a condensed
 * edge, and multiple original edges map to it; in {@link Biconnectivity#blocks()} the group is
 * a biconnected component. Edges that belong to no group are unmapped
 * ({@code get(edgeIndex) == -1}) - edges inside a group or from or to a node in no group in a
 * condensation, self-loops in a biconnectivity result.
 * <p>
 * The forward direction (edge → group) is {@link #get(int)}. The reverse direction
 * (group → edges) is provided by {@link #edgesOf(int)}.
 * <p>
 * Extends {@link IndexPartition} for generic partition access via {@link #groupCount()},
 * {@link #indexCount()}, {@link #indicesOf(int)}, {@link #sizeOf(int)}, and the
 * {@code reduce} methods. Domain-specific convenience methods ({@link #edgeCount()},
 * {@link #edgesOf(int)}) are aliases of the generic ones.
 * <p>
 * Instances are created via {@link Graphs#edgePartition(Graph, NodePartition)} and
 * {@link Biconnectivity#blocks()}.
 *
 * <pre>{@code
 * EdgePartition ep = Graphs.edgePartition(condensed, partition);
 * SortedIntView origEdges = ep.edgesOf(condensedEdge);         // edges behind one condensed edge
 * double[] weights = ep.reduce(originalWeights, 0.0, Double::sum);  // one value per condensed edge
 *
 * // the edges from node group g to node group h of the partition
 * int gh = condensed.edgeIndexOf(g, h);                         // -1 if none runs from g to h
 * SortedIntView between = gh < 0 ? SortedIntView.EMPTY : ep.edgesOf(gh);
 * }</pre>
 *
 * @see EdgeMapping
 * @see IndexPartition
 * @see Graphs#edgePartition(Graph, NodePartition)
 * @see Biconnectivity#blocks()
 */
public final class EdgePartition extends IndexPartition {

  /**
   * Package-private constructor.
   *
   * @param mapping     forward mapping: original edge index → group ID, or -1
   * @param members     CSR values: original edge indices per group, ascending within each group
   * @param memberStart CSR offsets: per-group start indices into members
   */
  EdgePartition(int[] mapping, int[] members, int[] memberStart) {
    super(mapping, members, memberStart);
  }

  /**
   * Returns the total number of original edges.
   * Equivalent to {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return original edge count
   */
  public int edgeCount() {
    return indexCount();
  }

  /**
   * Returns the edge indices in the given group as a sorted, zero-copy view.
   * Equivalent to {@link #indicesOf(int)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID (condensed edge index, or block ID) in {@code 0..groupCount()-1}
   * @return edge indices in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public SortedIntView edgesOf(int groupId) {
    return indicesOf(groupId);
  }
}
