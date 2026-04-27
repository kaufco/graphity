package com.kobayami.graphity.benchmarks.webgraph;

import com.kobayami.graphity.benchmarks.common.BenchEdgePairs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;

import java.util.BitSet;

/**
 * Back-edge detection on WebGraph's ImmutableGraph.
 */
public final class WebGraphBackEdges {

  private WebGraphBackEdges() {}

  public static BenchEdgePairs of(ImmutableGraph graph) {
    int n = graph.numNodes();
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
      ImmutableGraph graph,
      BitSet visited,
      BitSet onStack,
      IntArrayList sources,
      IntArrayList targets) {
    visited.set(node);
    onStack.set(node);

    LazyIntIterator it = graph.successors(node);
    int target;
    while ((target = it.nextInt()) != -1) {
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
