/**
 * Export of Graphity graphs to DOT, the language of Graphviz, for viewing them with a layout
 * tool.
 * <p>
 * The module {@code graphity-dot} builds on {@code graphity-core} and is released together with
 * it, at the same version. Export is output only and has no place on the path of an analysis;
 * callers who never print a graph do not carry it. Mermaid, for Markdown, issues and
 * documentation, is the module {@code graphity-mermaid}.
 *
 * <ul>
 *   <li>{@link DotWriter} - DOT with labels, free attributes per node and edge, graph attributes
 *       and clusters; for layout tools and large graphs.</li>
 *   <li>{@link AttributeHook} - the hook that adds the attributes of one node or edge.</li>
 * </ul>
 * <p>
 * The graph holds structure only, so every hook is addressed by node ID or edge index and reads
 * the caller's arrays: names, weights, colours, groups. The writer escapes what the hooks return
 * and writes in a fixed order - the clusters in group order with their nodes, then the other
 * nodes by ID, then the edges in edge index order - so the same input always gives the same
 * text. It exports part of a graph through a node filter without renumbering it, and can write
 * the undirected reading with each reciprocal pair as one edge.
 * <p>
 * The package follows the core's null contract: nothing is {@code null} unless its type is
 * annotated {@code @Nullable}. The label hooks of nodes, edges and clusters may return
 * {@code null}, which means none.
 */
@NullMarked
package com.kobayami.graphity.dot;

import org.jspecify.annotations.NullMarked;
