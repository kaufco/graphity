package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Strong bridges and strong articulation points of a directed graph.
 * <p>
 * Obtain via {@link Components#strongConnectivityOf(BiAdjacentGraph)}.
 * <ul>
 *   <li>A <strong>strong bridge</strong> is an edge whose removal increases the number of
 *       strongly connected components.</li>
 *   <li>A <strong>strong articulation point</strong> is a node whose removal does the same:
 *       some two other nodes reach each other in the graph, but not without it.</li>
 * </ul>
 * Both lie inside strongly connected components with more than one node, since removing an edge
 * or node between components never splits a component. On a DAG both are empty, and a self-loop
 * is never a strong bridge. In a cycle without chords every edge is a strong bridge, and every
 * node is a strong articulation point once the cycle has three nodes or more.
 *
 * <h2>Directed interpretation</h2>
 * Edges are read as directed, as in {@link Components#sccsOf(Graph)}: {@code a → b} and
 * {@code b → a} are two edges, and each of them is a strong bridge of the two-node cycle they
 * form. For the undirected reading, see {@link Biconnectivity}.
 *
 * <pre>{@code
 * var strong = Components.strongConnectivityOf(dependencies);   // a BiAdjacentGraph
 * Edges onlyLinks = strong.strongBridges();
 * SortedIntView onlyHubs = strong.strongArticulationPoints();
 * }</pre>
 *
 * @see Components#strongConnectivityOf(BiAdjacentGraph)
 * @see Components#sccsOf(Graph)
 * @see Biconnectivity
 */
public final class StrongConnectivity {

  private final Edges strongBridges;
  private final SortedIntView strongArticulationPoints;

  private StrongConnectivity(Edges strongBridges, SortedIntView strongArticulationPoints) {
    this.strongBridges = strongBridges;
    this.strongArticulationPoints = strongArticulationPoints;
  }

  /**
   * Computes both results, following Italiano, Laura and Santaroni: within a strongly connected
   * component and for any node {@code r} of it, an edge is a strong bridge exactly if it is a
   * bridge of the flow graph rooted at {@code r} or of its transpose, and a node other than
   * {@code r} is a strong articulation point exactly if it dominates another node in one of the
   * two. Whether {@code r} itself is one is a separate check: the rest of its component stays
   * strongly connected or not.
   */
  static StrongConnectivity of(BiAdjacentGraph graph) {
    int n = graph.nodeCount();
    Sccs sccs = Components.sccsOf(graph);
    int sccCount = sccs.sccCount();
    int[] sccOf = sccs.mapping;
    int[] members = sccs.members;
    int[] memberStart = sccs.memberStart;

    // One entry per component, its first member: sink components first for the graph,
    // source components first for its transpose, so every search stays in its component.
    var sinksFirst = new IntArrayList(sccCount);
    var sourcesFirst = new IntArrayList(sccCount);
    for (int c = 0; c < sccCount; c++) {
      sinksFirst.add(members[memberStart[c]]);
      sourcesFirst.add(members[memberStart[sccCount - 1 - c]]);
    }
    int[] forward = Dominators.withinBlocks(graph, sinksFirst, "Strong connectivity");
    int[] backward =
        Dominators.withinBlocks(graph.transposed(), sourcesFirst, "Strong connectivity");

    var bridgeTriples = new IntArrayList();
    var isArticulation = new boolean[n];
    collect(forward, Csr.inAdjacencyStart(graph), Csr.inAdjacency(graph), sccOf, false, bridgeTriples,
        isArticulation);
    collect(backward, Csr.outAdjacencyStart(graph), Csr.outAdjacency(graph), sccOf, true, bridgeTriples,
        isArticulation);
    markEntries(graph, sccOf, members, memberStart, isArticulation);
    return new StrongConnectivity(
        Edges.sortedEdges(graph, bridgeTriples.elements(), bridgeTriples.size() / 3, true),
        articulationPoints(isArticulation));
  }

  /**
   * Adds the bridges and non-trivial dominators of one flow graph, given by its immediate
   * dominators and its in-adjacency. The edge {@code idom(v) → v} is a bridge exactly if it
   * exists - the immediate dominator need not be a predecessor - and {@code v} dominates every
   * other predecessor of {@code v} in its component: every other way in already passes through
   * {@code v}.
   *
   * @param transposed whether the flow graph is the transpose, so a bridge {@code u → v} found
   *     there is the edge {@code v → u} of the graph, and the in-adjacency slot of {@code u} is
   *     the out-adjacency slot of the graph - its edge index
   */
  private static void collect(int[] idom, int[] inStart, int[] in, int[] sccOf,
      boolean transposed, IntArrayList bridgeTriples, boolean[] isArticulation) {
    int[] interval = intervals(idom);
    for (int v = 0; v < idom.length; v++) {
      int u = idom[v];
      if (u < 0) continue;
      if (idom[u] >= 0) isArticulation[u] = true;
      int scc = sccOf[v];
      int fromIdom = -1;
      boolean bridge = true;
      for (int i = inStart[v], end = inStart[v + 1]; i < end; i++) {
        int w = in[i];
        if (w == u) {
          fromIdom = i;
        } else if (sccOf[w] == scc && !dominates(interval, v, w)) {
          bridge = false;
          break;
        }
      }
      if (bridge && fromIdom >= 0) {
        bridgeTriples.add(transposed ? v : u);
        bridgeTriples.add(transposed ? u : v);
        bridgeTriples.add(transposed ? fromIdom : -1);
      }
    }
  }

  /**
   * Preorder interval of every node in the dominator forest given by {@code idom}, two
   * interleaved ints per node: its preorder number and the last preorder number in its subtree.
   */
  private static int[] intervals(int[] idom) {
    int n = idom.length;
    var childStart = new int[n + 1];
    for (int v = 0; v < n; v++) {
      if (idom[v] >= 0) childStart[idom[v] + 1]++;
    }
    for (int v = 0; v < n; v++) {
      childStart[v + 1] += childStart[v];
    }
    var children = new int[childStart[n]];
    var fill = java.util.Arrays.copyOf(childStart, n);
    for (int v = 0; v < n; v++) {
      if (idom[v] >= 0) children[fill[idom[v]]++] = v;
    }
    // A stack that pushes all children of the node it pops yields a preorder in which every
    // subtree is contiguous; subtree sizes then follow in reverse preorder.
    var preorder = fill;
    var stack = new int[n];
    int count = 0;
    for (int root = 0; root < n; root++) {
      if (idom[root] >= 0) continue;
      int top = 0;
      stack[0] = root;
      while (top >= 0) {
        int v = stack[top--];
        preorder[count++] = v;
        for (int i = childStart[v]; i < childStart[v + 1]; i++) {
          stack[++top] = children[i];
        }
      }
    }
    var interval = new int[2 * n];
    for (int k = 0; k < n; k++) {
      interval[2 * preorder[k]] = k;
      interval[2 * preorder[k] + 1] = 1;
    }
    for (int k = n - 1; k >= 0; k--) {
      int v = preorder[k];
      if (idom[v] >= 0) interval[2 * idom[v] + 1] += interval[2 * v + 1];
    }
    for (int v = 0; v < n; v++) {
      interval[2 * v + 1] += interval[2 * v] - 1;
    }
    return interval;
  }

  /** Whether {@code a} dominates {@code b}, given the intervals of {@link #intervals}. */
  private static boolean dominates(int[] interval, int a, int b) {
    int pre = interval[2 * b];
    return interval[2 * a] <= pre && pre <= interval[2 * a + 1];
  }

  /**
   * Marks the entry {@code r} of every component of three or more nodes if the rest of the
   * component is not strongly connected: a search from another member, avoiding {@code r} and
   * the edges leaving the component, misses a member forwards or backwards. Components of two
   * nodes keep one node when either is removed, which is still strongly connected.
   */
  private static void markEntries(BiAdjacentGraph graph, int[] sccOf, int[] members,
      int[] memberStart, boolean[] isArticulation) {
    int n = graph.nodeCount();
    var stamp = new int[n];
    var queue = new int[n];
    for (int c = 0; c < memberStart.length - 1; c++) {
      int size = memberStart[c + 1] - memberStart[c];
      if (size < 3) continue;
      int entry = members[memberStart[c]];
      int start = members[memberStart[c] + 1];
      if (reachedAvoiding(Csr.outAdjacencyStart(graph), Csr.outAdjacency(graph), sccOf, c, entry, start,
              2 * c + 1, stamp, queue) < size - 1
          || reachedAvoiding(Csr.inAdjacencyStart(graph), Csr.inAdjacency(graph), sccOf, c, entry, start,
              2 * c + 2, stamp, queue) < size - 1) {
        isArticulation[entry] = true;
      }
    }
  }

  /** Breadth-first count of the members of component {@code scc} reached from {@code start}. */
  private static int reachedAvoiding(int[] adjStart, int[] adj, int[] sccOf, int scc,
      int avoided, int start, int mark, int[] stamp, int[] queue) {
    stamp[avoided] = mark;
    stamp[start] = mark;
    queue[0] = start;
    int head = 0;
    int tail = 1;
    while (head < tail) {
      int u = queue[head++];
      for (int i = adjStart[u], end = adjStart[u + 1]; i < end; i++) {
        int v = adj[i];
        if (stamp[v] != mark && sccOf[v] == scc) {
          stamp[v] = mark;
          queue[tail++] = v;
        }
      }
    }
    return tail;
  }

  private static SortedIntView articulationPoints(boolean[] isArticulation) {
    int count = 0;
    for (boolean flag : isArticulation) {
      if (flag) count++;
    }
    if (count == 0) return SortedIntView.EMPTY;
    var points = new int[count];
    int pos = 0;
    for (int node = 0; node < isArticulation.length; node++) {
      if (isArticulation[node]) points[pos++] = node;
    }
    return new SortedIntView(points);
  }

  /**
   * Returns the strong bridges with their edge indices, ordered by edge index - that is, by
   * source and then target.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the strong bridges of the graph
   */
  public Edges strongBridges() {
    return strongBridges;
  }

  /**
   * Returns the strong articulation points, sorted ascending.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the strong articulation points of the graph
   */
  public SortedIntView strongArticulationPoints() {
    return strongArticulationPoints;
  }
}
