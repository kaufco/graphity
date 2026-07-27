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
│   └── summary/               ← combined per-machine reports (THE files to look at)
│       ├── synthetic-ubuntu-ryzen-9950x.md   ✓ open this one
│       ├── synthetic-macos-m5.md           ✓ or this one
│       ├── realworld-ubuntu-ryzen-9950x.md    ✓ or this one
│       └── fig/
│           ├── synthetic-ubuntu-ryzen-9950x/
│           │   └── *.svg
│           └── realworld-ubuntu-ryzen-9950x/
│               └── *.svg
│
├── benchmark-matrix/          ← benchmark profile configs (JSON)
├── times-to-ops.py            ← generator script (versions/ → summary/)
└── run-benchmarks.sh          ← orchestrator
```

## What to look at

Open a file from **`reports/summary/`**. Each summary file combines all available
Graphity versions and peer libraries for one machine into a single document:

- **§1 Performance** — Times (ms/op, lower is better) and Throughput (op/s, higher is
  better) tables per algorithm × n-value, with bar-chart SVGs
- **§2 Memory** — retained heap footprint in bytes (synthetic only)
- **§3 Allocations** — bytes allocated per algorithm invocation (synthetic only)

## What is in `versions/`

One file per `(Graphity version, kind, machine)` combination.  These are the raw per-run
source files from which `summary/` is generated.  They accumulate over time — adding a
new version's measurements does not overwrite older data.

File naming: `<version>-<kind>-<machine>.md`

## Adding new measurements

```bash
cd graphity-benchmarks
./run-benchmarks.sh clean
./run-benchmarks.sh bench          # or individual: synthetic/realworld
./run-benchmarks.sh report         # version from pom.xml; promotes + regenerates summary
```

To regenerate `summary/` from existing `versions/` without new measurements:

```bash
./run-benchmarks.sh report         # falls back to regenerate-only when measurements/ is empty
```

Or call the generator directly:

```bash
python3 times-to-ops.py            # reads reports/versions/, writes reports/summary/
```
