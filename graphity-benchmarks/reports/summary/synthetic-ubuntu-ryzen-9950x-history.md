# Graphity Version History — synthetic / ubuntu-ryzen-9950x

Benchmarked versions: `1.0`, `2.0.0`, `2.1.0`, `2.2.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`synthetic-ubuntu-ryzen-9950x-peers.md`](synthetic-ubuntu-ryzen-9950x-peers.md).

```
Profile:      synthetic
Timestamp:    2026-10-11 14:31:16
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
  - [1.12 Dominators](#112-dominators)
  - [1.13 Dominance frontiers](#113-dominance-frontiers)
  - [1.14 Iterated dominance frontier (build + one query per variable)](#114-iterated-dominance-frontier-build-one-query-per-variable)
  - [1.15 Loops](#115-loops)
  - [1.16 Ancestry of the dominator tree](#116-ancestry-of-the-dominator-tree)
  - [1.17 PreorderAncestry of the dominator tree](#117-preorderancestry-of-the-dominator-tree)
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
  - [3.10 Dominators](#310-dominators)
  - [3.11 Dominance frontiers](#311-dominance-frontiers)
  - [3.12 Iterated dominance frontier (build + one query per variable)](#312-iterated-dominance-frontier-build-one-query-per-variable)
  - [3.13 Loops](#313-loops)
  - [3.14 Ancestry of the dominator tree](#314-ancestry-of-the-dominator-tree)
  - [3.15 PreorderAncestry of the dominator tree](#315-preorderancestry-of-the-dominator-tree)

## 1 Performance

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.2.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 Build

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.083 ms (1.0×) | 0.083 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.084 ms (1.0×) | 0.084 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.092 ms (1.0×) | 0.092 ms (1.0×) | 0.093 ms (1.0×) | 0.093 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.164 ms (1.0×) | 0.162 ms (1.0×) | 0.163 ms (1.0×) | 0.163 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.597 ms (1.0×) | 1.573 ms (1.0×) | 1.581 ms (1.0×) | 1.650 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.572 ms (1.0×) | 1.546 ms (1.0×) | 1.623 ms (1.0×) | 1.614 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.550 ms (1.0×) | 1.588 ms (1.0×) | 1.587 ms (1.0×) | 1.622 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.551 ms (0.9×) | 1.553 ms (0.9×) | 1.634 ms (1.0×) | 1.650 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (100.0%) | 12 195 op/s (101%) | 12 048 op/s (100.0%) | 12 048 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (101%) | 12 195 op/s (102%) | 11 905 op/s (100%) | 11 905 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 870 op/s (101%) | 10 870 op/s (101%) | 10 753 op/s (100%) | 10 753 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 098 op/s (99.4%) | 6 173 op/s (101%) | 6 135 op/s (100%) | 6 135 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 626.2 op/s (103%) | 635.7 op/s (105%) | 632.5 op/s (104%) | 606.1 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 636.1 op/s (103%) | 646.8 op/s (104%) | 616.1 op/s (99.4%) | 619.6 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 645.2 op/s (105%) | 629.7 op/s (102%) | 630.1 op/s (102%) | 616.5 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 644.7 op/s (106%) | 643.9 op/s (106%) | 612.0 op/s (101%) | 606.1 op/s |

![1.1.1 Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-1-build-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.686 ms (1.0×) | 0.680 ms (1.0×) | 0.695 ms (1.0×) | 0.696 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.687 ms (1.0×) | 0.682 ms (1.0×) | 0.694 ms (1.0×) | 0.698 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.093 ms (1.0×) | 1.091 ms (1.0×) | 1.100 ms (1.0×) | 1.111 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.249 ms (1.0×) | 2.257 ms (1.0×) | 2.218 ms (1.0×) | 2.274 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.27 ms (1.0×) | 16.93 ms (1.0×) | 17.11 ms (1.0×) | 17.48 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 16.99 ms (1.0×) | 16.74 ms (1.0×) | 17.28 ms (1.0×) | 17.50 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 16.96 ms (1.0×) | 16.82 ms (1.0×) | 17.11 ms (1.0×) | 17.45 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 17.04 ms (1.0×) | 16.06 ms (0.9×) | 17.09 ms (1.0×) | 16.96 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 458 op/s (101%) | 1 471 op/s (102%) | 1 439 op/s (100%) | 1 437 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 456 op/s (102%) | 1 466 op/s (102%) | 1 441 op/s (101%) | 1 433 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 914.9 op/s (102%) | 916.6 op/s (102%) | 909.1 op/s (101%) | 900.1 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 444.6 op/s (101%) | 443.1 op/s (101%) | 450.9 op/s (103%) | 439.8 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.90 op/s (101%) | 59.06 op/s (103%) | 58.44 op/s (102%) | 57.19 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 58.85 op/s (103%) | 59.74 op/s (105%) | 57.87 op/s (101%) | 57.16 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 58.96 op/s (103%) | 59.46 op/s (104%) | 58.45 op/s (102%) | 57.32 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 58.70 op/s (99.5%) | 62.28 op/s (106%) | 58.52 op/s (99.2%) | 58.97 op/s |

![1.1.2 Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-2-build-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 22.97 ms (1.0×) | 23.63 ms (1.0×) | 24.02 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 170.6 ms (0.9×) | 167.8 ms (0.9×) | 181.1 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 167.6 ms (1.0×) | 166.8 ms (1.0×) | 169.9 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 173.2 ms (1.0×) | 169.5 ms (1.0×) | 167.1 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 43.54 op/s (105%) | 42.31 op/s (102%) | 41.63 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 5.862 op/s (106%) | 5.959 op/s (108%) | 5.522 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 5.967 op/s (101%) | 5.995 op/s (102%) | 5.886 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 5.774 op/s (96.5%) | 5.900 op/s (98.6%) | 5.984 op/s |

![1.1.3 Build @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-3-build-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 248.6 ms (1.0×) | 240.4 ms (1.0×) | 243.3 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 2052.8 ms (0.9×) | 1916.6 ms (0.8×) | 2326.6 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 1806.0 ms (0.8×) | 1829.5 ms (0.8×) | 2301.9 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1784.0 ms (1.0×) | 1794.6 ms (1.0×) | 1846.2 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4.023 op/s (97.9%) | 4.160 op/s (101%) | 4.110 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.487 op/s (113%) | 0.522 op/s (121%) | 0.430 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.554 op/s (127%) | 0.547 op/s (126%) | 0.434 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 0.561 op/s (103%) | 0.557 op/s (103%) | 0.542 op/s |

![1.1.4 Build @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-4-build-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 2945.8 ms (1.0×) | 2971.7 ms (1.0×) | 2986.8 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.339 op/s (101%) | 0.337 op/s (101%) | 0.335 op/s |

![1.1.5 Build @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-5-build-n-100-000-000.svg)

### 1.2 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.106 ms (1.0×) | 0.113 ms (1.1×) | 0.106 ms (1.0×) | 0.106 ms | 0.112 ms (1.1×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.129 ms (1.0×) | 0.134 ms (1.0×) | 0.134 ms (1.0×) | 0.135 ms | 0.108 ms (0.8×) | 0.105 ms (0.8×) | 0.106 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.271 ms (2.6×) | 0.107 ms (1.0×) | 0.104 ms (1.0×) | 0.105 ms | 0.095 ms (0.9×) | 0.086 ms (0.8×) | 0.086 ms (0.8×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.245 ms (1.0×) | 0.239 ms (1.0×) | 0.242 ms (1.0×) | 0.245 ms | 0.111 ms (0.5×) | 0.112 ms (0.5×) | 0.112 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.3×) | 0.539 ms (1.0×) | 0.559 ms (1.0×) | 0.550 ms | 0.378 ms (0.7×) | 0.381 ms (0.7×) | 0.386 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.3×) | 0.531 ms (0.9×) | 0.539 ms (0.9×) | 0.576 ms | 0.384 ms (0.7×) | 0.373 ms (0.6×) | 0.379 ms (0.7×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.563 ms (2.2×) | 0.260 ms (1.0×) | 0.261 ms (1.0×) | 0.261 ms | 0.259 ms (1.0×) | 0.260 ms (1.0×) | 0.266 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.8×) | 0.338 ms (1.0×) | 0.348 ms (1.0×) | 0.349 ms | 0.250 ms (0.7×) | 0.265 ms (0.8×) | 0.264 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 434 op/s (100%) | 8 850 op/s (93.8%) | 9 434 op/s (100%) | 9 434 op/s | 8 929 op/s (94.6%) | 9 434 op/s (100%) | 9 434 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 752 op/s (105%) | 7 463 op/s (101%) | 7 463 op/s (101%) | 7 407 op/s | 9 259 op/s (125%) | 9 524 op/s (129%) | 9 434 op/s (127%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 3 690 op/s (38.7%) | 9 346 op/s (98.1%) | 9 615 op/s (101%) | 9 524 op/s | 10 526 op/s (111%) | 11 628 op/s (122%) | 11 628 op/s (122%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 082 op/s (100%) | 4 184 op/s (103%) | 4 132 op/s (101%) | 4 082 op/s | 9 009 op/s (221%) | 8 929 op/s (219%) | 8 929 op/s (219%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (75.3%) | 1 855 op/s (102%) | 1 789 op/s (98.4%) | 1 818 op/s | 2 646 op/s (146%) | 2 625 op/s (144%) | 2 591 op/s (142%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (78.9%) | 1 883 op/s (108%) | 1 855 op/s (107%) | 1 736 op/s | 2 604 op/s (150%) | 2 681 op/s (154%) | 2 639 op/s (152%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 776 op/s (46.4%) | 3 846 op/s (100%) | 3 831 op/s (100%) | 3 831 op/s | 3 861 op/s (101%) | 3 846 op/s (100%) | 3 759 op/s (98.1%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (54.6%) | 2 959 op/s (103%) | 2 874 op/s (100%) | 2 865 op/s | 4 000 op/s (140%) | 3 774 op/s (132%) | 3 788 op/s (132%) |

![1.2.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 1.130 ms (1.1×) | 1.030 ms (1.0×) | 1.021 ms | 1.117 ms (1.1×) | 1.025 ms (1.0×) | 1.030 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.415 ms (1.1×) | 2.192 ms (1.0×) | 2.146 ms (1.0×) | 2.236 ms | 1.038 ms (0.5×) | 1.035 ms (0.5×) | 1.052 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 11.69 ms (5.4×) | 2.240 ms (1.0×) | 2.122 ms (1.0×) | 2.153 ms | 1.413 ms (0.7×) | 1.305 ms (0.6×) | 1.303 ms (0.6×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.872 ms (1.1×) | 4.329 ms (0.9×) | 4.447 ms (1.0×) | 4.617 ms | 2.247 ms (0.5×) | 2.265 ms (0.5×) | 2.220 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.957 ms (1.2×) | 7.389 ms (0.9×) | 7.397 ms (0.9×) | 8.010 ms | 4.475 ms (0.6×) | 4.431 ms (0.6×) | 4.693 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 7.595 ms (0.9×) | 7.548 ms (0.9×) | 8.094 ms | 4.423 ms (0.5×) | 4.537 ms (0.6×) | 4.517 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.702 ms (2.3×) | 2.623 ms (1.0×) | 2.606 ms (1.0×) | 2.509 ms | 2.625 ms (1.0×) | 2.598 ms (1.0×) | 2.645 ms (1.1×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 7.381 ms (1.5×) | 4.697 ms (1.0×) | 4.820 ms (1.0×) | 4.904 ms | 2.542 ms (0.5×) | 2.596 ms (0.5×) | 2.789 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 885.0 op/s (90.4%) | 970.9 op/s (99.1%) | 979.4 op/s | 895.3 op/s (91.4%) | 975.6 op/s (99.6%) | 970.9 op/s (99.1%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 414.1 op/s (92.6%) | 456.2 op/s (102%) | 466.0 op/s (104%) | 447.2 op/s | 963.4 op/s (215%) | 966.2 op/s (216%) | 950.6 op/s (213%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 85.56 op/s (18.4%) | 446.4 op/s (96.1%) | 471.3 op/s (101%) | 464.5 op/s | 707.7 op/s (152%) | 766.3 op/s (165%) | 767.5 op/s (165%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 205.3 op/s (94.8%) | 231.0 op/s (107%) | 224.9 op/s (104%) | 216.6 op/s | 445.0 op/s (205%) | 441.5 op/s (204%) | 450.5 op/s (208%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 100.4 op/s (80.4%) | 135.3 op/s (108%) | 135.2 op/s (108%) | 124.8 op/s | 223.5 op/s (179%) | 225.7 op/s (181%) | 213.1 op/s (171%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 131.7 op/s (107%) | 132.5 op/s (107%) | 123.5 op/s | 226.1 op/s (183%) | 220.4 op/s (178%) | 221.4 op/s (179%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 175.4 op/s (44.0%) | 381.2 op/s (95.7%) | 383.7 op/s (96.3%) | 398.6 op/s | 381.0 op/s (95.6%) | 384.9 op/s (96.6%) | 378.1 op/s (94.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 135.5 op/s (66.4%) | 212.9 op/s (104%) | 207.5 op/s (102%) | 203.9 op/s | 393.4 op/s (193%) | 385.2 op/s (189%) | 358.6 op/s (176%) |

![1.2.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 77.49 ms (0.9×) | 76.73 ms (0.9×) | 86.77 ms | 26.75 ms (0.3×) | 26.04 ms (0.3×) | 25.87 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 238.7 ms (1.0×) | 233.4 ms (0.9×) | 246.1 ms | — | 61.63 ms (0.3×) | 61.93 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 232.9 ms (0.9×) | 229.7 ms (0.9×) | 248.1 ms | 66.11 ms (0.3×) | 62.45 ms (0.3×) | 62.57 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.36 ms (1.0×) | 26.24 ms (1.0×) | 26.51 ms | — | 26.27 ms (1.0×) | 26.54 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 12.91 op/s (112%) | 13.03 op/s (113%) | 11.52 op/s | 37.38 op/s (324%) | 38.41 op/s (333%) | 38.65 op/s (335%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.189 op/s (103%) | 4.284 op/s (105%) | 4.063 op/s | — | 16.23 op/s (399%) | 16.15 op/s (397%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.294 op/s (107%) | 4.354 op/s (108%) | 4.031 op/s | 15.13 op/s (375%) | 16.01 op/s (397%) | 15.98 op/s (397%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 39.43 op/s (105%) | 38.10 op/s (101%) | 37.72 op/s | — | 38.06 op/s (101%) | 37.67 op/s (99.9%) |

![1.2.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3085.1 ms (1.0×) | 3051.6 ms (1.0×) | 3120.3 ms | 739.3 ms (0.2×) | 700.8 ms (0.2×) | 757.2 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6301.4 ms (0.9×) | 6518.8 ms (1.0×) | 6783.8 ms | — | 2978.2 ms (0.4×) | 3038.0 ms (0.4×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 6297.6 ms (0.9×) | 6583.3 ms (1.0×) | 6754.9 ms | 2740.8 ms (0.4×) | 2700.6 ms (0.4×) | 3578.0 ms (0.5×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 268.4 ms (1.0×) | 275.9 ms (1.1×) | 256.3 ms | — | 271.5 ms (1.1×) | 274.0 ms (1.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.324 op/s (101%) | 0.328 op/s (102%) | 0.320 op/s | 1.353 op/s (422%) | 1.427 op/s (445%) | 1.321 op/s (412%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (108%) | 0.153 op/s (104%) | 0.147 op/s | — | 0.336 op/s (228%) | 0.329 op/s (223%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (107%) | 0.152 op/s (103%) | 0.148 op/s | 0.365 op/s (246%) | 0.370 op/s (250%) | 0.279 op/s (189%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.726 op/s (95.5%) | 3.625 op/s (92.9%) | 3.902 op/s | — | 3.683 op/s (94.4%) | 3.650 op/s (93.5%) |

![1.2.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 36814 ms (1.0×) | 36265 ms (1.0×) | 36545 ms | 9963.5 ms (0.3×) | 9839.3 ms (0.3×) | 9918.4 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.027 op/s (99.3%) | 0.028 op/s (101%) | 0.027 op/s | 0.100 op/s (367%) | 0.102 op/s (371%) | 0.101 op/s (368%) |

![1.2.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-5-scc-n-100-000-000.svg)

### 1.3 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (1.0×) | 0.036 ms (1.0×) | 0.037 ms (1.0×) | 0.036 ms | 0.042 ms (1.2×) | 0.040 ms (1.1×) | 0.042 ms (1.2×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.046 ms (1.0×) | 0.046 ms (1.0×) | 0.053 ms (1.2×) | 0.046 ms | 0.042 ms (0.9×) | 0.042 ms (0.9×) | 0.042 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.038 ms (1.0×) | 0.038 ms (1.0×) | 0.038 ms (1.0×) | 0.038 ms | 0.038 ms (1.0×) | 0.044 ms (1.2×) | 0.039 ms (1.0×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.095 ms (0.9×) | 0.083 ms (0.8×) | 0.097 ms (1.0×) | 0.102 ms | 0.086 ms (0.8×) | 0.082 ms (0.8×) | 0.081 ms (0.8×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.530 ms (1.0×) | 0.524 ms (1.0×) | 0.523 ms (1.0×) | 0.531 ms | 0.524 ms (1.0×) | 0.520 ms (1.0×) | 0.537 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms (1.0×) | 0.524 ms (1.0×) | 0.525 ms (1.0×) | 0.528 ms | 0.527 ms (1.0×) | 0.521 ms (1.0×) | 0.525 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.579 ms (1.0×) | 0.581 ms | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.589 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.640 ms (1.0×) | 0.640 ms (1.0×) | 0.641 ms (1.0×) | 0.648 ms | 0.581 ms (0.9×) | 0.614 ms (0.9×) | 0.620 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (97.3%) | 27 778 op/s (100%) | 27 027 op/s (97.3%) | 27 778 op/s | 23 810 op/s (85.7%) | 25 000 op/s (90.0%) | 23 810 op/s (85.7%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 739 op/s (100%) | 21 739 op/s (100%) | 18 868 op/s (86.8%) | 21 739 op/s | 23 810 op/s (110%) | 23 810 op/s (110%) | 23 810 op/s (110%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s (100%) | 26 316 op/s (100%) | 26 316 op/s (100%) | 26 316 op/s | 26 316 op/s (100%) | 22 727 op/s (86.4%) | 25 641 op/s (97.4%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 10 526 op/s (107%) | 12 048 op/s (123%) | 10 309 op/s (105%) | 9 804 op/s | 11 628 op/s (119%) | 12 195 op/s (124%) | 12 346 op/s (126%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 887 op/s (100%) | 1 908 op/s (101%) | 1 912 op/s (102%) | 1 883 op/s | 1 908 op/s (101%) | 1 923 op/s (102%) | 1 862 op/s (98.9%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s (101%) | 1 908 op/s (101%) | 1 905 op/s (101%) | 1 894 op/s | 1 898 op/s (100%) | 1 919 op/s (101%) | 1 905 op/s (101%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 730 op/s (101%) | 1 730 op/s (101%) | 1 727 op/s (100%) | 1 721 op/s | 1 730 op/s (101%) | 1 730 op/s (101%) | 1 698 op/s (98.6%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 562 op/s (101%) | 1 562 op/s (101%) | 1 560 op/s (101%) | 1 543 op/s | 1 721 op/s (112%) | 1 629 op/s (106%) | 1 613 op/s (105%) |

![1.3.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.393 ms (1.0×) | 0.398 ms (1.0×) | 0.406 ms (1.0×) | 0.395 ms | 0.400 ms (1.0×) | 0.352 ms (0.9×) | 0.357 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.228 ms (1.0×) | 1.232 ms (1.0×) | 1.291 ms | 0.390 ms (0.3×) | 0.347 ms (0.3×) | 0.356 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.661 ms (1.0×) | 0.636 ms (1.0×) | 0.669 ms (1.0×) | 0.665 ms | 0.732 ms (1.1×) | 0.770 ms (1.2×) | 0.732 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.769 ms (1.0×) | 1.767 ms (1.0×) | 1.794 ms (1.0×) | 1.800 ms | 1.550 ms (0.9×) | 1.561 ms (0.9×) | 1.572 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.082 ms (1.0×) | 6.090 ms (1.0×) | 6.096 ms (1.0×) | 6.096 ms | 6.069 ms (1.0×) | 6.054 ms (1.0×) | 6.263 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.163 ms (1.0×) | 6.091 ms (1.0×) | 6.088 ms (1.0×) | 6.158 ms | 6.163 ms (1.0×) | 6.118 ms (1.0×) | 6.194 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.858 ms (0.9×) | 6.000 ms (0.9×) | 5.778 ms (0.9×) | 6.479 ms | 5.927 ms (0.9×) | 5.764 ms (0.9×) | 6.018 ms (0.9×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.955 ms (1.0×) | 5.953 ms (1.0×) | 5.988 ms (1.0×) | 6.054 ms | 6.021 ms (1.0×) | 6.133 ms (1.0×) | 5.936 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 545 op/s (101%) | 2 513 op/s (99.2%) | 2 463 op/s (97.3%) | 2 532 op/s | 2 500 op/s (98.8%) | 2 841 op/s (112%) | 2 801 op/s (111%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (105%) | 814.3 op/s (105%) | 811.7 op/s (105%) | 774.6 op/s | 2 564 op/s (331%) | 2 882 op/s (372%) | 2 809 op/s (363%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 513 op/s (101%) | 1 572 op/s (105%) | 1 495 op/s (99.4%) | 1 504 op/s | 1 366 op/s (90.8%) | 1 299 op/s (86.4%) | 1 366 op/s (90.8%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 565.3 op/s (102%) | 565.9 op/s (102%) | 557.4 op/s (100%) | 555.6 op/s | 645.2 op/s (116%) | 640.6 op/s (115%) | 636.1 op/s (115%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 164.4 op/s (100%) | 164.2 op/s (100%) | 164.0 op/s (100%) | 164.0 op/s | 164.8 op/s (100%) | 165.2 op/s (101%) | 159.7 op/s (97.3%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.3 op/s (99.9%) | 164.2 op/s (101%) | 164.3 op/s (101%) | 162.4 op/s | 162.3 op/s (99.9%) | 163.5 op/s (101%) | 161.4 op/s (99.4%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 170.7 op/s (111%) | 166.7 op/s (108%) | 173.1 op/s (112%) | 154.3 op/s | 168.7 op/s (109%) | 173.5 op/s (112%) | 166.2 op/s (108%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 167.9 op/s (102%) | 168.0 op/s (102%) | 167.0 op/s (101%) | 165.2 op/s | 166.1 op/s (101%) | 163.1 op/s (98.7%) | 168.5 op/s (102%) |

![1.3.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.74 ms (1.1×) | 21.57 ms (1.1×) | 19.97 ms | 17.26 ms (0.9×) | 16.24 ms (0.8×) | 16.39 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 65.50 ms (1.0×) | 78.43 ms (1.2×) | 67.53 ms | — | 66.27 ms (1.0×) | 69.74 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 70.47 ms (1.0×) | 66.99 ms (1.0×) | 68.44 ms | 69.61 ms (1.0×) | 66.64 ms (1.0×) | 68.78 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.95 ms (1.0×) | 51.65 ms (1.0×) | 50.92 ms | — | 50.10 ms (1.0×) | 50.64 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.00 op/s (91.9%) | 46.37 op/s (92.6%) | 50.08 op/s | 57.94 op/s (116%) | 61.58 op/s (123%) | 61.02 op/s (122%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 15.27 op/s (103%) | 12.75 op/s (86.1%) | 14.81 op/s | — | 15.09 op/s (102%) | 14.34 op/s (96.8%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.19 op/s (97.1%) | 14.93 op/s (102%) | 14.61 op/s | 14.37 op/s (98.3%) | 15.01 op/s (103%) | 14.54 op/s (99.5%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.63 op/s (99.9%) | 19.36 op/s (98.6%) | 19.64 op/s | — | 19.96 op/s (102%) | 19.75 op/s (101%) |

![1.3.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 615.6 ms (1.1×) | 600.5 ms (1.1×) | 569.5 ms | 410.6 ms (0.7×) | 378.4 ms (0.7×) | 447.4 ms (0.8×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4094.7 ms (0.9×) | 4106.6 ms (0.9×) | 4580.5 ms | — | 3774.2 ms (0.8×) | 4418.3 ms (1.0×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3899.0 ms (1.0×) | 3919.9 ms (1.0×) | 3734.0 ms | 3844.9 ms (1.0×) | 3828.2 ms (1.0×) | 4399.7 ms (1.2×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 754.7 ms (1.3×) | 516.1 ms (0.9×) | 594.1 ms | — | 516.0 ms (0.9×) | 561.6 ms (0.9×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.624 op/s (92.5%) | 1.665 op/s (94.8%) | 1.756 op/s | 2.435 op/s (139%) | 2.643 op/s (151%) | 2.235 op/s (127%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.244 op/s (112%) | 0.244 op/s (112%) | 0.218 op/s | — | 0.265 op/s (121%) | 0.226 op/s (104%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s (95.8%) | 0.255 op/s (95.3%) | 0.268 op/s | 0.260 op/s (97.1%) | 0.261 op/s (97.5%) | 0.227 op/s (84.9%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.325 op/s (78.7%) | 1.938 op/s (115%) | 1.683 op/s | — | 1.938 op/s (115%) | 1.781 op/s (106%) |

![1.3.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8231.6 ms (0.9×) | 9442.8 ms (1.0×) | 9600.9 ms | 6807.7 ms (0.7×) | 6812.3 ms (0.7×) | 6010.4 ms (0.6×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.121 op/s (117%) | 0.106 op/s (102%) | 0.104 op/s | 0.147 op/s (141%) | 0.147 op/s (141%) | 0.166 op/s (160%) |

![1.3.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-5-wcc-n-100-000-000.svg)

### 1.4 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (0.9×) | 0.042 ms (1.0×) | 0.042 ms (1.0×) | 0.043 ms | 0.036 ms (0.8×) | 0.042 ms (1.0×) | 0.037 ms (0.9×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.053 ms (0.9×) | 0.053 ms (0.9×) | 0.045 ms (0.8×) | 0.058 ms | 0.036 ms (0.6×) | 0.042 ms (0.7×) | 0.036 ms (0.6×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.044 ms (1.1×) | 0.038 ms (1.0×) | 0.042 ms (1.1×) | 0.039 ms | 0.043 ms (1.1×) | 0.038 ms (1.0×) | 0.044 ms (1.1×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.106 ms (1.0×) | 0.090 ms (0.8×) | 0.094 ms (0.9×) | 0.110 ms | 0.088 ms (0.8×) | 0.087 ms (0.8×) | 0.087 ms (0.8×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.518 ms (1.0×) | 0.526 ms (1.0×) | 0.524 ms (1.0×) | 0.530 ms | 0.520 ms (1.0×) | 0.527 ms (1.0×) | 0.530 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.522 ms (1.0×) | 0.524 ms (1.0×) | 0.524 ms (1.0×) | 0.535 ms | 0.534 ms (1.0×) | 0.521 ms (1.0×) | 0.535 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.586 ms (1.0×) | 0.580 ms (1.0×) | 0.588 ms (1.0×) | 0.586 ms | 0.584 ms (1.0×) | 0.588 ms (1.0×) | 0.556 ms (0.9×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.0×) | 0.640 ms (1.0×) | 0.643 ms (1.0×) | 0.648 ms | 0.625 ms (1.0×) | 0.611 ms (0.9×) | 0.625 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (116%) | 23 810 op/s (102%) | 23 810 op/s (102%) | 23 256 op/s | 27 778 op/s (119%) | 23 810 op/s (102%) | 27 027 op/s (116%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s (109%) | 18 868 op/s (109%) | 22 222 op/s (129%) | 17 241 op/s | 27 778 op/s (161%) | 23 810 op/s (138%) | 27 778 op/s (161%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 22 727 op/s (88.6%) | 26 316 op/s (103%) | 23 810 op/s (92.9%) | 25 641 op/s | 23 256 op/s (90.7%) | 26 316 op/s (103%) | 22 727 op/s (88.6%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 9 434 op/s (104%) | 11 111 op/s (122%) | 10 638 op/s (117%) | 9 091 op/s | 11 364 op/s (125%) | 11 494 op/s (126%) | 11 494 op/s (126%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 931 op/s (102%) | 1 901 op/s (101%) | 1 908 op/s (101%) | 1 887 op/s | 1 923 op/s (102%) | 1 898 op/s (101%) | 1 887 op/s (100%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 916 op/s (102%) | 1 908 op/s (102%) | 1 908 op/s (102%) | 1 869 op/s | 1 873 op/s (100%) | 1 919 op/s (103%) | 1 869 op/s (100%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 706 op/s (100%) | 1 724 op/s (101%) | 1 701 op/s (99.7%) | 1 706 op/s | 1 712 op/s (100%) | 1 701 op/s (99.7%) | 1 799 op/s (105%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (101%) | 1 562 op/s (101%) | 1 555 op/s (101%) | 1 543 op/s | 1 600 op/s (104%) | 1 637 op/s (106%) | 1 600 op/s (104%) |

![1.4.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.396 ms (1.0×) | 0.403 ms (1.0×) | 0.394 ms (1.0×) | 0.398 ms | 0.392 ms (1.0×) | 0.394 ms (1.0×) | 0.399 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.326 ms (1.0×) | 1.232 ms (1.0×) | 1.276 ms | 0.369 ms (0.3×) | 0.365 ms (0.3×) | 0.403 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.665 ms (1.0×) | 0.649 ms (1.0×) | 0.664 ms (1.0×) | 0.644 ms | 0.728 ms (1.1×) | 0.751 ms (1.2×) | 0.720 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.762 ms (1.0×) | 1.769 ms (1.0×) | 1.763 ms (1.0×) | 1.836 ms | 1.501 ms (0.8×) | 1.542 ms (0.8×) | 1.515 ms (0.8×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.173 ms (1.0×) | 6.095 ms (1.0×) | 6.117 ms (1.0×) | 6.177 ms | 6.057 ms (1.0×) | 6.066 ms (1.0×) | 6.129 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.157 ms (1.0×) | 6.186 ms (1.0×) | 6.175 ms (1.0×) | 6.303 ms | 6.029 ms (1.0×) | 6.032 ms (1.0×) | 6.203 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.806 ms (1.0×) | 5.871 ms (1.0×) | 5.816 ms (1.0×) | 5.949 ms | 5.856 ms (1.0×) | 5.781 ms (1.0×) | 5.961 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.999 ms (1.0×) | 5.949 ms (1.0×) | 5.935 ms (1.0×) | 6.078 ms | 6.055 ms (1.0×) | 6.115 ms (1.0×) | 6.133 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 525 op/s (101%) | 2 481 op/s (98.8%) | 2 538 op/s (101%) | 2 513 op/s | 2 551 op/s (102%) | 2 538 op/s (101%) | 2 506 op/s (99.7%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (104%) | 754.1 op/s (96.2%) | 811.7 op/s (104%) | 783.7 op/s | 2 710 op/s (346%) | 2 740 op/s (350%) | 2 481 op/s (317%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 504 op/s (96.8%) | 1 541 op/s (99.2%) | 1 506 op/s (97.0%) | 1 553 op/s | 1 374 op/s (88.5%) | 1 332 op/s (85.8%) | 1 389 op/s (89.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 567.5 op/s (104%) | 565.3 op/s (104%) | 567.2 op/s (104%) | 544.7 op/s | 666.2 op/s (122%) | 648.5 op/s (119%) | 660.1 op/s (121%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 162.0 op/s (100%) | 164.1 op/s (101%) | 163.5 op/s (101%) | 161.9 op/s | 165.1 op/s (102%) | 164.9 op/s (102%) | 163.2 op/s (101%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.4 op/s (102%) | 161.7 op/s (102%) | 161.9 op/s (102%) | 158.7 op/s | 165.9 op/s (105%) | 165.8 op/s (104%) | 161.2 op/s (102%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 172.2 op/s (102%) | 170.3 op/s (101%) | 171.9 op/s (102%) | 168.1 op/s | 170.8 op/s (102%) | 173.0 op/s (103%) | 167.8 op/s (99.8%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 166.7 op/s (101%) | 168.1 op/s (102%) | 168.5 op/s (102%) | 164.5 op/s | 165.2 op/s (100%) | 163.5 op/s (99.4%) | 163.1 op/s (99.1%) |

![1.4.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 19.83 ms (1.0×) | 19.65 ms (1.0×) | 20.46 ms | 17.28 ms (0.8×) | 17.28 ms (0.8×) | 17.11 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 79.11 ms (1.2×) | 67.00 ms (1.0×) | 67.57 ms | — | 69.08 ms (1.0×) | 67.67 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 67.63 ms (1.0×) | 66.92 ms (0.9×) | 71.04 ms | 66.60 ms (0.9×) | 67.00 ms (0.9×) | 69.03 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.35 ms (1.0×) | 50.47 ms (1.0×) | 50.58 ms | — | 50.11 ms (1.0×) | 50.61 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 50.44 op/s (103%) | 50.88 op/s (104%) | 48.87 op/s | 57.86 op/s (118%) | 57.86 op/s (118%) | 58.46 op/s (120%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 12.64 op/s (85.4%) | 14.93 op/s (101%) | 14.80 op/s | — | 14.48 op/s (97.8%) | 14.78 op/s (99.9%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.79 op/s (105%) | 14.94 op/s (106%) | 14.08 op/s | 15.02 op/s (107%) | 14.92 op/s (106%) | 14.49 op/s (103%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.86 op/s (100%) | 19.82 op/s (100%) | 19.77 op/s | — | 19.96 op/s (101%) | 19.76 op/s (99.9%) |

![1.4.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 576.6 ms (0.9×) | 602.9 ms (1.0×) | 615.4 ms | 410.4 ms (0.7×) | 409.6 ms (0.7×) | 445.8 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4122.2 ms (0.9×) | 4102.7 ms (0.9×) | 4620.4 ms | — | 3844.3 ms (0.8×) | 4435.6 ms (1.0×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3909.4 ms (0.8×) | 3884.4 ms (0.8×) | 4689.5 ms | 4002.5 ms (0.9×) | 3959.5 ms (0.8×) | 4352.3 ms (0.9×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 569.9 ms (1.0×) | 550.5 ms (0.9×) | 598.6 ms | — | 595.1 ms (1.0×) | 557.8 ms (0.9×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.734 op/s (107%) | 1.659 op/s (102%) | 1.625 op/s | 2.437 op/s (150%) | 2.441 op/s (150%) | 2.243 op/s (138%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.243 op/s (112%) | 0.244 op/s (113%) | 0.216 op/s | — | 0.260 op/s (120%) | 0.225 op/s (104%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s (120%) | 0.257 op/s (121%) | 0.213 op/s | 0.250 op/s (117%) | 0.253 op/s (118%) | 0.230 op/s (108%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.755 op/s (105%) | 1.817 op/s (109%) | 1.671 op/s | — | 1.680 op/s (101%) | 1.793 op/s (107%) |

![1.4.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8479.6 ms (1.0×) | 8563.2 ms (1.0×) | 8594.8 ms | 6840.6 ms (0.8×) | 6788.3 ms (0.8×) | 6004.0 ms (0.7×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.118 op/s (101%) | 0.117 op/s (100%) | 0.116 op/s | 0.146 op/s (126%) | 0.147 op/s (127%) | 0.167 op/s (143%) |

![1.4.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-5-ccs-n-100-000-000.svg)

### 1.5 Condense

#### Times @ 1k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 0.011 ms (1.0×) | 0.011 ms |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 0.007 ms (0.9×) | 0.008 ms |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 0.024 ms (1.0×) | 0.024 ms |
| DAG GNP sparse | 0.017 ms (1.0×) | 0.017 ms |

#### Throughput @ 1k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 90 909 op/s (100%) | 90 909 op/s |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 142 857 op/s (114%) | 125 000 op/s |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 41 667 op/s (100%) | 41 667 op/s |
| DAG GNP sparse | 58 824 op/s (100%) | 58 824 op/s |

![1.5.1 Condense @ n = 1 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-1-condense-n-1-000.svg)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.080 ms (1.0×) | 0.080 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.099 ms (1.1×) | 0.092 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.182 ms (1.0×) | 0.185 ms |
| DAG GNP sparse | 0.256 ms (1.0×) | 0.261 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 500 op/s (100%) | 12 500 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 10 101 op/s (92.9%) | 10 870 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 5 495 op/s (102%) | 5 405 op/s |
| DAG GNP sparse | 3 906 op/s (102%) | 3 831 op/s |

![1.5.2 Condense @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-2-condense-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.772 ms (1.0×) | 0.768 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.523 ms (1.0×) | 1.591 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1.722 ms (0.9×) | 1.852 ms |
| DAG GNP sparse | 3.542 ms (1.0×) | 3.641 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 295 op/s (99.5%) | 1 302 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 656.6 op/s (104%) | 628.5 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 580.7 op/s (108%) | 540.0 op/s |
| DAG GNP sparse | 282.3 op/s (103%) | 274.6 op/s |

![1.5.3 Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-3-condense-n-100-000.svg)

### 1.6 TopOrder

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.0×) | 0.058 ms (0.9×) | 0.067 ms | 0.067 ms (1.0×) | 0.066 ms (1.0×) | 0.067 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.099 ms (1.4×) | 0.072 ms (1.0×) | 0.070 ms (1.0×) | 0.070 ms | 0.056 ms (0.8×) | 0.052 ms (0.7×) | 0.052 ms (0.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.198 ms (4.0×) | 0.044 ms (0.9×) | 0.056 ms (1.1×) | 0.049 ms | 0.039 ms (0.8×) | 0.053 ms (1.1×) | 0.037 ms (0.8×) |
| Tree shfld. | 0.088 ms (1.8×) | 0.052 ms (1.0×) | 0.050 ms (1.0×) | 0.050 ms | 0.047 ms (0.9×) | 0.047 ms (0.9×) | 0.047 ms (0.9×) |
| DAG GNP sparse | 0.279 ms (3.8×) | 0.070 ms (0.9×) | 0.084 ms (1.1×) | 0.074 ms | 0.061 ms (0.8×) | 0.067 ms (0.9×) | 0.064 ms (0.9×) |
| DAG GNP dense | 0.448 ms (1.4×) | 0.337 ms (1.0×) | 0.324 ms (1.0×) | 0.324 ms | 0.245 ms (0.8×) | 0.243 ms (0.8×) | 0.245 ms (0.8×) |
| DAG GNP dense shfld. | 0.560 ms (1.5×) | 0.387 ms (1.1×) | 0.371 ms (1.0×) | 0.366 ms | 0.244 ms (0.7×) | 0.233 ms (0.6×) | 0.246 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (116%) | 14 925 op/s (100%) | 17 241 op/s (116%) | 14 925 op/s | 14 925 op/s (100%) | 15 152 op/s (102%) | 14 925 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 10 101 op/s (70.7%) | 13 889 op/s (97.2%) | 14 286 op/s (100%) | 14 286 op/s | 17 857 op/s (125%) | 19 231 op/s (135%) | 19 231 op/s (135%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 051 op/s (24.7%) | 22 727 op/s (111%) | 17 857 op/s (87.5%) | 20 408 op/s | 25 641 op/s (126%) | 18 868 op/s (92.5%) | 27 027 op/s (132%) |
| Tree shfld. | 11 364 op/s (56.8%) | 19 231 op/s (96.2%) | 20 000 op/s (100%) | 20 000 op/s | 21 277 op/s (106%) | 21 277 op/s (106%) | 21 277 op/s (106%) |
| DAG GNP sparse | 3 584 op/s (26.5%) | 14 286 op/s (106%) | 11 905 op/s (88.1%) | 13 514 op/s | 16 393 op/s (121%) | 14 925 op/s (110%) | 15 625 op/s (116%) |
| DAG GNP dense | 2 232 op/s (72.3%) | 2 967 op/s (96.1%) | 3 086 op/s (100%) | 3 086 op/s | 4 082 op/s (132%) | 4 115 op/s (133%) | 4 082 op/s (132%) |
| DAG GNP dense shfld. | 1 786 op/s (65.4%) | 2 584 op/s (94.6%) | 2 695 op/s (98.7%) | 2 732 op/s | 4 098 op/s (150%) | 4 292 op/s (157%) | 4 065 op/s (149%) |

![1.6.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.539 ms (0.9×) | 0.656 ms (1.0×) | 0.632 ms (1.0×) | 0.632 ms | 0.662 ms (1.0×) | 0.629 ms (1.0×) | 0.628 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.287 ms (1.2×) | 1.051 ms (1.0×) | 0.964 ms (0.9×) | 1.068 ms | 0.566 ms (0.5×) | 0.521 ms (0.5×) | 0.528 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.963 ms (7.0×) | 1.311 ms (0.9×) | 1.440 ms (1.0×) | 1.426 ms | 1.023 ms (0.7×) | 1.142 ms (0.8×) | 1.182 ms (0.8×) |
| Tree shfld. | 5.091 ms (3.4×) | 1.487 ms (1.0×) | 1.475 ms (1.0×) | 1.479 ms | 1.212 ms (0.8×) | 1.242 ms (0.8×) | 1.255 ms (0.8×) |
| DAG GNP sparse | 9.521 ms (4.2×) | 2.251 ms (1.0×) | 2.201 ms (1.0×) | 2.242 ms | 1.480 ms (0.7×) | 1.470 ms (0.7×) | 1.499 ms (0.7×) |
| DAG GNP dense | 6.487 ms (1.4×) | 4.533 ms (1.0×) | 4.333 ms (1.0×) | 4.515 ms | 3.208 ms (0.7×) | 3.086 ms (0.7×) | 3.127 ms (0.7×) |
| DAG GNP dense shfld. | 8.053 ms (1.6×) | 5.303 ms (1.0×) | 5.127 ms (1.0×) | 5.063 ms | 3.254 ms (0.6×) | 3.085 ms (0.6×) | 3.173 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 855 op/s (117%) | 1 524 op/s (96.3%) | 1 582 op/s (100%) | 1 582 op/s | 1 511 op/s (95.5%) | 1 590 op/s (100%) | 1 592 op/s (101%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 777.0 op/s (83.0%) | 951.5 op/s (102%) | 1 037 op/s (111%) | 936.3 op/s | 1 767 op/s (189%) | 1 919 op/s (205%) | 1 894 op/s (202%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 100.4 op/s (14.3%) | 762.8 op/s (109%) | 694.4 op/s (99.0%) | 701.3 op/s | 977.5 op/s (139%) | 875.7 op/s (125%) | 846.0 op/s (121%) |
| Tree shfld. | 196.4 op/s (29.1%) | 672.5 op/s (99.5%) | 678.0 op/s (100%) | 676.1 op/s | 825.1 op/s (122%) | 805.2 op/s (119%) | 796.8 op/s (118%) |
| DAG GNP sparse | 105.0 op/s (23.5%) | 444.2 op/s (99.6%) | 454.3 op/s (102%) | 446.0 op/s | 675.7 op/s (151%) | 680.3 op/s (153%) | 667.1 op/s (150%) |
| DAG GNP dense | 154.2 op/s (69.6%) | 220.6 op/s (99.6%) | 230.8 op/s (104%) | 221.5 op/s | 311.7 op/s (141%) | 324.0 op/s (146%) | 319.8 op/s (144%) |
| DAG GNP dense shfld. | 124.2 op/s (62.9%) | 188.6 op/s (95.5%) | 195.0 op/s (98.8%) | 197.5 op/s | 307.3 op/s (156%) | 324.1 op/s (164%) | 315.2 op/s (160%) |

![1.6.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 30.49 ms (1.0×) | 30.46 ms (1.0×) | 31.70 ms | 15.46 ms (0.5×) | 14.97 ms (0.5×) | 15.34 ms (0.5×) |
| DAG GNP dense | 103.7 ms (1.0×) | 103.6 ms (1.0×) | 106.4 ms | — | 37.35 ms (0.4×) | 38.21 ms (0.4×) |
| DAG GNP dense shfld. | 176.2 ms (1.0×) | 173.5 ms (1.0×) | 177.6 ms | 39.69 ms (0.2×) | 38.60 ms (0.2×) | 39.11 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 32.80 op/s (104%) | 32.83 op/s (104%) | 31.55 op/s | 64.68 op/s (205%) | 66.82 op/s (212%) | 65.18 op/s (207%) |
| DAG GNP dense | 9.643 op/s (103%) | 9.653 op/s (103%) | 9.398 op/s | — | 26.77 op/s (285%) | 26.17 op/s (278%) |
| DAG GNP dense shfld. | 5.675 op/s (101%) | 5.764 op/s (102%) | 5.631 op/s | 25.20 op/s (447%) | 25.90 op/s (460%) | 25.57 op/s (454%) |

![1.6.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1170.9 ms (0.9×) | 1145.1 ms (0.9×) | 1241.1 ms | 160.8 ms (0.1×) | 156.7 ms (0.1×) | 163.7 ms (0.1×) |
| DAG GNP dense | 1966.4 ms (1.0×) | 1890.5 ms (0.9×) | 2005.5 ms | — | 450.2 ms (0.2×) | 466.3 ms (0.2×) |
| DAG GNP dense shfld. | 2823.5 ms (1.0×) | 2805.5 ms (1.0×) | 2927.1 ms | 414.3 ms (0.1×) | 405.1 ms (0.1×) | 418.1 ms (0.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.854 op/s (106%) | 0.873 op/s (108%) | 0.806 op/s | 6.219 op/s (772%) | 6.382 op/s (792%) | 6.109 op/s (758%) |
| DAG GNP dense | 0.509 op/s (102%) | 0.529 op/s (106%) | 0.499 op/s | — | 2.221 op/s (445%) | 2.145 op/s (430%) |
| DAG GNP dense shfld. | 0.354 op/s (104%) | 0.356 op/s (104%) | 0.342 op/s | 2.414 op/s (707%) | 2.469 op/s (723%) | 2.392 op/s (700%) |

![1.6.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 17135 ms (0.9×) | 17065 ms (0.9×) | 18079 ms | 1810.4 ms (0.1×) | 1759.8 ms (0.1×) | 1855.1 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.058 op/s (106%) | 0.059 op/s (106%) | 0.055 op/s | 0.552 op/s (999%) | 0.568 op/s (1027%) | 0.539 op/s (975%) |

![1.6.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-5-toporder-n-100-000-000.svg)

### 1.7 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.040 ms (1.0×) | 0.039 ms | 0.037 ms (0.9×) | 0.040 ms (1.0×) | 0.039 ms (1.0×) | 0.041 ms (1.1×) | 0.041 ms (1.1×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.055 ms (1.0×) | 0.056 ms (1.0×) | 0.056 ms (1.0×) | 0.057 ms | 0.038 ms (0.7×) | 0.040 ms (0.7×) | 0.040 ms (0.7×) | 0.054 ms (0.9×) | 0.055 ms (1.0×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.034 ms (1.0×) | 0.035 ms | 0.030 ms (0.9×) | 0.027 ms (0.8×) | 0.031 ms (0.9×) | 0.028 ms (0.8×) | 0.029 ms (0.8×) |
| Tree shfld. | 0.034 ms (1.1×) | 0.031 ms (1.0×) | 0.034 ms (1.1×) | 0.032 ms | 0.031 ms (1.0×) | 0.031 ms (1.0×) | 0.031 ms (1.0×) | 0.030 ms (0.9×) | 0.030 ms (0.9×) |
| DAG GNP sparse | 0.052 ms (0.9×) | 0.057 ms (0.9×) | 0.052 ms (0.9×) | 0.061 ms | 0.050 ms (0.8×) | 0.054 ms (0.9×) | 0.054 ms (0.9×) | 0.042 ms (0.7×) | 0.045 ms (0.7×) |
| DAG GNP dense | 0.364 ms (1.0×) | 0.358 ms (1.0×) | 0.355 ms (1.0×) | 0.364 ms | 0.326 ms (0.9×) | 0.324 ms (0.9×) | 0.334 ms (0.9×) | 0.240 ms (0.7×) | 0.249 ms (0.7×) |
| DAG GNP dense shfld. | 0.393 ms (1.0×) | 0.396 ms (1.0×) | 0.400 ms (1.0×) | 0.404 ms | 0.323 ms (0.8×) | 0.321 ms (0.8×) | 0.328 ms (0.8×) | 0.289 ms (0.7×) | 0.295 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s (100%) | 25 641 op/s (100%) | 25 000 op/s (97.5%) | 25 641 op/s | 27 027 op/s (105%) | 25 000 op/s (97.5%) | 25 641 op/s (100%) | 24 390 op/s (95.1%) | 24 390 op/s (95.1%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 182 op/s (104%) | 17 857 op/s (102%) | 17 857 op/s (102%) | 17 544 op/s | 26 316 op/s (150%) | 25 000 op/s (142%) | 25 000 op/s (142%) | 18 519 op/s (106%) | 18 182 op/s (104%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 32 258 op/s (113%) | 32 258 op/s (113%) | 29 412 op/s (103%) | 28 571 op/s | 33 333 op/s (117%) | 37 037 op/s (130%) | 32 258 op/s (113%) | 35 714 op/s (125%) | 34 483 op/s (121%) |
| Tree shfld. | 29 412 op/s (94.1%) | 32 258 op/s (103%) | 29 412 op/s (94.1%) | 31 250 op/s | 32 258 op/s (103%) | 32 258 op/s (103%) | 32 258 op/s (103%) | 33 333 op/s (107%) | 33 333 op/s (107%) |
| DAG GNP sparse | 19 231 op/s (117%) | 17 544 op/s (107%) | 19 231 op/s (117%) | 16 393 op/s | 20 000 op/s (122%) | 18 519 op/s (113%) | 18 519 op/s (113%) | 23 810 op/s (145%) | 22 222 op/s (136%) |
| DAG GNP dense | 2 747 op/s (100%) | 2 793 op/s (102%) | 2 817 op/s (103%) | 2 747 op/s | 3 067 op/s (112%) | 3 086 op/s (112%) | 2 994 op/s (109%) | 4 167 op/s (152%) | 4 016 op/s (146%) |
| DAG GNP dense shfld. | 2 545 op/s (103%) | 2 525 op/s (102%) | 2 500 op/s (101%) | 2 475 op/s | 3 096 op/s (125%) | 3 115 op/s (126%) | 3 049 op/s (123%) | 3 460 op/s (140%) | 3 390 op/s (137%) |

![1.7.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.562 ms (1.2×) | 0.456 ms (1.0×) | 0.457 ms (1.0×) | 0.462 ms | 0.560 ms (1.2×) | 0.458 ms (1.0×) | 0.571 ms (1.2×) | 0.348 ms (0.8×) | 0.360 ms (0.8×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.463 ms (1.0×) | 1.378 ms (1.0×) | 1.459 ms (1.0×) | 1.424 ms | 0.559 ms (0.4×) | 0.564 ms (0.4×) | 0.565 ms (0.4×) | 1.253 ms (0.9×) | 1.335 ms (0.9×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.934 ms (1.0×) | 0.929 ms (1.0×) | 0.927 ms (1.0×) | 0.947 ms | 0.863 ms (0.9×) | 0.782 ms (0.8×) | 0.845 ms (0.9×) | 0.839 ms (0.9×) | 0.852 ms (0.9×) |
| Tree shfld. | 1.048 ms (1.1×) | 0.945 ms (1.0×) | 1.032 ms (1.1×) | 0.954 ms | 0.829 ms (0.9×) | 0.891 ms (0.9×) | 0.893 ms (0.9×) | 0.884 ms (0.9×) | 0.896 ms (0.9×) |
| DAG GNP sparse | 2.167 ms (1.0×) | 2.211 ms (1.0×) | 2.159 ms (1.0×) | 2.261 ms | 1.974 ms (0.9×) | 1.962 ms (0.9×) | 1.985 ms (0.9×) | 1.989 ms (0.9×) | 1.981 ms (0.9×) |
| DAG GNP dense | 5.160 ms (1.0×) | 5.093 ms (0.9×) | 5.070 ms (0.9×) | 5.364 ms | 4.574 ms (0.9×) | 4.551 ms (0.8×) | 4.696 ms (0.9×) | 3.777 ms (0.7×) | 4.360 ms (0.8×) |
| DAG GNP dense shfld. | 5.877 ms (1.0×) | 5.806 ms (1.0×) | 5.763 ms (1.0×) | 5.866 ms | 4.496 ms (0.8×) | 4.496 ms (0.8×) | 4.690 ms (0.8×) | 4.487 ms (0.8×) | 4.509 ms (0.8×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 779 op/s (82.2%) | 2 193 op/s (101%) | 2 188 op/s (101%) | 2 165 op/s | 1 786 op/s (82.5%) | 2 183 op/s (101%) | 1 751 op/s (80.9%) | 2 874 op/s (133%) | 2 778 op/s (128%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 683.5 op/s (97.3%) | 725.7 op/s (103%) | 685.4 op/s (97.6%) | 702.2 op/s | 1 789 op/s (255%) | 1 773 op/s (252%) | 1 770 op/s (252%) | 798.1 op/s (114%) | 749.1 op/s (107%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 071 op/s (101%) | 1 076 op/s (102%) | 1 079 op/s (102%) | 1 056 op/s | 1 159 op/s (110%) | 1 279 op/s (121%) | 1 183 op/s (112%) | 1 192 op/s (113%) | 1 174 op/s (111%) |
| Tree shfld. | 954.2 op/s (91.0%) | 1 058 op/s (101%) | 969.0 op/s (92.4%) | 1 048 op/s | 1 206 op/s (115%) | 1 122 op/s (107%) | 1 120 op/s (107%) | 1 131 op/s (108%) | 1 116 op/s (106%) |
| DAG GNP sparse | 461.5 op/s (104%) | 452.3 op/s (102%) | 463.2 op/s (105%) | 442.3 op/s | 506.6 op/s (115%) | 509.7 op/s (115%) | 503.8 op/s (114%) | 502.8 op/s (114%) | 504.8 op/s (114%) |
| DAG GNP dense | 193.8 op/s (104%) | 196.3 op/s (105%) | 197.2 op/s (106%) | 186.4 op/s | 218.6 op/s (117%) | 219.7 op/s (118%) | 212.9 op/s (114%) | 264.8 op/s (142%) | 229.4 op/s (123%) |
| DAG GNP dense shfld. | 170.2 op/s (99.8%) | 172.2 op/s (101%) | 173.5 op/s (102%) | 170.5 op/s | 222.4 op/s (130%) | 222.4 op/s (130%) | 213.2 op/s (125%) | 222.9 op/s (131%) | 221.8 op/s (130%) |

![1.7.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 29.60 ms (0.9×) | 29.09 ms (0.9×) | 32.07 ms | 26.85 ms (0.8×) | 25.49 ms (0.8×) | 27.96 ms (0.9×) |
| DAG GNP dense | 112.1 ms (1.0×) | 113.1 ms (1.0×) | 112.9 ms | — | 92.44 ms (0.8×) | 95.81 ms (0.8×) |
| DAG GNP dense shfld. | 191.1 ms (0.9×) | 190.3 ms (0.9×) | 206.4 ms | 89.45 ms (0.4×) | 92.20 ms (0.4×) | 96.05 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 33.78 op/s (108%) | 34.38 op/s (110%) | 31.18 op/s | 37.24 op/s (119%) | 39.23 op/s (126%) | 35.76 op/s (115%) |
| DAG GNP dense | 8.921 op/s (101%) | 8.842 op/s (99.8%) | 8.857 op/s | — | 10.82 op/s (122%) | 10.44 op/s (118%) |
| DAG GNP dense shfld. | 5.233 op/s (108%) | 5.255 op/s (108%) | 4.845 op/s | 11.18 op/s (231%) | 10.85 op/s (224%) | 10.41 op/s (215%) |

![1.7.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 856.2 ms (1.0×) | 853.6 ms (1.0×) | 877.4 ms | 751.9 ms (0.9×) | 759.4 ms (0.9×) | 772.4 ms (0.9×) |
| DAG GNP dense | 3085.9 ms (0.9×) | 2971.3 ms (0.9×) | 3289.1 ms | — | 2553.0 ms (0.8×) | 3050.5 ms (0.9×) |
| DAG GNP dense shfld. | 4398.5 ms (0.9×) | 4386.9 ms (0.9×) | 4671.3 ms | 2631.0 ms (0.6×) | 2623.6 ms (0.6×) | 2667.8 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1.168 op/s (102%) | 1.172 op/s (103%) | 1.140 op/s | 1.330 op/s (117%) | 1.317 op/s (116%) | 1.295 op/s (114%) |
| DAG GNP dense | 0.324 op/s (107%) | 0.337 op/s (111%) | 0.304 op/s | — | 0.392 op/s (129%) | 0.328 op/s (108%) |
| DAG GNP dense shfld. | 0.227 op/s (106%) | 0.228 op/s (106%) | 0.214 op/s | 0.380 op/s (178%) | 0.381 op/s (178%) | 0.375 op/s (175%) |

![1.7.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 11384 ms (1.0×) | 11412 ms (1.0×) | 11575 ms | 9441.1 ms (0.8×) | 9419.7 ms (0.8×) | 9530.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.088 op/s (102%) | 0.088 op/s (101%) | 0.086 op/s | 0.106 op/s (123%) | 0.106 op/s (123%) | 0.105 op/s (121%) |

![1.7.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-5-toporderandlevels-n-100-000-000.svg)

### 1.8 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.047 ms (0.9×) | 0.061 ms (1.2×) | 0.060 ms (1.2×) | 0.052 ms | 0.061 ms (1.2×) | 0.060 ms (1.2×) | 0.061 ms (1.2×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.0×) | 0.065 ms (1.0×) | 0.065 ms | 0.051 ms (0.8×) | 0.050 ms (0.8×) | 0.051 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.223 ms (4.6×) | 0.049 ms (1.0×) | 0.049 ms (1.0×) | 0.049 ms | 0.048 ms (1.0×) | 0.046 ms (0.9×) | 0.047 ms (1.0×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.207 ms (1.4×) | 0.119 ms (0.8×) | 0.164 ms (1.1×) | 0.143 ms | 0.090 ms (0.6×) | 0.087 ms (0.6×) | 0.093 ms (0.7×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.797 ms (1.0×) | 0.776 ms (1.0×) | 0.771 ms (1.0×) | 0.775 ms | 0.647 ms (0.8×) | 0.665 ms (0.9×) | 0.658 ms (0.8×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.811 ms (1.0×) | 0.779 ms (1.0×) | 0.780 ms (1.0×) | 0.776 ms | 0.669 ms (0.9×) | 0.652 ms (0.8×) | 0.662 ms (0.9×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.512 ms (0.9×) | 0.592 ms (1.0×) | 0.582 ms (1.0×) | 0.582 ms | 0.592 ms (1.0×) | 0.580 ms (1.0×) | 0.596 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.619 ms (0.9×) | 0.644 ms (1.0×) | 0.642 ms (1.0×) | 0.657 ms | 0.588 ms (0.9×) | 0.634 ms (1.0×) | 0.588 ms (0.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 21 277 op/s (111%) | 16 393 op/s (85.2%) | 16 667 op/s (86.7%) | 19 231 op/s | 16 393 op/s (85.2%) | 16 667 op/s (86.7%) | 16 393 op/s (85.2%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (112%) | 14 925 op/s (97.0%) | 15 385 op/s (100%) | 15 385 op/s | 19 608 op/s (127%) | 20 000 op/s (130%) | 19 608 op/s (127%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 484 op/s (22.0%) | 20 408 op/s (100%) | 20 408 op/s (100%) | 20 408 op/s | 20 833 op/s (102%) | 21 739 op/s (107%) | 21 277 op/s (104%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 831 op/s (69.1%) | 8 403 op/s (120%) | 6 098 op/s (87.2%) | 6 993 op/s | 11 111 op/s (159%) | 11 494 op/s (164%) | 10 753 op/s (154%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 255 op/s (97.2%) | 1 289 op/s (99.9%) | 1 297 op/s (101%) | 1 290 op/s | 1 546 op/s (120%) | 1 504 op/s (117%) | 1 520 op/s (118%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 233 op/s (95.7%) | 1 284 op/s (99.6%) | 1 282 op/s (99.5%) | 1 289 op/s | 1 495 op/s (116%) | 1 534 op/s (119%) | 1 511 op/s (117%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 953 op/s (114%) | 1 689 op/s (98.3%) | 1 718 op/s (100%) | 1 718 op/s | 1 689 op/s (98.3%) | 1 724 op/s (100%) | 1 678 op/s (97.7%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 616 op/s (106%) | 1 553 op/s (102%) | 1 558 op/s (102%) | 1 522 op/s | 1 701 op/s (112%) | 1 577 op/s (104%) | 1 701 op/s (112%) |

![1.8.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.507 ms (0.9×) | 0.606 ms (1.1×) | 0.563 ms (1.0×) | 0.568 ms | 0.605 ms (1.1×) | 0.556 ms (1.0×) | 0.569 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.351 ms (1.4×) | 0.945 ms (1.0×) | 1.063 ms (1.1×) | 0.994 ms | 0.510 ms (0.5×) | 0.513 ms (0.5×) | 0.506 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.871 ms (7.6×) | 1.327 ms (1.0×) | 1.271 ms (1.0×) | 1.295 ms | 1.083 ms (0.8×) | 0.872 ms (0.7×) | 1.035 ms (0.8×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.691 ms (1.5×) | 3.177 ms (1.0×) | 3.086 ms (1.0×) | 3.124 ms | 1.962 ms (0.6×) | 1.890 ms (0.6×) | 1.895 ms (0.6×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.3×) | 12.91 ms (1.0×) | 12.72 ms (0.9×) | 13.51 ms | 7.303 ms (0.5×) | 7.055 ms (0.5×) | 7.279 ms (0.5×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.3×) | 12.63 ms (0.9×) | 12.71 ms (0.9×) | 13.88 ms | 7.353 ms (0.5×) | 7.081 ms (0.5×) | 7.242 ms (0.5×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.563 ms (1.4×) | 6.365 ms (1.0×) | 6.191 ms (1.0×) | 6.179 ms | 6.292 ms (1.0×) | 6.108 ms (1.0×) | 6.220 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 13.28 ms (1.1×) | 11.15 ms (0.9×) | 11.15 ms (0.9×) | 12.63 ms | 6.545 ms (0.5×) | 6.298 ms (0.5×) | 6.482 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 972 op/s (112%) | 1 650 op/s (93.7%) | 1 776 op/s (101%) | 1 761 op/s | 1 653 op/s (93.9%) | 1 799 op/s (102%) | 1 757 op/s (99.8%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 740.2 op/s (73.6%) | 1 058 op/s (105%) | 940.7 op/s (93.5%) | 1 006 op/s | 1 961 op/s (195%) | 1 949 op/s (194%) | 1 976 op/s (196%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 101.3 op/s (13.1%) | 753.6 op/s (97.6%) | 786.8 op/s (102%) | 772.2 op/s | 923.4 op/s (120%) | 1 147 op/s (149%) | 966.2 op/s (125%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 213.2 op/s (66.6%) | 314.8 op/s (98.3%) | 324.0 op/s (101%) | 320.1 op/s | 509.7 op/s (159%) | 529.1 op/s (165%) | 527.7 op/s (165%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (77.7%) | 77.44 op/s (105%) | 78.61 op/s (106%) | 74.05 op/s | 136.9 op/s (185%) | 141.7 op/s (191%) | 137.4 op/s (186%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (79.8%) | 79.20 op/s (110%) | 78.71 op/s (109%) | 72.07 op/s | 136.0 op/s (189%) | 141.2 op/s (196%) | 138.1 op/s (192%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 116.8 op/s (72.2%) | 157.1 op/s (97.1%) | 161.5 op/s (99.8%) | 161.8 op/s | 158.9 op/s (98.2%) | 163.7 op/s (101%) | 160.8 op/s (99.3%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 75.31 op/s (95.1%) | 89.65 op/s (113%) | 89.69 op/s (113%) | 79.16 op/s | 152.8 op/s (193%) | 158.8 op/s (201%) | 154.3 op/s (195%) |

![1.8.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 49.47 ms (1.0×) | 45.23 ms (0.9×) | 49.41 ms | 20.62 ms (0.4×) | 19.54 ms (0.4×) | 19.64 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 240.2 ms (1.0×) | 242.3 ms (1.0×) | 251.1 ms | — | 92.69 ms (0.4×) | 93.71 ms (0.4×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 241.4 ms (1.0×) | 240.9 ms (1.0×) | 253.4 ms | 94.11 ms (0.4×) | 90.99 ms (0.4×) | 92.78 ms (0.4×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 75.32 ms (1.0×) | 76.46 ms (1.0×) | 77.08 ms | — | 76.53 ms (1.0×) | 77.78 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 20.22 op/s (99.9%) | 22.11 op/s (109%) | 20.24 op/s | 48.50 op/s (240%) | 51.17 op/s (253%) | 50.91 op/s (252%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.163 op/s (105%) | 4.127 op/s (104%) | 3.982 op/s | — | 10.79 op/s (271%) | 10.67 op/s (268%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.143 op/s (105%) | 4.151 op/s (105%) | 3.946 op/s | 10.63 op/s (269%) | 10.99 op/s (279%) | 10.78 op/s (273%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 13.28 op/s (102%) | 13.08 op/s (101%) | 12.97 op/s | — | 13.07 op/s (101%) | 12.86 op/s (99.1%) |

![1.8.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1984.9 ms (1.0×) | 1952.3 ms (1.0×) | 2052.4 ms | 254.0 ms (0.1×) | 244.4 ms (0.1×) | 244.8 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3708.4 ms (1.0×) | 3722.5 ms (1.0×) | 3843.7 ms | — | 1223.7 ms (0.3×) | 1022.2 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3672.0 ms (0.9×) | 3874.8 ms (1.0×) | 3989.1 ms | 930.6 ms (0.2×) | 1124.4 ms (0.3×) | 1135.8 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 815.0 ms (1.0×) | 839.7 ms (1.0×) | 838.2 ms | — | 797.1 ms (1.0×) | 802.5 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.504 op/s (103%) | 0.512 op/s (105%) | 0.487 op/s | 3.937 op/s (808%) | 4.092 op/s (840%) | 4.085 op/s (838%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.270 op/s (104%) | 0.269 op/s (103%) | 0.260 op/s | — | 0.817 op/s (314%) | 0.978 op/s (376%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.272 op/s (109%) | 0.258 op/s (103%) | 0.251 op/s | 1.075 op/s (429%) | 0.889 op/s (355%) | 0.880 op/s (351%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.227 op/s (103%) | 1.191 op/s (99.8%) | 1.193 op/s | — | 1.255 op/s (105%) | 1.246 op/s (104%) |

![1.8.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 27110 ms (1.0×) | 26787 ms (1.0×) | 28157 ms | 2988.7 ms (0.1×) | 2799.7 ms (0.1×) | 3364.5 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.037 op/s (104%) | 0.037 op/s (105%) | 0.036 op/s | 0.335 op/s (942%) | 0.357 op/s (1006%) | 0.297 op/s (837%) |

![1.8.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-5-backedges-n-100-000-000.svg)

### 1.9 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.051 ms (0.8×) | 0.064 ms (1.0×) | 0.063 ms (1.0×) | 0.064 ms | 0.064 ms (1.0×) | 0.064 ms (1.0×) | 0.065 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.066 ms (0.9×) | 0.077 ms (1.0×) | 0.077 ms (1.0×) | 0.077 ms | 0.059 ms (0.8×) | 0.060 ms (0.8×) | 0.062 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.196 ms (3.7×) | 0.053 ms (1.0×) | 0.053 ms (1.0×) | 0.053 ms | 0.046 ms (0.9×) | 0.046 ms (0.9×) | 0.047 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.222 ms (1.2×) | 0.183 ms (1.0×) | 0.186 ms (1.0×) | 0.182 ms | 0.093 ms (0.5×) | 0.094 ms (0.5×) | 0.090 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.653 ms (1.2×) | 0.511 ms (1.0×) | 0.530 ms (1.0×) | 0.533 ms | 0.373 ms (0.7×) | 0.377 ms (0.7×) | 0.369 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.658 ms (1.2×) | 0.514 ms (1.0×) | 0.525 ms (1.0×) | 0.528 ms | 0.372 ms (0.7×) | 0.380 ms (0.7×) | 0.382 ms (0.7×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.375 ms (1.3×) | 0.282 ms (1.0×) | 0.296 ms (1.0×) | 0.296 ms | 0.285 ms (1.0×) | 0.297 ms (1.0×) | 0.303 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.499 ms (1.2×) | 0.383 ms (1.0×) | 0.394 ms (1.0×) | 0.402 ms | 0.276 ms (0.7×) | 0.297 ms (0.7×) | 0.300 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 19 608 op/s (125%) | 15 625 op/s (100%) | 15 873 op/s (102%) | 15 625 op/s | 15 625 op/s (100%) | 15 625 op/s (100%) | 15 385 op/s (98.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 152 op/s (117%) | 12 987 op/s (100.0%) | 12 987 op/s (100.0%) | 12 987 op/s | 16 949 op/s (131%) | 16 667 op/s (128%) | 16 129 op/s (124%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 102 op/s (27.0%) | 18 868 op/s (100%) | 18 868 op/s (100%) | 18 868 op/s | 21 739 op/s (115%) | 21 739 op/s (115%) | 21 277 op/s (113%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 505 op/s (82.0%) | 5 464 op/s (99.5%) | 5 376 op/s (97.8%) | 5 495 op/s | 10 753 op/s (196%) | 10 638 op/s (194%) | 11 111 op/s (202%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 531 op/s (81.6%) | 1 957 op/s (104%) | 1 887 op/s (101%) | 1 876 op/s | 2 681 op/s (143%) | 2 653 op/s (141%) | 2 710 op/s (144%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 520 op/s (80.2%) | 1 946 op/s (103%) | 1 905 op/s (101%) | 1 894 op/s | 2 688 op/s (142%) | 2 632 op/s (139%) | 2 618 op/s (138%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 2 667 op/s (78.9%) | 3 546 op/s (105%) | 3 378 op/s (100%) | 3 378 op/s | 3 509 op/s (104%) | 3 367 op/s (99.7%) | 3 300 op/s (97.7%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 004 op/s (80.6%) | 2 611 op/s (105%) | 2 538 op/s (102%) | 2 488 op/s | 3 623 op/s (146%) | 3 367 op/s (135%) | 3 333 op/s (134%) |

![1.9.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.554 ms (0.9×) | 0.647 ms (1.1×) | 0.607 ms (1.0×) | 0.603 ms | 0.642 ms (1.1×) | 0.592 ms (1.0×) | 0.605 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.563 ms (1.1×) | 1.365 ms (1.0×) | 1.408 ms (1.0×) | 1.430 ms | 0.615 ms (0.4×) | 0.629 ms (0.4×) | 0.634 ms (0.4×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 10.29 ms (6.9×) | 1.288 ms (0.9×) | 1.480 ms (1.0×) | 1.497 ms | 1.022 ms (0.7×) | 1.066 ms (0.7×) | 1.071 ms (0.7×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.006 ms (1.4×) | 3.626 ms (1.0×) | 3.587 ms (1.0×) | 3.696 ms | 1.957 ms (0.5×) | 1.853 ms (0.5×) | 1.962 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.351 ms (1.2×) | 7.009 ms (0.9×) | 6.926 ms (0.9×) | 7.537 ms | 4.296 ms (0.6×) | 4.197 ms (0.6×) | 4.320 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 9.400 ms (1.2×) | 7.103 ms (0.9×) | 6.963 ms (0.9×) | 7.584 ms | 4.185 ms (0.6×) | 4.193 ms (0.6×) | 4.299 ms (0.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.372 ms (1.6×) | 2.812 ms (1.0×) | 2.706 ms (1.0×) | 2.696 ms | 2.815 ms (1.0×) | 2.689 ms (1.0×) | 2.754 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.766 ms (1.1×) | 5.303 ms (0.9×) | 5.599 ms (0.9×) | 6.171 ms | 3.053 ms (0.5×) | 2.982 ms (0.5×) | 2.973 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 805 op/s (109%) | 1 546 op/s (93.2%) | 1 647 op/s (99.3%) | 1 658 op/s | 1 558 op/s (93.9%) | 1 689 op/s (102%) | 1 653 op/s (99.7%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 639.8 op/s (91.5%) | 732.6 op/s (105%) | 710.2 op/s (102%) | 699.3 op/s | 1 626 op/s (233%) | 1 590 op/s (227%) | 1 577 op/s (226%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 97.17 op/s (14.5%) | 776.4 op/s (116%) | 675.7 op/s (101%) | 668.0 op/s | 978.5 op/s (146%) | 938.1 op/s (140%) | 933.7 op/s (140%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 199.8 op/s (73.8%) | 275.8 op/s (102%) | 278.8 op/s (103%) | 270.6 op/s | 511.0 op/s (189%) | 539.7 op/s (199%) | 509.7 op/s (188%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 106.9 op/s (80.6%) | 142.7 op/s (108%) | 144.4 op/s (109%) | 132.7 op/s | 232.8 op/s (175%) | 238.3 op/s (180%) | 231.5 op/s (174%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 106.4 op/s (80.7%) | 140.8 op/s (107%) | 143.6 op/s (109%) | 131.9 op/s | 238.9 op/s (181%) | 238.5 op/s (181%) | 232.6 op/s (176%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 228.7 op/s (61.7%) | 355.6 op/s (95.9%) | 369.5 op/s (99.6%) | 370.9 op/s | 355.2 op/s (95.8%) | 371.9 op/s (100%) | 363.1 op/s (97.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 147.8 op/s (91.2%) | 188.6 op/s (116%) | 178.6 op/s (110%) | 162.0 op/s | 327.5 op/s (202%) | 335.3 op/s (207%) | 336.4 op/s (208%) |

![1.9.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 60.41 ms (0.9×) | 58.24 ms (0.8×) | 69.55 ms | 23.47 ms (0.3×) | 20.58 ms (0.3×) | 22.69 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 235.5 ms (0.9×) | 238.8 ms (0.9×) | 252.1 ms | — | 60.05 ms (0.2×) | 60.84 ms (0.2×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 231.9 ms (0.9×) | 235.5 ms (0.9×) | 252.0 ms | 60.28 ms (0.2×) | 60.09 ms (0.2×) | 60.77 ms (0.2×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.97 ms (1.0×) | 26.59 ms (1.0×) | 26.76 ms | — | 26.68 ms (1.0×) | 26.80 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 16.55 op/s (115%) | 17.17 op/s (119%) | 14.38 op/s | 42.60 op/s (296%) | 48.59 op/s (338%) | 44.08 op/s (307%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.246 op/s (107%) | 4.188 op/s (106%) | 3.967 op/s | — | 16.65 op/s (420%) | 16.44 op/s (414%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.312 op/s (109%) | 4.246 op/s (107%) | 3.968 op/s | 16.59 op/s (418%) | 16.64 op/s (419%) | 16.46 op/s (415%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 38.51 op/s (103%) | 37.62 op/s (101%) | 37.37 op/s | — | 37.48 op/s (100%) | 37.32 op/s (99.9%) |

![1.9.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2629.9 ms (1.0×) | 2492.1 ms (1.0×) | 2557.2 ms | 422.5 ms (0.2×) | 414.7 ms (0.2×) | 447.1 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4515.8 ms (0.9×) | 4552.4 ms (0.9×) | 4861.4 ms | — | 1515.5 ms (0.3×) | 1624.0 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4526.0 ms (0.9×) | 4492.7 ms (0.9×) | 4853.4 ms | 1383.2 ms (0.3×) | 1393.9 ms (0.3×) | 1557.3 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 276.6 ms (0.8×) | 281.8 ms (0.8×) | 336.4 ms | — | 275.9 ms (0.8×) | 284.6 ms (0.8×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.380 op/s (97.2%) | 0.401 op/s (103%) | 0.391 op/s | 2.367 op/s (605%) | 2.411 op/s (617%) | 2.237 op/s (572%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (108%) | 0.220 op/s (107%) | 0.206 op/s | — | 0.660 op/s (321%) | 0.616 op/s (299%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (107%) | 0.223 op/s (108%) | 0.206 op/s | 0.723 op/s (351%) | 0.717 op/s (348%) | 0.642 op/s (312%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.615 op/s (122%) | 3.549 op/s (119%) | 2.973 op/s | — | 3.625 op/s (122%) | 3.514 op/s (118%) |

![1.9.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 31792 ms (1.0×) | 31282 ms (1.0×) | 32754 ms | 7181.5 ms (0.2×) | 6992.4 ms (0.2×) | 7210.5 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.031 op/s (103%) | 0.032 op/s (105%) | 0.031 op/s | 0.139 op/s (456%) | 0.143 op/s (468%) | 0.139 op/s (454%) |

![1.9.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-5-edgetypes-n-100-000-000.svg)

### 1.10 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.121 ms (1.1×) | 0.110 ms (1.0×) | 0.110 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.145 ms (1.1×) | 0.133 ms (1.0×) | 0.130 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.185 ms (2.1×) | 0.087 ms (1.0×) | 0.090 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.483 ms (1.3×) | 0.386 ms (1.0×) | 0.385 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.127 ms (1.4×) | 0.821 ms (1.0×) | 0.818 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.114 ms (1.4×) | 0.812 ms (1.0×) | 0.817 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.715 ms (1.3×) | 0.532 ms (1.0×) | 0.533 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.867 ms (1.4×) | 0.612 ms (1.0×) | 0.618 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 8 264 op/s (90.9%) | 9 091 op/s (100%) | 9 091 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 6 897 op/s (89.7%) | 7 519 op/s (97.7%) | 7 692 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 405 op/s (48.6%) | 11 494 op/s (103%) | 11 111 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 070 op/s (79.7%) | 2 591 op/s (99.7%) | 2 597 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 887.3 op/s (72.6%) | 1 218 op/s (99.6%) | 1 222 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 897.7 op/s (73.3%) | 1 232 op/s (101%) | 1 224 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 399 op/s (74.5%) | 1 880 op/s (100%) | 1 876 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 153 op/s (71.3%) | 1 634 op/s (101%) | 1 618 op/s |

![1.10.1 Reorder (DFS-preorder) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-1-reorder-dfs-preorder-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.259 ms (1.2×) | 1.033 ms (1.0×) | 1.043 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.144 ms (1.1×) | 1.929 ms (1.0×) | 2.016 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.144 ms (1.4×) | 2.215 ms (1.0×) | 2.314 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 6.609 ms (1.2×) | 5.411 ms (1.0×) | 5.436 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 15.07 ms (1.3×) | 10.81 ms (0.9×) | 11.74 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 15.00 ms (1.3×) | 10.87 ms (0.9×) | 11.77 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 7.332 ms (1.4×) | 5.374 ms (1.0×) | 5.406 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 11.95 ms (1.3×) | 8.836 ms (1.0×) | 9.153 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 794.3 op/s (82.8%) | 968.1 op/s (101%) | 958.8 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 466.4 op/s (94.0%) | 518.4 op/s (105%) | 496.0 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 318.1 op/s (73.6%) | 451.5 op/s (104%) | 432.2 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 151.3 op/s (82.3%) | 184.8 op/s (100%) | 184.0 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 66.37 op/s (77.9%) | 92.53 op/s (109%) | 85.17 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 66.66 op/s (78.4%) | 92.00 op/s (108%) | 84.97 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 136.4 op/s (73.7%) | 186.1 op/s (101%) | 185.0 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 83.70 op/s (76.6%) | 113.2 op/s (104%) | 109.3 op/s |

![1.10.2 Reorder (DFS-preorder) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-2-reorder-dfs-preorder-cost-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 95.36 ms (1.1×) | 79.17 ms (0.9×) | 84.36 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 414.3 ms (1.2×) | 315.0 ms (0.9×) | 336.1 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 317.2 ms (0.9×) | 336.7 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 52.67 ms (1.0×) | 53.13 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 10.49 op/s (88.5%) | 12.63 op/s (107%) | 11.85 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 2.414 op/s (81.1%) | 3.175 op/s (107%) | 2.975 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 3.153 op/s (106%) | 2.970 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 18.99 op/s (101%) | 18.82 op/s |

![1.10.3 Reorder (DFS-preorder) cost @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-3-reorder-dfs-preorder-cost-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4011.1 ms (1.2×) | 3263.5 ms (1.0×) | 3401.7 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 10805 ms (1.4×) | 7613.8 ms (1.0×) | 7838.8 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 7509.7 ms (1.0×) | 7782.3 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 617.1 ms (1.0×) | 622.8 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.249 op/s (84.8%) | 0.306 op/s (104%) | 0.294 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.093 op/s (72.5%) | 0.131 op/s (103%) | 0.128 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 0.133 op/s (104%) | 0.128 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 1.620 op/s (101%) | 1.606 op/s |

![1.10.4 Reorder (DFS-preorder) cost @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-4-reorder-dfs-preorder-cost-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 49550 ms (1.2×) | 41091 ms (1.0×) | 41896 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.020 op/s (84.6%) | 0.024 op/s (102%) | 0.024 op/s |

![1.10.5 Reorder (DFS-preorder) cost @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-5-reorder-dfs-preorder-cost-n-100-000-000.svg)

### 1.11 BiAdjacency (in-adjacency) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.022 ms (1.0×) | 0.022 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.018 ms (0.8×) | 0.022 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.016 ms (0.7×) | 0.023 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.058 ms (1.0×) | 0.058 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.546 ms (1.0×) | 0.548 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.544 ms (1.0×) | 0.547 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.545 ms (1.0×) | 0.542 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.548 ms (1.0×) | 0.547 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 45 455 op/s (100%) | 45 455 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 55 556 op/s (122%) | 45 455 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 62 500 op/s (144%) | 43 478 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 17 241 op/s (100%) | 17 241 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 832 op/s (100%) | 1 825 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 838 op/s (101%) | 1 828 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 835 op/s (99.4%) | 1 845 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 825 op/s (99.8%) | 1 828 op/s |

![1.11.1 BiAdjacency (in-adjacency) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-1-biadjacency-in-adjacency-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.185 ms (0.9×) | 0.204 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.238 ms (1.0×) | 0.236 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.612 ms (1.0×) | 0.613 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.837 ms (0.6×) | 1.423 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.148 ms (0.9×) | 7.932 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 7.216 ms (0.9×) | 7.744 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.559 ms (1.0×) | 5.555 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.951 ms (0.9×) | 7.824 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 405 op/s (110%) | 4 902 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 202 op/s (99.2%) | 4 237 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 634 op/s (100%) | 1 631 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 195 op/s (170%) | 702.7 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 139.9 op/s (111%) | 126.1 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 138.6 op/s (107%) | 129.1 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 179.9 op/s (99.9%) | 180.0 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 143.9 op/s (113%) | 127.8 op/s |

![1.11.2 BiAdjacency (in-adjacency) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-2-biadjacency-in-adjacency-cost-n-100-000.svg)

### 1.12 Dominators

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 0.168 ms |
| Irreducible | 0.178 ms |
| Ladder | 0.155 ms |
| Chain | 0.114 ms |
| Fan behind chain | 0.144 ms |
| Small methods | 0.201 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 5 952 op/s |
| Irreducible | 5 618 op/s |
| Ladder | 6 452 op/s |
| Chain | 8 772 op/s |
| Fan behind chain | 6 944 op/s |
| Small methods | 4 975 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 3.606 ms |
| Irreducible | 3.848 ms |
| Ladder | 1.528 ms |
| Chain | 1.098 ms |
| Fan behind chain | 1.469 ms |
| Small methods | 2.946 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 277.3 op/s |
| Irreducible | 259.9 op/s |
| Ladder | 654.5 op/s |
| Chain | 910.7 op/s |
| Fan behind chain | 680.7 op/s |
| Small methods | 339.4 op/s |

### 1.13 Dominance frontiers

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 0.069 ms |
| Irreducible | 0.083 ms |
| Ladder | 45.91 ms |
| Chain | 0.037 ms |
| Fan behind chain | 105.8 ms |
| Small methods | 0.057 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 14 493 op/s |
| Irreducible | 12 048 op/s |
| Ladder | 21.78 op/s |
| Chain | 27 027 op/s |
| Fan behind chain | 9.452 op/s |
| Small methods | 17 544 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 1.991 ms |
| Irreducible | 2.347 ms |
| Chain | 0.368 ms |
| Small methods | 1.455 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 502.3 op/s |
| Irreducible | 426.1 op/s |
| Chain | 2 717 op/s |
| Small methods | 687.3 op/s |

### 1.14 Iterated dominance frontier (build + one query per variable)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 30.88 ms |
| Irreducible | 42.61 ms |
| Ladder | 36.01 ms |
| Chain | 21.29 ms |
| Fan behind chain | 33.67 ms |
| Small methods | 0.432 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 32.38 op/s |
| Irreducible | 23.47 op/s |
| Ladder | 27.77 op/s |
| Chain | 46.96 op/s |
| Fan behind chain | 29.70 op/s |
| Small methods | 2 315 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 438.1 ms |
| Irreducible | 572.2 ms |
| Ladder | 375.5 ms |
| Chain | 231.5 ms |
| Fan behind chain | 405.6 ms |
| Small methods | 4.535 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 2.283 op/s |
| Irreducible | 1.748 op/s |
| Ladder | 2.663 op/s |
| Chain | 4.320 op/s |
| Fan behind chain | 2.465 op/s |
| Small methods | 220.5 op/s |

### 1.15 Loops

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 0.400 ms |
| Irreducible | 0.420 ms |
| Ladder | 0.102 ms |
| Chain | 0.103 ms |
| Fan behind chain | 0.103 ms |
| Small methods | 0.465 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 2 500 op/s |
| Irreducible | 2 381 op/s |
| Ladder | 9 804 op/s |
| Chain | 9 709 op/s |
| Fan behind chain | 9 709 op/s |
| Small methods | 2 151 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 5.543 ms |
| Irreducible | 7.337 ms |
| Ladder | 1.035 ms |
| Chain | 0.992 ms |
| Fan behind chain | 1.005 ms |
| Small methods | 4.997 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 180.4 op/s |
| Irreducible | 136.3 op/s |
| Ladder | 966.2 op/s |
| Chain | 1 008 op/s |
| Fan behind chain | 995.0 op/s |
| Small methods | 200.1 op/s |

### 1.16 Ancestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 0.065 ms |
| Irreducible | 0.067 ms |
| Ladder | 0.052 ms |
| Chain | 0.056 ms |
| Fan behind chain | 0.052 ms |
| Small methods | 0.058 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 15 385 op/s |
| Irreducible | 14 925 op/s |
| Ladder | 19 231 op/s |
| Chain | 17 857 op/s |
| Fan behind chain | 19 231 op/s |
| Small methods | 17 241 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 1.897 ms |
| Irreducible | 1.727 ms |
| Ladder | 0.504 ms |
| Chain | 0.559 ms |
| Fan behind chain | 0.543 ms |
| Small methods | 1.591 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 527.1 op/s |
| Irreducible | 579.0 op/s |
| Ladder | 1 984 op/s |
| Chain | 1 789 op/s |
| Fan behind chain | 1 842 op/s |
| Small methods | 628.5 op/s |

### 1.17 PreorderAncestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 0.052 ms |
| Irreducible | 0.052 ms |
| Ladder | 0.045 ms |
| Chain | 0.046 ms |
| Fan behind chain | 0.044 ms |
| Small methods | 0.050 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 19 231 op/s |
| Irreducible | 19 231 op/s |
| Ladder | 22 222 op/s |
| Chain | 21 739 op/s |
| Fan behind chain | 22 727 op/s |
| Small methods | 20 000 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 1.072 ms |
| Irreducible | 1.051 ms |
| Ladder | 0.378 ms |
| Chain | 0.426 ms |
| Fan behind chain | 0.376 ms |
| Small methods | 0.868 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured | 932.8 op/s |
| Irreducible | 951.5 op/s |
| Ladder | 2 646 op/s |
| Chain | 2 347 op/s |
| Fan behind chain | 2 660 op/s |
| Small methods | 1 152 op/s |

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
| Path<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100%) | 458 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100%) | 458 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100%) | 458 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB (100%) | 960 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB (100%) | 9.9 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB (100%) | 9.9 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100%) | 10.0 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100%) | 10.0 MB |

![3.1.1 · Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-1-build-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100%) | 3.8 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100%) | 3.8 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100%) | 3.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB (100%) | 10.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 534 kB (100%) | 534 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 534 kB (100%) | 534 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 283 kB (98.1%) | 288 kB (100%) | 288 kB (100%) | 288 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 250 kB (67.5%) | 356 kB (96.0%) | 371 kB (100%) | 371 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (49.5%) | 507 kB (102%) | 498 kB (100%) | 498 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (49.4%) | 507 kB (102%) | 497 kB (100%) | 497 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (49.3%) | 505 kB (102%) | 495 kB (100%) | 495 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (67.0%) | 350 kB (95.9%) | 364 kB (100.0%) | 364 kB |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 6.0 MB (122%) | 4.9 MB (100%) | 4.9 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (57.8%) | 4.9 MB (100%) | 4.9 MB (100.0%) | 4.9 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (101%) | 2.8 MB (100%) | 2.8 MB (100%) | 2.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.5 MB (71.1%) | 3.9 MB (110%) | 3.5 MB (100.0%) | 3.5 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (54.3%) | 5.6 MB (124%) | 4.5 MB (100.0%) | 4.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (54.3%) | 5.6 MB (124%) | 4.5 MB (100.0%) | 4.5 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (54.2%) | 5.6 MB (124%) | 4.5 MB (100.0%) | 4.5 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (70.6%) | 3.3 MB (96.8%) | 3.5 MB (100.0%) | 3.5 MB |

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
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100%) | 200 kB (100.0%) | 200 kB |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
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
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100%) | 200 kB (100%) | 200 kB (100%) | 200 kB |

![3.4.1 · CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-1-ccs-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB (100%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |

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
| DAG GNP sparse<br><sub>100k nodes</sub> | — | — | 5.9 MB (100.0%) | 5.9 MB |

![3.5.3 · Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-3-condense-n-100-000.svg)

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 297 kB (100%) | 297 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 297 kB (100%) | 297 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| Tree shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP dense<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 3.6 MB (143%) | 2.5 MB (100.0%) | 2.5 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 2.5 MB (101%) | 2.5 MB (100.0%) | 2.5 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100%) | 433 kB |
| Tree shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB (100%) | 433 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB (100%) | 433 kB |
| DAG GNP dense<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100%) | 433 kB (100.0%) | 433 kB |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) | 160 kB (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) | 160 kB (100%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) | 160 kB (100%) |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) | 160 kB (100%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100%) | 160 kB (100%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100%) | 1.6 MB (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100%) | 1.6 MB (100.0%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB (100%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 257 kB (100%) | 257 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 257 kB (100%) | 257 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (25.2%) | 10.9 kB (100%) | 10.9 kB (100%) | 10.9 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 228 kB (65.0%) | 336 kB (95.8%) | 351 kB (100%) | 351 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100%) | 4.1 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100.0%) | 4.1 MB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100%) | 4.1 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (96.9%) | 4.0 MB (99.6%) | 4.0 MB (100%) | 4.0 MB |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 3.2 MB (151%) | 2.1 MB (100%) | 2.1 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 2.1 MB (101%) | 2.1 MB (100%) | 2.1 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 25.3 kB (75.8%) | 33.4 kB (100%) | 33.4 kB (100.0%) | 33.4 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.6 MB (71.3%) | 4.0 MB (110%) | 3.6 MB (100.0%) | 3.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB (100.0%) | 45.9 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB (100.0%) | 45.9 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB (100%) | 45.9 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (97.7%) | 44.7 MB (99.8%) | 44.8 MB (100.0%) | 44.8 MB |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 307 kB (100%) | 307 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 307 kB (100%) | 307 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (86.6%) | 60.8 kB (100%) | 60.8 kB (100%) | 60.8 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 72.7 kB (37.2%) | 181 kB (92.4%) | 196 kB (100%) | 196 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (57.4%) | 606 kB (102%) | 596 kB (100%) | 596 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (57.4%) | 606 kB (102%) | 596 kB (100%) | 596 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (58.2%) | 617 kB (102%) | 607 kB (100%) | 607 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (74.2%) | 462 kB (96.9%) | 476 kB (100%) | 476 kB |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 3.7 MB (141%) | 2.6 MB (100%) | 2.6 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 2.6 MB (101%) | 2.6 MB (100%) | 2.6 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 525 kB (98.5%) | 533 kB (100.0%) | 533 kB (100%) | 533 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 725 kB (41.1%) | 2.1 MB (120%) | 1.8 MB (100%) | 1.8 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (62.1%) | 6.6 MB (120%) | 5.5 MB (100.0%) | 5.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (62.1%) | 6.6 MB (120%) | 5.5 MB (100.0%) | 5.5 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (62.8%) | 6.7 MB (119%) | 5.6 MB (100%) | 5.6 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (77.2%) | 4.5 MB (97.5%) | 4.6 MB (100.0%) | 4.6 MB |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-2-edgetypes-n-100-000.svg)

### 3.10 Dominators

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 371 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 371 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 486 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 566 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 486 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 475 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 3.6 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 3.7 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 4.7 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 5.5 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 4.7 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 4.7 MB |

### 3.11 Dominance frontiers

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 134 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 144 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 50.1 MB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 80.1 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 100 MB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 140 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 1.3 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 1.4 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 800 kB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 1.4 MB |

### 3.12 Iterated dominance frontier (build + one query per variable)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 25.4 MB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 28.9 MB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 53.8 MB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 21.4 MB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 65.4 MB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 604 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 370 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 451 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 504 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 198 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 662 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 5.8 MB |

### 3.13 Loops

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 772 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 812 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 675 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 735 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 675 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 897 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 7.5 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 13.5 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 6.6 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 7.2 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 6.6 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 8.9 MB |

### 3.14 Ancestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 280 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 280 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 280 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 280 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 280 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 297 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 2.8 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 2.8 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 2.8 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 2.8 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 2.8 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 2.9 MB |

### 3.15 PreorderAncestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 200 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 200 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 200 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 200 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 200 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 216 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 |
|---|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 2.0 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 2.0 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 2.0 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 2.0 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 2.0 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 2.1 MB |
