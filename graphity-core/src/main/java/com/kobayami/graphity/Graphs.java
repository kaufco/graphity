package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongArrays;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

/**
 * Operations on graphs, in five families: derived graphs, edge mappings, node sets, shape checks,
 * and a graph as text, hash or diff.
 * <ul>
 *   <li><strong>Derived graphs:</strong> {@link #remapped(Graph, NodeMapping)} renumbers the
 *       nodes of a graph by a {@link NodeMapping}, which also selects subgraphs;
 *       {@link #transitiveClosure(Graph)} and {@link #transitiveReduction(Graph)} add and drop
 *       the edges that other paths imply; {@link #dominatorTree(Dominators)} turns
 *       {@link Dominators} into a graph.</li>
 *   <li><strong>Edge mappings:</strong> {@link #edgeMapping(Graph, Graph, NodeMapping)},
 *       {@link #transposedEdgeMapping(Graph)} and {@link #edgePartition(Graph, NodePartition)}
 *       relate the edges of a remapped, transposed or condensed graph to the edges of the graph
 *       it came from.</li>
 *   <li><strong>Node sets:</strong> {@link #rootNodes(BiAdjacentGraph)} and
 *       {@link #reachableFrom(Graph, IntList)}.</li>
 *   <li><strong>Shape checks:</strong> {@link #isDag(Graph)}, {@link #isForest(Graph)},
 *       {@link #isTree(Graph)}, {@link #isPath(Graph)} and {@link #isStronglyConnected(Graph)}
 *       read the graph as directed, and {@link #findCycle(Graph)} returns a cycle that makes a
 *       graph no DAG (directed acyclic graph); {@link #isUndirectedForest(Graph)},
 *       {@link #isUndirectedTree(Graph)}, {@link #isBipartite(Graph)} and
 *       {@link #isWeaklyConnected(Graph)} read it as undirected.</li>
 *   <li><strong>Text, hash and diff:</strong> {@link #adjacencyListOf(Graph)} writes a whole
 *       graph as text, {@link #topologyHash(Graph)} hashes it into 64 bits to keep, and
 *       {@link #diff(Graph, Graph)} returns the edges added and removed between two versions of
 *       a graph.</li>
 * </ul>
 * <p>
 * Data on nodes and edges follows a renumbered, transposed or condensed graph through the
 * mappings. Like the node mappings that the factories of {@link NodeMapping} make, the edge
 * mappings point from new to old: from the derived graph to the original one. Their
 * {@code gather} methods carry an array of data over to the derived graph. An
 * {@link EdgePartition} groups the edges of the original by the condensed edge they became, and
 * its {@code reduce} methods combine their data.
 * <p>
 * A shape check without a prefix reads the graph as directed. One prefixed {@code undirected} reads
 * it as undirected, as {@link Components#ccsOf(Graph)} does: each edge connects its source and
 * target both ways, and the edges {@code a -> b} and {@code b -> a} count as one.
 * {@code isBipartite} and {@code isWeaklyConnected} read it so too, without the prefix, because
 * neither property has a directed reading. The checks of the undirected reading use <a
 * href="https://en.wikipedia.org/wiki/Disjoint-set_data_structure">union-find</a>, whose cost grows
 * with alpha(nodeCount). The inverse Ackermann function alpha is below 5 for any graph. No shape
 * check needs memory in proportion to the edge count.
 * <p>
 * Every result is computed anew on every call, not cached, and shares no array with the
 * arguments: keep a result to use it more than once. No method recurses, so deep graphs do not
 * overflow the call stack.
 */
public final class Graphs {

  // The constants of topologyHash. Changing one of them, or a shift or rotation distance in the
  // code, changes the hash of every graph, which a release must announce in its changelog.

  // Seed: the fractional part of the square root of 2, scaled to 64 bits and rounded.
  private static final long HASH_SEED = 0x6A09E667F3BCC909L;

  // Spreads each input over all 64 bits: 2^64 / golden ratio, rounded down.
  private static final long HASH_GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

  // Multiplier of each step: PRIME64_2 of xxHash64.
  private static final long HASH_STEP_PRIME = 0xC2B2AE3D27D4EB4FL;

  // The two multipliers of the SplitMix64 finalizer, as in java.util.SplittableRandom (Steele,
  // Lea and Flood, 2014).
  private static final long SPLITMIX64_MULTIPLIER_1 = 0xBF58476D1CE4E5B9L;

  private static final long SPLITMIX64_MULTIPLIER_2 = 0x94D049BB133111EBL;

  // The initial capacity of the builder in adjacencyListOf, an estimate it grows past if needed:
  // per node a line without its out-nodes, "1234567 -> []\n", and some slack; per out-node
  // ", 123456".
  private static final int LIST_CHARS_PER_NODE = 16;

  private static final int LIST_CHARS_PER_EDGE = 8;

  private Graphs() {}

  /**
   * Returns {@code graph} with its nodes renumbered by {@code newToOld}: node {@code i} of the
   * result is node {@code newToOld.get(i)} of {@code graph}.
   * <p>
   * The result has the edges of {@code graph} between the nodes that {@code newToOld} maps to;
   * the other nodes are left out, with their edges. The factories of {@link NodeMapping} make the
   * mapping: {@link NodeMapping#forSubgraph(IntList) forSubgraph} selects nodes,
   * {@link NodeMapping#forPermutation(IntList) forPermutation} and
   * {@link NodeMapping#reindex(Graph) reindex} reorder all of them. Data on the nodes follows
   * with the {@code gather} methods of the mapping, data on the edges with
   * {@link #edgeMapping(Graph, Graph, NodeMapping)}. Here, {@code calls} holds the calls between
   * the methods of a program, and the methods that {@code load} reaches become a graph of their
   * own, with their names from {@code names}:
   * {@snippet file="com/kobayami/graphity/examples/Mappings.java" region="subgraph"}
   * To merge nodes into groups instead, use {@link NodePartition#condense()}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount log maxOutDegree).<br>
   * <strong>Memory:</strong> 4 bytes per node and per edge for the result; while it runs,
   * 4 x nodeCount bytes more.
   *
   * @param graph    the graph to renumber
   * @param newToOld the node of {@code graph} for each node of the result
   * @return the renumbered graph
   * @throws IllegalArgumentException  if a node of the result maps to {@code -1}, or two map to
   *                                   the same node
   * @throws IndexOutOfBoundsException if a node of the result maps to an ID that is not a node
   *                                   of {@code graph}
   */
  public static Graph remapped(Graph graph, NodeMapping newToOld) {
    int newNodeCount = newToOld.mapping.length;
    // The general path gives the same, but allocates the inverse over every old node first.
    if (newNodeCount == 0) {
      return new Graph(new int[]{0}, new int[0]);
    }

    // Inverse (old to new) for edge-target translation, built in the same pass that checks every
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
   * Returns the dominator tree of {@code dominators} as a graph, with an edge {@code d -> n} for
   * every node {@code n} whose immediate dominator is {@code d}.
   * <p>
   * The result has the node IDs of the analyzed graph, and its edges point away from the roots
   * of the tree, for a walk down the tree such as with a {@link DfsTraverser}. Its root nodes are
   * the entries, the nodes that only the virtual root above several entries dominates, and the
   * unreachable nodes. To ask whether one node dominates another, use
   * {@link Ancestry#of(Dominators)}, which shares the immediate dominators instead of building
   * this graph.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 4 bytes per node and per edge for the result.
   *
   * @param dominators the dominators of a graph
   * @return the dominator tree
   */
  public static Graph dominatorTree(Dominators dominators) {
    var children = ParentForest.childrenOf(dominators.immediateDominator);
    return new Graph(children.start(), children.members());
  }

  /**
   * Returns the transitive closure of {@code graph}: the same nodes, with an edge {@code u -> v}
   * wherever a path of one or more edges leads from {@code u} to {@code v}.
   * <p>
   * A node has a self-loop in the result if and only if it lies on a cycle. The result can have
   * up to nodeCount<sup>2</sup> edges; for the nodes reachable from a few nodes,
   * {@link #reachableFrom(Graph, IntList)} costs far less.
   * <p>
   * The closure is computed on the SCCs (strongly connected components) of {@code graph}, with a
   * row of bits per SCC for the SCCs it reaches. The rows take sccCount<sup>2</sup> / 8 bytes,
   * about 1.25 GB for 100 000 SCCs.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + sccCount x (sccCount + edgeCount) / 64 +
   * resultEdgeCount log nodeCount), with sccCount the number of SCCs.<br>
   * <strong>Memory:</strong> 4 bytes per node and per edge for the result; while it runs,
   * sccCount<sup>2</sup> / 8 bytes for the rows, which are held together with the result at the
   * peak, and O(nodeCount + edgeCount) more.
   *
   * @param graph the graph to close
   * @return the transitive closure
   * @throws IllegalArgumentException if the closure has more edges than a graph can have, or
   *                                  {@code graph} has more than 2<sup>30</sup> - 5 nodes
   */
  public static Graph transitiveClosure(Graph graph) {
    int n = graph.nodeCount();
    var sccs = Sccs.of(graph);
    int k = sccs.sccCount();
    Graph dag = sccs.condense();
    int[] dagStart = Csr.outAdjacencyStart(dag);
    int[] dagTargets = Csr.outAdjacency(dag);
    int words = (k + 63) >>> 6;

    // SCC IDs are in reverse topological order: every out-node of c in dag has a smaller ID.
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
   * Returns the transitive reduction of {@code dag}: the same nodes, keeping each edge
   * {@code u -> v} unless another path leads from {@code u} to {@code v}.
   * <p>
   * From each node, the result reaches the same nodes as {@code dag}, with as few edges as
   * possible; on a DAG, this result is unique. For a graph with cycles, reduce the DAG of its
   * SCCs, which is {@code Components.sccsOf(graph).condense()}.
   * <p>
   * The reduction is computed with a row of bits per node for the nodes it reaches. The rows
   * take nodeCount<sup>2</sup> / 8 bytes, about 1.25 GB for 100 000 nodes.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount log maxOutDegree + (nodeCount + edgeCount)
   * x nodeCount / 64).<br>
   * <strong>Memory:</strong> 4 bytes per node and per edge for the result; while it runs,
   * nodeCount<sup>2</sup> / 8 bytes for the rows and O(nodeCount + edgeCount) more.
   *
   * @param dag the DAG to reduce
   * @return the transitive reduction
   * @throws IllegalArgumentException if {@code dag} has a cycle, a self-loop included, or if a
   *                                  depth-first path is longer than 2<sup>30</sup> - 5 nodes
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
   * Returns the edge mapping from {@code newGraph} to {@code oldGraph} that goes with the node
   * mapping {@code newToOld}.
   * <p>
   * Edge {@code s -> t} of {@code newGraph} maps to the edge
   * {@code newToOld.get(s) -> newToOld.get(t)} of {@code oldGraph}, or to {@code -1} if
   * {@code oldGraph} has no such edge or {@code s} or {@code t} maps to {@code -1}. For a graph
   * from {@link #remapped(Graph, NodeMapping) remapped(oldGraph, newToOld)}, every edge has its
   * counterpart, and the {@code gather} methods of the result carry data on the edges over to
   * the new graph. Here, {@code siteCount} holds the number of call sites of each call by edge
   * index of {@code calls}, and the subgraph {@code loading} from the example at
   * {@link #remapped(Graph, NodeMapping)} takes these counts along:
   * {@snippet file="com/kobayami/graphity/examples/Mappings.java" region="edgeMapping"}
   * For the opposite direction, from the edges of {@code oldGraph} to those of {@code newGraph},
   * invert the result with {@link EdgeMapping#inverted(int)}. The transposed graph needs
   * {@link #transposedEdgeMapping(Graph)} instead, as its edges run the other way.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount log maxOutDegree), with the node and edge
   * count of {@code newGraph} and the out-degrees of {@code oldGraph}.<br>
   * <strong>Memory:</strong> 4 bytes per edge of {@code newGraph} for the result.
   *
   * @param newGraph the graph whose edges are mapped
   * @param oldGraph the graph they are mapped to
   * @param newToOld the node of {@code oldGraph} for each node of {@code newGraph}, or
   *                 {@code -1}
   * @return the edge mapping
   * @throws IllegalArgumentException  if {@code newToOld.indexCount() != newGraph.nodeCount()}
   * @throws IndexOutOfBoundsException if a node of {@code newGraph} maps to an ID that is not a
   *                                   node of {@code oldGraph}
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
   * Returns the edge mapping from the transposed graph of {@code graph} to {@code graph}.
   * <p>
   * Edge {@code e} of {@code BiAdjacentGraph.of(graph).transposed()} maps to the edge of
   * {@code graph} it reverses: the transposed graph keeps the node IDs, but not the edge indices.
   * The mapping is total, and its {@code gather} methods carry data on the edges over to the
   * transposed graph. Here, {@code siteCount} holds the number of call sites of each call by edge
   * index of {@code calls}, and the counts follow the calls to the transposed graph, which lists
   * the callers of {@code parse}:
   * {@snippet file="com/kobayami/graphity/examples/Mappings.java" region="transposedEdges"}
   * {@link BiAdjacentGraph#transposedEdgeIndex(int)} translates a single edge, in the opposite
   * direction.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x edgeCount bytes for the result; while it runs, 4 x nodeCount
   * bytes more.
   *
   * @param graph the graph whose transposed graph is mapped to it
   * @return the edge mapping from the transposed graph to {@code graph}
   */
  public static EdgeMapping transposedEdgeMapping(Graph graph) {
    return EdgeMapping.forTransposed(graph);
  }

  /**
   * Returns the edge partition that goes with the node partition {@code partition}, from the
   * edges of the partitioned graph to the edges of {@code condensedGraph}.
   * <p>
   * The edges from group {@code g} to group {@code h} all map to the condensed edge {@code g -> h},
   * which is edge {@code condensedGraph.edgeIndexOf(g, h)}. {@link EdgePartition#edgesOf(int)}
   * lists them, and the {@code reduce} methods of the result combine their data. An edge inside a
   * group, or from or to a node in no group, maps to {@code -1}. Here, {@code byPackage} groups the
   * methods of a program by package. Its condensed graph {@code packageCalls} holds the calls
   * between packages (see {@link NodePartition#of(Graph, int[], int)}). {@code byPackageCall} tells
   * which calls between methods are behind the call from {@code service} to {@code model}:
   * {@snippet file="com/kobayami/graphity/examples/Partitions.java" region="edgesOf"}
   * {@code condensedGraph} must be {@code partition.condense()}; only its node count is checked.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount log maxOutDegree), with the node and edge
   * count of the partitioned graph and the out-degrees of {@code condensedGraph}.<br>
   * <strong>Memory:</strong> up to 12 bytes per edge of the partitioned graph for the result.
   *
   * @param condensedGraph the graph that {@code partition.condense()} returned
   * @param partition      the partition of the nodes
   * @return the edge partition, with a group per edge of {@code condensedGraph}
   * @throws IllegalArgumentException if {@code condensedGraph.nodeCount()} is not
   *                                  {@code partition.groupCount()}
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
   * Returns the root nodes of {@code graph} in ascending order. A root node has in-degree 0.
   * <p>
   * A traversal of a DAG or a forest starts from them, as in
   * {@code traverser.run(Graphs.rootNodes(graph))} with a {@link DfsTraverser}.
   * {@link BiAdjacentGraph#isRootNode(int)} checks a single node in O(1).
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 4 bytes per root node for the result.
   *
   * @param graph the graph to search
   * @return the root nodes
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
   * Returns the nodes reachable from {@code source}, in ascending order and with {@code source}
   * included. Equivalent to {@code reachableFrom(graph, IntLists.singleton(source))}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> as for {@link #reachableFrom(Graph, IntList)}.
   *
   * @param graph  the graph to search
   * @param source node ID in {@code 0..nodeCount-1}
   * @return the reachable nodes
   * @throws IndexOutOfBoundsException if {@code source} is out of range
   */
  public static SortedIntView reachableFrom(Graph graph, int source) {
    return reachableFrom(graph, IntLists.singleton(source));
  }

  /**
   * Returns the nodes reachable from at least one of {@code sources}, in ascending order and with
   * the sources included.
   * <p>
   * The search follows the edges in their direction; for the nodes from which a source is
   * reachable, search the transposed graph, as shown at {@link BiAdjacentGraph#transposed()}.
   * A source given twice counts once, and with no sources, no node is reachable. Being sorted, the
   * result goes straight into {@link NodeMapping#forSubgraph(IntList)} for the subgraph of the
   * reachable nodes, as shown at {@link #remapped(Graph, NodeMapping)}. For distances and
   * shortest paths, use {@link BfsDistances}.
   * <p>
   * <strong>Time:</strong> O(sources.size() + nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 bytes per reachable node for the result; while it runs, 1 bit per
   * node and briefly up to 12 bytes per reachable node.
   *
   * @param graph   the graph to search
   * @param sources node IDs, each in {@code 0..nodeCount-1}
   * @return the reachable nodes
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
   * Returns whether {@code graph} is a DAG: a graph without cycles. A self-loop is a cycle.
   * <p>
   * {@link #findCycle(Graph)} returns a cycle where this method finds one.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), stopping at the first cycle.<br>
   * <strong>Memory:</strong> 2 bits per node while it runs, and up to 8 x nodeCount bytes for
   * the stack, which grows with the deepest path.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} has no cycle
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   */
  public static boolean isDag(Graph graph) {
    var checker = new CycleChecker(graph, false);
    checker.run();
    return !checker.hasCycle;
  }

  /**
   * Returns the nodes of a cycle of {@code graph} in the order of its edges, or an empty list if
   * {@code graph} is a DAG.
   * <p>
   * The last node has an edge to the first, and a self-loop is a cycle of one node. The search
   * goes depth-first, from each unvisited node in ascending order and through the out-nodes in
   * ascending order, and returns the first cycle it closes: always the same one, but not
   * necessarily the shortest. Here, the cycle in the dependencies between the packages of a
   * program:
   * {@snippet file="com/kobayami/graphity/examples/GraphOperations.java" region="findCycle"}
   * For a cycle inside a given SCC, search the subgraph of its nodes from
   * {@link #remapped(Graph, NodeMapping)}; the node mapping of the subgraph translates the result
   * back.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), stopping at the first cycle.<br>
   * <strong>Memory:</strong> 4 bytes per node of the cycle for the result; while it runs, 2 bits
   * per node and up to 8 x nodeCount bytes for the stack, which grows with the deepest path.
   *
   * @param graph the graph to search
   * @return the nodes of the cycle, each once, or an empty list; unmodifiable
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   */
  public static IntList findCycle(Graph graph) {
    var checker = new CycleChecker(graph, true);
    checker.run();
    int[] cycle = checker.cycle;
    return cycle == null ? IntLists.EMPTY_LIST : IntLists.unmodifiable(IntArrayList.wrap(cycle));
  }

  /**
   * Returns whether {@code graph} is a forest: a DAG in which every node has in-degree at most 1,
   * so that the edges point away from the root nodes.
   * <p>
   * The empty graph is a forest. For the undirected reading, see
   * {@link #isUndirectedForest(Graph)}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes while it runs.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} is a forest
   */
  public static boolean isForest(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null;
  }

  /**
   * Returns whether {@code graph} is a tree: a forest with at most one root node.
   * <p>
   * The edges of a tree point away from its root. For a tree whose edges point toward the root,
   * check the transposed graph, which is {@code BiAdjacentGraph.of(graph).transposed()}. For the
   * undirected reading, see {@link #isUndirectedTree(Graph)}. The empty graph is a tree.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes while it runs.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} is a tree
   */
  public static boolean isTree(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null && info.rootCount <= 1;
  }

  /**
   * Returns whether {@code graph} is a path: a tree in which every node has out-degree at most 1.
   * <p>
   * The empty graph is a path.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes while it runs.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} is a path
   */
  public static boolean isPath(Graph graph) {
    var info = computeForestInfo(graph);
    return info != null && info.rootCount <= 1 && info.maxOutDegree <= 1;
  }

  /**
   * Returns whether {@code graph} is strongly connected: every node reaches every other along
   * the edges.
   * <p>
   * Answers without the partition of {@link Components#sccsOf(Graph)}. Uses
   * <a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Tarjan's
   * algorithm</a>, without recursion. It starts from node 0 and stops as soon as an SCC without
   * node 0 is complete.
   * Where the SCCs are already computed, {@link Sccs#isStronglyConnected()} answers in O(1). The
   * empty graph is strongly connected, and so is a single node. For the undirected reading, see
   * {@link #isWeaklyConnected(Graph)}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), stopping at the first SCC without node 0.<br>
   * <strong>Memory:</strong> 8 x nodeCount bytes while it runs, and up to 8 x nodeCount bytes for
   * the stack, which grows with the deepest path.
   *
   * @param graph the graph to check
   * @return {@code true} if every node reaches every other
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   */
  public static boolean isStronglyConnected(Graph graph) {
    int n = graph.nodeCount();
    if (n <= 1) {
      return true;
    }
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);
    // Until a second component closes, no node has left Tarjan's stack, so the stack is exactly
    // the set of visited nodes and needs no array of its own.
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

  // The depth-first search of isDag and findCycle, without recursion; it stops at the first edge
  // back to a node on the current path, a self-loop included. The visited and on-stack flags of
  // 16 nodes share one int, 2 bits each, so a node check touches memory once instead of twice.
  private static final class CycleChecker {

    private static final int VISITED = 0b01;
    private static final int ON_STACK = 0b10;

    private final Graph graph;
    private final int[] state;

    // Whether to keep the cycle; isDag needs only the answer.
    private final boolean recordCycle;

    boolean hasCycle;

    // The cycle closed by the first back edge, from its target to its source.
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

  // The root count and the largest out-degree of graph if it is a forest, or null if some node
  // has in-degree > 1 or lies on or below a cycle. Ancestry.of(Graph) makes the same check, with a
  // witness for its message; a change to what counts as a forest changes both.
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
   * Returns whether {@code graph} is bipartite: its nodes split into two sets such that every
   * edge runs from one set to the other.
   * <p>
   * Reads the graph as undirected, so the direction of the edges does not matter. A self-loop
   * makes the graph not bipartite, unlike the other checks of the undirected reading, which
   * ignore self-loops. The empty graph is bipartite.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount x alpha(nodeCount)), stopping at the first
   * cycle of odd length.<br>
   * <strong>Memory:</strong> 10 x nodeCount bytes while it runs.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} is bipartite
   * @throws IllegalArgumentException if {@code graph} has more than 2<sup>30</sup> - 5 nodes
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
   * Returns whether {@code graph} is a tree when read as undirected: connected and without
   * cycles.
   * <p>
   * Unlike for {@link #isTree(Graph)}, the direction of the edges does not matter: edges toward
   * the root, or in mixed directions, make a tree as well. The edges {@code a -> b} and
   * {@code b -> a} count as one, so a tree may store its edges in both directions, and self-loops
   * are ignored. The empty graph is a tree.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount x alpha(nodeCount)), plus O(log maxOutDegree)
   * for each pair {@code a -> b}, {@code b -> a}; O(1) for more than 3 x nodeCount - 2 edges, too
   * many for any tree.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes while it runs.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} is a tree when read as undirected
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
   * Returns whether {@code graph} is a forest when read as undirected: without cycles.
   * <p>
   * Unlike for {@link #isForest(Graph)}, the direction of the edges does not matter. The edges
   * {@code a -> b} and {@code b -> a} count as one, so they make no cycle, and self-loops are
   * ignored. The empty graph is a forest.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount x alpha(nodeCount)), plus O(log maxOutDegree)
   * for each pair {@code a -> b}, {@code b -> a}; O(1) for more than 3 x nodeCount - 2 edges, too
   * many for any forest.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes while it runs.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} is a forest when read as undirected
   */
  public static boolean isUndirectedForest(Graph graph) {
    if (exceedsAcyclicEdgeBound(graph)) {
      return false;
    }
    return undirectedMergeCount(graph) >= 0;
  }

  /**
   * Returns whether {@code graph} is weakly connected: connected when the direction of the edges
   * is ignored.
   * <p>
   * Answers without the partition of {@link Components#wccsOf(Graph)}, and stops as soon as all
   * nodes are connected. The empty graph is weakly connected, and so is a single node. For the
   * directed reading, see {@link #isStronglyConnected(Graph)}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount x alpha(nodeCount)), stopping once all nodes
   * are connected.<br>
   * <strong>Memory:</strong> 5 x nodeCount bytes while it runs.
   *
   * @param graph the graph to check
   * @return {@code true} if {@code graph} is weakly connected
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

  // Whether graph holds more edges than any acyclic undirected reading could: at most
  // nodeCount - 1 undirected edges, each stored in up to two directions, plus at most one
  // self-loop per node. A shortcut only: undirectedMergeCount rejects such a graph as well,
  // within its first 3 x nodeCount edges, but this answers in O(1) and allocates nothing.
  private static boolean exceedsAcyclicEdgeBound(Graph graph) {
    int n = graph.nodeCount();
    return graph.edgeCount() > (n == 0 ? 0 : 3L * n - 2);
  }

  // Merges the endpoints of every undirected edge and returns the number of merges, so that the
  // nodes form nodeCount - result components; or -1 as soon as an edge closes an undirected
  // cycle. Self-loops are skipped.
  //
  // Nodes are walked in ascending order, and that is what makes a failed union classifiable. A
  // failure means both endpoints are already connected, so the edge is either the second stored
  // direction of an edge already counted or a genuine extra edge closing a cycle. For v > u the
  // opposite direction sits in a list not yet visited, so this is the first encounter with {u,v}
  // and the failure is a cycle. For v < u one binary search in v's out-adjacency decides it. A
  // successful union never needs the test: had the opposite direction been counted, the
  // endpoints would already be connected and the union would have failed. Searching always would
  // cost a search per edge; searching never would report every symmetrically stored edge as a
  // cycle.
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
   * Returns the whole graph as text: one line per node with its out-nodes, in ascending order.
   * <p>
   * Each line ends with a line feed. For the control flow of a method with a loop:
   * {@snippet file="com/kobayami/graphity/examples/GraphOperations.java" region="adjacencyListOf"}
   * Unlike {@link Graph#toString()}, which is cut short for large graphs, this lists every node
   * and edge; a {@link BiAdjacentGraph} lists its out-nodes. The text of a graph with a few
   * hundred million edges is longer than a {@code String} can be and ends in an
   * {@link OutOfMemoryError}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 1 byte per character for the result; while it runs, about as much
   * again.
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
   * Returns a 64-bit hash of the nodes and edges of {@code graph}, consistent with
   * {@link Graph#equals(Object)}.
   * <p>
   * Equal graphs have equal hashes, and different graphs rarely do. A {@link BiAdjacentGraph} and a
   * {@code Graph} with the same nodes and edges count as equal. Like {@link Graph#hashCode()}, the
   * hash is computed on every call: compute it once and keep it, for instance as the key of a cache
   * of results per graph, or to see when a transformation stops changing a graph. It depends on
   * nothing but the nodes and edges, so it is the same in every run and on every JVM; a release of
   * Graphity that changes it says so in its changelog. Every implementation of Graphity computes
   * the same hash. Equal hashes do not prove equal graphs, and graphs can be made to collide on
   * purpose: compare with {@link Graph#equals(Object)} where it matters.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   *
   * @param graph the graph to hash
   * @return the hash
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

  // One order-sensitive step of topologyHash.
  private static long mixInto(long h, int value) {
    return Long.rotateLeft(h ^ (value * HASH_GOLDEN_GAMMA), 29) * HASH_STEP_PRIME;
  }

  /**
   * Returns the edges added and removed between {@code before} and {@code after}. Both are graphs
   * over the same nodes.
   * <p>
   * Added edges come with their edge indices in {@code after}, removed edges with theirs in
   * {@code before}. With these edge indices, the caller can look up data on the edges of either
   * graph in arrays indexed by edge index. Both lists are in edge index order. An empty diff means
   * that the graphs are equal.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount), with the edges of both graphs.<br>
   * <strong>Memory:</strong> 12 bytes per added or removed edge for the result; while it runs,
   * up to 18 bytes more per such edge.
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
