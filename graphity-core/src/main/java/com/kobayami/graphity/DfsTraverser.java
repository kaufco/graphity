package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.BitSet;
import java.util.Objects;

/**
 * Abstract base class for depth-first traversal of a {@link Graph}.
 * <p>
 * Subclasses override {@link #visitNode(int)} to define what happens when a node is visited.
 * Within {@code visitNode}, call {@link #visitChildren(int)} to recurse into unvisited
 * out-neighbors (standard DFS descent), or use {@link #visit(int)} and {@link #isVisited(int)}
 * for manual edge iteration (needed by algorithms like Tarjan SCC that must inspect all edges).
 * <p>
 * <strong>Pre/Post processing:</strong> code before {@code visitChildren()} executes in
 * preorder, code after in postorder.<br>
 * <strong>Cut (pruning):</strong> omit the {@code visitChildren()} call to skip a subtree.
 *
 * <h2>Example: parenthesized DFS trace</h2>
 * <pre>{@code
 * class TraceDfs extends DfsTraverser {
 *     TraceDfs(Graph graph) { super(graph); }
 *     @Override protected void visitNode(int node) {
 *         System.out.print("(" + node);
 *         visitChildren(node);
 *         System.out.print(")");
 *     }
 * }
 * new TraceDfs(graph).run();
 * }</pre>
 *
 * <h2>Example: depth-limited DFS (cut)</h2>
 * <pre>{@code
 * class ShallowDfs extends DfsTraverser {
 *     int maxDepth, depth;
 *     ShallowDfs(Graph graph, int maxDepth) {
 *         super(graph);
 *         this.maxDepth = maxDepth;
 *     }
 *     @Override protected void visitNode(int node) {
 *         process(node);
 *         if (depth < maxDepth) {
 *             depth++;
 *             visitChildren(node);
 *             depth--;
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>{@code run} methods may be called <strong>multiple times</strong>, and freely combined
 * (e.g., {@code runFromRoots(); run(); runFromNodes(list)}). Already-visited nodes are
 * never revisited — the visited set is cumulative across calls.
 * <p>
 * Subclasses may pre-mark nodes in {@link #visited} before calling a {@code run} method
 * to exclude them from traversal. This is useful for multi-phase algorithms, subgraph
 * traversal, or interleaved multi-source exploration.
 *
 * @see Graph
 * @see BiAdjacentGraph
 * @see BiAdjacentDfsTraverser
 */
public abstract class DfsTraverser {

  /** The graph being traversed. */
  protected final Graph graph;

  /**
   * Tracks which nodes have been visited. Subclasses may pre-mark nodes before calling
   * a {@code run} method to exclude them from traversal.
   */
  protected final BitSet visited;

  /**
   * Creates a traverser for the given graph.
   *
   * @param graph the graph to traverse
   */
  protected DfsTraverser(Graph graph) {
    this.graph = graph;
    this.visited = new BitSet(graph.nodeCount());
  }

  /**
   * Called for each node during DFS. Override to define visitation behavior.
   * <p>
   * Call {@link #visitChildren(int)} to descend into unvisited out-neighbors,
   * or omit it to prune the subtree. For algorithms that need to inspect all edges
   * (including edges to already-visited nodes), iterate out-nodes manually using
   * {@link Graph#outAdjacencyStart}, {@link Graph#outAdjacency}, and {@link #isVisited(int)}.
   *
   * @param node the node being visited
   */
  protected abstract void visitNode(int node);

  /**
   * Visits all unvisited out-neighbors of {@code node} via DFS.
   * Each unvisited neighbor triggers a recursive call to {@link #visitNode(int)}.
   * Already-visited neighbors are silently skipped.
   * <p>
   * <strong>Time:</strong> O(outDegree) for the immediate expansion.
   *
   * @param node the node whose children to visit
   */
  protected final void visitChildren(int node) {
    // Cache array references as locals so the JIT keeps them in registers
    // across the virtual visitNode() call (which would otherwise force a reload).
    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;
    int end = adjacencyStart[node + 1];
    for (int i = adjacencyStart[node]; i < end; i++) {
      int child = adjacency[i];
      if (!visited.get(child)) {
        visited.set(child);
        visitNode(child);
      }
    }
  }

  /**
   * Visits a single node if not already visited. Marks it as visited and calls
   * {@link #visitNode(int)}.
   * <p>
   * Intended for manual edge iteration in algorithms that need to inspect all edges
   * (e.g., Tarjan SCC). For standard DFS descent, prefer {@link #visitChildren(int)}.
   * <p>
   * <strong>Time:</strong> O(1) for the visited check.
   *
   * @param node the node to visit
   * @return {@code true} if the node was unvisited and is now being visited
   */
  protected final boolean visit(int node) {
    if (visited.get(node)) return false;
    visited.set(node);
    visitNode(node);
    return true;
  }

  /**
   * Returns whether {@code node} has already been visited.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID
   * @return {@code true} if visited
   */
  protected final boolean isVisited(int node) {
    return visited.get(node);
  }

  /**
   * Runs a full DFS forest over all nodes.
   * Forest roots are picked sequentially (0, 1, 2, …), skipping already-visited nodes
   * (including nodes visited by previous {@code run} calls or pre-marked nodes).
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
      visited.set(root);
      visitNode(root);
      root = visited.nextClearBit(root + 1);
    }
  }

  /**
   * Runs DFS from a single start node. Only visits nodes reachable from {@code startNode}
   * that have not been visited yet (by this or previous {@code run} calls).
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
    visited.set(startNode);
    visitNode(startNode);
  }

  /**
   * Runs DFS starting from the given nodes, in order. Only visits nodes reachable from
   * the specified start set that have not been visited yet.
   * Already-visited start nodes are skipped.
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(reachable nodes + reachable edges).
   *
   * @param startNodes the start nodes, visited in list order
   */
  public final void runFromNodes(IntList startNodes) {
    for (int i = 0; i < startNodes.size(); i++) {
      int node = startNodes.getInt(i);
      if (!visited.get(node)) {
        visited.set(node);
        visitNode(node);
      }
    }
  }

}
