package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * The strong bridges and strong articulation points of a graph, as
 * {@link Components#strongConnectivityOf(BiAdjacentGraph)} returns them.
 * <p>
 * A strong bridge is an edge whose removal increases the number of SCCs (strongly connected
 * components), and a strong articulation point is a node whose removal does the same. Strong
 * bridges lie inside SCCs of two or more nodes. Strong articulation points lie inside SCCs of
 * three or more nodes, since without one node of an SCC of two, the other still forms an SCC. So
 * a graph without cycles has neither, and a self-loop is never a strong bridge.
 * <p>
 * The edges are followed in their direction, as by {@link Components#sccsOf(Graph)}: the edges
 * {@code a -> b} and {@code b -> a} are two edges, and both are strong bridges of the SCC they
 * form. {@link Biconnectivity} has the counterparts for a graph read as undirected. Here, in the
 * control flow of the method {@code load}, the loop splits into several SCCs without
 * {@code header}, {@code body} or {@code next}, but not without {@code then}:
 * {@snippet file="com/kobayami/graphity/examples/Connectivity.java" region="strongConnectivity"}
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> up to 28 x nodeCount bytes.
 */
public final class StrongConnectivity {

  private final Edges strongBridges;
  private final SortedIntView strongArticulationPoints;

  private StrongConnectivity(Edges strongBridges, SortedIntView strongArticulationPoints) {
    this.strongBridges = strongBridges;
    this.strongArticulationPoints = strongArticulationPoints;
  }

  // The body of Components.strongConnectivityOf, following Italiano, Laura and Santaroni: within
  // an SCC and for any node r of it, an edge is a strong bridge exactly if it is a bridge of the
  // flow graph rooted at r or of its transposed graph, and a node other than r is a strong
  // articulation point exactly if it dominates another node in one of the two. Whether r itself
  // is one is a separate check: the rest of its SCC stays strongly connected or not.
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
    // source components first for its transposed graph, so every search stays in its component.
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

  // Adds the bridges and non-trivial dominators of one flow graph, given by its immediate
  // dominators and its in-adjacency. The edge idom(v) -> v is a bridge exactly if it exists - the
  // immediate dominator need not be an in-node - and v dominates every other in-node of v in its
  // SCC: every other way in already passes through v.
  //
  // With transposed, the flow graph is the transposed graph, so a bridge u -> v found there is
  // the edge v -> u of the graph, and the in-adjacency slot of u is the out-adjacency slot of the
  // graph - its edge index.
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

  // Marks the entry r of every SCC of three or more nodes if the rest of the SCC is not strongly
  // connected: a search from another member, avoiding r and the edges leaving the SCC, misses a
  // member forwards or backwards. SCCs of two nodes keep one node when either is removed, which
  // is still strongly connected.
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

  // Breadth-first count of the members of SCC scc reached from start.
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
   * Returns the strong bridges, in edge index order.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return strong bridges
   */
  public Edges strongBridges() {
    return strongBridges;
  }

  /**
   * Returns the strong articulation points, in ascending order.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return strong articulation points
   */
  public SortedIntView strongArticulationPoints() {
    return strongArticulationPoints;
  }
}
