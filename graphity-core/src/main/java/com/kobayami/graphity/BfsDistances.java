package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Objects;

/**
 * The distances of the nodes of a graph from one or several sources, with a shortest path to
 * every node reached.
 * <p>
 * The sources are the nodes where the search starts. The distance of a node is the number of edges
 * on a shortest path to it from the nearest source. A source has distance 0, and a node that no
 * source reaches has distance {@code -1}. Edges are followed in their direction. For distances
 * against their direction, pass the transposed graph:
 * {@code BiAdjacentGraph.of(graph).transposed()}. Here, {@code calls} are the calls between the
 * methods of a program, and the search finds a shortest chain of calls from {@code main} to
 * {@code parse}:
 * {@snippet file="com/kobayami/graphity/examples/Traversals.java" region="pathTo"}
 * <p>
 * Uses a <a href="https://en.wikipedia.org/wiki/Breadth-first_search">breadth-first search</a>
 * that takes the sources in list order and the out-nodes of each node in ascending order. For the
 * reachable nodes alone, {@link Graphs#reachableFrom(Graph, IntList)} needs less memory.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 8 x nodeCount bytes: the distance of each node and the node before it
 * on its shortest path.
 */
public final class BfsDistances {

  // The distance of each node from the nearest source, -1 if no source reaches it.
  private final int[] distance;

  // The node before each node on its shortest path, -1 for a source and for a node not reached.
  private final int[] predecessor;

  private final int maxDistance;

  private BfsDistances(int[] distance, int[] predecessor, int maxDistance) {
    this.distance = distance;
    this.predecessor = predecessor;
    this.maxDistance = maxDistance;
  }

  /**
   * Returns the distances of the nodes of {@code graph} from {@code source}. Equivalent to
   * {@code of(graph, IntList.of(source))}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> as for {@link #of(Graph, IntList)}.
   *
   * @param graph  the graph to search
   * @param source node ID in {@code 0..nodeCount-1}
   * @return the distances
   * @throws IndexOutOfBoundsException if {@code source} is out of range
   */
  public static BfsDistances of(Graph graph, int source) {
    return of(graph, IntLists.singleton(source));
  }

  /**
   * Returns the distances of the nodes of {@code graph} from the nearest of {@code sources}.
   * <p>
   * A source given twice counts once, and with no sources, no node is reachable.
   * <p>
   * <strong>Time:</strong> O(sources.size() + nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes for the result; while it runs, 4 x nodeCount
   * bytes more.
   *
   * @param graph   the graph to search
   * @param sources node IDs, each in {@code 0..nodeCount-1}
   * @return the distances
   * @throws IndexOutOfBoundsException if a source is out of range
   */
  public static BfsDistances of(Graph graph, IntList sources) {
    int n = graph.nodeCount();
    Checks.checkNodes(sources, n);
    int sourceCount = sources.size();
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);
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
   * Returns the distance of {@code node}, or {@code -1} if no source reaches it. The distance is
   * the number of edges on a shortest path to {@code node} from the nearest source.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return distance, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int distanceOf(int node) {
    Objects.checkIndex(node, distance.length);
    return distance[node];
  }

  /**
   * Returns whether a source reaches {@code node}. Equivalent to {@code distanceOf(node) >= 0}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return {@code true} if a source reaches {@code node}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public boolean isReachable(int node) {
    Objects.checkIndex(node, distance.length);
    return distance[node] >= 0;
  }

  /**
   * Returns a shortest path to {@code node} from the nearest source, or an empty list if no source
   * reaches {@code node}.
   * <p>
   * The path has {@code distanceOf(node) + 1} nodes, with the source first and {@code node} last.
   * Of several shortest paths, this method returns the one that the search finds first.
   * <p>
   * <strong>Time:</strong> O(distanceOf(node)).<br>
   * <strong>Memory:</strong> 4 x (distanceOf(node) + 1) bytes for the result.
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return the node IDs of the path, or an empty list; unmodifiable
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
   * Returns the largest distance of a node that a source reaches, or {@code -1} if there are no
   * sources.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return largest distance, or {@code -1}
   */
  public int maxDistance() {
    return maxDistance;
  }
}
