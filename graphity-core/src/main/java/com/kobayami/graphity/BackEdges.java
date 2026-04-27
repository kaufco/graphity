package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.BitSet;

/**
 * Back-edge detection for directed graphs.
 * <p>
 * Back-edges are edges that point from a node to one of its ancestors in the DFS tree
 * (including self-loops). Removing all back-edges from a graph makes it a DAG — they
 * form a <em>feedback arc set</em> (not necessarily minimal, but practical for most
 * dependency-analysis scenarios).
 * <p>
 * Uses a single DFS pass, O(nodeCount + edgeCount) time and O(nodeCount) memory.
 *
 * <pre>{@code
 * Edges backEdges = BackEdges.of(graph);
 * // backEdges.count() == 0  ⟺  graph is a DAG
 * for (int i = 0; i < backEdges.count(); i++) {
 *     int src = backEdges.sourceNodes().getInt(i);
 *     int tgt = backEdges.targetNodes().getInt(i);
 *     // src → tgt is a back-edge (cycle-causing)
 * }
 * }</pre>
 *
 * @see Edges
 * @see EdgeTypes
 * @see Graphs#isDag(Graph)
 */
public final class BackEdges {

  private BackEdges() {}

  /**
   * Computes all back-edges of the given graph via a single DFS pass.
   * <p>
   * Back-edges point from a node to an ancestor in the DFS tree (or to itself = self-loop).
   * Their removal yields a DAG.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount) + O(backEdgeCount) for the result.
   *
   * @param graph the graph to analyze
   * @return the back-edges as an {@link Edges} collection
   */
  public static Edges of(Graph graph) {
    if (graph.nodeCount() == 0) {
      return Edges.empty();
    }
    var traverser = new Traverser(graph);
    traverser.run();
    return new Edges(
        IntLists.unmodifiable(traverser.sources),
        IntLists.unmodifiable(traverser.targets));
  }

  /**
   * DFS traverser that collects back-edges. A back-edge is an edge to a node that is
   * currently on the DFS stack (tracked via {@code onStack} BitSet).
   */
  private static final class Traverser extends DfsTraverser {

    private final BitSet onStack;
    final IntArrayList sources = new IntArrayList();
    final IntArrayList targets = new IntArrayList();

    Traverser(Graph graph) {
      super(graph);
      this.onStack = new BitSet(graph.nodeCount());
    }

    @Override
    protected void visitNode(int node) {
      onStack.set(node);

      var adjacency = graph.outAdjacency;
      var adjacencyStart = graph.outAdjacencyStart;
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        int target = adjacency[i];
        if (onStack.get(target)) {
          sources.add(node);
          targets.add(target);
        } else {
          visit(target);
        }
      }

      onStack.clear(node);
    }
  }
}
