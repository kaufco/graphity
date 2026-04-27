# Graphity

Graphity is a lightweight, high-performance Java library for analyzing directed and undirected graphs. It is built for analysis tools that work with structured graphs like abstract syntax trees, data- and control-flow graphs, dependency graphs, and related shapes. More broadly, anywhere you build a graph from your domain model, run analyses on it, and use the results.

Graphity stores graphs in a compact CSR layout and returns algorithm results as typed, immutable value objects with useful follow-up APIs. On the algorithms covered by [JGraphT](https://jgrapht.org/) or [Guava Graph](https://github.com/google/guava/wiki/GraphsExplained), Graphity is **2×–50× faster** and uses **17×–55× less memory per (node + edge)**. [WebGraph](https://webgraph.di.unimi.it/) (LAW Milano), a specialist for compressed billion-node web crawls, beats Graphity on raw memory compactness and on sparse graphs above 10⁶ nodes (see [Limitations](#limitations)).

> **Status:** first public snapshot. The 1.0.0 release is not yet published to Maven Central — the snippets below show the planned coordinates.

## Installation

The Maven Central coordinates below are final. The artifact is published with the 1.0.0 release; until then, build from source (see [Building, testing, and benchmarking from source](#building-testing-and-benchmarking-from-source)).

### Maven

```xml
<dependency>
    <groupId>com.kobayami</groupId>
    <artifactId>graphity-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'com.kobayami:graphity-core:1.0.0'
}
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.kobayami:graphity-core:1.0.0")
}
```

Requires JDK 21 or newer.

## Quick Start

```java
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
sccs.groupCount();                     // number of SCCs
sccs.get(0);                           // SCC ID of node 0
sccs.nodesOf(0);                       // sorted view of nodes in SCC 0
sccs.containsCycle(0);                 // does this SCC contain a cycle?
var condensed = sccs.condense();       // condensation DAG

// In-adjacency access (opt-in)
var bi = graph.biAdjacentGraph();
bi.inDegree(3);                        // number of incoming edges at node 3
bi.inNodes(3);                         // sorted view of predecessors

// Topological order on a DAG
var order = TopOrder.of(condensed);
```

The result types are themselves graphs or partitions backed by CSR — they compose, and there are no per-call allocations on hot paths. For a deeper introduction and more examples, see [Documentation](#documentation).

## What Graphity gives you

**Compact, topology-only data model.** Graphs hold nodes as integer IDs and edges as integer pairs. No labels, no weights, no per-node objects. Constant 4 bytes per (node + edge) regardless of topology. Labels and weights live in user-owned parallel arrays indexed by node ID or edge ID:

```java
double[] weights = new double[graph.edgeCount()];
```

**`BiAdjacentGraph` for O(1) bidirectional access.** Opt-in via `graph.biAdjacentGraph()`. Doubles the storage to 8 B per (node + edge) and gives constant-time access in both directions — useful for Kahn's topological order, bidirectional BFS, and weakly-connected-component detection.

**Algorithm results as first-class typed values.** Where other libraries hand you `Set<Set<V>>` or `Map<V, Integer>` and let you figure out the rest, Graphity returns a domain object with useful follow-ups:

| Algorithm                  | Result          | Useful follow-ups                            |
|----------------------------|-----------------|----------------------------------------------|
| `Components.sccsOf(graph)` | `Sccs`          | `containsCycle()`, `condense()`, `nodesOf()` |
| `Components.wccsOf(graph)` | `NodePartition` | `nodesOf()`, `condense()`                    |
| `EdgeTypes.of(graph)`      | `EdgeTypes`     | `typeOf(edgeIndex)`, `edgesOfType(TREE)`     |
| `BackEdges.of(graph)`      | `Edges`         | `sourceNodes()`, `targetNodes()`             |
| `NodeMapping.of(...)`      | `NodeMapping`   | `inverse()`, `compose(other)`                |

These types compose. `sccs.condense()` returns a `Graph` plus a `NodeMapping` from original nodes to SCC IDs that you can run further algorithms on; `nodeMapping.compose(other)` chains transformations and applies the result to your own metadata arrays in one step.

**Edge indices as the bridge to user metadata.** Edges have stable CSR positions. You hold edge-attached data in a parallel array of length `graph.edgeCount()`. When a transformation reorders or filters edges (subgraph, condensation, remap), Graphity returns the corresponding `EdgeMapping` so you can reindex your metadata array in one pass.

**Zero-copy views.** Adjacency lists, partition members, edge lists — wherever the underlying CSR already holds the data in the right form, the API hands back a view, not a copy. No allocation on hot loops.

**Immutable graphs, safe to share across threads.** Once built, a `Graph` never changes. The same instance can be passed to multiple algorithms or threads without locking or defensive copying. Mutation happens exclusively through transformations (`subgraph`, `remap`, `condense`) that produce new graphs. The immutability is what makes the zero-copy result types and shared-graph parallelism safe by construction, not by convention.

## Performance and memory at a glance

Selected results at n = 100 000, measured on an AMD Ryzen 9 9950X with OpenJDK 25. Times in milliseconds; the parenthesised factor is relative to Graphity (bigger = slower / more memory).

### Strongly connected components

| Shape             | Graphity | JGraphT          | Guava            | WebGraph         |
|-------------------|---------:|-----------------:|-----------------:|-----------------:|
| GNP_SPARSE        | 4.84 ms  | 103.0 ms (21.3×) | 106.4 ms (22.0×) | 10.27 ms (2.12×) |
| GNP_DENSE         | 10.23 ms | 440.4 ms (43.1×) | —                | 35.07 ms (3.43×) |
| TREE              | 11.30 ms | 39.23 ms (3.47×) | 38.47 ms (3.40×) | 5.65 ms (0.50×)  |

### Topological order

| Shape             | Graphity | JGraphT          | Guava            | WebGraph         |
|-------------------|---------:|-----------------:|-----------------:|-----------------:|
| DAG_GNP_SPARSE    | 9.52 ms  | 60.53 ms (6.36×) | 53.83 ms (5.66×) | 16.67 ms (1.75×) |
| DAG_GNP_DENSE    | 6.25 ms  | 263.5 ms (42.1×) | 264.1 ms (42.2×) | 24.09 ms (3.85×) |

### Memory — bytes per (node + edge)

| Shape             | Graphity | JGraphT          | Guava            | WebGraph         |
|-------------------|---------:|-----------------:|-----------------:|-----------------:|
| GNP_SPARSE        | 4.00 B   | 212.40 B (53.1×) | 120.65 B (30.2×) | 2.38 B (0.59×)   |
| GNP_DENSE         | 4.00 B   | 219.51 B (54.9×) | 108.63 B (27.2×) | 2.07 B (0.52×)   |
| TREE              | 4.00 B   | 208.34 B (52.1×) | 137.73 B (34.4×) | 1.86 B (0.46×)   |

For full results — all algorithms (`Build`, `Sccs`, `Wccs`, `Ccs`, `TopOrder`, `TopOrderAndLevels`, `BackEdges`, `EdgeTypes`), additional graph sizes, allocation counts per operation, shuffled variants, and confidence-interval analysis — see the v1.0 baseline report at [`graphity-benchmarks/baselines/1.0.md`](graphity-benchmarks/baselines/1.0.md).

## Algorithms

Graphity ships the following algorithms out of the box. All of them return typed result values; entry points live on the algorithm class itself, not on the graph.

| Algorithm                                                       | Entry point                   |
|-----------------------------------------------------------------|-------------------------------|
| Strongly connected components (Tarjan)                          | `Components.sccsOf(graph)`    |
| Weakly connected components                                     | `Components.wccsOf(graph)`    |
| Connected components (undirected interpretation)                | `Components.ccsOf(graph)`     |
| Topological order (DFS reverse postorder)                       | `TopOrder.of(graph)`          |
| Topological order with levels (Kahn's)                          | `TopOrderAndLevels.of(graph)` |
| DFS edge classification (TREE / BACK / FORWARD / CROSS)         | `EdgeTypes.of(graph)`         |
| Back edges                                                      | `BackEdges.of(graph)`         |
| Shape predicates (`isDag`, `isForest`, `isOutTree`, `isPath`)   | `Graphs.isDag(graph)`         |
| Condensation DAG                                                | `sccs.condense()`             |
| Subgraph extraction and node remapping                          | `Graphs.remapped(...)`        |

For custom traversals, subclass `DfsTraverser` (or `BiAdjacentDfsTraverser` for combined in/out access). Multiple `run()` calls accumulate visited state, so two-phase walks compose without external bookkeeping.

## Limitations

**DFS-based algorithms have a stack-size ceiling around n = 10⁶.** `Sccs`, `TopOrder`, `BackEdges`, `EdgeTypes` and the public `DfsTraverser` are recursive in 1.0. On long-DFS-path topologies (paths, deep trees) at n in the millions, the JVM stack overflows; the JVM `-Xss` cap (~1 GB) is not enough for the worst cases. Replacing recursion with explicit stacks — both internally and in a new iterative public traverser API — is the headline item for 1.1. Until then, treat 10⁶ as the upper safe range for DFS-driven analysis. GNP-shaped graphs at that size are fine because their recursion depth is logarithmic.

**On sparse graphs above 10⁶ nodes, WebGraph overtakes on runtime.** At n = 1 000 000 on `GNP_SPARSE`, WebGraph's SCC is roughly 1.8× faster than Graphity's, and topological order is roughly tied. The cross-over is real and is driven by `bytes/edge` cache traffic — WebGraph's compressed BVGraph format pays off above a certain density and node count. Graphity 1.0 does not optimise for this corner; whether 1.1 should is on the open agenda. On dense graphs at 10⁶ Graphity still wins comfortably.

## Documentation

This README is the primary user-facing documentation for 1.0 and 1.1; the API reference is a Javadoc site.

- **API reference (Javadoc):** [kobayami.github.io/graphity](https://kobayami.github.io/graphity/). Currently tracks `main`; from 1.0.0 onward each release tag updates the published Javadoc.
- **Benchmark baseline (v1.0):** [`graphity-benchmarks/baselines/1.0.md`](graphity-benchmarks/baselines/1.0.md) — per-shape, per-algorithm performance, memory and allocation tables against JGraphT, Guava and WebGraph, with confidence-interval data.

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

```bash
mvn package -DskipTests
./graphity-benchmarks/run-benchmarks.sh all quick
./graphity-benchmarks/run-benchmarks.sh all full
```

`quick` is a minute-scale developer loop, `full` runs the full synthetic matrix and is hours-scale. For all profiles, JMH parameters, dataset support and the matrix schema, see [`graphity-benchmarks/README.md`](graphity-benchmarks/README.md).

## License

Graphity is licensed under the [Apache License, Version 2.0](LICENSE).

Copyright 2025–2026 Marco Kaufmann.

Forks and modifications are welcome under the terms of the license. The name "Graphity" refers to the original project at https://github.com/kobayami/graphity; please pick a different name for any redistributed version. See [`NOTICE`](NOTICE).
