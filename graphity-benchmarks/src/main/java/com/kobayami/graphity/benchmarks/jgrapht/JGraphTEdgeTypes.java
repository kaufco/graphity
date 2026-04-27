package com.kobayami.graphity.benchmarks.jgrapht;

import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.benchmarks.common.BenchTypedEdges;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.BitSet;

/**
 * DFS edge-type classification (TREE/BACK/FORWARD/CROSS) on JGraphT graphs.
 */
public final class JGraphTEdgeTypes {

  private JGraphTEdgeTypes() {}

  public static BenchTypedEdges of(Graph<Integer, DefaultEdge> graph) {
    int n = graph.vertexSet().size();
    if (n == 0) {
      return new BenchTypedEdges(new IntArrayList(0), new IntArrayList(0), new ByteArrayList(0));
    }
    BitSet visited = new BitSet(n);
    BitSet onStack = new BitSet(n);
    int[] discovery = new int[n];
    int[] time = {0};
    IntArrayList sources = new IntArrayList();
    IntArrayList targets = new IntArrayList();
    ByteArrayList types = new ByteArrayList();

    for (int node = 0; node < n; node++) {
      if (!visited.get(node)) {
        visit(node, graph, visited, onStack, discovery, time, sources, targets, types);
      }
    }
    return new BenchTypedEdges(sources, targets, types);
  }

  private static void visit(
      int node,
      Graph<Integer, DefaultEdge> graph,
      BitSet visited,
      BitSet onStack,
      int[] discovery,
      int[] time,
      IntArrayList sources,
      IntArrayList targets,
      ByteArrayList types) {
    visited.set(node);
    onStack.set(node);
    discovery[node] = ++time[0];

    for (DefaultEdge edge : graph.outgoingEdgesOf(node)) {
      int target = graph.getEdgeTarget(edge);
      sources.add(node);
      targets.add(target);
      if (!visited.get(target)) {
        types.add((byte) EdgeType.TREE.ordinal());
        visit(target, graph, visited, onStack, discovery, time, sources, targets, types);
      } else if (onStack.get(target)) {
        types.add((byte) EdgeType.BACK.ordinal());
      } else if (discovery[node] < discovery[target]) {
        types.add((byte) EdgeType.FORWARD.ordinal());
      } else {
        types.add((byte) EdgeType.CROSS.ordinal());
      }
    }

    onStack.clear(node);
  }
}
