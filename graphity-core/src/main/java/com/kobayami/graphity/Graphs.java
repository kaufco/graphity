package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Objects;

/**
 * Utility methods for graph transformations and shape checks.
 * <p>
 * Provides operations that create new {@link Graph} instances from existing graphs
 * and {@link NodeMapping}s (the original graph is never modified), predicate methods to test
 * structural properties of a graph, its complete text form, {@link #adjacencyListOf(Graph)}, a
 * structural hash, {@link #topologyHash(Graph)}, and the {@link #diff(Graph, Graph)} of two
 * versions.
 * <p>
 * A predicate whose name does not say otherwise reads the graph as directed; the ones prefixed
 * {@code undirected} read it as {@link Components#ccsOf(Graph)} does. {@code isBipartite} and
 * {@code isWeaklyConnected} carry no prefix because neither property has a directed reading that
 * could be meant instead; {@code isStronglyConnected} is directed by definition.
 *
 * <pre>{@code
 * // Shape checks, directed reading
 * Graphs.isDag(graph);              // acyclic?
 * Graphs.isForest(graph);           // DAG with all inDegrees <= 1?
 * Graphs.isTree(graph);             // forest with exactly one root?
 * Graphs.isPath(graph);             // tree with all outDegrees <= 1?
 * Graphs.isStronglyConnected(graph); // every node reaches every other?
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
 * Graph reached = Graphs.remapped(graph, NodeMapping.forSubgraph(Graphs.reachableFrom(graph, entry)));
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
   * @throws IllegalArgumentException if a new node is unmapped, or two new nodes map to the same
   *         original node
   * @throws IndexOutOfBoundsException if a new node maps to an ID that is not a node of
   *         {@code graph}
   * @see NodeMapping#reindex(Graph)
   * @see NodeMapping#forSubgraph(java.util.BitSet)
   * @see NodeMapping#forPermutation(IntList)
   */
  public static Graph remapped(Graph graph, NodeMapping newToOld) {
    int newNodeCount = newToOld.mapping.length;
    if (newNodeCount == 0) {
      return new Graph(new int[]{0}, new int[0]);
    }

    // Inverse (old → new) for edge-target translation, sized by the graph rather than by the
    // largest mapped value, and built in the same pass that checks every mapped node.
    int[] fwd = newToOld.mapping;
    int oldNodeCount = graph.nodeCount();
    int[] bwd = new int[oldNodeCount];
    Arrays.fill(bwd, -1);
    for (int n = 0; n < newNodeCount; n++) {
      int oldNode = fwd[n];
      if (oldNode == -1) {
        throw new IllegalArgumentException("new node " + n + " is unmapped");
      }
      if (oldNode < 0 || oldNode >= oldNodeCount) throw notANode(n, oldNode, oldNodeCount);
      if (bwd[oldNode] != -1) {
        throw new IllegalArgumentException("new nodes " + bwd[oldNode] + " and " + n
            + " both map to node " + oldNode);
      }
      bwd[oldNode] = n;
    }

    var origAdj = Csr.outAdjacency(graph);
    var origStart = Csr.outAdjacencyStart(graph);

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
          if (bwd[oldTarget] >= 0) {
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
          if (bwd[oldTarget] >= 0) {
            outAdj[writePos++] = bwd[oldTarget];
          }
        }
        Arrays.sort(outAdj, outStart[n], outStart[n + 1]);
      }
    }

    return new Graph(outStart, outAdj);
  }

  private static IndexOutOfBoundsException notANode(int newNode, int oldNode, int oldNodeCount) {
    return new IndexOutOfBoundsException("new node " + newNode + " maps to " + oldNode
        + ", which is not a node of the graph (nodeCount=" + oldNodeCount + ")");
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
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
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
   * apply this to the transpose, {@code BiAdjacentGraph.of(graph).transposed()} - which builds the
   * in-adjacency and therefore costs 4 x (nodeCount + edgeCount) bytes, unlike this method itself.
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

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);

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
   * {@link #isStronglyConnected(Graph)}. An empty graph and a single node are weakly connected
   * (vacuously). Self-loops are irrelevant here, as they connect nothing.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph consists of at most one weakly connected component
   * @see Components#wccsOf(Graph)
   * @see #isStronglyConnected(Graph)
   */
  public static boolean isWeaklyConnected(Graph graph) {
    int n = graph.nodeCount();
    if (n <= 1) {
      return true;
    }

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);
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
   * Returns whether the given graph is strongly connected: every node reaches every other,
   * following edge directions.
   * <p>
   * This is the whole-graph question behind {@link Components#sccsOf(Graph)}, answered without
   * building the partition: one depth-first search from node 0 in the manner of Tarjan's
   * algorithm, which stops as soon as a node below the root closes a component of its own - the
   * graph then has a second component. Until that moment no component has been removed from
   * Tarjan's stack, so the stack is exactly the set of visited nodes and needs no storage of its
   * own. On a result already computed, {@link Sccs#isStronglyConnected()} answers in O(1). An
   * empty graph and a single node are strongly connected (vacuously). Iterative; deep graphs do
   * not overflow the call stack.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes, plus a frame stack that grows with the search
   * depth, up to another 8 x nodeCount bytes; nothing afterwards.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph consists of at most one strongly connected component
   * @throws IllegalArgumentException if the depth-first search is deeper than 2^30 - 5 nodes, the
   *     most frames an array can hold
   * @see Components#sccsOf(Graph)
   * @see Sccs#isStronglyConnected()
   * @see #isWeaklyConnected(Graph)
   */
  public static boolean isStronglyConnected(Graph graph) {
    int n = graph.nodeCount();
    if (n <= 1) {
      return true;
    }
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);
    var index = new int[n];   // discovery time from 1; 0 = unvisited
    var low = new int[n];
    int[] frames = DfsFrames.initial(n);
    int time = 1;
    index[0] = 1;
    low[0] = 1;
    frames[0] = 0;
    frames[1] = out[0];
    int top = 0;

    outer:
    while (true) {
      int u = frames[2 * top];
      int end = out[u + 1];
      int i = frames[2 * top + 1];
      while (i < end) {
        int v = adj[i++];
        if (index[v] == 0) {
          frames[2 * top + 1] = i;
          index[v] = ++time;
          low[v] = time;
          if (++top == frames.length >> 1) {
            frames = DfsFrames.grow(frames, n);
          }
          frames[2 * top] = v;
          frames[2 * top + 1] = out[v];
          continue outer;
        }
        if (index[v] < low[u]) {
          low[u] = index[v];
        }
      }
      if (top == 0) {
        return time == n;
      }
      if (low[u] == index[u]) {
        return false;
      }
      int parent = frames[2 * (top - 1)];
      if (low[u] < low[parent]) {
        low[parent] = low[u];
      }
      top--;
    }
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
    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);
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

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);

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
   * Returns the root nodes of the given graph - the nodes without incoming edges - in ascending
   * order. They are the natural starting points of a traversal over a DAG or a forest:
   * <pre>{@code
   * traverser.runFromNodes(Graphs.rootNodes(graph));
   * }</pre>
   * Each call scans all nodes; keep the result if it is needed more than once. For a single node,
   * {@link BiAdjacentGraph#isRootNode(int)} answers in constant time.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 4 bytes per root node for the result.
   *
   * @param graph the graph to search, with in-adjacency
   * @return the root nodes, sorted ascending; newly allocated
   */
  public static SortedIntView rootNodes(BiAdjacentGraph graph) {
    int n = graph.nodeCount();
    int[] inStart = Csr.inAdjacencyStart(graph);
    int count = 0;
    for (int node = 0; node < n; node++) {
      if (inStart[node] == inStart[node + 1]) count++;
    }
    int[] roots = new int[count];
    int next = 0;
    for (int node = 0; node < n; node++) {
      if (inStart[node] == inStart[node + 1]) roots[next++] = node;
    }
    return new SortedIntView(roots, 0, count);
  }

  /**
   * Returns the nodes reachable from {@code source}, the source included, in ascending order.
   * <p>
   * Equivalent to {@link #reachableFrom(Graph, IntList)} with a one-element list.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> as for {@link #reachableFrom(Graph, IntList)}.
   *
   * @param graph  the graph to search
   * @param source source node ID in {@code 0..nodeCount()-1}
   * @return the reachable nodes, sorted ascending
   * @throws IndexOutOfBoundsException if {@code source} is out of range
   */
  public static SortedIntView reachableFrom(Graph graph, int source) {
    return reachableFrom(graph, IntLists.singleton(source));
  }

  /**
   * Returns the nodes reachable from at least one of the given sources, the sources included,
   * in ascending order.
   * <p>
   * Edges are followed in their direction; for the nodes that reach the sources, pass the
   * transpose. Duplicate sources are ignored, an empty list gives an empty result. The result is
   * sorted, so it extracts the reachable part directly:
   *
   * <pre>{@code
   * Graph function = Graphs.remapped(cfg, NodeMapping.forSubgraph(Graphs.reachableFrom(cfg, entry)));
   * }</pre>
   *
   * For distances and shortest paths, use {@link BfsDistances}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 bytes per reachable node for the result. During the traversal,
   * one bit per node and a queue that grows by doubling towards the reachable count, briefly up
   * to 12 bytes per reachable node while it is copied.
   *
   * @param graph   the graph to search
   * @param sources source node IDs, each in {@code 0..nodeCount()-1}
   * @return the reachable nodes, sorted ascending
   * @throws IndexOutOfBoundsException if a source is out of range
   */
  public static SortedIntView reachableFrom(Graph graph, IntList sources) {
    Objects.requireNonNull(graph, "graph");
    Objects.requireNonNull(sources, "sources");
    int n = graph.nodeCount();
    int sourceCount = sources.size();
    for (int i = 0; i < sourceCount; i++) {
      Objects.checkIndex(sources.getInt(i), n);
    }
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);
    var visited = new long[(n >> 6) + 1];
    // Every reachable node enters the queue exactly once, so the queue grows into the result.
    // The initial capacity holds every distinct source.
    var queue = new int[Math.min(n, Math.max(16, sourceCount))];
    int tail = 0;
    for (int i = 0; i < sourceCount; i++) {
      int s = sources.getInt(i);
      if ((visited[s >> 6] & (1L << s)) == 0) {
        visited[s >> 6] |= 1L << s;
        queue[tail++] = s;
      }
    }
    for (int head = 0; head < tail; head++) {
      int u = queue[head];
      for (int e = out[u], end = out[u + 1]; e < end; e++) {
        int v = adj[e];
        if ((visited[v >> 6] & (1L << v)) == 0) {
          visited[v >> 6] |= 1L << v;
          if (tail == queue.length) {
            queue = Arrays.copyOf(queue, (int) Math.min(n, 2L * queue.length));
          }
          queue[tail++] = v;
        }
      }
    }
    // The queue is done; refill it in ascending order from the bit set, at its exact length.
    if (tail < queue.length) queue = Arrays.copyOf(queue, tail);
    int k = 0;
    for (int w = 0; w < visited.length; w++) {
      for (long bits = visited[w]; bits != 0; bits &= bits - 1) {
        queue[k++] = (w << 6) + Long.numberOfTrailingZeros(bits);
      }
    }
    return SortedIntView.viewOf(queue, 0, tail);
  }

  /**
   * Returns the dominator tree as a graph: an edge {@code idom(n) -> n} for every node that has
   * an immediate dominator.
   * <p>
   * Every root of the dominator forest - each entry, and each node only the virtual root above
   * several entries dominates - has in-degree 0, and so has every unreachable node. The result
   * is a forest of out-trees, ready for a traversal such as SSA renaming — assigning each
   * use its reaching definition — with {@link IterativeDfsTraverser#enterNode(int)} and
   * {@code leaveNode(int)}. For ancestor
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
   * Derives an edge mapping from a node mapping and two graphs.
   * <p>
   * For each edge in {@code newGraph}, looks up the corresponding edge in {@code oldGraph}
   * using the node mapping. The result maps new edge indices to old edge indices.
   * <p>
   * The node mapping must be in the direction new → old (as produced by all
   * {@link NodeMapping} factory methods).
   * <p>
   * <strong>Not applicable to a transpose.</strong> The lookup searches the target of each
   * new edge in the out-adjacency of its source, and {@link BiAdjacentGraph#transposed()} swaps
   * those two roles - so {@code edgeMapping(transpose, graph, identity)} returns almost
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

    var newAdj = Csr.outAdjacency(newGraph);
    var newStart = Csr.outAdjacencyStart(newGraph);
    var oldAdj = Csr.outAdjacency(oldGraph);
    var oldStart = Csr.outAdjacencyStart(oldGraph);

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
   * Maps each edge index of the transpose of {@code graph} to the edge index of the same edge
   * in {@code graph}, in the direction new → old like every other mapping factory here.
   * {@link BiAdjacentGraph#transposed()} preserves nodes but not edge indices, so this is what
   * translates edge metadata onto the transpose:
   * <pre>{@code
   * Graph transpose = BiAdjacentGraph.of(graph).transposed();
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
   * @see BiAdjacentGraph#transposed()
   * @see BiAdjacentGraph#transposedEdgeIndex(int)
   */
  public static EdgeMapping transposedEdgeMapping(Graph graph) {
    int n = graph.nodeCount();
    int m = graph.edgeCount();
    var outAdjacency = Csr.outAdjacency(graph);
    var outAdjacencyStart = Csr.outAdjacencyStart(graph);

    // Write pointer per target node: where that node's next in-edge slot goes.
    // A bi-adjacent graph already has those offsets; otherwise count and prefix-sum them.
    int[] writePos;
    if (graph instanceof BiAdjacentGraph biAdjacent) {
      writePos = Arrays.copyOf(Csr.inAdjacencyStart(biAdjacent), n);
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

    // Same counting sort as BiAdjacentGraph.of, with the edge index as payload instead
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

    var origAdj = Csr.outAdjacency(originalGraph);
    var origStart = Csr.outAdjacencyStart(originalGraph);
    var condAdj = Csr.outAdjacency(condensedGraph);
    var condStart = Csr.outAdjacencyStart(condensedGraph);

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
      int[] out = Csr.outAdjacencyStart(graph);
      int[] adj = Csr.outAdjacency(graph);
      int[] state = this.state;
      // Each frame is two interleaved ints, the node and its resume position in the
      // adjacency array: both share a cache line, and the array grows on demand.
      int[] frames = DfsFrames.initial(n);

      for (int root = 0; root < n; root++) {
        int rShift = (root & 0xF) << 1;
        if (((state[root >> 4] >>> rShift) & VISITED) != 0) {
          continue;
        }
        state[root >> 4] |= (VISITED | ON_STACK) << rShift;
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
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;
            if ((flags & ON_STACK) != 0) {
              hasCycle = true;
              return;
            }
            if ((flags & VISITED) == 0) {
              frames[2 * top + 1] = i;
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              if (++top == frames.length >> 1) {
                frames = DfsFrames.grow(frames, n);
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
          top--;
        }
      }
    }
  }

  /**
   * Returns the complete adjacency list of the graph, one line per node in ascending order:
   * <pre>
   * 0 -&gt; [17, 532]
   * 1 -&gt; [4]
   * 2 -&gt; []
   * </pre>
   * Each line ends with a line feed. Unlike {@link Graph#toString()}, which is bounded because it
   * runs implicitly, this lists every node and edge; a {@link BiAdjacentGraph} lists its
   * out-adjacency. The text of a graph with a few hundred million edges exceeds what a
   * {@code String} can hold and ends in an {@link OutOfMemoryError}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) characters for the result.
   *
   * @param graph the graph to list
   * @return the adjacency list, one line per node
   */
  public static String adjacencyListOf(Graph graph) {
    int n = graph.nodeCount();
    int[] start = Csr.outAdjacencyStart(graph);
    int[] targets = Csr.outAdjacency(graph);
    var sb = new StringBuilder((int) Math.min(Integer.MAX_VALUE - 8,
        16L * n + 8L * graph.edgeCount()));
    for (int v = 0; v < n; v++) {
      sb.append(v).append(" -> [");
      for (int e = start[v], end = start[v + 1]; e < end; e++) {
        if (e > start[v]) sb.append(", ");
        sb.append(targets[e]);
      }
      sb.append("]\n");
    }
    return sb.toString();
  }

  /**
   * Returns a 64-bit hash of the graph's structure, consistent with {@link Graph#equals(Object)}:
   * equal graphs - a {@link BiAdjacentGraph} and its uni-adjacent counterpart included - always
   * have equal hashes, and different graphs rarely do.
   * <p>
   * Computed on every call, not cached - compute it once and keep it, for instance as the key of
   * a cache over graph structure or to detect the fixpoint of a transformation. The value depends
   * only on the structure, so it is the same in every run and on every JVM; a Graphity release
   * that changes it says so in its changelog. Equal hashes do not prove equal graphs, and the hash
   * is not built to resist graphs crafted to collide; compare with {@link Graph#equals(Object)}
   * where it matters. {@link Graph#hashCode()} stays O(nodeCount + edgeCount) per call.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   *
   * @param graph the graph to hash
   * @return the structural hash
   */
  public static long topologyHash(Graph graph) {
    int n = graph.nodeCount();
    int[] start = Csr.outAdjacencyStart(graph);
    int[] targets = Csr.outAdjacency(graph);
    long h = mixInto(0x6A09E667F3BCC909L, n);
    h = mixInto(h, graph.edgeCount());
    for (int v = 0; v < n; v++) {
      int end = start[v + 1];
      h = mixInto(h, end - start[v]);
      for (int e = start[v]; e < end; e++) {
        h = mixInto(h, targets[e]);
      }
    }
    // SplitMix64 finalizer: every input bit reaches every output bit.
    h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
    h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
    return h ^ (h >>> 31);
  }

  /** One order-sensitive step of {@link #topologyHash(Graph)}. */
  private static long mixInto(long h, int value) {
    return Long.rotateLeft(h ^ (value * 0x9E3779B97F4A7C15L), 29) * 0xC2B2AE3D27D4EB4FL;
  }

  /**
   * Returns the edges added and removed between two graphs over the same nodes.
   * <p>
   * Merges the sorted adjacency lists of both graphs node by node, so both lists of the result
   * come out in edge index order, each edge named by its index in the graph it belongs to: added
   * edges by their index in {@code after}, removed edges by their index in {@code before}. An
   * empty diff means the graphs are equal.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount of both graphs).<br>
   * <strong>Memory:</strong> 12 bytes per added or removed edge for the result, and up to 18 more
   * while it is collected.
   *
   * @param before the older graph
   * @param after  the newer graph, with the same node count
   * @return the added and removed edges
   * @throws IllegalArgumentException if the node counts differ
   */
  public static GraphDiff diff(Graph before, Graph after) {
    int n = before.nodeCount();
    if (after.nodeCount() != n) {
      throw new IllegalArgumentException("diff needs graphs over the same nodes, but before has "
          + n + " nodes and after " + after.nodeCount());
    }
    int[] oldStart = Csr.outAdjacencyStart(before);
    int[] oldTargets = Csr.outAdjacency(before);
    int[] newStart = Csr.outAdjacencyStart(after);
    int[] newTargets = Csr.outAdjacency(after);
    var added = new IntArrayList();
    var removed = new IntArrayList();
    for (int v = 0; v < n; v++) {
      int o = oldStart[v];
      int oEnd = oldStart[v + 1];
      int a = newStart[v];
      int aEnd = newStart[v + 1];
      // No target reaches Integer.MAX_VALUE - node IDs stay below it -, so it marks an exhausted
      // list.
      while (o < oEnd || a < aEnd) {
        int oldTarget = o < oEnd ? oldTargets[o] : Integer.MAX_VALUE;
        int newTarget = a < aEnd ? newTargets[a] : Integer.MAX_VALUE;
        if (oldTarget == newTarget) {
          o++;
          a++;
        } else if (oldTarget < newTarget) {
          removed.add(v);
          removed.add(oldTarget);
          removed.add(o++);
        } else {
          added.add(v);
          added.add(newTarget);
          added.add(a++);
        }
      }
    }
    return new GraphDiff(edgesOf(added), edgesOf(removed));
  }

  /** Edges from interleaved (source, target, edge index) triples, already in edge index order. */
  private static Edges edgesOf(IntArrayList triples) {
    int count = triples.size() / 3;
    if (count == 0) return Edges.empty();
    var sources = new int[count];
    var targets = new int[count];
    var edgeIndices = new int[count];
    for (int p = 0; p < count; p++) {
      sources[p] = triples.getInt(3 * p);
      targets[p] = triples.getInt(3 * p + 1);
      edgeIndices[p] = triples.getInt(3 * p + 2);
    }
    return Edges.wrap(sources, targets, edgeIndices);
  }
}
