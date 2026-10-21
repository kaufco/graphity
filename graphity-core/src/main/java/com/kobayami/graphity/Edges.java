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
   * Orders {@code count} edges of {@code graph}, given as interleaved (source, target, edge index)
   * triples in their direction in the graph, by source and then target - which is edge index
   * order -, optionally dropping repeated edges. An edge index of {@code -1} is looked up with one
   * binary search in the source's adjacency; callers pass the index wherever they know it. Two
   * stable counting passes, first by target, then by source: O(nodeCount + count) without
   * comparisons. Input that is already in order, or in exactly reverse order - the bridges of a
   * path arrive that way - is copied instead; the check stops at the first edge out of line.
   * <p>
   * <strong>Memory:</strong> 4 x nodeCount bytes for the counts and 24 x count bytes for the
   * intermediate and final lists.
   *
   * @param triples interleaved triples, each an edge of {@code graph}; read only
   * @param count number of triples
   * @param dropDuplicates whether to keep only the first of equal edges
   */
  static Edges sortedEdges(Graph graph, int[] triples, int count, boolean dropDuplicates) {
    if (count == 0) return EMPTY;
    var sources = new int[count];
    var targets = new int[count];
    var edgeIndices = new int[count];
    int direction = direction(triples, count);
    if (direction != 0) {
      for (int i = 0; i < count; i++) {
        int from = 3 * (direction > 0 ? i : count - 1 - i);
        sources[i] = triples[from];
        targets[i] = triples[from + 1];
        edgeIndices[i] = triples[from + 2];
      }
    } else {
      countingSort(triples, count, graph.nodeCount(), sources, targets, edgeIndices);
    }
    int kept = count;
    if (dropDuplicates) {
      kept = 0;
      for (int i = 0; i < count; i++) {
        if (kept > 0 && sources[kept - 1] == sources[i] && targets[kept - 1] == targets[i]) {
          if (edgeIndices[kept - 1] < 0) edgeIndices[kept - 1] = edgeIndices[i];
          continue;
        }
        sources[kept] = sources[i];
        targets[kept] = targets[i];
        edgeIndices[kept] = edgeIndices[i];
        kept++;
      }
    }
    if (kept < count) {
      sources = Arrays.copyOf(sources, kept);
      targets = Arrays.copyOf(targets, kept);
      edgeIndices = Arrays.copyOf(edgeIndices, kept);
    }
    for (int p = 0; p < kept; p++) {
      if (edgeIndices[p] < 0) edgeIndices[p] = graph.edgeIndexOf(sources[p], targets[p]);
      assert edgeIndices[p] == graph.edgeIndexOf(sources[p], targets[p])
          : sources[p] + " -> " + targets[p] + " is not edge " + edgeIndices[p];
    }
    return wrap(sources, targets, edgeIndices);
  }

  /** {@code 1} if the triples are in order, {@code -1} if exactly reversed, else {@code 0}. */
  private static int direction(int[] triples, int count) {
    int direction = 0;
    for (int i = 1; i < count; i++) {
      int a = 3 * (i - 1);
      int b = 3 * i;
      int bySource = Integer.compare(triples[a], triples[b]);
      int step = bySource != 0 ? bySource : Integer.compare(triples[a + 1], triples[b + 1]);
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
  private static void countingSort(int[] triples, int count, int nodeCount, int[] sources,
      int[] targets, int[] edgeIndices) {
    var start = new int[nodeCount + 1];
    for (int i = 0; i < count; i++) {
      start[triples[3 * i + 1] + 1]++;
    }
    for (int v = 0; v < nodeCount; v++) {
      start[v + 1] += start[v];
    }
    var byTarget = new int[3 * count];
    for (int i = 0; i < count; i++) {
      int slot = 3 * start[triples[3 * i + 1]]++;
      byTarget[slot] = triples[3 * i];
      byTarget[slot + 1] = triples[3 * i + 1];
      byTarget[slot + 2] = triples[3 * i + 2];
    }
    Arrays.fill(start, 0);
    for (int i = 0; i < count; i++) {
      start[byTarget[3 * i] + 1]++;
    }
    for (int v = 0; v < nodeCount; v++) {
      start[v + 1] += start[v];
    }
    for (int i = 0; i < count; i++) {
      int source = byTarget[3 * i];
      int slot = start[source]++;
      sources[slot] = source;
      targets[slot] = byTarget[3 * i + 1];
      edgeIndices[slot] = byTarget[3 * i + 2];
    }
  }
}
