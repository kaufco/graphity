package com.kobayami.graphity;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Factory for computing connected components of a {@link Graph}.
 * <p>
 * Provides three entry points for different component types:
 * <ul>
 *   <li>{@link #sccsOf(Graph)} — Strongly Connected Components (Tarjan's algorithm)</li>
 *   <li>{@link #wccsOf(Graph)} — Weakly Connected Components (Union-Find)</li>
 *   <li>{@link #ccsOf(Graph)} — Connected Components for undirected interpretation
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
 *   <li>{@code sccsOf} treats edges as directed — two nodes are in the same SCC only if
 *       each is reachable from the other <em>following edge directions</em>.</li>
 *   <li>{@code wccsOf} / {@code ccsOf} ignores edge direction — two nodes are in the same
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
   * Uses Union-Find (disjoint-set) — only requires out-edges, no {@link BiAdjacentGraph} needed.
   * <p>
   * <strong>Time:</strong> O(nodeCount × α(nodeCount) + edgeCount), effectively O(nodeCount + edgeCount).<br>
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
   * This is identical to {@link #wccsOf(Graph)} — for an undirected graph represented
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

  // ---- Union-Find helpers ----

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

  // ---- Tarjan SCC traverser ----

  /**
   * Tarjan SCC traverser. Performs a single-pass DFS, identifying all strongly connected
   * components. SCC IDs are assigned in reverse topological order of the component DAG.
   */
  private static final class TarjanTraverser extends DfsTraverser {

    private final int[] index;
    private final int[] lowlink;
    private final int[] stack;
    private final BitSet onStack;
    private int nextIndex;
    private int stackTop;

    private final int[] nodeToScc;
    private final int[] sccNodes;
    private int[] sccStart;
    private int sccCount;
    private int sccNodesPos;

    TarjanTraverser(Graph graph) {
      super(graph);
      int n = graph.nodeCount();
      this.index = new int[n];
      this.lowlink = new int[n];
      this.stack = new int[n];
      this.onStack = new BitSet(n);
      this.nodeToScc = new int[n];
      this.sccNodes = new int[n];
      this.sccStart = new int[n + 1];
    }

    @Override
    protected void visitNode(int node) {
      index[node] = nextIndex;
      lowlink[node] = nextIndex;
      nextIndex++;
      stack[stackTop++] = node;
      onStack.set(node);

      var adjacency = graph.outAdjacency;
      var adjacencyStart = graph.outAdjacencyStart;
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        int target = adjacency[i];
        if (visit(target)) {
          lowlink[node] = Math.min(lowlink[node], lowlink[target]);
        } else if (onStack.get(target)) {
          lowlink[node] = Math.min(lowlink[node], index[target]);
        }
      }

      if (lowlink[node] == index[node]) {
        int start = sccNodesPos;
        sccStart[sccCount] = start;
        int w;
        do {
          w = stack[--stackTop];
          onStack.clear(w);
          nodeToScc[w] = sccCount;
          sccNodes[sccNodesPos++] = w;
        } while (w != node);
        Arrays.sort(sccNodes, start, sccNodesPos);
        sccCount++;
      }
    }

    Sccs buildResult() {
      sccStart[sccCount] = sccNodesPos;
      return new Sccs(graph, nodeToScc, sccNodes, Arrays.copyOf(sccStart, sccCount + 1));
    }
  }
}
