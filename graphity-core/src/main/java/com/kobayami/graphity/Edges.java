package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;

/**
 * A lightweight, immutable collection of directed edges.
 * <p>
 * Each edge is identified by its index in {@code 0..count()-1}. Edge {@code i} goes from
 * {@code sourceNodes().getInt(i)} to {@code targetNodes().getInt(i)}.
 * <p>
 * Both lists are unmodifiable {@link IntList} instances - use their standard API for
 * iteration, streaming, and element access. No Collection API is replicated on this class.
 *
 * <pre>{@code
 * Edges edges = BackEdges.of(graph);
 * for (int i = 0; i < edges.count(); i++) {
 *     int source = edges.sourceNodes().getInt(i);
 *     int target = edges.targetNodes().getInt(i);
 *     // process edge source → target
 * }
 * }</pre>
 *
 * @see BackEdges
 * @see EdgeTypes
 */
public final class Edges {

  private static final Edges EMPTY = new Edges(
      IntLists.unmodifiable(new IntArrayList()),
      IntLists.unmodifiable(new IntArrayList()));

  private final IntList sourceNodes;
  private final IntList targetNodes;

  Edges(IntList sourceNodes, IntList targetNodes) {
    this.sourceNodes = sourceNodes;
    this.targetNodes = targetNodes;
  }

  /**
   * Returns the source node IDs, one per edge.
   * {@code sourceNodes().getInt(i)} is the source of edge {@code i}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return unmodifiable list of source node IDs
   */
  public IntList sourceNodes() {
    return sourceNodes;
  }

  /**
   * Returns the target node IDs, one per edge.
   * {@code targetNodes().getInt(i)} is the target of edge {@code i}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return unmodifiable list of target node IDs
   */
  public IntList targetNodes() {
    return targetNodes;
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
   * Orders {@code count} edges, given as interleaved (source, target) pairs, by source and then
   * target, optionally dropping repeated pairs. Two stable counting passes, first by target, then
   * by source: O(nodeCount + count) without comparisons. Input that is already in order, or in
   * exactly reverse order - the bridges of a path arrive that way - is copied instead; the check
   * stops at the first pair out of line.
   * <p>
   * <strong>Memory:</strong> 4 x nodeCount bytes for the counts and 16 x count bytes for the
   * intermediate and final lists.
   *
   * @param pairs interleaved pairs, each node ID in {@code 0..nodeCount-1}; read only
   * @param count number of pairs
   * @param nodeCount size of the node ID range
   * @param dropDuplicates whether to keep only the first of equal pairs
   */
  static Edges sortedPairs(int[] pairs, int count, int nodeCount, boolean dropDuplicates) {
    if (count == 0) return EMPTY;
    var sources = new int[count];
    var targets = new int[count];
    int direction = direction(pairs, count);
    if (direction != 0) {
      for (int i = 0; i < count; i++) {
        int from = direction > 0 ? i : count - 1 - i;
        sources[i] = pairs[2 * from];
        targets[i] = pairs[2 * from + 1];
      }
    } else {
      countingSort(pairs, count, nodeCount, sources, targets);
    }
    int kept = count;
    if (dropDuplicates) {
      kept = 0;
      for (int i = 0; i < count; i++) {
        if (kept > 0 && sources[kept - 1] == sources[i] && targets[kept - 1] == targets[i]) {
          continue;
        }
        sources[kept] = sources[i];
        targets[kept] = targets[i];
        kept++;
      }
    }
    if (kept < count) {
      sources = Arrays.copyOf(sources, kept);
      targets = Arrays.copyOf(targets, kept);
    }
    return new Edges(IntLists.unmodifiable(IntArrayList.wrap(sources)),
        IntLists.unmodifiable(IntArrayList.wrap(targets)));
  }

  /** {@code 1} if the pairs are in order, {@code -1} if exactly reversed, else {@code 0}. */
  private static int direction(int[] pairs, int count) {
    int direction = 0;
    for (int i = 1; i < count; i++) {
      int bySource = Integer.compare(pairs[2 * i - 2], pairs[2 * i]);
      int step = bySource != 0 ? bySource : Integer.compare(pairs[2 * i - 1], pairs[2 * i + 1]);
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
  private static void countingSort(
      int[] pairs, int count, int nodeCount, int[] sources, int[] targets) {
    var start = new int[nodeCount + 1];
    for (int i = 0; i < count; i++) {
      start[pairs[2 * i + 1] + 1]++;
    }
    for (int v = 0; v < nodeCount; v++) {
      start[v + 1] += start[v];
    }
    var byTargetSource = new int[count];
    var byTargetTarget = new int[count];
    for (int i = 0; i < count; i++) {
      int target = pairs[2 * i + 1];
      int slot = start[target]++;
      byTargetSource[slot] = pairs[2 * i];
      byTargetTarget[slot] = target;
    }
    Arrays.fill(start, 0);
    for (int i = 0; i < count; i++) {
      start[byTargetSource[i] + 1]++;
    }
    for (int v = 0; v < nodeCount; v++) {
      start[v + 1] += start[v];
    }
    for (int i = 0; i < count; i++) {
      int source = byTargetSource[i];
      int slot = start[source]++;
      sources[slot] = source;
      targets[slot] = byTargetTarget[i];
    }
  }
}
