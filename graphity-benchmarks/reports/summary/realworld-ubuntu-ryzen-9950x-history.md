# Graphity Version History — realworld / ubuntu-ryzen-9950x

Benchmarked versions: `2.0.0`, `2.1.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`realworld-ubuntu-ryzen-9950x-peers.md`](realworld-ubuntu-ryzen-9950x-peers.md).

```
Profile:      realworld
Timestamp:    2026-08-17 01:30:48
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

## 1 Performance

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.1.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 SCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 92.50 ms (1.0×) | 88.53 ms | 86.90 ms (1.0×) | 89.83 ms (1.0×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.601 ms (1.0×) | 1.612 ms | 1.156 ms (0.7×) | 1.137 ms (0.7×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 41.54 ms (1.0×) | 40.75 ms | 20.55 ms (0.5×) | 20.38 ms (0.5×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.01 ms (1.0×) | 60.01 ms | 34.95 ms (0.6×) | 34.66 ms (0.6×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1043.8 ms (1.0×) | 1017.2 ms | 292.3 ms (0.3×) | 296.3 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 10.81 op/s (95.7%) | 11.30 op/s | 11.51 op/s (102%) | 11.13 op/s (98.6%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 624.6 op/s (101%) | 620.3 op/s | 865.1 op/s (139%) | 879.5 op/s (142%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 24.07 op/s (98.1%) | 24.54 op/s | 48.65 op/s (198%) | 49.07 op/s (200%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.39 op/s (98.4%) | 16.66 op/s | 28.61 op/s (172%) | 28.85 op/s (173%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.958 op/s (97.5%) | 0.983 op/s | 3.421 op/s (348%) | 3.375 op/s (343%) |

![1.1 SCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-1-scc-real-world-graphs.svg)

### 1.2 WCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 17.36 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 18.86 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 246.1 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 65.73 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.024 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 57.60 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 53.01 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 4.063 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 15.21 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 976.6 op/s |

### 1.3 TopOrderAndLevels — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 218.1 ms | 217.4 ms (1.0×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 4.585 op/s | 4.600 op/s (100%) |

![1.3 TopOrderAndLevels — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-3-toporderandlevels-real-world-graphs.svg)

Self-loops are dropped for this algorithm: it requires a DAG, and a self-loop is a cycle.
Every library in a row reads that same graph; the other tables measure each file unmodified.

For cit-Patents that is one edge of 16,518,948 — patent 5489070 cites itself, and that loop is the graph's only cycle.

### 1.4 BackEdges — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 42.84 ms (1.1×) | 40.55 ms | 43.32 ms (1.1×) | 40.70 ms (1.0×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.916 ms (1.0×) | 0.903 ms | 0.773 ms (0.9×) | 0.752 ms (0.8×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 38.04 ms (1.0×) | 37.93 ms | 22.52 ms (0.6×) | 22.17 ms (0.6×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 31.30 ms (1.0×) | 30.56 ms | 19.88 ms (0.7×) | 17.82 ms (0.6×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 865.3 ms (1.0×) | 835.5 ms | 279.3 ms (0.3×) | 279.9 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 23.34 op/s (94.6%) | 24.66 op/s | 23.08 op/s (93.6%) | 24.57 op/s (99.6%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 092 op/s (98.6%) | 1 107 op/s | 1 294 op/s (117%) | 1 330 op/s (120%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 26.29 op/s (99.7%) | 26.36 op/s | 44.41 op/s (168%) | 45.10 op/s (171%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 31.95 op/s (97.7%) | 32.72 op/s | 50.29 op/s (154%) | 56.13 op/s (172%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.156 op/s (96.6%) | 1.197 op/s | 3.580 op/s (299%) | 3.573 op/s (298%) |

![1.4 BackEdges — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-4-backedges-real-world-graphs.svg)

### 1.5 EdgeTypes — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 66.97 ms (1.0×) | 64.11 ms | 65.10 ms (1.0×) | 67.50 ms (1.1×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.207 ms (1.0×) | 1.235 ms | 0.960 ms (0.8×) | 0.951 ms (0.8×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 30.93 ms (1.0×) | 30.89 ms | 18.91 ms (0.6×) | 18.59 ms (0.6×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 34.31 ms (1.0×) | 34.14 ms | 20.44 ms (0.6×) | 20.29 ms (0.6×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 885.6 ms (1.0×) | 879.6 ms | 250.2 ms (0.3×) | 243.9 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 14.93 op/s (95.7%) | 15.60 op/s | 15.36 op/s (98.5%) | 14.82 op/s (95.0%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 828.5 op/s (102%) | 809.7 op/s | 1 042 op/s (129%) | 1 052 op/s (130%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 32.33 op/s (99.9%) | 32.37 op/s | 52.89 op/s (163%) | 53.79 op/s (166%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 29.15 op/s (99.5%) | 29.29 op/s | 48.92 op/s (167%) | 49.29 op/s (168%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.129 op/s (99.3%) | 1.137 op/s | 3.997 op/s (352%) | 4.100 op/s (361%) |

![1.5 EdgeTypes — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-5-edgetypes-real-world-graphs.svg)

### 1.6 Reorder (DFS-preorder) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 151.0 ms (1.4×) | 108.9 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 3.348 ms (1.2×) | 2.854 ms |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 61.88 ms (1.2×) | 50.07 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 79.96 ms (1.2×) | 66.02 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1850.1 ms (1.4×) | 1335.1 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 6.623 op/s (72.1%) | 9.183 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 298.7 op/s (85.2%) | 350.4 op/s |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 16.16 op/s (80.9%) | 19.97 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 12.51 op/s (82.6%) | 15.15 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.541 op/s (72.2%) | 0.749 op/s |

![1.6 Reorder (DFS-preorder) cost — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-6-reorder-dfs-preorder-cost-real-world-graphs.svg)

### 1.7 BiAdjacency (in-adjacency) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 14.62 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.17 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 406.0 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 106.9 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.558 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 |
|---|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 68.38 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.84 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 2.463 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.355 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 792 op/s |
