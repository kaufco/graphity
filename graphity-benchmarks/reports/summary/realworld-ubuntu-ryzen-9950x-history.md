# Graphity Version History — realworld / ubuntu-ryzen-9950x

Benchmarked versions: `2.0.0`, `2.1.0`, `2.2.0`, `2.3.0`, `2.4.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`realworld-ubuntu-ryzen-9950x-peers.md`](realworld-ubuntu-ryzen-9950x-peers.md).

Versions `2.0.0`, `2.1.0`, `2.2.0` report the mean over all measurement windows; the later ones a near-best window (`Estimate` below), which comes out a few percent faster for unchanged code.

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

## 1 Performance

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.4.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 SCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 92.50 ms (0.7×) | 88.53 ms (0.7×) | 106.4 ms (0.8×) | 91.56 ms (0.7×) | 125.2 ms | 86.90 ms (0.7×) | 89.83 ms (0.7×) | 94.13 ms (0.8×) | 87.57 ms (0.7×) | 126.3 ms (1.0×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.601 ms (1.0×) | 1.612 ms (1.0×) | 1.602 ms (1.0×) | 1.620 ms (1.0×) | 1.678 ms | 1.156 ms (0.7×) | 1.137 ms (0.7×) | 1.149 ms (0.7×) | 1.150 ms (0.7×) | 1.147 ms (0.7×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 41.54 ms (0.7×) | 40.75 ms (0.7×) | 44.45 ms (0.8×) | 45.19 ms (0.8×) | 57.55 ms | 20.55 ms (0.4×) | 20.38 ms (0.4×) | 20.51 ms (0.4×) | 20.56 ms (0.4×) | 22.96 ms (0.4×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.01 ms (0.8×) | 60.01 ms (0.8×) | 62.68 ms (0.8×) | 61.57 ms (0.8×) | 74.37 ms | 34.95 ms (0.5×) | 34.66 ms (0.5×) | 35.38 ms (0.5×) | 34.39 ms (0.5×) | 39.88 ms (0.5×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1043.8 ms (1.0×) | 1017.2 ms (0.9×) | 1063.4 ms (1.0×) | 993.9 ms (0.9×) | 1073.8 ms | 292.3 ms (0.3×) | 296.3 ms (0.3×) | 297.1 ms (0.3×) | 287.2 ms (0.3×) | 324.4 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 10.81 op/s (135%) | 11.30 op/s (141%) | 9.398 op/s (118%) | 10.92 op/s (137%) | 7.987 op/s | 11.51 op/s (144%) | 11.13 op/s (139%) | 10.62 op/s (133%) | 11.42 op/s (143%) | 7.918 op/s (99.1%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 624.6 op/s (105%) | 620.3 op/s (104%) | 624.2 op/s (105%) | 617.3 op/s (104%) | 595.9 op/s | 865.1 op/s (145%) | 879.5 op/s (148%) | 870.3 op/s (146%) | 869.6 op/s (146%) | 871.8 op/s (146%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 24.07 op/s (139%) | 24.54 op/s (141%) | 22.50 op/s (129%) | 22.13 op/s (127%) | 17.38 op/s | 48.65 op/s (280%) | 49.07 op/s (282%) | 48.77 op/s (281%) | 48.63 op/s (280%) | 43.56 op/s (251%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.39 op/s (122%) | 16.66 op/s (124%) | 15.95 op/s (119%) | 16.24 op/s (121%) | 13.45 op/s | 28.61 op/s (213%) | 28.85 op/s (215%) | 28.27 op/s (210%) | 29.08 op/s (216%) | 25.08 op/s (187%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.958 op/s (103%) | 0.983 op/s (106%) | 0.940 op/s (101%) | 1.006 op/s (108%) | 0.931 op/s | 3.421 op/s (367%) | 3.375 op/s (362%) | 3.366 op/s (361%) | 3.482 op/s (374%) | 3.083 op/s (331%) |

![1.1 SCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-1-scc-real-world-graphs.svg)

### 1.2 WCC — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 17.36 ms (0.8×) | 17.69 ms (0.8×) | 17.17 ms (0.8×) | 21.54 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 18.86 ms (0.7×) | 20.29 ms (0.8×) | 19.70 ms (0.8×) | 25.28 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 246.1 ms (1.0×) | 258.1 ms (1.0×) | 242.3 ms (0.9×) | 257.6 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 65.73 ms (0.9×) | 65.57 ms (0.9×) | 66.03 ms (0.9×) | 69.86 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.024 ms (0.9×) | 1.052 ms (1.0×) | 1.082 ms (1.0×) | 1.101 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 57.60 op/s (124%) | 56.52 op/s (122%) | 58.24 op/s (125%) | 46.44 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 53.01 op/s (134%) | 49.29 op/s (125%) | 50.77 op/s (128%) | 39.55 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 4.063 op/s (105%) | 3.874 op/s (99.8%) | 4.127 op/s (106%) | 3.882 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 15.21 op/s (106%) | 15.25 op/s (107%) | 15.15 op/s (106%) | 14.31 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 976.6 op/s (108%) | 950.6 op/s (105%) | 924.2 op/s (102%) | 908.3 op/s |

![1.2 WCC — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-2-wcc-real-world-graphs.svg)

### 1.3 TopOrderAndLevels — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 218.1 ms (0.9×) | 231.1 ms (1.0×) | 227.8 ms (1.0×) | 239.6 ms | 217.4 ms (0.9×) | 226.4 ms (0.9×) | 222.0 ms (0.9×) | 236.1 ms (1.0×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 4.585 op/s (110%) | 4.327 op/s (104%) | 4.390 op/s (105%) | 4.174 op/s | 4.600 op/s (110%) | 4.417 op/s (106%) | 4.505 op/s (108%) | 4.235 op/s (101%) |

![1.3 TopOrderAndLevels — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-3-toporderandlevels-real-world-graphs.svg)

Self-loops are dropped for this algorithm: it requires a DAG, and a self-loop is a cycle.
Every library in a row reads that same graph; the other tables measure each file unmodified.

For cit-Patents that is one edge of 16,518,948 — patent 5489070 cites itself, and that loop is the graph's only cycle.

### 1.4 BackEdges — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 42.84 ms (1.0×) | 40.55 ms (1.0×) | 41.24 ms (1.0×) | 45.36 ms (1.1×) | 42.62 ms | 43.32 ms (1.0×) | 40.70 ms (1.0×) | 43.52 ms (1.0×) | 42.56 ms (1.0×) | 43.63 ms (1.0×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.916 ms (1.0×) | 0.903 ms (0.9×) | 0.902 ms (0.9×) | 0.923 ms (1.0×) | 0.953 ms | 0.773 ms (0.8×) | 0.752 ms (0.8×) | 0.751 ms (0.8×) | 0.774 ms (0.8×) | 0.770 ms (0.8×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 38.04 ms (0.8×) | 37.93 ms (0.8×) | 38.81 ms (0.8×) | 41.03 ms (0.9×) | 45.98 ms | 22.52 ms (0.5×) | 22.17 ms (0.5×) | 22.03 ms (0.5×) | 21.94 ms (0.5×) | 23.57 ms (0.5×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 31.30 ms (0.8×) | 30.56 ms (0.8×) | 28.86 ms (0.7×) | 33.18 ms (0.9×) | 38.51 ms | 19.88 ms (0.5×) | 17.82 ms (0.5×) | 17.73 ms (0.5×) | 18.98 ms (0.5×) | 20.78 ms (0.5×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 865.3 ms (1.0×) | 835.5 ms (1.0×) | 851.8 ms (1.0×) | 836.3 ms (1.0×) | 845.5 ms | 279.3 ms (0.3×) | 279.9 ms (0.3×) | 280.3 ms (0.3×) | 281.4 ms (0.3×) | 284.0 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 23.34 op/s (99.5%) | 24.66 op/s (105%) | 24.25 op/s (103%) | 22.05 op/s (94.0%) | 23.46 op/s | 23.08 op/s (98.4%) | 24.57 op/s (105%) | 22.98 op/s (97.9%) | 23.50 op/s (100%) | 22.92 op/s (97.7%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 092 op/s (104%) | 1 107 op/s (106%) | 1 109 op/s (106%) | 1 083 op/s (103%) | 1 049 op/s | 1 294 op/s (123%) | 1 330 op/s (127%) | 1 332 op/s (127%) | 1 292 op/s (123%) | 1 299 op/s (124%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 26.29 op/s (121%) | 26.36 op/s (121%) | 25.77 op/s (118%) | 24.37 op/s (112%) | 21.75 op/s | 44.41 op/s (204%) | 45.10 op/s (207%) | 45.38 op/s (209%) | 45.57 op/s (210%) | 42.42 op/s (195%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 31.95 op/s (123%) | 32.72 op/s (126%) | 34.65 op/s (133%) | 30.14 op/s (116%) | 25.97 op/s | 50.29 op/s (194%) | 56.13 op/s (216%) | 56.40 op/s (217%) | 52.70 op/s (203%) | 48.12 op/s (185%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.156 op/s (97.7%) | 1.197 op/s (101%) | 1.174 op/s (99.3%) | 1.196 op/s (101%) | 1.183 op/s | 3.580 op/s (303%) | 3.573 op/s (302%) | 3.568 op/s (302%) | 3.554 op/s (300%) | 3.521 op/s (298%) |

![1.4 BackEdges — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-4-backedges-real-world-graphs.svg)

### 1.5 EdgeTypes — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 66.97 ms (0.9×) | 64.11 ms (0.9×) | 66.14 ms (0.9×) | 67.67 ms (0.9×) | 73.77 ms | 65.10 ms (0.9×) | 67.50 ms (0.9×) | 62.51 ms (0.8×) | 65.93 ms (0.9×) | 73.40 ms (1.0×) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1.207 ms (1.0×) | 1.235 ms (1.0×) | 1.202 ms (1.0×) | 1.218 ms (1.0×) | 1.202 ms | 0.960 ms (0.8×) | 0.951 ms (0.8×) | 0.956 ms (0.8×) | 0.943 ms (0.8×) | 0.972 ms (0.8×) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 30.93 ms (0.7×) | 30.89 ms (0.7×) | 31.89 ms (0.8×) | 34.55 ms (0.8×) | 41.42 ms | 18.91 ms (0.5×) | 18.59 ms (0.4×) | 18.39 ms (0.4×) | 18.47 ms (0.4×) | 20.39 ms (0.5×) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 34.31 ms (0.8×) | 34.14 ms (0.8×) | 34.82 ms (0.8×) | 37.63 ms (0.9×) | 41.47 ms | 20.44 ms (0.5×) | 20.29 ms (0.5×) | 20.50 ms (0.5×) | 19.78 ms (0.5×) | 22.00 ms (0.5×) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 885.6 ms (1.0×) | 879.6 ms (1.0×) | 894.1 ms (1.0×) | 882.1 ms (1.0×) | 887.3 ms | 250.2 ms (0.3×) | 243.9 ms (0.3×) | 247.4 ms (0.3×) | 248.4 ms (0.3×) | 251.9 ms (0.3×) |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 14.93 op/s (110%) | 15.60 op/s (115%) | 15.12 op/s (112%) | 14.78 op/s (109%) | 13.56 op/s | 15.36 op/s (113%) | 14.82 op/s (109%) | 16.00 op/s (118%) | 15.17 op/s (112%) | 13.62 op/s (101%) |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 828.5 op/s (99.6%) | 809.7 op/s (97.3%) | 831.9 op/s (100%) | 821.0 op/s (98.7%) | 831.9 op/s | 1 042 op/s (125%) | 1 052 op/s (126%) | 1 046 op/s (126%) | 1 060 op/s (127%) | 1 029 op/s (124%) |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 32.33 op/s (134%) | 32.37 op/s (134%) | 31.36 op/s (130%) | 28.95 op/s (120%) | 24.14 op/s | 52.89 op/s (219%) | 53.79 op/s (223%) | 54.37 op/s (225%) | 54.15 op/s (224%) | 49.04 op/s (203%) |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 29.15 op/s (121%) | 29.29 op/s (121%) | 28.72 op/s (119%) | 26.57 op/s (110%) | 24.11 op/s | 48.92 op/s (203%) | 49.29 op/s (204%) | 48.79 op/s (202%) | 50.56 op/s (210%) | 45.45 op/s (188%) |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1.129 op/s (100%) | 1.137 op/s (101%) | 1.118 op/s (99.2%) | 1.134 op/s (101%) | 1.127 op/s | 3.997 op/s (355%) | 4.100 op/s (364%) | 4.042 op/s (359%) | 4.026 op/s (357%) | 3.970 op/s (352%) |

![1.5 EdgeTypes — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-5-edgetypes-real-world-graphs.svg)

### 1.6 Reorder (DFS-preorder) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 151.0 ms (1.4×) | 108.9 ms (1.0×) | 115.9 ms (1.1×) | 107.0 ms (1.0×) | 106.0 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 3.348 ms (1.2×) | 2.854 ms (1.0×) | 2.844 ms (1.0×) | 2.843 ms (1.0×) | 2.905 ms |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 61.88 ms (1.0×) | 50.07 ms (0.8×) | 50.77 ms (0.8×) | 54.63 ms (0.9×) | 63.50 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 79.96 ms (1.0×) | 66.02 ms (0.8×) | 65.97 ms (0.8×) | 73.47 ms (0.9×) | 79.13 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 1850.1 ms (1.4×) | 1335.1 ms (1.0×) | 1388.8 ms (1.0×) | 1305.9 ms (1.0×) | 1328.1 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 6.623 op/s (70.2%) | 9.183 op/s (97.3%) | 8.628 op/s (91.5%) | 9.346 op/s (99.1%) | 9.434 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 298.7 op/s (86.8%) | 350.4 op/s (102%) | 351.6 op/s (102%) | 351.7 op/s (102%) | 344.2 op/s |
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 16.16 op/s (103%) | 19.97 op/s (127%) | 19.70 op/s (125%) | 18.31 op/s (116%) | 15.75 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 12.51 op/s (99.0%) | 15.15 op/s (120%) | 15.16 op/s (120%) | 13.61 op/s (108%) | 12.64 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 0.541 op/s (71.8%) | 0.749 op/s (99.5%) | 0.720 op/s (95.6%) | 0.766 op/s (102%) | 0.753 op/s |

![1.6 Reorder (DFS-preorder) cost — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-6-reorder-dfs-preorder-cost-real-world-graphs.svg)

### 1.7 BiAdjacency (in-adjacency) cost — real-world graphs

#### Times (ms/op, lower is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 14.62 ms (0.8×) | 14.53 ms (0.8×) | 15.33 ms (0.8×) | 18.22 ms |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 16.17 ms (0.8×) | 16.25 ms (0.8×) | 17.13 ms (0.8×) | 20.35 ms |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 406.0 ms (1.0×) | 408.0 ms (1.0×) | 381.5 ms (1.0×) | 391.8 ms |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 106.9 ms (0.9×) | 106.4 ms (0.9×) | 108.0 ms (0.9×) | 122.7 ms |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 0.558 ms (1.0×) | 0.558 ms (1.0×) | 0.557 ms (1.0×) | 0.581 ms |

#### Throughput (op/s, higher is better)

| Dataset | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| web-Google<br><sub>875.7k nodes, 5.1M edges</sub> | 68.38 op/s (125%) | 68.82 op/s (125%) | 65.24 op/s (119%) | 54.89 op/s |
| wiki-Talk<br><sub>2.4M nodes, 5M edges</sub> | 61.84 op/s (126%) | 61.54 op/s (125%) | 58.37 op/s (119%) | 49.14 op/s |
| soc-LiveJournal1<br><sub>4.8M nodes, 69M edges</sub> | 2.463 op/s (96.5%) | 2.451 op/s (96.0%) | 2.621 op/s (103%) | 2.552 op/s |
| cit-Patents<br><sub>3.8M nodes, 16.5M edges</sub> | 9.355 op/s (115%) | 9.398 op/s (115%) | 9.259 op/s (114%) | 8.150 op/s |
| cit-HepPh<br><sub>34.5k nodes, 421.6k edges</sub> | 1 792 op/s (104%) | 1 792 op/s (104%) | 1 795 op/s (104%) | 1 721 op/s |

![1.7 BiAdjacency (in-adjacency) cost — real-world graphs](fig/realworld-ubuntu-ryzen-9950x-history/1-7-biadjacency-in-adjacency-cost-real-world-graphs.svg)
