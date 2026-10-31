package com.kobayami.graphity;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Bidirectional partition of nodes into disjoint groups.
 * <p>
 * Each node belongs to at most one group, identified by a group ID in
 * {@code 0..groupCount()-1}. The forward direction (node → group) is
 * {@link #get(int)}, with {@code -1} for a node in no group. The reverse
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
 * Extends {@link IndexPartition} for generic partition access via {@link #groupCount()},
 * {@link #indexCount()}, {@link #indicesOf(int)}, {@link #sizeOf(int)}, and the
 * {@code reduce} methods. Domain-specific convenience methods ({@link #nodeCount()},
 * {@link #nodesOf(int)}) are aliases of the generic ones.
 * <p>
 * Sealed to package-internal domain-specific partitions (e.g. {@link Sccs}); the
 * package-private constructor already restricts extension to this package, and
 * {@code permits} makes that explicit.
 * <p>
 * <strong>Memory:</strong> up to 4 x (2 x nodeCount + groupCount + 1) bytes for the backing
 * arrays ({@code mapping}, {@link #members}, {@link #memberStart}), plus a shared reference to
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
public sealed class NodePartition extends IndexPartition permits Sccs {

  /** The graph from which this partition was computed. */
  final Graph graph;

  /**
   * Package-private constructor.
   *
   * @param graph       the graph from which this partition was computed
   * @param nodeToGroup mapping from node ID to group ID, or {@code -1} for a node in no group
   * @param members     CSR values: group member node IDs, sorted ascending within each group
   * @param memberStart CSR offsets: per-group start indices into {@code members}
   */
  NodePartition(Graph graph, int[] nodeToGroup, int[] members, int[] memberStart) {
    super(nodeToGroup, members, memberStart);
    this.graph = graph;
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
   * Graph packages = partition.condense();                                // node IDs are group IDs
   * EdgePartition provenance = Graphs.edgePartition(packages, partition); // edges behind each package edge
   * }</pre>
   *
   * <strong>Time:</strong> O(nodeCount + groupCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + groupedNodeCount + groupCount + 1) bytes for the
   * result.
   *
   * @param graph      the graph whose nodes are partitioned
   * @param groupOf    group ID of each node, or {@code -1}; length {@code graph.nodeCount()}
   * @param groupCount number of groups, in {@code 0..Integer.MAX_VALUE-9}, as many as a graph
   *                   can have nodes: each group becomes a node of {@link #condense()}
   * @return the partition
   * @throws IllegalArgumentException if {@code groupOf.length != graph.nodeCount()},
   *                                  {@code groupCount} is out of range, or an entry lies
   *                                  outside {@code -1..groupCount-1}
   */
  public static NodePartition of(Graph graph, int[] groupOf, int groupCount) {
    int n = graph.nodeCount();
    checkLength(groupOf.length, n, "groupOf");
    if (groupCount < 0 || groupCount > ArrayLimits.MAX_NODE_COUNT) {
      throw new IllegalArgumentException("groupCount out of range: " + groupCount);
    }
    var nodeToGroup = groupOf.clone();
    // Counted here rather than in ParentForest.groupIndices, so that the range check needs no
    // pass of its own.
    var memberStart = new int[groupCount + 1];
    for (int node = 0; node < n; node++) {
      int group = nodeToGroup[node];
      if (group < -1 || group >= groupCount) {
        throw new IndexOutOfBoundsException("group of node " + node + " is " + group
            + ", outside -1.." + (groupCount - 1));
      }
      if (group >= 0) memberStart[group]++;
    }
    int[] members = ParentForest.groupCounted(nodeToGroup, memberStart);
    return new NodePartition(graph, nodeToGroup, members, memberStart);
  }

  /**
   * Returns the number of nodes of the partitioned graph, including nodes in no group.
   * Equivalent to {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node count of the partitioned graph
   */
  public final int nodeCount() {
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
  public final SortedIntView nodesOf(int groupId) {
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
   * <strong>Time:</strong> O(nodeCount + groupCount + edgeCount + Σ d x log d), where d is the
   * out-degree of each group in the condensation (one sort per group).<br>
   * <strong>Memory:</strong> O(groupCount + condensationEdgeCount) for the result;
   * O(groupCount) additional during computation.
   *
   * @return the condensation graph
   */
  public final Graph condense() {
    int groups = groupCount();
    var nodeToGroup = mapping;
    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);

    var condensedStart = new int[groups + 1];
    var condensedTargets = new IntArrayList();
    // Own bit array rather than java.util.BitSet: clearing single bits there triggers
    // recalculateWordsInUse(), which scans back for the highest set word and would
    // reintroduce the very cost this avoids.
    var seen = new long[(groups >> 6) + 1];

    for (int g = 0; g < groups; g++) {
      int groupStart = condensedTargets.size();
      condensedStart[g] = groupStart;

      int nodesEnd = memberStart[g + 1];
      for (int ni = memberStart[g]; ni < nodesEnd; ni++) {
        int u = members[ni];
        int edgeEnd = adjacencyStart[u + 1];
        for (int ei = adjacencyStart[u]; ei < edgeEnd; ei++) {
          int tgtGroup = nodeToGroup[adjacency[ei]];
          if (tgtGroup != g && tgtGroup >= 0 && (seen[tgtGroup >>> 6] & (1L << tgtGroup)) == 0) {
            seen[tgtGroup >>> 6] |= 1L << tgtGroup;
            condensedTargets.add(tgtGroup);
          }
        }
      }

      // The bits set above are exactly the group's emitted targets, so resetting them
      // costs one write per emitted edge instead of one per word up to the highest ID
      // ever marked. Zeroing the whole word is safe: every set bit in it belongs to
      // this group and is being cleared anyway.
      int groupEnd = condensedTargets.size();
      int[] elements = condensedTargets.elements();
      for (int i = groupStart; i < groupEnd; i++) {
        seen[elements[i] >>> 6] = 0;
      }

      Arrays.sort(elements, groupStart, groupEnd);
    }
    condensedStart[groups] = condensedTargets.size();

    return new Graph(condensedStart, condensedTargets.toIntArray());
  }
}
