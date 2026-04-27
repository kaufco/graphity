package com.kobayami.graphity.benchmarks.webgraph;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

import java.util.BitSet;

/**
 * Direct port of Graphity's topological-sort algorithm ({@code com.kobayami.graphity.TopOrder})
 * onto WebGraph's {@link ImmutableGraph} data model.
 *
 * <p>Algorithmically identical to the Graphity reference: recursive DFS producing
 * reverse post-order, with an {@code onStack} set for back-edge (cycle) detection.
 * Since WebGraph node IDs are dense integers {@code [0, n)}, we can use primitive
 * {@link BitSet}s just like Graphity — which means the only measurable difference
 * against Graphity's {@code TopOrder} is the successor-iteration cost (decoding the
 * BV bitstream vs. reading a CSR {@code int[]}).
 */
public final class WebGraphDfsTopOrder {

  private final ImmutableGraph graph;
  private final BitSet visited;
  private final BitSet onStack;
  private final int[] order;
  private int orderPos;
  private boolean hasCycle;

  public WebGraphDfsTopOrder(ImmutableGraph graph) {
    this.graph = graph;
    int n = graph.numNodes();
    this.visited = new BitSet(n);
    this.onStack = new BitSet(n);
    this.order = new int[n];
    this.orderPos = n;
  }

  /** @return topological order (nodes[0] has no predecessors within the order). */
  public int[] compute() {
    int n = graph.numNodes();
    for (int v = 0; v < n; v++) {
      if (!visited.get(v)) {
        visit(v);
      }
      if (hasCycle) {
        throw new IllegalArgumentException("Graph contains a cycle");
      }
    }
    return order;
  }

  private void visit(int node) {
    if (hasCycle) return;
    visited.set(node);
    onStack.set(node);

    LazyIntIterator it = graph.successors(node);
    int target;
    while ((target = it.nextInt()) != -1) {
      if (onStack.get(target)) {
        hasCycle = true;
        return;
      }
      if (!visited.get(target)) {
        visit(target);
      }
      if (hasCycle) return;
    }

    onStack.clear(node);
    order[--orderPos] = node;
  }
}
