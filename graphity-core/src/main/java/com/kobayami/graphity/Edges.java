package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;

/**
 * A list of edges of a graph, each with its source, target and edge index. Algorithms such as
 * {@link BackEdges#of(Graph)} and {@link Biconnectivity#bridges()} return it.
 * <p>
 * The edge at position {@code p}, in {@code 0..count()-1}, is
 * {@code sourceNodes().getInt(p) -> targetNodes().getInt(p)}, and {@code edgeIndices().getInt(p)}
 * is its edge index in the graph the algorithm ran on. The edge index finds the data of the edge
 * in the caller's arrays indexed by edge index, without a search. Here, the back edges of the
 * calls between the methods of a program are its recursive calls. {@code names} holds the names
 * of the methods, and {@code siteCount} holds the number of call sites of each call, by edge
 * index:
 * {@snippet file="com/kobayami/graphity/examples/BuildingAndReading.java" region="edges"}
 * Each method that returns {@code Edges} says in which order the edges come.
 * {@link GraphBuilder#addEdges(Edges)} adds them to a graph under construction.
 * <p>
 * Each of the three lists is an unmodifiable {@link IntList} of the fastutil library. Instances
 * are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 12 bytes per edge.
 */
public final class Edges {

  private static final Edges EMPTY = new Edges(IntLists.EMPTY_LIST, IntLists.EMPTY_LIST,
      IntLists.EMPTY_LIST);

  private final IntList sourceNodes;
  private final IntList targetNodes;
  private final IntList edgeIndices;

  // The three lists have the same size and are unmodifiable; nobody else may modify them.
  Edges(IntList sourceNodes, IntList targetNodes, IntList edgeIndices) {
    this.sourceNodes = sourceNodes;
    this.targetNodes = targetNodes;
    this.edgeIndices = edgeIndices;
  }

  // For three arrays of the same length that nobody else holds.
  static Edges wrap(int[] sourceNodes, int[] targetNodes, int[] edgeIndices) {
    return new Edges(IntLists.unmodifiable(IntArrayList.wrap(sourceNodes)),
        IntLists.unmodifiable(IntArrayList.wrap(targetNodes)),
        IntLists.unmodifiable(IntArrayList.wrap(edgeIndices)));
  }

  /**
   * Returns the edges of one type from an edge classification, in edge index order.
   * <p>
   * Each call goes through every edge, however few have the type: call it once per type and keep
   * the result.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 12 bytes per edge of the result.
   *
   * @param edgeTypes the classification of the edges of a graph
   * @param type      the edge type to collect
   * @return the edges of type {@code type}
   */
  public static Edges of(EdgeTypes edgeTypes, EdgeType type) {
    // Counts the edges of the type first, so that the arrays of the result get their exact length,
    // then fills them in one scan over all edges.
    Graph graph = edgeTypes.graph;
    byte[] types = edgeTypes.types;
    byte ordinal = (byte) type.ordinal();
    int count = 0;
    for (byte t : types) {
      if (t == ordinal) count++;
    }
    if (count == 0) {
      return empty();
    }
    var sources = new int[count];
    var targets = new int[count];
    var edgeIndices = new int[count];

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);
    int n = graph.nodeCount();
    int p = 0;

    for (int node = 0; node < n; node++) {
      int start = adjacencyStart[node];
      int end = adjacencyStart[node + 1];
      for (int e = start; e < end; e++) {
        if (types[e] == ordinal) {
          sources[p] = node;
          targets[p] = adjacency[e];
          edgeIndices[p] = e;
          p++;
        }
      }
    }
    return wrap(sources, targets, edgeIndices);
  }

  /**
   * Returns the source of each edge, by position.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return source node IDs
   */
  public IntList sourceNodes() {
    return sourceNodes;
  }

  /**
   * Returns the target of each edge, by position.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return target node IDs
   */
  public IntList targetNodes() {
    return targetNodes;
  }

  /**
   * Returns the edge index of each edge in the graph the algorithm ran on, by position.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return edge indices
   */
  public IntList edgeIndices() {
    return edgeIndices;
  }

  /**
   * Returns the number of edges.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return edge count
   */
  public int count() {
    return sourceNodes.size();
  }

  /**
   * Returns whether there are no edges. Equivalent to {@code count() == 0}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if there are no edges
   */
  public boolean isEmpty() {
    return count() == 0;
  }

  static Edges empty() {
    return EMPTY;
  }

  // Returns count edges of graph, given as parallel arrays in their direction in the graph, in
  // edge index order - by source, then target - and drops repeated edges if dropDuplicates is
  // set, keeping the first. The arrays are read, not kept, and may be longer than count. An edge
  // index of -1 is looked up with one binary search in the adjacency of the source; callers pass
  // the index wherever they know it.
  //
  // Two stable counting passes, first by target, then by source, sort in O(nodeCount + count)
  // without comparisons. Input already in order, or in exactly reverse order - the bridges of a
  // path arrive that way - is copied instead; the check stops at the first edge out of line. It
  // takes 4 x nodeCount bytes for the counts and 24 x count bytes for the intermediate and final
  // lists.
  //
  // The arrays are parallel so that none of them needs more than count entries. Interleaved in
  // one array, the strong bridges of a graph with a third of the supported node count would
  // already exceed the largest array.
  static Edges sortedEdges(Graph graph, int[] sources, int[] targets, int[] edgeIndices,
      int count, boolean dropDuplicates) {
    if (count == 0) return EMPTY;
    var sortedSources = new int[count];
    var sortedTargets = new int[count];
    var sortedEdgeIndices = new int[count];
    int direction = direction(sources, targets, count);
    if (direction > 0) {
      System.arraycopy(sources, 0, sortedSources, 0, count);
      System.arraycopy(targets, 0, sortedTargets, 0, count);
      System.arraycopy(edgeIndices, 0, sortedEdgeIndices, 0, count);
    } else if (direction < 0) {
      for (int i = 0, from = count - 1; i < count; i++, from--) {
        sortedSources[i] = sources[from];
        sortedTargets[i] = targets[from];
        sortedEdgeIndices[i] = edgeIndices[from];
      }
    } else {
      countingSort(sources, targets, edgeIndices, count, graph.nodeCount(),
          sortedSources, sortedTargets, sortedEdgeIndices);
    }
    int kept = count;
    if (dropDuplicates) {
      kept = 0;
      for (int i = 0; i < count; i++) {
        if (kept > 0 && sortedSources[kept - 1] == sortedSources[i]
            && sortedTargets[kept - 1] == sortedTargets[i]) {
          if (sortedEdgeIndices[kept - 1] < 0) sortedEdgeIndices[kept - 1] = sortedEdgeIndices[i];
          continue;
        }
        sortedSources[kept] = sortedSources[i];
        sortedTargets[kept] = sortedTargets[i];
        sortedEdgeIndices[kept] = sortedEdgeIndices[i];
        kept++;
      }
    }
    if (kept < count) {
      sortedSources = Arrays.copyOf(sortedSources, kept);
      sortedTargets = Arrays.copyOf(sortedTargets, kept);
      sortedEdgeIndices = Arrays.copyOf(sortedEdgeIndices, kept);
    }
    for (int p = 0; p < kept; p++) {
      if (sortedEdgeIndices[p] < 0) {
        sortedEdgeIndices[p] = graph.edgeIndexOf(sortedSources[p], sortedTargets[p]);
      }
      assert sortedEdgeIndices[p] == graph.edgeIndexOf(sortedSources[p], sortedTargets[p])
          : sortedSources[p] + " -> " + sortedTargets[p] + " is not edge " + sortedEdgeIndices[p];
    }
    return wrap(sortedSources, sortedTargets, sortedEdgeIndices);
  }

  // 1 if the edges are in order, -1 if exactly reversed, otherwise 0.
  private static int direction(int[] sources, int[] targets, int count) {
    int direction = 0;
    for (int i = 1; i < count; i++) {
      int bySource = Integer.compare(sources[i - 1], sources[i]);
      int step = bySource != 0 ? bySource : Integer.compare(targets[i - 1], targets[i]);
      if (step == 0) continue;
      int sign = step < 0 ? 1 : -1;
      if (direction == 0) {
        direction = sign;
      } else if (direction != sign) {
        return 0;
      }
    }
    return direction == 0 ? 1 : direction;
  }

  // Two stable counting passes, by target and then by source, into the sorted arrays.
  private static void countingSort(int[] sources, int[] targets, int[] edgeIndices, int count,
      int nodeCount, int[] sortedSources, int[] sortedTargets, int[] sortedEdgeIndices) {
    var start = new int[nodeCount + 1];
    for (int i = 0; i < count; i++) {
      start[targets[i] + 1]++;
    }
    for (int v = 0; v < nodeCount; v++) {
      start[v + 1] += start[v];
    }
    var byTargetSources = new int[count];
    var byTargetTargets = new int[count];
    var byTargetEdgeIndices = new int[count];
    for (int i = 0; i < count; i++) {
      int slot = start[targets[i]]++;
      byTargetSources[slot] = sources[i];
      byTargetTargets[slot] = targets[i];
      byTargetEdgeIndices[slot] = edgeIndices[i];
    }
    Arrays.fill(start, 0);
    for (int i = 0; i < count; i++) {
      start[byTargetSources[i] + 1]++;
    }
    for (int v = 0; v < nodeCount; v++) {
      start[v + 1] += start[v];
    }
    for (int i = 0; i < count; i++) {
      int source = byTargetSources[i];
      int slot = start[source]++;
      sortedSources[slot] = source;
      sortedTargets[slot] = byTargetTargets[i];
      sortedEdgeIndices[slot] = byTargetEdgeIndices[i];
    }
  }
}
