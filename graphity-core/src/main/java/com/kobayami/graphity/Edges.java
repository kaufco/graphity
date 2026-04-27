package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

/**
 * A lightweight, immutable collection of directed edges.
 * <p>
 * Each edge is identified by its index in {@code 0..count()-1}. Edge {@code i} goes from
 * {@code sourceNodes().getInt(i)} to {@code targetNodes().getInt(i)}.
 * <p>
 * Both lists are unmodifiable {@link IntList} instances — use their standard API for
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
   *
   * @return an empty {@code Edges} instance
   */
  static Edges empty() {
    return EMPTY;
  }
}
