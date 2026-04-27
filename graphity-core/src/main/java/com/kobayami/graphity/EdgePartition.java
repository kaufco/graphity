package com.kobayami.graphity;

import java.util.Objects;

/**
 * Bidirectional partition of edges into disjoint groups, with efficient reverse lookup.
 * <p>
 * Used for many-to-one edge mappings, such as condensation: multiple original edges
 * may map to a single condensed edge. Intra-group edges (self-edges in the condensation)
 * are unmapped ({@code get(edgeIndex) == -1}).
 * <p>
 * The forward direction (original edge → condensed edge) is inherited from
 * {@link IndexMapping#get(int)}. The reverse direction (condensed edge → original edges)
 * is provided by {@link #edgesOf(int)}.
 * <p>
 * Implements {@link IndexPartition} for generic partition access via {@link #groupCount()},
 * {@link #indexCount()}, {@link #indicesOf(int)}, and {@link #sizeOf(int)}.
 * Domain-specific convenience methods ({@link #edgeCount()}, {@link #edgesOf(int)})
 * delegate to the generic interface.
 * <p>
 * Instances are created via {@link Graphs#edgePartition(Graph, Graph, NodePartition)}.
 *
 * <pre>{@code
 * EdgePartition ep = Graphs.edgePartition(condensed, original, partition);
 * for (int ce = 0; ce < condensed.edgeCount(); ce++) {
 *     SortedIntView origEdges = ep.edgesOf(ce);
 *     // aggregate metadata from original edges
 * }
 * }</pre>
 *
 * @see EdgeMapping
 * @see IndexPartition
 * @see Graphs#edgePartition(Graph, Graph, NodePartition)
 */
public non-sealed class EdgePartition extends EdgeMapping implements IndexPartition {

  /** CSR values: original edge indices of all group members, grouped by condensed edge. */
  private final int[] members;

  /** CSR offsets: group {@code g}'s members span {@code members[memberStart[g]..memberStart[g+1]-1]}. */
  private final int[] memberStart;

  /**
   * Package-private constructor.
   *
   * @param mapping     forward mapping: original edge index → condensed edge index, or -1
   * @param validCount  number of non-negative entries in mapping
   * @param partial     whether mapping contains -1 entries
   * @param members     CSR values: original edge indices per condensed edge group
   * @param memberStart CSR offsets: per-group start indices into members
   */
  EdgePartition(int[] mapping, int validCount, boolean partial, int[] members, int[] memberStart) {
    super(mapping, validCount, partial);
    this.members = members;
    this.memberStart = memberStart;
  }

  // ---- IndexPartition implementation ----

  /**
   * Returns the number of groups (condensed edges).
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public int groupCount() {
    return memberStart.length - 1;
  }

  /**
   * Returns the total number of original edges.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public int indexCount() {
    return mapping.length;
  }

  /**
   * Returns the original edge indices in the given group as a sorted, zero-copy view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID (condensed edge index) in {@code 0..groupCount()-1}
   * @return original edge indices in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  @Override
  public SortedIntView indicesOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return SortedIntView.viewOf(members, memberStart[groupId], memberStart[groupId + 1]);
  }

  /**
   * Returns the number of original edges in the given group.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID (condensed edge index) in {@code 0..groupCount()-1}
   * @return number of original edges in this group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  @Override
  public int sizeOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return memberStart[groupId + 1] - memberStart[groupId];
  }

  // ---- Domain-specific convenience (delegate to IndexPartition) ----

  /**
   * Returns the total number of original edges.
   * Equivalent to {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return original edge count, summed over all condensed edges
   */
  public int edgeCount() {
    return indexCount();
  }

  /**
   * Returns the original edge indices for the given condensed edge as a sorted, zero-copy view.
   * Equivalent to {@link #indicesOf(int)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId condensed edge index in {@code 0..groupCount()-1}
   * @return original edge indices, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public SortedIntView edgesOf(int groupId) {
    return indicesOf(groupId);
  }

  // ---- Override ----

  /**
   * Always throws — a partition mapping is many-to-one and cannot be inverted
   * to an {@link EdgeMapping}. Use {@link #edgesOf(int)} for the reverse direction.
   *
   * @throws IllegalArgumentException always
   */
  @Override
  public EdgeMapping inverse() {
    throw new IllegalArgumentException(
        "Cannot invert a partition mapping (many-to-one). Use edgesOf() for the reverse direction.");
  }
}
