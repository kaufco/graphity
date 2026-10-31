package com.kobayami.graphity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/**
 * A directed graph, which is the input of the algorithms in this library.
 * <p>
 * Nodes are the {@code int} IDs {@code 0..nodeCount()-1}. Edges are numbered by edge index
 * {@code 0..edgeCount()-1}, ordered by source and then by target. No two edges have the same
 * source and target; self-loops are allowed. The graph holds nothing but this structure: names,
 * weights and any other data live in arrays of the caller, indexed by node ID or by edge index.
 * <p>
 * {@link GraphBuilder} builds a graph from edges in any order. {@link #of(int[], int[])} takes the
 * two arrays of a CSR (compressed sparse row) layout. A graph keeps its edges in this form, and
 * {@link Csr} hands the arrays to algorithm code. {@link #writeTo(OutputStream)} and
 * {@link #readFrom(InputStream)} store a graph in a stream and load it again.
 * <p>
 * A {@code Graph} holds the out-nodes of every node, called its out-adjacency. Algorithms that
 * also need the in-nodes take a {@link BiAdjacentGraph}, which {@link BiAdjacentGraph#of(Graph)}
 * builds and which also gives the transposed graph. Algorithms are not methods of the graph but
 * classes of their own, such as {@link Components}, {@link TopOrder} and {@link Dominators}; the
 * {@linkplain com.kobayami.graphity package documentation} lists them by topic.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 4 x (nodeCount + edgeCount) bytes.
 * <p>
 * A graph has at most {@code Integer.MAX_VALUE - 9} nodes and {@code Integer.MAX_VALUE - 8}
 * edges.
 */
public sealed class Graph permits BiAdjacentGraph {

  // toString() runs implicitly, in debuggers and logs, so it lists a bounded part of the graph:
  // at most this many nodes, and this many out-nodes over all listed nodes together.
  private static final int TO_STRING_NODES = 16;
  private static final int TO_STRING_TARGETS = 64;

  // The longest listing the two limits allow stays under 1000 characters, even with ten-digit
  // counts and IDs, so the builder never grows.
  private static final int TO_STRING_CAPACITY = 1024;

  // The CSR layout: the out-nodes of node v are
  // outAdjacency[outAdjacencyStart[v]..outAdjacencyStart[v + 1] - 1], sorted ascending, and the
  // position of an out-node in outAdjacency is the edge index of its edge. The sentinel
  // outAdjacencyStart[nodeCount] == edgeCount makes outAdjacencyStart[v + 1] valid for every node
  // without a branch.
  private final int[] outAdjacencyStart;
  private final int[] outAdjacency;

  // Takes the arrays as they are, neither copied nor checked: the caller guarantees the layout.
  Graph(int[] outAdjacencyStart, int[] outAdjacency) {
    this.outAdjacencyStart = outAdjacencyStart;
    this.outAdjacency = outAdjacency;
  }

  /**
   * Returns a graph from the two arrays of its CSR layout, as {@link Csr} returns them and
   * {@link #writeTo(OutputStream)} writes them.
   * <p>
   * The out-nodes of node {@code v} are
   * {@code outAdjacency[outAdjacencyStart[v]..outAdjacencyStart[v + 1] - 1]}, and the position of
   * an out-node in {@code outAdjacency} is the edge index of its edge. For example, the control
   * flow of a method with a loop:
   * {@snippet file="com/kobayami/graphity/examples/Building.java" region="of"}
   * Both arrays are copied and checked: the offsets start at {@code 0}, never decrease and end at
   * {@code outAdjacency.length}, and the out-nodes of each node are strictly ascending. For edges
   * in any order or with duplicates, use {@link GraphBuilder}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + edgeCount) bytes for the copies, which the result
   * keeps.
   *
   * @param outAdjacencyStart where the out-nodes of each node start in {@code outAdjacency},
   *                          followed by {@code edgeCount}; length {@code nodeCount + 1}
   * @param outAdjacency      the out-nodes of all nodes, node after node; length
   *                          {@code edgeCount}
   * @return the graph
   * @throws IndexOutOfBoundsException if an out-node is not in {@code 0..nodeCount-1}
   * @throws IllegalArgumentException if {@code outAdjacencyStart} is empty, does not start at
   *                                  {@code 0}, decreases or does not end at
   *                                  {@code outAdjacency.length}, the out-nodes of a node are not
   *                                  strictly ascending, or the arrays hold more nodes or edges
   *                                  than a graph can have
   */
  public static Graph of(int[] outAdjacencyStart, int[] outAdjacency) {
    return ofOwned(outAdjacencyStart.clone(), outAdjacency.clone());
  }

  // of(int[], int[]) without the copy, for arrays that nobody else holds, as readFrom reads them.
  static Graph ofOwned(int[] outAdjacencyStart, int[] outAdjacency) {
    if (outAdjacencyStart.length == 0) {
      throw new IllegalArgumentException(
          "outAdjacencyStart must hold nodeCount + 1 offsets, but is empty");
    }
    int n = outAdjacencyStart.length - 1;
    int m = outAdjacency.length;
    if (n > ArrayLimits.MAX_NODE_COUNT || m > ArrayLimits.MAX_ARRAY_LENGTH) {
      throw new IllegalArgumentException("a graph holds at most " + ArrayLimits.MAX_NODE_COUNT
          + " nodes and " + ArrayLimits.MAX_ARRAY_LENGTH + " edges, but these arrays hold " + n
          + " and " + m);
    }
    if (outAdjacencyStart[0] != 0) {
      throw new IllegalArgumentException(
          "outAdjacencyStart[0] must be 0, but was " + outAdjacencyStart[0]);
    }
    // Offsets first: once they rise from 0 to m, every list below lies inside outAdjacency.
    for (int v = 0; v < n; v++) {
      if (outAdjacencyStart[v + 1] < outAdjacencyStart[v]) {
        throw new IllegalArgumentException("outAdjacencyStart decreases after node " + v + ": "
            + outAdjacencyStart[v] + " > " + outAdjacencyStart[v + 1]);
      }
    }
    if (outAdjacencyStart[n] != m) {
      throw new IllegalArgumentException("outAdjacencyStart[" + n
          + "] must be outAdjacency.length " + m + ", but was " + outAdjacencyStart[n]);
    }
    for (int v = 0; v < n; v++) {
      int end = outAdjacencyStart[v + 1];
      int previous = -1;
      for (int e = outAdjacencyStart[v]; e < end; e++) {
        int target = outAdjacency[e];
        if (target < 0 || target >= n) {
          throw new IndexOutOfBoundsException("out-node " + target + " of node " + v
              + " at position " + e + " is not in 0.." + (n - 1));
        }
        if (target <= previous) {
          throw new IllegalArgumentException("the out-nodes of node " + v
              + " must be strictly ascending, but " + target + " follows " + previous);
        }
        previous = target;
      }
    }
    return new Graph(outAdjacencyStart, outAdjacency);
  }

  /**
   * Writes this graph to {@code out}, for {@link #readFrom(InputStream)} to read back with the same
   * node IDs and edge indices.
   * <p>
   * Only the structure is written. Data in arrays indexed by node ID or edge index stays valid for
   * the graph read back and can follow it in the same stream. Here, the names of the methods of a
   * program follow the calls between them:
   * {@snippet file="com/kobayami/graphity/examples/Storing.java" region="writeTo"}
   * A {@link BiAdjacentGraph} writes its out-adjacency only.
   * <p>
   * The format is the two arrays that {@link #of(int[], int[])} takes, preceded by the node count
   * and the edge count. Every field is a signed 32-bit integer in little-endian byte order:
   * <pre>
   * offset in bytes          size in bytes         field
   * 0                        4                     nodeCount
   * 4                        4                     edgeCount
   * 8                        4 x (nodeCount + 1)   outAdjacencyStart
   * 8 + 4 x (nodeCount + 1)  4 x edgeCount         outAdjacency
   * </pre>
   * The arrays follow the rules of {@link #of(int[], int[])}. Nothing else is written: no
   * padding, checksum, magic number or version. So a graph takes exactly
   * 8 + 4 x (nodeCount + 1 + edgeCount) bytes, and {@link #readFrom(InputStream)} recognizes
   * foreign data only where it does not form a valid graph. Every implementation of Graphity reads
   * and writes this format, so a graph written in one language can be read in another.
   * <p>
   * Writes in blocks of up to 64 KiB, so {@code out} needs a {@link java.io.BufferedOutputStream}
   * only if other data shares it in small pieces, as in the example.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> a 64 KiB buffer.
   *
   * @param out the stream to write to; not closed
   * @throws IOException if writing to the stream fails
   */
  public final void writeTo(OutputStream out) throws IOException {
    CsrStreams.write(this, out);
  }

  /**
   * Reads a graph that {@link #writeTo(OutputStream)} wrote, with the same node IDs and edge
   * indices.
   * <p>
   * Reads exactly the bytes of the graph and leaves {@code in} right after them, where data
   * written after the graph follows. Here, that data is the names of the methods of a program:
   * {@snippet file="com/kobayami/graphity/examples/Storing.java" region="readFrom"}
   * The result has no in-adjacency, even if the stream was written from a {@link BiAdjacentGraph}
   * and even when called as {@code BiAdjacentGraph.readFrom(in)}; {@link BiAdjacentGraph#of(Graph)}
   * builds it again.
   * <p>
   * The arrays are checked as by {@link #of(int[], int[])}, but not copied. They grow with the data
   * that arrives, so a header that announces more than the stream holds ends in an
   * {@link java.io.EOFException}, not in an allocation of the announced size. Reads in blocks of
   * up to 64 KiB, so {@code in} needs a {@link java.io.BufferedInputStream} only if other data
   * shares it in small pieces.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + edgeCount) bytes for the result and a 64 KiB
   * buffer; while an array grows, briefly up to 4 x max(nodeCount, edgeCount) bytes more.
   *
   * @param in the stream to read from; not closed
   * @return the graph
   * @throws java.io.EOFException if the stream ends before the graph does
   * @throws IOException if reading fails, a count in the header is negative or larger than a
   *                     graph can have, or the arrays do not form a graph that
   *                     {@link #of(int[], int[])} accepts, whose exception is then the cause
   */
  public static Graph readFrom(InputStream in) throws IOException {
    return CsrStreams.read(in);
  }

  // For Csr, which hands the arrays to the algorithms of the core and of other modules.
  final int[] outAdjacencyStart() {
    return outAdjacencyStart;
  }

  final int[] outAdjacency() {
    return outAdjacency;
  }

  /**
   * Returns the number of nodes.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return node count
   */
  public final int nodeCount() {
    return outAdjacencyStart.length - 1;
  }

  /**
   * Returns the number of edges.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return edge count
   */
  public final int edgeCount() {
    return outAdjacency.length;
  }

  /**
   * Returns whether this graph has no nodes. Equivalent to {@code nodeCount() == 0}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if there are no nodes
   */
  public final boolean isEmpty() {
    return nodeCount() == 0;
  }

  /**
   * Returns the out-degree of {@code node}. That is the number of edges with {@code node} as
   * source.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return out-degree
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int outDegree(int node) {
    Objects.checkIndex(node, nodeCount());
    return outAdjacencyStart[node + 1] - outAdjacencyStart[node];
  }

  /**
   * Returns the out-nodes of {@code node} in ascending order. These are the targets of the edges
   * with {@code node} as source.
   * <p>
   * The result is a view of this graph's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return out-nodes
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final SortedIntView outNodes(int node) {
    Objects.checkIndex(node, nodeCount());
    return new SortedIntView(outAdjacency, outAdjacencyStart[node], outAdjacencyStart[node + 1]);
  }

  /**
   * Returns the start of the edge index range of the out-edges of {@code node}. The out-edges are
   * the edges with {@code node} as source.
   * <p>
   * Their edge indices are {@code outEdgeStart(node)..outEdgeEnd(node)-1}, in the order of
   * {@link #outNodes(int)}; the range is empty if {@code node} has no out-edges. A loop over the
   * range reads the data of each edge from arrays indexed by edge index. Here, it lists the methods
   * that {@code run} calls, with the number of call sites of each call in {@code siteCount}:
   * {@snippet file="com/kobayami/graphity/examples/EdgeData.java" region="outEdges"}
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return start of the range
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int outEdgeStart(int node) {
    Objects.checkIndex(node, nodeCount());
    return outAdjacencyStart[node];
  }

  /**
   * Returns the end, exclusive, of the edge index range of the out-edges of {@code node}; see
   * {@link #outEdgeStart(int)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return end of the range, exclusive
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final int outEdgeEnd(int node) {
    Objects.checkIndex(node, nodeCount());
    return outAdjacencyStart[node + 1];
  }

  /**
   * Returns the node IDs {@code 0..nodeCount()-1} as a stream, in ascending order.
   * <p>
   * <strong>Time:</strong> O(1) to create the stream.
   *
   * @return node IDs
   */
  public final IntStream nodes() {
    return IntStream.range(0, nodeCount());
  }

  /**
   * Calls {@code action} for every edge with its source, target and edge index, in edge index
   * order.
   * <p>
   * The source comes with each edge at no cost, whereas {@link #edgeSource(int)} searches for it.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   *
   * @param action called with {@code (source, target, edgeIndex)} for each edge
   */
  public final void forEachEdge(EdgeConsumer action) {
    int n = nodeCount();
    for (int source = 0; source < n; source++) {
      for (int e = outAdjacencyStart[source], end = outAdjacencyStart[source + 1]; e < end; e++) {
        action.accept(source, outAdjacency[e], e);
      }
    }
  }

  /**
   * Returns the target of edge {@code edgeIndex}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param edgeIndex edge index in {@code 0..edgeCount()-1}
   * @return target node ID
   * @throws IndexOutOfBoundsException if {@code edgeIndex} is out of range
   */
  public final int edgeTarget(int edgeIndex) {
    Objects.checkIndex(edgeIndex, edgeCount());
    return outAdjacency[edgeIndex];
  }

  /**
   * Returns the source of edge {@code edgeIndex}.
   * <p>
   * Unlike the target, the source is searched for; {@link #forEachEdge(EdgeConsumer)} gives every
   * edge with its source without a search.
   * <p>
   * <strong>Time:</strong> O(log nodeCount).
   *
   * @param edgeIndex edge index in {@code 0..edgeCount()-1}
   * @return source node ID
   * @throws IndexOutOfBoundsException if {@code edgeIndex} is out of range
   */
  public final int edgeSource(int edgeIndex) {
    Objects.checkIndex(edgeIndex, edgeCount());
    // The largest node with outAdjacencyStart[node] <= edgeIndex: a node without out-edges shares
    // its offset with the next one.
    int lo = 0, hi = nodeCount() - 1;
    while (lo < hi) {
      int mid = (lo + hi + 1) >>> 1;
      if (outAdjacencyStart[mid] <= edgeIndex) {
        lo = mid;
      } else {
        hi = mid - 1;
      }
    }
    return lo;
  }

  /**
   * Returns whether the edge {@code node -> outNode} exists.
   * <p>
   * <strong>Time:</strong> O(log outDegree(node)).
   *
   * @param node    node ID in {@code 0..nodeCount()-1}
   * @param outNode node ID; a value out of range yields {@code false}
   * @return {@code true} if the edge exists
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean hasOutNode(int node, int outNode) {
    Objects.checkIndex(node, nodeCount());
    return SortedIntView.containsInRange(
        outAdjacency, outAdjacencyStart[node], outAdjacencyStart[node + 1], outNode);
  }

  /**
   * Returns the edge index of the edge {@code source -> target}, or {@code -1} if there is none.
   * <p>
   * {@link GraphBuilder} sorts the edges and merges duplicates, so an edge does not keep the
   * position at which it was added; this method finds where it went. Here, {@code calls} was built
   * with one edge per call site, and the call sites of each call are counted:
   * {@snippet file="com/kobayami/graphity/examples/EdgeData.java" region="edgeIndexOf"}
   * <p>
   * <strong>Time:</strong> O(log outDegree(source)).
   *
   * @param source node ID in {@code 0..nodeCount()-1}
   * @param target node ID; a value out of range yields {@code -1}
   * @return edge index in {@code 0..edgeCount()-1}, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code source} is out of range
   */
  public final int edgeIndexOf(int source, int target) {
    Objects.checkIndex(source, nodeCount());
    return SortedIntView.indexOfInRange(
        outAdjacency, outAdjacencyStart[source], outAdjacencyStart[source + 1], target);
  }

  /**
   * Returns whether {@code node} has an edge to itself, called a self-loop. Equivalent to
   * {@link #hasOutNode(int, int) hasOutNode(node, node)}.
   * <p>
   * <strong>Time:</strong> O(log outDegree(node)).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return {@code true} if the edge {@code node -> node} exists
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public final boolean containsSelfLoop(int node) {
    return hasOutNode(node, node);
  }

  // The body of BiAdjacentGraph.of(Graph) for a graph without in-adjacency.
  final BiAdjacentGraph withInAdjacency() {
    int n = nodeCount();
    int m = edgeCount();
    int[] inAdjacencyStart = new int[n + 1];
    int[] inAdjacency = new int[m];

    // 1) Count in-degrees
    for (int e = 0; e < m; e++) {
      inAdjacencyStart[outAdjacency[e]]++;
    }

    // 2) Prefix-sum: convert in-degrees to start offsets
    int nextStart = 0;
    for (int v = 0; v < n; v++) {
      int inDegree = inAdjacencyStart[v];
      inAdjacencyStart[v] = nextStart;
      nextStart += inDegree;
    }

    // 3) Fill in-adjacency. inAdjacencyStart[target]++ doubles as write pointer.
    //    Ascending node scan ensures each in-list ends up sorted.
    for (int v = 0; v < n; v++) {
      for (int e = outAdjacencyStart[v], end = outAdjacencyStart[v + 1]; e < end; e++) {
        inAdjacency[inAdjacencyStart[outAdjacency[e]]++] = v;
      }
    }

    // 4) Repair: step 3 shifted inAdjacencyStart left by one; right-shift restores it, the
    //    sentinel included.
    for (int v = n; v > 0; v--) {
      inAdjacencyStart[v] = inAdjacencyStart[v - 1];
    }
    inAdjacencyStart[0] = 0;

    return new BiAdjacentGraph(outAdjacencyStart, outAdjacency, inAdjacencyStart, inAdjacency);
  }

  /**
   * Returns a graph with the same nodes and edges, without in-adjacency. A plain {@code Graph}
   * returns itself, and a {@link BiAdjacentGraph} returns a {@code Graph} over its out-adjacency.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return this graph, or a graph over the out-adjacency of a {@code BiAdjacentGraph}
   */
  public Graph uniAdjacentGraph() {
    return this;
  }

  /**
   * Returns whether {@code obj} is a graph with the same nodes and edges.
   * <p>
   * The in-adjacency does not count: a {@link BiAdjacentGraph} equals a {@code Graph} with the
   * same nodes and edges.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   */
  @Override
  public final boolean equals(@Nullable Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Graph other)) return false;
    return Arrays.equals(outAdjacencyStart, other.outAdjacencyStart)
        && Arrays.equals(outAdjacency, other.outAdjacency);
  }

  /**
   * Returns a hash code over the nodes and edges, consistent with {@link #equals(Object)}.
   * <p>
   * It is computed on every call, so each lookup of a graph in a hash-based collection walks the
   * whole graph. {@link Graphs#topologyHash(Graph)} gives a 64-bit hash to compute once and keep.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).
   */
  @Override
  public final int hashCode() {
    return 31 * Arrays.hashCode(outAdjacencyStart) + Arrays.hashCode(outAdjacency);
  }

  /**
   * Returns the node and edge counts and the out-nodes of the first nodes, for debuggers and logs.
   * For the control flow of a method with a loop:
   * {@snippet file="com/kobayami/graphity/examples/Building.java" region="toString"}
   * The text stays short however large the graph: it lists at most 16 nodes and 64 out-nodes in
   * total. {@code ...} marks where it is cut - after the 16th node, or inside the list that would
   * go beyond 64 out-nodes. A {@link BiAdjacentGraph} is named as such and lists its out-nodes.
   * {@link Graphs#adjacencyListOf(Graph)} gives the whole graph as text.
   * <p>
   * <strong>Time:</strong> O(1), as the text is bounded.
   */
  @Override
  public final String toString() {
    int n = nodeCount();
    var sb = new StringBuilder(TO_STRING_CAPACITY);
    sb.append(getClass().getSimpleName()).append("[nodes=").append(n)
        .append(", edges=").append(edgeCount()).append("] {");
    int budget = TO_STRING_TARGETS;
    int listed = Math.min(n, TO_STRING_NODES);
    for (int v = 0; v < listed; v++) {
      if (v > 0) sb.append(", ");
      sb.append(v).append(" -> [");
      int start = outAdjacencyStart[v];
      int end = outAdjacencyStart[v + 1];
      int shown = Math.min(end - start, budget);
      for (int e = start; e < start + shown; e++) {
        if (e > start) sb.append(", ");
        sb.append(outAdjacency[e]);
      }
      budget -= shown;
      if (shown < end - start) {
        sb.append(shown > 0 ? ", ...]" : "...]");
        if (v + 1 < n) sb.append(", ...");
        return sb.append('}').toString();
      }
      sb.append(']');
    }
    if (listed < n) sb.append(", ...");
    return sb.append('}').toString();
  }
}
