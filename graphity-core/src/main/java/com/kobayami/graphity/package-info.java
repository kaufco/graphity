/**
 * Graphity: a compact, high-performance library for analyzing directed and
 * undirected graphs.
 * <p>
 * It targets compiler and analysis pipelines - abstract syntax trees, control- and
 * data-flow graphs, dependency graphs - and more generally any case where a graph
 * is built from a domain model, analyzed, and the results used.
 * <p>
 * Graphs are stored in a CSR (Compressed Sparse Row) layout: nodes are contiguous
 * {@code int} IDs, edges are flat {@code int[]} arrays, and algorithm results come
 * back as typed, immutable value objects rather than generic collections.
 * <p>
 * The model is topology only: no labels, no weights, no per-node objects. Domain
 * data lives in caller-owned arrays indexed by node or edge ID, and the mapping
 * types below keep those arrays aligned when a transformation renumbers nodes or
 * edges. Direction is likewise an interpretation rather than a graph type: edges
 * are stored as directed pairs, and the algorithms for which the distinction
 * matters offer both readings.
 *
 * <h2>Core data model</h2>
 * <ul>
 *   <li>{@link com.kobayami.graphity.Graph} - read-only graph with out-adjacency only.</li>
 *   <li>{@link com.kobayami.graphity.BiAdjacentGraph} - adds in-adjacency for constant-time
 *       predecessor access; obtained via {@link com.kobayami.graphity.Graph#biAdjacentGraph()}.</li>
 *   <li>{@link com.kobayami.graphity.GraphBuilder} - mutable, add-only builder that produces
 *       immutable {@code Graph} instances.</li>
 *   <li>{@link com.kobayami.graphity.SortedIntView} - the zero-copy, sorted view type returned
 *       by adjacency and group-membership lookups throughout the library.</li>
 * </ul>
 *
 * <h2>Traversal</h2>
 * <ul>
 *   <li>{@link com.kobayami.graphity.DfsTraverser} - recursive depth-first traversal with
 *       per-edge control, for algorithms that must inspect edges to already-visited nodes.
 *       Can throw {@link java.lang.StackOverflowError} on very deep or degenerate graphs;
 *       prefer {@link com.kobayami.graphity.IterativeDfsTraverser} unless per-edge control
 *       is needed.</li>
 *   <li>{@link com.kobayami.graphity.IterativeDfsTraverser} - stack-safe depth-first traversal
 *       for arbitrarily deep graphs, with node-centric pre/postorder hooks.</li>
 *   <li>{@link com.kobayami.graphity.FrontierTraverser} - breadth-first traversal with level
 *       tracking.</li>
 *   <li>{@link com.kobayami.graphity.BiAdjacentDfsTraverser} and
 *       {@link com.kobayami.graphity.BiAdjacentFrontierTraverser} - root-aware variants that
 *       start traversal from in-degree-0 nodes of a {@code BiAdjacentGraph}.</li>
 * </ul>
 *
 * <h2>Algorithms</h2>
 * <ul>
 *   <li>{@link com.kobayami.graphity.Components} - strongly connected components
 *       ({@link com.kobayami.graphity.Sccs}, via Tarjan's algorithm), weakly connected
 *       components (via Union-Find), and connected components under an undirected
 *       reading of the same graph.</li>
 *   <li>{@link com.kobayami.graphity.TopOrder} and {@link com.kobayami.graphity.TopOrderAndLevels}
 *       - topological ordering, with or without level assignment.</li>
 *   <li>{@link com.kobayami.graphity.EdgeTypes} and {@link com.kobayami.graphity.EdgeType} - full
 *       DFS edge classification (tree/back/forward/cross edges).</li>
 *   <li>{@link com.kobayami.graphity.BackEdges} - back-edge detection, the cycle-breaking subset
 *       of edge classification.</li>
 *   <li>{@link com.kobayami.graphity.Graphs} - graph transformations (remapping, DFS reordering)
 *       and structural shape checks ({@code isDag}, {@code isForest}, {@code isOutTree},
 *       {@code isPath}).</li>
 * </ul>
 *
 * <h2>Mappings and partitions</h2>
 * <p>
 * {@link com.kobayami.graphity.NodeMapping} and {@link com.kobayami.graphity.EdgeMapping} model
 * one-to-one (or partial) translations between ID spaces, such as those produced by remapping or
 * subgraph extraction. {@link com.kobayami.graphity.NodePartition} and
 * {@link com.kobayami.graphity.EdgePartition} model many-to-one groupings, such as connected
 * components or graph condensation; both share the reverse-lookup contract defined by
 * {@link com.kobayami.graphity.IndexPartition}.
 */
package com.kobayami.graphity;
