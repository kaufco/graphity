/**
 * Graph analysis algorithms for Java on a compact, immutable graph: connected components,
 * bridges and articulation points, topological order, cycle and edge classification,
 * reachability and breadth-first distances, tree ancestry, dominators, and quotient graphs.
 * <p>
 * Graphity targets compiler and analysis pipelines - abstract syntax trees, control- and
 * data-flow graphs, dependency graphs - and more generally any case where a graph is built from
 * a domain model, analyzed, and the results used. Every algorithm runs without recursion, so deep
 * graphs need no stack tuning, and returns a typed, immutable result with follow-up queries
 * rather than a generic collection.
 *
 * <h2>Algorithms</h2>
 *
 * <h3>Components and connectivity</h3>
 * <ul>
 *   <li>{@link Components#sccsOf(Graph)} - strongly connected components (Tarjan), as
 *       {@link Sccs} with a cycle test per component and the condensation.</li>
 *   <li>{@link Components#wccsOf(Graph)} and {@link Components#ccsOf(Graph)} - weakly connected
 *       components, and connected components under the undirected reading.</li>
 *   <li>{@link Components#biconnectivityOf(BiAdjacentGraph)} - bridges, articulation points and
 *       biconnected components ({@link Biconnectivity}).</li>
 *   <li>{@link Components#strongConnectivityOf(BiAdjacentGraph)} - strong bridges and strong
 *       articulation points ({@link StrongConnectivity}).</li>
 *   <li>{@link Graphs#isStronglyConnected(Graph)} and {@link Graphs#isWeaklyConnected(Graph)}.</li>
 * </ul>
 *
 * <h3>Order, cycles and edge classification</h3>
 * <ul>
 *   <li>{@link TopOrder#of(Graph)} - topological order by depth-first search;
 *       {@link TopOrderAndLevels#of(Graph)} - Kahn's order with a level per node.</li>
 *   <li>{@link Graphs#isDag(Graph)} and {@link Sccs#containsCycle(int)} - cycle tests.</li>
 *   <li>{@link EdgeTypes#of(Graph)} - tree, back, forward and cross edges;
 *       {@link BackEdges#of(Graph)} - the back edges alone.</li>
 * </ul>
 *
 * <h3>Reachability and distances</h3>
 * <ul>
 *   <li>{@link BfsDistances#of(Graph, int)} - breadth-first distances and shortest paths from
 *       one or several sources.</li>
 *   <li>{@link Graphs#reachableFrom(Graph, int)} - the nodes reachable from a source, sorted.</li>
 * </ul>
 *
 * <h3>Trees and ancestry</h3>
 * <ul>
 *   <li>{@link Ancestry#of(Graph)} - parent, depth and lowest common ancestor in trees and
 *       forests; {@link PreorderAncestry#of(Ancestry)} - constant-time ancestor test and subtree
 *       views.</li>
 *   <li>Shape checks: {@link Graphs#isTree(Graph)}, {@link Graphs#isForest(Graph)} and
 *       {@link Graphs#isPath(Graph)}, and under the undirected reading
 *       {@link Graphs#isUndirectedTree(Graph)}, {@link Graphs#isUndirectedForest(Graph)} and
 *       {@link Graphs#isBipartite(Graph)}.</li>
 * </ul>
 *
 * <h3>Dominators</h3>
 * <ul>
 *   <li>{@link Dominators#of(BiAdjacentGraph, int)} - dominators from one or several entries
 *       (Lengauer-Tarjan), and post-dominators on the transposed graph;
 *       {@link Graphs#dominatorTree(Dominators)} builds the dominator tree.</li>
 * </ul>
 * <p>
 * Loops, dominance frontiers, control dependence and phi placement are in the module
 * {@code graphity-controlflow}, package {@code com.kobayami.graphity.controlflow}.
 *
 * <h3>Transformations</h3>
 * <ul>
 *   <li>{@link NodePartition#of(Graph, int[], int)} with {@link NodePartition#condense()} - the
 *       quotient graph by a partition of your choosing; {@link Sccs} condenses to the DAG of
 *       components the same way.</li>
 *   <li>{@link Graphs#remapped(Graph, NodeMapping)} - subgraph extraction and renumbering;
 *       {@link NodeMapping#reindex(Graph)} - a depth-first numbering for cache locality.</li>
 *   <li>{@link BiAdjacentGraph#transposed()} - the transposed graph as a constant-time view.</li>
 * </ul>
 *
 * <h2>Data on nodes and edges</h2>
 * <p>
 * The graph holds structure only. Names, weights and any other data live in arrays you own,
 * indexed by node ID {@code 0..nodeCount()-1} or by edge index {@code 0..edgeCount()-1}:
 * <pre>{@code
 * String[] names = new String[graph.nodeCount()];      // names[node]
 * double[] weights = new double[graph.edgeCount()];    // weights[edgeIndex]
 * int edgeIndex = graph.edgeIndexOf(source, target);
 * }</pre>
 * Every transformation that renumbers nodes or edges comes with a {@link NodeMapping} or
 * {@link EdgeMapping} that carries such arrays along ({@code gather}), and a
 * {@link NodePartition} or {@link EdgePartition} combines them per group ({@code reduce}).
 *
 * <h2>Graph model</h2>
 * <ul>
 *   <li>{@link Graph} - read-only graph with out-adjacency only.</li>
 *   <li>{@link BiAdjacentGraph} - adds in-adjacency for constant-time predecessor access;
 *       obtained via {@link BiAdjacentGraph#of(Graph)}.</li>
 *   <li>{@link GraphBuilder} - mutable, add-only builder that produces immutable {@code Graph}
 *       instances; {@link Graph#of(int[], int[])} takes arrays already in CSR form.</li>
 *   <li>{@link SortedIntView} - the zero-copy, sorted view returned by adjacency and
 *       group-membership lookups throughout the library.</li>
 *   <li>{@link Graph#nodes()} and {@link Graph#forEachEdge(EdgeConsumer)} - every node as a
 *       stream, every edge with source, target and edge index, in edge index order.</li>
 *   <li>{@link Csr} - read access to the CSR arrays themselves, for algorithm code that loops
 *       over all edges; the built-in algorithms use it too.</li>
 *   <li>{@link GraphFormat} - a versioned binary format for storing and loading the topology.</li>
 * </ul>
 * <p>
 * Graphs are stored in a CSR (Compressed Sparse Row) layout: nodes are contiguous {@code int}
 * IDs and the adjacency lists are flat {@code int[]} arrays, 4 bytes per node and per edge.
 * Direction is an interpretation rather than a graph type: edges are stored as directed pairs,
 * and the algorithms for which the distinction matters offer both readings.
 *
 * <h2>Custom traversals</h2>
 * <ul>
 *   <li>{@link IterativeDfsTraverser} - depth-first traversal for arbitrarily deep graphs, with
 *       pre- and postorder hooks.</li>
 *   <li>{@link IterativeDfsEdgeTraverser} - the same with a hook per edge that reports the
 *       target's state, for algorithms that must look at every edge.</li>
 *   <li>{@link FrontierTraverser} - breadth-first traversal with level tracking.</li>
 * </ul>
 * <p>
 * None of them recurses, so none is limited by the call stack. All of them can be run several
 * times with accumulated visited state, started from any set of nodes - for instance
 * {@link Graphs#rootNodes(BiAdjacentGraph)} - and stopped from any hook.
 */
package com.kobayami.graphity;
