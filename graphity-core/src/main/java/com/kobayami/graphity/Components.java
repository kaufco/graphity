package com.kobayami.graphity;

import java.util.Arrays;

/**
 * The algorithms for the components of a graph, and for the edges and nodes whose removal splits
 * them.
 * <p>
 * {@link #sccsOf(Graph)} returns the SCCs (strongly connected components), and
 * {@link #wccsOf(Graph)} the weakly connected components. {@link #ccsOf(Graph)} returns the weakly
 * connected components under another name: the connected components of the graph read as
 * undirected. {@link #biconnectivityOf(BiAdjacentGraph)} returns the bridges, articulation points
 * and biconnected components. Bridges and articulation points are the edges and nodes whose removal
 * splits a connected component. {@link #strongConnectivityOf(BiAdjacentGraph)} returns the strong
 * bridges and strong articulation points, which split an SCC.
 * <p>
 * A graph is always directed, and each method says how it reads the edges. {@code sccsOf} and
 * {@code strongConnectivityOf} follow them in their direction. {@code wccsOf}, {@code ccsOf} and
 * {@code biconnectivityOf} read the graph as undirected: an edge connects its source and target
 * whether or not the reverse edge exists, and the edges {@code a -> b} and {@code b -> a} count
 * as one undirected edge, not two. An undirected graph can therefore be stored with one edge per
 * pair of neighbors or with both directions, and these methods read it the same way. For the
 * bridges this matters: read as two edges, the two directions would form a cycle, and no edge
 * stored in both directions could be a bridge.
 */
public final class Components {

  private Components() {}

  /**
   * Returns the SCCs of {@code graph}. Uses
   * <a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Tarjan's
   * algorithm</a>, without recursion.
   * <p>
   * Every node is in exactly one SCC; a node that lies on no cycle is an SCC of its own. SCC IDs
   * are in reverse topological order: every edge between two SCCs goes from the higher SCC ID to
   * the lower one, so SCC {@code 0} has no edges to other SCCs. The nodes of each SCC are in
   * ascending order. Here, {@code dependencies} are the dependencies between the packages of a
   * program, and {@code names} holds the names of the packages. The example groups the packages by
   * SCC, in the order of the SCC IDs. In this order, each package comes after the packages it uses,
   * and the packages that use each other come together:
   * {@snippet file="com/kobayami/graphity/examples/Connectivity.java" region="sccsOf"}
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), plus sorting the nodes of each SCC: up to
   * O(nodeCount log nodeCount) when one SCC holds most of the nodes.<br>
   * <strong>Memory:</strong> up to 12 x nodeCount bytes for the result; while it runs,
   * 24 x nodeCount bytes more.
   *
   * @param graph the graph
   * @return the SCCs
   * @throws IllegalArgumentException if {@code graph} has more than 2<sup>30</sup> - 5 nodes
   */
  public static Sccs sccsOf(Graph graph) {
    return Sccs.of(graph);
  }

  /**
   * Returns the weakly connected components of {@code graph}. These are the components of the
   * graph read as undirected.
   * <p>
   * Every node is in exactly one component. Each component is a group of the result. The groups are
   * in the order of their smallest nodes: group {@code 0} holds node {@code 0}, group {@code 1} the
   * smallest node not in group {@code 0}, and so on. Uses <a
   * href="https://en.wikipedia.org/wiki/Disjoint-set_data_structure">union-find</a>, whose cost
   * grows with alpha(nodeCount). The inverse Ackermann function alpha is below 5 for any graph.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) x alpha(nodeCount)).<br>
   * <strong>Memory:</strong> up to 12 x nodeCount bytes for the result; while it runs,
   * 9 x nodeCount bytes more.
   *
   * @param graph the graph
   * @return the weakly connected components
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
   * Returns the connected components of {@code graph} read as undirected. Equivalent to
   * {@link #wccsOf(Graph) wccsOf(graph)}.
   * <p>
   * The two names let a call say which reading of the graph it means.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) x alpha(nodeCount)), as for
   * {@link #wccsOf(Graph)}.<br>
   * <strong>Memory:</strong> up to 12 x nodeCount bytes for the result; while it runs,
   * 9 x nodeCount bytes more.
   *
   * @param graph the graph
   * @return the connected components
   */
  public static NodePartition ccsOf(Graph graph) {
    return wccsOf(graph);
  }

  /**
   * Returns the bridges, articulation points and biconnected components of {@code graph}, read
   * as undirected.
   * <p>
   * All three come from one depth-first search, so there is no cheaper method for one of them
   * alone; {@link Biconnectivity} describes them. The search follows the edges of every node in
   * both directions, so it takes a {@link BiAdjacentGraph}. For a plain {@link Graph}, call
   * {@code Components.biconnectivityOf(BiAdjacentGraph.of(graph))}.
   * Uses <a href="https://en.wikipedia.org/wiki/Biconnected_component">the algorithm of Hopcroft
   * and Tarjan</a>, without recursion.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> up to 8 x edgeCount + 20 x nodeCount bytes for the result; while it
   * runs, 8 x edgeCount + 63 x nodeCount bytes more.
   *
   * @param graph the graph
   * @return the bridges, articulation points and biconnected components
   */
  public static Biconnectivity biconnectivityOf(BiAdjacentGraph graph) {
    return Biconnectivity.of(graph);
  }

  /**
   * Returns the strong bridges and strong articulation points of {@code graph}. These are the
   * edges and nodes whose removal splits an SCC.
   * <p>
   * The edges are followed in their direction, as by {@link #sccsOf(Graph)};
   * {@link StrongConnectivity} describes the results. The computation also follows them against
   * their direction, so it takes a {@link BiAdjacentGraph}. For a plain {@link Graph}, call
   * {@code Components.strongConnectivityOf(BiAdjacentGraph.of(graph))}. Uses the algorithm of
   * Italiano, Laura and Santaroni, which finds both from the
   * {@linkplain Dominators dominators} within each SCC, without recursion.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) log nodeCount).<br>
   * <strong>Memory:</strong> up to 28 x nodeCount bytes for the result; while it runs, about
   * 80 x nodeCount bytes more.
   *
   * @param graph the graph
   * @return the strong bridges and strong articulation points
   * @throws IllegalArgumentException if {@code graph} has more than 2<sup>30</sup> - 6 nodes
   */
  public static StrongConnectivity strongConnectivityOf(BiAdjacentGraph graph) {
    return StrongConnectivity.of(graph);
  }
}
