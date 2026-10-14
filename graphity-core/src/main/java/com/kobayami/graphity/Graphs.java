package com.kobayami.graphity;

import java.util.Arrays;

/**
 * Utility methods for graph transformations and shape checks.
 * <p>
 * Provides operations that create new {@link Graph} instances from existing graphs
 * and {@link NodeMapping}s (the original graph is never modified), as well as
 * predicate methods to test structural properties of a graph.
 * <p>
 * A predicate whose name does not say otherwise reads the graph as directed; the ones prefixed
 * {@code undirected} read it as {@link Components#ccsOf(Graph)} does. {@code isBipartite} and
 * {@code isWeaklyConnected} carry no prefix because neither property has a directed reading that
 * could be meant instead.
 *
 * <pre>{@code
 * // Shape checks, directed reading
 * Graphs.isDag(graph);              // acyclic?
 * Graphs.isForest(graph);           // DAG with all inDegrees <= 1?
 * Graphs.isTree(graph);             // forest with exactly one root?
 * Graphs.isPath(graph);             // tree with all outDegrees <= 1?
 *
 * // Shape checks, undirected reading
 * Graphs.isBipartite(graph);        // nodes split into two classes?
 * Graphs.isUndirectedTree(graph);   // connected and acyclic?
 * Graphs.isUndirectedForest(graph); // acyclic?
 * Graphs.isWeaklyConnected(graph);  // one component?
 *
 * // Reindexing for cache-friendly layout
 * NodeMapping newToOld = NodeMapping.reindex(graph);
 * Graph reindexed = Graphs.remapped(graph, newToOld);
 *
 * // Subgraph extraction
 * Graph sub = Graphs.remapped(graph, NodeMapping.forSubgraph(selectedNodes));
 *
 * // Custom permutation
 * Graph permuted = Graphs.remapped(graph, NodeMapping.forPermutation(order));
 *
 * // Edge metadata on a transpose
 * EdgeMapping newToOld = Graphs.transposedEdgeMapping(graph);
 * }</pre>
 *
 * @see NodeMapping
 * @see NodePartition
 * @see EdgeMapping
 * @see EdgePartition
 * @see Graph
 */
public final class Graphs {

  /**
   * Node count from which the double cover of {@link #isBipartite(Graph)} would need more elements
   * than an {@code int[]} can hold.
   */
  private static final int MAX_BIPARTITE_NODE_COUNT = 1 << 30;

  private Graphs() {}

  /**
   * Creates a new graph by remapping node IDs according to the given mapping.
   * <p>
   * The mapping must be in the <strong>natural direction</strong> (new → old), as produced
   * by all {@link NodeMapping} factory methods. Internally, the inverse (old → new) is
   * computed in O(n) for edge-target translation.
   * <p>
   * For <strong>permutations</strong> (reorder), all edges are preserved with remapped IDs.
   * For <strong>subgraph</strong> mappings, edges to nodes not in the subgraph are dropped.
   * Adjacency lists in the resulting graph are sorted ascending.
   * <p>
   * The parameter type is {@link NodeMapping}, <strong>not</strong> a {@link NodePartition}:
   * a partition is many-to-one, so it cannot be inverted and is not a {@code NodeMapping}.
   * Passing one is therefore a compile error, not a runtime failure.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + Σ newDegree x log(newDegree)).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result;
   * O(nodeCount) additional during computation (inverse mapping).
   *
   * @param graph    the original graph
   * @param newToOld mapping from new node IDs to original node IDs
   * @return a new graph with remapped node IDs
   * @throws IllegalArgumentException if the mapping contains duplicate targets
   *         (not injective - cannot compute inverse)
   * @see NodeMapping#reindex(Graph)
   * @see NodeMapping#forSubgraph(java.util.BitSet)
   * @see NodeMapping#forPermutation(it.unimi.dsi.fastutil.ints.IntList)
   */
  public static Graph remapped(Graph graph, NodeMapping newToOld) {
    int newNodeCount = newToOld.mapping.length;
    if (newNodeCount == 0) {
      return new Graph(new int[]{0}, new int[0]);
    }

    // Compute inverse (old → new) for edge-target translation.
    int[] fwd = newToOld.mapping;
    int[] bwd = newToOld.inverse().mapping;

    var origAdj = graph.outAdjacency;
    var origStart = graph.outAdjacencyStart;

    // A mapping that covers every node one-to-one drops no edge, so the target filter below is
    // always true. Knowing that up front turns the degree count into offset arithmetic and saves
    // a full pass over the adjacency: reindexing (the common case) never needs the filter.
    boolean total = !newToOld.isPartial() && newNodeCount == graph.nodeCount();

    // Pass 1: count out-degrees in new graph.
    var outStart = new int[newNodeCount + 1];
    if (total) {
      for (int n = 0; n < newNodeCount; n++) {
        int oldNode = fwd[n];
        outStart[n + 1] = origStart[oldNode + 1] - origStart[oldNode];
      }
    } else {
      for (int n = 0; n < newNodeCount; n++) {
        int oldNode = fwd[n];
        int end = origStart[oldNode + 1];
        int count = 0;
        for (int i = origStart[oldNode]; i < end; i++) {
          int oldTarget = origAdj[i];
          if (oldTarget < bwd.length && bwd[oldTarget] >= 0) {
            count++;
          }
        }
        outStart[n + 1] = count;
      }
    }

    // Prefix sum: convert degrees to CSR offsets.
    for (int n = 0; n < newNodeCount; n++) {
      outStart[n + 1] += outStart[n];
    }
    int totalEdges = outStart[newNodeCount];

    // Pass 2: fill adjacency and sort each adjacency list.
    var outAdj = new int[totalEdges];
    if (total) {
      for (int n = 0; n < newNodeCount; n++) {
        int writePos = outStart[n];
        int oldNode = fwd[n];
        int end = origStart[oldNode + 1];
        for (int i = origStart[oldNode]; i < end; i++) {
          outAdj[writePos++] = bwd[origAdj[i]];
        }
        Arrays.sort(outAdj, outStart[n], writePos);
      }
    } else {
      for (int n = 0; n < newNodeCount; n++) {
        int writePos = outStart[n];
        int oldNode = fwd[n];
        int end = origStart[oldNode + 1];
        for (int i = origStart[oldNode]; i < end; i++) {
          int oldTarget = origAdj[i];
          if (oldTarget < bwd.length && bwd[oldTarget] >= 0) {
            outAdj[writePos++] = bwd[oldTarget];
          }
        }
        Arrays.sort(outAdj, outStart[n], outStart[n + 1]);
      }
    }

    return new Graph(outStart, outAdj);
  }

  /**
   * Returns a copy of {@code graph} with node IDs reassigned in DFS preorder, so that nodes
   * visited close together during traversal receive adjacent IDs.
   * <p>
   * This is a convenience method for cache-friendly reindexing. It is equivalent to:
   * <pre>{@code
   * NodeMapping newToOld = NodeMapping.reindex(graph);
   * Graph reordered = Graphs.remapped(graph, newToOld);
   * }</pre>
   * It adds nothing beyond those two calls and discards {@code newToOld}. Write them out instead:
   * the mapping is what translates results, and any data the caller keys by node ID, back to the
   * original numbering.
   * <p>
   * DFS-preorder relabeling clusters each node's out-neighbors near it in ID space. This turns
   * the random {@code state[target]} accesses of the DFS-based algorithms (SCC, WCC, CCs,
   * TopOrder, back-edge and edge-type classification) into near-sequential scans. On
   * poorly-ordered inputs this speeds those traversals up by a multiple; on already
   * well-ordered inputs it is effectively a no-op.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result.
   *
   * @param graph the graph to reorder
   * @return a new graph with DFS-preorder node IDs
   * @see NodeMapping#reindex(Graph)
   * @see #remapped(Graph, NodeMapping)
   * @deprecated Reordering renumbers every node, so anything the caller keys by node ID - labels,
   *     weights, source locations - has to be permuted along with it, and this method throws away
   *     the only thing that can do that. Code that reorders and keeps such data compiles, runs,
   *     and reads the wrong entries. Use {@link NodeMapping#reindex(Graph)} followed by
   *     {@link #remapped(Graph, NodeMapping)} and keep the mapping. Whether shortcuts of this kind
   *     belong in the API in any form is under review; nothing replaces this method for now.
   */
  @Deprecated(since = "2.1.0")
  public static Graph dfsReordered(Graph graph) {
    return remapped(graph, NodeMapping.reindex(graph));
  }

  /**
   * Returns whether the given graph is a DAG (contains no cycles).
   * <p>
   * Uses a single DFS pass with back-edge detection. Self-loops are detected as cycles.
   * Terminates early on the first cycle found.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph contains no cycles
   */
  public static boolean isDag(Graph graph) {
    var checker = new CycleChecker(graph);
    checker.run();
    return !checker.hasCycle;
  }

  /**
   * Returns whether the given graph is a forest (DAG where every node has in-degree ≤ 1).
   * <p>
   * Equivalently: a forest is a DAG consisting of zero or more disjoint trees.
   * An empty graph is a forest.
   * <p>
   * Reads the graph as directed. For the undirected reading see
   * {@link #isUndirectedForest(Graph)}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a forest
   */
  public static boolean isForest(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null;
  }

  /**
   * Returns whether the given graph is a tree (forest that is weakly connected).
   * <p>
   * A tree has exactly one root node (in-degree 0), and every other node has
   * in-degree exactly 1, so its edges point away from the root. An empty graph is a
   * tree (vacuously). A single node with no edges is a tree.
   * <p>
   * Reads the graph as directed. For the undirected reading see
   * {@link #isUndirectedTree(Graph)}; for the reading in which edges point toward the root,
   * apply this to {@link Graph#transposed()} - which builds a second graph and therefore costs
   * 4 x (nodeCount + edgeCount) bytes, unlike this method itself.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a tree
   */
  public static boolean isTree(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null && info.rootCount <= 1;
  }

  /**
   * Returns whether the given graph is a tree. Equivalent to {@link #isTree(Graph)}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a tree
   * @deprecated Use {@link #isTree(Graph)}. The result is the same in every case; only the name
   *     changed, because an unqualified predicate name here reads the graph as directed and the
   *     spelled-out direction made this the only exception. Whether this name is removed in a
   *     later major version is not decided.
   */
  @Deprecated(since = "2.1.0")
  public static boolean isOutTree(Graph graph) {
    return isTree(graph);
  }

  /**
   * Returns whether the given graph is a path (tree where every node has out-degree ≤ 1).
   * <p>
   * A path is a linear chain of nodes. An empty graph is a path. A single node is a path.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a path
   */
  public static boolean isPath(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null && info.rootCount <= 1 && info.maxOutDegree <= 1;
  }

  /**
   * Returns whether the given graph is bipartite, reading it as undirected.
   * <p>
   * The nodes of a bipartite graph split into two classes such that every edge runs between the
   * classes rather than inside one. The graph is read as undirected like
   * {@link Components#ccsOf(Graph)}: the edges {@code a → b} and {@code b → a} are one undirected
   * edge rather than two. That collapsing cannot change the answer, since a reciprocal pair is an
   * even cycle either way - but a <strong>self-loop</strong> can, and it makes the graph not
   * bipartite, because a node adjacent to itself belongs in neither class. This is the one place
   * where the undirected predicates here do not ignore self-loops: two-colorability is not a
   * connectivity question. An empty graph is bipartite.
   * <p>
   * Terminates as soon as an edge closes an odd cycle. This is the only predicate here that holds
   * two elements per node, and therefore the only one limited in graph size: it requires
   * {@code nodeCount < 2^30}. That limit is reachable rather than theoretical - a graph of 10^9
   * nodes fits in a large heap - so exceeding it is rejected outright instead of surfacing as a
   * failure to allocate an array of negative length.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> 10 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is bipartite
   * @throws IllegalArgumentException if {@code graph.nodeCount() >= 2^30}
   * @see #isUndirectedForest(Graph)
   */
  public static boolean isBipartite(Graph graph) {
    int n = graph.nodeCount();
    if (n == 0) {
      return true;
    }
    if (n >= MAX_BIPARTITE_NODE_COUNT) {
      throw new IllegalArgumentException(
          "isBipartite holds two elements per node and so requires nodeCount < 2^30, but was " + n);
    }

    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;

    // Double cover: element u means "u in class A", element u + n means "u in class B". An edge
    // ties each endpoint to the other's opposite, so once a node shares a set with its own
    // opposite, both classes would have to hold it - an odd cycle. Both unions are symmetric in
    // u and v, which is why the stored direction is irrelevant and a reciprocal pair simply
    // repeats them.
    var sets = new UnionFind(2 * n);

    for (int u = 0; u < n; u++) {
      int end = adjacencyStart[u + 1];
      for (int ei = adjacencyStart[u]; ei < end; ei++) {
        int v = adjacency[ei];
        if (u == v) {
          return false;
        }
        sets.union(u, v + n);
        sets.union(u + n, v);
        if (sets.find(u) == sets.find(u + n)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns whether the given graph is a tree when read as undirected: connected, with exactly
   * {@code nodeCount - 1} undirected edges.
   * <p>
   * Where {@link #isTree(Graph)} requires every edge to point away from one root, this ignores
   * direction entirely, so a graph whose edges point toward a root, or in mixed directions, is a
   * tree here as long as the underlying undirected structure is one. The graph is read as
   * undirected like {@link Components#ccsOf(Graph)}: the edges {@code a → b} and {@code b → a} are
   * one undirected edge rather than two, so a graph storing its undirected edges symmetrically is
   * a tree exactly when the same structure stored with a single direction per edge is. Self-loops
   * are ignored. An empty graph is a tree (vacuously), and so is a single node with no edges.
   * <p>
   * A non-empty graph holding more than {@code 3 x nodeCount - 2} edges is rejected without
   * reading any of them, since no tree can store that many even with a self-loop on every node.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), plus O(log outDegree) per edge that is stored in both directions;
   * O(1) when the edge count alone rules out a tree.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a tree when read as undirected
   * @see #isTree(Graph)
   * @see #isUndirectedForest(Graph)
   */
  public static boolean isUndirectedTree(Graph graph) {
    int n = graph.nodeCount();
    if (n == 0) {
      return true;
    }
    if (exceedsAcyclicEdgeBound(graph)) {
      return false;
    }
    // A cycle yields -1, which can never equal n - 1 here, so one comparison covers both
    // acyclicity and connectedness.
    return undirectedMergeCount(graph) == n - 1;
  }

  /**
   * Returns whether the given graph is a forest when read as undirected, that is, whether it holds
   * no undirected cycle.
   * <p>
   * A forest is a disjoint union of trees, so this is {@link #isUndirectedTree(Graph)} without the
   * requirement that there be exactly one component. It is also the undirected counterpart of
   * {@link #isDag(Graph)}: an undirected graph is acyclic exactly when it is a forest. The graph is
   * read as undirected like {@link Components#ccsOf(Graph)}, so a reciprocal pair
   * {@code a → b} / {@code b → a} is one edge and not a cycle, and self-loops are ignored. An empty
   * graph is a forest.
   * <p>
   * A non-empty graph holding more than {@code 3 x nodeCount - 2} edges is rejected without
   * reading any of them, since no forest can store that many even with a self-loop on every node.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), plus O(log outDegree) per edge that is stored in both directions;
   * O(1) when the edge count alone rules out a forest.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph holds no undirected cycle
   * @see #isForest(Graph)
   * @see #isUndirectedTree(Graph)
   */
  public static boolean isUndirectedForest(Graph graph) {
    if (exceedsAcyclicEdgeBound(graph)) {
      return false;
    }
    return undirectedMergeCount(graph) >= 0;
  }

  /**
   * Returns whether the given graph is weakly connected: at most one component when edge
   * direction is ignored.
   * <p>
   * This is the whole-graph question behind {@link Components#wccsOf(Graph)}, which it would
   * otherwise be answered from, and it needs neither that partition nor a full pass - the check
   * stops once the nodes are known to form one component. For the directed reading see
   * {@link Sccs#isStronglyConnected()}. An empty graph and a single node are weakly connected
   * (vacuously). Self-loops are irrelevant here, as they connect nothing.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph consists of at most one weakly connected component
   * @see Components#wccsOf(Graph)
   * @see Sccs#isStronglyConnected()
   */
  public static boolean isWeaklyConnected(Graph graph) {
    int n = graph.nodeCount();
    if (n <= 1) {
      return true;
    }

    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;
    // Reciprocal pairs need no attention: a second union of the same two nodes merges nothing,
    // and neither does a self-loop.
    var sets = new UnionFind(n);
    int merges = 0;

    for (int u = 0; u < n; u++) {
      int end = adjacencyStart[u + 1];
      for (int ei = adjacencyStart[u]; ei < end; ei++) {
        if (sets.union(u, adjacency[ei]) && ++merges == n - 1) {
          return true; // n - 1 merges join every node; no later edge can undo that
        }
      }
    }
    return false;
  }

  /**
   * Returns whether {@code graph} holds more edges than any acyclic undirected reading could: at
   * most {@code nodeCount - 1} undirected edges, each stored in up to two directions, plus at most
   * one self-loop per node. Rejecting on that count alone is what bounds the reciprocal-pair
   * searches in {@link #undirectedMergeCount(Graph)} by nodeCount instead of edgeCount - without
   * it, a dense symmetric graph would reach the search for every edge it holds.
   */
  private static boolean exceedsAcyclicEdgeBound(Graph graph) {
    int n = graph.nodeCount();
    return graph.edgeCount() > (n == 0 ? 0 : 3L * n - 2);
  }

  /**
   * Merges the endpoints of every undirected edge and returns the number of merges performed, so
   * that the nodes form {@code nodeCount - result} components; or {@code -1} as soon as an edge
   * closes an undirected cycle. Self-loops are skipped.
   * <p>
   * Nodes are walked in ascending order, and that is what makes a failed union classifiable. A
   * failure means both endpoints are already connected, so the edge is either the second stored
   * direction of an edge already counted or a genuine extra edge closing a cycle. For
   * {@code v > u} the opposite direction sits in a list not yet visited, so this is the first
   * encounter with {@code {u,v}} and the failure is a cycle. For {@code v < u} one binary search
   * in v's out-adjacency decides it. A successful union never needs the test: had the opposite
   * direction been counted, the endpoints would already be connected and the union would have
   * failed. Searching always would cost a search per edge; searching never would report every
   * symmetrically stored edge as a cycle.
   */
  private static int undirectedMergeCount(Graph graph) {
    int n = graph.nodeCount();
    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;
    var sets = new UnionFind(n);
    int merges = 0;

    for (int u = 0; u < n; u++) {
      int end = adjacencyStart[u + 1];
      for (int ei = adjacencyStart[u]; ei < end; ei++) {
        int v = adjacency[ei];
        if (u == v) {
          continue;
        }
        if (sets.union(u, v)) {
          merges++;
        } else if (v > u || SortedIntView.indexOfInRange(
            adjacency, adjacencyStart[v], adjacencyStart[v + 1], u) < 0) {
          return -1;
        }
      }
    }
    return merges;
  }

  /**
   * Union-find over a fixed element count, with path halving and union by rank. {@code union}
   * reports whether it merged two distinct sets, which is what lets a caller count components
   * without a second pass. Union by rank keeps the rank below {@code log2(elementCount)}, so a
   * byte per element suffices for any array Java can allocate.
   */
  private static final class UnionFind {

    private final int[] parent;
    private final byte[] rank;

    UnionFind(int elementCount) {
      this.parent = new int[elementCount];
      this.rank = new byte[elementCount];
      for (int i = 0; i < elementCount; i++) {
        parent[i] = i;
      }
    }

    int find(int element) {
      int x = element;
      while (parent[x] != x) {
        parent[x] = parent[parent[x]]; // path halving
        x = parent[x];
      }
      return x;
    }

    boolean union(int a, int b) {
      int ra = find(a);
      int rb = find(b);
      if (ra == rb) {
        return false;
      }
      if (rank[ra] < rank[rb]) {
        parent[ra] = rb;
      } else if (rank[ra] > rank[rb]) {
        parent[rb] = ra;
      } else {
        parent[rb] = ra;
        rank[ra]++;
      }
      return true;
    }
  }

  /**
   * Computes forest properties in a single edge scan pass. Returns null if the graph
   * is not a forest (some node has in-degree > 1, or the graph contains a cycle).
   */
  private static ForestInfo computeForestInfo(Graph graph) {
    int n = graph.nodeCount();
    if (n == 0) {
      return new ForestInfo(0, 0);
    }

    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;

    // Count in-degrees via edge scan.
    var inDegree = new int[n];
    for (int target : adjacency) {
      if (++inDegree[target] > 1) {
        return null; // in-degree > 1 → not a forest
      }
    }

    // Combined pass: count roots (enqueueing them for BFS), track maxOutDegree.
    // Acyclicity check via BFS from all roots along out-edges: with inDegree ≤ 1,
    // each non-root has a unique parent, so every node is enqueued at most once.
    // If every node is reached, the graph is a forest. Unreached nodes form cycles.
    int[] queue = new int[n];
    int head = 0;
    int tail = 0;
    int maxOutDegree = 0;
    for (int i = 0; i < n; i++) {
      int outDeg = adjacencyStart[i + 1] - adjacencyStart[i];
      if (outDeg > maxOutDegree) maxOutDegree = outDeg;
      if (inDegree[i] == 0) queue[tail++] = i;
    }
    int rootCount = tail;
    while (head < tail) {
      int node = queue[head++];
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        queue[tail++] = adjacency[i];
      }
    }
    if (tail != n) return null; // unreached nodes → cycle(s)

    return new ForestInfo(rootCount, maxOutDegree);
  }

  private record ForestInfo(int rootCount, int maxOutDegree) {}

  /**
   * Returns the dominator tree as a graph: an edge {@code idom(n) -> n} for every node that has
   * an immediate dominator.
   * <p>
   * Every root of the dominator forest - each entry, and each node only the virtual root above
   * several entries dominates - has in-degree 0, and so has every unreachable node. The result
   * is a forest of out-trees, ready for a traversal such as SSA renaming with
   * {@link IterativeDfsTraverser#enterNode(int)} and {@code leaveNode(int)}. For ancestor
   * queries, {@link Ancestry#of(Dominators)} is cheaper than {@code Ancestry.of(dominatorTree(d))}:
   * it shares the immediate dominators instead of building and checking a graph.
   * <p>
   * Computed on every call, not cached - keep the result.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1) bytes plus 4 bytes per tree edge for the result.
   *
   * @param dominators the dominators to arrange as a tree
   * @return the dominator tree, with the node IDs of the analyzed graph
   */
  public static Graph dominatorTree(Dominators dominators) {
    var children = ParentForest.childrenOf(dominators.immediateDominator);
    return new Graph(children.start(), children.nodes());
  }

  /**
   * Returns the dominance frontiers as a graph: an edge {@code n -> m} whenever {@code m} is in
   * the dominance frontier of {@code n}.
   * <p>
   * The dominance frontier of {@code n} holds the nodes where its dominance ends: {@code m} is in
   * it if {@code n} dominates a predecessor of {@code m} but does not strictly dominate {@code m}.
   * A node can be in its own frontier, so the result may have self-loops. Unreachable nodes have
   * empty frontiers and appear in none. For phi placement, use {@link IteratedDominanceFrontier},
   * which never materializes this relation.
   * <p>
   * Computed on every call, not cached - keep the result. The relation can grow quadratically
   * in the node count, for example on a ladder of branches that all join at the end.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + frontierSize).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + frontierSize) bytes for the result;
   * 4 x nodeCount bytes additional during computation. The predecessors come from the
   * in-adjacency of the analyzed graph.
   *
   * @param dominators the dominators whose frontiers to compute
   * @return the frontier relation, with the node IDs of the analyzed graph
   * @throws IllegalArgumentException if the relation has more than {@code Integer.MAX_VALUE - 8}
   *                                  pairs
   */
  public static Graph dominanceFrontiers(Dominators dominators) {
    BiAdjacentGraph graph = dominators.graph;
    int[] idom = dominators.immediateDominator;
    int n = idom.length;
    int[] inStart = graph.inAdjacencyStart;
    int[] in = graph.inAdjacency;

    // Walk up from every predecessor of m to just below idom(m), adding m on the way (Cooper,
    // Harvey and Kennedy). A node already holding m means the rest of the walk is done, so every
    // step adds a new pair. Run twice - count, then fill - so that each frontier comes out sorted
    // without an intermediate pair buffer.
    var lastAdded = new int[n];
    var start = new int[n + 1];
    Arrays.fill(lastAdded, -1);
    long total = 0;
    for (int m = 0; m < n; m++) {
      if (!dominators.isReachable(m)) continue;
      int stop = idom[m];
      for (int i = inStart[m], end = inStart[m + 1]; i < end; i++) {
        int runner = in[i];
        if (!dominators.isReachable(runner)) continue;
        while (runner != stop && lastAdded[runner] != m) {
          lastAdded[runner] = m;
          start[runner]++;
          total++;
          runner = idom[runner];
        }
      }
    }
    if (total > Integer.MAX_VALUE - 8) {
      throw new IllegalArgumentException(
          "the dominance frontier relation has " + total + " pairs, more than an array can hold");
    }
    int next = 0;
    for (int node = 0; node < n; node++) {
      int count = start[node];
      start[node] = next;
      next += count;
    }
    start[n] = next;
    var frontier = new int[next];
    Arrays.fill(lastAdded, -1);
    for (int m = 0; m < n; m++) {
      if (!dominators.isReachable(m)) continue;
      int stop = idom[m];
      for (int i = inStart[m], end = inStart[m + 1]; i < end; i++) {
        int runner = in[i];
        if (!dominators.isReachable(runner)) continue;
        while (runner != stop && lastAdded[runner] != m) {
          lastAdded[runner] = m;
          frontier[start[runner]++] = m;
          runner = idom[runner];
        }
      }
    }
    for (int node = n; node > 0; node--) {
      start[node] = start[node - 1];
    }
    start[0] = 0;
    return new Graph(start, frontier);
  }

  /**
   * Derives an edge mapping from a node mapping and two graphs.
   * <p>
   * For each edge in {@code newGraph}, looks up the corresponding edge in {@code oldGraph}
   * using the node mapping. The result maps new edge indices to old edge indices.
   * <p>
   * The node mapping must be in the direction new → old (as produced by all
   * {@link NodeMapping} factory methods).
   * <p>
   * <strong>Not applicable to a transpose.</strong> The lookup searches the target of each
   * new edge in the out-adjacency of its source, and {@link Graph#transposed()} swaps those
   * two roles - so {@code edgeMapping(graph.transposed(), graph, identity)} returns almost
   * exclusively {@code -1} instead of failing. Use
   * {@link #transposedEdgeMapping(Graph)} for that case.
   * <p>
   * <strong>Time:</strong> O(newEdgeCount x log d̄) where d̄ is the average out-degree
   * in the old graph (binary search per edge).<br>
   * <strong>Memory:</strong> O(newEdgeCount) for the result.
   *
   * @param newGraph the new (remapped) graph
   * @param oldGraph the original graph
   * @param newToOld node mapping from new node IDs to old node IDs
   * @return edge mapping from new edge indices to old edge indices
   * @see #transposedEdgeMapping(Graph)
   */
  public static EdgeMapping edgeMapping(Graph newGraph, Graph oldGraph, NodeMapping newToOld) {
    int newEdgeCount = newGraph.edgeCount();
    int[] mapping = new int[newEdgeCount];

    var newAdj = newGraph.outAdjacency;
    var newStart = newGraph.outAdjacencyStart;
    var oldAdj = oldGraph.outAdjacency;
    var oldStart = oldGraph.outAdjacencyStart;

    int ei = 0;
    for (int newNode = 0; newNode < newGraph.nodeCount(); newNode++) {
      int oldNode = newToOld.get(newNode);
      int newEnd = newStart[newNode + 1];
      for (int i = newStart[newNode]; i < newEnd; i++) {
        int newTarget = newAdj[i];
        int oldTarget = newToOld.get(newTarget);
        // Binary search for oldTarget in old node's adjacency list
        int oldEdgeIdx = SortedIntView.indexOfInRange(
            oldAdj, oldStart[oldNode], oldStart[oldNode + 1], oldTarget);
        mapping[ei++] = oldEdgeIdx;
      }
    }

    int valid = IndexMapping.countValid(mapping);
    return new EdgeMapping(mapping, valid, valid < mapping.length);
  }

  /**
   * Derives the edge mapping between a graph and its transpose.
   * <p>
   * Maps each edge index of {@code graph.transposed()} to the edge index of the same edge
   * in {@code graph}, in the direction new → old like every other mapping factory here.
   * {@link Graph#transposed()} preserves nodes but not edge indices, so this is what
   * translates edge metadata onto the transpose:
   * <pre>{@code
   * Graph transpose = graph.transposed();
   * EdgeMapping newToOld = Graphs.transposedEdgeMapping(graph);
   * double[] transposedWeights = new double[transpose.edgeCount()];
   * for (int e = 0; e < transpose.edgeCount(); e++) {
   *     transposedWeights[e] = weights[newToOld.get(e)];
   * }
   * }</pre>
   * The mapping is <strong>total</strong>: every edge of the transpose is an edge of the
   * original with source and target swapped, so it holds no {@code -1} entries and
   * {@link IndexMapping#isPartial()} is {@code false}.
   * <p>
   * For a single edge rather than a full pass, use
   * {@link BiAdjacentGraph#transposedEdgeIndex(int)}, which is the inverse direction.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x edgeCount bytes for the result, plus 4 x nodeCount bytes
   * during computation.
   *
   * @param graph the graph whose transpose is to be addressed
   * @return edge mapping from the transpose's edge indices to this graph's edge indices
   * @see Graph#transposed()
   * @see BiAdjacentGraph#transposedEdgeIndex(int)
   */
  public static EdgeMapping transposedEdgeMapping(Graph graph) {
    int n = graph.nodeCount();
    int m = graph.edgeCount();
    var outAdjacency = graph.outAdjacency;
    var outAdjacencyStart = graph.outAdjacencyStart;

    // Write pointer per target node: where that node's next in-edge slot goes.
    // A bi-adjacent graph already has those offsets; otherwise count and prefix-sum them.
    int[] writePos;
    if (graph instanceof BiAdjacentGraph biAdjacent) {
      writePos = Arrays.copyOf(biAdjacent.inAdjacencyStart, n);
    } else {
      writePos = new int[n];
      for (int target : outAdjacency) {
        writePos[target]++;
      }
      int nextStart = 0;
      for (int node = 0; node < n; node++) {
        int inDegree = writePos[node];
        writePos[node] = nextStart;
        nextStart += inDegree;
      }
    }

    // Same counting sort as Graph.biAdjacentGraph(), with the edge index as payload instead
    // of the source node. Ascending node scan makes slot q of the transpose and entry q of
    // the mapping describe the same edge.
    int[] mapping = new int[m];
    for (int node = 0; node < n; node++) {
      int end = outAdjacencyStart[node + 1];
      for (int edgeIndex = outAdjacencyStart[node]; edgeIndex < end; edgeIndex++) {
        mapping[writePos[outAdjacency[edgeIndex]]++] = edgeIndex;
      }
    }

    return new EdgeMapping(mapping, m, false);
  }

  /**
   * Derives an edge partition from a node partition and two graphs.
   * <p>
   * Maps original edge indices to condensed edge indices. Intra-group edges (edges where
   * both source and target belong to the same group) are unmapped ({@code -1}), and so are
   * edges from or to a node in no group.
   * Multiple original cross-group edges that connect the same pair of groups map to
   * the same condensed edge (many-to-one mapping).
   * <p>
   * The condensed graph must be the result of {@link NodePartition#condense()} applied to
   * the given partition.
   * <p>
   * <strong>Time:</strong> O(originalEdgeCount x log d̄) where d̄ is the average out-degree
   * in the condensed graph (binary search per edge).<br>
   * <strong>Memory:</strong> O(originalEdgeCount + condensedEdgeCount) for the result.
   *
   * @param condensedGraph the condensation graph (from {@link NodePartition#condense()})
   * @param originalGraph  the original graph
   * @param partition      node partition mapping (from {@link Components} or
   *                       {@link NodePartition#of(Graph, int[], int)})
   * @return edge partition from original edge indices to condensed edge indices
   */
  public static EdgePartition edgePartition(Graph condensedGraph, Graph originalGraph, NodePartition partition) {
    int oldEdgeCount = originalGraph.edgeCount();
    int[] mapping = new int[oldEdgeCount];
    Arrays.fill(mapping, -1);

    var origAdj = originalGraph.outAdjacency;
    var origStart = originalGraph.outAdjacencyStart;
    var condAdj = condensedGraph.outAdjacency;
    var condStart = condensedGraph.outAdjacencyStart;

    // Forward pass: for each original edge, find the condensed edge (if cross-group).
    for (int origNode = 0; origNode < originalGraph.nodeCount(); origNode++) {
      int sourceGroup = partition.get(origNode);
      if (sourceGroup < 0) continue;
      int origEnd = origStart[origNode + 1];
      for (int ei = origStart[origNode]; ei < origEnd; ei++) {
        int origTarget = origAdj[ei];
        int targetGroup = partition.get(origTarget);
        if (sourceGroup != targetGroup && targetGroup >= 0) {
          int condEdgeIdx = SortedIntView.indexOfInRange(
              condAdj, condStart[sourceGroup], condStart[sourceGroup + 1], targetGroup);
          mapping[ei] = condEdgeIdx;
        }
      }
    }

    // Build reverse CSR: for each condensed edge, which original edges map to it.
    int condEdgeCount = condensedGraph.edgeCount();
    int[] memberStart = new int[condEdgeCount + 1];

    // Count members per group.
    for (int ei = 0; ei < oldEdgeCount; ei++) {
      if (mapping[ei] >= 0) {
        memberStart[mapping[ei] + 1]++;
      }
    }

    // Prefix sum.
    for (int g = 0; g < condEdgeCount; g++) {
      memberStart[g + 1] += memberStart[g];
    }

    // Fill members (ascending scan of ei ensures sorted order within each group).
    int totalMapped = memberStart[condEdgeCount];
    int[] members = new int[totalMapped];
    int[] writePos = Arrays.copyOf(memberStart, condEdgeCount);
    for (int ei = 0; ei < oldEdgeCount; ei++) {
      if (mapping[ei] >= 0) {
        members[writePos[mapping[ei]]++] = ei;
      }
    }

    int valid = IndexMapping.countValid(mapping);
    return new EdgePartition(mapping, valid, valid < mapping.length, members, memberStart);
  }

  /**
   * Iterative DFS (explicit stack, no recursion) for cycle detection. Detects back-edges
   * (including self-loops) and terminates early on the first cycle found.
   * <p>
   * visited and on-stack flags are packed 2 bits per node into a single {@code int[]}
   * (bit 0 = visited, bit 1 = on-stack, 16 nodes per int): one memory touch per node check
   * instead of two separate bit-set lookups.
   */
  private static final class CycleChecker {

    private static final int VISITED = 0b01;
    private static final int ON_STACK = 0b10;

    private final Graph graph;
    private final int[] state;
    boolean hasCycle;

    CycleChecker(Graph graph) {
      this.graph = graph;
      this.state = new int[(graph.nodeCount() >> 4) + 1];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = graph.outAdjacencyStart;
      int[] adj = graph.outAdjacency;
      int[] state = this.state;
      // Each frame packs (node << 32 | resume cursor) into one long: node and cursor share a
      // cache line, and the array grows on demand (one allocation per resize instead of two).
      long[] frames = new long[Math.max(16, Math.min(n, 1024))];

      for (int root = 0; root < n; root++) {
        int rShift = (root & 0xF) << 1;
        if (((state[root >> 4] >>> rShift) & VISITED) != 0) {
          continue;
        }
        state[root >> 4] |= (VISITED | ON_STACK) << rShift;
        frames[0] = ((long) root << 32) | (out[root] & 0xFFFFFFFFL);
        int top = 0;

        outer:
        while (top >= 0) {
          long frame = frames[top];
          int u = (int) (frame >>> 32);
          int end = out[u + 1];
          // Tight neighbor scan: i and end stay in registers; the frame list is
          // touched only when we actually descend, not once per edge.
          int i = (int) frame;
          while (i < end) {
            int v = adj[i++];
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;
            if ((flags & ON_STACK) != 0) {
              hasCycle = true;
              return;
            }
            if ((flags & VISITED) == 0) {
              frames[top] = (frame & 0xFFFFFFFF00000000L) | (i & 0xFFFFFFFFL);
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              if (++top == frames.length) {
                frames = Arrays.copyOf(frames, top << 1);
              }
              frames[top] = ((long) v << 32) | (out[v] & 0xFFFFFFFFL);
              continue outer;
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
          top--;
        }
      }
    }
  }
}
