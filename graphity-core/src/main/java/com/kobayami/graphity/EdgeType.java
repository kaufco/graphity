package com.kobayami.graphity;

/**
 * The type of an edge in a DFS (depth-first search), as {@link EdgeTypes} classifies the edges of
 * a graph.
 * <p>
 * A depth-first search through all nodes reaches each node once, either by a tree edge or as the
 * root of a new search tree; the tree edges form the DFS forest. Every other edge gets its type
 * from where its target lies relative to its source in that forest, so each edge has exactly one
 * type. {@link Edges#of(EdgeTypes, EdgeType)} collects the edges of one type.
 */
public enum EdgeType {

  /** An edge by which the search reaches its target for the first time: an edge of the forest. */
  TREE,

  /**
   * An edge to an ancestor of its source in the DFS forest, or to the source itself. A graph has
   * a cycle exactly when it has a back edge.
   */
  BACK,

  /** An edge to a descendant of its source in the DFS forest, other than a tree edge. */
  FORWARD,

  /**
   * An edge between two nodes of which neither is an ancestor of the other in the DFS forest: the
   * search had finished the target before it reached the source.
   */
  CROSS
}
