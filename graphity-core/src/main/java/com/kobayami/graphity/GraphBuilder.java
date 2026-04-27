package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

/**
 * Mutable, add-only builder for constructing immutable {@link Graph} instances.
 * <p>
 * Nodes are added explicitly via {@link #addNode()} or {@link #addNodes(int)}, returning
 * sequential IDs (0, 1, 2, …). Edges are added via {@link #addEdge(int, int)} or
 * {@link #addEdges(int, IntList)}, referencing existing node IDs. An entire existing graph
 * can be imported via {@link #addGraph(Graph)}.
 * <p>
 * The builder is <strong>add-only</strong>: nodes and edges cannot be removed. For subgraph
 * extraction or node removal, build the graph first and use
 * {@link Graphs#remapped(Graph, NodeMapping)} with {@link NodeMapping#forSubgraph(java.util.BitSet)}.
 * <p>
 * Calling {@link #build()} produces an immutable {@link Graph} with sorted, deduplicated
 * adjacency lists. The builder is <strong>not consumed</strong> — further modifications and
 * subsequent builds are allowed.
 *
 * <pre>{@code
 * // Build from scratch
 * var builder = new GraphBuilder();
 * builder.addNodes(4);
 * builder.addEdge(0, 1);
 * builder.addEdge(0, 2);
 * builder.addEdge(1, 3);
 * Graph graph = builder.build();
 *
 * // Merge two graphs with bridge edges
 * var merger = new GraphBuilder();
 * int off1 = merger.addGraph(graph1);
 * int off2 = merger.addGraph(graph2);
 * merger.addEdge(off1 + nodeA, off2 + nodeB);
 * Graph merged = merger.build();
 *
 * // Copy edges from existing graph (zero-copy via IntList)
 * var builder = new GraphBuilder();
 * builder.addNodes(oldGraph.nodeCount());
 * for (int node = 0; node < oldGraph.nodeCount(); node++) {
 *     builder.addEdges(node, oldGraph.outNodes(node));
 * }
 * Graph copy = builder.build();
 * }</pre>
 *
 * <p>Edge node references are validated via {@code assert} statements — enable assertions
 * ({@code -ea}) during development to catch invalid node references. In production,
 * assertions are disabled and validation has zero cost.
 *
 * @see Graph
 * @see Graphs#remapped(Graph, NodeMapping)
 */
public final class GraphBuilder {

  private int nodeCount;
  private final IntArrayList edgeSources;
  private final IntArrayList edgeTargets;

  /**
   * Creates an empty builder with no nodes or edges.
   */
  public GraphBuilder() {
    this.edgeSources = new IntArrayList();
    this.edgeTargets = new IntArrayList();
  }

  // ---- Nodes ----

  /**
   * Adds a single node and returns its ID.
   * Node IDs are assigned sequentially starting from 0.
   *
   * @return the ID of the new node
   */
  public int addNode() {
    return nodeCount++;
  }

  /**
   * Adds {@code count} nodes and returns the ID of the first added node.
   * The added nodes have IDs {@code firstId..firstId+count-1}.
   *
   * @param count number of nodes to add (must be ≥ 0)
   * @return the ID of the first added node
   * @throws IllegalArgumentException if {@code count} is negative
   */
  public int addNodes(int count) {
    if (count < 0) throw new IllegalArgumentException("count must be non-negative: " + count);
    int firstId = nodeCount;
    nodeCount += count;
    return firstId;
  }

  // ---- Edges ----

  /**
   * Adds a directed edge from {@code source} to {@code target}.
   * Both nodes must already exist (checked via assertions).
   * <p>
   * Duplicate edges are allowed during building — they are deduplicated when
   * {@link #build()} is called.
   *
   * @param source source node ID
   * @param target target node ID
   */
  public void addEdge(int source, int target) {
    assert source >= 0 && source < nodeCount
        : "source node does not exist: " + source + " (nodeCount=" + nodeCount + ")";
    assert target >= 0 && target < nodeCount
        : "target node does not exist: " + target + " (nodeCount=" + nodeCount + ")";
    edgeSources.add(source);
    edgeTargets.add(target);
  }

  /**
   * Adds directed edges from {@code source} to each target in the list.
   * All nodes must already exist (checked via assertions).
   * <p>
   * Accepts any {@link IntList}, including {@link SortedIntView} from
   * {@link Graph#outNodes(int)} — enabling zero-copy edge transfer from existing graphs.
   *
   * @param source  source node ID
   * @param targets list of target node IDs
   */
  public void addEdges(int source, IntList targets) {
    assert source >= 0 && source < nodeCount
        : "source node does not exist: " + source + " (nodeCount=" + nodeCount + ")";
    int count = targets.size();
    edgeSources.ensureCapacity(edgeSources.size() + count);
    edgeTargets.ensureCapacity(edgeTargets.size() + count);
    for (int i = 0; i < count; i++) {
      int target = targets.getInt(i);
      assert target >= 0 && target < nodeCount
          : "target node does not exist: " + target + " (nodeCount=" + nodeCount + ")";
      edgeSources.add(source);
      edgeTargets.add(target);
    }
  }

  // ---- Graph import ----

  /**
   * Adds all nodes and edges of the given graph to this builder.
   * <p>
   * Node IDs from the source graph are shifted by the current node count (offset).
   * The returned offset can be used to reference the imported nodes:
   * node {@code i} in the source graph becomes node {@code offset + i} in this builder.
   * <p>
   * <strong>Time:</strong> O(sourceGraph.nodeCount() + sourceGraph.edgeCount()).
   *
   * @param graph the graph to import
   * @return the offset (= ID of the first imported node)
   */
  public int addGraph(Graph graph) {
    int offset = nodeCount;
    nodeCount += graph.nodeCount();

    int srcEdgeCount = graph.edgeCount();
    edgeSources.ensureCapacity(edgeSources.size() + srcEdgeCount);
    edgeTargets.ensureCapacity(edgeTargets.size() + srcEdgeCount);

    var adjacency = graph.outAdjacency;
    var adjacencyStart = graph.outAdjacencyStart;
    int srcNodeCount = graph.nodeCount();

    for (int node = 0; node < srcNodeCount; node++) {
      int end = adjacencyStart[node + 1];
      for (int i = adjacencyStart[node]; i < end; i++) {
        edgeSources.add(node + offset);
        edgeTargets.add(adjacency[i] + offset);
      }
    }

    return offset;
  }

  // ---- Build ----

  /**
   * Builds an immutable {@link Graph} from the current state.
   * <p>
   * Adjacency lists are sorted ascending and deduplicated (duplicate edges are silently
   * removed). The builder is not consumed — further modifications and subsequent builds
   * are allowed.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount × log d̄) where d̄ is the average
   * out-degree (sorting per node).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result.
   *
   * @return an immutable graph
   */
  public Graph build() {
    if (nodeCount == 0) {
      return new Graph(new int[]{0}, new int[0]);
    }

    int rawEdgeCount = edgeSources.size();
    int[] sources = edgeSources.elements();
    int[] targets = edgeTargets.elements();

    // Pass 1: count out-degrees.
    var outStart = new int[nodeCount + 1];
    for (int i = 0; i < rawEdgeCount; i++) {
      outStart[sources[i] + 1]++;
    }

    // Prefix sum: convert degrees to CSR start offsets.
    for (int n = 0; n < nodeCount; n++) {
      outStart[n + 1] += outStart[n];
    }

    // Pass 2: distribute edge targets into per-node buckets.
    var outAdj = new int[outStart[nodeCount]];
    var writePos = Arrays.copyOf(outStart, nodeCount);
    for (int i = 0; i < rawEdgeCount; i++) {
      outAdj[writePos[sources[i]]++] = targets[i];
    }

    // Pass 3: sort each adjacency list and deduplicate in a single compacting pass.
    // Safe because compactEnd ≤ outStart[node] for all nodes (we only remove elements).
    int compactEnd = 0;
    for (int node = 0; node < nodeCount; node++) {
      int start = outStart[node];
      int end = outStart[node + 1];
      Arrays.sort(outAdj, start, end);

      outStart[node] = compactEnd;
      if (start < end) {
        outAdj[compactEnd++] = outAdj[start];
        for (int r = start + 1; r < end; r++) {
          if (outAdj[r] != outAdj[r - 1]) {
            outAdj[compactEnd++] = outAdj[r];
          }
        }
      }
    }
    outStart[nodeCount] = compactEnd;

    // Trim adjacency array if deduplication removed edges.
    if (compactEnd < outAdj.length) {
      outAdj = Arrays.copyOf(outAdj, compactEnd);
    }

    return new Graph(outStart, outAdj);
  }
}
