package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Arrays;

/**
 * Bridges, articulation points and biconnected components of a graph read as undirected.
 * <p>
 * Obtain via {@link Components#biconnectivityOf(Graph)}. All three results come out of one
 * depth-first pass; there is no cheaper entry point that computes only one of them.
 * <ul>
 *   <li>A <strong>bridge</strong> is an edge whose removal increases the number of connected
 *       components.</li>
 *   <li>An <strong>articulation point</strong> (cut vertex) is a node whose removal does the
 *       same.</li>
 *   <li>A <strong>block</strong> (biconnected component) is a maximal set of edges in which
 *       any two edges lie on a common cycle. Blocks partition the edges, not the nodes - an
 *       articulation point belongs to every block that meets it.</li>
 * </ul>
 *
 * <h2>Undirected interpretation</h2>
 * The graph data model is directed, and this algorithm reads it as undirected, like
 * {@link Components#ccsOf(Graph)}: the edges {@code a → b} and {@code b → a} are one
 * undirected edge, not two, so a graph that stores undirected edges in both directions still
 * has bridges. Both edge indices of such a reciprocal pair land in the same block. Self-loops
 * are ignored - they are in no block, and they can neither disconnect a graph nor lie on a
 * cycle with another edge.
 *
 * <pre>{@code
 * var biconnectivity = Components.biconnectivityOf(dependencies);
 * Edges criticalLinks = biconnectivity.bridges();               // source < target
 * SortedIntView singlePointsOfFailure = biconnectivity.articulationPoints();
 * EdgePartition blocks = biconnectivity.blocks();
 * for (int block = 0; block < blocks.groupCount(); block++) {
 *     SortedIntView edgeIndices = blocks.edgesOf(block);
 * }
 * }</pre>
 *
 * @see Components#biconnectivityOf(Graph)
 * @see Components#ccsOf(Graph)
 * @see EdgePartition
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

  /**
   * Computes bridges, articulation points and blocks of the given graph, read as undirected.
   * In-adjacency is obtained via {@link Graph#biAdjacentGraph()} unless the argument already
   * provides it.
   */
  static Biconnectivity of(Graph graph) {
    var finder = new BlockFinder(graph.biAdjacentGraph());
    finder.run();
    return new Biconnectivity(finder.bridges(), finder.articulationPoints(), finder.blocks());
  }

  /**
   * Returns the bridges, each as a single undirected edge with the lower node ID as source,
   * ordered by source and then target.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the bridges of the graph
   */
  public Edges bridges() {
    return bridges;
  }

  /**
   * Returns the articulation points, sorted ascending.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the articulation points of the graph
   */
  public SortedIntView articulationPoints() {
    return articulationPoints;
  }

  /**
   * Returns the biconnected components as a partition of the edge indices.
   * <p>
   * Group IDs are block IDs; {@code blocks().edgesOf(blockId)} lists the edge indices of one
   * block, and {@code blocks().get(edgeIndex)} names the block of one edge. Self-loops are
   * unmapped ({@code -1}), so {@link IndexMapping#isPartial()} is {@code true} exactly when
   * the graph has self-loops. Both edge indices of a reciprocal pair are in the same block.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the blocks of the graph
   */
  public EdgePartition blocks() {
    return blocks;
  }

  /**
   * Hopcroft-Tarjan biconnectivity on an explicit stack (no recursion, so deep graphs cannot
   * overflow the JVM call stack).
   * <p>
   * Neighbours are read as undirected by merging a node's out- and in-adjacency, both sorted
   * ascending, emitting each neighbour once. The merge is also where a block's edge indices
   * come from: for one undirected edge it sees the out-adjacency position - already an edge
   * index - and the in-adjacency position in the same step, and the latter translates to an
   * edge index through {@link Graphs#transposedEdgeMapping(Graph)}.
   * <p>
   * Collapsing the reciprocal pair removes the usual complication around the edge back to the
   * depth-first parent: the parent appears exactly once in the merged sequence, so skipping it
   * needs no per-frame flag and no parent-edge identity.
   * <p>
   * Per-node state ({@code disc}, {@code low}, {@code dfsParent}, {@code blockMark} and the two
   * merge cursors) lives in arrays rather than in stack frames, because a node is on the
   * depth-first stack at most once - which keeps the stack itself a plain {@code int[]} of node
   * IDs and needs no resizing. The edge stack does grow on demand: a graph whose blocks close
   * early never allocates it in full.
   */
  private static final class BlockFinder {

    private static final int UNVISITED = 0;

    private final BiAdjacentGraph graph;
    private final int[] outAdjacency;
    private final int[] outAdjacencyStart;
    private final int[] inAdjacency;
    private final int[] inAdjacencyStart;

    /** In-adjacency slot → edge index of the same edge in the graph. */
    private final int[] inSlotToEdge;

    /** Discovery time, {@code UNVISITED} until the node is reached. Counting starts at 1. */
    private final int[] disc;
    private final int[] low;
    private final int[] dfsParent;

    /** Edge-stack height before the tree edge into this node was pushed. */
    private final int[] blockMark;

    private final int[] outCursor;
    private final int[] inCursor;

    private final int[] dfsStack;
    private int[] edgeStack;
    private int edgeTop;

    private final boolean[] isArticulation;
    private final LongArrayList bridgeKeys = new LongArrayList();

    /** Edge index → block ID, {@code -1} for a self-loop. */
    private final int[] blockOfEdge;
    private int blockCount;

    private int timer;

    /** Neighbour found by {@link #advance(int)}, with the edge indices of its two directions. */
    private int pendingNode;
    private int pendingOutEdge;
    private int pendingInSlot;

    BlockFinder(BiAdjacentGraph graph) {
      this.graph = graph;
      int n = graph.nodeCount();
      int m = graph.edgeCount();
      this.outAdjacency = graph.outAdjacency;
      this.outAdjacencyStart = graph.outAdjacencyStart;
      this.inAdjacency = graph.inAdjacency;
      this.inAdjacencyStart = graph.inAdjacencyStart;
      this.inSlotToEdge = Graphs.transposedEdgeMapping(graph).mapping;
      this.disc = new int[n];
      this.low = new int[n];
      this.dfsParent = new int[n];
      this.blockMark = new int[n];
      this.outCursor = new int[n];
      this.inCursor = new int[n];
      this.dfsStack = new int[n];
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
              closeBlock(blockMark[u]);
              // The root is an articulation point only if it has two or more subtrees.
              if (parent != root) isArticulation[parent] = true;
            }
            if (low[u] > disc[parent]) {
              bridgeKeys.add(bridgeKey(parent, u));
            }
          }
        }

        if (rootChildren >= 2) isArticulation[root] = true;
      }
    }

    /** Discovers {@code node} as a child of {@code parent} and initialises its merge cursors. */
    private void discover(int node, int parent) {
      disc[node] = ++timer;
      low[node] = disc[node];
      dfsParent[node] = parent;
      outCursor[node] = outAdjacencyStart[node];
      inCursor[node] = inAdjacencyStart[node];
    }

    /**
     * Advances {@code node}'s merge cursors to its next undirected neighbour, skipping
     * self-loops, and reports it in {@code pendingNode}. Returns {@code false} once the
     * neighbours are exhausted.
     */
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

    /**
     * Pushes the edge indices of the pending undirected edge - one per stored direction, so a
     * reciprocal pair pushes both and lands in one block.
     */
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

    /** Turns the edges above {@code mark} into one block. */
    private void closeBlock(int mark) {
      int block = blockCount++;
      while (edgeTop > mark) {
        blockOfEdge[edgeStack[--edgeTop]] = block;
      }
    }

    private static long bridgeKey(int a, int b) {
      int source = Math.min(a, b);
      int target = Math.max(a, b);
      return ((long) source << 32) | target;
    }

    Edges bridges() {
      if (bridgeKeys.isEmpty()) return Edges.empty();
      long[] keys = bridgeKeys.elements();
      int count = bridgeKeys.size();
      // Both node IDs are non-negative, so the packed key sorts by source, then target.
      Arrays.sort(keys, 0, count);
      var sources = new IntArrayList(count);
      var targets = new IntArrayList(count);
      for (int i = 0; i < count; i++) {
        sources.add((int) (keys[i] >>> 32));
        targets.add((int) keys[i]);
      }
      return new Edges(IntLists.unmodifiable(sources), IntLists.unmodifiable(targets));
    }

    SortedIntView articulationPoints() {
      int count = 0;
      for (boolean flag : isArticulation) {
        if (flag) count++;
      }
      if (count == 0) return SortedIntView.EMPTY;
      var points = new int[count];
      int pos = 0;
      for (int node = 0; node < isArticulation.length; node++) {
        if (isArticulation[node]) points[pos++] = node;
      }
      return new SortedIntView(points);
    }

    EdgePartition blocks() {
      int m = blockOfEdge.length;
      var memberStart = new int[blockCount + 1];
      for (int edgeIndex = 0; edgeIndex < m; edgeIndex++) {
        int block = blockOfEdge[edgeIndex];
        if (block >= 0) memberStart[block + 1]++;
      }
      for (int block = 0; block < blockCount; block++) {
        memberStart[block + 1] += memberStart[block];
      }

      // Ascending edge scan keeps the members of every block sorted.
      var members = new int[memberStart[blockCount]];
      var writePos = Arrays.copyOf(memberStart, blockCount);
      for (int edgeIndex = 0; edgeIndex < m; edgeIndex++) {
        int block = blockOfEdge[edgeIndex];
        if (block >= 0) members[writePos[block]++] = edgeIndex;
      }

      int valid = members.length;
      return new EdgePartition(blockOfEdge, valid, valid < m, members, memberStart);
    }
  }
}
