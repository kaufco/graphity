package com.kobayami.graphity;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;

/**
 * Factory for computing connected components of a {@link Graph}.
 * <p>
 * Provides three entry points for different component types:
 * <ul>
 *   <li>{@link #sccsOf(Graph)} - Strongly Connected Components (Tarjan's algorithm)</li>
 *   <li>{@link #wccsOf(Graph)} - Weakly Connected Components (Union-Find)</li>
 *   <li>{@link #ccsOf(Graph)} - Connected Components for undirected interpretation
 *       (identical to {@code wccsOf})</li>
 * </ul>
 *
 * <pre>{@code
 * var sccs = Components.sccsOf(graph);   // directed: SCCs via Tarjan
 * var wccs = Components.wccsOf(graph);   // directed: WCCs via Union-Find
 * var ccs  = Components.ccsOf(graph);    // undirected interpretation: same as wccsOf
 * }</pre>
 *
 * <h2>Directed vs. undirected interpretation</h2>
 * The graph data model is always directed (edges have a source and target). Whether a graph
 * is treated as directed or undirected depends on the algorithm:
 * <ul>
 *   <li>{@code sccsOf} treats edges as directed - two nodes are in the same SCC only if
 *       each is reachable from the other <em>following edge directions</em>.</li>
 *   <li>{@code wccsOf} / {@code ccsOf} ignores edge direction - two nodes are in the same
 *       component if connected by any path, regardless of edge direction.</li>
 * </ul>
 * For graphs that represent undirected structures (with both a→b and b→a for each edge),
 * {@code ccsOf} is the natural choice. The result is identical to {@code wccsOf}.
 *
 * @see Sccs
 * @see NodePartition
 * @see Graph
 */
public final class Components {

  private Components() {}

  /**
   * Computes the strongly connected components of the given graph using Tarjan's algorithm.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount) for the result; O(nodeCount) additional during computation.
   *
   * @param graph the graph to analyze
   * @return the SCCs as an {@link Sccs} instance
   */
  public static Sccs sccsOf(Graph graph) {
    var tarjan = new TarjanTraverser(graph);
    tarjan.run();
    return tarjan.buildResult();
  }

  /**
   * Computes the weakly connected components of the given directed graph.
   * <p>
   * Two nodes are in the same WCC if they are connected by any path, ignoring edge direction.
   * Uses Union-Find (disjoint-set) - only requires out-edges, no {@link BiAdjacentGraph} needed.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to analyze
   * @return the WCCs as a {@link NodePartition}
   */
  public static NodePartition wccsOf(Graph graph) {
    int n = graph.nodeCount();
    if (n == 0) {
      return new NodePartition(graph, new int[0], new int[0], new int[]{0});
    }

    // Union-Find with path compression and union by rank.
    int[] parent = new int[n];
    int[] rank = new int[n];
    for (int i = 0; i < n; i++) {
      parent[i] = i;
    }

    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;

    for (int u = 0; u < n; u++) {
      int end = adjacencyStart[u + 1];
      for (int ei = adjacencyStart[u]; ei < end; ei++) {
        union(parent, rank, u, adjacency[ei]);
      }
    }

    // Build partition: assign dense component IDs.
    int[] nodeToGroup = new int[n];
    int[] compId = new int[n];
    Arrays.fill(compId, -1);
    int groupCount = 0;

    for (int i = 0; i < n; i++) {
      int root = find(parent, i);
      if (compId[root] < 0) {
        compId[root] = groupCount++;
      }
      nodeToGroup[i] = compId[root];
    }

    // Build CSR: count members per group, then fill.
    int[] memberStart = new int[groupCount + 1];
    for (int i = 0; i < n; i++) {
      memberStart[nodeToGroup[i] + 1]++;
    }
    for (int g = 0; g < groupCount; g++) {
      memberStart[g + 1] += memberStart[g];
    }

    int[] members = new int[n];
    int[] writePos = Arrays.copyOf(memberStart, groupCount);
    for (int i = 0; i < n; i++) {
      members[writePos[nodeToGroup[i]]++] = i;
    }
    // members within each group are already sorted (ascending scan of i).

    return new NodePartition(graph, nodeToGroup, members, memberStart);
  }

  /**
   * Computes the connected components of the given graph, treating it as undirected.
   * <p>
   * This is identical to {@link #wccsOf(Graph)} - for an undirected graph represented
   * as a directed graph (with edges in both directions), WCCs and CCs are the same.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to analyze
   * @return the connected components as a {@link NodePartition}
   * @see #wccsOf(Graph)
   */
  public static NodePartition ccsOf(Graph graph) {
    return wccsOf(graph);
  }

  private static int find(int[] parent, int x) {
    while (parent[x] != x) {
      parent[x] = parent[parent[x]]; // path halving
      x = parent[x];
    }
    return x;
  }

  private static void union(int[] parent, int[] rank, int a, int b) {
    int ra = find(parent, a);
    int rb = find(parent, b);
    if (ra == rb) return;
    if (rank[ra] < rank[rb]) {
      parent[ra] = rb;
    } else if (rank[ra] > rank[rb]) {
      parent[rb] = ra;
    } else {
      parent[rb] = ra;
      rank[ra]++;
    }
  }

  /**
   * Iterative Tarjan SCC. Performs a single-pass DFS with an explicit stack (no recursion,
   * so deep graphs cannot overflow the JVM call stack), identifying all strongly connected
   * components. SCC IDs are assigned in reverse topological order of the component DAG.
   * <p>
   * The per-node state - index, lowlink, visited and on-stack - is packed into a single
   * {@code long[]} (array-of-structs): index in bits 0-30, lowlink in bits 31-61, visited in
   * bit 62, on-stack (SCC-stack membership) in bit 63. index and lowlink are both node IDs, so
   * 31 bits each suffice. Every node touch is then one cache line instead of two int arrays plus
   * two bit-sets; the shift/mask ALU work is free next to the cache miss it avoids.
   * <p>
   * Two stacks drive the traversal: {@code frames} for the DFS descent (each entry packs a node
   * ID and its resume cursor), and {@code sccStack} holding nodes not yet assigned to a
   * completed SCC - a node may be finished in DFS but still on {@code sccStack} until its SCC
   * root finishes.
   */
  private static final class TarjanTraverser {

    private static final long INDEX_MASK = (1L << 31) - 1;    // bits 0-30
    private static final int LOWLINK_SHIFT = 31;              // lowlink occupies bits 31-61
    private static final long LOWLINK_FIELD = INDEX_MASK << LOWLINK_SHIFT;
    private static final long VISITED = 1L << 62;
    private static final long ON_STACK = 1L << 63;

    private final Graph graph;
    private final long[] node;   // packed index | lowlink | visited | onStack per node
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
      this.node = new long[n];
      this.sccStack = new int[n];
      this.nodeToScc = new int[n];
      this.sccNodes = new int[n];
      this.sccStart = new int[n + 1];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = graph.outAdjacencyStart;
      int[] adj = graph.outAdjacency;
      long[] node = this.node;

      // DFS frames grow on demand: depth is data-dependent and usually far below n, so
      // pre-sizing to n would needlessly zero a huge array for shallow graphs. Each frame
      // packs (node << 32 | resume cursor) into one long: node and cursor share a cache
      // line, and the list grows with one allocation per resize instead of two.
      LongArrayList frames = new LongArrayList(Math.max(16, Math.min(n, 1024)));

      for (int root = 0; root < n; root++) {
        if ((node[root] & VISITED) != 0) {
          continue;
        }
        preorder(root);
        frames.add(((long) root << 32) | (out[root] & 0xFFFFFFFFL));

        outer:
        while (!frames.isEmpty()) {
          int top = frames.size() - 1;
          long frame = frames.getLong(top);
          int u = (int) (frame >>> 32);
          int end = out[u + 1];
          // Tight neighbor scan: i and end stay in registers; the frame list is
          // touched only when we actually descend, not once per edge.
          int i = (int) frame;
          while (i < end) {
            int v = adj[i++];
            long nv = node[v];               // one load yields all of v's packed state
            if ((nv & VISITED) == 0) {
              frames.set(top, (frame & 0xFFFFFFFF00000000L) | (i & 0xFFFFFFFFL));  // resume here after v's subtree finishes
              preorder(v);
              frames.add(((long) v << 32) | (out[v] & 0xFFFFFFFFL));
              continue outer;
            } else if ((nv & ON_STACK) != 0) {
              // lowlink[u] = min(lowlink[u], index[v])
              int idxV = (int) (nv & INDEX_MASK);
              long nu = node[u];
              if (idxV < (int) ((nu >>> LOWLINK_SHIFT) & INDEX_MASK)) {
                node[u] = (nu & ~LOWLINK_FIELD) | ((long) idxV << LOWLINK_SHIFT);
              }
            }
          }

          // Postorder: u is finished. Close its SCC if it is a root, then propagate its
          // lowlink to the parent (the node beneath it on the DFS stack).
          long nu = node[u];
          int lowU = (int) ((nu >>> LOWLINK_SHIFT) & INDEX_MASK);
          if (lowU == (int) (nu & INDEX_MASK)) {
            int start = sccNodesPos;
            sccStart[sccCount] = start;
            int w;
            do {
              w = sccStack[--sccTop];
              node[w] &= ~ON_STACK;
              nodeToScc[w] = sccCount;
              sccNodes[sccNodesPos++] = w;
            } while (w != u);
            Arrays.sort(sccNodes, start, sccNodesPos);
            sccCount++;
          }
          frames.size(top);
          if (top > 0) {
            int parent = (int) (frames.getLong(top - 1) >>> 32);
            long np = node[parent];
            if (lowU < (int) ((np >>> LOWLINK_SHIFT) & INDEX_MASK)) {
              node[parent] = (np & ~LOWLINK_FIELD) | ((long) lowU << LOWLINK_SHIFT);
            }
          }
        }
      }
    }

    private void preorder(int v) {
      long idx = nextIndex++;
      node[v] = idx | (idx << LOWLINK_SHIFT) | VISITED | ON_STACK;
      sccStack[sccTop++] = v;
    }

    Sccs buildResult() {
      sccStart[sccCount] = sccNodesPos;
      return new Sccs(graph, nodeToScc, sccNodes, Arrays.copyOf(sccStart, sccCount + 1));
    }
  }
}
