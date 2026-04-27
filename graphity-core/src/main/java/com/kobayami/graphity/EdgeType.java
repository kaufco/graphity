package com.kobayami.graphity;

/**
 * Classification of a directed edge in a DFS traversal.
 * <p>
 * Every edge in a directed graph falls into exactly one of these four categories,
 * determined by the relationship between source and target in the DFS forest:
 * <ul>
 *   <li>{@link #TREE} — target is discovered for the first time via this edge</li>
 *   <li>{@link #BACK} — target is an ancestor of source in the DFS tree (including self-loops)</li>
 *   <li>{@link #FORWARD} — target is a descendant of source, but not via a tree edge</li>
 *   <li>{@link #CROSS} — target is neither ancestor nor descendant (already fully processed)</li>
 * </ul>
 *
 * @see EdgeTypes
 */
public enum EdgeType {

  /** Target is discovered for the first time via this edge. */
  TREE,

  /** Target is an ancestor of source in the DFS tree (or source itself). */
  BACK,

  /** Target is a descendant of source, but already discovered via another path. */
  FORWARD,

  /** Target is in a different subtree, already fully processed. */
  CROSS
}
