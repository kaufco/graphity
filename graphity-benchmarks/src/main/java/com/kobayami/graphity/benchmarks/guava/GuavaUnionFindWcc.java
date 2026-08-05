package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Direct port of Graphity's weakly-connected-component algorithm
 * ({@code com.kobayami.graphity.Components.wccsOf}) onto Guava's {@link Graph} data model.
 *
 * <p>Guava has no connectivity algorithm for directed graphs, so this is the code a Guava
 * user would write. Algorithmically identical to Graphity: Union-Find with path halving and
 * union by rank over every edge, then a second pass that assigns dense component IDs in
 * ascending node order. The difference lies only in the data structures:
 * <ul>
 *   <li>Graphity: primitive {@code int[]} for parent/rank, CSR adjacency access.</li>
 *   <li>This port: {@link HashMap}&lt;Integer,Integer&gt; for parent/rank, Guava's
 *       {@link Graph#edges()} for the edge scan.</li>
 * </ul>
 */
public final class GuavaUnionFindWcc {

  private final Graph<Integer> graph;
  private final Map<Integer, Integer> parent = new HashMap<>();
  private final Map<Integer, Integer> rank = new HashMap<>();
  private int groupCount;

  public GuavaUnionFindWcc(Graph<Integer> graph) {
    this.graph = graph;
  }

  /** @return a map from node id to component id; caller gets the count via {@link #groupCount()}. */
  public Map<Integer, Integer> compute() {
    for (Integer v : graph.nodes()) {
      parent.put(v, v);
      rank.put(v, 0);
    }
    for (EndpointPair<Integer> edge : graph.edges()) {
      union(edge.source(), edge.target());
    }

    Map<Integer, Integer> componentOfRoot = new HashMap<>();
    Map<Integer, Integer> nodeToGroup = new HashMap<>();
    for (Integer v : graph.nodes()) {
      Integer root = find(v);
      Integer component = componentOfRoot.get(root);
      if (component == null) {
        component = groupCount++;
        componentOfRoot.put(root, component);
      }
      nodeToGroup.put(v, component);
    }
    return nodeToGroup;
  }

  public int groupCount() {
    return groupCount;
  }

  private Integer find(Integer node) {
    Integer current = node;
    Integer up = parent.get(current);
    while (!current.equals(up)) {
      Integer grandparent = parent.get(up);
      parent.put(current, grandparent); // path halving, as in the Graphity reference
      current = grandparent;
      up = parent.get(current);
    }
    return current;
  }

  private void union(Integer a, Integer b) {
    Integer rootA = find(a);
    Integer rootB = find(b);
    if (rootA.equals(rootB)) return;
    int rankA = rank.get(rootA);
    int rankB = rank.get(rootB);
    if (rankA < rankB) {
      parent.put(rootA, rootB);
    } else if (rankA > rankB) {
      parent.put(rootB, rootA);
    } else {
      parent.put(rootB, rootA);
      rank.put(rootA, rankA + 1);
    }
  }
}
