package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

import java.util.Objects;

/**
 * Topological order and level assignment of a directed acyclic graph (DAG).
 * <p>
 * Computed via Kahn's algorithm (BFS from roots) in a single pass.
 * Each node is assigned a level: root nodes (in-degree 0) are level 0,
 * and {@code level(v) = max(level(predecessors)) + 1}.
 * <p>
 * If the graph is a {@link BiAdjacentGraph}, in-degrees are read directly in O(nodeCount).
 * Otherwise, in-degrees are computed by scanning all edges in O(nodeCount + edgeCount).
 *
 * <pre>{@code
 * var tol = TopOrderAndLevels.of(graph);
 * IntList order = tol.order();         // topological order
 * int lvl = tol.levelOf(node);         // level of a node
 * int depth = tol.levelCount();        // number of levels (DAG depth + 1)
 * }</pre>
 *
 * <strong>Time:</strong> O(nodeCount + edgeCount) for computation.<br>
 * <strong>Memory:</strong> O(nodeCount) for the result; O(nodeCount) additional during computation.
 *
 * @see TopOrder
 * @see Graphs#isDag(Graph)
 */
public final class TopOrderAndLevels {

  private final IntList order;
  private final IntList levels;
  private final int levelCount;

  private TopOrderAndLevels(int[] order, int[] levels, int levelCount) {
    this.order = IntLists.unmodifiable(IntArrayList.wrap(order));
    this.levels = IntLists.unmodifiable(IntArrayList.wrap(levels));
    this.levelCount = levelCount;
  }

  /**
   * Computes topological order and levels of the given graph via Kahn's algorithm.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount) for the result; O(nodeCount) additional during computation.
   *
   * @param graph the graph to analyze (must be a DAG)
   * @return topological order with level information
   * @throws IllegalArgumentException if the graph contains a cycle
   */
  public static TopOrderAndLevels of(Graph graph) {
    int n = graph.nodeCount();
    if (n == 0) {
      return new TopOrderAndLevels(new int[0], new int[0], 0);
    }

    // Compute in-degrees.
    var inDegree = new int[n];
    if (graph instanceof BiAdjacentGraph biGraph) {
      var inStart = biGraph.inAdjacencyStart;
      for (int i = 0; i < n; i++) {
        inDegree[i] = inStart[i + 1] - inStart[i];
      }
    } else {
      var outAdj = graph.outAdjacency;
      for (int i = 0; i < outAdj.length; i++) {
        inDegree[outAdj[i]]++;
      }
    }

    // Seed queue with roots (in-degree 0).
    var queue = new int[n];
    int queueHead = 0;
    int queueTail = 0;
    for (int i = 0; i < n; i++) {
      if (inDegree[i] == 0) {
        queue[queueTail++] = i;
      }
    }

    // BFS level by level.
    var order = new int[n];
    var levels = new int[n];
    int orderPos = 0;
    int level = 0;

    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;

    while (queueHead < queueTail) {
      int levelEnd = queueTail;
      while (queueHead < levelEnd) {
        int node = queue[queueHead++];
        order[orderPos++] = node;
        levels[node] = level;

        int end = adjacencyStart[node + 1];
        for (int i = adjacencyStart[node]; i < end; i++) {
          int target = adjacency[i];
          if (--inDegree[target] == 0) {
            queue[queueTail++] = target;
          }
        }
      }
      level++;
    }

    if (orderPos != n) {
      throw new IllegalArgumentException("Graph contains a cycle");
    }

    return new TopOrderAndLevels(order, levels, level);
  }

  /**
   * Returns the topological order as an unmodifiable list of node IDs.
   * For every directed edge u → v, u appears before v in this list.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node IDs in topological order
   */
  public IntList order() {
    return order;
  }

  /**
   * Returns the level assignment as an unmodifiable list, indexed by node ID.
   * {@code levels().getInt(node)} equals {@link #levelOf(int) levelOf(node)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return level per node (index = node ID, value = level)
   */
  public IntList levels() {
    return levels;
  }

  /**
   * Returns the level of the given node. Root nodes (in-degree 0) have level 0,
   * other nodes have {@code max(level(predecessors)) + 1}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return level of the node (0-based)
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int levelOf(int node) {
    return levels.getInt(node);
  }

  /**
   * Returns the number of distinct levels in the DAG (equals the longest path length + 1).
   * An empty graph has 0 levels.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return number of levels
   */
  public int levelCount() {
    return levelCount;
  }
}
