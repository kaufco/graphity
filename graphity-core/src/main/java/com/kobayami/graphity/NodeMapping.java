package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Unidirectional mapping between two node ID spaces.
 * <p>
 * Maps source IDs to target IDs. A target value of {@code -1} indicates that the source ID
 * has no mapping (partial mapping). Use {@link #inverse()} to create the reverse mapping.
 * <p>
 * Factory methods create mappings in a <strong>natural direction</strong> (new → old):
 * <ul>
 *   <li>{@link #forSubgraph} — maps dense new IDs (0..k-1) to original IDs</li>
 *   <li>{@link #forPermutation} — maps new IDs to old IDs ({@code order[newId] = oldId})</li>
 *   <li>{@link #reindex(Graph)} — DFS-preorder reindexing for cache-friendly layouts</li>
 *   <li>{@link #of(int[])} — general-purpose, caller defines direction</li>
 * </ul>
 * Call {@link #inverse()} to obtain the reverse direction.
 * Use {@link Graphs#remapped(Graph, NodeMapping)} to apply a mapping to a graph.
 * Use {@link #compose(NodeMapping)} to chain multiple mappings.
 *
 * <pre>{@code
 * // Reindexing for cache-friendly layout (DFS preorder)
 * Graph reindexed = Graphs.remapped(graph, NodeMapping.reindex(graph));
 *
 * // DAG-specific: topological order reindexing
 * Graph topo = Graphs.remapped(graph, NodeMapping.forPermutation(TopOrder.of(graph)));
 *
 * // Subgraph extraction
 * Graph sub = Graphs.remapped(graph, NodeMapping.forSubgraph(selectedNodes));
 *
 * // Custom permutation
 * Graph permuted = Graphs.remapped(graph, NodeMapping.forPermutation(order));
 *
 * // Chaining: g1 → g2 → g3
 * NodeMapping nm12 = ...;  // g2 → g1
 * NodeMapping nm23 = ...;  // g3 → g2
 * NodeMapping nm13 = nm12.compose(nm23);  // g3 → g1
 * }</pre>
 *
 * <p>Input arrays are not validated at runtime (no duplicate checks, no range checks).
 * Invalid inputs (e.g. duplicate target values in a permutation) lead to undefined behavior.
 * Factory methods contain {@code assert} statements that catch common errors when
 * assertions are enabled ({@code -ea}).
 *
 * @see IndexMapping
 * @see NodePartition
 */
public sealed class NodeMapping extends IndexMapping permits NodePartition {

  /**
   * Package-private constructor. Used by factory methods and subclasses.
   *
   * @param mapping    the mapping array (not copied — caller must not retain a reference)
   * @param validCount number of non-negative entries in {@code mapping}
   * @param partial    whether the mapping contains unmapped entries ({@code -1})
   */
  NodeMapping(int[] mapping, int validCount, boolean partial) {
    super(mapping, validCount, partial);
  }

  // ---- Factory methods ----

  /**
   * Creates a mapping from the given array. The array is copied.
   * <p>
   * {@code mapping[sourceId] = targetId}, or {@code -1} for unmapped source IDs.
   * <p>
   * <strong>Time:</strong> O(n) where n = mapping.length.
   *
   * @param mapping source-to-target mapping
   * @return a new node mapping
   */
  public static NodeMapping of(int[] mapping) {
    int[] copy = Arrays.copyOf(mapping, mapping.length);
    int valid = countValid(copy);
    return new NodeMapping(copy, valid, valid < copy.length);
  }

  /**
   * Creates a subgraph mapping from a {@link BitSet} of selected node IDs.
   * <p>
   * Selected nodes receive dense new IDs in ascending order of their original IDs.
   * The resulting mapping maps dense IDs (0..k-1) to original IDs.
   * <p>
   * <strong>Time:</strong> O(k) where k = number of selected nodes.
   *
   * @param selected bit set where each set bit represents a selected node ID
   * @return mapping from dense IDs to original IDs
   */
  public static NodeMapping forSubgraph(BitSet selected) {
    int k = selected.cardinality();
    int[] map = new int[k];
    int idx = 0;
    for (int bit = selected.nextSetBit(0); bit >= 0; bit = selected.nextSetBit(bit + 1)) {
      map[idx++] = bit;
    }
    return new NodeMapping(map, k, false);
  }

  /**
   * Creates a subgraph mapping from a list of selected node IDs.
   * <p>
   * Selected nodes receive dense new IDs in ascending order of their original IDs.
   * If the list is not already a {@link SortedIntView}, it is sorted internally.
   * The resulting mapping maps dense IDs (0..k-1) to original IDs.
   * <p>
   * <strong>Time:</strong> O(k) if sorted, O(k log k) otherwise.
   *
   * @param selected list of selected node IDs (duplicates lead to undefined behavior)
   * @return mapping from dense IDs to original IDs
   */
  public static NodeMapping forSubgraph(IntList selected) {
    int k = selected.size();
    int[] map = new int[k];
    for (int i = 0; i < k; i++) {
      map[i] = selected.getInt(i);
    }
    if (!(selected instanceof SortedIntView)) {
      Arrays.sort(map);
    }
    assert noDuplicates(map) : "selected contains duplicate node IDs";
    return new NodeMapping(map, k, false);
  }

  /**
   * Creates a permutation mapping from the given order.
   * <p>
   * {@code order[newId] = oldId}. All nodes are preserved (no subset, no unmapped entries).
   * The resulting mapping maps new IDs to old IDs.
   * <p>
   * <strong>Time:</strong> O(n).
   *
   * @param order permutation: {@code order.getInt(newId)} is the old ID
   *              (duplicates lead to undefined behavior)
   * @return mapping from new IDs to old IDs
   */
  public static NodeMapping forPermutation(IntList order) {
    int n = order.size();
    int[] map = new int[n];
    for (int i = 0; i < n; i++) {
      map[i] = order.getInt(i);
    }
    assert isValidPermutation(map) : "order is not a valid permutation (out-of-range or duplicate values)";
    return new NodeMapping(map, n, false);
  }

  // ---- Inverse ----

  /**
   * Creates the inverse mapping (target → source).
   * <p>
   * Each target value in this mapping becomes a source in the inverse, and vice versa.
   * If the inverse is partial (not all target-space IDs are covered), unmapped IDs
   * map to {@code -1}.
   * <p>
   * <strong>Time:</strong> O(n).<br>
   * <strong>Memory:</strong> O(max target ID) for the inverse array.
   *
   * @return the inverse mapping
   * @throws IllegalArgumentException if multiple source IDs map to the same target
   *         (the mapping is not injective and cannot be inverted)
   */
  @Override
  public NodeMapping inverse() {
    int[] inv = computeInverse(mapping);
    int valid = countValid(inv);
    return new NodeMapping(inv, valid, valid < inv.length);
  }

  // ---- Compose ----

  /**
   * Composes this mapping with an inner mapping: {@code result(x) = this(inner(x))}.
   * <p>
   * If {@code this} maps B → C and {@code inner} maps A → B, the result maps A → C.
   * Unmapped entries ({@code -1}) propagate: if {@code inner} maps x to {@code -1},
   * or {@code this} does not map the intermediate value, the result is {@code -1}.
   * <p>
   * <strong>Time:</strong> O(n) where n = inner source space size.
   *
   * @param inner the inner mapping to apply first
   * @return the composed mapping from inner's source space to this mapping's target space
   */
  public NodeMapping compose(NodeMapping inner) {
    int[] comp = computeCompose(this.mapping, inner.mapping);
    int valid = countValid(comp);
    return new NodeMapping(comp, valid, valid < comp.length);
  }

  /**
   * Creates a cache-friendly reindexing mapping via DFS preorder traversal.
   * <p>
   * Nodes visited close together during DFS receive adjacent new IDs, improving cache
   * locality for subsequent traversals. The resulting mapping maps new IDs to old IDs
   * ({@code mapping[newId] = oldId}).
   * <p>
   * For DAGs, topological-order reindexing may be more appropriate:
   * <pre>{@code
   * NodeMapping.forPermutation(TopOrder.of(graph))
   * }</pre>
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to reindex
   * @return permutation mapping from new IDs to old IDs (DFS preorder)
   * @see Graphs#remapped(Graph, NodeMapping)
   */
  public static NodeMapping reindex(Graph graph) {
    int n = graph.nodeCount();
    if (n == 0) {
      return new NodeMapping(new int[0], 0, false);
    }
    var traverser = new ReindexTraverser(graph);
    traverser.run();
    return new NodeMapping(traverser.order, n, false);
  }

  /**
   * DFS traverser that records preorder (= visitation order) for cache-friendly reindexing.
   */
  private static final class ReindexTraverser extends DfsTraverser {

    final int[] order;
    int pos;

    ReindexTraverser(Graph graph) {
      super(graph);
      this.order = new int[graph.nodeCount()];
    }

    @Override
    protected void visitNode(int node) {
      order[pos++] = node;
      visitChildren(node);
    }
  }

  /** Checks that a sorted array contains no consecutive duplicates. */
  private static boolean noDuplicates(int[] sorted) {
    for (int i = 1; i < sorted.length; i++) {
      if (sorted[i] == sorted[i - 1]) return false;
    }
    return true;
  }

  /** Checks that the array is a valid permutation of 0..n-1. */
  private static boolean isValidPermutation(int[] map) {
    int n = map.length;
    var seen = new BitSet(n);
    for (int v : map) {
      if (v < 0 || v >= n || seen.get(v)) return false;
      seen.set(v);
    }
    return true;
  }
}
