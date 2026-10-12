# Graphity vs. JGraphT, Guava and WebGraph — realworld / ubuntu-ryzen-9950x

Graphity `2.2.0` against the peer libraries. Peer numbers come from the newest run that measured them, which need not be the run of the newest Graphity version; every column states the version it belongs to. Blocks without a peer column are Graphity-only benchmarks or graph sizes beyond the peers' reach — for the comparison across Graphity versions see [`realworld-ubuntu-ryzen-9950x-history.md`](realworld-ubuntu-ryzen-9950x-history.md).

```
Profile:      realworld
Timestamp:    2026-10-11 14:31:16
JDK:          OpenJDK 64-Bit Server VM 25.0.2 (Amazon.com Inc.)
OS:           Ubuntu 25.10, kernel 6.17.0-41-generic (amd64)
CPU:          AMD Ryzen 9 9950X 16-Core Processor (32 logical cores)
RAM:          89.7 GiB
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

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.2.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 SCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 44.45 ms | 20.51 ms (0.5×) | 1325.0 ms (30×) | 807.0 ms (18×) | 239.3 ms (5.4×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 62.68 ms | 35.38 ms (0.6×) | 1572.4 ms (25×) | 1591.0 ms (25×) | 153.0 ms (2.4×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1063.4 ms | 297.1 ms (0.3×) | 24532 ms (23×) | 17416 ms (16×) | 3949.5 ms (3.7×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 106.4 ms | 94.13 ms (0.9×) | 6056.1 ms (57×) | 4300.6 ms (40×) | 471.0 ms (4.4×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.602 ms | 1.149 ms (0.7×) | 44.36 ms (28×) | 29.00 ms (18×) | 12.23 ms (7.6×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 22.50 op/s | 48.77 op/s (217%) | 0.755 op/s (3.4%) | 1.239 op/s (5.5%) | 4.179 op/s (18.6%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 15.95 op/s | 28.27 op/s (177%) | 0.636 op/s (4.0%) | 0.629 op/s (3.9%) | 6.536 op/s (41.0%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.940 op/s | 3.366 op/s (358%) | 0.041 op/s (4.3%) | 0.057 op/s (6.1%) | 0.253 op/s (26.9%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.398 op/s | 10.62 op/s (113%) | 0.165 op/s (1.8%) | 0.233 op/s (2.5%) | 2.123 op/s (22.6%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 624.2 op/s | 870.3 op/s (139%) | 22.54 op/s (3.6%) | 34.49 op/s (5.5%) | 81.79 op/s (13.1%) |

![1.1 SCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-1-scc-real-world-graphs.svg)

### 1.2 WCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 17.69 ms | 1158.3 ms (65×) | 730.8 ms (41×) | 368.3 ms (21×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 20.29 ms | 1356.9 ms (67×) | 1251.2 ms (62×) | 535.5 ms (26×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 258.1 ms | 18006 ms (70×) | 14852 ms (58×) | 4435.2 ms (17×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 65.57 ms | 7915.9 ms (121×) | 5902.2 ms (90×) | 1583.7 ms (24×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.052 ms | 55.33 ms (53×) | 34.01 ms (32×) | 28.18 ms (27×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 56.52 op/s | 0.863 op/s (1.5%) | 1.368 op/s (2.4%) | 2.715 op/s (4.8%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 49.29 op/s | 0.737 op/s (1.5%) | 0.799 op/s (1.6%) | 1.867 op/s (3.8%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 3.874 op/s | 0.056 op/s (1.4%) | 0.067 op/s (1.7%) | 0.225 op/s (5.8%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 15.25 op/s | 0.126 op/s (0.8%) | 0.169 op/s (1.1%) | 0.631 op/s (4.1%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 950.6 op/s | 18.07 op/s (1.9%) | 29.41 op/s (3.1%) | 35.48 op/s (3.7%) |

![1.2 WCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-2-wcc-real-world-graphs.svg)

### 1.3 TopOrderAndLevels — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 231.1 ms | 226.4 ms (1.0×) | 1612.4 ms (7.0×) | 2819.7 ms (12×) | 1045.5 ms (4.5×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 4.327 op/s | 4.417 op/s (102%) | 0.620 op/s (14.3%) | 0.355 op/s (8.2%) | 0.956 op/s (22.1%) |

![1.3 TopOrderAndLevels — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-3-toporderandlevels-real-world-graphs.svg)

Self-loops are dropped for this algorithm: it requires a DAG, and a self-loop is a cycle.
Every library in a row reads that same graph; the other tables measure each file unmodified.

For cit-Patents that is one edge of 16,518,948 — patent 5489070 cites itself, and that loop is the graph's only cycle.

### 1.4 BackEdges — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 38.81 ms | 22.03 ms (0.6×) | 711.4 ms (18×) | 1031.8 ms (27×) | 661.9 ms (17×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 28.86 ms | 17.73 ms (0.6×) | 1710.7 ms (59×) | 753.3 ms (26×) | 1969.9 ms (68×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 851.8 ms | 280.3 ms (0.3×) | 9981.6 ms (12×) | 11201 ms (13×) | 13894 ms (16×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 41.24 ms | 43.52 ms (1.1×) | 15542 ms (377×) | 16800 ms (407×) | 15732 ms (381×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.902 ms | 0.751 ms (0.8×) | 11.80 ms (13×) | 23.77 ms (26×) | 12.95 ms (14×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 25.77 op/s | 45.38 op/s (176%) | 1.406 op/s (5.5%) | 0.969 op/s (3.8%) | 1.511 op/s (5.9%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 34.65 op/s | 56.40 op/s (163%) | 0.585 op/s (1.7%) | 1.327 op/s (3.8%) | 0.508 op/s (1.5%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.174 op/s | 3.568 op/s (304%) | 0.100 op/s (8.5%) | 0.089 op/s (7.6%) | 0.072 op/s (6.1%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 24.25 op/s | 22.98 op/s (94.8%) | 0.064 op/s (0.3%) | 0.060 op/s (0.2%) | 0.064 op/s (0.3%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 109 op/s | 1 332 op/s (120%) | 84.74 op/s (7.6%) | 42.08 op/s (3.8%) | 77.21 op/s (7.0%) |

![1.4 BackEdges — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-4-backedges-real-world-graphs.svg)

### 1.5 EdgeTypes — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 31.89 ms | 18.39 ms (0.6×) | 829.3 ms (26×) | 811.8 ms (25×) | 692.4 ms (22×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 34.82 ms | 20.50 ms (0.6×) | 2120.5 ms (61×) | 1043.5 ms (30×) | 2133.4 ms (61×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 894.1 ms | 247.4 ms (0.3×) | 10805 ms (12×) | 13086 ms (15×) | 10902 ms (12×) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 66.14 ms | 62.51 ms (0.9×) | 15819 ms (239×) | 17396 ms (263×) | 27296 ms (413×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.202 ms | 0.956 ms (0.8×) | 19.09 ms (16×) | 21.07 ms (18×) | 14.15 ms (12×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 31.36 op/s | 54.37 op/s (173%) | 1.206 op/s (3.8%) | 1.232 op/s (3.9%) | 1.444 op/s (4.6%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 28.72 op/s | 48.79 op/s (170%) | 0.472 op/s (1.6%) | 0.958 op/s (3.3%) | 0.469 op/s (1.6%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.118 op/s | 4.042 op/s (361%) | 0.093 op/s (8.3%) | 0.076 op/s (6.8%) | 0.092 op/s (8.2%) |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 15.12 op/s | 16.00 op/s (106%) | 0.063 op/s (0.4%) | 0.057 op/s (0.4%) | 0.037 op/s (0.2%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 831.9 op/s | 1 046 op/s (126%) | 52.40 op/s (6.3%) | 47.47 op/s (5.7%) | 70.67 op/s (8.5%) |

![1.5 EdgeTypes — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-peers/1-5-edgetypes-real-world-graphs.svg)

### 1.6 Reorder (DFS-preorder) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.2.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 50.77 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 65.97 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1388.8 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 115.9 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 2.844 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.2.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 19.70 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 15.16 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.720 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 8.628 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 351.6 op/s |

### 1.7 BiAdjacency (in-adjacency) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.2.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 14.53 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.25 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 408.0 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 106.4 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.558 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.2.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 68.82 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.54 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 2.451 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.398 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 792 op/s |

### 1.8 Symmetrize (WebGraph) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | WebGraph |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 688.1 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 825.9 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 7389.8 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 2165.5 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 149.7 ms |

#### Throughput (op/s, higher is better)

| Dataset | WebGraph |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 1.453 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 1.211 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.135 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 0.462 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 6.680 op/s |
