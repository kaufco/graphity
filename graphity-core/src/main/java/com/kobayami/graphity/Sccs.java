package com.kobayami.graphity;

import java.util.Objects;

/**
 * Strongly connected components of a directed {@link Graph}.
 * <p>
 * Each node belongs to exactly one SCC, identified by an SCC ID in {@code 0..groupCount()-1}.
 * Trivial SCCs (single node, no self-loop) are included — use {@link #sizeOf(int)} to
 * distinguish trivial from non-trivial components.
 * <p>
 * SCC IDs are assigned in <strong>reverse topological order</strong> of the component DAG:
 * the first completed SCC (a "sink" with no outgoing edges to other SCCs) gets SCC ID 0.
 * <p>
 * Extends {@link NodePartition}: use {@link #get(int)} for node → SCC ID,
 * {@link #nodesOf(int)} for SCC ID → nodes, {@link #groupCount()} for the number of SCCs.
 * <p>
 * Obtain via {@link Components#sccsOf(Graph)}:
 * <pre>{@code
 * var sccs = Components.sccsOf(graph);
 * sccs.groupCount();           // number of SCCs
 * var scc = sccs.get(node);    // SCC ID of this node
 * sccs.sizeOf(scc);            // number of nodes in this SCC
 * sccs.nodesOf(scc);           // sorted view of nodes in this SCC
 * sccs.containsCycle(scc);     // does this SCC contain a cycle?
 * Graph condensed = sccs.condense(); // condensation DAG
 * }</pre>
 *
 * <strong>Time:</strong> O(nodeCount + edgeCount) for computation.<br>
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
