package com.kobayami.graphity;

import java.util.Objects;

/**
 * A {@link Graph} that also holds the in-nodes of every node, for algorithms that follow edges
 * against their direction.
 * <p>
 * {@link #of(Graph)} builds the in-adjacency of a graph - the in-nodes of every node. The result
 * has the node IDs and edge indices of the graph and shares the out-nodes of every node, called
 * its out-adjacency. Algorithms that need in-nodes, such as {@link Dominators}, take a
 * {@code BiAdjacentGraph}, so that the in-adjacency is built once and serves all of them.
 * {@link #transposed()} reverses every edge without copying, and {@link #uniAdjacentGraph()} drops
 * the in-adjacency again.
 * <p>
 * The in-nodes of a node are in ascending order, like its out-nodes. A {@code BiAdjacentGraph}
 * equals a {@code Graph} with the same nodes and edges. Instances are immutable and safe to share
 * between threads.
 * <p>
 * <strong>Memory:</strong> 8 x (nodeCount + edgeCount) bytes, half of it for the out-adjacency,
 * which is shared with the graph it was built from.
 */
public final class BiAdjacentGraph extends Graph {

  // The in-adjacency in the layout of the out-adjacency: the in-nodes of node v are
  // inAdjacency[inAdjacencyStart[v]..inAdjacencyStart[v + 1] - 1], sorted ascending, with the
  // sentinel inAdjacencyStart[nodeCount] == edgeCount. A position in inAdjacency is not an edge
  // index.
  private final int[] inAdjacencyStart;
  private final int[] inAdjacency;

  // Takes the arrays as they are, neither copied nor checked: the caller guarantees the layout.
  BiAdjacentGraph(
      int[] outAdjacencyStart, int[] outAdjacency, int[] inAdjacencyStart, int[] inAdjacency) {
    super(outAdjacencyStart, outAdjacency);
    this.inAdjacencyStart = inAdjacencyStart;
    this.inAdjacency = inAdjacency;
  }

  // The body of of(Graph) for a graph without in-adjacency.
  private static BiAdjacentGraph withInAdjacency(Graph graph) {
    int[] outAdjacencyStart = graph.outAdjacencyStart();
    int[] outAdjacency = graph.outAdjacency();
    int n = graph.nodeCount();
    int m = graph.edgeCount();
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

  // For Csr, which hands the arrays to the algorithms of the core and of other modules.
  final int[] inAdjacencyStart() {
    return inAdjacencyStart;
  }

  final int[] inAdjacency() {
    return inAdjacency;
  }

  /**
   * Returns a {@code BiAdjacentGraph} with the nodes and edges of {@code graph}, building its
   * in-adjacency.
   * <p>
   * Node IDs and edge indices stay the same, and the out-adjacency is shared with {@code graph},
   * not copied. The in-adjacency is built anew on every call, not cached: keep the result to use
   * it more than once. If {@code graph} already is a {@code BiAdjacentGraph}, returns it.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), or O(1) if already bi-adjacent.<br>
   * <strong>Memory:</strong> +4 x (nodeCount + edgeCount) bytes, or none if already bi-adjacent.
   *
   * @param graph the graph to build the in-adjacency for
   * @return the graph with in-adjacency
   */
  public static BiAdjacentGraph of(Graph graph) {
    return graph instanceof BiAdjacentGraph biAdjacent ? biAdjacent : withInAdjacency(graph);
  }

  /**
   * Returns the in-degree of {@code node}. That is the number of edges with {@code node} as target.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return in-degree
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int inDegree(int node) {
    Objects.checkIndex(node, nodeCount());
    return inAdjacencyStart[node + 1] - inAdjacencyStart[node];
  }

  /**
   * Returns the in-nodes of {@code node} in ascending order. These are the sources of the edges
   * with {@code node} as target.
   * <p>
   * The result is a view of this graph's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return in-nodes
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final SortedIntView inNodes(int node) {
    Objects.checkIndex(node, nodeCount());
    return new SortedIntView(inAdjacency, inAdjacencyStart[node], inAdjacencyStart[node + 1]);
  }

  /**
   * Returns whether the edge {@code inNode -> node} exists.
   * <p>
   * <strong>Time:</strong> O(log inDegree(node)).
   *
   * @param node   node ID in {@code 0..nodeCount()-1}
   * @param inNode node ID; a value out of range yields {@code false}
   * @return {@code true} if the edge exists
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean hasInNode(int node, int inNode) {
    Objects.checkIndex(node, nodeCount());
    return SortedIntView.containsInRange(
        inAdjacency, inAdjacencyStart[node], inAdjacencyStart[node + 1], inNode);
  }

  /**
   * Returns a {@code Graph} with the same nodes and edges, without the in-adjacency.
   * <p>
   * The result shares the out-adjacency of this graph; nothing is copied. Once only the result is
   * kept, the in-adjacency can be garbage-collected.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return a graph over the out-adjacency of this graph
   */
  @Override
  public final Graph uniAdjacentGraph() {
    return new Graph(outAdjacencyStart(), outAdjacency());
  }

  /**
   * Returns the transposed graph as a view of this graph's arrays. The transposed graph has every
   * edge reversed.
   * <p>
   * Node IDs stay the same, edge indices do not: edge {@code e} of the transposed graph is in
   * general not the reverse of edge {@code e} of this graph. {@link #transposedEdgeIndex(int)}
   * translates one edge index, and {@link Graphs#transposedEdgeMapping(Graph)} translates all of
   * them.
   * <p>
   * A plain {@link Graph} needs its in-adjacency first:
   * {@code BiAdjacentGraph.of(graph).transposed()}. Here, the packages of a program that depend on
   * {@code model}, directly or indirectly, are the ones reachable from it against the direction of
   * the dependencies:
   * {@snippet file="com/kobayami/graphity/examples/Transposing.java" region="transposed"}
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the transposed graph
   */
  public final BiAdjacentGraph transposed() {
    return new BiAdjacentGraph(inAdjacencyStart, inAdjacency, outAdjacencyStart(), outAdjacency());
  }

  /**
   * Returns the edge index that edge {@code edgeIndex} has in the transposed graph.
   * <p>
   * Called on {@link #transposed()}, it translates back:
   * {@code transposed().transposedEdgeIndex(transposedEdgeIndex(e)) == e}. To translate every
   * edge, {@link Graphs#transposedEdgeMapping(Graph)} needs one pass instead of a search per edge;
   * it maps from the transposed graph to this one.
   * <p>
   * <strong>Time:</strong> O(log nodeCount).
   *
   * @param edgeIndex edge index in {@code 0..edgeCount()-1}
   * @return edge index in the transposed graph
   * @throws IndexOutOfBoundsException if {@code edgeIndex} is out of range
   */
  public final int transposedEdgeIndex(int edgeIndex) {
    Objects.checkIndex(edgeIndex, edgeCount());
    int source = edgeSource(edgeIndex);
    int target = outAdjacency()[edgeIndex];
    return SortedIntView.indexOfInRange(
        inAdjacency, inAdjacencyStart[target], inAdjacencyStart[target + 1], source);
  }

  /**
   * Returns the degree of {@code node}. Equivalent to {@code inDegree(node) + outDegree(node)}.
   * <p>
   * It counts edges, not neighbors: a self-loop counts twice, and so do the edges
   * {@code node -> v} and {@code v -> node}, although {@code v} is one neighbor.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return degree
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int degree(int node) {
    return inDegree(node) + outDegree(node);
  }

  /**
   * Returns the neighbors of {@code node} in ascending order and without duplicates. These are its
   * in-nodes and out-nodes.
   * <p>
   * Unlike {@link #inNodes(int)} and {@link #outNodes(int)}, which are views, the result is a copy,
   * merged on every call.
   * <p>
   * <strong>Time:</strong> O(inDegree(node) + outDegree(node)).<br>
   * <strong>Memory:</strong> up to 4 x (inDegree(node) + outDegree(node)) bytes for the result;
   * while it runs, up to as much again if a neighbor is both an in-node and an out-node.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return neighbors
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final SortedIntView neighbors(int node) {
    Objects.checkIndex(node, nodeCount());
    int[] outAdjacencyStart = outAdjacencyStart();
    return new SortedIntView(SortedIntView.unionInRange(
        outAdjacency(), outAdjacencyStart[node], outAdjacencyStart[node + 1],
        inAdjacency, inAdjacencyStart[node], inAdjacencyStart[node + 1]));
  }

  /**
   * Returns whether {@code neighbor} is a neighbor of {@code node}. Equivalent to
   * {@code hasOutNode(node, neighbor) || hasInNode(node, neighbor)}.
   * <p>
   * <strong>Time:</strong> O(log outDegree(node) + log inDegree(node)).
   *
   * @param node     node ID in {@code 0..nodeCount()-1}
   * @param neighbor node ID; a value out of range yields {@code false}
   * @return {@code true} if {@code neighbor} is an in-node or an out-node of {@code node}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean hasNeighbor(int node, int neighbor) {
    return hasOutNode(node, neighbor) || hasInNode(node, neighbor);
  }

  /**
   * Returns whether {@code node} is a root node. Equivalent to {@code inDegree(node) == 0}.
   * <p>
   * {@link Graphs#rootNodes(BiAdjacentGraph)} returns all root nodes.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if no edge has {@code node} as target
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean isRootNode(int node) {
    return inDegree(node) == 0;
  }
}
