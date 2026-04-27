package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.Graph;

import java.util.HashSet;
import java.util.Set;

/**
 * Direct port of Graphity's topological-sort algorithm ({@code com.kobayami.graphity.TopOrder})
 * onto Guava's {@link Graph} data model.
 *
 * <p>Algorithmically identical: recursive DFS producing reverse post-order, with an
 * {@code onStack} set for back-edge (cycle) detection. The difference is purely in
 * the data structures:
 * <ul>
 *   <li>Graphity: primitive {@code int[] order}, {@link java.util.BitSet} for visited/onStack,
 *       CSR adjacency access.</li>
 *   <li>This port: {@code Integer[] order}, {@link HashSet}&lt;Integer&gt; for visited/onStack,
 *       Guava's {@link Graph#successors} for adjacency.</li>
 * </ul>
 */
public final class GuavaDfsTopOrder {

  private final Graph<Integer> graph;
  private final Set<Integer> visited;
  private final Set<Integer> onStack;
  private final Integer[] order;
  private int orderPos;
  private boolean hasCycle;

  public GuavaDfsTopOrder(Graph<Integer> graph) {
    this.graph = graph;
    int n = graph.nodes().size();
    this.visited = new HashSet<>(n * 2);
    this.onStack = new HashSet<>();
    this.order = new Integer[n];
    this.orderPos = n;
  }

  /** @return topological order (nodes[0] has no predecessors within the order). */
  public Integer[] compute() {
    for (Integer v : graph.nodes()) {
      if (!visited.contains(v)) {
        visit(v);
      }
      if (hasCycle) {
        throw new IllegalArgumentException("Graph contains a cycle");
      }
    }
    return order;
  }

  private void visit(Integer node) {
    if (hasCycle) return;
    visited.add(node);
    onStack.add(node);

    for (Integer target : graph.successors(node)) {
      if (onStack.contains(target)) {
        hasCycle = true;
        return;
      }
      if (!visited.contains(target)) {
        visit(target);
      }
      if (hasCycle) return;
    }

    onStack.remove(node);
    order[--orderPos] = node;
  }
}
