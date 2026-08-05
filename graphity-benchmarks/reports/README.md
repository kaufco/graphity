# Benchmark Reports

This directory holds the **published** benchmark reports. Two kinds of files live here:

```
graphity-benchmarks/
│
├── measurements/              ← gitignored scratch (raw JMH JSON, JOL CSV)
│   ├── perf-full-20260722-143012.json
│   ├── alloc-full-20260722-143012.json
│   └── mem-full-20260722-150811.csv
│
├── reports/                   ← committed
│   │
│   ├── versions/              ← per-version source files (one per Graphity version × machine)
│   │   ├── 1.0-synthetic-ubuntu-ryzen-9950x.md
│   │   ├── 1.0-synthetic-macos-m5.md
│   │   ├── 2.0.0-synthetic-ubuntu-ryzen-9950x.md
│   │   ├── 2.0.0-synthetic-macos-m5.md
│   │   └── 2.0.0-realworld-ubuntu-ryzen-9950x.md
│   │
│   └── summary/               ← per-machine reports (THE files to look at)
│       ├── synthetic-ubuntu-ryzen-9950x-peers.md     ✓ open this one
│       ├── synthetic-ubuntu-ryzen-9950x-history.md   ✓ or this one
│       ├── synthetic-macos-m5-peers.md
│       ├── realworld-ubuntu-ryzen-9950x-peers.md
│       └── fig/
│           ├── synthetic-ubuntu-ryzen-9950x-peers/
│           │   └── *.svg
│           └── synthetic-ubuntu-ryzen-9950x-history/
│               └── *.svg
│
├── benchmark-matrix/          ← benchmark profile configs (JSON)
├── times-to-ops.py            ← generator script (versions/ → summary/)
└── run-benchmarks.sh          ← orchestrator
```

## What to look at

Open a file from **`reports/summary/`**. There are two per machine and kind, and they answer
different questions:

- **`<kind>-<machine>-peers.md`** — how the newest measured Graphity version compares to
  JGraphT, Guava and WebGraph. Start here.
- **`<kind>-<machine>-history.md`** — how the Graphity versions compare to each other, one
  column per measured release. This is where a regression between two releases shows up.

Both carry the same chapters:

- **§1 Performance** — Times (ms/op, lower is better) and Throughput (op/s, higher is
  better) tables per algorithm × n-value, with bar-chart SVGs
- **§2 Memory** — retained heap footprint in bytes (synthetic only)
- **§3 Allocations** — bytes allocated per algorithm invocation (synthetic only)

Percentages and factors are relative to the newest Graphity version in both views. Peer
columns and reordered columns state the version whose run they come from, because a peer is
not re-measured for every Graphity release.

## What is in `versions/`

One file per `(Graphity version, kind, machine)` combination.  These are the raw per-run
source files from which `summary/` is generated.  They accumulate over time — adding a
new version's measurements does not overwrite older data.

File naming: `<version>-<kind>-<machine>.md`

Their section numbers are a running index over the algorithms that had data in that
run, so the same algorithm can carry different numbers in different files (`Condense`
entered the matrix after 2.0.0 and pushed `TopOrder` from 1.5 to 1.6). Both summary views
match sections by algorithm and node count and assign their own numbering, so the
numbers in `versions/` are never authoritative.

## Adding new measurements

```bash
cd graphity-benchmarks
./run-benchmarks.sh clean          # mandatory: measurements/ is also the resume cache
./run-benchmarks.sh bench          # measures everything and runs report at the end
```

`bench` takes an optional version (`bench 2.2.0`); without it the version comes from
`pom.xml`. Running `synthetic`/`realworld` individually requires a separate `report`
call afterwards.

To regenerate `summary/` from existing `versions/` without new measurements:

```bash
./run-benchmarks.sh report         # falls back to regenerate-only when measurements/ is empty
```

Or call the generator directly:

```bash
python3 times-to-ops.py            # reads reports/versions/, writes both views to reports/summary/
```

Either way both views and both `fig/` directories are rewritten, so they cannot drift apart.
After changing the generator, run its tests:

```bash
python3 test-times-to-ops.py
```
