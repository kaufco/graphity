package com.kobayami.graphity;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * A partition of the nodes of a graph into groups, such as its components.
 * <p>
 * {@link #get(int) get(node)} returns the group of {@code node}, or {@code -1} if it is in no
 * group, and {@link #nodesOf(int)} the nodes of a group in ascending order. {@link Components}
 * returns partitions into components, in which every node is in exactly one group: an
 * {@link Sccs} for the strongly connected components, a plain {@code NodePartition} for the weakly
 * connected ones.
 * {@link #of(Graph, int[], int)} takes groups of the caller's choice, which may leave nodes out.
 * <p>
 * A partition holds the graph whose nodes it partitions. {@link #condense()} builds the condensed
 * graph, with a node for each group and an edge between two groups wherever an edge of the graph
 * runs between them. {@link Graphs#edgePartition(Graph, NodePartition)} builds the
 * {@link EdgePartition} that tells which edges of the graph are behind each edge of the condensed
 * graph. The methods for groups of any kind, such as {@link #groupCount()}, {@link #sizeOf(int)}
 * and {@code reduce}, come from {@link IndexPartition}.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x (nodeCount + mappedCount + groupCount) bytes, where mappedCount is
 * the number of nodes that belong to a group; the graph is shared, not copied.
 */
public sealed class NodePartition extends IndexPartition permits Sccs {

  // The partitioned graph, for condense() and Graphs.edgePartition: neither takes it as a
  // parameter, so a wrong one cannot be passed.
  final Graph graph;

  // Takes the arrays as they are, neither copied nor checked: the caller guarantees the layout of
  // IndexPartition.
  NodePartition(Graph graph, int[] nodeToGroup, int[] members, int[] memberStart) {
    super(nodeToGroup, members, memberStart);
    this.graph = graph;
  }

  /**
   * Returns a partition of the nodes of {@code graph} into groups of the caller's choice.
   * <p>
   * {@code groupOf[node]} is the group of each node, in {@code 0..groupCount-1}, or {@code -1}
   * for a node in no group; the array is copied. A group may have no nodes: it becomes a node
   * without edges in {@link #condense()}, so that group IDs from a numbering of the caller stay
   * valid. Here, {@code packageOf} holds the package of each method of {@code calls} as a node ID
   * of the package graph {@code dependencies}, and the condensed graph shows which packages call
   * which:
   * {@snippet file="com/kobayami/graphity/examples/Partitions.java" region="of"}
   * <p>
   * <strong>Time:</strong> O(nodeCount + groupCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + mappedCount + groupCount) bytes for the result.
   *
   * @param graph      the graph whose nodes are partitioned
   * @param groupOf    the group of each node, or {@code -1}; length {@code graph.nodeCount()}
   * @param groupCount the number of groups, at most as many as a graph can have nodes
   * @return the partition
   * @throws IndexOutOfBoundsException if a group is not in {@code -1..groupCount-1}
   * @throws IllegalArgumentException  if {@code groupOf.length != graph.nodeCount()}, or
   *                                   {@code groupCount} is negative or larger than a graph can
   *                                   have nodes
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
   * Returns the number of nodes of the partitioned graph, in a group or not. Equivalent to
   * {@link #indexCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node count of the partitioned graph
   */
  public final int nodeCount() {
    return indexCount();
  }

  /**
   * Returns the nodes in group {@code groupId}, in ascending order. Equivalent to
   * {@link #indicesOf(int) indicesOf(groupId)}.
   * <p>
   * The result is a view of this partition's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param groupId group ID in {@code 0..groupCount()-1}
   * @return nodes in the group
   * @throws IndexOutOfBoundsException if {@code groupId} is out of range
   */
  public final SortedIntView nodesOf(int groupId) {
    return indicesOf(groupId);
  }

  /**
   * Returns the condensed graph, with a node for each group and an edge between two groups
   * wherever an edge of the partitioned graph runs between them.
   * <p>
   * The node IDs of the condensed graph are the group IDs. It has the edge {@code g -> h} if a
   * node in group {@code g} has an out-node in group {@code h} and {@code g != h}. Edges within a
   * group and edges from or to a node in no group are left out. The edges from one group to
   * another become a single edge. {@link Graphs#edgePartition(Graph, NodePartition)} tells which
   * edges are behind each edge of the condensed graph. For an {@link Sccs}, the condensed graph is
   * acyclic.
   * <p>
   * The condensed graph is built anew on every call, not cached: keep the result to use it more
   * than once.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + groupCount), plus O(d log d) for each group
   * with d out-nodes in the result.<br>
   * <strong>Memory:</strong> 4 x (groupCount + condensedEdgeCount) bytes for the result, where
   * condensedEdgeCount is its edge count; while it runs, 1 bit per group and up to
   * 6 x condensedEdgeCount bytes more.
   *
   * @return the condensed graph
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
