package com.kobayami.graphity;

import java.util.Objects;

/**
 * A read-only directed graph with both in- and out-adjacency, enabling constant-time access
 * to incoming and outgoing nodes.
 * <p>
 * Extends {@link Graph} with in-adjacency lists in the same flat-array layout,
 * each node's in-nodes sorted ascending.
 * Obtain via {@link #of(Graph)}; convert back via {@link #uniAdjacentGraph()}.
 * <p>
 * The analyses that walk predecessors take a {@code BiAdjacentGraph}, so that the in-adjacency is
 * built once and shared: {@link Dominators}, {@link Loops},
 * {@link Components#biconnectivityOf(BiAdjacentGraph)} and
 * {@link Components#strongConnectivityOf(BiAdjacentGraph)}.
 * <p>
 * <strong>Memory:</strong> 8 x (nodeCount + edgeCount) bytes (2x {@link Graph}).
 *
 * @see Graph
 * @see Graphs#rootNodes(BiAdjacentGraph)
 * @see Dominators
 * @see Loops
 */
public final class BiAdjacentGraph extends Graph {

  /*
   * Per-node start offset into inAdjacency. Same layout as the out-adjacency offsets but for
   * incoming edges, including the sentinel at index nodeCount.
   */
  private final int[] inAdjacencyStart;

  /* Flat array of all in-adjacency lists, indexed via inAdjacencyStart. */
  private final int[] inAdjacency;

  // Package-private. Arrays are shared, not copied.
  BiAdjacentGraph(int[] outAdjacencyStart, int[] outAdjacency, int[] inAdjacencyStart, int[] inAdjacency) {
    super(outAdjacencyStart, outAdjacency);
    this.inAdjacencyStart = inAdjacencyStart;
    this.inAdjacency = inAdjacency;
  }

  /** The in-adjacency offset array, for {@link Csr}. */
  final int[] inAdjacencyStart() {
    return inAdjacencyStart;
  }

  /** The in-adjacency source array, for {@link Csr}. */
  final int[] inAdjacency() {
    return inAdjacency;
  }

  /**
   * Returns the given graph with in-adjacency added, for constant-time access to the
   * predecessors of every node. The result shares the out-adjacency data; only the in-adjacency
   * is newly computed, and it is not cached - hold on to the result to use it more than once.
   * <p>
   * If {@code graph} already is a {@code BiAdjacentGraph}, returns it unchanged in O(1).
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), or O(1) if already bi-adjacent.<br>
   * <strong>Memory:</strong> +4 x (nodeCount + edgeCount) bytes, or none if already bi-adjacent.
   *
   * @param graph the graph to add the in-adjacency to
   * @return a bi-adjacent graph with the same nodes and edges
   */
  public static BiAdjacentGraph of(Graph graph) {
    return graph instanceof BiAdjacentGraph biAdjacent ? biAdjacent : graph.withInAdjacency();
  }

  /**
   * Returns the in-degree (number of incoming edges) of {@code node}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return in-degree
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int inDegree(int node) {
    Objects.checkIndex(node, nodeCount());
    return inAdjacencyEnd(node) - inAdjacencyStart[node];
  }

  /**
   * Returns the total degree (inDegree + outDegree) of {@code node}.
   * <p>
   * A self-loop counts twice: once as an out-edge, once as an in-edge.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return total degree (in + out)
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  @Override
  public int degree(int node) {
    return inDegree(node) + outDegree(node);
  }

  /**
   * Returns a zero-copy view of {@code node}'s in-nodes, sorted ascending.
   * In-nodes are the predecessors of {@code node} (sources of its incoming edges).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return in-nodes as a read-only sorted view
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final SortedIntView inNodes(int node) {
    Objects.checkIndex(node, nodeCount());
    return new SortedIntView(inAdjacency, inAdjacencyStart[node], inAdjacencyEnd(node));
  }

  private int inAdjacencyEnd(int node) {
    return inAdjacencyStart[node + 1];
  }

  /**
   * Tests whether the edge {@code inNode → node} exists.
   * <p>
   * <strong>Time:</strong> O(log inDegree) via binary search.
   *
   * @param node   node ID in {@code 0..nodeCount()-1}
   * @param inNode potential in-node ID
   * @return {@code true} if the edge exists
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean hasInNode(int node, int inNode) {
    Objects.checkIndex(node, nodeCount());
    return SortedIntView.containsInRange(inAdjacency, inAdjacencyStart[node], inAdjacencyEnd(node), inNode);
  }

  /**
   * Returns a {@link Graph} view with only out-adjacency, sharing the underlying arrays.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return a uni-adjacent graph over this graph's out-adjacency data
   */
  @Override
  public final Graph uniAdjacentGraph() {
    return new Graph(outAdjacencyStart(), outAdjacency());
  }

  /**
   * Returns a zero-copy view with in- and out-adjacency swapped (graph transpose).
   * <p>
   * Node IDs are preserved, <strong>edge indices are not</strong>: the edge at index {@code i}
   * of the transpose is a different edge from the one at index {@code i} here. To address
   * external edge metadata across the transpose, translate the index with
   * {@link #transposedEdgeIndex(int)} or {@link Graphs#transposedEdgeMapping(Graph)}.
   * <p>
   * A plain {@link Graph} has no transpose of its own: building one costs the in-adjacency, so
   * a caller writes {@code BiAdjacentGraph.of(graph).transposed()}, which shows that cost and
   * keeps the {@code BiAdjacentGraph} for further use.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return a bi-adjacent view with all edge directions reversed
   * @see #transposedEdgeIndex(int)
   * @see Graphs#transposedEdgeMapping(Graph)
   */
  public final BiAdjacentGraph transposed() {
    return new BiAdjacentGraph(inAdjacencyStart, inAdjacency, outAdjacencyStart(), outAdjacency());
  }

  /**
   * Returns the index of the same edge in {@link #transposed()}.
   * <p>
   * The edge at {@code edgeIndex} runs {@code source → target}; in the transpose the same
   * edge runs {@code target → source} and sits at the returned index. This is the inverse
   * direction of {@link Graphs#transposedEdgeMapping(Graph)} and its own inverse across the
   * two graphs:
   * <pre>{@code
   * BiAdjacentGraph transpose = graph.transposed();
   * int back = transpose.transposedEdgeIndex(graph.transposedEdgeIndex(edgeIndex));
   * assert back == edgeIndex;
   * }</pre>
   * The result is never {@code -1} - the edge being translated exists by definition. Use
   * {@link Graphs#transposedEdgeMapping(Graph)} instead when translating every edge, which
   * costs one linear pass rather than a binary search per edge.
   * <p>
   * <strong>Time:</strong> O(log nodeCount + log inDegree) via binary search.
   *
   * @param edgeIndex edge index in {@code 0..edgeCount()-1}
   * @return the index of the same edge in {@link #transposed()}
   * @throws IndexOutOfBoundsException if {@code edgeIndex} is out of range
   * @see #transposed()
   * @see Graphs#transposedEdgeMapping(Graph)
   */
  public final int transposedEdgeIndex(int edgeIndex) {
    Objects.checkIndex(edgeIndex, edgeCount());
    int source = edgeSource(edgeIndex);
    int target = outAdjacency()[edgeIndex];
    return SortedIntView.indexOfInRange(
        inAdjacency, inAdjacencyStart[target], inAdjacencyEnd(target), source);
  }

  /**
   * Returns a merged, duplicate-free view of {@code node}'s in- and out-nodes (neighbors),
   * sorted ascending.
   * <p>
   * <strong>Time:</strong> O(inDegree + outDegree).<br>
   * <strong>Memory:</strong> allocates a merged array.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return neighbors as a sorted, duplicate-free view
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final SortedIntView neighbors(int node) {
    Objects.checkIndex(node, nodeCount());
    return new SortedIntView(
      SortedIntView.union(
        outAdjacency(),
        outAdjacencyStart()[node],
        outAdjacencyEnd(node),
        inAdjacency,
        inAdjacencyStart[node],
        inAdjacencyEnd(node)
      )
    );
  }

  /**
   * Tests whether {@code neighbor} is connected to {@code node} via any edge direction.
   * <p>
   * <strong>Time:</strong> O(log outDegree + log inDegree) via binary search.
   *
   * @param node     node ID in {@code 0..nodeCount()-1}
   * @param neighbor potential neighbor ID
   * @return {@code true} if {@code neighbor} is an in- or out-node of {@code node}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean hasNeighbor(int node, int neighbor) {
    return hasOutNode(node, neighbor) || hasInNode(node, neighbor);
  }

  /**
   * Tests whether {@code node} is a root node (in-degree 0, no incoming edges).
   * A root node is a natural entry point for traversal; this is distinct from an edge's
   * source node (see {@link #edgeSource(int)}).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if {@code node} has in-degree 0
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   * @see Graphs#rootNodes(BiAdjacentGraph)
   */
  public final boolean isRootNode(int node) {
    return inDegree(node) == 0;
  }
}
