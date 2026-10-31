/**
 * Export of graphs to DOT (the language of Graphviz) to look at them with its layout tools.
 * <p>
 * This package is the module {@code graphity-dot}, released with {@code graphity-core} at the same
 * version. {@link DotWriter} writes a graph as DOT text. A graph holds no data, so the writer takes
 * labels and attributes from hooks that receive a node ID or an edge index and read the caller's
 * arrays. {@link AttributeHook} is the hook for the attributes of one node or edge. The clusters
 * come from a partition of the nodes, with a hook for their labels. Here, the packages of a small
 * program that depend on each other in a cycle:
 * {@snippet file="com/kobayami/graphity/examples/WritingDot.java" region="cycle"}
 * The node filter keeps the node IDs of the graph, so the hooks read the same arrays as for the
 * whole graph. For Markdown and documentation, the module {@code graphity-mermaid} writes Mermaid
 * flowcharts.
 * <p>
 * No parameter, return value or element in this package is {@code null} unless its type is
 * annotated {@code @Nullable}.
 */
@NullMarked
package com.kobayami.graphity.dot;

import org.jspecify.annotations.NullMarked;
