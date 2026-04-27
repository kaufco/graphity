package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;

/**
 * A read-only directed graph, optimized for performance, low memory and cache-friendly traversal.
 * <p>
 * Nodes are contiguous {@code int} IDs in {@code 0..nodeCount()-1}.
 * Out-adjacency lists are stored back-to-back in a flat array, each sorted ascending.
 * The graph stores only structure (nodes and edges) - labels, weights or metadata
 * must be kept externally (e.g. in parallel arrays indexed by node/edge ID).
 * <p>
 * Use {@code Graph} for algorithms that only need out-edges.
 * For in-edge access, obtain a {@link BiAdjacentGraph} via {@link #biAdjacentGraph()}.
 * <p>
 * <strong>Memory:</strong> 4 × (nodeCount + edgeCount) bytes.
 *
 * @see BiAdjacentGraph
 */
public sealed class Graph permits BiAdjacentGraph {

  /**
   * Per-node start offset into {@link #outAdjacency}.
   * Node {@code i}'s out-nodes occupy {@code outAdjacency[outAdjacencyStart[i]..outAdjacencyStart[i+1]-1]},
   * sorted ascending.
   * <p>
   * Length {@code nodeCount + 1}; the sentinel {@code outAdjacencyStart[nodeCount] == edgeCount}
   * keeps {@code outAdjacencyStart[i+1]} always valid (branchless end lookup).
   */
  final int[] outAdjacencyStart;

  /** Flat array of all out-adjacency lists, indexed via {@link #outAdjacencyStart}. */
  final int[] outAdjacency;

  // Package-private. Arrays are shared, not copied.
  Graph(int[] outAdjacencyStart, int[] outAdjacency) {
    this.outAdjacencyStart = outAdjacencyStart;
    this.outAdjacency = outAdjacency;
  }

  /**
   * Returns the number of nodes in this graph.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node count
   */
  public final int nodeCount() {
    return outAdjacencyStart.length - 1;
  }

  /**
   * Returns the number of edges (directed) in this graph.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return edge count
   */
  public final int edgeCount() {
    return outAdjacency.length;
  }

  /**
   * Returns {@code true} if this graph contains no nodes (and therefore no edges).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if {@code nodeCount() == 0}
   */
  public final boolean isEmpty() {
    return nodeCount() == 0;
  }

  /**
   * Returns the out-degree (number of outgoing edges) of {@code node}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return out-degree
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int outDegree(int node) {
    Objects.checkIndex(node, nodeCount());
    return outAdjacencyEnd(node) - outAdjacencyStart[node];
  }

  /**
   * Returns the degree of {@code node}.
   * <p>
   * On a uni-adjacent {@code Graph} this equals {@link #outDegree(int) outDegree(node)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return degree
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int degree(int node) {
    return outDegree(node);
  }

  /**
   * Returns a zero-copy view of {@code node}'s out-nodes, sorted ascending.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return out-nodes as a read-only sorted view
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final SortedIntView outNodes(int node) {
    Objects.checkIndex(node, nodeCount());
    return new SortedIntView(outAdjacency, outAdjacencyStart[node], outAdjacencyEnd(node));
  }

  int outAdjacencyEnd(int node) {
    return outAdjacencyStart[node + 1];
  }

  // ---- Edge index API ----

  /**
   * Returns the edge index of the first out-edge of the given node.
   * <p>
   * Together with {@link #outEdgeEnd(int)}, this defines the edge index range for
   * a node's out-edges: {@code outEdgeStart(node)..outEdgeEnd(node)-1}. Edge indices
   * correspond to positions in the CSR adjacency array and can be used as keys for
   * external edge metadata (e.g. {@code weights[edgeIndex]}).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return edge index of the first out-edge (equal to {@code outEdgeEnd(node)} if no out-edges)
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int outEdgeStart(int node) {
    Objects.checkIndex(node, nodeCount());
    return outAdjacencyStart[node];
  }

  /**
   * Returns the edge index past the last out-edge of the given node (exclusive).
   * <p>
   * Together with {@link #outEdgeStart(int)}, this defines the edge index range for
   * a node's out-edges.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return edge index past the last out-edge (exclusive)
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int outEdgeEnd(int node) {
    Objects.checkIndex(node, nodeCount());
    return outAdjacencyStart[node + 1];
  }

  /**
   * Returns the target node of the edge at the given edge index.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param edgeIndex edge index in {@code 0..edgeCount()-1}
   * @return target node ID
   * @throws IndexOutOfBoundsException if {@code edgeIndex} is out of range
   */
  public final int edgeTarget(int edgeIndex) {
    Objects.checkIndex(edgeIndex, edgeCount());
    return outAdjacency[edgeIndex];
  }

  /**
   * Returns the source node of the edge at the given edge index.
   * <p>
   * Uses binary search over the CSR offset array.
   * <p>
   * <strong>Time:</strong> O(log nodeCount).
   *
   * @param edgeIndex edge index in {@code 0..edgeCount()-1}
   * @return source node ID
   * @throws IndexOutOfBoundsException if {@code edgeIndex} is out of range
   */
  public int edgeSource(int edgeIndex) {
    Objects.checkIndex(edgeIndex, edgeCount());
    // Binary search: find largest node where outAdjacencyStart[node] <= edgeIndex
    int lo = 0, hi = nodeCount() - 1;
    while (lo < hi) {
      int mid = (lo + hi + 1) >>> 1;
      if (outAdjacencyStart[mid] <= edgeIndex) {
        lo = mid;
      } else {
        hi = mid - 1;
      }
    }
    return lo;
  }

  /**
   * Tests whether the edge {@code node → outNode} exists.
   * <p>
   * <strong>Time:</strong> O(log outDegree) via binary search.
   *
   * @param node    node ID in {@code 0..nodeCount()-1}
   * @param outNode potential out-node ID
   * @return {@code true} if the edge exists
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean hasOutNode(int node, int outNode) {
    Objects.checkIndex(node, nodeCount());
    return SortedIntView.containsInRange(outAdjacency, outAdjacencyStart[node], outAdjacencyEnd(node), outNode);
  }

  /**
   * Tests whether {@code node} has a self-loop (an edge to itself).
   * Equivalent to {@link #hasOutNode(int, int) hasOutNode(node, node)}.
   * <p>
   * <strong>Time:</strong> O(log outDegree) via binary search.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if the edge {@code node -> node} exists
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean containsSelfLoop(int node) {
    return hasOutNode(node, node);
  }

  /**
   * Returns a {@link BiAdjacentGraph} with both in- and out-adjacency.
   * The result shares the out-adjacency data; only in-adjacency is newly computed.
   * Not cached — store the result if needed across multiple calls.
   * <p>
   * If this graph is already a {@link BiAdjacentGraph}, returns {@code this} in O(1).
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), or O(1) if already bi-adjacent.<br>
   * <strong>Memory:</strong> +4 × (nodeCount + edgeCount) bytes, or none if already bi-adjacent.
   *
   * @return a bi-adjacent graph with constant-time in- and out-node access
   */
  public BiAdjacentGraph biAdjacentGraph() {
    var totalNodes = nodeCount();
    var totalEdges = edgeCount();
    var inAdjacencyStart = new int[totalNodes + 1];
    var inAdjacency = new int[totalEdges];

    // 1) Count in-degrees
    for (var i = 0; i < totalEdges; i++) {
      inAdjacencyStart[outAdjacency[i]]++;
    }

    // 2) Prefix-sum: convert in-degrees to start offsets
    var nextStart = 0;
    for (var i = 0; i < totalNodes; i++) {
      var inDegree = inAdjacencyStart[i];
      inAdjacencyStart[i] = nextStart;
      nextStart += inDegree;
    }
    inAdjacencyStart[totalNodes] = totalEdges; // sentinel

    // 3) Fill in-adjacency. inAdjacencyStart[target]++ doubles as write pointer.
    //    Ascending node scan ensures each in-list ends up sorted.
    for (var node = 0; node < totalNodes; node++) {
      for (int i = outAdjacencyStart[node]; i < outAdjacencyStart[node + 1]; i++) {
        inAdjacency[inAdjacencyStart[outAdjacency[i]]++] = node;
      }
    }

    // 4) Repair: step 3 shifted inAdjacencyStart left by one; right-shift restores it.
    for (var i = totalNodes; i > 0; i--) {
      inAdjacencyStart[i] = inAdjacencyStart[i - 1];
    }
    inAdjacencyStart[0] = 0;

    return new BiAdjacentGraph(outAdjacencyStart, outAdjacency, inAdjacencyStart, inAdjacency);
  }

  /**
   * Returns a {@link Graph} with only out-adjacency, dropping any in-adjacency data.
   * If already uni-adjacent, returns {@code this}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return a graph with out-adjacency only
   */
  public Graph uniAdjacentGraph() {
    return this;
  }

  /**
   * Returns the transposed graph (all edge directions reversed).
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount)<br>
   * <strong>Memory:</strong> 4 × (nodeCount + edgeCount) bytes.
   *
   * @return a graph with all edge directions reversed
   */
  public Graph transposed() {
    return biAdjacentGraph().transposed();
  }

  /**
   * Two graphs are equal if they have the same nodes and edges.
   * The concrete type ({@link Graph} vs {@link BiAdjacentGraph}) is irrelevant -
   * a bi-adjacent graph equals its uni-adjacent counterpart.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   */
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Graph other)) return false;
    return Arrays.equals(outAdjacencyStart, other.outAdjacencyStart)
        && Arrays.equals(outAdjacency, other.outAdjacency);
  }

  @Override
  public final int hashCode() {
    return 31 * Arrays.hashCode(outAdjacencyStart) + Arrays.hashCode(outAdjacency);
  }
}
