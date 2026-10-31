package com.kobayami.graphity;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * A partition of indices into disjoint groups, with efficient reverse lookup.
 * <p>
 * Each index (node ID or edge index) belongs to at most one group, identified by a group ID in
 * {@code 0..groupCount()-1}. The forward direction (index → group) is {@link #get(int)}, with
 * {@code -1} for an index in no group. The reverse direction (group → indices) is
 * {@link #indicesOf(int)}, a zero-copy view into a CSR (Compressed Sparse Row) layout that lists
 * each group's indices in ascending order.
 * <p>
 * The concrete partitions add domain-specific aliases:
 * <ul>
 *   <li>{@link NodePartition} - partitions nodes, with {@code nodesOf()}, {@code nodeCount()}</li>
 *   <li>{@link EdgePartition} - partitions edges, with {@code edgesOf()}, {@code edgeCount()}</li>
 * </ul>
 * <p>
 * <strong>Memory:</strong> 4 x (indexCount + mappedCount + groupCount + 1) bytes for the backing
 * arrays ({@code mapping}, {@link #members}, {@link #memberStart}), where
 * {@link #mappedCount()} is the number of indices in a group.
 *
 * @see NodePartition
 * @see EdgePartition
 */
public abstract sealed class IndexPartition extends IndexMapping
    permits NodePartition, EdgePartition {

  /** CSR values: the indices of all groups, grouped by group ID, ascending within each group. */
  final int[] members;

  /** CSR offsets: group {@code g}'s members span {@code members[memberStart[g]..memberStart[g+1]-1]}. */
  final int[] memberStart;

  /**
   * Package-private constructor. Used by the subclasses.
   *
   * @param mapping     forward mapping: index → group ID, or {@code -1} for an index in no group
   * @param members     CSR values: the indices of each group, ascending within the group
   * @param memberStart CSR offsets: per-group start indices into {@code members}
   */
  IndexPartition(int[] mapping, int[] members, int[] memberStart) {
    super(mapping, members.length);
    this.members = members;
    this.memberStart = memberStart;
  }

  /**
   * Returns the number of groups.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return group count
   */
  public final int groupCount() {
    return memberStart.length - 1;
  }

  /**
   * Returns the indices in the given group as a sorted, zero-copy view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return indices in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public final SortedIntView indicesOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return new SortedIntView(members, memberStart[groupId], memberStart[groupId + 1]);
  }

  /**
   * Returns the number of indices in the given group.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return number of indices in this group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public final int sizeOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return memberStart[groupId + 1] - memberStart[groupId];
  }

  /**
   * Folds per-index values into one value per group: for each group, starting from
   * {@code identity}, {@code combine} is applied to the values of its members in ascending
   * member order. A group without members gets {@code identity}; indices in no group are ignored.
   *
   * <pre>{@code
   * int[] perGroup = partition.reduce(values, 0, Integer::sum);
   * }</pre>
   *
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 4 x groupCount bytes for the result.
   *
   * @param values   values indexed by node or edge, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public final int[] reduce(int[] values, int identity, IntBinaryOperator combine) {
    checkLength(values.length, indexCount(), "values");
    int[] members = this.members;
    int[] memberStart = this.memberStart;
    int groups = memberStart.length - 1;
    var result = new int[groups];
    for (int g = 0; g < groups; g++) {
      int acc = identity;
      for (int i = memberStart[g], end = memberStart[g + 1]; i < end; i++) {
        acc = combine.applyAsInt(acc, values[members[i]]);
      }
      result[g] = acc;
    }
    return result;
  }

  /**
   * Folds per-index values into one value per group, as {@link #reduce(int[], int,
   * IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   values indexed by node or edge, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public final long[] reduce(long[] values, long identity, LongBinaryOperator combine) {
    checkLength(values.length, indexCount(), "values");
    int[] members = this.members;
    int[] memberStart = this.memberStart;
    int groups = memberStart.length - 1;
    var result = new long[groups];
    for (int g = 0; g < groups; g++) {
      long acc = identity;
      for (int i = memberStart[g], end = memberStart[g + 1]; i < end; i++) {
        acc = combine.applyAsLong(acc, values[members[i]]);
      }
      result[g] = acc;
    }
    return result;
  }

  /**
   * Folds per-index values into one value per group, as {@link #reduce(int[], int,
   * IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   values indexed by node or edge, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public final double[] reduce(double[] values, double identity, DoubleBinaryOperator combine) {
    checkLength(values.length, indexCount(), "values");
    int[] members = this.members;
    int[] memberStart = this.memberStart;
    int groups = memberStart.length - 1;
    var result = new double[groups];
    for (int g = 0; g < groups; g++) {
      double acc = identity;
      for (int i = memberStart[g], end = memberStart[g + 1]; i < end; i++) {
        acc = combine.applyAsDouble(acc, values[members[i]]);
      }
      result[g] = acc;
    }
    return result;
  }

  /**
   * Folds per-index objects into one object per group, written into an array the caller
   * allocated: for each group with members, the fold starts from its first member's value and
   * applies {@code combine} with each further member in ascending member order. A group without
   * members keeps its entry in {@code groupValues}; indices in no group are ignored.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).
   *
   * @param <T>         element type, nullable or not
   * @param values      values indexed by node or edge, of length {@link #indexCount()}
   * @param groupValues receives one value per group; length {@link #groupCount()}, and a
   *                    different array than {@code values}
   * @param combine     folds the accumulated value with the next member's value
   * @throws IllegalArgumentException if {@code values.length != indexCount()},
   *                                  {@code groupValues.length != groupCount()}, or both are the
   *                                  same array
   */
  public final <T extends @Nullable Object> void reduce(T[] values, T[] groupValues,
      BinaryOperator<T> combine) {
    checkDistinct(values, groupValues, "groupValues");
    checkLength(values.length, indexCount(), "values");
    int[] members = this.members;
    int[] memberStart = this.memberStart;
    int groups = memberStart.length - 1;
    checkLength(groupValues.length, groups, "groupValues");
    for (int g = 0; g < groups; g++) {
      int i = memberStart[g];
      int end = memberStart[g + 1];
      if (i == end) continue;
      T acc = values[members[i]];
      for (i++; i < end; i++) {
        acc = combine.apply(acc, values[members[i]]);
      }
      groupValues[g] = acc;
    }
  }
}
