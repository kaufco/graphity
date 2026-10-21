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
 * Build a graph with {@link GraphBuilder}, or with {@link #of(int[], int[])} from arrays that
 * are already in CSR form; {@link GraphFormat} stores and loads it.
 * <p>
 * Use {@code Graph} for algorithms that only need out-edges.
 * For in-edge access, obtain a {@link BiAdjacentGraph} via {@link BiAdjacentGraph#of(Graph)};
 * its {@link BiAdjacentGraph#transposed()} is the transposed graph as a constant-time view.
 * <p>
 * Algorithms are not methods on the graph: they take it as input and live on their own classes,
 * {@link Components}, {@link TopOrder}, {@link EdgeTypes}, {@link BfsDistances}, {@link Ancestry}
 * and {@link Dominators} among them. The
 * {@linkplain com.kobayami.graphity package documentation} lists all of them by question.
 * <p>
 * <strong>Memory:</strong> 4 x (nodeCount + edgeCount) bytes.
 *
 * @see BiAdjacentGraph
 * @see BiAdjacentGraph#transposed()
 * @see Graphs#rootNodes(BiAdjacentGraph)
 * @see GraphBuilder
 * @see GraphFormat
 * @see Components
 */
public sealed class Graph permits BiAdjacentGraph {

  /*
   * Per-node start offset into outAdjacency. Node i's out-nodes occupy
   * outAdjacency[outAdjacencyStart[i]..outAdjacencyStart[i+1]-1], sorted ascending. Length
   * nodeCount + 1; the sentinel outAdjacencyStart[nodeCount] == edgeCount keeps
   * outAdjacencyStart[i+1] always valid (branchless end lookup).
   */
  private final int[] outAdjacencyStart;

  /* Flat array of all out-adjacency lists, indexed via outAdjacencyStart. */
  private final int[] outAdjacency;

  // Package-private. Arrays are shared, not copied.
  Graph(int[] outAdjacencyStart, int[] outAdjacency) {
    this.outAdjacencyStart = outAdjacencyStart;
    this.outAdjacency = outAdjacency;
  }

  /**
   * Returns a graph over arrays that are already in CSR form, as {@link Csr} returns them or
   * {@link GraphFormat} stores them.
   * <p>
   * The out-nodes of node {@code v} are
   * {@code outAdjacency[outAdjacencyStart[v]..outAdjacencyStart[v + 1] - 1]}; the node count is
   * {@code outAdjacencyStart.length - 1}. Both arrays are copied, then checked: the offsets start
   * at {@code 0}, never decrease and end at {@code outAdjacency.length}, every target is a node,
   * and each adjacency list is sorted ascending without duplicates. For edges in any order, with
   * duplicates, use {@link GraphBuilder}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + edgeCount) bytes for the copies, which the
   * result keeps.
   *
   * @param outAdjacencyStart the offset of each node's out-nodes, length {@code nodeCount + 1}
   * @param outAdjacency      the out-nodes of all nodes back to back, length {@code edgeCount}
   * @return a graph with these edges; edge {@code e} is the one at position {@code e} of
   *         {@code outAdjacency}
   * @throws IndexOutOfBoundsException if a target is not in {@code 0..nodeCount-1}
   * @throws IllegalArgumentException if {@code outAdjacencyStart} is empty, does not start at
   *                                  {@code 0}, decreases or does not end at
   *                                  {@code outAdjacency.length}, or an adjacency list is not
   *                                  strictly ascending
   */
  public static Graph of(int[] outAdjacencyStart, int[] outAdjacency) {
    Objects.requireNonNull(outAdjacencyStart, "outAdjacencyStart");
    Objects.requireNonNull(outAdjacency, "outAdjacency");
    return ofOwned(outAdjacencyStart.clone(), outAdjacency.clone());
  }

  /** {@link #of(int[], int[])} on arrays nobody else holds: checked, not copied. */
  static Graph ofOwned(int[] outAdjacencyStart, int[] outAdjacency) {
    if (outAdjacencyStart.length == 0) {
      throw new IllegalArgumentException(
          "outAdjacencyStart must hold nodeCount + 1 offsets, but is empty");
    }
    int n = outAdjacencyStart.length - 1;
    int m = outAdjacency.length;
    if (outAdjacencyStart[0] != 0) {
      throw new IllegalArgumentException(
          "outAdjacencyStart[0] must be 0, but was " + outAdjacencyStart[0]);
    }
    // Offsets first: once they rise from 0 to m, every list below lies inside outAdjacency.
    for (int v = 0; v < n; v++) {
      if (outAdjacencyStart[v + 1] < outAdjacencyStart[v]) {
        throw new IllegalArgumentException("outAdjacencyStart decreases after node " + v + ": "
            + outAdjacencyStart[v] + " > " + outAdjacencyStart[v + 1]);
      }
    }
    if (outAdjacencyStart[n] != m) {
      throw new IllegalArgumentException("outAdjacencyStart[" + n
          + "] must be outAdjacency.length " + m + ", but was " + outAdjacencyStart[n]);
    }
    for (int v = 0; v < n; v++) {
      int end = outAdjacencyStart[v + 1];
      int previous = -1;
      for (int e = outAdjacencyStart[v]; e < end; e++) {
        int target = outAdjacency[e];
        if (target < 0 || target >= n) {
          throw new IndexOutOfBoundsException("out-node " + target + " of node " + v
              + " at position " + e + " is not in 0.." + (n - 1));
        }
        if (target <= previous) {
          throw new IllegalArgumentException("the out-nodes of node " + v
              + " must be strictly ascending, but " + target + " follows " + previous);
        }
        previous = target;
      }
    }
    return new Graph(outAdjacencyStart, outAdjacency);
  }

  /** The offset array, for {@link Csr}. */
  final int[] outAdjacencyStart() {
    return outAdjacencyStart;
  }

  /** The target array, for {@link Csr}. */
  final int[] outAdjacency() {
    return outAdjacency;
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
   * Out-nodes are the successors of {@code node} (targets of its outgoing edges).
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
   * Returns the edge index of the edge {@code source → target}, or {@code -1} if there is none.
   * <p>
   * {@link GraphBuilder#build()} sorts every adjacency list and merges duplicate edges, so the
   * k-th added edge does not keep index k. This lookup translates an insertion to its final
   * index; insertions that map to the same index are the duplicates that were merged:
   *
   * <pre>{@code
   * int[] edgeOfInsertion = new int[insertions];
   * for (int k = 0; k < insertions; k++) {
   *     edgeOfInsertion[k] = graph.edgeIndexOf(sources[k], targets[k]);
   * }
   * }</pre>
   *
   * <strong>Time:</strong> O(log outDegree(source)) via binary search.
   *
   * @param source source node ID in {@code 0..nodeCount()-1}
   * @param target target node ID; any value, an out-of-range one yields {@code -1}
   * @return edge index in {@code 0..edgeCount()-1}, or {@code -1} if the edge does not exist
   * @throws IndexOutOfBoundsException if {@code source} is out of range
   */
  public final int edgeIndexOf(int source, int target) {
    Objects.checkIndex(source, nodeCount());
    return SortedIntView.indexOfInRange(
        outAdjacency, outAdjacencyStart[source], outAdjacencyStart[source + 1], target);
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
   * Builds the in-adjacency of this graph; the body of {@link BiAdjacentGraph#of(Graph)} for a
   * graph that is not yet bi-adjacent.
   */
  final BiAdjacentGraph withInAdjacency() {
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

  /**
   * Hash code consistent with {@link #equals(Object)}, computed over nodes and edges.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   */
  @Override
  public final int hashCode() {
    return 31 * Arrays.hashCode(outAdjacencyStart) + Arrays.hashCode(outAdjacency);
  }
}
