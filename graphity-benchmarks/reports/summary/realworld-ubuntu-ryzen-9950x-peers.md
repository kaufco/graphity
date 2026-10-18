# Graphity vs. JGraphT, Guava and WebGraph — realworld / ubuntu-ryzen-9950x

Graphity `2.4.0` against the peer libraries. Peer numbers come from the newest run that measured them, which need not be the run of the newest Graphity version; every column states the version it belongs to. Blocks without a peer column are Graphity-only benchmarks or graph sizes beyond the peers' reach — for the comparison across Graphity versions see [`realworld-ubuntu-ryzen-9950x-history.md`](realworld-ubuntu-ryzen-9950x-history.md).

```
Profile:      realworld
Timestamp:    2026-10-17 01:17:42
JDK:          OpenJDK 64-Bit Server VM 25.0.2 (Amazon.com Inc.)
OS:           Ubuntu 25.10, kernel 6.17.0-41-generic (amd64)
CPU:          AMD Ryzen 9 9950X 16-Core Processor (32 logical cores)
RAM:          89.7 GiB
Estimate:     near-best window - 3rd-fastest of 15 or more, 2nd-fastest of 5 or more
Revisions:    2, the report combines runs of different states
              303d801d508b + uncommitted changes b4b1cd1fe72e "Prepare 2.3.0 release" (68 cases)
              270157fce154 "Changelog and Javadoc: current issue titles, SSA and phi explained at first use" (47 cases)
```

## Contents

- [1 Performance](#1-performance)
  - [1.1 SCC — real-world graphs](#11-scc-real-world-graphs)
  - [1.2 WCC — real-world graphs](#12-wcc-real-world-graphs)
  - [1.3 TopOrderAndLevels — real-world graphs](#13-toporderandlevels-real-world-graphs)
  - [1.4 BackEdges — real-world graphs](#14-backedges-real-world-graphs)
  - [1.5 EdgeTypes — real-world graphs](#15-edgetypes-real-world-graphs)
  - [1.6 Reorder (DFS-preorder) cost — real-world graphs](#16-reorder-dfs-preorder-cost-real-world-graphs)
  - [1.7 BiAdjacency (in-adjacency) cost — real-world graphs](#17-biadjacency-in-adjacency-cost-real-world-graphs)
  - [1.8 Symmetrize (WebGraph) cost — real-world graphs](#18-symmetrize-webgraph-cost-real-world-graphs)

## 1 Performance

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.4.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 SCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 57.55 ms | 22.96 ms (0.4×) | 1266.8 ms (22×) | 799.0 ms (14×) | 234.3 ms (4.1×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 74.37 ms | 39.88 ms (0.5×) | 1391.8 ms (19×) | 1466.2 ms (20×) | 153.2 ms (2.1×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1073.8 ms | 324.4 ms (0.3×) | 22122 ms (21×) | 16631 ms (15×) | 3864.4 ms (3.6×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 125.2 ms | 126.3 ms (1.0×) | 6075.4 ms (49×) | 2804.5 ms (22×) | 450.9 ms (3.6×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.678 ms | 1.147 ms (0.7×) | 43.35 ms (26×) | 28.88 ms (17×) | 12.19 ms (7.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 17.38 op/s | 43.56 op/s (251%) | 0.789 op/s (4.5%) | 1.252 op/s (7.2%) | 4.268 op/s (24.6%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 13.45 op/s | 25.08 op/s (187%) | 0.718 op/s (5.3%) | 0.682 op/s (5.1%) | 6.527 op/s (48.5%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.931 op/s | 3.083 op/s (331%) | 0.045 op/s (4.9%) | 0.060 op/s (6.5%) | 0.259 op/s (27.8%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 7.987 op/s | 7.918 op/s (99.1%) | 0.165 op/s (2.1%) | 0.357 op/s (4.5%) | 2.218 op/s (27.8%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 595.9 op/s | 871.8 op/s (146%) | 23.07 op/s (3.9%) | 34.63 op/s (5.8%) | 82.06 op/s (13.8%) |

![1.1 SCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-1-scc-real-world-graphs.svg)

### 1.2 WCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 21.54 ms | 1132.2 ms (53×) | 661.3 ms (31×) | 352.0 ms (16×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 25.28 ms | 1307.4 ms (52×) | 1147.6 ms (45×) | 535.5 ms (21×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 257.6 ms | 16029 ms (62×) | 14360 ms (56×) | 4147.3 ms (16×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 69.86 ms | 8079.4 ms (116×) | 5134.9 ms (74×) | 1456.5 ms (21×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.101 ms | 48.33 ms (44×) | 36.85 ms (33×) | 28.91 ms (26×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 46.44 op/s | 0.883 op/s (1.9%) | 1.512 op/s (3.3%) | 2.841 op/s (6.1%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 39.55 op/s | 0.765 op/s (1.9%) | 0.871 op/s (2.2%) | 1.867 op/s (4.7%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 3.882 op/s | 0.062 op/s (1.6%) | 0.070 op/s (1.8%) | 0.241 op/s (6.2%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 14.31 op/s | 0.124 op/s (0.9%) | 0.195 op/s (1.4%) | 0.687 op/s (4.8%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 908.3 op/s | 20.69 op/s (2.3%) | 27.13 op/s (3.0%) | 34.59 op/s (3.8%) |

![1.2 WCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-2-wcc-real-world-graphs.svg)

### 1.3 TopOrderAndLevels — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 239.6 ms | 236.1 ms (1.0×) | 1313.5 ms (5.5×) | 2019.4 ms (8.4×) | 956.0 ms (4.0×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 4.174 op/s | 4.235 op/s (101%) | 0.761 op/s (18.2%) | 0.495 op/s (11.9%) | 1.046 op/s (25.1%) |

![1.3 TopOrderAndLevels — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-3-toporderandlevels-real-world-graphs.svg)

Self-loops are dropped for this algorithm: it requires a DAG, and a self-loop is a cycle.
Every library in a row reads that same graph; the other tables measure each file unmodified.

For cit-Patents that is one edge of 16,518,948 — patent 5489070 cites itself, and that loop is the graph's only cycle.

### 1.4 BackEdges — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 45.98 ms | 23.57 ms (0.5×) | 718.3 ms (16×) | 760.2 ms (17×) | 648.3 ms (14×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 38.51 ms | 20.78 ms (0.5×) | 1628.6 ms (42×) | 729.0 ms (19×) | 1934.7 ms (50×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 845.5 ms | 284.0 ms (0.3×) | 9517.0 ms (11×) | 11079 ms (13×) | 10269 ms (12×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 42.62 ms | 43.63 ms (1.0×) | 26972 ms (633×) | 15728 ms (369×) | 15947 ms (374×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.953 ms | 0.770 ms (0.8×) | 20.36 ms (21×) | 25.09 ms (26×) | 12.73 ms (13×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 21.75 op/s | 42.42 op/s (195%) | 1.392 op/s (6.4%) | 1.315 op/s (6.0%) | 1.542 op/s (7.1%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 25.97 op/s | 48.12 op/s (185%) | 0.614 op/s (2.4%) | 1.372 op/s (5.3%) | 0.517 op/s (2.0%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.183 op/s | 3.521 op/s (298%) | 0.105 op/s (8.9%) | 0.090 op/s (7.6%) | 0.097 op/s (8.2%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 23.46 op/s | 22.92 op/s (97.7%) | 0.037 op/s (0.2%) | 0.064 op/s (0.3%) | 0.063 op/s (0.3%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 049 op/s | 1 299 op/s (124%) | 49.11 op/s (4.7%) | 39.85 op/s (3.8%) | 78.55 op/s (7.5%) |

![1.4 BackEdges — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-4-backedges-real-world-graphs.svg)

### 1.5 EdgeTypes — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 41.42 ms | 20.39 ms (0.5×) | 739.5 ms (18×) | 865.5 ms (21×) | 684.3 ms (17×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 41.47 ms | 22.00 ms (0.5×) | 2116.9 ms (51×) | 1057.6 ms (26×) | 2075.3 ms (50×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 887.3 ms | 251.9 ms (0.3×) | 10176 ms (11×) | 16251 ms (18×) | 10715 ms (12×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 73.77 ms | 73.40 ms (1.0×) | 27285 ms (370×) | 20626 ms (280×) | 17712 ms (240×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.202 ms | 0.972 ms (0.8×) | 20.42 ms (17×) | 23.96 ms (20×) | 14.59 ms (12×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 24.14 op/s | 49.04 op/s (203%) | 1.352 op/s (5.6%) | 1.155 op/s (4.8%) | 1.461 op/s (6.1%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 24.11 op/s | 45.45 op/s (188%) | 0.472 op/s (2.0%) | 0.946 op/s (3.9%) | 0.482 op/s (2.0%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.127 op/s | 3.970 op/s (352%) | 0.098 op/s (8.7%) | 0.062 op/s (5.5%) | 0.093 op/s (8.3%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 13.56 op/s | 13.62 op/s (101%) | 0.037 op/s (0.3%) | 0.048 op/s (0.4%) | 0.056 op/s (0.4%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 831.9 op/s | 1 029 op/s (124%) | 48.96 op/s (5.9%) | 41.73 op/s (5.0%) | 68.54 op/s (8.2%) |

![1.5 EdgeTypes — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-5-edgetypes-real-world-graphs.svg)

### 1.6 Reorder (DFS-preorder) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.4.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 63.50 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 79.13 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1328.1 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 106.0 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 2.905 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.4.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 15.75 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 12.64 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.753 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.434 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 344.2 op/s |

### 1.7 BiAdjacency (in-adjacency) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.4.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 18.22 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 20.35 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 391.8 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 122.7 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.581 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.4.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 54.89 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 49.14 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 2.552 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 8.150 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 721 op/s |

### 1.8 Symmetrize (WebGraph) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | WebGraph |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 720.8 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 923.3 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 9019.1 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 2307.9 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 150.5 ms |

#### Throughput (op/s, higher is better)

| Dataset | WebGraph |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 1.387 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 1.083 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.111 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 0.433 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 6.645 op/s |
