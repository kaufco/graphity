package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Graph;

/** A graph made from the two arrays of its CSR layout, and the arrays read back. */
final class UsingCsrArrays {

  private UsingCsrArrays() {}

  /** The graph and the arrays read back from it. */
  record FromArrays(Graph graph, int[] start, int[] adjacency) {}

  /** Returns the control flow of the method load, made from its two arrays. */
  static FromArrays load() {
    // @start region="csr"
    // The control flow of the method load, as another tool might hand it over: the
    // out-nodes of all its blocks, block after block, and where the out-nodes of each
    // block start. The blocks entry, header, body, then, next and exit are the nodes
    // 0 to 5.
    int[] start = {0, 1, 3, 5, 6, 7, 7};
    int[] adjacency = {1, 2, 5, 3, 4, 4, 1};
    Graph cfg = Graph.of(start, adjacency);     // copies and checks both arrays
    // cfg: 6 nodes, 7 edges

    // The other way round: the arrays of a graph, for code of your own that reads every
    // edge. They are the graph's own arrays, not copies: read them, never change them.
    int[] outStart = Csr.outAdjacencyStart(cfg);   // [0, 1, 3, 5, 6, 7, 7]
    int[] outNodes = Csr.outAdjacency(cfg);        // [1, 2, 5, 3, 4, 4, 1]
    // @end
    return new FromArrays(cfg, outStart, outNodes);
  }
}
