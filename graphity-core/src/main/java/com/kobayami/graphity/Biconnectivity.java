package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Arrays;

/**
 * The bridges, articulation points and biconnected components of a graph read as undirected, as
 * {@link Components#biconnectivityOf(BiAdjacentGraph)} returns them.
 * <p>
 * A bridge is an edge whose removal increases the number of connected components, and an
 * articulation point is a node whose removal does the same. A biconnected component, also called
 * a block, is a maximal group of edges in which any two edges lie on a common cycle. The blocks
 * partition the edges, not the nodes: an articulation point belongs to every block it touches.
 * <p>
 * The graph is read as undirected, as described at {@link Components}: the edges
 * {@code a -> b} and {@code b -> a} are one undirected edge. So a graph that stores its edges in
 * both directions still has bridges, and the two directions of an edge lie in the same block.
 * Self-loops are in no block and never a bridge. Here, the control flow of the method
 * {@code load}, whose loop is connected to the rest only through {@code header}:
 * {@snippet file="com/kobayami/graphity/examples/Connectivity.java" region="biconnectivity"}
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> up to 8 x edgeCount + 20 x nodeCount bytes.
 */
public final class Biconnectivity {

  private final Edges bridges;
  private final SortedIntView articulationPoints;
  private final EdgePartition blocks;

  private Biconnectivity(Edges bridges, SortedIntView articulationPoints, EdgePartition blocks) {
    this.bridges = bridges;
    this.articulationPoints = articulationPoints;
    this.blocks = blocks;
  }

  // The body of Components.biconnectivityOf, here because the constructor is private.
  static Biconnectivity of(BiAdjacentGraph graph) {
    var finder = new BlockFinder(graph);
    finder.run();
    return new Biconnectivity(finder.bridges(), finder.articulationPoints(), finder.blocks());
  }

  /**
   * Returns the bridges, in edge index order.
   * <p>
   * Of a bridge stored in both directions, only the edge from the lower to the higher node ID is
   * listed; {@link #blocks()} puts the other direction in the same block.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return bridges
   */
  public Edges bridges() {
    return bridges;
  }

  /**
   * Returns the articulation points, in ascending order.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return articulation points
   */
  public SortedIntView articulationPoints() {
    return articulationPoints;
  }

  /**
   * Returns the blocks, as a partition of the edges.
   * <p>
   * Each block is a group of the partition: {@code blocks().get(e)} returns the block of edge
   * {@code e}, and {@code blocks().edgesOf(block)} the edges of a block. Self-loops are in no
   * block and map to {@code -1}, so {@link IndexMapping#isPartial()} returns {@code true} exactly
   * if the graph has self-loops. Which block gets which group ID is not specified, but the same
   * graph always yields the same group IDs.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return blocks
   */
  public EdgePartition blocks() {
    return blocks;
  }

  // The nodes whose flag is set, in ascending order; StrongConnectivity uses it as well.
  static SortedIntView flaggedNodes(boolean[] isFlagged) {
    int count = 0;
    for (boolean flag : isFlagged) {
      if (flag) count++;
    }
    if (count == 0) return SortedIntView.EMPTY;
    var nodes = new int[count];
    int pos = 0;
    for (int node = 0; node < isFlagged.length; node++) {
      if (isFlagged[node]) nodes[pos++] = node;
    }
    return new SortedIntView(nodes);
  }

  // Hopcroft and Tarjan's algorithm on an explicit stack, so deep graphs cannot overflow the call
  // stack.
  //
  // Neighbors are read as undirected by merging a node's out- and in-adjacency, both sorted
  // ascending, emitting each neighbor once. The merge is also where a block's edge indices come
  // from: for one undirected edge it sees the out-adjacency slot - already an edge index - and the
  // in-adjacency slot in the same step, and the latter translates to an edge index through
  // Graphs.transposedEdgeMapping.
  //
  // Collapsing the reciprocal pair removes the usual complication around the edge back to the
  // depth-first parent: the parent appears exactly once in the merged sequence, so skipping it
  // needs no per-frame flag and no parent-edge identity.
  //
  // Per-node state (disc, low, dfsParent, blockMark and the two merge cursors) lives in arrays
  // rather than in stack frames, because a node is on the depth-first stack at most once - which
  // keeps the stack itself a plain int[] of node IDs that needs no resizing. The edge stack does
  // grow on demand: a graph whose blocks close early never allocates it in full.
  private static final class BlockFinder {

    private static final int UNVISITED = 0;

    private final BiAdjacentGraph graph;
    private final int[] outAdjacency;
    private final int[] outAdjacencyStart;
    private final int[] inAdjacency;
    private final int[] inAdjacencyStart;

    // In-adjacency slot -> edge index of the same edge in the graph.
    private final int[] inSlotToEdge;

    // Discovery time, UNVISITED until the node is reached. Counting starts at 1.
    private final int[] disc;
    private final int[] low;
    private final int[] dfsParent;

    // Edge-stack height before the tree edge into this node was pushed.
    private final int[] blockMark;

    private final int[] outCursor;
    private final int[] inCursor;

    private final int[] dfsStack;
    private int[] edgeStack;
    private int edgeTop;

    private final boolean[] isArticulation;
    // Bridges as parallel lists of source, target and edge index, each a stored edge.
    private final IntArrayList bridgeSources = new IntArrayList();
    private final IntArrayList bridgeTargets = new IntArrayList();
    private final IntArrayList bridgeEdgeIndices = new IntArrayList();

    // Edge index -> block ID, -1 for a self-loop.
    private final int[] blockOfEdge;
    private int blockCount;

    private int timer;

    // Neighbor found by advance(int), with the edge indices of its two directions.
    private int pendingNode;
    private int pendingOutEdge;
    private int pendingInSlot;

    BlockFinder(BiAdjacentGraph graph) {
      this.graph = graph;
      int n = graph.nodeCount();
      int m = graph.edgeCount();
      this.outAdjacency = Csr.outAdjacency(graph);
      this.outAdjacencyStart = Csr.outAdjacencyStart(graph);
      this.inAdjacency = Csr.inAdjacency(graph);
      this.inAdjacencyStart = Csr.inAdjacencyStart(graph);
      this.inSlotToEdge = Graphs.transposedEdgeMapping(graph).mapping;
      this.disc = new int[n];
      this.low = new int[n];
      this.dfsParent = new int[n];
      this.blockMark = new int[n];
      this.outCursor = new int[n];
      this.inCursor = new int[n];
      this.dfsStack = new int[n];
      // Sized like DfsFrames.initial: one slot per edge, but at least 16 and at most 1024.
      this.edgeStack = new int[Math.max(16, Math.min(m, 1024))];
      this.isArticulation = new boolean[n];
      this.blockOfEdge = new int[m];
      Arrays.fill(blockOfEdge, -1);
    }

    void run() {
      int n = graph.nodeCount();
      for (int root = 0; root < n; root++) {
        if (disc[root] != UNVISITED) continue;

        discover(root, -1);
        dfsStack[0] = root;
        int top = 0;
        int rootChildren = 0;

        outer:
        while (top >= 0) {
          int u = dfsStack[top];
          while (advance(u)) {
            int v = pendingNode;
            // The one occurrence of the parent is the tree edge back up; it is already pushed.
            if (v == dfsParent[u]) continue;
            if (disc[v] == UNVISITED) {
              int mark = edgeTop;
              pushPendingEdges();
              discover(v, u);
              blockMark[v] = mark;
              if (u == root) rootChildren++;
              dfsStack[++top] = v;
              continue outer;
            }
            if (disc[v] < disc[u]) {
              // Back edge to an ancestor. Undirected depth-first search has no cross edges, so
              // the other case - a finished descendant - was already handled from its own side.
              pushPendingEdges();
              if (disc[v] < low[u]) low[u] = disc[v];
            }
          }

          top--;
          int parent = dfsParent[u];
          if (parent >= 0) {
            if (low[u] < low[parent]) low[parent] = low[u];
            if (low[u] >= disc[parent]) {
              if (low[u] > disc[parent]) addBridge(parent, u, blockMark[u]);
              closeBlock(blockMark[u]);
              // The root is an articulation point only if it has two or more subtrees.
              if (parent != root) isArticulation[parent] = true;
            }
          }
        }

        if (rootChildren >= 2) isArticulation[root] = true;
      }
    }

    // Discovers node as a child of parent and initializes its merge cursors.
    private void discover(int node, int parent) {
      disc[node] = ++timer;
      low[node] = disc[node];
      dfsParent[node] = parent;
      outCursor[node] = outAdjacencyStart[node];
      inCursor[node] = inAdjacencyStart[node];
    }

    // Advances node's merge cursors to its next undirected neighbor, skipping self-loops, and
    // reports it in pendingNode. Returns false once the neighbors are exhausted.
    private boolean advance(int node) {
      int out = outCursor[node];
      int outEnd = outAdjacencyStart[node + 1];
      int in = inCursor[node];
      int inEnd = inAdjacencyStart[node + 1];

      while (out < outEnd || in < inEnd) {
        int outNeighbor = out < outEnd ? outAdjacency[out] : Integer.MAX_VALUE;
        int inNeighbor = in < inEnd ? inAdjacency[in] : Integer.MAX_VALUE;

        int neighbor;
        int outEdge = -1;
        int inSlot = -1;
        if (outNeighbor < inNeighbor) {
          neighbor = outNeighbor;
          outEdge = out++;
        } else if (inNeighbor < outNeighbor) {
          neighbor = inNeighbor;
          inSlot = in++;
        } else {
          // Both directions of one undirected edge; both indices belong to the same block.
          neighbor = outNeighbor;
          outEdge = out++;
          inSlot = in++;
        }

        if (neighbor == node) continue; // self-loop: in no block, never a bridge

        outCursor[node] = out;
        inCursor[node] = in;
        pendingNode = neighbor;
        pendingOutEdge = outEdge;
        pendingInSlot = inSlot;
        return true;
      }

      outCursor[node] = out;
      inCursor[node] = in;
      return false;
    }

    // Pushes the edge indices of the pending undirected edge - one per stored direction, so a
    // reciprocal pair pushes both and lands in one block.
    private void pushPendingEdges() {
      if (pendingOutEdge >= 0) pushEdge(pendingOutEdge);
      if (pendingInSlot >= 0) pushEdge(inSlotToEdge[pendingInSlot]);
    }

    private void pushEdge(int edgeIndex) {
      if (edgeTop == edgeStack.length) {
        // Every non-self-loop edge is pushed at most once, so the edge count is the hard cap -
        // capping there also keeps the doubling from overflowing above 2^30 edges.
        int grown = (int) Math.min((long) edgeTop << 1, blockOfEdge.length);
        edgeStack = Arrays.copyOf(edgeStack, grown);
      }
      edgeStack[edgeTop++] = edgeIndex;
    }

    // Records the bridge parent - child before its block closes. Every block below the child is
    // closed already, so the edges above mark are exactly the one or two stored directions of the
    // bridge, pushed parent -> child first. A bridge stored both ways is reported from the lower
    // node ID.
    private void addBridge(int parent, int child, int mark) {
      assert edgeTop - mark == 1 || edgeTop - mark == 2;
      int edge = edgeTop - mark == 2 && parent > child ? edgeStack[mark + 1] : edgeStack[mark];
      int target = outAdjacency[edge];
      bridgeSources.add(target == child ? parent : child);
      bridgeTargets.add(target);
      bridgeEdgeIndices.add(edge);
    }

    // Turns the edges above mark into one block.
    private void closeBlock(int mark) {
      int block = blockCount++;
      while (edgeTop > mark) {
        blockOfEdge[edgeStack[--edgeTop]] = block;
      }
    }

    Edges bridges() {
      return Edges.sortedEdges(graph, bridgeSources.elements(), bridgeTargets.elements(),
          bridgeEdgeIndices.elements(), bridgeSources.size(), false);
    }

    SortedIntView articulationPoints() {
      return flaggedNodes(isArticulation);
    }

    EdgePartition blocks() {
      var groups = ParentForest.groupIndices(blockOfEdge, blockCount);
      return new EdgePartition(blockOfEdge, groups.members(), groups.start());
    }
  }
}
