package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Objects;

/**
 * The base class of a depth-first search of your own over a {@link Graph}, with a hook for every
 * edge. The hook also sees the edges to nodes already visited.
 * <p>
 * Like {@link DfsTraverser}, it calls {@link #enterNode(int)} when the search reaches a node and
 * {@link #leaveNode(int)} when it leaves the node. In between, it reports each out-edge of the
 * node to {@link #visitEdge(int, int, int, TargetState) visitEdge}, in edge index order, with the
 * state of the target: {@linkplain TargetState#UNVISITED unvisited},
 * {@linkplain TargetState#ON_STACK on the current path} or
 * {@linkplain TargetState#FINISHED finished}. The hook returns what the search does with the edge:
 * {@linkplain EdgeAction#DESCEND descend} into an unvisited target and search from it before the
 * next edge, {@linkplain EdgeAction#SKIP skip} the edge, or {@linkplain EdgeAction#STOP stop} the
 * run; any hook can stop the run with {@link #stop()}.
 * <p>
 * By default, the search descends into every unvisited target, and the hooks come in the order of
 * a textbook recursive
 * <a href="https://en.wikipedia.org/wiki/Depth-first_search">depth-first search</a>, as in
 * {@link DfsTraverser}. The search itself runs without recursion and keeps its stack on the heap.
 * An edge to a target on the current path closes a cycle. Here, {@code dependencies} are the
 * dependencies between the packages of a program, and the search stops at the first dependency
 * that closes a cycle:
 * {@snippet file="com/kobayami/graphity/examples/Traversals.java" region="visitEdge"}
 * {@link Graphs#findCycle(Graph)} returns such a cycle as a list of nodes. The state of a target
 * does not tell a forward edge from a cross edge; {@link EdgeTypes} classifies every edge.
 * <p>
 * The runs work as in {@link DfsTraverser}. The visited nodes add up over the runs of an instance.
 * A hook must not start another run of the same instance, which throws
 * {@link IllegalStateException}. After a run that ends with an exception, the state of the
 * instance is undefined. A search that needs no hook per edge is faster on {@link DfsTraverser}.
 * <p>
 * Instances are mutable and not safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 2 bits per node for the visited state, and up to 8 x nodeCount bytes
 * for the stack, which grows with the deepest path searched.
 */
public abstract class DfsEdgeTraverser {

  /** The state of the target of an edge when {@link #visitEdge visitEdge} reports the edge. */
  public enum TargetState {
    /** The search has not reached the target yet. */
    UNVISITED,
    /**
     * The target is on the current path - the source itself or one of its ancestors in the
     * search - so the edge closes a cycle.
     */
    ON_STACK,
    /**
     * The search has reached the target and left it, or a stopped run left it on its path.
     */
    FINISHED
  }

  /** What the search does with an edge that {@link #visitEdge visitEdge} reports. */
  public enum EdgeAction {
    /**
     * Enters the target if it is unvisited, and searches from it before the next edge; for any
     * other target, the same as {@link #SKIP}.
     */
    DESCEND,
    /**
     * Goes on with the next edge of the source. An unvisited target stays unvisited; a later edge
     * or run may still reach it.
     */
    SKIP,
    /**
     * Ends the run, as {@link DfsEdgeTraverser#stop() stop()} does.
     * {@link DfsEdgeTraverser#leaveNode(int) leaveNode} is not called for the nodes on the current
     * path, but they count as finished afterwards; an unvisited target stays unvisited.
     */
    STOP
  }

  // The two flags of a node in the packed state: reached by the search, and on the current path
  // (the gray set).
  private static final int VISITED = 0b01;
  private static final int ON_STACK = 0b10;

  // The target state by packed flags. On the path without visited (index 2) does not occur; it
  // holds ON_STACK only so that the table contains no null.
  private static final TargetState[] TARGET_STATES = {
      TargetState.UNVISITED, TargetState.FINISHED, TargetState.ON_STACK, TargetState.ON_STACK
  };

  /** The graph that this traverser searches. */
  protected final Graph graph;

  // Both flags of a node in one int, 16 nodes per int (bit 0 visited, bit 1 on the path), so that
  // a check of a node reads one int. Kept over all runs.
  private final int[] state;

  // The stack that replaces the call stack. A frame is two interleaved ints, the node at depth d
  // and its resume position in the adjacency array. The field only carries the grown capacity
  // from one run to the next.
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
  protected DfsEdgeTraverser(Graph graph) {
    this.graph = graph;
    int n = graph.nodeCount();
    this.state = new int[(n >> 4) + 1];
    this.frames = DfsFrames.initial(n);
  }

  /**
   * Called when the search reaches {@code node}, before its out-edges: the preorder hook. Returns
   * {@code true} unless overridden.
   * <p>
   * The search calls it once per node: for a start node, or for the target of an edge for which
   * {@link #visitEdge visitEdge} returned {@link EdgeAction#DESCEND DESCEND} while the target was
   * unvisited. From this call on, {@code node} counts as visited and is on the current path.
   * Returning {@code false} skips the out-edges of {@code node}: none of them is reported, and
   * {@link #leaveNode(int)} follows right away.
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return {@code true} to report the out-edges of {@code node}, {@code false} to skip them
   */
  protected boolean enterNode(int node) {
    return true;
  }

  /**
   * Called when the search leaves {@code node}, after all its out-edges have been reported and
   * searched: the postorder hook. Does nothing unless overridden.
   * <p>
   * The search calls it once for every node it has visited, right after
   * {@link #enterNode(int)} if that returned {@code false}. From this call on, {@code node} is no
   * longer on the current path. After the run is stopped, it is not called for the nodes still on
   * the path.
   *
   * @param node node ID in {@code 0..nodeCount-1}
   */
  protected void leaveNode(int node) {}

  /**
   * Called for each out-edge of a node that the search has entered, with the state of its target;
   * returns what the search does with the edge.
   * <p>
   * The search reports the out-edges of {@code source} in edge index order, between
   * {@link #enterNode(int) enterNode(source)} and {@link #leaveNode(int) leaveNode(source)}. It
   * reports no out-edge of a node whose {@code enterNode} returned {@code false}. The state is that
   * of the target when the edge is reported: an unvisited target that one edge skips is reported
   * as unvisited again by the next edge to it. Unless overridden, returns
   * {@link EdgeAction#DESCEND DESCEND} for an unvisited target and {@link EdgeAction#SKIP SKIP} for
   * any other.
   *
   * @param source    the source of the edge, which is on the current path
   * @param target    the target of the edge
   * @param edgeIndex edge index of the edge {@code source -> target}
   * @param state     the state of {@code target}
   * @return what the search does with the edge
   */
  protected EdgeAction visitEdge(int source, int target, int edgeIndex, TargetState state) {
    return state == TargetState.UNVISITED ? EdgeAction.DESCEND : EdgeAction.SKIP;
  }

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
   * node is on the path from its {@link #enterNode(int)} until its {@link #leaveNode(int)}.
   * Outside a run, no node is on the path.
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
   * It works from every hook; from {@link #visitEdge visitEdge}, it does the same as returning
   * {@link EdgeAction#STOP STOP}. {@link #leaveNode(int)} is not called for the nodes on the
   * current path, but they count as finished afterwards. This includes a node whose
   * {@link #enterNode(int)} called this method. A later run skips every node visited so far; it
   * does not go on where the stopped run ended. Called outside a run, this method has no effect. To
   * cancel a run from another thread, let a hook check a flag of your own and call this method.
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
   * unchanged. {@link EdgeAction#STOP STOP} and {@link #stop()} end the whole call, not only the
   * search from one start node.
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

  // The search from root, which is unvisited. The scan keeps i and end in locals rather than
  // fields across the hook call and touches the stack only when it descends. Returns false if the
  // run was stopped.
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
        int edge = i++;
        int v = adj[edge];
        int shift = (v & 0xF) << 1;
        int flags = (state[v >> 4] >>> shift) & (VISITED | ON_STACK);
        EdgeAction action = visitEdge(u, v, edge, TARGET_STATES[flags]);
        if (stopRequested) {
          action = EdgeAction.STOP;
        }
        switch (action) {
          case DESCEND:
            if (flags != 0) {
              break;
            }
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
                this.frames = frames;
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            }
            state[v >> 4] &= ~(ON_STACK << shift);
            leaveNode(v);
            if (stopRequested) {
              leaveStack(frames, top);
              return false;
            }
            break;
          case SKIP:
            break;
          case STOP:
            leaveStack(frames, top);
            return false;
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
