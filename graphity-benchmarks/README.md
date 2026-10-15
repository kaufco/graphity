# Graphity Benchmarks

JMH-based microbenchmark suite for Graphity, with [JGraphT](https://jgrapht.org/), [Guava Graph](https://github.com/google/guava/wiki/GraphsExplained) and [WebGraph](https://webgraph.di.unimi.it/) as comparison peers. Measures runtime, allocations and retained heap footprint across a matrix of graph sizes, shapes, libraries and algorithms; aggregates the results into a single Markdown report.

This module is a development artifact — it is not published to Maven Central and is not part of the Graphity release. Reports generated here back the public claims in the main [`README.md`](../README.md); a more detailed public-facing benchmark report is planned with the 1.0.0 release.

> **Status:** stable enough for the Graphity 1.0 baseline numbers. Variance handling at n ≥ 10⁶ is on the 1.1 work list — see [Variance at large n](#variance-at-large-n).

## Quick start

From the repository root:

```bash
mvn package -DskipTests
cd graphity-benchmarks
./run-benchmarks.sh clean                        # start from a clean measurements/ dir
./run-benchmarks.sh synthetic all --profile quick
./run-benchmarks.sh report                       # → measurements/report-synthetic-*.md, then promoted to reports/
```

This builds the benchmark JAR and runs the smallest synthetic profile (`quick`, minute-scale) through all measurement phases, aggregates a Markdown report, and updates the versioned baseline archive. Output is stored in `graphity-benchmarks/measurements/` (gitignored scratch) and `graphity-benchmarks/reports/` (versioned).

Requires JDK 21 or newer (same as the main library), Maven, and `python3` on `PATH` (used by the runner for matrix validation, JMH-JSON aggregation, and report post-processing).

## What gets measured

Each run command executes several measurement phases per matrix case:

| Phase    | Tool             | Output         | What it captures                                          |
|----------|------------------|----------------|-----------------------------------------------------------|
| `perf`   | JMH              | `perf-*.json`  | Wall-time per operation, with confidence intervals        |
| `alloc`  | JMH (`-prof gc`) | `alloc-*.json` | Allocations per operation (bytes, GC-profiler-sampled)    |
| `mem`    | JOL              | `mem-*.csv`    | Retained heap footprint of a built graph object           |

Performance and allocation are measured separately: the `-prof gc` instrumentation distorts wall-time numbers, so it is never enabled during `perf`. Memory measurement uses [JOL](https://github.com/openjdk/jol) for precise retained-size accounting and runs in its own JVM. Which phases a case runs is declared in the matrix (`"phases"`); the real-world dataset profiles are perf-only.

Both `perf` and `alloc` are preceded by a fail-fast `validate` phase that rejects invalid library / algorithm / shape combinations. Long runs never start on a broken matrix.

## Commands

The runner is a single script with a three-step workflow — **clean → run → report**:

```text
./run-benchmarks.sh <command> [args]
```

| Command                        | Effect                                                                                              |
|--------------------------------|-----------------------------------------------------------------------------------------------------|
| `clean`                        | Delete run artifacts (JSON/CSV/MD/log) from `measurements/`                                              |
| `clean --lib <a,b>` / `--keep-peers` | Remove only the results of the named libraries (`--keep-peers`: the three Graphity variants), keep the rest as cache |
| `bench [version]`              | Run all phases in sequence: `synthetic full` → `synthetic big` → `realworld` → `report` (~3 h total). |
|                                | `version` is handed to the `report` step. Skips realworld if no dataset files are found under        |
|                                | `datasets/` — loudly, and again in a closing "skipped phases" banner.                                |
| `synthetic <lib\|all>`         | `perf`+`alloc`+`mem` on synthetic graphs (default profile: `full`)                                  |
| `realworld <lib\|all>`         | `perf` on real-world SNAP datasets (default profile: `realworld`)                                   |
| `report [version] [machine]`   | Merge JSON → times Markdown → promote to `reports/versions/` → regenerate both summary views + chart SVGs. |
|                                | Version defaults to the `pom.xml` value (SNAPSHOT suffix stripped); machine is auto-derived.        |
| `ab <label> [--cases a,b]`     | A/B measurement of the current state with the `ab` profile into `measurements/ab/<label>/`, with the commit in `meta.json` |
| `compare <label-a> <label-b>`  | Markdown table of B against A: means, 99.9 % half-widths, B/A; flags `noise` and `wide` (> 15 %)     |

Results in `measurements/` are reused across runs only while each entry's library version and JDK match the current ones: a new Graphity commit or working tree is measured again, the JGraphT, Guava and WebGraph columns are kept. Memory CSVs are reused on the same terms. Changes to the generators, fixtures or peer adapters do not show in a peer's version; after such a change, run `clean --lib jgrapht,guava,webgraph` as well.

`report` does not re-run benchmarks; it aggregates whatever JSON is present in `measurements/`. Because it merges per benchmark (newest run wins), running libraries separately and then calling `report` once produces a single merged report. `report` then copies the result into the versioned `reports/` archive and regenerates the two summary views per machine, with embedded Times and Ops tables and chart SVGs.

### Running one library at a time

`synthetic` and `realworld` take a single library or `all`. Running peers one at a time means a single slow or crashing library can never block the whole run — useful on large real-world graphs where the recursive peers can be orders of magnitude slower than Graphity, or overflow the stack entirely. (A crashing library no longer blocks anything even within a single `all` run — see [Resuming after a crash or interruption](#resuming-after-a-crash-or-interruption) — but splitting by library is still the only way to avoid *waiting* on a much slower peer.)

```bash
./run-benchmarks.sh realworld graphity            # plain Graphity + reorder cost
./run-benchmarks.sh realworld graphity-reordered  # the "Graphity (reordered)" column
./run-benchmarks.sh realworld webgraph            # then each peer on its own
./run-benchmarks.sh realworld jgrapht
./run-benchmarks.sh report                        # merges all of the above into one report
```

Supported libraries: `graphity`, `graphity-reordered`, `graphity-biadjacent`, `jgrapht`, `guava`, `webgraph`, or `all`. The value matches library tokens exactly, so `graphity` runs the plain-Graphity cases (and, in the dataset profiles, the structure-cost cases) but **not** the variants — pass `graphity-reordered` or `graphity-biadjacent` for the `Graphity (reordered)` and `Graphity (bi-adjacent)` columns. `all` runs everything in one go.

## Built-in profiles

By default `synthetic` uses `full` and `realworld` uses `realworld`. Override with `--profile <name>`, resolved as `benchmark-matrix/<name>.json` (a name) or as a path to any JSON file. The shipped profiles cover the common scopes:

| Profile           | Command                                                 | Libraries             | n-values / scope                  | Phases     | Typical wall time |
|-------------------|---------------------------------------------------------|-----------------------|-----------------------------------|------------|-------------------|
| `quick`           | `synthetic all --profile quick`                         | all                   | 10⁴, 10⁵                          | perf+alloc | minutes           |
| `full`            | `synthetic all`                                         | all                   | 10⁴, 10⁵                          | perf+alloc | hours             |
| `big`             | `synthetic all --profile big`                           | Graphity + reordered  | 10⁶, 10⁷, (sparse-only) 10⁸       | perf       | hours             |
| `ab`              | `ab <label> --cases …`                                  | Graphity              | 10⁴, 10⁵, 10⁶, few shapes         | perf       | minutes per case  |
| `realworld-quick` | `realworld all --profile realworld-quick`               | all                   | web-Google only                   | perf       | minutes           |
| `realworld`       | `realworld all`                                         | all                   | broader SNAP coverage             | perf       | hours             |

A **complete baseline** for one machine requires two synthetic profiles plus the real-world run:

```
full  →  n = 10⁴/10⁵, all libraries, perf + alloc + mem
big   →  n = 10⁶/10⁷/10⁸, Graphity + reordered only, perf
```

Use `bench` to run all three plus the report step in one go (see [Commands](#commands)). The real-world part needs the datasets on disk; if they are missing, `bench` skips that phase and says so both at the point of the skip and in a closing banner — see [Real-world graphs](#real-world-graphs-a-separate-run).

- **`big`** is Graphity-only: at n ≥ 10⁶ the generalist peers (JGraphT/Guava object graphs) and WebGraph's in-RAM `ArrayListMutableGraph` build path run out of memory, and the peers' recursive DFS reference wrappers overflow the stack, whereas Graphity's algorithm cores are iterative. The 10⁸ tier is sparse-only because Graphity's int-indexed CSR caps out-adjacency at ~2.1 B edges. Each traversal algorithm is also run on a DFS-preorder reordered copy (see [Reordering](#reordering-graphity-reordered-and-reorder)).
- **`ab`** is for decisions between two Graphity states, not for reports: 3 forks, 10 short iterations, `-Xmx16g -XX:+UseZGC -XX:+AlwaysPreTouch`, `PATH` always among the shapes. Measure each state under its own label and run `compare`.
- **`realworld` / `realworld-quick`** run on real-world edge lists — see [Real-world graphs](#real-world-graphs-a-separate-run).

## Custom matrices

A matrix is plain JSON. Pass an explicit path via `--profile` to use a custom one:

```bash
./run-benchmarks.sh synthetic all --profile ./benchmark-matrix/my-profile.json
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

| Field        | Values                                                                                            |
|--------------|---------------------------------------------------------------------------------------------------|
| `libraries`  | `graphity`, `graphity-reordered`, `graphity-biadjacent`, `jgrapht`, `guava`, `webgraph`             |
| `algorithms` | `build`, `scc`, `wcc`, `ccs`, `condense`, `toporder`, `toporderandlevels`, `backedges`, `edgetypes`, `reorder`, `biadjacency`, `symmetrize` |
| `shape`      | see [Shapes](#shapes) below                                                                        |

Algorithm coverage is asymmetric: `ccs` has no peer implementation, `condense`, `reorder` and `biadjacency` are `graphity`-only, `symmetrize` is `webgraph`-only, and `graphity-biadjacent` exists for `toporderandlevels` alone — it is the only algorithm whose result changes with in-adjacency. `graphity-reordered` supports the traversal algorithms but not `build`. The matrix validator rejects unsupported library / algorithm pairings before any run starts.

See [Reordering](#reordering-graphity-reordered-and-reorder) and [In-adjacency](#in-adjacency-graphity-biadjacent-and-biadjacency) for what the pseudo-libraries and the structure-cost algorithms mean.

### Shapes

Synthetic shapes are deterministic — a given `(shape, nodeCount)` produces the same graph across all libraries.

General directed shapes (`build`, `scc`, `wcc`, `ccs`, `backedges`, `edgetypes`):

- `PATH` — chain of nodes; maximum DFS depth; acyclic
- `TREE` — fan-out tree; acyclic
- `GNP_SPARSE`, `GNP_DENSE` — random Erdős–Rényi directed graphs
- `CHAIN_OF_CLIQUES` — clique chain; SCC-heavy

DAG-only shapes (`toporder`, `toporderandlevels`):

- `DAG_GNP_SPARSE`, `DAG_GNP_DENSE` — random DAGs

`condense` accepts every shape from both groups. It measures `NodePartition.condense()` on the SCC partition, and its cost scales with the *group* count rather than the node count — so the useful axis is how many SCCs a shape produces at a given `nodeCount`: `DAG_GNP_SPARSE` and `PATH` yield one group per node, `CHAIN_OF_CLIQUES` roughly one per 32 nodes, and `GNP_DENSE` collapses into a single giant SCC.

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
| `jvm_heap`               | optional `-Xmx` value passed to the JVM (e.g. `-Xmx64g`) |

Practical defaults: `forks = 0` and short windows for fast feedback during development; `forks = 1` (or higher) and longer windows when comparing libraries.

## Reordering (`graphity-reordered` and `reorder`)

Graphity can permute node IDs into DFS-preorder (`Graphs.remapped(graph, NodeMapping.reindex(graph))`), which gives cache-friendly adjacency for the random `state[v]` accesses that dominate DFS-based algorithms. The benchmark suite measures this in two ways:

- **`graphity-reordered`** is a *pseudo-library*: it runs the identical Graphity algorithm on a reordered copy of the graph and appears as a separate **`Graphity (reordered)`** column next to `Graphity`. Its ratio is relative to the `Graphity` baseline, so `0.50×` means the reordered run is twice as fast. It is kept as a dedicated JMH state so reordered and as-built runs never warm each other up; the one-time reordering itself is excluded from the measured algorithm.
- **`reorder`** is a `graphity`-only algorithm that measures exactly that one-time cost (`NodeMapping.reindex` plus `Graphs.remapped`), reported in its own subsection. Compare it against the per-algorithm savings to judge amortization: reordering only pays off when the layout is poor *and* the graph is traversed several times.

Reordering is a no-op on already ID-local shapes (`PATH`, `TREE`, `CHAIN_OF_CLIQUES`) and helps most on `GNP_*` and `*_SHUFFLED`. All shipped profiles run the reordered/`reorder` cases on the same shapes as their plain counterparts, so the `Graphity (reordered)` column is never partially empty. (The `big` profile used to restrict them to `GNP_SPARSE` and `GNP_DENSE_SHUFFLED` to save runtime, which left visible gaps in the report charts from 10⁶ nodes up.)

## In-adjacency (`graphity-biadjacent` and `biadjacency`)

`graph.biAdjacentGraph()` adds incoming adjacency to a graph that stores outgoing edges only, doubling its footprint. One algorithm in the suite reads it: `TopOrderAndLevels.of` takes in-degrees straight from the in-adjacency offsets in O(nodeCount), where a uni-adjacent graph leaves it scanning every edge in O(nodeCount + edgeCount). The suite measures both halves of that trade, in the same shape as reordering:

- **`graphity-biadjacent`** is a *pseudo-library*: the identical `TopOrderAndLevels.of` call on a bi-adjacent graph, shown as a **`Graphity (bi-adjacent)`** column beside `Graphity`. The conversion happens in trial setup and is not part of the measured time.
- **`biadjacency`** is a `graphity`-only algorithm timing `graph.biAdjacentGraph()` itself, in its own subsection next to the `reorder` cost. Building the in-adjacency is O(nodeCount + edgeCount) — the same order the algorithm saves — so the column and the cost together answer the only interesting question: after how many runs the structure pays for itself.

The peer side of the same trade is **`symmetrize`**, a `webgraph`-only algorithm. WebGraph's `ConnectedComponents` requires a symmetric graph, which a `BVGraph` storing out-adjacency cannot provide, so the `wcc` column for WebGraph runs on a symmetric closure built once per trial with `Transform.symmetrizeOffline` and re-stored as a `BVGraph`. `symmetrize` prices that closure. Graphity needs nothing of the sort for components: `Components.wccsOf` is Union-Find over the out-edges.

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

### Real-world graphs (a separate run)

Real-world benchmarking is its own run via the `realworld` command, independent of the synthetic profiles. The shipped `realworld-quick` and `realworld` profiles reference SNAP files under `datasets/`. The datasets are not versioned in git; fetch them once, then run:

```bash
cd graphity-benchmarks
./datasets/download-datasets.sh          # web-Google, wiki-Talk, soc-LiveJournal1, cit-Patents, cit-HepPh
./run-benchmarks.sh realworld all        # or: realworld all --profile realworld-quick (web-Google only)
./run-benchmarks.sh report
```

The `realworld` profile runs `scc`, `backedges`, `edgetypes` and `wcc` across all libraries on all five datasets, plus the `Graphity (reordered)` column and the `reorder` cost (see [Reordering](#reordering-graphity-reordered-and-reorder)) — so the reordering payoff can be read directly on irregular real inputs, where node IDs are typically not ID-local. `toporderandlevels` runs on `cit-Patents` alone, the only acyclic dataset of the five, and there with the `Graphity (bi-adjacent)` column as well; the `biadjacency` and `symmetrize` costs are measured beside them (see [In-adjacency](#in-adjacency-graphity-biadjacent-and-biadjacency)). `realworld-quick` rehearses every one of those cases on one dataset each — `web-Google` for the general ones, `cit-Patents` for levelling — as a fast smoke test. In the aggregated report the real-world results form their own chapter, with one row per dataset (instead of per shape). The run writes to `measurements/report-realworld-*.md` (times) and is promoted to `reports/versions/` by `report`.

`toporder` is not run on datasets at all, and `toporderandlevels` only on `cit-Patents`: public SNAP graphs are typically not acyclic after normalization, and the matrix validator checks `dataset.dag` against `Graphs.isDag` before a run starts. `cit-HepPh` is a citation network too, and close to a DAG by construction (papers only cite earlier ones), but it does contain cycles after ID normalization — it runs with `dag: false` and is excluded from the DAG-only algorithms.

#### Peer stack sizes on large graphs (`jvm_stack` / `jvm_heap`)

The peer libraries use **recursive** DFS — JGraphT's `KosarajuStrongConnectivityInspector`, the Guava SCC port, and WebGraph's own `StronglyConnectedComponents` (whose Javadoc literally advises setting "a large stack size"). On a deep, large graph like `soc-LiveJournal1` (4.8M nodes) they overflow the default JVM stack. Graphity's cores are iterative and need none of this, but a mixed-library run has to satisfy the peers. Measured minimums to complete SCC on `soc-LiveJournal1`:

| Library  | min `-Xss` | notes                                   |
|----------|-----------:|-----------------------------------------|
| Graphity | (default)  | iterative — stack-safe at any depth     |
| JGraphT  | 16m        | recursive, but shallower recursion here |
| WebGraph | 256m       | recursive; overflows ≤ 128m             |
| Guava    | 512m       | recursive; overflows ≤ 256m             |

`realworld` therefore ships with `jvm_stack: -Xss512m` and `jvm_heap: -Xmx64g` (the combined four-library cross-validation of `soc-LiveJournal1`, the largest of the five datasets, peaks at ~38 GB RSS). `realworld-quick` (only `web-Google`) stays at the default `-Xss16m`. For even deeper graphs the recursive peers fail at any `-Xss`, while Graphity keeps running — see the iterative-algorithms note in the main [`README.md`](../README.md#what-graphity-gives-you).

## Output and reports

There are two levels of output: the **scratch** `measurements/` directory (gitignored) and the **curated** `reports/` directory (versioned, committed).

### Scratch: `measurements/`

`synthetic`/`realworld` runs land in `graphity-benchmarks/measurements/` as one file per matrix case: `<phase>-<profile>@<case>__<algorithm>__<libraries>.json` (and one `mem-<profile>@<libraries>.csv` per `mem` run) — no timestamp in these filenames; see [Resuming after a crash or interruption](#resuming-after-a-crash-or-interruption) below for why. `report` reads all of a profile's case files and turns them into a times Markdown report, `report-<profile>-<ts>.md` — this one *is* timestamped, with the time `report` itself was run. The times report contains:

- an environment header (host, kernel, JVM, CPU, RAM, JMH parameters)
- a performance section per algorithm — rows are graph shapes, columns are libraries (with a separate `Graphity (reordered)` or `Graphity (bi-adjacent)` column when the matrix runs those pseudo-libraries)
- a separate real-world chapter for dataset runs, with one row per dataset instead of per shape
- a memory-footprint section in *bytes per (node + edge)*
- an allocation section in bytes per call
- a precision summary that lists relative confidence-interval widths, sorted by descending error

Numbers in cells follow the format `value (factor×)`, where the factor is relative to Graphity.

### Resuming after a crash or interruption

`synthetic` and `realworld` runs are resumable at the granularity of a matrix **case** (the same unit the matrix file itself is organized into — e.g. `full.json`'s "build" case covers one algorithm across all its libraries and shapes/nodeCounts in a single JMH invocation, exactly as before this feature existed). The rule is simple: **a case whose JSON file already exists in `measurements/` is skipped; only cases without a file there actually run.** There is no separate cache directory — the files in `measurements/` themselves are both the result and the "already done" record.

This only works because the filename is stable across invocations (`<phase>-<profile>@<case>__<algorithm>__<libraries>.json` — no timestamp, no run ID) and because it only ever appears once the case is actually finished: the `java` invocation writes to a temp path first, and only a successful run gets renamed into that final name. A killed process (crash, OOM, `Ctrl-C`) never leaves a half-written file under the name that resume logic checks for.

In practice:

- Interrupting a multi-hour `bench` run and re-running the same command later only redoes whatever file is still missing — not the whole run.
- A crashing or OOM-killed library no longer aborts the rest of the run: that one case is marked failed and the run continues with everything else. `run-benchmarks.sh` lists every failed case at the end and exits non-zero, but only after attempting all of them.
- Deleting a specific case's JSON file by hand (e.g. because you suspect it's stale) makes exactly that case rerun next time; everything else is left alone.
- **A full rerun from scratch requires `clean` first.** Nothing detects "the matrix or its `jmh` settings changed since last time" — if you edit a profile's `forks`/`warmup`/`measurement`/`stack`/`heap` (or its cases) between two runs and don't `clean`, the leftover files from the old settings are silently reused as "already done". `measurements/` is scratch and gitignored specifically so `clean` is always a cheap, safe reset.

Each case's JSON file carries a `measuredAt` (ISO 8601 UTC) field on every entry, added right before the atomic rename — the filename itself has no timestamp, but the data does. (`mem`'s CSV output has no equivalent column; the CSV file's own mtime is the only provenance there.) The date shown in a generated report's header is unrelated to this: it's simply when `report` itself was run.

Caveat: the filename also encodes the library filter actually used for that invocation (see [Running one library at a time](#running-one-library-at-a-time)). Running a case as `all` and later re-running it with a narrower `--lib`/positional filter (or vice versa) produces a differently-named file and reruns rather than reusing a partial match — keep the library filter consistent across resume attempts of what's meant to be "the same" run.

### Curated: `reports/`

`report [version]` copies the newest `measurements/` report into the versioned archive, auto-deriving the machine slug from the report's CPU/OS header (override with a second argument):

- synthetic runs → `reports/versions/<version>-synthetic-<machine>.md`
- real-world runs → `reports/versions/<version>-realworld-<machine>.md`

It then immediately regenerates two summary reports per machine and kind, each with per-subsection **Times (ms/op)** and **Throughput (op/s)** tables plus bar-chart SVGs, and — for synthetic runs — memory and allocation chapters:

- `reports/summary/<kind>-<machine>-peers.md` — the newest measured Graphity version, plus its reordered and bi-adjacent variants, against JGraphT, Guava and WebGraph
- `reports/summary/<kind>-<machine>-history.md` — every measured Graphity version, each with its own variant columns where they were measured, without peers
- `reports/summary/fig/<kind>-<machine>-peers/` and `…-history/` — chart SVGs linked from the respective Markdown

Both views come out of a single generator pass over the same source files, so they can never disagree. Splitting them keeps the peer comparison at a constant width: a new version only widens the history view.

Previous versions' data is never lost: the history view is generated from all `<version>-<kind>-<machine>.md` source files present in the directory, so adding a new version just adds columns. A section without peer data — a Graphity-only algorithm, or a node count beyond the peers' reach — stays in the peer view with a single Graphity column, so that view remains a complete report of the current version; its chart is omitted, because a lone bar at 100 % says nothing.

Sections are matched across versions by **algorithm name and node count**, never by the section number. The numbers in a per-version report are a running index over the algorithms that had data in that run, so they shift as soon as an algorithm is added or missing — `Condense` entered the matrix after 2.0.0 and pushed `TopOrder` from 1.5 to 1.6, for example. Both summary views assign their own numbering from the union of all versions, in the canonical algorithm order, which is why an algorithm measured in only some versions shows up exactly once, with empty columns for the versions that lack it.

Peer columns and the variant columns come from the newest version that measured them, which need not be the newest version overall: a release benchmarked without peers would otherwise drop them from the report. Every column states the version it belongs to, and in the peer view the same rule applies to the Graphity column itself, so a section the newest run skipped is still shown, labelled with the version whose numbers it holds.

Only `reports/` is committed; `measurements/` is scratch and gitignored.

## Variance at large n

At n ≥ 10⁶, several benchmarks show 50–97 % CI half-widths. The cause is GC pauses inside the long measurement iterations rather than insufficient measurement time — *lengthening* iterations makes the variance worse, not better. The recommended JMH flags for the 1.1 benchmark round are:

```text
-Xmx16G -XX:+UseZGC -XX:+AlwaysPreTouch -f 3 -wi 5 -w 3s -i 10 -r 5s
```

They are not yet baked into any shipped profile. Until the 1.1 round, treat 1M-node factors as directional and quote ranges rather than single numbers.
