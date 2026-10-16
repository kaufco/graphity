# Graphity vs. JGraphT, Guava and WebGraph — realworld / ubuntu-ryzen-9950x

Graphity `2.3.0` against the peer libraries. Peer numbers come from the newest run that measured them, which need not be the run of the newest Graphity version; every column states the version it belongs to. Blocks without a peer column are Graphity-only benchmarks or graph sizes beyond the peers' reach — for the comparison across Graphity versions see [`realworld-ubuntu-ryzen-9950x-history.md`](realworld-ubuntu-ryzen-9950x-history.md).

```
Profile:      realworld
Timestamp:    2026-10-16 04:51:31
JDK:          OpenJDK 64-Bit Server VM 25.0.2 (Amazon.com Inc.)
OS:           Ubuntu 25.10, kernel 6.17.0-41-generic (amd64)
CPU:          AMD Ryzen 9 9950X 16-Core Processor (32 logical cores)
RAM:          89.7 GiB
Estimate:     near-best window - 3rd-fastest of 15 or more, 2nd-fastest of 5 or more
Revision:     303d801d508b + uncommitted changes b4b1cd1fe72e "Prepare 2.3.0 release"
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

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.3.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 SCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 45.19 ms | 20.56 ms (0.5×) | 1266.8 ms (28×) | 799.0 ms (18×) | 234.3 ms (5.2×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.57 ms | 34.39 ms (0.6×) | 1391.8 ms (23×) | 1466.2 ms (24×) | 153.2 ms (2.5×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 993.9 ms | 287.2 ms (0.3×) | 22122 ms (22×) | 16631 ms (17×) | 3864.4 ms (3.9×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 91.56 ms | 87.57 ms (1.0×) | 6075.4 ms (66×) | 2804.5 ms (31×) | 450.9 ms (4.9×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.620 ms | 1.150 ms (0.7×) | 43.35 ms (27×) | 28.88 ms (18×) | 12.19 ms (7.5×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 22.13 op/s | 48.63 op/s (220%) | 0.789 op/s (3.6%) | 1.252 op/s (5.7%) | 4.268 op/s (19.3%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.24 op/s | 29.08 op/s (179%) | 0.718 op/s (4.4%) | 0.682 op/s (4.2%) | 6.527 op/s (40.2%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.006 op/s | 3.482 op/s (346%) | 0.045 op/s (4.5%) | 0.060 op/s (6.0%) | 0.259 op/s (25.7%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 10.92 op/s | 11.42 op/s (105%) | 0.165 op/s (1.5%) | 0.357 op/s (3.3%) | 2.218 op/s (20.3%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 617.3 op/s | 869.6 op/s (141%) | 23.07 op/s (3.7%) | 34.63 op/s (5.6%) | 82.06 op/s (13.3%) |

![1.1 SCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-1-scc-real-world-graphs.svg)

### 1.2 WCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 17.17 ms | 1132.2 ms (66×) | 661.3 ms (39×) | 352.0 ms (20×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 19.70 ms | 1307.4 ms (66×) | 1147.6 ms (58×) | 535.5 ms (27×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 242.3 ms | 16029 ms (66×) | 14360 ms (59×) | 4147.3 ms (17×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 66.03 ms | 8079.4 ms (122×) | 5134.9 ms (78×) | 1456.5 ms (22×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.082 ms | 48.33 ms (45×) | 36.85 ms (34×) | 28.91 ms (27×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 58.24 op/s | 0.883 op/s (1.5%) | 1.512 op/s (2.6%) | 2.841 op/s (4.9%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 50.77 op/s | 0.765 op/s (1.5%) | 0.871 op/s (1.7%) | 1.867 op/s (3.7%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 4.127 op/s | 0.062 op/s (1.5%) | 0.070 op/s (1.7%) | 0.241 op/s (5.8%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 15.15 op/s | 0.124 op/s (0.8%) | 0.195 op/s (1.3%) | 0.687 op/s (4.5%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 924.2 op/s | 20.69 op/s (2.2%) | 27.13 op/s (2.9%) | 34.59 op/s (3.7%) |

![1.2 WCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-2-wcc-real-world-graphs.svg)

### 1.3 TopOrderAndLevels — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 227.8 ms | 222.0 ms (1.0×) | 1313.5 ms (5.8×) | 2019.4 ms (8.9×) | 956.0 ms (4.2×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 4.390 op/s | 4.505 op/s (103%) | 0.761 op/s (17.3%) | 0.495 op/s (11.3%) | 1.046 op/s (23.8%) |

![1.3 TopOrderAndLevels — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-3-toporderandlevels-real-world-graphs.svg)

Self-loops are dropped for this algorithm: it requires a DAG, and a self-loop is a cycle.
Every library in a row reads that same graph; the other tables measure each file unmodified.

For cit-Patents that is one edge of 16,518,948 — patent 5489070 cites itself, and that loop is the graph's only cycle.

### 1.4 BackEdges — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 41.03 ms | 21.94 ms (0.5×) | 718.3 ms (18×) | 760.2 ms (19×) | 648.3 ms (16×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 33.18 ms | 18.98 ms (0.6×) | 1628.6 ms (49×) | 729.0 ms (22×) | 1934.7 ms (58×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 836.3 ms | 281.4 ms (0.3×) | 9517.0 ms (11×) | 11079 ms (13×) | 10269 ms (12×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 45.36 ms | 42.56 ms (0.9×) | 26972 ms (595×) | 15728 ms (347×) | 15947 ms (352×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.923 ms | 0.774 ms (0.8×) | 20.36 ms (22×) | 25.09 ms (27×) | 12.73 ms (14×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 24.37 op/s | 45.57 op/s (187%) | 1.392 op/s (5.7%) | 1.315 op/s (5.4%) | 1.542 op/s (6.3%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 30.14 op/s | 52.70 op/s (175%) | 0.614 op/s (2.0%) | 1.372 op/s (4.6%) | 0.517 op/s (1.7%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.196 op/s | 3.554 op/s (297%) | 0.105 op/s (8.8%) | 0.090 op/s (7.5%) | 0.097 op/s (8.1%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 22.05 op/s | 23.50 op/s (107%) | 0.037 op/s (0.2%) | 0.064 op/s (0.3%) | 0.063 op/s (0.3%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 083 op/s | 1 292 op/s (119%) | 49.11 op/s (4.5%) | 39.85 op/s (3.7%) | 78.55 op/s (7.3%) |

![1.4 BackEdges — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-4-backedges-real-world-graphs.svg)

### 1.5 EdgeTypes — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 34.55 ms | 18.47 ms (0.5×) | 739.5 ms (21×) | 865.5 ms (25×) | 684.3 ms (20×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 37.63 ms | 19.78 ms (0.5×) | 2116.9 ms (56×) | 1057.6 ms (28×) | 2075.3 ms (55×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 882.1 ms | 248.4 ms (0.3×) | 10176 ms (12×) | 16251 ms (18×) | 10715 ms (12×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 67.67 ms | 65.93 ms (1.0×) | 27285 ms (403×) | 20626 ms (305×) | 17712 ms (262×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.218 ms | 0.943 ms (0.8×) | 20.42 ms (17×) | 23.96 ms (20×) | 14.59 ms (12×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.3.0 | Graphity 2.3.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 28.95 op/s | 54.15 op/s (187%) | 1.352 op/s (4.7%) | 1.155 op/s (4.0%) | 1.461 op/s (5.0%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 26.57 op/s | 50.56 op/s (190%) | 0.472 op/s (1.8%) | 0.946 op/s (3.6%) | 0.482 op/s (1.8%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.134 op/s | 4.026 op/s (355%) | 0.098 op/s (8.7%) | 0.062 op/s (5.4%) | 0.093 op/s (8.2%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 14.78 op/s | 15.17 op/s (103%) | 0.037 op/s (0.2%) | 0.048 op/s (0.3%) | 0.056 op/s (0.4%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 821.0 op/s | 1 060 op/s (129%) | 48.96 op/s (6.0%) | 41.73 op/s (5.1%) | 68.54 op/s (8.3%) |

![1.5 EdgeTypes — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-5-edgetypes-real-world-graphs.svg)

### 1.6 Reorder (DFS-preorder) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.3.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 54.63 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 73.47 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1305.9 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 107.0 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 2.843 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.3.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 18.31 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 13.61 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.766 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.346 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 351.7 op/s |

### 1.7 BiAdjacency (in-adjacency) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.3.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 15.33 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 17.13 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 381.5 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 108.0 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.557 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.3.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 65.24 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 58.37 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 2.621 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.259 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 795 op/s |

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
