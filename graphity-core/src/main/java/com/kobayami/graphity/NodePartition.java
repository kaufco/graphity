package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Bidirectional partition of nodes into disjoint groups.
 * <p>
 * Each node belongs to at most one group, identified by a group ID in
 * {@code 0..groupCount()-1}. The forward direction (node → group) is inherited
 * from {@link IndexMapping#get(int)}, with {@code -1} for a node in no group. The reverse
 * direction (group → nodes) is provided by {@link #nodesOf(int)}. Partitions from
 * {@link Components} cover every node; one built with {@link #of(Graph, int[], int)} may
 * leave nodes out, which {@link #isPartial()} reports.
 * <p>
 * Internally uses a CSR (Compressed Sparse Row) layout for the reverse direction,
 * enabling zero-copy {@link SortedIntView} access to group members.
 * <p>
 * Holds a reference to the original {@link Graph} from which the partition was computed.
 * This enables {@link #condense()} to build the condensation graph without requiring
 * the graph as an explicit parameter.
 * <p>
 * Implements {@link IndexPartition} for generic partition access via {@link #groupCount()},
 * {@link #indexCount()}, {@link #indicesOf(int)}, and {@link #sizeOf(int)}.
 * Domain-specific convenience methods ({@link #nodeCount()}, {@link #nodesOf(int)})
 * delegate to the generic interface.
 * <p>
 * Sealed to package-internal domain-specific partitions (e.g. {@link Sccs}); the
 * package-private constructor already restricts extension to this package, and
 * {@code permits} makes that explicit.
 * <p>
 * <strong>Memory:</strong> up to 4 x (2 x nodeCount + groupCount) bytes for the backing arrays
 * ({@code mapping}, {@link #members}, {@link #memberStart}), plus a shared reference to
 * the original {@link #graph}.
 *
 * <pre>{@code
 * int groupId = partition.get(node);
 * SortedIntView members = partition.nodesOf(groupId);
 * int groups = partition.groupCount();
 * Graph condensed = partition.condense();
 * }</pre>
 *
 * @see NodeMapping
 * @see IndexPartition
 * @see Sccs
 * @see Components
 */
public sealed class NodePartition extends IndexMapping implements IndexPartition permits Sccs {

  /** The graph from which this partition was computed. */
  final Graph graph;

  /** CSR values: node IDs of all group members, grouped by partition, sorted ascending within each group. */
  final int[] members;

  /** CSR offsets: group {@code g}'s members span {@code members[memberStart[g]..memberStart[g+1]-1]}. */
  final int[] memberStart;

  /**
   * Package-private constructor.
   *
   * @param graph       the graph from which this partition was computed
   * @param nodeToGroup mapping from node ID to group ID (no {@code -1} entries)
   * @param members     CSR values: group member node IDs, sorted ascending within each group
   * @param memberStart CSR offsets: per-group start indices into {@code members}
   */
  NodePartition(Graph graph, int[] nodeToGroup, int[] members, int[] memberStart) {
    this(graph, nodeToGroup, members, memberStart, members.length);
  }

  private NodePartition(
      Graph graph, int[] nodeToGroup, int[] members, int[] memberStart, int groupedCount) {
    super(nodeToGroup, groupedCount, groupedCount < nodeToGroup.length);
    this.graph = graph;
    this.members = members;
    this.memberStart = memberStart;
  }

  /**
   * Creates a partition of the graph's nodes into groups chosen by the caller.
   * <p>
   * {@code groupOf[node]} is the group of each node, in {@code 0..groupCount-1}, or {@code -1}
   * for a node in no group. Such nodes and their edges do not appear in {@link #condense()}.
   * Groups without members are allowed and become isolated nodes of the condensation, so group
   * IDs taken from an external numbering stay valid. The array is copied.
   * <p>
   * The partition then contracts the graph like one from {@link Components}:
   *
   * <pre>{@code
   * var partition = NodePartition.of(graph, packageOf, packageCount);
   * Graph packages = partition.condense();                                       // node IDs are group IDs
   * EdgePartition provenance = Graphs.edgePartition(packages, graph, partition); // edges behind each package edge
   * }</pre>
   *
   * <strong>Time:</strong> O(nodeCount + groupCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + groupedNodeCount + groupCount + 1) bytes for the
   * result; 4 x groupCount bytes additional during construction.
   *
   * @param graph      the graph whose nodes are partitioned
   * @param groupOf    group ID of each node, or {@code -1}; length {@code graph.nodeCount()}
   * @param groupCount number of groups, in {@code 0..Integer.MAX_VALUE-1}
   * @return the partition
   * @throws IllegalArgumentException if {@code groupOf.length != graph.nodeCount()},
   *                                  {@code groupCount} is out of range, or an entry lies
   *                                  outside {@code -1..groupCount-1}
   */
  public static NodePartition of(Graph graph, int[] groupOf, int groupCount) {
    Objects.requireNonNull(graph, "graph");
    Objects.requireNonNull(groupOf, "groupOf");
    int n = graph.nodeCount();
    if (groupOf.length != n) {
      throw new IllegalArgumentException(
          "groupOf has length " + groupOf.length + ", but the graph has " + n + " nodes");
    }
    if (groupCount < 0 || groupCount == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("groupCount out of range: " + groupCount);
    }
    var nodeToGroup = groupOf.clone();
    var memberStart = new int[groupCount + 1];
    for (int node = 0; node < n; node++) {
      int group = nodeToGroup[node];
      if (group < -1 || group >= groupCount) {
        throw new IllegalArgumentException("group of node " + node + " is " + group
            + ", outside -1.." + (groupCount - 1));
      }
      memberStart[group + 1]++;
    }
    // memberStart[g + 1] now holds the size of group g, memberStart[0] the nodes in no group.
    int grouped = n - memberStart[0];
    memberStart[0] = 0;
    for (int g = 0; g < groupCount; g++) {
      memberStart[g + 1] += memberStart[g];
    }
    var members = new int[grouped];
    var write = Arrays.copyOf(memberStart, groupCount);
    for (int node = 0; node < n; node++) {
      int group = nodeToGroup[node];
      if (group >= 0) members[write[group]++] = node;
    }
    return new NodePartition(graph, nodeToGroup, members, memberStart, grouped);
  }

  /**
   * Returns the number of groups (partitions).
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public int groupCount() {
    return memberStart.length - 1;
  }

  /**
   * Returns the number of nodes of the partitioned graph, including nodes in no group.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node count of the partitioned graph
   */
  @Override
  public int indexCount() {
    return mapping.length;
  }

  /**
   * Returns the indices in the given group as a sorted, zero-copy view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return indices in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  @Override
  public SortedIntView indicesOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return SortedIntView.viewOf(members, memberStart[groupId], memberStart[groupId + 1]);
  }

  /**
   * Returns the number of indices in the given group.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return number of indices in this group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  @Override
  public int sizeOf(int groupId) {
    Objects.checkIndex(groupId, groupCount());
    return memberStart[groupId + 1] - memberStart[groupId];
  }

  /**
   * Returns the number of nodes of the partitioned graph, including nodes in no group.
   * Equivalent to {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node count of the partitioned graph
   */
  public int nodeCount() {
    return indexCount();
  }

  /**
   * Returns the nodes in the given group as a sorted, zero-copy view.
   * Equivalent to {@link #indicesOf(int)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return nodes in this group, sorted ascending
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public SortedIntView nodesOf(int groupId) {
    return indicesOf(groupId);
  }

  /**
   * Builds the condensation graph from this partition.
   * <p>
   * Each group becomes a single node in the condensation graph. An edge exists from group
   * {@code g} to group {@code h} if any node in {@code g} has an out-edge to any node in
   * {@code h} in the original graph. Self-edges (intra-group edges) are omitted, and so are
   * edges from or to a node in no group. Duplicate edges are removed; adjacency lists are
   * sorted ascending.
   * <p>
   * The condensation graph has {@code groupCount()} nodes, where node IDs correspond to group IDs.
   * Use {@code get(originalNode)} to map an original node to its condensation node ID,
   * and {@code nodesOf(condensationNode)} to retrieve all original nodes within a
   * condensation node.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + condensationEdgeCount x log d̄) where d̄
   * is the average out-degree in the condensation (per-group adjacency sort).<br>
   * <strong>Memory:</strong> O(groupCount + condensationEdgeCount) for the result;
   * O(groupCount) additional during computation.
   *
   * @return the condensation graph
   */
  public Graph condense() {
    int groups = groupCount();
    if (groups == 0) {
      return new Graph(new int[]{0}, new int[0]);
    }

    var nodeToGroup = mapping;
    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;

    var dagStart = new int[groups + 1];
    var dagAdj = new IntArrayList();
    // Own bit array rather than java.util.BitSet: clearing single bits there triggers
    // recalculateWordsInUse(), which scans back for the highest set word and would
    // reintroduce the very cost this avoids.
    var seen = new long[(groups >> 6) + 1];

    for (int g = 0; g < groups; g++) {
      int groupStart = dagAdj.size();
      dagStart[g] = groupStart;

      int nodesEnd = memberStart[g + 1];
      for (int ni = memberStart[g]; ni < nodesEnd; ni++) {
        int u = members[ni];
        int edgeEnd = adjacencyStart[u + 1];
        for (int ei = adjacencyStart[u]; ei < edgeEnd; ei++) {
          int tgtGroup = nodeToGroup[adjacency[ei]];
          if (tgtGroup != g && tgtGroup >= 0 && (seen[tgtGroup >>> 6] & (1L << tgtGroup)) == 0) {
            seen[tgtGroup >>> 6] |= 1L << tgtGroup;
            dagAdj.add(tgtGroup);
          }
        }
      }

      // The bits set above are exactly the group's emitted targets, so resetting them
      // costs one write per emitted edge instead of one per word up to the highest ID
      // ever marked. Zeroing the whole word is safe: every set bit in it belongs to
      // this group and is being cleared anyway.
      int groupEnd = dagAdj.size();
      int[] elements = dagAdj.elements();
      for (int i = groupStart; i < groupEnd; i++) {
        seen[elements[i] >>> 6] = 0;
      }

      Arrays.sort(elements, groupStart, groupEnd);
    }
    dagStart[groups] = dagAdj.size();

    return new Graph(dagStart, dagAdj.toIntArray());
  }

  /**
   * Folds per-node values into one value per group: for each group, starting from
   * {@code identity}, {@code combine} is applied to the values of its members in ascending
   * member order. A group without members gets {@code identity}; nodes in no group are ignored.
   *
   * <pre>{@code
   * int[] perGroup = partition.reduce(values, 0, Integer::sum);
   * }</pre>
   *
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 4 x groupCount bytes for the result.
   *
   * @param values   values indexed by node, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public int[] reduce(int[] values, int identity, IntBinaryOperator combine) {
    return reduce(members, memberStart, indexCount(), values, identity, combine);
  }

  /**
   * Folds per-node values into one value per group, as {@link #reduce(int[], int,
   * IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   values indexed by node, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public long[] reduce(long[] values, long identity, LongBinaryOperator combine) {
    return reduce(members, memberStart, indexCount(), values, identity, combine);
  }

  /**
   * Folds per-node values into one value per group, as {@link #reduce(int[], int,
   * IntBinaryOperator)} does.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).<br>
   * <strong>Memory:</strong> 8 x groupCount bytes for the result.
   *
   * @param values   values indexed by node, of length {@link #indexCount()}
   * @param identity start value of every group
   * @param combine  folds the accumulated value with the next member's value
   * @return one value per group, of length {@link #groupCount()}
   * @throws IllegalArgumentException if {@code values.length != indexCount()}
   */
  public double[] reduce(double[] values, double identity, DoubleBinaryOperator combine) {
    return reduce(members, memberStart, indexCount(), values, identity, combine);
  }

  /**
   * Folds per-node objects into one object per group, written into an array the caller
   * allocated: for each group with members, the fold starts from its first member's value and
   * applies {@code combine} with each further member in ascending member order. A group without
   * members keeps its entry in {@code groupValues}; nodes in no group are ignored.
   * The two arrays must be distinct.
   * <p>
   * <strong>Time:</strong> O(indexCount + groupCount).
   *
   * @param <T>         element type
   * @param values      values indexed by node, of length {@link #indexCount()}
   * @param groupValues receives one value per group; length {@link #groupCount()}
   * @param combine     folds the accumulated value with the next member's value
   * @throws IllegalArgumentException if {@code values.length != indexCount()} or
   *                                  {@code groupValues.length != groupCount()}
   */
  public <T> void reduce(T[] values, T[] groupValues, BinaryOperator<T> combine) {
    reduce(members, memberStart, indexCount(), values, groupValues, combine);
  }
}
