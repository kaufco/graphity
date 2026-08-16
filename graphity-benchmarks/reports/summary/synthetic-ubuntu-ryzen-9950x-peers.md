# Graphity vs. JGraphT, Guava and WebGraph — synthetic / ubuntu-ryzen-9950x

Graphity `2.1.0` against the peer libraries. Peer numbers come from the newest run that measured them, which need not be the run of the newest Graphity version; every column states the version it belongs to. Blocks without a peer column are Graphity-only benchmarks or graph sizes beyond the peers' reach — for the comparison across Graphity versions see [`synthetic-ubuntu-ryzen-9950x-history.md`](synthetic-ubuntu-ryzen-9950x-history.md).

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
  - [1.12 Symmetrize (WebGraph) cost](#112-symmetrize-webgraph-cost)
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

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.083 ms | 1.063 ms (13×) | 0.364 ms (4.4×) | 2.132 ms (26×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.084 ms | 1.133 ms (13×) | 0.560 ms (6.7×) | 2.851 ms (34×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.093 ms | 1.240 ms (13×) | 0.526 ms (5.7×) | 2.568 ms (28×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.163 ms | 4.019 ms (25×) | 2.084 ms (13×) | 7.810 ms (48×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.581 ms | 54.03 ms (34×) | 32.60 ms (21×) | 45.52 ms (29×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.623 ms | 54.34 ms (33×) | 32.79 ms (20×) | 43.59 ms (27×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.587 ms | 42.99 ms (27×) | 8.230 ms (5.2×) | 6.465 ms (4.1×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.634 ms | 54.99 ms (34×) | 19.02 ms (12×) | 44.54 ms (27×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s | 940.7 op/s (7.8%) | 2 747 op/s (22.8%) | 469.0 op/s (3.9%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 11 905 op/s | 882.6 op/s (7.4%) | 1 786 op/s (15.0%) | 350.8 op/s (2.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 753 op/s | 806.5 op/s (7.5%) | 1 901 op/s (17.7%) | 389.4 op/s (3.6%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 135 op/s | 248.8 op/s (4.1%) | 479.8 op/s (7.8%) | 128.0 op/s (2.1%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 632.5 op/s | 18.51 op/s (2.9%) | 30.68 op/s (4.8%) | 21.97 op/s (3.5%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 616.1 op/s | 18.40 op/s (3.0%) | 30.50 op/s (4.9%) | 22.94 op/s (3.7%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 630.1 op/s | 23.26 op/s (3.7%) | 121.5 op/s (19.3%) | 154.7 op/s (24.5%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 612.0 op/s | 18.19 op/s (3.0%) | 52.58 op/s (8.6%) | 22.45 op/s (3.7%) |

![1.1.1 Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-1-1-build-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.695 ms | 14.18 ms (20×) | 5.486 ms (7.9×) | 20.09 ms (29×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.694 ms | 22.97 ms (33×) | 8.473 ms (12×) | 28.01 ms (40×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.100 ms | 24.42 ms (22×) | 9.714 ms (8.8×) | 28.67 ms (26×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.218 ms | 94.78 ms (43×) | 47.36 ms (21×) | 86.78 ms (39×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.11 ms | 1502.7 ms (88×) | 1014.7 ms (59×) | 555.1 ms (32×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 17.28 ms | 1556.4 ms (90×) | 1010.1 ms (58×) | 522.4 ms (30×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 17.11 ms | 849.8 ms (50×) | 110.0 ms (6.4×) | 62.31 ms (3.6×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 17.09 ms | 1498.6 ms (88×) | 702.2 ms (41×) | 538.2 ms (31×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 439 op/s | 70.54 op/s (4.9%) | 182.3 op/s (12.7%) | 49.79 op/s (3.5%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 441 op/s | 43.53 op/s (3.0%) | 118.0 op/s (8.2%) | 35.70 op/s (2.5%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 909.1 op/s | 40.95 op/s (4.5%) | 102.9 op/s (11.3%) | 34.88 op/s (3.8%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 450.9 op/s | 10.55 op/s (2.3%) | 21.11 op/s (4.7%) | 11.52 op/s (2.6%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 58.44 op/s | 0.665 op/s (1.1%) | 0.986 op/s (1.7%) | 1.801 op/s (3.1%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 57.87 op/s | 0.643 op/s (1.1%) | 0.990 op/s (1.7%) | 1.914 op/s (3.3%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 58.45 op/s | 1.177 op/s (2.0%) | 9.091 op/s (15.6%) | 16.05 op/s (27.5%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 58.52 op/s | 0.667 op/s (1.1%) | 1.424 op/s (2.4%) | 1.858 op/s (3.2%) |

![1.1.2 Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-1-2-build-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 23.63 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 167.8 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 166.8 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 169.5 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 42.31 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 5.959 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 5.995 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 5.900 op/s |

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 240.4 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 1916.6 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 1829.5 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1794.6 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4.160 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.522 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.547 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 0.557 op/s |

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 2971.7 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.337 op/s |

### 1.2 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.106 ms | 0.106 ms (1.0×) | 0.745 ms (7.0×) | 0.783 ms (7.4×) | 0.345 ms (3.3×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.134 ms | 0.105 ms (0.8×) | 1.249 ms (9.3×) | 1.321 ms (9.9×) | 0.497 ms (3.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.104 ms | 0.086 ms (0.8×) | 1.127 ms (11×) | 1.235 ms (12×) | 0.468 ms (4.5×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.242 ms | 0.112 ms (0.5×) | 2.151 ms (8.9×) | 2.455 ms (10×) | 0.852 ms (3.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.559 ms | 0.381 ms (0.7×) | 21.70 ms (39×) | 18.64 ms (33×) | 2.345 ms (4.2×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.539 ms | 0.373 ms (0.7×) | 23.37 ms (43×) | 22.93 ms (43×) | 2.373 ms (4.4×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.261 ms | 0.260 ms (1.0×) | 15.63 ms (60×) | 5.921 ms (23×) | 3.276 ms (13×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.348 ms | 0.265 ms (0.8×) | 18.26 ms (52×) | 7.138 ms (21×) | 2.052 ms (5.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 434 op/s | 9 434 op/s (100%) | 1 342 op/s (14.2%) | 1 277 op/s (13.5%) | 2 899 op/s (30.7%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 463 op/s | 9 524 op/s (128%) | 800.6 op/s (10.7%) | 757.0 op/s (10.1%) | 2 012 op/s (27.0%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 9 615 op/s | 11 628 op/s (121%) | 887.3 op/s (9.2%) | 809.7 op/s (8.4%) | 2 137 op/s (22.2%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 132 op/s | 8 929 op/s (216%) | 464.9 op/s (11.3%) | 407.3 op/s (9.9%) | 1 174 op/s (28.4%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 789 op/s | 2 625 op/s (147%) | 46.08 op/s (2.6%) | 53.66 op/s (3.0%) | 426.4 op/s (23.8%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 855 op/s | 2 681 op/s (145%) | 42.80 op/s (2.3%) | 43.62 op/s (2.4%) | 421.4 op/s (22.7%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3 831 op/s | 3 846 op/s (100%) | 63.99 op/s (1.7%) | 168.9 op/s (4.4%) | 305.3 op/s (8.0%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 874 op/s | 3 774 op/s (131%) | 54.77 op/s (1.9%) | 140.1 op/s (4.9%) | 487.3 op/s (17.0%) |

![1.2.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.030 ms | 1.025 ms (1.0×) | 11.49 ms (11×) | 23.59 ms (23×) | 3.294 ms (3.2×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.146 ms | 1.035 ms (0.5×) | 50.99 ms (24×) | 45.97 ms (21×) | 5.515 ms (2.6×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.122 ms | 1.305 ms (0.6×) | 38.65 ms (18×) | 37.13 ms (17×) | 5.626 ms (2.7×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.447 ms | 2.265 ms (0.5×) | 104.0 ms (23×) | 103.9 ms (23×) | 10.47 ms (2.4×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.397 ms | 4.431 ms (0.6×) | 492.8 ms (67×) | 426.5 ms (58×) | 36.02 ms (4.9×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 7.548 ms | 4.537 ms (0.6×) | 499.6 ms (66×) | 361.7 ms (48×) | 35.31 ms (4.7×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.606 ms | 2.598 ms (1.0×) | 182.4 ms (70×) | 111.5 ms (43×) | 36.77 ms (14×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4.820 ms | 2.596 ms (0.5×) | 351.0 ms (73×) | 160.2 ms (33×) | 29.60 ms (6.1×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 970.9 op/s | 975.6 op/s (100%) | 87.04 op/s (9.0%) | 42.39 op/s (4.4%) | 303.6 op/s (31.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 466.0 op/s | 966.2 op/s (207%) | 19.61 op/s (4.2%) | 21.75 op/s (4.7%) | 181.3 op/s (38.9%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 471.3 op/s | 766.3 op/s (163%) | 25.87 op/s (5.5%) | 26.94 op/s (5.7%) | 177.7 op/s (37.7%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 224.9 op/s | 441.5 op/s (196%) | 9.615 op/s (4.3%) | 9.625 op/s (4.3%) | 95.51 op/s (42.5%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 135.2 op/s | 225.7 op/s (167%) | 2.029 op/s (1.5%) | 2.345 op/s (1.7%) | 27.76 op/s (20.5%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 132.5 op/s | 220.4 op/s (166%) | 2.002 op/s (1.5%) | 2.765 op/s (2.1%) | 28.32 op/s (21.4%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 383.7 op/s | 384.9 op/s (100%) | 5.482 op/s (1.4%) | 8.969 op/s (2.3%) | 27.20 op/s (7.1%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 207.5 op/s | 385.2 op/s (186%) | 2.849 op/s (1.4%) | 6.242 op/s (3.0%) | 33.78 op/s (16.3%) |

![1.2.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 76.73 ms | 26.04 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 233.4 ms | 61.63 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 229.7 ms | 62.45 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 26.24 ms | 26.27 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 13.03 op/s | 38.41 op/s (295%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.284 op/s | 16.23 op/s (379%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.354 op/s | 16.01 op/s (368%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 38.10 op/s | 38.06 op/s (99.9%) |

![1.2.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3051.6 ms | 700.8 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6518.8 ms | 2978.2 ms (0.5×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 6583.3 ms | 2700.6 ms (0.4×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 275.9 ms | 271.5 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.328 op/s | 1.427 op/s (435%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.153 op/s | 0.336 op/s (219%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.152 op/s | 0.370 op/s (244%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.625 op/s | 3.683 op/s (102%) |

![1.2.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 36265 ms | 9839.3 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.028 op/s | 0.102 op/s (369%) |

![1.2.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-2-5-scc-n-100-000-000.svg)

### 1.3 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms | 0.040 ms (1.1×) | 0.850 ms (23×) | 0.772 ms (21×) | 0.945 ms (26×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.053 ms | 0.042 ms (0.8×) | 1.046 ms (20×) | 1.316 ms (25×) | 1.108 ms (21×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.038 ms | 0.044 ms (1.2×) | 1.295 ms (34×) | 1.008 ms (27×) | 0.761 ms (20×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.097 ms | 0.082 ms (0.8×) | 2.005 ms (21×) | 2.624 ms (27×) | 1.140 ms (12×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.523 ms | 0.520 ms (1.0×) | 22.08 ms (42×) | 21.17 ms (40×) | 4.724 ms (9.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms | 0.521 ms (1.0×) | 28.11 ms (54×) | 23.11 ms (44×) | 4.621 ms (8.8×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.579 ms | 0.578 ms (1.0×) | 8.975 ms (16×) | 9.086 ms (16×) | 4.764 ms (8.2×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.641 ms | 0.614 ms (1.0×) | 11.57 ms (18×) | 12.87 ms (20×) | 2.523 ms (3.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s | 25 000 op/s (92.5%) | 1 176 op/s (4.4%) | 1 295 op/s (4.8%) | 1 058 op/s (3.9%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s | 23 810 op/s (126%) | 956.0 op/s (5.1%) | 759.9 op/s (4.0%) | 902.5 op/s (4.8%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s | 22 727 op/s (86.4%) | 772.2 op/s (2.9%) | 992.1 op/s (3.8%) | 1 314 op/s (5.0%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 10 309 op/s | 12 195 op/s (118%) | 498.8 op/s (4.8%) | 381.1 op/s (3.7%) | 877.2 op/s (8.5%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 912 op/s | 1 923 op/s (101%) | 45.29 op/s (2.4%) | 47.24 op/s (2.5%) | 211.7 op/s (11.1%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s | 1 919 op/s (101%) | 35.57 op/s (1.9%) | 43.26 op/s (2.3%) | 216.4 op/s (11.4%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 727 op/s | 1 730 op/s (100%) | 111.4 op/s (6.5%) | 110.1 op/s (6.4%) | 209.9 op/s (12.2%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 560 op/s | 1 629 op/s (104%) | 86.41 op/s (5.5%) | 77.73 op/s (5.0%) | 396.4 op/s (25.4%) |

![1.3.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.406 ms | 0.352 ms (0.9×) | 14.55 ms (36×) | 10.86 ms (27×) | 8.856 ms (22×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.232 ms | 0.347 ms (0.3×) | 42.43 ms (34×) | 21.20 ms (17×) | 11.38 ms (9.2×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.669 ms | 0.770 ms (1.2×) | 39.06 ms (58×) | 19.51 ms (29×) | 9.234 ms (14×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.794 ms | 1.561 ms (0.9×) | 85.02 ms (47×) | 51.56 ms (29×) | 15.90 ms (8.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.096 ms | 6.054 ms (1.0×) | 498.0 ms (82×) | 348.8 ms (57×) | 49.92 ms (8.2×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.088 ms | 6.118 ms (1.0×) | 440.8 ms (72×) | 296.5 ms (49×) | 49.49 ms (8.1×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.778 ms | 5.764 ms (1.0×) | 152.3 ms (26×) | 108.4 ms (19×) | 51.68 ms (8.9×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.988 ms | 6.133 ms (1.0×) | 251.3 ms (42×) | 263.3 ms (44×) | 32.21 ms (5.4×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 463 op/s | 2 841 op/s (115%) | 68.72 op/s (2.8%) | 92.11 op/s (3.7%) | 112.9 op/s (4.6%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 811.7 op/s | 2 882 op/s (355%) | 23.57 op/s (2.9%) | 47.16 op/s (5.8%) | 87.83 op/s (10.8%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 495 op/s | 1 299 op/s (86.9%) | 25.60 op/s (1.7%) | 51.25 op/s (3.4%) | 108.3 op/s (7.2%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 557.4 op/s | 640.6 op/s (115%) | 11.76 op/s (2.1%) | 19.39 op/s (3.5%) | 62.90 op/s (11.3%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 164.0 op/s | 165.2 op/s (101%) | 2.008 op/s (1.2%) | 2.867 op/s (1.7%) | 20.03 op/s (12.2%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 164.3 op/s | 163.5 op/s (99.5%) | 2.269 op/s (1.4%) | 3.373 op/s (2.1%) | 20.21 op/s (12.3%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 173.1 op/s | 173.5 op/s (100%) | 6.566 op/s (3.8%) | 9.225 op/s (5.3%) | 19.35 op/s (11.2%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 167.0 op/s | 163.1 op/s (97.6%) | 3.979 op/s (2.4%) | 3.798 op/s (2.3%) | 31.05 op/s (18.6%) |

![1.3.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.57 ms | 16.24 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 78.43 ms | 66.27 ms (0.8×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 66.99 ms | 66.64 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 51.65 ms | 50.10 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.37 op/s | 61.58 op/s (133%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 12.75 op/s | 15.09 op/s (118%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.93 op/s | 15.01 op/s (101%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.36 op/s | 19.96 op/s (103%) |

![1.3.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 600.5 ms | 378.4 ms (0.6×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4106.6 ms | 3774.2 ms (0.9×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3919.9 ms | 3828.2 ms (1.0×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 516.1 ms | 516.0 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.665 op/s | 2.643 op/s (159%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.244 op/s | 0.265 op/s (109%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.255 op/s | 0.261 op/s (102%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.938 op/s | 1.938 op/s (100%) |

![1.3.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 9442.8 ms | 6812.3 ms (0.7×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.106 op/s | 0.147 op/s (139%) |

![1.3.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-3-5-wcc-n-100-000-000.svg)

### 1.4 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.042 ms | 0.042 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.045 ms | 0.042 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.042 ms | 0.038 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.094 ms | 0.087 ms (0.9×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.524 ms | 0.527 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.524 ms | 0.521 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.588 ms | 0.588 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.643 ms | 0.611 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 23 810 op/s | 23 810 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 22 222 op/s | 23 810 op/s (107%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 23 810 op/s | 26 316 op/s (111%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 10 638 op/s | 11 494 op/s (108%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 908 op/s | 1 898 op/s (99.4%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 908 op/s | 1 919 op/s (101%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 701 op/s | 1 701 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 555 op/s | 1 637 op/s (105%) |

![1.4.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.394 ms | 0.394 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.232 ms | 0.365 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.664 ms | 0.751 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.763 ms | 1.542 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.117 ms | 6.066 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.175 ms | 6.032 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.816 ms | 5.781 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.935 ms | 6.115 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 538 op/s | 2 538 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 811.7 op/s | 2 740 op/s (338%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 506 op/s | 1 332 op/s (88.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 567.2 op/s | 648.5 op/s (114%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 163.5 op/s | 164.9 op/s (101%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 161.9 op/s | 165.8 op/s (102%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 171.9 op/s | 173.0 op/s (101%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 168.5 op/s | 163.5 op/s (97.1%) |

![1.4.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 19.65 ms | 17.28 ms (0.9×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 67.00 ms | 69.08 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 66.92 ms | 67.00 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.47 ms | 50.11 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 50.88 op/s | 57.86 op/s (114%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 14.93 op/s | 14.48 op/s (97.0%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.94 op/s | 14.92 op/s (99.9%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.82 op/s | 19.96 op/s (101%) |

![1.4.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 602.9 ms | 409.6 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4102.7 ms | 3844.3 ms (0.9×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3884.4 ms | 3959.5 ms (1.0×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 550.5 ms | 595.1 ms (1.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.659 op/s | 2.441 op/s (147%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.244 op/s | 0.260 op/s (107%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.257 op/s | 0.253 op/s (98.1%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.817 op/s | 1.680 op/s (92.5%) |

![1.4.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8563.2 ms | 6788.3 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.117 op/s | 0.147 op/s (126%) |

![1.4.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-5-ccs-n-100-000-000.svg)

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

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.058 ms | 0.066 ms (1.1×) | 0.433 ms (7.5×) | 0.510 ms (8.8×) | 0.555 ms (9.6×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.070 ms | 0.052 ms (0.7×) | 0.702 ms (10×) | 0.867 ms (12×) | 0.429 ms (6.1×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.056 ms | 0.053 ms (0.9×) | 0.713 ms (13×) | 0.775 ms (14×) | 0.626 ms (11×) |
| Tree shfld. | 0.050 ms | 0.047 ms (0.9×) | 0.713 ms (14×) | 0.686 ms (14×) | 0.519 ms (10×) |
| DAG GNP sparse | 0.084 ms | 0.067 ms (0.8×) | 1.153 ms (14×) | 1.512 ms (18×) | 0.823 ms (9.8×) |
| DAG GNP dense | 0.324 ms | 0.243 ms (0.8×) | 19.51 ms (60×) | 9.331 ms (29×) | 2.086 ms (6.4×) |
| DAG GNP dense shfld. | 0.371 ms | 0.233 ms (0.6×) | 17.45 ms (47×) | 12.55 ms (34×) | 2.328 ms (6.3×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s | 15 152 op/s (87.9%) | 2 309 op/s (13.4%) | 1 961 op/s (11.4%) | 1 802 op/s (10.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 14 286 op/s | 19 231 op/s (135%) | 1 425 op/s (10.0%) | 1 153 op/s (8.1%) | 2 331 op/s (16.3%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 17 857 op/s | 18 868 op/s (106%) | 1 403 op/s (7.9%) | 1 290 op/s (7.2%) | 1 597 op/s (8.9%) |
| Tree shfld. | 20 000 op/s | 21 277 op/s (106%) | 1 403 op/s (7.0%) | 1 458 op/s (7.3%) | 1 927 op/s (9.6%) |
| DAG GNP sparse | 11 905 op/s | 14 925 op/s (125%) | 867.3 op/s (7.3%) | 661.4 op/s (5.6%) | 1 215 op/s (10.2%) |
| DAG GNP dense | 3 086 op/s | 4 115 op/s (133%) | 51.25 op/s (1.7%) | 107.2 op/s (3.5%) | 479.4 op/s (15.5%) |
| DAG GNP dense shfld. | 2 695 op/s | 4 292 op/s (159%) | 57.30 op/s (2.1%) | 79.66 op/s (3.0%) | 429.6 op/s (15.9%) |

![1.6.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.632 ms | 0.629 ms (1.0×) | 7.313 ms (12×) | 15.87 ms (25×) | 3.527 ms (5.6×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.964 ms | 0.521 ms (0.5×) | 26.47 ms (27×) | 26.03 ms (27×) | 4.977 ms (5.2×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.440 ms | 1.142 ms (0.8×) | 25.34 ms (18×) | 18.86 ms (13×) | 14.65 ms (10×) |
| Tree shfld. | 1.475 ms | 1.242 ms (0.8×) | 25.92 ms (18×) | 20.13 ms (14×) | 10.00 ms (6.8×) |
| DAG GNP sparse | 2.201 ms | 1.470 ms (0.7×) | 64.26 ms (29×) | 43.94 ms (20×) | 17.29 ms (7.9×) |
| DAG GNP dense | 4.333 ms | 3.086 ms (0.7×) | 267.6 ms (62×) | 298.3 ms (69×) | 24.72 ms (5.7×) |
| DAG GNP dense shfld. | 5.127 ms | 3.085 ms (0.6×) | 266.9 ms (52×) | 306.7 ms (60×) | 31.33 ms (6.1×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 582 op/s | 1 590 op/s (100%) | 136.7 op/s (8.6%) | 63.00 op/s (4.0%) | 283.5 op/s (17.9%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 037 op/s | 1 919 op/s (185%) | 37.78 op/s (3.6%) | 38.42 op/s (3.7%) | 200.9 op/s (19.4%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 694.4 op/s | 875.7 op/s (126%) | 39.46 op/s (5.7%) | 53.02 op/s (7.6%) | 68.27 op/s (9.8%) |
| Tree shfld. | 678.0 op/s | 805.2 op/s (119%) | 38.58 op/s (5.7%) | 49.69 op/s (7.3%) | 99.96 op/s (14.7%) |
| DAG GNP sparse | 454.3 op/s | 680.3 op/s (150%) | 15.56 op/s (3.4%) | 22.76 op/s (5.0%) | 57.83 op/s (12.7%) |
| DAG GNP dense | 230.8 op/s | 324.0 op/s (140%) | 3.737 op/s (1.6%) | 3.352 op/s (1.5%) | 40.45 op/s (17.5%) |
| DAG GNP dense shfld. | 195.0 op/s | 324.1 op/s (166%) | 3.747 op/s (1.9%) | 3.261 op/s (1.7%) | 31.92 op/s (16.4%) |

![1.6.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 30.46 ms | 14.97 ms (0.5×) |
| DAG GNP dense | 103.6 ms | 37.35 ms (0.4×) |
| DAG GNP dense shfld. | 173.5 ms | 38.60 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 32.83 op/s | 66.82 op/s (204%) |
| DAG GNP dense | 9.653 op/s | 26.77 op/s (277%) |
| DAG GNP dense shfld. | 5.764 op/s | 25.90 op/s (449%) |

![1.6.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 1145.1 ms | 156.7 ms (0.1×) |
| DAG GNP dense | 1890.5 ms | 450.2 ms (0.2×) |
| DAG GNP dense shfld. | 2805.5 ms | 405.1 ms (0.1×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.873 op/s | 6.382 op/s (731%) |
| DAG GNP dense | 0.529 op/s | 2.221 op/s (420%) |
| DAG GNP dense shfld. | 0.356 op/s | 2.469 op/s (693%) |

![1.6.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 17065 ms | 1759.8 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.059 op/s | 0.568 op/s (970%) |

![1.6.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-5-toporder-n-100-000-000.svg)

### 1.7 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.040 ms | 0.040 ms (1.0×) | 0.041 ms (1.0×) | 0.215 ms (5.4×) | 0.350 ms (8.8×) | 0.401 ms (10×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.056 ms | 0.040 ms (0.7×) | 0.054 ms (1.0×) | 0.385 ms (6.9×) | 0.497 ms (8.9×) | 0.642 ms (11×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.034 ms | 0.027 ms (0.8×) | 0.028 ms (0.8×) | 0.212 ms (6.2×) | 0.531 ms (16×) | 0.648 ms (19×) |
| Tree shfld. | 0.034 ms | 0.031 ms (0.9×) | 0.030 ms (0.9×) | 0.207 ms (6.1×) | 0.542 ms (16×) | 0.672 ms (20×) |
| DAG GNP sparse | 0.052 ms | 0.054 ms (1.0×) | 0.042 ms (0.8×) | 0.311 ms (6.0×) | 1.132 ms (22×) | 1.061 ms (20×) |
| DAG GNP dense | 0.355 ms | 0.324 ms (0.9×) | 0.240 ms (0.7×) | 4.084 ms (12×) | 6.704 ms (19×) | 3.702 ms (10×) |
| DAG GNP dense shfld. | 0.400 ms | 0.321 ms (0.8×) | 0.289 ms (0.7×) | 4.129 ms (10×) | 8.960 ms (22×) | 4.128 ms (10×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 000 op/s | 25 000 op/s (100%) | 24 390 op/s (97.6%) | 4 651 op/s (18.6%) | 2 857 op/s (11.4%) | 2 494 op/s (10.0%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 857 op/s | 25 000 op/s (140%) | 18 519 op/s (104%) | 2 597 op/s (14.5%) | 2 012 op/s (11.3%) | 1 558 op/s (8.7%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 29 412 op/s | 37 037 op/s (126%) | 35 714 op/s (121%) | 4 717 op/s (16.0%) | 1 883 op/s (6.4%) | 1 543 op/s (5.2%) |
| Tree shfld. | 29 412 op/s | 32 258 op/s (110%) | 33 333 op/s (113%) | 4 831 op/s (16.4%) | 1 845 op/s (6.3%) | 1 488 op/s (5.1%) |
| DAG GNP sparse | 19 231 op/s | 18 519 op/s (96.3%) | 23 810 op/s (124%) | 3 215 op/s (16.7%) | 883.4 op/s (4.6%) | 942.5 op/s (4.9%) |
| DAG GNP dense | 2 817 op/s | 3 086 op/s (110%) | 4 167 op/s (148%) | 244.9 op/s (8.7%) | 149.2 op/s (5.3%) | 270.1 op/s (9.6%) |
| DAG GNP dense shfld. | 2 500 op/s | 3 115 op/s (125%) | 3 460 op/s (138%) | 242.2 op/s (9.7%) | 111.6 op/s (4.5%) | 242.2 op/s (9.7%) |

![1.7.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.457 ms | 0.458 ms (1.0×) | 0.348 ms (0.8×) | 4.437 ms (9.7×) | 4.595 ms (10×) | 3.680 ms (8.1×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.459 ms | 0.564 ms (0.4×) | 1.253 ms (0.9×) | 11.67 ms (8.0×) | 10.91 ms (7.5×) | 6.985 ms (4.8×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.927 ms | 0.782 ms (0.8×) | 0.839 ms (0.9×) | 7.025 ms (7.6×) | 12.08 ms (13×) | 7.662 ms (8.3×) |
| Tree shfld. | 1.032 ms | 0.891 ms (0.9×) | 0.884 ms (0.9×) | 6.854 ms (6.6×) | 11.65 ms (11×) | 7.885 ms (7.6×) |
| DAG GNP sparse | 2.159 ms | 1.962 ms (0.9×) | 1.989 ms (0.9×) | 19.93 ms (9.2×) | 33.34 ms (15×) | 14.16 ms (6.6×) |
| DAG GNP dense | 5.070 ms | 4.551 ms (0.9×) | 3.777 ms (0.7×) | 64.39 ms (13×) | 210.1 ms (41×) | 44.57 ms (8.8×) |
| DAG GNP dense shfld. | 5.763 ms | 4.496 ms (0.8×) | 4.487 ms (0.8×) | 79.32 ms (14×) | 214.7 ms (37×) | 56.91 ms (9.9×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 188 op/s | 2 183 op/s (99.8%) | 2 874 op/s (131%) | 225.4 op/s (10.3%) | 217.6 op/s (9.9%) | 271.7 op/s (12.4%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 685.4 op/s | 1 773 op/s (259%) | 798.1 op/s (116%) | 85.70 op/s (12.5%) | 91.68 op/s (13.4%) | 143.2 op/s (20.9%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 079 op/s | 1 279 op/s (119%) | 1 192 op/s (110%) | 142.3 op/s (13.2%) | 82.77 op/s (7.7%) | 130.5 op/s (12.1%) |
| Tree shfld. | 969.0 op/s | 1 122 op/s (116%) | 1 131 op/s (117%) | 145.9 op/s (15.1%) | 85.80 op/s (8.9%) | 126.8 op/s (13.1%) |
| DAG GNP sparse | 463.2 op/s | 509.7 op/s (110%) | 502.8 op/s (109%) | 50.19 op/s (10.8%) | 29.99 op/s (6.5%) | 70.60 op/s (15.2%) |
| DAG GNP dense | 197.2 op/s | 219.7 op/s (111%) | 264.8 op/s (134%) | 15.53 op/s (7.9%) | 4.760 op/s (2.4%) | 22.43 op/s (11.4%) |
| DAG GNP dense shfld. | 173.5 op/s | 222.4 op/s (128%) | 222.9 op/s (128%) | 12.61 op/s (7.3%) | 4.658 op/s (2.7%) | 17.57 op/s (10.1%) |

![1.7.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 29.09 ms | 25.49 ms (0.9×) |
| DAG GNP dense | 113.1 ms | 92.44 ms (0.8×) |
| DAG GNP dense shfld. | 190.3 ms | 92.20 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 34.38 op/s | 39.23 op/s (114%) |
| DAG GNP dense | 8.842 op/s | 10.82 op/s (122%) |
| DAG GNP dense shfld. | 5.255 op/s | 10.85 op/s (206%) |

![1.7.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 853.6 ms | 759.4 ms (0.9×) |
| DAG GNP dense | 2971.3 ms | 2553.0 ms (0.9×) |
| DAG GNP dense shfld. | 4386.9 ms | 2623.6 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 1.172 op/s | 1.317 op/s (112%) |
| DAG GNP dense | 0.337 op/s | 0.392 op/s (116%) |
| DAG GNP dense shfld. | 0.228 op/s | 0.381 op/s (167%) |

![1.7.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 11412 ms | 9419.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.088 op/s | 0.106 op/s (121%) |

![1.7.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-7-5-toporderandlevels-n-100-000-000.svg)

### 1.8 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.060 ms | 0.060 ms (1.0×) | 0.164 ms (2.7×) | 0.215 ms (3.6×) | 0.559 ms (9.3×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.065 ms | 0.050 ms (0.8×) | 0.366 ms (5.6×) | 0.518 ms (8.0×) | 0.435 ms (6.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.049 ms | 0.046 ms (0.9×) | 0.470 ms (9.6×) | 0.718 ms (15×) | 0.592 ms (12×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.164 ms | 0.087 ms (0.5×) | 0.616 ms (3.8×) | 1.366 ms (8.3×) | 0.794 ms (4.8×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.771 ms | 0.665 ms (0.9×) | 4.303 ms (5.6×) | 11.46 ms (15×) | 2.591 ms (3.4×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.780 ms | 0.652 ms (0.8×) | 4.904 ms (6.3×) | 13.09 ms (17×) | 2.611 ms (3.3×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.582 ms | 0.580 ms (1.0×) | 3.663 ms (6.3×) | 3.226 ms (5.5×) | 3.897 ms (6.7×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.642 ms | 0.634 ms (1.0×) | 4.491 ms (7.0×) | 4.839 ms (7.5×) | 2.420 ms (3.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 16 667 op/s | 16 667 op/s (100%) | 6 098 op/s (36.6%) | 4 651 op/s (27.9%) | 1 789 op/s (10.7%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 385 op/s | 20 000 op/s (130%) | 2 732 op/s (17.8%) | 1 931 op/s (12.5%) | 2 299 op/s (14.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 20 408 op/s | 21 739 op/s (107%) | 2 128 op/s (10.4%) | 1 393 op/s (6.8%) | 1 689 op/s (8.3%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 098 op/s | 11 494 op/s (189%) | 1 623 op/s (26.6%) | 732.1 op/s (12.0%) | 1 259 op/s (20.7%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 297 op/s | 1 504 op/s (116%) | 232.4 op/s (17.9%) | 87.26 op/s (6.7%) | 386.0 op/s (29.8%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 282 op/s | 1 534 op/s (120%) | 203.9 op/s (15.9%) | 76.37 op/s (6.0%) | 383.0 op/s (29.9%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 718 op/s | 1 724 op/s (100%) | 273.0 op/s (15.9%) | 310.0 op/s (18.0%) | 256.6 op/s (14.9%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 558 op/s | 1 577 op/s (101%) | 222.7 op/s (14.3%) | 206.7 op/s (13.3%) | 413.2 op/s (26.5%) |

![1.8.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.563 ms | 0.556 ms (1.0×) | 10.01 ms (18×) | 8.167 ms (15×) | 3.674 ms (6.5×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.063 ms | 0.513 ms (0.5×) | 19.39 ms (18×) | 15.92 ms (15×) | 5.028 ms (4.7×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.271 ms | 0.872 ms (0.7×) | 26.02 ms (20×) | 18.89 ms (15×) | 15.11 ms (12×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.086 ms | 1.890 ms (0.6×) | 43.81 ms (14×) | 46.46 ms (15×) | 11.30 ms (3.7×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.72 ms | 7.055 ms (0.6×) | 116.3 ms (9.1×) | 265.1 ms (21×) | 47.52 ms (3.7×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 12.71 ms | 7.081 ms (0.6×) | 128.6 ms (10×) | 218.1 ms (17×) | 46.05 ms (3.6×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 6.191 ms | 6.108 ms (1.0×) | 82.98 ms (13×) | 78.80 ms (13×) | 48.59 ms (7.8×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 11.15 ms | 6.298 ms (0.6×) | 134.2 ms (12×) | 89.69 ms (8.0×) | 39.95 ms (3.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 776 op/s | 1 799 op/s (101%) | 99.93 op/s (5.6%) | 122.4 op/s (6.9%) | 272.2 op/s (15.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 940.7 op/s | 1 949 op/s (207%) | 51.58 op/s (5.5%) | 62.80 op/s (6.7%) | 198.9 op/s (21.1%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 786.8 op/s | 1 147 op/s (146%) | 38.43 op/s (4.9%) | 52.93 op/s (6.7%) | 66.18 op/s (8.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 324.0 op/s | 529.1 op/s (163%) | 22.82 op/s (7.0%) | 21.52 op/s (6.6%) | 88.47 op/s (27.3%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 78.61 op/s | 141.7 op/s (180%) | 8.598 op/s (10.9%) | 3.772 op/s (4.8%) | 21.05 op/s (26.8%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 78.71 op/s | 141.2 op/s (179%) | 7.776 op/s (9.9%) | 4.585 op/s (5.8%) | 21.71 op/s (27.6%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 161.5 op/s | 163.7 op/s (101%) | 12.05 op/s (7.5%) | 12.69 op/s (7.9%) | 20.58 op/s (12.7%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 89.69 op/s | 158.8 op/s (177%) | 7.452 op/s (8.3%) | 11.15 op/s (12.4%) | 25.03 op/s (27.9%) |

![1.8.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 45.23 ms | 19.54 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 242.3 ms | 92.69 ms (0.4×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 240.9 ms | 90.99 ms (0.4×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 76.46 ms | 76.53 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 22.11 op/s | 51.17 op/s (231%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.127 op/s | 10.79 op/s (261%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.151 op/s | 10.99 op/s (265%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 13.08 op/s | 13.07 op/s (99.9%) |

![1.8.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1952.3 ms | 244.4 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3722.5 ms | 1223.7 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3874.8 ms | 1124.4 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 839.7 ms | 797.1 ms (0.9×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.512 op/s | 4.092 op/s (799%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.269 op/s | 0.817 op/s (304%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.258 op/s | 0.889 op/s (345%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.191 op/s | 1.255 op/s (105%) |

![1.8.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 26787 ms | 2799.7 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.037 op/s | 0.357 op/s (957%) |

![1.8.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-5-backedges-n-100-000-000.svg)

### 1.9 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.063 ms | 0.064 ms (1.0×) | 0.198 ms (3.1×) | 0.400 ms (6.3×) | 0.605 ms (9.6×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.077 ms | 0.060 ms (0.8×) | 0.389 ms (5.1×) | 0.572 ms (7.4×) | 0.484 ms (6.3×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.053 ms | 0.046 ms (0.9×) | 0.509 ms (9.6×) | 0.691 ms (13×) | 0.708 ms (13×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.186 ms | 0.094 ms (0.5×) | 0.698 ms (3.8×) | 1.425 ms (7.7×) | 0.888 ms (4.8×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.530 ms | 0.377 ms (0.7×) | 6.640 ms (13×) | 13.11 ms (25×) | 3.176 ms (6.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms | 0.380 ms (0.7×) | 6.303 ms (12×) | 14.73 ms (28×) | 3.125 ms (6.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.296 ms | 0.297 ms (1.0×) | 4.846 ms (16×) | 4.394 ms (15×) | 4.675 ms (16×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.394 ms | 0.297 ms (0.8×) | 6.184 ms (16×) | 5.946 ms (15×) | 3.070 ms (7.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 15 873 op/s | 15 625 op/s (98.4%) | 5 051 op/s (31.8%) | 2 500 op/s (15.8%) | 1 653 op/s (10.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 12 987 op/s | 16 667 op/s (128%) | 2 571 op/s (19.8%) | 1 748 op/s (13.5%) | 2 066 op/s (15.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s | 21 739 op/s (115%) | 1 965 op/s (10.4%) | 1 447 op/s (7.7%) | 1 412 op/s (7.5%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 5 376 op/s | 10 638 op/s (198%) | 1 433 op/s (26.6%) | 701.8 op/s (13.1%) | 1 126 op/s (20.9%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 887 op/s | 2 653 op/s (141%) | 150.6 op/s (8.0%) | 76.31 op/s (4.0%) | 314.9 op/s (16.7%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s | 2 632 op/s (138%) | 158.7 op/s (8.3%) | 67.87 op/s (3.6%) | 320.0 op/s (16.8%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3 378 op/s | 3 367 op/s (99.7%) | 206.4 op/s (6.1%) | 227.6 op/s (6.7%) | 213.9 op/s (6.3%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 538 op/s | 3 367 op/s (133%) | 161.7 op/s (6.4%) | 168.2 op/s (6.6%) | 325.7 op/s (12.8%) |

![1.9.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.607 ms | 0.592 ms (1.0×) | 5.985 ms (9.9×) | 9.766 ms (16×) | 4.835 ms (8.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.408 ms | 0.629 ms (0.4×) | 23.05 ms (16×) | 21.18 ms (15×) | 6.128 ms (4.4×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.480 ms | 1.066 ms (0.7×) | 23.79 ms (16×) | 24.16 ms (16×) | 17.95 ms (12×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.587 ms | 1.853 ms (0.5×) | 48.11 ms (13×) | 51.80 ms (14×) | 13.79 ms (3.8×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.926 ms | 4.197 ms (0.6×) | 148.7 ms (21×) | 252.4 ms (36×) | 59.49 ms (8.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.963 ms | 4.193 ms (0.6×) | 162.5 ms (23×) | 222.4 ms (32×) | 58.52 ms (8.4×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.706 ms | 2.689 ms (1.0×) | 101.2 ms (37×) | 90.68 ms (34×) | 57.05 ms (21×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.599 ms | 2.982 ms (0.5×) | 169.3 ms (30×) | 108.8 ms (19×) | 53.52 ms (9.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 647 op/s | 1 689 op/s (103%) | 167.1 op/s (10.1%) | 102.4 op/s (6.2%) | 206.8 op/s (12.6%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 710.2 op/s | 1 590 op/s (224%) | 43.37 op/s (6.1%) | 47.21 op/s (6.6%) | 163.2 op/s (23.0%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 675.7 op/s | 938.1 op/s (139%) | 42.03 op/s (6.2%) | 41.38 op/s (6.1%) | 55.70 op/s (8.2%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 278.8 op/s | 539.7 op/s (194%) | 20.79 op/s (7.5%) | 19.31 op/s (6.9%) | 72.51 op/s (26.0%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 144.4 op/s | 238.3 op/s (165%) | 6.725 op/s (4.7%) | 3.962 op/s (2.7%) | 16.81 op/s (11.6%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 143.6 op/s | 238.5 op/s (166%) | 6.154 op/s (4.3%) | 4.496 op/s (3.1%) | 17.09 op/s (11.9%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 369.5 op/s | 371.9 op/s (101%) | 9.881 op/s (2.7%) | 11.03 op/s (3.0%) | 17.53 op/s (4.7%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 178.6 op/s | 335.3 op/s (188%) | 5.907 op/s (3.3%) | 9.191 op/s (5.1%) | 18.69 op/s (10.5%) |

![1.9.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 58.24 ms | 20.58 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 238.8 ms | 60.05 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 235.5 ms | 60.09 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 26.59 ms | 26.68 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 17.17 op/s | 48.59 op/s (283%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.188 op/s | 16.65 op/s (398%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.246 op/s | 16.64 op/s (392%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 37.62 op/s | 37.48 op/s (99.6%) |

![1.9.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2492.1 ms | 414.7 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4552.4 ms | 1515.5 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4492.7 ms | 1393.9 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 281.8 ms | 275.9 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.401 op/s | 2.411 op/s (601%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.220 op/s | 0.660 op/s (300%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.223 op/s | 0.717 op/s (322%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.549 op/s | 3.625 op/s (102%) |

![1.9.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 31282 ms | 6992.4 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.032 op/s | 0.143 op/s (447%) |

![1.9.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-5-edgetypes-n-100-000-000.svg)

### 1.10 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.110 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.133 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.087 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.386 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.821 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.812 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.532 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.612 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 091 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 519 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 11 494 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 591 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 218 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 232 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 880 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 634 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.033 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.929 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.215 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.411 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 10.81 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 10.87 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.374 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 8.836 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 968.1 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 518.4 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 451.5 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 184.8 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 92.53 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 92.00 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 186.1 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 113.2 op/s |

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 79.17 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 317.2 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 315.0 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 52.67 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 12.63 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 3.153 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 3.175 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 18.99 op/s |

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3263.5 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 7509.7 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 7613.8 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 617.1 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.306 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.133 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.131 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.620 op/s |

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 41091 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.024 op/s |

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

### 1.12 Symmetrize (WebGraph) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 4.936 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7.796 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 8.144 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 18.58 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 112.6 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 96.63 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 14.04 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 67.68 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 202.6 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 128.3 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 122.8 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 53.83 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 8.881 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 10.35 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 71.22 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 14.78 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 33.78 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 63.42 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 70.60 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 184.2 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1128.9 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 1102.6 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 122.9 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 687.4 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 29.60 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 15.77 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 14.16 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.429 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 0.886 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 0.907 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.137 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1.455 op/s |

## 2 Memory Footprint

Retained heap in bytes for a fully built graph. Lower is better. Baseline (= 100%): **Graphity 2.1.0**, or the newest version present in a table it is missing from. Two charts per section: *Graphity & WebGraph* (same league) and *All libraries*.

### 2.1 Total Graph Size

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5037%) | 2.7 MB (3375%) | 18.8 kB (23.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5037%) | 2.7 MB (3375%) | 37.8 kB (47.2%) |
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

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
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

Bytes allocated per algorithm invocation (JMH `-prof gc`). Lower is better. Baseline (= 100%): **Graphity 2.1.0**, or the newest version present in a table it is missing from.

### 3.1 Build

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
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

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 54.4 MB (1447%) | 28.5 MB (757%) | 14.2 MB (378%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 54.3 MB (1443%) | 28.5 MB (757%) | 14.5 MB (385%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 55.6 MB (1479%) | 28.8 MB (766%) | 11.4 MB (302%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB | 129 MB (1215%) | 52.3 MB (492%) | 15.3 MB (144%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB | 1094 MB (983%) | 376 MB (338%) | 69.8 MB (62.8%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 111 MB | 1094 MB (983%) | 376 MB (338%) | 69.8 MB (62.7%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB | 1139 MB (1020%) | 294 MB (264%) | 60.2 MB (53.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB | 1139 MB (1020%) | 294 MB (264%) | 66.3 MB (59.4%) |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 534 kB | 5.2 MB (975%) | 4.0 MB (758%) | 1.4 MB (269%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 534 kB | 5.2 MB (966%) | 4.0 MB (747%) | 1.3 MB (247%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 288 kB | 5.0 MB (1746%) | 3.7 MB (1299%) | 908 kB (315%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 371 kB | 3.2 MB (854%) | 4.0 MB (1091%) | 1.3 MB (346%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 498 kB | 6.3 MB (1276%) | 5.2 MB (1044%) | 1.5 MB (295%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 497 kB | 6.3 MB (1277%) | 5.2 MB (1044%) | 1.5 MB (294%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 495 kB | 4.8 MB (964%) | 8.5 MB (1720%) | 6.1 MB (1237%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 364 kB | 6.0 MB (1634%) | 8.7 MB (2378%) | 1.3 MB (348%) |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 4.9 MB | 51.6 MB (1056%) | 28.9 MB (591%) | 13.3 MB (272%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4.9 MB | 51.7 MB (1058%) | 41.8 MB (855%) | 13.5 MB (275%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB | 50.3 MB (1792%) | 25.5 MB (907%) | 8.8 MB (314%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.5 MB | 32.1 MB (916%) | 42.9 MB (1222%) | 12.3 MB (351%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4.5 MB | 70.9 MB (1570%) | 45.2 MB (1002%) | 13.5 MB (298%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 4.5 MB | 71.4 MB (1582%) | 53.3 MB (1181%) | 13.5 MB (298%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.5 MB | 48.5 MB (1078%) | 76.8 MB (1706%) | 60.3 MB (1339%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB | 61.4 MB (1777%) | 75.3 MB (2181%) | 12.3 MB (356%) |

![3.2.2 · SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-2-2-scc-n-100-000.svg)

### 3.3 WCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB | 3.0 MB (1516%) | 3.0 MB (1516%) | 2.0 MB (989%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB | 2.3 MB (1156%) | 3.0 MB (1516%) | 1.9 MB (929%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB | 3.3 MB (1665%) | 2.4 MB (1196%) | 1.2 MB (611%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB | 3.1 MB (1551%) | 3.5 MB (1754%) | 1.2 MB (600%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 10.3 MB (5169%) | 9.3 MB (4668%) | 1.3 MB (650%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 10.3 MB (5169%) | 9.3 MB (4668%) | 1.3 MB (641%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB | 9.4 MB (4720%) | 9.6 MB (4803%) | 6.1 MB (3071%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB | 9.4 MB (4720%) | 9.6 MB (4803%) | 1.3 MB (642%) |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 32.7 MB (1635%) | 26.3 MB (1315%) | 19.3 MB (964%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 32.7 MB (1635%) | 26.3 MB (1315%) | 16.5 MB (824%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 33.2 MB (1661%) | 26.3 MB (1315%) | 11.1 MB (557%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB | 37.5 MB (1875%) | 37.5 MB (1873%) | 11.0 MB (552%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 106 MB (5304%) | 95.9 MB (4794%) | 11.7 MB (587%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 106 MB (5304%) | 95.9 MB (4794%) | 11.8 MB (589%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB | 101 MB (5037%) | 98.4 MB (4918%) | 61.4 MB (3071%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB | 96.7 MB (4837%) | 105 MB (5238%) | 12.5 MB (623%) |

![3.3.2 · WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-3-2-wcc-n-100-000.svg)

### 3.4 CCs

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 |
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

| Shape | Graphity 2.1.0 |
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

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 23.2 kB |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 96 B |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 760 B |
| DAG GNP sparse<br><sub>1k nodes</sub> | 66.4 kB |

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 250 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 97 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 7.2 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 541 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.1 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 107 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 75.7 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 5.9 MB |

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 297 kB | 1.3 MB (422%) | 943 kB (318%) | 1.5 MB (494%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 297 kB | 1.3 MB (422%) | 2.4 MB (803%) | 1.0 MB (345%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 50.8 kB | 1.3 MB (2507%) | 813 kB (1600%) | 885 kB (1742%) |
| Tree shfld.<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2507%) | 956 kB (1880%) | 845 kB (1663%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2507%) | 2.3 MB (4430%) | 984 kB (1936%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2468%) | 812 kB (1597%) | 1.1 MB (2162%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2468%) | 812 kB (1597%) | 1.1 MB (2119%) |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.5 MB | 13.3 MB (529%) | 24.3 MB (968%) | 10.5 MB (417%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.5 MB | 13.3 MB (529%) | 23.3 MB (927%) | 10.0 MB (400%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 433 kB | 13.5 MB (3108%) | 22.2 MB (5134%) | 8.5 MB (1953%) |
| Tree shfld.<br><sub>100k nodes</sub> | 433 kB | 13.5 MB (3108%) | 22.2 MB (5134%) | 8.4 MB (1945%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 433 kB | 13.6 MB (3128%) | 7.8 MB (1811%) | 9.5 MB (2203%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 433 kB | 13.3 MB (3074%) | 7.9 MB (1812%) | 10.3 MB (2375%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 433 kB | 13.3 MB (3074%) | 7.9 MB (1812%) | 10.2 MB (2361%) |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.7 MB (1088%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.7 MB (1070%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.3 MB (828%) |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.3 MB (796%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 1.9 MB (1196%) | 1.6 MB (1016%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB | 160 kB (100.0%) | 476 kB (297%) | 476 kB (297%) | 1.8 MB (1126%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB | 160 kB (100.0%) | 476 kB (297%) | 1.9 MB (1196%) | 1.8 MB (1140%) |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | Graphity 2.1.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 19.2 MB (1200%) | 16.9 MB (1054%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 19.2 MB (1200%) | 16.8 MB (1052%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 12.5 MB (780%) |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 19.2 MB (1200%) | 12.4 MB (777%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 19.2 MB (1200%) | 15.5 MB (967%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 17.1 MB (1070%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 17.1 MB (1069%) |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 257 kB | 161 kB (62.6%) | 161 kB (62.7%) | 1.4 MB (555%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 257 kB | 161 kB (62.6%) | 1.6 MB (624%) | 985 kB (384%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10.9 kB | 161 kB (1476%) | 1.6 MB (14712%) | 845 kB (7768%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 351 kB | 386 kB (110%) | 1.8 MB (520%) | 1.2 MB (336%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4.1 MB | 4.0 MB (97.7%) | 4.0 MB (97.7%) | 4.9 MB (120%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 4.1 MB | 4.0 MB (97.7%) | 5.4 MB (133%) | 4.8 MB (118%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4.1 MB | 4.0 MB (97.7%) | 5.4 MB (133%) | 9.7 MB (236%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4.0 MB | 4.0 MB (101%) | 5.4 MB (137%) | 4.9 MB (124%) |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.1 MB | 1.6 MB (76.8%) | 16.0 MB (758%) | 10.1 MB (477%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.1 MB | 1.6 MB (76.8%) | 16.0 MB (758%) | 9.6 MB (456%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 33.4 kB | 1.6 MB (4861%) | 16.0 MB (47985%) | 8.1 MB (24142%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.6 MB | 4.2 MB (115%) | 18.6 MB (512%) | 12.1 MB (332%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 45.9 MB | 45.4 MB (98.9%) | 53.9 MB (117%) | 53.6 MB (117%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 45.9 MB | 45.4 MB (98.9%) | 59.8 MB (130%) | 53.6 MB (117%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 45.9 MB | 45.4 MB (98.9%) | 59.8 MB (130%) | 101 MB (219%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 44.8 MB | 45.4 MB (101%) | 59.8 MB (133%) | 53.5 MB (119%) |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 307 kB | 581 kB (190%) | 581 kB (190%) | 1.8 MB (602%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 307 kB | 581 kB (190%) | 2.0 MB (659%) | 1.4 MB (458%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 60.8 kB | 581 kB (956%) | 583 kB (958%) | 1.3 MB (2081%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 196 kB | 1.1 MB (540%) | 2.5 MB (1276%) | 1.8 MB (945%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 596 kB | 9.9 MB (1665%) | 11.4 MB (1906%) | 10.8 MB (1818%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 596 kB | 9.9 MB (1665%) | 11.4 MB (1906%) | 10.8 MB (1806%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 607 kB | 9.9 MB (1634%) | 11.4 MB (1871%) | 15.6 MB (2565%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 476 kB | 9.9 MB (2084%) | 11.4 MB (2386%) | 10.8 MB (2275%) |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.1.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.6 MB | 4.9 MB (188%) | 19.3 MB (738%) | 13.4 MB (511%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.6 MB | 4.9 MB (188%) | 19.3 MB (738%) | 12.9 MB (495%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 533 kB | 4.9 MB (920%) | 19.3 MB (3620%) | 11.3 MB (2127%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.8 MB | 11.7 MB (665%) | 26.1 MB (1481%) | 19.6 MB (1112%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 5.5 MB | 113 MB (2046%) | 121 MB (2191%) | 121 MB (2194%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 5.5 MB | 113 MB (2046%) | 127 MB (2307%) | 121 MB (2195%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.6 MB | 113 MB (2008%) | 127 MB (2264%) | 168 MB (2990%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4.6 MB | 113 MB (2469%) | 117 MB (2556%) | 121 MB (2646%) |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-9-2-edgetypes-n-100-000.svg)
