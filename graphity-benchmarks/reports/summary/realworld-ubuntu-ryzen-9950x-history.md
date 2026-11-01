# Graphity Version History — realworld / ubuntu-ryzen-9950x

Benchmarked versions: `2.0.0`, `2.1.0`, `2.2.0`, `2.3.0`, `2.4.0`, `3.0.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`realworld-ubuntu-ryzen-9950x-peers.md`](realworld-ubuntu-ryzen-9950x-peers.md).

Versions `2.0.0`, `2.1.0`, `2.2.0` report the mean over all measurement windows; the later ones a near-best window (`Estimate` below), which comes out a few percent faster for unchanged code.

```
Profile:      realworld
Timestamp:    2026-10-31 05:43:55
JDK:          OpenJDK 64-Bit Server VM 25.0.2 (Amazon.com Inc.)
OS:           Ubuntu 25.10, kernel 6.17.0-41-generic (amd64)
CPU:          AMD Ryzen 9 9950X 16-Core Processor (32 logical cores)
RAM:          89.7 GiB
Estimate:     near-best window - 3rd-fastest of 15 or more, 2nd-fastest of 5 or more
Revision:     3.0.0
```

## Contents

- [1 Performance](#1-performance)
  - [1.1 SCC — real-world graphs](#11-scc--real-world-graphs)
  - [1.2 WCC — real-world graphs](#12-wcc--real-world-graphs)
  - [1.3 TopOrderAndLevels — real-world graphs](#13-toporderandlevels--real-world-graphs)
  - [1.4 BackEdges — real-world graphs](#14-backedges--real-world-graphs)
  - [1.5 EdgeTypes — real-world graphs](#15-edgetypes--real-world-graphs)
  - [1.6 Reorder (DFS-preorder) cost — real-world graphs](#16-reorder-dfs-preorder-cost--real-world-graphs)
  - [1.7 BiAdjacency (in-adjacency) cost — real-world graphs](#17-biadjacency-in-adjacency-cost--real-world-graphs)

## 1 Performance

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 3.0.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 SCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 3.0.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 92.50 ms (1.0×) | 88.53 ms (1.0×) | 106.4 ms (1.1×) | 91.56 ms (1.0×) | 125.2 ms (1.3×) | 92.95 ms | 86.90 ms (0.9×) | 89.83 ms (1.0×) | 94.13 ms (1.0×) | 87.57 ms (0.9×) | 126.3 ms (1.4×) | 91.46 ms (1.0×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.601 ms (1.0×) | 1.612 ms (1.0×) | 1.602 ms (1.0×) | 1.620 ms (1.0×) | 1.678 ms (1.0×) | 1.618 ms | 1.156 ms (0.7×) | 1.137 ms (0.7×) | 1.149 ms (0.7×) | 1.150 ms (0.7×) | 1.147 ms (0.7×) | 1.141 ms (0.7×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 41.54 ms (1.0×) | 40.75 ms (1.0×) | 44.45 ms (1.1×) | 45.19 ms (1.1×) | 57.55 ms (1.4×) | 42.05 ms | 20.55 ms (0.5×) | 20.38 ms (0.5×) | 20.51 ms (0.5×) | 20.56 ms (0.5×) | 22.96 ms (0.5×) | 20.39 ms (0.5×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.01 ms (1.0×) | 60.01 ms (1.0×) | 62.68 ms (1.0×) | 61.57 ms (1.0×) | 74.37 ms (1.2×) | 62.74 ms | 34.95 ms (0.6×) | 34.66 ms (0.6×) | 35.38 ms (0.6×) | 34.39 ms (0.5×) | 39.88 ms (0.6×) | 35.82 ms (0.6×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1043.8 ms (1.0×) | 1017.2 ms (1.0×) | 1063.4 ms (1.0×) | 993.9 ms (0.9×) | 1073.8 ms (1.0×) | 1049.9 ms | 292.3 ms (0.3×) | 296.3 ms (0.3×) | 297.1 ms (0.3×) | 287.2 ms (0.3×) | 324.4 ms (0.3×) | 294.4 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 3.0.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 10.81 op/s (100%) | 11.30 op/s (105%) | 9.398 op/s (87.4%) | 10.92 op/s (102%) | 7.987 op/s (74.2%) | 10.76 op/s | 11.51 op/s (107%) | 11.13 op/s (103%) | 10.62 op/s (98.7%) | 11.42 op/s (106%) | 7.918 op/s (73.6%) | 10.93 op/s (102%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 624.6 op/s (101%) | 620.3 op/s (100%) | 624.2 op/s (101%) | 617.3 op/s (99.9%) | 595.9 op/s (96.4%) | 618.0 op/s | 865.1 op/s (140%) | 879.5 op/s (142%) | 870.3 op/s (141%) | 869.6 op/s (141%) | 871.8 op/s (141%) | 876.4 op/s (142%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 24.07 op/s (101%) | 24.54 op/s (103%) | 22.50 op/s (94.6%) | 22.13 op/s (93.1%) | 17.38 op/s (73.1%) | 23.78 op/s | 48.65 op/s (205%) | 49.07 op/s (206%) | 48.77 op/s (205%) | 48.63 op/s (205%) | 43.56 op/s (183%) | 49.05 op/s (206%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.39 op/s (103%) | 16.66 op/s (105%) | 15.95 op/s (100%) | 16.24 op/s (102%) | 13.45 op/s (84.4%) | 15.94 op/s | 28.61 op/s (180%) | 28.85 op/s (181%) | 28.27 op/s (177%) | 29.08 op/s (182%) | 25.08 op/s (157%) | 27.92 op/s (175%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.958 op/s (101%) | 0.983 op/s (103%) | 0.940 op/s (98.7%) | 1.006 op/s (106%) | 0.931 op/s (97.8%) | 0.952 op/s | 3.421 op/s (359%) | 3.375 op/s (354%) | 3.366 op/s (353%) | 3.482 op/s (366%) | 3.083 op/s (324%) | 3.397 op/s (357%) |

![1.1 SCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-1-scc-real-world-graphs.svg)

### 1.2 WCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 17.36 ms (1.0×) | 17.69 ms (1.0×) | 17.17 ms (1.0×) | 21.54 ms (1.3×) | 17.06 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 18.86 ms (1.0×) | 20.29 ms (1.1×) | 19.70 ms (1.0×) | 25.28 ms (1.3×) | 18.81 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 246.1 ms (1.1×) | 258.1 ms (1.1×) | 242.3 ms (1.1×) | 257.6 ms (1.1×) | 225.8 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 65.73 ms (1.1×) | 65.57 ms (1.1×) | 66.03 ms (1.1×) | 69.86 ms (1.2×) | 60.30 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.024 ms (1.1×) | 1.052 ms (1.1×) | 1.082 ms (1.1×) | 1.101 ms (1.2×) | 0.945 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 57.60 op/s (98.2%) | 56.52 op/s (96.4%) | 58.24 op/s (99.3%) | 46.44 op/s (79.2%) | 58.63 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 53.01 op/s (99.7%) | 49.29 op/s (92.7%) | 50.77 op/s (95.5%) | 39.55 op/s (74.4%) | 53.16 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 4.063 op/s (91.8%) | 3.874 op/s (87.5%) | 4.127 op/s (93.2%) | 3.882 op/s (87.7%) | 4.429 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 15.21 op/s (91.7%) | 15.25 op/s (92.0%) | 15.15 op/s (91.3%) | 14.31 op/s (86.3%) | 16.58 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 976.6 op/s (92.3%) | 950.6 op/s (89.8%) | 924.2 op/s (87.3%) | 908.3 op/s (85.8%) | 1 058 op/s |

![1.2 WCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-2-wcc-real-world-graphs.svg)

### 1.3 TopOrderAndLevels — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) | Graphity 3.0.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 218.1 ms (1.0×) | 231.1 ms (1.0×) | 227.8 ms (1.0×) | 239.6 ms (1.1×) | 222.7 ms | 217.4 ms (1.0×) | 226.4 ms (1.0×) | 222.0 ms (1.0×) | 236.1 ms (1.1×) | 215.3 ms (1.0×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) | Graphity 3.0.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 4.585 op/s (102%) | 4.327 op/s (96.4%) | 4.390 op/s (97.8%) | 4.174 op/s (92.9%) | 4.490 op/s | 4.600 op/s (102%) | 4.417 op/s (98.4%) | 4.505 op/s (100%) | 4.235 op/s (94.3%) | 4.645 op/s (103%) |

![1.3 TopOrderAndLevels — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-3-toporderandlevels-real-world-graphs.svg)

Self-loops are dropped for this algorithm: it requires a DAG, and a self-loop is a cycle.
Every library in a row reads that same graph; the other tables measure each file unmodified.

For cit-Patents that is one edge of 16,518,948 — patent 5489070 cites itself, and that loop is the graph's only cycle.

### 1.4 BackEdges — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 3.0.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 42.84 ms (1.0×) | 40.55 ms (0.9×) | 41.24 ms (0.9×) | 45.36 ms (1.0×) | 42.62 ms (1.0×) | 44.33 ms | 43.32 ms (1.0×) | 40.70 ms (0.9×) | 43.52 ms (1.0×) | 42.56 ms (1.0×) | 43.63 ms (1.0×) | 41.42 ms (0.9×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.916 ms (1.0×) | 0.903 ms (1.0×) | 0.902 ms (1.0×) | 0.923 ms (1.1×) | 0.953 ms (1.1×) | 0.879 ms | 0.773 ms (0.9×) | 0.752 ms (0.9×) | 0.751 ms (0.9×) | 0.774 ms (0.9×) | 0.770 ms (0.9×) | 0.716 ms (0.8×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 38.04 ms (0.8×) | 37.93 ms (0.8×) | 38.81 ms (0.8×) | 41.03 ms (0.8×) | 45.98 ms (0.9×) | 50.42 ms | 22.52 ms (0.4×) | 22.17 ms (0.4×) | 22.03 ms (0.4×) | 21.94 ms (0.4×) | 23.57 ms (0.5×) | 25.11 ms (0.5×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 31.30 ms (0.9×) | 30.56 ms (0.9×) | 28.86 ms (0.8×) | 33.18 ms (1.0×) | 38.51 ms (1.1×) | 34.22 ms | 19.88 ms (0.6×) | 17.82 ms (0.5×) | 17.73 ms (0.5×) | 18.98 ms (0.6×) | 20.78 ms (0.6×) | 21.47 ms (0.6×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 865.3 ms (0.9×) | 835.5 ms (0.9×) | 851.8 ms (0.9×) | 836.3 ms (0.9×) | 845.5 ms (0.9×) | 922.7 ms | 279.3 ms (0.3×) | 279.9 ms (0.3×) | 280.3 ms (0.3×) | 281.4 ms (0.3×) | 284.0 ms (0.3×) | 366.4 ms (0.4×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 3.0.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 23.34 op/s (103%) | 24.66 op/s (109%) | 24.25 op/s (107%) | 22.05 op/s (97.7%) | 23.46 op/s (104%) | 22.56 op/s | 23.08 op/s (102%) | 24.57 op/s (109%) | 22.98 op/s (102%) | 23.50 op/s (104%) | 22.92 op/s (102%) | 24.14 op/s (107%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 092 op/s (96.0%) | 1 107 op/s (97.3%) | 1 109 op/s (97.5%) | 1 083 op/s (95.2%) | 1 049 op/s (92.2%) | 1 138 op/s | 1 294 op/s (114%) | 1 330 op/s (117%) | 1 332 op/s (117%) | 1 292 op/s (114%) | 1 299 op/s (114%) | 1 397 op/s (123%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 26.29 op/s (133%) | 26.36 op/s (133%) | 25.77 op/s (130%) | 24.37 op/s (123%) | 21.75 op/s (110%) | 19.83 op/s | 44.41 op/s (224%) | 45.10 op/s (227%) | 45.38 op/s (229%) | 45.57 op/s (230%) | 42.42 op/s (214%) | 39.82 op/s (201%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 31.95 op/s (109%) | 32.72 op/s (112%) | 34.65 op/s (119%) | 30.14 op/s (103%) | 25.97 op/s (88.9%) | 29.23 op/s | 50.29 op/s (172%) | 56.13 op/s (192%) | 56.40 op/s (193%) | 52.70 op/s (180%) | 48.12 op/s (165%) | 46.58 op/s (159%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.156 op/s (107%) | 1.197 op/s (110%) | 1.174 op/s (108%) | 1.196 op/s (110%) | 1.183 op/s (109%) | 1.084 op/s | 3.580 op/s (330%) | 3.573 op/s (330%) | 3.568 op/s (329%) | 3.554 op/s (328%) | 3.521 op/s (325%) | 2.729 op/s (252%) |

![1.4 BackEdges — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-4-backedges-real-world-graphs.svg)

### 1.5 EdgeTypes — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 3.0.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 66.97 ms (1.0×) | 64.11 ms (1.0×) | 66.14 ms (1.0×) | 67.67 ms (1.0×) | 73.77 ms (1.1×) | 65.29 ms | 65.10 ms (1.0×) | 67.50 ms (1.0×) | 62.51 ms (1.0×) | 65.93 ms (1.0×) | 73.40 ms (1.1×) | 65.48 ms (1.0×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.207 ms (1.0×) | 1.235 ms (1.0×) | 1.202 ms (1.0×) | 1.218 ms (1.0×) | 1.202 ms (1.0×) | 1.188 ms | 0.960 ms (0.8×) | 0.951 ms (0.8×) | 0.956 ms (0.8×) | 0.943 ms (0.8×) | 0.972 ms (0.8×) | 0.965 ms (0.8×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 30.93 ms (1.0×) | 30.89 ms (1.0×) | 31.89 ms (1.0×) | 34.55 ms (1.1×) | 41.42 ms (1.3×) | 32.47 ms | 18.91 ms (0.6×) | 18.59 ms (0.6×) | 18.39 ms (0.6×) | 18.47 ms (0.6×) | 20.39 ms (0.6×) | 17.99 ms (0.6×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 34.31 ms (1.1×) | 34.14 ms (1.0×) | 34.82 ms (1.1×) | 37.63 ms (1.2×) | 41.47 ms (1.3×) | 32.63 ms | 20.44 ms (0.6×) | 20.29 ms (0.6×) | 20.50 ms (0.6×) | 19.78 ms (0.6×) | 22.00 ms (0.7×) | 18.61 ms (0.6×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 885.6 ms (1.0×) | 879.6 ms (1.0×) | 894.1 ms (1.0×) | 882.1 ms (1.0×) | 887.3 ms (1.0×) | 876.1 ms | 250.2 ms (0.3×) | 243.9 ms (0.3×) | 247.4 ms (0.3×) | 248.4 ms (0.3×) | 251.9 ms (0.3×) | 240.7 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 3.0.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 14.93 op/s (97.5%) | 15.60 op/s (102%) | 15.12 op/s (98.7%) | 14.78 op/s (96.5%) | 13.56 op/s (88.5%) | 15.32 op/s | 15.36 op/s (100%) | 14.82 op/s (96.7%) | 16.00 op/s (104%) | 15.17 op/s (99.0%) | 13.62 op/s (89.0%) | 15.27 op/s (99.7%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 828.5 op/s (98.4%) | 809.7 op/s (96.2%) | 831.9 op/s (98.8%) | 821.0 op/s (97.5%) | 831.9 op/s (98.8%) | 841.8 op/s | 1 042 op/s (124%) | 1 052 op/s (125%) | 1 046 op/s (124%) | 1 060 op/s (126%) | 1 029 op/s (122%) | 1 036 op/s (123%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 32.33 op/s (105%) | 32.37 op/s (105%) | 31.36 op/s (102%) | 28.95 op/s (94.0%) | 24.14 op/s (78.4%) | 30.80 op/s | 52.89 op/s (172%) | 53.79 op/s (175%) | 54.37 op/s (177%) | 54.15 op/s (176%) | 49.04 op/s (159%) | 55.58 op/s (180%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 29.15 op/s (95.1%) | 29.29 op/s (95.6%) | 28.72 op/s (93.7%) | 26.57 op/s (86.7%) | 24.11 op/s (78.7%) | 30.64 op/s | 48.92 op/s (160%) | 49.29 op/s (161%) | 48.79 op/s (159%) | 50.56 op/s (165%) | 45.45 op/s (148%) | 53.72 op/s (175%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.129 op/s (98.9%) | 1.137 op/s (99.6%) | 1.118 op/s (98.0%) | 1.134 op/s (99.3%) | 1.127 op/s (98.7%) | 1.141 op/s | 3.997 op/s (350%) | 4.100 op/s (359%) | 4.042 op/s (354%) | 4.026 op/s (353%) | 3.970 op/s (348%) | 4.155 op/s (364%) |

![1.5 EdgeTypes — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-5-edgetypes-real-world-graphs.svg)

### 1.6 Reorder (DFS-preorder) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 |
|---|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 151.0 ms (1.4×) | 108.9 ms (1.0×) | 115.9 ms (1.1×) | 107.0 ms (1.0×) | 106.0 ms (1.0×) | 104.9 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 3.348 ms (1.2×) | 2.854 ms (1.0×) | 2.844 ms (1.0×) | 2.843 ms (1.0×) | 2.905 ms (1.0×) | 2.838 ms |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 61.88 ms (1.3×) | 50.07 ms (1.0×) | 50.77 ms (1.0×) | 54.63 ms (1.1×) | 63.50 ms (1.3×) | 49.41 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 79.96 ms (1.1×) | 66.02 ms (0.9×) | 65.97 ms (0.9×) | 73.47 ms (1.1×) | 79.13 ms (1.1×) | 69.56 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1850.1 ms (1.4×) | 1335.1 ms (1.0×) | 1388.8 ms (1.1×) | 1305.9 ms (1.0×) | 1328.1 ms (1.0×) | 1312.9 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 |
|---|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 6.623 op/s (69.5%) | 9.183 op/s (96.3%) | 8.628 op/s (90.5%) | 9.346 op/s (98.0%) | 9.434 op/s (99.0%) | 9.533 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 298.7 op/s (84.8%) | 350.4 op/s (99.4%) | 351.6 op/s (99.8%) | 351.7 op/s (99.8%) | 344.2 op/s (97.7%) | 352.4 op/s |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 16.16 op/s (79.9%) | 19.97 op/s (98.7%) | 19.70 op/s (97.3%) | 18.31 op/s (90.5%) | 15.75 op/s (77.8%) | 20.24 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 12.51 op/s (87.0%) | 15.15 op/s (105%) | 15.16 op/s (105%) | 13.61 op/s (94.7%) | 12.64 op/s (87.9%) | 14.38 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.541 op/s (71.0%) | 0.749 op/s (98.3%) | 0.720 op/s (94.5%) | 0.766 op/s (101%) | 0.753 op/s (98.9%) | 0.762 op/s |

![1.6 Reorder (DFS-preorder) cost — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-6-reorder-dfs-preorder-cost-real-world-graphs.svg)

### 1.7 BiAdjacency (in-adjacency) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 14.62 ms (0.8×) | 14.53 ms (0.8×) | 15.33 ms (0.8×) | 18.22 ms (1.0×) | 18.67 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.17 ms (1.1×) | 16.25 ms (1.1×) | 17.13 ms (1.1×) | 20.35 ms (1.3×) | 15.34 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 406.0 ms (0.9×) | 408.0 ms (0.9×) | 381.5 ms (0.9×) | 391.8 ms (0.9×) | 441.5 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 106.9 ms (1.0×) | 106.4 ms (1.0×) | 108.0 ms (1.1×) | 122.7 ms (1.2×) | 102.4 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.558 ms (0.7×) | 0.558 ms (0.7×) | 0.557 ms (0.7×) | 0.581 ms (0.8×) | 0.766 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 3.0.0 |
|---|---:|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 68.38 op/s (128%) | 68.82 op/s (128%) | 65.24 op/s (122%) | 54.89 op/s (102%) | 53.57 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.84 op/s (94.9%) | 61.54 op/s (94.4%) | 58.37 op/s (89.5%) | 49.14 op/s (75.4%) | 65.19 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 2.463 op/s (109%) | 2.451 op/s (108%) | 2.621 op/s (116%) | 2.552 op/s (113%) | 2.265 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.355 op/s (95.8%) | 9.398 op/s (96.2%) | 9.259 op/s (94.8%) | 8.150 op/s (83.5%) | 9.766 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 792 op/s (137%) | 1 792 op/s (137%) | 1 795 op/s (138%) | 1 721 op/s (132%) | 1 305 op/s |

![1.7 BiAdjacency (in-adjacency) cost — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-7-biadjacency-in-adjacency-cost-real-world-graphs.svg)
