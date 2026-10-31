package com.kobayami.graphity;

import org.jspecify.annotations.Nullable;

/**
 * A mapping from the edge indices of one graph to those of another, to carry data on edges over
 * to a subgraph, a renumbered graph or the transposed graph.
 * <p>
 * {@link #get(int) get(e)} is the edge index that edge {@code e} maps to, or {@code -1} if edge
 * {@code e} is not mapped. The factories return mappings from a new graph to an old one. These map
 * each new edge to the edge it comes from. {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)}
 * derives one from the {@link NodeMapping} of a subgraph or a renumbering, and leaves a new edge
 * without a counterpart in the old graph unmapped. {@link Graphs#transposedEdgeMapping(Graph)} maps
 * each edge of the transposed graph to the edge it reverses.
 * <p>
 * {@code gather} carries data on the old edges over to the new ones. Here, {@code siteCount}
 * holds the number of call sites of each call by edge index of {@code calls}, and the subgraph
 * {@code loading} from the example at {@link NodeMapping} takes these counts along:
 * {@snippet file="com/kobayami/graphity/examples/Mappings.java" region="edgeMapping"}
 * {@link #inverted(int)} returns the mapping from old to new, and {@link #compose(EdgeMapping)}
 * chains two mappings.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x indexCount bytes.
 */
public final class EdgeMapping extends IndexMapping {

  // Takes the array as it is, not copied: the caller hands over an array that nobody else holds,
  // and validCount matches it.
  EdgeMapping(int[] mapping, int validCount) {
    super(mapping, validCount);
  }

  /**
   * Returns the inverse mapping, which maps {@code get(e)} back to {@code e}.
   * <p>
   * The inverse maps the {@code targetCount} edges of the graph that this mapping maps to; an edge
   * that no edge maps to is not mapped, and the inverse is then partial. For a mapping from new
   * to old edge indices, as the factories return it, {@code targetCount} is the edge count of the
   * old graph, and the inverse gives the new edge index of an old edge:
   * {@code newToOld.inverted(oldGraph.edgeCount()).get(oldEdge)}.
   * <p>
   * <strong>Time:</strong> O(indexCount + targetCount).<br>
   * <strong>Memory:</strong> 4 x targetCount bytes for the result.
   *
   * @param targetCount the edge count of the graph that this mapping maps to
   * @return the inverse mapping, with an index count of {@code targetCount}
   * @throws IllegalArgumentException if {@code targetCount} is negative or larger than
   *                                  {@code Integer.MAX_VALUE - 8}, or two edges map to the same
   *                                  edge
   * @throws IndexOutOfBoundsException if an edge maps to {@code targetCount} or more
   */
  public EdgeMapping inverted(int targetCount) {
    return new EdgeMapping(invertArray(mapping, targetCount), mappedCount());
  }

  /**
   * Returns the mapping that applies {@code inner} first and then this mapping.
   * <p>
   * If {@code inner} maps the edges of a graph A to those of B, and this mapping maps B to C, the
   * result maps A to C. An edge is not mapped in the result if {@code inner} does not map it, or
   * if this mapping does not map the edge that {@code inner} maps it to.
   * <p>
   * <strong>Time:</strong> O(inner.indexCount()).<br>
   * <strong>Memory:</strong> 4 x inner.indexCount() bytes for the result.
   *
   * @param inner the mapping to apply first
   * @return the composed mapping
   * @throws IndexOutOfBoundsException if {@code inner} maps an edge to an edge index outside
   *                                   {@code 0..indexCount()-1}
   */
  public EdgeMapping compose(EdgeMapping inner) {
    int[] composed = composeArrays(this.mapping, inner.mapping);
    return new EdgeMapping(composed, countValid(composed));
  }

  /**
   * Returns {@code values} renumbered by this mapping: the value at position {@code i} of the
   * result is {@code values[get(i)]}, or {@code 0} if edge {@code i} is not mapped.
   * <p>
   * For a mapping from new to old edge indices, as the factories return it, this carries data on
   * the edges of the old graph over to the new graph. {@link #gather(Object[], Object[])} does the
   * same for objects.
   * <p>
   * <strong>Time:</strong> O(indexCount).<br>
   * <strong>Memory:</strong> 4 x indexCount bytes for the result.
   *
   * @param values the value of each edge of the graph that this mapping maps to
   * @return the renumbered values, of length {@link #indexCount()}
   * @throws IndexOutOfBoundsException if an edge maps to an edge index outside {@code values}
   */
  public int[] gather(int[] values) {
    return gatherArray(mapping, values);
  }

  /**
   * Returns {@code values} renumbered by this mapping, as {@link #gather(int[])} does.
   * <p>
   * <strong>Time:</strong> O(indexCount).<br>
   * <strong>Memory:</strong> 8 x indexCount bytes for the result.
   *
   * @param values the value of each edge of the graph that this mapping maps to
   * @return the renumbered values, of length {@link #indexCount()}
   * @throws IndexOutOfBoundsException if an edge maps to an edge index outside {@code values}
   */
  public long[] gather(long[] values) {
    return gatherArray(mapping, values);
  }

  /**
   * Returns {@code values} renumbered by this mapping, as {@link #gather(int[])} does.
   * <p>
   * <strong>Time:</strong> O(indexCount).<br>
   * <strong>Memory:</strong> 8 x indexCount bytes for the result.
   *
   * @param values the value of each edge of the graph that this mapping maps to
   * @return the renumbered values, of length {@link #indexCount()}
   * @throws IndexOutOfBoundsException if an edge maps to an edge index outside {@code values}
   */
  public double[] gather(double[] values) {
    return gatherArray(mapping, values);
  }

  /**
   * Writes {@code values} renumbered by this mapping into {@code newValues}, for objects.
   * <p>
   * {@code newValues[i]} becomes {@code values[get(i)]}, and stays as it is if edge {@code i} is
   * not mapped. References are copied, not the objects. If an exception is thrown,
   * {@code newValues} may be partly written.
   * <p>
   * <strong>Time:</strong> O(indexCount).
   *
   * @param <T>       the element type, nullable or not
   * @param values    the value of each edge of the graph that this mapping maps to
   * @param newValues receives the renumbered values; length {@link #indexCount()}, and not the
   *                  same array as {@code values}
   * @throws IllegalArgumentException if {@code newValues.length != indexCount()}, or both are the
   *                                  same array
   * @throws IndexOutOfBoundsException if an edge maps to an edge index outside {@code values}
   */
  public <T extends @Nullable Object> void gather(T[] values, T[] newValues) {
    gatherArray(mapping, values, newValues);
  }
}
