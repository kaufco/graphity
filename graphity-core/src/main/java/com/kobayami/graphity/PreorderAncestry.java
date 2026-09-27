package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Objects;

/**
 * An {@link Ancestry} that also knows the preorder of its forest, which makes the ancestor test
 * constant-time and every subtree a contiguous range.
 * <p>
 * Built from an existing index via {@link #of(Ancestry)}. It shares the three arrays of that
 * index and adds three of its own: the preorder position of each node, the size of each
 * subtree, and the node at each preorder position. In preorder, the subtree of a node is the
 * range {@code [preorderIndexOf(node), preorderIndexOf(node) + subtreeSizeOf(node))}, so
 * {@link #isAncestorOf(int, int)} is a range test and {@link #subtreeOf(int)} a view.
 * <p>
 * The preorder is canonical: roots in ascending order, the children of each node in ascending
 * order. For a tree whose IDs were assigned in source-order preorder, as is usual for a syntax
 * tree, {@link #preorder()} is the identity and the subtree of {@code node} is the ID range
 * {@code [node, node + subtreeSizeOf(node))}.
 *
 * <pre>{@code
 * var ancestry = PreorderAncestry.of(Ancestry.of(ast));
 * boolean inMethod = ancestry.isAncestorOf(method, expression);   // O(1)
 * IntList body = ancestry.subtreeOf(method);                      // zero-copy
 * }</pre>
 *
 * <strong>Memory:</strong> 12 x nodeCount bytes on top of the {@link Ancestry} it extends.
 *
 * @see Ancestry
 */
public final class PreorderAncestry extends Ancestry {

  /** Position of each node in {@link #order}. */
  private final int[] preorderIndex;

  /** Number of nodes in the subtree of each node, the node itself included. */
  private final int[] subtreeSize;

  /** Node at each preorder position, wrapped once so that views need no further copy. */
  private final IntImmutableList order;

  private PreorderAncestry(Ancestry ancestry, int[] preorderIndex, int[] subtreeSize, int[] order) {
    super(ancestry.parent, ancestry.depth, ancestry.chainHead);
    this.preorderIndex = preorderIndex;
    this.subtreeSize = subtreeSize;
    this.order = new IntImmutableList(order);
  }

  /**
   * Derives the preorder index of the given ancestor index.
   * <p>
   * The result shares the arrays of {@code ancestry}. If {@code ancestry} already is a
   * {@code PreorderAncestry}, it is returned unchanged. Not cached - keep the result.
   * <p>
   * <strong>Time:</strong> O(nodeCount), or O(1) if already a {@code PreorderAncestry}.<br>
   * <strong>Memory:</strong> 12 x nodeCount bytes for the result; 8 x nodeCount bytes
   * additional during computation.
   *
   * @param ancestry the ancestor index to extend
   * @return the preorder index
   */
  public static PreorderAncestry of(Ancestry ancestry) {
    if (ancestry instanceof PreorderAncestry preorderAncestry) return preorderAncestry;
    int[] parent = ancestry.parent;
    int n = parent.length;
    var preorderIndex = new int[n];
    var order = new int[n];
    var subtreeSize = new int[n];
    ParentForest.preorder(
        parent, ParentForest.childrenOf(parent), preorderIndex, order, subtreeSize);
    return new PreorderAncestry(ancestry, preorderIndex, subtreeSize, order);
  }

  /**
   * Returns whether {@code node} is an ancestor of {@code descendant}. A node is its own
   * ancestor.
   * <p>
   * <strong>Time:</strong> O(1) - a range test on the preorder positions.
   *
   * @param node       potential ancestor, a node ID in {@code 0..nodeCount()-1}
   * @param descendant potential descendant, a node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if {@code node} is an ancestor of {@code descendant}
   * @throws IndexOutOfBoundsException if a node ID is out of range
   */
  @Override
  public boolean isAncestorOf(int node, int descendant) {
    Objects.checkIndex(node, preorderIndex.length);
    Objects.checkIndex(descendant, preorderIndex.length);
    int offset = preorderIndex[descendant] - preorderIndex[node];
    return offset >= 0 && offset < subtreeSize[node];
  }

  /**
   * Returns the number of nodes in the subtree of {@code node}, the node itself included.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return subtree size, at least 1
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int subtreeSizeOf(int node) {
    Objects.checkIndex(node, subtreeSize.length);
    return subtreeSize[node];
  }

  /**
   * Returns the subtree of {@code node} in preorder, starting with {@code node}.
   * <p>
   * The list is a zero-copy, read-only view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return the nodes of the subtree, {@code subtreeSizeOf(node)} of them
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public IntList subtreeOf(int node) {
    Objects.checkIndex(node, preorderIndex.length);
    int from = preorderIndex[node];
    return order.subList(from, from + subtreeSize[node]);
  }

  /**
   * Returns all nodes in preorder: roots in ascending order, the children of each node in
   * ascending order.
   * <p>
   * The list is a zero-copy, read-only view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return all nodes, each exactly once
   */
  public IntList preorder() {
    return order;
  }

  /**
   * Returns the position of {@code node} in {@link #preorder()}.
   * <p>
   * With it, a caller can lay out per-node data in preorder - for example prefix sums, which
   * then aggregate any subtree in O(1).
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return preorder position in {@code 0..nodeCount()-1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int preorderIndexOf(int node) {
    Objects.checkIndex(node, preorderIndex.length);
    return preorderIndex[node];
  }
}
