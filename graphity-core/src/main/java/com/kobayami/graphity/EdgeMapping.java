package com.kobayami.graphity;

/**
 * Unidirectional mapping between two edge index spaces.
 * <p>
 * Maps source edge indices to target edge indices. A target value of {@code -1} indicates
 * that the source edge has no mapping (e.g. an intra-group edge dropped during condensation).
 * <p>
 * Edge indices correspond to positions in the CSR adjacency array of a {@link Graph}:
 * edge 0 is the first out-edge of node 0, edge 1 the second, etc.
 * <p>
 * Instances are created via {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)}.
 * Use {@link #inverse()} to obtain the reverse direction, and
 * {@link #compose(EdgeMapping)} to chain multiple mappings.
 *
 * <pre>{@code
 * // Derive edge mapping from node mapping
 * EdgeMapping em = Graphs.edgeMapping(newGraph, oldGraph, nodeMapping);
 *
 * // Translate edge metadata
 * double[] newWeights = new double[newGraph.edgeCount()];
 * for (int i = 0; i < em.size(); i++) {
 *     newWeights[i] = oldWeights[em.get(i)];
 * }
 * }</pre>
 *
 * @see IndexMapping
 * @see EdgePartition
 * @see Graphs#edgeMapping(Graph, Graph, NodeMapping)
 */
public sealed class EdgeMapping extends IndexMapping permits EdgePartition {

  /**
   * Package-private constructor.
   *
   * @param mapping    the mapping array (not copied — caller must not retain a reference)
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
  @Override
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
}
