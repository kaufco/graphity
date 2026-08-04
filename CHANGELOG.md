# Changelog

All notable changes to Graphity are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Graphity adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- **`GraphBuilder` construction API.** Eighteen methods covering the patterns that previously
  had to be written out by hand. All additive; existing code is unaffected.

  *Node allocation.* `addChild(parent)`, `addChildren(parent, count)`, `addParent(child)` and
  `addParents(child, count)` add nodes and link them in one call, returning the first new ID.

  *Edge batches.* `addEdges(source, int...)` complements the existing `IntList` overload,
  `addInEdges(target, …)` is its fan-in counterpart, `addPath(…)` chains a sequence of
  nodes, and `addEdgePairs(int...)` takes a flat list of alternating sources and targets.

  *Parameter forms.* The fan-out, fan-in and path methods accept their node IDs as loose
  arguments, as an `int[]`, or as an `IntList` — including the `SortedIntView` that
  `outNodes(node)` and its relatives return, so a result of the library can be fed back into a
  builder without being copied into an array first. `addEdges`, `addEdgesAndNodes`,
  `addInEdges` and `addInEdgesAndNodes` became variable-arity in the process, which changes
  neither their descriptor nor the meaning of any existing call.

  *Creating nodes on reference.* Every edge-adding method has a twin with an `AndNodes`
  suffix — `addEdgeAndNodes`, `addEdgesAndNodes`, `addInEdgesAndNodes`, `addPathAndNodes`,
  `addEdgePairsAndNodes` — that raises the node count to cover the highest ID it references
  instead of requiring the nodes to exist. This is what makes externally numbered input, such
  as block IDs arriving in arbitrary order, work without tracking the maximum by hand.

- **`GraphBuilder.strict()`** — opt-in validation of node references, throwing
  `IndexOutOfBoundsException` instead of relying on assertions. Must be called before the first
  edge, so a builder can never hold a mix of validated and unvalidated edges. Without it,
  behaviour is unchanged.

- **`GraphBuilder.ensureEdgeCapacity(totalEdges)`** — pre-sizes the internal edge storage when
  the edge count is known or can be estimated, avoiding the reallocation and copying that
  filling a growing list to millions of edges otherwise costs.

- **`NodeMapping.forSubgraph(int...)`** — the parameter form that was missing next to the
  existing `BitSet` and `IntList` overloads, so a selection that already is a plain `int[]`, or
  just a handful of loose IDs, no longer has to be wrapped in an `IntArrayList` first. The IDs are
  copied and sorted, leaving the caller's array unchanged; unlike the `IntList` form there is no
  sortedness shortcut, since a plain array carries no such guarantee.

- **`Components.biconnectivityOf(graph)`** — bridges, articulation points and biconnected
  components in one depth-first pass, returned as a `Biconnectivity`. `bridges()` yields an
  `Edges` collection with the lower node ID as source, `articulationPoints()` a sorted
  `SortedIntView`, and `blocks()` an `EdgePartition` over edge indices, because blocks partition
  the edges rather than the nodes — an articulation point belongs to every block that meets it.

  The graph is read as undirected, like `ccsOf`, with the reading now stated explicitly: `a → b`
  and `b → a` are one undirected edge rather than two, so a graph storing its undirected edges
  symmetrically still has bridges, and both edge indices of such a pair land in the same block.
  Self-loops are ignored and left unmapped. The traversal is iterative and verified on a path of
  10^6 nodes. It is the most memory-hungry entry point in the library — during the pass it holds
  up to 37 bytes per node and up to 8 bytes per edge, which the Javadoc states as a number rather
  than as a complexity class.

- **`Ancestry`** — ancestor queries on a forest of out-trees, built once via
  `Ancestry.of(forest)` and then queried arbitrarily often: `lowestCommonAncestorOf(a, b)`,
  `isAncestorOf(node, descendant)`, `depthOf(node)` and `parentOf(node)`. Preprocessing is a
  heavy-path decomposition — O(nodeCount + edgeCount) to build, O(log nodeCount) per
  lowest-common-ancestor query, and 12 bytes per node, an order of magnitude less than the
  level tables that binary lifting or a sparse-table RMQ would need for the same queries.

  Two nodes in different trees of the forest yield `-1`. A graph that is not a forest of
  out-trees is rejected with `IllegalArgumentException` naming the offending node; for a forest
  of in-trees, pass the transpose. Construction and queries are iterative and verified on a
  chain of 10^6 nodes.

- **Edge indices across a transpose.** `transposed()` keeps node IDs but not edge indices, so
  edge metadata held in a parallel array could not be addressed while iterating a transpose.
  Two additions close that gap: `Graphs.transposedEdgeMapping(graph)` translates every edge
  index of `graph.transposed()` back to `graph` in one linear pass and is always total, and
  `BiAdjacentGraph.transposedEdgeIndex(edgeIndex)` answers the same question for a single edge
  in the opposite direction, in O(log nodeCount + log inDegree). Both `transposed()` methods now
  document that edge indices are not preserved, and `Graphs.edgeMapping` documents that it
  cannot be used for a transpose — with the identity node mapping it silently returns `-1`
  entries, because a transpose swaps the two roles its lookup relies on.

- **Undirected shape predicates on `Graphs`.** `isBipartite(graph)`, `isUndirectedTree(graph)`,
  `isUndirectedForest(graph)` and `isWeaklyConnected(graph)` read the graph as undirected, like
  `ccsOf` and `biconnectivityOf`: `a → b` and `b → a` are one undirected edge rather than two. A
  graph storing an undirected structure symmetrically is therefore answered on its actual shape,
  instead of on the two-cycle that every such edge forms under the directed reading — until now
  every shape predicate read the graph as directed, and there was no connectivity predicate at all.

  Self-loops are ignored, with one exception: `isBipartite` reports them as disqualifying, because
  a node adjacent to itself belongs in neither class, and two-colorability is not a connectivity
  question. An empty graph satisfies all four.

  None of the four needs in-adjacency, so passing a plain `Graph` costs nothing over a
  `BiAdjacentGraph`. `isBipartite` and `isWeaklyConnected` are O(nodeCount + edgeCount) and hold 10
  and 5 bytes per node, each stopping as soon as the answer is settled. Holding two elements per
  node is also why `isBipartite` is the one predicate limited in graph size: it requires
  `nodeCount < 2^30` and throws `IllegalArgumentException` above that, rather than failing on an
  array length that has overflowed. `isUndirectedTree` and
  `isUndirectedForest` reject in O(1) when the edge count alone exceeds what a forest could store,
  and otherwise cost O(nodeCount + edgeCount) plus one binary search per edge stored in both
  directions, at 5 bytes per node.

- **`Graphs.isTree(graph)`** — the canonical name for the check that `isOutTree` performs, with
  identical behaviour. Throughout `Graphs` an unqualified predicate name reads the graph as
  directed, which left the spelled-out direction in `isOutTree` as the only exception; putting an
  undirected tree check beside it made that visible at the call site.

### Deprecated

- **`Graphs.isOutTree(graph)`** — use `Graphs.isTree(graph)`, which answers identically in every
  case. Only the name changed, and the old one still works. Whether it is removed in a later major
  version is not decided, so it is not marked `forRemoval`.

- **`Graphs.dfsReordered(graph)`** — reordering renumbers every node, so anything held outside the
  graph and keyed by node ID has to be permuted along with it, and this method discards the only
  thing that can do that. Code that reorders and keeps such data compiles, runs, and reads the
  wrong entries. Use `NodeMapping.reindex(graph)` followed by `Graphs.remapped(graph, newToOld)`
  and keep the mapping; the two calls are exactly what the method did. Nothing replaces it for now,
  and it still works.

## [2.0.0] — 2026-07-23

### Added

- **`IterativeDfsTraverser`** — new public abstract base class for stack-safe depth-first
  traversal. Keeps the DFS stack on the heap; handles arbitrarily deep graphs without any
  `-Xss` tuning. Supports preorder (`enterNode`), postorder (`leaveNode`), subtree pruning,
  and cycle detection via `isOnStack`. Companion to the existing recursive `DfsTraverser`,
  which remains for edge-centric algorithms that must inspect back edges directly.

- **Real-world benchmark profile** — `realworld` and `realworld-quick` profiles run SCC,
  BackEdges and EdgeTypes across five SNAP datasets (web-Google, wiki-Talk, soc-LiveJournal1,
  cit-Patents, cit-HepPh); first published baseline on Ubuntu / AMD Ryzen 9 9950X.

- **`bench` command** in `run-benchmarks.sh` — runs `full` → `big` → `realworld` in one go
  (~3 h total).

- **`big` benchmark profile** — replaces the former `full-big-graph` profile; covers
  n = 1M, 10M, and (sparse-only) 100M nodes, Graphity-only.

### Changed

- **All DFS-based algorithms are now iterative.** `Sccs`, `TopOrder`, `BackEdges`,
  `EdgeTypes`, the shape predicates (`isDag`, `isForest`, `isOutTree`, `isPath`), and node
  reordering (`Graphs.dfsReordered`) run on an explicit heap-allocated stack instead of the
  JVM call stack. They complete on graphs of arbitrary DFS depth under the default `-Xss`,
  with no JVM tuning required.

  Performance impact at n = 100k (AMD Ryzen 9 9950X, OpenJDK 25):

  | Algorithm | Shape            | 1.0     | 2.0.0   | Δ      |
  |-----------|------------------|--------:|--------:|-------:|
  | SCC       | Tree             | 11.7 ms |  2.2 ms | −81%   |
  | SCC       | GNP sparse       |  4.9 ms |  4.3 ms | −12%   |
  | SCC       | GNP dense        | 10.0 ms |  7.4 ms | −26%   |
  | TopOrder  | Tree             |  9.9 ms |  1.3 ms | −87%   |
  | TopOrder  | DAG GNP sparse   |  9.5 ms |  2.3 ms | −76%   |
  | BackEdges | Tree             |  9.9 ms |  1.3 ms | −87%   |
  | EdgeTypes | Tree             | 10.3 ms |  1.3 ms | −87%   |

  Tree and path-shaped graphs see the largest gains because the old recursive implementation
  paid call-stack overhead proportional to DFS depth.

- **`NodePartition` is now sealed.** The class hierarchy is closed; `Sccs` and the WCC/CC
  result types are final implementation details.

- **Benchmark runner profile names updated:** `full-big-graph` → `big`,
  `dataset-quick` → `realworld-quick`, `dataset-full` → `realworld`.

### Fixed

- SCC, TopOrder and all other DFS-based algorithms no longer throw `StackOverflowError`
  on graphs with deep paths (e.g. long chains, path-shaped inputs) under the default JVM
  stack size. Previously, inputs with DFS depth above ~10k–50k (depending on `-Xss`)
  would overflow.

---

## [1.0.0] — 2026-04-27

Initial release.

- Graph representation: immutable CSR layout, 4 B per (node + edge).
- Algorithms: SCC (Tarjan), WCC, CCs, TopOrder (DFS reverse postorder),
  TopOrderAndLevels (Kahn's), EdgeTypes, BackEdges, shape predicates, condensation,
  subgraph extraction, node remapping, DFS-preorder reordering.
- `BiAdjacentGraph` for O(1) bidirectional access.
- Extension points: `DfsTraverser`, `BiAdjacentDfsTraverser`, `FrontierTraverser`,
  `BiAdjacentFrontierTraverser`.
- JMH benchmark suite with synthetic profiles (`quick`, `full`) against JGraphT,
  Guava Graph and WebGraph.
