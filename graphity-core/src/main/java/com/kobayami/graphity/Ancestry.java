package com.kobayami.graphity;

import java.util.Arrays;
import java.util.Objects;

/**
 * An index of a forest for ancestor queries: the parent and depth of a node, whether one node is
 * an ancestor of another, and the lowest common ancestor of two nodes.
 * <p>
 * {@link #of(Graph)} builds the index of a forest whose edges point from parent to child, such
 * as a syntax tree. For a forest whose edges point from child to parent, pass the transposed
 * graph: {@code Ancestry.of(BiAdjacentGraph.of(forest).transposed())}. {@link #of(Dominators)}
 * builds the index of a dominator tree, in which the ancestors of a node are its dominators. The
 * index is built once and answers each query in O(log nodeCount) at most, however deep the
 * forest; {@link PreorderAncestry#of(Ancestry)} adds an ancestor test in O(1) and the nodes of
 * each subtree.
 * <p>
 * A node is its own ancestor, so {@code isAncestorOf(a, a)} is {@code true} and
 * {@code lowestCommonAncestorOf(a, a)} is {@code a}. Two nodes in different trees of the forest
 * have no common ancestor. Uses
 * <a href="https://en.wikipedia.org/wiki/Heavy-light_decomposition">heavy-light
 * decomposition</a>.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 12 x nodeCount bytes, of which an index built by
 * {@link #of(Dominators)} shares 4 x nodeCount bytes with the dominators.
 */
public sealed class Ancestry permits PreorderAncestry {

  // Parent of each node, -1 for a tree root.
  final int[] parent;

  // Distance from the tree root, 0 for a tree root.
  final int[] depth;

  // Topmost node of the heavy chain containing each node. Jumping from a node to
  // parent[chainHead[node]] skips a whole chain, and every root-to-node path crosses
  // O(log nodeCount) chains.
  final int[] chainHead;

  Ancestry(int[] parent, int[] depth, int[] chainHead) {
    this.parent = parent;
    this.depth = depth;
    this.chainHead = chainHead;
  }

  /**
   * Returns the ancestor index of {@code forest}, whose edges point from parent to child.
   * <p>
   * In such a forest, no node has more than one in-node, and there is no cycle, not even a
   * self-loop; {@link Graphs#isForest(Graph)} tests this without an exception. The roots of the
   * trees are the root nodes of the graph.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 12 x nodeCount bytes for the result; while it runs, 12 x nodeCount
   * bytes more.
   *
   * @param forest a forest whose edges point from parent to child
   * @return the ancestor index
   * @throws IllegalArgumentException if a node of {@code forest} has more than one in-node, or
   *                                  {@code forest} has a cycle
   */
  public static Ancestry of(Graph forest) {
    // The forest check is that of Graphs.computeForestInfo, here with witnesses for the
    // messages; a change to what counts as a forest belongs in both.
    int n = forest.nodeCount();
    var adjacency = Csr.outAdjacency(forest);
    var adjacencyStart = Csr.outAdjacencyStart(forest);

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

    // Breadth-first search from all roots. With in-degree at most 1, every node is enqueued at most
    // once, so the queue doubles as a topological order: parents before children. A node it misses
    // lies on a cycle, a self-loop included, or below one.
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
   * Returns the ancestor index of the dominator tree of {@code dominators}, in which the
   * ancestors of a node are its dominators.
   * <p>
   * {@code isAncestorOf(a, b)} returns whether {@code a} dominates {@code b}.
   * {@code lowestCommonAncestorOf(a, b)} returns the closest node that dominates both, or
   * {@code -1} if no node does. With several entries, only a virtual root lies above them. A node
   * that no entry reaches is a tree of its own and dominates only itself here;
   * {@link Dominators#isReachable(int)} returns {@code false} for such a node.
   * <p>
   * The result is the same as that of {@code Ancestry.of(Graphs.dominatorTree(dominators))}, but
   * shares the immediate dominators instead of building the tree as a graph.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes for the result, which shares the immediate
   * dominators; while it runs, 20 x nodeCount bytes more.
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
    int[] children = forest.members();

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

  // Completes the index from the parents, the depths and an order in which every parent comes
  // before its children.
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

  // Names a node that actually lies on a cycle, not merely one hanging below it. An unreached
  // node has an unreached parent - the search visits every child of every node it reaches - so
  // walking up from any unreached node stays unreached and cannot end at a root. After nodeCount
  // steps the walk is on the cycle it circles.
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
   * Returns the lowest common ancestor of {@code a} and {@code b}, or {@code -1} if they are in
   * different trees.
   * <p>
   * The lowest common ancestor is the deepest node that is an ancestor of both. As a node is its
   * own ancestor, the result is {@code a} if {@code a} is an ancestor of {@code b}.
   * <p>
   * <strong>Time:</strong> O(log nodeCount).
   *
   * @param a node ID in {@code 0..nodeCount-1}
   * @param b node ID in {@code 0..nodeCount-1}
   * @return lowest common ancestor, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code a} or {@code b} is out of range
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
   * Returns whether {@code node} is an ancestor of {@code descendant}: whether it lies on the path
   * from the root of its tree to {@code descendant}. Equivalent to
   * {@code lowestCommonAncestorOf(node, descendant) == node}.
   * <p>
   * A node is its own ancestor. {@link PreorderAncestry} answers in O(1).
   * <p>
   * <strong>Time:</strong> O(log nodeCount).
   *
   * @param node       node ID in {@code 0..nodeCount-1}
   * @param descendant node ID in {@code 0..nodeCount-1}
   * @return {@code true} if {@code node} is an ancestor of {@code descendant}
   * @throws IndexOutOfBoundsException if {@code node} or {@code descendant} is out of range
   */
  public boolean isAncestorOf(int node, int descendant) {
    return lowestCommonAncestorOf(node, descendant) == node;
  }

  /**
   * Returns the depth of {@code node}: the number of edges from the root of its tree to it. A root
   * has depth {@code 0}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return depth
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
   * @param node node ID in {@code 0..nodeCount-1}
   * @return parent, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int parentOf(int node) {
    Objects.checkIndex(node, parent.length);
    return parent[node];
  }
}
