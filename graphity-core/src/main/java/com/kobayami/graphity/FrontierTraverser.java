package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.BitSet;
import java.util.Objects;

/**
 * Abstract base class for frontier-based traversal of a {@link Graph}.
 * <p>
 * Nodes are processed in the order they appear in an internal frontier queue (FIFO).
 * Subclasses override {@link #visitNode(int)} to define what happens when a node is visited.
 * Within {@code visitNode}, call {@link #enqueueChildren(int)} to enqueue all unvisited
 * out-neighbors, or {@link #enqueue(int)} to schedule individual nodes.
 *
 * <h2>BFS (breadth-first search)</h2>
 * To implement standard BFS, call {@link #enqueueChildren(int)} in {@code visitNode}.
 * This ensures all nodes at distance <em>d</em> from the seed are visited before any node
 * at distance <em>d+1</em>:
 * <pre>{@code
 * class MyBfs extends FrontierTraverser {
 *     MyBfs(Graph graph) { super(graph); }
 *     @Override protected void visitNode(int node) {
 *         process(node);           // your logic here
 *         enqueueChildren(node);   // → BFS behavior
 *     }
 * }
 * new MyBfs(graph).run(startNode);
 * }</pre>
 *
 * <h2>Custom frontier</h2>
 * Use {@link #enqueue(int)} to schedule arbitrary nodes (e.g., Dijkstra-style):
 * <pre>{@code
 * class CustomFrontier extends FrontierTraverser {
 *     CustomFrontier(Graph graph) { super(graph); }
 *     @Override protected void visitNode(int node) {
 *         process(node);
 *         for (int neighbor : selectNeighbors(node)) {
 *             enqueue(neighbor);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Level tracking</h2>
 * {@link #currentLevel()} returns the current BFS wave (0 = seed nodes,
 * 1 = nodes enqueued during level 0, …). Override {@link #onLevelComplete(int)}
 * to react to level boundaries:
 * <pre>{@code
 * class Levelizer extends FrontierTraverser {
 *     Levelizer(Graph graph) { super(graph); }
 *     @Override protected void visitNode(int node) {
 *         assignLevel(node, currentLevel());
 *         enqueueChildren(node);
 *     }
 *     @Override protected void onLevelComplete(int level) {
 *         System.out.println("Level " + level + " complete");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Cut (pruning):</strong> omit the {@link #enqueueChildren(int)} call (or don't
 * {@link #enqueue(int)} any nodes) to prevent further expansion from a node.
 * <p>
 * <strong>Note on post-order:</strong> unlike {@link DfsTraverser}, there is no post-order
 * processing. When {@code visitNode} returns, the enqueued children have not yet been visited.
 * <p>
 * {@code run} methods may be called <strong>multiple times</strong>, and freely combined
 * (e.g., {@code runFromRoots(); run(); runFromNodes(list)}). Already-visited nodes are
 * never revisited — the visited set is cumulative across calls.
 * <p>
 * Subclasses may pre-mark nodes in {@link #visited} before calling a {@code run} method
 * to exclude them from traversal.
 *
 * @see DfsTraverser
 * @see Graph
 * @see BiAdjacentGraph
 * @see BiAdjacentFrontierTraverser
 */
public abstract class FrontierTraverser {

  /** The graph being traversed. */
  protected final Graph graph;

  /**
   * Tracks which nodes have been visited. Subclasses may pre-mark nodes before calling
   * a {@code run} method to exclude them from traversal.
   */
  protected final BitSet visited;

  private final IntArrayList frontier = new IntArrayList();
  private int cursor;
  private int level;
  private int levelEnd;

  /**
   * Creates a traverser for the given graph.
   *
   * @param graph the graph to traverse
   */
  protected FrontierTraverser(Graph graph) {
    this.graph = graph;
    this.visited = new BitSet(graph.nodeCount());
  }

  /**
   * Called for each node when it is dequeued from the frontier. Override to define
   * visitation behavior.
   * <p>
   * Call {@link #enqueueChildren(int)} to enqueue all unvisited out-neighbors (standard BFS),
   * or use {@link #enqueue(int)} to schedule specific nodes. Omit both to prune.
   *
   * @param node the node being visited
   */
  protected abstract void visitNode(int node);

  /**
   * Enqueues all unvisited out-neighbors of {@code node} into the frontier.
   * Each unvisited neighbor is marked as visited and appended to the frontier for
   * later processing. Already-visited neighbors are silently skipped.
   * <p>
   * Call this in {@link #visitNode(int)} for standard BFS behavior.
   * <p>
   * <strong>Time:</strong> O(outDegree).
   *
   * @param node the node whose children to enqueue
   */
  protected final void enqueueChildren(int node) {
    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;
    int end = adjacencyStart[node + 1];
    for (int i = adjacencyStart[node]; i < end; i++) {
      int child = adjacency[i];
      if (!visited.get(child)) {
        visited.set(child);
        frontier.add(child);
      }
    }
  }

  /**
   * Enqueues a single node into the frontier if not already visited.
   * The node is marked as visited and appended to the frontier for later processing.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node the node to enqueue
   * @return {@code true} if the node was unvisited and is now enqueued
   */
  protected final boolean enqueue(int node) {
    if (visited.get(node)) return false;
    visited.set(node);
    frontier.add(node);
    return true;
  }

  /**
   * Returns whether {@code node} has already been visited (or enqueued).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID
   * @return {@code true} if visited or enqueued
   */
  protected final boolean isVisited(int node) {
    return visited.get(node);
  }

  /**
   * Returns the current frontier level (BFS wave). Level 0 contains the seed nodes,
   * level 1 the nodes enqueued during level 0, and so on.
   * <p>
   * Valid during {@link #visitNode(int)} and {@link #onLevelComplete(int)}.
   *
   * @return the current level (0-based)
   */
  protected final int currentLevel() {
    return level;
  }

  /**
   * Called after all nodes of a level have been processed. Override to react to
   * level boundaries (e.g., for level-based termination or statistics).
   * <p>
   * The default implementation does nothing.
   *
   * @param level the level that just completed (0-based)
   */
  protected void onLevelComplete(int level) {
  }

  /**
   * Runs a full frontier traversal over all nodes.
   * Unreachable nodes are picked sequentially (0, 1, 2, …) as new tree roots,
   * skipping already-visited nodes (including nodes visited by previous {@code run} calls
   * or pre-marked nodes). The level resets to 0 for each new tree.
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * Each call continues from the current visited state — already-visited nodes are never revisited.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   */
  public final void run() {
    int n = graph.nodeCount();
    int root = visited.nextClearBit(0);
    while (root < n) {
      seedAndProcess(root);
      root = visited.nextClearBit(root + 1);
    }
  }

  /**
   * Runs frontier traversal from a single start node. Only visits nodes reachable
   * from {@code startNode} that have not been visited yet.
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(reachable nodes + reachable edges).
   *
   * @param startNode the start node
   * @throws IndexOutOfBoundsException if {@code startNode} is out of range
   */
  public final void run(int startNode) {
    Objects.checkIndex(startNode, graph.nodeCount());
    if (visited.get(startNode)) return;
    seedAndProcess(startNode);
  }

  /**
   * Runs frontier traversal starting from the given nodes, in order. Only visits nodes
   * reachable from the specified start set that have not been visited yet.
   * Already-visited start nodes are skipped.
   * <p>
   * All provided start nodes form level 0 (those that are unvisited).
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(reachable nodes + reachable edges).
   *
   * @param startNodes the start nodes
   */
  public final void runFromNodes(IntList startNodes) {
    for (int i = 0; i < startNodes.size(); i++) {
      int node = startNodes.getInt(i);
      if (!visited.get(node)) {
        visited.set(node);
        frontier.add(node);
      }
    }
    processFrontier();
  }

  /**
   * Seeds the frontier with a single root and processes until empty.
   */
  private void seedAndProcess(int root) {
    visited.set(root);
    frontier.add(root);
    processFrontier();
  }

  /**
   * Processes all nodes in the frontier. Tracks level boundaries and fires
   * {@link #onLevelComplete(int)} after each level.
   */
  private void processFrontier() {
    level = 0;
    int levelStart = cursor;
    levelEnd = frontier.size();
    while (cursor < frontier.size()) {
      if (cursor == levelEnd) {
        onLevelComplete(level);
        level++;
        levelStart = cursor;
        levelEnd = frontier.size();
      }
      visitNode(frontier.getInt(cursor++));
    }
    if (cursor > levelStart) {
      onLevelComplete(level);
    }
  }

}
