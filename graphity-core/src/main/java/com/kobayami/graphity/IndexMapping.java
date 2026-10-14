package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * Abstract base for unidirectional index mappings between two ID spaces.
 * <p>
 * Maps source indices to target indices. A target value of {@code -1} indicates that
 * the source index has no mapping (partial mapping). Inversion is defined only on the
 * invertible subclasses ({@link NodeMapping#inverse()}, {@link EdgeMapping#inverse()}),
 * not on partitions, which are many-to-one.
 * <p>
 * {@link NodeMapping} and {@link EdgeMapping} specialize this base with one-to-one,
 * invertible mapping semantics, for node ID spaces and edge index spaces respectively.
 * {@link NodePartition} and {@link EdgePartition} specialize it with many-to-one
 * partition semantics instead, and additionally implement {@link IndexPartition} for
 * reverse (group → indices) lookup.
 * <p>
 * <strong>Memory:</strong> 4 x (source space size) bytes for the backing {@link #mapping} array.
 *
 * @see NodeMapping
 * @see EdgeMapping
 * @see IndexPartition
 */
public abstract sealed class IndexMapping permits NodeMapping, EdgeMapping, NodePartition, EdgePartition {

  /** Source-to-target mapping. {@code mapping[sourceId] = targetId}, or {@code -1} if unmapped. */
  final int[] mapping;

  private final int validCount;
  private final boolean partial;

  /**
   * Package-private constructor. Used by factory methods and subclasses.
   *
   * @param mapping    the mapping array (not copied - caller must not retain a reference)
   * @param validCount number of non-negative entries in {@code mapping}
   * @param partial    whether the mapping contains unmapped entries ({@code -1})
   */
  IndexMapping(int[] mapping, int validCount, boolean partial) {
    this.mapping = mapping;
    this.validCount = validCount;
    this.partial = partial;
  }

  /**
   * Returns the target index for the given source index, or {@code -1} if unmapped.
   * <p>
   * Returns {@code -1} for out-of-range source indices (negative or &ge; source space size).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param id source index
   * @return target index, or {@code -1} if unmapped or out of range
   */
  public int get(int id) {
    if (id < 0 || id >= mapping.length) return -1;
    return mapping[id];
  }

  /**
   * Returns whether the given source index has a mapping (target &ge; 0).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param id source index
   * @return {@code true} if mapped
   */
  public boolean contains(int id) {
    return get(id) >= 0;
  }

  /**
   * Returns whether this mapping is partial (some source indices have no target).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if at least one source index maps to {@code -1}
   */
  public boolean isPartial() {
    return partial;
  }

  /**
   * Returns the number of valid (non-{@code -1}) mappings.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return count of mapped source indices
   */
  public int size() {
    return validCount;
  }

  /**
   * Computes the inverse of the given mapping array.
   *
   * @param mapping the mapping array to invert
   * @return the inverse array, or empty array if input is empty or all-negative
   * @throws IllegalArgumentException if multiple sources map to the same target
   */
  static int[] computeInverse(int[] mapping) {
    if (mapping.length == 0) {
      return new int[0];
    }

    int maxTarget = -1;
    for (int v : mapping) {
      if (v > maxTarget) maxTarget = v;
    }
    if (maxTarget < 0) {
      return new int[0];
    }

    int[] inv = new int[maxTarget + 1];
    Arrays.fill(inv, -1);
    for (int i = 0; i < mapping.length; i++) {
      int target = mapping[i];
      if (target >= 0) {
        if (inv[target] != -1) {
          throw new IllegalArgumentException(
              "Cannot invert mapping: multiple source IDs map to target " + target);
        }
        inv[target] = i;
      }
    }
    return inv;
  }

  /**
   * Computes the composition of two mapping arrays: {@code result[i] = outer[inner[i]]}.
   * Unmapped entries ({@code -1}) propagate.
   */
  static int[] computeCompose(int[] outer, int[] inner) {
    int[] result = new int[inner.length];
    for (int i = 0; i < result.length; i++) {
      int mid = inner[i];
      if (mid >= 0 && mid < outer.length) {
        result[i] = outer[mid];
      } else {
        result[i] = -1;
      }
    }
    return result;
  }

  // --- gather: result[i] = values[mapping[i]], unmapped entries 0 or untouched ---

  static int[] gather(int[] mapping, int[] values) {
    Objects.requireNonNull(values, "values");
    var result = new int[mapping.length];
    for (int i = 0; i < result.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
    return result;
  }

  static long[] gather(int[] mapping, long[] values) {
    Objects.requireNonNull(values, "values");
    var result = new long[mapping.length];
    for (int i = 0; i < result.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
    return result;
  }

  static double[] gather(int[] mapping, double[] values) {
    Objects.requireNonNull(values, "values");
    var result = new double[mapping.length];
    for (int i = 0; i < result.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
    return result;
  }

  static <T> void gather(int[] mapping, T[] values, T[] result) {
    Objects.requireNonNull(values, "values");
    checkLength(result, mapping.length, "newValues");
    for (int i = 0; i < mapping.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
  }

  // --- reduce over the member CSR of a partition, members ascending within each group ---

  static int[] reduce(int[] members, int[] memberStart, int indexCount, int[] values,
      int identity, IntBinaryOperator combine) {
    checkLength(values, indexCount, "values");
    Objects.requireNonNull(combine, "combine");
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

  static long[] reduce(int[] members, int[] memberStart, int indexCount, long[] values,
      long identity, LongBinaryOperator combine) {
    checkLength(values, indexCount, "values");
    Objects.requireNonNull(combine, "combine");
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

  static double[] reduce(int[] members, int[] memberStart, int indexCount, double[] values,
      double identity, DoubleBinaryOperator combine) {
    checkLength(values, indexCount, "values");
    Objects.requireNonNull(combine, "combine");
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

  static <T> void reduce(int[] members, int[] memberStart, int indexCount, T[] values,
      T[] groupValues, BinaryOperator<T> combine) {
    checkLength(values, indexCount, "values");
    int groups = memberStart.length - 1;
    checkLength(groupValues, groups, "groupValues");
    Objects.requireNonNull(combine, "combine");
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

  private static void checkLength(int[] array, int expected, String name) {
    checkLength(Objects.requireNonNull(array, name).length, expected, name);
  }

  private static void checkLength(long[] array, int expected, String name) {
    checkLength(Objects.requireNonNull(array, name).length, expected, name);
  }

  private static void checkLength(double[] array, int expected, String name) {
    checkLength(Objects.requireNonNull(array, name).length, expected, name);
  }

  private static void checkLength(Object[] array, int expected, String name) {
    checkLength(Objects.requireNonNull(array, name).length, expected, name);
  }

  private static void checkLength(int length, int expected, String name) {
    if (length != expected) {
      throw new IllegalArgumentException(
          name + " has length " + length + ", expected " + expected);
    }
  }

  /**
   * Counts non-negative entries in the given array.
   */
  static int countValid(int[] mapping) {
    int count = 0;
    for (int v : mapping) {
      if (v >= 0) count++;
    }
    return count;
  }
}
