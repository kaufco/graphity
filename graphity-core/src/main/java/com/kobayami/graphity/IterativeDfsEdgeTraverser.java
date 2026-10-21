package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Objects;

/**
 * Abstract base class for <strong>stack-safe, edge-centric</strong> depth-first traversal of a
 * {@link Graph}.
 * <p>
 * The edge-centric sibling of {@link IterativeDfsTraverser}: besides {@link #enterNode(int)} and
 * {@link #leaveNode(int)}, every edge leaving the current node is reported to
 * {@link #visitEdge(int, int, int, TargetState)}, together with the state of its target -
 * {@linkplain TargetState#UNVISITED unvisited}, {@linkplain TargetState#ON_STACK on the current
 * path} or {@linkplain TargetState#FINISHED finished}. The hook returns what the traversal does
 * with the edge: {@linkplain EdgeAction#DESCEND descend} into an unvisited target,
 * {@linkplain EdgeAction#SKIP skip} the edge, or {@linkplain EdgeAction#STOP stop} the whole
 * traversal. The stack lives on the heap, so depth is limited by memory, not by {@code -Xss}.
 * <p>
 * An edge to a target on the current path closes a cycle; that is enough for cycle detection and
 * for collecting back edges. The target state does not tell forward from cross edges, which needs
 * a discovery time per node; for the full classification use {@link EdgeTypes}.
 * <p>
 * The decision stays in the traversal loop and the hook only returns an action, so the loop keeps
 * its cursor in local variables. A traversal that needs no per-edge logic is faster on
 * {@link IterativeDfsTraverser}, which calls no hook per edge.
 *
 * <h2>Example: find a cycle and stop</h2>
 * <pre>{@code
 * class CycleFinder extends IterativeDfsEdgeTraverser {
 *     int closingEdge = -1;
 *     CycleFinder(Graph graph) { super(graph); }
 *     @Override protected EdgeAction visitEdge(int source, int target, int edgeIndex,
 *                                              TargetState state) {
 *         if (state == TargetState.ON_STACK) {
 *             closingEdge = edgeIndex;
 *             return EdgeAction.STOP;
 *         }
 *         return super.visitEdge(source, target, edgeIndex, state);
 *     }
 * }
 * var finder = new CycleFinder(graph);
 * finder.run();
 * boolean acyclic = finder.closingEdge < 0;
 * }</pre>
 *
 * <h2>Example: depth-first search from one node, confined to a subset of nodes</h2>
 * <pre>{@code
 * class ConfinedDfs extends IterativeDfsEdgeTraverser {
 *     final BitSet allowed;
 *     ConfinedDfs(Graph graph, BitSet allowed) {
 *         super(graph);
 *         this.allowed = allowed;
 *     }
 *     @Override protected EdgeAction visitEdge(int source, int target, int edgeIndex,
 *                                              TargetState state) {
 *         return state == TargetState.UNVISITED && allowed.get(target)
 *             ? EdgeAction.DESCEND : EdgeAction.SKIP;
 *     }
 * }
 * new ConfinedDfs(graph, allowed).run(start);   // run() would also enter roots outside the set
 * }</pre>
 *
 * <p>With the default {@link #visitEdge}, discovery order is identical to
 * {@link IterativeDfsTraverser}: out-edges are reported in ascending adjacency order, and the
 * traversal descends immediately into the first unvisited target.
 * <p>{@code run} methods may be called <strong>multiple times</strong>, and freely combined.
 * Already-visited nodes are never revisited - the visited set is cumulative across calls. A target
 * skipped while unvisited stays unvisited; a later edge or {@code run} call may still reach it.
 * {@link #leaveNode(int)} is invoked exactly once for every visited node, including nodes whose
 * subtree was pruned by {@link #enterNode(int)}, unless the traversal is stopped before
 * ({@link EdgeAction#STOP} or {@link #stop()}).
 * <p>
 * <strong>Memory:</strong> O(nodeCount) - visited/on-stack state is packed 2 bits per node into a
 * single {@code int[]}; the explicit stack grows on demand up to the longest DFS path.
 *
 * @see IterativeDfsTraverser
 * @see EdgeTypes
 */
public abstract class IterativeDfsEdgeTraverser {

  /** The state of an edge's target when the edge is reported. */
  public enum TargetState {
    /** The target has not been discovered yet. */
    UNVISITED,
    /**
     * The target lies on the current DFS path - it is the source itself or one of its
     * ancestors - so the edge closes a cycle.
     */
    ON_STACK,
    /** The target has been discovered and left. */
    FINISHED
  }

  /** What the traversal does with a reported edge. */
  public enum EdgeAction {
    /**
     * Descends into the target if it is unvisited; for any other target this is the same as
     * {@link #SKIP}.
     */
    DESCEND,
    /** Ignores the edge and continues with the next edge of the source. */
    SKIP,
    /**
     * Ends the current {@code run} call. Nodes on the current path are not left -
     * {@link #leaveNode(int)} is not called for them - and count as finished afterwards.
     */
    STOP
  }

  /** Bit 0 of a node's packed state: the node has been discovered. */
  private static final int VISITED = 0b01;

  /** Bit 1 of a node's packed state: the node is currently on the DFS path (gray set). */
  private static final int ON_STACK = 0b10;

  /** Target state by packed flags; on-stack without visited does not occur. */
  private static final TargetState[] TARGET_STATES = {
      TargetState.UNVISITED, TargetState.FINISHED, null, TargetState.ON_STACK
  };

  /** The graph being traversed. */
  protected final Graph graph;

  /**
   * Packed per-node state: visited and on-stack flags, 2 bits per node, 16 nodes per int
   * (bit 0 = visited, bit 1 = on-stack). Cumulative across {@code run} calls.
   */
  private final int[] state;

  /**
   * Explicit DFS stack, replacing the call stack; grows on demand. Each frame is two
   * interleaved ints: the node ID at depth {@code d} and its resume position in the
   * adjacency array. The field only carries the capacity across {@code run} calls.
   */
  private int[] frames;

  /** Set by {@link #stop()}; cleared when a {@code run} call starts. */
  private boolean stopRequested;

  /**
   * Creates a traverser for the given graph.
   *
   * @param graph the graph to traverse
   */
  protected IterativeDfsEdgeTraverser(Graph graph) {
    this.graph = graph;
    int n = graph.nodeCount();
    this.state = new int[(n >> 4) + 1];
    this.frames = DfsFrames.initial(n);
  }

  /**
   * Preorder hook, called once when {@code node} is discovered. Default: returns {@code true}.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} to go on with the node's out-edges; {@code false} to prune the subtree
   *     (the node is treated like a leaf - its out-edges are not reported and
   *     {@link #leaveNode(int)} still runs)
   */
  protected boolean enterNode(int node) {
    return true;
  }

  /**
   * Postorder hook, called once per visited node, after all its out-edges have been reported
   * (or right after {@link #enterNode(int)} if that pruned the subtree). Not called for the nodes
   * on the current path when the traversal is stopped with {@link EdgeAction#STOP} or
   * {@link #stop()}. Default: no-op.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   */
  protected void leaveNode(int node) {}

  /**
   * Edge hook, called once for every out-edge of a node that {@link #enterNode(int)} did not
   * prune, in ascending adjacency order, between the source's {@code enterNode} and
   * {@code leaveNode} - until the traversal is stopped, after which no edge is reported.
   * Default: {@link EdgeAction#DESCEND} into an unvisited target, {@link EdgeAction#SKIP} for any
   * other.
   * <p>
   * The target state is taken when the edge is reported. A target skipped while
   * {@link TargetState#UNVISITED} may be reported as unvisited again by a later edge.
   *
   * @param source the node the edge leaves, which is on the current path
   * @param target the node the edge enters
   * @param edgeIndex the edge's index, as used by {@link Graph#edgeTarget(int)}
   * @param state the target's state
   * @return what to do with the edge; not {@code null}
   */
  protected EdgeAction visitEdge(int source, int target, int edgeIndex, TargetState state) {
    return state == TargetState.UNVISITED ? EdgeAction.DESCEND : EdgeAction.SKIP;
  }

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
   * ancestor of it).
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
   * that calls this method returns, without calling any further hook - like returning
   * {@link EdgeAction#STOP} from {@link #visitEdge}, but callable from every hook. Called from
   * {@link #enterNode(int)}, it leaves the entered node visited, so it counts as finished
   * afterwards like the rest of the path; a target that {@code STOP} declines stays unvisited.
   * <p>
   * Outside a {@code run} call it has no effect. To cancel from another thread, let a hook check a
   * flag of your own and call this method.
   * <p>
   * <strong>Time:</strong> O(1); the traversal checks for it once per hook call.
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
   * <strong>Time:</strong> O(nodeCount + edgeCount) plus the hooks.
   *
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   */
  public final void run() {
    stopRequested = false;
    int n = graph.nodeCount();
    for (int root = 0; root < n; root++) {
      if (!dfsIfUnvisited(root)) {
        return;
      }
    }
  }

  /**
   * Runs DFS from a single start node. Only visits nodes reachable from {@code startNode}
   * that have not been visited yet (by this or previous {@code run} calls).
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(reachable nodes + reachable edges) plus the hooks.
   *
   * @param startNode the start node
   * @throws IndexOutOfBoundsException if {@code startNode} is out of range
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   */
  public final void run(int startNode) {
    Objects.checkIndex(startNode, graph.nodeCount());
    stopRequested = false;
    dfsIfUnvisited(startNode);
  }

  /**
   * Runs DFS starting from the given nodes, in list order. Only visits nodes reachable from
   * the specified start set that have not been visited yet. Already-visited start nodes are
   * skipped; {@link EdgeAction#STOP} and {@link #stop()} end the whole call, not just the current
   * start node.
   * <p>
   * May be called multiple times, and freely combined with other {@code run} methods.
   * <p>
   * <strong>Time:</strong> O(reachable nodes + reachable edges) plus the hooks.
   *
   * @param startNodes the start nodes, visited in list order
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   */
  public final void runFromNodes(IntList startNodes) {
    stopRequested = false;
    for (int i = 0; i < startNodes.size(); i++) {
      if (!dfsIfUnvisited(startNodes.getInt(i))) {
        return;
      }
    }
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
   * Iterative DFS from {@code root} (assumed unvisited). The scan keeps {@code i}/{@code end} in
   * locals rather than fields across the hook call and touches the stack only when descending.
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
        int c = i++;
        int v = adj[c];
        int shift = (v & 0xF) << 1;
        int flags = (state[v >> 4] >>> shift) & (VISITED | ON_STACK);
        EdgeAction action = visitEdge(u, v, c, TARGET_STATES[flags]);
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

  /** Clears the on-stack flag of every node in {@code frames[0..top]}, once per stop. */
  private void leaveStack(int[] frames, int top) {
    for (int d = 0; d <= top; d++) {
      int w = frames[2 * d];
      state[w >> 4] &= ~(ON_STACK << ((w & 0xF) << 1));
    }
  }
}
