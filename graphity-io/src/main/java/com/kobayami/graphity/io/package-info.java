/**
 * Export of Graphity graphs to text formats for viewing: DOT for Graphviz and Mermaid for
 * Markdown, issues and documentation.
 * <p>
 * The module {@code graphity-io} builds on {@code graphity-core} and is released together
 * with it, at the same version. Export is output only and has no place on the path of an
 * analysis; callers who never print a graph do not carry it.
 *
 * <ul>
 *   <li>{@link DotWriter} - DOT with labels, free attributes per node and edge, graph attributes
 *       and clusters; for layout tools and large graphs.</li>
 *   <li>{@link MermaidWriter} - a Mermaid flowchart with labels, node shapes, classes, link
 *       styles and subgraphs; renders directly in Markdown, for graphs of a few hundred
 *       nodes.</li>
 * </ul>
 * <p>
 * The graph holds structure only, so every hook is addressed by node ID or edge index and reads
 * the caller's arrays: names, weights, colours, groups. Both writers escape what the hooks
 * return, write in node and edge index order so the same input always gives the same text,
 * export part of a graph through a node filter without renumbering it, and can write the
 * undirected reading with each reciprocal pair as one edge.
 */
package com.kobayami.graphity.io;
