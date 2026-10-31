package com.kobayami.graphity;

import java.util.Objects;

/**
 * Strongly connected components of a directed {@link Graph}.
 * <p>
 * Each node belongs to exactly one SCC, identified by an SCC ID in {@code 0..groupCount()-1}.
 * Trivial SCCs (single node, no self-loop) are included - use {@link #containsCycle(int)} to
 * distinguish trivial from non-trivial components.
 * <p>
 * SCC IDs are assigned in <strong>reverse topological order</strong> of the component DAG:
 * the first completed SCC (a "sink" with no outgoing edges to other SCCs) gets SCC ID 0.
 * <p>
 * Extends {@link NodePartition}: use {@link #sccOf(int)} for node → SCC ID,
 * {@link #nodesOf(int)} for SCC ID → nodes, {@link #sccCount()} for the number of SCCs. The
 * generic {@link #get(int)} and {@link #groupCount()} answer the same.
 * <p>
 * Obtain via {@link Components#sccsOf(Graph)}:
 * <pre>{@code
 * var sccs = Components.sccsOf(graph);
 * sccs.sccCount();             // number of SCCs
 * var scc = sccs.sccOf(node);  // SCC ID of this node
 * sccs.sizeOf(scc);            // number of nodes in this SCC
 * sccs.nodesOf(scc);           // sorted view of nodes in this SCC
 * sccs.containsCycle(scc);     // does this SCC contain a cycle?
 * Graph condensed = sccs.condense(); // condensation DAG
 * }</pre>
 *
 * <strong>Time:</strong> O(nodeCount + edgeCount) for computation, plus sorting the members of
 * each component: O(nodeCount x log nodeCount) in the worst case, as for
 * {@link Components#sccsOf(Graph)}.<br>
 * <strong>Memory:</strong> O(nodeCount) for the result; O(nodeCount) additional during computation.
 *
 * @see Graph
 * @see NodePartition
 * @see Components
 */
public final class Sccs extends NodePartition {

  Sccs(Graph graph, int[] nodeToScc, int[] sccNodes, int[] sccStart) {
    super(graph, nodeToScc, sccNodes, sccStart);
  }

  /**
   * Returns the SCC ID of the given node. Equivalent to {@link #get(int)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return SCC ID in {@code 0..sccCount()-1}
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
   * @return number of SCCs
   */
  public int sccCount() {
    return groupCount();
  }

  /**
   * Returns whether the given SCC contains a cycle.
   * An SCC is cyclic if it has more than one node, or if its single node has a self-loop.
   * <p>
   * <strong>Time:</strong> O(1) for multi-node SCCs; O(log outDegree) for single-node SCCs
   * (self-loop check via binary search).
   *
   * @param sccId SCC ID in {@code 0..groupCount()-1}
   * @return {@code true} if this SCC contains a cycle
   * @throws IndexOutOfBoundsException if {@code sccId} is out of range
   */
  public boolean containsCycle(int sccId) {
    Objects.checkIndex(sccId, groupCount());
    return memberStart[sccId + 1] - memberStart[sccId] > 1
        || graph.containsSelfLoop(members[memberStart[sccId]]);
  }

  /**
   * Returns whether the graph is strongly connected (consists of at most one SCC).
   * An empty graph (0 nodes) is considered strongly connected (vacuously true).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if there is at most one SCC
   */
  public boolean isStronglyConnected() {
    return groupCount() <= 1;
  }
}
