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
 * {@link Graphs#remapped(Graph, NodeMapping)} with
 * {@link NodeMapping#forSubgraph(java.util.BitSet)}.
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
 * builder.addOutEdges(source, 1, 2, 3);            // loose arguments
 * builder.addOutEdges(source, targetArray);        // int[]
 * builder.addOutEdges(source, graph.outNodes(n));  // IntList, no copy
 * }</pre>
 *
 * A list is read during the call and never retained. {@link #addEdgePairs(int...)} has no
 * {@link IntList} form: no result of this library is a flat sequence of alternating endpoints.
 * The results that list edges, {@link Edges}, go in whole through {@link #addEdges(Edges)}.
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
 * Every method that consumes a node ID checks it and throws {@link IndexOutOfBoundsException}
 * for an ID outside {@code 0..nodeCount-1}, so an edge to a node that does not exist is an error
 * at the call, not a wrong graph later. The check costs two predictable branches per consumed
 * ID. A batch method that rejects an ID adds none of its edges. The {@code AndNodes} twins
 * create the nodes they reference instead of requiring them to exist.
 * <p>
 * A builder holds at most {@code Integer.MAX_VALUE - 9} nodes, the <em>node limit</em>: the most
 * for which {@link #build()} can allocate the {@code nodeCount + 1} node offsets within the JDK's
 * soft maximum array length. A method that would create nodes beyond it throws before it changes
 * anything. Likewise it holds at most {@code Integer.MAX_VALUE - 8} added edges, duplicates
 * included, the <em>edge limit</em>: that array length itself. A batch method that would add
 * edges beyond it throws {@link IllegalArgumentException} before it changes anything. The
 * single-edge methods {@link #addEdge(int, int)}, {@link #addEdgeAndNodes(int, int)},
 * {@link #addChild(int)} and {@link #addParent(int)} do not check it, so that an edge costs no
 * more than its ID checks; past it, they fail with {@link OutOfMemoryError} once the edge storage
 * can grow no further.
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
   * @param totalEdges expected total number of edges (must be ≥ 0 and at most the edge limit)
   * @throws IllegalArgumentException if {@code totalEdges} is negative or above the edge limit
   */
  public void ensureEdgeCapacity(int totalEdges) {
    if (totalEdges < 0) {
      throw new IllegalArgumentException("totalEdges must be non-negative: " + totalEdges);
    }
    if (totalEdges > ArrayLimits.MAX_ARRAY_LENGTH) {
      throw new IllegalArgumentException("totalEdges " + totalEdges
          + " exceeds the edge limit of " + ArrayLimits.MAX_ARRAY_LENGTH);
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
   * @throws IllegalStateException if the node count has reached the node limit
   */
  public int addNode() {
    checkRoomForNode();
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
   * @throws IllegalArgumentException if {@code count} is negative or would raise the node count
   *     above the node limit
   */
  public int addNodes(int count) {
    checkNewNodeCount(count);
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
   * @throws IllegalStateException     if the node count has reached the node limit
   * @throws IndexOutOfBoundsException if {@code parent} does not exist
   */
  public int addChild(int parent) {
    checkRoomForNode();
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
   * @throws IllegalArgumentException  if {@code count} is negative or would raise the node
   *     count above the node limit, or the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if {@code parent} does not exist
   */
  public int addChildren(int parent, int count) {
    checkNewNodeCount(count);
    checkNode(parent, "parent");
    reserve(count);
    int firstId = nodeCount;
    nodeCount += count;
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
   * @throws IllegalStateException     if the node count has reached the node limit
   * @throws IndexOutOfBoundsException if {@code child} does not exist
   */
  public int addParent(int child) {
    checkRoomForNode();
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
   * @throws IllegalArgumentException  if {@code count} is negative or would raise the node
   *     count above the node limit, or the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if {@code child} does not exist
   */
  public int addParents(int child, int count) {
    checkNewNodeCount(count);
    checkNode(child, "child");
    reserve(count);
    int firstId = nodeCount;
    nodeCount += count;
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
   * @throws IndexOutOfBoundsException if either node does not exist
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
   * @throws IndexOutOfBoundsException if either ID is negative or at least the node limit
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
   * <strong>Time:</strong> O(k) amortized, where k = {@code targets.size()}.
   *
   * @param source  source node ID
   * @param targets list of target node IDs
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addOutEdges(int source, IntList targets) {
    checkNode(source, "source");
    checkNodes(targets, "target");
    writeFanOut(source, targets);
  }

  /**
   * Adds directed edges from {@code source} to each target in the list, creating any node that
   * does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code targets.size()}.
   *
   * @param source  source node ID (must be ≥ 0)
   * @param targets list of target node IDs (each must be ≥ 0)
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addOutEdgesAndNodes(int source, IntList targets) {
    checkCreatable(source);
    int highest = Math.max(source, highestCreatable(targets));
    writeFanOut(source, targets);
    raiseNodeCount(highest);
  }

  /**
   * Adds directed edges from {@code source} to each of the given targets.
   * All nodes must already exist.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code targets.length}.
   *
   * @param source  source node ID
   * @param targets target node IDs
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addOutEdges(int source, int... targets) {
    checkNode(source, "source");
    checkNodes(targets, "target");
    writeFanOut(source, targets);
  }

  /**
   * Adds directed edges from {@code source} to each of the given targets, creating any node
   * that does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code targets.length}.
   *
   * @param source  source node ID (must be ≥ 0)
   * @param targets target node IDs (each must be ≥ 0)
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addOutEdgesAndNodes(int source, int... targets) {
    checkCreatable(source);
    int highest = Math.max(source, highestCreatable(targets));
    writeFanOut(source, targets);
    raiseNodeCount(highest);
  }

  /**
   * Adds directed edges from each source in the list to {@code target}.
   * All nodes must already exist.
   * <p>
   * The fan-in counterpart of {@link #addOutEdges(int, IntList)}.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code sources.size()}.
   *
   * @param target  target node ID
   * @param sources list of source node IDs
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addInEdges(int target, IntList sources) {
    checkNode(target, "target");
    checkNodes(sources, "source");
    writeFanIn(target, sources);
  }

  /**
   * Adds directed edges from each source in the list to {@code target}, creating any node that
   * does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code sources.size()}.
   *
   * @param target  target node ID (must be ≥ 0)
   * @param sources list of source node IDs (each must be ≥ 0)
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addInEdgesAndNodes(int target, IntList sources) {
    checkCreatable(target);
    int highest = Math.max(target, highestCreatable(sources));
    writeFanIn(target, sources);
    raiseNodeCount(highest);
  }

  /**
   * Adds directed edges from each of the given sources to {@code target}.
   * All nodes must already exist.
   * <p>
   * The fan-in counterpart of {@link #addOutEdges(int, int...)}.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code sources.length}.
   *
   * @param target  target node ID
   * @param sources source node IDs
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addInEdges(int target, int... sources) {
    checkNode(target, "target");
    checkNodes(sources, "source");
    writeFanIn(target, sources);
  }

  /**
   * Adds directed edges from each of the given sources to {@code target}, creating any node
   * that does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code sources.length}.
   *
   * @param target  target node ID (must be ≥ 0)
   * @param sources source node IDs (each must be ≥ 0)
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addInEdgesAndNodes(int target, int... sources) {
    checkCreatable(target);
    int highest = Math.max(target, highestCreatable(sources));
    writeFanIn(target, sources);
    raiseNodeCount(highest);
  }

  /**
   * Chains the nodes in the list with edges: {@code nodes[0] → nodes[1] → … → nodes[n-1]}.
   * All nodes must already exist.
   * <p>
   * Fewer than two nodes add no edge.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code nodes.size()}.
   *
   * @param nodes list of node IDs to chain, in order
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addPath(IntList nodes) {
    checkNodes(nodes, "path");
    writePath(nodes);
  }

  /**
   * Chains the nodes in the list with edges, creating any node that does not exist yet.
   * <p>
   * Fewer than two nodes add no edge, but every ID passed is still created.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code nodes.size()}.
   *
   * @param nodes list of node IDs to chain, in order (each must be ≥ 0)
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addPathAndNodes(IntList nodes) {
    int highest = highestCreatable(nodes);
    writePath(nodes);
    raiseNodeCount(highest);
  }

  /**
   * Chains the given nodes with edges: {@code nodes[0] → nodes[1] → … → nodes[n-1]}.
   * All nodes must already exist.
   * <p>
   * Fewer than two nodes add no edge.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code nodes.length}.
   *
   * @param nodes node IDs to chain, in order
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addPath(int... nodes) {
    checkNodes(nodes, "path");
    writePath(nodes);
  }

  /**
   * Chains the given nodes with edges, creating any node that does not exist yet.
   * <p>
   * Fewer than two nodes add no edge, but every ID passed is still created.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code nodes.length}.
   *
   * @param nodes node IDs to chain, in order (each must be ≥ 0)
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addPathAndNodes(int... nodes) {
    int highest = highestCreatable(nodes);
    writePath(nodes);
    raiseNodeCount(highest);
  }

  /**
   * Adds one edge per pair of consecutive arguments: {@code endpoints[0] → endpoints[1]},
   * {@code endpoints[2] → endpoints[3]}, and so on.
   * All nodes must already exist.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code endpoints.length}.
   *
   * @param endpoints alternating source and target node IDs
   * @throws IllegalArgumentException  if the number of arguments is odd, or the edges would
   *     exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addEdgePairs(int... endpoints) {
    checkPairCount(endpoints.length);
    checkNodes(endpoints, "endpoint");
    writePairs(endpoints);
  }

  /**
   * Adds one edge per pair of consecutive arguments, creating any node that does not exist yet.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code endpoints.length}.
   *
   * @param endpoints alternating source and target node IDs (each must be ≥ 0)
   * @throws IllegalArgumentException  if the number of arguments is odd, or the edges would
   *     exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addEdgePairsAndNodes(int... endpoints) {
    checkPairCount(endpoints.length);
    int highest = highestCreatable(endpoints);
    writePairs(endpoints);
    raiseNodeCount(highest);
  }

  /**
   * Adds the edges of an algorithm result, such as {@link BackEdges#of(Graph)} or
   * {@link Biconnectivity#bridges()}, in their order.
   * All nodes must already exist.
   * <p>
   * Reads sources and targets; the edge indices refer to the analyzed graph and are ignored.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code edges.count()}.
   *
   * @param edges the edges to add
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any node does not exist
   */
  public void addEdges(Edges edges) {
    IntList sources = edges.sourceNodes();
    IntList targets = edges.targetNodes();
    checkNodes(sources, "source");
    checkNodes(targets, "target");
    writeLists(sources, targets);
  }

  /**
   * Adds the edges of an algorithm result in their order, creating any node that does not exist
   * yet.
   * <p>
   * Reads sources and targets; the edge indices refer to the analyzed graph and are ignored.
   * <p>
   * <strong>Time:</strong> O(k) amortized, where k = {@code edges.count()}.
   *
   * @param edges the edges to add
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if any ID is negative or at least the node limit
   */
  public void addEdgesAndNodes(Edges edges) {
    IntList sources = edges.sourceNodes();
    IntList targets = edges.targetNodes();
    int highest = Math.max(highestCreatable(sources), highestCreatable(targets));
    writeLists(sources, targets);
    raiseNodeCount(highest);
  }

  /**
   * Adds all nodes and edges of the given graph to this builder.
   * <p>
   * Node IDs from the source graph are shifted by the current node count (offset).
   * The returned offset can be used to reference the imported nodes:
   * node {@code i} in the source graph becomes node {@code offset + i} in this builder.
   * <p>
   * <strong>Time:</strong> O(graph.nodeCount() + graph.edgeCount()) amortized.
   *
   * @param graph the graph to import
   * @return the offset (= ID of the first imported node)
   * @throws IllegalArgumentException if the nodes of {@code graph} would raise the node count
   *     above the node limit, or its edges would exceed the edge limit
   */
  public int addGraph(Graph graph) {
    int srcNodeCount = graph.nodeCount();
    if (srcNodeCount > ArrayLimits.MAX_NODE_COUNT - nodeCount) {
      throw beyondNodeLimit("a graph of " + srcNodeCount + " nodes");
    }
    reserve(graph.edgeCount());
    int offset = nodeCount;
    nodeCount += srcNodeCount;

    var adjacency = Csr.outAdjacency(graph);
    var adjacencyStart = Csr.outAdjacencyStart(graph);

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
   * <strong>Time:</strong> O(nodeCount + k + Σ d x log d), where k is the number of edges
   * added, duplicates included, and d the number of them leaving each node (one sort per
   * node).<br>
   * <strong>Memory:</strong> O(nodeCount + edgeCount) for the result.
   *
   * @return an immutable graph
   * @throws IllegalArgumentException if the edges added exceed the edge limit
   */
  public Graph build() {
    int rawEdgeCount = edgeSources.size();
    checkEdgeLimit(rawEdgeCount);
    int[] sources = edgeSources.elements();
    int[] targets = edgeTargets.elements();

    // 1) Count out-degrees
    var outStart = new int[nodeCount + 1];
    for (int i = 0; i < rawEdgeCount; i++) {
      outStart[sources[i] + 1]++;
    }

    // 2) Prefix sum: degrees to start offsets
    for (int n = 0; n < nodeCount; n++) {
      outStart[n + 1] += outStart[n];
    }

    // 3) Distribute targets into per-node buckets
    var outAdj = new int[outStart[nodeCount]];
    var writePos = Arrays.copyOf(outStart, nodeCount);
    for (int i = 0; i < rawEdgeCount; i++) {
      outAdj[writePos[sources[i]]++] = targets[i];
    }

    // 4) Sort and deduplicate each bucket, compacting left: writes never pass the read position
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

    if (compactEnd < outAdj.length) {
      outAdj = Arrays.copyOf(outAdj, compactEnd);
    }

    return new Graph(outStart, outAdj);
  }

  /**
   * Current capacity of the internal edge storage. Exists so that tests can observe when the
   * storage reallocates.
   */
  int edgeCapacity() {
    return Math.min(edgeSources.elements().length, edgeTargets.elements().length);
  }

  private void writeFanOut(int source, int[] targets) {
    reserve(targets.length);
    for (int target : targets) {
      edgeSources.add(source);
      edgeTargets.add(target);
    }
  }

  private void writeFanOut(int source, IntList targets) {
    int count = targets.size();
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(source);
      edgeTargets.add(targets.getInt(i));
    }
  }

  private void writeFanIn(int target, int[] sources) {
    reserve(sources.length);
    for (int source : sources) {
      edgeSources.add(source);
      edgeTargets.add(target);
    }
  }

  private void writeFanIn(int target, IntList sources) {
    int count = sources.size();
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(sources.getInt(i));
      edgeTargets.add(target);
    }
  }

  private void writePath(int[] nodes) {
    reserve(Math.max(nodes.length - 1, 0));
    for (int i = 1; i < nodes.length; i++) {
      edgeSources.add(nodes[i - 1]);
      edgeTargets.add(nodes[i]);
    }
  }

  private void writePath(IntList nodes) {
    int count = nodes.size();
    reserve(Math.max(count - 1, 0));
    for (int i = 1; i < count; i++) {
      edgeSources.add(nodes.getInt(i - 1));
      edgeTargets.add(nodes.getInt(i));
    }
  }

  private void writePairs(int[] endpoints) {
    reserve(endpoints.length >> 1);
    for (int i = 0; i < endpoints.length; i += 2) {
      edgeSources.add(endpoints[i]);
      edgeTargets.add(endpoints[i + 1]);
    }
  }

  private void writeLists(IntList sources, IntList targets) {
    int count = sources.size();
    reserve(count);
    for (int i = 0; i < count; i++) {
      edgeSources.add(sources.getInt(i));
      edgeTargets.add(targets.getInt(i));
    }
  }

  private void checkNode(int node, String role) {
    if (node < 0 || node >= nodeCount) throw notFound(role, node);
  }

  /**
   * Validates every consumed node ID before any of them is written, so that a rejected batch
   * leaves the builder unchanged.
   */
  private void checkNodes(int[] nodes, String role) {
    for (int node : nodes) {
      checkNode(node, role);
    }
  }

  private void checkNodes(IntList nodes, String role) {
    for (int i = 0; i < nodes.size(); i++) {
      checkNode(nodes.getInt(i), role);
    }
  }

  private IndexOutOfBoundsException notFound(String role, int node) {
    return new IndexOutOfBoundsException(
        role + " node does not exist: " + node + " (nodeCount=" + nodeCount + ")");
  }

  /**
   * Rejects IDs that cannot be created: negative ones, and those whose node count would exceed
   * {@link ArrayLimits#MAX_NODE_COUNT}.
   */
  private static void checkCreatable(int node) {
    if (node < 0) {
      throw new IndexOutOfBoundsException("node ID must be non-negative: " + node);
    }
    if (node >= ArrayLimits.MAX_NODE_COUNT) {
      throw new IndexOutOfBoundsException(
          "node ID must be below the node limit of " + ArrayLimits.MAX_NODE_COUNT + ": " + node);
    }
  }

  /**
   * Rejects a negative {@code count} and one that would exceed {@link ArrayLimits#MAX_NODE_COUNT}.
   */
  private void checkNewNodeCount(int count) {
    if (count < 0) throw new IllegalArgumentException("count must be non-negative: " + count);
    // Cannot overflow: the node count never exceeds the limit.
    if (count > ArrayLimits.MAX_NODE_COUNT - nodeCount) throw beyondNodeLimit("count " + count);
  }

  private void checkRoomForNode() {
    if (nodeCount == ArrayLimits.MAX_NODE_COUNT) {
      throw new IllegalStateException(
          "the node limit of " + ArrayLimits.MAX_NODE_COUNT + " is reached");
    }
  }

  private IllegalArgumentException beyondNodeLimit(String what) {
    return new IllegalArgumentException(what + " would exceed the node limit of "
        + ArrayLimits.MAX_NODE_COUNT + " (nodeCount=" + nodeCount + ")");
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
   * <p>
   * The batch twins call it after writing their edges, because the write can still reject the
   * batch at the edge limit and must then leave the node count as it was. The writers do not
   * read the node count.
   */
  private void raiseNodeCount(int node) {
    if (node >= nodeCount) {
      nodeCount = node + 1;
    }
  }

  // The single-edge methods leave the edge limit to build(): fastutil's lists grow past it on
  // HotSpot. Static, so that the tests reach the limit without adding the edges.
  static void checkEdgeLimit(int addedEdges) {
    if (addedEdges > ArrayLimits.MAX_ARRAY_LENGTH) {
      throw new IllegalArgumentException(addedEdges + " edges added exceed the edge limit of "
          + ArrayLimits.MAX_ARRAY_LENGTH);
    }
  }

  private static void checkPairCount(int length) {
    if ((length & 1) != 0) {
      throw new IllegalArgumentException(
          "expected an even number of node IDs, got " + length);
    }
  }

  /**
   * Reserves room for {@code count} additional edges in both edge lists, or rejects them before
   * any is written if they would exceed the edge limit. Grows by at least half the capacity, like
   * {@link IntArrayList#add(int)}, so that batch calls in a loop cost amortized O(1) per edge;
   * {@link IntArrayList#ensureCapacity(int)} alone grows to the exact size, which would make such
   * a loop quadratic. The half step stops at the edge limit, and a batch larger than the step
   * gets exactly the room it needs, one allocation per list.
   */
  private void reserve(int count) {
    int size = edgeSources.size();
    long required = (long) size + count;
    if (required > ArrayLimits.MAX_ARRAY_LENGTH) {
      throw new IllegalArgumentException(count + " more edges would exceed the edge limit of "
          + ArrayLimits.MAX_ARRAY_LENGTH + " (" + size + " edges added)");
    }
    int capacity = edgeSources.elements().length;
    if (required <= capacity) return;
    long grown = Math.max(required, capacity + (long) (capacity >> 1));
    int newCapacity = (int) Math.min(grown, ArrayLimits.MAX_ARRAY_LENGTH);
    edgeSources.ensureCapacity(newCapacity);
    edgeTargets.ensureCapacity(newCapacity);
  }
}
