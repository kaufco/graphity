package com.kobayami.graphity;

import org.jspecify.annotations.Nullable;

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
 * Instances are created via {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)} and
 * {@link Graphs#transposedEdgeMapping(Graph)}.
 * Use {@link #inverted(int)} to obtain the reverse direction,
 * {@link #compose(EdgeMapping)} to chain multiple mappings, and {@link #gather(int[])} to carry
 * per-edge data along.
 *
 * <pre>{@code
 * // Derive edge mapping from node mapping
 * EdgeMapping em = Graphs.edgeMapping(newGraph, oldGraph, nodeMapping);
 *
 * // Translate edge metadata; edges with no correspondence in the old graph get 0
 * double[] newWeights = em.gather(oldWeights);
 *
 * // A subgraph keeps the edges between selected nodes, and their data with them
 * NodeMapping keep = NodeMapping.forSubgraph(selected);
 * Graph sub = Graphs.remapped(graph, keep);
 * double[] subWeights = Graphs.edgeMapping(sub, graph, keep).gather(weights);
 * }</pre>
 *
 * A transpose keeps node IDs but swaps the roles of source and target; its mapping comes from
 * {@link Graphs#transposedEdgeMapping(Graph)} instead.
 *
 * @see IndexMapping
 * @see EdgePartition
 * @see Graphs#edgeMapping(Graph, Graph, NodeMapping)
 * @see Graphs#transposedEdgeMapping(Graph)
 */
public final class EdgeMapping extends IndexMapping {

  /**
   * Package-private constructor.
   *
   * @param mapping    the mapping array (not copied - caller must not retain a reference)
   * @param validCount number of non-negative entries in {@code mapping}
   */
  EdgeMapping(int[] mapping, int validCount) {
    super(mapping, validCount);
  }

  /**
   * Creates the inverse mapping (target → source) over a target space of {@code targetCount}
   * edge indices.
   * <p>
   * Each target value in this mapping becomes a source in the inverse, and vice versa. The
   * inverse covers every index in {@code 0..targetCount-1}; an index that no source maps to maps
   * to {@code -1}, and then the inverse is partial. For a mapping from new to old edge indices,
   * as {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)} produces it, {@code targetCount} is
   * the edge count of the old graph:
   *
   * <pre>{@code
   * EdgeMapping oldToNew = newToOld.inverted(oldGraph.edgeCount());
   * }</pre>
   *
   * <strong>Time:</strong> O(n + targetCount) where n = source space size.<br>
   * <strong>Memory:</strong> 4 x targetCount bytes for the inverse array.
   *
   * @param targetCount size of the target space, which becomes the source space of the inverse
   * @return the inverse edge mapping, with {@link #indexCount()} equal to {@code targetCount}
   * @throws IllegalArgumentException  if {@code targetCount} is negative or larger than
   *                                   {@code Integer.MAX_VALUE - 8}, the largest array, or
   *                                   multiple source indices map to the same target
   * @throws IndexOutOfBoundsException if a target is {@code targetCount} or more
   */
  public EdgeMapping inverted(int targetCount) {
    return new EdgeMapping(invertArray(mapping, targetCount), mappedCount());
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
   * @throws IndexOutOfBoundsException if {@code inner} maps to a value outside this mapping's
   *                                   source space
   */
  public EdgeMapping compose(EdgeMapping inner) {
    int[] composed = composeArrays(this.mapping, inner.mapping);
    return new EdgeMapping(composed, countValid(composed));
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
    return gatherArray(mapping, values);
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
    return gatherArray(mapping, values);
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
    return gatherArray(mapping, values);
  }

  /**
   * Carries per-edge objects from the target space into an array the caller allocated for the
   * source space: {@code newValues[i] = values[get(i)]}, and {@code newValues[i]} untouched
   * where {@code i} is unmapped. References are copied, not the objects. If an exception is
   * thrown, {@code newValues} may be partly written.
   * <p>
   * <strong>Time:</strong> O(n) where n = source space size.
   *
   * @param <T>       element type, nullable or not
   * @param values    values indexed by the target space; must cover every mapped target
   * @param newValues receives the values indexed by the source space; length
   *                  {@link #indexCount()}, and a different array than {@code values}
   * @throws IllegalArgumentException if {@code newValues.length != indexCount()}, or both are the
   *                                  same array
   * @throws IndexOutOfBoundsException if a mapped target lies outside {@code values}
   */
  public <T extends @Nullable Object> void gather(T[] values, T[] newValues) {
    gatherArray(mapping, values, newValues);
  }
}
