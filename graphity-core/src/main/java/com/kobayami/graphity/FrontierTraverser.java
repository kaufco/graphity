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
 * 1 = nodes enqueued during level 0, …). Override {@link #leaveLevel(int)}
 * to react to level boundaries:
 * <pre>{@code
 * class Levelizer extends FrontierTraverser {
 *     Levelizer(Graph graph) { super(graph); }
 *     @Override protected void visitNode(int node) {
 *         assignLevel(node, currentLevel());
 *         enqueueChildren(node);
 *     }
 *     @Override protected void leaveLevel(int level) {
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
 * (e.g., {@code run(int); run(); run(list)}). Already-visited nodes are
 * never revisited - the visited set is cumulative across calls.
 * <p>
 * A hook must not call a {@code run} method of the same instance; such a call throws
 * {@link IllegalStateException}. The list passed to {@link #run(IntList)} must not be modified
 * until that call returns. If a {@code run} call ends with an exception - thrown by a hook
 * or by the traversal itself - the state of the instance is undefined (a later run may visit the
 * rest of the frontier, for instance); use a new instance for further runs.
 * <p>
 * Subclasses may pre-mark nodes in {@link #visited} before calling a {@code run} method
 * to exclude them from traversal. This is useful for multi-phase algorithms, subgraph
 * traversal, or interleaved multi-source exploration.
 * <p>
 * <strong>Memory:</strong> O(nodeCount) - the {@link #visited} bitset plus an internal
 * frontier array. The frontier array is never trimmed: it retains every node visited
 * across the traverser's lifetime, not just the currently active wave, though its size
 * is still bounded by nodeCount since each node is enqueued at most once.
 *
 * @see DfsTraverser
 * @see Graph
 * @see Graphs#rootNodes(BiAdjacentGraph)
 */
public abstract class FrontierTraverser {

  /** The graph being traversed. */
  protected final Graph graph;

  /**
   * Tracks which nodes have been visited. See the class doc for pre-marking nodes to
   * exclude them from traversal.
   */
  protected final BitSet visited;

  private final IntArrayList frontier = new IntArrayList();
  private int cursor;
  private int level;

  /** Set by {@link #stop()}; cleared when a {@code run} call starts. */
  private boolean stopRequested;

  /** Set while a {@code run} call is in progress, so that a hook cannot start another one. */
  private boolean running;

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
    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);
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
   * The node is marked as visited and appended to the frontier for later processing. The
   * {@code run} methods seed the frontier the same way.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if the node was unvisited and is now enqueued
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  protected final boolean enqueue(int node) {
    Objects.checkIndex(node, graph.nodeCount());
    return enqueueUnchecked(node);
  }

  /** {@link #enqueue(int)} without the range check, for the seeds of the {@code run} methods. */
  private boolean enqueueUnchecked(int node) {
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
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if visited or enqueued
   */
  protected final boolean isVisited(int node) {
    return visited.get(node);
  }

  /**
   * Returns the current frontier level (BFS wave). Level 0 contains the seed nodes,
   * level 1 the nodes enqueued during level 0, and so on.
   * <p>
   * Valid during {@link #visitNode(int)} and {@link #leaveLevel(int)}.
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
  protected void leaveLevel(int level) {
  }

  /**
   * Stops the traversal in progress: the current {@code run} call returns as soon as the hook
   * that calls this method returns, without calling any further hook. Nodes already enqueued but
   * not yet visited stay marked as visited and are dropped from the frontier, so no later
   * {@code run} call visits them. The visited state is kept, so a later {@code run} call skips
   * everything visited or enqueued so far.
   * <p>
   * Callable from {@link #visitNode(int)} and {@link #leaveLevel(int)}; outside a
   * {@code run} call it has no effect. To cancel from another thread, let a hook check a flag of
   * your own and call this method.
   * <p>
   * <strong>Time:</strong> O(1); the traversal checks for it after every hook call - once per
   * visited node and once per completed level.
   */
  protected final void stop() {
    stopRequested = true;
  }

  /**
   * Runs a full frontier traversal over all nodes.
   * Unreachable nodes are picked sequentially (0, 1, 2, …) as new tree roots,
   * skipping already-visited nodes (including nodes visited by previous {@code run} calls
   * or pre-marked nodes). The level resets to 0 for each new tree.
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * Each call continues from the current visited state - already-visited nodes are never revisited.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   *
   * @throws IllegalStateException if called from a hook, while a {@code run} call is in progress
   */
  public final void run() {
    begin();
    try {
      int n = graph.nodeCount();
      int root = visited.nextClearBit(0);
      while (root < n) {
        enqueueUnchecked(root);
        if (!processFrontier()) {
          return;
        }
        root = visited.nextClearBit(root + 1);
      }
    } finally {
      running = false;
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
   * @throws IllegalStateException if called from a hook, while a {@code run} call is in progress
   */
  public final void run(int startNode) {
    Objects.checkIndex(startNode, graph.nodeCount());
    begin();
    try {
      if (enqueueUnchecked(startNode)) {
        processFrontier();
      }
    } finally {
      running = false;
    }
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
   * @param startNodes the start nodes, each in {@code 0..nodeCount()-1}
   * @throws IndexOutOfBoundsException if a start node is out of range; then no node is visited
   * @throws IllegalStateException if called from a hook, while a {@code run} call is in progress
   */
  public final void run(IntList startNodes) {
    Graphs.checkNodes(startNodes, graph.nodeCount());
    begin();
    try {
      for (int i = 0; i < startNodes.size(); i++) {
        enqueueUnchecked(startNodes.getInt(i));
      }
      processFrontier();
    } finally {
      running = false;
    }
  }

  /**
   * Starts a {@code run} call after its argument checks: rejects a call from a hook and clears an
   * earlier {@link #stop()}. The caller clears {@link #running} in a {@code finally}.
   */
  private void begin() {
    if (running) {
      throw new IllegalStateException("a run is in progress; a hook must not start another run");
    }
    running = true;
    stopRequested = false;
  }

  /**
   * Processes all nodes in the frontier. Tracks level boundaries and fires
   * {@link #leaveLevel(int)} after each level.
   *
   * @return {@code false} if the traversal was stopped
   */
  private boolean processFrontier() {
    level = 0;
    int levelStart = cursor;
    int levelEnd = frontier.size();
    while (cursor < frontier.size()) {
      if (cursor == levelEnd) {
        leaveLevel(level);
        if (stopRequested) {
          cursor = frontier.size();
          return false;
        }
        level++;
        levelStart = cursor;
        levelEnd = frontier.size();
      }
      visitNode(frontier.getInt(cursor++));
      if (stopRequested) {
        cursor = frontier.size();
        return false;
      }
    }
    if (cursor > levelStart) {
      leaveLevel(level);
    }
    return !stopRequested;
  }

}
