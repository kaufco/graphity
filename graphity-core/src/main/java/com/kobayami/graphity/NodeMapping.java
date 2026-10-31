package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.BitSet;
import org.jspecify.annotations.Nullable;

/**
 * A mapping from the node IDs of one graph to those of another, to extract a subgraph or to
 * renumber the nodes of a graph.
 * <p>
 * {@link #get(int) get(node)} is the node ID that {@code node} maps to, or {@code -1} if
 * {@code node} is not mapped. The factories return mappings from a new graph to an old one. These
 * map each new node to the node it comes from:
 * <ul>
 *   <li>{@link #forSubgraph(IntList)} and its overloads - the selected nodes, numbered in
 *       ascending order;</li>
 *   <li>{@link #forPermutation(IntList)} - every node, in an order of the caller's choice;</li>
 *   <li>{@link #reindex(Graph)} - every node, in depth-first preorder;</li>
 *   <li>{@link #of(int[])} - any mapping, from an array.</li>
 * </ul>
 * {@link Graphs#remapped(Graph, NodeMapping)} builds the new graph from the old one, and
 * {@code gather} carries data on the old nodes, such as their names, over to the new ones. Here,
 * {@code calls} holds the calls between the methods of a program, and the methods that
 * {@code load} reaches become a graph of their own, with their names from {@code names}:
 * {@snippet file="com/kobayami/graphity/examples/Mappings.java" region="subgraph"}
 * {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)} derives the {@link EdgeMapping} that
 * carries data on edges. {@link #inverted(int)} returns the mapping from old to new, and
 * {@link #compose(NodeMapping)} chains two mappings.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x indexCount bytes.
 */
public final class NodeMapping extends IndexMapping {

  // Takes the array as it is, not copied: the caller hands over an array that nobody else holds,
  // and validCount matches it.
  NodeMapping(int[] mapping, int validCount) {
    super(mapping, validCount);
  }

  /**
   * Returns the mapping that maps each node {@code i} to {@code mapping[i]}, and leaves it
   * unmapped where that is {@code -1}.
   * <p>
   * Use it for mappings that the other factories do not cover, from new to old or from old to new.
   * The array is copied. Each value must be {@code -1} or a node ID, and no two nodes may map to
   * the same node; without a graph, this method can only reject a value below {@code -1}.
   * {@link Graphs#remapped(Graph, NodeMapping)} rejects a value that is not a node of its graph,
   * and both it and {@link #inverted(int)} reject two nodes that map to the same node.
   * <p>
   * <strong>Time:</strong> O(mapping.length).<br>
   * <strong>Memory:</strong> 4 x mapping.length bytes for the copy, which the result keeps.
   *
   * @param mapping the node ID that each node maps to, or {@code -1}
   * @return the mapping
   * @throws IndexOutOfBoundsException if a value is less than {@code -1}
   */
  public static NodeMapping of(int[] mapping) {
    int[] copy = Arrays.copyOf(mapping, mapping.length);
    for (int node = 0; node < copy.length; node++) {
      if (copy[node] < -1) {
        throw new IndexOutOfBoundsException(
            "a mapping value must be -1 or a node ID, but node " + node + " maps to " + copy[node]);
      }
    }
    return new NodeMapping(copy, countValid(copy));
  }

  /**
   * Returns a mapping from the nodes of a subgraph to the selected nodes, given as the set bits
   * of {@code selected}.
   * <p>
   * The selected nodes get the new node IDs {@code 0..selected.cardinality()-1}, in ascending
   * order of their node IDs. The selected node IDs are checked against a graph only when the
   * mapping is used: {@link Graphs#remapped(Graph, NodeMapping)} rejects one that is not a node of
   * its graph.
   * <p>
   * <strong>Time:</strong> O(selected.length()).<br>
   * <strong>Memory:</strong> 4 x selected.cardinality() bytes for the result.
   *
   * @param selected the selected node IDs
   * @return the mapping from new to old node IDs
   */
  public static NodeMapping forSubgraph(BitSet selected) {
    int k = selected.cardinality();
    int[] map = new int[k];
    int idx = 0;
    for (int bit = selected.nextSetBit(0); bit >= 0; bit = selected.nextSetBit(bit + 1)) {
      map[idx++] = bit;
    }
    return new NodeMapping(map, k);
  }

  /**
   * Returns a mapping from the nodes of a subgraph to the selected nodes, given as a list.
   * <p>
   * The selected nodes get the new node IDs {@code 0..selected.size()-1}, in ascending order of
   * their node IDs, whatever the order of the list. A {@link SortedIntView}, as
   * {@link Graphs#reachableFrom(Graph, int)} and {@link NodePartition#nodesOf(int)} return it, is
   * taken as it is; any other list is sorted first, in a copy. As with
   * {@link #forSubgraph(BitSet)}, the node IDs are checked against a graph only when the mapping
   * is used.
   * <p>
   * <strong>Time:</strong> O(k) for a {@link SortedIntView}, O(k log k) for any other list, where
   * k is {@code selected.size()}.<br>
   * <strong>Memory:</strong> 4 x selected.size() bytes for the result.
   *
   * @param selected the selected node IDs
   * @return the mapping from new to old node IDs
   * @throws IndexOutOfBoundsException if a node ID is negative
   * @throws IllegalArgumentException if a node ID occurs more than once
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
    checkSelection(map);
    return new NodeMapping(map, k);
  }

  /**
   * Returns a mapping from the nodes of a subgraph to the selected nodes, given as node IDs.
   * <p>
   * The selected nodes get the new node IDs {@code 0..selected.length-1}, in ascending order of
   * their node IDs, whatever the order of the arguments; the array is sorted in a copy. As with
   * {@link #forSubgraph(BitSet)}, the node IDs are checked against a graph only when the mapping
   * is used.
   * <p>
   * <strong>Time:</strong> O(k log k), where k is {@code selected.length}.<br>
   * <strong>Memory:</strong> 4 x selected.length bytes for the result.
   *
   * @param selected the selected node IDs
   * @return the mapping from new to old node IDs
   * @throws IndexOutOfBoundsException if a node ID is negative
   * @throws IllegalArgumentException if a node ID occurs more than once
   */
  public static NodeMapping forSubgraph(int... selected) {
    int[] map = Arrays.copyOf(selected, selected.length);
    Arrays.sort(map);
    checkSelection(map);
    return new NodeMapping(map, map.length);
  }

  /**
   * Returns a mapping that numbers the nodes in the given order, from new to old node IDs.
   * <p>
   * {@code order.getInt(newNode)} is the node that gets the new node ID {@code newNode}.
   * {@code order} holds every node ID in {@code 0..order.size()-1} once, so the mapping maps every
   * node. For example, {@code NodeMapping.forPermutation(TopOrder.of(dag))} numbers the
   * nodes of a DAG (directed acyclic graph) in topological order, so that every edge of the
   * remapped graph goes from a lower to a higher node ID.
   * <p>
   * <strong>Time:</strong> O(order.size()).<br>
   * <strong>Memory:</strong> 4 x order.size() bytes for the result; while it runs, 1 bit per
   * node.
   *
   * @param order the node IDs in their new order
   * @return the mapping from new to old node IDs
   * @throws IndexOutOfBoundsException if a node ID is not in {@code 0..order.size()-1}
   * @throws IllegalArgumentException if a node ID occurs more than once
   */
  public static NodeMapping forPermutation(IntList order) {
    int n = order.size();
    int[] map = new int[n];
    var seen = new BitSet(n);
    for (int i = 0; i < n; i++) {
      int old = order.getInt(i);
      if (old < 0 || old >= n) {
        throw new IndexOutOfBoundsException(
            "order value out of range 0.." + (n - 1) + " at position " + i + ": " + old);
      }
      if (seen.get(old)) {
        throw new IllegalArgumentException("order contains " + old + " more than once");
      }
      seen.set(old);
      map[i] = old;
    }
    return new NodeMapping(map, n);
  }

  /**
   * Returns the inverse mapping, which maps {@code get(node)} back to {@code node}.
   * <p>
   * The inverse maps the {@code targetCount} nodes of the graph that this mapping maps to; a node
   * that no node maps to is not mapped, and the inverse is then partial. For a mapping from new
   * to old node IDs, as the factories return it, {@code targetCount} is the node count of the old
   * graph, and the inverse gives the new node ID of an old node:
   * {@code newToOld.inverted(oldGraph.nodeCount()).get(oldNode)}.
   * <p>
   * <strong>Time:</strong> O(indexCount + targetCount).<br>
   * <strong>Memory:</strong> 4 x targetCount bytes for the result.
   *
   * @param targetCount the node count of the graph that this mapping maps to
   * @return the inverse mapping, with an index count of {@code targetCount}
   * @throws IllegalArgumentException if {@code targetCount} is negative or larger than
   *                                  {@code Integer.MAX_VALUE - 8}, or two nodes map to the same
   *                                  node
   * @throws IndexOutOfBoundsException if a node maps to {@code targetCount} or more
   */
  public NodeMapping inverted(int targetCount) {
    return new NodeMapping(invertArray(mapping, targetCount), mappedCount());
  }

  /**
   * Returns the mapping that applies {@code inner} first and then this mapping.
   * <p>
   * If {@code inner} maps the nodes of a graph A to those of B, and this mapping maps B to C, the
   * result maps A to C. A node is not mapped in the result if {@code inner} does not map it, or if
   * this mapping does not map the node that {@code inner} maps it to. For mappings from new to old
   * node IDs, {@code firstToOriginal.compose(secondToFirst)} maps the nodes of a graph remapped
   * twice back to those of the original one.
   * <p>
   * <strong>Time:</strong> O(inner.indexCount()).<br>
   * <strong>Memory:</strong> 4 x inner.indexCount() bytes for the result.
   *
   * @param inner the mapping to apply first
   * @return the composed mapping
   * @throws IndexOutOfBoundsException if {@code inner} maps a node to a node ID outside
   *                                   {@code 0..indexCount()-1}
   */
  public NodeMapping compose(NodeMapping inner) {
    int[] composed = composeArrays(this.mapping, inner.mapping);
    return new NodeMapping(composed, countValid(composed));
  }

  /**
   * Returns {@code values} renumbered by this mapping: the value at position {@code i} of the
   * result is {@code values[get(i)]}, or {@code 0} if {@code i} is not mapped.
   * <p>
   * For a mapping from new to old node IDs, as the factories return it, this carries data on the
   * nodes of the old graph over to the new graph. {@link #gather(Object[], Object[])} does the
   * same for objects, such as names.
   * <p>
   * <strong>Time:</strong> O(indexCount).<br>
   * <strong>Memory:</strong> 4 x indexCount bytes for the result.
   *
   * @param values the value of each node of the graph that this mapping maps to
   * @return the renumbered values, of length {@link #indexCount()}
   * @throws IndexOutOfBoundsException if a node maps to a node ID outside {@code values}
   */
  public int[] gather(int[] values) {
    return gatherArray(mapping, values);
  }

  /**
   * Returns {@code values} renumbered by this mapping, as {@link #gather(int[])} does.
   * <p>
   * <strong>Time:</strong> O(indexCount).<br>
   * <strong>Memory:</strong> 8 x indexCount bytes for the result.
   *
   * @param values the value of each node of the graph that this mapping maps to
   * @return the renumbered values, of length {@link #indexCount()}
   * @throws IndexOutOfBoundsException if a node maps to a node ID outside {@code values}
   */
  public long[] gather(long[] values) {
    return gatherArray(mapping, values);
  }

  /**
   * Returns {@code values} renumbered by this mapping, as {@link #gather(int[])} does.
   * <p>
   * <strong>Time:</strong> O(indexCount).<br>
   * <strong>Memory:</strong> 8 x indexCount bytes for the result.
   *
   * @param values the value of each node of the graph that this mapping maps to
   * @return the renumbered values, of length {@link #indexCount()}
   * @throws IndexOutOfBoundsException if a node maps to a node ID outside {@code values}
   */
  public double[] gather(double[] values) {
    return gatherArray(mapping, values);
  }

  /**
   * Writes {@code values} renumbered by this mapping into {@code newValues}, for objects such as
   * names.
   * <p>
   * {@code newValues[i]} becomes {@code values[get(i)]}, and stays as it is if {@code i} is not
   * mapped. References are copied, not the objects. If an exception is thrown, {@code newValues}
   * may be partly written.
   * <p>
   * <strong>Time:</strong> O(indexCount).
   *
   * @param <T>       the element type, nullable or not
   * @param values    the value of each node of the graph that this mapping maps to
   * @param newValues receives the renumbered values; length {@link #indexCount()}, and not the
   *                  same array as {@code values}
   * @throws IllegalArgumentException if {@code newValues.length != indexCount()}, or both are the
   *                                  same array
   * @throws IndexOutOfBoundsException if a node maps to a node ID outside {@code values}
   */
  public <T extends @Nullable Object> void gather(T[] values, T[] newValues) {
    gatherArray(mapping, values, newValues);
  }

  /**
   * Returns a mapping that numbers the nodes of {@code graph} in depth-first preorder, from new to
   * old node IDs.
   * <p>
   * The search starts at node {@code 0}, follows the out-nodes of each node in ascending order,
   * and starts again at the smallest node not yet visited until it has visited every node; each
   * node gets the next new node ID when the search reaches it. Nodes that the search visits one
   * after another thus get adjacent node IDs. Traversals of the remapped graph then read its
   * arrays more in order, which can make them faster on large graphs.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x nodeCount bytes for the result; while it runs, 1 bit per node
   * and up to 8 x nodeCount bytes for the stack, which grows with the deepest path.
   *
   * @param graph the graph whose nodes are numbered
   * @return the mapping from new to old node IDs
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   */
  public static NodeMapping reindex(Graph graph) {
    var traverser = new ReindexTraverser(graph);
    traverser.run();
    return new NodeMapping(traverser.order, graph.nodeCount());
  }

  // Depth-first search with an explicit stack instead of recursion, recording the preorder.
  private static final class ReindexTraverser {

    private final Graph graph;

    // Visited bits, one per node. A raw long[] rather than a BitSet, so that the inner scan can
    // test and set a node's bit with a single word load and store instead of two independent
    // get/set calls.
    private final long[] visited;

    final int[] order;
    int pos;

    ReindexTraverser(Graph graph) {
      int n = graph.nodeCount();
      this.graph = graph;
      this.visited = new long[(n >> 6) + 1];
      this.order = new int[n];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = Csr.outAdjacencyStart(graph);
      int[] adj = Csr.outAdjacency(graph);
      long[] visited = this.visited;
      // Each frame is two interleaved ints, the node and its resume position in the
      // adjacency array: both share a cache line, and the array grows on demand.
      int[] frames = DfsFrames.initial(n);

      for (int root = nextClearBit(visited, 0); root < n; root = nextClearBit(visited, root + 1)) {
        visited[root >>> 6] |= 1L << root;
        order[pos++] = root;
        frames[0] = root;
        frames[1] = out[root];
        int top = 0;

        outer:
        while (top >= 0) {
          int u = frames[2 * top];
          int end = out[u + 1];
          // Tight neighbor scan: i and end stay in registers; the frame list is
          // touched only when we actually descend, not once per edge.
          int i = frames[2 * top + 1];
          while (i < end) {
            int v = adj[i++];
            int w = v >>> 6;
            long bits = visited[w];
            long mask = 1L << v;
            if ((bits & mask) == 0) {
              visited[w] = bits | mask;
              order[pos++] = v;
              frames[2 * top + 1] = i;
              if (++top == frames.length >> 1) {
                frames = DfsFrames.grow(frames, n);
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            }
          }
          top--;
        }
      }
    }

    // BitSet.nextClearBit on the raw words: words of set bits are skipped 64 at a time, and a
    // result beyond the node count is left for the caller's bound check to reject.
    private static int nextClearBit(long[] bits, int from) {
      int word = from >>> 6;
      long candidates = ~bits[word] & (-1L << from);
      while (candidates == 0) {
        if (++word == bits.length) {
          return bits.length << 6;
        }
        candidates = ~bits[word];
      }
      return (word << 6) + Long.numberOfTrailingZeros(candidates);
    }
  }

  // Rejects a repeated ID in a sorted selection, and then a negative first ID, the only place one
  // can be.
  private static void checkSelection(int[] sorted) {
    for (int i = 1; i < sorted.length; i++) {
      if (sorted[i] == sorted[i - 1]) {
        throw new IllegalArgumentException("selected contains node " + sorted[i] + " more than once");
      }
    }
    if (sorted.length > 0 && sorted[0] < 0) {
      throw new IndexOutOfBoundsException("selected node ID must be non-negative: " + sorted[0]);
    }
  }
}
