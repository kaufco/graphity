package com.kobayami.graphity;

import java.util.Arrays;

/**
 * Helpers for a forest given as a parent array, {@code -1} for a root - the form in which
 * {@link Ancestry} and {@link Dominators} hold their trees -, and the grouping by key that its
 * child lists are one case of: the partitions build their member lists with it as well.
 */
final class ParentForest {

  private ParentForest() {}

  /**
   * Indices grouped by key as CSR: those with key {@code k} are
   * {@code members[start[k]..start[k + 1])}, ascending.
   */
  record Groups(int[] start, int[] members) {}

  /**
   * The child lists of the forest, each ascending: {@link #groupIndices} with the parent as key.
   * <p>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + childCount) bytes.
   */
  static Groups childrenOf(int[] parent) {
    return groupIndices(parent, parent.length);
  }

  /**
   * Groups the indices {@code i} of {@code keyOf} by their key {@code keyOf[i]}, which lies in
   * {@code 0..keyCount-1}; an index with a negative key is in no group.
   * <p>
   * <strong>Memory:</strong> 4 x (keyCount + 1 + groupedCount) bytes.
   */
  static Groups groupIndices(int[] keyOf, int keyCount) {
    var start = new int[keyCount + 1];
    for (int key : keyOf) {
      if (key >= 0) start[key]++;
    }
    return new Groups(start, groupCounted(keyOf, start));
  }

  /**
   * The second half of {@link #groupIndices}, for a caller that counts the keys in a pass it
   * makes anyway. On entry {@code start[k]} holds the number of indices with key {@code k} and
   * {@code start[keyCount]} is {@code 0}; on return {@code start} holds the offsets of the groups.
   *
   * @return the members of all groups, grouped by key
   */
  static int[] groupCounted(int[] keyOf, int[] start) {
    int keyCount = start.length - 1;
    int next = 0;
    for (int key = 0; key < keyCount; key++) {
      int count = start[key];
      start[key] = next;
      next += count;
    }
    var members = new int[next];
    // The ascending scan leaves every group sorted; start[k] doubles as write pointer and is
    // shifted back afterwards.
    for (int i = 0; i < keyOf.length; i++) {
      int key = keyOf[i];
      if (key >= 0) members[start[key]++] = i;
    }
    for (int key = keyCount; key > 0; key--) {
      start[key] = start[key - 1];
    }
    start[0] = 0;
    return members;
  }

  /**
   * The canonical preorder of the forest - roots ascending, children ascending - with the size
   * of every subtree. Fills the three given arrays, each of length nodeCount.
   * <p>
   * {@code IteratedDominanceFrontier.of} in {@code graphity-controlflow} keeps a copy, extended
   * by the depth of every node, and {@code Loops.of} there copies this and
   * {@link #groupCounted}; change them together.
   * <p>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + childCount) bytes for the child lists.
   */
  static void preorder(int[] parent, int[] preorderIndex, int[] order, int[] subtreeSize) {
    int n = parent.length;
    var children = childrenOf(parent);
    int[] start = children.start();
    int[] nodes = children.members();

    // Every node is pushed exactly once, so the stack fits in n slots; the subtree-size array
    // serves as the stack until it is filled below.
    int[] stack = subtreeSize;
    int top = 0;
    for (int node = n - 1; node >= 0; node--) {
      if (parent[node] < 0) stack[top++] = node;
    }
    int position = 0;
    while (top > 0) {
      int node = stack[--top];
      preorderIndex[node] = position;
      order[position++] = node;
      for (int i = start[node + 1] - 1; i >= start[node]; i--) {
        stack[top++] = nodes[i];
      }
    }

    // Reverse preorder visits every child before its parent.
    Arrays.fill(subtreeSize, 1);
    for (int i = n - 1; i >= 0; i--) {
      int node = order[i];
      int p = parent[node];
      if (p >= 0) subtreeSize[p] += subtreeSize[node];
    }
  }
}
