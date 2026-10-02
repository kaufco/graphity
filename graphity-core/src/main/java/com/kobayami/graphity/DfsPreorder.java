package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Depth-first preorder of the nodes reachable from a list of entries - the common first step of
 * {@link Dominators} and {@link Loops}.
 * <p>
 * Preorder numbers start at 1. Number 0 is a virtual root above all entries, so that several
 * entries need no special case downstream: every entry the search starts from is a child of
 * the virtual root, and {@link #isEntry(int)} marks every entry, also one that the search
 * reached from an earlier entry. Successors are visited in ascending node order and entries in
 * list order. Iterative; the frame stack lives on the heap.
 */
final class DfsPreorder {

  /**
   * Largest supported node count. The control-flow analyses hold two {@code int}s per preorder
   * number, the virtual root included, and HotSpot caps an {@code int[]} just below
   * {@code Integer.MAX_VALUE} elements: {@code 2 x (MAX_NODE_COUNT + 1)} has to fit.
   */
  static final int MAX_NODE_COUNT = (1 << 30) - 3;

  /** Number of nodes reachable from the entries. */
  final int reachedCount;

  /** Node with each preorder number, {@code order[0] == -1} for the virtual root. */
  final int[] order;

  /** Preorder number of each node, {@code 0} for a node not reachable from any entry. */
  final int[] preorderNumber;

  /** Preorder number of each node's parent in the search tree, {@code 0} below the virtual root. */
  final int[] parent;

  /** Bit set over preorder numbers: set for every entry. */
  private final long[] entryBits;

  private DfsPreorder(
      int reachedCount, int[] order, int[] preorderNumber, int[] parent, long[] entryBits) {
    this.reachedCount = reachedCount;
    this.order = order;
    this.preorderNumber = preorderNumber;
    this.parent = parent;
    this.entryBits = entryBits;
  }

  /**
   * Runs the search.
   *
   * @param analysis name of the calling analysis, for the exception message
   * @throws IndexOutOfBoundsException if an entry is out of range
   * @throws IllegalArgumentException if {@code graph.nodeCount() > MAX_NODE_COUNT}
   */
  static DfsPreorder of(Graph graph, IntList entries, String analysis) {
    int n = graph.nodeCount();
    if (n > MAX_NODE_COUNT) {
      throw new IllegalArgumentException(analysis
          + " keeps two ints per node in its working arrays and so requires nodeCount < 2^30 - 2,"
          + " but was " + n);
    }
    int entryCount = entries.size();
    for (int i = 0; i < entryCount; i++) {
      Objects.checkIndex(entries.getInt(i), n);
    }
    int[] out = graph.outAdjacencyStart;
    int[] adj = graph.outAdjacency;

    var preorderNumber = new int[n];
    var order = new int[n + 1];
    var parent = new int[n + 1];
    order[0] = -1;
    parent[0] = -1;
    // Each frame is (node, resume cursor), interleaved, so a frame is one cache line.
    var frames = new int[2 * Math.max(16, Math.min(n, 1024))];
    int count = 0;

    for (int e = 0; e < entryCount; e++) {
      int entry = entries.getInt(e);
      if (preorderNumber[entry] != 0) continue;
      preorderNumber[entry] = ++count;
      order[count] = entry;
      parent[count] = 0;
      frames[0] = entry;
      frames[1] = out[entry];
      int top = 0;

      outer:
      while (top >= 0) {
        int u = frames[2 * top];
        int i = frames[2 * top + 1];
        int end = out[u + 1];
        while (i < end) {
          int v = adj[i++];
          if (preorderNumber[v] == 0) {
            frames[2 * top + 1] = i;
            preorderNumber[v] = ++count;
            order[count] = v;
            parent[count] = preorderNumber[u];
            if (2 * (++top + 1) > frames.length) {
              // At most one frame per node, so 2 x nodeCount suffices and cannot overflow.
              frames = Arrays.copyOf(frames, (int) Math.min(2L * frames.length, 2L * n));
            }
            frames[2 * top] = v;
            frames[2 * top + 1] = out[v];
            continue outer;
          }
        }
        top--;
      }
    }

    var entryBits = new long[(count >> 6) + 1];
    for (int e = 0; e < entryCount; e++) {
      int number = preorderNumber[entries.getInt(e)];
      entryBits[number >> 6] |= 1L << number;
    }
    return new DfsPreorder(count, order, preorderNumber, parent, entryBits);
  }

  /** Whether the node with this preorder number is one of the entries. */
  boolean isEntry(int number) {
    return (entryBits[number >> 6] & (1L << number)) != 0;
  }
}
