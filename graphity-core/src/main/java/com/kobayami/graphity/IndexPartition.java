package com.kobayami.graphity;

/**
 * A partition of indices into disjoint groups, with efficient reverse lookup.
 * <p>
 * Every index (node ID or edge index) belongs to exactly one group, identified by a
 * group ID in {@code 0..groupCount()-1}. The forward direction (index → group) is
 * provided by {@link IndexMapping#get(int)}. The reverse direction (group → indices)
 * is provided by {@link #indicesOf(int)}.
 * <p>
 * Concrete implementations provide domain-specific convenience methods:
 * <ul>
 *   <li>{@link NodePartition} — partitions nodes, with {@code nodesOf()}, {@code nodeCount()}</li>
 *   <li>{@link EdgePartition} — partitions edges, with {@code edgesOf()}, {@code edgeCount()}</li>
 * </ul>
 *
 * @see NodePartition
 * @see EdgePartition
 */
public sealed interface IndexPartition permits NodePartition, EdgePartition {

  /**
   * Returns the number of groups (partitions).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return group count
   */
  int groupCount();

  /**
   * Returns the total number of indices in this partition (nodes or edges).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return total index count, summed over all groups
   */
  int indexCount();

  /**
   * Returns the indices in the given group as a sorted, zero-copy view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return indices in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  SortedIntView indicesOf(int groupId);

  /**
   * Returns the number of indices in the given group.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return number of indices in this group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  int sizeOf(int groupId);
}
