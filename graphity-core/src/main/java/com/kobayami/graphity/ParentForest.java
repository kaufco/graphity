package com.kobayami.graphity;

import java.util.Arrays;

// Helpers for a forest given as a parent array, -1 for a root - the form in which Ancestry and
// Dominators hold their trees - and for the grouping by key that its child lists are one case
// of: the partitions build their member lists with it as well.
final class ParentForest {

  private ParentForest() {}

  // Indices grouped by key, in the layout of the out-adjacency of a Graph: those with key k are
  // members[start[k]..start[k + 1] - 1], ascending.
  record Groups(int[] start, int[] members) {}

  // The child lists of the forest, each ascending: groupIndices with the parent as key.
  static Groups childrenOf(int[] parent) {
    return groupIndices(parent, parent.length);
  }

  // Groups the indices i of keyOf by their key keyOf[i], which lies in 0..keyCount-1; an index
  // with a negative key is in no group.
  static Groups groupIndices(int[] keyOf, int keyCount) {
    var start = new int[keyCount + 1];
    for (int key : keyOf) {
      if (key >= 0) start[key]++;
    }
    return new Groups(start, groupCounted(keyOf, start));
  }

  // The second half of groupIndices, for a caller that counts the keys in a pass it makes
  // anyway. On entry start[k] holds the number of indices with key k and start[keyCount] is 0;
  // on return start holds the offsets of the groups, and the result their members.
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

  // The canonical preorder of the forest - roots ascending, children ascending - with the size
  // of every subtree. Fills the three given arrays, each of length nodeCount.
  //
  // IteratedDominanceFrontier.of in graphity-controlflow keeps a copy, extended by the depth of
  // every node, and Loops.of there copies this and groupCounted; change them together.
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
