package com.kobayami.graphity;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * Bidirectional partition of edges into disjoint groups, with efficient reverse lookup.
 * <p>
 * Used wherever several edges belong to one group. In a condensation the group is a condensed
 * edge, and multiple original edges map to it; in {@link Biconnectivity#blocks()} the group is
 * a biconnected component. Edges that belong to no group are unmapped
 * ({@code get(edgeIndex) == -1}) - intra-group edges in a condensation, self-loops in a
 * biconnectivity result.
 * <p>
 * The forward direction (edge → group) is inherited from {@link IndexMapping#get(int)}. The
 * reverse direction (group → edges) is provided by {@link #edgesOf(int)}.
 * <p>
 * Implements {@link IndexPartition} for generic partition access via {@link #groupCount()},
 * {@link #indexCount()}, {@link #indicesOf(int)}, and {@link #sizeOf(int)}.
 * Domain-specific convenience methods ({@link #edgeCount()}, {@link #edgesOf(int)})
 * delegate to the generic interface.
 * <p>
 * Instances are created via {@link Graphs#edgePartition(Graph, Graph, NodePartition)} and
 * {@link Biconnectivity#blocks()}.
 *
 * <pre>{@code
 * EdgePartition ep = Graphs.edgePartition(condensed, original, partition);
 * SortedIntView origEdges = ep.edgesOf(condensedEdge);         // edges behind one condensed edge
 * double[] weights = ep.reduce(originalWeights, 0.0, Double::sum);  // one value per condensed edge
 * }</pre>
 *
 * @see EdgeMapping
 * @see IndexPartition
 * @see Graphs#edgePartition(Graph, Graph, NodePartition)
 * @see Biconnectivity#blocks()
 */
public final class EdgePartition extends IndexMapping implements IndexPartition {

  /** CSR values: original edge indices of all group members, grouped by group ID. */
  private final int[] members;

  /** CSR offsets: group {@code g}'s members span {@code members[memberStart[g]..memberStart[g+1]-1]}. */
  private final int[] memberStart;

  /**
   * Package-private constructor.
   *
   * @param mapping     forward mapping: original edge index → group ID, or -1
   * @param validCount  number of non-negative entries in mapping
   * @param partial     whether mapping contains -1 entries
   * @param members     CSR values: original edge indices per group
   * @param memberStart CSR offsets: per-group start indices into members
   */
  EdgePartition(int[] mapping, int validCount, boolean partial, int[] members, int[] memberStart) {
    super(mapping, validCount, partial);
    this.members = members;
    this.memberStart = memberStart;
  }

  /**
   * Returns the number of groups (condensed edges, or blocks).
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public int groupCount() {
    return memberStart.length - 1;
  }

  /**
   * Returns the total number of original edges.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public int indexCount() {
    return mapping.length;
  }

  /**
   * Returns the original edge indices in the given group as a sorted, zero-copy view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID (condensed edge index, or block ID) in {@code 0..groupCount()-1}
   * @return original edge indices in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  @Override
  public SortedIntView indicesOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return SortedIntView.viewOf(members, memberStart[groupId], memberStart[groupId + 1]);
  }

  /**
   * Returns the number of original edges in the given group.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID (condensed edge index, or block ID) in {@code 0..groupCount()-1}
   * @return number of original edges in this group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  @Override
  public int sizeOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return memberStart[groupId + 1] - memberStart[groupId];
  }

  /**
   * Returns the total number of original edges.
   * Equivalent to {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return original edge count
   */
  public int edgeCount() {
    return indexCount();
  }

  /**
   * Returns the edge indices in the given group as a sorted, zero-copy view.
   * Equivalent to {@link #indicesOf(int)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID (condensed edge index, or block ID) in {@code 0..groupCount()-1}
   * @return edge indices in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public SortedIntView edgesOf(int groupId) {
    return indicesOf(groupId);
  }

  /**
   * Folds per-edge values into one value per group: for each group, starting from
   * {@code identity}, {@code combine} is applied to the values of its members in ascending
   * member order. A group without members gets {@code identity}; edges in no group are ignored.
   *
   * <pre>{@code
   * int[] perGroup = partition.reduce(values, 0, Integer::sum);
   * }</pre>
   *
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 4 x groupCount bytes for the result.
   *
   * @param values   values indexed by edge, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public int[] reduce(int[] values, int identity, IntBinaryOperator combine) {
    return reduce(members, memberStart, indexCount(), values, identity, combine);
  }

  /**
   * Folds per-edge values into one value per group, as {@link #reduce(int[], int,
   * IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   values indexed by edge, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public long[] reduce(long[] values, long identity, LongBinaryOperator combine) {
    return reduce(members, memberStart, indexCount(), values, identity, combine);
  }

  /**
   * Folds per-edge values into one value per group, as {@link #reduce(int[], int,
   * IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   values indexed by edge, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public double[] reduce(double[] values, double identity, DoubleBinaryOperator combine) {
    return reduce(members, memberStart, indexCount(), values, identity, combine);
  }

  /**
   * Folds per-edge objects into one object per group, written into an array the caller
   * allocated: for each group with members, the fold starts from its first member's value and
   * applies {@code combine} with each further member in ascending member order. A group without
   * members keeps its entry in {@code groupValues}; edges in no group are ignored.
   * The two arrays must be distinct.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).
   *
   * @param <T>         element type
   * @param values      values indexed by edge, of length {@link #indexCount()}
   * @param groupValues receives one value per group; length {@link #groupCount()}
   * @param combine     folds the accumulated value with the next member's value
   * @throws IllegalArgumentException if {@code values.length != indexCount()} or
   *                                  {@code groupValues.length != groupCount()}
   */
  public <T> void reduce(T[] values, T[] groupValues, BinaryOperator<T> combine) {
    reduce(members, memberStart, indexCount(), values, groupValues, combine);
  }
}
