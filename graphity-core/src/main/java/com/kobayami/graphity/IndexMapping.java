package com.kobayami.graphity;

import java.util.Arrays;

/**
 * Abstract base for unidirectional index mappings between two ID spaces.
 * <p>
 * Maps source indices to target indices. A target value of {@code -1} indicates that
 * the source index has no mapping (partial mapping). Use {@link #inverse()} to create
 * the reverse mapping.
 * <p>
 * Concrete subclasses provide domain-specific semantics:
 * <ul>
 *   <li>{@link NodeMapping} — maps between node ID spaces</li>
 *   <li>{@link EdgeMapping} — maps between edge index spaces</li>
 * </ul>
 * <p>
 * For partition mappings (many-to-one), see {@link IndexPartition}.
 *
 * @see NodeMapping
 * @see EdgeMapping
 * @see IndexPartition
 */
public abstract sealed class IndexMapping permits NodeMapping, EdgeMapping {

  /** Source-to-target mapping. {@code mapping[sourceId] = targetId}, or {@code -1} if unmapped. */
  final int[] mapping;

  private final int validCount;
  private final boolean partial;

  /**
   * Package-private constructor. Used by factory methods and subclasses.
   *
   * @param mapping    the mapping array (not copied — caller must not retain a reference)
   * @param validCount number of non-negative entries in {@code mapping}
   * @param partial    whether the mapping contains unmapped entries ({@code -1})
   */
  IndexMapping(int[] mapping, int validCount, boolean partial) {
    this.mapping = mapping;
    this.validCount = validCount;
    this.partial = partial;
  }

  // ---- Read-only API ----

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

  // ---- Abstract ----

  /**
   * Creates the inverse mapping (target → source).
   * <p>
   * Each target value in this mapping becomes a source in the inverse, and vice versa.
   * If the inverse is partial (not all target-space indices are covered), unmapped indices
   * map to {@code -1}.
   *
   * @return the inverse mapping
   * @throws IllegalArgumentException if the mapping is not injective (cannot be inverted)
   */
  public abstract IndexMapping inverse();

  // ---- Shared implementation utilities (package-private) ----

  /**
   * Computes the inverse of the given mapping array.
   *
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
