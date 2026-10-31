package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

/**
 * A builder for a {@link Graph}, from nodes and edges added in any order.
 * <p>
 * The nodes added so far have the node IDs {@code 0..nodeCount-1}, in the order they were added;
 * {@link #addNode()} and {@link #addNodes(int)} return the IDs of new ones. Edges name their
 * source and target by node ID, in any order and as often as the input has them. Here, the calls
 * between the methods of a program are built from its call sites, one edge per call site;
 * {@code names} holds the names of the methods:
 * {@snippet file="com/kobayami/graphity/examples/BuildingAndReading.java" region="build"}
 * {@link #build()} sorts the edges into edge index order and merges duplicates. The builder stays
 * usable afterwards: more nodes and edges can be added, and {@code build()} called again.
 * <p>
 * Besides single edges, the builder takes edges in batches: the out-edges or in-edges of one node
 * ({@link #addOutEdges(int, int...)}, {@link #addInEdges(int, int...)}), a path
 * ({@link #addPath(int...)}), pairs of node IDs ({@link #addEdgePairs(int...)}), the edges of an
 * algorithm result ({@link #addEdges(Edges)}) or a whole graph ({@link #addGraph(Graph)}).
 * {@link #addChild(int)}, {@link #addParent(int)} and their plural forms add new nodes together
 * with their edges. The out-edges, in-edges and paths take their node IDs as {@code int...} or as
 * an {@link IntList}, such as the {@link SortedIntView} that {@link Graph#outNodes(int)} returns.
 * So a result of this library goes in without being copied into an array first. A list is read
 * during the call and not kept.
 * <p>
 * A method that takes the node ID of an existing node throws {@link IndexOutOfBoundsException}
 * if it is not in {@code 0..nodeCount-1}, so that an edge to a missing node fails at the call. A
 * batch method that throws adds nothing. The methods that add edges between given nodes each
 * have a twin with the suffix {@code AndNodes}, such as {@link #addEdgeAndNodes(int, int)}, which
 * adds missing nodes instead. A twin raises the node count to one more than the highest node ID
 * it is given, and the nodes in between are added too, without edges. So a graph can be built
 * from input numbered elsewhere without counting its nodes first. However, a node ID that is too
 * high adds nodes instead of failing.
 * <p>
 * Nodes and edges cannot be removed. To leave nodes out, build the graph and take a subgraph
 * with {@link Graphs#remapped(Graph, NodeMapping)} and
 * {@link NodeMapping#forSubgraph(java.util.BitSet)}.
 * <p>
 * Instances are mutable and not safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 8 bytes per added edge, duplicates included, plus room to grow.
 * <p>
 * A builder holds at most {@code Integer.MAX_VALUE - 9} nodes, as many as a graph can have. This
 * number is the <em>node limit</em>. A method that would add more throws before it changes
 * anything. A builder builds a graph from at most {@code Integer.MAX_VALUE - 8} added edges,
 * duplicates included. This number is the <em>edge limit</em>. A batch method that would add more
 * throws {@link IllegalArgumentException} and adds nothing. The single-edge methods
 * {@link #addEdge(int, int)}, {@link #addEdgeAndNodes(int, int)}, {@link #addChild(int)} and
 * {@link #addParent(int)} do not check the edge limit: beyond it, {@link #build()} throws
 * {@link IllegalArgumentException}, and the single-edge methods throw {@link OutOfMemoryError}
 * once the edge storage cannot grow any further.
 */
public final class GraphBuilder {

  private int nodeCount;
  private final IntArrayList edgeSources;
  private final IntArrayList edgeTargets;

  /**
   * Creates an empty builder, without nodes or edges.
   */
  public GraphBuilder() {
    this.edgeSources = new IntArrayList();
    this.edgeTargets = new IntArrayList();
  }

  /**
   * Makes room for {@code totalEdges} edges in total, so that adding up to that many allocates
   * nothing more.
   * <p>
   * The argument counts all edges the builder will hold, duplicates included, not the ones still
   * to come. It can be called at any time and does not change the graph that {@link #build()}
   * returns.
   * <p>
   * <strong>Time:</strong> O(totalEdges) if the room does not suffice yet, otherwise O(1).<br>
   * <strong>Memory:</strong> 8 x totalEdges bytes if the room does not suffice yet, otherwise
   * none.
   *
   * @param totalEdges the number of edges to make room for, duplicates included
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
   * Adds a node and returns its node ID, which is the next in order.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node ID of the new node
   * @throws IllegalStateException if the node count has reached the node limit
   */
  public int addNode() {
    checkRoomForNode();
    return nodeCount++;
  }

  /**
   * Adds {@code count} nodes and returns the node ID of the first; the others follow in order.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param count number of nodes to add
   * @return node ID of the first new node
   * @throws IllegalArgumentException if {@code count} is negative or the nodes would exceed the
   *                                  node limit
   */
  public int addNodes(int count) {
    checkNewNodeCount(count);
    int firstId = nodeCount;
    nodeCount += count;
    return firstId;
  }

  /**
   * Adds a node with an edge from {@code parent} to it, and returns its node ID.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param parent node ID in {@code 0..nodeCount-1}
   * @return node ID of the new node
   * @throws IllegalStateException     if the node count has reached the node limit
   * @throws IndexOutOfBoundsException if {@code parent} is out of range
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
   * Adds {@code count} nodes, each with an edge from {@code parent} to it, and returns the node ID
   * of the first; the others follow in order.
   * <p>
   * <strong>Time:</strong> O(count) amortized.
   *
   * @param parent node ID in {@code 0..nodeCount-1}
   * @param count  number of nodes to add
   * @return node ID of the first new node
   * @throws IllegalArgumentException  if {@code count} is negative, or the nodes or edges would
   *                                   exceed their limit
   * @throws IndexOutOfBoundsException if {@code parent} is out of range
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
   * Adds a node with an edge from it to {@code child}, and returns its node ID.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param child node ID in {@code 0..nodeCount-1}
   * @return node ID of the new node
   * @throws IllegalStateException     if the node count has reached the node limit
   * @throws IndexOutOfBoundsException if {@code child} is out of range
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
   * Adds {@code count} nodes, each with an edge from it to {@code child}, and returns the node ID
   * of the first; the others follow in order.
   * <p>
   * <strong>Time:</strong> O(count) amortized.
   *
   * @param child node ID in {@code 0..nodeCount-1}
   * @param count number of nodes to add
   * @return node ID of the first new node
   * @throws IllegalArgumentException  if {@code count} is negative, or the nodes or edges would
   *                                   exceed their limit
   * @throws IndexOutOfBoundsException if {@code child} is out of range
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
   * Adds the edge {@code source -> target}.
   * <p>
   * An edge added more than once is still one edge in the graph: {@link #build()} merges
   * duplicates.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param source node ID in {@code 0..nodeCount-1}
   * @param target node ID in {@code 0..nodeCount-1}
   * @throws IndexOutOfBoundsException if {@code source} or {@code target} is out of range
   */
  public void addEdge(int source, int target) {
    checkNode(source, "source");
    checkNode(target, "target");
    edgeSources.add(source);
    edgeTargets.add(target);
  }

  /**
   * Adds the edge {@code source -> target}, and the missing nodes up to the higher of the two
   * node IDs.
   * <p>
   * <strong>Time:</strong> O(1) amortized.
   *
   * @param source node ID, {@code 0} or more
   * @param target node ID, {@code 0} or more
   * @throws IndexOutOfBoundsException if {@code source} or {@code target} is negative or not below
   *                                   the node limit
   */
  public void addEdgeAndNodes(int source, int target) {
    checkCreatable(source);
    checkCreatable(target);
    raiseNodeCount(Math.max(source, target));
    edgeSources.add(source);
    edgeTargets.add(target);
  }

  /**
   * Adds an edge from {@code source} to each node of {@code targets}.
   * <p>
   * <strong>Time:</strong> O(targets.size()) amortized.
   *
   * @param source  node ID in {@code 0..nodeCount-1}
   * @param targets node IDs in {@code 0..nodeCount-1}
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if {@code source} or a node of {@code targets} is out of
   *                                   range
   */
  public void addOutEdges(int source, IntList targets) {
    checkNode(source, "source");
    checkNodes(targets, "target");
    writeFanOut(source, targets);
  }

  /**
   * Adds an edge from {@code source} to each node of {@code targets}, and the missing nodes up to
   * the highest of these node IDs.
   * <p>
   * <strong>Time:</strong> O(targets.size()) amortized.
   *
   * @param source  node ID, {@code 0} or more
   * @param targets node IDs, {@code 0} or more
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node ID is negative or not below the node limit
   */
  public void addOutEdgesAndNodes(int source, IntList targets) {
    checkCreatable(source);
    int highest = Math.max(source, highestCreatable(targets));
    writeFanOut(source, targets);
    raiseNodeCount(highest);
  }

  /**
   * Adds an edge from {@code source} to each node of {@code targets}.
   * <p>
   * <strong>Time:</strong> O(targets.length) amortized.
   *
   * @param source  node ID in {@code 0..nodeCount-1}
   * @param targets node IDs in {@code 0..nodeCount-1}
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if {@code source} or a node of {@code targets} is out of
   *                                   range
   */
  public void addOutEdges(int source, int... targets) {
    checkNode(source, "source");
    checkNodes(targets, "target");
    writeFanOut(source, targets);
  }

  /**
   * Adds an edge from {@code source} to each node of {@code targets}, and the missing nodes up to
   * the highest of these node IDs.
   * <p>
   * <strong>Time:</strong> O(targets.length) amortized.
   *
   * @param source  node ID, {@code 0} or more
   * @param targets node IDs, {@code 0} or more
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node ID is negative or not below the node limit
   */
  public void addOutEdgesAndNodes(int source, int... targets) {
    checkCreatable(source);
    int highest = Math.max(source, highestCreatable(targets));
    writeFanOut(source, targets);
    raiseNodeCount(highest);
  }

  /**
   * Adds an edge from each node of {@code sources} to {@code target}.
   * <p>
   * <strong>Time:</strong> O(sources.size()) amortized.
   *
   * @param target  node ID in {@code 0..nodeCount-1}
   * @param sources node IDs in {@code 0..nodeCount-1}
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if {@code target} or a node of {@code sources} is out of
   *                                   range
   */
  public void addInEdges(int target, IntList sources) {
    checkNode(target, "target");
    checkNodes(sources, "source");
    writeFanIn(target, sources);
  }

  /**
   * Adds an edge from each node of {@code sources} to {@code target}, and the missing nodes up to
   * the highest of these node IDs.
   * <p>
   * <strong>Time:</strong> O(sources.size()) amortized.
   *
   * @param target  node ID, {@code 0} or more
   * @param sources node IDs, {@code 0} or more
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node ID is negative or not below the node limit
   */
  public void addInEdgesAndNodes(int target, IntList sources) {
    checkCreatable(target);
    int highest = Math.max(target, highestCreatable(sources));
    writeFanIn(target, sources);
    raiseNodeCount(highest);
  }

  /**
   * Adds an edge from each node of {@code sources} to {@code target}.
   * <p>
   * <strong>Time:</strong> O(sources.length) amortized.
   *
   * @param target  node ID in {@code 0..nodeCount-1}
   * @param sources node IDs in {@code 0..nodeCount-1}
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if {@code target} or a node of {@code sources} is out of
   *                                   range
   */
  public void addInEdges(int target, int... sources) {
    checkNode(target, "target");
    checkNodes(sources, "source");
    writeFanIn(target, sources);
  }

  /**
   * Adds an edge from each node of {@code sources} to {@code target}, and the missing nodes up to
   * the highest of these node IDs.
   * <p>
   * <strong>Time:</strong> O(sources.length) amortized.
   *
   * @param target  node ID, {@code 0} or more
   * @param sources node IDs, {@code 0} or more
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node ID is negative or not below the node limit
   */
  public void addInEdgesAndNodes(int target, int... sources) {
    checkCreatable(target);
    int highest = Math.max(target, highestCreatable(sources));
    writeFanIn(target, sources);
    raiseNodeCount(highest);
  }

  /**
   * Adds a path through {@code nodes}: an edge from each node of the list to the next.
   * <p>
   * Fewer than two nodes add no edge.
   * <p>
   * <strong>Time:</strong> O(nodes.size()) amortized.
   *
   * @param nodes node IDs in {@code 0..nodeCount-1}, in the order of the path
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node of {@code nodes} is out of range
   */
  public void addPath(IntList nodes) {
    checkNodes(nodes, "path");
    writePath(nodes);
  }

  /**
   * Adds a path through {@code nodes}, and the missing nodes up to the highest of these node IDs.
   * The path has an edge from each node of the list to the next.
   * <p>
   * Fewer than two nodes add no edge, but still add the nodes.
   * <p>
   * <strong>Time:</strong> O(nodes.size()) amortized.
   *
   * @param nodes node IDs, {@code 0} or more, in the order of the path
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node ID is negative or not below the node limit
   */
  public void addPathAndNodes(IntList nodes) {
    int highest = highestCreatable(nodes);
    writePath(nodes);
    raiseNodeCount(highest);
  }

  /**
   * Adds a path through {@code nodes}: an edge from each node of the array to the next.
   * <p>
   * Fewer than two nodes add no edge.
   * <p>
   * <strong>Time:</strong> O(nodes.length) amortized.
   *
   * @param nodes node IDs in {@code 0..nodeCount-1}, in the order of the path
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node of {@code nodes} is out of range
   */
  public void addPath(int... nodes) {
    checkNodes(nodes, "path");
    writePath(nodes);
  }

  /**
   * Adds a path through {@code nodes}, and the missing nodes up to the highest of these node IDs.
   * The path has an edge from each node of the array to the next.
   * <p>
   * Fewer than two nodes add no edge, but still add the nodes.
   * <p>
   * <strong>Time:</strong> O(nodes.length) amortized.
   *
   * @param nodes node IDs, {@code 0} or more, in the order of the path
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a node ID is negative or not below the node limit
   */
  public void addPathAndNodes(int... nodes) {
    int highest = highestCreatable(nodes);
    writePath(nodes);
    raiseNodeCount(highest);
  }

  /**
   * Adds an edge for each pair of node IDs in {@code endpoints}: {@code endpoints[0] ->
   * endpoints[1]}, {@code endpoints[2] -> endpoints[3]}, and so on.
   * <p>
   * <strong>Time:</strong> O(endpoints.length) amortized.
   *
   * @param endpoints node IDs in {@code 0..nodeCount-1}, source and target of each edge in turn
   * @throws IllegalArgumentException  if {@code endpoints} has an odd length, or the edges would
   *                                   exceed the edge limit
   * @throws IndexOutOfBoundsException if a node of {@code endpoints} is out of range
   */
  public void addEdgePairs(int... endpoints) {
    checkPairCount(endpoints.length);
    checkNodes(endpoints, "endpoint");
    writePairs(endpoints);
  }

  /**
   * Adds an edge for each pair of node IDs in {@code endpoints}, as
   * {@link #addEdgePairs(int...)} does, and the missing nodes up to the highest of these node IDs.
   * <p>
   * <strong>Time:</strong> O(endpoints.length) amortized.
   *
   * @param endpoints node IDs, {@code 0} or more, source and target of each edge in turn
   * @throws IllegalArgumentException  if {@code endpoints} has an odd length, or the edges would
   *                                   exceed the edge limit
   * @throws IndexOutOfBoundsException if a node ID is negative or not below the node limit
   */
  public void addEdgePairsAndNodes(int... endpoints) {
    checkPairCount(endpoints.length);
    int highest = highestCreatable(endpoints);
    writePairs(endpoints);
    raiseNodeCount(highest);
  }

  /**
   * Adds the edges of an algorithm result, such as {@link BackEdges#of(Graph)} or
   * {@link Biconnectivity#bridges()}.
   * <p>
   * Only the sources and targets are read: the edge indices of {@code edges} belong to the graph
   * the algorithm ran on.
   * <p>
   * <strong>Time:</strong> O(edges.count()) amortized.
   *
   * @param edges the edges to add, between nodes in {@code 0..nodeCount-1}
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a source or target is out of range
   */
  public void addEdges(Edges edges) {
    IntList sources = edges.sourceNodes();
    IntList targets = edges.targetNodes();
    checkNodes(sources, "source");
    checkNodes(targets, "target");
    writeLists(sources, targets);
  }

  /**
   * Adds the edges of an algorithm result, as {@link #addEdges(Edges)} does, and the missing nodes
   * up to the highest of their node IDs.
   * <p>
   * <strong>Time:</strong> O(edges.count()) amortized.
   *
   * @param edges the edges to add
   * @throws IllegalArgumentException  if the edges would exceed the edge limit
   * @throws IndexOutOfBoundsException if a source or target is not below the node limit
   */
  public void addEdgesAndNodes(Edges edges) {
    IntList sources = edges.sourceNodes();
    IntList targets = edges.targetNodes();
    int highest = Math.max(highestCreatable(sources), highestCreatable(targets));
    writeLists(sources, targets);
    raiseNodeCount(highest);
  }

  /**
   * Adds the nodes and edges of {@code graph} after the nodes already added, and returns the
   * offset of its node IDs.
   * <p>
   * Node {@code v} of {@code graph} becomes node {@code offset + v} of this builder, where the
   * offset is the node count before the call. Into an empty builder, the node IDs stay the same,
   * which makes this the way to extend a graph: a {@link Graph} is immutable, but a builder that
   * starts with it can add to it and build a new one. Here, the dependencies between the packages
   * of a program are extended to a next version, in which {@code ui} also uses {@code storage}:
   * {@snippet file="com/kobayami/graphity/examples/BuildingAndReading.java" region="addGraph"}
   * <p>
   * <strong>Time:</strong> O(graph.nodeCount() + graph.edgeCount()) amortized.
   *
   * @param graph the graph to add
   * @return the offset: node ID of node {@code 0} of {@code graph} in this builder
   * @throws IllegalArgumentException if the nodes or edges of {@code graph} would exceed their
   *                                  limit
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
   * Returns a graph with the nodes and edges added so far.
   * <p>
   * The edges are sorted into edge index order (by source and then by target), and duplicates are
   * merged, so an edge does not keep the position at which it was added;
   * {@link Graph#edgeIndexOf(int, int)} finds where it went. The builder does not change: more
   * nodes and edges can be added and built into another graph.
   * <p>
   * <strong>Time:</strong> O(nodeCount + k log d), where k is the number of added edges,
   * duplicates included, and d the most of them with the same source.<br>
   * <strong>Memory:</strong> 4 x (nodeCount + edgeCount) bytes for the result; while it runs,
   * briefly up to 4 x (nodeCount + k) bytes more.
   *
   * @return the graph
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

  // For the tests, which observe when the edge storage grows.
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

  // Checks every node ID before any edge is written, so that a rejected batch leaves the builder
  // unchanged.
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

  // Node ID k needs a node count of k + 1, so the highest node ID that can be created is one below
  // the node limit.
  private static void checkCreatable(int node) {
    if (node < 0) {
      throw new IndexOutOfBoundsException("node ID must be non-negative: " + node);
    }
    if (node >= ArrayLimits.MAX_NODE_COUNT) {
      throw new IndexOutOfBoundsException(
          "node ID must be below the node limit of " + ArrayLimits.MAX_NODE_COUNT + ": " + node);
    }
  }

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

  // Checks every node ID before any node is created, and returns the highest, or -1 if there is
  // none.
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

  // Node IDs are contiguous, so the nodes below node are created too; -1 creates none. The
  // AndNodes batch methods call it after writing their edges, because the write can still reject
  // the batch at the edge limit and must then leave the node count as it was. The writers do not
  // read the node count.
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

  // Reserves room for count more edges in both lists, or rejects them before any is written if
  // they would exceed the edge limit. Grows by at least half the capacity, like
  // IntArrayList.add(int), so that batch calls in a loop cost amortized O(1) per edge;
  // IntArrayList.ensureCapacity(int) alone grows to the exact size, which would make such a loop
  // quadratic. The half step stops at the edge limit, and a batch larger than the step gets
  // exactly the room it needs, one allocation per list.
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
