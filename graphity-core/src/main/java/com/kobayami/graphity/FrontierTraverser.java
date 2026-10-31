package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.BitSet;
import java.util.Objects;

/**
 * The base class of a breadth-first search of your own over a {@link Graph}, or of any search that
 * works off a queue of nodes and chooses which nodes enter it.
 * <p>
 * The queue is called the frontier and works first in first out. The traverser calls
 * {@link #visitNode(int)} for each node in the order in which it entered the frontier. The
 * {@code run} methods put the start nodes into the frontier. The hook puts in the nodes that come
 * next: the out-nodes of a node with {@link #enqueueChildren(int)}, or a single node with
 * {@link #enqueue(int)}. A node counts as visited from the moment it enters the frontier, so it
 * enters the frontier at most once. From a node for which the hook enqueues nothing, the search
 * goes no further. With {@code enqueueChildren(node)} in every {@code visitNode(node)}, the
 * traversal is a
 * <a href="https://en.wikipedia.org/wiki/Breadth-first_search">breadth-first search</a>.
 * <p>
 * The nodes come in levels: the start nodes form level 0, and the nodes that enter the frontier
 * while level {@code k} is visited form level {@code k + 1}. In a breadth-first search, those are
 * the nodes at distance {@code k + 1} from the nearest start node. {@link #currentLevel()} returns
 * the level of the node being visited, and {@link #leaveLevel(int)} is called after the last node
 * of each level. Here, {@code calls} are the calls between the methods of a program, and the
 * search follows them from {@code main} for two levels:
 * {@snippet file="com/kobayami/graphity/examples/Traversals.java" region="currentLevel"}
 * When {@code visitNode} returns, the nodes it enqueued are still waiting in the frontier: unlike
 * {@link DfsTraverser}, this traverser has no postorder. A hook can end the whole run with
 * {@link #stop()}.
 * <p>
 * {@link #run()} searches from every node in ascending order, {@link #run(int)} from one node and
 * {@link #run(IntList)} from several, such as the root nodes that
 * {@link Graphs#rootNodes(BiAdjacentGraph)} returns. The visited nodes add up over the runs of an
 * instance: a node is visited at most once in the lifetime of the instance, and a later run skips
 * it. Nodes enqueued outside a run come first in the next run. A subclass can also keep nodes out
 * of the search by marking them in {@link #visited}.
 * <p>
 * A hook must not start another run of the same instance, which throws
 * {@link IllegalStateException}. If a run ends with an exception from a hook or from the search,
 * the state of the instance is undefined: a later run may visit the rest of the frontier, for
 * example. A new instance is needed for further runs. {@link BfsDistances} and
 * {@link Graphs#reachableFrom(Graph, IntList)} are ready-made breadth-first searches.
 * <p>
 * Instances are mutable and not safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 1 bit per node for the visited nodes, and 4 to 6 bytes for every node
 * that has entered the frontier, which keeps them all: up to 6 x nodeCount bytes.
 */
public abstract class FrontierTraverser {

  /** The graph that this traverser searches. */
  protected final Graph graph;

  /**
   * The visited nodes, one bit per node ID: a bit is set when its node enters the frontier.
   * <p>
   * A subclass may set bits to keep nodes out of the search, also before the first run: a node
   * whose bit is set counts as visited and no longer enters the frontier, neither from a
   * {@code run} method nor from {@link #enqueue(int)} or {@link #enqueueChildren(int)}. A node
   * already in the frontier is visited all the same. A subclass must not clear a bit; what happens
   * then is not specified.
   */
  protected final BitSet visited;

  private final IntArrayList frontier = new IntArrayList();
  private int cursor;
  private int level;

  // Set by stop(); cleared when a run starts.
  private boolean stopRequested;

  // Set while a run is in progress, so that a hook cannot start another one.
  private boolean running;

  /**
   * Creates a traverser for {@code graph}, with no node visited.
   * <p>
   * <strong>Time:</strong> O(nodeCount).
   *
   * @param graph the graph to search
   */
  protected FrontierTraverser(Graph graph) {
    this.graph = graph;
    this.visited = new BitSet(graph.nodeCount());
  }

  /**
   * Called for each node of the frontier, in the order in which the nodes entered it.
   * <p>
   * The hook puts the nodes that come next into the frontier:
   * {@link #enqueueChildren(int) enqueueChildren(node)} for a breadth-first search,
   * {@link #enqueue(int)} for single nodes, or nothing to end the search at {@code node}. They
   * join the next level and are visited after the rest of the current one.
   *
   * @param node node ID in {@code 0..nodeCount-1}
   */
  protected abstract void visitNode(int node);

  /**
   * Puts the out-nodes of {@code node} that are not visited yet into the frontier, in ascending
   * order, and marks them visited.
   * <p>
   * Called from {@link #visitNode(int) visitNode(node)}, it makes the traversal a breadth-first
   * search. Called outside a run, it adds the nodes to the next run, as {@link #enqueue(int)} does.
   * <p>
   * <strong>Time:</strong> O(outDegree(node)).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
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
   * Puts {@code node} into the frontier and marks it visited, unless it is visited already.
   * <p>
   * Called from a hook, it adds {@code node} to the next level. Called outside a run, it adds
   * {@code node} to the next run: the nodes enqueued then come first in its level 0, ahead of its
   * start nodes, and {@link #run()} visits them before it starts from a node of its own.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return {@code true} if {@code node} was not visited and is now in the frontier
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  protected final boolean enqueue(int node) {
    Objects.checkIndex(node, graph.nodeCount());
    return enqueueUnchecked(node);
  }

  // enqueue(int) without the range check, for the start nodes of the run methods.
  private boolean enqueueUnchecked(int node) {
    if (visited.get(node)) return false;
    visited.set(node);
    frontier.add(node);
    return true;
  }

  /**
   * Returns whether {@code node} is visited: it has entered the frontier, or a subclass has
   * marked it in {@link #visited}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}; for any other value the method returns
   *             {@code false} or throws {@code IndexOutOfBoundsException}
   * @return {@code true} if {@code node} is visited
   */
  protected final boolean isVisited(int node) {
    return visited.get(node);
  }

  /**
   * Returns the level of the node being visited: 0 for the start nodes, {@code k + 1} for the
   * nodes that entered the frontier while level {@code k} was visited.
   * <p>
   * In {@link #leaveLevel(int)}, it is the level just completed. Outside a run, the value has no
   * meaning.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the current level
   */
  protected final int currentLevel() {
    return level;
  }

  /**
   * Called after the last node of a level has been visited, before the first node of the next
   * level. Does nothing unless overridden.
   * <p>
   * Nodes enqueued here join the next level, which then follows even if the completed level
   * enqueued nothing.
   *
   * @param level the level just completed
   */
  protected void leaveLevel(int level) {
  }

  /**
   * Stops the run in progress: the run returns as soon as the hook that calls this method
   * returns, without calling another hook.
   * <p>
   * The nodes still waiting in the frontier are dropped from it, but stay marked visited, so no
   * later run visits them. A later run skips every node visited so far. Called outside a run, this
   * method has no effect. To cancel a run from another thread, let a hook check a flag of your own
   * and call this method.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  protected final void stop() {
    stopRequested = true;
  }

  /**
   * Searches from every node not visited yet, in ascending order, until every node is visited.
   * <p>
   * Nodes enqueued before the call come first, as a level 0 of their own. Then each node that no
   * earlier search has reached when its turn comes starts a search of its own, with levels counted
   * from 0 again.
   * <p>
   * <strong>Time:</strong> O(nodeCount), plus the hooks: O(nodeCount + edgeCount) for a
   * breadth-first search.
   *
   * @throws IllegalStateException if called from a hook, during a run of this instance
   */
  public final void run() {
    begin();
    try {
      // Nodes enqueued outside a run first; an empty frontier calls no hook.
      if (!processFrontier()) {
        return;
      }
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
   * Searches from {@code startNode} and from the nodes enqueued before the call, visiting the
   * nodes they reach that are not visited yet.
   * <p>
   * Level 0 holds the nodes enqueued before the call, then {@code startNode} unless it is visited
   * already.
   * <p>
   * <strong>Time:</strong> O(nodes visited), plus the hooks: O(reachable nodes + reachable
   * edges) for a breadth-first search.
   *
   * @param startNode node ID in {@code 0..nodeCount-1}
   * @throws IndexOutOfBoundsException if {@code startNode} is out of range
   * @throws IllegalStateException if called from a hook, during a run of this instance
   */
  public final void run(int startNode) {
    Objects.checkIndex(startNode, graph.nodeCount());
    begin();
    try {
      enqueueUnchecked(startNode);
      processFrontier();
    } finally {
      running = false;
    }
  }

  /**
   * Searches from {@code startNodes} and from the nodes enqueued before the call, visiting the
   * nodes they reach that are not visited yet.
   * <p>
   * Level 0 holds the nodes enqueued before the call, then the start nodes in list order, skipping
   * those visited already. The list is checked first, so a start node out of range leaves the
   * traverser unchanged.
   * <p>
   * <strong>Time:</strong> O(startNodes.size() + nodes visited), plus the hooks:
   * O(startNodes.size() + reachable nodes + reachable edges) for a breadth-first search.
   *
   * @param startNodes node IDs, each in {@code 0..nodeCount-1}
   * @throws IndexOutOfBoundsException if a start node is out of range
   * @throws IllegalStateException if called from a hook, during a run of this instance
   */
  public final void run(IntList startNodes) {
    Checks.checkNodes(startNodes, graph.nodeCount());
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

  // Starts a run after its argument checks: rejects a call from a hook and clears an earlier
  // stop(). The caller clears running in a finally.
  private void begin() {
    if (running) {
      throw new IllegalStateException("a run is in progress; a hook must not start another run");
    }
    running = true;
    stopRequested = false;
  }

  // Visits the frontier one level per round and calls leaveLevel after each; the nodes enqueued
  // there form the next level. Returns false if the run was stopped.
  private boolean processFrontier() {
    level = 0;
    while (cursor < frontier.size()) {
      int levelEnd = frontier.size();
      while (cursor < levelEnd) {
        visitNode(frontier.getInt(cursor++));
        if (stopRequested) {
          cursor = frontier.size();
          return false;
        }
      }
      leaveLevel(level);
      if (stopRequested) {
        cursor = frontier.size();
        return false;
      }
      level++;
    }
    return true;
  }

}
