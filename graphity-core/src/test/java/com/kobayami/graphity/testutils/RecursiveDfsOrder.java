package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Preorder and postorder of a textbook recursive depth-first search, used as an oracle for the
 * iterative traversal cores. Built on plain recursion over the public adjacency API, not on any
 * library traverser, so that it stays independent of the code under test.
 * <p>
 * Order: starts from node 0, continues with the next unvisited node in ascending order, and
 * within each node descends into its out-neighbors in ascending adjacency order. Recursion depth
 * equals the longest DFS path, so only shallow test graphs are suitable.
 */
public final class RecursiveDfsOrder {

  public final IntArrayList preOrder = new IntArrayList();
  public final IntArrayList postOrder = new IntArrayList();
  private final Graph graph;
  private final boolean[] visited;

  public RecursiveDfsOrder(Graph graph) {
    this.graph = graph;
    this.visited = new boolean[graph.nodeCount()];
  }

  /** Visits every node, starting a new tree at each unvisited node in ascending order. */
  public void run() {
    for (int node = 0; node < visited.length; node++) {
      if (!visited[node]) visit(node);
    }
  }

  private void visit(int node) {
    visited[node] = true;
    preOrder.add(node);
    for (int edge = graph.outEdgeStart(node); edge < graph.outEdgeEnd(node); edge++) {
      int target = graph.edgeTarget(edge);
      if (!visited[target]) visit(target);
    }
    postOrder.add(node);
  }

  public IntList preOrder() {
    return preOrder;
  }

  public IntList postOrder() {
    return postOrder;
  }
}
