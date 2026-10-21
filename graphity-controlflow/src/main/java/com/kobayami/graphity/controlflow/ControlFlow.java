package com.kobayami.graphity.controlflow;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import java.util.Arrays;

/**
 * Control-flow relations derived from {@link Dominators}, returned as graphs.
 * <p>
 * Control dependence is the transposed dominance frontier of the post-dominators - the
 * dominators of the transposed graph, with the exits as entries:
 *
 * <pre>{@code
 * var postDominators = Dominators.of(cfg.transposed(), exits);   // cfg: a BiAdjacentGraph
 * Graph controlDependence =
 *     BiAdjacentGraph.of(ControlFlow.dominanceFrontiers(postDominators)).transposed();
 * // edge x -> y: x decides whether y runs
 * }</pre>
 *
 * @see Dominators
 * @see IteratedDominanceFrontier
 */
public final class ControlFlow {

  private ControlFlow() {}

  /**
   * Returns the dominance frontiers as a graph: an edge {@code n -> m} whenever {@code m} is in
   * the dominance frontier of {@code n}.
   * <p>
   * The dominance frontier of {@code n} holds the nodes where its dominance ends: {@code m} is in
   * it if {@code n} dominates a predecessor of {@code m} but does not strictly dominate {@code m}.
   * A node can be in its own frontier, so the result may have self-loops. Unreachable nodes have
   * empty frontiers and appear in none. For placing φ-nodes - the merge of variable versions
   * where paths meet in SSA form - use {@link IteratedDominanceFrontier}, which never
   * materializes this relation.
   * <p>
   * Computed on every call, not cached - keep the result. The relation can grow quadratically
   * in the node count, for example on a ladder of branches that all join at the end.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + frontierSize).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + frontierSize) bytes for the result, and as
   * much again during computation, since {@link Graph#of(int[], int[])} copies the arrays; plus
   * 8 x nodeCount bytes. The predecessors come from the in-adjacency of the analyzed graph.
   *
   * @param dominators the dominators whose frontiers to compute
   * @return the frontier relation, with the node IDs of the analyzed graph
   * @throws IllegalArgumentException if the relation has more than {@code Integer.MAX_VALUE - 8}
   *                                  pairs
   */
  public static Graph dominanceFrontiers(Dominators dominators) {
    BiAdjacentGraph graph = dominators.graph();
    int n = graph.nodeCount();
    int[] inStart = Csr.inAdjacencyStart(graph);
    int[] in = Csr.inAdjacency(graph);
    var idom = new int[n];
    for (int node = 0; node < n; node++) {
      idom[node] = dominators.immediateDominatorOf(node);
    }

    // Walk up from every predecessor of m to just below idom(m), adding m on the way (Cooper,
    // Harvey and Kennedy). A node already holding m means the rest of the walk is done, so every
    // step adds a new pair. Run twice - count, then fill - so that each frontier comes out sorted
    // without an intermediate pair buffer.
    var lastAdded = new int[n];
    var start = new int[n + 1];
    Arrays.fill(lastAdded, -1);
    long total = 0;
    for (int m = 0; m < n; m++) {
      if (!dominators.isReachable(m)) continue;
      int stop = idom[m];
      for (int i = inStart[m], end = inStart[m + 1]; i < end; i++) {
        int runner = in[i];
        if (!dominators.isReachable(runner)) continue;
        while (runner != stop && lastAdded[runner] != m) {
          lastAdded[runner] = m;
          start[runner]++;
          total++;
          runner = idom[runner];
        }
      }
    }
    if (total > Integer.MAX_VALUE - 8) {
      throw new IllegalArgumentException(
          "the dominance frontier relation has " + total + " pairs, more than an array can hold");
    }
    int next = 0;
    for (int node = 0; node < n; node++) {
      int count = start[node];
      start[node] = next;
      next += count;
    }
    start[n] = next;
    var frontier = new int[next];
    Arrays.fill(lastAdded, -1);
    for (int m = 0; m < n; m++) {
      if (!dominators.isReachable(m)) continue;
      int stop = idom[m];
      for (int i = inStart[m], end = inStart[m + 1]; i < end; i++) {
        int runner = in[i];
        if (!dominators.isReachable(runner)) continue;
        while (runner != stop && lastAdded[runner] != m) {
          lastAdded[runner] = m;
          frontier[start[runner]++] = m;
          runner = idom[runner];
        }
      }
    }
    for (int node = n; node > 0; node--) {
      start[node] = start[node - 1];
    }
    start[0] = 0;
    return Graph.of(start, frontier);
  }
}
