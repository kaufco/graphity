# Graphity Benchmarks

JMH-based microbenchmark suite for Graphity, with [JGraphT](https://jgrapht.org/), [Guava Graph](https://github.com/google/guava/wiki/GraphsExplained) and [WebGraph](https://webgraph.di.unimi.it/) as comparison peers. Measures runtime, allocations and retained heap footprint across a matrix of graph sizes, shapes, libraries and algorithms; aggregates the results into a single Markdown report.

This module is a development artifact — it is not published to Maven Central and is not part of the Graphity release. Reports generated here back the public claims in the main [`README.md`](../README.md); a more detailed public-facing benchmark report is planned with the 1.0.0 release.

> **Status:** stable enough for the Graphity 1.0 baseline numbers. Variance handling at n ≥ 10⁶ is on the 1.1 work list — see [Variance at large n](#variance-at-large-n).

## Quick start

From the repository root:

```bash
mvn package -DskipTests
./graphity-benchmarks/run-benchmarks.sh all quick
```

This builds the benchmark JAR and runs the smallest shipped profile (`quick`, minute-scale) end to end through all four phases. Output is stored in `graphity-benchmarks/results/`.

Requires JDK 21 or newer (same as the main library), Maven, and `python3` on `PATH` (used by the runner for matrix validation and JMH-JSON aggregation).

## What gets measured

A benchmark run is divided into four phases. The `all` subcommand runs them in sequence; each can also be invoked on its own.

| Phase    | Tool             | Output         | What it captures                                          |
|----------|------------------|----------------|-----------------------------------------------------------|
| `perf`   | JMH              | `perf-*.json`  | Wall-time per operation, with confidence intervals        |
| `alloc`  | JMH (`-prof gc`) | `alloc-*.json` | Allocations per operation (bytes, GC-profiler-sampled)    |
| `mem`    | JOL              | `mem-*.csv`    | Retained heap footprint of a built graph object           |
| `report` | aggregator       | `report-*.md`  | Combines the latest `perf` / `alloc` / `mem` to Markdown  |

Performance and allocation runs are intentionally separate: the `-prof gc` instrumentation distorts wall-time numbers, so it is never enabled during `perf`. Memory measurement uses [JOL](https://github.com/openjdk/jol) for precise retained-size accounting and runs in its own JVM.

Both `perf` and `alloc` are preceded by a fail-fast `validate` phase that rejects invalid library / algorithm / shape combinations. Long runs never start on a broken matrix.

## Subcommands

```text
./run-benchmarks.sh <subcommand> <profile>
```

| Subcommand | Effect                                                              |
|------------|---------------------------------------------------------------------|
| `perf`     | Runtime measurement (JMH) for all matrix cases                      |
| `alloc`    | Allocation measurement (JMH `-prof gc`)                             |
| `mem`      | Memory footprint measurement (JOL)                                  |
| `report`   | Aggregate the latest existing `perf` / `alloc` / `mem` outputs to Markdown |
| `all`      | `perf` → `alloc` → `mem` → `report`, in that order                  |

The `report` subcommand does not re-run benchmarks; it requires that at least one upstream phase has already produced output for the given profile.

## Built-in profiles

`<profile>` is resolved as `benchmark-matrix/<profile>.json` (a name) or as a path to any JSON file (relative or absolute). The shipped profiles cover the common scopes:

| Profile          | Scope                                              | Typical wall time |
|------------------|----------------------------------------------------|-------------------|
| `quick`          | small synthetic loop, n = 10⁴ and 10⁵              | minutes           |
| `full`           | full synthetic matrix, all shapes and sizes        | hours             |
| `full-big-graph` | n = 10⁶ and 10⁷, `graphity` + `webgraph` only      | hours             |
| `dataset-quick`  | smoke run on a small SNAP dataset                  | minutes           |
| `dataset-full`   | broader real-world SNAP coverage                   | hours             |

Above roughly n = 10⁶, only `graphity` and `webgraph` finish in reasonable time on input volume alone; the large profiles default to that pairing.

## Custom matrices

A matrix is plain JSON. Pass an explicit path to use a custom one:

```bash
./run-benchmarks.sh all ./graphity-benchmarks/benchmark-matrix/my-profile.json
```

Minimal example:

```json
{
  "name": "my-profile",
  "jmh": {
    "forks": 1,
    "warmup_iterations": 3,
    "warmup_time": "1s",
    "measurement_iterations": 5,
    "measurement_time": "2s",
    "jvm_stack": "-Xss16m"
  },
  "cases": [
    {
      "name": "scc-synth",
      "libraries": ["graphity", "jgrapht"],
      "algorithms": ["scc"],
      "params": {
        "nodeCount": [10000, 100000],
        "shape": ["PATH", "TREE", "GNP_SPARSE"]
      }
    }
  ],
  "mem": {
    "enabled": true,
    "nodes": [10000, 100000],
    "shapes": ["PATH", "TREE", "GNP_SPARSE"],
    "libraries": ["graphity", "jgrapht"]
  }
}
```

### Cases

Each entry in `cases[]` defines one input mode — either synthetic (`params`) or dataset-driven (`dataset`):

```json
{
  "name": "scc-dataset",
  "libraries": ["graphity", "jgrapht", "guava", "webgraph"],
  "algorithms": ["scc", "backedges", "edgetypes"],
  "dataset": {
    "dag": false,
    "files": ["../datasets/snap/web-Google.txt"]
  }
}
```

Dataset paths support `~` expansion and `${ENV}` variables; relative paths resolve against the matrix-file location. Set `dataset.dag` to `true` only if every listed input is guaranteed acyclic — DAG-only algorithms (`toporder`, `toporderandlevels`) require it. `build` is intentionally not allowed in dataset mode (no synthetic generator means there is nothing to time).

### Supported strings

| Field        | Values                                                                                  |
|--------------|-----------------------------------------------------------------------------------------|
| `libraries`  | `graphity`, `jgrapht`, `guava`, `webgraph`                                              |
| `algorithms` | `build`, `scc`, `wcc`, `ccs`, `toporder`, `toporderandlevels`, `backedges`, `edgetypes` |
| `shape`      | see [Shapes](#shapes) below                                                             |

Algorithm coverage is asymmetric: only `graphity` implements `wcc`, `ccs` and `toporderandlevels` natively. The matrix validator rejects unsupported library / algorithm pairings before any run starts.

### Shapes

Synthetic shapes are deterministic — a given `(shape, nodeCount)` produces the same graph across all libraries.

General directed shapes (`build`, `scc`, `wcc`, `ccs`, `backedges`, `edgetypes`):

- `PATH` — chain of nodes; maximum DFS depth; acyclic
- `TREE` — fan-out tree; acyclic
- `GNP_SPARSE`, `GNP_DENSE` — random Erdős–Rényi directed graphs
- `CHAIN_OF_CLIQUES` — clique chain; SCC-heavy

DAG-only shapes (`toporder`, `toporderandlevels`):

- `DAG_GNP_SPARSE`, `DAG_GNP_DENSE` — random DAGs

Shuffled variants (`*_SHUFFLED`) take the corresponding base shape and randomise node IDs. Topology is unchanged; ID-locality is broken. They are the basis for measuring how much each library's locality assumptions are worth — relevant in particular for compression-based representations like WebGraph's BVGraph.

### JMH parameters (`jmh` block)

| Field                    | Meaning                                                |
|--------------------------|--------------------------------------------------------|
| `forks`                  | Number of fresh JVM forks per benchmark                |
| `warmup_iterations`      | JIT warmup iteration count                             |
| `warmup_time`            | Time per warmup iteration (e.g. `1s`)                  |
| `measurement_iterations` | Iterations the score is averaged over                  |
| `measurement_time`       | Time per measurement iteration (e.g. `2s`)             |
| `jvm_stack`              | `-Xss` value passed to the JMH JVM (e.g. `-Xss16m`)    |

Practical defaults: `forks = 0` and short windows for fast feedback during development; `forks = 1` (or higher) and longer windows when comparing libraries.

## Datasets

Real-world inputs are loaded from a normalised text edge-list — one edge per line, whitespace-separated source / target IDs, `#`-prefixed comments. IDs are remapped to `0..n-1` internally. Isolated nodes that have no edges cannot be represented in this format.

```text
# comment
123 456
456 789
```

Common sources:

- [SNAP](https://snap.stanford.edu/data/) — Stanford Network Analysis Platform datasets
- [LAW / WebGraph](http://law.di.unimi.it/datasets.php) — large web graphs

The shipped `dataset-quick` and `dataset-full` profiles reference SNAP files under `datasets/`. Quick setup from the module directory:

```bash
cd graphity-benchmarks
./datasets/download-datasets.sh
./run-benchmarks.sh all dataset-quick
```

The shipped dataset profiles intentionally exclude `toporder` and `toporderandlevels` — public SNAP graphs are typically not acyclic after normalization, so DAG-only algorithms cannot be measured on them without further preprocessing.

## Output and reports

All output is stored in `graphity-benchmarks/results/` with the filename pattern `<phase>-<profile>-<timestamp>`. The aggregated report (`report-*.md`) contains:

- an environment header (host, kernel, JVM, CPU, RAM, JMH parameters)
- a performance section per algorithm — rows are graph shapes, columns are libraries
- a memory-footprint section in *bytes per (node + edge)*
- an allocation section in bytes per call
- a precision summary that lists relative confidence-interval widths, sorted by descending error

Numbers in cells follow the format `value (factor×)`, where the factor is relative to Graphity. Larger factors mean slower or heavier than Graphity in that cell.

Reports are not committed to the Graphity repository; they are produced on demand and live in the local `results/` directory.

## Variance at large n

At n ≥ 10⁶, several benchmarks show 50–97 % CI half-widths. The cause is GC pauses inside the long measurement iterations rather than insufficient measurement time — *lengthening* iterations makes the variance worse, not better. The recommended JMH flags for the 1.1 benchmark round are:

```text
-Xmx16G -XX:+UseZGC -XX:+AlwaysPreTouch -f 3 -wi 5 -w 3s -i 10 -r 5s
```

They are not yet baked into any shipped profile. Until the 1.1 round, treat 1M-node factors as directional and quote ranges rather than single numbers.
