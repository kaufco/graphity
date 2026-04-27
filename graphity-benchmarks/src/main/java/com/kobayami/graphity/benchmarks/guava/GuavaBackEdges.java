package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.Graph;
import com.kobayami.graphity.benchmarks.common.BenchEdgePairs;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.BitSet;

/**
 * Back-edge detection on top of Guava Graph.
 */
public final class GuavaBackEdges {

  private GuavaBackEdges() {}

  public static BenchEdgePairs of(Graph<Integer> graph) {
    int n = graph.nodes().size();
    if (n == 0) {
      return BenchEdgePairs.empty();
    }
    BitSet visited = new BitSet(n);
    BitSet onStack = new BitSet(n);
    IntArrayList sources = new IntArrayList();
    IntArrayList targets = new IntArrayList();
    for (int node = 0; node < n; node++) {
      if (!visited.get(node)) {
        visit(node, graph, visited, onStack, sources, targets);
      }
    }
    return new BenchEdgePairs(sources, targets);
  }

  private static void visit(
      int node,
      Graph<Integer> graph,
      BitSet visited,
      BitSet onStack,
      IntArrayList sources,
      IntArrayList targets) {
    visited.set(node);
    onStack.set(node);

    for (int target : graph.successors(node)) {
      if (onStack.get(target)) {
        sources.add(node);
        targets.add(target);
      } else if (!visited.get(target)) {
        visit(target, graph, visited, onStack, sources, targets);
      }
    }

    onStack.clear(node);
  }
}
