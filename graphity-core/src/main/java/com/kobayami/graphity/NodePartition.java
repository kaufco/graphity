package com.kobayami.graphity;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Bidirectional partition of nodes into disjoint groups.
 * <p>
 * Every node belongs to exactly one group, identified by a group ID in
 * {@code 0..groupCount()-1}. The forward direction (node → group) is inherited
 * from {@link IndexMapping#get(int)}. The reverse direction (group → nodes) is
 * provided by {@link #nodesOf(int)}.
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
 * Extend this class for domain-specific partitions (e.g. {@link Sccs}).
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
public non-sealed class NodePartition extends NodeMapping implements IndexPartition {

  /** The graph from which this partition was computed. */
  final Graph graph;

  /** CSR values: node IDs of all group members, grouped by partition. */
  final int[] members;

  /** CSR offsets: group {@code g}'s members span {@code members[memberStart[g]..memberStart[g+1]-1]}. */
  final int[] memberStart;

  /**
   * Package-private constructor.
   *
   * @param graph       the graph from which this partition was computed
   * @param nodeToGroup mapping from node ID to group ID (no {@code -1} entries)
   * @param members     CSR values: group member node IDs
   * @param memberStart CSR offsets: per-group start indices into {@code members}
   */
  NodePartition(Graph graph, int[] nodeToGroup, int[] members, int[] memberStart) {
    super(nodeToGroup, nodeToGroup.length, false);
    this.graph = graph;
    this.members = members;
    this.memberStart = memberStart;
  }

  // ---- IndexPartition implementation ----

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
   * Returns the total number of nodes in the partition.
   * <p>
   * <strong>Time:</strong> O(1).
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

  // ---- Domain-specific convenience (delegate to IndexPartition) ----

  /**
   * Returns the total number of nodes in the partition.
   * Equivalent to {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node count, summed over all groups
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

  // ---- Condensation ----

  /**
   * Builds the condensation graph from this partition.
   * <p>
   * Each group becomes a single node in the condensation graph. An edge exists from group
   * {@code g} to group {@code h} if any node in {@code g} has an out-edge to any node in
   * {@code h} in the original graph. Self-edges (intra-group edges) are omitted.
   * Duplicate edges are removed; adjacency lists are sorted ascending.
   * <p>
   * The condensation graph has {@code groupCount()} nodes, where node IDs correspond to group IDs.
   * Use {@code get(originalNode)} to map an original node to its condensation node ID,
   * and {@code nodesOf(condensationNode)} to retrieve all original nodes within a
   * condensation node.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
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
    var seen = new BitSet(groups);

    for (int g = 0; g < groups; g++) {
      dagStart[g] = dagAdj.size();
      seen.clear();

      int nodesEnd = memberStart[g + 1];
      for (int ni = memberStart[g]; ni < nodesEnd; ni++) {
        int u = members[ni];
        int edgeEnd = adjacencyStart[u + 1];
        for (int ei = adjacencyStart[u]; ei < edgeEnd; ei++) {
          int tgtGroup = nodeToGroup[adjacency[ei]];
          if (tgtGroup != g && !seen.get(tgtGroup)) {
            seen.set(tgtGroup);
            dagAdj.add(tgtGroup);
          }
        }
      }

      Arrays.sort(dagAdj.elements(), dagStart[g], dagAdj.size());
    }
    dagStart[groups] = dagAdj.size();

    return new Graph(dagStart, dagAdj.toIntArray());
  }

  /**
   * Always throws — a partition mapping is many-to-one and cannot be inverted
   * to a {@link NodeMapping}. Use {@link #nodesOf(int)} for the reverse direction.
   *
   * @throws IllegalArgumentException always
   */
  @Override
  public NodeMapping inverse() {
    throw new IllegalArgumentException(
        "Cannot invert a partition mapping (many-to-one). Use nodesOf() for the reverse direction.");
  }
}
