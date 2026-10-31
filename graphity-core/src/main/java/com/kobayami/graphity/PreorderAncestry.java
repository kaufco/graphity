package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Objects;

/**
 * An {@link Ancestry} that also holds the preorder of its forest, for an ancestor test in O(1) and
 * the nodes of every subtree.
 * <p>
 * {@link #of(Ancestry)} builds it from an index and shares the arrays of that index. In preorder,
 * every subtree is a contiguous range: the subtree of {@code node} takes the positions
 * {@code preorderIndexOf(node)..preorderIndexOf(node) + subtreeSizeOf(node) - 1} of
 * {@link #preorder()}. So {@link #isAncestorOf(int, int)} is a range test, and
 * {@link #subtreeOf(int)} returns a view of that range. Here, the basic blocks of the method
 * {@code load} that {@code body} dominates, from the {@code dominators} of {@code load}:
 * {@snippet file="com/kobayami/graphity/examples/Dominance.java" region="subtreeOf"}
 * <p>
 * The preorder visits the roots in ascending order, and the children of each node in ascending
 * order. If the node IDs were assigned in this order, as is usual for a syntax tree numbered in
 * source order, the preorder is {@code 0..nodeCount-1}, and the subtree of {@code node} is
 * {@code node..node + subtreeSizeOf(node) - 1}.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 12 x nodeCount bytes on top of the {@link Ancestry} it extends.
 */
public final class PreorderAncestry extends Ancestry {

  // Position of each node in order.
  private final int[] preorderIndex;

  // Number of nodes in the subtree of each node, the node itself included.
  private final int[] subtreeSize;

  // Node at each preorder position, wrapped once so that views need no further copy.
  private final IntImmutableList order;

  private PreorderAncestry(Ancestry ancestry, int[] preorderIndex, int[] subtreeSize, int[] order) {
    super(ancestry.parent, ancestry.depth, ancestry.chainHead);
    this.preorderIndex = preorderIndex;
    this.subtreeSize = subtreeSize;
    this.order = new IntImmutableList(order);
  }

  /**
   * Returns a {@code PreorderAncestry} for the forest of {@code ancestry}.
   * <p>
   * The result shares the arrays of {@code ancestry} instead of copying them. The preorder is
   * computed anew on every call, not cached: keep the result to use it more than once. If
   * {@code ancestry} already is a {@code PreorderAncestry}, returns it.
   * <p>
   * <strong>Time:</strong> O(nodeCount), or O(1) if already a {@code PreorderAncestry}.<br>
   * <strong>Memory:</strong> +12 x nodeCount bytes for the result; while it runs, 8 x nodeCount
   * bytes more; none if already a {@code PreorderAncestry}.
   *
   * @param ancestry the index to add the preorder to
   * @return the index with the preorder
   */
  public static PreorderAncestry of(Ancestry ancestry) {
    if (ancestry instanceof PreorderAncestry preorderAncestry) return preorderAncestry;
    int[] parent = ancestry.parent;
    int n = parent.length;
    var preorderIndex = new int[n];
    var order = new int[n];
    var subtreeSize = new int[n];
    ParentForest.preorder(parent, preorderIndex, order, subtreeSize);
    return new PreorderAncestry(ancestry, preorderIndex, subtreeSize, order);
  }

  /**
   * Returns whether {@code node} is an ancestor of {@code descendant}: whether it lies on the path
   * from the root of its tree to {@code descendant}. A node is its own ancestor.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node       node ID in {@code 0..nodeCount-1}
   * @param descendant node ID in {@code 0..nodeCount-1}
   * @return {@code true} if {@code node} is an ancestor of {@code descendant}
   * @throws IndexOutOfBoundsException if {@code node} or {@code descendant} is out of range
   */
  @Override
  public boolean isAncestorOf(int node, int descendant) {
    Objects.checkIndex(node, preorderIndex.length);
    Objects.checkIndex(descendant, preorderIndex.length);
    int offset = preorderIndex[descendant] - preorderIndex[node];
    return offset >= 0 && offset < subtreeSize[node];
  }

  /**
   * Returns the number of nodes in the subtree of {@code node}, {@code node} included.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return subtree size, at least {@code 1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int subtreeSizeOf(int node) {
    Objects.checkIndex(node, subtreeSize.length);
    return subtreeSize[node];
  }

  /**
   * Returns the nodes of the subtree of {@code node}, in preorder, starting with {@code node}.
   * <p>
   * The result is an unmodifiable view of this index's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return nodes of the subtree
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public IntList subtreeOf(int node) {
    Objects.checkIndex(node, preorderIndex.length);
    int from = preorderIndex[node];
    return order.subList(from, from + subtreeSize[node]);
  }

  /**
   * Returns all nodes in preorder: the roots in ascending order, and the children of each node in
   * ascending order.
   * <p>
   * The result is an unmodifiable view of this index's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return all nodes, each once
   */
  public IntList preorder() {
    return order;
  }

  /**
   * Returns the position of {@code node} in {@link #preorder()}.
   * <p>
   * Data in an array indexed by preorder position has every subtree in a contiguous range. Prefix
   * sums over such an array, for example, give the sum over any subtree in O(1).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return position in {@code 0..nodeCount-1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int preorderIndexOf(int node) {
    Objects.checkIndex(node, preorderIndex.length);
    return preorderIndex[node];
  }
}
