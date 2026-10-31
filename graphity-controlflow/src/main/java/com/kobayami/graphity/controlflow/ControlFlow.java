package com.kobayami.graphity.controlflow;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import java.util.Arrays;

/**
 * Relations of control flow that follow from {@link Dominators}, returned as graphs: the dominance
 * frontiers and, from them, control dependence.
 * <p>
 * A block is control dependent on another if that block decides whether it runs. Control
 * dependence is the transposed graph of the dominance frontiers of the post-dominators. The
 * post-dominators are the dominators of the transposed graph with the exits as entries. Here, for
 * a method {@code load} with a for loop and an if in it, whose control flow {@code cfg} ends in
 * the block {@code exit}:
 * {@snippet file="com/kobayami/graphity/examples/AnalyzingControlFlow.java"
 *     region="controlDependence"}
 * For the placement of phi-nodes, {@link IteratedDominanceFrontier} answers without building the
 * dominance frontiers.
 */
public final class ControlFlow {

  // The JDK's soft maximum array length, as ArrayLimits.MAX_ARRAY_LENGTH in the core, which a
  // module cannot see; change both together. The module derives its other limits from it.
  static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

  private ControlFlow() {}

  /**
   * Returns the dominance frontiers as a graph: an edge {@code n -> m} for every node {@code m} in
   * the dominance frontier of {@code n}.
   * <p>
   * The dominance frontier of {@code n} holds the nodes where its dominance ends: {@code m} is in
   * it if {@code n} dominates an in-node of {@code m} but does not strictly dominate {@code m}. A
   * node can be in its own dominance frontier, as the header of a natural loop is, so the result
   * can have self-loops. Nodes that the entries do not reach have empty dominance frontiers and
   * are in none.
   * <p>
   * Uses the algorithm of Cooper, Harvey and Kennedy. The result is computed anew on every call,
   * not cached: keep it to use it more than once. It can grow quadratically with the node count,
   * for example on deeply nested do-while loops.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + frontierCount), where frontierCount is the
   * number of edges of the result.<br>
   * <strong>Memory:</strong> 4 x (nodeCount + frontierCount) bytes for the result; while it runs,
   * as much again and 8 x nodeCount bytes more.
   *
   * @param dominators the dominators of the control-flow graph
   * @return the dominance frontiers, with the node IDs of the graph of {@code dominators}
   * @throws IllegalArgumentException if the result would have more edges than a graph can have
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

    // Walk up from every in-node of m to just below idom(m), adding m on the way (Cooper,
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
    if (total > MAX_ARRAY_LENGTH) {
      throw new IllegalArgumentException("the dominance frontier relation has more than "
          + MAX_ARRAY_LENGTH + " pairs, the largest array length");
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
    // start[runner] doubles as write pointer and is shifted back afterwards.
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
