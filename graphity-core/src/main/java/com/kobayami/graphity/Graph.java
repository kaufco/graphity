package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

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

  /** Most nodes {@link #toString()} lists. */
  private static final int TO_STRING_NODES = 16;

  /** Most out-nodes {@link #toString()} lists, over all listed nodes together. */
  private static final int TO_STRING_TARGETS = 64;

  /**
   * Initial capacity of the {@link #toString()} builder. The longest listing the two limits above
   * allow stays under 1000 characters, even with ten-digit counts and IDs, so the builder never
   * grows.
   */
  private static final int TO_STRING_CAPACITY = 1024;

  /*
   * Per-node start offset into outAdjacency. Node i's out-nodes occupy
   * outAdjacency[outAdjacencyStart[i]..outAdjacencyStart[i+1]-1], sorted ascending. Length
   * nodeCount + 1; the sentinel outAdjacencyStart[nodeCount] == edgeCount keeps
   * outAdjacencyStart[i+1] always valid (branchless end lookup).
   */
  private final int[] outAdjacencyStart;

  /* Flat array of all out-adjacency lists, indexed via outAdjacencyStart. */
  private final int[] outAdjacency;

  // Takes the arrays as they are: neither copied nor checked.
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
   *                                  {@code outAdjacency.length}, an adjacency list is not
   *                                  strictly ascending, or the arrays describe more than
   *                                  {@code Integer.MAX_VALUE - 9} nodes or
   *                                  {@code Integer.MAX_VALUE - 8} edges, the limits that
   *                                  {@link GraphFormat} also reads back
   */
  public static Graph of(int[] outAdjacencyStart, int[] outAdjacency) {
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
    if (n > ArrayLimits.MAX_NODE_COUNT || m > ArrayLimits.MAX_ARRAY_LENGTH) {
      throw new IllegalArgumentException("a graph holds at most " + ArrayLimits.MAX_NODE_COUNT
          + " nodes and " + ArrayLimits.MAX_ARRAY_LENGTH + " edges, but these arrays hold " + n
          + " and " + m);
    }
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
    return outAdjacencyStart[node + 1] - outAdjacencyStart[node];
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
    return new SortedIntView(outAdjacency, outAdjacencyStart[node], outAdjacencyStart[node + 1]);
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
   * Returns the node IDs {@code 0..nodeCount()-1} as a stream, in ascending order.
   * <p>
   * <strong>Time:</strong> O(1) to create; the stream yields nodeCount values.
   *
   * @return the node IDs
   */
  public final IntStream nodes() {
    return IntStream.range(0, nodeCount());
  }

  /**
   * Calls {@code action} once per edge with its source, target and edge index, in edge index
   * order - that is, by source and then target. On a {@link BiAdjacentGraph} these are its
   * edges, each once, as stored in the out-adjacency.
   * <p>
   * Walks the out-adjacency arrays, so the source of each edge costs nothing, unlike
   * {@link #edgeSource(int)} per edge.
   *
   * <pre>{@code
   * graph.forEachEdge((source, target, edgeIndex) ->
   *     System.out.println(names[source] + " -> " + names[target] + ": " + weights[edgeIndex]));
   * }</pre>
   *
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   *
   * @param action called with {@code (source, target, edgeIndex)} for every edge
   */
  public final void forEachEdge(EdgeConsumer action) {
    int n = nodeCount();
    for (int source = 0; source < n; source++) {
      for (int e = outAdjacencyStart[source], end = outAdjacencyStart[source + 1]; e < end; e++) {
        action.accept(source, outAdjacency[e], e);
      }
    }
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
  public final int edgeSource(int edgeIndex) {
    Objects.checkIndex(edgeIndex, edgeCount());
    // The largest node with outAdjacencyStart[node] <= edgeIndex: a node without out-edges shares
    // its offset with the next one.
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
    return SortedIntView.containsInRange(
        outAdjacency, outAdjacencyStart[node], outAdjacencyStart[node + 1], outNode);
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
    int n = nodeCount();
    int m = edgeCount();
    int[] inAdjacencyStart = new int[n + 1];
    int[] inAdjacency = new int[m];

    // 1) Count in-degrees
    for (int e = 0; e < m; e++) {
      inAdjacencyStart[outAdjacency[e]]++;
    }

    // 2) Prefix-sum: convert in-degrees to start offsets
    int nextStart = 0;
    for (int v = 0; v < n; v++) {
      int inDegree = inAdjacencyStart[v];
      inAdjacencyStart[v] = nextStart;
      nextStart += inDegree;
    }

    // 3) Fill in-adjacency. inAdjacencyStart[target]++ doubles as write pointer.
    //    Ascending node scan ensures each in-list ends up sorted.
    for (int v = 0; v < n; v++) {
      for (int e = outAdjacencyStart[v], end = outAdjacencyStart[v + 1]; e < end; e++) {
        inAdjacency[inAdjacencyStart[outAdjacency[e]]++] = v;
      }
    }

    // 4) Repair: step 3 shifted inAdjacencyStart left by one; right-shift restores it, the
    //    sentinel included.
    for (int v = n; v > 0; v--) {
      inAdjacencyStart[v] = inAdjacencyStart[v - 1];
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
  public final boolean equals(@Nullable Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Graph other)) return false;
    return Arrays.equals(outAdjacencyStart, other.outAdjacencyStart)
        && Arrays.equals(outAdjacency, other.outAdjacency);
  }

  /**
   * Hash code consistent with {@link #equals(Object)}, computed over nodes and edges.
   * <p>
   * Computed on every call, so every lookup of a graph in a hash-based collection walks the whole
   * graph. To key a cache by structure, compute {@link Graphs#topologyHash(Graph)} once and keep
   * it.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   */
  @Override
  public final int hashCode() {
    return 31 * Arrays.hashCode(outAdjacencyStart) + Arrays.hashCode(outAdjacency);
  }

  /**
   * Returns the node and edge counts and the out-nodes of the first nodes, bounded regardless of
   * the graph's size, for debuggers and logs:
   * <pre>
   * Graph[nodes=5, edges=4] {0 -&gt; [1, 2], 1 -&gt; [3], 2 -&gt; [], 3 -&gt; [4], 4 -&gt; []}
   * </pre>
   * At most 16 nodes and 64 out-nodes in total are listed; {@code ...} marks where a list or the
   * listing is cut - after node 15 in a larger graph, inside the list that reaches the 64th
   * out-node, as in {@code {0 -> [1, 2, 3, ..., 64, ...], ...}} for a hub. A
   * {@link BiAdjacentGraph} is named as such and lists its out-adjacency. For the complete
   * listing, use {@link Graphs#adjacencyListOf(Graph)}.
   * <p>
   * <strong>Time:</strong> O(1) - bounded by the limits above.
   *
   * @return a bounded description of this graph
   */
  @Override
  public final String toString() {
    int n = nodeCount();
    var sb = new StringBuilder(TO_STRING_CAPACITY);
    sb.append(getClass().getSimpleName()).append("[nodes=").append(n)
        .append(", edges=").append(edgeCount()).append("] {");
    int budget = TO_STRING_TARGETS;
    int listed = Math.min(n, TO_STRING_NODES);
    for (int v = 0; v < listed; v++) {
      if (v > 0) sb.append(", ");
      sb.append(v).append(" -> [");
      int start = outAdjacencyStart[v];
      int end = outAdjacencyStart[v + 1];
      int shown = Math.min(end - start, budget);
      for (int e = start; e < start + shown; e++) {
        if (e > start) sb.append(", ");
        sb.append(outAdjacency[e]);
      }
      budget -= shown;
      if (shown < end - start) {
        sb.append(shown > 0 ? ", ...]" : "...]");
        if (v + 1 < n) sb.append(", ...");
        return sb.append('}').toString();
      }
      sb.append(']');
    }
    if (listed < n) sb.append(", ...");
    return sb.append('}').toString();
  }
}
