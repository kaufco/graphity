package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Objects;

/**
 * The dominators of a graph with one or more entries, such as the control flow of a method,
 * given by the immediate dominator of every node.
 * <p>
 * A node {@code d} dominates a node {@code n} if every path from an entry to {@code n} passes
 * through {@code d}; every node dominates itself. The immediate dominator of {@code n} is the
 * dominator closest to {@code n} other than {@code n} itself. The immediate dominators form the
 * dominator tree, which {@link Graphs#dominatorTree(Dominators)} returns as a graph. Several
 * entries act like one virtual root above all of them. A node that only this virtual root
 * dominates, such as an entry, has no immediate dominator and is a root of the dominator tree,
 * which is then a forest. A node that no entry reaches has no immediate dominator either;
 * {@link #isReachable(int)} tells the two apart.
 * <p>
 * Whether one node dominates another is an ancestor query on the dominator tree.
 * {@link Ancestry#of(Dominators)} answers it in O(log nodeCount) and {@link PreorderAncestry} in
 * O(1). The analysis follows the edges against their direction, so it takes a
 * {@link BiAdjacentGraph}. Here, the dominators of the basic blocks of the method {@code load} from
 * its {@code entry}, and whether {@code body} and {@code then} dominate {@code next}:
 * {@snippet file="com/kobayami/graphity/examples/Dominance.java" region="dominators"}
 * <p>
 * The post-dominators are the dominators of the transposed graph, with the exits as entries. A
 * node {@code p} post-dominates {@code n} if every path from {@code n} to an exit passes through
 * {@code p}. Here, {@code next} is the first block that every path from {@code body} to the
 * {@code exit} of {@code load} passes through:
 * {@snippet file="com/kobayami/graphity/examples/Dominance.java" region="postDominators"}
 * A node from which no exit can be reached, such as one in an endless loop, has no
 * post-dominator, unless an extra exit with an edge from that loop is added to the graph first.
 * <p>
 * Uses <a href="https://en.wikipedia.org/wiki/Dominator_(graph_theory)">the algorithm of
 * Lengauer and Tarjan</a>, without recursion. Dominance frontiers and control dependence are in
 * the module {@code graphity-controlflow}.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x nodeCount bytes and 1 bit per node, plus the graph, which stays
 * reachable as long as the dominators are.
 */
public final class Dominators {

  // Unlinked marker in the ancestor lane of the path-compression forest.
  private static final int UNLINKED = -1;

  private final BiAdjacentGraph graph;

  // Immediate dominator of each node, -1 for a root of the forest and when unreachable. Shared
  // as the parent array by Ancestry.of(Dominators).
  final int[] immediateDominator;

  // Bit set of the nodes reachable from at least one entry.
  private final long[] reachable;

  private Dominators(BiAdjacentGraph graph, int[] immediateDominator, long[] reachable) {
    this.graph = graph;
    this.immediateDominator = immediateDominator;
    this.reachable = reachable;
  }

  /**
   * Returns the dominators of {@code graph} with the single entry {@code entry}. Equivalent to
   * {@link #of(BiAdjacentGraph, IntList) of(graph, IntList.of(entry))}.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) log nodeCount).<br>
   * <strong>Memory:</strong> 4 x nodeCount bytes and 1 bit per node for the result; while it
   * runs, up to 32 x nodeCount bytes more.
   *
   * @param graph the graph
   * @param entry node ID in {@code 0..nodeCount-1}
   * @return the dominators
   * @throws IndexOutOfBoundsException if {@code entry} is out of range
   * @throws IllegalArgumentException if {@code graph} has more than 2<sup>30</sup> - 6 nodes
   */
  public static Dominators of(BiAdjacentGraph graph, int entry) {
    return of(graph, IntLists.singleton(entry));
  }

  /**
   * Returns the dominators of {@code graph} with the given entries.
   * <p>
   * Several entries act like one virtual root above all of them, as the class documentation
   * describes. Their order does not matter. An entry given twice counts once, and with no
   * entries, no node is reachable.
   * <p>
   * <strong>Time:</strong> O(entries.size() + (nodeCount + edgeCount) log nodeCount).<br>
   * <strong>Memory:</strong> 4 x nodeCount bytes and 1 bit per node for the result; while it
   * runs, up to 32 x nodeCount bytes more.
   *
   * @param graph   the graph
   * @param entries node IDs, each in {@code 0..nodeCount-1}
   * @return the dominators
   * @throws IndexOutOfBoundsException if an entry is out of range
   * @throws IllegalArgumentException if {@code graph} has more than 2<sup>30</sup> - 6 nodes
   */
  public static Dominators of(BiAdjacentGraph graph, IntList entries) {
    int n = graph.nodeCount();
    var dfs = DfsPreorder.of(graph, entries, "Dominators");
    int[] idom = lengauerTarjan(dfs, Csr.inAdjacencyStart(graph), Csr.inAdjacency(graph), false);

    // Back to node IDs; the virtual root becomes -1.
    int[] order = dfs.order;
    var immediateDominator = new int[n];
    Arrays.fill(immediateDominator, -1);
    var reachable = new long[(n >> 6) + 1];
    for (int v = 1; v <= dfs.reachedCount; v++) {
      int node = order[v];
      reachable[node >> 6] |= 1L << node;
      if (idom[v] != 0) immediateDominator[node] = order[idom[v]];
    }
    return new Dominators(graph, immediateDominator, reachable);
  }

  // Immediate dominators of each block's induced subgraph, as node IDs, -1 for the entry of a
  // block: StrongConnectivity needs the dominators within each SCC. A block is the set of nodes
  // the search numbers from one entry. No entry may be reached by the search from an earlier one,
  // and every node must be reached. One node of every SCC, sink SCCs first, satisfies this: each
  // search stays inside its SCC, so the blocks are the SCCs and the edges between them are
  // ignored. Throws IllegalArgumentException above DfsPreorder.MAX_NODE_COUNT nodes.
  static int[] withinBlocks(BiAdjacentGraph graph, IntList entries) {
    var dfs = DfsPreorder.of(graph, entries, "Dominators");
    int[] idom = lengauerTarjan(dfs, Csr.inAdjacencyStart(graph), Csr.inAdjacency(graph), true);
    int[] order = dfs.order;
    var immediateDominator = new int[graph.nodeCount()];
    Arrays.fill(immediateDominator, -1);
    for (int v = 1; v <= dfs.reachedCount; v++) {
      if (idom[v] != 0) immediateDominator[order[v]] = order[idom[v]];
    }
    return immediateDominator;
  }

  // Immediate dominators by preorder number, 0 for the virtual root. inStart and in are the
  // in-adjacency over node IDs, translated per edge into preorder numbers (0 = unreachable). Kept
  // apart from the caller so that the JIT compiles the hot loop on its own.
  //
  // Step 1 settles a node once the search path from its semidominator down to it is linked: the
  // node then gets its immediate dominator, or a node with the same one, which step 2 resolves.
  //
  // With withinBlocks, in-nodes outside the block of the node are ignored, as described at
  // withinBlocks. A block is a contiguous range of preorder numbers that starts at its entry, and
  // every in-node in another block comes from a later one.
  private static int[] lengauerTarjan(
      DfsPreorder dfs, int[] inStart, int[] in, boolean withinBlocks) {
    int size = dfs.reachedCount + 1;
    int[] parent = dfs.parent;
    int[] order = dfs.order;
    int[] number = dfs.preorderNumber;
    var semi = new int[size];
    // Two lanes per preorder number, ancestor and label of the path-compression forest: one
    // cache line serves both reads of a compression step.
    var lanes = new int[2 * size];
    // While a node waits in a bucket, its slot links it to the next node of the same bucket.
    var idom = new int[size];
    var bucket = new int[size];
    Arrays.fill(bucket, -1);
    for (int v = 0; v < size; v++) {
      semi[v] = v;
      lanes[2 * v] = UNLINKED;
      lanes[2 * v + 1] = v;
    }

    // 1) Reverse preorder: semidominator, link to the search parent, settle the parent's bucket.
    int blockEnd = size - 1;
    for (int v = size - 1; v >= 1; v--) {
      int best = v;
      int node = order[v];
      if (withinBlocks) {
        for (int i = inStart[node], end = inStart[node + 1]; i < end; i++) {
          int u = number[in[i]];
          if (u > blockEnd) continue;
          int candidate = u < v ? u : semi[eval(lanes, semi, u)];
          if (candidate < best) best = candidate;
        }
        if (dfs.isEntry(v)) blockEnd = v - 1;
      } else {
        for (int i = inStart[node], end = inStart[node + 1]; i < end; i++) {
          int u = number[in[i]];
          if (u == 0) continue;
          int candidate = u < v ? u : semi[eval(lanes, semi, u)];
          if (candidate < best) best = candidate;
        }
      }
      int s = dfs.isEntry(v) ? 0 : best;
      semi[v] = s;
      idom[v] = bucket[s];
      bucket[s] = v;
      int p = parent[v];
      lanes[2 * v] = p;
      for (int w = bucket[p]; w >= 0; ) {
        int next = idom[w];
        int u = eval(lanes, semi, w);
        idom[w] = semi[u] < semi[w] ? u : p;
        w = next;
      }
      bucket[p] = -1;
    }

    // 2) Preorder: a node settled with a deferred dominator takes that node's, already final.
    for (int v = 1; v < size; v++) {
      if (idom[v] != semi[v]) idom[v] = idom[idom[v]];
    }
    return idom;
  }

  // The node of least semidominator on the forest path from v up to, but excluding, its tree
  // root, compressing the path on the way.
  private static int eval(int[] lanes, int[] semi, int v) {
    int ancestor = lanes[2 * v];
    if (ancestor == UNLINKED) return v;
    if (lanes[2 * ancestor] != UNLINKED) compress(lanes, semi, v);
    return lanes[2 * v + 1];
  }

  // Path compression without recursion and without a stack: the way up reverses the ancestor
  // pointers, the way down restores them pointing at the tree root and carries the best label
  // along.
  private static void compress(int[] lanes, int[] semi, int v) {
    int below = UNLINKED;
    int x = v;
    while (lanes[2 * lanes[2 * x]] != UNLINKED) {
      int up = lanes[2 * x];
      lanes[2 * x] = below;
      below = x;
      x = up;
    }
    while (below != UNLINKED) {
      int y = below;
      below = lanes[2 * y];
      int xLabel = lanes[2 * x + 1];
      if (semi[xLabel] < semi[lanes[2 * y + 1]]) lanes[2 * y + 1] = xLabel;
      lanes[2 * y] = lanes[2 * x];
      x = y;
    }
  }

  /**
   * Returns the graph these dominators were computed on, for analyses that need both, such as
   * dominance frontiers.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the graph
   */
  public BiAdjacentGraph graph() {
    return graph;
  }

  /**
   * Returns the immediate dominator of {@code node}, or {@code -1} if it has none.
   * <p>
   * A node has none if it is an entry, if only the virtual root above several entries dominates
   * it, or if no entry reaches it.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return immediate dominator, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int immediateDominatorOf(int node) {
    Objects.checkIndex(node, immediateDominator.length);
    return immediateDominator[node];
  }

  /**
   * Returns whether {@code node} is reachable from an entry.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return {@code true} if a path leads from an entry to {@code node}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public boolean isReachable(int node) {
    Objects.checkIndex(node, immediateDominator.length);
    return (reachable[node >> 6] & (1L << node)) != 0;
  }
}
