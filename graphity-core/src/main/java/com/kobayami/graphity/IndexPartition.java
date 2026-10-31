package com.kobayami.graphity;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import org.jspecify.annotations.Nullable;

/**
 * A partition of indices into groups: the common base of {@link NodePartition} and
 * {@link EdgePartition}.
 * <p>
 * The indices are node IDs or edge indices. Each index is in at most one group, and the groups
 * are numbered by group ID {@code 0..groupCount()-1}. {@link #get(int) get(i)} returns the group
 * of index {@code i}, or {@code -1} if it is in no group, and {@link #indicesOf(int)} the indices
 * of a group in ascending order. {@code reduce} combines data on the indices into one value per
 * group. The data is an array indexed by node ID or edge index.
 * <p>
 * A partition is a mapping from indices to groups that maps several indices to the same group,
 * so unlike a {@link NodeMapping} or an {@link EdgeMapping}, it cannot be inverted.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x (indexCount + mappedCount + groupCount) bytes, where mappedCount
 * is the number of indices that belong to a group.
 */
public abstract sealed class IndexPartition extends IndexMapping
    permits NodePartition, EdgePartition {

  // The indices of each group in the layout of the out-adjacency of a Graph: the indices of group
  // g are members[memberStart[g]..memberStart[g + 1] - 1], sorted ascending, with the sentinel
  // memberStart[groupCount] == members.length.
  final int[] members;
  final int[] memberStart;

  // Takes the arrays as they are, neither copied nor checked: the caller guarantees the layout,
  // and that mapping and members describe the same groups.
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
   * Returns the indices in group {@code groupId}, in ascending order.
   * <p>
   * The result is a view of this partition's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return indices in the group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public final SortedIntView indicesOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return new SortedIntView(members, memberStart[groupId], memberStart[groupId + 1]);
  }

  /**
   * Returns the number of indices in group {@code groupId}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return size of the group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public final int sizeOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return memberStart[groupId + 1] - memberStart[groupId];
  }

  /**
   * Returns one value per group, combined from the values of its indices.
   * <p>
   * For each group, starts from {@code identity} and combines the result so far with the value of
   * each index in the group, in ascending order of the indices. A group without indices gets
   * {@code identity}, and the values of indices in no group are left out. Here,
   * {@code byPackageCall} groups the calls between the methods of a program by the call between
   * packages that each belongs to (see {@link EdgePartition}), and the call sites in
   * {@code siteCount} are summed per call between packages:
   * {@snippet file="com/kobayami/graphity/examples/Partitions.java" region="reduce"}
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 4 x groupCount bytes for the result.
   *
   * @param values   the value of each index; length {@link #indexCount()}
   * @param identity the start value of each group
   * @param combine  called with the result so far and the value of the next index, returns
   *                 the two combined
   * @return one value per group, by group ID
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
   * Returns one value per group, combined from the values of its indices, as
   * {@link #reduce(int[], int, IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   the value of each index; length {@link #indexCount()}
   * @param identity the start value of each group
   * @param combine  called with the result so far and the value of the next index, returns
   *                 the two combined
   * @return one value per group, by group ID
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
   * Returns one value per group, combined from the values of its indices, as
   * {@link #reduce(int[], int, IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   the value of each index; length {@link #indexCount()}
   * @param identity the start value of each group
   * @param combine  called with the result so far and the value of the next index, returns
   *                 the two combined
   * @return one value per group, by group ID
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
   * Writes one value per group into {@code groupValues}, combined from the values of the indices of
   * that group. This is the form of {@code reduce} for objects.
   * <p>
   * For each group, starts from the value of its first index and combines the result so far with
   * the value of each further index, in ascending order of the indices. A group without indices
   * keeps its entry in {@code groupValues}, and the values of indices in no group are left out.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).
   *
   * @param <T>         the element type, nullable or not
   * @param values      the value of each index; length {@link #indexCount()}
   * @param groupValues receives one value per group, by group ID; length {@link #groupCount()},
   *                    and not the same array as {@code values}
   * @param combine     called with the result so far and the value of the next index, returns
   *                    the two combined
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
