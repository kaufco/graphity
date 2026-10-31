package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Objects;

/**
 * The base class of a depth-first search of your own over a {@link Graph}. A subclass overrides
 * the hooks {@link #enterNode(int)} and {@link #leaveNode(int)}, and a {@code run} method starts
 * the search.
 * <p>
 * The search visits each node it reaches once. It calls {@link #enterNode(int)} when it reaches a
 * node (preorder) and {@link #leaveNode(int)} when it has searched from all out-nodes of the node
 * (postorder). It takes the out-nodes of each node in ascending order, so the hooks come in the
 * order of a textbook recursive
 * <a href="https://en.wikipedia.org/wiki/Depth-first_search">depth-first search</a>. The search
 * itself runs without recursion and keeps its stack on the heap, so a deep graph does not overflow
 * the call stack. Here, {@code calls} are the calls between the methods of a program, and the
 * postorder puts each method after the methods it calls, except along calls that close a cycle:
 * {@snippet file="com/kobayami/graphity/examples/Traversals.java" region="leaveNode"}
 * <p>
 * Between its two hooks, a node is on the current path. The current path leads from the node where
 * the search started to the node being searched. The hooks may call {@link #isVisited(int)} and
 * {@link #isOnStack(int)}, end the search at a node by returning {@code false} from
 * {@link #enterNode(int)}, and end the whole run with {@link #stop()}.
 * <p>
 * {@link #run()} searches from every node in ascending order, {@link #run(int)} from one node and
 * {@link #run(IntList)} from several, such as the root nodes that
 * {@link Graphs#rootNodes(BiAdjacentGraph)} returns. The visited nodes add up over the runs of an
 * instance: a node is visited at most once in the lifetime of the instance, and a later run skips
 * it. A hook must not start another run of the same instance, which throws
 * {@link IllegalStateException}. If a run ends with an exception from a hook or from the search,
 * the state of the instance is undefined: nodes of the interrupted path may still count as on the
 * path, for example. A new instance is needed for further runs.
 * <p>
 * {@link DfsEdgeTraverser} also calls a hook for every edge, which suits searches that look at
 * edges to nodes already visited. This class calls no such hook, which makes it the faster choice
 * where node hooks suffice. {@link FrontierTraverser} searches breadth-first. Common searches are
 * ready-made, such as {@link TopOrder}, {@link EdgeTypes}, {@link BackEdges} and
 * {@link Graphs#findCycle(Graph)}.
 * <p>
 * Instances are mutable and not safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 2 bits per node for the visited state, and up to 8 x nodeCount bytes
 * for the stack, which grows with the deepest path searched.
 */
public abstract class DfsTraverser {

  // The two flags of a node in the packed state: reached by the search, and on the current path
  // (the gray set).
  private static final int VISITED = 0b01;
  private static final int ON_STACK = 0b10;

  /** The graph that this traverser searches. */
  protected final Graph graph;

  // Both flags of a node in one int, 16 nodes per int (bit 0 visited, bit 1 on the path), so that
  // a check of a node reads one int instead of two bit sets. Kept over all runs.
  private final int[] state;

  // The stack that replaces the call stack. A frame is two interleaved ints, the node at depth d
  // and its resume position in the adjacency array, on one cache line. A raw array rather than a
  // list, so that the loop keeps the array and the top index in registers; the field only carries
  // the grown capacity from one run to the next.
  private int[] frames;

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
  protected DfsTraverser(Graph graph) {
    this.graph = graph;
    int n = graph.nodeCount();
    this.state = new int[(n >> 4) + 1];
    this.frames = DfsFrames.initial(n);
  }

  /**
   * Called when the search reaches {@code node}, before its out-nodes: the preorder hook.
   * <p>
   * The search calls it once per node: for a start node, or for an unvisited out-node of the node
   * it is searching. From this call on, {@code node} counts as visited and is on the current path.
   * Returning {@code true} lets the search go on with the out-nodes of {@code node}. Returning
   * {@code false} skips them, and {@link #leaveNode(int)} follows right away; out-nodes skipped
   * this way stay unvisited unless the search reaches them from another node.
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return {@code true} to search from the out-nodes of {@code node}, {@code false} to skip them
   */
  protected abstract boolean enterNode(int node);

  /**
   * Called when the search leaves {@code node}, after it has searched from all its out-nodes: the
   * postorder hook. Does nothing unless overridden.
   * <p>
   * The search calls it once for every node it has visited, right after
   * {@link #enterNode(int)} if that returned {@code false}. From this call on, {@code node} is no
   * longer on the current path. After {@link #stop()}, it is not called for the nodes still on
   * the path.
   *
   * @param node node ID in {@code 0..nodeCount-1}
   */
  protected void leaveNode(int node) {}

  /**
   * Returns whether the search has reached {@code node}, in this run or in an earlier one.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}; for any other value the method returns
   *             {@code false} or throws {@code IndexOutOfBoundsException}
   * @return {@code true} if {@link #enterNode(int)} has been called for {@code node}
   */
  protected final boolean isVisited(int node) {
    return (state[node >> 4] >>> ((node & 0xF) << 1) & VISITED) != 0;
  }

  /**
   * Returns whether {@code node} is on the stack of the search, that is, on the current path.
   * <p>
   * The current path leads from the node where the search started to the node being searched. A
   * node is on the path from its {@link #enterNode(int)} until its {@link #leaveNode(int)}. An
   * edge from the node being searched to a node on the path closes a cycle. Outside a run, no node
   * is on the path.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}; for any other value the method returns
   *             {@code false} or throws {@code IndexOutOfBoundsException}
   * @return {@code true} if {@code node} is on the current path
   */
  protected final boolean isOnStack(int node) {
    return (state[node >> 4] >>> ((node & 0xF) << 1) & ON_STACK) != 0;
  }

  /**
   * Stops the run in progress: the run returns as soon as the hook that calls this method
   * returns, without calling another hook.
   * <p>
   * {@link #leaveNode(int)} is not called for the nodes on the current path, but they count as
   * visited and as no longer on the path. A later run skips every node visited so far; it does not
   * go on where the stopped run ended. Called outside a run, this method has no effect. To cancel
   * a run from another thread, let a hook check a flag of your own and call this method.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  protected final void stop() {
    stopRequested = true;
  }

  /**
   * Searches from every node not visited yet, in ascending order, until every node is visited.
   * <p>
   * A node that no earlier search has reached when its turn comes starts a search of its own.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), plus the hooks.
   *
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   * @throws IllegalStateException if called from a hook, during a run of this instance
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
   * Searches from {@code startNode}, visiting the nodes it reaches that are not visited yet.
   * <p>
   * If {@code startNode} is visited already, nothing happens.
   * <p>
   * <strong>Time:</strong> O(reachable nodes + reachable edges), plus the hooks.
   *
   * @param startNode node ID in {@code 0..nodeCount-1}
   * @throws IndexOutOfBoundsException if {@code startNode} is out of range
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   * @throws IllegalStateException if called from a hook, during a run of this instance
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
   * Searches from each of {@code startNodes} in list order, skipping those visited already.
   * <p>
   * The list is checked before the first hook, so a start node out of range leaves the traverser
   * unchanged. {@link #stop()} ends the whole call, not only the search from one start node.
   * <p>
   * <strong>Time:</strong> O(startNodes.size() + reachable nodes + reachable edges), plus the
   * hooks.
   *
   * @param startNodes node IDs, each in {@code 0..nodeCount-1}; not to be changed until the call
   *                   returns
   * @throws IndexOutOfBoundsException if a start node is out of range
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   * @throws IllegalStateException if called from a hook, during a run of this instance
   */
  public final void run(IntList startNodes) {
    Checks.checkNodes(startNodes, graph.nodeCount());
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

  // Starts a run after its argument checks: rejects a call from a hook and clears an earlier
  // stop(). The caller clears running in a finally.
  private void begin() {
    if (running) {
      throw new IllegalStateException("a run is in progress; a hook must not start another run");
    }
    running = true;
    stopRequested = false;
  }

  // The single entry of every run method, so that the visited check lives here only. Returns false
  // if the run was stopped.
  private boolean dfsIfUnvisited(int root) {
    return isVisited(root) || dfsFrom(root);
  }

  // The search from root, which is unvisited. The inner scan keeps i and end in registers and
  // touches the stack only when it descends. Returns false if the run was stopped.
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

  // Clears the on-stack flag of every node in frames[0..top], once per stop.
  private void leaveStack(int[] frames, int top) {
    for (int d = 0; d <= top; d++) {
      int w = frames[2 * d];
      state[w >> 4] &= ~(ON_STACK << ((w & 0xF) << 1));
    }
  }
}
