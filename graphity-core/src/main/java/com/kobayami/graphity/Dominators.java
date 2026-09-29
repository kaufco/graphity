package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immediate dominators of a directed graph with one or more entries.
 * <p>
 * A node {@code d} dominates {@code n} if every path from an entry to {@code n} passes through
 * {@code d}; every node dominates itself. The immediate dominator of {@code n} is its closest
 * strict dominator, and the immediate dominators form the dominator tree. Several entries
 * behave like a virtual root above all of them: a node that only this root dominates - every
 * entry, and every node reachable from two entries without a common real dominator - has no
 * immediate dominator and is a root of the dominator forest.
 * <p>
 * The analysis reads the predecessors of each node and so takes a {@link BiAdjacentGraph}. A
 * caller holding a plain {@link Graph} builds the in-adjacency once, at a visible cost, and can
 * hand the same graph to every other analysis that needs it:
 *
 * <pre>{@code
 * BiAdjacentGraph cfg = graph.biAdjacentGraph();   // +4 x (nodeCount + edgeCount) bytes
 * }</pre>
 *
 * The type is deliberately lean. It holds the immediate dominators, a reachability bit set and
 * the graph, and everything else is built from it on demand:
 *
 * <pre>{@code
 * var dominators = Dominators.of(cfg, entry);
 * Graph tree = Graphs.dominatorTree(dominators);              // edge idom(n) -> n
 * Graph frontiers = Graphs.dominanceFrontiers(dominators);    // edge n -> m, m in DF(n)
 * var index = Ancestry.of(dominators);                        // "a dominates b" in O(log n)
 * boolean dominates = index.isAncestorOf(a, b);
 * }</pre>
 *
 * There is no {@code dominates(a, b)} here: with the immediate dominators alone it would walk up
 * the tree in O(depth), and a long straight-line region makes the depth linear in the graph.
 * {@link Ancestry#of(Dominators)} answers it in O(log nodeCount), {@link PreorderAncestry} in
 * O(1). In that index an unreachable node is an isolated root, so check {@link #isReachable(int)}
 * where that matters.
 * <p>
 * <strong>Post-dominators</strong> are the dominators of the transposed graph, with the exits
 * as entries. The caller chooses the exits; a node from which no exit is reachable - an
 * infinite loop - has no post-dominator unless a virtual exit node with edges from such loops is
 * added before the analysis. Control dependence is the transposed dominance frontier of the
 * post-dominators:
 *
 * <pre>{@code
 * var postDominators = Dominators.of(cfg.transposed(), exits);
 * Graph controlDependence = Graphs.dominanceFrontiers(postDominators).transposed();
 * // edge x -> y: x decides whether y runs
 * }</pre>
 *
 * With {@code cfg} a {@link BiAdjacentGraph}, the transpose is a constant-time view sharing its
 * arrays, and again a {@code BiAdjacentGraph}.
 * <p>
 * <strong>Common ancestors in a DAG.</strong> The deepest node that lies on every path from a
 * root to both {@code a} and {@code b} is their nearest common dominator:
 * {@code Ancestry.of(Dominators.of(dag, roots)).lowestCommonAncestorOf(a, b)}, with {@code -1}
 * if only the virtual root qualifies. On the transpose with the sinks as entries, the same query
 * gives the nearest node that every path from {@code a} and from {@code b} to a sink passes
 * through.
 * <p>
 * <strong>Memory:</strong> 4 x nodeCount bytes plus a bit per node, and a reference to the
 * graph, which stays reachable for as long as the dominators are held.
 *
 * @see Graphs#dominatorTree(Dominators)
 * @see Graphs#dominanceFrontiers(Dominators)
 * @see Ancestry#of(Dominators)
 */
public final class Dominators {

  /** Unlinked marker in the ancestor lane of the path-compression forest. */
  private static final int UNLINKED = -1;

  final BiAdjacentGraph graph;

  /** Immediate dominator of each node, {@code -1} for a root of the forest and when unreachable. */
  final int[] immediateDominator;

  /** Bit set of the nodes reachable from at least one entry. */
  private final long[] reachable;

  private Dominators(BiAdjacentGraph graph, int[] immediateDominator, long[] reachable) {
    this.graph = graph;
    this.immediateDominator = immediateDominator;
    this.reachable = reachable;
  }

  /**
   * Computes the dominators of a graph with a single entry.
   * <p>
   * Equivalent to {@link #of(BiAdjacentGraph, IntList)} with a one-element list.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) x log nodeCount) in the worst case.<br>
   * <strong>Memory:</strong> as for {@link #of(BiAdjacentGraph, IntList)}.
   *
   * @param graph the graph to analyze
   * @param entry the entry node ID in {@code 0..nodeCount()-1}
   * @return the dominators
   * @throws IndexOutOfBoundsException if {@code entry} is out of range
   * @throws IllegalArgumentException if {@code graph.nodeCount() >= 2^30 - 2}
   */
  public static Dominators of(BiAdjacentGraph graph, int entry) {
    return of(graph, IntLists.singleton(entry));
  }

  /**
   * Computes the dominators of a graph with the given entries.
   * <p>
   * Several entries behave like a virtual root above all of them. An empty list leaves every
   * node unreachable; duplicate entries are ignored. The predecessors are read from the
   * in-adjacency of the graph. Construction is iterative and follows Lengauer and Tarjan, in the
   * simple version with path compression: O(m log n) also on the inputs where the iterative
   * algorithm and plain SEMI-NCA turn quadratic.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) x log nodeCount) in the worst case.<br>
   * <strong>Memory:</strong> 4 x nodeCount bytes plus a bit per node for the result; up to
   * 40 x nodeCount bytes additional during computation.
   *
   * @param graph   the graph to analyze
   * @param entries the entry node IDs, each in {@code 0..nodeCount()-1}
   * @return the dominators
   * @throws IndexOutOfBoundsException if an entry is out of range
   * @throws IllegalArgumentException if {@code graph.nodeCount() >= 2^30 - 2}
   */
  public static Dominators of(BiAdjacentGraph graph, IntList entries) {
    Objects.requireNonNull(graph, "graph");
    Objects.requireNonNull(entries, "entries");
    int n = graph.nodeCount();
    var dfs = DfsPreorder.of(graph, entries, "Dominators");
    int[] idom = lengauerTarjan(dfs, graph.inAdjacencyStart, graph.inAdjacency);

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

  /**
   * Immediate dominators by preorder number, {@code 0} for the virtual root. {@code inStart} and
   * {@code in} are the in-adjacency over node IDs, translated per edge into preorder numbers
   * (0 = unreachable). Kept apart from the caller so that the JIT compiles the hot loop on its
   * own.
   */
  private static int[] lengauerTarjan(DfsPreorder dfs, int[] inStart, int[] in) {
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

    // 1) Reverse preorder, Lengauer and Tarjan's simple version: the semidominator from the
    //    predecessors, then link the node to its search parent and settle every node whose
    //    semidominator is that parent, whose subtree is now complete. A settled node gets its
    //    immediate dominator, or a node with the same one, resolved in step 2.
    for (int v = size - 1; v >= 1; v--) {
      int best = v;
      int node = order[v];
      for (int i = inStart[node], end = inStart[node + 1]; i < end; i++) {
        int u = number[in[i]];
        if (u == 0) continue;
        int candidate = u < v ? u : semi[eval(lanes, semi, u)];
        if (candidate < best) best = candidate;
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

  /**
   * The node of least semidominator on the forest path from {@code v} up to, but excluding, its
   * tree root, compressing the path on the way.
   */
  private static int eval(int[] lanes, int[] semi, int v) {
    int ancestor = lanes[2 * v];
    if (ancestor == UNLINKED) return v;
    if (lanes[2 * ancestor] != UNLINKED) compress(lanes, semi, v);
    return lanes[2 * v + 1];
  }

  /**
   * Path compression without recursion and without a stack: the way up reverses the ancestor
   * pointers, the way down restores them pointing at the tree root and carries the best label
   * along.
   */
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
   * Returns the immediate dominator of {@code node}, or {@code -1} if it has none: for an
   * entry, for a node that only the virtual root above several entries dominates, and for an
   * unreachable node.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return the immediate dominator, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int immediateDominatorOf(int node) {
    Objects.checkIndex(node, immediateDominator.length);
    return immediateDominator[node];
  }

  /**
   * Returns whether {@code node} is reachable from at least one entry.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if the node is reachable
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public boolean isReachable(int node) {
    Objects.checkIndex(node, immediateDominator.length);
    return (reachable[node >> 6] & (1L << node)) != 0;
  }
}
