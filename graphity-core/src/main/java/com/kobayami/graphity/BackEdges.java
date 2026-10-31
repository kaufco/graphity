package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;

/**
 * The back edges of a graph: the edges that a depth-first search finds pointing back to a node on
 * its current path, self-loops included.
 * <p>
 * Every cycle contains a back edge, so a graph without back edges is a DAG (directed acyclic
 * graph), and removing the back edges of a graph leaves a DAG. They form a feedback arc set,
 * though not in general a smallest one.
 * <p>
 * Uses the
 * <a href="https://en.wikipedia.org/wiki/Depth-first_search#Output_of_a_depth-first_search">
 * depth-first search</a> of {@link EdgeTypes#of(Graph)}, without recursion: {@link #of(Graph)}
 * finds the edges that {@code EdgeTypes} classifies as {@link EdgeType#BACK}, without classifying
 * the others.
 */
public final class BackEdges {

  private BackEdges() {}

  /**
   * Returns the back edges of {@code graph}, in the order in which the search finds them.
   * <p>
   * Unlike the other results that list edges, they do not come in edge index order. For that
   * order, use {@link Edges#of(EdgeTypes, EdgeType) Edges.of(EdgeTypes.of(graph), EdgeType.BACK)}:
   * the same edges, at the cost of classifying every edge first.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 12 bytes per back edge for the result; while it runs, up to 18 bytes
   * more per back edge, 2 bits per node and up to 8 x nodeCount bytes for the stack, which grows
   * with the deepest path.
   *
   * @param graph the graph to search
   * @return the back edges with their edge indices
   * @throws IllegalArgumentException if a depth-first path is longer than 2<sup>30</sup> - 5 nodes
   */
  public static Edges of(Graph graph) {
    var traverser = new BackEdgeCollector(graph);
    traverser.run();
    // The search does not know the number of back edges in advance; trimming copies the lists to
    // their exact length.
    traverser.sources.trim();
    traverser.targets.trim();
    traverser.edgeIndices.trim();
    return new Edges(
        IntLists.unmodifiable(traverser.sources),
        IntLists.unmodifiable(traverser.targets),
        IntLists.unmodifiable(traverser.edgeIndices));
  }

  // The depth-first search without recursion, collecting every edge to a node on its path. Each
  // edge checks both flags of its target, so they share one int, 16 nodes per int (bit 0
  // visited, bit 1 on the path): one load per edge instead of two bit-set lookups, and the shifts
  // cost nothing next to the cache miss they save.
  private static final class BackEdgeCollector {

    private static final int VISITED = 0b01;
    private static final int ON_STACK = 0b10;

    private final Graph graph;
    private final int[] state;
    final IntArrayList sources = new IntArrayList();
    final IntArrayList targets = new IntArrayList();
    final IntArrayList edgeIndices = new IntArrayList();

    BackEdgeCollector(Graph graph) {
      this.graph = graph;
      this.state = new int[(graph.nodeCount() >> 4) + 1];
    }

    void run() {
      int n = graph.nodeCount();
      int[] out = Csr.outAdjacencyStart(graph);
      int[] adj = Csr.outAdjacency(graph);
      int[] state = this.state;
      IntArrayList sources = this.sources;
      IntArrayList targets = this.targets;
      IntArrayList edgeIndices = this.edgeIndices;
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
          // The scan keeps i and end in registers and touches the stack only when it
          // descends, not once per edge.
          int i = frames[2 * top + 1];
          while (i < end) {
            int v = adj[i++];
            int shift = (v & 0xF) << 1;
            int flags = state[v >> 4] >>> shift;      // one load yields both of v's flags
            if ((flags & VISITED) == 0) {
              frames[2 * top + 1] = i;
              state[v >> 4] |= (VISITED | ON_STACK) << shift;
              if (++top == frames.length >> 1) {
                frames = DfsFrames.grow(frames, n);
              }
              frames[2 * top] = v;
              frames[2 * top + 1] = out[v];
              continue outer;
            } else if ((flags & ON_STACK) != 0) {
              sources.add(u);
              targets.add(v);
              edgeIndices.add(i - 1);
            }
          }
          state[u >> 4] &= ~(ON_STACK << ((u & 0xF) << 1));   // postorder: leave the DFS stack
          top--;
        }
      }
    }
  }
}
