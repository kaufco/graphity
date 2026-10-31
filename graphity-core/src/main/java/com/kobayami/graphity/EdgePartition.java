package com.kobayami.graphity;

/**
 * A partition of the edges of a graph into groups: the edges behind each edge of a condensed
 * graph, or the blocks of a graph.
 * <p>
 * {@link #get(int) get(e)} returns the group of edge {@code e}, or {@code -1} if it is in no
 * group, and {@link #edgesOf(int)} the edge indices of a group in ascending order. The groups
 * come from one of two methods:
 * <ul>
 *   <li>{@link Graphs#edgePartition(Graph, NodePartition)} groups the edges of the graph of a
 *       {@link NodePartition} by the edge of its condensed graph that they condense to: the group
 *       IDs are the edge indices of the condensed graph. Edges within a group of nodes, and edges
 *       from or to a node in no group, condense to no edge and are in no group.</li>
 *   <li>{@link Biconnectivity#blocks()} groups the edges by block (biconnected component);
 *       self-loops are in no block.</li>
 * </ul>
 * Here, {@code byPackage} groups the methods of a program by package. Its condensed graph
 * {@code packageCalls} holds the calls between packages (see
 * {@link NodePartition#of(Graph, int[], int)}). {@code byPackageCall} tells which calls between
 * methods are behind the call from {@code service} to {@code model}:
 * {@snippet file="com/kobayami/graphity/examples/Partitions.java" region="edgesOf"}
 * {@code reduce} combines data on the edges into one value per group, such as the number of call
 * sites behind each call between packages.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x (edgeCount + mappedCount + groupCount) bytes, where mappedCount is
 * the number of edges that belong to a group.
 */
public final class EdgePartition extends IndexPartition {

  // Takes the arrays as they are, neither copied nor checked: the caller guarantees the layout of
  // IndexPartition.
  EdgePartition(int[] mapping, int[] members, int[] memberStart) {
    super(mapping, members, memberStart);
  }

  /**
   * Returns the number of edges of the partitioned graph, in a group or not. Equivalent to
   * {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return edge count of the partitioned graph
   */
  public int edgeCount() {
    return indexCount();
  }

  /**
   * Returns the edge indices in group {@code groupId}, in ascending order. Equivalent to
   * {@link #indicesOf(int) indicesOf(groupId)}.
   * <p>
   * The result is a view of this partition's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return edge indices in the group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public SortedIntView edgesOf(int groupId) {
    return indicesOf(groupId);
  }
}
