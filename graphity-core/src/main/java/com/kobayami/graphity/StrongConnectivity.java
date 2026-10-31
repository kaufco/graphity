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
    // The dominators below allow one node less than Tarjan's algorithm: reject before it runs.
    ArrayLimits.checkNodeCount(n, DfsPreorder.MAX_NODE_COUNT, "strongConnectivityOf");
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
    int[] forward = Dominators.withinBlocks(graph, sinksFirst);
    int[] backward = Dominators.withinBlocks(graph.transposed(), sourcesFirst);

    var sources = new IntArrayList();
    var targets = new IntArrayList();
    var edgeIndices = new IntArrayList();
    var isArticulation = new boolean[n];
    collect(forward, Csr.inAdjacencyStart(graph), Csr.inAdjacency(graph), sccOf, false,
        sources, targets, edgeIndices, isArticulation);
    collect(backward, Csr.outAdjacencyStart(graph), Csr.outAdjacency(graph), sccOf, true,
        sources, targets, edgeIndices, isArticulation);
    markEntries(graph, sccOf, members, memberStart, isArticulation);
    return new StrongConnectivity(
        Edges.sortedEdges(graph, sources.elements(), targets.elements(), edgeIndices.elements(),
            sources.size(), true),
        Biconnectivity.flaggedNodes(isArticulation));
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
      boolean transposed, IntArrayList sources, IntArrayList targets, IntArrayList edgeIndices,
      boolean[] isArticulation) {
    int n = idom.length;
    var preorderIndex = new int[n];
    var subtreeSize = new int[n];
    ParentForest.preorder(idom, preorderIndex, new int[n], subtreeSize);
    for (int v = 0; v < n; v++) {
      int u = idom[v];
      if (u < 0) continue;
      if (idom[u] >= 0) isArticulation[u] = true;
      int scc = sccOf[v];
      int pre = preorderIndex[v];
      int size = subtreeSize[v];
      int fromIdom = -1;
      boolean bridge = true;
      for (int i = inStart[v], end = inStart[v + 1]; i < end; i++) {
        int w = in[i];
        if (w == u) {
          fromIdom = i;
        } else if (sccOf[w] == scc) {
          // v dominates w exactly if w lies in v's subtree, as in PreorderAncestry.isAncestorOf.
          int offset = preorderIndex[w] - pre;
          if (offset < 0 || offset >= size) {
            bridge = false;
            break;
          }
        }
      }
      if (bridge && fromIdom >= 0) {
        sources.add(transposed ? v : u);
        targets.add(transposed ? u : v);
        edgeIndices.add(transposed ? fromIdom : -1);
      }
    }
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
    int[] outStart = Csr.outAdjacencyStart(graph);
    int[] out = Csr.outAdjacency(graph);
    int[] inStart = Csr.inAdjacencyStart(graph);
    int[] in = Csr.inAdjacency(graph);
    var stamp = new int[n];
    var queue = new int[n];
    for (int c = 0; c < memberStart.length - 1; c++) {
      int size = memberStart[c + 1] - memberStart[c];
      if (size < 3) continue;
      int entry = members[memberStart[c]];
      int start = members[memberStart[c] + 1];
      int rest = size - 1;
      // A fresh mark per component and direction, so stamp is never cleared.
      if (reachedAvoiding(outStart, out, sccOf, c, entry, start, 2 * c + 1, stamp, queue) < rest
          || reachedAvoiding(inStart, in, sccOf, c, entry, start, 2 * c + 2, stamp, queue) < rest) {
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
