# Graphity vs. JGraphT, Guava and WebGraph — synthetic / ubuntu-ryzen-9950x

Graphity `2.2.0` against the peer libraries. Peer numbers come from the newest run that measured them, which need not be the run of the newest Graphity version; every column states the version it belongs to. Blocks without a peer column are Graphity-only benchmarks or graph sizes beyond the peers' reach — for the comparison across Graphity versions see [`synthetic-ubuntu-ryzen-9950x-history.md`](synthetic-ubuntu-ryzen-9950x-history.md).

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
  - [1.12 Symmetrize (WebGraph) cost](#112-symmetrize-webgraph-cost)
  - [1.13 Dominators](#113-dominators)
  - [1.14 Dominance frontiers](#114-dominance-frontiers)
  - [1.15 Iterated dominance frontier (build + one query per variable)](#115-iterated-dominance-frontier-build-one-query-per-variable)
  - [1.16 Loops](#116-loops)
  - [1.17 Ancestry of the dominator tree](#117-ancestry-of-the-dominator-tree)
  - [1.18 PreorderAncestry of the dominator tree](#118-preorderancestry-of-the-dominator-tree)
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

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.083 ms | 1.041 ms (13×) | 0.363 ms (4.4×) | 2.151 ms (26×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.084 ms | 1.200 ms (14×) | 0.593 ms (7.1×) | 2.857 ms (34×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.093 ms | 1.221 ms (13×) | 0.524 ms (5.6×) | 2.602 ms (28×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.163 ms | 4.210 ms (26×) | 1.931 ms (12×) | 7.975 ms (49×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.650 ms | 61.46 ms (37×) | 38.25 ms (23×) | 45.80 ms (28×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.614 ms | 61.19 ms (38×) | 39.39 ms (24×) | 44.34 ms (27×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.622 ms | 45.39 ms (28×) | 8.373 ms (5.2×) | 6.531 ms (4.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.650 ms | 61.18 ms (37×) | 20.60 ms (12×) | 44.64 ms (27×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s | 960.6 op/s (8.0%) | 2 755 op/s (22.9%) | 464.9 op/s (3.9%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 11 905 op/s | 833.3 op/s (7.0%) | 1 686 op/s (14.2%) | 350.0 op/s (2.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 753 op/s | 819.0 op/s (7.6%) | 1 908 op/s (17.7%) | 384.3 op/s (3.6%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 135 op/s | 237.5 op/s (3.9%) | 517.9 op/s (8.4%) | 125.4 op/s (2.0%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 606.1 op/s | 16.27 op/s (2.7%) | 26.14 op/s (4.3%) | 21.84 op/s (3.6%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 619.6 op/s | 16.34 op/s (2.6%) | 25.39 op/s (4.1%) | 22.55 op/s (3.6%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 616.5 op/s | 22.03 op/s (3.6%) | 119.4 op/s (19.4%) | 153.1 op/s (24.8%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 606.1 op/s | 16.34 op/s (2.7%) | 48.54 op/s (8.0%) | 22.40 op/s (3.7%) |

![1.1.1 Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-1-1-build-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.696 ms | 15.49 ms (22×) | 5.517 ms (7.9×) | 20.03 ms (29×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.698 ms | 26.62 ms (38×) | 12.53 ms (18×) | 28.11 ms (40×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.111 ms | 27.47 ms (25×) | 10.91 ms (9.8×) | 29.45 ms (27×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.274 ms | 101.4 ms (45×) | 59.74 ms (26×) | 86.14 ms (38×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.48 ms | 1631.8 ms (93×) | 1157.5 ms (66×) | 563.3 ms (32×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 17.50 ms | 1605.5 ms (92×) | 1174.8 ms (67×) | 521.3 ms (30×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 17.45 ms | 896.3 ms (51×) | 103.5 ms (5.9×) | 64.30 ms (3.7×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 16.96 ms | 1585.8 ms (94×) | 774.0 ms (46×) | 534.8 ms (32×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 437 op/s | 64.54 op/s (4.5%) | 181.3 op/s (12.6%) | 49.92 op/s (3.5%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 433 op/s | 37.57 op/s (2.6%) | 79.81 op/s (5.6%) | 35.57 op/s (2.5%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 900.1 op/s | 36.40 op/s (4.0%) | 91.64 op/s (10.2%) | 33.96 op/s (3.8%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 439.8 op/s | 9.862 op/s (2.2%) | 16.74 op/s (3.8%) | 11.61 op/s (2.6%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.19 op/s | 0.613 op/s (1.1%) | 0.864 op/s (1.5%) | 1.775 op/s (3.1%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 57.16 op/s | 0.623 op/s (1.1%) | 0.851 op/s (1.5%) | 1.918 op/s (3.4%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 57.32 op/s | 1.116 op/s (1.9%) | 9.662 op/s (16.9%) | 15.55 op/s (27.1%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 58.97 op/s | 0.631 op/s (1.1%) | 1.292 op/s (2.2%) | 1.870 op/s (3.2%) |

![1.1.2 Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-1-2-build-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 24.02 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 181.1 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 169.9 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 167.1 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 41.63 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 5.522 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 5.886 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 5.984 op/s |

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 243.3 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 2326.6 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 2301.9 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1846.2 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4.110 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.430 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.434 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 0.542 op/s |

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 2986.8 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.335 op/s |

### 1.2 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.106 ms | 0.106 ms (1.0×) | 0.749 ms (7.1×) | 0.813 ms (7.7×) | 0.348 ms (3.3×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.135 ms | 0.106 ms (0.8×) | 1.235 ms (9.1×) | 1.348 ms (10.0×) | 0.493 ms (3.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.105 ms | 0.086 ms (0.8×) | 1.108 ms (11×) | 1.101 ms (10×) | 0.470 ms (4.5×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.245 ms | 0.112 ms (0.5×) | 2.219 ms (9.1×) | 2.512 ms (10×) | 0.861 ms (3.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.550 ms | 0.386 ms (0.7×) | 29.16 ms (53×) | 22.23 ms (40×) | 2.359 ms (4.3×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.576 ms | 0.379 ms (0.7×) | 29.73 ms (52×) | 27.34 ms (47×) | 2.377 ms (4.1×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.261 ms | 0.266 ms (1.0×) | 22.65 ms (87×) | 6.822 ms (26×) | 3.359 ms (13×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.349 ms | 0.264 ms (0.8×) | 24.54 ms (70×) | 9.666 ms (28×) | 2.065 ms (5.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 434 op/s | 9 434 op/s (100%) | 1 335 op/s (14.2%) | 1 230 op/s (13.0%) | 2 874 op/s (30.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 407 op/s | 9 434 op/s (127%) | 809.7 op/s (10.9%) | 741.8 op/s (10.0%) | 2 028 op/s (27.4%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 9 524 op/s | 11 628 op/s (122%) | 902.5 op/s (9.5%) | 908.3 op/s (9.5%) | 2 128 op/s (22.3%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 082 op/s | 8 929 op/s (219%) | 450.7 op/s (11.0%) | 398.1 op/s (9.8%) | 1 161 op/s (28.5%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 818 op/s | 2 591 op/s (142%) | 34.30 op/s (1.9%) | 44.99 op/s (2.5%) | 423.9 op/s (23.3%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 736 op/s | 2 639 op/s (152%) | 33.63 op/s (1.9%) | 36.58 op/s (2.1%) | 420.7 op/s (24.2%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3 831 op/s | 3 759 op/s (98.1%) | 44.16 op/s (1.2%) | 146.6 op/s (3.8%) | 297.7 op/s (7.8%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 865 op/s | 3 788 op/s (132%) | 40.75 op/s (1.4%) | 103.5 op/s (3.6%) | 484.3 op/s (16.9%) |

![1.2.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.021 ms | 1.030 ms (1.0×) | 16.76 ms (16×) | 24.68 ms (24×) | 3.364 ms (3.3×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.236 ms | 1.052 ms (0.5×) | 60.90 ms (27×) | 52.78 ms (24×) | 5.556 ms (2.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.153 ms | 1.303 ms (0.6×) | 49.38 ms (23×) | 41.68 ms (19×) | 5.713 ms (2.7×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.617 ms | 2.220 ms (0.5×) | 119.9 ms (26×) | 115.9 ms (25×) | 10.53 ms (2.3×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 8.010 ms | 4.693 ms (0.6×) | 503.0 ms (63×) | 584.2 ms (73×) | 37.19 ms (4.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 8.094 ms | 4.517 ms (0.6×) | 546.7 ms (68×) | 481.1 ms (59×) | 35.15 ms (4.3×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.509 ms | 2.645 ms (1.1×) | 177.4 ms (71×) | 116.1 ms (46×) | 37.72 ms (15×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4.904 ms | 2.789 ms (0.6×) | 382.6 ms (78×) | 152.9 ms (31×) | 30.57 ms (6.2×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 979.4 op/s | 970.9 op/s (99.1%) | 59.66 op/s (6.1%) | 40.52 op/s (4.1%) | 297.3 op/s (30.4%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 447.2 op/s | 950.6 op/s (213%) | 16.42 op/s (3.7%) | 18.95 op/s (4.2%) | 180.0 op/s (40.2%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 464.5 op/s | 767.5 op/s (165%) | 20.25 op/s (4.4%) | 23.99 op/s (5.2%) | 175.0 op/s (37.7%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 216.6 op/s | 450.5 op/s (208%) | 8.340 op/s (3.9%) | 8.628 op/s (4.0%) | 94.98 op/s (43.9%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 124.8 op/s | 213.1 op/s (171%) | 1.988 op/s (1.6%) | 1.712 op/s (1.4%) | 26.89 op/s (21.5%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 123.5 op/s | 221.4 op/s (179%) | 1.829 op/s (1.5%) | 2.079 op/s (1.7%) | 28.45 op/s (23.0%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 398.6 op/s | 378.1 op/s (94.9%) | 5.637 op/s (1.4%) | 8.613 op/s (2.2%) | 26.51 op/s (6.7%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 203.9 op/s | 358.6 op/s (176%) | 2.614 op/s (1.3%) | 6.540 op/s (3.2%) | 32.71 op/s (16.0%) |

![1.2.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 86.77 ms | 25.87 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 246.1 ms | 61.93 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 248.1 ms | 62.57 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 26.51 ms | 26.54 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 11.52 op/s | 38.65 op/s (335%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.063 op/s | 16.15 op/s (397%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.031 op/s | 15.98 op/s (397%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 37.72 op/s | 37.67 op/s (99.9%) |

![1.2.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3120.3 ms | 757.2 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6783.8 ms | 3038.0 ms (0.4×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 6754.9 ms | 3578.0 ms (0.5×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 256.3 ms | 274.0 ms (1.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.320 op/s | 1.321 op/s (412%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.147 op/s | 0.329 op/s (223%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.148 op/s | 0.279 op/s (189%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.902 op/s | 3.650 op/s (93.5%) |

![1.2.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 36545 ms | 9918.4 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.027 op/s | 0.101 op/s (368%) |

![1.2.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-5-scc-n-100-000-000.svg)

### 1.3 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.036 ms | 0.042 ms (1.2×) | 0.814 ms (23×) | 0.781 ms (22×) | 0.954 ms (26×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.046 ms | 0.042 ms (0.9×) | 1.086 ms (24×) | 1.331 ms (29×) | 1.124 ms (24×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.038 ms | 0.039 ms (1.0×) | 1.252 ms (33×) | 1.037 ms (27×) | 0.805 ms (21×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.102 ms | 0.081 ms (0.8×) | 2.049 ms (20×) | 2.546 ms (25×) | 1.163 ms (11×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.531 ms | 0.537 ms (1.0×) | 32.13 ms (61×) | 23.57 ms (44×) | 4.674 ms (8.8×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.528 ms | 0.525 ms (1.0×) | 34.89 ms (66×) | 27.16 ms (51×) | 4.730 ms (9.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.581 ms | 0.589 ms (1.0×) | 12.95 ms (22×) | 9.549 ms (16×) | 4.927 ms (8.5×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.648 ms | 0.620 ms (1.0×) | 16.02 ms (25×) | 14.44 ms (22×) | 2.606 ms (4.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 778 op/s | 23 810 op/s (85.7%) | 1 229 op/s (4.4%) | 1 280 op/s (4.6%) | 1 048 op/s (3.8%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 739 op/s | 23 810 op/s (110%) | 920.8 op/s (4.2%) | 751.3 op/s (3.5%) | 889.7 op/s (4.1%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s | 25 641 op/s (97.4%) | 798.7 op/s (3.0%) | 964.3 op/s (3.7%) | 1 242 op/s (4.7%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 9 804 op/s | 12 346 op/s (126%) | 488.0 op/s (5.0%) | 392.8 op/s (4.0%) | 859.8 op/s (8.8%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 883 op/s | 1 862 op/s (98.9%) | 31.12 op/s (1.7%) | 42.42 op/s (2.3%) | 213.9 op/s (11.4%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 894 op/s | 1 905 op/s (101%) | 28.66 op/s (1.5%) | 36.82 op/s (1.9%) | 211.4 op/s (11.2%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 721 op/s | 1 698 op/s (98.6%) | 77.25 op/s (4.5%) | 104.7 op/s (6.1%) | 203.0 op/s (11.8%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 543 op/s | 1 613 op/s (105%) | 62.41 op/s (4.0%) | 69.24 op/s (4.5%) | 383.7 op/s (24.9%) |

![1.3.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.395 ms | 0.357 ms (0.9×) | 18.60 ms (47×) | 11.88 ms (30×) | 9.112 ms (23×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.291 ms | 0.356 ms (0.3×) | 48.53 ms (38×) | 24.14 ms (19×) | 11.75 ms (9.1×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.665 ms | 0.732 ms (1.1×) | 59.60 ms (90×) | 23.02 ms (35×) | 9.231 ms (14×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.800 ms | 1.572 ms (0.9×) | 119.6 ms (66×) | 69.85 ms (39×) | 15.98 ms (8.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.096 ms | 6.263 ms (1.0×) | 644.8 ms (106×) | 365.1 ms (60×) | 55.03 ms (9.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.158 ms | 6.194 ms (1.0×) | 631.2 ms (103×) | 426.7 ms (69×) | 54.32 ms (8.8×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 6.479 ms | 6.018 ms (0.9×) | 155.6 ms (24×) | 110.4 ms (17×) | 50.55 ms (7.8×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.054 ms | 5.936 ms (1.0×) | 292.5 ms (48×) | 283.5 ms (47×) | 35.87 ms (5.9×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 532 op/s | 2 801 op/s (111%) | 53.77 op/s (2.1%) | 84.18 op/s (3.3%) | 109.7 op/s (4.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 774.6 op/s | 2 809 op/s (363%) | 20.61 op/s (2.7%) | 41.42 op/s (5.3%) | 85.11 op/s (11.0%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 504 op/s | 1 366 op/s (90.8%) | 16.78 op/s (1.1%) | 43.44 op/s (2.9%) | 108.3 op/s (7.2%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 555.6 op/s | 636.1 op/s (115%) | 8.361 op/s (1.5%) | 14.32 op/s (2.6%) | 62.56 op/s (11.3%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 164.0 op/s | 159.7 op/s (97.3%) | 1.551 op/s (0.9%) | 2.739 op/s (1.7%) | 18.17 op/s (11.1%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.4 op/s | 161.4 op/s (99.4%) | 1.584 op/s (1.0%) | 2.344 op/s (1.4%) | 18.41 op/s (11.3%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 154.3 op/s | 166.2 op/s (108%) | 6.427 op/s (4.2%) | 9.058 op/s (5.9%) | 19.78 op/s (12.8%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 165.2 op/s | 168.5 op/s (102%) | 3.419 op/s (2.1%) | 3.527 op/s (2.1%) | 27.88 op/s (16.9%) |

![1.3.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 19.97 ms | 16.39 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 67.53 ms | 69.74 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 68.44 ms | 68.78 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.92 ms | 50.64 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 50.08 op/s | 61.02 op/s (122%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 14.81 op/s | 14.34 op/s (96.8%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.61 op/s | 14.54 op/s (99.5%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.64 op/s | 19.75 op/s (101%) |

![1.3.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 569.5 ms | 447.4 ms (0.8×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4580.5 ms | 4418.3 ms (1.0×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3734.0 ms | 4399.7 ms (1.2×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 594.1 ms | 561.6 ms (0.9×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.756 op/s | 2.235 op/s (127%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.218 op/s | 0.226 op/s (104%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.268 op/s | 0.227 op/s (84.9%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.683 op/s | 1.781 op/s (106%) |

![1.3.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 9600.9 ms | 6010.4 ms (0.6×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.104 op/s | 0.166 op/s (160%) |

![1.3.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-5-wcc-n-100-000-000.svg)

### 1.4 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.043 ms | 0.037 ms (0.9×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.058 ms | 0.036 ms (0.6×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.039 ms | 0.044 ms (1.1×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.110 ms | 0.087 ms (0.8×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.530 ms | 0.530 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.535 ms | 0.535 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.586 ms | 0.556 ms (0.9×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.648 ms | 0.625 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 23 256 op/s | 27 027 op/s (116%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s | 27 778 op/s (161%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s | 22 727 op/s (88.6%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 9 091 op/s | 11 494 op/s (126%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 887 op/s | 1 887 op/s (100%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 869 op/s | 1 869 op/s (100%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 706 op/s | 1 799 op/s (105%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 543 op/s | 1 600 op/s (104%) |

![1.4.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.398 ms | 0.399 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.276 ms | 0.403 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.644 ms | 0.720 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.836 ms | 1.515 ms (0.8×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.177 ms | 6.129 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.303 ms | 6.203 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.949 ms | 5.961 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.078 ms | 6.133 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 513 op/s | 2 506 op/s (99.7%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 783.7 op/s | 2 481 op/s (317%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 553 op/s | 1 389 op/s (89.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 544.7 op/s | 660.1 op/s (121%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 161.9 op/s | 163.2 op/s (101%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 158.7 op/s | 161.2 op/s (102%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 168.1 op/s | 167.8 op/s (99.8%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 164.5 op/s | 163.1 op/s (99.1%) |

![1.4.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 20.46 ms | 17.11 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 67.57 ms | 67.67 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 71.04 ms | 69.03 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.58 ms | 50.61 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 48.87 op/s | 58.46 op/s (120%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 14.80 op/s | 14.78 op/s (99.9%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.08 op/s | 14.49 op/s (103%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.77 op/s | 19.76 op/s (99.9%) |

![1.4.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 615.4 ms | 445.8 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4620.4 ms | 4435.6 ms (1.0×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4689.5 ms | 4352.3 ms (0.9×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 598.6 ms | 557.8 ms (0.9×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.625 op/s | 2.243 op/s (138%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.216 op/s | 0.225 op/s (104%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.213 op/s | 0.230 op/s (108%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.671 op/s | 1.793 op/s (107%) |

![1.4.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8594.8 ms | 6004.0 ms (0.7×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.116 op/s | 0.167 op/s (143%) |

![1.4.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-5-ccs-n-100-000-000.svg)

### 1.5 Condense

#### Times @ 1k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 0.011 ms |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 0.008 ms |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 0.024 ms |
| DAG GNP sparse | 0.017 ms |

#### Throughput @ 1k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 90 909 op/s |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 125 000 op/s |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 41 667 op/s |
| DAG GNP sparse | 58 824 op/s |

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.080 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.092 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.185 ms |
| DAG GNP sparse | 0.261 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 500 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 10 870 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 5 405 op/s |
| DAG GNP sparse | 3 831 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.768 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.591 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1.852 ms |
| DAG GNP sparse | 3.641 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 302 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 628.5 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 540.0 op/s |
| DAG GNP sparse | 274.6 op/s |

### 1.6 TopOrder

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.067 ms | 0.067 ms (1.0×) | 0.430 ms (6.4×) | 0.412 ms (6.1×) | 0.561 ms (8.4×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.070 ms | 0.052 ms (0.7×) | 0.674 ms (9.6×) | 0.907 ms (13×) | 0.440 ms (6.3×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.049 ms | 0.037 ms (0.8×) | 0.705 ms (14×) | 0.779 ms (16×) | 0.605 ms (12×) |
| Tree shfld. | 0.050 ms | 0.047 ms (0.9×) | 0.734 ms (15×) | 0.698 ms (14×) | 0.524 ms (10×) |
| DAG GNP sparse | 0.074 ms | 0.064 ms (0.9×) | 1.112 ms (15×) | 1.554 ms (21×) | 0.854 ms (12×) |
| DAG GNP dense | 0.324 ms | 0.245 ms (0.8×) | 17.84 ms (55×) | 10.95 ms (34×) | 2.149 ms (6.6×) |
| DAG GNP dense shfld. | 0.366 ms | 0.246 ms (0.7×) | 19.50 ms (53×) | 15.74 ms (43×) | 2.356 ms (6.4×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 14 925 op/s | 14 925 op/s (100%) | 2 326 op/s (15.6%) | 2 427 op/s (16.3%) | 1 783 op/s (11.9%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 14 286 op/s | 19 231 op/s (135%) | 1 484 op/s (10.4%) | 1 103 op/s (7.7%) | 2 273 op/s (15.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 20 408 op/s | 27 027 op/s (132%) | 1 418 op/s (7.0%) | 1 284 op/s (6.3%) | 1 653 op/s (8.1%) |
| Tree shfld. | 20 000 op/s | 21 277 op/s (106%) | 1 362 op/s (6.8%) | 1 433 op/s (7.2%) | 1 908 op/s (9.5%) |
| DAG GNP sparse | 13 514 op/s | 15 625 op/s (116%) | 899.3 op/s (6.7%) | 643.5 op/s (4.8%) | 1 171 op/s (8.7%) |
| DAG GNP dense | 3 086 op/s | 4 082 op/s (132%) | 56.06 op/s (1.8%) | 91.29 op/s (3.0%) | 465.3 op/s (15.1%) |
| DAG GNP dense shfld. | 2 732 op/s | 4 065 op/s (149%) | 51.27 op/s (1.9%) | 63.54 op/s (2.3%) | 424.4 op/s (15.5%) |

![1.6.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.632 ms | 0.628 ms (1.0×) | 7.642 ms (12×) | 15.93 ms (25×) | 3.453 ms (5.5×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.068 ms | 0.528 ms (0.5×) | 34.83 ms (33×) | 30.21 ms (28×) | 5.051 ms (4.7×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.426 ms | 1.182 ms (0.8×) | 37.97 ms (27×) | 22.72 ms (16×) | 14.76 ms (10×) |
| Tree shfld. | 1.479 ms | 1.255 ms (0.8×) | 33.53 ms (23×) | 25.75 ms (17×) | 10.15 ms (6.9×) |
| DAG GNP sparse | 2.242 ms | 1.499 ms (0.7×) | 72.19 ms (32×) | 60.72 ms (27×) | 16.83 ms (7.5×) |
| DAG GNP dense | 4.515 ms | 3.127 ms (0.7×) | 276.4 ms (61×) | 317.3 ms (70×) | 25.01 ms (5.5×) |
| DAG GNP dense shfld. | 5.063 ms | 3.173 ms (0.6×) | 294.4 ms (58×) | 318.4 ms (63×) | 31.84 ms (6.3×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 582 op/s | 1 592 op/s (101%) | 130.9 op/s (8.3%) | 62.79 op/s (4.0%) | 289.6 op/s (18.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 936.3 op/s | 1 894 op/s (202%) | 28.71 op/s (3.1%) | 33.10 op/s (3.5%) | 198.0 op/s (21.1%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 701.3 op/s | 846.0 op/s (121%) | 26.34 op/s (3.8%) | 44.01 op/s (6.3%) | 67.75 op/s (9.7%) |
| Tree shfld. | 676.1 op/s | 796.8 op/s (118%) | 29.82 op/s (4.4%) | 38.84 op/s (5.7%) | 98.54 op/s (14.6%) |
| DAG GNP sparse | 446.0 op/s | 667.1 op/s (150%) | 13.85 op/s (3.1%) | 16.47 op/s (3.7%) | 59.40 op/s (13.3%) |
| DAG GNP dense | 221.5 op/s | 319.8 op/s (144%) | 3.618 op/s (1.6%) | 3.152 op/s (1.4%) | 39.98 op/s (18.1%) |
| DAG GNP dense shfld. | 197.5 op/s | 315.2 op/s (160%) | 3.397 op/s (1.7%) | 3.141 op/s (1.6%) | 31.41 op/s (15.9%) |

![1.6.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 31.70 ms | 15.34 ms (0.5×) |
| DAG GNP dense | 106.4 ms | 38.21 ms (0.4×) |
| DAG GNP dense shfld. | 177.6 ms | 39.11 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 31.55 op/s | 65.18 op/s (207%) |
| DAG GNP dense | 9.398 op/s | 26.17 op/s (278%) |
| DAG GNP dense shfld. | 5.631 op/s | 25.57 op/s (454%) |

![1.6.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 1241.1 ms | 163.7 ms (0.1×) |
| DAG GNP dense | 2005.5 ms | 466.3 ms (0.2×) |
| DAG GNP dense shfld. | 2927.1 ms | 418.1 ms (0.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.806 op/s | 6.109 op/s (758%) |
| DAG GNP dense | 0.499 op/s | 2.145 op/s (430%) |
| DAG GNP dense shfld. | 0.342 op/s | 2.392 op/s (700%) |

![1.6.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 18079 ms | 1855.1 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.055 op/s | 0.539 op/s (975%) |

![1.6.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-5-toporder-n-100-000-000.svg)

### 1.7 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.039 ms | 0.039 ms (1.0×) | 0.041 ms (1.1×) | 0.221 ms (5.7×) | 0.363 ms (9.3×) | 0.409 ms (10×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.057 ms | 0.040 ms (0.7×) | 0.055 ms (1.0×) | 0.363 ms (6.4×) | 0.504 ms (8.8×) | 0.638 ms (11×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.035 ms | 0.031 ms (0.9×) | 0.029 ms (0.8×) | 0.210 ms (6.0×) | 0.546 ms (16×) | 0.653 ms (19×) |
| Tree shfld. | 0.032 ms | 0.031 ms (1.0×) | 0.030 ms (0.9×) | 0.218 ms (6.8×) | 0.552 ms (17×) | 0.691 ms (22×) |
| DAG GNP sparse | 0.061 ms | 0.054 ms (0.9×) | 0.045 ms (0.7×) | 0.384 ms (6.3×) | 1.151 ms (19×) | 1.071 ms (18×) |
| DAG GNP dense | 0.364 ms | 0.334 ms (0.9×) | 0.249 ms (0.7×) | 4.566 ms (13×) | 8.607 ms (24×) | 3.764 ms (10×) |
| DAG GNP dense shfld. | 0.404 ms | 0.328 ms (0.8×) | 0.295 ms (0.7×) | 5.709 ms (14×) | 13.28 ms (33×) | 4.180 ms (10×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s | 25 641 op/s (100%) | 24 390 op/s (95.1%) | 4 525 op/s (17.6%) | 2 755 op/s (10.7%) | 2 445 op/s (9.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 544 op/s | 25 000 op/s (142%) | 18 182 op/s (104%) | 2 755 op/s (15.7%) | 1 984 op/s (11.3%) | 1 567 op/s (8.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 28 571 op/s | 32 258 op/s (113%) | 34 483 op/s (121%) | 4 762 op/s (16.7%) | 1 832 op/s (6.4%) | 1 531 op/s (5.4%) |
| Tree shfld. | 31 250 op/s | 32 258 op/s (103%) | 33 333 op/s (107%) | 4 587 op/s (14.7%) | 1 812 op/s (5.8%) | 1 447 op/s (4.6%) |
| DAG GNP sparse | 16 393 op/s | 18 519 op/s (113%) | 22 222 op/s (136%) | 2 604 op/s (15.9%) | 868.8 op/s (5.3%) | 933.7 op/s (5.7%) |
| DAG GNP dense | 2 747 op/s | 2 994 op/s (109%) | 4 016 op/s (146%) | 219.0 op/s (8.0%) | 116.2 op/s (4.2%) | 265.7 op/s (9.7%) |
| DAG GNP dense shfld. | 2 475 op/s | 3 049 op/s (123%) | 3 390 op/s (137%) | 175.2 op/s (7.1%) | 75.28 op/s (3.0%) | 239.2 op/s (9.7%) |

![1.7.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.462 ms | 0.571 ms (1.2×) | 0.360 ms (0.8×) | 4.695 ms (10×) | 5.601 ms (12×) | 3.763 ms (8.1×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.424 ms | 0.565 ms (0.4×) | 1.335 ms (0.9×) | 25.18 ms (18×) | 18.03 ms (13×) | 7.208 ms (5.1×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.947 ms | 0.845 ms (0.9×) | 0.852 ms (0.9×) | 11.93 ms (13×) | 12.06 ms (13×) | 7.956 ms (8.4×) |
| Tree shfld. | 0.954 ms | 0.893 ms (0.9×) | 0.896 ms (0.9×) | 12.11 ms (13×) | 11.26 ms (12×) | 8.159 ms (8.6×) |
| DAG GNP sparse | 2.261 ms | 1.985 ms (0.9×) | 1.981 ms (0.9×) | 23.32 ms (10×) | 38.09 ms (17×) | 14.35 ms (6.3×) |
| DAG GNP dense | 5.364 ms | 4.696 ms (0.9×) | 4.360 ms (0.8×) | 64.37 ms (12×) | 232.5 ms (43×) | 47.48 ms (8.9×) |
| DAG GNP dense shfld. | 5.866 ms | 4.690 ms (0.8×) | 4.509 ms (0.8×) | 78.56 ms (13×) | 217.3 ms (37×) | 58.47 ms (10.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 165 op/s | 1 751 op/s (80.9%) | 2 778 op/s (128%) | 213.0 op/s (9.8%) | 178.5 op/s (8.2%) | 265.7 op/s (12.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 702.2 op/s | 1 770 op/s (252%) | 749.1 op/s (107%) | 39.72 op/s (5.7%) | 55.47 op/s (7.9%) | 138.7 op/s (19.8%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 056 op/s | 1 183 op/s (112%) | 1 174 op/s (111%) | 83.80 op/s (7.9%) | 82.90 op/s (7.9%) | 125.7 op/s (11.9%) |
| Tree shfld. | 1 048 op/s | 1 120 op/s (107%) | 1 116 op/s (106%) | 82.58 op/s (7.9%) | 88.82 op/s (8.5%) | 122.6 op/s (11.7%) |
| DAG GNP sparse | 442.3 op/s | 503.8 op/s (114%) | 504.8 op/s (114%) | 42.88 op/s (9.7%) | 26.25 op/s (5.9%) | 69.71 op/s (15.8%) |
| DAG GNP dense | 186.4 op/s | 212.9 op/s (114%) | 229.4 op/s (123%) | 15.53 op/s (8.3%) | 4.301 op/s (2.3%) | 21.06 op/s (11.3%) |
| DAG GNP dense shfld. | 170.5 op/s | 213.2 op/s (125%) | 221.8 op/s (130%) | 12.73 op/s (7.5%) | 4.602 op/s (2.7%) | 17.10 op/s (10.0%) |

![1.7.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 32.07 ms | 27.96 ms (0.9×) |
| DAG GNP dense | 112.9 ms | 95.81 ms (0.8×) |
| DAG GNP dense shfld. | 206.4 ms | 96.05 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 31.18 op/s | 35.76 op/s (115%) |
| DAG GNP dense | 8.857 op/s | 10.44 op/s (118%) |
| DAG GNP dense shfld. | 4.845 op/s | 10.41 op/s (215%) |

![1.7.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 877.4 ms | 772.4 ms (0.9×) |
| DAG GNP dense | 3289.1 ms | 3050.5 ms (0.9×) |
| DAG GNP dense shfld. | 4671.3 ms | 2667.8 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 1.140 op/s | 1.295 op/s (114%) |
| DAG GNP dense | 0.304 op/s | 0.328 op/s (108%) |
| DAG GNP dense shfld. | 0.214 op/s | 0.375 op/s (175%) |

![1.7.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 11575 ms | 9530.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.086 op/s | 0.105 op/s (121%) |

![1.7.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-5-toporderandlevels-n-100-000-000.svg)

### 1.8 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.052 ms | 0.061 ms (1.2×) | 0.168 ms (3.2×) | 0.217 ms (4.2×) | 0.558 ms (11×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.065 ms | 0.051 ms (0.8×) | 0.367 ms (5.6×) | 0.506 ms (7.8×) | 0.442 ms (6.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.049 ms | 0.047 ms (1.0×) | 0.496 ms (10×) | 0.624 ms (13×) | 0.587 ms (12×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.143 ms | 0.093 ms (0.7×) | 0.614 ms (4.3×) | 1.369 ms (9.6×) | 0.784 ms (5.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.775 ms | 0.658 ms (0.8×) | 6.489 ms (8.4×) | 12.77 ms (16×) | 2.568 ms (3.3×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.776 ms | 0.662 ms (0.9×) | 5.123 ms (6.6×) | 14.78 ms (19×) | 2.511 ms (3.2×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.582 ms | 0.596 ms (1.0×) | 3.848 ms (6.6×) | 3.656 ms (6.3×) | 3.915 ms (6.7×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.657 ms | 0.588 ms (0.9×) | 4.819 ms (7.3×) | 5.799 ms (8.8×) | 2.409 ms (3.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 19 231 op/s | 16 393 op/s (85.2%) | 5 952 op/s (31.0%) | 4 608 op/s (24.0%) | 1 792 op/s (9.3%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 385 op/s | 19 608 op/s (127%) | 2 725 op/s (17.7%) | 1 976 op/s (12.8%) | 2 262 op/s (14.7%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 20 408 op/s | 21 277 op/s (104%) | 2 016 op/s (9.9%) | 1 603 op/s (7.9%) | 1 704 op/s (8.3%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 993 op/s | 10 753 op/s (154%) | 1 629 op/s (23.3%) | 730.5 op/s (10.4%) | 1 276 op/s (18.2%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 290 op/s | 1 520 op/s (118%) | 154.1 op/s (11.9%) | 78.30 op/s (6.1%) | 389.4 op/s (30.2%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 289 op/s | 1 511 op/s (117%) | 195.2 op/s (15.1%) | 67.64 op/s (5.2%) | 398.2 op/s (30.9%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 718 op/s | 1 678 op/s (97.7%) | 259.9 op/s (15.1%) | 273.5 op/s (15.9%) | 255.4 op/s (14.9%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 522 op/s | 1 701 op/s (112%) | 207.5 op/s (13.6%) | 172.4 op/s (11.3%) | 415.1 op/s (27.3%) |

![1.8.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.568 ms | 0.569 ms (1.0×) | 5.870 ms (10×) | 9.034 ms (16×) | 3.710 ms (6.5×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.994 ms | 0.506 ms (0.5×) | 25.61 ms (26×) | 20.62 ms (21×) | 5.590 ms (5.6×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.295 ms | 1.035 ms (0.8×) | 32.46 ms (25×) | 23.01 ms (18×) | 14.83 ms (11×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.124 ms | 1.895 ms (0.6×) | 46.38 ms (15×) | 44.02 ms (14×) | 11.49 ms (3.7×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 13.51 ms | 7.279 ms (0.5×) | 131.2 ms (9.7×) | 266.9 ms (20×) | 51.57 ms (3.8×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 13.88 ms | 7.242 ms (0.5×) | 128.4 ms (9.3×) | 215.3 ms (16×) | 49.13 ms (3.5×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 6.179 ms | 6.220 ms (1.0×) | 84.33 ms (14×) | 78.64 ms (13×) | 48.91 ms (7.9×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 12.63 ms | 6.482 ms (0.5×) | 132.4 ms (10×) | 90.29 ms (7.1×) | 40.70 ms (3.2×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 761 op/s | 1 757 op/s (99.8%) | 170.4 op/s (9.7%) | 110.7 op/s (6.3%) | 269.5 op/s (15.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 006 op/s | 1 976 op/s (196%) | 39.04 op/s (3.9%) | 48.50 op/s (4.8%) | 178.9 op/s (17.8%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 772.2 op/s | 966.2 op/s (125%) | 30.81 op/s (4.0%) | 43.46 op/s (5.6%) | 67.42 op/s (8.7%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 320.1 op/s | 527.7 op/s (165%) | 21.56 op/s (6.7%) | 22.72 op/s (7.1%) | 87.00 op/s (27.2%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 74.05 op/s | 137.4 op/s (186%) | 7.622 op/s (10.3%) | 3.747 op/s (5.1%) | 19.39 op/s (26.2%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 72.07 op/s | 138.1 op/s (192%) | 7.788 op/s (10.8%) | 4.645 op/s (6.4%) | 20.35 op/s (28.2%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 161.8 op/s | 160.8 op/s (99.3%) | 11.86 op/s (7.3%) | 12.72 op/s (7.9%) | 20.45 op/s (12.6%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 79.16 op/s | 154.3 op/s (195%) | 7.553 op/s (9.5%) | 11.08 op/s (14.0%) | 24.57 op/s (31.0%) |

![1.8.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 49.41 ms | 19.64 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 251.1 ms | 93.71 ms (0.4×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 253.4 ms | 92.78 ms (0.4×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 77.08 ms | 77.78 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 20.24 op/s | 50.91 op/s (252%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 3.982 op/s | 10.67 op/s (268%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 3.946 op/s | 10.78 op/s (273%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 12.97 op/s | 12.86 op/s (99.1%) |

![1.8.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2052.4 ms | 244.8 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3843.7 ms | 1022.2 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3989.1 ms | 1135.8 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 838.2 ms | 802.5 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.487 op/s | 4.085 op/s (838%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.260 op/s | 0.978 op/s (376%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.251 op/s | 0.880 op/s (351%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.193 op/s | 1.246 op/s (104%) |

![1.8.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 28157 ms | 3364.5 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.036 op/s | 0.297 op/s (837%) |

![1.8.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-5-backedges-n-100-000-000.svg)

### 1.9 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.064 ms | 0.065 ms (1.0×) | 0.202 ms (3.2×) | 0.391 ms (6.1×) | 0.611 ms (9.5×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.077 ms | 0.062 ms (0.8×) | 0.367 ms (4.8×) | 0.562 ms (7.3×) | 0.490 ms (6.4×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.053 ms | 0.047 ms (0.9×) | 0.498 ms (9.4×) | 0.700 ms (13×) | 0.650 ms (12×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.182 ms | 0.090 ms (0.5×) | 0.707 ms (3.9×) | 1.446 ms (7.9×) | 0.883 ms (4.9×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.533 ms | 0.369 ms (0.7×) | 6.936 ms (13×) | 16.58 ms (31×) | 3.212 ms (6.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.528 ms | 0.382 ms (0.7×) | 7.719 ms (15×) | 16.52 ms (31×) | 3.149 ms (6.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.296 ms | 0.303 ms (1.0×) | 5.107 ms (17×) | 4.767 ms (16×) | 4.784 ms (16×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.402 ms | 0.300 ms (0.7×) | 6.272 ms (16×) | 6.516 ms (16×) | 3.051 ms (7.6×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 15 625 op/s | 15 385 op/s (98.5%) | 4 950 op/s (31.7%) | 2 558 op/s (16.4%) | 1 637 op/s (10.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 12 987 op/s | 16 129 op/s (124%) | 2 725 op/s (21.0%) | 1 779 op/s (13.7%) | 2 041 op/s (15.7%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s | 21 277 op/s (113%) | 2 008 op/s (10.6%) | 1 429 op/s (7.6%) | 1 538 op/s (8.2%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 5 495 op/s | 11 111 op/s (202%) | 1 414 op/s (25.7%) | 691.6 op/s (12.6%) | 1 133 op/s (20.6%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 876 op/s | 2 710 op/s (144%) | 144.2 op/s (7.7%) | 60.31 op/s (3.2%) | 311.3 op/s (16.6%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 894 op/s | 2 618 op/s (138%) | 129.6 op/s (6.8%) | 60.55 op/s (3.2%) | 317.6 op/s (16.8%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3 378 op/s | 3 300 op/s (97.7%) | 195.8 op/s (5.8%) | 209.8 op/s (6.2%) | 209.0 op/s (6.2%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 488 op/s | 3 333 op/s (134%) | 159.4 op/s (6.4%) | 153.5 op/s (6.2%) | 327.8 op/s (13.2%) |

![1.9.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.603 ms | 0.605 ms (1.0×) | 7.376 ms (12×) | 11.48 ms (19×) | 4.590 ms (7.6×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.430 ms | 0.634 ms (0.4×) | 27.91 ms (20×) | 19.76 ms (14×) | 6.337 ms (4.4×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.497 ms | 1.071 ms (0.7×) | 29.47 ms (20×) | 28.19 ms (19×) | 17.91 ms (12×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.696 ms | 1.962 ms (0.5×) | 48.84 ms (13×) | 53.84 ms (15×) | 14.20 ms (3.8×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.537 ms | 4.320 ms (0.6×) | 167.7 ms (22×) | 257.6 ms (34×) | 58.94 ms (7.8×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 7.584 ms | 4.299 ms (0.6×) | 171.3 ms (23×) | 229.0 ms (30×) | 60.77 ms (8.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.696 ms | 2.754 ms (1.0×) | 100.4 ms (37×) | 93.77 ms (35×) | 59.96 ms (22×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.171 ms | 2.973 ms (0.5×) | 163.0 ms (26×) | 114.9 ms (19×) | 57.97 ms (9.4×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 658 op/s | 1 653 op/s (99.7%) | 135.6 op/s (8.2%) | 87.11 op/s (5.3%) | 217.9 op/s (13.1%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 699.3 op/s | 1 577 op/s (226%) | 35.83 op/s (5.1%) | 50.61 op/s (7.2%) | 157.8 op/s (22.6%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 668.0 op/s | 933.7 op/s (140%) | 33.93 op/s (5.1%) | 35.47 op/s (5.3%) | 55.84 op/s (8.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 270.6 op/s | 509.7 op/s (188%) | 20.48 op/s (7.6%) | 18.57 op/s (6.9%) | 70.41 op/s (26.0%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 132.7 op/s | 231.5 op/s (174%) | 5.963 op/s (4.5%) | 3.882 op/s (2.9%) | 16.97 op/s (12.8%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 131.9 op/s | 232.6 op/s (176%) | 5.838 op/s (4.4%) | 4.367 op/s (3.3%) | 16.46 op/s (12.5%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 370.9 op/s | 363.1 op/s (97.9%) | 9.960 op/s (2.7%) | 10.66 op/s (2.9%) | 16.68 op/s (4.5%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 162.0 op/s | 336.4 op/s (208%) | 6.135 op/s (3.8%) | 8.703 op/s (5.4%) | 17.25 op/s (10.6%) |

![1.9.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 69.55 ms | 22.69 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 252.1 ms | 60.84 ms (0.2×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 252.0 ms | 60.77 ms (0.2×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 26.76 ms | 26.80 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 14.38 op/s | 44.08 op/s (307%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 3.967 op/s | 16.44 op/s (414%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 3.968 op/s | 16.46 op/s (415%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 37.37 op/s | 37.32 op/s (99.9%) |

![1.9.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2557.2 ms | 447.1 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4861.4 ms | 1624.0 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4853.4 ms | 1557.3 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 336.4 ms | 284.6 ms (0.8×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.391 op/s | 2.237 op/s (572%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.206 op/s | 0.616 op/s (299%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.206 op/s | 0.642 op/s (312%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 2.973 op/s | 3.514 op/s (118%) |

![1.9.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 32754 ms | 7210.5 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.031 op/s | 0.139 op/s (454%) |

![1.9.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-5-edgetypes-n-100-000-000.svg)

### 1.10 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.110 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.130 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.090 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.385 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.818 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.817 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.533 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.618 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 091 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 692 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 11 111 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 597 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 222 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 224 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 876 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 618 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.043 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.016 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.314 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.436 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 11.74 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 11.77 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.406 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 9.153 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 958.8 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 496.0 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 432.2 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 184.0 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 85.17 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 84.97 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 185.0 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 109.3 op/s |

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 84.36 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 336.7 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 336.1 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 53.13 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 11.85 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 2.970 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 2.975 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 18.82 op/s |

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3401.7 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 7782.3 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 7838.8 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 622.8 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.294 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.128 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.128 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.606 op/s |

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 41896 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.024 op/s |

### 1.11 BiAdjacency (in-adjacency) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.022 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.022 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.023 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.058 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.548 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.547 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.542 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.547 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 45 455 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 45 455 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 43 478 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 17 241 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 825 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 828 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 845 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 828 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.204 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.236 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.613 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.423 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.932 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 7.744 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.555 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 7.824 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 4 902 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 237 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 631 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 702.7 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 126.1 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 129.1 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 180.0 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 127.8 op/s |

### 1.12 Symmetrize (WebGraph) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 5.113 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7.914 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 8.270 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 18.40 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 113.2 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 95.85 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 14.77 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 67.55 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 195.6 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 126.4 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 120.9 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 54.34 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 8.834 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 10.43 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 67.71 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 14.80 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 34.09 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 63.65 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 68.92 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 185.3 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1131.1 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 1157.1 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 120.7 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 685.3 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 29.33 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 15.71 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 14.51 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.397 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 0.884 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 0.864 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.285 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1.459 op/s |

### 1.13 Dominators

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

### 1.14 Dominance frontiers

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

### 1.15 Iterated dominance frontier (build + one query per variable)

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

### 1.16 Loops

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

### 1.17 Ancestry of the dominator tree

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

### 1.18 PreorderAncestry of the dominator tree

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

Retained heap in bytes for a fully built graph. Lower is better. Baseline (= 100%): **Graphity 2.2.0**, or the newest version present in a table it is missing from. Two charts per section: *Graphity & WebGraph* (same league) and *All libraries*.

### 2.1 Total Graph Size

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5037%) | 2.7 MB (3375%) | 18.8 kB (23.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5037%) | 2.7 MB (3375%) | 37.8 kB (47.3%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5043%) | 2.7 MB (3378%) | 31.9 kB (39.8%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 160 kB | 8.6 MB (5343%) | 4.8 MB (2984%) | 78.8 kB (49.2%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB | 68.6 MB (5544%) | 33.4 MB (2700%) | 475 kB (38.4%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB | 68.6 MB (5543%) | 33.4 MB (2700%) | 475 kB (38.4%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB | 71.3 MB (5558%) | 22.7 MB (1769%) | 50.6 kB (3.9%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB | 71.3 MB (5559%) | 22.7 MB (1769%) | 482 kB (37.6%) |
| Path<br><sub>100k nodes, 100k edges</sub> | 800 kB | 41.5 MB (5192%) | 27.4 MB (3430%) | 181 kB (22.6%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 800 kB | 41.5 MB (5192%) | 27.4 MB (3430%) | 430 kB (53.7%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 800 kB | 41.7 MB (5208%) | 27.5 MB (3443%) | 372 kB (46.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.6 MB | 85.0 MB (5310%) | 48.3 MB (3016%) | 952 kB (59.5%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.4 MB | 680 MB (5488%) | 337 MB (2716%) | 6.4 MB (51.7%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 12.4 MB | 680 MB (5488%) | 337 MB (2716%) | 6.4 MB (51.7%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB | 706 MB (5506%) | 228 MB (1777%) | 499 kB (3.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB | 706 MB (5506%) | 228 MB (1777%) | 6.6 MB (51.6%) |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 16.0 MB | — | — | — |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 160 MB | — | — | — |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 1600 MB | — | — | — |

**Graphity & WebGraph**

![2.1 · Total Graph Size](fig/synthetic-ubuntu-ryzen-9950x-peers/2-1-total-graph-size-a-league.svg)

**All libraries**

![2.1 · Total Graph Size](fig/synthetic-ubuntu-ryzen-9950x-peers/2-1-total-graph-size-b-vs-graphity.svg)

### 2.2 Bytes per Element  `totalBytes / (Nodes + Edges)`

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 4 B | 201.65 B (5041%) | 135.10 B (3378%) | 0.94 B (23.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 4 B | 201.65 B (5041%) | 135.10 B (3378%) | 1.89 B (47.2%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 B | 201.90 B (5048%) | 135.24 B (3381%) | 1.60 B (40.0%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 B | 213.79 B (5345%) | 119.42 B (2986%) | 1.97 B (49.2%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4 B | 221.75 B (5544%) | 108.02 B (2700%) | 1.54 B (38.5%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 4 B | 221.75 B (5544%) | 108.01 B (2700%) | 1.53 B (38.2%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4 B | 222.34 B (5558%) | 70.78 B (1770%) | 0.16 B (4.0%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4 B | 222.36 B (5559%) | 70.78 B (1770%) | 1.50 B (37.5%) |
| Path<br><sub>100k nodes, 100k edges</sub> | 4 B | 207.71 B (5193%) | 137.23 B (3431%) | 0.90 B (22.5%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 B | 207.71 B (5193%) | 137.22 B (3430%) | 2.15 B (53.8%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 4 B | 208.34 B (5208%) | 137.73 B (3443%) | 1.86 B (46.5%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4 B | 212.40 B (5310%) | 120.65 B (3016%) | 2.38 B (59.5%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4 B | 219.51 B (5488%) | 108.63 B (2716%) | 2.07 B (51.7%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 4 B | 219.52 B (5488%) | 108.63 B (2716%) | 2.07 B (51.7%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4 B | 220.25 B (5506%) | 71.08 B (1777%) | 0.16 B (4.0%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4 B | 220.25 B (5506%) | 71.08 B (1777%) | 2.06 B (51.5%) |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 4 B | — | — | — |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4 B | — | — | — |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 4 B | — | — | — |

**Graphity & WebGraph**

![2.2 · Bytes per Element  `totalBytes / (Nodes + Edges)`](fig/synthetic-ubuntu-ryzen-9950x-peers/2-2-bytes-per-element-totalbytes-nodes-edges-a-league.svg)

**All libraries**

![2.2 · Bytes per Element  `totalBytes / (Nodes + Edges)`](fig/synthetic-ubuntu-ryzen-9950x-peers/2-2-bytes-per-element-totalbytes-nodes-edges-b-vs-graphity.svg)

## 3 Allocations

Bytes allocated per algorithm invocation (JMH `-prof gc`). Lower is better. Baseline (= 100%): **Graphity 2.2.0**, or the newest version present in a table it is missing from.

### 3.1 Build

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 458 kB | 5.3 MB (1163%) | 2.8 MB (604%) | 4.3 MB (948%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 458 kB | 5.2 MB (1133%) | 2.8 MB (604%) | 4.4 MB (954%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 458 kB | 5.3 MB (1161%) | 2.8 MB (609%) | 4.1 MB (887%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 960 kB | 13.1 MB (1360%) | 5.1 MB (536%) | 4.4 MB (463%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB | 111 MB (1116%) | 37.3 MB (376%) | 9.7 MB (98.2%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB | 111 MB (1116%) | 37.3 MB (376%) | 9.7 MB (98.1%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB | 116 MB (1159%) | 29.3 MB (294%) | 8.9 MB (89.7%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB | 116 MB (1159%) | 29.3 MB (294%) | 9.4 MB (94.1%) |

![3.1.1 · Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-1-1-build-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 54.4 MB (1447%) | 28.5 MB (757%) | 14.2 MB (378%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 54.3 MB (1443%) | 28.5 MB (757%) | 14.5 MB (385%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 55.6 MB (1479%) | 28.8 MB (766%) | 11.4 MB (302%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB | 129 MB (1215%) | 52.3 MB (492%) | 15.3 MB (144%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB | 1094 MB (983%) | 376 MB (338%) | 69.8 MB (62.7%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 111 MB | 1094 MB (983%) | 376 MB (338%) | 69.8 MB (62.7%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB | 1139 MB (1020%) | 294 MB (264%) | 60.2 MB (53.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB | 1139 MB (1020%) | 294 MB (264%) | 66.4 MB (59.5%) |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 534 kB | 5.2 MB (975%) | 4.0 MB (758%) | 1.4 MB (269%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 534 kB | 5.2 MB (966%) | 4.0 MB (747%) | 1.3 MB (247%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 288 kB | 5.0 MB (1746%) | 3.7 MB (1299%) | 908 kB (315%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 371 kB | 3.2 MB (854%) | 4.0 MB (1091%) | 1.3 MB (346%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 498 kB | 6.3 MB (1276%) | 5.2 MB (1044%) | 1.5 MB (295%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 497 kB | 6.3 MB (1277%) | 5.2 MB (1044%) | 1.5 MB (294%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 495 kB | 4.8 MB (964%) | 8.5 MB (1720%) | 6.1 MB (1237%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 364 kB | 6.0 MB (1634%) | 7.2 MB (1982%) | 1.3 MB (348%) |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 4.9 MB | 51.6 MB (1056%) | 28.9 MB (591%) | 13.3 MB (272%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4.9 MB | 51.7 MB (1058%) | 41.8 MB (855%) | 13.5 MB (275%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB | 50.3 MB (1792%) | 25.5 MB (907%) | 8.8 MB (314%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.5 MB | 32.1 MB (916%) | 42.9 MB (1222%) | 12.3 MB (351%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4.5 MB | 71.7 MB (1588%) | 46.2 MB (1023%) | 13.5 MB (298%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 4.5 MB | 71.7 MB (1588%) | 53.7 MB (1189%) | 13.5 MB (298%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.5 MB | 48.7 MB (1081%) | 76.8 MB (1706%) | 60.3 MB (1339%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB | 61.4 MB (1777%) | 89.7 MB (2598%) | 12.3 MB (356%) |

![3.2.2 · SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-2-2-scc-n-100-000.svg)

### 3.3 WCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB | 3.0 MB (1516%) | 3.0 MB (1516%) | 2.0 MB (989%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB | 2.3 MB (1156%) | 3.0 MB (1516%) | 1.9 MB (929%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB | 3.1 MB (1545%) | 3.0 MB (1516%) | 1.2 MB (611%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB | 3.1 MB (1551%) | 3.5 MB (1754%) | 1.2 MB (600%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 10.3 MB (5169%) | 9.3 MB (4668%) | 1.3 MB (650%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 10.3 MB (5169%) | 9.3 MB (4668%) | 1.3 MB (641%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB | 9.4 MB (4720%) | 9.6 MB (4803%) | 6.1 MB (3071%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB | 9.4 MB (4720%) | 9.6 MB (4803%) | 1.3 MB (642%) |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 32.7 MB (1635%) | 26.3 MB (1315%) | 19.3 MB (964%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 32.7 MB (1635%) | 26.3 MB (1315%) | 16.5 MB (824%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 33.2 MB (1661%) | 26.3 MB (1315%) | 11.1 MB (557%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB | 37.5 MB (1875%) | 37.5 MB (1873%) | 11.0 MB (552%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 106 MB (5304%) | 95.9 MB (4794%) | 11.7 MB (587%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 106 MB (5304%) | 95.9 MB (4794%) | 11.8 MB (589%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB | 101 MB (5037%) | 98.4 MB (4918%) | 61.4 MB (3071%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB | 96.7 MB (4837%) | 98.4 MB (4918%) | 12.5 MB (623%) |

![3.3.2 · WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-3-2-wcc-n-100-000.svg)

### 3.4 CCs

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB |

### 3.5 Condense

**Allocations @ 1k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 23.2 kB |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 96 B |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 760 B |
| DAG GNP sparse<br><sub>1k nodes</sub> | 66.4 kB |

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 250 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 97 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 7.2 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 541 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.1 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 107 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 75.7 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 5.9 MB |

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 297 kB | 1.3 MB (422%) | 943 kB (318%) | 1.5 MB (494%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 297 kB | 1.3 MB (422%) | 2.4 MB (803%) | 1.0 MB (345%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 50.8 kB | 1.3 MB (2507%) | 2.3 MB (4430%) | 885 kB (1742%) |
| Tree shfld.<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2507%) | 2.3 MB (4430%) | 845 kB (1663%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2507%) | 2.3 MB (4430%) | 984 kB (1936%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2468%) | 812 kB (1597%) | 1.1 MB (2162%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2468%) | 2.3 MB (4430%) | 1.1 MB (2119%) |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.5 MB | 13.3 MB (529%) | 24.3 MB (968%) | 10.5 MB (417%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.5 MB | 13.3 MB (529%) | 8.9 MB (354%) | 10.0 MB (400%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 433 kB | 13.5 MB (3108%) | 7.8 MB (1811%) | 8.5 MB (1953%) |
| Tree shfld.<br><sub>100k nodes</sub> | 433 kB | 13.5 MB (3108%) | 22.2 MB (5134%) | 8.4 MB (1945%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 433 kB | 13.6 MB (3128%) | 12.6 MB (2904%) | 9.5 MB (2203%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 433 kB | 13.3 MB (3074%) | 7.9 MB (1812%) | 10.3 MB (2375%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 433 kB | 13.3 MB (3074%) | 7.9 MB (1812%) | 10.2 MB (2361%) |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.7 MB (1088%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.7 MB (1070%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.3 MB (828%) |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.3 MB (796%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 476 kB (297%) | 1.6 MB (1016%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB | 160 kB (100.0%) | 476 kB (297%) | 1.9 MB (1196%) | 1.8 MB (1126%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB | 160 kB (100.0%) | 476 kB (297%) | 1.9 MB (1196%) | 1.8 MB (1140%) |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | Graphity 2.2.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100%) | 4.8 MB (300%) | 4.8 MB (300%) | 16.9 MB (1054%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100%) | 4.8 MB (300%) | 19.2 MB (1200%) | 16.8 MB (1052%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 19.2 MB (1200%) | 12.5 MB (780%) |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 19.2 MB (1200%) | 12.4 MB (777%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 15.5 MB (967%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 17.1 MB (1070%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 17.1 MB (1069%) |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 257 kB | 161 kB (62.6%) | 161 kB (62.7%) | 1.4 MB (555%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 257 kB | 161 kB (62.6%) | 1.6 MB (624%) | 985 kB (384%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10.9 kB | 161 kB (1476%) | 1.6 MB (14712%) | 845 kB (7768%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 351 kB | 386 kB (110%) | 1.8 MB (520%) | 1.2 MB (336%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4.1 MB | 4.0 MB (97.7%) | 4.8 MB (117%) | 4.9 MB (120%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 4.1 MB | 4.0 MB (97.7%) | 5.4 MB (133%) | 4.8 MB (118%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4.1 MB | 4.0 MB (97.7%) | 5.4 MB (133%) | 9.7 MB (236%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4.0 MB | 4.0 MB (101%) | 5.4 MB (137%) | 4.9 MB (124%) |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.1 MB | 1.6 MB (76.8%) | 16.0 MB (758%) | 10.1 MB (477%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.1 MB | 1.6 MB (76.8%) | 16.0 MB (758%) | 9.6 MB (456%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 33.4 kB | 1.6 MB (4861%) | 1.6 MB (4861%) | 8.1 MB (24141%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.6 MB | 4.2 MB (115%) | 4.2 MB (115%) | 12.1 MB (332%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 45.9 MB | 45.4 MB (98.9%) | 53.9 MB (117%) | 53.6 MB (117%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 45.9 MB | 45.4 MB (98.9%) | 53.9 MB (118%) | 53.6 MB (117%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 45.9 MB | 45.4 MB (98.9%) | 59.8 MB (130%) | 101 MB (219%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 44.8 MB | 45.4 MB (101%) | 45.4 MB (101%) | 53.5 MB (119%) |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 307 kB | 581 kB (190%) | 581 kB (190%) | 1.8 MB (602%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 307 kB | 581 kB (190%) | 2.0 MB (659%) | 1.4 MB (458%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 60.8 kB | 581 kB (956%) | 583 kB (958%) | 1.3 MB (2081%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 196 kB | 1.1 MB (540%) | 2.5 MB (1276%) | 1.8 MB (945%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 596 kB | 9.9 MB (1665%) | 10.7 MB (1799%) | 10.8 MB (1818%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 596 kB | 9.9 MB (1665%) | 11.4 MB (1906%) | 10.8 MB (1806%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 607 kB | 9.9 MB (1634%) | 11.4 MB (1871%) | 15.6 MB (2565%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 476 kB | 9.9 MB (2084%) | 11.4 MB (2386%) | 10.8 MB (2275%) |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.6 MB | 4.9 MB (188%) | 19.3 MB (738%) | 13.4 MB (511%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.6 MB | 4.9 MB (188%) | 19.3 MB (738%) | 12.9 MB (495%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 533 kB | 4.9 MB (920%) | 4.9 MB (920%) | 11.3 MB (2127%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.8 MB | 11.7 MB (665%) | 26.1 MB (1481%) | 19.6 MB (1112%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 5.5 MB | 113 MB (2046%) | 121 MB (2191%) | 121 MB (2194%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 5.5 MB | 113 MB (2046%) | 121 MB (2191%) | 121 MB (2195%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.6 MB | 113 MB (2008%) | 127 MB (2264%) | 168 MB (2990%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4.6 MB | 113 MB (2469%) | 117 MB (2556%) | 121 MB (2646%) |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-9-2-edgetypes-n-100-000.svg)

### 3.10 Dominators

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 371 kB |
| Irreducible<br><sub>10k nodes</sub> | 371 kB |
| Ladder<br><sub>10k nodes</sub> | 486 kB |
| Chain<br><sub>10k nodes</sub> | 566 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 486 kB |
| Small methods<br><sub>10k nodes</sub> | 475 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 3.6 MB |
| Irreducible<br><sub>100k nodes</sub> | 3.7 MB |
| Ladder<br><sub>100k nodes</sub> | 4.7 MB |
| Chain<br><sub>100k nodes</sub> | 5.5 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 4.7 MB |
| Small methods<br><sub>100k nodes</sub> | 4.7 MB |

### 3.11 Dominance frontiers

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 134 kB |
| Irreducible<br><sub>10k nodes</sub> | 144 kB |
| Ladder<br><sub>10k nodes</sub> | 50.1 MB |
| Chain<br><sub>10k nodes</sub> | 80.1 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 100 MB |
| Small methods<br><sub>10k nodes</sub> | 140 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 1.3 MB |
| Irreducible<br><sub>100k nodes</sub> | 1.4 MB |
| Chain<br><sub>100k nodes</sub> | 800 kB |
| Small methods<br><sub>100k nodes</sub> | 1.4 MB |

### 3.12 Iterated dominance frontier (build + one query per variable)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 25.4 MB |
| Irreducible<br><sub>10k nodes</sub> | 28.9 MB |
| Ladder<br><sub>10k nodes</sub> | 53.8 MB |
| Chain<br><sub>10k nodes</sub> | 21.4 MB |
| Fan behind chain<br><sub>10k nodes</sub> | 65.4 MB |
| Small methods<br><sub>10k nodes</sub> | 604 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 370 MB |
| Irreducible<br><sub>100k nodes</sub> | 451 MB |
| Ladder<br><sub>100k nodes</sub> | 504 MB |
| Chain<br><sub>100k nodes</sub> | 198 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 662 MB |
| Small methods<br><sub>100k nodes</sub> | 5.8 MB |

### 3.13 Loops

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 772 kB |
| Irreducible<br><sub>10k nodes</sub> | 812 kB |
| Ladder<br><sub>10k nodes</sub> | 675 kB |
| Chain<br><sub>10k nodes</sub> | 735 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 675 kB |
| Small methods<br><sub>10k nodes</sub> | 897 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 7.5 MB |
| Irreducible<br><sub>100k nodes</sub> | 13.5 MB |
| Ladder<br><sub>100k nodes</sub> | 6.6 MB |
| Chain<br><sub>100k nodes</sub> | 7.2 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 6.6 MB |
| Small methods<br><sub>100k nodes</sub> | 8.9 MB |

### 3.14 Ancestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 280 kB |
| Irreducible<br><sub>10k nodes</sub> | 280 kB |
| Ladder<br><sub>10k nodes</sub> | 280 kB |
| Chain<br><sub>10k nodes</sub> | 280 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 280 kB |
| Small methods<br><sub>10k nodes</sub> | 297 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 2.8 MB |
| Irreducible<br><sub>100k nodes</sub> | 2.8 MB |
| Ladder<br><sub>100k nodes</sub> | 2.8 MB |
| Chain<br><sub>100k nodes</sub> | 2.8 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 2.8 MB |
| Small methods<br><sub>100k nodes</sub> | 2.9 MB |

### 3.15 PreorderAncestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 200 kB |
| Irreducible<br><sub>10k nodes</sub> | 200 kB |
| Ladder<br><sub>10k nodes</sub> | 200 kB |
| Chain<br><sub>10k nodes</sub> | 200 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 200 kB |
| Small methods<br><sub>10k nodes</sub> | 216 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.2.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 2.0 MB |
| Irreducible<br><sub>100k nodes</sub> | 2.0 MB |
| Ladder<br><sub>100k nodes</sub> | 2.0 MB |
| Chain<br><sub>100k nodes</sub> | 2.0 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 2.0 MB |
| Small methods<br><sub>100k nodes</sub> | 2.1 MB |
