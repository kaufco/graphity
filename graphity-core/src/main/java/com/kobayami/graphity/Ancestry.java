package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;

/**
 * Ancestor queries on a forest of out-trees, in particular the lowest common ancestor.
 * <p>
 * Built once via {@link #of(Graph)} and then queried arbitrarily often: the preprocessing is a
 * heavy-path decomposition, which answers a lowest-common-ancestor query in O(log nodeCount)
 * without storing a table per level.
 * <p>
 * A node is its own ancestor, so {@code lowestCommonAncestorOf(a, a) == a} and
 * {@code isAncestorOf(a, a)} is {@code true}. Two nodes in different trees of the forest have
 * no common ancestor, and the query returns {@code -1}.
 * <p>
 * The input must be a forest of <strong>out-trees</strong> - edges point from parent to child.
 * For a forest of in-trees, pass the transpose:
 * {@code Ancestry.of(inTreeForest.transposed())}. A graph that is not a forest is rejected;
 * in a general directed acyclic graph a node can have several parents, and then the common
 * ancestors of two nodes have no unique deepest element.
 *
 * <pre>{@code
 * var ancestry = Ancestry.of(ast);
 * int enclosing = ancestry.lowestCommonAncestorOf(exprA, exprB);  // innermost common element
 * boolean inScope = ancestry.isAncestorOf(scope, symbol);
 * }</pre>
 *
 * {@link #of(Dominators)} indexes a dominator tree the same way, so that {@code isAncestorOf}
 * answers "dominates". For constant-time ancestor tests and subtree views, derive a
 * {@link PreorderAncestry} via {@link PreorderAncestry#of(Ancestry)}; it shares the arrays of
 * this index.
 * <p>
 * <strong>Time:</strong> O(nodeCount + edgeCount) for construction.<br>
 * <strong>Memory:</strong> 12 x nodeCount bytes for the result;
 * 12 x nodeCount bytes additional during construction.
 *
 * @see Graphs#isForest(Graph)
 * @see Graph#transposed()
 */
public sealed class Ancestry permits PreorderAncestry {

  /** Parent of each node, {@code -1} for a tree root. */
  final int[] parent;

  /** Distance from the tree root, {@code 0} for a tree root. */
  final int[] depth;

  /**
   * Topmost node of the heavy chain containing each node. Jumping from a node to
   * {@code parent[chainHead[node]]} skips a whole chain, and every root-to-node path crosses
   * O(log nodeCount) chains.
   */
  final int[] chainHead;

  Ancestry(int[] parent, int[] depth, int[] chainHead) {
    this.parent = parent;
    this.depth = depth;
    this.chainHead = chainHead;
  }

  /**
   * Builds the ancestor index of the given forest.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 12 x nodeCount bytes for the result;
   * 12 x nodeCount bytes additional during computation.
   *
   * @param forest a forest of out-trees (every node has in-degree ≤ 1, no cycles)
   * @return the ancestor index
   * @throws IllegalArgumentException if the graph is not a forest of out-trees
   */
  public static Ancestry of(Graph forest) {
    int n = forest.nodeCount();
    var adjacency = forest.outAdjacency;
    var adjacencyStart = forest.outAdjacencyStart;

    // Parent per node. A second parent means in-degree > 1, which no forest has.
    var parent = new int[n];
    Arrays.fill(parent, -1);
    for (int node = 0; node < n; node++) {
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        int child = adjacency[i];
        if (parent[child] >= 0) {
          throw new IllegalArgumentException(
              "not a forest: node " + child + " has more than one parent ("
                  + parent[child] + " and " + node + ")");
        }
        parent[child] = node;
      }
    }

    // BFS from all roots. With in-degree ≤ 1 every node is enqueued at most once, so the queue
    // doubles as a topological order: parents before children. Unreached nodes lie on a cycle -
    // including a self-loop, whose node is not a root and is reached by nothing else.
    var depth = new int[n];
    Arrays.fill(depth, -1);
    var order = new int[n];
    int head = 0;
    int tail = 0;
    for (int node = 0; node < n; node++) {
      if (parent[node] < 0) {
        depth[node] = 0;
        order[tail++] = node;
      }
    }
    while (head < tail) {
      int node = order[head++];
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        int child = adjacency[i];
        depth[child] = depth[node] + 1;
        order[tail++] = child;
      }
    }
    if (tail != n) {
      throw new IllegalArgumentException(
          "not a forest: node " + nodeOnCycle(parent, depth) + " lies on a cycle");
    }

    return withHeavyChains(parent, depth, order);
  }

  /**
   * Builds the ancestor index of a dominator tree. Equivalent to
   * {@code Ancestry.of(Graphs.dominatorTree(dominators))}, but without building and checking the
   * tree as a graph: the immediate dominators are shared as the parent array.
   * <p>
   * On the result, {@code isAncestorOf(a, b)} answers "{@code a} dominates {@code b}" and
   * {@code lowestCommonAncestorOf(a, b)} returns the nearest common dominator, {@code -1} if only
   * the virtual root above several entries dominates both. An unreachable node is an isolated
   * root, so it is its own ancestor; check {@link Dominators#isReachable(int)} where that
   * matters.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes for the result, the parent array being shared;
   * 20 x nodeCount bytes additional during computation.
   *
   * @param dominators the dominators whose tree to index
   * @return the ancestor index of the dominator tree
   */
  public static Ancestry of(Dominators dominators) {
    int[] parent = dominators.immediateDominator;
    int n = parent.length;

    // Children per node, then a BFS from all roots for a parents-first order and the depths.
    // The dominator tree is acyclic by construction, so no check is needed.
    var forest = ParentForest.childrenOf(parent);
    int[] childStart = forest.start();
    int[] children = forest.nodes();

    var depth = new int[n];
    var order = new int[n];
    int head = 0;
    int tail = 0;
    for (int node = 0; node < n; node++) {
      if (parent[node] < 0) order[tail++] = node;
    }
    while (head < tail) {
      int node = order[head++];
      for (int i = childStart[node], end = childStart[node + 1]; i < end; i++) {
        int child = children[i];
        depth[child] = depth[node] + 1;
        order[tail++] = child;
      }
    }

    return withHeavyChains(parent, depth, order);
  }

  /**
   * Completes the index from the parents, the depths and an order in which every parent comes
   * before its children.
   */
  private static Ancestry withHeavyChains(int[] parent, int[] depth, int[] order) {
    int n = parent.length;

    // Subtree sizes, then the heaviest child per node. Children come after their parent in the
    // order, so a single backward pass accumulates the sizes.
    var subtreeSize = new int[n];
    Arrays.fill(subtreeSize, 1);
    var heavyChild = new int[n];
    Arrays.fill(heavyChild, -1);
    for (int i = n - 1; i >= 0; i--) {
      int node = order[i];
      int nodeParent = parent[node];
      if (nodeParent < 0) continue;
      subtreeSize[nodeParent] += subtreeSize[node];
      int rival = heavyChild[nodeParent];
      if (rival < 0 || subtreeSize[node] > subtreeSize[rival]) {
        heavyChild[nodeParent] = node;
      }
    }

    // Chain heads, forwards this time: a heavy child continues its parent's chain, every other
    // node starts one.
    var chainHead = new int[n];
    for (int i = 0; i < n; i++) {
      int node = order[i];
      int nodeParent = parent[node];
      chainHead[node] = nodeParent >= 0 && heavyChild[nodeParent] == node
          ? chainHead[nodeParent]
          : node;
    }

    return new Ancestry(parent, depth, chainHead);
  }

  /**
   * Names a node that actually lies on a cycle, not merely one hanging below it. An unreached
   * node has an unreached parent - the search visits every child of every node it reaches - so
   * walking up from any unreached node stays unreached and cannot end at a root. After
   * {@code nodeCount} steps the walk is on the cycle it circles.
   */
  private static int nodeOnCycle(int[] parent, int[] depth) {
    int n = depth.length;
    int node = -1;
    for (int candidate = 0; candidate < n; candidate++) {
      if (depth[candidate] < 0) {
        node = candidate;
        break;
      }
    }
    if (node < 0) throw new AssertionError("caller established that some node is unreached");
    for (int step = 0; step < n; step++) {
      node = parent[node];
    }
    return node;
  }

  /**
   * Returns the lowest common ancestor of the two nodes, or {@code -1} if they lie in
   * different trees of the forest.
   * <p>
   * The lowest common ancestor is the deepest node that is an ancestor of both. Since a node
   * counts as its own ancestor, the result is {@code a} itself if {@code a} is an ancestor of
   * {@code b}, and the tree root if the two branch apart directly below it.
   * <p>
   * <strong>Time:</strong> O(log nodeCount).
   *
   * @param a first node ID in {@code 0..nodeCount()-1}
   * @param b second node ID in {@code 0..nodeCount()-1}
   * @return the lowest common ancestor, or {@code -1} if the nodes are in different trees
   * @throws IndexOutOfBoundsException if a node ID is out of range
   */
  public final int lowestCommonAncestorOf(int a, int b) {
    Objects.checkIndex(a, parent.length);
    Objects.checkIndex(b, parent.length);

    // Lift whichever node sits on the deeper chain to its chain head's parent, until both are
    // on one chain. Once they are, the shallower of the two is the ancestor of the other.
    while (chainHead[a] != chainHead[b]) {
      if (depth[chainHead[a]] < depth[chainHead[b]]) {
        b = parent[chainHead[b]];
        if (b < 0) return -1;
      } else {
        a = parent[chainHead[a]];
        if (a < 0) return -1;
      }
    }
    return depth[a] < depth[b] ? a : b;
  }

  /**
   * Returns whether {@code node} is an ancestor of {@code descendant}, that is, whether it
   * lies on the path from {@code descendant} to its tree root. A node is its own ancestor.
   * <p>
   * <strong>Time:</strong> O(log nodeCount) - this is a lowest-common-ancestor query, not a
   * constant-time interval test. O(1) on a {@link PreorderAncestry}.
   *
   * @param node       potential ancestor, a node ID in {@code 0..nodeCount()-1}
   * @param descendant potential descendant, a node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if {@code node} is an ancestor of {@code descendant}
   * @throws IndexOutOfBoundsException if a node ID is out of range
   */
  public boolean isAncestorOf(int node, int descendant) {
    return lowestCommonAncestorOf(node, descendant) == node;
  }

  /**
   * Returns the distance from {@code node} to the root of its tree, {@code 0} for a root.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return depth of the node
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int depthOf(int node) {
    Objects.checkIndex(node, parent.length);
    return depth[node];
  }

  /**
   * Returns the parent of {@code node}, or {@code -1} if it is the root of its tree.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return parent node ID, or {@code -1} for a tree root
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int parentOf(int node) {
    Objects.checkIndex(node, parent.length);
    return parent[node];
  }
}
