/**
 * Directed graphs and their analysis, for tools that see a program as graphs: components, orders
 * and cycles, reachability, trees and dominators, and graphs derived from other graphs.
 * <p>
 * This package is the module {@code graphity-core}; the modules {@code graphity-controlflow},
 * {@code graphity-dot} and {@code graphity-mermaid} build on it and are released with it at the
 * same version. A {@link Graph} has the nodes {@code 0..nodeCount()-1} and holds nothing but its
 * edges, in 4 bytes per node and per edge; names and other data on nodes and edges stay in arrays
 * of the caller, indexed by node ID or edge index. {@link GraphBuilder} builds a graph from a
 * model of the caller, and the algorithms take the graph and return immutable results. None of
 * them recurses, so a deep graph does not overflow the call stack. Here, the packages of a small
 * program and the packages each of them uses become a graph, which has a cycle:
 * {@snippet file="com/kobayami/graphity/examples/GettingStarted.java" region="firstResult"}
 *
 * <h2>Graphs</h2>
 * <ul>
 *   <li>{@link Graph} - a directed graph with the out-nodes of every node;
 *       {@link BiAdjacentGraph} - one that also holds the in-nodes, for the algorithms that follow
 *       edges against their direction. It gives the transposed graph as well.</li>
 *   <li>{@link GraphBuilder} - builds a graph from edges in any order;
 *       {@link Graph#of(int[], int[])} takes the arrays of its CSR (compressed sparse row) layout,
 *       which {@link Csr} hands to algorithm code of your own.</li>
 *   <li>{@link Graph#writeTo(java.io.OutputStream)} and
 *       {@link Graph#readFrom(java.io.InputStream)} - store a graph in a stream and load it
 *       again.</li>
 *   <li>{@link SortedIntView} - the sorted list of node IDs or edge indices that lookups such as
 *       {@link Graph#outNodes(int)} return; {@link Edges} - a list of edges, each with its source,
 *       target and edge index.</li>
 *   <li>{@link Graphs#adjacencyListOf(Graph)}, {@link Graphs#topologyHash(Graph)} and
 *       {@link Graphs#diff(Graph, Graph)} - a whole graph as text, a hash to keep, and the edges
 *       added and removed between two versions of a graph.</li>
 * </ul>
 *
 * <h2>Components and connectivity</h2>
 * <ul>
 *   <li>{@link Components#sccsOf(Graph)} - the SCCs (strongly connected components), as
 *       {@link Sccs}.</li>
 *   <li>{@link Components#wccsOf(Graph)} - the weakly connected components, which are the
 *       components of the graph read as undirected.</li>
 *   <li>{@link Components#biconnectivityOf(BiAdjacentGraph)} - the bridges, articulation points
 *       and biconnected components, as {@link Biconnectivity}.</li>
 *   <li>{@link Components#strongConnectivityOf(BiAdjacentGraph)} - the strong bridges and strong
 *       articulation points, as {@link StrongConnectivity}.</li>
 *   <li>{@link Graphs#isStronglyConnected(Graph)} and {@link Graphs#isWeaklyConnected(Graph)} -
 *       the same questions for the whole graph, answered without the components.</li>
 * </ul>
 *
 * <h2>Orders, cycles and traversals</h2>
 * <ul>
 *   <li>{@link TopOrder} - a topological order of a DAG (directed acyclic graph);
 *       {@link TopOrderAndLevels} - one with a level per node.</li>
 *   <li>{@link Graphs#isDag(Graph)} and {@link Graphs#findCycle(Graph)} - whether a graph has a
 *       cycle, and one of its cycles.</li>
 *   <li>{@link EdgeTypes} - the tree, back, forward and cross edges of a depth-first search;
 *       {@link BackEdges} - the back edges alone.</li>
 *   <li>{@link DfsTraverser}, {@link DfsEdgeTraverser} and {@link FrontierTraverser} - the base
 *       classes of depth-first and breadth-first searches of your own.</li>
 * </ul>
 *
 * <h2>Reachability and distances</h2>
 * <ul>
 *   <li>{@link Graphs#reachableFrom(Graph, int)} - the nodes reachable from one or several nodes;
 *       {@link Graphs#rootNodes(BiAdjacentGraph)} - the nodes with in-degree 0, called root
 *       nodes.</li>
 *   <li>{@link BfsDistances} - distances and shortest paths from one or several nodes.</li>
 *   <li>{@link Graphs#transitiveClosure(Graph)} and {@link Graphs#transitiveReduction(Graph)} -
 *       an edge for every path, and the fewest edges for the same paths.</li>
 * </ul>
 *
 * <h2>Trees and dominators</h2>
 * <ul>
 *   <li>{@link Ancestry} - parent, depth and lowest common ancestor in a forest;
 *       {@link PreorderAncestry} - the ancestor test in O(1) and the nodes of each subtree.</li>
 *   <li>{@link Graphs#isTree(Graph)}, {@link Graphs#isForest(Graph)} and
 *       {@link Graphs#isPath(Graph)} - shape checks, and their counterparts for the graph read as
 *       undirected, such as {@link Graphs#isUndirectedTree(Graph)}.</li>
 *   <li>{@link Dominators} - the dominators of a control-flow graph, and on its transposed graph
 *       the post-dominators; {@link Graphs#dominatorTree(Dominators)} - the dominator tree as a
 *       graph.</li>
 * </ul>
 * <p>
 * Loops, dominance frontiers, control dependence and the placement of phi-nodes are in the module
 * {@code graphity-controlflow}. The module {@code graphity-mermaid} writes a graph as a Mermaid
 * flowchart, and {@code graphity-dot} writes it as text in DOT (the language of Graphviz).
 *
 * <h2>Derived graphs and the data on them</h2>
 * <ul>
 *   <li>{@link Graphs#remapped(Graph, NodeMapping)} with a {@link NodeMapping} - a subgraph or a
 *       graph with renumbered nodes; {@link EdgeMapping} carries data on the edges over to
 *       it.</li>
 *   <li>{@link NodePartition#condense()} - the condensed graph of a partition of the nodes into
 *       groups; {@link EdgePartition} - the edges behind each of its edges.</li>
 *   <li>{@link BiAdjacentGraph#transposed()} - the transposed graph, without copying;
 *       {@link Graphs#transposedEdgeMapping(Graph)} carries data on the edges over to it.</li>
 * </ul>
 * <p>
 * No parameter, return value or element in this package is {@code null} unless its type is
 * annotated {@code @Nullable}.
 */
@NullMarked
package com.kobayami.graphity;

import org.jspecify.annotations.NullMarked;
