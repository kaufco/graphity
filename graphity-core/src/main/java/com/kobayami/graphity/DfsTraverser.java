package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Objects;

/**
 * Abstract base class for <strong>stack-safe</strong> depth-first traversal of a {@link Graph}.
 * <p>
 * It keeps its DFS stack on the heap rather than on the call stack, so
 * it never overflows even on graphs with very deep paths (DFS depth &ge; 10^6).
 * It is <strong>node-centric</strong>: subclasses override {@link #enterNode(int)} (preorder) and
 * optionally {@link #leaveNode(int)} (postorder). There are deliberately no per-edge hooks, which
 * keeps the traversal loop tight and fast.
 * <p>
 * This covers the large majority of user traversals: pre/postorder processing, subtree pruning,
 * reachability, DFS ordering, cycle detection via {@link #isOnStack(int)}, and depth tracking.
 * It cannot express edge-centric algorithms that must inspect edges to already-visited nodes;
 * for those, use {@link DfsEdgeTraverser}, which reports every edge with the state of
 * its target, or the ready-made {@link Components}, {@link EdgeTypes} and {@link BackEdges}.
 *
 * <h2>Example: parenthesized DFS trace</h2>
 * <pre>{@code
 * class TraceDfs extends DfsTraverser {
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
 * class ShallowDfs extends DfsTraverser {
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
 * <p>Discovery order is that of a textbook recursive depth-first search: out-neighbors are scanned
 * in ascending adjacency order, descending immediately into the first unvisited child.
 * <p>{@code run} methods may be called <strong>multiple times</strong>, and freely combined
 * (e.g., {@code run(int); run(); run(list)}). Already-visited nodes are never revisited -
 * the visited set is cumulative across calls. {@link #leaveNode(int)} is invoked exactly once for
 * every visited node, including nodes whose subtree was pruned by {@link #enterNode(int)}, unless
 * the traversal is stopped before with {@link #stop()}.
 * <p>A hook must not call a {@code run} method of the same instance; such a call throws
 * {@link IllegalStateException}. The list passed to {@link #run(IntList)} must not be modified
 * until that call returns. If a {@code run} call ends with an exception - thrown by a hook
 * or by the traversal itself - the state of the instance is undefined (nodes of the interrupted
 * path may still count as on the stack, for instance); use a new instance for further runs.
 * <p>
 * <strong>Memory:</strong> O(nodeCount) - visited/on-stack state is packed 2 bits per node into a
 * single {@code int[]}; the explicit stack grows on demand up to the longest DFS path.
 *
 * @see Graph
 * @see DfsEdgeTraverser
 * @see Graphs#rootNodes(BiAdjacentGraph)
 */
public abstract class DfsTraverser {

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
   * Explicit DFS stack, replacing the call stack; grows on demand. Each frame is two
   * interleaved ints: the node ID at depth {@code d} and its resume position in the
   * adjacency array, sharing a cache line. A raw array rather than a list, so that the
   * traversal loop keeps array reference and stack pointer in registers; the field only
   * carries the capacity across {@code run} calls.
   */
  private int[] frames;

  /** Set by {@link #stop()}; cleared when a {@code run} call starts. */
  private boolean stopRequested;

  /** Set while a {@code run} call is in progress, so that a hook cannot start another one. */
  private boolean running;

  /**
   * Creates a traverser for the given graph.
   *
   * @param graph the graph to traverse
   */
  protected DfsTraverser(Graph graph) {
    this.graph = graph;
    int n = graph.nodeCount();
    this.state = new int[(n >> 4) + 1];
    this.frames = DfsFrames.initial(n);
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
   * finished. Not called for the nodes on the current path when the traversal is stopped with
   * {@link #stop()}. Default: no-op.
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
   * Stops the traversal in progress: the current {@code run} call returns as soon as the hook
   * that calls this method returns, without calling any further hook. Nodes on the current DFS
   * path are not left - {@link #leaveNode(int)} is not called for them - and count as visited
   * and no longer on the stack afterwards. The visited state is kept, so a later {@code run} call
   * skips everything visited so far; the edges of the stopped path that were not scanned yet are
   * not resumed.
   * <p>
   * Callable from {@link #enterNode(int)} and {@link #leaveNode(int)}; outside a {@code run} call
   * it has no effect. To cancel from another thread, let a hook check a flag of your own and call
   * this method.
   * <p>
   * <strong>Time:</strong> O(1); the traversal checks for it after every hook call - twice per
   * node, not per edge.
   */
  protected final void stop() {
    stopRequested = true;
  }

  /**
   * Runs a full DFS forest over all nodes. Forest roots are picked sequentially (0, 1, 2, …),
   * skipping already-visited nodes (including nodes visited by previous {@code run} calls).
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   *
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   * @throws IllegalStateException if called from a hook, while a {@code run} call is in progress
   */
  public final void run() {
    begin();
    try {
      int n = graph.nodeCount();
      for (int root = 0; root < n; root++) {
        if (!dfsIfUnvisited(root)) {
          return;
        }
      }
    } finally {
      running = false;
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
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   * @throws IllegalStateException if called from a hook, while a {@code run} call is in progress
   */
  public final void run(int startNode) {
    Objects.checkIndex(startNode, graph.nodeCount());
    begin();
    try {
      dfsIfUnvisited(startNode);
    } finally {
      running = false;
    }
  }

  /**
   * Runs DFS starting from the given nodes, in list order. Only visits nodes reachable from
   * the specified start set that have not been visited yet. Already-visited start nodes are
   * skipped; {@link #stop()} ends the whole call, not just the current start node.
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(reachable nodes + reachable edges).
   *
   * @param startNodes the start nodes, each in {@code 0..nodeCount()-1}, visited in list order
   * @throws IndexOutOfBoundsException if a start node is out of range; then no node is visited
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   * @throws IllegalStateException if called from a hook, while a {@code run} call is in progress
   */
  public final void run(IntList startNodes) {
    Graphs.checkNodes(startNodes, graph.nodeCount());
    begin();
    try {
      for (int i = 0; i < startNodes.size(); i++) {
        if (!dfsIfUnvisited(startNodes.getInt(i))) {
          return;
        }
      }
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
   * The single entry of every {@code run} method: the visited check lives here only.
   *
   * @return {@code false} if the traversal was stopped
   */
  private boolean dfsIfUnvisited(int root) {
    return isVisited(root) || dfsFrom(root);
  }

  /**
   * Iterative DFS from {@code root} (assumed unvisited). The tight inner scan keeps
   * {@code i}/{@code end} in registers and touches the stack only when descending.
   *
   * @return {@code false} if the traversal was stopped
   */
  private boolean dfsFrom(int root) {
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);
    int[] state = this.state;
    int[] frames = this.frames;

    int rShift = (root & 0xF) << 1;
    state[root >> 4] |= (VISITED | ON_STACK) << rShift;
    boolean descend = enterNode(root);
    if (stopRequested) {
      state[root >> 4] &= ~(ON_STACK << rShift);
      return false;
    }
    if (!descend) {
      state[root >> 4] &= ~(ON_STACK << rShift);
      leaveNode(root);
      return !stopRequested;
    }
    frames[0] = root;
    frames[1] = out[root];
    int top = 0;

    outer:
    while (top >= 0) {
      int u = frames[2 * top];
      int end = out[u + 1];
      int i = frames[2 * top + 1];
      while (i < end) {
        int v = adj[i++];
        int shift = (v & 0xF) << 1;
        if (((state[v >> 4] >>> shift) & VISITED) == 0) {  // tree edge
          state[v >> 4] |= (VISITED | ON_STACK) << shift;
          descend = enterNode(v);
          if (stopRequested) {
            state[v >> 4] &= ~(ON_STACK << shift);
            leaveStack(frames, top);
            return false;
          }
          if (descend) {
            frames[2 * top + 1] = i;
            if (++top == frames.length >> 1) {
              frames = DfsFrames.grow(frames, graph.nodeCount());
              this.frames = frames;  // keep the grown capacity for later run calls
            }
            frames[2 * top] = v;
            frames[2 * top + 1] = out[v];
            continue outer;
          }
          // The frame's cursor is saved only on descent; u's scan goes on from the local i.
          state[v >> 4] &= ~(ON_STACK << shift);
          leaveNode(v);
          if (stopRequested) {
            leaveStack(frames, top);
            return false;
          }
        }
      }
      state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
      leaveNode(u);
      top--;
      if (stopRequested) {
        leaveStack(frames, top);
        return false;
      }
    }
    return true;
  }

  /** Clears the on-stack flag of every node in {@code frames[0..top]}, once per stop. */
  private void leaveStack(int[] frames, int top) {
    for (int d = 0; d <= top; d++) {
      int w = frames[2 * d];
      state[w >> 4] &= ~(ON_STACK << ((w & 0xF) << 1));
    }
  }
}
