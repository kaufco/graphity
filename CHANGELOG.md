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

- **Edge indices across a transpose.** `transposed()` keeps node IDs but not edge indices, so
  edge metadata held in a parallel array could not be addressed while iterating a transpose.
  Two additions close that gap: `Graphs.transposedEdgeMapping(graph)` translates every edge
  index of `graph.transposed()` back to `graph` in one linear pass and is always total, and
  `BiAdjacentGraph.transposedEdgeIndex(edgeIndex)` answers the same question for a single edge
  in the opposite direction, in O(log nodeCount + log inDegree). Both `transposed()` methods now
  document that edge indices are not preserved, and `Graphs.edgeMapping` documents that it
  cannot be used for a transpose — with the identity node mapping it silently returns `-1`
  entries, because a transpose swaps the two roles its lookup relies on.

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
