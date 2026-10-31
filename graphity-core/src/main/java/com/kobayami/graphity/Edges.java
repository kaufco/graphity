package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;

/**
 * A lightweight, immutable list of edges of a graph, as an algorithm returns them.
 * <p>
 * The edge at position {@code p} in {@code 0..count()-1} goes from
 * {@code sourceNodes().getInt(p)} to {@code targetNodes().getInt(p)}, and
 * {@code edgeIndices().getInt(p)} is its edge index in the graph the algorithm ran on - the key
 * of arrays of edge data, so a result reaches that data without looking the edge up. Each
 * result documents the order of its positions.
 * <p>
 * The three lists are unmodifiable {@link IntList} instances - use their standard API for
 * iteration, streaming, and element access. No Collection API is replicated on this class.
 *
 * <pre>{@code
 * Edges edges = BackEdges.of(graph);
 * for (int p = 0; p < edges.count(); p++) {
 *     int source = edges.sourceNodes().getInt(p);
 *     int target = edges.targetNodes().getInt(p);
 *     double weight = weights[edges.edgeIndices().getInt(p)];   // weights[edgeIndex]
 * }
 * }</pre>
 *
 * @see BackEdges
 * @see EdgeTypes
 */
public final class Edges {

  private static final Edges EMPTY = new Edges(IntLists.EMPTY_LIST, IntLists.EMPTY_LIST,
      IntLists.EMPTY_LIST);

  private final IntList sourceNodes;
  private final IntList targetNodes;
  private final IntList edgeIndices;

  /** The three lists must have the same size and be unmodifiable. */
  Edges(IntList sourceNodes, IntList targetNodes, IntList edgeIndices) {
    this.sourceNodes = sourceNodes;
    this.targetNodes = targetNodes;
    this.edgeIndices = edgeIndices;
  }

  /** Edges over three lists of equal length that nobody else holds. */
  static Edges wrap(int[] sourceNodes, int[] targetNodes, int[] edgeIndices) {
    return new Edges(IntLists.unmodifiable(IntArrayList.wrap(sourceNodes)),
        IntLists.unmodifiable(IntArrayList.wrap(targetNodes)),
        IntLists.unmodifiable(IntArrayList.wrap(edgeIndices)));
  }

  /**
   * Collects the edges of one type from an edge classification, in edge index order. Each call
   * scans every edge, however few match; to collect several types, call it once per type and
   * keep the results.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(matchingEdgeCount).
   *
   * @param edgeTypes the classification to collect from
   * @param type the edge type to collect
   * @return the edges of the given type
   */
  public static Edges of(EdgeTypes edgeTypes, EdgeType type) {
    return edgeTypes.collect(type);
  }

  /**
   * Returns the source node IDs, one per position.
   * {@code sourceNodes().getInt(p)} is the source of the edge at position {@code p}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return unmodifiable list of source node IDs
   */
  public IntList sourceNodes() {
    return sourceNodes;
  }

  /**
   * Returns the target node IDs, one per position.
   * {@code targetNodes().getInt(p)} is the target of the edge at position {@code p}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return unmodifiable list of target node IDs
   */
  public IntList targetNodes() {
    return targetNodes;
  }

  /**
   * Returns the edge indices in the analyzed graph, one per position.
   * {@code edgeIndices().getInt(p)} is the index of the edge at position {@code p}: in that
   * graph, {@code edgeTarget(edgeIndices().getInt(p)) == targetNodes().getInt(p)}, and the edge
   * starts at {@code sourceNodes().getInt(p)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return unmodifiable list of edge indices
   */
  public IntList edgeIndices() {
    return edgeIndices;
  }

  /**
   * Returns the number of edges in this collection.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return number of edges
   */
  public int count() {
    return sourceNodes.size();
  }

  /**
   * Returns {@code true} if this collection contains no edges.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if {@code count() == 0}
   */
  public boolean isEmpty() {
    return count() == 0;
  }

  /**
   * Returns an empty edge collection.
   */
  static Edges empty() {
    return EMPTY;
  }

  /**
   * Orders {@code count} edges of {@code graph}, given as three parallel arrays in their direction
   * in the graph, by source and then target - which is edge index order -, optionally dropping
   * repeated edges. An edge index of {@code -1} is looked up with one binary search in the
   * source's adjacency; callers pass the index wherever they know it. Two stable counting passes,
   * first by target, then by source: O(nodeCount + count) without comparisons. Input that is
   * already in order, or in exactly reverse order - the bridges of a path arrive that way - is
   * copied instead; the check stops at the first edge out of line.
   * <p>
   * The arrays are parallel so that none of them needs more than {@code count} entries.
   * Interleaved in one array, the strong bridges of a graph with a third of the supported node
   * count would already exceed the largest array.
   * <p>
   * <strong>Memory:</strong> 4 x nodeCount bytes for the counts and 24 x count bytes for the
   * intermediate and final lists.
   *
   * @param sources source of each edge, at least {@code count} entries; read only
   * @param targets target of each edge, at least {@code count} entries; read only
   * @param edgeIndices edge index of each edge or {@code -1}, at least {@code count} entries;
   *     read only
   * @param count number of edges
   * @param dropDuplicates whether to keep only the first of equal edges
   */
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

  /** {@code 1} if the edges are in order, {@code -1} if exactly reversed, else {@code 0}. */
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

  /** Two stable counting passes, by target and then by source, into the given arrays. */
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
