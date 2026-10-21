# Graphity

[![Maven Central](https://img.shields.io/maven-central/v/com.kobayami/graphity-core?label=Maven%20Central)](https://central.sonatype.com/artifact/com.kobayami/graphity-core)
[![Javadoc](https://img.shields.io/badge/Javadoc-online-blue)](https://kaufco.github.io/graphity/)
[![CI](https://github.com/kaufco/graphity/actions/workflows/ci.yml/badge.svg)](https://github.com/kaufco/graphity/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-17%2B-blue)](https://adoptium.net/temurin/releases/?version=17)

Graphity is a Java library of graph analysis algorithms: strongly and weakly connected components, bridges and articulation points, topological order and levels, cycle detection and edge classification, breadth-first distances and reachability, lowest common ancestors, dominators, dominance frontiers and loop nesting, and quotient graphs. It is built for compiler and analysis pipelines — abstract syntax trees, control- and data-flow graphs, dependency graphs — and more generally for anywhere you build a graph from your domain model, run analyses on it, and use the results.

Every algorithm returns a typed, immutable result with follow-up queries, and runs without recursion, so deep graphs need no stack tuning. On the algorithms covered by [JGraphT](https://jgrapht.org/) and [Guava Graph](https://github.com/google/guava/wiki/GraphsExplained), Graphity is **8×–79× faster** and uses **17×–55× less memory per (node + edge)**. [WebGraph](https://webgraph.di.unimi.it/) (LAW Milano), a specialist for compressed billion-node web crawls, beats Graphity on raw memory compactness (see [when to use something else](#when-to-use-something-else)); on runtime, Graphity is faster across all measured shapes and sizes (2.4×–21×).

Your data stays in your own types: names, weights and any other attributes live in plain arrays indexed by node ID or edge index, and every transformation tells you how to carry them along. The graph itself stores only structure, as a compact CSR layout of 4 bytes per node and per edge — which is where speed and footprint come from.

## Contents

1. [Installation](#installation)
2. [Quick start](#quick-start)
3. [Algorithms](#algorithms)
4. [Data on nodes and edges](#data-on-nodes-and-edges)
5. [Graphity, Guava and JGraphT](#graphity-guava-and-jgrapht)
6. [Performance and memory at a glance](#performance-and-memory-at-a-glance)
7. [Documentation](#documentation)
8. [Building, testing, and benchmarking from source](#building-testing-and-benchmarking-from-source)
9. [License](#license)

## Installation

### Maven

```xml
<dependency>
    <groupId>com.kobayami</groupId>
    <artifactId>graphity-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'com.kobayami:graphity-core:2.4.0'
}
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.kobayami:graphity-core:2.4.0")
}
```

Requires JDK 17 or newer.

## Quick start

```java
import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.TopOrder;

var builder = new GraphBuilder();
builder.addNodes(4);
builder.addEdge(0, 1);
builder.addEdge(0, 2);
builder.addEdge(1, 3);
builder.addEdge(2, 0);

var graph = builder.build();           // immutable, thread-safe

// Strongly connected components
var sccs = Components.sccsOf(graph);
sccs.sccCount();                       // number of SCCs
sccs.sccOf(0);                         // SCC ID of node 0
sccs.nodesOf(0);                       // sorted view of nodes in SCC 0
sccs.containsCycle(0);                 // does this SCC contain a cycle?
var condensed = sccs.condense();       // condensation DAG

// In-adjacency access (opt-in)
var bi = BiAdjacentGraph.of(graph);    // builds the in-adjacency once
bi.inDegree(3);                        // number of incoming edges at node 3
bi.inNodes(3);                         // sorted view of predecessors

// Topological order on a DAG
var order = TopOrder.of(condensed);
```

The result types are themselves graphs or partitions backed by CSR — they compose, and there are no per-call allocations on hot paths. The [tutorial](docs/tutorial.md) walks through building graphs, result types and custom traversals in depth.

## Algorithms

- **Components and connectivity:** strongly connected components (Tarjan), weakly connected components, connected components in the undirected reading; bridges, articulation points and biconnected components; strong bridges and strong articulation points; strong and weak connectivity checks.
- **Order and cycles:** topological order by depth-first search, Kahn's order with a level per node; DAG check and a cycle test per strongly connected component; edge classification into tree, back, forward and cross edges; back edges.
- **Reachability and distances:** breadth-first distances and shortest paths from one or several sources; the set of reachable nodes.
- **Trees and ancestry:** parent, depth and lowest common ancestor in trees and forests; constant-time ancestor test and subtree views; tree, forest and path checks, directed and undirected; bipartiteness.
- **Control flow:** dominators from one or several entries (Lengauer-Tarjan) and post-dominators; dominator tree, dominance frontiers and control dependence; iterated dominance frontiers for phi placement; the loop nesting forest, irreducible loops included (Havlak).
- **Transformations:** quotient graph by a partition of your choosing, condensation of the strongly connected components, subgraph extraction, renumbering for cache locality, transpose — each with the mapping that carries your data along.
- **Custom traversals:** iterative depth-first traversal with node or edge hooks, breadth-first traversal with levels, pruning and early stop — no stack limit.
- **Storage:** graphs from CSR arrays you already have, checked and copied; a versioned little-endian binary format for files, caches and typed arrays in the browser.

Every entry point and its result type is listed in the [algorithm reference](docs/tutorial.md#algorithm-reference); the [Javadoc](https://kaufco.github.io/graphity/) states each method's contract and cost.

## Data on nodes and edges

The graph stores structure only, and that is a feature rather than a chore: names, weights and any other data stay in your own arrays, indexed by node ID `0..nodeCount()-1` or by edge index `0..edgeCount()-1`. One array per attribute, read in tight loops, no per-node objects. When a transformation renumbers nodes or merges them, it comes with the mapping or partition that brings your arrays along:

```java
String[] names = ...;                                            // names[node]
double[] weights = new double[graph.edgeCount()];                // weights[edgeIndex]
weights[graph.edgeIndexOf(a, b)] = 2.5;                          // where an added edge ended up

Sccs sccs = Components.sccsOf(graph);
Graph condensed = sccs.condense();                               // one node per SCC
EdgePartition provenance = Graphs.edgePartition(condensed, graph, sccs);
double[] condensedWeights = provenance.reduce(weights, 0.0, Double::sum);
```

Subgraphs and renumberings work the same way with `NodeMapping.gather` and `EdgeMapping.gather`; the [tutorial](docs/tutorial.md#data-on-nodes-and-edges) has the details.

## Graphity, Guava and JGraphT

The three libraries solve overlapping but different problems.

| | Graphity | JGraphT | Guava Graph |
|---|---|---|---|
| Primary job | Fast analysis of large, static structure graphs | A broad toolbox of graph theory and algorithms | A common, generic way to model graph-shaped domain data |
| Data model | Nodes are `int` IDs, edges `int` pairs, data in your arrays | Arbitrary vertex and edge objects, weights, many graph kinds | Arbitrary node objects; `Graph`, `ValueGraph`, `Network` |
| Algorithms | Focused: components, connectivity, order, cycles, distances, ancestry, dominators, loops, quotients | Very broad: paths, flows, matching, colouring, clustering, isomorphism, planarity, TSP and more | Deliberately small: traversal, cycle check, reachability, transitive closure, transpose, induced subgraph |
| Best fit | Compiler and analysis pipelines, dependency graphs, ASTs and CFGs, repeated analyses, deep graphs | Research, optimization, routing, networks, combinatorial problems | Application models with object identity and simple graph operations |

**Deep graphs: Graphity runs where the others need tuning.** JGraphT, Guava and WebGraph use recursive depth-first search. On `soc-LiveJournal1` (4.8M nodes), JGraphT's and WebGraph's SCC overflow the default stack and complete only with `-Xss` raised to 256–512 MB; on deeper topologies they fail regardless. Every Graphity algorithm keeps its stack on the heap and runs under the default `-Xss`, verified at a depth of 1M.

### When to use something else

**Weighted optimization: use JGraphT.** Graphity works on pure topology — components, ordering, edge classification, condensation, remapping. Weights and capacities are deliberately outside the data model, and with them the optimization domains built on top: weighted shortest paths, flows and cuts, matching and assignment, TSP, colouring. For those, JGraphT is the right tool and a good one.

**Mutable graphs of domain objects: use Guava.** Graphity graphs are immutable and built once; nodes are integers. If your graph changes edge by edge and you want to query it by your own objects, Guava's model fits better.

**Memory compactness: WebGraph uses less memory.** Graphity stores 4 B per (node + edge) — constant regardless of topology. WebGraph's compressed BVGraph format achieves 0.16–2.38 B per element by exploiting locality and delta coding. On memory-limited machines with very large graphs, that difference matters. Graphity does not target this corner; its CSR layout is optimised for fast random access, not compression.

## Performance and memory at a glance

Measured on an AMD Ryzen 9 9950X with OpenJDK 25. The parenthesised factor is relative to Graphity.

### SCC (Strongly connected components) on synthetic graphs

#### Times @ 100k nodes (ms/op, lower is better)

| Shape            | Graphity | JGraphT         | Guava           | WebGraph        |
|------------------|---------:|----------------:|----------------:|----------------:|
| Tree             | 2.161 ms | 41.51 ms (19×)  | 38.09 ms (18×)  | 5.712 ms (2.6×) |
| GNP sparse       | 4.389 ms | 109.6 ms (25×)  | 87.01 ms (20×)  | 10.30 ms (2.3×) |
| GNP dense        | 9.265 ms | 446.5 ms (48×)  | 333.1 ms (36×)  | 36.12 ms (3.9×) |
| Chain of cliques | 3.058 ms | 166.0 ms (54×)  | 108.0 ms (35×)  | 37.02 ms (12×)  |

#### Throughput @ 100k nodes (higher is better)

![SCC at n = 100k](graphity-benchmarks/reports/summary/fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-2-scc-n-100-000.svg)

### SCC on real-world graphs (SNAP datasets)

#### Times (ms/op, lower is better)

| Dataset                                  | Graphity | JGraphT          | Guava            | WebGraph        |
|------------------------------------------|---------:|-----------------:|-----------------:|----------------:|
| web-Google (875k nodes, 5.1M edges)      | 57.55 ms | 1266.8 ms (22×)  | 799.0 ms (14×)   | 234.3 ms (4.1×) |
| wiki-Talk (2.4M nodes, 5M edges)         | 74.37 ms | 1391.8 ms (19×)  | 1466.2 ms (20×)  | 153.2 ms (2.1×) |
| soc-LiveJournal1 (4.8M nodes, 69M edges) | 1073.8 ms | 22122 ms (21×) | 16631 ms (15×)   | 3864.4 ms (3.6×) |

#### Throughput (higher is better)

![SCC on real-world graphs](graphity-benchmarks/reports/summary/fig/realworld-ubuntu-ryzen-9950x-peers/1-1-scc-real-world-graphs.svg)

### Memory — bytes per node + edge (lower is better)

| Shape      | Graphity | JGraphT          | Guava            | WebGraph         |
|------------|---------:|-----------------:|-----------------:|-----------------:|
| GNP sparse | 4.00 B   | 212.40 B (53×)   | 120.65 B (30×)   | 2.38 B (0.60×)   |
| GNP dense  | 4.00 B   | 219.51 B (55×)   | 108.63 B (27×)   | 2.07 B (0.52×)   |
| Tree       | 4.00 B   | 208.34 B (52×)   | 137.73 B (34×)   | 1.86 B (0.47×)   |

For full results — all algorithms (`Build`, `Sccs`, `Wccs`, `Ccs`, `TopOrder`, `TopOrderAndLevels`, `BackEdges`, `EdgeTypes`), graph sizes from 10k to 100M nodes, allocation counts, shuffled variants, and confidence-interval analysis — see the benchmark reports:

- **Synthetic:** [`graphity-benchmarks/reports/summary/synthetic-ubuntu-ryzen-9950x-peers.md`](graphity-benchmarks/reports/summary/synthetic-ubuntu-ryzen-9950x-peers.md)
- **Real-world (SNAP datasets):** [`graphity-benchmarks/reports/summary/realworld-ubuntu-ryzen-9950x-peers.md`](graphity-benchmarks/reports/summary/realworld-ubuntu-ryzen-9950x-peers.md)

How the numbers developed across Graphity releases is a separate report per machine, `…-history.md` next to the two above.

## Documentation

This README is the overview; the tutorial goes into depth, and the Javadoc is the contract.

- **Tutorial:** [`docs/tutorial.md`](docs/tutorial.md) — the data model, building graphs, result types and their follow-ups, carrying your data through transformations, custom traversals, and the reference of all algorithm entry points.
- **API reference (Javadoc):** [kaufco.github.io/graphity](https://kaufco.github.io/graphity/). It opens on an overview of the algorithms by question and is republished with every release.
- **Synthetic benchmark report:** [`graphity-benchmarks/reports/summary/synthetic-ubuntu-ryzen-9950x-peers.md`](graphity-benchmarks/reports/summary/synthetic-ubuntu-ryzen-9950x-peers.md) — per-shape, per-algorithm timings and throughput against JGraphT, Guava and WebGraph, plus memory and allocation chapters, with bar charts.
- **Real-world benchmark report:** [`graphity-benchmarks/reports/summary/realworld-ubuntu-ryzen-9950x-peers.md`](graphity-benchmarks/reports/summary/realworld-ubuntu-ryzen-9950x-peers.md) — SCC, BackEdges, EdgeTypes and weakly connected components across five SNAP datasets up to 4.8M nodes, plus topological levelling on the one acyclic dataset among them.
- **Version history:** [`graphity-benchmarks/reports/summary/synthetic-ubuntu-ryzen-9950x-history.md`](graphity-benchmarks/reports/summary/synthetic-ubuntu-ryzen-9950x-history.md) — the same measurements with one column per released version instead of the peer libraries.
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)

## Building, testing, and benchmarking from source

Most users do not need to build from source — the published artifact (see [Installation](#installation)) is the recommended way to consume Graphity. The instructions below are for contributors who want to fork or extend the library.

The project is a multi-module Maven build: `graphity-core` is the library and the only artifact published to Maven Central; `graphity-benchmarks` is a development module containing the JMH benchmark suite and is not part of the release.

### Build and test

From the repository root:

```bash
mvn verify
```

Compiles both modules and runs unit and fuzz tests for `graphity-core`. For tests only, without packaging:

```bash
mvn test
```

### Benchmarks

The benchmark runner is a single script with a three-step workflow: **clean → run → report**. All commands are subcommands of `run-benchmarks.sh`; run `./run-benchmarks.sh --help` for the full reference.

```bash
mvn package -DskipTests
cd graphity-benchmarks

# 1 — clear scratch output from previous runs
./run-benchmarks.sh clean

# 2 — run the benchmarks (synthetic and/or real-world).
#     Pass a single library or 'all'. Running one library at a time means a
#     slow or crashing peer can never block the whole run.
./run-benchmarks.sh synthetic all          # all libraries, synthetic graphs
./run-benchmarks.sh realworld all          # all libraries, real-world graphs
./run-benchmarks.sh synthetic graphity     # graphity only, synthetic graphs

#     To run all benchmarks (~3 h total):
./run-benchmarks.sh bench

# 3 — report measured results into the versioned reports/ folder
./run-benchmarks.sh report                 # report as current graphity version
./run-benchmarks.sh report 2.1.0           # report as a specific version
```

`synthetic` and `realworld` accept a library (`graphity`, `graphity-reordered`, `graphity-biadjacent`, `jgrapht`, `guava`, `webgraph`) or `all`. Each run writes JSON artifacts to `graphity-benchmarks/measurements/` (gitignored); `report` merges them, promotes to `reports/versions/<version>-<kind>-<machine>.md`, and regenerates the summary views.

**Profiles.** By default `synthetic` uses the `full` matrix (n = 10k and 100k, all libraries; hours-scale) and `realworld` uses `realworld`. Override with `--profile`:

```bash
./run-benchmarks.sh synthetic all --profile quick           # minute-scale dev loop
./run-benchmarks.sh synthetic all --profile big             # n = 1M, 10M, 100M (sparse)
./run-benchmarks.sh realworld all --profile realworld-quick # web-Google only, smoke run
```

- **`big`** covers n = 1M, 10M and (sparse-only) 100M and is **Graphity-only**: at n ≥ 1M the generalist peers' object graphs and WebGraph's in-RAM build path run out of memory or overflow the stack, whereas Graphity's algorithm cores are iterative.
- Each traversal algorithm is additionally run on a DFS-preorder reordered copy of the graph — reported as the **`Graphity (reordered)`** column — and the one-time reorder cost is captured by the graphity-only `reorder` algorithm. Kahn's algorithm also runs on a bi-adjacent graph, where it reads in-degrees without scanning the edges, as the **`Graphity (bi-adjacent)`** column; `biadjacency` prices that structure, and `symmetrize` the symmetric graph WebGraph's connected components require.

**Real-world datasets** are not versioned in git; fetch them once before the first `realworld` run:

```bash
./datasets/download-datasets.sh   # web-Google, wiki-Talk, soc-LiveJournal1, …
```

**Reports.** Promoted results land in `reports/versions/` as `<version>-<kind>-<machine>.md` source files. Two summary views are regenerated from all source files present: `reports/summary/<kind>-<machine>-peers.md` puts the newest measured version against the peer libraries, and `…-history.md` puts the measured versions against each other. Historical data from previous versions is never lost. Both reports and chart SVGs are committed to the repository.

For JMH parameters, dataset support and the matrix schema, see [`graphity-benchmarks/README.md`](graphity-benchmarks/README.md).

## License

Graphity is licensed under the [Apache License, Version 2.0](LICENSE).

Copyright 2025–2026 Marco Kaufmann.

Forks and modifications are welcome under the terms of the license. The name "Graphity" refers to the original project at https://github.com/kaufco/graphity; please pick a different name for any redistributed version. See [`NOTICE`](NOTICE).
