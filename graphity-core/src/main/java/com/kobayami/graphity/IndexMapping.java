package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base for unidirectional index mappings between two ID spaces.
 * <p>
 * Maps source indices in {@code 0..indexCount()-1} to target indices. A target value of
 * {@code -1} indicates that the source index has no mapping (partial mapping). Inversion is
 * defined only on the invertible subclasses ({@link NodeMapping#inverted(int)},
 * {@link EdgeMapping#inverted(int)}), not on partitions, which are many-to-one.
 * <p>
 * {@link NodeMapping} and {@link EdgeMapping} specialize this base for node ID spaces and edge
 * index spaces respectively, with one-to-one, invertible mapping semantics. One-to-one is a
 * contract, not a check: the factories that derive a mapping guarantee it, but for
 * {@link NodeMapping#of(int[])} it is the caller's responsibility, and a mapping that breaks it
 * is rejected only by {@code inverted} and {@link Graphs#remapped(Graph, NodeMapping)}.
 * {@link IndexPartition} specializes this base with many-to-one partition semantics instead, and
 * adds the reverse (group → indices) lookup.
 * <p>
 * <strong>Memory:</strong> 4 x (source space size) bytes for the backing {@link #mapping} array.
 *
 * @see NodeMapping
 * @see EdgeMapping
 * @see IndexPartition
 */
public abstract sealed class IndexMapping permits NodeMapping, EdgeMapping, IndexPartition {

  /** Source-to-target mapping. {@code mapping[sourceId] = targetId}, or {@code -1} if unmapped. */
  final int[] mapping;

  /** Number of non-negative entries in {@link #mapping}. */
  private final int validCount;

  /**
   * Package-private constructor. Used by the subclasses.
   *
   * @param mapping    the mapping array (not copied - caller must not retain a reference)
   * @param validCount number of non-negative entries in {@code mapping}
   */
  IndexMapping(int[] mapping, int validCount) {
    this.mapping = mapping;
    this.validCount = validCount;
  }

  /**
   * Returns the target index for the given source index, or {@code -1} if it is unmapped.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param id source index in {@code 0..indexCount()-1}
   * @return target index, or {@code -1} if unmapped
   * @throws IndexOutOfBoundsException if {@code id} is out of range
   */
  public final int get(int id) {
    Objects.checkIndex(id, mapping.length);
    return mapping[id];
  }

  /**
   * Returns whether the given source index has a mapping (target &ge; 0).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param id source index in {@code 0..indexCount()-1}
   * @return {@code true} if mapped
   * @throws IndexOutOfBoundsException if {@code id} is out of range
   */
  public final boolean isMapped(int id) {
    return get(id) >= 0;
  }

  /**
   * Returns whether this mapping is partial (some source indices have no target).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if at least one source index maps to {@code -1}
   */
  public final boolean isPartial() {
    return validCount < mapping.length;
  }

  /**
   * Returns the number of valid (non-{@code -1}) mappings.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return count of mapped source indices
   */
  public final int mappedCount() {
    return validCount;
  }

  /**
   * Returns the size of the source index space, including unmapped indices. This is the length
   * of the arrays that {@code gather} produces, and the length {@code reduce} expects.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return size of the source index space
   */
  public final int indexCount() {
    return mapping.length;
  }

  /**
   * Inverts the given mapping array into a target space of {@code targetCount} indices.
   *
   * @throws IllegalArgumentException  if {@code targetCount} is negative, or multiple sources map
   *                                   to the same target
   * @throws IndexOutOfBoundsException if a target is {@code targetCount} or more
   */
  static int[] invertArray(int[] mapping, int targetCount) {
    if (targetCount < 0) {
      throw new IllegalArgumentException("targetCount out of range: " + targetCount);
    }
    int[] inverse = new int[targetCount];
    Arrays.fill(inverse, -1);
    for (int source = 0; source < mapping.length; source++) {
      int target = mapping[source];
      if (target < 0) continue;
      if (target >= targetCount) {
        throw new IndexOutOfBoundsException("source " + source + " maps to " + target
            + ", outside the target space 0.." + (targetCount - 1));
      }
      if (inverse[target] != -1) {
        throw new IllegalArgumentException(
            "cannot invert mapping: multiple source IDs map to target " + target);
      }
      inverse[target] = source;
    }
    return inverse;
  }

  /**
   * Composes two mapping arrays: {@code result[i] = outer[inner[i]]}. Unmapped entries
   * ({@code -1}) propagate.
   *
   * @throws IndexOutOfBoundsException if {@code inner} maps to a value outside {@code outer}
   */
  static int[] composeArrays(int[] outer, int[] inner) {
    int[] result = new int[inner.length];
    for (int i = 0; i < result.length; i++) {
      int mid = inner[i];
      if (mid < 0) {
        result[i] = -1;
      } else if (mid < outer.length) {
        result[i] = outer[mid];
      } else {
        throw new IndexOutOfBoundsException("inner maps " + i + " to " + mid
            + ", outside the outer mapping's source space 0.." + (outer.length - 1));
      }
    }
    return result;
  }

  static void checkLength(int length, int expected, String name) {
    if (length != expected) {
      throw new IllegalArgumentException(
          name + " has length " + length + ", expected " + expected);
    }
  }

  /** Rejects writing the result into the input array, which would overwrite unread values. */
  static void checkDistinct(@Nullable Object[] values, @Nullable Object[] result, String name) {
    if (values == result) {
      throw new IllegalArgumentException("values and " + name + " must be distinct arrays");
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

  // --- gather: result[i] = values[mapping[i]], unmapped entries 0 or untouched ---

  static int[] gatherArray(int[] mapping, int[] values) {
    var result = new int[mapping.length];
    for (int i = 0; i < result.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
    return result;
  }

  static long[] gatherArray(int[] mapping, long[] values) {
    var result = new long[mapping.length];
    for (int i = 0; i < result.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
    return result;
  }

  static double[] gatherArray(int[] mapping, double[] values) {
    var result = new double[mapping.length];
    for (int i = 0; i < result.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
    return result;
  }

  static <T extends @Nullable Object> void gatherArray(int[] mapping, T[] values, T[] result) {
    checkDistinct(values, result, "newValues");
    checkLength(result.length, mapping.length, "newValues");
    for (int i = 0; i < mapping.length; i++) {
      int source = mapping[i];
      if (source >= 0) result[i] = values[source];
    }
  }
}
