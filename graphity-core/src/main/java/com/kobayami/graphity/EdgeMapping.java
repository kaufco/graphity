package com.kobayami.graphity;

/**
 * Unidirectional mapping between two edge index spaces.
 * <p>
 * Maps source edge indices to target edge indices. A target value of {@code -1} indicates
 * that the source edge has no mapping (e.g. an edge that exists in the new graph but not
 * in the old graph).
 * <p>
 * Edge indices correspond to positions in the CSR adjacency array of a {@link Graph}:
 * edge 0 is the first out-edge of node 0, edge 1 the second, etc.
 * <p>
 * Instances are created via {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)}.
 * Use {@link #inverse()} to obtain the reverse direction,
 * {@link #compose(EdgeMapping)} to chain multiple mappings, and {@link #gather(int[])} to carry
 * per-edge data along.
 *
 * <pre>{@code
 * // Derive edge mapping from node mapping
 * EdgeMapping em = Graphs.edgeMapping(newGraph, oldGraph, nodeMapping);
 *
 * // Translate edge metadata; edges with no correspondence in the old graph get 0
 * double[] newWeights = em.gather(oldWeights);
 * }</pre>
 *
 * @see IndexMapping
 * @see EdgePartition
 * @see Graphs#edgeMapping(Graph, Graph, NodeMapping)
 */
public final class EdgeMapping extends IndexMapping {

  /**
   * Package-private constructor.
   *
   * @param mapping    the mapping array (not copied - caller must not retain a reference)
   * @param validCount number of non-negative entries in {@code mapping}
   * @param partial    whether the mapping contains unmapped entries ({@code -1})
   */
  EdgeMapping(int[] mapping, int validCount, boolean partial) {
    super(mapping, validCount, partial);
  }

  /**
   * Creates the inverse mapping (target → source).
   * <p>
   * <strong>Time:</strong> O(n).<br>
   * <strong>Memory:</strong> O(max target index) for the inverse array.
   *
   * @return the inverse edge mapping
   * @throws IllegalArgumentException if multiple source indices map to the same target
   */
  public EdgeMapping inverse() {
    int[] inv = computeInverse(mapping);
    int valid = countValid(inv);
    return new EdgeMapping(inv, valid, valid < inv.length);
  }

  /**
   * Composes this mapping with an inner mapping: {@code result(x) = this(inner(x))}.
   * <p>
   * If {@code this} maps B → C and {@code inner} maps A → B, the result maps A → C.
   * Unmapped entries ({@code -1}) propagate.
   * <p>
   * <strong>Time:</strong> O(n) where n = inner source space size.
   *
   * @param inner the inner mapping to apply first
   * @return the composed mapping
   */
  public EdgeMapping compose(EdgeMapping inner) {
    int[] comp = computeCompose(this.mapping, inner.mapping);
    int valid = countValid(comp);
    return new EdgeMapping(comp, valid, valid < comp.length);
  }

  /**
   * Carries per-edge data from the target space of this mapping into its source space:
   * {@code result[i] = values[get(i)]}, and {@code 0} where {@code i} is unmapped.
   * <p>
   * For a mapping from new to old indices, as the factories produce it, this moves an array
   * indexed by old edges to the new numbering:
   *
   * <pre>{@code
   * int[] newValues = newToOld.gather(oldValues);
   * }</pre>
   *
   * <strong>Time:</strong> O(n) where n = source space size.<br>
   * <strong>Memory:</strong> 4 x n bytes for the result.
   *
   * @param values values indexed by the target space; must cover every mapped target
   * @return values indexed by the source space, of length n
   * @throws IndexOutOfBoundsException if a mapped target lies outside {@code values}
   */
  public int[] gather(int[] values) {
    return gather(mapping, values);
  }

  /**
   * Carries per-edge data from the target space into the source space, as
   * {@link #gather(int[])} does.
   * <p>
   * <strong>Time:</strong> O(n) where n = source space size.<br>
   * <strong>Memory:</strong> 8 x n bytes for the result.
   *
   * @param values values indexed by the target space; must cover every mapped target
   * @return values indexed by the source space, {@code 0} where unmapped
   * @throws IndexOutOfBoundsException if a mapped target lies outside {@code values}
   */
  public long[] gather(long[] values) {
    return gather(mapping, values);
  }

  /**
   * Carries per-edge data from the target space into the source space, as
   * {@link #gather(int[])} does.
   * <p>
   * <strong>Time:</strong> O(n) where n = source space size.<br>
   * <strong>Memory:</strong> 8 x n bytes for the result.
   *
   * @param values values indexed by the target space; must cover every mapped target
   * @return values indexed by the source space, {@code 0} where unmapped
   * @throws IndexOutOfBoundsException if a mapped target lies outside {@code values}
   */
  public double[] gather(double[] values) {
    return gather(mapping, values);
  }

  /**
   * Carries per-edge objects from the target space into an array the caller allocated for the
   * source space: {@code newValues[i] = values[get(i)]}, and {@code newValues[i]} untouched
   * where {@code i} is unmapped. References are copied, not the objects. The two arrays must
   * be distinct; if an exception is thrown, {@code newValues} may be partly written.
   * <p>
   * <strong>Time:</strong> O(n) where n = source space size.
   *
   * @param <T>       element type
   * @param values    values indexed by the target space; must cover every mapped target
   * @param newValues receives the values indexed by the source space; length n
   * @throws IllegalArgumentException if {@code newValues.length} is not the source space size
   * @throws IndexOutOfBoundsException if a mapped target lies outside {@code values}
   */
  public <T> void gather(T[] values, T[] newValues) {
    gather(mapping, values, newValues);
  }
}
