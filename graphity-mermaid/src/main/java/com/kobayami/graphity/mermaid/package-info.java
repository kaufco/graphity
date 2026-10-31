/**
 * Export of Graphity graphs to Mermaid flowcharts, which render directly in Markdown, issues and
 * documentation.
 * <p>
 * The module {@code graphity-mermaid} builds on {@code graphity-core} and is released together
 * with it, at the same version. Export is output only and has no place on the path of an
 * analysis; callers who never print a graph do not carry it. DOT, for Graphviz and large graphs,
 * is the module {@code graphity-dot}.
 *
 * <ul>
 *   <li>{@link MermaidWriter} - a Mermaid flowchart with labels, node shapes, classes, edge
 *       styles and subgraphs; for graphs of a few hundred nodes.</li>
 *   <li>{@link MermaidWriter.Direction} and {@link MermaidWriter.Shape} - the direction of the
 *       flowchart and the shape of a node.</li>
 * </ul>
 * <p>
 * The graph holds structure only, so every hook is addressed by node ID or edge index and reads
 * the caller's arrays: names, weights, colours, groups. The writer escapes what the hooks return
 * and writes in a fixed order - the subgraphs in group order with their nodes, then the other
 * nodes by ID, then the links in edge index order - so the same input always gives the same
 * text. It exports part of a graph through a node filter without renumbering it, and can write
 * the undirected reading with each reciprocal pair as one link.
 * <p>
 * The package follows the core's null contract: nothing is {@code null} unless its type is
 * annotated {@code @Nullable}. The hooks for labels, shapes, classes and styles may return
 * {@code null}, which means none.
 */
@NullMarked
package com.kobayami.graphity.mermaid;

import org.jspecify.annotations.NullMarked;
