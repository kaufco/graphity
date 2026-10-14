package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Objects;

/**
 * Breadth-first distances from one or several sources, with a shortest path to every reachable
 * node.
 * <p>
 * The distance of a node is the number of edges on a shortest path from the nearest source;
 * every source has distance 0. Edges are followed in their direction; for distances against the
 * direction, pass the transpose. Unreachable nodes have distance {@code -1}.
 *
 * <pre>{@code
 * var distances = BfsDistances.of(graph, entry);
 * int hops = distances.distanceOf(target);    // -1 if unreachable
 * IntList path = distances.pathTo(target);    // entry, ..., target
 * }</pre>
 *
 * If only the set of reachable nodes is needed, {@link Graphs#reachableFrom(Graph, IntList)} is
 * far cheaper.
 * <p>
 * <strong>Memory:</strong> 8 x nodeCount bytes: the distance and the breadth-first predecessor
 * of each node.
 *
 * @see Graphs#reachableFrom(Graph, IntList)
 * @see FrontierTraverser
 */
public final class BfsDistances {

  /** Distance of each node from the nearest source, {@code -1} if unreachable. */
  private final int[] distance;

  /** Breadth-first predecessor of each node, {@code -1} for a source and when unreachable. */
  private final int[] predecessor;

  private final int maxDistance;

  private BfsDistances(int[] distance, int[] predecessor, int maxDistance) {
    this.distance = distance;
    this.predecessor = predecessor;
    this.maxDistance = maxDistance;
  }

  /**
   * Computes the distances from a single source.
   * <p>
   * Equivalent to {@link #of(Graph, IntList)} with a one-element list.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> as for {@link #of(Graph, IntList)}.
   *
   * @param graph  the graph to search
   * @param source source node ID in {@code 0..nodeCount()-1}
   * @return the distances
   * @throws IndexOutOfBoundsException if {@code source} is out of range
   */
  public static BfsDistances of(Graph graph, int source) {
    return of(graph, IntLists.singleton(source));
  }

  /**
   * Computes the distance of every node to the nearest of the given sources.
   * <p>
   * Duplicate sources are ignored; an empty list leaves every node unreachable. The search is
   * iterative.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes for the result; 4 x nodeCount bytes additional
   * during the search.
   *
   * @param graph   the graph to search
   * @param sources source node IDs, each in {@code 0..nodeCount()-1}
   * @return the distances
   * @throws IndexOutOfBoundsException if a source is out of range
   */
  public static BfsDistances of(Graph graph, IntList sources) {
    Objects.requireNonNull(graph, "graph");
    Objects.requireNonNull(sources, "sources");
    int n = graph.nodeCount();
    int sourceCount = sources.size();
    for (int i = 0; i < sourceCount; i++) {
      Objects.checkIndex(sources.getInt(i), n);
    }
    int[] out = graph.outAdjacencyStart;
    int[] adj = graph.outAdjacency;
    var distance = new int[n];
    var predecessor = new int[n];
    Arrays.fill(distance, -1);
    Arrays.fill(predecessor, -1);
    var queue = new int[n];
    int tail = 0;
    for (int i = 0; i < sourceCount; i++) {
      int s = sources.getInt(i);
      if (distance[s] < 0) {
        distance[s] = 0;
        queue[tail++] = s;
      }
    }
    for (int head = 0; head < tail; head++) {
      int u = queue[head];
      int next = distance[u] + 1;
      for (int e = out[u], end = out[u + 1]; e < end; e++) {
        int v = adj[e];
        if (distance[v] < 0) {
          distance[v] = next;
          predecessor[v] = u;
          queue[tail++] = v;
        }
      }
    }
    int maxDistance = tail == 0 ? -1 : distance[queue[tail - 1]];
    return new BfsDistances(distance, predecessor, maxDistance);
  }

  /**
   * Returns the number of edges on a shortest path from the nearest source to {@code node}, or
   * {@code -1} if no source reaches it.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return the distance, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int distanceOf(int node) {
    Objects.checkIndex(node, distance.length);
    return distance[node];
  }

  /**
   * Returns whether a source reaches {@code node}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if the node is reachable
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public boolean isReachable(int node) {
    Objects.checkIndex(node, distance.length);
    return distance[node] >= 0;
  }

  /**
   * Returns a shortest path from the nearest source to {@code node}: the source first,
   * {@code node} last, {@code distanceOf(node) + 1} nodes in all. Empty if {@code node} is
   * unreachable.
   * <p>
   * Among several shortest paths, the one found first by the breadth-first search is returned.
   * <p>
   * <strong>Time:</strong> O(path length).<br>
   * <strong>Memory:</strong> 4 x (path length) bytes for the result.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return the path as an unmodifiable list, or an empty list
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public IntList pathTo(int node) {
    Objects.checkIndex(node, distance.length);
    int d = distance[node];
    if (d < 0) return IntLists.emptyList();
    var path = new int[d + 1];
    for (int i = d, v = node; i >= 0; i--, v = predecessor[v]) {
      path[i] = v;
    }
    return new IntImmutableList(path);
  }

  /**
   * Returns the largest finite distance, {@code -1} if no node is reachable.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the largest distance of a reachable node, or {@code -1}
   */
  public int maxDistance() {
    return maxDistance;
  }
}
