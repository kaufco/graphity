package com.kobayami.graphity;

/**
 * Union-find with path halving and union by rank, as static helpers over two arrays the caller
 * holds: {@code parent}, from {@link #singletons(int)}, and a {@code byte[] rank} of the same
 * length, all zero. {@code union} reports whether it merged two distinct sets, which is what lets
 * a caller count components without a second pass. Union by rank keeps every rank at most
 * {@code log2(elementCount)}, so a byte per element suffices for any array Java can allocate.
 * <p>
 * Static helpers rather than an object: {@link Components#wccsOf(Graph)}, which the peer
 * benchmarks compare with other libraries, ran its own union-find over local arrays in this form,
 * and sharing it was measured there without a difference. The predicates in {@link Graphs} take
 * the same form, so there is one.
 */
final class UnionFind {

  private UnionFind() {}

  /** A parent array of {@code elementCount} singleton sets: every element is its own root. */
  static int[] singletons(int elementCount) {
    int[] parent = new int[elementCount];
    for (int i = 0; i < elementCount; i++) {
      parent[i] = i;
    }
    return parent;
  }

  /** The root of {@code element}'s set. */
  static int find(int[] parent, int element) {
    int x = element;
    while (parent[x] != x) {
      parent[x] = parent[parent[x]]; // path halving
      x = parent[x];
    }
    return x;
  }

  /**
   * Merges the sets of {@code a} and {@code b}; {@code false} if they were one set already.
   * {@code rank[r]} bounds the height of the tree below root {@code r} and means nothing for a
   * non-root.
   */
  static boolean union(int[] parent, byte[] rank, int a, int b) {
    int ra = find(parent, a);
    int rb = find(parent, b);
    if (ra == rb) {
      return false;
    }
    if (rank[ra] < rank[rb]) {
      parent[ra] = rb;
    } else if (rank[ra] > rank[rb]) {
      parent[rb] = ra;
    } else {
      parent[rb] = ra;
      rank[ra]++;
    }
    return true;
  }
}
