package com.kobayami.graphity.benchmarks.jgrapht;

import com.kobayami.graphity.benchmarks.common.BenchEdgePairs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.BitSet;

/**
 * Back-edge detection on top of JGraphT's data model.
 */
public final class JGraphTBackEdges {

  private JGraphTBackEdges() {}

  public static BenchEdgePairs of(Graph<Integer, DefaultEdge> graph) {
    int n = graph.vertexSet().size();
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
      Graph<Integer, DefaultEdge> graph,
      BitSet visited,
      BitSet onStack,
      IntArrayList sources,
      IntArrayList targets) {
    visited.set(node);
    onStack.set(node);

    for (DefaultEdge edge : graph.outgoingEdgesOf(node)) {
      int target = graph.getEdgeTarget(edge);
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
