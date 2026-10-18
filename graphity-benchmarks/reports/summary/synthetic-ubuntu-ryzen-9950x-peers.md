# Graphity vs. JGraphT, Guava and WebGraph — synthetic / ubuntu-ryzen-9950x

Graphity `2.4.0` against the peer libraries. Peer numbers come from the newest run that measured them, which need not be the run of the newest Graphity version; every column states the version it belongs to. Blocks without a peer column are Graphity-only benchmarks or graph sizes beyond the peers' reach — for the comparison across Graphity versions see [`synthetic-ubuntu-ryzen-9950x-history.md`](synthetic-ubuntu-ryzen-9950x-history.md).

```
Profile:      synthetic
Timestamp:    2026-10-17 01:17:42
JDK:          OpenJDK 64-Bit Server VM 25.0.2 (Amazon.com Inc.)
OS:           Ubuntu 25.10, kernel 6.17.0-41-generic (amd64)
CPU:          AMD Ryzen 9 9950X 16-Core Processor (32 logical cores)
RAM:          89.7 GiB
Estimate:     near-best window - 3rd-fastest of 15 or more, 2nd-fastest of 5 or more
Revisions:    2, the report combines runs of different states
              270157fce154 "Changelog and Javadoc: current issue titles, SSA and phi explained at first use" (647 cases)
              303d801d508b + uncommitted changes b4b1cd1fe72e "Prepare 2.3.0 release" (602 cases)
Memory:       revision not recorded per row
```

## Contents

- [1 Performance](#1-performance)
  - [1.1 Build](#11-build)
  - [1.2 Out-adjacency traversal (outNodes, no algorithm)](#12-out-adjacency-traversal-outnodes-no-algorithm)
  - [1.3 In-adjacency traversal (inNodes, no algorithm)](#13-in-adjacency-traversal-innodes-no-algorithm)
  - [1.4 SCC](#14-scc)
  - [1.5 WCC](#15-wcc)
  - [1.6 CCs](#16-ccs)
  - [1.7 Condense](#17-condense)
  - [1.8 TopOrder](#18-toporder)
  - [1.9 TopOrderAndLevels](#19-toporderandlevels)
  - [1.10 BackEdges](#110-backedges)
  - [1.11 EdgeTypes](#111-edgetypes)
  - [1.12 Reorder (DFS-preorder) cost](#112-reorder-dfs-preorder-cost)
  - [1.13 BiAdjacency (in-adjacency) cost](#113-biadjacency-in-adjacency-cost)
  - [1.14 Symmetrize (WebGraph) cost](#114-symmetrize-webgraph-cost)
  - [1.15 Dominators](#115-dominators)
  - [1.16 Dominance frontiers](#116-dominance-frontiers)
  - [1.17 Iterated dominance frontier (build + one query per variable)](#117-iterated-dominance-frontier-build-one-query-per-variable)
  - [1.18 Loops](#118-loops)
  - [1.19 Ancestry of the dominator tree](#119-ancestry-of-the-dominator-tree)
  - [1.20 PreorderAncestry of the dominator tree](#120-preorderancestry-of-the-dominator-tree)
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

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.4.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 Build

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.083 ms | 1.111 ms (13×) | 0.372 ms (4.5×) | 2.260 ms (27×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.084 ms | 1.115 ms (13×) | 0.555 ms (6.6×) | 2.936 ms (35×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.093 ms | 1.153 ms (12×) | 0.526 ms (5.7×) | 2.559 ms (28×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.171 ms | 4.088 ms (24×) | 2.145 ms (13×) | 7.956 ms (47×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.569 ms | 55.01 ms (35×) | 31.20 ms (20×) | 43.27 ms (28×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.600 ms | 40.13 ms (25×) | 9.044 ms (5.7×) | 6.600 ms (4.1×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.606 ms | 57.04 ms (36×) | 17.65 ms (11×) | 43.73 ms (27×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s | 900.1 op/s (7.5%) | 2 688 op/s (22.3%) | 442.5 op/s (3.7%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 11 905 op/s | 896.9 op/s (7.5%) | 1 802 op/s (15.1%) | 340.6 op/s (2.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 753 op/s | 867.3 op/s (8.1%) | 1 901 op/s (17.7%) | 390.8 op/s (3.6%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 5 848 op/s | 244.6 op/s (4.2%) | 466.2 op/s (8.0%) | 125.7 op/s (2.1%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 637.3 op/s | 18.18 op/s (2.9%) | 32.06 op/s (5.0%) | 23.11 op/s (3.6%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 625.0 op/s | 24.92 op/s (4.0%) | 110.6 op/s (17.7%) | 151.5 op/s (24.2%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 622.7 op/s | 17.53 op/s (2.8%) | 56.66 op/s (9.1%) | 22.87 op/s (3.7%) |

![1.1.1 Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-1-1-build-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.693 ms | 14.04 ms (20×) | 4.043 ms (5.8×) | 21.64 ms (31×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.695 ms | 23.03 ms (33×) | 8.956 ms (13×) | 29.21 ms (42×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.101 ms | 25.14 ms (23×) | 8.092 ms (7.3×) | 28.09 ms (26×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.278 ms | 95.96 ms (42×) | 47.37 ms (21×) | 85.02 ms (37×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.57 ms | 1498.0 ms (85×) | 965.5 ms (55×) | 542.8 ms (31×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 17.14 ms | 818.9 ms (48×) | 93.49 ms (5.5×) | 63.61 ms (3.7×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 17.10 ms | 1482.1 ms (87×) | 612.9 ms (36×) | 508.0 ms (30×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 443 op/s | 71.21 op/s (4.9%) | 247.3 op/s (17.1%) | 46.21 op/s (3.2%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 439 op/s | 43.42 op/s (3.0%) | 111.7 op/s (7.8%) | 34.23 op/s (2.4%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 908.3 op/s | 39.77 op/s (4.4%) | 123.6 op/s (13.6%) | 35.60 op/s (3.9%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 439.0 op/s | 10.42 op/s (2.4%) | 21.11 op/s (4.8%) | 11.76 op/s (2.7%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 56.93 op/s | 0.668 op/s (1.2%) | 1.036 op/s (1.8%) | 1.842 op/s (3.2%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 58.35 op/s | 1.221 op/s (2.1%) | 10.70 op/s (18.3%) | 15.72 op/s (26.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 58.48 op/s | 0.675 op/s (1.2%) | 1.632 op/s (2.8%) | 1.969 op/s (3.4%) |

![1.1.2 Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-1-2-build-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 27.59 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 169.7 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 166.0 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 36.24 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 5.893 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 6.024 op/s |

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 242.5 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 1739.0 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 2061.3 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4.124 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.575 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 0.485 op/s |

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 2457.0 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.407 op/s |

### 1.2 Out-adjacency traversal (outNodes, no algorithm)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.010 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.017 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.047 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.051 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.053 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 100 000 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 58 824 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 21 277 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 19 608 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 18 868 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.170 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.169 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.405 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.653 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.130 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 0.518 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 0.518 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 882 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 5 917 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2 469 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 531 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 885.0 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1 931 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1 931 op/s |

### 1.3 In-adjacency traversal (inNodes, no algorithm)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.017 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.050 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.054 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.056 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 58 824 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 20 000 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 18 519 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 17 857 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.171 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.171 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.171 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.662 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.186 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 0.541 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 0.541 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 848 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 5 848 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 5 848 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 511 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 843.2 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1 848 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1 848 op/s |

### 1.4 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.109 ms | 0.110 ms (1.0×) | 0.763 ms (7.0×) | 0.674 ms (6.2×) | 0.347 ms (3.2×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.138 ms | 0.111 ms (0.8×) | 1.263 ms (9.2×) | 1.194 ms (8.7×) | 0.492 ms (3.6×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.112 ms | 0.101 ms (0.9×) | 1.136 ms (10×) | 1.085 ms (9.7×) | 0.481 ms (4.3×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.241 ms | 0.121 ms (0.5×) | 2.189 ms (9.1×) | 2.241 ms (9.3×) | 0.840 ms (3.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.559 ms | 0.415 ms (0.7×) | 20.54 ms (37×) | 19.72 ms (35×) | 2.357 ms (4.2×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.310 ms | 0.302 ms (1.0×) | 8.923 ms (29×) | 4.842 ms (16×) | 3.357 ms (11×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.401 ms | 0.302 ms (0.8×) | 13.57 ms (34×) | 7.107 ms (18×) | 2.084 ms (5.2×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 174 op/s | 9 091 op/s (99.1%) | 1 311 op/s (14.3%) | 1 484 op/s (16.2%) | 2 882 op/s (31.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 246 op/s | 9 009 op/s (124%) | 791.8 op/s (10.9%) | 837.5 op/s (11.6%) | 2 033 op/s (28.0%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 8 929 op/s | 9 901 op/s (111%) | 880.3 op/s (9.9%) | 921.7 op/s (10.3%) | 2 079 op/s (23.3%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 149 op/s | 8 264 op/s (199%) | 456.8 op/s (11.0%) | 446.2 op/s (10.8%) | 1 190 op/s (28.7%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 789 op/s | 2 410 op/s (135%) | 48.69 op/s (2.7%) | 50.71 op/s (2.8%) | 424.3 op/s (23.7%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3 226 op/s | 3 311 op/s (103%) | 112.1 op/s (3.5%) | 206.5 op/s (6.4%) | 297.9 op/s (9.2%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 494 op/s | 3 311 op/s (133%) | 73.66 op/s (3.0%) | 140.7 op/s (5.6%) | 479.8 op/s (19.2%) |

![1.4.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.158 ms | 1.060 ms (0.9×) | 15.26 ms (13×) | 21.46 ms (19×) | 3.401 ms (2.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.451 ms | 1.091 ms (0.4×) | 46.48 ms (19×) | 45.40 ms (19×) | 5.508 ms (2.2×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.161 ms | 1.354 ms (0.6×) | 41.51 ms (19×) | 38.09 ms (18×) | 5.712 ms (2.6×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.389 ms | 2.246 ms (0.5×) | 109.6 ms (25×) | 87.01 ms (20×) | 10.30 ms (2.3×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.265 ms | 4.946 ms (0.5×) | 446.5 ms (48×) | 333.1 ms (36×) | 36.12 ms (3.9×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 3.058 ms | 2.936 ms (1.0×) | 166.0 ms (54×) | 108.0 ms (35×) | 37.02 ms (12×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.351 ms | 3.040 ms (0.5×) | 379.0 ms (60×) | 175.2 ms (28×) | 31.00 ms (4.9×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 863.6 op/s | 943.4 op/s (109%) | 65.51 op/s (7.6%) | 46.60 op/s (5.4%) | 294.0 op/s (34.0%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 408.0 op/s | 916.6 op/s (225%) | 21.51 op/s (5.3%) | 22.03 op/s (5.4%) | 181.6 op/s (44.5%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 462.7 op/s | 738.6 op/s (160%) | 24.09 op/s (5.2%) | 26.26 op/s (5.7%) | 175.1 op/s (37.8%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 227.8 op/s | 445.2 op/s (195%) | 9.124 op/s (4.0%) | 11.49 op/s (5.0%) | 97.10 op/s (42.6%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 107.9 op/s | 202.2 op/s (187%) | 2.240 op/s (2.1%) | 3.002 op/s (2.8%) | 27.69 op/s (25.7%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 327.0 op/s | 340.6 op/s (104%) | 6.024 op/s (1.8%) | 9.259 op/s (2.8%) | 27.01 op/s (8.3%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 157.5 op/s | 328.9 op/s (209%) | 2.639 op/s (1.7%) | 5.708 op/s (3.6%) | 32.25 op/s (20.5%) |

![1.4.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 117.5 ms | 31.28 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 267.9 ms | 70.19 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 30.70 ms | 31.18 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 8.511 op/s | 31.97 op/s (376%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 3.733 op/s | 14.25 op/s (382%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 32.58 op/s | 32.07 op/s (98.5%) |

![1.4.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3155.9 ms | 773.9 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6771.3 ms | 3192.5 ms (0.5×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 318.4 ms | 322.3 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.317 op/s | 1.292 op/s (408%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.148 op/s | 0.313 op/s (212%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.141 op/s | 3.103 op/s (98.8%) |

![1.4.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 37032 ms | 10158 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.027 op/s | 0.098 op/s (365%) |

![1.4.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-4-5-scc-n-100-000-000.svg)

### 1.5 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.038 ms | 0.037 ms (1.0×) | 0.839 ms (22×) | 0.720 ms (19×) | 0.864 ms (23×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.047 ms | 0.042 ms (0.9×) | 1.062 ms (23×) | 1.235 ms (26×) | 1.017 ms (22×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.044 ms | 0.040 ms (0.9×) | 1.246 ms (28×) | 0.941 ms (21×) | 0.687 ms (16×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.087 ms | 0.083 ms (1.0×) | 2.088 ms (24×) | 2.473 ms (28×) | 1.066 ms (12×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.516 ms | 0.515 ms (1.0×) | 14.50 ms (28×) | 22.65 ms (44×) | 4.431 ms (8.6×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.572 ms | 0.573 ms (1.0×) | 8.927 ms (16×) | 9.144 ms (16×) | 4.730 ms (8.3×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.632 ms | 0.610 ms (1.0×) | 9.382 ms (15×) | 13.34 ms (21×) | 2.438 ms (3.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s | 27 027 op/s (103%) | 1 192 op/s (4.5%) | 1 389 op/s (5.3%) | 1 157 op/s (4.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 277 op/s | 23 810 op/s (112%) | 941.6 op/s (4.4%) | 809.7 op/s (3.8%) | 983.3 op/s (4.6%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 22 727 op/s | 25 000 op/s (110%) | 802.6 op/s (3.5%) | 1 063 op/s (4.7%) | 1 456 op/s (6.4%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 11 494 op/s | 12 048 op/s (105%) | 478.9 op/s (4.2%) | 404.4 op/s (3.5%) | 938.1 op/s (8.2%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 938 op/s | 1 942 op/s (100%) | 68.96 op/s (3.6%) | 44.15 op/s (2.3%) | 225.7 op/s (11.6%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 748 op/s | 1 745 op/s (99.8%) | 112.0 op/s (6.4%) | 109.4 op/s (6.3%) | 211.4 op/s (12.1%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 582 op/s | 1 639 op/s (104%) | 106.6 op/s (6.7%) | 74.94 op/s (4.7%) | 410.2 op/s (25.9%) |

![1.5.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-5-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.406 ms | 0.405 ms (1.0×) | 14.51 ms (36×) | 11.36 ms (28×) | 8.595 ms (21×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.233 ms | 0.401 ms (0.3×) | 41.33 ms (34×) | 26.05 ms (21×) | 10.75 ms (8.7×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.677 ms | 0.733 ms (1.1×) | 44.03 ms (65×) | 20.77 ms (31×) | 8.299 ms (12×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.767 ms | 1.566 ms (0.9×) | 84.45 ms (48×) | 54.94 ms (31×) | 14.68 ms (8.3×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.139 ms | 6.131 ms (1.0×) | 515.0 ms (84×) | 338.6 ms (55×) | 48.08 ms (7.8×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.724 ms | 5.744 ms (1.0×) | 121.2 ms (21×) | 110.0 ms (19×) | 49.67 ms (8.7×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.045 ms | 6.146 ms (1.0×) | 220.5 ms (36×) | 298.2 ms (49×) | 31.10 ms (5.1×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 463 op/s | 2 469 op/s (100%) | 68.94 op/s (2.8%) | 88.04 op/s (3.6%) | 116.3 op/s (4.7%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 811.0 op/s | 2 494 op/s (307%) | 24.19 op/s (3.0%) | 38.39 op/s (4.7%) | 93.02 op/s (11.5%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 477 op/s | 1 364 op/s (92.4%) | 22.71 op/s (1.5%) | 48.15 op/s (3.3%) | 120.5 op/s (8.2%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 565.9 op/s | 638.6 op/s (113%) | 11.84 op/s (2.1%) | 18.20 op/s (3.2%) | 68.13 op/s (12.0%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 162.9 op/s | 163.1 op/s (100%) | 1.942 op/s (1.2%) | 2.953 op/s (1.8%) | 20.80 op/s (12.8%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 174.7 op/s | 174.1 op/s (99.7%) | 8.251 op/s (4.7%) | 9.091 op/s (5.2%) | 20.13 op/s (11.5%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 165.4 op/s | 162.7 op/s (98.4%) | 4.535 op/s (2.7%) | 3.353 op/s (2.0%) | 32.15 op/s (19.4%) |

![1.5.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-5-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.28 ms | 19.21 ms (0.9×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 67.51 ms | 67.42 ms (1.0×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 49.98 ms | 50.46 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.99 op/s | 52.06 op/s (111%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 14.81 op/s | 14.83 op/s (100%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 20.01 op/s | 19.82 op/s (99.1%) |

![1.5.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-5-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 606.7 ms | 414.5 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3273.0 ms | 3301.6 ms (1.0×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 520.9 ms | 522.3 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.648 op/s | 2.413 op/s (146%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.306 op/s | 0.303 op/s (99.1%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.920 op/s | 1.915 op/s (99.7%) |

![1.5.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-5-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8761.4 ms | 7238.6 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.114 op/s | 0.138 op/s (121%) |

![1.5.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-5-5-wcc-n-100-000-000.svg)

### 1.6 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms | 0.038 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.047 ms | 0.043 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.039 ms | 0.040 ms (1.0×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.085 ms | 0.092 ms (1.1×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.517 ms | 0.516 ms (1.0×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.572 ms | 0.573 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.633 ms | 0.615 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s | 26 316 op/s (97.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 277 op/s | 23 256 op/s (109%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s | 25 000 op/s (97.5%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 11 765 op/s | 10 870 op/s (92.4%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 934 op/s | 1 938 op/s (100%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 748 op/s | 1 745 op/s (99.8%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 580 op/s | 1 626 op/s (103%) |

![1.6.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.405 ms | 0.370 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.230 ms | 0.401 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.648 ms | 0.737 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.783 ms | 1.536 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.197 ms | 6.198 ms (1.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.753 ms | 5.732 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.050 ms | 5.782 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 469 op/s | 2 703 op/s (109%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.0 op/s | 2 494 op/s (307%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 543 op/s | 1 357 op/s (87.9%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 560.9 op/s | 651.0 op/s (116%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 161.4 op/s | 161.3 op/s (100.0%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 173.8 op/s | 174.5 op/s (100%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 165.3 op/s | 173.0 op/s (105%) |

![1.6.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.33 ms | 17.79 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 81.85 ms | 66.53 ms (0.8×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.09 ms | 49.68 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.87 op/s | 56.21 op/s (120%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 12.22 op/s | 15.03 op/s (123%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.96 op/s | 20.13 op/s (101%) |

![1.6.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 606.4 ms | 414.4 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3338.5 ms | 3229.5 ms (1.0×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 522.7 ms | 521.5 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.649 op/s | 2.413 op/s (146%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.300 op/s | 0.310 op/s (103%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.913 op/s | 1.918 op/s (100%) |

![1.6.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8837.8 ms | 7216.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.113 op/s | 0.139 op/s (122%) |

![1.6.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-6-5-ccs-n-100-000-000.svg)

### 1.7 Condense

#### Times @ 1k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 0.010 ms |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 0.007 ms |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 0.022 ms |
| DAG GNP sparse | 0.017 ms |

#### Throughput @ 1k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 100 000 op/s |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 142 857 op/s |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 45 455 op/s |
| DAG GNP sparse | 58 824 op/s |

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.081 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.098 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.201 ms |
| DAG GNP sparse | 0.261 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 346 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 10 204 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4 975 op/s |
| DAG GNP sparse | 3 831 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.758 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.642 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.038 ms |
| DAG GNP sparse | 3.603 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 319 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 609.0 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 490.7 op/s |
| DAG GNP sparse | 277.5 op/s |

### 1.8 TopOrder

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.062 ms | 0.054 ms (0.9×) | 0.436 ms (7.0×) | 0.406 ms (6.5×) | 0.536 ms (8.6×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.066 ms | 0.055 ms (0.8×) | 0.679 ms (10×) | 0.729 ms (11×) | 0.420 ms (6.4×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.058 ms | 0.051 ms (0.9×) | 0.698 ms (12×) | 0.669 ms (12×) | 0.600 ms (10×) |
| Tree shfld. | 0.053 ms | 0.048 ms (0.9×) | 0.714 ms (13×) | 0.674 ms (13×) | 0.504 ms (9.5×) |
| DAG GNP sparse | 0.077 ms | 0.067 ms (0.9×) | 1.152 ms (15×) | 1.316 ms (17×) | 0.829 ms (11×) |
| DAG GNP dense | 0.336 ms | 0.256 ms (0.8×) | 16.46 ms (49×) | 9.801 ms (29×) | 1.985 ms (5.9×) |
| DAG GNP dense shfld. | 0.378 ms | 0.267 ms (0.7×) | 17.71 ms (47×) | 10.05 ms (27×) | 2.258 ms (6.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 16 129 op/s | 18 519 op/s (115%) | 2 294 op/s (14.2%) | 2 463 op/s (15.3%) | 1 866 op/s (11.6%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 152 op/s | 18 182 op/s (120%) | 1 473 op/s (9.7%) | 1 372 op/s (9.1%) | 2 381 op/s (15.7%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s | 19 608 op/s (114%) | 1 433 op/s (8.3%) | 1 495 op/s (8.7%) | 1 667 op/s (9.7%) |
| Tree shfld. | 18 868 op/s | 20 833 op/s (110%) | 1 401 op/s (7.4%) | 1 484 op/s (7.9%) | 1 984 op/s (10.5%) |
| DAG GNP sparse | 12 987 op/s | 14 925 op/s (115%) | 868.1 op/s (6.7%) | 759.9 op/s (5.9%) | 1 206 op/s (9.3%) |
| DAG GNP dense | 2 976 op/s | 3 906 op/s (131%) | 60.74 op/s (2.0%) | 102.0 op/s (3.4%) | 503.8 op/s (16.9%) |
| DAG GNP dense shfld. | 2 646 op/s | 3 745 op/s (142%) | 56.48 op/s (2.1%) | 99.45 op/s (3.8%) | 442.9 op/s (16.7%) |

![1.8.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.524 ms | 0.580 ms (1.1×) | 7.961 ms (15×) | 11.70 ms (22×) | 3.041 ms (5.8×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.101 ms | 0.665 ms (0.6×) | 25.93 ms (24×) | 25.97 ms (24×) | 4.956 ms (4.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.191 ms | 0.924 ms (0.8×) | 26.52 ms (22×) | 19.22 ms (16×) | 14.43 ms (12×) |
| Tree shfld. | 1.303 ms | 1.099 ms (0.8×) | 26.51 ms (20×) | 18.65 ms (14×) | 9.834 ms (7.5×) |
| DAG GNP sparse | 2.185 ms | 1.394 ms (0.6×) | 54.43 ms (25×) | 44.66 ms (20×) | 17.21 ms (7.9×) |
| DAG GNP dense | 5.141 ms | 3.204 ms (0.6×) | 234.8 ms (46×) | 305.7 ms (59×) | 24.48 ms (4.8×) |
| DAG GNP dense shfld. | 5.836 ms | 3.309 ms (0.6×) | 243.6 ms (42×) | 300.5 ms (51×) | 31.28 ms (5.4×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 908 op/s | 1 724 op/s (90.3%) | 125.6 op/s (6.6%) | 85.51 op/s (4.5%) | 328.8 op/s (17.2%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 908.3 op/s | 1 504 op/s (166%) | 38.56 op/s (4.2%) | 38.51 op/s (4.2%) | 201.8 op/s (22.2%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 839.6 op/s | 1 082 op/s (129%) | 37.71 op/s (4.5%) | 52.02 op/s (6.2%) | 69.32 op/s (8.3%) |
| Tree shfld. | 767.5 op/s | 909.9 op/s (119%) | 37.72 op/s (4.9%) | 53.62 op/s (7.0%) | 101.7 op/s (13.2%) |
| DAG GNP sparse | 457.7 op/s | 717.4 op/s (157%) | 18.37 op/s (4.0%) | 22.39 op/s (4.9%) | 58.11 op/s (12.7%) |
| DAG GNP dense | 194.5 op/s | 312.1 op/s (160%) | 4.259 op/s (2.2%) | 3.271 op/s (1.7%) | 40.84 op/s (21.0%) |
| DAG GNP dense shfld. | 171.4 op/s | 302.2 op/s (176%) | 4.105 op/s (2.4%) | 3.328 op/s (1.9%) | 31.97 op/s (18.7%) |

![1.8.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 40.37 ms | 14.15 ms (0.4×) |
| DAG GNP dense | 119.5 ms | 39.13 ms (0.3×) |
| DAG GNP dense shfld. | 197.0 ms | 42.15 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 24.77 op/s | 70.69 op/s (285%) |
| DAG GNP dense | 8.368 op/s | 25.56 op/s (305%) |
| DAG GNP dense shfld. | 5.076 op/s | 23.73 op/s (467%) |

![1.8.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 1145.6 ms | 157.0 ms (0.1×) |
| DAG GNP dense | 1964.4 ms | 515.7 ms (0.3×) |
| DAG GNP dense shfld. | 2712.5 ms | 420.8 ms (0.2×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.873 op/s | 6.369 op/s (730%) |
| DAG GNP dense | 0.509 op/s | 1.939 op/s (381%) |
| DAG GNP dense shfld. | 0.369 op/s | 2.376 op/s (645%) |

![1.8.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 16683 ms | 1719.5 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.060 op/s | 0.582 op/s (970%) |

![1.8.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-8-5-toporder-n-100-000-000.svg)

### 1.9 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.040 ms | 0.040 ms (1.0×) | 0.041 ms (1.0×) | 0.262 ms (6.5×) | 0.230 ms (5.8×) | 0.388 ms (9.7×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.056 ms | 0.040 ms (0.7×) | 0.055 ms (1.0×) | 0.374 ms (6.7×) | 0.365 ms (6.5×) | 0.626 ms (11×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.035 ms | 0.028 ms (0.8×) | 0.030 ms (0.9×) | 0.201 ms (5.7×) | 0.347 ms (9.9×) | 0.639 ms (18×) |
| Tree shfld. | 0.035 ms | 0.029 ms (0.8×) | 0.031 ms (0.9×) | 0.206 ms (5.9×) | 0.343 ms (9.8×) | 0.671 ms (19×) |
| DAG GNP sparse | 0.053 ms | 0.055 ms (1.0×) | 0.043 ms (0.8×) | 0.288 ms (5.4×) | 1.030 ms (19×) | 1.018 ms (19×) |
| DAG GNP dense | 0.357 ms | 0.323 ms (0.9×) | 0.238 ms (0.7×) | 4.308 ms (12×) | 7.351 ms (21×) | 3.629 ms (10×) |
| DAG GNP dense shfld. | 0.390 ms | 0.314 ms (0.8×) | 0.286 ms (0.7×) | 4.203 ms (11×) | 7.433 ms (19×) | 3.963 ms (10×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 000 op/s | 25 000 op/s (100%) | 24 390 op/s (97.6%) | 3 817 op/s (15.3%) | 4 348 op/s (17.4%) | 2 577 op/s (10.3%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 857 op/s | 25 000 op/s (140%) | 18 182 op/s (102%) | 2 674 op/s (15.0%) | 2 740 op/s (15.3%) | 1 597 op/s (8.9%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 28 571 op/s | 35 714 op/s (125%) | 33 333 op/s (117%) | 4 975 op/s (17.4%) | 2 882 op/s (10.1%) | 1 565 op/s (5.5%) |
| Tree shfld. | 28 571 op/s | 34 483 op/s (121%) | 32 258 op/s (113%) | 4 854 op/s (17.0%) | 2 915 op/s (10.2%) | 1 490 op/s (5.2%) |
| DAG GNP sparse | 18 868 op/s | 18 182 op/s (96.4%) | 23 256 op/s (123%) | 3 472 op/s (18.4%) | 970.9 op/s (5.1%) | 982.3 op/s (5.2%) |
| DAG GNP dense | 2 801 op/s | 3 096 op/s (111%) | 4 202 op/s (150%) | 232.1 op/s (8.3%) | 136.0 op/s (4.9%) | 275.6 op/s (9.8%) |
| DAG GNP dense shfld. | 2 564 op/s | 3 185 op/s (124%) | 3 497 op/s (136%) | 237.9 op/s (9.3%) | 134.5 op/s (5.2%) | 252.3 op/s (9.8%) |

![1.9.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.567 ms | 0.458 ms (0.8×) | 0.359 ms (0.6×) | 4.201 ms (7.4×) | 3.671 ms (6.5×) | 3.702 ms (6.5×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.454 ms | 0.457 ms (0.3×) | 1.346 ms (0.9×) | 14.55 ms (10×) | 10.38 ms (7.1×) | 7.255 ms (5.0×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.936 ms | 0.838 ms (0.9×) | 0.869 ms (0.9×) | 7.912 ms (8.5×) | 8.530 ms (9.1×) | 7.634 ms (8.2×) |
| Tree shfld. | 0.962 ms | 0.839 ms (0.9×) | 0.872 ms (0.9×) | 7.294 ms (7.6×) | 8.774 ms (9.1×) | 7.867 ms (8.2×) |
| DAG GNP sparse | 2.210 ms | 1.940 ms (0.9×) | 1.938 ms (0.9×) | 18.56 ms (8.4×) | 29.87 ms (14×) | 13.99 ms (6.3×) |
| DAG GNP dense | 5.634 ms | 5.018 ms (0.9×) | 4.212 ms (0.7×) | 64.01 ms (11×) | 202.5 ms (36×) | 44.30 ms (7.9×) |
| DAG GNP dense shfld. | 6.536 ms | 4.979 ms (0.8×) | 5.049 ms (0.8×) | 72.93 ms (11×) | 213.5 ms (33×) | 56.25 ms (8.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 764 op/s | 2 183 op/s (124%) | 2 786 op/s (158%) | 238.0 op/s (13.5%) | 272.4 op/s (15.4%) | 270.1 op/s (15.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 687.8 op/s | 2 188 op/s (318%) | 742.9 op/s (108%) | 68.72 op/s (10.0%) | 96.32 op/s (14.0%) | 137.8 op/s (20.0%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 068 op/s | 1 193 op/s (112%) | 1 151 op/s (108%) | 126.4 op/s (11.8%) | 117.2 op/s (11.0%) | 131.0 op/s (12.3%) |
| Tree shfld. | 1 040 op/s | 1 192 op/s (115%) | 1 147 op/s (110%) | 137.1 op/s (13.2%) | 114.0 op/s (11.0%) | 127.1 op/s (12.2%) |
| DAG GNP sparse | 452.5 op/s | 515.5 op/s (114%) | 516.0 op/s (114%) | 53.87 op/s (11.9%) | 33.48 op/s (7.4%) | 71.49 op/s (15.8%) |
| DAG GNP dense | 177.5 op/s | 199.3 op/s (112%) | 237.4 op/s (134%) | 15.62 op/s (8.8%) | 4.938 op/s (2.8%) | 22.57 op/s (12.7%) |
| DAG GNP dense shfld. | 153.0 op/s | 200.8 op/s (131%) | 198.1 op/s (129%) | 13.71 op/s (9.0%) | 4.684 op/s (3.1%) | 17.78 op/s (11.6%) |

![1.9.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 38.38 ms | 36.68 ms (1.0×) |
| DAG GNP dense | 129.6 ms | 113.3 ms (0.9×) |
| DAG GNP dense shfld. | 209.4 ms | 110.4 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 26.06 op/s | 27.26 op/s (105%) |
| DAG GNP dense | 7.716 op/s | 8.826 op/s (114%) |
| DAG GNP dense shfld. | 4.776 op/s | 9.058 op/s (190%) |

![1.9.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 860.1 ms | 770.8 ms (0.9×) |
| DAG GNP dense | 3105.4 ms | 2757.7 ms (0.9×) |
| DAG GNP dense shfld. | 4418.6 ms | 2608.4 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 1.163 op/s | 1.297 op/s (112%) |
| DAG GNP dense | 0.322 op/s | 0.363 op/s (113%) |
| DAG GNP dense shfld. | 0.226 op/s | 0.383 op/s (169%) |

![1.9.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 11304 ms | 9506.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| DAG GNP sparse | 0.088 op/s | 0.105 op/s (119%) |

![1.9.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-9-5-toporderandlevels-n-100-000-000.svg)

### 1.10 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.049 ms | 0.050 ms (1.0×) | 0.157 ms (3.2×) | 0.212 ms (4.3×) | 0.538 ms (11×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.062 ms | 0.051 ms (0.8×) | 0.357 ms (5.8×) | 0.368 ms (5.9×) | 0.423 ms (6.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.042 ms | 0.038 ms (0.9×) | 0.439 ms (10×) | 0.669 ms (16×) | 0.585 ms (14×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.190 ms | 0.094 ms (0.5×) | 0.641 ms (3.4×) | 1.139 ms (6.0×) | 0.775 ms (4.1×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.806 ms | 0.690 ms (0.9×) | 4.477 ms (5.6×) | 12.04 ms (15×) | 2.629 ms (3.3×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.616 ms | 0.617 ms (1.0×) | 3.378 ms (5.5×) | 2.229 ms (3.6×) | 3.964 ms (6.4×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.656 ms | 0.600 ms (0.9×) | 4.182 ms (6.4×) | 3.747 ms (5.7×) | 2.436 ms (3.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 20 408 op/s | 20 000 op/s (98.0%) | 6 369 op/s (31.2%) | 4 717 op/s (23.1%) | 1 859 op/s (9.1%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 16 129 op/s | 19 608 op/s (122%) | 2 801 op/s (17.4%) | 2 717 op/s (16.8%) | 2 364 op/s (14.7%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 23 810 op/s | 26 316 op/s (111%) | 2 278 op/s (9.6%) | 1 495 op/s (6.3%) | 1 709 op/s (7.2%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 5 263 op/s | 10 638 op/s (202%) | 1 560 op/s (29.6%) | 878.0 op/s (16.7%) | 1 290 op/s (24.5%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 241 op/s | 1 449 op/s (117%) | 223.4 op/s (18.0%) | 83.09 op/s (6.7%) | 380.4 op/s (30.7%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 623 op/s | 1 621 op/s (99.8%) | 296.0 op/s (18.2%) | 448.6 op/s (27.6%) | 252.3 op/s (15.5%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 524 op/s | 1 667 op/s (109%) | 239.1 op/s (15.7%) | 266.9 op/s (17.5%) | 410.5 op/s (26.9%) |

![1.10.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-10-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.478 ms | 0.454 ms (0.9×) | 6.257 ms (13×) | 5.693 ms (12×) | 3.301 ms (6.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.027 ms | 0.524 ms (0.5×) | 23.32 ms (23×) | 15.49 ms (15×) | 5.017 ms (4.9×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.166 ms | 0.903 ms (0.8×) | 26.66 ms (23×) | 20.59 ms (18×) | 14.48 ms (12×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.246 ms | 1.952 ms (0.6×) | 44.84 ms (14×) | 43.34 ms (13×) | 10.88 ms (3.4×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 14.30 ms | 7.507 ms (0.5×) | 135.9 ms (9.5×) | 204.1 ms (14×) | 43.18 ms (3.0×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 6.510 ms | 6.525 ms (1.0×) | 77.86 ms (12×) | 62.76 ms (9.6×) | 46.98 ms (7.2×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 12.52 ms | 6.633 ms (0.5×) | 138.9 ms (11×) | 110.1 ms (8.8×) | 42.60 ms (3.4×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 092 op/s | 2 203 op/s (105%) | 159.8 op/s (7.6%) | 175.7 op/s (8.4%) | 302.9 op/s (14.5%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 973.7 op/s | 1 908 op/s (196%) | 42.88 op/s (4.4%) | 64.57 op/s (6.6%) | 199.3 op/s (20.5%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 857.6 op/s | 1 107 op/s (129%) | 37.51 op/s (4.4%) | 48.57 op/s (5.7%) | 69.05 op/s (8.1%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 308.1 op/s | 512.3 op/s (166%) | 22.30 op/s (7.2%) | 23.07 op/s (7.5%) | 91.88 op/s (29.8%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 69.93 op/s | 133.2 op/s (191%) | 7.358 op/s (10.5%) | 4.900 op/s (7.0%) | 23.16 op/s (33.1%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 153.6 op/s | 153.3 op/s (99.8%) | 12.84 op/s (8.4%) | 15.93 op/s (10.4%) | 21.29 op/s (13.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 79.90 op/s | 150.8 op/s (189%) | 7.199 op/s (9.0%) | 9.083 op/s (11.4%) | 23.47 op/s (29.4%) |

![1.10.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-10-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 68.33 ms | 20.93 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 258.1 ms | 104.2 ms (0.4×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 79.86 ms | 79.00 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 14.63 op/s | 47.77 op/s (326%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 3.874 op/s | 9.597 op/s (248%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 12.52 op/s | 12.66 op/s (101%) |

![1.10.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-10-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1996.7 ms | 261.7 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3711.0 ms | 976.8 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 689.9 ms | 709.6 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.501 op/s | 3.821 op/s (763%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.269 op/s | 1.024 op/s (380%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.449 op/s | 1.409 op/s (97.2%) |

![1.10.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-10-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 27875 ms | 3042.7 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.036 op/s | 0.329 op/s (916%) |

![1.10.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-10-5-backedges-n-100-000-000.svg)

### 1.11 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.075 ms | 0.066 ms (0.9×) | 0.187 ms (2.5×) | 0.243 ms (3.2×) | 0.610 ms (8.1×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.083 ms | 0.069 ms (0.8×) | 0.382 ms (4.6×) | 0.410 ms (4.9×) | 0.505 ms (6.1×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.050 ms | 0.040 ms (0.8×) | 0.501 ms (10×) | 0.681 ms (14×) | 0.653 ms (13×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.189 ms | 0.086 ms (0.5×) | 0.656 ms (3.5×) | 1.388 ms (7.3×) | 0.917 ms (4.9×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms | 0.381 ms (0.7×) | 8.162 ms (16×) | 13.61 ms (26×) | 3.287 ms (6.3×) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.280 ms | 0.282 ms (1.0×) | 5.143 ms (18×) | 3.720 ms (13×) | 4.633 ms (17×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.392 ms | 0.304 ms (0.8×) | 6.289 ms (16×) | 5.361 ms (14×) | 3.152 ms (8.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 13 333 op/s | 15 152 op/s (114%) | 5 348 op/s (40.1%) | 4 115 op/s (30.9%) | 1 639 op/s (12.3%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s | 14 493 op/s (120%) | 2 618 op/s (21.7%) | 2 439 op/s (20.2%) | 1 980 op/s (16.4%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 20 000 op/s | 25 000 op/s (125%) | 1 996 op/s (10.0%) | 1 468 op/s (7.3%) | 1 531 op/s (7.7%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 5 291 op/s | 11 628 op/s (220%) | 1 524 op/s (28.8%) | 720.5 op/s (13.6%) | 1 091 op/s (20.6%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s | 2 625 op/s (138%) | 122.5 op/s (6.4%) | 73.50 op/s (3.9%) | 304.2 op/s (16.0%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3 571 op/s | 3 546 op/s (99.3%) | 194.4 op/s (5.4%) | 268.8 op/s (7.5%) | 215.8 op/s (6.0%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 551 op/s | 3 289 op/s (129%) | 159.0 op/s (6.2%) | 186.5 op/s (7.3%) | 317.3 op/s (12.4%) |

![1.11.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-11-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.629 ms | 0.698 ms (1.1×) | 9.131 ms (15×) | 8.504 ms (14×) | 4.627 ms (7.4×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.500 ms | 0.643 ms (0.4×) | 20.39 ms (14×) | 17.50 ms (12×) | 6.072 ms (4.0×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.375 ms | 0.871 ms (0.6×) | 32.24 ms (23×) | 21.79 ms (16×) | 17.55 ms (13×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.558 ms | 1.840 ms (0.5×) | 46.91 ms (13×) | 48.17 ms (14×) | 13.27 ms (3.7×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 8.165 ms | 4.314 ms (0.5×) | 158.3 ms (19×) | 218.7 ms (27×) | 54.56 ms (6.7×) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.616 ms | 2.615 ms (1.0×) | 88.85 ms (34×) | 74.75 ms (29×) | 58.03 ms (22×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.794 ms | 3.402 ms (0.5×) | 164.0 ms (24×) | 127.1 ms (19×) | 54.12 ms (8.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 590 op/s | 1 433 op/s (90.1%) | 109.5 op/s (6.9%) | 117.6 op/s (7.4%) | 216.1 op/s (13.6%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 666.7 op/s | 1 555 op/s (233%) | 49.04 op/s (7.4%) | 57.15 op/s (8.6%) | 164.7 op/s (24.7%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 727.3 op/s | 1 148 op/s (158%) | 31.01 op/s (4.3%) | 45.89 op/s (6.3%) | 56.97 op/s (7.8%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 281.1 op/s | 543.5 op/s (193%) | 21.32 op/s (7.6%) | 20.76 op/s (7.4%) | 75.35 op/s (26.8%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 122.5 op/s | 231.8 op/s (189%) | 6.317 op/s (5.2%) | 4.572 op/s (3.7%) | 18.33 op/s (15.0%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 382.3 op/s | 382.4 op/s (100%) | 11.26 op/s (2.9%) | 13.38 op/s (3.5%) | 17.23 op/s (4.5%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 147.2 op/s | 293.9 op/s (200%) | 6.098 op/s (4.1%) | 7.868 op/s (5.3%) | 18.48 op/s (12.6%) |

![1.11.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-11-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 103.5 ms | 23.44 ms (0.2×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 261.1 ms | 67.87 ms (0.3×) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 28.06 ms | 27.32 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 9.662 op/s | 42.66 op/s (441%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 3.830 op/s | 14.73 op/s (385%) |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 35.64 op/s | 36.60 op/s (103%) |

![1.11.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-11-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2524.1 ms | 425.6 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4540.2 ms | 1449.5 ms (0.3×) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 281.0 ms | 282.8 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.396 op/s | 2.350 op/s (593%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.220 op/s | 0.690 op/s (313%) |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.559 op/s | 3.536 op/s (99.4%) |

![1.11.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-11-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 31522 ms | 6892.8 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.032 op/s | 0.145 op/s (457%) |

![1.11.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-peers/1-11-5-edgetypes-n-100-000-000.svg)

### 1.12 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.108 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.135 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.093 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.373 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.827 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.525 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.631 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 259 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 407 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 753 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 681 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 209 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 905 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 585 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.113 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.096 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.501 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.441 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.03 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.510 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 10.30 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 898.5 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 477.1 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 399.8 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 183.8 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 83.13 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 181.5 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 97.06 op/s |

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 127.2 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 328.2 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 54.61 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 7.862 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 3.047 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 18.31 op/s |

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3228.1 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 7233.0 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 548.8 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.310 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.138 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.822 op/s |

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 40274 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.025 op/s |

### 1.13 BiAdjacency (in-adjacency) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.022 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.023 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.024 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.059 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.551 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.547 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.551 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 45 455 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 43 478 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 41 667 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 16 949 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 815 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 828 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 815 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.206 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.241 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.618 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.418 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.949 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.611 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 8.096 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 4 854 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 149 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 618 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 705.2 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 125.8 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 216.9 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 123.5 op/s |

### 1.14 Symmetrize (WebGraph) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 5.437 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 8.017 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 8.057 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 18.51 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 111.5 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 14.95 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 58.27 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 183.9 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 124.7 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 124.1 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 54.04 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 8.969 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 66.88 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 17.16 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 38.47 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 62.81 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 69.78 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 183.9 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1111.0 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 122.3 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 667.5 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | WebGraph |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 26.00 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 15.92 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 14.33 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.438 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 0.900 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.177 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1.498 op/s |

### 1.15 Dominators

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 0.177 ms |
| Irreducible | 0.174 ms |
| Ladder | 0.157 ms |
| Chain | 0.117 ms |
| Fan behind chain | 0.143 ms |
| Small methods | 0.183 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 5 650 op/s |
| Irreducible | 5 747 op/s |
| Ladder | 6 369 op/s |
| Chain | 8 547 op/s |
| Fan behind chain | 6 993 op/s |
| Small methods | 5 464 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 3.727 ms |
| Irreducible | 4.013 ms |
| Ladder | 1.540 ms |
| Chain | 1.119 ms |
| Fan behind chain | 1.414 ms |
| Small methods | 2.925 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 268.3 op/s |
| Irreducible | 249.2 op/s |
| Ladder | 649.4 op/s |
| Chain | 893.7 op/s |
| Fan behind chain | 707.2 op/s |
| Small methods | 341.9 op/s |

### 1.16 Dominance frontiers

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 0.069 ms |
| Irreducible | 0.081 ms |
| Ladder | 45.78 ms |
| Chain | 0.037 ms |
| Fan behind chain | 107.1 ms |
| Small methods | 0.056 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 14 493 op/s |
| Irreducible | 12 346 op/s |
| Ladder | 21.84 op/s |
| Chain | 27 027 op/s |
| Fan behind chain | 9.337 op/s |
| Small methods | 17 857 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 1.980 ms |
| Irreducible | 2.293 ms |
| Chain | 0.368 ms |
| Small methods | 1.424 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 505.1 op/s |
| Irreducible | 436.1 op/s |
| Chain | 2 717 op/s |
| Small methods | 702.2 op/s |

### 1.17 Iterated dominance frontier (build + one query per variable)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 30.64 ms |
| Irreducible | 41.00 ms |
| Ladder | 36.06 ms |
| Chain | 21.12 ms |
| Fan behind chain | 33.82 ms |
| Small methods | 0.427 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 32.64 op/s |
| Irreducible | 24.39 op/s |
| Ladder | 27.73 op/s |
| Chain | 47.34 op/s |
| Fan behind chain | 29.56 op/s |
| Small methods | 2 342 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 432.0 ms |
| Irreducible | 559.8 ms |
| Ladder | 369.3 ms |
| Chain | 234.4 ms |
| Fan behind chain | 402.3 ms |
| Small methods | 4.475 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 2.315 op/s |
| Irreducible | 1.786 op/s |
| Ladder | 2.708 op/s |
| Chain | 4.266 op/s |
| Fan behind chain | 2.486 op/s |
| Small methods | 223.5 op/s |

### 1.18 Loops

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 0.400 ms |
| Irreducible | 0.423 ms |
| Ladder | 0.107 ms |
| Chain | 0.105 ms |
| Fan behind chain | 0.105 ms |
| Small methods | 0.461 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 2 500 op/s |
| Irreducible | 2 364 op/s |
| Ladder | 9 346 op/s |
| Chain | 9 524 op/s |
| Fan behind chain | 9 524 op/s |
| Small methods | 2 169 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 5.993 ms |
| Irreducible | 7.556 ms |
| Ladder | 1.063 ms |
| Chain | 1.025 ms |
| Fan behind chain | 1.029 ms |
| Small methods | 4.950 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 166.9 op/s |
| Irreducible | 132.3 op/s |
| Ladder | 940.7 op/s |
| Chain | 975.6 op/s |
| Fan behind chain | 971.8 op/s |
| Small methods | 202.0 op/s |

### 1.19 Ancestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 0.058 ms |
| Irreducible | 0.068 ms |
| Ladder | 0.053 ms |
| Chain | 0.049 ms |
| Fan behind chain | 0.052 ms |
| Small methods | 0.059 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 17 241 op/s |
| Irreducible | 14 706 op/s |
| Ladder | 18 868 op/s |
| Chain | 20 408 op/s |
| Fan behind chain | 19 231 op/s |
| Small methods | 16 949 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 1.816 ms |
| Irreducible | 1.814 ms |
| Ladder | 0.506 ms |
| Chain | 0.566 ms |
| Fan behind chain | 0.513 ms |
| Small methods | 1.577 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 550.7 op/s |
| Irreducible | 551.3 op/s |
| Ladder | 1 976 op/s |
| Chain | 1 767 op/s |
| Fan behind chain | 1 949 op/s |
| Small methods | 634.1 op/s |

### 1.20 PreorderAncestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 0.053 ms |
| Irreducible | 0.053 ms |
| Ladder | 0.045 ms |
| Chain | 0.047 ms |
| Fan behind chain | 0.046 ms |
| Small methods | 0.049 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 18 868 op/s |
| Irreducible | 18 868 op/s |
| Ladder | 22 222 op/s |
| Chain | 21 277 op/s |
| Fan behind chain | 21 739 op/s |
| Small methods | 20 408 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 1.079 ms |
| Irreducible | 1.168 ms |
| Ladder | 0.364 ms |
| Chain | 0.424 ms |
| Fan behind chain | 0.363 ms |
| Small methods | 0.858 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured | 926.8 op/s |
| Irreducible | 856.2 op/s |
| Ladder | 2 747 op/s |
| Chain | 2 358 op/s |
| Fan behind chain | 2 755 op/s |
| Small methods | 1 166 op/s |

## 2 Memory Footprint

Retained heap in bytes for a fully built graph. Lower is better. Baseline (= 100%): **Graphity 2.4.0**, or the newest version present in a table it is missing from. Two charts per section: *Graphity & WebGraph* (same league) and *All libraries*.

### 2.1 Total Graph Size

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5037%) | 2.7 MB (3375%) | 18.8 kB (23.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5037%) | 2.7 MB (3375%) | 37.8 kB (47.2%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 80.1 kB | 4.0 MB (5043%) | 2.7 MB (3378%) | 31.9 kB (39.8%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 160 kB | 8.6 MB (5343%) | 4.8 MB (2984%) | 78.8 kB (49.2%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB | 68.6 MB (5544%) | 33.4 MB (2700%) | 475 kB (38.4%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB | 71.3 MB (5558%) | 22.7 MB (1769%) | 50.6 kB (3.9%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB | 71.3 MB (5559%) | 22.7 MB (1769%) | 482 kB (37.6%) |
| Path<br><sub>100k nodes, 100k edges</sub> | 800 kB | 41.5 MB (5192%) | 27.4 MB (3430%) | 181 kB (22.6%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 800 kB | 41.5 MB (5192%) | 27.4 MB (3430%) | 430 kB (53.7%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 800 kB | 41.7 MB (5208%) | 27.5 MB (3443%) | 372 kB (46.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.6 MB | 85.0 MB (5310%) | 48.3 MB (3016%) | 952 kB (59.5%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.4 MB | 680 MB (5488%) | 337 MB (2716%) | 6.4 MB (51.7%) |
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

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 4 B | 201.65 B (5041%) | 135.10 B (3378%) | 0.94 B (23.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 4 B | 201.65 B (5041%) | 135.10 B (3378%) | 1.89 B (47.2%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 B | 201.90 B (5048%) | 135.24 B (3381%) | 1.60 B (40.0%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 B | 213.79 B (5345%) | 119.42 B (2986%) | 1.97 B (49.2%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4 B | 221.75 B (5544%) | 108.02 B (2700%) | 1.54 B (38.5%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4 B | 222.34 B (5558%) | 70.78 B (1770%) | 0.16 B (4.0%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4 B | 222.36 B (5559%) | 70.78 B (1770%) | 1.50 B (37.5%) |
| Path<br><sub>100k nodes, 100k edges</sub> | 4 B | 207.71 B (5193%) | 137.23 B (3431%) | 0.90 B (22.5%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 B | 207.71 B (5193%) | 137.22 B (3430%) | 2.15 B (53.8%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 4 B | 208.34 B (5208%) | 137.73 B (3443%) | 1.86 B (46.5%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4 B | 212.40 B (5310%) | 120.65 B (3016%) | 2.38 B (59.5%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4 B | 219.51 B (5488%) | 108.63 B (2716%) | 2.07 B (51.7%) |
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

Bytes allocated per algorithm invocation (JMH `-prof gc`). Lower is better. Baseline (= 100%): **Graphity 2.4.0**, or the newest version present in a table it is missing from.

### 3.1 Build

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 458 kB | 5.3 MB (1163%) | 2.8 MB (604%) | 4.3 MB (949%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 458 kB | 5.2 MB (1133%) | 2.8 MB (604%) | 4.4 MB (955%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 458 kB | 5.3 MB (1161%) | 2.8 MB (609%) | 4.1 MB (888%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 960 kB | 13.1 MB (1360%) | 5.1 MB (536%) | 4.5 MB (464%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB | 111 MB (1116%) | 37.3 MB (376%) | 9.8 MB (98.5%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB | 116 MB (1159%) | 29.3 MB (294%) | 8.9 MB (89.7%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB | 116 MB (1159%) | 29.3 MB (294%) | 9.4 MB (94.5%) |

![3.1.1 · Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-1-1-build-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.3.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 54.4 MB (1447%) | 28.5 MB (757%) | 14.3 MB (379%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 54.3 MB (1443%) | 28.5 MB (757%) | 14.5 MB (386%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB | 55.7 MB (1479%) | 28.8 MB (766%) | 11.4 MB (303%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB | 129 MB (1215%) | 52.3 MB (492%) | 15.4 MB (145%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB | 1094 MB (983%) | 376 MB (338%) | 69.9 MB (62.8%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB | 1139 MB (1020%) | 294 MB (264%) | 60.2 MB (53.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB | 1139 MB (1020%) | 294 MB (264%) | 66.4 MB (59.5%) |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 483 kB | 5.2 MB (1078%) | 2.6 MB (540%) | 1.4 MB (297%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 483 kB | 5.2 MB (1068%) | 2.6 MB (528%) | 1.3 MB (274%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 288 kB | 5.0 MB (1746%) | 2.3 MB (799%) | 908 kB (315%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 371 kB | 3.2 MB (854%) | 2.6 MB (703%) | 1.3 MB (346%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 446 kB | 6.4 MB (1422%) | 4.6 MB (1020%) | 1.5 MB (329%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 444 kB | 4.8 MB (1075%) | 7.1 MB (1593%) | 6.1 MB (1379%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 364 kB | 6.0 MB (1634%) | 7.2 MB (1982%) | 1.3 MB (348%) |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 4.6 MB | 51.6 MB (1113%) | 28.9 MB (622%) | 13.3 MB (287%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4.6 MB | 51.7 MB (1114%) | 27.4 MB (591%) | 13.5 MB (290%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB | 50.3 MB (1792%) | 25.5 MB (907%) | 8.8 MB (314%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.5 MB | 32.2 MB (916%) | 28.5 MB (812%) | 12.3 MB (351%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4.3 MB | 70.1 MB (1642%) | 45.2 MB (1060%) | 13.5 MB (315%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.3 MB | 48.5 MB (1141%) | 76.8 MB (1806%) | 60.3 MB (1417%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB | 61.4 MB (1777%) | 75.3 MB (2181%) | 12.3 MB (356%) |

![3.2.2 · SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-2-2-scc-n-100-000.svg)

### 3.3 WCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB | 3.0 MB (1516%) | 2.4 MB (1196%) | 2.0 MB (989%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB | 2.3 MB (1156%) | 2.4 MB (1196%) | 1.9 MB (929%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB | 3.1 MB (1545%) | 2.4 MB (1196%) | 1.2 MB (611%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB | 3.1 MB (1551%) | 2.9 MB (1435%) | 1.2 MB (600%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 10.3 MB (5169%) | 9.3 MB (4668%) | 1.3 MB (650%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB | 9.4 MB (4720%) | 9.6 MB (4802%) | 6.1 MB (3070%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB | 9.8 MB (4919%) | 9.6 MB (4802%) | 1.3 MB (642%) |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 32.7 MB (1635%) | 26.3 MB (1315%) | 19.3 MB (965%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 32.7 MB (1635%) | 26.3 MB (1315%) | 16.5 MB (824%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB | 33.2 MB (1661%) | 26.3 MB (1315%) | 11.1 MB (557%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB | 37.5 MB (1875%) | 31.1 MB (1554%) | 11.0 MB (552%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 106 MB (5303%) | 95.9 MB (4793%) | 11.7 MB (587%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB | 101 MB (5036%) | 98.4 MB (4917%) | 61.4 MB (3070%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB | 96.8 MB (4836%) | 98.4 MB (4917%) | 12.5 MB (623%) |

![3.3.2 · WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-3-2-wcc-n-100-000.svg)

### 3.4 CCs

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB |

### 3.5 Condense

**Allocations @ 1k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 23.2 kB |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 97 B |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 761 B |
| DAG GNP sparse<br><sub>1k nodes</sub> | 66.4 kB |

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 250 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 103 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 7.2 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 541 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.1 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 217 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 75.9 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 5.9 MB |

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 246 kB | 1.3 MB (510%) | 943 kB (384%) | 1.5 MB (597%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 246 kB | 1.3 MB (510%) | 1.1 MB (449%) | 1.0 MB (417%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 50.8 kB | 1.3 MB (2508%) | 812 kB (1598%) | 885 kB (1743%) |
| Tree shfld.<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2508%) | 812 kB (1598%) | 845 kB (1664%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2508%) | 930 kB (1831%) | 984 kB (1937%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 50.8 kB | 1.3 MB (2469%) | 812 kB (1597%) | 1.1 MB (2161%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 50.9 kB | 1.3 MB (2469%) | 812 kB (1597%) | 1.1 MB (2118%) |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.3 MB | 13.3 MB (587%) | 9.9 MB (439%) | 10.5 MB (463%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.3 MB | 13.3 MB (587%) | 8.9 MB (393%) | 10.0 MB (443%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 433 kB | 13.5 MB (3108%) | 7.9 MB (1811%) | 8.5 MB (1953%) |
| Tree shfld.<br><sub>100k nodes</sub> | 433 kB | 13.5 MB (3108%) | 7.9 MB (1811%) | 8.4 MB (1944%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 433 kB | 13.6 MB (3128%) | 9.6 MB (2210%) | 9.5 MB (2203%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 434 kB | 13.3 MB (3073%) | 7.9 MB (1811%) | 10.3 MB (2374%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 434 kB | 13.3 MB (3073%) | 7.9 MB (1811%) | 10.2 MB (2360%) |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 476 kB (297%) | 1.7 MB (1088%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 476 kB (297%) | 1.7 MB (1070%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 476 kB (297%) | 1.3 MB (829%) |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB | 160 kB (100%) | 476 kB (297%) | 476 kB (297%) | 1.3 MB (796%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB | 160 kB (100.0%) | 476 kB (297%) | 476 kB (297%) | 1.6 MB (1016%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB | 160 kB (100.0%) | 476 kB (297%) | 477 kB (297%) | 1.8 MB (1126%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB | 160 kB (100.0%) | 476 kB (297%) | 477 kB (297%) | 1.8 MB (1140%) |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | Graphity 2.4.0<br>(bi-adjacent) | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 16.9 MB (1054%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 16.8 MB (1052%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 12.5 MB (780%) |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 12.4 MB (777%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 15.5 MB (967%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 17.1 MB (1070%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB | 1.6 MB (100.0%) | 4.8 MB (300%) | 4.8 MB (300%) | 17.1 MB (1069%) |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 206 kB | 161 kB (78.1%) | 161 kB (78.1%) | 1.4 MB (693%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 206 kB | 161 kB (78.1%) | 161 kB (78.1%) | 985 kB (479%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10.9 kB | 161 kB (1480%) | 161 kB (1481%) | 845 kB (7789%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 351 kB | 386 kB (110%) | 386 kB (110%) | 1.2 MB (336%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4.0 MB | 4.0 MB (98.9%) | 4.8 MB (119%) | 4.9 MB (121%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4.0 MB | 4.0 MB (98.9%) | 4.0 MB (98.9%) | 9.7 MB (239%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4.0 MB | 4.0 MB (101%) | 4.0 MB (101%) | 4.9 MB (124%) |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.9 MB | 1.6 MB (87.0%) | 1.6 MB (87.0%) | 10.1 MB (541%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.9 MB | 1.6 MB (87.1%) | 3.2 MB (173%) | 9.6 MB (517%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 33.5 kB | 1.6 MB (4855%) | 1.6 MB (4854%) | 8.1 MB (24092%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 3.6 MB | 4.2 MB (115%) | 4.2 MB (115%) | 12.1 MB (332%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 45.6 MB | 45.4 MB (99.5%) | 53.4 MB (117%) | 53.6 MB (117%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 45.6 MB | 45.4 MB (99.5%) | 45.4 MB (99.5%) | 101 MB (220%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 44.8 MB | 45.4 MB (101%) | 45.4 MB (101%) | 53.5 MB (119%) |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 256 kB | 581 kB (227%) | 581 kB (227%) | 1.8 MB (722%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 256 kB | 581 kB (227%) | 581 kB (227%) | 1.4 MB (550%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 60.8 kB | 581 kB (955%) | 581 kB (956%) | 1.3 MB (2081%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 196 kB | 1.1 MB (540%) | 1.5 MB (756%) | 1.8 MB (945%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 545 kB | 9.9 MB (1821%) | 10.7 MB (1968%) | 10.8 MB (1988%) |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 556 kB | 9.9 MB (1784%) | 9.9 MB (1784%) | 15.6 MB (2801%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 476 kB | 9.9 MB (2084%) | 9.9 MB (2084%) | 10.8 MB (2275%) |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.4 MB | 4.9 MB (207%) | 4.9 MB (207%) | 13.4 MB (565%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.4 MB | 4.9 MB (207%) | 4.9 MB (207%) | 12.9 MB (547%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 533 kB | 4.9 MB (920%) | 4.9 MB (920%) | 11.3 MB (2127%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.8 MB | 11.8 MB (666%) | 16.0 MB (903%) | 19.6 MB (1111%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 5.3 MB | 113 MB (2142%) | 121 MB (2294%) | 121 MB (2298%) |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.4 MB | 113 MB (2101%) | 113 MB (2101%) | 168 MB (3128%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4.6 MB | 113 MB (2469%) | 113 MB (2469%) | 121 MB (2645%) |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-peers/3-9-2-edgetypes-n-100-000.svg)

### 3.10 Dominators

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 371 kB |
| Irreducible<br><sub>10k nodes</sub> | 371 kB |
| Ladder<br><sub>10k nodes</sub> | 486 kB |
| Chain<br><sub>10k nodes</sub> | 566 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 486 kB |
| Small methods<br><sub>10k nodes</sub> | 475 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 3.7 MB |
| Irreducible<br><sub>100k nodes</sub> | 3.7 MB |
| Ladder<br><sub>100k nodes</sub> | 4.7 MB |
| Chain<br><sub>100k nodes</sub> | 5.5 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 4.7 MB |
| Small methods<br><sub>100k nodes</sub> | 4.7 MB |

### 3.11 Dominance frontiers

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 134 kB |
| Irreducible<br><sub>10k nodes</sub> | 144 kB |
| Ladder<br><sub>10k nodes</sub> | 50.1 MB |
| Chain<br><sub>10k nodes</sub> | 80.1 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 100 MB |
| Small methods<br><sub>10k nodes</sub> | 140 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 1.3 MB |
| Irreducible<br><sub>100k nodes</sub> | 1.4 MB |
| Chain<br><sub>100k nodes</sub> | 800 kB |
| Small methods<br><sub>100k nodes</sub> | 1.4 MB |

### 3.12 Iterated dominance frontier (build + one query per variable)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 25.4 MB |
| Irreducible<br><sub>10k nodes</sub> | 28.9 MB |
| Ladder<br><sub>10k nodes</sub> | 53.8 MB |
| Chain<br><sub>10k nodes</sub> | 21.4 MB |
| Fan behind chain<br><sub>10k nodes</sub> | 65.4 MB |
| Small methods<br><sub>10k nodes</sub> | 604 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 370 MB |
| Irreducible<br><sub>100k nodes</sub> | 451 MB |
| Ladder<br><sub>100k nodes</sub> | 504 MB |
| Chain<br><sub>100k nodes</sub> | 198 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 662 MB |
| Small methods<br><sub>100k nodes</sub> | 5.8 MB |

### 3.13 Loops

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 772 kB |
| Irreducible<br><sub>10k nodes</sub> | 812 kB |
| Ladder<br><sub>10k nodes</sub> | 675 kB |
| Chain<br><sub>10k nodes</sub> | 735 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 675 kB |
| Small methods<br><sub>10k nodes</sub> | 897 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 7.5 MB |
| Irreducible<br><sub>100k nodes</sub> | 13.5 MB |
| Ladder<br><sub>100k nodes</sub> | 6.6 MB |
| Chain<br><sub>100k nodes</sub> | 7.2 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 6.6 MB |
| Small methods<br><sub>100k nodes</sub> | 8.9 MB |

### 3.14 Ancestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 280 kB |
| Irreducible<br><sub>10k nodes</sub> | 280 kB |
| Ladder<br><sub>10k nodes</sub> | 280 kB |
| Chain<br><sub>10k nodes</sub> | 280 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 280 kB |
| Small methods<br><sub>10k nodes</sub> | 297 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 2.8 MB |
| Irreducible<br><sub>100k nodes</sub> | 2.8 MB |
| Ladder<br><sub>100k nodes</sub> | 2.8 MB |
| Chain<br><sub>100k nodes</sub> | 2.8 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 2.8 MB |
| Small methods<br><sub>100k nodes</sub> | 2.9 MB |

### 3.15 PreorderAncestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>10k nodes</sub> | 200 kB |
| Irreducible<br><sub>10k nodes</sub> | 200 kB |
| Ladder<br><sub>10k nodes</sub> | 200 kB |
| Chain<br><sub>10k nodes</sub> | 200 kB |
| Fan behind chain<br><sub>10k nodes</sub> | 200 kB |
| Small methods<br><sub>10k nodes</sub> | 216 kB |

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 2.4.0 |
|---|---:|
| Structured<br><sub>100k nodes</sub> | 2.0 MB |
| Irreducible<br><sub>100k nodes</sub> | 2.0 MB |
| Ladder<br><sub>100k nodes</sub> | 2.0 MB |
| Chain<br><sub>100k nodes</sub> | 2.0 MB |
| Fan behind chain<br><sub>100k nodes</sub> | 2.0 MB |
| Small methods<br><sub>100k nodes</sub> | 2.1 MB |
