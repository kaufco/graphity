## [2.3.0] — 2026-10-16

Adds contraction by a partition of your choosing, per-node and per-edge data along mappings
and partitions, edge lookup after `build()`, and BFS distances and reachability.

### Added

- API
  - [Issue #21](../../issues/21) - Add `Graph.edgeIndexOf` - find where an added edge ended up
  - [Issue #22](../../issues/22) - Carry per-node and per-edge data along mappings and partitions
- Algorithms
  - [Issue #20](../../issues/20) - Contract a graph by a partition of your choosing
  - [Issue #23](../../issues/23) - Add `BfsDistances` and `Graphs.reachableFrom`
- Benchmarks
  - [Issue #24](../../issues/24) - Add a more precise benchmark profile for comparing two versions
  - [Issue #25](../../issues/25) - Reuse peer measurements across benchmark runs
  - [Issue #26](../../issues/26) - Benchmark report: record the revision, add a medium profile and traversal-only benchmarks
  - [Issue #39](../../issues/39) - Shorten benchmark runs: short windows, a near-best estimate, cached graphs

---

## [2.2.0] — 2026-10-13

Adds the control-flow analyses a compiler pipeline starts with: dominators, iterated dominance
frontiers, loop nesting forests, and constant-time ancestor tests. The library now builds and
runs on Java 17.

### Added

- API
  - [Issue #15](../../issues/15) - Add `PreorderAncestry` - constant-time ancestor tests and subtree views
- Algorithms
  - [Issue #16](../../issues/16) - Add `Dominators` - dominator trees and dominance frontiers
  - [Issue #17](../../issues/17) - Add `IteratedDominanceFrontier` - phi placement for SSA construction
  - [Issue #18](../../issues/18) - Add `Loops` - loop nesting forest of a control-flow graph, irreducible loops included
- Benchmarks
  - [Issue #19](../../issues/19) - Benchmark the control-flow analyses on graphs shaped like control flow

### Changed

- Infrastructure
  - [Issue #14](../../issues/14) - Support Java 17

---

## [2.1.0] — 2026-08-17

Adds construction and connectivity capabilities: GraphBuilder, ancestry queries,
biconnectivity, and undirected shape predicates. A packed visited set and a raw frame stack
make depth-first search use less memory and run faster.

### Added

- API
  - [Issue #1](../../issues/1) - GraphBuilder: construction API, strict validation and capacity hint
  - [Issue #2](../../issues/2) - Make the edge indices of a transposed graph translatable
  - [Issue #9](../../issues/9) - Let `NodeMapping.forSubgraph` take the selection as an int array
- Algorithms
  - [Issue #4](../../issues/4) - Add `Ancestry` - lowest common ancestor queries on trees and forests
  - [Issue #5](../../issues/5) - Add `Biconnectivity` - bridges, articulation points and biconnected components
  - [Issue #11](../../issues/11) - Shape predicates for the undirected reading of a graph
- Benchmarks
  - [Issue #13](../../issues/13) - Peer columns for weakly connected components and topological levelling

### Changed

- Performance
  - [Issue #3](../../issues/3) - Make the dedup set of `NodePartition.condense()` reset in constant time
  - [Issue #6](../../issues/6) - Replace the `BitSet` of the reindexing DFS core with a packed bit array
  - [Issue #7](../../issues/7) - Hold the DFS frame stack in a raw `long[]` instead of a `LongArrayList`
  - [Issue #8](../../issues/8) - Give `Graphs.remapped` a fast path for mappings that keep every node
- Benchmarks
  - [Issue #12](../../issues/12) - Split the benchmark summary into a peer view and a version history

### Deprecated

Both still work and neither is marked `forRemoval`.

- API
  - [Issue #10](../../issues/10) - Deprecate `Graphs.dfsReordered`
  - [Issue #11](../../issues/11) - Deprecate `Graphs.isOutTree`
    

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
