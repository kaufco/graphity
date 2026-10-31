/**
 * Export of graphs to Mermaid flowcharts, which render directly in Markdown, in issues and in
 * documentation.
 * <p>
 * This package is the module {@code graphity-mermaid}, released with {@code graphity-core} at the
 * same version. {@link MermaidWriter} writes a graph as the text of a Mermaid flowchart, in the
 * {@linkplain MermaidWriter.Direction direction} of choice and with node
 * {@linkplain MermaidWriter.Shape shapes}. A graph holds no data, so the writer takes labels,
 * shapes, classes and styles from hooks that receive a node ID or an edge index and read the
 * caller's arrays. The clusters come from a partition of the nodes, with a hook for their labels.
 * Here, the packages of a small program that depend on each other in a cycle:
 * {@snippet file="com/kobayami/graphity/examples/WritingMermaid.java" region="cycle"}
 * The node filter keeps the node IDs of the graph, so the hooks read the same arrays as for the
 * whole graph. Mermaid renders graphs of a few hundred nodes well; for larger ones, the module
 * {@code graphity-dot} writes DOT (the language of Graphviz).
 * <p>
 * No parameter, return value or element in this package is {@code null} unless its type is
 * annotated {@code @Nullable}.
 */
@NullMarked
package com.kobayami.graphity.mermaid;

import org.jspecify.annotations.NullMarked;
