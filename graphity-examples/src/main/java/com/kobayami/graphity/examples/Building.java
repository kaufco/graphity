package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;

/** Building a graph, and looking at it. */
final class Building {

  private Building() {}

  /** Builds the control flow of {@code load} from the two arrays of its CSR layout. */
  static Graph cfgFromArrays() {
    // @start region="of"
    // blocks 0..5: entry, header, body, then, next, exit
    int[] outAdjacencyStart = {0, 1, 3, 5, 6, 7, 7};
    int[] outAdjacency = {
        1,       // entry  -> header
        2, 5,    // header -> body, exit
        3, 4,    // body   -> then, next
        4,       // then   -> next
        1};      // next   -> header
    Graph cfg = Graph.of(outAdjacencyStart, outAdjacency);
    // @end
    return cfg;
  }

  /** Returns the text of the control flow of {@code load}. */
  static String cfgAsText() {
    Graph cfg = SmallProgram.cfg();
    // @start region="toString"
    String text = cfg.toString();
    // Graph[nodes=6, edges=7] {0 -> [1], 1 -> [2, 5], 2 -> [3, 4], 3 -> [4], 4 -> [1],
    //                          5 -> []}
    // @end
    return text;
  }
}
