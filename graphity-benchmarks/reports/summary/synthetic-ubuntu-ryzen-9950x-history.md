# Graphity Version History — synthetic / ubuntu-ryzen-9950x

Benchmarked versions: `1.0`, `2.0.0`, `2.1.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`synthetic-ubuntu-ryzen-9950x-peers.md`](synthetic-ubuntu-ryzen-9950x-peers.md).

```
Profile:      synthetic
Timestamp:    2026-08-17 01:30:48
JDK:          OpenJDK 64-Bit Server VM 25.0.2 (Amazon.com Inc.)
OS:           Ubuntu 25.10, kernel 6.17.0-41-generic (amd64)
CPU:          AMD Ryzen 9 9950X 16-Core Processor (32 logical cores)
RAM:          89.7 GiB
```

## Contents

- [1 Performance](#1-performance)
  - [1.1 Build](#11-build)
  - [1.2 SCC](#12-scc)
  - [1.3 WCC](#13-wcc)
  - [1.4 CCs](#14-ccs)
  - [1.5 Condense](#15-condense)
  - [1.6 TopOrder](#16-toporder)
  - [1.7 TopOrderAndLevels](#17-toporderandlevels)
  - [1.8 BackEdges](#18-backedges)
  - [1.9 EdgeTypes](#19-edgetypes)
  - [1.10 Reorder (DFS-preorder) cost](#110-reorder-dfs-preorder-cost)
  - [1.11 BiAdjacency (in-adjacency) cost](#111-biadjacency-in-adjacency-cost)
- [2 Memory Footprint](#2-memory-footprint)
  - [2.1 Total Graph Size](#21-total-graph-size)
  - [2.2 Bytes per Element  `totalBytes / (Nodes + Edges)`](#22-bytes-per-element-totalbytes-nodes-edges)
- [3 Allocations](#3-allocations)
  - [3.1 Build](#31-build)
  - [3.2 SCC](#32-scc)
  - [3.3 WCC](#33-wcc)
  - [3.4 CCs](#34-ccs)
  - [3.5 Condense](#35-condense)
  - [3.6 TopOrder](#36-toporder)
  - [3.7 TopOrderAndLevels](#37-toporderandlevels)
  - [3.8 BackEdges](#38-backedges)
  - [3.9 EdgeTypes](#39-edgetypes)

## 1 Performance

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.1.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 Build

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.083 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.084 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.092 ms (1.0×) | 0.092 ms (1.0×) | 0.093 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.164 ms (1.0×) | 0.162 ms (1.0×) | 0.163 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.597 ms (1.0×) | 1.573 ms (1.0×) | 1.581 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.572 ms (1.0×) | 1.546 ms (1.0×) | 1.623 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.550 ms (1.0×) | 1.588 ms (1.0×) | 1.587 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.551 ms (0.9×) | 1.553 ms (1.0×) | 1.634 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (100.0%) | 12 195 op/s (101%) | 12 048 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (101%) | 12 195 op/s (102%) | 11 905 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 870 op/s (101%) | 10 870 op/s (101%) | 10 753 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 098 op/s (99.4%) | 6 173 op/s (101%) | 6 135 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 626.2 op/s (99.0%) | 635.7 op/s (101%) | 632.5 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 636.1 op/s (103%) | 646.8 op/s (105%) | 616.1 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 645.2 op/s (102%) | 629.7 op/s (99.9%) | 630.1 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 644.7 op/s (105%) | 643.9 op/s (105%) | 612.0 op/s |

![1.1.1 Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-1-build-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.686 ms (1.0×) | 0.680 ms (1.0×) | 0.695 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.687 ms (1.0×) | 0.682 ms (1.0×) | 0.694 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.093 ms (1.0×) | 1.091 ms (1.0×) | 1.100 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.249 ms (1.0×) | 2.257 ms (1.0×) | 2.218 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.27 ms (1.0×) | 16.93 ms (1.0×) | 17.11 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 16.99 ms (1.0×) | 16.74 ms (1.0×) | 17.28 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 16.96 ms (1.0×) | 16.82 ms (1.0×) | 17.11 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 17.04 ms (1.0×) | 16.06 ms (0.9×) | 17.09 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 458 op/s (101%) | 1 471 op/s (102%) | 1 439 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 456 op/s (101%) | 1 466 op/s (102%) | 1 441 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 914.9 op/s (101%) | 916.6 op/s (101%) | 909.1 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 444.6 op/s (98.6%) | 443.1 op/s (98.3%) | 450.9 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.90 op/s (99.1%) | 59.06 op/s (101%) | 58.44 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 58.85 op/s (102%) | 59.74 op/s (103%) | 57.87 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 58.96 op/s (101%) | 59.46 op/s (102%) | 58.45 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 58.70 op/s (100%) | 62.28 op/s (106%) | 58.52 op/s |

![1.1.2 Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-2-build-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 22.97 ms (1.0×) | 23.63 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 170.6 ms (1.0×) | 167.8 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 167.6 ms (1.0×) | 166.8 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 173.2 ms (1.0×) | 169.5 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 43.54 op/s (103%) | 42.31 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 5.862 op/s (98.4%) | 5.959 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 5.967 op/s (99.5%) | 5.995 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 5.774 op/s (97.9%) | 5.900 op/s |

![1.1.3 Build @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-3-build-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 248.6 ms (1.0×) | 240.4 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 2052.8 ms (1.1×) | 1916.6 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 1806.0 ms (1.0×) | 1829.5 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1784.0 ms (1.0×) | 1794.6 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4.023 op/s (96.7%) | 4.160 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.487 op/s (93.4%) | 0.522 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.554 op/s (101%) | 0.547 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 0.561 op/s (101%) | 0.557 op/s |

![1.1.4 Build @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-4-build-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 2945.8 ms (1.0×) | 2971.7 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.339 op/s (101%) | 0.337 op/s |

![1.1.5 Build @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-5-build-n-100-000-000.svg)

### 1.2 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.106 ms (1.0×) | 0.113 ms (1.1×) | 0.106 ms | 0.112 ms (1.1×) | 0.106 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.129 ms (1.0×) | 0.134 ms (1.0×) | 0.134 ms | 0.108 ms (0.8×) | 0.105 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.271 ms (2.6×) | 0.107 ms (1.0×) | 0.104 ms | 0.095 ms (0.9×) | 0.086 ms (0.8×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.245 ms (1.0×) | 0.239 ms (1.0×) | 0.242 ms | 0.111 ms (0.5×) | 0.112 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.3×) | 0.539 ms (1.0×) | 0.559 ms | 0.378 ms (0.7×) | 0.381 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.4×) | 0.531 ms (1.0×) | 0.539 ms | 0.384 ms (0.7×) | 0.373 ms (0.7×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.563 ms (2.2×) | 0.260 ms (1.0×) | 0.261 ms | 0.259 ms (1.0×) | 0.260 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.8×) | 0.338 ms (1.0×) | 0.348 ms | 0.250 ms (0.7×) | 0.265 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 434 op/s (100%) | 8 850 op/s (93.8%) | 9 434 op/s | 8 929 op/s (94.6%) | 9 434 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 752 op/s (104%) | 7 463 op/s (100%) | 7 463 op/s | 9 259 op/s (124%) | 9 524 op/s (128%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 3 690 op/s (38.4%) | 9 346 op/s (97.2%) | 9 615 op/s | 10 526 op/s (109%) | 11 628 op/s (121%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 082 op/s (98.8%) | 4 184 op/s (101%) | 4 132 op/s | 9 009 op/s (218%) | 8 929 op/s (216%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (76.6%) | 1 855 op/s (104%) | 1 789 op/s | 2 646 op/s (148%) | 2 625 op/s (147%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (73.8%) | 1 883 op/s (102%) | 1 855 op/s | 2 604 op/s (140%) | 2 681 op/s (145%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 776 op/s (46.4%) | 3 846 op/s (100%) | 3 831 op/s | 3 861 op/s (101%) | 3 846 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (54.5%) | 2 959 op/s (103%) | 2 874 op/s | 4 000 op/s (139%) | 3 774 op/s (131%) |

![1.2.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 1.130 ms (1.1×) | 1.030 ms | 1.117 ms (1.1×) | 1.025 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.415 ms (1.1×) | 2.192 ms (1.0×) | 2.146 ms | 1.038 ms (0.5×) | 1.035 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 11.69 ms (5.5×) | 2.240 ms (1.1×) | 2.122 ms | 1.413 ms (0.7×) | 1.305 ms (0.6×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.872 ms (1.1×) | 4.329 ms (1.0×) | 4.447 ms | 2.247 ms (0.5×) | 2.265 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.957 ms (1.3×) | 7.389 ms (1.0×) | 7.397 ms | 4.475 ms (0.6×) | 4.431 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 7.595 ms (1.0×) | 7.548 ms | 4.423 ms (0.6×) | 4.537 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.702 ms (2.2×) | 2.623 ms (1.0×) | 2.606 ms | 2.625 ms (1.0×) | 2.598 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 7.381 ms (1.5×) | 4.697 ms (1.0×) | 4.820 ms | 2.542 ms (0.5×) | 2.596 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 885.0 op/s (91.2%) | 970.9 op/s | 895.3 op/s (92.2%) | 975.6 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 414.1 op/s (88.9%) | 456.2 op/s (97.9%) | 466.0 op/s | 963.4 op/s (207%) | 966.2 op/s (207%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 85.56 op/s (18.2%) | 446.4 op/s (94.7%) | 471.3 op/s | 707.7 op/s (150%) | 766.3 op/s (163%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 205.3 op/s (91.3%) | 231.0 op/s (103%) | 224.9 op/s | 445.0 op/s (198%) | 441.5 op/s (196%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 100.4 op/s (74.3%) | 135.3 op/s (100%) | 135.2 op/s | 223.5 op/s (165%) | 225.7 op/s (167%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 131.7 op/s (99.4%) | 132.5 op/s | 226.1 op/s (171%) | 220.4 op/s (166%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 175.4 op/s (45.7%) | 381.2 op/s (99.4%) | 383.7 op/s | 381.0 op/s (99.3%) | 384.9 op/s (100%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 135.5 op/s (65.3%) | 212.9 op/s (103%) | 207.5 op/s | 393.4 op/s (190%) | 385.2 op/s (186%) |

![1.2.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 77.49 ms (1.0×) | 76.73 ms | 26.75 ms (0.3×) | 26.04 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 238.7 ms (1.0×) | 233.4 ms | — | 61.63 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 232.9 ms (1.0×) | 229.7 ms | 66.11 ms (0.3×) | 62.45 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.36 ms (1.0×) | 26.24 ms | — | 26.27 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 12.91 op/s (99.0%) | 13.03 op/s | 37.38 op/s (287%) | 38.41 op/s (295%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.189 op/s (97.8%) | 4.284 op/s | — | 16.23 op/s (379%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.294 op/s (98.6%) | 4.354 op/s | 15.13 op/s (347%) | 16.01 op/s (368%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 39.43 op/s (103%) | 38.10 op/s | — | 38.06 op/s (99.9%) |

![1.2.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3085.1 ms (1.0×) | 3051.6 ms | 739.3 ms (0.2×) | 700.8 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6301.4 ms (1.0×) | 6518.8 ms | — | 2978.2 ms (0.5×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 6297.6 ms (1.0×) | 6583.3 ms | 2740.8 ms (0.4×) | 2700.6 ms (0.4×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 268.4 ms (1.0×) | 275.9 ms | — | 271.5 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.324 op/s (98.9%) | 0.328 op/s | 1.353 op/s (413%) | 1.427 op/s (435%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (103%) | 0.153 op/s | — | 0.336 op/s (219%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (105%) | 0.152 op/s | 0.365 op/s (240%) | 0.370 op/s (244%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.726 op/s (103%) | 3.625 op/s | — | 3.683 op/s (102%) |

![1.2.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 36814 ms (1.0×) | 36265 ms | 9963.5 ms (0.3×) | 9839.3 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.027 op/s (98.5%) | 0.028 op/s | 0.100 op/s (364%) | 0.102 op/s (369%) |

![1.2.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-5-scc-n-100-000-000.svg)

### 1.3 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (1.0×) | 0.036 ms (1.0×) | 0.037 ms | 0.042 ms (1.1×) | 0.040 ms (1.1×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.046 ms (0.9×) | 0.046 ms (0.9×) | 0.053 ms | 0.042 ms (0.8×) | 0.042 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.038 ms (1.0×) | 0.038 ms (1.0×) | 0.038 ms | 0.038 ms (1.0×) | 0.044 ms (1.2×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.095 ms (1.0×) | 0.083 ms (0.9×) | 0.097 ms | 0.086 ms (0.9×) | 0.082 ms (0.8×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.530 ms (1.0×) | 0.524 ms (1.0×) | 0.523 ms | 0.524 ms (1.0×) | 0.520 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms (1.0×) | 0.524 ms (1.0×) | 0.525 ms | 0.527 ms (1.0×) | 0.521 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.579 ms | 0.578 ms (1.0×) | 0.578 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.640 ms (1.0×) | 0.640 ms (1.0×) | 0.641 ms | 0.581 ms (0.9×) | 0.614 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (100%) | 27 778 op/s (103%) | 27 027 op/s | 23 810 op/s (88.1%) | 25 000 op/s (92.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 739 op/s (115%) | 21 739 op/s (115%) | 18 868 op/s | 23 810 op/s (126%) | 23 810 op/s (126%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s (100%) | 26 316 op/s (100%) | 26 316 op/s | 26 316 op/s (100%) | 22 727 op/s (86.4%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 10 526 op/s (102%) | 12 048 op/s (117%) | 10 309 op/s | 11 628 op/s (113%) | 12 195 op/s (118%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 887 op/s (98.7%) | 1 908 op/s (99.8%) | 1 912 op/s | 1 908 op/s (99.8%) | 1 923 op/s (101%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s (100%) | 1 908 op/s (100%) | 1 905 op/s | 1 898 op/s (99.6%) | 1 919 op/s (101%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 730 op/s (100%) | 1 730 op/s (100%) | 1 727 op/s | 1 730 op/s (100%) | 1 730 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 562 op/s (100%) | 1 562 op/s (100%) | 1 560 op/s | 1 721 op/s (110%) | 1 629 op/s (104%) |

![1.3.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.393 ms (1.0×) | 0.398 ms (1.0×) | 0.406 ms | 0.400 ms (1.0×) | 0.352 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.228 ms (1.0×) | 1.232 ms | 0.390 ms (0.3×) | 0.347 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.661 ms (1.0×) | 0.636 ms (1.0×) | 0.669 ms | 0.732 ms (1.1×) | 0.770 ms (1.2×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.769 ms (1.0×) | 1.767 ms (1.0×) | 1.794 ms | 1.550 ms (0.9×) | 1.561 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.082 ms (1.0×) | 6.090 ms (1.0×) | 6.096 ms | 6.069 ms (1.0×) | 6.054 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.163 ms (1.0×) | 6.091 ms (1.0×) | 6.088 ms | 6.163 ms (1.0×) | 6.118 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.858 ms (1.0×) | 6.000 ms (1.0×) | 5.778 ms | 5.927 ms (1.0×) | 5.764 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.955 ms (1.0×) | 5.953 ms (1.0×) | 5.988 ms | 6.021 ms (1.0×) | 6.133 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 545 op/s (103%) | 2 513 op/s (102%) | 2 463 op/s | 2 500 op/s (102%) | 2 841 op/s (115%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (100%) | 814.3 op/s (100%) | 811.7 op/s | 2 564 op/s (316%) | 2 882 op/s (355%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 513 op/s (101%) | 1 572 op/s (105%) | 1 495 op/s | 1 366 op/s (91.4%) | 1 299 op/s (86.9%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 565.3 op/s (101%) | 565.9 op/s (102%) | 557.4 op/s | 645.2 op/s (116%) | 640.6 op/s (115%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 164.4 op/s (100%) | 164.2 op/s (100%) | 164.0 op/s | 164.8 op/s (100%) | 165.2 op/s (101%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.3 op/s (98.8%) | 164.2 op/s (100.0%) | 164.3 op/s | 162.3 op/s (98.8%) | 163.5 op/s (99.5%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 170.7 op/s (98.6%) | 166.7 op/s (96.3%) | 173.1 op/s | 168.7 op/s (97.5%) | 173.5 op/s (100%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 167.9 op/s (101%) | 168.0 op/s (101%) | 167.0 op/s | 166.1 op/s (99.5%) | 163.1 op/s (97.6%) |

![1.3.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.74 ms (1.0×) | 21.57 ms | 17.26 ms (0.8×) | 16.24 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 65.50 ms (0.8×) | 78.43 ms | — | 66.27 ms (0.8×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 70.47 ms (1.1×) | 66.99 ms | 69.61 ms (1.0×) | 66.64 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.95 ms (1.0×) | 51.65 ms | — | 50.10 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.00 op/s (99.2%) | 46.37 op/s | 57.94 op/s (125%) | 61.58 op/s (133%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 15.27 op/s (120%) | 12.75 op/s | — | 15.09 op/s (118%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.19 op/s (95.1%) | 14.93 op/s | 14.37 op/s (96.2%) | 15.01 op/s (101%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.63 op/s (101%) | 19.36 op/s | — | 19.96 op/s (103%) |

![1.3.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 615.6 ms (1.0×) | 600.5 ms | 410.6 ms (0.7×) | 378.4 ms (0.6×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4094.7 ms (1.0×) | 4106.6 ms | — | 3774.2 ms (0.9×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3899.0 ms (1.0×) | 3919.9 ms | 3844.9 ms (1.0×) | 3828.2 ms (1.0×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 754.7 ms (1.5×) | 516.1 ms | — | 516.0 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.624 op/s (97.5%) | 1.665 op/s | 2.435 op/s (146%) | 2.643 op/s (159%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.244 op/s (100%) | 0.244 op/s | — | 0.265 op/s (109%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s (101%) | 0.255 op/s | 0.260 op/s (102%) | 0.261 op/s (102%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.325 op/s (68.4%) | 1.938 op/s | — | 1.938 op/s (100%) |

![1.3.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8231.6 ms (0.9×) | 9442.8 ms | 6807.7 ms (0.7×) | 6812.3 ms (0.7×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.121 op/s (115%) | 0.106 op/s | 0.147 op/s (139%) | 0.147 op/s (139%) |

![1.3.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-5-wcc-n-100-000-000.svg)

### 1.4 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (0.9×) | 0.042 ms (1.0×) | 0.042 ms | 0.036 ms (0.9×) | 0.042 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.053 ms (1.2×) | 0.053 ms (1.2×) | 0.045 ms | 0.036 ms (0.8×) | 0.042 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.044 ms (1.0×) | 0.038 ms (0.9×) | 0.042 ms | 0.043 ms (1.0×) | 0.038 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.106 ms (1.1×) | 0.090 ms (1.0×) | 0.094 ms | 0.088 ms (0.9×) | 0.087 ms (0.9×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.518 ms (1.0×) | 0.526 ms (1.0×) | 0.524 ms | 0.520 ms (1.0×) | 0.527 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.522 ms (1.0×) | 0.524 ms (1.0×) | 0.524 ms | 0.534 ms (1.0×) | 0.521 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.586 ms (1.0×) | 0.580 ms (1.0×) | 0.588 ms | 0.584 ms (1.0×) | 0.588 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.0×) | 0.640 ms (1.0×) | 0.643 ms | 0.625 ms (1.0×) | 0.611 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (114%) | 23 810 op/s (100%) | 23 810 op/s | 27 778 op/s (117%) | 23 810 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s (84.9%) | 18 868 op/s (84.9%) | 22 222 op/s | 27 778 op/s (125%) | 23 810 op/s (107%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 22 727 op/s (95.5%) | 26 316 op/s (111%) | 23 810 op/s | 23 256 op/s (97.7%) | 26 316 op/s (111%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 9 434 op/s (88.7%) | 11 111 op/s (104%) | 10 638 op/s | 11 364 op/s (107%) | 11 494 op/s (108%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 931 op/s (101%) | 1 901 op/s (99.6%) | 1 908 op/s | 1 923 op/s (101%) | 1 898 op/s (99.4%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 916 op/s (100%) | 1 908 op/s (100%) | 1 908 op/s | 1 873 op/s (98.1%) | 1 919 op/s (101%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 706 op/s (100%) | 1 724 op/s (101%) | 1 701 op/s | 1 712 op/s (101%) | 1 701 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (101%) | 1 562 op/s (100%) | 1 555 op/s | 1 600 op/s (103%) | 1 637 op/s (105%) |

![1.4.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.396 ms (1.0×) | 0.403 ms (1.0×) | 0.394 ms | 0.392 ms (1.0×) | 0.394 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.326 ms (1.1×) | 1.232 ms | 0.369 ms (0.3×) | 0.365 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.665 ms (1.0×) | 0.649 ms (1.0×) | 0.664 ms | 0.728 ms (1.1×) | 0.751 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.762 ms (1.0×) | 1.769 ms (1.0×) | 1.763 ms | 1.501 ms (0.9×) | 1.542 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.173 ms (1.0×) | 6.095 ms (1.0×) | 6.117 ms | 6.057 ms (1.0×) | 6.066 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.157 ms (1.0×) | 6.186 ms (1.0×) | 6.175 ms | 6.029 ms (1.0×) | 6.032 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.806 ms (1.0×) | 5.871 ms (1.0×) | 5.816 ms | 5.856 ms (1.0×) | 5.781 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.999 ms (1.0×) | 5.949 ms (1.0×) | 5.935 ms | 6.055 ms (1.0×) | 6.115 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 525 op/s (99.5%) | 2 481 op/s (97.8%) | 2 538 op/s | 2 551 op/s (101%) | 2 538 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (100%) | 754.1 op/s (92.9%) | 811.7 op/s | 2 710 op/s (334%) | 2 740 op/s (338%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 504 op/s (99.8%) | 1 541 op/s (102%) | 1 506 op/s | 1 374 op/s (91.2%) | 1 332 op/s (88.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 567.5 op/s (100%) | 565.3 op/s (99.7%) | 567.2 op/s | 666.2 op/s (117%) | 648.5 op/s (114%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 162.0 op/s (99.1%) | 164.1 op/s (100%) | 163.5 op/s | 165.1 op/s (101%) | 164.9 op/s (101%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.4 op/s (100%) | 161.7 op/s (99.8%) | 161.9 op/s | 165.9 op/s (102%) | 165.8 op/s (102%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 172.2 op/s (100%) | 170.3 op/s (99.1%) | 171.9 op/s | 170.8 op/s (99.3%) | 173.0 op/s (101%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 166.7 op/s (98.9%) | 168.1 op/s (99.8%) | 168.5 op/s | 165.2 op/s (98.0%) | 163.5 op/s (97.1%) |

![1.4.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 19.83 ms (1.0×) | 19.65 ms | 17.28 ms (0.9×) | 17.28 ms (0.9×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 79.11 ms (1.2×) | 67.00 ms | — | 69.08 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 67.63 ms (1.0×) | 66.92 ms | 66.60 ms (1.0×) | 67.00 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.35 ms (1.0×) | 50.47 ms | — | 50.11 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 50.44 op/s (99.1%) | 50.88 op/s | 57.86 op/s (114%) | 57.86 op/s (114%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 12.64 op/s (84.7%) | 14.93 op/s | — | 14.48 op/s (97.0%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.79 op/s (99.0%) | 14.94 op/s | 15.02 op/s (100%) | 14.92 op/s (99.9%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.86 op/s (100%) | 19.82 op/s | — | 19.96 op/s (101%) |

![1.4.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 576.6 ms (1.0×) | 602.9 ms | 410.4 ms (0.7×) | 409.6 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4122.2 ms (1.0×) | 4102.7 ms | — | 3844.3 ms (0.9×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3909.4 ms (1.0×) | 3884.4 ms | 4002.5 ms (1.0×) | 3959.5 ms (1.0×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 569.9 ms (1.0×) | 550.5 ms | — | 595.1 ms (1.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.734 op/s (105%) | 1.659 op/s | 2.437 op/s (147%) | 2.441 op/s (147%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.243 op/s (99.5%) | 0.244 op/s | — | 0.260 op/s (107%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s (99.4%) | 0.257 op/s | 0.250 op/s (97.0%) | 0.253 op/s (98.1%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.755 op/s (96.6%) | 1.817 op/s | — | 1.680 op/s (92.5%) |

![1.4.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8479.6 ms (1.0×) | 8563.2 ms | 6840.6 ms (0.8×) | 6788.3 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.118 op/s (101%) | 0.117 op/s | 0.146 op/s (125%) | 0.147 op/s (126%) |

![1.4.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-5-ccs-n-100-000-000.svg)

### 1.5 Condense

#### Times @ 1k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 0.011 ms |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 0.007 ms |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 0.024 ms |
| DAG GNP sparse | 0.017 ms |

#### Throughput @ 1k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 90 909 op/s |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 142 857 op/s |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 41 667 op/s |
| DAG GNP sparse | 58 824 op/s |

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.080 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.099 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.182 ms |
| DAG GNP sparse | 0.256 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 500 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 10 101 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 5 495 op/s |
| DAG GNP sparse | 3 906 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.772 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.523 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1.722 ms |
| DAG GNP sparse | 3.542 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 295 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 656.6 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 580.7 op/s |
| DAG GNP sparse | 282.3 op/s |

### 1.6 TopOrder

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (1.0×) | 0.067 ms (1.2×) | 0.058 ms | 0.067 ms (1.2×) | 0.066 ms (1.1×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.099 ms (1.4×) | 0.072 ms (1.0×) | 0.070 ms | 0.056 ms (0.8×) | 0.052 ms (0.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.198 ms (3.5×) | 0.044 ms (0.8×) | 0.056 ms | 0.039 ms (0.7×) | 0.053 ms (0.9×) |
| Tree shfld. | 0.088 ms (1.8×) | 0.052 ms (1.0×) | 0.050 ms | 0.047 ms (0.9×) | 0.047 ms (0.9×) |
| DAG GNP sparse | 0.279 ms (3.3×) | 0.070 ms (0.8×) | 0.084 ms | 0.061 ms (0.7×) | 0.067 ms (0.8×) |
| DAG GNP dense | 0.448 ms (1.4×) | 0.337 ms (1.0×) | 0.324 ms | 0.245 ms (0.8×) | 0.243 ms (0.8×) |
| DAG GNP dense shfld. | 0.560 ms (1.5×) | 0.387 ms (1.0×) | 0.371 ms | 0.244 ms (0.7×) | 0.233 ms (0.6×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (100%) | 14 925 op/s (86.6%) | 17 241 op/s | 14 925 op/s (86.6%) | 15 152 op/s (87.9%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 10 101 op/s (70.7%) | 13 889 op/s (97.2%) | 14 286 op/s | 17 857 op/s (125%) | 19 231 op/s (135%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 051 op/s (28.3%) | 22 727 op/s (127%) | 17 857 op/s | 25 641 op/s (144%) | 18 868 op/s (106%) |
| Tree shfld. | 11 364 op/s (56.8%) | 19 231 op/s (96.2%) | 20 000 op/s | 21 277 op/s (106%) | 21 277 op/s (106%) |
| DAG GNP sparse | 3 584 op/s (30.1%) | 14 286 op/s (120%) | 11 905 op/s | 16 393 op/s (138%) | 14 925 op/s (125%) |
| DAG GNP dense | 2 232 op/s (72.3%) | 2 967 op/s (96.1%) | 3 086 op/s | 4 082 op/s (132%) | 4 115 op/s (133%) |
| DAG GNP dense shfld. | 1 786 op/s (66.2%) | 2 584 op/s (95.9%) | 2 695 op/s | 4 098 op/s (152%) | 4 292 op/s (159%) |

![1.6.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.539 ms (0.9×) | 0.656 ms (1.0×) | 0.632 ms | 0.662 ms (1.0×) | 0.629 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.287 ms (1.3×) | 1.051 ms (1.1×) | 0.964 ms | 0.566 ms (0.6×) | 0.521 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.963 ms (6.9×) | 1.311 ms (0.9×) | 1.440 ms | 1.023 ms (0.7×) | 1.142 ms (0.8×) |
| Tree shfld. | 5.091 ms (3.5×) | 1.487 ms (1.0×) | 1.475 ms | 1.212 ms (0.8×) | 1.242 ms (0.8×) |
| DAG GNP sparse | 9.521 ms (4.3×) | 2.251 ms (1.0×) | 2.201 ms | 1.480 ms (0.7×) | 1.470 ms (0.7×) |
| DAG GNP dense | 6.487 ms (1.5×) | 4.533 ms (1.0×) | 4.333 ms | 3.208 ms (0.7×) | 3.086 ms (0.7×) |
| DAG GNP dense shfld. | 8.053 ms (1.6×) | 5.303 ms (1.0×) | 5.127 ms | 3.254 ms (0.6×) | 3.085 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 855 op/s (117%) | 1 524 op/s (96.3%) | 1 582 op/s | 1 511 op/s (95.5%) | 1 590 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 777.0 op/s (74.9%) | 951.5 op/s (91.7%) | 1 037 op/s | 1 767 op/s (170%) | 1 919 op/s (185%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 100.4 op/s (14.5%) | 762.8 op/s (110%) | 694.4 op/s | 977.5 op/s (141%) | 875.7 op/s (126%) |
| Tree shfld. | 196.4 op/s (29.0%) | 672.5 op/s (99.2%) | 678.0 op/s | 825.1 op/s (122%) | 805.2 op/s (119%) |
| DAG GNP sparse | 105.0 op/s (23.1%) | 444.2 op/s (97.8%) | 454.3 op/s | 675.7 op/s (149%) | 680.3 op/s (150%) |
| DAG GNP dense | 154.2 op/s (66.8%) | 220.6 op/s (95.6%) | 230.8 op/s | 311.7 op/s (135%) | 324.0 op/s (140%) |
| DAG GNP dense shfld. | 124.2 op/s (63.7%) | 188.6 op/s (96.7%) | 195.0 op/s | 307.3 op/s (158%) | 324.1 op/s (166%) |

![1.6.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 30.49 ms (1.0×) | 30.46 ms | 15.46 ms (0.5×) | 14.97 ms (0.5×) |
| DAG GNP dense | 103.7 ms (1.0×) | 103.6 ms | — | 37.35 ms (0.4×) |
| DAG GNP dense shfld. | 176.2 ms (1.0×) | 173.5 ms | 39.69 ms (0.2×) | 38.60 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 32.80 op/s (99.9%) | 32.83 op/s | 64.68 op/s (197%) | 66.82 op/s (204%) |
| DAG GNP dense | 9.643 op/s (99.9%) | 9.653 op/s | — | 26.77 op/s (277%) |
| DAG GNP dense shfld. | 5.675 op/s (98.5%) | 5.764 op/s | 25.20 op/s (437%) | 25.90 op/s (449%) |

![1.6.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 1170.9 ms (1.0×) | 1145.1 ms | 160.8 ms (0.1×) | 156.7 ms (0.1×) |
| DAG GNP dense | 1966.4 ms (1.0×) | 1890.5 ms | — | 450.2 ms (0.2×) |
| DAG GNP dense shfld. | 2823.5 ms (1.0×) | 2805.5 ms | 414.3 ms (0.1×) | 405.1 ms (0.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 0.854 op/s (97.8%) | 0.873 op/s | 6.219 op/s (712%) | 6.382 op/s (731%) |
| DAG GNP dense | 0.509 op/s (96.1%) | 0.529 op/s | — | 2.221 op/s (420%) |
| DAG GNP dense shfld. | 0.354 op/s (99.4%) | 0.356 op/s | 2.414 op/s (677%) | 2.469 op/s (693%) |

![1.6.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 17135 ms (1.0×) | 17065 ms | 1810.4 ms (0.1×) | 1759.8 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 0.058 op/s (99.6%) | 0.059 op/s | 0.552 op/s (943%) | 0.568 op/s (970%) |

![1.6.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-5-toporder-n-100-000-000.svg)

### 1.7 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.040 ms | 0.037 ms (0.9×) | 0.040 ms (1.0×) | 0.041 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.055 ms (1.0×) | 0.056 ms (1.0×) | 0.056 ms | 0.038 ms (0.7×) | 0.040 ms (0.7×) | 0.054 ms (1.0×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.034 ms | 0.030 ms (0.9×) | 0.027 ms (0.8×) | 0.028 ms (0.8×) |
| Tree shfld. | 0.034 ms (1.0×) | 0.031 ms (0.9×) | 0.034 ms | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.030 ms (0.9×) |
| DAG GNP sparse | 0.052 ms (1.0×) | 0.057 ms (1.1×) | 0.052 ms | 0.050 ms (1.0×) | 0.054 ms (1.0×) | 0.042 ms (0.8×) |
| DAG GNP dense | 0.364 ms (1.0×) | 0.358 ms (1.0×) | 0.355 ms | 0.326 ms (0.9×) | 0.324 ms (0.9×) | 0.240 ms (0.7×) |
| DAG GNP dense shfld. | 0.393 ms (1.0×) | 0.396 ms (1.0×) | 0.400 ms | 0.323 ms (0.8×) | 0.321 ms (0.8×) | 0.289 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s (103%) | 25 641 op/s (103%) | 25 000 op/s | 27 027 op/s (108%) | 25 000 op/s (100%) | 24 390 op/s (97.6%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 182 op/s (102%) | 17 857 op/s (100%) | 17 857 op/s | 26 316 op/s (147%) | 25 000 op/s (140%) | 18 519 op/s (104%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 32 258 op/s (110%) | 32 258 op/s (110%) | 29 412 op/s | 33 333 op/s (113%) | 37 037 op/s (126%) | 35 714 op/s (121%) |
| Tree shfld. | 29 412 op/s (100%) | 32 258 op/s (110%) | 29 412 op/s | 32 258 op/s (110%) | 32 258 op/s (110%) | 33 333 op/s (113%) |
| DAG GNP sparse | 19 231 op/s (100%) | 17 544 op/s (91.2%) | 19 231 op/s | 20 000 op/s (104%) | 18 519 op/s (96.3%) | 23 810 op/s (124%) |
| DAG GNP dense | 2 747 op/s (97.5%) | 2 793 op/s (99.2%) | 2 817 op/s | 3 067 op/s (109%) | 3 086 op/s (110%) | 4 167 op/s (148%) |
| DAG GNP dense shfld. | 2 545 op/s (102%) | 2 525 op/s (101%) | 2 500 op/s | 3 096 op/s (124%) | 3 115 op/s (125%) | 3 460 op/s (138%) |

![1.7.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.562 ms (1.2×) | 0.456 ms (1.0×) | 0.457 ms | 0.560 ms (1.2×) | 0.458 ms (1.0×) | 0.348 ms (0.8×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.463 ms (1.0×) | 1.378 ms (0.9×) | 1.459 ms | 0.559 ms (0.4×) | 0.564 ms (0.4×) | 1.253 ms (0.9×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.934 ms (1.0×) | 0.929 ms (1.0×) | 0.927 ms | 0.863 ms (0.9×) | 0.782 ms (0.8×) | 0.839 ms (0.9×) |
| Tree shfld. | 1.048 ms (1.0×) | 0.945 ms (0.9×) | 1.032 ms | 0.829 ms (0.8×) | 0.891 ms (0.9×) | 0.884 ms (0.9×) |
| DAG GNP sparse | 2.167 ms (1.0×) | 2.211 ms (1.0×) | 2.159 ms | 1.974 ms (0.9×) | 1.962 ms (0.9×) | 1.989 ms (0.9×) |
| DAG GNP dense | 5.160 ms (1.0×) | 5.093 ms (1.0×) | 5.070 ms | 4.574 ms (0.9×) | 4.551 ms (0.9×) | 3.777 ms (0.7×) |
| DAG GNP dense shfld. | 5.877 ms (1.0×) | 5.806 ms (1.0×) | 5.763 ms | 4.496 ms (0.8×) | 4.496 ms (0.8×) | 4.487 ms (0.8×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 779 op/s (81.3%) | 2 193 op/s (100%) | 2 188 op/s | 1 786 op/s (81.6%) | 2 183 op/s (99.8%) | 2 874 op/s (131%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 683.5 op/s (99.7%) | 725.7 op/s (106%) | 685.4 op/s | 1 789 op/s (261%) | 1 773 op/s (259%) | 798.1 op/s (116%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 071 op/s (99.3%) | 1 076 op/s (99.8%) | 1 079 op/s | 1 159 op/s (107%) | 1 279 op/s (119%) | 1 192 op/s (110%) |
| Tree shfld. | 954.2 op/s (98.5%) | 1 058 op/s (109%) | 969.0 op/s | 1 206 op/s (124%) | 1 122 op/s (116%) | 1 131 op/s (117%) |
| DAG GNP sparse | 461.5 op/s (99.6%) | 452.3 op/s (97.6%) | 463.2 op/s | 506.6 op/s (109%) | 509.7 op/s (110%) | 502.8 op/s (109%) |
| DAG GNP dense | 193.8 op/s (98.3%) | 196.3 op/s (99.5%) | 197.2 op/s | 218.6 op/s (111%) | 219.7 op/s (111%) | 264.8 op/s (134%) |
| DAG GNP dense shfld. | 170.2 op/s (98.1%) | 172.2 op/s (99.3%) | 173.5 op/s | 222.4 op/s (128%) | 222.4 op/s (128%) | 222.9 op/s (128%) |

![1.7.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 29.60 ms (1.0×) | 29.09 ms | 26.85 ms (0.9×) | 25.49 ms (0.9×) |
| DAG GNP dense | 112.1 ms (1.0×) | 113.1 ms | — | 92.44 ms (0.8×) |
| DAG GNP dense shfld. | 191.1 ms (1.0×) | 190.3 ms | 89.45 ms (0.5×) | 92.20 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 33.78 op/s (98.3%) | 34.38 op/s | 37.24 op/s (108%) | 39.23 op/s (114%) |
| DAG GNP dense | 8.921 op/s (101%) | 8.842 op/s | — | 10.82 op/s (122%) |
| DAG GNP dense shfld. | 5.233 op/s (99.6%) | 5.255 op/s | 11.18 op/s (213%) | 10.85 op/s (206%) |

![1.7.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 856.2 ms (1.0×) | 853.6 ms | 751.9 ms (0.9×) | 759.4 ms (0.9×) |
| DAG GNP dense | 3085.9 ms (1.0×) | 2971.3 ms | — | 2553.0 ms (0.9×) |
| DAG GNP dense shfld. | 4398.5 ms (1.0×) | 4386.9 ms | 2631.0 ms (0.6×) | 2623.6 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 1.168 op/s (99.7%) | 1.172 op/s | 1.330 op/s (114%) | 1.317 op/s (112%) |
| DAG GNP dense | 0.324 op/s (96.3%) | 0.337 op/s | — | 0.392 op/s (116%) |
| DAG GNP dense shfld. | 0.227 op/s (99.7%) | 0.228 op/s | 0.380 op/s (167%) | 0.381 op/s (167%) |

![1.7.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 11384 ms (1.0×) | 11412 ms | 9441.1 ms (0.8×) | 9419.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| DAG GNP sparse | 0.088 op/s (100%) | 0.088 op/s | 0.106 op/s (121%) | 0.106 op/s (121%) |

![1.7.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-5-toporderandlevels-n-100-000-000.svg)

### 1.8 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.047 ms (0.8×) | 0.061 ms (1.0×) | 0.060 ms | 0.061 ms (1.0×) | 0.060 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.0×) | 0.065 ms | 0.051 ms (0.8×) | 0.050 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.223 ms (4.6×) | 0.049 ms (1.0×) | 0.049 ms | 0.048 ms (1.0×) | 0.046 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.207 ms (1.3×) | 0.119 ms (0.7×) | 0.164 ms | 0.090 ms (0.5×) | 0.087 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.797 ms (1.0×) | 0.776 ms (1.0×) | 0.771 ms | 0.647 ms (0.8×) | 0.665 ms (0.9×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.811 ms (1.0×) | 0.779 ms (1.0×) | 0.780 ms | 0.669 ms (0.9×) | 0.652 ms (0.8×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.512 ms (0.9×) | 0.592 ms (1.0×) | 0.582 ms | 0.592 ms (1.0×) | 0.580 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.619 ms (1.0×) | 0.644 ms (1.0×) | 0.642 ms | 0.588 ms (0.9×) | 0.634 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 21 277 op/s (128%) | 16 393 op/s (98.4%) | 16 667 op/s | 16 393 op/s (98.4%) | 16 667 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (112%) | 14 925 op/s (97.0%) | 15 385 op/s | 19 608 op/s (127%) | 20 000 op/s (130%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 484 op/s (22.0%) | 20 408 op/s (100%) | 20 408 op/s | 20 833 op/s (102%) | 21 739 op/s (107%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 831 op/s (79.2%) | 8 403 op/s (138%) | 6 098 op/s | 11 111 op/s (182%) | 11 494 op/s (189%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 255 op/s (96.7%) | 1 289 op/s (99.4%) | 1 297 op/s | 1 546 op/s (119%) | 1 504 op/s (116%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 233 op/s (96.2%) | 1 284 op/s (100%) | 1 282 op/s | 1 495 op/s (117%) | 1 534 op/s (120%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 953 op/s (114%) | 1 689 op/s (98.3%) | 1 718 op/s | 1 689 op/s (98.3%) | 1 724 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 616 op/s (104%) | 1 553 op/s (99.7%) | 1 558 op/s | 1 701 op/s (109%) | 1 577 op/s (101%) |

![1.8.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.507 ms (0.9×) | 0.606 ms (1.1×) | 0.563 ms | 0.605 ms (1.1×) | 0.556 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.351 ms (1.3×) | 0.945 ms (0.9×) | 1.063 ms | 0.510 ms (0.5×) | 0.513 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.871 ms (7.8×) | 1.327 ms (1.0×) | 1.271 ms | 1.083 ms (0.9×) | 0.872 ms (0.7×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.691 ms (1.5×) | 3.177 ms (1.0×) | 3.086 ms | 1.962 ms (0.6×) | 1.890 ms (0.6×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.4×) | 12.91 ms (1.0×) | 12.72 ms | 7.303 ms (0.6×) | 7.055 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.4×) | 12.63 ms (1.0×) | 12.71 ms | 7.353 ms (0.6×) | 7.081 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.563 ms (1.4×) | 6.365 ms (1.0×) | 6.191 ms | 6.292 ms (1.0×) | 6.108 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 13.28 ms (1.2×) | 11.15 ms (1.0×) | 11.15 ms | 6.545 ms (0.6×) | 6.298 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 972 op/s (111%) | 1 650 op/s (92.9%) | 1 776 op/s | 1 653 op/s (93.1%) | 1 799 op/s (101%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 740.2 op/s (78.7%) | 1 058 op/s (112%) | 940.7 op/s | 1 961 op/s (208%) | 1 949 op/s (207%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 101.3 op/s (12.9%) | 753.6 op/s (95.8%) | 786.8 op/s | 923.4 op/s (117%) | 1 147 op/s (146%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 213.2 op/s (65.8%) | 314.8 op/s (97.1%) | 324.0 op/s | 509.7 op/s (157%) | 529.1 op/s (163%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (73.2%) | 77.44 op/s (98.5%) | 78.61 op/s | 136.9 op/s (174%) | 141.7 op/s (180%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (73.1%) | 79.20 op/s (101%) | 78.71 op/s | 136.0 op/s (173%) | 141.2 op/s (179%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 116.8 op/s (72.3%) | 157.1 op/s (97.3%) | 161.5 op/s | 158.9 op/s (98.4%) | 163.7 op/s (101%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 75.31 op/s (84.0%) | 89.65 op/s (100.0%) | 89.69 op/s | 152.8 op/s (170%) | 158.8 op/s (177%) |

![1.8.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 49.47 ms (1.1×) | 45.23 ms | 20.62 ms (0.5×) | 19.54 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 240.2 ms (1.0×) | 242.3 ms | — | 92.69 ms (0.4×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 241.4 ms (1.0×) | 240.9 ms | 94.11 ms (0.4×) | 90.99 ms (0.4×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 75.32 ms (1.0×) | 76.46 ms | — | 76.53 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 20.22 op/s (91.4%) | 22.11 op/s | 48.50 op/s (219%) | 51.17 op/s (231%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.163 op/s (101%) | 4.127 op/s | — | 10.79 op/s (261%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.143 op/s (99.8%) | 4.151 op/s | 10.63 op/s (256%) | 10.99 op/s (265%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 13.28 op/s (102%) | 13.08 op/s | — | 13.07 op/s (99.9%) |

![1.8.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1984.9 ms (1.0×) | 1952.3 ms | 254.0 ms (0.1×) | 244.4 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3708.4 ms (1.0×) | 3722.5 ms | — | 1223.7 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3672.0 ms (0.9×) | 3874.8 ms | 930.6 ms (0.2×) | 1124.4 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 815.0 ms (1.0×) | 839.7 ms | — | 797.1 ms (0.9×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.504 op/s (98.4%) | 0.512 op/s | 3.937 op/s (769%) | 4.092 op/s (799%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.270 op/s (100%) | 0.269 op/s | — | 0.817 op/s (304%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.272 op/s (106%) | 0.258 op/s | 1.075 op/s (416%) | 0.889 op/s (345%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.227 op/s (103%) | 1.191 op/s | — | 1.255 op/s (105%) |

![1.8.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 27110 ms (1.0×) | 26787 ms | 2988.7 ms (0.1×) | 2799.7 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.037 op/s (98.8%) | 0.037 op/s | 0.335 op/s (896%) | 0.357 op/s (957%) |

![1.8.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-5-backedges-n-100-000-000.svg)

### 1.9 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.051 ms (0.8×) | 0.064 ms (1.0×) | 0.063 ms | 0.064 ms (1.0×) | 0.064 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.066 ms (0.9×) | 0.077 ms (1.0×) | 0.077 ms | 0.059 ms (0.8×) | 0.060 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.196 ms (3.7×) | 0.053 ms (1.0×) | 0.053 ms | 0.046 ms (0.9×) | 0.046 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.222 ms (1.2×) | 0.183 ms (1.0×) | 0.186 ms | 0.093 ms (0.5×) | 0.094 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.653 ms (1.2×) | 0.511 ms (1.0×) | 0.530 ms | 0.373 ms (0.7×) | 0.377 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.658 ms (1.3×) | 0.514 ms (1.0×) | 0.525 ms | 0.372 ms (0.7×) | 0.380 ms (0.7×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.375 ms (1.3×) | 0.282 ms (1.0×) | 0.296 ms | 0.285 ms (1.0×) | 0.297 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.499 ms (1.3×) | 0.383 ms (1.0×) | 0.394 ms | 0.276 ms (0.7×) | 0.297 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 19 608 op/s (124%) | 15 625 op/s (98.4%) | 15 873 op/s | 15 625 op/s (98.4%) | 15 625 op/s (98.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 152 op/s (117%) | 12 987 op/s (100.0%) | 12 987 op/s | 16 949 op/s (131%) | 16 667 op/s (128%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 102 op/s (27.0%) | 18 868 op/s (100%) | 18 868 op/s | 21 739 op/s (115%) | 21 739 op/s (115%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 505 op/s (83.8%) | 5 464 op/s (102%) | 5 376 op/s | 10 753 op/s (200%) | 10 638 op/s (198%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 531 op/s (81.2%) | 1 957 op/s (104%) | 1 887 op/s | 2 681 op/s (142%) | 2 653 op/s (141%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 520 op/s (79.8%) | 1 946 op/s (102%) | 1 905 op/s | 2 688 op/s (141%) | 2 632 op/s (138%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 2 667 op/s (78.9%) | 3 546 op/s (105%) | 3 378 op/s | 3 509 op/s (104%) | 3 367 op/s (99.7%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 004 op/s (79.0%) | 2 611 op/s (103%) | 2 538 op/s | 3 623 op/s (143%) | 3 367 op/s (133%) |

![1.9.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.554 ms (0.9×) | 0.647 ms (1.1×) | 0.607 ms | 0.642 ms (1.1×) | 0.592 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.563 ms (1.1×) | 1.365 ms (1.0×) | 1.408 ms | 0.615 ms (0.4×) | 0.629 ms (0.4×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 10.29 ms (7.0×) | 1.288 ms (0.9×) | 1.480 ms | 1.022 ms (0.7×) | 1.066 ms (0.7×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.006 ms (1.4×) | 3.626 ms (1.0×) | 3.587 ms | 1.957 ms (0.5×) | 1.853 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.351 ms (1.4×) | 7.009 ms (1.0×) | 6.926 ms | 4.296 ms (0.6×) | 4.197 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 9.400 ms (1.3×) | 7.103 ms (1.0×) | 6.963 ms | 4.185 ms (0.6×) | 4.193 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.372 ms (1.6×) | 2.812 ms (1.0×) | 2.706 ms | 2.815 ms (1.0×) | 2.689 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.766 ms (1.2×) | 5.303 ms (0.9×) | 5.599 ms | 3.053 ms (0.5×) | 2.982 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 805 op/s (110%) | 1 546 op/s (93.8%) | 1 647 op/s | 1 558 op/s (94.5%) | 1 689 op/s (103%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 639.8 op/s (90.1%) | 732.6 op/s (103%) | 710.2 op/s | 1 626 op/s (229%) | 1 590 op/s (224%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 97.17 op/s (14.4%) | 776.4 op/s (115%) | 675.7 op/s | 978.5 op/s (145%) | 938.1 op/s (139%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 199.8 op/s (71.7%) | 275.8 op/s (98.9%) | 278.8 op/s | 511.0 op/s (183%) | 539.7 op/s (194%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 106.9 op/s (74.1%) | 142.7 op/s (98.8%) | 144.4 op/s | 232.8 op/s (161%) | 238.3 op/s (165%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 106.4 op/s (74.1%) | 140.8 op/s (98.0%) | 143.6 op/s | 238.9 op/s (166%) | 238.5 op/s (166%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 228.7 op/s (61.9%) | 355.6 op/s (96.2%) | 369.5 op/s | 355.2 op/s (96.1%) | 371.9 op/s (101%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 147.8 op/s (82.8%) | 188.6 op/s (106%) | 178.6 op/s | 327.5 op/s (183%) | 335.3 op/s (188%) |

![1.9.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 60.41 ms (1.0×) | 58.24 ms | 23.47 ms (0.4×) | 20.58 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 235.5 ms (1.0×) | 238.8 ms | — | 60.05 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 231.9 ms (1.0×) | 235.5 ms | 60.28 ms (0.3×) | 60.09 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.97 ms (1.0×) | 26.59 ms | — | 26.68 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 16.55 op/s (96.4%) | 17.17 op/s | 42.60 op/s (248%) | 48.59 op/s (283%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.246 op/s (101%) | 4.188 op/s | — | 16.65 op/s (398%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.312 op/s (102%) | 4.246 op/s | 16.59 op/s (391%) | 16.64 op/s (392%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 38.51 op/s (102%) | 37.62 op/s | — | 37.48 op/s (99.6%) |

![1.9.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2629.9 ms (1.1×) | 2492.1 ms | 422.5 ms (0.2×) | 414.7 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4515.8 ms (1.0×) | 4552.4 ms | — | 1515.5 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4526.0 ms (1.0×) | 4492.7 ms | 1383.2 ms (0.3×) | 1393.9 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 276.6 ms (1.0×) | 281.8 ms | — | 275.9 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.380 op/s (94.8%) | 0.401 op/s | 2.367 op/s (590%) | 2.411 op/s (601%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (101%) | 0.220 op/s | — | 0.660 op/s (300%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (99.3%) | 0.223 op/s | 0.723 op/s (325%) | 0.717 op/s (322%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.615 op/s (102%) | 3.549 op/s | — | 3.625 op/s (102%) |

![1.9.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 31792 ms (1.0×) | 31282 ms | 7181.5 ms (0.2×) | 6992.4 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.031 op/s (98.4%) | 0.032 op/s | 0.139 op/s (436%) | 0.143 op/s (447%) |

![1.9.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-5-edgetypes-n-100-000-000.svg)

### 1.10 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.121 ms (1.1×) | 0.110 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.145 ms (1.1×) | 0.133 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.185 ms (2.1×) | 0.087 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.483 ms (1.3×) | 0.386 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.127 ms (1.4×) | 0.821 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.114 ms (1.4×) | 0.812 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.715 ms (1.3×) | 0.532 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.867 ms (1.4×) | 0.612 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 8 264 op/s (90.9%) | 9 091 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 6 897 op/s (91.7%) | 7 519 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 405 op/s (47.0%) | 11 494 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 070 op/s (79.9%) | 2 591 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 887.3 op/s (72.8%) | 1 218 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 897.7 op/s (72.9%) | 1 232 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 399 op/s (74.4%) | 1 880 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 153 op/s (70.6%) | 1 634 op/s |

![1.10.1 Reorder (DFS-preorder) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-1-reorder-dfs-preorder-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.259 ms (1.2×) | 1.033 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.144 ms (1.1×) | 1.929 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.144 ms (1.4×) | 2.215 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 6.609 ms (1.2×) | 5.411 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 15.07 ms (1.4×) | 10.81 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 15.00 ms (1.4×) | 10.87 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 7.332 ms (1.4×) | 5.374 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 11.95 ms (1.4×) | 8.836 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 794.3 op/s (82.0%) | 968.1 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 466.4 op/s (90.0%) | 518.4 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 318.1 op/s (70.5%) | 451.5 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 151.3 op/s (81.9%) | 184.8 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 66.37 op/s (71.7%) | 92.53 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 66.66 op/s (72.5%) | 92.00 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 136.4 op/s (73.3%) | 186.1 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 83.70 op/s (74.0%) | 113.2 op/s |

![1.10.2 Reorder (DFS-preorder) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-2-reorder-dfs-preorder-cost-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 95.36 ms (1.2×) | 79.17 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 414.3 ms (1.3×) | 315.0 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 317.2 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 52.67 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 10.49 op/s (83.0%) | 12.63 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 2.414 op/s (76.0%) | 3.175 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 3.153 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 18.99 op/s |

![1.10.3 Reorder (DFS-preorder) cost @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-3-reorder-dfs-preorder-cost-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4011.1 ms (1.2×) | 3263.5 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 10805 ms (1.4×) | 7613.8 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 7509.7 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 617.1 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.249 op/s (81.4%) | 0.306 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.093 op/s (70.5%) | 0.131 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 0.133 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 1.620 op/s |

![1.10.4 Reorder (DFS-preorder) cost @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-4-reorder-dfs-preorder-cost-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 49550 ms (1.2×) | 41091 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.020 op/s (82.9%) | 0.024 op/s |

![1.10.5 Reorder (DFS-preorder) cost @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-5-reorder-dfs-preorder-cost-n-100-000-000.svg)

### 1.11 BiAdjacency (in-adjacency) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.022 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.018 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.016 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.058 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.546 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.544 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.545 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.548 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 45 455 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 55 556 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 62 500 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 17 241 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 832 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 838 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 835 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 825 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.185 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.238 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.612 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.837 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.148 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 7.216 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.559 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.951 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 405 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 202 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 634 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 195 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 139.9 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 138.6 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 179.9 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 143.9 op/s |

## 2 Memory Footprint

Retained heap in bytes for a fully built graph. Lower is better. Baseline (= 100%): **Graphity 2.1.0**, or the newest version present in a table it is missing from.

### 2.1 Total Graph Size

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Path<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 16.0 MB (100%) | 16.0 MB |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 160 MB (100%) | 160 MB |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 1600 MB (100%) | 1600 MB |

![2.1 · Total Graph Size](fig/synthetic-ubuntu-ryzen-9950x-history/2-1-total-graph-size.svg)

### 2.2 Bytes per Element  `totalBytes / (Nodes + Edges)`

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Path<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 4 B (100%) | 4 B |

![2.2 · Bytes per Element  `totalBytes / (Nodes + Edges)`](fig/synthetic-ubuntu-ryzen-9950x-history/2-2-bytes-per-element-totalbytes-nodes-edges.svg)

## 3 Allocations

Bytes allocated per algorithm invocation (JMH `-prof gc`). Lower is better. Baseline (= 100%): **Graphity 2.1.0**, or the newest version present in a table it is missing from.

### 3.1 Build

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB |

![3.1.1 · Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-1-build-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 534 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 534 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 283 kB (98.1%) | 288 kB (100%) | 288 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 250 kB (67.5%) | 356 kB (96.0%) | 371 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (49.5%) | 507 kB (102%) | 498 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (49.4%) | 507 kB (102%) | 497 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (49.3%) | 505 kB (102%) | 495 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (67.0%) | 350 kB (95.9%) | 364 kB |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 6.0 MB (122%) | 4.9 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (57.8%) | 4.9 MB (100%) | 4.9 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (101%) | 2.8 MB (100%) | 2.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.5 MB (71.1%) | 3.9 MB (110%) | 3.5 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (54.3%) | 5.6 MB (124%) | 4.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (54.3%) | 5.6 MB (124%) | 4.5 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (54.2%) | 5.6 MB (124%) | 4.5 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (70.6%) | 3.3 MB (96.8%) | 3.5 MB |

![3.2.2 · SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-2-scc-n-100-000.svg)

### 3.3 WCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |

![3.3.2 · WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-2-wcc-n-100-000.svg)

### 3.4 CCs

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100.0%) | 200 kB |

![3.4.1 · CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-1-ccs-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |

![3.4.2 · CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-2-ccs-n-100-000.svg)

### 3.5 Condense

**Allocations @ 1k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | — | — | 23.2 kB |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | — | — | 96 B |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | — | — | 760 B |
| DAG GNP sparse<br><sub>1k nodes</sub> | — | — | 66.4 kB |

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | — | — | 250 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | — | — | 97 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | — | — | 7.2 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | — | — | 541 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | — | 2.1 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | — | — | 107 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | — | — | 75.7 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | — | — | 5.9 MB |

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 297 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 297 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB |
| Tree shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB |
| DAG GNP dense<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 3.6 MB (143%) | 2.5 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 2.5 MB (101%) | 2.5 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB |
| Tree shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB |
| DAG GNP dense<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.1.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 257 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 257 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (25.2%) | 10.9 kB (100%) | 10.9 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 228 kB (65.0%) | 336 kB (95.8%) | 351 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (96.9%) | 4.0 MB (99.6%) | 4.0 MB |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 3.2 MB (151%) | 2.1 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 2.1 MB (101%) | 2.1 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 25.3 kB (75.8%) | 33.4 kB (100%) | 33.4 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.6 MB (71.3%) | 4.0 MB (110%) | 3.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (97.7%) | 44.7 MB (99.8%) | 44.8 MB |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 307 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 307 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (86.6%) | 60.8 kB (100%) | 60.8 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 72.7 kB (37.2%) | 181 kB (92.4%) | 196 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (57.4%) | 606 kB (102%) | 596 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (57.4%) | 606 kB (102%) | 596 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (58.2%) | 617 kB (102%) | 607 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (74.2%) | 462 kB (96.9%) | 476 kB |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 3.7 MB (141%) | 2.6 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 2.6 MB (101%) | 2.6 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 525 kB (98.5%) | 533 kB (100.0%) | 533 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 725 kB (41.1%) | 2.1 MB (120%) | 1.8 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (62.1%) | 6.6 MB (120%) | 5.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (62.1%) | 6.6 MB (120%) | 5.5 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (62.8%) | 6.7 MB (119%) | 5.6 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (77.2%) | 4.5 MB (97.5%) | 4.6 MB |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-2-edgetypes-n-100-000.svg)
