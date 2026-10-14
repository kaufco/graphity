package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

/**
 * Mutable, add-only builder for constructing immutable {@link Graph} instances.
 * <p>
 * Nodes are added explicitly via {@link #addNode()} or {@link #addNodes(int)}, returning
 * sequential IDs (0, 1, 2, …). Edges are added via {@link #addEdge(int, int)} or one of the
 * batch methods, referencing existing node IDs. An entire existing graph can be imported via
 * {@link #addGraph(Graph)}.
 * <p>
 * The builder is <strong>add-only</strong>: nodes and edges cannot be removed. For subgraph
 * extraction or node removal, build the graph first and use
 * {@link Graphs#remapped(Graph, NodeMapping)} with {@link NodeMapping#forSubgraph(java.util.BitSet)}.
 * <p>
 * Calling {@link #build()} produces an immutable {@link Graph} with sorted, deduplicated
 * adjacency lists. The builder is <strong>not consumed</strong> - further modifications and
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
 * }</pre>
 *
 * <h2>Passing node IDs</h2>
 *
 * The fan-out, fan-in and path methods accept their node IDs as loose arguments, as an
 * {@code int[]}, or as an {@link IntList} - including the {@link SortedIntView} that
 * {@link Graph#outNodes(int)} and its relatives return, so a result of this library can be fed
 * back without copying it into an array first:
 *
 * <pre>{@code
 * builder.addEdges(source, 1, 2, 3);            // loose arguments
 * builder.addEdges(source, targetArray);        // int[]
 * builder.addEdges(source, graph.outNodes(n));  // IntList, no copy
 * }</pre>
 *
 * A list is read during the call and never retained. {@link #addEdgePairs(int...)} has no
 * {@link IntList} form: no result of this library is a flat sequence of alternating endpoints.
 * {@link #addEdge(int, int)}, {@link #addChild(int)} and {@link #addParent(int)} are
 * fixed-arity: they take single IDs and are meant to run per node, where a varargs array per
 * call would dominate the cost.
 *
 * <h2>Creating nodes on reference</h2>
 *
 * Every edge-adding method that only references node IDs - as opposed to
 * {@link #addChild(int)} and its relatives, which allocate them - has a twin with an
 * {@code AndNodes} suffix that raises the node count to cover the highest ID it references,
 * instead of requiring the nodes to exist. Since node IDs are contiguous, referencing ID
 * {@code k} creates every ID up to {@code k}; the ones in between become isolated nodes. This
 * is what makes externally numbered input work:
 *
 * <pre>{@code
 * var builder = new GraphBuilder();
 * builder.addEdgeAndNodes(7, 3);   // 8 nodes, one edge
 * }</pre>
 *
 * <h2>Validation</h2>
 *
 * By default, node references are checked via {@code assert} - enable assertions
 * ({@code -ea}) during development to catch invalid node references. With assertions
 * disabled, what remains per call is a single predictable branch on the strict-mode flag.
 * <p>
 * {@link #strict()} replaces this with unconditional validation that throws
 * {@link IndexOutOfBoundsException}. It must be called before the first edge is added, so
 * that a builder can never hold a mix of validated and unvalidated edges. A batch method that
 * rejects an ID adds none of its edges.
 *
 * @see Graph
 * @see Graphs#remapped(Graph, NodeMapping)
 */
public final class GraphBuilder {

  private int nodeCount;
  private final IntArrayList edgeSources;
  private final IntArrayList edgeTargets;
  private boolean strict;

  /**
   * Creates an empty builder with no nodes or edges.
   */
  public GraphBuilder() {
    this.edgeSources = new IntArrayList();
    this.edgeTargets = new IntArrayList();
  }

  /**
   * Enables strict validation of node references for the lifetime of this builder.
   * <p>
   * Every method that consumes a node ID rejects out-of-range values with an
   * {@link IndexOutOfBoundsException} instead of relying on assertions, and a batch method
   * that rejects an ID adds none of its edges. Methods that create nodes on reference still
   * do so; only the IDs they consume are validated.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @throws IllegalStateException if an edge has already been added
   */
  public void strict() {
    if (!edgeSources.isEmpty()) {
      throw new IllegalStateException("strict() must be called before the first edge is added");
    }
    strict = true;
  }

  /**
   * Grows the internal edge storage so that at least {@code totalEdges} edges fit without
   * further reallocation.
   * <p>
   * The argument is the total number of edges this builder will hold, not an increment. Can be
   * called at any point, including part-way through construction, and has no effect on the
   * graph that {@link #build()} produces.
   * <p>
   * <strong>Time:</strong> O(totalEdges) if it reallocates, O(1) if the capacity already
   * suffices.<br>
   * <strong>Memory:</strong> two int arrays of {@code totalEdges} elements if it reallocates,
   * none otherwise.
   *
   * @param totalEdges expected total number of edges (must be ≥ 0)
   * @throws IllegalArgumentException if {@code totalEdges} is negative
   */
  public void ensureEdgeCapacity(int totalEdges) {
    if (totalEdges < 0) {
      throw new IllegalArgumentException("totalEdges must be non-negative: " + totalEdges);
    }
    edgeSources.ensureCapacity(totalEdges);
    edgeTargets.ensureCapacity(totalEdges);
  }

  /**
   * Adds a single node and returns its ID.
   * Node IDs are assigned sequentially starting from 0.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return new node ID
   */
  public int addNode() {
    return nodeCount++;
  }

  /**
   * Adds {@code count} nodes and returns the ID of the first added node.
   * The added nodes have IDs {@code firstId..firstId+count-1}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param count number of nodes to add (must be ≥ 0)
   * @return first added node ID
   * @throws IllegalArgumentException if {@code count} is negative
   */
  public int addNodes(int count) {
    if (count < 0) throw new IllegalArgumentException("count must be non-negative: " + count);
    int firstId = nodeCount;
    nodeCount += count;
    return firstId;
  }

  /**
   * Adds a node and an edge from {@code parent} to it.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param parent parent node ID, must already exist
   * @return new node ID
   * @throws IndexOutOfBoundsException in strict mode if {@code parent} does not exist
   */
  public int addChild(int parent) {
    checkNode(parent, "parent");
    int child = nodeCount++;
    edgeSources.add(parent);
    edgeTargets.add(child);
    return child;
  }

  /**
   * Adds {@code count} nodes and an edge from {@code parent} to each of them.
   * The added nodes have IDs {@code firstId..firstId+count-1}.
   * <p>
   * <strong>Time:</strong> O(count) amortized.
   *
   * @param parent parent node ID, must already exist
   * @param count  number of children to add (must be ≥ 0)
   * @return first added node ID
   * @throws IllegalArgumentException  if {@code count} is negative
   * @throws IndexOutOfBoundsException in strict mode if {@code parent} does not exist
   */
  public int addChildren(int parent, int count) {
    if (count < 0) throw new IllegalArgumentException("count must be non-negative: " + count);
    checkNode(parent, "parent");
    int firstId = nodeCount;
    nodeCount += count;
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(parent);
      edgeTargets.add(firstId + i);
    }
    return firstId;
  }

  /**
   * Adds a node and an edge from it to {@code child}.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param child child node ID, must already exist
   * @return new node ID
   * @throws IndexOutOfBoundsException in strict mode if {@code child} does not exist
   */
  public int addParent(int child) {
    checkNode(child, "child");
    int parent = nodeCount++;
    edgeSources.add(parent);
    edgeTargets.add(child);
    return parent;
  }

  /**
   * Adds {@code count} nodes and an edge from each of them to {@code child}.
   * The added nodes have IDs {@code firstId..firstId+count-1}.
   * <p>
   * <strong>Time:</strong> O(count) amortized.
   *
   * @param child child node ID, must already exist
   * @param count number of parents to add (must be ≥ 0)
   * @return first added node ID
   * @throws IllegalArgumentException  if {@code count} is negative
   * @throws IndexOutOfBoundsException in strict mode if {@code child} does not exist
   */
  public int addParents(int child, int count) {
    if (count < 0) throw new IllegalArgumentException("count must be non-negative: " + count);
    checkNode(child, "child");
    int firstId = nodeCount;
    nodeCount += count;
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(firstId + i);
      edgeTargets.add(child);
    }
    return firstId;
  }

  /**
   * Adds a directed edge from {@code source} to {@code target}.
   * Both nodes must already exist.
   * <p>
   * Duplicate edges are allowed during building - they are deduplicated when
   * {@link #build()} is called.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param source source node ID
   * @param target target node ID
   * @throws IndexOutOfBoundsException in strict mode if either node does not exist
   */
  public void addEdge(int source, int target) {
    checkNode(source, "source");
    checkNode(target, "target");
    edgeSources.add(source);
    edgeTargets.add(target);
  }

  /**
   * Adds a directed edge from {@code source} to {@code target}, creating both nodes if they do
   * not exist yet.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param source source node ID (must be ≥ 0)
   * @param target target node ID (must be ≥ 0)
   * @throws IndexOutOfBoundsException if either ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addEdgeAndNodes(int source, int target) {
    checkCreatable(source);
    checkCreatable(target);
    raiseNodeCount(Math.max(source, target));
    edgeSources.add(source);
    edgeTargets.add(target);
  }

  /**
   * Adds directed edges from {@code source} to each target in the list.
   * All nodes must already exist.
   * <p>
   * Accepts any {@link IntList}, including {@link SortedIntView} from
   * {@link Graph#outNodes(int)} - enabling zero-copy edge transfer from existing graphs.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code targets.size()}.
   *
   * @param source  source node ID
   * @param targets list of target node IDs
   * @throws IndexOutOfBoundsException in strict mode if any node does not exist
   */
  public void addEdges(int source, IntList targets) {
    checkNode(source, "source");
    checkNodes(targets, "target");
    int count = targets.size();
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(source);
      edgeTargets.add(targets.getInt(i));
    }
  }

  /**
   * Adds directed edges from {@code source} to each target in the list, creating any node that
   * does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code targets.size()}.
   *
   * @param source  source node ID (must be ≥ 0)
   * @param targets list of target node IDs (each must be ≥ 0)
   * @throws IndexOutOfBoundsException if any ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addEdgesAndNodes(int source, IntList targets) {
    checkCreatable(source);
    raiseNodeCount(Math.max(source, highestCreatable(targets)));
    int count = targets.size();
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(source);
      edgeTargets.add(targets.getInt(i));
    }
  }

  /**
   * Adds directed edges from {@code source} to each of the given targets.
   * All nodes must already exist.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code targets.length}.
   *
   * @param source  source node ID
   * @param targets target node IDs
   * @throws IndexOutOfBoundsException in strict mode if any node does not exist
   */
  public void addEdges(int source, int... targets) {
    checkNode(source, "source");
    checkNodes(targets, "target");
    reserve(targets.length);
    for (int target : targets) {
      edgeSources.add(source);
      edgeTargets.add(target);
    }
  }

  /**
   * Adds directed edges from {@code source} to each of the given targets, creating any node
   * that does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code targets.length}.
   *
   * @param source  source node ID (must be ≥ 0)
   * @param targets target node IDs (each must be ≥ 0)
   * @throws IndexOutOfBoundsException if any ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addEdgesAndNodes(int source, int... targets) {
    checkCreatable(source);
    raiseNodeCount(Math.max(source, highestCreatable(targets)));
    reserve(targets.length);
    for (int target : targets) {
      edgeSources.add(source);
      edgeTargets.add(target);
    }
  }

  /**
   * Adds directed edges from each source in the list to {@code target}.
   * All nodes must already exist.
   * <p>
   * The fan-in counterpart of {@link #addEdges(int, IntList)}.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code sources.size()}.
   *
   * @param target  target node ID
   * @param sources list of source node IDs
   * @throws IndexOutOfBoundsException in strict mode if any node does not exist
   */
  public void addInEdges(int target, IntList sources) {
    checkNode(target, "target");
    checkNodes(sources, "source");
    int count = sources.size();
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(sources.getInt(i));
      edgeTargets.add(target);
    }
  }

  /**
   * Adds directed edges from each source in the list to {@code target}, creating any node that
   * does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code sources.size()}.
   *
   * @param target  target node ID (must be ≥ 0)
   * @param sources list of source node IDs (each must be ≥ 0)
   * @throws IndexOutOfBoundsException if any ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addInEdgesAndNodes(int target, IntList sources) {
    checkCreatable(target);
    raiseNodeCount(Math.max(target, highestCreatable(sources)));
    int count = sources.size();
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(sources.getInt(i));
      edgeTargets.add(target);
    }
  }

  /**
   * Adds directed edges from each of the given sources to {@code target}.
   * All nodes must already exist.
   * <p>
   * The fan-in counterpart of {@link #addEdges(int, int...)}.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code sources.length}.
   *
   * @param target  target node ID
   * @param sources source node IDs
   * @throws IndexOutOfBoundsException in strict mode if any node does not exist
   */
  public void addInEdges(int target, int... sources) {
    checkNode(target, "target");
    checkNodes(sources, "source");
    reserve(sources.length);
    for (int source : sources) {
      edgeSources.add(source);
      edgeTargets.add(target);
    }
  }

  /**
   * Adds directed edges from each of the given sources to {@code target}, creating any node
   * that does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code sources.length}.
   *
   * @param target  target node ID (must be ≥ 0)
   * @param sources source node IDs (each must be ≥ 0)
   * @throws IndexOutOfBoundsException if any ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addInEdgesAndNodes(int target, int... sources) {
    checkCreatable(target);
    raiseNodeCount(Math.max(target, highestCreatable(sources)));
    reserve(sources.length);
    for (int source : sources) {
      edgeSources.add(source);
      edgeTargets.add(target);
    }
  }

  /**
   * Chains the nodes in the list with edges: {@code nodes[0] → nodes[1] → … → nodes[n-1]}.
   * All nodes must already exist.
   * <p>
   * Fewer than two nodes add no edge.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code nodes.size()}.
   *
   * @param nodes list of node IDs to chain, in order
   * @throws IndexOutOfBoundsException in strict mode if any node does not exist
   */
  public void addPath(IntList nodes) {
    checkNodes(nodes, "path");
    int count = nodes.size();
    reserve(Math.max(count - 1, 0));
    for (int i = 1; i < count; i++) {
      edgeSources.add(nodes.getInt(i - 1));
      edgeTargets.add(nodes.getInt(i));
    }
  }

  /**
   * Chains the nodes in the list with edges, creating any node that does not exist yet.
   * <p>
   * Fewer than two nodes add no edge, but every ID passed is still created.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code nodes.size()}.
   *
   * @param nodes list of node IDs to chain, in order (each must be ≥ 0)
   * @throws IndexOutOfBoundsException if any ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addPathAndNodes(IntList nodes) {
    raiseNodeCount(highestCreatable(nodes));
    int count = nodes.size();
    reserve(Math.max(count - 1, 0));
    for (int i = 1; i < count; i++) {
      edgeSources.add(nodes.getInt(i - 1));
      edgeTargets.add(nodes.getInt(i));
    }
  }

  /**
   * Chains the given nodes with edges: {@code nodes[0] → nodes[1] → … → nodes[n-1]}.
   * All nodes must already exist.
   * <p>
   * Fewer than two nodes add no edge.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code nodes.length}.
   *
   * @param nodes node IDs to chain, in order
   * @throws IndexOutOfBoundsException in strict mode if any node does not exist
   */
  public void addPath(int... nodes) {
    checkNodes(nodes, "path");
    reserve(Math.max(nodes.length - 1, 0));
    for (int i = 1; i < nodes.length; i++) {
      edgeSources.add(nodes[i - 1]);
      edgeTargets.add(nodes[i]);
    }
  }

  /**
   * Chains the given nodes with edges, creating any node that does not exist yet.
   * <p>
   * Fewer than two nodes add no edge, but every ID passed is still created.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code nodes.length}.
   *
   * @param nodes node IDs to chain, in order (each must be ≥ 0)
   * @throws IndexOutOfBoundsException if any ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addPathAndNodes(int... nodes) {
    raiseNodeCount(highestCreatable(nodes));
    reserve(Math.max(nodes.length - 1, 0));
    for (int i = 1; i < nodes.length; i++) {
      edgeSources.add(nodes[i - 1]);
      edgeTargets.add(nodes[i]);
    }
  }

  /**
   * Adds one edge per pair of consecutive arguments: {@code fromToPairs[0] → fromToPairs[1]},
   * {@code fromToPairs[2] → fromToPairs[3]}, and so on.
   * All nodes must already exist.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code fromToPairs.length}.
   *
   * @param fromToPairs alternating source and target node IDs
   * @throws IllegalArgumentException  if the number of arguments is odd
   * @throws IndexOutOfBoundsException in strict mode if any node does not exist
   */
  public void addEdgePairs(int... fromToPairs) {
    checkPairCount(fromToPairs.length);
    checkNodes(fromToPairs, "endpoint");
    addPairs(fromToPairs);
  }

  /**
   * Adds one edge per pair of consecutive arguments, creating any node that does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) where k = {@code fromToPairs.length}.
   *
   * @param fromToPairs alternating source and target node IDs (each must be ≥ 0)
   * @throws IllegalArgumentException  if the number of arguments is odd
   * @throws IndexOutOfBoundsException if any ID is negative or {@code Integer.MAX_VALUE}
   */
  public void addEdgePairsAndNodes(int... fromToPairs) {
    checkPairCount(fromToPairs.length);
    raiseNodeCount(highestCreatable(fromToPairs));
    addPairs(fromToPairs);
  }

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

    reserve(graph.edgeCount());

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

  /**
   * Builds an immutable {@link Graph} from the current state.
   * <p>
   * Adjacency lists are sorted ascending and deduplicated (duplicate edges are silently
   * removed), so an added edge does not keep the position at which it was added;
   * {@link Graph#edgeIndexOf(int, int)} finds its final edge index. The builder is not
   * consumed - further modifications and subsequent builds are allowed.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount x log d̄) where d̄ is the average
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

  /**
   * Current capacity of the internal edge storage. Exists so that tests can observe that
   * {@link #ensureEdgeCapacity(int)} actually prevents reallocation.
   */
  int edgeCapacity() {
    return Math.min(edgeSources.elements().length, edgeTargets.elements().length);
  }

  private void addPairs(int[] fromToPairs) {
    reserve(fromToPairs.length >> 1);
    for (int i = 0; i < fromToPairs.length; i += 2) {
      edgeSources.add(fromToPairs[i]);
      edgeTargets.add(fromToPairs[i + 1]);
    }
  }

  /**
   * Validates a consumed node ID: unconditionally in strict mode, via assertion otherwise.
   */
  private void checkNode(int node, String role) {
    if (strict) {
      if (node < 0 || node >= nodeCount) throw notFound(role, node);
    } else {
      assert node >= 0 && node < nodeCount
          : role + " node does not exist: " + node + " (nodeCount=" + nodeCount + ")";
    }
  }

  /**
   * Validates every consumed node ID before any of them is written, so that a rejected batch
   * leaves the builder unchanged.
   */
  private void checkNodes(int[] nodes, String role) {
    if (strict) {
      for (int node : nodes) {
        if (node < 0 || node >= nodeCount) throw notFound(role, node);
      }
    } else {
      assert allExist(nodes) : missingMessage(nodes, role);
    }
  }

  private void checkNodes(IntList nodes, String role) {
    if (strict) {
      for (int i = 0; i < nodes.size(); i++) {
        int node = nodes.getInt(i);
        if (node < 0 || node >= nodeCount) throw notFound(role, node);
      }
    } else {
      assert allExist(nodes) : missingMessage(nodes, role);
    }
  }

  private boolean allExist(int[] nodes) {
    for (int node : nodes) {
      if (node < 0 || node >= nodeCount) return false;
    }
    return true;
  }

  private boolean allExist(IntList nodes) {
    for (int i = 0; i < nodes.size(); i++) {
      int node = nodes.getInt(i);
      if (node < 0 || node >= nodeCount) return false;
    }
    return true;
  }

  /** Only ever evaluated on the failing assertion path. */
  private String missingMessage(int[] nodes, String role) {
    for (int node : nodes) {
      if (node < 0 || node >= nodeCount) return notFound(role, node).getMessage();
    }
    return "";
  }

  private String missingMessage(IntList nodes, String role) {
    for (int i = 0; i < nodes.size(); i++) {
      int node = nodes.getInt(i);
      if (node < 0 || node >= nodeCount) return notFound(role, node).getMessage();
    }
    return "";
  }

  private IndexOutOfBoundsException notFound(String role, int node) {
    return new IndexOutOfBoundsException(
        role + " node does not exist: " + node + " (nodeCount=" + nodeCount + ")");
  }

  /**
   * Rejects IDs that cannot be created. {@code Integer.MAX_VALUE} is excluded because the node
   * count that would have to hold it does not fit in an {@code int}.
   */
  private static void checkCreatable(int node) {
    if (node < 0) {
      throw new IndexOutOfBoundsException("node ID must be non-negative: " + node);
    }
    if (node == Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("node ID must be below Integer.MAX_VALUE: " + node);
    }
  }

  /**
   * Validates every ID before any node is created, and returns the highest, or -1 if empty.
   */
  private static int highestCreatable(int[] nodes) {
    int highest = -1;
    for (int node : nodes) {
      checkCreatable(node);
      if (node > highest) highest = node;
    }
    return highest;
  }

  private static int highestCreatable(IntList nodes) {
    int highest = -1;
    for (int i = 0; i < nodes.size(); i++) {
      int node = nodes.getInt(i);
      checkCreatable(node);
      if (node > highest) highest = node;
    }
    return highest;
  }

  /**
   * Raises the node count so that {@code node} exists. Every ID below it is created too,
   * because node IDs are contiguous. A negative argument leaves the count untouched.
   */
  private void raiseNodeCount(int node) {
    if (node >= nodeCount) {
      nodeCount = node + 1;
    }
  }

  private static void checkPairCount(int length) {
    if ((length & 1) != 0) {
      throw new IllegalArgumentException(
          "expected an even number of node IDs, got " + length);
    }
  }

  /**
   * Reserves room for {@code count} additional edges in both edge lists.
   */
  private void reserve(int count) {
    edgeSources.ensureCapacity(edgeSources.size() + count);
    edgeTargets.ensureCapacity(edgeTargets.size() + count);
  }
}
