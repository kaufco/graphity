package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Objects;

/**
 * A topological order of a DAG (directed acyclic graph) with the level of each node. A root node
 * has level 0, and every other node has one more than the highest level of its in-nodes.
 * <p>
 * The level of a node is the number of edges on a longest path to it from a root node, so no path
 * connects two nodes of the same level. {@link #order()} lists the nodes level by level,
 * {@link #levelOf(int)} and {@link #levels()} return their levels, and {@link #levelCount()} the
 * number of levels.
 * <p>
 * Uses <a href="https://en.wikipedia.org/wiki/Topological_sorting#Kahn%27s_algorithm">Kahn's
 * algorithm</a>: {@link #of(Graph)} starts with the root nodes in ascending order, places a node
 * once all its in-nodes are placed and takes the out-nodes of each node in ascending order, so a
 * graph always gets the same order. For the order alone, {@link TopOrder#of(Graph)} needs less
 * memory.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 8 x nodeCount bytes.
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
   * Returns the topological order and the levels of {@code graph}.
   * <p>
   * On a {@link BiAdjacentGraph}, it reads the in-degrees instead of counting them in a pass over
   * the edges.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes for the result; while it runs, 8 x nodeCount
   * bytes more.
   *
   * @param graph the DAG to sort
   * @return the order and the levels
   * @throws IllegalArgumentException if {@code graph} has a cycle
   */
  public static TopOrderAndLevels of(Graph graph) {
    int n = graph.nodeCount();

    // 1) In-degrees; a BiAdjacentGraph has them in its offsets.
    var inDegree = new int[n];
    if (graph instanceof BiAdjacentGraph biGraph) {
      var inStart = Csr.inAdjacencyStart(biGraph);
      for (int i = 0; i < n; i++) {
        inDegree[i] = inStart[i + 1] - inStart[i];
      }
    } else {
      var outAdj = Csr.outAdjacency(graph);
      for (int i = 0; i < outAdj.length; i++) {
        inDegree[outAdj[i]]++;
      }
    }

    // 2) Root nodes.
    var queue = new int[n];
    int queueHead = 0;
    int queueTail = 0;
    for (int i = 0; i < n; i++) {
      if (inDegree[i] == 0) {
        queue[queueTail++] = i;
      }
    }

    // 3) Kahn, one wave per level: a node joins the wave after that of its last in-node.
    var order = new int[n];
    var levels = new int[n];
    int orderPos = 0;
    int level = 0;

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);

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
      throw new IllegalArgumentException("graph contains a cycle");
    }

    return new TopOrderAndLevels(order, levels, level);
  }

  /**
   * Returns the nodes in topological order, level by level: for every edge {@code u -> v},
   * {@code u} comes before {@code v}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node IDs in topological order; unmodifiable
   */
  public IntList order() {
    return order;
  }

  /**
   * Returns the level of every node, by node ID.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the levels, indexed by node ID; unmodifiable
   */
  public IntList levels() {
    return levels;
  }

  /**
   * Returns the level of {@code node}. Equivalent to {@code levels().getInt(node)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return level
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int levelOf(int node) {
    Objects.checkIndex(node, levels.size());
    return levels.getInt(node);
  }

  /**
   * Returns the number of levels, or {@code 0} for a graph without nodes. It equals the number of
   * nodes on a longest path.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return level count
   */
  public int levelCount() {
    return levelCount;
  }
}
