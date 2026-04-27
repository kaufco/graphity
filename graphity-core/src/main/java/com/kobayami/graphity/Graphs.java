package com.kobayami.graphity;

import java.util.Arrays;
import java.util.BitSet;

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
 * Graph reindexed = Graphs.remapped(graph, NodeMapping.reindex(graph));
 *
 * // Subgraph extraction
 * Graph sub = Graphs.remapped(graph, NodeMapping.forSubgraph(selectedNodes));
 *
 * // Custom permutation
 * Graph permuted = Graphs.remapped(graph, NodeMapping.forPermutation(order));
 * }</pre>
 *
 * @see NodeMapping
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
   * <strong>Time:</strong> O(nodeCount + edgeCount + Σ newDegree × log(newDegree)).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result;
   * O(nodeCount) additional during computation (inverse mapping).
   *
   * @param graph    the original graph
   * @param newToOld mapping from new node IDs to original node IDs
   * @return a new graph with remapped node IDs
   * @throws IllegalArgumentException if the mapping contains duplicate targets
   *         (not injective — cannot compute inverse)
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

    // Pass 1: count out-degrees in new graph.
    var outStart = new int[newNodeCount + 1];
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

    // Prefix sum: convert degrees to CSR offsets.
    for (int n = 0; n < newNodeCount; n++) {
      outStart[n + 1] += outStart[n];
    }
    int totalEdges = outStart[newNodeCount];

    // Pass 2: fill adjacency and sort each adjacency list.
    var outAdj = new int[totalEdges];
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

    return new Graph(outStart, outAdj);
  }

  // ---- Shape checks ----

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

  // ---- Shape check internals ----

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

  // ---- Edge mapping derivation ----

  /**
   * Derives an edge mapping from a node mapping and two graphs.
   * <p>
   * For each edge in {@code newGraph}, looks up the corresponding edge in {@code oldGraph}
   * using the node mapping. The result maps new edge indices to old edge indices.
   * <p>
   * The node mapping must be in the direction new → old (as produced by all
   * {@link NodeMapping} factory methods).
   * <p>
   * <strong>Time:</strong> O(newEdgeCount × log d̄) where d̄ is the average out-degree
   * in the old graph (binary search per edge).<br>
   * <strong>Memory:</strong> O(newEdgeCount) for the result.
   *
   * @param newGraph the new (remapped) graph
   * @param oldGraph the original graph
   * @param newToOld node mapping from new node IDs to old node IDs
   * @return edge mapping from new edge indices to old edge indices
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
   * <strong>Time:</strong> O(originalEdgeCount × log d̄) where d̄ is the average out-degree
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
   * DFS traverser for cycle detection. Detects back-edges (including self-loops) via an
   * {@code onStack} BitSet. Terminates early on the first cycle found.
   */
  private static final class CycleChecker extends DfsTraverser {

    private final BitSet onStack;
    boolean hasCycle;

    CycleChecker(Graph graph) {
      super(graph);
      this.onStack = new BitSet(graph.nodeCount());
    }

    @Override
    protected void visitNode(int node) {
      if (hasCycle) return;
      onStack.set(node);

      var adjacency = graph.outAdjacency;
      var adjacencyStart = graph.outAdjacencyStart;
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        int target = adjacency[i];
        if (onStack.get(target)) {
          hasCycle = true;
          return;
        }
        visit(target);
        if (hasCycle) return;
      }

      onStack.clear(node);
    }
  }
}
