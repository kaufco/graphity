package com.kobayami.graphity;

import java.util.Objects;

/**
 * The SCCs (strongly connected components) of a graph, as {@link Components#sccsOf(Graph)}
 * returns them.
 * <p>
 * It is a {@link NodePartition} whose groups are the SCCs, numbered by SCC ID
 * {@code 0..sccCount()-1}. {@link #sccOf(int)} returns the SCC of a node and {@link #nodesOf(int)}
 * the nodes of an SCC. {@link #containsCycle(int)} distinguishes the SCCs with a cycle from those
 * of a single node without one. {@link #condense()} returns the DAG (directed acyclic graph) of
 * the SCCs.
 * <p>
 * Every node is in exactly one SCC; a node that lies on no cycle is an SCC of its own. SCC IDs are
 * in reverse topological order: every edge between two SCCs goes from the higher SCC ID to the
 * lower one, so SCC {@code 0} has no edges to other SCCs. The nodes of each SCC are in ascending
 * order. Here, {@code dependencies} are the dependencies between the packages of a program, and
 * {@code names} holds the names of the packages. The example groups the packages by SCC, in the
 * order of the SCC IDs. In this order, each package comes after the packages it uses, and the
 * packages that use each other come together:
 * {@snippet file="com/kobayami/graphity/examples/Connectivity.java" region="sccsOf"}
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x (2 x nodeCount + sccCount) bytes; the graph is shared, not
 * copied.
 */
public final class Sccs extends NodePartition {

  Sccs(Graph graph, int[] nodeToScc, int[] sccNodes, int[] sccStart) {
    super(graph, nodeToScc, sccNodes, sccStart);
  }

  /**
   * Returns the SCC ID of {@code node}. Equivalent to {@link #get(int) get(node)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return SCC ID
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int sccOf(int node) {
    return get(node);
  }

  /**
   * Returns the number of SCCs. Equivalent to {@link #groupCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return SCC count
   */
  public int sccCount() {
    return groupCount();
  }

  /**
   * Returns whether SCC {@code sccId} contains a cycle: whether it has more than one node, or a
   * single node with a self-loop.
   * <p>
   * <strong>Time:</strong> O(1) for an SCC of more than one node, O(log outDegree(v)) for an SCC
   * of a single node {@code v}.
   *
   * @param sccId SCC ID in {@code 0..sccCount()-1}
   * @return {@code true} if the SCC contains a cycle
   * @throws IndexOutOfBoundsException if {@code sccId} is out of range
   */
  public boolean containsCycle(int sccId) {
    Objects.checkIndex(sccId, groupCount());
    return memberStart[sccId + 1] - memberStart[sccId] > 1
        || graph.containsSelfLoop(members[memberStart[sccId]]);
  }

  /**
   * Returns whether the graph is strongly connected: whether it has at most one SCC. Equivalent to
   * {@code sccCount() <= 1}.
   * <p>
   * A graph without nodes counts as strongly connected.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if there is at most one SCC
   */
  public boolean isStronglyConnected() {
    return groupCount() <= 1;
  }
}
