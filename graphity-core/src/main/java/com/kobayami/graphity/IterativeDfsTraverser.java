package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Objects;

/**
 * Abstract base class for <strong>stack-safe</strong> depth-first traversal of a {@link Graph}.
 * <p>
 * Unlike the recursive {@link DfsTraverser}, this traverser keeps its DFS stack on the heap, so
 * it never overflows the call stack even on graphs with very deep paths (DFS depth &ge; 10^6).
 * It is <strong>node-centric</strong>: subclasses override {@link #enterNode(int)} (preorder) and
 * optionally {@link #leaveNode(int)} (postorder). There are deliberately no per-edge hooks, which
 * keeps the traversal loop tight and fast.
 * <p>
 * This covers the large majority of user traversals: pre/postorder processing, subtree pruning,
 * reachability, DFS ordering, cycle detection via {@link #isOnStack(int)}, and depth tracking.
 * It cannot express edge-centric algorithms (e.g. Tarjan SCC) that must inspect edges to
 * already-visited nodes; for those, use the ready-made {@link Components}, {@link EdgeTypes},
 * {@link BackEdges}, or the recursive {@link DfsTraverser} with manual edge iteration.
 *
 * <h2>Example: parenthesized DFS trace</h2>
 * <pre>{@code
 * class TraceDfs extends IterativeDfsTraverser {
 *     TraceDfs(Graph graph) { super(graph); }
 *     @Override protected boolean enterNode(int node) {
 *         System.out.print("(" + node);
 *         return true;   // descend into children
 *     }
 *     @Override protected void leaveNode(int node) {
 *         System.out.print(")");
 *     }
 * }
 * new TraceDfs(graph).run();
 * }</pre>
 *
 * <h2>Example: depth-limited DFS (cut)</h2>
 * <pre>{@code
 * class ShallowDfs extends IterativeDfsTraverser {
 *     final int maxDepth;
 *     int depth;
 *     ShallowDfs(Graph graph, int maxDepth) {
 *         super(graph);
 *         this.maxDepth = maxDepth;
 *     }
 *     @Override protected boolean enterNode(int node) {
 *         process(node);
 *         depth++;
 *         return depth < maxDepth;   // false → prune the subtree
 *     }
 *     @Override protected void leaveNode(int node) {
 *         depth--;
 *     }
 * }
 * }</pre>
 *
 * <p>Discovery order is identical to the recursive {@link DfsTraverser}: out-neighbors are scanned
 * in ascending adjacency order, descending immediately into the first unvisited child.
 * <p>{@code run} methods may be called <strong>multiple times</strong>, and freely combined
 * (e.g., {@code run(int); run(); runFromNodes(list)}). Already-visited nodes are never revisited -
 * the visited set is cumulative across calls. {@link #leaveNode(int)} is invoked exactly once for
 * every visited node, including nodes whose subtree was pruned by {@link #enterNode(int)}.
 * <p>
 * <strong>Memory:</strong> O(nodeCount) - visited/on-stack state is packed 2 bits per node into a
 * single {@code int[]}; the explicit stack grows on demand up to the longest DFS path.
 *
 * @see Graph
 * @see DfsTraverser
 */
public abstract class IterativeDfsTraverser {

  /** Bit 0 of a node's packed state: the node has been discovered. */
  private static final int VISITED = 0b01;

  /** Bit 1 of a node's packed state: the node is currently on the DFS path (gray set). */
  private static final int ON_STACK = 0b10;

  /** The graph being traversed. */
  protected final Graph graph;

  /**
   * Packed per-node state: visited and on-stack flags, 2 bits per node, 16 nodes per int
   * (bit 0 = visited, bit 1 = on-stack). One memory touch per node check instead of two
   * separate bit-set lookups. Cumulative across {@code run} calls.
   */
  private final int[] state;

  /**
   * Explicit DFS stack, replacing the call stack; grows on demand. Each frame packs
   * {@code (node << 32 | resume cursor)} into one {@code long}: the node ID at depth
   * {@code d} and the next adjacency index to scan for it share a cache line, and the
   * backing array grows with one allocation per resize instead of two.
   */
  private final LongArrayList frames;

  /**
   * Creates a traverser for the given graph.
   *
   * @param graph the graph to traverse
   */
  protected IterativeDfsTraverser(Graph graph) {
    this.graph = graph;
    int n = graph.nodeCount();
    this.state = new int[(n >> 4) + 1];
    this.frames = new LongArrayList(Math.max(16, Math.min(n, 1024)));
  }

  /**
   * Preorder hook, called once when {@code node} is discovered.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} to descend into the node's children; {@code false} to prune the subtree
   *     (the node is treated like a leaf - {@link #leaveNode(int)} still runs)
   */
  protected abstract boolean enterNode(int node);

  /**
   * Postorder hook, called once per visited node, after its subtree (if not pruned) has
   * finished. Default: no-op.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   */
  protected void leaveNode(int node) {}

  /**
   * Returns whether {@code node} has already been discovered.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if visited
   */
  protected final boolean isVisited(int node) {
    return (state[node >> 4] >>> ((node & 0xF) << 1) & VISITED) != 0;
  }

  /**
   * Returns whether {@code node} is currently on the DFS path (i.e. it is the current node or an
   * ancestor of it). This is the gray set that edge-centric algorithms would otherwise maintain
   * in their own {@code onStack} bit-set.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if {@code node} lies on the current DFS path
   */
  protected final boolean isOnStack(int node) {
    return (state[node >> 4] >>> ((node & 0xF) << 1) & ON_STACK) != 0;
  }

  /**
   * Runs a full DFS forest over all nodes. Forest roots are picked sequentially (0, 1, 2, …),
   * skipping already-visited nodes (including nodes visited by previous {@code run} calls).
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   */
  public final void run() {
    int n = graph.nodeCount();
    for (int root = 0; root < n; root++) {
      if (!isVisited(root)) {
        dfsFrom(root);
      }
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
    if (!isVisited(startNode)) {
      dfsFrom(startNode);
    }
  }

  /**
   * Runs DFS starting from the given nodes, in list order. Only visits nodes reachable from
   * the specified start set that have not been visited yet. Already-visited start nodes are
   * skipped.
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
      if (!isVisited(node)) {
        dfsFrom(node);
      }
    }
  }

  /**
   * Iterative DFS from {@code root} (assumed unvisited). The tight inner scan keeps
   * {@code i}/{@code end} in registers and touches the stack/cursor arrays only when
   * actually descending.
   */
  private void dfsFrom(int root) {
    int[] out = graph.outAdjacencyStart;
    int[] adj = graph.outAdjacency;
    int[] state = this.state;
    LongArrayList frames = this.frames;

    int rShift = (root & 0xF) << 1;
    state[root >> 4] |= (VISITED | ON_STACK) << rShift;
    if (!enterNode(root)) {
      // Pruned root: no descent, immediate postorder.
      state[root >> 4] &= ~(ON_STACK << rShift);
      leaveNode(root);
      return;
    }
    frames.add(((long) root << 32) | (out[root] & 0xFFFFFFFFL));

    outer:
    while (!frames.isEmpty()) {
      int top = frames.size() - 1;
      long frame = frames.getLong(top);
      int u = (int) (frame >>> 32);
      int end = out[u + 1];
      int i = (int) frame;
      while (i < end) {
        int v = adj[i++];
        int shift = (v & 0xF) << 1;
        if (((state[v >> 4] >>> shift) & VISITED) == 0) {  // tree edge
          state[v >> 4] |= (VISITED | ON_STACK) << shift;
          if (enterNode(v)) {
            frames.set(top, (frame & 0xFFFFFFFF00000000L) | (i & 0xFFFFFFFFL));  // save u's resume point before descending
            frames.add(((long) v << 32) | (out[v] & 0xFFFFFFFFL));
            continue outer;
          }
          // Pruned: no descent, immediate postorder. u's scan continues with the
          // already-advanced local i (the frame's cursor is only updated when descending).
          state[v >> 4] &= ~(ON_STACK << shift);
          leaveNode(v);
        }
      }
      // Postorder u.
      state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
      leaveNode(u);
      frames.size(top);
    }
  }
}
