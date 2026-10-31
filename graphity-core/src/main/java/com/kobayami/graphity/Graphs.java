package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongArrays;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

/**
 * Utility methods for graph transformations and shape checks.
 * <p>
 * Provides operations that create new {@link Graph} instances from existing graphs
 * and {@link NodeMapping}s (the original graph is never modified), predicate methods to test
 * structural properties of a graph - with {@link #findCycle(Graph)} as the witness for
 * {@link #isDag(Graph)} -, its complete text form, {@link #adjacencyListOf(Graph)}, a structural
 * hash, {@link #topologyHash(Graph)}, and the {@link #diff(Graph, Graph)} of two versions.
 * <p>
 * A predicate whose name does not say otherwise reads the graph as directed; the ones prefixed
 * {@code undirected} read it as {@link Components#ccsOf(Graph)} does. {@code isBipartite} and
 * {@code isWeaklyConnected} carry no prefix because neither property has a directed reading that
 * could be meant instead; {@code isStronglyConnected} is directed by definition.
 *
 * <pre>{@code
 * // Shape checks, directed reading
 * Graphs.isDag(graph);              // acyclic?
 * Graphs.isForest(graph);           // DAG with all inDegrees <= 1?
 * Graphs.isTree(graph);             // forest with exactly one root?
 * Graphs.isPath(graph);             // tree with all outDegrees <= 1?
 * Graphs.isStronglyConnected(graph); // every node reaches every other?
 *
 * // Shape checks, undirected reading
 * Graphs.isBipartite(graph);        // nodes split into two classes?
 * Graphs.isUndirectedTree(graph);   // connected and acyclic?
 * Graphs.isUndirectedForest(graph); // acyclic?
 * Graphs.isWeaklyConnected(graph);  // one component?
 *
 * // Reindexing for cache-friendly layout
 * NodeMapping newToOld = NodeMapping.reindex(graph);
 * Graph reindexed = Graphs.remapped(graph, newToOld);
 *
 * // Subgraph extraction
 * Graph sub = Graphs.remapped(graph, NodeMapping.forSubgraph(selectedNodes));
 * Graph reached = Graphs.remapped(graph, NodeMapping.forSubgraph(Graphs.reachableFrom(graph, entry)));
 *
 * // Custom permutation
 * Graph permuted = Graphs.remapped(graph, NodeMapping.forPermutation(order));
 *
 * // Edge metadata on a transpose
 * EdgeMapping transposedToOld = Graphs.transposedEdgeMapping(graph);
 * }</pre>
 *
 * @see NodeMapping
 * @see NodePartition
 * @see EdgeMapping
 * @see EdgePartition
 * @see Graph
 */
public final class Graphs {

  // The constants of topologyHash. Changing one of them, or a shift or rotation distance in the
  // code, changes the hash of every graph, which a release must announce in its changelog.

  /** Seed: the fractional part of the square root of 2, scaled to 64 bits and rounded. */
  private static final long HASH_SEED = 0x6A09E667F3BCC909L;

  /** Spreads each input over all 64 bits: 2^64 / golden ratio, rounded down. */
  private static final long HASH_GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

  /** Multiplier of each step: PRIME64_2 of xxHash64. */
  private static final long HASH_STEP_PRIME = 0xC2B2AE3D27D4EB4FL;

  /**
   * The two multipliers of the SplitMix64 finalizer, as in {@code java.util.SplittableRandom}
   * (Steele, Lea and Flood, 2014).
   */
  private static final long SPLITMIX64_MULTIPLIER_1 = 0xBF58476D1CE4E5B9L;

  private static final long SPLITMIX64_MULTIPLIER_2 = 0x94D049BB133111EBL;

  /**
   * Characters the {@link #adjacencyListOf(Graph)} builder reserves per node: a line without its
   * targets, {@code "1234567 -> []\n"}, and some slack. An estimate for the initial capacity; the
   * builder grows past it if needed.
   */
  private static final int LIST_CHARS_PER_NODE = 16;

  /** Characters the same builder reserves per target, {@code ", 123456"}. */
  private static final int LIST_CHARS_PER_EDGE = 8;

  private Graphs() {}

  /**
   * Creates a new graph by remapping node IDs according to the given mapping.
   * <p>
   * The mapping must be in the <strong>natural direction</strong> (new → old), as produced
   * by all {@link NodeMapping} factory methods. Internally, the inverse (old → new) is
   * computed in O(n) for edge-target translation.
   * <p>
   * For <strong>permutations</strong> (reorder), all edges are preserved with remapped IDs.
   * For <strong>subgraph</strong> mappings, edges to nodes not in the subgraph are dropped.
   * Adjacency lists in the resulting graph are sorted ascending.
   * <p>
   * The parameter type is {@link NodeMapping}, <strong>not</strong> a {@link NodePartition}:
   * a partition is many-to-one, so it cannot be inverted and is not a {@code NodeMapping}.
   * Passing one is therefore a compile error, not a runtime failure.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + Σ newDegree x log(newDegree)).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result;
   * O(nodeCount) additional during computation (inverse mapping).
   *
   * @param graph    the original graph
   * @param newToOld mapping from new node IDs to original node IDs
   * @return a new graph with remapped node IDs
   * @throws IllegalArgumentException if a new node is unmapped, or two new nodes map to the same
   *         original node
   * @throws IndexOutOfBoundsException if a new node maps to an ID that is not a node of
   *         {@code graph}
   * @see NodeMapping#reindex(Graph)
   * @see NodeMapping#forSubgraph(java.util.BitSet)
   * @see NodeMapping#forPermutation(IntList)
   */
  public static Graph remapped(Graph graph, NodeMapping newToOld) {
    int newNodeCount = newToOld.mapping.length;
    // The general path gives the same, but allocates the inverse over every old node first.
    if (newNodeCount == 0) {
      return new Graph(new int[]{0}, new int[0]);
    }

    // Inverse (old → new) for edge-target translation, built in the same pass that checks every
    // new node. Not IndexMapping.invertArray: remapped rejects an unmapped new node, which an
    // inverse skips, and names both new nodes of a repeat.
    int[] oldOf = newToOld.mapping;
    int oldNodeCount = graph.nodeCount();
    int[] newOf = new int[oldNodeCount];
    Arrays.fill(newOf, -1);
    for (int newNode = 0; newNode < newNodeCount; newNode++) {
      int oldNode = oldOf[newNode];
      if (oldNode == -1) {
        throw new IllegalArgumentException("new node " + newNode + " is unmapped");
      }
      // Every NodeMapping holds -1 or a non-negative ID, so the upper bound is all that is left.
      if (oldNode >= oldNodeCount) {
        throw notANode(newNode, oldNode, oldNodeCount);
      }
      if (newOf[oldNode] != -1) {
        throw new IllegalArgumentException("new nodes " + newOf[oldNode] + " and " + newNode
            + " both map to node " + oldNode);
      }
      newOf[oldNode] = newNode;
    }

    var oldTargets = Csr.outAdjacency(graph);
    var oldStart = Csr.outAdjacencyStart(graph);

    // The checks above make the mapping injective into the old nodes, so equal counts make it a
    // bijection: no edge is dropped, and the degree count becomes offset arithmetic.
    boolean total = newNodeCount == oldNodeCount;

    // 1) Out-degree of every new node
    var newStart = new int[newNodeCount + 1];
    if (total) {
      for (int newNode = 0; newNode < newNodeCount; newNode++) {
        int oldNode = oldOf[newNode];
        newStart[newNode + 1] = oldStart[oldNode + 1] - oldStart[oldNode];
      }
    } else {
      for (int newNode = 0; newNode < newNodeCount; newNode++) {
        int oldNode = oldOf[newNode];
        int end = oldStart[oldNode + 1];
        int count = 0;
        for (int i = oldStart[oldNode]; i < end; i++) {
          int oldTarget = oldTargets[i];
          if (newOf[oldTarget] >= 0) {
            count++;
          }
        }
        newStart[newNode + 1] = count;
      }
    }

    // 2) Prefix sum: degrees to start offsets
    for (int newNode = 0; newNode < newNodeCount; newNode++) {
      newStart[newNode + 1] += newStart[newNode];
    }
    int totalEdges = newStart[newNodeCount];

    // 3) Targets, each list sorted
    var newTargets = new int[totalEdges];
    if (total) {
      for (int newNode = 0; newNode < newNodeCount; newNode++) {
        int writePos = newStart[newNode];
        int oldNode = oldOf[newNode];
        int end = oldStart[oldNode + 1];
        for (int i = oldStart[oldNode]; i < end; i++) {
          newTargets[writePos++] = newOf[oldTargets[i]];
        }
        Arrays.sort(newTargets, newStart[newNode], writePos);
      }
    } else {
      for (int newNode = 0; newNode < newNodeCount; newNode++) {
        int writePos = newStart[newNode];
        int oldNode = oldOf[newNode];
        int end = oldStart[oldNode + 1];
        for (int i = oldStart[oldNode]; i < end; i++) {
          int oldTarget = oldTargets[i];
          if (newOf[oldTarget] >= 0) {
            newTargets[writePos++] = newOf[oldTarget];
          }
        }
        Arrays.sort(newTargets, newStart[newNode], newStart[newNode + 1]);
      }
    }

    return new Graph(newStart, newTargets);
  }

  private static IndexOutOfBoundsException notANode(int newNode, int oldNode, int oldNodeCount) {
    return new IndexOutOfBoundsException("new node " + newNode + " maps to " + oldNode
        + ", which is not a node of the old graph (nodeCount=" + oldNodeCount + ")");
  }

  /**
   * Returns the dominator tree as a graph: an edge {@code idom(n) -> n} for every node that has
   * an immediate dominator.
   * <p>
   * Every root of the dominator forest - each entry, and each node only the virtual root above
   * several entries dominates - has in-degree 0, and so has every unreachable node. The result
   * is a forest of out-trees, ready for a traversal such as SSA renaming — assigning each
   * use its reaching definition — with {@link DfsTraverser#enterNode(int)} and
   * {@code leaveNode(int)}. For ancestor
   * queries, {@link Ancestry#of(Dominators)} is cheaper than {@code Ancestry.of(dominatorTree(d))}:
   * it shares the immediate dominators instead of building and checking a graph.
   * <p>
   * Computed on every call, not cached - keep the result.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1) bytes plus 4 bytes per tree edge for the result.
   *
   * @param dominators the dominators to arrange as a tree
   * @return the dominator tree, with the node IDs of the analyzed graph
   */
  public static Graph dominatorTree(Dominators dominators) {
    var children = ParentForest.childrenOf(dominators.immediateDominator);
    return new Graph(children.start(), children.members());
  }

  /**
   * Returns the transitive closure: an edge {@code u -> v} whenever {@code v} is reachable from
   * {@code u} by a non-empty path. A node has an edge to itself exactly if it lies on a cycle.
   * <p>
   * Computed on one bit row per strongly connected component over the condensation, sinks first,
   * so the work is about sccCount x condensationEdgeCount / 64 word operations, and the rows
   * hold sccCount<sup>2</sup> / 8 bytes while it runs - about 1.25 GB for 10<sup>5</sup>
   * components. The result can have up to nodeCount<sup>2</sup> edges, 4 bytes each. For the
   * nodes reachable from one node, {@link #reachableFrom(Graph, int)} is far cheaper.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + sccCount x (sccCount + condensationEdgeCount)
   * / 64 + resultEdgeCount x log nodeCount) - the rows are allocated and scanned whole, and the
   * logarithm sorts each component's targets once.<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + resultEdgeCount) bytes for the result;
   * sccCount<sup>2</sup> / 8 bytes for the rows plus O(nodeCount + edgeCount) during
   * computation, rows and result side by side at the peak.
   *
   * @param graph the graph to close
   * @return the transitive closure, over the same nodes
   * @throws IllegalArgumentException if the closure has more than {@code Integer.MAX_VALUE - 8}
   *                                  edges, or {@code graph.nodeCount() > 2^30 - 5}, the limit of
   *                                  {@link Components#sccsOf(Graph)}
   */
  public static Graph transitiveClosure(Graph graph) {
    int n = graph.nodeCount();
    var sccs = Components.sccsOf(graph);
    int k = sccs.sccCount();
    Graph dag = sccs.condense();
    int[] dagStart = Csr.outAdjacencyStart(dag);
    int[] dagTargets = Csr.outAdjacency(dag);
    int words = (k + 63) >>> 6;

    // SCC IDs are in reverse topological order: every successor of c has a smaller ID.
    var reach = new long[k][];
    var reachSize = new int[k];
    long total = 0;
    for (int c = 0; c < k; c++) {
      var row = new long[words];
      if (sccs.containsCycle(c)) row[c >>> 6] |= 1L << c;
      for (int e = dagStart[c], end = dagStart[c + 1]; e < end; e++) {
        int d = dagTargets[e];
        row[d >>> 6] |= 1L << d;
        long[] below = reach[d];
        for (int w = 0; w < words; w++) row[w] |= below[w];
      }
      reach[c] = row;
      long size = 0;
      for (int w = 0; w < words; w++) {
        for (long bits = row[w]; bits != 0; bits &= bits - 1) {
          size += sccs.sizeOf((w << 6) + Long.numberOfTrailingZeros(bits));
        }
      }
      reachSize[c] = (int) size;
      total += size * sccs.sizeOf(c);
      if (total > ArrayLimits.MAX_ARRAY_LENGTH) {
        throw new IllegalArgumentException("the transitive closure has more than "
            + ArrayLimits.MAX_ARRAY_LENGTH + " edges, the largest array length");
      }
    }

    // Each component's sorted target list is written once, at its first node, and copied from
    // there to its other nodes; its bit row is dropped as soon as the list exists.
    var start = new int[n + 1];
    for (int v = 0; v < n; v++) start[v + 1] = start[v] + reachSize[sccs.sccOf(v)];
    var targets = new int[(int) total];
    for (int c = 0; c < k; c++) {
      long[] row = reach[c];
      var nodes = sccs.nodesOf(c);
      int first = start[nodes.getInt(0)];
      int at = first;
      for (int w = 0; w < words; w++) {
        for (long bits = row[w]; bits != 0; bits &= bits - 1) {
          var members = sccs.nodesOf((w << 6) + Long.numberOfTrailingZeros(bits));
          for (int i = 0; i < members.size(); i++) targets[at++] = members.getInt(i);
        }
      }
      Arrays.sort(targets, first, at);
      for (int i = 1; i < nodes.size(); i++) {
        System.arraycopy(targets, first, targets, start[nodes.getInt(i)], at - first);
      }
      reach[c] = LongArrays.EMPTY_ARRAY;
    }
    return new Graph(start, targets);
  }

  /**
   * Returns the transitive reduction of a DAG: the edges not implied by a longer path. The result
   * has the same reachability as the input with as few edges as possible, and on a DAG it is
   * unique. For a graph with cycles, reduce its condensation, {@link Sccs#condense()}.
   * <p>
   * Visits each node's out-nodes in topological order and keeps an edge only if no earlier
   * out-node already reaches its target, on one bit row per node, sinks first: about
   * edgeCount x nodeCount / 64 word operations, and nodeCount<sup>2</sup> / 8 bytes for the rows
   * while it runs - about 1.25 GB for 10<sup>5</sup> nodes.
   * <p>
   * <strong>Time:</strong> O(nodeCount + (nodeCount + edgeCount) x nodeCount / 64 +
   * edgeCount x log maxOutDegree) - the rows are allocated whole.<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + resultEdgeCount) bytes for the result;
   * nodeCount<sup>2</sup> / 8 bytes plus O(nodeCount + edgeCount) during computation.
   *
   * @param dag the DAG to reduce
   * @return the transitive reduction, over the same nodes
   * @throws IllegalArgumentException if the graph has a cycle, a self-loop included
   */
  public static Graph transitiveReduction(Graph dag) {
    int n = dag.nodeCount();
    IntList order = TopOrder.of(dag);
    var position = new int[n];
    for (int i = 0; i < n; i++) position[order.getInt(i)] = i;
    int[] start = Csr.outAdjacencyStart(dag);
    int[] targets = Csr.outAdjacency(dag);
    int words = (n + 63) >>> 6;
    var reach = new long[n][];
    var keep = new boolean[targets.length];
    int kept = 0;
    var byPosition = new long[16];

    for (int i = n - 1; i >= 0; i--) {
      int u = order.getInt(i);
      int from = start[u];
      int degree = start[u + 1] - from;
      if (byPosition.length < degree) {
        byPosition = new long[Math.max(degree, 2 * byPosition.length)];
      }
      // Sort key: topological position of the target high, edge index low.
      for (int j = 0; j < degree; j++) {
        byPosition[j] = (long) position[targets[from + j]] << 32 | (from + j);
      }
      Arrays.sort(byPosition, 0, degree);
      var row = new long[words];
      for (int j = 0; j < degree; j++) {
        int e = (int) byPosition[j];
        int v = targets[e];
        if ((row[v >>> 6] & (1L << v)) != 0) continue;
        keep[e] = true;
        kept++;
        row[v >>> 6] |= 1L << v;
        long[] below = reach[v];
        for (int w = 0; w < words; w++) row[w] |= below[w];
      }
      reach[u] = row;
    }

    var resultStart = new int[n + 1];
    var resultTargets = new int[kept];
    int next = 0;
    for (int v = 0; v < n; v++) {
      resultStart[v] = next;
      for (int e = start[v], end = start[v + 1]; e < end; e++) {
        if (keep[e]) resultTargets[next++] = targets[e];
      }
    }
    resultStart[n] = next;
    return new Graph(resultStart, resultTargets);
  }

  /**
   * Derives an edge mapping from a node mapping and two graphs.
   * <p>
   * For each edge in {@code newGraph}, looks up the corresponding edge in {@code oldGraph}
   * using the node mapping. The result maps new edge indices to old edge indices.
   * <p>
   * The node mapping must be in the direction new → old (as produced by all
   * {@link NodeMapping} factory methods), and its source space must be the nodes of
   * {@code newGraph}. It may be partial: a new edge without a counterpart in {@code oldGraph} maps
   * to {@code -1}, and so does every edge from or to a new node that maps to {@code -1}. Through
   * the inverse of a subgraph's node mapping, the edges of a graph thus map onto the subgraph, and
   * those of removed nodes to {@code -1}.
   * <p>
   * <strong>Not applicable to a transpose.</strong> The lookup searches the target of each
   * new edge in the out-adjacency of its source, and {@link BiAdjacentGraph#transposed()} swaps
   * those two roles - so {@code edgeMapping(transpose, graph, identity)} returns almost
   * exclusively {@code -1} instead of failing. Use
   * {@link #transposedEdgeMapping(Graph)} for that case.
   * <p>
   * <strong>Time:</strong> O(newNodeCount + newEdgeCount + Σ newDegree x log(oldDegree)), summed
   * over the new nodes, where oldDegree is the out-degree of a node's old counterpart: one binary
   * search in that list per edge.<br>
   * <strong>Memory:</strong> O(newEdgeCount) for the result.
   *
   * @param newGraph the new (remapped) graph
   * @param oldGraph the original graph
   * @param newToOld node mapping from new node IDs to old node IDs, {@code -1} for a new node
   *     without an old counterpart
   * @return edge mapping from new edge indices to old edge indices
   * @throws IllegalArgumentException if the source space of {@code newToOld} is not
   *     {@code newGraph.nodeCount()}
   * @throws IndexOutOfBoundsException if a node of {@code newGraph} maps to an ID that is not a
   *     node of {@code oldGraph}
   * @see #transposedEdgeMapping(Graph)
   */
  public static EdgeMapping edgeMapping(Graph newGraph, Graph oldGraph, NodeMapping newToOld) {
    int newNodeCount = newGraph.nodeCount();
    int[] oldOf = newToOld.mapping;
    if (oldOf.length != newNodeCount) {
      throw new IllegalArgumentException("newToOld has an index count of " + oldOf.length
          + ", but newGraph has " + newNodeCount + " nodes");
    }
    int oldNodeCount = oldGraph.nodeCount();
    var newTargets = Csr.outAdjacency(newGraph);
    var newStart = Csr.outAdjacencyStart(newGraph);
    var oldTargets = Csr.outAdjacency(oldGraph);
    var oldStart = Csr.outAdjacencyStart(oldGraph);
    int[] mapping = new int[newGraph.edgeCount()];

    // Every entry is checked in its own node's iteration before the result is returned, so a
    // target needs no check: one that maps to -1 or beyond the old graph is simply not found.
    for (int newNode = 0; newNode < newNodeCount; newNode++) {
      int oldNode = oldOf[newNode];
      int newEnd = newStart[newNode + 1];
      if (oldNode >= oldNodeCount) {
        throw notANode(newNode, oldNode, oldNodeCount);
      }
      if (oldNode < 0) {
        Arrays.fill(mapping, newStart[newNode], newEnd, -1);
        continue;
      }
      int oldFrom = oldStart[oldNode];
      int oldEnd = oldStart[oldNode + 1];
      for (int i = newStart[newNode]; i < newEnd; i++) {
        int oldTarget = oldOf[newTargets[i]];
        mapping[i] = SortedIntView.indexOfInRange(oldTargets, oldFrom, oldEnd, oldTarget);
      }
    }

    return new EdgeMapping(mapping, IndexMapping.countValid(mapping));
  }

  /**
   * Derives the edge mapping between a graph and its transpose.
   * <p>
   * Maps each edge index of the transpose of {@code graph} to the edge index of the same edge
   * in {@code graph}, in the direction new → old like every other mapping factory here.
   * {@link BiAdjacentGraph#transposed()} preserves nodes but not edge indices, so this is what
   * translates edge metadata onto the transpose:
   * <pre>{@code
   * Graph transpose = BiAdjacentGraph.of(graph).transposed();
   * EdgeMapping newToOld = Graphs.transposedEdgeMapping(graph);
   * double[] transposedWeights = new double[transpose.edgeCount()];
   * for (int e = 0; e < transpose.edgeCount(); e++) {
   *     transposedWeights[e] = weights[newToOld.get(e)];
   * }
   * }</pre>
   * The mapping is <strong>total</strong>: every edge of the transpose is an edge of the
   * original with source and target swapped, so it holds no {@code -1} entries and
   * {@link IndexMapping#isPartial()} is {@code false}.
   * <p>
   * For a single edge rather than a full pass, use
   * {@link BiAdjacentGraph#transposedEdgeIndex(int)}, which is the inverse direction.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x edgeCount bytes for the result, plus 4 x nodeCount bytes
   * during computation.
   *
   * @param graph the graph whose transpose is to be addressed
   * @return edge mapping from the transpose's edge indices to this graph's edge indices
   * @see BiAdjacentGraph#transposed()
   * @see BiAdjacentGraph#transposedEdgeIndex(int)
   */
  public static EdgeMapping transposedEdgeMapping(Graph graph) {
    int n = graph.nodeCount();
    int m = graph.edgeCount();
    var targets = Csr.outAdjacency(graph);
    var start = Csr.outAdjacencyStart(graph);

    // Write pointer per target node: where that node's next in-edge slot goes.
    // A bi-adjacent graph already has those offsets; otherwise count and prefix-sum them.
    int[] writePos;
    if (graph instanceof BiAdjacentGraph biAdjacent) {
      writePos = Arrays.copyOf(Csr.inAdjacencyStart(biAdjacent), n);
    } else {
      writePos = new int[n];
      for (int target : targets) {
        writePos[target]++;
      }
      int nextStart = 0;
      for (int node = 0; node < n; node++) {
        int inDegree = writePos[node];
        writePos[node] = nextStart;
        nextStart += inDegree;
      }
    }

    // Same counting sort as BiAdjacentGraph.of, with the edge index as payload instead
    // of the source node. Ascending node scan makes slot q of the transpose and entry q of
    // the mapping describe the same edge.
    int[] mapping = new int[m];
    for (int node = 0; node < n; node++) {
      int end = start[node + 1];
      for (int edgeIndex = start[node]; edgeIndex < end; edgeIndex++) {
        mapping[writePos[targets[edgeIndex]]++] = edgeIndex;
      }
    }

    return new EdgeMapping(mapping, m);
  }

  /**
   * Derives an edge partition from a node partition and its condensation.
   * <p>
   * Maps the edge indices of the original graph - the one the partition was computed from - to
   * condensed edge indices. Intra-group edges (edges where both source and target belong to the
   * same group) are unmapped ({@code -1}), and so are edges from or to a node in no group.
   * Multiple original cross-group edges that connect the same pair of groups map to the same
   * condensed edge (many-to-one mapping).
   * <p>
   * The condensed graph must be the result of {@link NodePartition#condense()} applied to the
   * given partition; only its node count is checked. The original graph is not a parameter: the
   * partition holds it, so a wrong one cannot be passed.
   * <p>
   * <strong>Time:</strong> O(originalNodeCount + originalEdgeCount + Σ log(condensedDegree)),
   * summed over the cross-group edges, where condensedDegree is the out-degree of an edge's source
   * group in the condensed graph: one binary search in that list per edge.<br>
   * <strong>Memory:</strong> O(originalEdgeCount + condensedEdgeCount) for the result.
   *
   * @param condensedGraph the condensation graph (from {@link NodePartition#condense()})
   * @param partition      node partition mapping (from {@link Components} or
   *                       {@link NodePartition#of(Graph, int[], int)})
   * @return edge partition from original edge indices to condensed edge indices
   * @throws IllegalArgumentException if {@code condensedGraph.nodeCount()} is not
   *     {@code partition.groupCount()}
   */
  public static EdgePartition edgePartition(Graph condensedGraph, NodePartition partition) {
    int groupCount = partition.groupCount();
    if (condensedGraph.nodeCount() != groupCount) {
      throw new IllegalArgumentException("condensedGraph has " + condensedGraph.nodeCount()
          + " nodes, but the partition has " + groupCount + " groups");
    }
    Graph graph = partition.graph;
    int[] mapping = new int[graph.edgeCount()];
    Arrays.fill(mapping, -1);

    int[] groupOf = partition.mapping;
    var targets = Csr.outAdjacency(graph);
    var start = Csr.outAdjacencyStart(graph);
    var condensedTargets = Csr.outAdjacency(condensedGraph);
    var condensedStart = Csr.outAdjacencyStart(condensedGraph);

    // 1) Condensed edge of every cross-group edge
    for (int node = 0; node < graph.nodeCount(); node++) {
      int sourceGroup = groupOf[node];
      if (sourceGroup < 0) continue;
      int end = start[node + 1];
      for (int ei = start[node]; ei < end; ei++) {
        int targetGroup = groupOf[targets[ei]];
        if (sourceGroup != targetGroup && targetGroup >= 0) {
          mapping[ei] = SortedIntView.indexOfInRange(condensedTargets,
              condensedStart[sourceGroup], condensedStart[sourceGroup + 1], targetGroup);
        }
      }
    }

    // 2) Original edges per condensed edge, as CSR
    var groups = ParentForest.groupIndices(mapping, condensedGraph.edgeCount());
    return new EdgePartition(mapping, groups.members(), groups.start());
  }

  /**
   * Returns the root nodes of the given graph - the nodes without incoming edges - in ascending
   * order. They are the natural starting points of a traversal over a DAG or a forest:
   * <pre>{@code
   * traverser.run(Graphs.rootNodes(graph));
   * }</pre>
   * Each call scans all nodes; keep the result if it is needed more than once. For a single node,
   * {@link BiAdjacentGraph#isRootNode(int)} answers in constant time.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 4 bytes per root node for the result.
   *
   * @param graph the graph to search, with in-adjacency
   * @return the root nodes, sorted ascending; newly allocated
   */
  public static SortedIntView rootNodes(BiAdjacentGraph graph) {
    int n = graph.nodeCount();
    int[] inStart = Csr.inAdjacencyStart(graph);
    int count = 0;
    for (int node = 0; node < n; node++) {
      if (inStart[node] == inStart[node + 1]) count++;
    }
    int[] roots = new int[count];
    int next = 0;
    for (int node = 0; node < n; node++) {
      if (inStart[node] == inStart[node + 1]) roots[next++] = node;
    }
    return new SortedIntView(roots, 0, count);
  }

  /**
   * Returns the nodes reachable from {@code source}, the source included, in ascending order.
   * <p>
   * Equivalent to {@link #reachableFrom(Graph, IntList)} with a one-element list.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> as for {@link #reachableFrom(Graph, IntList)}.
   *
   * @param graph  the graph to search
   * @param source source node ID in {@code 0..nodeCount()-1}
   * @return the reachable nodes, sorted ascending
   * @throws IndexOutOfBoundsException if {@code source} is out of range
   */
  public static SortedIntView reachableFrom(Graph graph, int source) {
    return reachableFrom(graph, IntLists.singleton(source));
  }

  /**
   * Returns the nodes reachable from at least one of the given sources, the sources included,
   * in ascending order.
   * <p>
   * Edges are followed in their direction; for the nodes that reach the sources, pass the
   * transpose. Duplicate sources are ignored, an empty list gives an empty result. The result is
   * sorted, so it extracts the reachable part directly:
   *
   * <pre>{@code
   * Graph function = Graphs.remapped(cfg, NodeMapping.forSubgraph(Graphs.reachableFrom(cfg, entry)));
   * }</pre>
   *
   * For distances and shortest paths, use {@link BfsDistances}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 bytes per reachable node for the result. During the traversal,
   * one bit per node and a queue that grows by doubling towards the reachable count, briefly up
   * to 12 bytes per reachable node while it is copied.
   *
   * @param graph   the graph to search
   * @param sources source node IDs, each in {@code 0..nodeCount()-1}
   * @return the reachable nodes, sorted ascending
   * @throws IndexOutOfBoundsException if a source is out of range
   */
  public static SortedIntView reachableFrom(Graph graph, IntList sources) {
    int n = graph.nodeCount();
    Checks.checkNodes(sources, n);
    int sourceCount = sources.size();
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);
    var visited = new long[(n >> 6) + 1];
    // Every reachable node enters the queue exactly once, so the queue grows into the result.
    // The initial capacity holds every distinct source.
    var queue = new int[Math.min(n, Math.max(16, sourceCount))];
    int tail = 0;
    for (int i = 0; i < sourceCount; i++) {
      int s = sources.getInt(i);
      if ((visited[s >> 6] & (1L << s)) == 0) {
        visited[s >> 6] |= 1L << s;
        queue[tail++] = s;
      }
    }
    for (int head = 0; head < tail; head++) {
      int u = queue[head];
      for (int e = out[u], end = out[u + 1]; e < end; e++) {
        int v = adj[e];
        if ((visited[v >> 6] & (1L << v)) == 0) {
          visited[v >> 6] |= 1L << v;
          if (tail == queue.length) {
            queue = Arrays.copyOf(queue, (int) Math.min(n, 2L * queue.length));
          }
          queue[tail++] = v;
        }
      }
    }
    // The queue is done; refill it in ascending order from the bit set, at its exact length.
    if (tail < queue.length) queue = Arrays.copyOf(queue, tail);
    int k = 0;
    for (int w = 0; w < visited.length; w++) {
      for (long bits = visited[w]; bits != 0; bits &= bits - 1) {
        queue[k++] = (w << 6) + Long.numberOfTrailingZeros(bits);
      }
    }
    return new SortedIntView(queue, 0, tail);
  }

  /**
   * Returns whether the given graph is a DAG (contains no cycles).
   * <p>
   * Uses a single DFS pass with back-edge detection. Self-loops are detected as cycles.
   * Terminates early on the first cycle found.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph contains no cycles
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   * @see #findCycle(Graph)
   */
  public static boolean isDag(Graph graph) {
    var checker = new CycleChecker(graph, false);
    checker.run();
    return !checker.hasCycle;
  }

  /**
   * Returns the nodes of one cycle of the graph in order, or an empty list if the graph is a DAG.
   * <p>
   * A depth-first search from node 0 upwards, successors in ascending order, stops at the first
   * edge {@code u -> v} back to a node on its current path; the result is that path from
   * {@code v} to {@code u}, and the edge {@code u -> v} closes it. A self-loop is a cycle of one
   * node. The result is deterministic, but not necessarily the shortest cycle. To find a cycle
   * inside one strongly connected component, call this on the component's subgraph; the result
   * then holds node IDs of the subgraph, which the {@link NodeMapping} of the subgraph translates
   * back.
   *
   * <pre>{@code
   * IntList cycle = Graphs.findCycle(dependencies);
   * if (!cycle.isEmpty()) {
   *     // report cycle.getInt(0) -> cycle.getInt(1) -> ... -> cycle.getInt(0)
   * }
   * }</pre>
   *
   * <strong>Time:</strong> O(nodeCount + edgeCount), stopping at the first cycle.<br>
   * <strong>Memory:</strong> O(nodeCount) during computation; 4 bytes per node of the cycle for
   * the result.
   *
   * @param graph the graph to search
   * @return the nodes of one cycle in order, each once, or an empty list; unmodifiable
   * @throws IllegalArgumentException if a depth-first path is longer than 2^30 - 5 nodes, the most
   *     frames an array can hold
   * @see #isDag(Graph)
   */
  public static IntList findCycle(Graph graph) {
    var checker = new CycleChecker(graph, true);
    checker.run();
    int[] cycle = checker.cycle;
    return cycle == null ? IntLists.EMPTY_LIST : IntLists.unmodifiable(IntArrayList.wrap(cycle));
  }

  /**
   * Returns whether the given graph is a forest (DAG where every node has in-degree ≤ 1).
   * <p>
   * Equivalently: a forest is a DAG consisting of zero or more disjoint trees.
   * An empty graph is a forest.
   * <p>
   * Reads the graph as directed. For the undirected reading see
   * {@link #isUndirectedForest(Graph)}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a forest
   */
  public static boolean isForest(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null;
  }

  /**
   * Returns whether the given graph is a tree (forest that is weakly connected).
   * <p>
   * A tree has exactly one root node (in-degree 0), and every other node has
   * in-degree exactly 1, so its edges point away from the root. An empty graph is a
   * tree (vacuously). A single node with no edges is a tree.
   * <p>
   * Reads the graph as directed. For the undirected reading see
   * {@link #isUndirectedTree(Graph)}; for the reading in which edges point toward the root,
   * apply this to the transpose, {@code BiAdjacentGraph.of(graph).transposed()} - which builds the
   * in-adjacency and therefore costs 4 x (nodeCount + edgeCount) bytes, unlike this method itself.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a tree
   */
  public static boolean isTree(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null && info.rootCount <= 1;
  }

  /**
   * Returns whether the given graph is a path (tree where every node has out-degree ≤ 1).
   * <p>
   * A path is a linear chain of nodes. An empty graph is a path. A single node is a path.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount).
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a path
   */
  public static boolean isPath(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null && info.rootCount <= 1 && info.maxOutDegree <= 1;
  }

  /**
   * Returns whether the given graph is strongly connected: every node reaches every other,
   * following edge directions.
   * <p>
   * This is the whole-graph question behind {@link Components#sccsOf(Graph)}, answered without
   * building the partition: one depth-first search from node 0 in the manner of Tarjan's
   * algorithm, which stops as soon as a node below the root closes a component of its own - the
   * graph then has a second component. Until that moment no component has been removed from
   * Tarjan's stack, so the stack is exactly the set of visited nodes and needs no storage of its
   * own. On a result already computed, {@link Sccs#isStronglyConnected()} answers in O(1). An
   * empty graph and a single node are strongly connected (vacuously). Iterative; deep graphs do
   * not overflow the call stack.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes, plus a frame stack that grows with the search
   * depth, up to another 8 x nodeCount bytes; nothing afterwards.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph consists of at most one strongly connected component
   * @throws IllegalArgumentException if the depth-first search is deeper than 2^30 - 5 nodes, the
   *     most frames an array can hold
   * @see Components#sccsOf(Graph)
   * @see Sccs#isStronglyConnected()
   * @see #isWeaklyConnected(Graph)
   */
  public static boolean isStronglyConnected(Graph graph) {
    int n = graph.nodeCount();
    if (n <= 1) {
      return true;
    }
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);
    var index = new int[n];   // discovery time from 1; 0 = unvisited
    var low = new int[n];
    int[] frames = DfsFrames.initial(n);
    int time = 1;
    index[0] = 1;
    low[0] = 1;
    frames[0] = 0;
    frames[1] = out[0];
    int top = 0;

    outer:
    while (true) {
      int u = frames[2 * top];
      int end = out[u + 1];
      int i = frames[2 * top + 1];
      while (i < end) {
        int v = adj[i++];
        if (index[v] == 0) {
          frames[2 * top + 1] = i;
          index[v] = ++time;
          low[v] = time;
          if (++top == frames.length >> 1) {
            frames = DfsFrames.grow(frames, n);
          }
          frames[2 * top] = v;
          frames[2 * top + 1] = out[v];
          continue outer;
        }
        if (index[v] < low[u]) {
          low[u] = index[v];
        }
      }
      if (top == 0) {
        return time == n;
      }
      if (low[u] == index[u]) {
        return false;
      }
      int parent = frames[2 * (top - 1)];
      if (low[u] < low[parent]) {
        low[parent] = low[u];
      }
      top--;
    }
  }

  /**
   * Iterative DFS (explicit stack, no recursion) for cycle detection. Detects back-edges
   * (including self-loops) and terminates early on the first cycle found.
   * <p>
   * visited and on-stack flags are packed 2 bits per node into a single {@code int[]}
   * (bit 0 = visited, bit 1 = on-stack, 16 nodes per int): one memory touch per node check
   * instead of two separate bit-set lookups.
   */
  private static final class CycleChecker {

    private static final int VISITED = 0b01;
    private static final int ON_STACK = 0b10;

    private final Graph graph;
    private final int[] state;

    /** Whether to keep the cycle; {@link #isDag(Graph)} needs only the answer. */
    private final boolean recordCycle;

    boolean hasCycle;

    /** The cycle closed by the first back edge, from its target to its source. */
    int @Nullable [] cycle;

    CycleChecker(Graph graph, boolean recordCycle) {
      this.graph = graph;
      this.state = new int[(graph.nodeCount() >> 4) + 1];
      this.recordCycle = recordCycle;
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = Csr.outAdjacencyStart(graph);
      int[] adj = Csr.outAdjacency(graph);
      int[] state = this.state;
      // Each frame is two interleaved ints, the node and its resume position in the
      // adjacency array: both share a cache line, and the array grows on demand.
      int[] frames = DfsFrames.initial(n);

      for (int root = 0; root < n; root++) {
        int rShift = (root & 0xF) << 1;
        if (((state[root >> 4] >>> rShift) & VISITED) != 0) {
          continue;
        }
        state[root >> 4] |= (VISITED | ON_STACK) << rShift;
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
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;
            if ((flags & ON_STACK) != 0) {
              hasCycle = true;
              if (recordCycle) {
                // The path from v up to u, the top, is on the frame stack: the cycle u -> v closes.
                int at = top;
                while (frames[2 * at] != v) at--;
                cycle = new int[top - at + 1];
                for (int k = 0; k < cycle.length; k++) cycle[k] = frames[2 * (at + k)];
              }
              return;
            }
            if ((flags & VISITED) == 0) {
              frames[2 * top + 1] = i;
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              if (++top == frames.length >> 1) {
                frames = DfsFrames.grow(frames, n);
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));
          top--;
        }
      }
    }
  }

  /**
   * Returns the root count and the largest out-degree of {@code graph} if it is a forest, or null
   * if some node has in-degree > 1 or lies on or below a cycle.
   * <p>
   * {@link Ancestry#of(Graph)} makes the same check, with a witness for its message; a change to
   * what counts as a forest changes both.
   */
  private static @Nullable ForestInfo computeForestInfo(Graph graph) {
    int n = graph.nodeCount();
    var targets = Csr.outAdjacency(graph);
    var start = Csr.outAdjacencyStart(graph);

    var inDegree = new int[n];
    for (int target : targets) {
      if (++inDegree[target] > 1) {
        return null;
      }
    }

    // With every in-degree <= 1, a BFS from the roots enqueues each node at most once; a node it
    // misses lies on a cycle or below one.
    int[] queue = new int[n];
    int head = 0;
    int tail = 0;
    int maxOutDegree = 0;
    for (int i = 0; i < n; i++) {
      int outDegree = start[i + 1] - start[i];
      if (outDegree > maxOutDegree) maxOutDegree = outDegree;
      if (inDegree[i] == 0) queue[tail++] = i;
    }
    int rootCount = tail;
    while (head < tail) {
      int node = queue[head++];
      int end = start[node + 1];
      for (int i = start[node]; i < end; i++) {
        queue[tail++] = targets[i];
      }
    }
    if (tail != n) return null;

    return new ForestInfo(rootCount, maxOutDegree);
  }

  private record ForestInfo(int rootCount, int maxOutDegree) {}

  /**
   * Returns whether the given graph is bipartite, reading it as undirected.
   * <p>
   * The nodes of a bipartite graph split into two classes such that every edge runs between the
   * classes rather than inside one. The graph is read as undirected like
   * {@link Components#ccsOf(Graph)}: the edges {@code a → b} and {@code b → a} are one undirected
   * edge rather than two. That collapsing cannot change the answer, since a reciprocal pair is an
   * even cycle either way - but a <strong>self-loop</strong> can, and it makes the graph not
   * bipartite, because a node adjacent to itself belongs in neither class. This is the one place
   * where the undirected predicates here do not ignore self-loops: two-colorability is not a
   * connectivity question. An empty graph is bipartite.
   * <p>
   * Terminates as soon as an edge closes an odd cycle. Its double cover holds two elements per
   * node in one array, so it requires {@code nodeCount <= 2^30 - 5} - the only predicate here that
   * rejects a graph by its node count alone, before reading an edge. That limit is reachable
   * rather than theoretical - a graph of 10^9 nodes fits in a large heap - so exceeding it is
   * rejected outright instead of surfacing as a failure to allocate the array.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> 10 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is bipartite
   * @throws IllegalArgumentException if {@code graph.nodeCount() > 2^30 - 5}
   * @see #isUndirectedForest(Graph)
   */
  public static boolean isBipartite(Graph graph) {
    int n = graph.nodeCount();
    ArrayLimits.checkNodeCount(n, ArrayLimits.MAX_NODE_COUNT_TWO_INTS, "isBipartite");

    var targets = Csr.outAdjacency(graph);
    var start = Csr.outAdjacencyStart(graph);

    // Double cover: element u means "u in class A", element u + n means "u in class B". An edge
    // ties each endpoint to the other's opposite, so once a node shares a set with its own
    // opposite, both classes would have to hold it - an odd cycle. Both unions are symmetric in
    // u and v, which is why the stored direction is irrelevant and a reciprocal pair simply
    // repeats them.
    int[] parent = UnionFind.singletons(2 * n);
    byte[] rank = new byte[2 * n];

    for (int u = 0; u < n; u++) {
      int end = start[u + 1];
      for (int ei = start[u]; ei < end; ei++) {
        int v = targets[ei];
        if (u == v) {
          return false;
        }
        UnionFind.union(parent, rank, u, v + n);
        UnionFind.union(parent, rank, u + n, v);
        if (UnionFind.find(parent, u) == UnionFind.find(parent, u + n)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns whether the given graph is a tree when read as undirected: connected, with exactly
   * {@code nodeCount - 1} undirected edges.
   * <p>
   * Where {@link #isTree(Graph)} requires every edge to point away from one root, this ignores
   * direction entirely, so a graph whose edges point toward a root, or in mixed directions, is a
   * tree here as long as the underlying undirected structure is one. The graph is read as
   * undirected like {@link Components#ccsOf(Graph)}: the edges {@code a → b} and {@code b → a} are
   * one undirected edge rather than two, so a graph storing its undirected edges symmetrically is
   * a tree exactly when the same structure stored with a single direction per edge is. Self-loops
   * are ignored. An empty graph is a tree (vacuously), and so is a single node with no edges.
   * <p>
   * A non-empty graph holding more than {@code 3 x nodeCount - 2} edges is rejected without
   * reading any of them, since no tree can store that many even with a self-loop on every node.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), plus O(log outDegree) per edge that is stored in both directions;
   * O(1) when the edge count alone rules out a tree.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph is a tree when read as undirected
   * @see #isTree(Graph)
   * @see #isUndirectedForest(Graph)
   */
  public static boolean isUndirectedTree(Graph graph) {
    int n = graph.nodeCount();
    if (n == 0) {
      return true;
    }
    if (exceedsAcyclicEdgeBound(graph)) {
      return false;
    }
    // A cycle yields -1, which can never equal n - 1 here, so one comparison covers both
    // acyclicity and connectedness.
    return undirectedMergeCount(graph) == n - 1;
  }

  /**
   * Returns whether the given graph is a forest when read as undirected, that is, whether it holds
   * no undirected cycle.
   * <p>
   * A forest is a disjoint union of trees, so this is {@link #isUndirectedTree(Graph)} without the
   * requirement that there be exactly one component. It is also the undirected counterpart of
   * {@link #isDag(Graph)}: an undirected graph is acyclic exactly when it is a forest. The graph is
   * read as undirected like {@link Components#ccsOf(Graph)}, so a reciprocal pair
   * {@code a → b} / {@code b → a} is one edge and not a cycle, and self-loops are ignored. An empty
   * graph is a forest.
   * <p>
   * A non-empty graph holding more than {@code 3 x nodeCount - 2} edges is rejected without
   * reading any of them, since no forest can store that many even with a self-loop on every node.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), plus O(log outDegree) per edge that is stored in both directions;
   * O(1) when the edge count alone rules out a forest.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph holds no undirected cycle
   * @see #isForest(Graph)
   * @see #isUndirectedTree(Graph)
   */
  public static boolean isUndirectedForest(Graph graph) {
    if (exceedsAcyclicEdgeBound(graph)) {
      return false;
    }
    return undirectedMergeCount(graph) >= 0;
  }

  /**
   * Returns whether the given graph is weakly connected: at most one component when edge
   * direction is ignored.
   * <p>
   * This is the whole-graph question behind {@link Components#wccsOf(Graph)}, which it would
   * otherwise be answered from, and it needs neither that partition nor a full pass - the check
   * stops once the nodes are known to form one component. For the directed reading see
   * {@link #isStronglyConnected(Graph)}. An empty graph and a single node are weakly connected
   * (vacuously). Self-loops are irrelevant here, as they connect nothing.
   * <p>
   * <strong>Time:</strong> O(nodeCount x α(nodeCount) + edgeCount), effectively
   * O(nodeCount + edgeCount), with early termination.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes.
   *
   * @param graph the graph to check
   * @return {@code true} if the graph consists of at most one weakly connected component
   * @see Components#wccsOf(Graph)
   * @see #isStronglyConnected(Graph)
   */
  public static boolean isWeaklyConnected(Graph graph) {
    int n = graph.nodeCount();
    if (n <= 1) {
      return true;
    }

    var targets = Csr.outAdjacency(graph);
    var start = Csr.outAdjacencyStart(graph);
    // Reciprocal pairs need no attention: a second union of the same two nodes merges nothing,
    // and neither does a self-loop.
    int[] parent = UnionFind.singletons(n);
    byte[] rank = new byte[n];
    int merges = 0;

    for (int u = 0; u < n; u++) {
      int end = start[u + 1];
      for (int ei = start[u]; ei < end; ei++) {
        if (UnionFind.union(parent, rank, u, targets[ei]) && ++merges == n - 1) {
          return true; // n - 1 merges join every node; no later edge can undo that
        }
      }
    }
    return false;
  }

  /**
   * Returns whether {@code graph} holds more edges than any acyclic undirected reading could: at
   * most {@code nodeCount - 1} undirected edges, each stored in up to two directions, plus at most
   * one self-loop per node. A shortcut only: {@link #undirectedMergeCount(Graph)} rejects such a
   * graph as well, within its first 3 x nodeCount edges, but this answers in O(1) and allocates
   * nothing.
   */
  private static boolean exceedsAcyclicEdgeBound(Graph graph) {
    int n = graph.nodeCount();
    return graph.edgeCount() > (n == 0 ? 0 : 3L * n - 2);
  }

  /**
   * Merges the endpoints of every undirected edge and returns the number of merges performed, so
   * that the nodes form {@code nodeCount - result} components; or {@code -1} as soon as an edge
   * closes an undirected cycle. Self-loops are skipped.
   * <p>
   * Nodes are walked in ascending order, and that is what makes a failed union classifiable. A
   * failure means both endpoints are already connected, so the edge is either the second stored
   * direction of an edge already counted or a genuine extra edge closing a cycle. For
   * {@code v > u} the opposite direction sits in a list not yet visited, so this is the first
   * encounter with {@code {u,v}} and the failure is a cycle. For {@code v < u} one binary search
   * in v's out-adjacency decides it. A successful union never needs the test: had the opposite
   * direction been counted, the endpoints would already be connected and the union would have
   * failed. Searching always would cost a search per edge; searching never would report every
   * symmetrically stored edge as a cycle.
   */
  private static int undirectedMergeCount(Graph graph) {
    int n = graph.nodeCount();
    var targets = Csr.outAdjacency(graph);
    var start = Csr.outAdjacencyStart(graph);
    int[] parent = UnionFind.singletons(n);
    byte[] rank = new byte[n];
    int merges = 0;

    for (int u = 0; u < n; u++) {
      int end = start[u + 1];
      for (int ei = start[u]; ei < end; ei++) {
        int v = targets[ei];
        if (u == v) {
          continue;
        }
        if (UnionFind.union(parent, rank, u, v)) {
          merges++;
        } else if (v > u || !SortedIntView.containsInRange(targets, start[v], start[v + 1], u)) {
          return -1;
        }
      }
    }
    return merges;
  }

  /**
   * Returns the complete adjacency list of the graph, one line per node in ascending order:
   * <pre>
   * 0 -&gt; [17, 532]
   * 1 -&gt; [4]
   * 2 -&gt; []
   * </pre>
   * Each line ends with a line feed. Unlike {@link Graph#toString()}, which is bounded because it
   * runs implicitly, this lists every node and edge; a {@link BiAdjacentGraph} lists its
   * out-adjacency. The text of a graph with a few hundred million edges exceeds what a
   * {@code String} can hold and ends in an {@link OutOfMemoryError}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) characters for the result.
   *
   * @param graph the graph to list
   * @return the adjacency list, one line per node
   */
  public static String adjacencyListOf(Graph graph) {
    int n = graph.nodeCount();
    int[] start = Csr.outAdjacencyStart(graph);
    int[] targets = Csr.outAdjacency(graph);
    var sb = new StringBuilder((int) Math.min(ArrayLimits.MAX_ARRAY_LENGTH,
        (long) LIST_CHARS_PER_NODE * n + (long) LIST_CHARS_PER_EDGE * graph.edgeCount()));
    for (int v = 0; v < n; v++) {
      sb.append(v).append(" -> [");
      for (int e = start[v], end = start[v + 1]; e < end; e++) {
        if (e > start[v]) sb.append(", ");
        sb.append(targets[e]);
      }
      sb.append("]\n");
    }
    return sb.toString();
  }

  /**
   * Returns a 64-bit hash of the graph's structure, consistent with {@link Graph#equals(Object)}:
   * equal graphs - a {@link BiAdjacentGraph} and its uni-adjacent counterpart included - always
   * have equal hashes, and different graphs rarely do.
   * <p>
   * Computed on every call, not cached - compute it once and keep it, for instance as the key of
   * a cache over graph structure or to detect the fixpoint of a transformation. The value depends
   * only on the structure, so it is the same in every run and on every JVM; a Graphity release
   * that changes it says so in its changelog. Equal hashes do not prove equal graphs, and the hash
   * is not built to resist graphs crafted to collide; compare with {@link Graph#equals(Object)}
   * where it matters. {@link Graph#hashCode()} stays O(nodeCount + edgeCount) per call.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   *
   * @param graph the graph to hash
   * @return the structural hash
   */
  public static long topologyHash(Graph graph) {
    int n = graph.nodeCount();
    int[] start = Csr.outAdjacencyStart(graph);
    int[] targets = Csr.outAdjacency(graph);
    long h = mixInto(HASH_SEED, n);
    h = mixInto(h, graph.edgeCount());
    for (int v = 0; v < n; v++) {
      int end = start[v + 1];
      h = mixInto(h, end - start[v]);
      for (int e = start[v]; e < end; e++) {
        h = mixInto(h, targets[e]);
      }
    }
    // SplitMix64 finalizer: every input bit reaches every output bit.
    h = (h ^ (h >>> 30)) * SPLITMIX64_MULTIPLIER_1;
    h = (h ^ (h >>> 27)) * SPLITMIX64_MULTIPLIER_2;
    return h ^ (h >>> 31);
  }

  /** One order-sensitive step of {@link #topologyHash(Graph)}. */
  private static long mixInto(long h, int value) {
    return Long.rotateLeft(h ^ (value * HASH_GOLDEN_GAMMA), 29) * HASH_STEP_PRIME;
  }

  /**
   * Returns the edges added and removed between two graphs over the same nodes.
   * <p>
   * Merges the sorted adjacency lists of both graphs node by node, so both lists of the result
   * come out in edge index order, each edge named by its index in the graph it belongs to: added
   * edges by their index in {@code after}, removed edges by their index in {@code before}. An
   * empty diff means the graphs are equal.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount of both graphs).<br>
   * <strong>Memory:</strong> 12 bytes per added or removed edge for the result, and up to 18 more
   * while it is collected.
   *
   * @param before the older graph
   * @param after  the newer graph, with the same node count
   * @return the added and removed edges
   * @throws IllegalArgumentException if the node counts differ
   */
  public static GraphDiff diff(Graph before, Graph after) {
    int n = before.nodeCount();
    if (after.nodeCount() != n) {
      throw new IllegalArgumentException("diff needs graphs over the same nodes, but before has "
          + n + " nodes and after " + after.nodeCount());
    }
    int[] oldStart = Csr.outAdjacencyStart(before);
    int[] oldTargets = Csr.outAdjacency(before);
    int[] newStart = Csr.outAdjacencyStart(after);
    int[] newTargets = Csr.outAdjacency(after);
    // Parallel lists, so that none needs more entries than its graph has edges.
    var addedSources = new IntArrayList();
    var addedTargets = new IntArrayList();
    var addedEdgeIndices = new IntArrayList();
    var removedSources = new IntArrayList();
    var removedTargets = new IntArrayList();
    var removedEdgeIndices = new IntArrayList();
    for (int v = 0; v < n; v++) {
      int oldPos = oldStart[v];
      int oldEnd = oldStart[v + 1];
      int newPos = newStart[v];
      int newEnd = newStart[v + 1];
      // No target reaches Integer.MAX_VALUE - node IDs stay below it -, so it marks an exhausted
      // list.
      while (oldPos < oldEnd || newPos < newEnd) {
        int oldTarget = oldPos < oldEnd ? oldTargets[oldPos] : Integer.MAX_VALUE;
        int newTarget = newPos < newEnd ? newTargets[newPos] : Integer.MAX_VALUE;
        if (oldTarget == newTarget) {
          oldPos++;
          newPos++;
        } else if (oldTarget < newTarget) {
          removedSources.add(v);
          removedTargets.add(oldTarget);
          removedEdgeIndices.add(oldPos++);
        } else {
          addedSources.add(v);
          addedTargets.add(newTarget);
          addedEdgeIndices.add(newPos++);
        }
      }
    }
    // Both lists are in edge index order already, which sortedEdges detects and only copies.
    return new GraphDiff(
        Edges.sortedEdges(after, addedSources.elements(), addedTargets.elements(),
            addedEdgeIndices.elements(), addedSources.size(), false),
        Edges.sortedEdges(before, removedSources.elements(), removedTargets.elements(),
            removedEdgeIndices.elements(), removedSources.size(), false));
  }
}
