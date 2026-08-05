# Graphity Version History — synthetic / ubuntu-ryzen-9950x

Benchmarked versions: `1.0`, `2.0.0`, `2.1.0`, `2.2.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`synthetic-ubuntu-ryzen-9950x-peers.md`](synthetic-ubuntu-ryzen-9950x-peers.md).

```
Profile:      synthetic
Timestamp:    2026-07-30 09:16:36
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

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.2.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 Build

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.082 ms (1.0×) | 0.082 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.082 ms (1.0×) | 0.082 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.092 ms (1.0×) | 0.092 ms (1.0×) | 0.091 ms (1.0×) | 0.092 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.164 ms (1.0×) | 0.162 ms (1.0×) | 0.158 ms (1.0×) | 0.162 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.597 ms (1.0×) | 1.573 ms (1.0×) | 1.533 ms (1.0×) | 1.556 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.572 ms (1.0×) | 1.546 ms (1.0×) | 1.567 ms (1.0×) | 1.584 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.550 ms (1.0×) | 1.588 ms (1.0×) | 1.571 ms (1.0×) | 1.572 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.551 ms (1.0×) | 1.553 ms (1.0×) | 1.605 ms (1.0×) | 1.562 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (98.8%) | 12 195 op/s (100%) | 12 195 op/s (100%) | 12 195 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (98.8%) | 12 195 op/s (100%) | 12 195 op/s (100%) | 12 195 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 870 op/s (100%) | 10 870 op/s (100%) | 10 989 op/s (101%) | 10 870 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 098 op/s (98.8%) | 6 173 op/s (100%) | 6 329 op/s (103%) | 6 173 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 626.2 op/s (97.4%) | 635.7 op/s (98.9%) | 652.3 op/s (102%) | 642.7 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 636.1 op/s (101%) | 646.8 op/s (102%) | 638.2 op/s (101%) | 631.3 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 645.2 op/s (101%) | 629.7 op/s (99.0%) | 636.5 op/s (100%) | 636.1 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 644.7 op/s (101%) | 643.9 op/s (101%) | 623.1 op/s (97.3%) | 640.2 op/s |

![1.1.1 Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-1-build-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.686 ms (1.0×) | 0.680 ms (1.0×) | 0.676 ms (1.0×) | 0.678 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.687 ms (1.0×) | 0.682 ms (1.0×) | 0.681 ms (1.0×) | 0.678 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.093 ms (1.0×) | 1.091 ms (1.0×) | 1.089 ms (1.0×) | 1.085 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.249 ms (0.9×) | 2.257 ms (0.9×) | 2.245 ms (0.9×) | 2.610 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.27 ms (1.1×) | 16.93 ms (1.1×) | 16.62 ms (1.0×) | 15.91 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 16.99 ms (1.0×) | 16.74 ms (1.0×) | 16.59 ms (1.0×) | 16.66 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 16.96 ms (1.0×) | 16.82 ms (1.0×) | 16.91 ms (1.0×) | 16.84 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 17.04 ms (1.1×) | 16.06 ms (1.0×) | 15.93 ms (1.0×) | 15.92 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 458 op/s (98.8%) | 1 471 op/s (99.7%) | 1 479 op/s (100%) | 1 475 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 456 op/s (98.7%) | 1 466 op/s (99.4%) | 1 468 op/s (99.6%) | 1 475 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 914.9 op/s (99.3%) | 916.6 op/s (99.5%) | 918.3 op/s (99.6%) | 921.7 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 444.6 op/s (116%) | 443.1 op/s (116%) | 445.4 op/s (116%) | 383.1 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.90 op/s (92.1%) | 59.06 op/s (94.0%) | 60.18 op/s (95.7%) | 62.85 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 58.85 op/s (98.0%) | 59.74 op/s (99.5%) | 60.29 op/s (100%) | 60.03 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 58.96 op/s (99.3%) | 59.46 op/s (100%) | 59.12 op/s (99.6%) | 59.39 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 58.70 op/s (93.4%) | 62.28 op/s (99.2%) | 62.77 op/s (99.9%) | 62.81 op/s |

![1.1.2 Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-2-build-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 22.97 ms (1.0×) | 22.83 ms (1.0×) | 22.76 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 170.6 ms (0.9×) | 177.0 ms (1.0×) | 181.6 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 167.6 ms (0.9×) | 172.8 ms (1.0×) | 176.7 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 173.2 ms (1.1×) | 161.4 ms (1.0×) | 163.2 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 43.54 op/s (99.1%) | 43.79 op/s (99.7%) | 43.94 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 5.862 op/s (106%) | 5.650 op/s (103%) | 5.507 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 5.967 op/s (105%) | 5.787 op/s (102%) | 5.659 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 5.774 op/s (94.2%) | 6.196 op/s (101%) | 6.127 op/s |

![1.1.3 Build @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-3-build-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 248.6 ms (1.0×) | 240.1 ms (1.0×) | 237.7 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 2052.8 ms (0.9×) | 1923.6 ms (0.8×) | 2294.9 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 1806.0 ms (1.0×) | 2313.7 ms (1.3×) | 1804.1 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1784.0 ms (1.0×) | 1780.5 ms (1.0×) | 1779.7 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4.023 op/s (95.6%) | 4.165 op/s (99.0%) | 4.207 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.487 op/s (112%) | 0.520 op/s (119%) | 0.436 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.554 op/s (99.9%) | 0.432 op/s (78.0%) | 0.554 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 0.561 op/s (99.8%) | 0.562 op/s (100.0%) | 0.562 op/s |

![1.1.4 Build @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-4-build-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 2945.8 ms (1.2×) | 2962.3 ms (1.2×) | 2518.5 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.339 op/s (85.5%) | 0.338 op/s (85.0%) | 0.397 op/s |

![1.1.5 Build @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-5-build-n-100-000-000.svg)

### 1.2 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.106 ms (0.9×) | 0.113 ms (1.0×) | 0.121 ms (1.1×) | 0.113 ms | 0.112 ms (1.0×) | 0.121 ms (1.1×) | 0.113 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.129 ms (1.0×) | 0.134 ms (1.0×) | 0.135 ms (1.0×) | 0.130 ms | 0.108 ms (0.8×) | 0.117 ms (0.9×) | 0.113 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.271 ms (3.4×) | 0.107 ms (1.4×) | 0.082 ms (1.0×) | 0.079 ms | 0.095 ms (1.2×) | 0.077 ms (1.0×) | 0.074 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.245 ms (1.0×) | 0.239 ms (1.0×) | 0.237 ms (1.0×) | 0.248 ms | 0.111 ms (0.4×) | 0.112 ms (0.5×) | 0.109 ms (0.4×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.3×) | 0.539 ms (1.0×) | 0.539 ms (1.0×) | 0.541 ms | 0.378 ms (0.7×) | 0.386 ms (0.7×) | 0.382 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.3×) | 0.531 ms (0.9×) | 0.539 ms (0.9×) | 0.572 ms | 0.384 ms (0.7×) | 0.377 ms (0.7×) | 0.372 ms (0.7×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.563 ms (2.1×) | 0.260 ms (1.0×) | 0.256 ms (1.0×) | 0.267 ms | 0.259 ms (1.0×) | 0.257 ms (1.0×) | 0.264 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.8×) | 0.338 ms (1.0×) | 0.347 ms (1.0×) | 0.353 ms | 0.250 ms (0.7×) | 0.255 ms (0.7×) | 0.274 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 434 op/s (107%) | 8 850 op/s (100%) | 8 264 op/s (93.4%) | 8 850 op/s | 8 929 op/s (101%) | 8 264 op/s (93.4%) | 8 850 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 752 op/s (101%) | 7 463 op/s (97.0%) | 7 407 op/s (96.3%) | 7 692 op/s | 9 259 op/s (120%) | 8 547 op/s (111%) | 8 850 op/s (115%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 3 690 op/s (29.2%) | 9 346 op/s (73.8%) | 12 195 op/s (96.3%) | 12 658 op/s | 10 526 op/s (83.2%) | 12 987 op/s (103%) | 13 514 op/s (107%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 082 op/s (101%) | 4 184 op/s (104%) | 4 219 op/s (105%) | 4 032 op/s | 9 009 op/s (223%) | 8 929 op/s (221%) | 9 174 op/s (228%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (74.1%) | 1 855 op/s (100%) | 1 855 op/s (100%) | 1 848 op/s | 2 646 op/s (143%) | 2 591 op/s (140%) | 2 618 op/s (142%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (78.4%) | 1 883 op/s (108%) | 1 855 op/s (106%) | 1 748 op/s | 2 604 op/s (149%) | 2 653 op/s (152%) | 2 688 op/s (154%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 776 op/s (47.4%) | 3 846 op/s (103%) | 3 906 op/s (104%) | 3 745 op/s | 3 861 op/s (103%) | 3 891 op/s (104%) | 3 788 op/s (101%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (55.2%) | 2 959 op/s (104%) | 2 882 op/s (102%) | 2 833 op/s | 4 000 op/s (141%) | 3 922 op/s (138%) | 3 650 op/s (129%) |

![1.2.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 1.130 ms (1.0×) | 1.208 ms (1.1×) | 1.098 ms | 1.117 ms (1.0×) | 1.209 ms (1.1×) | 1.119 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.415 ms (1.1×) | 2.192 ms (1.0×) | 2.121 ms (1.0×) | 2.158 ms | 1.038 ms (0.5×) | 1.130 ms (0.5×) | 1.092 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 11.69 ms (6.5×) | 2.240 ms (1.2×) | 1.805 ms (1.0×) | 1.807 ms | 1.413 ms (0.8×) | 1.171 ms (0.6×) | 1.111 ms (0.6×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.872 ms (1.1×) | 4.329 ms (1.0×) | 4.339 ms (1.0×) | 4.322 ms | 2.247 ms (0.5×) | 2.278 ms (0.5×) | 2.240 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.957 ms (1.4×) | 7.389 ms (1.0×) | 7.472 ms (1.0×) | 7.241 ms | 4.475 ms (0.6×) | 4.449 ms (0.6×) | 4.356 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 7.595 ms (1.0×) | 7.329 ms (1.0×) | 7.346 ms | 4.423 ms (0.6×) | 4.451 ms (0.6×) | 4.502 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.702 ms (2.0×) | 2.623 ms (0.9×) | 2.708 ms (0.9×) | 2.920 ms | 2.625 ms (0.9×) | 2.638 ms (0.9×) | 2.648 ms (0.9×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 7.381 ms (1.6×) | 4.697 ms (1.0×) | 4.711 ms (1.0×) | 4.717 ms | 2.542 ms (0.5×) | 2.536 ms (0.5×) | 2.733 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 885.0 op/s (97.2%) | 827.8 op/s (90.9%) | 910.7 op/s | 895.3 op/s (98.3%) | 827.1 op/s (90.8%) | 893.7 op/s (98.1%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 414.1 op/s (89.4%) | 456.2 op/s (98.4%) | 471.5 op/s (102%) | 463.4 op/s | 963.4 op/s (208%) | 885.0 op/s (191%) | 915.8 op/s (198%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 85.56 op/s (15.5%) | 446.4 op/s (80.7%) | 554.0 op/s (100%) | 553.4 op/s | 707.7 op/s (128%) | 854.0 op/s (154%) | 900.1 op/s (163%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 205.3 op/s (88.7%) | 231.0 op/s (99.8%) | 230.5 op/s (99.6%) | 231.4 op/s | 445.0 op/s (192%) | 439.0 op/s (190%) | 446.4 op/s (193%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 100.4 op/s (72.7%) | 135.3 op/s (98.0%) | 133.8 op/s (96.9%) | 138.1 op/s | 223.5 op/s (162%) | 224.8 op/s (163%) | 229.6 op/s (166%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 131.7 op/s (96.7%) | 136.4 op/s (100%) | 136.1 op/s | 226.1 op/s (166%) | 224.7 op/s (165%) | 222.1 op/s (163%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 175.4 op/s (51.2%) | 381.2 op/s (111%) | 369.3 op/s (108%) | 342.5 op/s | 381.0 op/s (111%) | 379.1 op/s (111%) | 377.6 op/s (110%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 135.5 op/s (63.9%) | 212.9 op/s (100%) | 212.3 op/s (100%) | 212.0 op/s | 393.4 op/s (186%) | 394.3 op/s (186%) | 365.9 op/s (173%) |

![1.2.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 77.49 ms (1.0×) | 79.07 ms (1.1×) | 74.83 ms | 26.75 ms (0.4×) | 27.79 ms (0.4×) | 25.24 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 238.7 ms (1.0×) | 234.5 ms (1.0×) | 234.8 ms | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 232.9 ms (1.0×) | 243.4 ms (1.0×) | 232.3 ms | 66.11 ms (0.3×) | 63.15 ms (0.3×) | 65.01 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.36 ms (1.0×) | 25.98 ms (1.0×) | 26.13 ms | — | — | — |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 12.91 op/s (96.6%) | 12.65 op/s (94.6%) | 13.36 op/s | 37.38 op/s (280%) | 35.99 op/s (269%) | 39.62 op/s (296%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.189 op/s (98.4%) | 4.264 op/s (100%) | 4.259 op/s | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.294 op/s (99.7%) | 4.108 op/s (95.4%) | 4.305 op/s | 15.13 op/s (351%) | 15.84 op/s (368%) | 15.38 op/s (357%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 39.43 op/s (103%) | 38.49 op/s (101%) | 38.27 op/s | — | — | — |

![1.2.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3085.1 ms (1.0×) | 3110.1 ms (1.0×) | 3038.1 ms | 739.3 ms (0.2×) | 737.5 ms (0.2×) | 718.3 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6301.4 ms (1.0×) | 6254.7 ms (1.0×) | 6535.4 ms | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 6297.6 ms (1.0×) | 6268.0 ms (1.0×) | 6472.2 ms | 2740.8 ms (0.4×) | 2992.0 ms (0.5×) | 2718.8 ms (0.4×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 268.4 ms (1.0×) | 272.4 ms (1.0×) | 260.5 ms | — | — | — |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.324 op/s (98.5%) | 0.322 op/s (97.7%) | 0.329 op/s | 1.353 op/s (411%) | 1.356 op/s (412%) | 1.392 op/s (423%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (104%) | 0.160 op/s (104%) | 0.153 op/s | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (103%) | 0.160 op/s (103%) | 0.155 op/s | 0.365 op/s (236%) | 0.334 op/s (216%) | 0.368 op/s (238%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.726 op/s (97.1%) | 3.671 op/s (95.6%) | 3.839 op/s | — | — | — |

![1.2.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 36814 ms (1.0×) | 36685 ms (1.0×) | 36204 ms | 9963.5 ms (0.3×) | 9992.3 ms (0.3×) | 9976.7 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.027 op/s (98.3%) | 0.027 op/s (98.7%) | 0.028 op/s | 0.100 op/s (363%) | 0.100 op/s (362%) | 0.100 op/s (363%) |

![1.2.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-5-scc-n-100-000-000.svg)

### 1.3 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (0.9×) | 0.036 ms (0.8×) | 0.036 ms (0.8×) | 0.043 ms | 0.042 ms (1.0×) | 0.036 ms (0.8×) | 0.036 ms (0.8×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.046 ms (1.0×) | 0.046 ms (1.0×) | 0.057 ms (1.3×) | 0.045 ms | 0.042 ms (0.9×) | 0.042 ms (0.9×) | 0.036 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.038 ms (0.8×) | 0.038 ms (0.8×) | 0.038 ms (0.8×) | 0.046 ms | 0.038 ms (0.8×) | 0.038 ms (0.8×) | 0.038 ms (0.8×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.095 ms (1.0×) | 0.083 ms (0.9×) | 0.093 ms (1.0×) | 0.096 ms | 0.086 ms (0.9×) | 0.079 ms (0.8×) | 0.082 ms (0.9×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.530 ms (1.0×) | 0.524 ms (1.0×) | 0.523 ms (1.0×) | 0.522 ms | 0.524 ms (1.0×) | 0.525 ms (1.0×) | 0.522 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms (1.0×) | 0.524 ms (1.0×) | 0.524 ms (1.0×) | 0.524 ms | 0.527 ms (1.0×) | 0.522 ms (1.0×) | 0.521 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.587 ms | 0.578 ms (1.0×) | 0.580 ms (1.0×) | 0.585 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.640 ms (1.0×) | 0.640 ms (1.0×) | 0.640 ms (1.0×) | 0.635 ms | 0.581 ms (0.9×) | 0.610 ms (1.0×) | 0.615 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (116%) | 27 778 op/s (119%) | 27 778 op/s (119%) | 23 256 op/s | 23 810 op/s (102%) | 27 778 op/s (119%) | 27 778 op/s (119%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 739 op/s (97.8%) | 21 739 op/s (97.8%) | 17 544 op/s (78.9%) | 22 222 op/s | 23 810 op/s (107%) | 23 810 op/s (107%) | 27 778 op/s (125%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s (121%) | 26 316 op/s (121%) | 26 316 op/s (121%) | 21 739 op/s | 26 316 op/s (121%) | 26 316 op/s (121%) | 26 316 op/s (121%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 10 526 op/s (101%) | 12 048 op/s (116%) | 10 753 op/s (103%) | 10 417 op/s | 11 628 op/s (112%) | 12 658 op/s (122%) | 12 195 op/s (117%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 887 op/s (98.5%) | 1 908 op/s (99.6%) | 1 912 op/s (99.8%) | 1 916 op/s | 1 908 op/s (99.6%) | 1 905 op/s (99.4%) | 1 916 op/s (100%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s (99.8%) | 1 908 op/s (100%) | 1 908 op/s (100%) | 1 908 op/s | 1 898 op/s (99.4%) | 1 916 op/s (100%) | 1 919 op/s (101%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 730 op/s (102%) | 1 730 op/s (102%) | 1 730 op/s (102%) | 1 704 op/s | 1 730 op/s (102%) | 1 724 op/s (101%) | 1 709 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 562 op/s (99.2%) | 1 562 op/s (99.2%) | 1 562 op/s (99.2%) | 1 575 op/s | 1 721 op/s (109%) | 1 639 op/s (104%) | 1 626 op/s (103%) |

![1.3.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.393 ms (1.0×) | 0.398 ms (1.0×) | 0.392 ms (1.0×) | 0.389 ms | 0.400 ms (1.0×) | 0.392 ms (1.0×) | 0.392 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (0.9×) | 1.228 ms (0.9×) | 1.267 ms (1.0×) | 1.294 ms | 0.390 ms (0.3×) | 0.391 ms (0.3×) | 0.366 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.661 ms (1.0×) | 0.636 ms (1.0×) | 0.663 ms (1.0×) | 0.642 ms | 0.732 ms (1.1×) | 0.729 ms (1.1×) | 0.735 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.769 ms (1.0×) | 1.767 ms (1.0×) | 1.753 ms (1.0×) | 1.757 ms | 1.550 ms (0.9×) | 1.533 ms (0.9×) | 1.552 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.082 ms (1.0×) | 6.090 ms (1.0×) | 6.078 ms (1.0×) | 6.083 ms | 6.069 ms (1.0×) | 6.065 ms (1.0×) | 6.037 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.163 ms (1.0×) | 6.091 ms (1.0×) | 6.067 ms (1.0×) | 6.048 ms | 6.163 ms (1.0×) | 6.053 ms (1.0×) | 6.112 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.858 ms (1.0×) | 6.000 ms (1.0×) | 5.843 ms (1.0×) | 5.756 ms | 5.927 ms (1.0×) | 5.834 ms (1.0×) | 6.002 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.955 ms (1.0×) | 5.953 ms (1.0×) | 5.998 ms (1.0×) | 5.903 ms | 6.021 ms (1.0×) | 6.071 ms (1.0×) | 6.117 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 545 op/s (99.0%) | 2 513 op/s (97.7%) | 2 551 op/s (99.2%) | 2 571 op/s | 2 500 op/s (97.2%) | 2 551 op/s (99.2%) | 2 551 op/s (99.2%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (105%) | 814.3 op/s (105%) | 789.3 op/s (102%) | 772.8 op/s | 2 564 op/s (332%) | 2 558 op/s (331%) | 2 732 op/s (354%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 513 op/s (97.1%) | 1 572 op/s (101%) | 1 508 op/s (96.8%) | 1 558 op/s | 1 366 op/s (87.7%) | 1 372 op/s (88.1%) | 1 361 op/s (87.3%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 565.3 op/s (99.3%) | 565.9 op/s (99.4%) | 570.5 op/s (100%) | 569.2 op/s | 645.2 op/s (113%) | 652.3 op/s (115%) | 644.3 op/s (113%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 164.4 op/s (100%) | 164.2 op/s (99.9%) | 164.5 op/s (100%) | 164.4 op/s | 164.8 op/s (100%) | 164.9 op/s (100%) | 165.6 op/s (101%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.3 op/s (98.1%) | 164.2 op/s (99.3%) | 164.8 op/s (99.7%) | 165.3 op/s | 162.3 op/s (98.1%) | 165.2 op/s (99.9%) | 163.6 op/s (99.0%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 170.7 op/s (98.3%) | 166.7 op/s (95.9%) | 171.1 op/s (98.5%) | 173.7 op/s | 168.7 op/s (97.1%) | 171.4 op/s (98.7%) | 166.6 op/s (95.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 167.9 op/s (99.1%) | 168.0 op/s (99.2%) | 166.7 op/s (98.4%) | 169.4 op/s | 166.1 op/s (98.0%) | 164.7 op/s (97.2%) | 163.5 op/s (96.5%) |

![1.3.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.74 ms (1.1×) | 21.51 ms (1.1×) | 19.61 ms | 17.26 ms (0.9×) | 17.48 ms (0.9×) | 16.15 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 65.50 ms (1.0×) | 79.32 ms (1.2×) | 66.47 ms | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 70.47 ms (1.0×) | 68.11 ms (1.0×) | 67.62 ms | 69.61 ms (1.0×) | 65.94 ms (1.0×) | 66.59 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.95 ms (1.0×) | 52.40 ms (1.0×) | 49.96 ms | — | — | — |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.00 op/s (90.2%) | 46.48 op/s (91.1%) | 51.01 op/s | 57.94 op/s (114%) | 57.21 op/s (112%) | 61.91 op/s (121%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 15.27 op/s (101%) | 12.61 op/s (83.8%) | 15.04 op/s | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.19 op/s (96.0%) | 14.68 op/s (99.3%) | 14.79 op/s | 14.37 op/s (97.1%) | 15.16 op/s (103%) | 15.02 op/s (102%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.63 op/s (98.0%) | 19.09 op/s (95.3%) | 20.02 op/s | — | — | — |

![1.3.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 615.6 ms (1.1×) | 609.6 ms (1.1×) | 539.1 ms | 410.6 ms (0.8×) | 406.7 ms (0.8×) | 405.0 ms (0.8×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4094.7 ms (1.0×) | 4138.0 ms (1.0×) | 4129.9 ms | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3899.0 ms (1.0×) | 4062.9 ms (1.0×) | 3913.6 ms | 3844.9 ms (1.0×) | 3752.3 ms (1.0×) | 3705.6 ms (0.9×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 754.7 ms (1.3×) | 594.2 ms (1.0×) | 589.2 ms | — | — | — |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.624 op/s (87.6%) | 1.640 op/s (88.4%) | 1.855 op/s | 2.435 op/s (131%) | 2.459 op/s (133%) | 2.469 op/s (133%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.244 op/s (101%) | 0.242 op/s (99.8%) | 0.242 op/s | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s (100%) | 0.246 op/s (96.3%) | 0.256 op/s | 0.260 op/s (102%) | 0.267 op/s (104%) | 0.270 op/s (106%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.325 op/s (78.1%) | 1.683 op/s (99.2%) | 1.697 op/s | — | — | — |

![1.3.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8231.6 ms (1.0×) | 9465.6 ms (1.1×) | 8506.2 ms | 6807.7 ms (0.8×) | 6849.6 ms (0.8×) | 6819.8 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.121 op/s (103%) | 0.106 op/s (89.9%) | 0.118 op/s | 0.147 op/s (125%) | 0.146 op/s (124%) | 0.147 op/s (125%) |

![1.3.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-5-wcc-n-100-000-000.svg)

### 1.4 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (1.0×) | 0.042 ms (1.2×) | 0.037 ms (1.0×) | 0.036 ms | 0.036 ms (1.0×) | 0.036 ms (1.0×) | 0.036 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.053 ms (0.9×) | 0.053 ms (0.9×) | 0.046 ms (0.8×) | 0.057 ms | 0.036 ms (0.6×) | 0.042 ms (0.7×) | 0.042 ms (0.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.044 ms (1.2×) | 0.038 ms (1.0×) | 0.038 ms (1.0×) | 0.038 ms | 0.043 ms (1.1×) | 0.041 ms (1.1×) | 0.043 ms (1.1×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.106 ms (1.0×) | 0.090 ms (0.9×) | 0.092 ms (0.9×) | 0.102 ms | 0.088 ms (0.9×) | 0.084 ms (0.8×) | 0.086 ms (0.8×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.518 ms (1.0×) | 0.526 ms (1.0×) | 0.523 ms (1.0×) | 0.522 ms | 0.520 ms (1.0×) | 0.529 ms (1.0×) | 0.519 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.522 ms (1.0×) | 0.524 ms (1.0×) | 0.525 ms (1.0×) | 0.527 ms | 0.534 ms (1.0×) | 0.522 ms (1.0×) | 0.521 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.586 ms (1.0×) | 0.580 ms (1.0×) | 0.578 ms (1.0×) | 0.578 ms | 0.584 ms (1.0×) | 0.578 ms (1.0×) | 0.596 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.0×) | 0.640 ms (1.0×) | 0.645 ms (1.0×) | 0.638 ms | 0.625 ms (1.0×) | 0.613 ms (1.0×) | 0.609 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (97.3%) | 23 810 op/s (85.7%) | 27 027 op/s (97.3%) | 27 778 op/s | 27 778 op/s (100%) | 27 778 op/s (100%) | 27 778 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s (108%) | 18 868 op/s (108%) | 21 739 op/s (124%) | 17 544 op/s | 27 778 op/s (158%) | 23 810 op/s (136%) | 23 810 op/s (136%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 22 727 op/s (86.4%) | 26 316 op/s (100%) | 26 316 op/s (100%) | 26 316 op/s | 23 256 op/s (88.4%) | 24 390 op/s (92.7%) | 23 256 op/s (88.4%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 9 434 op/s (96.2%) | 11 111 op/s (113%) | 10 870 op/s (111%) | 9 804 op/s | 11 364 op/s (116%) | 11 905 op/s (121%) | 11 628 op/s (119%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 931 op/s (101%) | 1 901 op/s (99.2%) | 1 912 op/s (99.8%) | 1 916 op/s | 1 923 op/s (100%) | 1 890 op/s (98.7%) | 1 927 op/s (101%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 916 op/s (101%) | 1 908 op/s (101%) | 1 905 op/s (100%) | 1 898 op/s | 1 873 op/s (98.7%) | 1 916 op/s (101%) | 1 919 op/s (101%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 706 op/s (98.6%) | 1 724 op/s (99.7%) | 1 730 op/s (100%) | 1 730 op/s | 1 712 op/s (99.0%) | 1 730 op/s (100%) | 1 678 op/s (97.0%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (99.8%) | 1 562 op/s (99.7%) | 1 550 op/s (98.9%) | 1 567 op/s | 1 600 op/s (102%) | 1 631 op/s (104%) | 1 642 op/s (105%) |

![1.4.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.396 ms (1.0×) | 0.403 ms (1.0×) | 0.393 ms (1.0×) | 0.399 ms | 0.392 ms (1.0×) | 0.392 ms (1.0×) | 0.356 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.326 ms (1.1×) | 1.276 ms (1.1×) | 1.213 ms | 0.369 ms (0.3×) | 0.359 ms (0.3×) | 0.359 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.665 ms (1.1×) | 0.649 ms (1.0×) | 0.637 ms (1.0×) | 0.630 ms | 0.728 ms (1.2×) | 0.715 ms (1.1×) | 0.789 ms (1.3×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.762 ms (1.0×) | 1.769 ms (1.0×) | 1.854 ms (1.1×) | 1.756 ms | 1.501 ms (0.9×) | 1.542 ms (0.9×) | 1.677 ms (1.0×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.173 ms (1.0×) | 6.095 ms (1.0×) | 6.056 ms (1.0×) | 6.065 ms | 6.057 ms (1.0×) | 6.031 ms (1.0×) | 6.053 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.157 ms (1.0×) | 6.186 ms (1.0×) | 6.064 ms (1.0×) | 6.141 ms | 6.029 ms (1.0×) | 6.023 ms (1.0×) | 6.191 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.806 ms (1.0×) | 5.871 ms (1.0×) | 5.853 ms (1.0×) | 5.839 ms | 5.856 ms (1.0×) | 6.314 ms (1.1×) | 5.790 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.999 ms (1.0×) | 5.949 ms (1.0×) | 5.916 ms (1.0×) | 5.968 ms | 6.055 ms (1.0×) | 6.129 ms (1.0×) | 6.131 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 525 op/s (101%) | 2 481 op/s (99.0%) | 2 545 op/s (102%) | 2 506 op/s | 2 551 op/s (102%) | 2 551 op/s (102%) | 2 809 op/s (112%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (98.7%) | 754.1 op/s (91.5%) | 783.7 op/s (95.1%) | 824.4 op/s | 2 710 op/s (329%) | 2 786 op/s (338%) | 2 786 op/s (338%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 504 op/s (94.7%) | 1 541 op/s (97.1%) | 1 570 op/s (98.9%) | 1 587 op/s | 1 374 op/s (86.5%) | 1 399 op/s (88.1%) | 1 267 op/s (79.8%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 567.5 op/s (99.7%) | 565.3 op/s (99.3%) | 539.4 op/s (94.7%) | 569.5 op/s | 666.2 op/s (117%) | 648.5 op/s (114%) | 596.3 op/s (105%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 162.0 op/s (98.3%) | 164.1 op/s (99.5%) | 165.1 op/s (100%) | 164.9 op/s | 165.1 op/s (100%) | 165.8 op/s (101%) | 165.2 op/s (100%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.4 op/s (99.7%) | 161.7 op/s (99.3%) | 164.9 op/s (101%) | 162.8 op/s | 165.9 op/s (102%) | 166.0 op/s (102%) | 161.5 op/s (99.2%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 172.2 op/s (101%) | 170.3 op/s (99.5%) | 170.9 op/s (99.8%) | 171.3 op/s | 170.8 op/s (99.7%) | 158.4 op/s (92.5%) | 172.7 op/s (101%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 166.7 op/s (99.5%) | 168.1 op/s (100%) | 169.0 op/s (101%) | 167.6 op/s | 165.2 op/s (98.6%) | 163.2 op/s (97.4%) | 163.1 op/s (97.3%) |

![1.4.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 19.83 ms (1.0×) | 19.67 ms (1.0×) | 19.82 ms | 17.28 ms (0.9×) | 16.68 ms (0.8×) | 17.22 ms (0.9×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 79.11 ms (1.2×) | 67.61 ms (1.0×) | 65.32 ms | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 67.63 ms (1.0×) | 66.69 ms (1.0×) | 68.51 ms | 66.60 ms (1.0×) | 67.72 ms (1.0×) | 66.55 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.35 ms (1.0×) | 50.48 ms (1.0×) | 52.13 ms | — | — | — |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 50.44 op/s (100.0%) | 50.84 op/s (101%) | 50.45 op/s | 57.86 op/s (115%) | 59.94 op/s (119%) | 58.07 op/s (115%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 12.64 op/s (82.6%) | 14.79 op/s (96.6%) | 15.31 op/s | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.79 op/s (101%) | 14.99 op/s (103%) | 14.60 op/s | 15.02 op/s (103%) | 14.77 op/s (101%) | 15.03 op/s (103%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.86 op/s (104%) | 19.81 op/s (103%) | 19.18 op/s | — | — | — |

![1.4.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 576.6 ms (1.0×) | 575.0 ms (1.0×) | 600.1 ms | 410.4 ms (0.7×) | 406.3 ms (0.7×) | 412.5 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4122.2 ms (1.0×) | 3954.9 ms (1.0×) | 4066.2 ms | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3909.4 ms (1.0×) | 4062.1 ms (1.0×) | 3914.0 ms | 4002.5 ms (1.0×) | 3895.3 ms (1.0×) | 3905.4 ms (1.0×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 569.9 ms (1.0×) | 607.6 ms (1.0×) | 594.7 ms | — | — | — |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.734 op/s (104%) | 1.739 op/s (104%) | 1.666 op/s | 2.437 op/s (146%) | 2.461 op/s (148%) | 2.424 op/s (145%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.243 op/s (98.6%) | 0.253 op/s (103%) | 0.246 op/s | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s (100%) | 0.246 op/s (96.4%) | 0.255 op/s | 0.250 op/s (97.8%) | 0.257 op/s (100%) | 0.256 op/s (100%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.755 op/s (104%) | 1.646 op/s (97.9%) | 1.682 op/s | — | — | — |

![1.4.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8479.6 ms (1.0×) | 8559.5 ms (1.0×) | 8533.7 ms | 6840.6 ms (0.8×) | 6829.3 ms (0.8×) | 5994.7 ms (0.7×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.118 op/s (101%) | 0.117 op/s (99.7%) | 0.117 op/s | 0.146 op/s (125%) | 0.146 op/s (125%) | 0.167 op/s (142%) |

![1.4.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-5-ccs-n-100-000-000.svg)

### 1.5 Condense

#### Times @ 1k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 0.010 ms (1.0×) | 0.010 ms |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 0.007 ms (1.0×) | 0.007 ms |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 0.024 ms (1.0×) | 0.024 ms |
| DAG GNP sparse | 0.017 ms (1.0×) | 0.017 ms |

#### Throughput @ 1k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 100 000 op/s (100%) | 100 000 op/s |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 142 857 op/s (100%) | 142 857 op/s |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 41 667 op/s (100%) | 41 667 op/s |
| DAG GNP sparse | 58 824 op/s (100%) | 58 824 op/s |

![1.5.1 Condense @ n = 1 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-1-condense-n-1-000.svg)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.080 ms (1.0×) | 0.080 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.094 ms (1.1×) | 0.082 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.182 ms (1.0×) | 0.181 ms |
| DAG GNP sparse | 0.259 ms (1.0×) | 0.257 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 500 op/s (100%) | 12 500 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 10 638 op/s (87.2%) | 12 195 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 5 495 op/s (99.5%) | 5 525 op/s |
| DAG GNP sparse | 3 861 op/s (99.2%) | 3 891 op/s |

![1.5.2 Condense @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-2-condense-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.763 ms (1.0×) | 0.765 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.522 ms (1.0×) | 1.525 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1.714 ms (1.0×) | 1.704 ms |
| DAG GNP sparse | 3.555 ms (1.0×) | 3.535 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 311 op/s (100%) | 1 307 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 657.0 op/s (100%) | 655.7 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 583.4 op/s (99.4%) | 586.9 op/s |
| DAG GNP sparse | 281.3 op/s (99.4%) | 282.9 op/s |

![1.5.3 Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-3-condense-n-100-000.svg)

### 1.6 TopOrder

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.8×) | 0.067 ms (0.9×) | 0.085 ms (1.1×) | 0.074 ms | 0.067 ms (0.9×) | 0.079 ms (1.1×) | 0.074 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.099 ms (1.4×) | 0.072 ms (1.0×) | 0.073 ms (1.0×) | 0.072 ms | 0.056 ms (0.8×) | 0.063 ms (0.9×) | 0.058 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.198 ms (3.2×) | 0.044 ms (0.7×) | 0.061 ms (1.0×) | 0.061 ms | 0.039 ms (0.6×) | 0.042 ms (0.7×) | 0.057 ms (0.9×) |
| Tree shfld. | 0.088 ms (1.7×) | 0.052 ms (1.0×) | 0.052 ms (1.0×) | 0.051 ms | 0.047 ms (0.9×) | 0.048 ms (0.9×) | 0.046 ms (0.9×) |
| DAG GNP sparse | 0.279 ms (3.2×) | 0.070 ms (0.8×) | 0.084 ms (1.0×) | 0.086 ms | 0.061 ms (0.7×) | 0.072 ms (0.8×) | 0.077 ms (0.9×) |
| DAG GNP dense | 0.448 ms (1.4×) | 0.337 ms (1.0×) | 0.327 ms (1.0×) | 0.327 ms | 0.245 ms (0.7×) | 0.264 ms (0.8×) | 0.279 ms (0.9×) |
| DAG GNP dense shfld. | 0.560 ms (1.6×) | 0.387 ms (1.1×) | 0.351 ms (1.0×) | 0.349 ms | 0.244 ms (0.7×) | 0.263 ms (0.8×) | 0.276 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (128%) | 14 925 op/s (110%) | 11 765 op/s (87.1%) | 13 514 op/s | 14 925 op/s (110%) | 12 658 op/s (93.7%) | 13 514 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 10 101 op/s (72.7%) | 13 889 op/s (100%) | 13 699 op/s (98.6%) | 13 889 op/s | 17 857 op/s (129%) | 15 873 op/s (114%) | 17 241 op/s (124%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 051 op/s (30.8%) | 22 727 op/s (139%) | 16 393 op/s (100%) | 16 393 op/s | 25 641 op/s (156%) | 23 810 op/s (145%) | 17 544 op/s (107%) |
| Tree shfld. | 11 364 op/s (58.0%) | 19 231 op/s (98.1%) | 19 231 op/s (98.1%) | 19 608 op/s | 21 277 op/s (109%) | 20 833 op/s (106%) | 21 739 op/s (111%) |
| DAG GNP sparse | 3 584 op/s (30.8%) | 14 286 op/s (123%) | 11 905 op/s (102%) | 11 628 op/s | 16 393 op/s (141%) | 13 889 op/s (119%) | 12 987 op/s (112%) |
| DAG GNP dense | 2 232 op/s (73.0%) | 2 967 op/s (97.0%) | 3 058 op/s (100%) | 3 058 op/s | 4 082 op/s (133%) | 3 788 op/s (124%) | 3 584 op/s (117%) |
| DAG GNP dense shfld. | 1 786 op/s (62.3%) | 2 584 op/s (90.2%) | 2 849 op/s (99.4%) | 2 865 op/s | 4 098 op/s (143%) | 3 802 op/s (133%) | 3 623 op/s (126%) |

![1.6.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.539 ms (0.8×) | 0.656 ms (1.0×) | 0.777 ms (1.1×) | 0.686 ms | 0.662 ms (1.0×) | 0.779 ms (1.1×) | 0.708 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.287 ms (1.2×) | 1.051 ms (1.0×) | 1.040 ms (1.0×) | 1.048 ms | 0.566 ms (0.5×) | 0.581 ms (0.6×) | 0.571 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.963 ms (8.1×) | 1.311 ms (1.1×) | 1.399 ms (1.1×) | 1.229 ms | 1.023 ms (0.8×) | 0.992 ms (0.8×) | 0.979 ms (0.8×) |
| Tree shfld. | 5.091 ms (3.7×) | 1.487 ms (1.1×) | 1.371 ms (1.0×) | 1.376 ms | 1.212 ms (0.9×) | 1.149 ms (0.8×) | 1.146 ms (0.8×) |
| DAG GNP sparse | 9.521 ms (4.0×) | 2.251 ms (0.9×) | 2.399 ms (1.0×) | 2.403 ms | 1.480 ms (0.6×) | 1.599 ms (0.7×) | 1.605 ms (0.7×) |
| DAG GNP dense | 6.487 ms (1.5×) | 4.533 ms (1.1×) | 4.260 ms (1.0×) | 4.220 ms | 3.208 ms (0.8×) | 3.051 ms (0.7×) | 2.983 ms (0.7×) |
| DAG GNP dense shfld. | 8.053 ms (1.6×) | 5.303 ms (1.1×) | 4.964 ms (1.0×) | 4.895 ms | 3.254 ms (0.7×) | 3.093 ms (0.6×) | 3.095 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 855 op/s (127%) | 1 524 op/s (105%) | 1 287 op/s (88.3%) | 1 458 op/s | 1 511 op/s (104%) | 1 284 op/s (88.1%) | 1 412 op/s (96.9%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 777.0 op/s (81.4%) | 951.5 op/s (99.7%) | 961.5 op/s (101%) | 954.2 op/s | 1 767 op/s (185%) | 1 721 op/s (180%) | 1 751 op/s (184%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 100.4 op/s (12.3%) | 762.8 op/s (93.7%) | 714.8 op/s (87.8%) | 813.7 op/s | 977.5 op/s (120%) | 1 008 op/s (124%) | 1 021 op/s (126%) |
| Tree shfld. | 196.4 op/s (27.0%) | 672.5 op/s (92.5%) | 729.4 op/s (100%) | 726.7 op/s | 825.1 op/s (114%) | 870.3 op/s (120%) | 872.6 op/s (120%) |
| DAG GNP sparse | 105.0 op/s (25.2%) | 444.2 op/s (107%) | 416.8 op/s (100%) | 416.1 op/s | 675.7 op/s (162%) | 625.4 op/s (150%) | 623.1 op/s (150%) |
| DAG GNP dense | 154.2 op/s (65.1%) | 220.6 op/s (93.1%) | 234.7 op/s (99.1%) | 237.0 op/s | 311.7 op/s (132%) | 327.8 op/s (138%) | 335.2 op/s (141%) |
| DAG GNP dense shfld. | 124.2 op/s (60.8%) | 188.6 op/s (92.3%) | 201.5 op/s (98.6%) | 204.3 op/s | 307.3 op/s (150%) | 323.3 op/s (158%) | 323.1 op/s (158%) |

![1.6.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 30.49 ms (1.1×) | 29.87 ms (1.0×) | 28.76 ms | 15.46 ms (0.5×) | 16.20 ms (0.6×) | 16.08 ms (0.6×) |
| DAG GNP dense | 103.7 ms (1.1×) | 93.48 ms (1.0×) | 94.60 ms | — | — | — |
| DAG GNP dense shfld. | 176.2 ms (1.0×) | 163.5 ms (1.0×) | 168.2 ms | 39.69 ms (0.2×) | 35.90 ms (0.2×) | 35.30 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 32.80 op/s (94.3%) | 33.48 op/s (96.3%) | 34.77 op/s | 64.68 op/s (186%) | 61.73 op/s (178%) | 62.18 op/s (179%) |
| DAG GNP dense | 9.643 op/s (91.2%) | 10.70 op/s (101%) | 10.57 op/s | — | — | — |
| DAG GNP dense shfld. | 5.675 op/s (95.5%) | 6.116 op/s (103%) | 5.945 op/s | 25.20 op/s (424%) | 27.86 op/s (469%) | 28.33 op/s (476%) |

![1.6.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1170.9 ms (1.1×) | 1170.7 ms (1.1×) | 1074.8 ms | 160.8 ms (0.1×) | 167.4 ms (0.2×) | 155.5 ms (0.1×) |
| DAG GNP dense | 1966.4 ms (1.0×) | 1940.7 ms (1.0×) | 1889.5 ms | — | — | — |
| DAG GNP dense shfld. | 2823.5 ms (1.0×) | 2832.5 ms (1.0×) | 2812.4 ms | 414.3 ms (0.1×) | 421.8 ms (0.1×) | 421.4 ms (0.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.854 op/s (91.8%) | 0.854 op/s (91.8%) | 0.930 op/s | 6.219 op/s (668%) | 5.974 op/s (642%) | 6.431 op/s (691%) |
| DAG GNP dense | 0.509 op/s (96.1%) | 0.515 op/s (97.4%) | 0.529 op/s | — | — | — |
| DAG GNP dense shfld. | 0.354 op/s (99.6%) | 0.353 op/s (99.3%) | 0.356 op/s | 2.414 op/s (679%) | 2.371 op/s (667%) | 2.373 op/s (667%) |

![1.6.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 17135 ms (1.1×) | 16671 ms (1.1×) | 15339 ms | 1810.4 ms (0.1×) | 1923.6 ms (0.1×) | 1682.4 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.058 op/s (89.5%) | 0.060 op/s (92.0%) | 0.065 op/s | 0.552 op/s (847%) | 0.520 op/s (797%) | 0.594 op/s (912%) |

![1.6.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-5-toporder-n-100-000-000.svg)

### 1.7 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.039 ms | 0.037 ms (0.9×) | 0.039 ms (1.0×) | 0.040 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.055 ms (1.0×) | 0.056 ms (1.0×) | 0.056 ms (1.0×) | 0.055 ms | 0.038 ms (0.7×) | 0.039 ms (0.7×) | 0.039 ms (0.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.034 ms | 0.030 ms (0.9×) | 0.030 ms (0.9×) | 0.031 ms (0.9×) |
| Tree shfld. | 0.034 ms (1.0×) | 0.031 ms (0.9×) | 0.035 ms (1.0×) | 0.034 ms | 0.031 ms (0.9×) | 0.030 ms (0.9×) | 0.028 ms (0.8×) |
| DAG GNP sparse | 0.052 ms (0.9×) | 0.057 ms (1.0×) | 0.056 ms (1.0×) | 0.055 ms | 0.050 ms (0.9×) | 0.053 ms (1.0×) | 0.053 ms (1.0×) |
| DAG GNP dense | 0.364 ms (1.0×) | 0.358 ms (1.0×) | 0.358 ms (1.0×) | 0.363 ms | 0.326 ms (0.9×) | 0.325 ms (0.9×) | 0.324 ms (0.9×) |
| DAG GNP dense shfld. | 0.393 ms (1.0×) | 0.396 ms (1.0×) | 0.398 ms (1.0×) | 0.395 ms | 0.323 ms (0.8×) | 0.317 ms (0.8×) | 0.318 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s (100%) | 25 641 op/s (100%) | 25 641 op/s (100%) | 25 641 op/s | 27 027 op/s (105%) | 25 641 op/s (100%) | 25 000 op/s (97.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 182 op/s (100%) | 17 857 op/s (98.2%) | 17 857 op/s (98.2%) | 18 182 op/s | 26 316 op/s (145%) | 25 641 op/s (141%) | 25 641 op/s (141%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 32 258 op/s (110%) | 32 258 op/s (110%) | 32 258 op/s (110%) | 29 412 op/s | 33 333 op/s (113%) | 33 333 op/s (113%) | 32 258 op/s (110%) |
| Tree shfld. | 29 412 op/s (100%) | 32 258 op/s (110%) | 28 571 op/s (97.1%) | 29 412 op/s | 32 258 op/s (110%) | 33 333 op/s (113%) | 35 714 op/s (121%) |
| DAG GNP sparse | 19 231 op/s (106%) | 17 544 op/s (96.5%) | 17 857 op/s (98.2%) | 18 182 op/s | 20 000 op/s (110%) | 18 868 op/s (104%) | 18 868 op/s (104%) |
| DAG GNP dense | 2 747 op/s (99.7%) | 2 793 op/s (101%) | 2 793 op/s (101%) | 2 755 op/s | 3 067 op/s (111%) | 3 077 op/s (112%) | 3 086 op/s (112%) |
| DAG GNP dense shfld. | 2 545 op/s (101%) | 2 525 op/s (99.7%) | 2 513 op/s (99.2%) | 2 532 op/s | 3 096 op/s (122%) | 3 155 op/s (125%) | 3 145 op/s (124%) |

![1.7.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.562 ms (1.0×) | 0.456 ms (0.8×) | 0.460 ms (0.8×) | 0.558 ms | 0.560 ms (1.0×) | 0.561 ms (1.0×) | 0.453 ms (0.8×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.463 ms (1.0×) | 1.378 ms (1.0×) | 1.443 ms (1.0×) | 1.445 ms | 0.559 ms (0.4×) | 0.568 ms (0.4×) | 0.452 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.934 ms (1.1×) | 0.929 ms (1.1×) | 1.014 ms (1.2×) | 0.874 ms | 0.863 ms (1.0×) | 0.826 ms (0.9×) | 0.828 ms (0.9×) |
| Tree shfld. | 1.048 ms (1.0×) | 0.945 ms (0.9×) | 1.034 ms (1.0×) | 1.048 ms | 0.829 ms (0.8×) | 0.883 ms (0.8×) | 0.882 ms (0.8×) |
| DAG GNP sparse | 2.167 ms (1.0×) | 2.211 ms (1.0×) | 2.201 ms (1.0×) | 2.150 ms | 1.974 ms (0.9×) | 1.931 ms (0.9×) | 1.937 ms (0.9×) |
| DAG GNP dense | 5.160 ms (1.0×) | 5.093 ms (1.0×) | 5.000 ms (1.0×) | 5.021 ms | 4.574 ms (0.9×) | 4.518 ms (0.9×) | 4.623 ms (0.9×) |
| DAG GNP dense shfld. | 5.877 ms (1.0×) | 5.806 ms (1.0×) | 5.716 ms (1.0×) | 5.760 ms | 4.496 ms (0.8×) | 4.448 ms (0.8×) | 4.521 ms (0.8×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 779 op/s (99.3%) | 2 193 op/s (122%) | 2 174 op/s (121%) | 1 792 op/s | 1 786 op/s (99.6%) | 1 783 op/s (99.5%) | 2 208 op/s (123%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 683.5 op/s (98.8%) | 725.7 op/s (105%) | 693.0 op/s (100%) | 692.0 op/s | 1 789 op/s (258%) | 1 761 op/s (254%) | 2 212 op/s (320%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 071 op/s (93.6%) | 1 076 op/s (94.1%) | 986.2 op/s (86.2%) | 1 144 op/s | 1 159 op/s (101%) | 1 211 op/s (106%) | 1 208 op/s (106%) |
| Tree shfld. | 954.2 op/s (100%) | 1 058 op/s (111%) | 967.1 op/s (101%) | 954.2 op/s | 1 206 op/s (126%) | 1 133 op/s (119%) | 1 134 op/s (119%) |
| DAG GNP sparse | 461.5 op/s (99.2%) | 452.3 op/s (97.2%) | 454.3 op/s (97.7%) | 465.1 op/s | 506.6 op/s (109%) | 517.9 op/s (111%) | 516.3 op/s (111%) |
| DAG GNP dense | 193.8 op/s (97.3%) | 196.3 op/s (98.6%) | 200.0 op/s (100%) | 199.2 op/s | 218.6 op/s (110%) | 221.3 op/s (111%) | 216.3 op/s (109%) |
| DAG GNP dense shfld. | 170.2 op/s (98.0%) | 172.2 op/s (99.2%) | 174.9 op/s (101%) | 173.6 op/s | 222.4 op/s (128%) | 224.8 op/s (129%) | 221.2 op/s (127%) |

![1.7.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 29.60 ms (1.0×) | 28.32 ms (1.0×) | 28.71 ms | 26.85 ms (0.9×) | 26.56 ms (0.9×) | 26.10 ms (0.9×) |
| DAG GNP dense | 112.1 ms (1.0×) | 111.4 ms (1.0×) | 109.2 ms | — | — | — |
| DAG GNP dense shfld. | 191.1 ms (1.0×) | 195.3 ms (1.0×) | 189.9 ms | 89.45 ms (0.5×) | 90.69 ms (0.5×) | 92.85 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 33.78 op/s (97.0%) | 35.31 op/s (101%) | 34.83 op/s | 37.24 op/s (107%) | 37.65 op/s (108%) | 38.31 op/s (110%) |
| DAG GNP dense | 8.921 op/s (97.4%) | 8.977 op/s (98.0%) | 9.158 op/s | — | — | — |
| DAG GNP dense shfld. | 5.233 op/s (99.4%) | 5.120 op/s (97.2%) | 5.266 op/s | 11.18 op/s (212%) | 11.03 op/s (209%) | 10.77 op/s (205%) |

![1.7.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 856.2 ms (1.0×) | 847.2 ms (1.0×) | 844.5 ms | 751.9 ms (0.9×) | 748.9 ms (0.9×) | 754.1 ms (0.9×) |
| DAG GNP dense | 3085.9 ms (1.1×) | 2980.6 ms (1.0×) | 2926.5 ms | — | — | — |
| DAG GNP dense shfld. | 4398.5 ms (1.0×) | 4438.1 ms (1.0×) | 4397.5 ms | 2631.0 ms (0.6×) | 2566.8 ms (0.6×) | 2573.2 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1.168 op/s (98.6%) | 1.180 op/s (99.7%) | 1.184 op/s | 1.330 op/s (112%) | 1.335 op/s (113%) | 1.326 op/s (112%) |
| DAG GNP dense | 0.324 op/s (94.8%) | 0.336 op/s (98.2%) | 0.342 op/s | — | — | — |
| DAG GNP dense shfld. | 0.227 op/s (100.0%) | 0.225 op/s (99.1%) | 0.227 op/s | 0.380 op/s (167%) | 0.390 op/s (171%) | 0.389 op/s (171%) |

![1.7.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 11384 ms (1.0×) | 11396 ms (1.0×) | 11441 ms | 9441.1 ms (0.8×) | 9423.2 ms (0.8×) | 9427.8 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.088 op/s (100%) | 0.088 op/s (100%) | 0.087 op/s | 0.106 op/s (121%) | 0.106 op/s (121%) | 0.106 op/s (121%) |

![1.7.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-5-toporderandlevels-n-100-000-000.svg)

### 1.8 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.047 ms (0.8×) | 0.061 ms (1.0×) | 0.062 ms (1.0×) | 0.062 ms | 0.061 ms (1.0×) | 0.060 ms (1.0×) | 0.060 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.0×) | 0.068 ms (1.0×) | 0.066 ms | 0.051 ms (0.8×) | 0.054 ms (0.8×) | 0.055 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.223 ms (5.6×) | 0.049 ms (1.2×) | 0.061 ms (1.5×) | 0.040 ms | 0.048 ms (1.2×) | 0.054 ms (1.3×) | 0.046 ms (1.1×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.207 ms (1.2×) | 0.119 ms (0.7×) | 0.185 ms (1.1×) | 0.176 ms | 0.090 ms (0.5×) | 0.133 ms (0.8×) | 0.098 ms (0.6×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.797 ms (1.0×) | 0.776 ms (1.0×) | 0.796 ms (1.0×) | 0.783 ms | 0.647 ms (0.8×) | 0.672 ms (0.9×) | 0.663 ms (0.8×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.811 ms (1.0×) | 0.779 ms (1.0×) | 0.793 ms (1.0×) | 0.790 ms | 0.669 ms (0.8×) | 0.683 ms (0.9×) | 0.660 ms (0.8×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.512 ms (0.9×) | 0.592 ms (1.0×) | 0.600 ms (1.0×) | 0.592 ms | 0.592 ms (1.0×) | 0.642 ms (1.1×) | 0.591 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.619 ms (1.0×) | 0.644 ms (1.0×) | 0.634 ms (1.0×) | 0.646 ms | 0.588 ms (0.9×) | 0.583 ms (0.9×) | 0.591 ms (0.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 21 277 op/s (132%) | 16 393 op/s (102%) | 16 129 op/s (100%) | 16 129 op/s | 16 393 op/s (102%) | 16 667 op/s (103%) | 16 667 op/s (103%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (114%) | 14 925 op/s (98.5%) | 14 706 op/s (97.1%) | 15 152 op/s | 19 608 op/s (129%) | 18 519 op/s (122%) | 18 182 op/s (120%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 484 op/s (17.9%) | 20 408 op/s (81.6%) | 16 393 op/s (65.6%) | 25 000 op/s | 20 833 op/s (83.3%) | 18 519 op/s (74.1%) | 21 739 op/s (87.0%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 831 op/s (85.0%) | 8 403 op/s (148%) | 5 405 op/s (95.1%) | 5 682 op/s | 11 111 op/s (196%) | 7 519 op/s (132%) | 10 204 op/s (180%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 255 op/s (98.2%) | 1 289 op/s (101%) | 1 256 op/s (98.4%) | 1 277 op/s | 1 546 op/s (121%) | 1 488 op/s (117%) | 1 508 op/s (118%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 233 op/s (97.4%) | 1 284 op/s (101%) | 1 261 op/s (99.6%) | 1 266 op/s | 1 495 op/s (118%) | 1 464 op/s (116%) | 1 515 op/s (120%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 953 op/s (116%) | 1 689 op/s (100%) | 1 667 op/s (98.7%) | 1 689 op/s | 1 689 op/s (100%) | 1 558 op/s (92.2%) | 1 692 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 616 op/s (104%) | 1 553 op/s (100%) | 1 577 op/s (102%) | 1 548 op/s | 1 701 op/s (110%) | 1 715 op/s (111%) | 1 692 op/s (109%) |

![1.8.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.507 ms (0.9×) | 0.606 ms (1.1×) | 0.565 ms (1.0×) | 0.561 ms | 0.605 ms (1.1×) | 0.580 ms (1.0×) | 0.561 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.351 ms (1.3×) | 0.945 ms (0.9×) | 1.000 ms (1.0×) | 1.014 ms | 0.510 ms (0.5×) | 0.550 ms (0.5×) | 0.547 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.871 ms (9.6×) | 1.327 ms (1.3×) | 1.084 ms (1.1×) | 1.023 ms | 1.083 ms (1.1×) | 0.809 ms (0.8×) | 0.931 ms (0.9×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.691 ms (1.6×) | 3.177 ms (1.1×) | 3.212 ms (1.1×) | 3.025 ms | 1.962 ms (0.6×) | 1.998 ms (0.7×) | 1.909 ms (0.6×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.3×) | 12.91 ms (1.0×) | 12.43 ms (1.0×) | 12.89 ms | 7.303 ms (0.6×) | 7.425 ms (0.6×) | 7.152 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.4×) | 12.63 ms (1.0×) | 12.43 ms (1.0×) | 12.79 ms | 7.353 ms (0.6×) | 7.339 ms (0.6×) | 7.276 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.563 ms (1.4×) | 6.365 ms (1.0×) | 6.467 ms (1.0×) | 6.201 ms | 6.292 ms (1.0×) | 6.442 ms (1.0×) | 6.199 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 13.28 ms (1.2×) | 11.15 ms (1.0×) | 10.94 ms (1.0×) | 10.96 ms | 6.545 ms (0.6×) | 6.419 ms (0.6×) | 6.456 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 972 op/s (111%) | 1 650 op/s (92.6%) | 1 770 op/s (99.3%) | 1 783 op/s | 1 653 op/s (92.7%) | 1 724 op/s (96.7%) | 1 783 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 740.2 op/s (75.1%) | 1 058 op/s (107%) | 1 000 op/s (101%) | 986.2 op/s | 1 961 op/s (199%) | 1 818 op/s (184%) | 1 828 op/s (185%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 101.3 op/s (10.4%) | 753.6 op/s (77.1%) | 922.5 op/s (94.4%) | 977.5 op/s | 923.4 op/s (94.5%) | 1 236 op/s (126%) | 1 074 op/s (110%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 213.2 op/s (64.5%) | 314.8 op/s (95.2%) | 311.3 op/s (94.2%) | 330.6 op/s | 509.7 op/s (154%) | 500.5 op/s (151%) | 523.8 op/s (158%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (74.2%) | 77.44 op/s (99.9%) | 80.48 op/s (104%) | 77.55 op/s | 136.9 op/s (177%) | 134.7 op/s (174%) | 139.8 op/s (180%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (73.6%) | 79.20 op/s (101%) | 80.42 op/s (103%) | 78.17 op/s | 136.0 op/s (174%) | 136.3 op/s (174%) | 137.4 op/s (176%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 116.8 op/s (72.4%) | 157.1 op/s (97.4%) | 154.6 op/s (95.9%) | 161.3 op/s | 158.9 op/s (98.6%) | 155.2 op/s (96.3%) | 161.3 op/s (100%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 75.31 op/s (82.5%) | 89.65 op/s (98.3%) | 91.38 op/s (100%) | 91.25 op/s | 152.8 op/s (167%) | 155.8 op/s (171%) | 154.9 op/s (170%) |

![1.8.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 49.47 ms (1.1×) | 49.15 ms (1.0×) | 46.87 ms | 20.62 ms (0.4×) | 22.27 ms (0.5×) | 19.57 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 240.2 ms (1.0×) | 241.1 ms (1.0×) | 242.1 ms | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 241.4 ms (1.0×) | 238.7 ms (1.0×) | 239.5 ms | 94.11 ms (0.4×) | 93.46 ms (0.4×) | 91.56 ms (0.4×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 75.32 ms (1.0×) | 74.24 ms (0.9×) | 78.20 ms | — | — | — |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 20.22 op/s (94.8%) | 20.35 op/s (95.4%) | 21.33 op/s | 48.50 op/s (227%) | 44.90 op/s (210%) | 51.09 op/s (239%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.163 op/s (101%) | 4.148 op/s (100%) | 4.131 op/s | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.143 op/s (99.2%) | 4.189 op/s (100%) | 4.175 op/s | 10.63 op/s (254%) | 10.70 op/s (256%) | 10.92 op/s (262%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 13.28 op/s (104%) | 13.47 op/s (105%) | 12.79 op/s | — | — | — |

![1.8.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1984.9 ms (1.0×) | 1965.1 ms (1.0×) | 1944.9 ms | 254.0 ms (0.1×) | 254.9 ms (0.1×) | 250.2 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3708.4 ms (1.0×) | 3717.3 ms (1.0×) | 3732.9 ms | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3672.0 ms (0.9×) | 3836.1 ms (1.0×) | 3925.2 ms | 930.6 ms (0.2×) | 946.3 ms (0.2×) | 1127.9 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 815.0 ms (1.0×) | 832.5 ms (1.0×) | 848.5 ms | — | — | — |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.504 op/s (98.0%) | 0.509 op/s (99.0%) | 0.514 op/s | 3.937 op/s (766%) | 3.923 op/s (763%) | 3.997 op/s (777%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.270 op/s (101%) | 0.269 op/s (100%) | 0.268 op/s | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.272 op/s (107%) | 0.261 op/s (102%) | 0.255 op/s | 1.075 op/s (422%) | 1.057 op/s (415%) | 0.887 op/s (348%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.227 op/s (104%) | 1.201 op/s (102%) | 1.179 op/s | — | — | — |

![1.8.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 27110 ms (1.0×) | 27148 ms (1.0×) | 26831 ms | 2988.7 ms (0.1×) | 3076.5 ms (0.1×) | 3009.2 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.037 op/s (99.0%) | 0.037 op/s (98.8%) | 0.037 op/s | 0.335 op/s (898%) | 0.325 op/s (872%) | 0.332 op/s (892%) |

![1.8.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-5-backedges-n-100-000-000.svg)

### 1.9 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.051 ms (0.7×) | 0.064 ms (0.9×) | 0.074 ms (1.0×) | 0.071 ms | 0.064 ms (0.9×) | 0.074 ms (1.0×) | 0.072 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.066 ms (0.8×) | 0.077 ms (1.0×) | 0.078 ms (1.0×) | 0.081 ms | 0.059 ms (0.7×) | 0.069 ms (0.9×) | 0.070 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.196 ms (4.3×) | 0.053 ms (1.2×) | 0.052 ms (1.1×) | 0.046 ms | 0.046 ms (1.0×) | 0.064 ms (1.4×) | 0.049 ms (1.1×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.222 ms (1.2×) | 0.183 ms (1.0×) | 0.176 ms (1.0×) | 0.178 ms | 0.093 ms (0.5×) | 0.082 ms (0.5×) | 0.084 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.653 ms (1.2×) | 0.511 ms (1.0×) | 0.507 ms (1.0×) | 0.526 ms | 0.373 ms (0.7×) | 0.367 ms (0.7×) | 0.391 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.658 ms (1.2×) | 0.514 ms (1.0×) | 0.518 ms (1.0×) | 0.532 ms | 0.372 ms (0.7×) | 0.363 ms (0.7×) | 0.381 ms (0.7×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.375 ms (1.2×) | 0.282 ms (0.9×) | 0.269 ms (0.9×) | 0.301 ms | 0.285 ms (0.9×) | 0.260 ms (0.9×) | 0.301 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.499 ms (1.2×) | 0.383 ms (0.9×) | 0.385 ms (0.9×) | 0.419 ms | 0.276 ms (0.7×) | 0.308 ms (0.7×) | 0.350 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 19 608 op/s (139%) | 15 625 op/s (111%) | 13 514 op/s (95.9%) | 14 085 op/s | 15 625 op/s (111%) | 13 514 op/s (95.9%) | 13 889 op/s (98.6%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 152 op/s (123%) | 12 987 op/s (105%) | 12 821 op/s (104%) | 12 346 op/s | 16 949 op/s (137%) | 14 493 op/s (117%) | 14 286 op/s (116%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 102 op/s (23.5%) | 18 868 op/s (86.8%) | 19 231 op/s (88.5%) | 21 739 op/s | 21 739 op/s (100%) | 15 625 op/s (71.9%) | 20 408 op/s (93.9%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 505 op/s (80.2%) | 5 464 op/s (97.3%) | 5 682 op/s (101%) | 5 618 op/s | 10 753 op/s (191%) | 12 195 op/s (217%) | 11 905 op/s (212%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 531 op/s (80.6%) | 1 957 op/s (103%) | 1 972 op/s (104%) | 1 901 op/s | 2 681 op/s (141%) | 2 725 op/s (143%) | 2 558 op/s (135%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 520 op/s (80.9%) | 1 946 op/s (104%) | 1 931 op/s (103%) | 1 880 op/s | 2 688 op/s (143%) | 2 755 op/s (147%) | 2 625 op/s (140%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 2 667 op/s (80.3%) | 3 546 op/s (107%) | 3 717 op/s (112%) | 3 322 op/s | 3 509 op/s (106%) | 3 846 op/s (116%) | 3 322 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 004 op/s (84.0%) | 2 611 op/s (109%) | 2 597 op/s (109%) | 2 387 op/s | 3 623 op/s (152%) | 3 247 op/s (136%) | 2 857 op/s (120%) |

![1.9.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.554 ms (0.8×) | 0.647 ms (0.9×) | 0.724 ms (1.0×) | 0.697 ms | 0.642 ms (0.9×) | 0.736 ms (1.1×) | 0.706 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.563 ms (1.1×) | 1.365 ms (1.0×) | 1.369 ms (1.0×) | 1.425 ms | 0.615 ms (0.4×) | 0.660 ms (0.5×) | 0.670 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 10.29 ms (7.8×) | 1.288 ms (1.0×) | 1.446 ms (1.1×) | 1.323 ms | 1.022 ms (0.8×) | 0.961 ms (0.7×) | 0.889 ms (0.7×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.006 ms (1.4×) | 3.626 ms (1.0×) | 3.590 ms (1.0×) | 3.494 ms | 1.957 ms (0.6×) | 1.827 ms (0.5×) | 1.820 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.351 ms (1.3×) | 7.009 ms (1.0×) | 6.862 ms (1.0×) | 7.023 ms | 4.296 ms (0.6×) | 4.250 ms (0.6×) | 4.148 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 9.400 ms (1.4×) | 7.103 ms (1.0×) | 6.879 ms (1.0×) | 6.786 ms | 4.185 ms (0.6×) | 4.308 ms (0.6×) | 4.201 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.372 ms (1.4×) | 2.812 ms (0.9×) | 2.630 ms (0.9×) | 3.040 ms | 2.815 ms (0.9×) | 2.656 ms (0.9×) | 2.829 ms (0.9×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.766 ms (1.2×) | 5.303 ms (0.9×) | 5.119 ms (0.9×) | 5.588 ms | 3.053 ms (0.5×) | 3.088 ms (0.6×) | 3.067 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 805 op/s (126%) | 1 546 op/s (108%) | 1 381 op/s (96.3%) | 1 435 op/s | 1 558 op/s (109%) | 1 359 op/s (94.7%) | 1 416 op/s (98.7%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 639.8 op/s (91.2%) | 732.6 op/s (104%) | 730.5 op/s (104%) | 701.8 op/s | 1 626 op/s (232%) | 1 515 op/s (216%) | 1 493 op/s (213%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 97.17 op/s (12.9%) | 776.4 op/s (103%) | 691.6 op/s (91.5%) | 755.9 op/s | 978.5 op/s (129%) | 1 041 op/s (138%) | 1 125 op/s (149%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 199.8 op/s (69.8%) | 275.8 op/s (96.4%) | 278.6 op/s (97.3%) | 286.2 op/s | 511.0 op/s (179%) | 547.3 op/s (191%) | 549.5 op/s (192%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 106.9 op/s (75.1%) | 142.7 op/s (100%) | 145.7 op/s (102%) | 142.4 op/s | 232.8 op/s (163%) | 235.3 op/s (165%) | 241.1 op/s (169%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 106.4 op/s (72.2%) | 140.8 op/s (95.5%) | 145.4 op/s (98.6%) | 147.4 op/s | 238.9 op/s (162%) | 232.1 op/s (158%) | 238.0 op/s (162%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 228.7 op/s (69.5%) | 355.6 op/s (108%) | 380.2 op/s (116%) | 328.9 op/s | 355.2 op/s (108%) | 376.5 op/s (114%) | 353.5 op/s (107%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 147.8 op/s (82.6%) | 188.6 op/s (105%) | 195.4 op/s (109%) | 179.0 op/s | 327.5 op/s (183%) | 323.8 op/s (181%) | 326.1 op/s (182%) |

![1.9.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 60.41 ms (1.1×) | 61.58 ms (1.1×) | 57.30 ms | 23.47 ms (0.4×) | 21.79 ms (0.4×) | 20.58 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 235.5 ms (1.0×) | 227.1 ms (1.0×) | 236.3 ms | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 231.9 ms (1.0×) | 233.0 ms (1.0×) | 238.3 ms | 60.28 ms (0.3×) | 59.78 ms (0.3×) | 59.31 ms (0.2×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.97 ms (0.9×) | 25.85 ms (0.9×) | 28.99 ms | — | — | — |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 16.55 op/s (94.9%) | 16.24 op/s (93.1%) | 17.45 op/s | 42.60 op/s (244%) | 45.90 op/s (263%) | 48.59 op/s (278%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.246 op/s (100%) | 4.403 op/s (104%) | 4.232 op/s | — | — | — |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.312 op/s (103%) | 4.292 op/s (102%) | 4.196 op/s | 16.59 op/s (395%) | 16.73 op/s (399%) | 16.86 op/s (402%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 38.51 op/s (112%) | 38.68 op/s (112%) | 34.50 op/s | — | — | — |

![1.9.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2629.9 ms (1.1×) | 2581.9 ms (1.1×) | 2430.7 ms | 422.5 ms (0.2×) | 420.9 ms (0.2×) | 414.2 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4515.8 ms (1.0×) | 4509.5 ms (1.0×) | 4532.5 ms | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4526.0 ms (1.0×) | 4520.0 ms (1.0×) | 4527.9 ms | 1383.2 ms (0.3×) | 1344.7 ms (0.3×) | 1369.3 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 276.6 ms (0.9×) | 271.6 ms (0.9×) | 301.6 ms | — | — | — |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.380 op/s (92.4%) | 0.387 op/s (94.1%) | 0.411 op/s | 2.367 op/s (575%) | 2.376 op/s (578%) | 2.414 op/s (587%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (100%) | 0.222 op/s (101%) | 0.221 op/s | — | — | — |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (100%) | 0.221 op/s (100%) | 0.221 op/s | 0.723 op/s (327%) | 0.744 op/s (337%) | 0.730 op/s (331%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.615 op/s (109%) | 3.682 op/s (111%) | 3.316 op/s | — | — | — |

![1.9.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 31792 ms (1.0×) | 32421 ms (1.0×) | 31524 ms | 7181.5 ms (0.2×) | 7275.1 ms (0.2×) | 7132.5 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.031 op/s (99.2%) | 0.031 op/s (97.2%) | 0.032 op/s | 0.139 op/s (439%) | 0.137 op/s (433%) | 0.140 op/s (442%) |

![1.9.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-5-edgetypes-n-100-000-000.svg)

### 1.10 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.121 ms (1.2×) | 0.140 ms (1.3×) | 0.105 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.145 ms (1.2×) | 0.150 ms (1.2×) | 0.125 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.185 ms (2.1×) | 0.176 ms (2.0×) | 0.089 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.483 ms (1.3×) | 0.485 ms (1.3×) | 0.380 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.127 ms (1.4×) | 1.160 ms (1.4×) | 0.815 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.114 ms (1.4×) | 1.153 ms (1.4×) | 0.822 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.715 ms (1.4×) | 0.780 ms (1.5×) | 0.526 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.867 ms (1.4×) | 0.897 ms (1.4×) | 0.619 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 8 264 op/s (86.8%) | 7 143 op/s (75.0%) | 9 524 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 6 897 op/s (86.2%) | 6 667 op/s (83.3%) | 8 000 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 405 op/s (48.1%) | 5 682 op/s (50.6%) | 11 236 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 070 op/s (78.7%) | 2 062 op/s (78.4%) | 2 632 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 887.3 op/s (72.3%) | 862.1 op/s (70.3%) | 1 227 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 897.7 op/s (73.8%) | 867.3 op/s (71.3%) | 1 217 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 399 op/s (73.6%) | 1 282 op/s (67.4%) | 1 901 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 153 op/s (71.4%) | 1 115 op/s (69.0%) | 1 616 op/s |

![1.10.1 Reorder (DFS-preorder) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-1-reorder-dfs-preorder-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.259 ms (1.3×) | 1.373 ms (1.4×) | 0.985 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.144 ms (1.2×) | 2.166 ms (1.2×) | 1.858 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.144 ms (1.4×) | 2.983 ms (1.3×) | 2.216 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 6.609 ms (1.2×) | 6.616 ms (1.2×) | 5.343 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 15.07 ms (1.4×) | 15.14 ms (1.4×) | 10.73 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 15.00 ms (1.4×) | 14.90 ms (1.4×) | 10.80 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 7.332 ms (1.4×) | 7.899 ms (1.5×) | 5.411 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 11.95 ms (1.4×) | 12.42 ms (1.4×) | 8.673 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 794.3 op/s (78.2%) | 728.3 op/s (71.7%) | 1 015 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 466.4 op/s (86.7%) | 461.7 op/s (85.8%) | 538.2 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 318.1 op/s (70.5%) | 335.2 op/s (74.3%) | 451.3 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 151.3 op/s (80.8%) | 151.1 op/s (80.8%) | 187.2 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 66.37 op/s (71.2%) | 66.05 op/s (70.9%) | 93.18 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 66.66 op/s (72.0%) | 67.11 op/s (72.5%) | 92.55 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 136.4 op/s (73.8%) | 126.6 op/s (68.5%) | 184.8 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 83.70 op/s (72.6%) | 80.54 op/s (69.9%) | 115.3 op/s |

![1.10.2 Reorder (DFS-preorder) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-2-reorder-dfs-preorder-cost-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 95.36 ms (1.3×) | 92.31 ms (1.2×) | 75.80 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 414.3 ms (1.3×) | 416.3 ms (1.3×) | 308.5 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 10.49 op/s (79.5%) | 10.83 op/s (82.1%) | 13.19 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 2.414 op/s (74.5%) | 2.402 op/s (74.1%) | 3.241 op/s |

![1.10.3 Reorder (DFS-preorder) cost @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-3-reorder-dfs-preorder-cost-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4011.1 ms (1.2×) | 4050.8 ms (1.3×) | 3231.4 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 10805 ms (1.4×) | 11080 ms (1.5×) | 7597.1 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.249 op/s (80.6%) | 0.247 op/s (79.8%) | 0.309 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.093 op/s (70.3%) | 0.090 op/s (68.6%) | 0.132 op/s |

![1.10.4 Reorder (DFS-preorder) cost @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-4-reorder-dfs-preorder-cost-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 49550 ms (1.2×) | 50895 ms (1.2×) | 41169 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.020 op/s (83.1%) | 0.020 op/s (80.9%) | 0.024 op/s |

![1.10.5 Reorder (DFS-preorder) cost @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-5-reorder-dfs-preorder-cost-n-100-000-000.svg)

## 2 Memory Footprint

Retained heap in bytes for a fully built graph. Lower is better. Baseline (= 100%): **Graphity 2.2.0**, or the newest version present in a table it is missing from.

### 2.1 Total Graph Size

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Path<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 16.0 MB (100%) | 16.0 MB (100%) | 16.0 MB |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 160 MB (100%) | 160 MB (100%) | 160 MB |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 1600 MB (100%) | 1600 MB (100%) | 1600 MB |

![2.1 · Total Graph Size](fig/synthetic-ubuntu-ryzen-9950x-history/2-1-total-graph-size.svg)

### 2.2 Bytes per Element  `totalBytes / (Nodes + Edges)`

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B |

![2.2 · Bytes per Element  `totalBytes / (Nodes + Edges)`](fig/synthetic-ubuntu-ryzen-9950x-history/2-2-bytes-per-element-totalbytes-nodes-edges.svg)

## 3 Allocations

Bytes allocated per algorithm invocation (JMH `-prof gc`). Lower is better. Baseline (= 100%): **Graphity 2.2.0**, or the newest version present in a table it is missing from.

### 3.1 Build

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 458 kB (100%) | 458 kB (100%) | 458 kB (100%) | 458 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 458 kB (100%) | 458 kB (100%) | 458 kB (100%) | 458 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 458 kB (100%) | 458 kB (100%) | 458 kB (100%) | 458 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 960 kB (100%) | 960 kB (100%) | 960 kB (100%) | 960 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100%) | 9.9 MB (100%) | 9.9 MB (100%) | 9.9 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100%) | 10.0 MB (100%) | 10.0 MB (100%) | 10.0 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100%) | 10.0 MB (100%) | 10.0 MB (100%) | 10.0 MB |

![3.1.1 · Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-1-build-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100%) | 3.8 MB (100%) | 3.8 MB (100%) | 3.8 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100%) | 3.8 MB (100%) | 3.8 MB (100%) | 3.8 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100%) | 3.8 MB (100%) | 3.8 MB (100%) | 3.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB (100%) | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB (100%) | 111 MB (100%) | 111 MB (100.0%) | 111 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 111 MB (100%) | 111 MB (100%) | 111 MB (100%) | 111 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100%) | 112 MB (100%) | 112 MB (100%) | 112 MB |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 544 kB (102%) | 534 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 544 kB (102%) | 534 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 283 kB (98.1%) | 288 kB (100%) | 288 kB (100%) | 288 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 250 kB (67.5%) | 356 kB (96.0%) | 356 kB (96.0%) | 371 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (49.5%) | 507 kB (102%) | 507 kB (102%) | 498 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (49.4%) | 507 kB (102%) | 507 kB (102%) | 497 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (49.3%) | 505 kB (102%) | 505 kB (102%) | 495 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (67.0%) | 350 kB (95.9%) | 350 kB (95.9%) | 364 kB |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 6.0 MB (122%) | 6.0 MB (122%) | 4.9 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (57.8%) | 4.9 MB (100%) | 4.9 MB (100%) | 4.9 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (101%) | 2.8 MB (100%) | 2.8 MB (100%) | 2.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.5 MB (71.1%) | 3.9 MB (110%) | 3.9 MB (110%) | 3.5 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (54.3%) | 5.6 MB (124%) | 5.6 MB (124%) | 4.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (54.3%) | 5.6 MB (124%) | 5.6 MB (124%) | 4.5 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (54.2%) | 5.6 MB (124%) | 5.6 MB (124%) | 4.5 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (70.6%) | 3.3 MB (96.8%) | 3.3 MB (96.8%) | 3.5 MB |

![3.2.2 · SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-2-scc-n-100-000.svg)

### 3.3 WCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |

![3.3.2 · WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-2-wcc-n-100-000.svg)

### 3.4 CCs

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |

![3.4.1 · CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-1-ccs-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB (100.0%) | 2.0 MB |

![3.4.2 · CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-2-ccs-n-100-000.svg)

### 3.5 Condense

**Allocations @ 1k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | — | — | 23.2 kB (100%) | 23.2 kB |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | — | — | 96 B (100%) | 96 B |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | — | — | 760 B (100%) | 760 B |
| DAG GNP sparse<br><sub>1k nodes</sub> | — | — | 66.4 kB (100%) | 66.4 kB |

![3.5.1 · Condense @ n = 1 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-1-condense-n-1-000.svg)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | — | — | 250 kB (100%) | 250 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | — | — | 97 B (100%) | 97 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | — | — | 7.2 kB (100%) | 7.2 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | — | — | 541 kB (100%) | 541 kB |

![3.5.2 · Condense @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-2-condense-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | — | 2.1 MB (100%) | 2.1 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | — | — | 107 B (100%) | 107 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | — | — | 75.7 kB (100%) | 75.7 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | — | — | 5.9 MB (100%) | 5.9 MB |

![3.5.3 · Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-3-condense-n-100-000.svg)

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 306 kB (103%) | 297 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 306 kB (103%) | 297 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| Tree shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP dense<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 3.6 MB (143%) | 3.6 MB (143%) | 2.5 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 2.5 MB (101%) | 2.5 MB (101%) | 2.5 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB (100%) | 433 kB |
| Tree shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB (100%) | 433 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB |
| DAG GNP dense<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB (100%) | 433 kB |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB (100%) | 433 kB |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 266 kB (104%) | 257 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 266 kB (104%) | 257 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (25.2%) | 10.9 kB (100%) | 10.9 kB (100%) | 10.9 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 228 kB (65.0%) | 336 kB (95.8%) | 336 kB (95.8%) | 351 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100%) | 4.1 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100%) | 4.1 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100%) | 4.1 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (96.9%) | 4.0 MB (99.6%) | 4.0 MB (99.6%) | 4.0 MB |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 3.2 MB (151%) | 3.2 MB (151%) | 2.1 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 2.1 MB (101%) | 2.1 MB (101%) | 2.1 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 25.3 kB (75.8%) | 33.4 kB (100%) | 33.4 kB (100%) | 33.4 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.6 MB (71.3%) | 4.0 MB (110%) | 4.0 MB (110%) | 3.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 47.0 MB (102%) | 45.9 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 47.0 MB (102%) | 45.9 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 47.0 MB (102%) | 45.9 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (97.7%) | 44.7 MB (99.8%) | 44.7 MB (99.8%) | 44.8 MB |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 316 kB (103%) | 307 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 316 kB (103%) | 307 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (86.6%) | 60.8 kB (100%) | 60.8 kB (100%) | 60.8 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 72.7 kB (37.2%) | 181 kB (92.4%) | 181 kB (92.4%) | 196 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (57.4%) | 606 kB (102%) | 606 kB (102%) | 596 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (57.4%) | 606 kB (102%) | 606 kB (102%) | 596 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (58.2%) | 617 kB (102%) | 617 kB (102%) | 607 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (74.2%) | 462 kB (96.9%) | 462 kB (96.9%) | 476 kB |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 3.7 MB (141%) | 3.7 MB (141%) | 2.6 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 2.6 MB (101%) | 2.6 MB (101%) | 2.6 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 525 kB (98.5%) | 533 kB (100%) | 533 kB (100.0%) | 533 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 725 kB (41.1%) | 2.1 MB (120%) | 2.1 MB (120%) | 1.8 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (62.1%) | 6.6 MB (120%) | 6.6 MB (120%) | 5.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (62.1%) | 6.6 MB (120%) | 6.6 MB (120%) | 5.5 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (62.8%) | 6.7 MB (119%) | 6.7 MB (119%) | 5.6 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (77.2%) | 4.5 MB (97.5%) | 4.5 MB (97.5%) | 4.6 MB |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-2-edgetypes-n-100-000.svg)
