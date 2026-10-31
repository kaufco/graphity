package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;

/**
 * The SCCs (strongly connected components) of a graph, as {@link Components#sccsOf(Graph)}
 * returns them.
 * <p>
 * It is a {@link NodePartition} whose groups are the SCCs, numbered by SCC ID
 * {@code 0..sccCount()-1}. {@link #sccOf(int)} returns the SCC of a node and {@link #nodesOf(int)}
 * the nodes of an SCC. {@link #containsCycle(int)} distinguishes the SCCs with a cycle from those
 * of a single node without one. {@link #condense()} returns the DAG (directed acyclic graph) of
 * the SCCs.
 * <p>
 * Every node is in exactly one SCC; a node that lies on no cycle is an SCC of its own. SCC IDs are
 * in reverse topological order: every edge between two SCCs goes from the higher SCC ID to the
 * lower one, so SCC {@code 0} has no edges to other SCCs. The nodes of each SCC are in ascending
 * order. Here, {@code dependencies} are the dependencies between the packages of a program, and
 * {@code names} holds the names of the packages. The example groups the packages by SCC, in the
 * order of the SCC IDs. In this order, each package comes after the packages it uses, and the
 * packages that use each other come together:
 * {@snippet file="com/kobayami/graphity/examples/Connectivity.java" region="sccsOf"}
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x (2 x nodeCount + sccCount) bytes; the graph is shared, not
 * copied.
 */
public final class Sccs extends NodePartition {

  Sccs(Graph graph, int[] nodeToScc, int[] sccNodes, int[] sccStart) {
    super(graph, nodeToScc, sccNodes, sccStart);
  }

  // The body of Components.sccsOf, here so that the algorithms that need SCCs do not call the
  // facade.
  static Sccs of(Graph graph) {
    var tarjan = new TarjanTraverser(graph);
    tarjan.run();
    return tarjan.buildResult();
  }

  /**
   * Returns the SCC ID of {@code node}. Equivalent to {@link #get(int) get(node)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return SCC ID
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int sccOf(int node) {
    return get(node);
  }

  /**
   * Returns the number of SCCs. Equivalent to {@link #groupCount()}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return SCC count
   */
  public int sccCount() {
    return groupCount();
  }

  /**
   * Returns whether SCC {@code sccId} contains a cycle: whether it has more than one node, or a
   * single node with a self-loop.
   * <p>
   * <strong>Time:</strong> O(1) for an SCC of more than one node, O(log outDegree(v)) for an SCC
   * of a single node {@code v}.
   *
   * @param sccId SCC ID in {@code 0..sccCount()-1}
   * @return {@code true} if the SCC contains a cycle
   * @throws IndexOutOfBoundsException if {@code sccId} is out of range
   */
  public boolean containsCycle(int sccId) {
    Objects.checkIndex(sccId, groupCount());
    return memberStart[sccId + 1] - memberStart[sccId] > 1
        || graph.containsSelfLoop(members[memberStart[sccId]]);
  }

  /**
   * Returns whether the graph is strongly connected: whether it has at most one SCC. Equivalent to
   * {@code sccCount() <= 1}.
   * <p>
   * A graph without nodes counts as strongly connected.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if there is at most one SCC
   */
  public boolean isStronglyConnected() {
    return groupCount() <= 1;
  }

  // Tarjan's algorithm with an explicit frame stack, so deep graphs cannot overflow the call
  // stack. An SCC closes only after every SCC it reaches, which puts the SCC IDs in reverse
  // topological order.
  //
  // The per-node state - index, lowlink, visited and on-stack - lives in two interleaved int lanes
  // per node, index | visited << 31 and lowlink | onStack << 31: index and lowlink are below
  // nodeCount, so 31 bits each suffice. Every node touch is then one cache line instead of two int
  // arrays plus two bit sets, and the layout stays within 32-bit arithmetic, which a port to a
  // language without 64-bit integer operators can mirror. Two lanes per node cap the node count at
  // ArrayLimits.MAX_NODE_COUNT_TWO_INTS, 2^30 - 5; a depth-first path through every node then
  // still fits into the frame stack.
  //
  // sccStack holds the nodes not yet assigned to a completed SCC: a node can be finished in the
  // search and still wait there until the root of its SCC finishes.
  private static final class TarjanTraverser {

    private static final int VALUE = Integer.MAX_VALUE;   // bits 0-30 of a lane
    // bit 31: visited in lane 0, on-stack in lane 1
    private static final int FLAG = Integer.MIN_VALUE;

    private final Graph graph;
    private final int[] lanes;   // per node: index | visited << 31, lowlink | onStack << 31
    private final int[] sccStack;
    private int nextIndex;
    private int sccTop;

    private final int[] nodeToScc;
    private final int[] sccNodes;
    private final int[] sccStart;
    private int sccCount;
    private int sccNodesPos;

    TarjanTraverser(Graph graph) {
      this.graph = graph;
      int n = graph.nodeCount();
      ArrayLimits.checkNodeCount(n, ArrayLimits.MAX_NODE_COUNT_TWO_INTS, "Tarjan's algorithm");
      this.lanes = new int[2 * n];
      this.sccStack = new int[n];
      this.nodeToScc = new int[n];
      this.sccNodes = new int[n];
      this.sccStart = new int[n + 1];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = Csr.outAdjacencyStart(graph);
      int[] adj = Csr.outAdjacency(graph);
      int[] lanes = this.lanes;

      // Each frame is two interleaved ints, the node and its resume position in the
      // adjacency array: both share a cache line, and the array grows on demand.
      int[] frames = DfsFrames.initial(n);

      for (int root = 0; root < n; root++) {
        if ((lanes[2 * root] & FLAG) != 0) {
          continue;
        }
        preorder(root);
        frames[0] = root;
        frames[1] = out[root];
        int top = 0;

        outer:
        while (top >= 0) {
          int u = frames[2 * top];
          int end = out[u + 1];
          // Tight neighbor scan: i and end stay in registers; the frame list is
          // touched only when we actually descend, not once per edge.
          int i = frames[2 * top + 1];
          while (i < end) {
            int v = adj[i++];
            if ((lanes[2 * v] & FLAG) == 0) {
              frames[2 * top + 1] = i;  // resume here after v's subtree finishes
              preorder(v);
              if (++top == frames.length >> 1) {
                frames = DfsFrames.grow(frames, n);
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            } else if ((lanes[2 * v + 1] & FLAG) != 0) {   // same cache line as v's lane 0
              // lowlink[u] = min(lowlink[u], index[v])
              int idxV = lanes[2 * v] & VALUE;
              int lowLane = lanes[2 * u + 1];
              if (idxV < (lowLane & VALUE)) {
                lanes[2 * u + 1] = (lowLane & FLAG) | idxV;
              }
            }
          }

          // Postorder: u is finished. Close its SCC if it is a root, then propagate its
          // lowlink to the parent (the node beneath it on the DFS stack).
          int lowU = lanes[2 * u + 1] & VALUE;
          if (lowU == (lanes[2 * u] & VALUE)) {
            int start = sccNodesPos;
            sccStart[sccCount] = start;
            int w;
            do {
              w = sccStack[--sccTop];
              lanes[2 * w + 1] &= VALUE;
              nodeToScc[w] = sccCount;
              sccNodes[sccNodesPos++] = w;
            } while (w != u);
            Arrays.sort(sccNodes, start, sccNodesPos);
            sccCount++;
          }
          if (--top >= 0) {
            int parent = frames[2 * top];
            int lowLane = lanes[2 * parent + 1];
            if (lowU < (lowLane & VALUE)) {
              lanes[2 * parent + 1] = (lowLane & FLAG) | lowU;
            }
          }
        }
      }
    }

    private void preorder(int v) {
      int idx = nextIndex++;
      lanes[2 * v] = idx | FLAG;
      lanes[2 * v + 1] = idx | FLAG;
      sccStack[sccTop++] = v;
    }

    Sccs buildResult() {
      sccStart[sccCount] = sccNodesPos;
      return new Sccs(graph, nodeToScc, sccNodes, Arrays.copyOf(sccStart, sccCount + 1));
    }
  }
}
