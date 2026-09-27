package com.kobayami.graphity;

import java.util.Arrays;

/**
 * Helpers for a forest given as a parent array, {@code -1} for a root - the form in which
 * {@link Ancestry} and {@link Dominators} hold their trees.
 */
final class ParentForest {

  private ParentForest() {}

  /** Children per node as CSR: those of {@code v} are {@code nodes[start[v]..start[v + 1])}. */
  record Children(int[] start, int[] nodes) {}

  /**
   * The child lists of the forest, each ascending.
   * <p>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + childCount) bytes.
   */
  static Children childrenOf(int[] parent) {
    int n = parent.length;
    var start = new int[n + 1];
    for (int node = 0; node < n; node++) {
      if (parent[node] >= 0) start[parent[node]]++;
    }
    int next = 0;
    for (int node = 0; node < n; node++) {
      int count = start[node];
      start[node] = next;
      next += count;
    }
    start[n] = next;
    var nodes = new int[next];
    // The ascending scan leaves every list sorted; start[p] doubles as write pointer and is
    // shifted back afterwards.
    for (int node = 0; node < n; node++) {
      int p = parent[node];
      if (p >= 0) nodes[start[p]++] = node;
    }
    for (int node = n; node > 0; node--) {
      start[node] = start[node - 1];
    }
    start[0] = 0;
    return new Children(start, nodes);
  }

  /**
   * The canonical preorder of the forest - roots ascending, children ascending - with the size
   * of every subtree. Fills the three given arrays, each of length nodeCount.
   */
  static void preorder(
      int[] parent, Children children, int[] preorderIndex, int[] order, int[] subtreeSize) {
    int n = parent.length;
    int[] start = children.start();
    int[] nodes = children.nodes();

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
