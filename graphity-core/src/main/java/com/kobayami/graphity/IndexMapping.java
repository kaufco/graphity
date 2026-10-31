package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A mapping from the indices {@code 0..indexCount()-1} to the indices of another numbering. It is
 * the common base of the node and edge mappings and of the partitions.
 * <p>
 * The indices are node IDs or edge indices. {@link #get(int) get(i)} is the index that {@code i}
 * maps to, or {@code -1} if {@code i} is not mapped; a mapping with unmapped indices is partial.
 * A {@link NodeMapping} maps the node IDs of one graph to those of another, and an
 * {@link EdgeMapping} does the same for edge indices. Neither maps two indices to the same index,
 * so both can be inverted. An {@link IndexPartition} maps indices to groups and lists the indices
 * of each group. Several indices can map to the same group.
 * <p>
 * That a node or edge mapping maps no two indices to the same index is a contract, not a check:
 * {@link NodeMapping#of(int[])} accepts an array that breaks it, and only {@code inverted} and
 * {@link Graphs#remapped(Graph, NodeMapping)} reject such a mapping.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x indexCount bytes; a partition holds more.
 */
public abstract sealed class IndexMapping permits NodeMapping, EdgeMapping, IndexPartition {

  // mapping[i] is the index that i maps to, or -1. Graphs and Biconnectivity read it directly.
  final int[] mapping;

  // The number of entries other than -1, counted once so that isPartial and mappedCount are O(1).
  private final int validCount;

  // Takes the array as it is, not copied: the caller hands over an array that nobody else holds,
  // and validCount matches it.
  IndexMapping(int[] mapping, int validCount) {
    this.mapping = mapping;
    this.validCount = validCount;
  }

  /**
   * Returns the index that {@code id} maps to, or {@code -1} if {@code id} is not mapped.
   * <p>
   * In a partition, it is the group of {@code id}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param id index in {@code 0..indexCount()-1}
   * @return index that {@code id} maps to, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code id} is out of range
   */
  public final int get(int id) {
    Objects.checkIndex(id, mapping.length);
    return mapping[id];
  }

  /**
   * Returns whether {@code id} is mapped. Equivalent to {@code get(id) != -1}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param id index in {@code 0..indexCount()-1}
   * @return {@code true} if {@code id} is mapped
   * @throws IndexOutOfBoundsException if {@code id} is out of range
   */
  public final boolean isMapped(int id) {
    return get(id) >= 0;
  }

  /**
   * Returns whether some index is not mapped. Equivalent to
   * {@code mappedCount() < indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if at least one index is not mapped
   */
  public final boolean isPartial() {
    return validCount < mapping.length;
  }

  /**
   * Returns the number of indices that are mapped; in a partition, the number of indices that
   * belong to a group.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return mapped count
   */
  public final int mappedCount() {
    return validCount;
  }

  /**
   * Returns the number of indices this mapping maps from, mapped or not.
   * <p>
   * It is the length of the arrays that {@code gather} returns and that {@code reduce} takes.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return index count
   */
  public final int indexCount() {
    return mapping.length;
  }

  // The inverse of mapping over targetCount indices, -1 where nothing maps to an index. Rejects an
  // index outside the inverse and two indices that map to the same one, which have no inverse.
  static int[] invertArray(int[] mapping, int targetCount) {
    if (targetCount < 0 || targetCount > ArrayLimits.MAX_ARRAY_LENGTH) {
      throw new IllegalArgumentException("targetCount out of range: " + targetCount);
    }
    int[] inverse = new int[targetCount];
    Arrays.fill(inverse, -1);
    for (int i = 0; i < mapping.length; i++) {
      int value = mapping[i];
      if (value < 0) continue;
      if (value >= targetCount) {
        throw new IndexOutOfBoundsException(
            "index " + i + " maps to " + value + ", but targetCount is " + targetCount);
      }
      if (inverse[value] != -1) {
        throw new IllegalArgumentException("cannot invert mapping: indices " + inverse[value]
            + " and " + i + " both map to " + value);
      }
      inverse[value] = i;
    }
    return inverse;
  }

  // result[i] = outer[inner[i]], and -1 where inner[i] is -1.
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
            + ", outside the indices 0.." + (outer.length - 1) + " of this mapping");
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

  // Rejects writing the result into the input array, which would overwrite values not yet read.
  static void checkDistinct(@Nullable Object[] values, @Nullable Object[] result, String name) {
    if (values == result) {
      throw new IllegalArgumentException("values and " + name + " must be distinct arrays");
    }
  }

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
