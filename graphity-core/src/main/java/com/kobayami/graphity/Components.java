package com.kobayami.graphity;

import java.util.Arrays;

/**
 * Factory for computing connected components of a {@link Graph}.
 * <p>
 * Provides five entry points for different component types:
 * <ul>
 *   <li>{@link #sccsOf(Graph)} - Strongly Connected Components (Tarjan's algorithm)</li>
 *   <li>{@link #wccsOf(Graph)} - Weakly Connected Components (Union-Find)</li>
 *   <li>{@link #ccsOf(Graph)} - Connected Components for undirected interpretation
 *       (identical to {@code wccsOf})</li>
 *   <li>{@link #biconnectivityOf(BiAdjacentGraph)} - bridges, articulation points and biconnected
 *       components, also for undirected interpretation</li>
 *   <li>{@link #strongConnectivityOf(BiAdjacentGraph)} - strong bridges and strong articulation
 *       points, the directed counterpart</li>
 * </ul>
 *
 * <pre>{@code
 * var sccs = Components.sccsOf(graph);   // directed: SCCs via Tarjan
 * var wccs = Components.wccsOf(graph);   // directed: WCCs via Union-Find
 * var ccs  = Components.ccsOf(graph);    // undirected interpretation: same as wccsOf
 * // undirected: bridges, cut vertices, blocks
 * var bcc  = Components.biconnectivityOf(BiAdjacentGraph.of(graph));
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
 *   <li>{@code biconnectivityOf} ignores edge direction as well, and it is where the rule
 *       below becomes visible: a reciprocal pair read as two edges would be a cycle, and no
 *       symmetrically stored edge could ever be a bridge.</li>
 * </ul>
 * The undirected reading takes each stored edge as connecting both of its endpoints - the
 * opposite edge does not have to exist - and counts a reciprocal pair {@code a → b} /
 * {@code b → a} as one undirected edge rather than two. Storing undirected edges in both
 * directions is therefore tolerated, not expected: a graph stored symmetrically is read
 * exactly like the same structure stored with a single direction per edge.
 *
 * @see Sccs
 * @see NodePartition
 * @see Biconnectivity
 * @see Graph
 */
public final class Components {

  private Components() {}

  /**
   * Computes the strongly connected components of the given graph using Tarjan's algorithm.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), plus sorting the members of each component,
   * O(k log k) for a component of k nodes: O(nodeCount x log nodeCount) in the worst case, when
   * one component holds most of the nodes.<br>
   * <strong>Memory:</strong> O(nodeCount) for the result; O(nodeCount) additional during
   * computation.
   *
   * @param graph the graph to analyze
   * @return the SCCs as an {@link Sccs} instance
   * @throws IllegalArgumentException if {@code graph.nodeCount() > 2^30 - 5}, the most nodes for
   *     which two ints per node fit into one array
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
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to analyze
   * @return the WCCs as a {@link NodePartition}
   */
  public static NodePartition wccsOf(Graph graph) {
    int n = graph.nodeCount();

    // 1) Union the endpoints of every edge; self-loops are no-ops.
    int[] parent = UnionFind.singletons(n);
    byte[] rank = new byte[n];

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);

    for (int u = 0; u < n; u++) {
      int end = adjacencyStart[u + 1];
      for (int ei = adjacencyStart[u]; ei < end; ei++) {
        UnionFind.union(parent, rank, u, adjacency[ei]);
      }
    }

    // 2) Dense component IDs, in order of each component's smallest node.
    int[] nodeToGroup = new int[n];
    int[] groupOfRoot = new int[n];
    Arrays.fill(groupOfRoot, -1);
    int groupCount = 0;

    for (int i = 0; i < n; i++) {
      int root = UnionFind.find(parent, i);
      if (groupOfRoot[root] < 0) {
        groupOfRoot[root] = groupCount++;
      }
      nodeToGroup[i] = groupOfRoot[root];
    }

    // 3) Members as CSR.
    var groups = ParentForest.groupIndices(nodeToGroup, groupCount);
    return new NodePartition(graph, nodeToGroup, groups.members(), groups.start());
  }

  /**
   * Computes the connected components of the given graph, reading it as undirected.
   * <p>
   * Each stored edge connects both of its endpoints here, so the opposite edge does not have to
   * exist, and a reciprocal pair {@code a → b} / {@code b → a} counts as one undirected edge
   * rather than two. Storing undirected edges in both directions is tolerated, not expected:
   * both forms give the same components. Self-loops contribute nothing to connectivity and are
   * ignored.
   * <p>
   * This is identical to {@link #wccsOf(Graph)}, and not by coincidence: once connectivity is
   * the question, ignoring edge direction is all the undirected reading amounts to. Both names
   * exist so that a call can say which reading it means.
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

  /**
   * Computes bridges, articulation points and biconnected components of the given graph,
   * treating it as undirected like {@link #ccsOf(Graph)}.
   * <p>
   * Where {@code ccsOf} answers whether a graph falls apart, this answers how close it is to
   * falling apart: which single node or single edge holds it together, and which parts are
   * robust because all their edges lie on shared cycles. All three results come out of one
   * depth-first pass and are always computed together.
   * <p>
   * The edges {@code a → b} and {@code b → a} count as one undirected edge, so a graph that
   * stores undirected edges in both directions still has bridges. Self-loops are ignored.
   * <p>
   * The pass reads the predecessors of every node and so takes a {@link BiAdjacentGraph}. A
   * caller holding a plain {@link Graph} builds the in-adjacency once with
   * {@link BiAdjacentGraph#of(Graph)}, at a cost visible at the call site.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount); the bridges, at most nodeCount - 1, are
   * ordered by counting.<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result; during computation
   * 29 x nodeCount bytes for the traversal state, 4 x edgeCount bytes for the edge index
   * translation, up to 4 x edgeCount bytes for the edge stack, which grows on demand and never
   * exceeds edgeCount entries, and for the bridges up to 12 x nodeCount bytes while they are
   * collected, half as much again in spare list capacity, and another 16 x nodeCount bytes while
   * they are ordered. That is up to 63 x nodeCount + 8 x edgeCount bytes, several times the size
   * of the graph itself, and worth checking against the available heap on very large graphs.
   *
   * @param graph the graph to analyze
   * @return bridges, articulation points and blocks as a {@link Biconnectivity} instance
   * @see Biconnectivity
   */
  public static Biconnectivity biconnectivityOf(BiAdjacentGraph graph) {
    return Biconnectivity.of(graph);
  }

  /**
   * Computes the strong bridges and strong articulation points of the given graph: the edges
   * and nodes whose removal increases the number of strongly connected components.
   * <p>
   * Edges are read as directed, as in {@link #sccsOf(Graph)}. Both results lie inside strongly
   * connected components with more than one node; on a DAG both are empty.
   * <p>
   * The computation works on the dominators of each component and of its transpose, following
   * Italiano, Laura and Santaroni, and so reads the predecessors of every node: it takes a
   * {@link BiAdjacentGraph}, whose transpose is a constant-time view. A caller holding a plain
   * {@link Graph} builds the in-adjacency once with {@link BiAdjacentGraph#of(Graph)}. Iterative;
   * deep graphs do not overflow the call stack.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) x log nodeCount) in the worst case, from the
   * dominators; the strong bridges are ordered by counting.<br>
   * <strong>Memory:</strong> O(nodeCount) for the result; up to about 105 x nodeCount bytes
   * additional during computation, most of it while the strong bridges are ordered: up to
   * 2 x nodeCount of them before the duplicates found in both directions are dropped.
   *
   * @param graph the graph to analyze
   * @return strong bridges and strong articulation points as a {@link StrongConnectivity}
   * @throws IllegalArgumentException if {@code graph.nodeCount() > 2^30 - 6}, the limit of
   *     {@link Dominators}
   * @see StrongConnectivity
   */
  public static StrongConnectivity strongConnectivityOf(BiAdjacentGraph graph) {
    return StrongConnectivity.of(graph);
  }

  /**
   * Iterative Tarjan SCC. Performs a single-pass DFS with an explicit stack (no recursion,
   * so deep graphs cannot overflow the JVM call stack), identifying all strongly connected
   * components. SCC IDs are assigned in reverse topological order of the component DAG.
   * <p>
   * The per-node state - index, lowlink, visited and on-stack - lives in two interleaved
   * {@code int} lanes per node: {@code index | visited << 31} and {@code lowlink | onStack << 31}.
   * index and lowlink are both node IDs, so 31 bits each suffice. Every node touch is then one
   * cache line instead of two int arrays plus two bit-sets, and the layout stays within 32-bit
   * arithmetic, which a port to a language without 64-bit integer operators can mirror. Two lanes
   * per node cap the node count at 2^30 - 5, {@link ArrayLimits#MAX_NODE_COUNT_TWO_INTS}; a
   * depth-first path through every node then still fits into the frame stack.
   * <p>
   * Two stacks drive the traversal: {@code frames} for the DFS descent (each entry a node ID and
   * its resume position), and {@code sccStack} holding nodes not yet assigned to a
   * completed SCC - a node may be finished in DFS but still on {@code sccStack} until its SCC
   * root finishes.
   */
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
