package com.kobayami.graphity;

import java.util.Arrays;

/**
 * Utility methods for graph transformations and shape checks.
 * <p>
 * Provides operations that create new {@link Graph} instances from existing graphs
 * and {@link NodeMapping}s (the original graph is never modified), as well as
 * predicate methods to test structural properties of a graph.
 *
 * <pre>{@code
 * // Shape checks
 * Graphs.isDag(graph);       // acyclic?
 * Graphs.isForest(graph);    // DAG with all inDegrees <= 1?
 * Graphs.isOutTree(graph);   // forest with exactly one root?
 * Graphs.isPath(graph);      // out-tree with all outDegrees <= 1?
 *
 * // Reindexing for cache-friendly layout
 * Graph reindexed = Graphs.dfsReordered(graph);
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
   * visited close together during traversal receive adjacent IDs. This is the cache-friendly
   * reindexing shortcut for {@code remapped(graph, NodeMapping.reindex(graph))}.
   * <p>
   * DFS-preorder relabeling clusters each node's out-neighbors near it in ID space. This turns
   * the random {@code state[target]} accesses of the DFS-based algorithms (SCC, WCC, CCs,
   * TopOrder, back-edge and edge-type classification) into near-sequential scans. On
   * poorly-ordered inputs this speeds those traversals up by a multiple; on already
   * well-ordered inputs it is effectively a no-op.
   * <p>
   * The returned graph uses fresh node IDs. When the caller also needs to translate results
   * back to the original IDs, obtain the mapping explicitly instead:
   * <pre>{@code
   * NodeMapping newToOld = NodeMapping.reindex(graph);
   * Graph reordered = Graphs.remapped(graph, newToOld);
   * }</pre>
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result.
   *
   * @param graph the graph to reorder
   * @return a new graph with DFS-preorder node IDs
   * @see NodeMapping#reindex(Graph)
   * @see #remapped(Graph, NodeMapping)
   */
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
   * Equivalently: a forest is a DAG consisting of zero or more disjoint out-trees.
   * An empty graph is a forest.
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
   * Returns whether the given graph is an out-tree (forest that is weakly connected).
   * <p>
   * An out-tree has exactly one root node (in-degree 0), and every other node has
   * in-degree exactly 1. An empty graph is an out-tree (vacuously). A single node
   * with no edges is an out-tree.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is an out-tree
   */
  public static boolean isOutTree(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null && info.rootCount <= 1;
  }

  /**
   * Returns whether the given graph is a path (out-tree where every node has out-degree ≤ 1).
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
   * both source and target belong to the same group) are unmapped ({@code -1}).
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
   * @param partition      node partition mapping (from {@link Components})
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
      int origEnd = origStart[origNode + 1];
      for (int ei = origStart[origNode]; ei < origEnd; ei++) {
        int origTarget = origAdj[ei];
        int targetGroup = partition.get(origTarget);
        if (sourceGroup != targetGroup) {
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
