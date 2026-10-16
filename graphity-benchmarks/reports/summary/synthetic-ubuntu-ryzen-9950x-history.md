# Graphity Version History — synthetic / ubuntu-ryzen-9950x

Benchmarked versions: `1.0`, `2.0.0`, `2.1.0`, `2.2.0`, `2.3.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`synthetic-ubuntu-ryzen-9950x-peers.md`](synthetic-ubuntu-ryzen-9950x-peers.md).

Versions `1.0`, `2.0.0`, `2.1.0`, `2.2.0` report the mean over all measurement windows; the later ones a near-best window (`Estimate` below), which comes out a few percent faster for unchanged code.

```
Profile:      synthetic
Timestamp:    2026-10-16 04:51:31
JDK:          OpenJDK 64-Bit Server VM 25.0.2 (Amazon.com Inc.)
OS:           Ubuntu 25.10, kernel 6.17.0-41-generic (amd64)
CPU:          AMD Ryzen 9 9950X 16-Core Processor (32 logical cores)
RAM:          89.7 GiB
Estimate:     near-best window - 3rd-fastest of 15 or more, 2nd-fastest of 5 or more
Revision:     303d801d508b + uncommitted changes b4b1cd1fe72e "Prepare 2.3.0 release"
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
  - [1.14 Dominators](#114-dominators)
  - [1.15 Dominance frontiers](#115-dominance-frontiers)
  - [1.16 Iterated dominance frontier (build + one query per variable)](#116-iterated-dominance-frontier-build-one-query-per-variable)
  - [1.17 Loops](#117-loops)
  - [1.18 Ancestry of the dominator tree](#118-ancestry-of-the-dominator-tree)
  - [1.19 PreorderAncestry of the dominator tree](#119-preorderancestry-of-the-dominator-tree)
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

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.3.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

### 1.1 Build

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.083 ms (1.0×) | 0.083 ms (1.0×) | 0.083 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.083 ms (1.0×) | 0.082 ms (1.0×) | 0.084 ms (1.0×) | 0.084 ms (1.0×) | 0.084 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.092 ms (1.0×) | 0.092 ms (1.0×) | 0.093 ms (1.0×) | 0.093 ms (1.0×) | 0.093 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.164 ms (1.0×) | 0.162 ms (0.9×) | 0.163 ms (1.0×) | 0.163 ms (1.0×) | 0.171 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.597 ms (1.0×) | 1.573 ms (1.0×) | 1.581 ms (1.0×) | 1.650 ms (1.1×) | 1.569 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.572 ms | 1.546 ms | 1.623 ms | 1.614 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.550 ms (1.0×) | 1.588 ms (1.0×) | 1.587 ms (1.0×) | 1.622 ms (1.0×) | 1.600 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.551 ms (1.0×) | 1.553 ms (1.0×) | 1.634 ms (1.0×) | 1.650 ms (1.0×) | 1.606 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (100.0%) | 12 195 op/s (101%) | 12 048 op/s (100.0%) | 12 048 op/s (100.0%) | 12 048 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 12 048 op/s (101%) | 12 195 op/s (102%) | 11 905 op/s (100%) | 11 905 op/s (100%) | 11 905 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 10 870 op/s (101%) | 10 870 op/s (101%) | 10 753 op/s (100%) | 10 753 op/s (100%) | 10 753 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 6 098 op/s (104%) | 6 173 op/s (106%) | 6 135 op/s (105%) | 6 135 op/s (105%) | 5 848 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 626.2 op/s (98.2%) | 635.7 op/s (99.7%) | 632.5 op/s (99.2%) | 606.1 op/s (95.1%) | 637.3 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 636.1 op/s | 646.8 op/s | 616.1 op/s | 619.6 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 645.2 op/s (103%) | 629.7 op/s (101%) | 630.1 op/s (101%) | 616.5 op/s (98.6%) | 625.0 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 644.7 op/s (104%) | 643.9 op/s (103%) | 612.0 op/s (98.3%) | 606.1 op/s (97.3%) | 622.7 op/s |

![1.1.1 Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-1-build-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.686 ms (1.0×) | 0.680 ms (1.0×) | 0.695 ms (1.0×) | 0.696 ms (1.0×) | 0.693 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.687 ms (1.0×) | 0.682 ms (1.0×) | 0.694 ms (1.0×) | 0.698 ms (1.0×) | 0.695 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.093 ms (1.0×) | 1.091 ms (1.0×) | 1.100 ms (1.0×) | 1.111 ms (1.0×) | 1.101 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.249 ms (1.0×) | 2.257 ms (1.0×) | 2.218 ms (1.0×) | 2.274 ms (1.0×) | 2.278 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.27 ms (1.0×) | 16.93 ms (1.0×) | 17.11 ms (1.0×) | 17.48 ms (1.0×) | 17.57 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 16.99 ms | 16.74 ms | 17.28 ms | 17.50 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 16.96 ms (1.0×) | 16.82 ms (1.0×) | 17.11 ms (1.0×) | 17.45 ms (1.0×) | 17.14 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 17.04 ms (1.0×) | 16.06 ms (0.9×) | 17.09 ms (1.0×) | 16.96 ms (1.0×) | 17.10 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 458 op/s (101%) | 1 471 op/s (102%) | 1 439 op/s (99.7%) | 1 437 op/s (99.6%) | 1 443 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1 456 op/s (101%) | 1 466 op/s (102%) | 1 441 op/s (100%) | 1 433 op/s (99.6%) | 1 439 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 914.9 op/s (101%) | 916.6 op/s (101%) | 909.1 op/s (100%) | 900.1 op/s (99.1%) | 908.3 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 444.6 op/s (101%) | 443.1 op/s (101%) | 450.9 op/s (103%) | 439.8 op/s (100%) | 439.0 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.90 op/s (102%) | 59.06 op/s (104%) | 58.44 op/s (103%) | 57.19 op/s (100%) | 56.93 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 58.85 op/s | 59.74 op/s | 57.87 op/s | 57.16 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 58.96 op/s (101%) | 59.46 op/s (102%) | 58.45 op/s (100%) | 57.32 op/s (98.2%) | 58.35 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 58.70 op/s (100%) | 62.28 op/s (106%) | 58.52 op/s (100%) | 58.97 op/s (101%) | 58.48 op/s |

![1.1.2 Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-2-build-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 22.97 ms (0.8×) | 23.63 ms (0.9×) | 24.02 ms (0.9×) | 27.59 ms |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 170.6 ms (1.0×) | 167.8 ms (1.0×) | 181.1 ms (1.1×) | 169.7 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 167.6 ms | 166.8 ms | 169.9 ms | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 173.2 ms (1.0×) | 169.5 ms (1.0×) | 167.1 ms (1.0×) | 166.0 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 43.54 op/s (120%) | 42.31 op/s (117%) | 41.63 op/s (115%) | 36.24 op/s |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 5.862 op/s (99.5%) | 5.959 op/s (101%) | 5.522 op/s (93.7%) | 5.893 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 5.967 op/s | 5.995 op/s | 5.886 op/s | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 5.774 op/s (95.8%) | 5.900 op/s (97.9%) | 5.984 op/s (99.3%) | 6.024 op/s |

![1.1.3 Build @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-3-build-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 248.6 ms (1.0×) | 240.4 ms (1.0×) | 243.3 ms (1.0×) | 242.5 ms |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 2052.8 ms (1.2×) | 1916.6 ms (1.1×) | 2326.6 ms (1.3×) | 1739.0 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 1806.0 ms | 1829.5 ms | 2301.9 ms | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1784.0 ms (0.9×) | 1794.6 ms (0.9×) | 1846.2 ms (0.9×) | 2061.3 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4.023 op/s (97.5%) | 4.160 op/s (101%) | 4.110 op/s (99.7%) | 4.124 op/s |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.487 op/s (84.7%) | 0.522 op/s (90.7%) | 0.430 op/s (74.7%) | 0.575 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.554 op/s | 0.547 op/s | 0.434 op/s | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 0.561 op/s (116%) | 0.557 op/s (115%) | 0.542 op/s (112%) | 0.485 op/s |

![1.1.4 Build @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-4-build-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 2945.8 ms (1.2×) | 2971.7 ms (1.2×) | 2986.8 ms (1.2×) | 2457.0 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.339 op/s (83.4%) | 0.337 op/s (82.7%) | 0.335 op/s (82.3%) | 0.407 op/s |

![1.1.5 Build @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-1-5-build-n-100-000-000.svg)

### 1.2 Out-adjacency traversal (outNodes, no algorithm)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.010 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.017 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.048 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.051 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.053 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 100 000 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 58 824 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 20 833 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 19 608 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 18 868 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.169 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.170 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.404 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.658 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.134 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 0.517 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 0.514 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 917 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 5 882 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2 475 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 520 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 881.8 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1 934 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1 946 op/s |

### 1.3 In-adjacency traversal (inNodes, no algorithm)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.014 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.017 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.050 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.054 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.056 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 58 824 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 20 000 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 18 519 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 17 857 op/s |

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.170 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.171 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.170 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.669 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.238 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 0.538 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 0.540 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 |
|---|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 882 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 5 848 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 5 882 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 495 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 807.8 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1 859 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1 852 op/s |

### 1.4 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.106 ms (1.0×) | 0.113 ms (1.1×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) | 0.106 ms | 0.112 ms (1.1×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.129 ms (1.0×) | 0.134 ms (1.0×) | 0.134 ms (1.0×) | 0.135 ms (1.0×) | 0.134 ms | 0.108 ms (0.8×) | 0.105 ms (0.8×) | 0.106 ms (0.8×) | 0.104 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.271 ms (2.6×) | 0.107 ms (1.0×) | 0.104 ms (1.0×) | 0.105 ms (1.0×) | 0.103 ms | 0.095 ms (0.9×) | 0.086 ms (0.8×) | 0.086 ms (0.8×) | 0.084 ms (0.8×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.245 ms (1.1×) | 0.239 ms (1.0×) | 0.242 ms (1.0×) | 0.245 ms (1.1×) | 0.232 ms | 0.111 ms (0.5×) | 0.112 ms (0.5×) | 0.112 ms (0.5×) | 0.110 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.4×) | 0.539 ms (1.0×) | 0.559 ms (1.0×) | 0.550 ms (1.0×) | 0.533 ms | 0.378 ms (0.7×) | 0.381 ms (0.7×) | 0.386 ms (0.7×) | 0.400 ms (0.8×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms | 0.531 ms | 0.539 ms | 0.576 ms | — | 0.384 ms | 0.373 ms | 0.379 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.563 ms (2.2×) | 0.260 ms (1.0×) | 0.261 ms (1.0×) | 0.261 ms (1.0×) | 0.260 ms | 0.259 ms (1.0×) | 0.260 ms (1.0×) | 0.266 ms (1.0×) | 0.260 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.9×) | 0.338 ms (1.0×) | 0.348 ms (1.0×) | 0.349 ms (1.0×) | 0.342 ms | 0.250 ms (0.7×) | 0.265 ms (0.8×) | 0.264 ms (0.8×) | 0.255 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 434 op/s (100%) | 8 850 op/s (93.8%) | 9 434 op/s (100%) | 9 434 op/s (100%) | 9 434 op/s | 8 929 op/s (94.6%) | 9 434 op/s (100%) | 9 434 op/s (100%) | 9 434 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 752 op/s (104%) | 7 463 op/s (100%) | 7 463 op/s (100%) | 7 407 op/s (99.3%) | 7 463 op/s | 9 259 op/s (124%) | 9 524 op/s (128%) | 9 434 op/s (126%) | 9 615 op/s (129%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 3 690 op/s (38.0%) | 9 346 op/s (96.3%) | 9 615 op/s (99.0%) | 9 524 op/s (98.1%) | 9 709 op/s | 10 526 op/s (108%) | 11 628 op/s (120%) | 11 628 op/s (120%) | 11 905 op/s (123%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 082 op/s (94.7%) | 4 184 op/s (97.1%) | 4 132 op/s (95.9%) | 4 082 op/s (94.7%) | 4 310 op/s | 9 009 op/s (209%) | 8 929 op/s (207%) | 8 929 op/s (207%) | 9 091 op/s (211%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (73.0%) | 1 855 op/s (98.9%) | 1 789 op/s (95.3%) | 1 818 op/s (96.9%) | 1 876 op/s | 2 646 op/s (141%) | 2 625 op/s (140%) | 2 591 op/s (138%) | 2 500 op/s (133%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s | 1 883 op/s | 1 855 op/s | 1 736 op/s | — | 2 604 op/s | 2 681 op/s | 2 639 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 776 op/s (46.2%) | 3 846 op/s (100%) | 3 831 op/s (99.6%) | 3 831 op/s (99.6%) | 3 846 op/s | 3 861 op/s (100%) | 3 846 op/s (100%) | 3 759 op/s (97.7%) | 3 846 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (53.5%) | 2 959 op/s (101%) | 2 874 op/s (98.3%) | 2 865 op/s (98.0%) | 2 924 op/s | 4 000 op/s (137%) | 3 774 op/s (129%) | 3 788 op/s (130%) | 3 922 op/s (134%) |

![1.4.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 1.130 ms (1.0×) | 1.030 ms (0.9×) | 1.021 ms (0.9×) | 1.118 ms | 1.117 ms (1.0×) | 1.025 ms (0.9×) | 1.030 ms (0.9×) | 1.027 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.415 ms (1.1×) | 2.192 ms (1.0×) | 2.146 ms (0.9×) | 2.236 ms (1.0×) | 2.279 ms | 1.038 ms (0.5×) | 1.035 ms (0.5×) | 1.052 ms (0.5×) | 1.040 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 11.69 ms (5.4×) | 2.240 ms (1.0×) | 2.122 ms (1.0×) | 2.153 ms (1.0×) | 2.159 ms | 1.413 ms (0.7×) | 1.305 ms (0.6×) | 1.303 ms (0.6×) | 1.285 ms (0.6×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.872 ms (1.1×) | 4.329 ms (1.0×) | 4.447 ms (1.0×) | 4.617 ms (1.0×) | 4.400 ms | 2.247 ms (0.5×) | 2.265 ms (0.5×) | 2.220 ms (0.5×) | 2.152 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.957 ms (1.3×) | 7.389 ms (1.0×) | 7.397 ms (1.0×) | 8.010 ms (1.0×) | 7.751 ms | 4.475 ms (0.6×) | 4.431 ms (0.6×) | 4.693 ms (0.6×) | 4.474 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 7.595 ms | 7.548 ms | 8.094 ms | — | 4.423 ms | 4.537 ms | 4.517 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.702 ms (2.2×) | 2.623 ms (1.0×) | 2.606 ms (1.0×) | 2.509 ms (1.0×) | 2.614 ms | 2.625 ms (1.0×) | 2.598 ms (1.0×) | 2.645 ms (1.0×) | 2.616 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 7.381 ms (1.5×) | 4.697 ms (0.9×) | 4.820 ms (1.0×) | 4.904 ms (1.0×) | 5.033 ms | 2.542 ms (0.5×) | 2.596 ms (0.5×) | 2.789 ms (0.6×) | 2.732 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 885.0 op/s (98.9%) | 970.9 op/s (109%) | 979.4 op/s (110%) | 894.5 op/s | 895.3 op/s (100%) | 975.6 op/s (109%) | 970.9 op/s (109%) | 973.7 op/s (109%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 414.1 op/s (94.4%) | 456.2 op/s (104%) | 466.0 op/s (106%) | 447.2 op/s (102%) | 438.8 op/s | 963.4 op/s (220%) | 966.2 op/s (220%) | 950.6 op/s (217%) | 961.5 op/s (219%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 85.56 op/s (18.5%) | 446.4 op/s (96.4%) | 471.3 op/s (102%) | 464.5 op/s (100%) | 463.2 op/s | 707.7 op/s (153%) | 766.3 op/s (165%) | 767.5 op/s (166%) | 778.2 op/s (168%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 205.3 op/s (90.3%) | 231.0 op/s (102%) | 224.9 op/s (98.9%) | 216.6 op/s (95.3%) | 227.3 op/s | 445.0 op/s (196%) | 441.5 op/s (194%) | 450.5 op/s (198%) | 464.7 op/s (204%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 100.4 op/s (77.8%) | 135.3 op/s (105%) | 135.2 op/s (105%) | 124.8 op/s (96.8%) | 129.0 op/s | 223.5 op/s (173%) | 225.7 op/s (175%) | 213.1 op/s (165%) | 223.5 op/s (173%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 131.7 op/s | 132.5 op/s | 123.5 op/s | — | 226.1 op/s | 220.4 op/s | 221.4 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 175.4 op/s (45.8%) | 381.2 op/s (99.7%) | 383.7 op/s (100%) | 398.6 op/s (104%) | 382.6 op/s | 381.0 op/s (99.6%) | 384.9 op/s (101%) | 378.1 op/s (98.8%) | 382.3 op/s (99.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 135.5 op/s (68.2%) | 212.9 op/s (107%) | 207.5 op/s (104%) | 203.9 op/s (103%) | 198.7 op/s | 393.4 op/s (198%) | 385.2 op/s (194%) | 358.6 op/s (180%) | 366.0 op/s (184%) |

![1.4.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 77.49 ms (0.9×) | 76.73 ms (0.9×) | 86.77 ms (1.0×) | 83.52 ms | 26.75 ms (0.3×) | 26.04 ms (0.3×) | 25.87 ms (0.3×) | 26.19 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 238.7 ms (1.0×) | 233.4 ms (1.0×) | 246.1 ms (1.0×) | 239.7 ms | — | 61.63 ms (0.3×) | 61.93 ms (0.3×) | 61.88 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 232.9 ms | 229.7 ms | 248.1 ms | — | 66.11 ms | 62.45 ms | 62.57 ms | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.36 ms (1.0×) | 26.24 ms (1.0×) | 26.51 ms (1.0×) | 26.41 ms | — | 26.27 ms (1.0×) | 26.54 ms (1.0×) | 26.47 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 12.91 op/s (108%) | 13.03 op/s (109%) | 11.52 op/s (96.2%) | 11.97 op/s | 37.38 op/s (312%) | 38.41 op/s (321%) | 38.65 op/s (323%) | 38.19 op/s (319%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.189 op/s (100%) | 4.284 op/s (103%) | 4.063 op/s (97.4%) | 4.172 op/s | — | 16.23 op/s (389%) | 16.15 op/s (387%) | 16.16 op/s (387%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.294 op/s | 4.354 op/s | 4.031 op/s | — | 15.13 op/s | 16.01 op/s | 15.98 op/s | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 39.43 op/s (104%) | 38.10 op/s (101%) | 37.72 op/s (99.6%) | 37.87 op/s | — | 38.06 op/s (101%) | 37.67 op/s (99.5%) | 37.78 op/s (99.8%) |

![1.4.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3085.1 ms (1.0×) | 3051.6 ms (1.0×) | 3120.3 ms (1.0×) | 3055.4 ms | 739.3 ms (0.2×) | 700.8 ms (0.2×) | 757.2 ms (0.2×) | 716.1 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6301.4 ms (0.9×) | 6518.8 ms (0.9×) | 6783.8 ms (1.0×) | 6885.2 ms | — | 2978.2 ms (0.4×) | 3038.0 ms (0.4×) | 2873.4 ms (0.4×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 6297.6 ms | 6583.3 ms | 6754.9 ms | — | 2740.8 ms | 2700.6 ms | 3578.0 ms | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 268.4 ms (0.9×) | 275.9 ms (1.0×) | 256.3 ms (0.9×) | 283.3 ms | — | 271.5 ms (1.0×) | 274.0 ms (1.0×) | 281.5 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.324 op/s (99.0%) | 0.328 op/s (100%) | 0.320 op/s (97.9%) | 0.327 op/s | 1.353 op/s (413%) | 1.427 op/s (436%) | 1.321 op/s (404%) | 1.396 op/s (427%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (109%) | 0.153 op/s (106%) | 0.147 op/s (101%) | 0.145 op/s | — | 0.336 op/s (231%) | 0.329 op/s (227%) | 0.348 op/s (240%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s | 0.152 op/s | 0.148 op/s | — | 0.365 op/s | 0.370 op/s | 0.279 op/s | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.726 op/s (106%) | 3.625 op/s (103%) | 3.902 op/s (111%) | 3.530 op/s | — | 3.683 op/s (104%) | 3.650 op/s (103%) | 3.552 op/s (101%) |

![1.4.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 36814 ms (1.0×) | 36265 ms (1.0×) | 36545 ms (1.0×) | 36155 ms | 9963.5 ms (0.3×) | 9839.3 ms (0.3×) | 9918.4 ms (0.3×) | 9842.1 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.027 op/s (98.2%) | 0.028 op/s (99.7%) | 0.027 op/s (98.9%) | 0.028 op/s | 0.100 op/s (363%) | 0.102 op/s (367%) | 0.101 op/s (365%) | 0.102 op/s (367%) |

![1.4.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-5-scc-n-100-000-000.svg)

### 1.5 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (0.9×) | 0.036 ms (0.9×) | 0.037 ms (0.9×) | 0.036 ms (0.9×) | 0.040 ms | 0.042 ms (1.1×) | 0.040 ms (1.0×) | 0.042 ms (1.1×) | 0.036 ms (0.9×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.046 ms (0.8×) | 0.046 ms (0.8×) | 0.053 ms (0.9×) | 0.046 ms (0.8×) | 0.056 ms | 0.042 ms (0.8×) | 0.042 ms (0.8×) | 0.042 ms (0.8×) | 0.042 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.038 ms (1.0×) | 0.038 ms (1.0×) | 0.038 ms (1.0×) | 0.038 ms (1.0×) | 0.038 ms | 0.038 ms (1.0×) | 0.044 ms (1.2×) | 0.039 ms (1.0×) | 0.039 ms (1.0×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.095 ms (1.0×) | 0.083 ms (0.9×) | 0.097 ms (1.0×) | 0.102 ms (1.1×) | 0.095 ms | 0.086 ms (0.9×) | 0.082 ms (0.9×) | 0.081 ms (0.9×) | 0.089 ms (0.9×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.530 ms (1.0×) | 0.524 ms (1.0×) | 0.523 ms (1.0×) | 0.531 ms (1.0×) | 0.513 ms | 0.524 ms (1.0×) | 0.520 ms (1.0×) | 0.537 ms (1.0×) | 0.512 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms | 0.524 ms | 0.525 ms | 0.528 ms | — | 0.527 ms | 0.521 ms | 0.525 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.579 ms (1.0×) | 0.581 ms (1.0×) | 0.572 ms | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.589 ms (1.0×) | 0.571 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.640 ms (1.0×) | 0.640 ms (1.0×) | 0.641 ms (1.0×) | 0.648 ms (1.0×) | 0.633 ms | 0.581 ms (0.9×) | 0.614 ms (1.0×) | 0.620 ms (1.0×) | 0.572 ms (0.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (108%) | 27 778 op/s (111%) | 27 027 op/s (108%) | 27 778 op/s (111%) | 25 000 op/s | 23 810 op/s (95.2%) | 25 000 op/s (100%) | 23 810 op/s (95.2%) | 27 778 op/s (111%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 739 op/s (122%) | 21 739 op/s (122%) | 18 868 op/s (106%) | 21 739 op/s (122%) | 17 857 op/s | 23 810 op/s (133%) | 23 810 op/s (133%) | 23 810 op/s (133%) | 23 810 op/s (133%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s (100%) | 26 316 op/s (100%) | 26 316 op/s (100%) | 26 316 op/s (100%) | 26 316 op/s | 26 316 op/s (100%) | 22 727 op/s (86.4%) | 25 641 op/s (97.4%) | 25 641 op/s (97.4%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 10 526 op/s (100%) | 12 048 op/s (114%) | 10 309 op/s (97.9%) | 9 804 op/s (93.1%) | 10 526 op/s | 11 628 op/s (110%) | 12 195 op/s (116%) | 12 346 op/s (117%) | 11 236 op/s (107%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 887 op/s (96.8%) | 1 908 op/s (97.9%) | 1 912 op/s (98.1%) | 1 883 op/s (96.6%) | 1 949 op/s | 1 908 op/s (97.9%) | 1 923 op/s (98.7%) | 1 862 op/s (95.5%) | 1 953 op/s (100%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s | 1 908 op/s | 1 905 op/s | 1 894 op/s | — | 1 898 op/s | 1 919 op/s | 1 905 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 730 op/s (99.0%) | 1 730 op/s (99.0%) | 1 727 op/s (98.8%) | 1 721 op/s (98.5%) | 1 748 op/s | 1 730 op/s (99.0%) | 1 730 op/s (99.0%) | 1 698 op/s (97.1%) | 1 751 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 562 op/s (98.9%) | 1 562 op/s (98.9%) | 1 560 op/s (98.8%) | 1 543 op/s (97.7%) | 1 580 op/s | 1 721 op/s (109%) | 1 629 op/s (103%) | 1 613 op/s (102%) | 1 748 op/s (111%) |

![1.5.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.393 ms (1.0×) | 0.398 ms (1.0×) | 0.406 ms (1.0×) | 0.395 ms (1.0×) | 0.395 ms | 0.400 ms (1.0×) | 0.352 ms (0.9×) | 0.357 ms (0.9×) | 0.400 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.228 ms (1.0×) | 1.232 ms (1.0×) | 1.291 ms (1.1×) | 1.228 ms | 0.390 ms (0.3×) | 0.347 ms (0.3×) | 0.356 ms (0.3×) | 0.392 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.661 ms (1.0×) | 0.636 ms (1.0×) | 0.669 ms (1.1×) | 0.665 ms (1.1×) | 0.632 ms | 0.732 ms (1.2×) | 0.770 ms (1.2×) | 0.732 ms (1.2×) | 0.713 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.769 ms (1.0×) | 1.767 ms (1.0×) | 1.794 ms (1.0×) | 1.800 ms (1.0×) | 1.772 ms | 1.550 ms (0.9×) | 1.561 ms (0.9×) | 1.572 ms (0.9×) | 1.525 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.082 ms (1.0×) | 6.090 ms (1.0×) | 6.096 ms (1.0×) | 6.096 ms (1.0×) | 6.078 ms | 6.069 ms (1.0×) | 6.054 ms (1.0×) | 6.263 ms (1.0×) | 6.004 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.163 ms | 6.091 ms | 6.088 ms | 6.158 ms | — | 6.163 ms | 6.118 ms | 6.194 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.858 ms (1.0×) | 6.000 ms (1.0×) | 5.778 ms (1.0×) | 6.479 ms (1.1×) | 5.791 ms | 5.927 ms (1.0×) | 5.764 ms (1.0×) | 6.018 ms (1.0×) | 5.672 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.955 ms (1.0×) | 5.953 ms (1.0×) | 5.988 ms (1.0×) | 6.054 ms (1.0×) | 5.883 ms | 6.021 ms (1.0×) | 6.133 ms (1.0×) | 5.936 ms (1.0×) | 5.888 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 545 op/s (101%) | 2 513 op/s (99.2%) | 2 463 op/s (97.3%) | 2 532 op/s (100%) | 2 532 op/s | 2 500 op/s (98.8%) | 2 841 op/s (112%) | 2 801 op/s (111%) | 2 500 op/s (98.8%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (99.9%) | 814.3 op/s (100%) | 811.7 op/s (99.7%) | 774.6 op/s (95.1%) | 814.3 op/s | 2 564 op/s (315%) | 2 882 op/s (354%) | 2 809 op/s (345%) | 2 551 op/s (313%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 513 op/s (95.6%) | 1 572 op/s (99.4%) | 1 495 op/s (94.5%) | 1 504 op/s (95.0%) | 1 582 op/s | 1 366 op/s (86.3%) | 1 299 op/s (82.1%) | 1 366 op/s (86.3%) | 1 403 op/s (88.6%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 565.3 op/s (100%) | 565.9 op/s (100%) | 557.4 op/s (98.8%) | 555.6 op/s (98.4%) | 564.3 op/s | 645.2 op/s (114%) | 640.6 op/s (114%) | 636.1 op/s (113%) | 655.7 op/s (116%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 164.4 op/s (99.9%) | 164.2 op/s (99.8%) | 164.0 op/s (99.7%) | 164.0 op/s (99.7%) | 164.5 op/s | 164.8 op/s (100%) | 165.2 op/s (100%) | 159.7 op/s (97.0%) | 166.6 op/s (101%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.3 op/s | 164.2 op/s | 164.3 op/s | 162.4 op/s | — | 162.3 op/s | 163.5 op/s | 161.4 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 170.7 op/s (98.9%) | 166.7 op/s (96.5%) | 173.1 op/s (100%) | 154.3 op/s (89.4%) | 172.7 op/s | 168.7 op/s (97.7%) | 173.5 op/s (100%) | 166.2 op/s (96.2%) | 176.3 op/s (102%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 167.9 op/s (98.8%) | 168.0 op/s (98.8%) | 167.0 op/s (98.2%) | 165.2 op/s (97.2%) | 170.0 op/s | 166.1 op/s (97.7%) | 163.1 op/s (95.9%) | 168.5 op/s (99.1%) | 169.8 op/s (99.9%) |

![1.5.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.74 ms (1.0×) | 21.57 ms (1.0×) | 19.97 ms (0.9×) | 21.40 ms | 17.26 ms (0.8×) | 16.24 ms (0.8×) | 16.39 ms (0.8×) | 17.45 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 65.50 ms (1.0×) | 78.43 ms (1.2×) | 67.53 ms (1.0×) | 67.67 ms | — | 66.27 ms (1.0×) | 69.74 ms (1.0×) | 66.93 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 70.47 ms | 66.99 ms | 68.44 ms | — | 69.61 ms | 66.64 ms | 68.78 ms | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.95 ms (1.0×) | 51.65 ms (1.0×) | 50.92 ms (1.0×) | 50.41 ms | — | 50.10 ms (1.0×) | 50.64 ms (1.0×) | 50.15 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.00 op/s (98.4%) | 46.37 op/s (99.2%) | 50.08 op/s (107%) | 46.73 op/s | 57.94 op/s (124%) | 61.58 op/s (132%) | 61.02 op/s (131%) | 57.29 op/s (123%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 15.27 op/s (103%) | 12.75 op/s (86.3%) | 14.81 op/s (100%) | 14.78 op/s | — | 15.09 op/s (102%) | 14.34 op/s (97.0%) | 14.94 op/s (101%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.19 op/s | 14.93 op/s | 14.61 op/s | — | 14.37 op/s | 15.01 op/s | 14.54 op/s | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.63 op/s (98.9%) | 19.36 op/s (97.6%) | 19.64 op/s (99.0%) | 19.84 op/s | — | 19.96 op/s (101%) | 19.75 op/s (99.6%) | 19.94 op/s (101%) |

![1.5.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 615.6 ms (1.0×) | 600.5 ms (1.0×) | 569.5 ms (1.0×) | 590.8 ms | 410.6 ms (0.7×) | 378.4 ms (0.6×) | 447.4 ms (0.8×) | 406.0 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4094.7 ms (1.2×) | 4106.6 ms (1.2×) | 4580.5 ms (1.3×) | 3441.2 ms | — | 3774.2 ms (1.1×) | 4418.3 ms (1.3×) | 3251.8 ms (0.9×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3899.0 ms | 3919.9 ms | 3734.0 ms | — | 3844.9 ms | 3828.2 ms | 4399.7 ms | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 754.7 ms (1.5×) | 516.1 ms (1.0×) | 594.1 ms (1.1×) | 518.9 ms | — | 516.0 ms (1.0×) | 561.6 ms (1.1×) | 521.1 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.624 op/s (96.0%) | 1.665 op/s (98.4%) | 1.756 op/s (104%) | 1.693 op/s | 2.435 op/s (144%) | 2.643 op/s (156%) | 2.235 op/s (132%) | 2.463 op/s (146%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.244 op/s (84.0%) | 0.244 op/s (83.8%) | 0.218 op/s (75.1%) | 0.291 op/s | — | 0.265 op/s (91.2%) | 0.226 op/s (77.9%) | 0.308 op/s (106%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s | 0.255 op/s | 0.268 op/s | — | 0.260 op/s | 0.261 op/s | 0.227 op/s | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.325 op/s (68.8%) | 1.938 op/s (101%) | 1.683 op/s (87.3%) | 1.927 op/s | — | 1.938 op/s (101%) | 1.781 op/s (92.4%) | 1.919 op/s (99.6%) |

![1.5.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8231.6 ms (0.9×) | 9442.8 ms (1.1×) | 9600.9 ms (1.1×) | 8807.1 ms | 6807.7 ms (0.8×) | 6812.3 ms (0.8×) | 6010.4 ms (0.7×) | 7245.4 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.121 op/s (107%) | 0.106 op/s (93.3%) | 0.104 op/s (91.7%) | 0.114 op/s | 0.147 op/s (129%) | 0.147 op/s (129%) | 0.166 op/s (147%) | 0.138 op/s (122%) |

![1.5.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-5-wcc-n-100-000-000.svg)

### 1.6 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (0.9×) | 0.042 ms (1.0×) | 0.042 ms (1.0×) | 0.043 ms (1.0×) | 0.042 ms | 0.036 ms (0.9×) | 0.042 ms (1.0×) | 0.037 ms (0.9×) | 0.036 ms (0.9×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.053 ms (1.2×) | 0.053 ms (1.2×) | 0.045 ms (1.0×) | 0.058 ms (1.3×) | 0.045 ms | 0.036 ms (0.8×) | 0.042 ms (0.9×) | 0.036 ms (0.8×) | 0.041 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.044 ms (1.0×) | 0.038 ms (0.9×) | 0.042 ms (1.0×) | 0.039 ms (0.9×) | 0.044 ms | 0.043 ms (1.0×) | 0.038 ms (0.9×) | 0.044 ms (1.0×) | 0.038 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.106 ms (1.3×) | 0.090 ms (1.1×) | 0.094 ms (1.1×) | 0.110 ms (1.3×) | 0.082 ms | 0.088 ms (1.1×) | 0.087 ms (1.1×) | 0.087 ms (1.1×) | 0.089 ms (1.1×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.518 ms (1.0×) | 0.526 ms (1.0×) | 0.524 ms (1.0×) | 0.530 ms (1.0×) | 0.516 ms | 0.520 ms (1.0×) | 0.527 ms (1.0×) | 0.530 ms (1.0×) | 0.512 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.522 ms | 0.524 ms | 0.524 ms | 0.535 ms | — | 0.534 ms | 0.521 ms | 0.535 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.586 ms (1.0×) | 0.580 ms (1.0×) | 0.588 ms (1.0×) | 0.586 ms (1.0×) | 0.582 ms | 0.584 ms (1.0×) | 0.588 ms (1.0×) | 0.556 ms (1.0×) | 0.569 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.0×) | 0.640 ms (1.0×) | 0.643 ms (1.0×) | 0.648 ms (1.0×) | 0.632 ms | 0.625 ms (1.0×) | 0.611 ms (1.0×) | 0.625 ms (1.0×) | 0.607 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (114%) | 23 810 op/s (100%) | 23 810 op/s (100%) | 23 256 op/s (97.7%) | 23 810 op/s | 27 778 op/s (117%) | 23 810 op/s (100%) | 27 027 op/s (114%) | 27 778 op/s (117%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s (84.9%) | 18 868 op/s (84.9%) | 22 222 op/s (100%) | 17 241 op/s (77.6%) | 22 222 op/s | 27 778 op/s (125%) | 23 810 op/s (107%) | 27 778 op/s (125%) | 24 390 op/s (110%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 22 727 op/s (100%) | 26 316 op/s (116%) | 23 810 op/s (105%) | 25 641 op/s (113%) | 22 727 op/s | 23 256 op/s (102%) | 26 316 op/s (116%) | 22 727 op/s (100%) | 26 316 op/s (116%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 9 434 op/s (77.4%) | 11 111 op/s (91.1%) | 10 638 op/s (87.2%) | 9 091 op/s (74.5%) | 12 195 op/s | 11 364 op/s (93.2%) | 11 494 op/s (94.3%) | 11 494 op/s (94.3%) | 11 236 op/s (92.1%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 931 op/s (99.6%) | 1 901 op/s (98.1%) | 1 908 op/s (98.5%) | 1 887 op/s (97.4%) | 1 938 op/s | 1 923 op/s (99.2%) | 1 898 op/s (97.9%) | 1 887 op/s (97.4%) | 1 953 op/s (101%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 916 op/s | 1 908 op/s | 1 908 op/s | 1 869 op/s | — | 1 873 op/s | 1 919 op/s | 1 869 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 706 op/s (99.3%) | 1 724 op/s (100%) | 1 701 op/s (99.0%) | 1 706 op/s (99.3%) | 1 718 op/s | 1 712 op/s (99.7%) | 1 701 op/s (99.0%) | 1 799 op/s (105%) | 1 757 op/s (102%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (98.9%) | 1 562 op/s (98.8%) | 1 555 op/s (98.3%) | 1 543 op/s (97.5%) | 1 582 op/s | 1 600 op/s (101%) | 1 637 op/s (103%) | 1 600 op/s (101%) | 1 647 op/s (104%) |

![1.6.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.396 ms (1.1×) | 0.403 ms (1.1×) | 0.394 ms (1.1×) | 0.398 ms (1.1×) | 0.359 ms | 0.392 ms (1.1×) | 0.394 ms (1.1×) | 0.399 ms (1.1×) | 0.359 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (0.9×) | 1.326 ms (1.0×) | 1.232 ms (0.9×) | 1.276 ms (1.0×) | 1.314 ms | 0.369 ms (0.3×) | 0.365 ms (0.3×) | 0.403 ms (0.3×) | 0.362 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.665 ms (1.0×) | 0.649 ms (1.0×) | 0.664 ms (1.0×) | 0.644 ms (1.0×) | 0.664 ms | 0.728 ms (1.1×) | 0.751 ms (1.1×) | 0.720 ms (1.1×) | 0.747 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.762 ms (1.0×) | 1.769 ms (1.0×) | 1.763 ms (1.0×) | 1.836 ms (1.0×) | 1.756 ms | 1.501 ms (0.9×) | 1.542 ms (0.9×) | 1.515 ms (0.9×) | 1.559 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.173 ms (1.0×) | 6.095 ms (1.0×) | 6.117 ms (1.0×) | 6.177 ms (1.0×) | 6.012 ms | 6.057 ms (1.0×) | 6.066 ms (1.0×) | 6.129 ms (1.0×) | 5.998 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.157 ms | 6.186 ms | 6.175 ms | 6.303 ms | — | 6.029 ms | 6.032 ms | 6.203 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.806 ms (1.0×) | 5.871 ms (1.0×) | 5.816 ms (1.0×) | 5.949 ms (1.0×) | 5.678 ms | 5.856 ms (1.0×) | 5.781 ms (1.0×) | 5.961 ms (1.0×) | 5.683 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.999 ms (1.0×) | 5.949 ms (1.0×) | 5.935 ms (1.0×) | 6.078 ms (1.0×) | 5.926 ms | 6.055 ms (1.0×) | 6.115 ms (1.0×) | 6.133 ms (1.0×) | 6.068 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 525 op/s (90.7%) | 2 481 op/s (89.1%) | 2 538 op/s (91.1%) | 2 513 op/s (90.2%) | 2 786 op/s | 2 551 op/s (91.6%) | 2 538 op/s (91.1%) | 2 506 op/s (90.0%) | 2 786 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (107%) | 754.1 op/s (99.1%) | 811.7 op/s (107%) | 783.7 op/s (103%) | 761.0 op/s | 2 710 op/s (356%) | 2 740 op/s (360%) | 2 481 op/s (326%) | 2 762 op/s (363%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 504 op/s (99.8%) | 1 541 op/s (102%) | 1 506 op/s (100.0%) | 1 553 op/s (103%) | 1 506 op/s | 1 374 op/s (91.2%) | 1 332 op/s (88.4%) | 1 389 op/s (92.2%) | 1 339 op/s (88.9%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 567.5 op/s (99.7%) | 565.3 op/s (99.3%) | 567.2 op/s (99.6%) | 544.7 op/s (95.6%) | 569.5 op/s | 666.2 op/s (117%) | 648.5 op/s (114%) | 660.1 op/s (116%) | 641.4 op/s (113%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 162.0 op/s (97.4%) | 164.1 op/s (98.6%) | 163.5 op/s (98.3%) | 161.9 op/s (97.3%) | 166.3 op/s | 165.1 op/s (99.3%) | 164.9 op/s (99.1%) | 163.2 op/s (98.1%) | 166.7 op/s (100%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.4 op/s | 161.7 op/s | 161.9 op/s | 158.7 op/s | — | 165.9 op/s | 165.8 op/s | 161.2 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 172.2 op/s (97.8%) | 170.3 op/s (96.7%) | 171.9 op/s (97.6%) | 168.1 op/s (95.4%) | 176.1 op/s | 170.8 op/s (97.0%) | 173.0 op/s (98.2%) | 167.8 op/s (95.3%) | 176.0 op/s (99.9%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 166.7 op/s (98.8%) | 168.1 op/s (99.6%) | 168.5 op/s (99.8%) | 164.5 op/s (97.5%) | 168.7 op/s | 165.2 op/s (97.9%) | 163.5 op/s (96.9%) | 163.1 op/s (96.6%) | 164.8 op/s (97.7%) |

![1.6.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 19.83 ms (0.9×) | 19.65 ms (0.9×) | 20.46 ms (0.9×) | 22.02 ms | 17.28 ms (0.8×) | 17.28 ms (0.8×) | 17.11 ms (0.8×) | 16.34 ms (0.7×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 79.11 ms (1.2×) | 67.00 ms (1.0×) | 67.57 ms (1.0×) | 68.75 ms | — | 69.08 ms (1.0×) | 67.67 ms (1.0×) | 64.90 ms (0.9×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 67.63 ms | 66.92 ms | 71.04 ms | — | 66.60 ms | 67.00 ms | 69.03 ms | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.35 ms (1.0×) | 50.47 ms (1.0×) | 50.58 ms (1.0×) | 49.23 ms | — | 50.11 ms (1.0×) | 50.61 ms (1.0×) | 50.53 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 50.44 op/s (111%) | 50.88 op/s (112%) | 48.87 op/s (108%) | 45.41 op/s | 57.86 op/s (127%) | 57.86 op/s (127%) | 58.46 op/s (129%) | 61.19 op/s (135%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 12.64 op/s (86.9%) | 14.93 op/s (103%) | 14.80 op/s (102%) | 14.55 op/s | — | 14.48 op/s (99.5%) | 14.78 op/s (102%) | 15.41 op/s (106%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.79 op/s | 14.94 op/s | 14.08 op/s | — | 15.02 op/s | 14.92 op/s | 14.49 op/s | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.86 op/s (97.8%) | 19.82 op/s (97.5%) | 19.77 op/s (97.3%) | 20.31 op/s | — | 19.96 op/s (98.2%) | 19.76 op/s (97.3%) | 19.79 op/s (97.4%) |

![1.6.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 576.6 ms (1.0×) | 602.9 ms (1.0×) | 615.4 ms (1.0×) | 589.4 ms | 410.4 ms (0.7×) | 409.6 ms (0.7×) | 445.8 ms (0.8×) | 405.6 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4122.2 ms (1.2×) | 4102.7 ms (1.2×) | 4620.4 ms (1.4×) | 3362.6 ms | — | 3844.3 ms (1.1×) | 4435.6 ms (1.3×) | 3218.0 ms (1.0×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3909.4 ms | 3884.4 ms | 4689.5 ms | — | 4002.5 ms | 3959.5 ms | 4352.3 ms | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 569.9 ms (1.1×) | 550.5 ms (1.1×) | 598.6 ms (1.2×) | 518.4 ms | — | 595.1 ms (1.1×) | 557.8 ms (1.1×) | 517.2 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.734 op/s (102%) | 1.659 op/s (97.8%) | 1.625 op/s (95.8%) | 1.697 op/s | 2.437 op/s (144%) | 2.441 op/s (144%) | 2.243 op/s (132%) | 2.465 op/s (145%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.243 op/s (81.6%) | 0.244 op/s (82.0%) | 0.216 op/s (72.8%) | 0.297 op/s | — | 0.260 op/s (87.5%) | 0.225 op/s (75.8%) | 0.311 op/s (104%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s | 0.257 op/s | 0.213 op/s | — | 0.250 op/s | 0.253 op/s | 0.230 op/s | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.755 op/s (91.0%) | 1.817 op/s (94.2%) | 1.671 op/s (86.6%) | 1.929 op/s | — | 1.680 op/s (87.1%) | 1.793 op/s (92.9%) | 1.933 op/s (100%) |

![1.6.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8479.6 ms (1.0×) | 8563.2 ms (1.0×) | 8594.8 ms (1.0×) | 8738.0 ms | 6840.6 ms (0.8×) | 6788.3 ms (0.8×) | 6004.0 ms (0.7×) | 7211.5 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.118 op/s (103%) | 0.117 op/s (102%) | 0.116 op/s (102%) | 0.114 op/s | 0.146 op/s (128%) | 0.147 op/s (129%) | 0.167 op/s (146%) | 0.139 op/s (121%) |

![1.6.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-5-ccs-n-100-000-000.svg)

### 1.7 Condense

#### Times @ 1k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 0.011 ms (1.1×) | 0.011 ms (1.1×) | 0.010 ms |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 0.007 ms (1.0×) | 0.008 ms (1.1×) | 0.007 ms |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 0.024 ms (1.1×) | 0.024 ms (1.1×) | 0.021 ms |
| DAG GNP sparse | 0.017 ms (1.1×) | 0.017 ms (1.1×) | 0.016 ms |

#### Throughput @ 1k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 90 909 op/s (90.9%) | 90 909 op/s (90.9%) | 100 000 op/s |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 142 857 op/s (100%) | 125 000 op/s (87.5%) | 142 857 op/s |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 41 667 op/s (87.5%) | 41 667 op/s (87.5%) | 47 619 op/s |
| DAG GNP sparse | 58 824 op/s (94.1%) | 58 824 op/s (94.1%) | 62 500 op/s |

![1.7.1 Condense @ n = 1 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-1-condense-n-1-000.svg)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.080 ms (1.0×) | 0.080 ms (1.0×) | 0.080 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.099 ms (1.0×) | 0.092 ms (0.9×) | 0.098 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.182 ms (0.9×) | 0.185 ms (0.9×) | 0.200 ms |
| DAG GNP sparse | 0.256 ms (1.0×) | 0.261 ms (1.0×) | 0.261 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 500 op/s (100%) | 12 500 op/s (100%) | 12 500 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 10 101 op/s (99.0%) | 10 870 op/s (107%) | 10 204 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 5 495 op/s (110%) | 5 405 op/s (108%) | 5 000 op/s |
| DAG GNP sparse | 3 906 op/s (102%) | 3 831 op/s (100%) | 3 831 op/s |

![1.7.2 Condense @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-2-condense-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.772 ms (1.0×) | 0.768 ms (1.0×) | 0.764 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.523 ms (1.0×) | 1.591 ms (1.0×) | 1.572 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1.722 ms (0.9×) | 1.852 ms (1.0×) | 1.908 ms |
| DAG GNP sparse | 3.542 ms (1.0×) | 3.641 ms (1.0×) | 3.590 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 295 op/s (99.0%) | 1 302 op/s (99.5%) | 1 309 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 656.6 op/s (103%) | 628.5 op/s (98.8%) | 636.1 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 580.7 op/s (111%) | 540.0 op/s (103%) | 524.1 op/s |
| DAG GNP sparse | 282.3 op/s (101%) | 274.6 op/s (98.6%) | 278.6 op/s |

![1.7.3 Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-3-condense-n-100-000.svg)

### 1.8 TopOrder

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.1×) | 0.058 ms (0.9×) | 0.067 ms (1.1×) | 0.063 ms | 0.067 ms (1.1×) | 0.066 ms (1.0×) | 0.067 ms (1.1×) | 0.066 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.099 ms (1.4×) | 0.072 ms (1.0×) | 0.070 ms (1.0×) | 0.070 ms (1.0×) | 0.070 ms | 0.056 ms (0.8×) | 0.052 ms (0.7×) | 0.052 ms (0.7×) | 0.052 ms (0.7×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.198 ms (3.6×) | 0.044 ms (0.8×) | 0.056 ms (1.0×) | 0.049 ms (0.9×) | 0.055 ms | 0.039 ms (0.7×) | 0.053 ms (1.0×) | 0.037 ms (0.7×) | 0.044 ms (0.8×) |
| Tree shfld. | 0.088 ms (1.8×) | 0.052 ms (1.0×) | 0.050 ms (1.0×) | 0.050 ms (1.0×) | 0.050 ms | 0.047 ms (0.9×) | 0.047 ms (0.9×) | 0.047 ms (0.9×) | 0.046 ms (0.9×) |
| DAG GNP sparse | 0.279 ms (3.6×) | 0.070 ms (0.9×) | 0.084 ms (1.1×) | 0.074 ms (0.9×) | 0.078 ms | 0.061 ms (0.8×) | 0.067 ms (0.9×) | 0.064 ms (0.8×) | 0.064 ms (0.8×) |
| DAG GNP dense | 0.448 ms (1.4×) | 0.337 ms (1.0×) | 0.324 ms (1.0×) | 0.324 ms (1.0×) | 0.326 ms | 0.245 ms (0.8×) | 0.243 ms (0.7×) | 0.245 ms (0.8×) | 0.239 ms (0.7×) |
| DAG GNP dense shfld. | 0.560 ms (1.5×) | 0.387 ms (1.0×) | 0.371 ms (1.0×) | 0.366 ms (1.0×) | 0.382 ms | 0.244 ms (0.6×) | 0.233 ms (0.6×) | 0.246 ms (0.6×) | 0.243 ms (0.6×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (109%) | 14 925 op/s (94.0%) | 17 241 op/s (109%) | 14 925 op/s (94.0%) | 15 873 op/s | 14 925 op/s (94.0%) | 15 152 op/s (95.5%) | 14 925 op/s (94.0%) | 15 152 op/s (95.5%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 10 101 op/s (70.7%) | 13 889 op/s (97.2%) | 14 286 op/s (100%) | 14 286 op/s (100%) | 14 286 op/s | 17 857 op/s (125%) | 19 231 op/s (135%) | 19 231 op/s (135%) | 19 231 op/s (135%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 051 op/s (27.8%) | 22 727 op/s (125%) | 17 857 op/s (98.2%) | 20 408 op/s (112%) | 18 182 op/s | 25 641 op/s (141%) | 18 868 op/s (104%) | 27 027 op/s (149%) | 22 727 op/s (125%) |
| Tree shfld. | 11 364 op/s (56.8%) | 19 231 op/s (96.2%) | 20 000 op/s (100%) | 20 000 op/s (100%) | 20 000 op/s | 21 277 op/s (106%) | 21 277 op/s (106%) | 21 277 op/s (106%) | 21 739 op/s (109%) |
| DAG GNP sparse | 3 584 op/s (28.0%) | 14 286 op/s (111%) | 11 905 op/s (92.9%) | 13 514 op/s (105%) | 12 821 op/s | 16 393 op/s (128%) | 14 925 op/s (116%) | 15 625 op/s (122%) | 15 625 op/s (122%) |
| DAG GNP dense | 2 232 op/s (72.8%) | 2 967 op/s (96.7%) | 3 086 op/s (101%) | 3 086 op/s (101%) | 3 067 op/s | 4 082 op/s (133%) | 4 115 op/s (134%) | 4 082 op/s (133%) | 4 184 op/s (136%) |
| DAG GNP dense shfld. | 1 786 op/s (68.2%) | 2 584 op/s (98.7%) | 2 695 op/s (103%) | 2 732 op/s (104%) | 2 618 op/s | 4 098 op/s (157%) | 4 292 op/s (164%) | 4 065 op/s (155%) | 4 115 op/s (157%) |

![1.8.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.539 ms (0.9×) | 0.656 ms (1.0×) | 0.632 ms (1.0×) | 0.632 ms (1.0×) | 0.631 ms | 0.662 ms (1.0×) | 0.629 ms (1.0×) | 0.628 ms (1.0×) | 0.631 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.287 ms (1.2×) | 1.051 ms (1.0×) | 0.964 ms (0.9×) | 1.068 ms (1.0×) | 1.080 ms | 0.566 ms (0.5×) | 0.521 ms (0.5×) | 0.528 ms (0.5×) | 0.520 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.963 ms (8.5×) | 1.311 ms (1.1×) | 1.440 ms (1.2×) | 1.426 ms (1.2×) | 1.175 ms | 1.023 ms (0.9×) | 1.142 ms (1.0×) | 1.182 ms (1.0×) | 1.128 ms (1.0×) |
| Tree shfld. | 5.091 ms (3.5×) | 1.487 ms (1.0×) | 1.475 ms (1.0×) | 1.479 ms (1.0×) | 1.473 ms | 1.212 ms (0.8×) | 1.242 ms (0.8×) | 1.255 ms (0.9×) | 1.236 ms (0.8×) |
| DAG GNP sparse | 9.521 ms (4.2×) | 2.251 ms (1.0×) | 2.201 ms (1.0×) | 2.242 ms (1.0×) | 2.269 ms | 1.480 ms (0.7×) | 1.470 ms (0.6×) | 1.499 ms (0.7×) | 1.442 ms (0.6×) |
| DAG GNP dense | 6.487 ms (1.4×) | 4.533 ms (1.0×) | 4.333 ms (1.0×) | 4.515 ms (1.0×) | 4.475 ms | 3.208 ms (0.7×) | 3.086 ms (0.7×) | 3.127 ms (0.7×) | 3.041 ms (0.7×) |
| DAG GNP dense shfld. | 8.053 ms (1.5×) | 5.303 ms (1.0×) | 5.127 ms (1.0×) | 5.063 ms (1.0×) | 5.321 ms | 3.254 ms (0.6×) | 3.085 ms (0.6×) | 3.173 ms (0.6×) | 3.130 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 855 op/s (117%) | 1 524 op/s (96.2%) | 1 582 op/s (99.8%) | 1 582 op/s (99.8%) | 1 585 op/s | 1 511 op/s (95.3%) | 1 590 op/s (100%) | 1 592 op/s (100%) | 1 585 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 777.0 op/s (83.9%) | 951.5 op/s (103%) | 1 037 op/s (112%) | 936.3 op/s (101%) | 925.9 op/s | 1 767 op/s (191%) | 1 919 op/s (207%) | 1 894 op/s (205%) | 1 923 op/s (208%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 100.4 op/s (11.8%) | 762.8 op/s (89.6%) | 694.4 op/s (81.6%) | 701.3 op/s (82.4%) | 851.1 op/s | 977.5 op/s (115%) | 875.7 op/s (103%) | 846.0 op/s (99.4%) | 886.5 op/s (104%) |
| Tree shfld. | 196.4 op/s (28.9%) | 672.5 op/s (99.1%) | 678.0 op/s (99.9%) | 676.1 op/s (99.6%) | 678.9 op/s | 825.1 op/s (122%) | 805.2 op/s (119%) | 796.8 op/s (117%) | 809.1 op/s (119%) |
| DAG GNP sparse | 105.0 op/s (23.8%) | 444.2 op/s (101%) | 454.3 op/s (103%) | 446.0 op/s (101%) | 440.7 op/s | 675.7 op/s (153%) | 680.3 op/s (154%) | 667.1 op/s (151%) | 693.5 op/s (157%) |
| DAG GNP dense | 154.2 op/s (69.0%) | 220.6 op/s (98.7%) | 230.8 op/s (103%) | 221.5 op/s (99.1%) | 223.5 op/s | 311.7 op/s (139%) | 324.0 op/s (145%) | 319.8 op/s (143%) | 328.8 op/s (147%) |
| DAG GNP dense shfld. | 124.2 op/s (66.1%) | 188.6 op/s (100%) | 195.0 op/s (104%) | 197.5 op/s (105%) | 187.9 op/s | 307.3 op/s (164%) | 324.1 op/s (172%) | 315.2 op/s (168%) | 319.5 op/s (170%) |

![1.8.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 30.49 ms (0.9×) | 30.46 ms (0.9×) | 31.70 ms (0.9×) | 33.89 ms | 15.46 ms (0.5×) | 14.97 ms (0.4×) | 15.34 ms (0.5×) | 14.76 ms (0.4×) |
| DAG GNP dense | 103.7 ms (1.0×) | 103.6 ms (1.0×) | 106.4 ms (1.0×) | 104.2 ms | — | 37.35 ms (0.4×) | 38.21 ms (0.4×) | 38.09 ms (0.4×) |
| DAG GNP dense shfld. | 176.2 ms (1.0×) | 173.5 ms (1.0×) | 177.6 ms (1.0×) | 178.7 ms | 39.69 ms (0.2×) | 38.60 ms (0.2×) | 39.11 ms (0.2×) | 38.93 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 32.80 op/s (111%) | 32.83 op/s (111%) | 31.55 op/s (107%) | 29.51 op/s | 64.68 op/s (219%) | 66.82 op/s (226%) | 65.18 op/s (221%) | 67.77 op/s (230%) |
| DAG GNP dense | 9.643 op/s (100%) | 9.653 op/s (101%) | 9.398 op/s (97.9%) | 9.597 op/s | — | 26.77 op/s (279%) | 26.17 op/s (273%) | 26.25 op/s (274%) |
| DAG GNP dense shfld. | 5.675 op/s (101%) | 5.764 op/s (103%) | 5.631 op/s (101%) | 5.596 op/s | 25.20 op/s (450%) | 25.90 op/s (463%) | 25.57 op/s (457%) | 25.69 op/s (459%) |

![1.8.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1170.9 ms (1.0×) | 1145.1 ms (1.0×) | 1241.1 ms (1.1×) | 1133.7 ms | 160.8 ms (0.1×) | 156.7 ms (0.1×) | 163.7 ms (0.1×) | 154.0 ms (0.1×) |
| DAG GNP dense | 1966.4 ms (1.0×) | 1890.5 ms (1.0×) | 2005.5 ms (1.0×) | 1935.8 ms | — | 450.2 ms (0.2×) | 466.3 ms (0.2×) | 466.2 ms (0.2×) |
| DAG GNP dense shfld. | 2823.5 ms (1.0×) | 2805.5 ms (1.0×) | 2927.1 ms (1.0×) | 2829.4 ms | 414.3 ms (0.1×) | 405.1 ms (0.1×) | 418.1 ms (0.1×) | 429.6 ms (0.2×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.854 op/s (96.8%) | 0.873 op/s (99.0%) | 0.806 op/s (91.3%) | 0.882 op/s | 6.219 op/s (705%) | 6.382 op/s (723%) | 6.109 op/s (693%) | 6.494 op/s (736%) |
| DAG GNP dense | 0.509 op/s (98.4%) | 0.529 op/s (102%) | 0.499 op/s (96.5%) | 0.517 op/s | — | 2.221 op/s (430%) | 2.145 op/s (415%) | 2.145 op/s (415%) |
| DAG GNP dense shfld. | 0.354 op/s (100%) | 0.356 op/s (101%) | 0.342 op/s (96.7%) | 0.353 op/s | 2.414 op/s (683%) | 2.469 op/s (698%) | 2.392 op/s (677%) | 2.328 op/s (659%) |

![1.8.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 17135 ms (1.0×) | 17065 ms (1.0×) | 18079 ms (1.1×) | 16836 ms | 1810.4 ms (0.1×) | 1759.8 ms (0.1×) | 1855.1 ms (0.1×) | 1845.1 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.058 op/s (98.3%) | 0.059 op/s (98.7%) | 0.055 op/s (93.1%) | 0.059 op/s | 0.552 op/s (930%) | 0.568 op/s (957%) | 0.539 op/s (908%) | 0.542 op/s (912%) |

![1.8.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-5-toporder-n-100-000-000.svg)

### 1.9 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.040 ms (1.0×) | 0.039 ms (1.0×) | 0.040 ms | 0.037 ms (0.9×) | 0.040 ms (1.0×) | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.041 ms (1.0×) | 0.041 ms (1.0×) | 0.040 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.055 ms (1.0×) | 0.056 ms (1.0×) | 0.056 ms (1.0×) | 0.057 ms (1.0×) | 0.056 ms | 0.038 ms (0.7×) | 0.040 ms (0.7×) | 0.040 ms (0.7×) | 0.039 ms (0.7×) | 0.054 ms (1.0×) | 0.055 ms (1.0×) | 0.054 ms (1.0×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.031 ms (1.0×) | 0.031 ms (1.0×) | 0.034 ms (1.1×) | 0.035 ms (1.1×) | 0.031 ms | 0.030 ms (1.0×) | 0.027 ms (0.9×) | 0.031 ms (1.0×) | 0.031 ms (1.0×) | 0.028 ms (0.9×) | 0.029 ms (0.9×) | 0.028 ms (0.9×) |
| Tree shfld. | 0.034 ms (1.1×) | 0.031 ms (1.0×) | 0.034 ms (1.1×) | 0.032 ms (1.0×) | 0.031 ms | 0.031 ms (1.0×) | 0.031 ms (1.0×) | 0.031 ms (1.0×) | 0.031 ms (1.0×) | 0.030 ms (1.0×) | 0.030 ms (1.0×) | 0.029 ms (0.9×) |
| DAG GNP sparse | 0.052 ms (1.0×) | 0.057 ms (1.1×) | 0.052 ms (1.0×) | 0.061 ms (1.2×) | 0.052 ms | 0.050 ms (1.0×) | 0.054 ms (1.0×) | 0.054 ms (1.0×) | 0.054 ms (1.0×) | 0.042 ms (0.8×) | 0.045 ms (0.9×) | 0.042 ms (0.8×) |
| DAG GNP dense | 0.364 ms (1.0×) | 0.358 ms (1.0×) | 0.355 ms (1.0×) | 0.364 ms (1.0×) | 0.350 ms | 0.326 ms (0.9×) | 0.324 ms (0.9×) | 0.334 ms (1.0×) | 0.320 ms (0.9×) | 0.240 ms (0.7×) | 0.249 ms (0.7×) | 0.241 ms (0.7×) |
| DAG GNP dense shfld. | 0.393 ms (1.0×) | 0.396 ms (1.0×) | 0.400 ms (1.0×) | 0.404 ms (1.0×) | 0.395 ms | 0.323 ms (0.8×) | 0.321 ms (0.8×) | 0.328 ms (0.8×) | 0.312 ms (0.8×) | 0.289 ms (0.7×) | 0.295 ms (0.7×) | 0.280 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s (103%) | 25 641 op/s (103%) | 25 000 op/s (100%) | 25 641 op/s (103%) | 25 000 op/s | 27 027 op/s (108%) | 25 000 op/s (100%) | 25 641 op/s (103%) | 25 641 op/s (103%) | 24 390 op/s (97.6%) | 24 390 op/s (97.6%) | 25 000 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 182 op/s (102%) | 17 857 op/s (100%) | 17 857 op/s (100%) | 17 544 op/s (98.2%) | 17 857 op/s | 26 316 op/s (147%) | 25 000 op/s (140%) | 25 000 op/s (140%) | 25 641 op/s (144%) | 18 519 op/s (104%) | 18 182 op/s (102%) | 18 519 op/s (104%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 32 258 op/s (100%) | 32 258 op/s (100%) | 29 412 op/s (91.2%) | 28 571 op/s (88.6%) | 32 258 op/s | 33 333 op/s (103%) | 37 037 op/s (115%) | 32 258 op/s (100%) | 32 258 op/s (100%) | 35 714 op/s (111%) | 34 483 op/s (107%) | 35 714 op/s (111%) |
| Tree shfld. | 29 412 op/s (91.2%) | 32 258 op/s (100%) | 29 412 op/s (91.2%) | 31 250 op/s (96.9%) | 32 258 op/s | 32 258 op/s (100%) | 32 258 op/s (100%) | 32 258 op/s (100%) | 32 258 op/s (100%) | 33 333 op/s (103%) | 33 333 op/s (103%) | 34 483 op/s (107%) |
| DAG GNP sparse | 19 231 op/s (100%) | 17 544 op/s (91.2%) | 19 231 op/s (100%) | 16 393 op/s (85.2%) | 19 231 op/s | 20 000 op/s (104%) | 18 519 op/s (96.3%) | 18 519 op/s (96.3%) | 18 519 op/s (96.3%) | 23 810 op/s (124%) | 22 222 op/s (116%) | 23 810 op/s (124%) |
| DAG GNP dense | 2 747 op/s (96.2%) | 2 793 op/s (97.8%) | 2 817 op/s (98.6%) | 2 747 op/s (96.2%) | 2 857 op/s | 3 067 op/s (107%) | 3 086 op/s (108%) | 2 994 op/s (105%) | 3 125 op/s (109%) | 4 167 op/s (146%) | 4 016 op/s (141%) | 4 149 op/s (145%) |
| DAG GNP dense shfld. | 2 545 op/s (101%) | 2 525 op/s (99.7%) | 2 500 op/s (98.8%) | 2 475 op/s (97.8%) | 2 532 op/s | 3 096 op/s (122%) | 3 115 op/s (123%) | 3 049 op/s (120%) | 3 205 op/s (127%) | 3 460 op/s (137%) | 3 390 op/s (134%) | 3 571 op/s (141%) |

![1.9.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.562 ms (1.2×) | 0.456 ms (1.0×) | 0.457 ms (1.0×) | 0.462 ms (1.0×) | 0.452 ms | 0.560 ms (1.2×) | 0.458 ms (1.0×) | 0.571 ms (1.3×) | 0.547 ms (1.2×) | 0.348 ms (0.8×) | 0.360 ms (0.8×) | 0.370 ms (0.8×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.463 ms (1.1×) | 1.378 ms (1.0×) | 1.459 ms (1.1×) | 1.424 ms (1.1×) | 1.314 ms | 0.559 ms (0.4×) | 0.564 ms (0.4×) | 0.565 ms (0.4×) | 0.450 ms (0.3×) | 1.253 ms (1.0×) | 1.335 ms (1.0×) | 1.280 ms (1.0×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.934 ms (1.0×) | 0.929 ms (1.0×) | 0.927 ms (1.0×) | 0.947 ms (1.0×) | 0.926 ms | 0.863 ms (0.9×) | 0.782 ms (0.8×) | 0.845 ms (0.9×) | 0.783 ms (0.8×) | 0.839 ms (0.9×) | 0.852 ms (0.9×) | 0.849 ms (0.9×) |
| Tree shfld. | 1.048 ms (1.1×) | 0.945 ms (1.0×) | 1.032 ms (1.1×) | 0.954 ms (1.0×) | 0.951 ms | 0.829 ms (0.9×) | 0.891 ms (0.9×) | 0.893 ms (0.9×) | 0.835 ms (0.9×) | 0.884 ms (0.9×) | 0.896 ms (0.9×) | 0.917 ms (1.0×) |
| DAG GNP sparse | 2.167 ms (1.0×) | 2.211 ms (1.0×) | 2.159 ms (1.0×) | 2.261 ms (1.1×) | 2.153 ms | 1.974 ms (0.9×) | 1.962 ms (0.9×) | 1.985 ms (0.9×) | 1.936 ms (0.9×) | 1.989 ms (0.9×) | 1.981 ms (0.9×) | 1.931 ms (0.9×) |
| DAG GNP dense | 5.160 ms (1.0×) | 5.093 ms (1.0×) | 5.070 ms (1.0×) | 5.364 ms (1.0×) | 5.212 ms | 4.574 ms (0.9×) | 4.551 ms (0.9×) | 4.696 ms (0.9×) | 4.618 ms (0.9×) | 3.777 ms (0.7×) | 4.360 ms (0.8×) | 3.651 ms (0.7×) |
| DAG GNP dense shfld. | 5.877 ms (1.0×) | 5.806 ms (1.0×) | 5.763 ms (1.0×) | 5.866 ms (1.0×) | 6.018 ms | 4.496 ms (0.7×) | 4.496 ms (0.7×) | 4.690 ms (0.8×) | 4.535 ms (0.8×) | 4.487 ms (0.7×) | 4.509 ms (0.7×) | 4.514 ms (0.8×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 779 op/s (80.4%) | 2 193 op/s (99.1%) | 2 188 op/s (98.9%) | 2 165 op/s (97.8%) | 2 212 op/s | 1 786 op/s (80.7%) | 2 183 op/s (98.7%) | 1 751 op/s (79.2%) | 1 828 op/s (82.6%) | 2 874 op/s (130%) | 2 778 op/s (126%) | 2 703 op/s (122%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 683.5 op/s (89.8%) | 725.7 op/s (95.4%) | 685.4 op/s (90.1%) | 702.2 op/s (92.3%) | 761.0 op/s | 1 789 op/s (235%) | 1 773 op/s (233%) | 1 770 op/s (233%) | 2 222 op/s (292%) | 798.1 op/s (105%) | 749.1 op/s (98.4%) | 781.2 op/s (103%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 071 op/s (99.1%) | 1 076 op/s (99.7%) | 1 079 op/s (99.9%) | 1 056 op/s (97.8%) | 1 080 op/s | 1 159 op/s (107%) | 1 279 op/s (118%) | 1 183 op/s (110%) | 1 277 op/s (118%) | 1 192 op/s (110%) | 1 174 op/s (109%) | 1 178 op/s (109%) |
| Tree shfld. | 954.2 op/s (90.7%) | 1 058 op/s (101%) | 969.0 op/s (92.2%) | 1 048 op/s (99.7%) | 1 052 op/s | 1 206 op/s (115%) | 1 122 op/s (107%) | 1 120 op/s (106%) | 1 198 op/s (114%) | 1 131 op/s (108%) | 1 116 op/s (106%) | 1 091 op/s (104%) |
| DAG GNP sparse | 461.5 op/s (99.4%) | 452.3 op/s (97.4%) | 463.2 op/s (99.7%) | 442.3 op/s (95.2%) | 464.5 op/s | 506.6 op/s (109%) | 509.7 op/s (110%) | 503.8 op/s (108%) | 516.5 op/s (111%) | 502.8 op/s (108%) | 504.8 op/s (109%) | 517.9 op/s (111%) |
| DAG GNP dense | 193.8 op/s (101%) | 196.3 op/s (102%) | 197.2 op/s (103%) | 186.4 op/s (97.2%) | 191.9 op/s | 218.6 op/s (114%) | 219.7 op/s (115%) | 212.9 op/s (111%) | 216.5 op/s (113%) | 264.8 op/s (138%) | 229.4 op/s (120%) | 273.9 op/s (143%) |
| DAG GNP dense shfld. | 170.2 op/s (102%) | 172.2 op/s (104%) | 173.5 op/s (104%) | 170.5 op/s (103%) | 166.2 op/s | 222.4 op/s (134%) | 222.4 op/s (134%) | 213.2 op/s (128%) | 220.5 op/s (133%) | 222.9 op/s (134%) | 221.8 op/s (133%) | 221.5 op/s (133%) |

![1.9.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 29.60 ms (0.9×) | 29.09 ms (0.9×) | 32.07 ms (1.0×) | 31.72 ms | 26.85 ms (0.8×) | 25.49 ms (0.8×) | 27.96 ms (0.9×) | 28.43 ms (0.9×) |
| DAG GNP dense | 112.1 ms (1.0×) | 113.1 ms (1.0×) | 112.9 ms (1.0×) | 112.5 ms | — | 92.44 ms (0.8×) | 95.81 ms (0.9×) | 93.99 ms (0.8×) |
| DAG GNP dense shfld. | 191.1 ms (1.0×) | 190.3 ms (1.0×) | 206.4 ms (1.0×) | 199.4 ms | 89.45 ms (0.4×) | 92.20 ms (0.5×) | 96.05 ms (0.5×) | 92.20 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 33.78 op/s (107%) | 34.38 op/s (109%) | 31.18 op/s (98.9%) | 31.53 op/s | 37.24 op/s (118%) | 39.23 op/s (124%) | 35.76 op/s (113%) | 35.18 op/s (112%) |
| DAG GNP dense | 8.921 op/s (100%) | 8.842 op/s (99.5%) | 8.857 op/s (99.6%) | 8.889 op/s | — | 10.82 op/s (122%) | 10.44 op/s (117%) | 10.64 op/s (120%) |
| DAG GNP dense shfld. | 5.233 op/s (104%) | 5.255 op/s (105%) | 4.845 op/s (96.6%) | 5.015 op/s | 11.18 op/s (223%) | 10.85 op/s (216%) | 10.41 op/s (208%) | 10.85 op/s (216%) |

![1.9.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 856.2 ms (1.0×) | 853.6 ms (1.0×) | 877.4 ms (1.0×) | 847.7 ms | 751.9 ms (0.9×) | 759.4 ms (0.9×) | 772.4 ms (0.9×) | 752.4 ms (0.9×) |
| DAG GNP dense | 3085.9 ms (1.0×) | 2971.3 ms (1.0×) | 3289.1 ms (1.1×) | 3082.4 ms | — | 2553.0 ms (0.8×) | 3050.5 ms (1.0×) | 2699.6 ms (0.9×) |
| DAG GNP dense shfld. | 4398.5 ms (1.0×) | 4386.9 ms (1.0×) | 4671.3 ms (1.1×) | 4329.4 ms | 2631.0 ms (0.6×) | 2623.6 ms (0.6×) | 2667.8 ms (0.6×) | 2707.5 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1.168 op/s (99.0%) | 1.172 op/s (99.3%) | 1.140 op/s (96.6%) | 1.180 op/s | 1.330 op/s (113%) | 1.317 op/s (112%) | 1.295 op/s (110%) | 1.329 op/s (113%) |
| DAG GNP dense | 0.324 op/s (99.9%) | 0.337 op/s (104%) | 0.304 op/s (93.7%) | 0.324 op/s | — | 0.392 op/s (121%) | 0.328 op/s (101%) | 0.370 op/s (114%) |
| DAG GNP dense shfld. | 0.227 op/s (98.4%) | 0.228 op/s (98.7%) | 0.214 op/s (92.7%) | 0.231 op/s | 0.380 op/s (165%) | 0.381 op/s (165%) | 0.375 op/s (162%) | 0.369 op/s (160%) |

![1.9.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 11384 ms (1.0×) | 11412 ms (1.0×) | 11575 ms (1.0×) | 11214 ms | 9441.1 ms (0.8×) | 9419.7 ms (0.8×) | 9530.7 ms (0.8×) | 9499.8 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.088 op/s (98.5%) | 0.088 op/s (98.3%) | 0.086 op/s (96.9%) | 0.089 op/s | 0.106 op/s (119%) | 0.106 op/s (119%) | 0.105 op/s (118%) | 0.105 op/s (118%) |

![1.9.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-5-toporderandlevels-n-100-000-000.svg)

### 1.10 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.047 ms (0.8×) | 0.061 ms (1.0×) | 0.060 ms (1.0×) | 0.052 ms (0.9×) | 0.059 ms | 0.061 ms (1.0×) | 0.060 ms (1.0×) | 0.061 ms (1.0×) | 0.059 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.0×) | 0.065 ms (1.0×) | 0.065 ms (1.0×) | 0.065 ms | 0.051 ms (0.8×) | 0.050 ms (0.8×) | 0.051 ms (0.8×) | 0.051 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.223 ms (4.6×) | 0.049 ms (1.0×) | 0.049 ms (1.0×) | 0.049 ms (1.0×) | 0.048 ms | 0.048 ms (1.0×) | 0.046 ms (1.0×) | 0.047 ms (1.0×) | 0.041 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.207 ms (1.3×) | 0.119 ms (0.7×) | 0.164 ms (1.0×) | 0.143 ms (0.9×) | 0.165 ms | 0.090 ms (0.5×) | 0.087 ms (0.5×) | 0.093 ms (0.6×) | 0.085 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.797 ms (1.0×) | 0.776 ms (1.0×) | 0.771 ms (1.0×) | 0.775 ms (1.0×) | 0.781 ms | 0.647 ms (0.8×) | 0.665 ms (0.9×) | 0.658 ms (0.8×) | 0.656 ms (0.8×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.811 ms | 0.779 ms | 0.780 ms | 0.776 ms | — | 0.669 ms | 0.652 ms | 0.662 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.512 ms (0.9×) | 0.592 ms (1.0×) | 0.582 ms (1.0×) | 0.582 ms (1.0×) | 0.583 ms | 0.592 ms (1.0×) | 0.580 ms (1.0×) | 0.596 ms (1.0×) | 0.589 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.619 ms (1.0×) | 0.644 ms (1.0×) | 0.642 ms (1.0×) | 0.657 ms (1.0×) | 0.642 ms | 0.588 ms (0.9×) | 0.634 ms (1.0×) | 0.588 ms (0.9×) | 0.582 ms (0.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 21 277 op/s (126%) | 16 393 op/s (96.7%) | 16 667 op/s (98.3%) | 19 231 op/s (113%) | 16 949 op/s | 16 393 op/s (96.7%) | 16 667 op/s (98.3%) | 16 393 op/s (96.7%) | 16 949 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (112%) | 14 925 op/s (97.0%) | 15 385 op/s (100%) | 15 385 op/s (100%) | 15 385 op/s | 19 608 op/s (127%) | 20 000 op/s (130%) | 19 608 op/s (127%) | 19 608 op/s (127%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 484 op/s (21.5%) | 20 408 op/s (98.0%) | 20 408 op/s (98.0%) | 20 408 op/s (98.0%) | 20 833 op/s | 20 833 op/s (100%) | 21 739 op/s (104%) | 21 277 op/s (102%) | 24 390 op/s (117%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 831 op/s (79.7%) | 8 403 op/s (139%) | 6 098 op/s (101%) | 6 993 op/s (115%) | 6 061 op/s | 11 111 op/s (183%) | 11 494 op/s (190%) | 10 753 op/s (177%) | 11 765 op/s (194%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 255 op/s (98.0%) | 1 289 op/s (101%) | 1 297 op/s (101%) | 1 290 op/s (101%) | 1 280 op/s | 1 546 op/s (121%) | 1 504 op/s (117%) | 1 520 op/s (119%) | 1 524 op/s (119%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 233 op/s | 1 284 op/s | 1 282 op/s | 1 289 op/s | — | 1 495 op/s | 1 534 op/s | 1 511 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 953 op/s (114%) | 1 689 op/s (98.5%) | 1 718 op/s (100%) | 1 718 op/s (100%) | 1 715 op/s | 1 689 op/s (98.5%) | 1 724 op/s (101%) | 1 678 op/s (97.8%) | 1 698 op/s (99.0%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 616 op/s (104%) | 1 553 op/s (99.7%) | 1 558 op/s (100%) | 1 522 op/s (97.7%) | 1 558 op/s | 1 701 op/s (109%) | 1 577 op/s (101%) | 1 701 op/s (109%) | 1 718 op/s (110%) |

![1.10.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.507 ms (0.9×) | 0.606 ms (1.1×) | 0.563 ms (1.0×) | 0.568 ms (1.0×) | 0.557 ms | 0.605 ms (1.1×) | 0.556 ms (1.0×) | 0.569 ms (1.0×) | 0.558 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.351 ms (1.3×) | 0.945 ms (0.9×) | 1.063 ms (1.0×) | 0.994 ms (1.0×) | 1.042 ms | 0.510 ms (0.5×) | 0.513 ms (0.5×) | 0.506 ms (0.5×) | 0.503 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.871 ms (7.5×) | 1.327 ms (1.0×) | 1.271 ms (1.0×) | 1.295 ms (1.0×) | 1.315 ms | 1.083 ms (0.8×) | 0.872 ms (0.7×) | 1.035 ms (0.8×) | 1.038 ms (0.8×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.691 ms (1.5×) | 3.177 ms (1.0×) | 3.086 ms (1.0×) | 3.124 ms (1.0×) | 3.047 ms | 1.962 ms (0.6×) | 1.890 ms (0.6×) | 1.895 ms (0.6×) | 1.962 ms (0.6×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.3×) | 12.91 ms (1.0×) | 12.72 ms (1.0×) | 13.51 ms (1.0×) | 13.08 ms | 7.303 ms (0.6×) | 7.055 ms (0.5×) | 7.279 ms (0.6×) | 7.220 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 17.38 ms | 12.63 ms | 12.71 ms | 13.88 ms | — | 7.353 ms | 7.081 ms | 7.242 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.563 ms (1.4×) | 6.365 ms (1.0×) | 6.191 ms (1.0×) | 6.179 ms (1.0×) | 6.134 ms | 6.292 ms (1.0×) | 6.108 ms (1.0×) | 6.220 ms (1.0×) | 6.154 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 13.28 ms (1.1×) | 11.15 ms (1.0×) | 11.15 ms (1.0×) | 12.63 ms (1.1×) | 11.61 ms | 6.545 ms (0.6×) | 6.298 ms (0.5×) | 6.482 ms (0.6×) | 6.395 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 972 op/s (110%) | 1 650 op/s (91.9%) | 1 776 op/s (98.9%) | 1 761 op/s (98.1%) | 1 795 op/s | 1 653 op/s (92.1%) | 1 799 op/s (100%) | 1 757 op/s (97.9%) | 1 792 op/s (99.8%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 740.2 op/s (77.1%) | 1 058 op/s (110%) | 940.7 op/s (98.0%) | 1 006 op/s (105%) | 959.7 op/s | 1 961 op/s (204%) | 1 949 op/s (203%) | 1 976 op/s (206%) | 1 988 op/s (207%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 101.3 op/s (13.3%) | 753.6 op/s (99.1%) | 786.8 op/s (103%) | 772.2 op/s (102%) | 760.5 op/s | 923.4 op/s (121%) | 1 147 op/s (151%) | 966.2 op/s (127%) | 963.4 op/s (127%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 213.2 op/s (65.0%) | 314.8 op/s (95.9%) | 324.0 op/s (98.7%) | 320.1 op/s (97.5%) | 328.2 op/s | 509.7 op/s (155%) | 529.1 op/s (161%) | 527.7 op/s (161%) | 509.7 op/s (155%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (75.3%) | 77.44 op/s (101%) | 78.61 op/s (103%) | 74.05 op/s (96.9%) | 76.43 op/s | 136.9 op/s (179%) | 141.7 op/s (185%) | 137.4 op/s (180%) | 138.5 op/s (181%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s | 79.20 op/s | 78.71 op/s | 72.07 op/s | — | 136.0 op/s | 141.2 op/s | 138.1 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 116.8 op/s (71.6%) | 157.1 op/s (96.4%) | 161.5 op/s (99.1%) | 161.8 op/s (99.3%) | 163.0 op/s | 158.9 op/s (97.5%) | 163.7 op/s (100%) | 160.8 op/s (98.6%) | 162.5 op/s (99.7%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 75.31 op/s (87.4%) | 89.65 op/s (104%) | 89.69 op/s (104%) | 79.16 op/s (91.9%) | 86.14 op/s | 152.8 op/s (177%) | 158.8 op/s (184%) | 154.3 op/s (179%) | 156.4 op/s (182%) |

![1.10.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 49.47 ms (0.9×) | 45.23 ms (0.9×) | 49.41 ms (0.9×) | 53.11 ms | 20.62 ms (0.4×) | 19.54 ms (0.4×) | 19.64 ms (0.4×) | 21.00 ms (0.4×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 240.2 ms (1.0×) | 242.3 ms (1.0×) | 251.1 ms (1.0×) | 247.6 ms | — | 92.69 ms (0.4×) | 93.71 ms (0.4×) | 89.72 ms (0.4×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 241.4 ms | 240.9 ms | 253.4 ms | — | 94.11 ms | 90.99 ms | 92.78 ms | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 75.32 ms (1.0×) | 76.46 ms (1.0×) | 77.08 ms (1.0×) | 74.96 ms | — | 76.53 ms (1.0×) | 77.78 ms (1.0×) | 76.19 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 20.22 op/s (107%) | 22.11 op/s (117%) | 20.24 op/s (107%) | 18.83 op/s | 48.50 op/s (258%) | 51.17 op/s (272%) | 50.91 op/s (270%) | 47.63 op/s (253%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.163 op/s (103%) | 4.127 op/s (102%) | 3.982 op/s (98.6%) | 4.039 op/s | — | 10.79 op/s (267%) | 10.67 op/s (264%) | 11.15 op/s (276%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.143 op/s | 4.151 op/s | 3.946 op/s | — | 10.63 op/s | 10.99 op/s | 10.78 op/s | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 13.28 op/s (99.5%) | 13.08 op/s (98.0%) | 12.97 op/s (97.2%) | 13.34 op/s | — | 13.07 op/s (97.9%) | 12.86 op/s (96.4%) | 13.12 op/s (98.4%) |

![1.10.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1984.9 ms (1.0×) | 1952.3 ms (1.0×) | 2052.4 ms (1.0×) | 2025.5 ms | 254.0 ms (0.1×) | 244.4 ms (0.1×) | 244.8 ms (0.1×) | 246.6 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3708.4 ms (1.0×) | 3722.5 ms (1.0×) | 3843.7 ms (1.0×) | 3661.0 ms | — | 1223.7 ms (0.3×) | 1022.2 ms (0.3×) | 927.2 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3672.0 ms | 3874.8 ms | 3989.1 ms | — | 930.6 ms | 1124.4 ms | 1135.8 ms | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 815.0 ms (1.2×) | 839.7 ms (1.2×) | 838.2 ms (1.2×) | 692.1 ms | — | 797.1 ms (1.2×) | 802.5 ms (1.2×) | 689.2 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.504 op/s (102%) | 0.512 op/s (104%) | 0.487 op/s (98.7%) | 0.494 op/s | 3.937 op/s (797%) | 4.092 op/s (829%) | 4.085 op/s (827%) | 4.055 op/s (821%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.270 op/s (98.7%) | 0.269 op/s (98.3%) | 0.260 op/s (95.2%) | 0.273 op/s | — | 0.817 op/s (299%) | 0.978 op/s (358%) | 1.079 op/s (395%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.272 op/s | 0.258 op/s | 0.251 op/s | — | 1.075 op/s | 0.889 op/s | 0.880 op/s | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.227 op/s (84.9%) | 1.191 op/s (82.4%) | 1.193 op/s (82.6%) | 1.445 op/s | — | 1.255 op/s (86.8%) | 1.246 op/s (86.2%) | 1.451 op/s (100%) |

![1.10.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 27110 ms (1.0×) | 26787 ms (1.0×) | 28157 ms (1.0×) | 27738 ms | 2988.7 ms (0.1×) | 2799.7 ms (0.1×) | 3364.5 ms (0.1×) | 3076.0 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.037 op/s (102%) | 0.037 op/s (104%) | 0.036 op/s (98.5%) | 0.036 op/s | 0.335 op/s (928%) | 0.357 op/s (991%) | 0.297 op/s (824%) | 0.325 op/s (902%) |

![1.10.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-5-backedges-n-100-000-000.svg)

### 1.11 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.051 ms (0.8×) | 0.064 ms (1.0×) | 0.063 ms (1.0×) | 0.064 ms (1.0×) | 0.064 ms | 0.064 ms (1.0×) | 0.064 ms (1.0×) | 0.065 ms (1.0×) | 0.064 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.066 ms (0.8×) | 0.077 ms (1.0×) | 0.077 ms (1.0×) | 0.077 ms (1.0×) | 0.078 ms | 0.059 ms (0.8×) | 0.060 ms (0.8×) | 0.062 ms (0.8×) | 0.060 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.196 ms (3.8×) | 0.053 ms (1.0×) | 0.053 ms (1.0×) | 0.053 ms (1.0×) | 0.052 ms | 0.046 ms (0.9×) | 0.046 ms (0.9×) | 0.047 ms (0.9×) | 0.046 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.222 ms (1.3×) | 0.183 ms (1.1×) | 0.186 ms (1.1×) | 0.182 ms (1.1×) | 0.166 ms | 0.093 ms (0.6×) | 0.094 ms (0.6×) | 0.090 ms (0.5×) | 0.078 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.653 ms (1.2×) | 0.511 ms (1.0×) | 0.530 ms (1.0×) | 0.533 ms (1.0×) | 0.531 ms | 0.373 ms (0.7×) | 0.377 ms (0.7×) | 0.369 ms (0.7×) | 0.384 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.658 ms | 0.514 ms | 0.525 ms | 0.528 ms | — | 0.372 ms | 0.380 ms | 0.382 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.375 ms (1.3×) | 0.282 ms (1.0×) | 0.296 ms (1.0×) | 0.296 ms (1.0×) | 0.293 ms | 0.285 ms (1.0×) | 0.297 ms (1.0×) | 0.303 ms (1.0×) | 0.293 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.499 ms (1.3×) | 0.383 ms (1.0×) | 0.394 ms (1.0×) | 0.402 ms (1.0×) | 0.394 ms | 0.276 ms (0.7×) | 0.297 ms (0.8×) | 0.300 ms (0.8×) | 0.295 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 19 608 op/s (125%) | 15 625 op/s (100%) | 15 873 op/s (102%) | 15 625 op/s (100%) | 15 625 op/s | 15 625 op/s (100%) | 15 625 op/s (100%) | 15 385 op/s (98.5%) | 15 625 op/s (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 152 op/s (118%) | 12 987 op/s (101%) | 12 987 op/s (101%) | 12 987 op/s (101%) | 12 821 op/s | 16 949 op/s (132%) | 16 667 op/s (130%) | 16 129 op/s (126%) | 16 667 op/s (130%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 102 op/s (26.5%) | 18 868 op/s (98.1%) | 18 868 op/s (98.1%) | 18 868 op/s (98.1%) | 19 231 op/s | 21 739 op/s (113%) | 21 739 op/s (113%) | 21 277 op/s (111%) | 21 739 op/s (113%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 505 op/s (74.8%) | 5 464 op/s (90.7%) | 5 376 op/s (89.2%) | 5 495 op/s (91.2%) | 6 024 op/s | 10 753 op/s (178%) | 10 638 op/s (177%) | 11 111 op/s (184%) | 12 821 op/s (213%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 531 op/s (81.3%) | 1 957 op/s (104%) | 1 887 op/s (100%) | 1 876 op/s (99.6%) | 1 883 op/s | 2 681 op/s (142%) | 2 653 op/s (141%) | 2 710 op/s (144%) | 2 604 op/s (138%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 520 op/s | 1 946 op/s | 1 905 op/s | 1 894 op/s | — | 2 688 op/s | 2 632 op/s | 2 618 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 2 667 op/s (78.1%) | 3 546 op/s (104%) | 3 378 op/s (99.0%) | 3 378 op/s (99.0%) | 3 413 op/s | 3 509 op/s (103%) | 3 367 op/s (98.7%) | 3 300 op/s (96.7%) | 3 413 op/s (100%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 004 op/s (79.0%) | 2 611 op/s (103%) | 2 538 op/s (100%) | 2 488 op/s (98.0%) | 2 538 op/s | 3 623 op/s (143%) | 3 367 op/s (133%) | 3 333 op/s (131%) | 3 390 op/s (134%) |

![1.11.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.554 ms (0.9×) | 0.647 ms (1.1×) | 0.607 ms (1.0×) | 0.603 ms (1.0×) | 0.595 ms | 0.642 ms (1.1×) | 0.592 ms (1.0×) | 0.605 ms (1.0×) | 0.604 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.563 ms (1.1×) | 1.365 ms (1.0×) | 1.408 ms (1.0×) | 1.430 ms (1.0×) | 1.386 ms | 0.615 ms (0.4×) | 0.629 ms (0.5×) | 0.634 ms (0.5×) | 0.623 ms (0.4×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 10.29 ms (7.2×) | 1.288 ms (0.9×) | 1.480 ms (1.0×) | 1.497 ms (1.0×) | 1.439 ms | 1.022 ms (0.7×) | 1.066 ms (0.7×) | 1.071 ms (0.7×) | 1.044 ms (0.7×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.006 ms (1.4×) | 3.626 ms (1.0×) | 3.587 ms (1.0×) | 3.696 ms (1.0×) | 3.580 ms | 1.957 ms (0.5×) | 1.853 ms (0.5×) | 1.962 ms (0.5×) | 1.830 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.351 ms (1.3×) | 7.009 ms (1.0×) | 6.926 ms (1.0×) | 7.537 ms (1.0×) | 7.214 ms | 4.296 ms (0.6×) | 4.197 ms (0.6×) | 4.320 ms (0.6×) | 4.159 ms (0.6×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 9.400 ms | 7.103 ms | 6.963 ms | 7.584 ms | — | 4.185 ms | 4.193 ms | 4.299 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.372 ms (1.6×) | 2.812 ms (1.0×) | 2.706 ms (1.0×) | 2.696 ms (1.0×) | 2.701 ms | 2.815 ms (1.0×) | 2.689 ms (1.0×) | 2.754 ms (1.0×) | 2.642 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.766 ms (1.2×) | 5.303 ms (0.9×) | 5.599 ms (1.0×) | 6.171 ms (1.1×) | 5.809 ms | 3.053 ms (0.5×) | 2.982 ms (0.5×) | 2.973 ms (0.5×) | 2.965 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 805 op/s (107%) | 1 546 op/s (92.0%) | 1 647 op/s (98.0%) | 1 658 op/s (98.7%) | 1 681 op/s | 1 558 op/s (92.7%) | 1 689 op/s (101%) | 1 653 op/s (98.3%) | 1 656 op/s (98.5%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 639.8 op/s (88.7%) | 732.6 op/s (102%) | 710.2 op/s (98.4%) | 699.3 op/s (96.9%) | 721.5 op/s | 1 626 op/s (225%) | 1 590 op/s (220%) | 1 577 op/s (219%) | 1 605 op/s (222%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 97.17 op/s (14.0%) | 776.4 op/s (112%) | 675.7 op/s (97.2%) | 668.0 op/s (96.1%) | 694.9 op/s | 978.5 op/s (141%) | 938.1 op/s (135%) | 933.7 op/s (134%) | 957.9 op/s (138%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 199.8 op/s (71.5%) | 275.8 op/s (98.7%) | 278.8 op/s (99.8%) | 270.6 op/s (96.9%) | 279.3 op/s | 511.0 op/s (183%) | 539.7 op/s (193%) | 509.7 op/s (182%) | 546.4 op/s (196%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 106.9 op/s (77.1%) | 142.7 op/s (103%) | 144.4 op/s (104%) | 132.7 op/s (95.7%) | 138.6 op/s | 232.8 op/s (168%) | 238.3 op/s (172%) | 231.5 op/s (167%) | 240.4 op/s (173%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 106.4 op/s | 140.8 op/s | 143.6 op/s | 131.9 op/s | — | 238.9 op/s | 238.5 op/s | 232.6 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 228.7 op/s (61.8%) | 355.6 op/s (96.1%) | 369.5 op/s (99.8%) | 370.9 op/s (100%) | 370.2 op/s | 355.2 op/s (96.0%) | 371.9 op/s (100%) | 363.1 op/s (98.1%) | 378.5 op/s (102%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 147.8 op/s (85.9%) | 188.6 op/s (110%) | 178.6 op/s (104%) | 162.0 op/s (94.1%) | 172.1 op/s | 327.5 op/s (190%) | 335.3 op/s (195%) | 336.4 op/s (195%) | 337.3 op/s (196%) |

![1.11.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 60.41 ms (0.9×) | 58.24 ms (0.9×) | 69.55 ms (1.0×) | 67.35 ms | 23.47 ms (0.3×) | 20.58 ms (0.3×) | 22.69 ms (0.3×) | 21.38 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 235.5 ms (1.0×) | 238.8 ms (1.0×) | 252.1 ms (1.0×) | 241.3 ms | — | 60.05 ms (0.2×) | 60.84 ms (0.3×) | 61.23 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 231.9 ms | 235.5 ms | 252.0 ms | — | 60.28 ms | 60.09 ms | 60.77 ms | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.97 ms (1.0×) | 26.59 ms (1.0×) | 26.76 ms (1.0×) | 26.94 ms | — | 26.68 ms (1.0×) | 26.80 ms (1.0×) | 26.71 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 16.55 op/s (112%) | 17.17 op/s (116%) | 14.38 op/s (96.8%) | 14.85 op/s | 42.60 op/s (287%) | 48.59 op/s (327%) | 44.08 op/s (297%) | 46.77 op/s (315%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.246 op/s (102%) | 4.188 op/s (101%) | 3.967 op/s (95.7%) | 4.144 op/s | — | 16.65 op/s (402%) | 16.44 op/s (397%) | 16.33 op/s (394%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.312 op/s | 4.246 op/s | 3.968 op/s | — | 16.59 op/s | 16.64 op/s | 16.46 op/s | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 38.51 op/s (104%) | 37.62 op/s (101%) | 37.37 op/s (101%) | 37.12 op/s | — | 37.48 op/s (101%) | 37.32 op/s (101%) | 37.44 op/s (101%) |

![1.11.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2629.9 ms (1.1×) | 2492.1 ms (1.0×) | 2557.2 ms (1.0×) | 2500.9 ms | 422.5 ms (0.2×) | 414.7 ms (0.2×) | 447.1 ms (0.2×) | 411.7 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4515.8 ms (1.0×) | 4552.4 ms (1.0×) | 4861.4 ms (1.1×) | 4606.3 ms | — | 1515.5 ms (0.3×) | 1624.0 ms (0.4×) | 1435.0 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4526.0 ms | 4492.7 ms | 4853.4 ms | — | 1383.2 ms | 1393.9 ms | 1557.3 ms | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 276.6 ms (1.0×) | 281.8 ms (1.0×) | 336.4 ms (1.2×) | 278.7 ms | — | 275.9 ms (1.0×) | 284.6 ms (1.0×) | 279.7 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.380 op/s (95.1%) | 0.401 op/s (100%) | 0.391 op/s (97.8%) | 0.400 op/s | 2.367 op/s (592%) | 2.411 op/s (603%) | 2.237 op/s (559%) | 2.429 op/s (607%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (102%) | 0.220 op/s (101%) | 0.206 op/s (94.8%) | 0.217 op/s | — | 0.660 op/s (304%) | 0.616 op/s (284%) | 0.697 op/s (321%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s | 0.223 op/s | 0.206 op/s | — | 0.723 op/s | 0.717 op/s | 0.642 op/s | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.615 op/s (101%) | 3.549 op/s (98.9%) | 2.973 op/s (82.8%) | 3.588 op/s | — | 3.625 op/s (101%) | 3.514 op/s (97.9%) | 3.575 op/s (99.6%) |

![1.11.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 31792 ms (1.0×) | 31282 ms (1.0×) | 32754 ms (1.0×) | 31640 ms | 7181.5 ms (0.2×) | 6992.4 ms (0.2×) | 7210.5 ms (0.2×) | 6770.4 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.031 op/s (99.5%) | 0.032 op/s (101%) | 0.031 op/s (96.6%) | 0.032 op/s | 0.139 op/s (441%) | 0.143 op/s (452%) | 0.139 op/s (439%) | 0.148 op/s (467%) |

![1.11.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-5-edgetypes-n-100-000-000.svg)

### 1.12 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.121 ms (1.1×) | 0.110 ms (1.0×) | 0.110 ms (1.0×) | 0.112 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.145 ms (1.2×) | 0.133 ms (1.1×) | 0.130 ms (1.0×) | 0.124 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.185 ms (2.2×) | 0.087 ms (1.0×) | 0.090 ms (1.1×) | 0.084 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.483 ms (1.3×) | 0.386 ms (1.0×) | 0.385 ms (1.0×) | 0.380 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.127 ms (1.4×) | 0.821 ms (1.0×) | 0.818 ms (1.0×) | 0.811 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.114 ms | 0.812 ms | 0.817 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.715 ms (1.4×) | 0.532 ms (1.0×) | 0.533 ms (1.0×) | 0.519 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.867 ms (1.4×) | 0.612 ms (1.0×) | 0.618 ms (1.0×) | 0.614 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 8 264 op/s (92.6%) | 9 091 op/s (102%) | 9 091 op/s (102%) | 8 929 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 6 897 op/s (85.5%) | 7 519 op/s (93.2%) | 7 692 op/s (95.4%) | 8 065 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 405 op/s (45.4%) | 11 494 op/s (96.6%) | 11 111 op/s (93.3%) | 11 905 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 070 op/s (78.7%) | 2 591 op/s (98.4%) | 2 597 op/s (98.7%) | 2 632 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 887.3 op/s (72.0%) | 1 218 op/s (98.8%) | 1 222 op/s (99.1%) | 1 233 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 897.7 op/s | 1 232 op/s | 1 224 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 399 op/s (72.6%) | 1 880 op/s (97.6%) | 1 876 op/s (97.4%) | 1 927 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 153 op/s (70.8%) | 1 634 op/s (100%) | 1 618 op/s (99.4%) | 1 629 op/s |

![1.12.1 Reorder (DFS-preorder) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-1-reorder-dfs-preorder-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.259 ms (1.1×) | 1.033 ms (0.9×) | 1.043 ms (0.9×) | 1.099 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.144 ms (1.0×) | 1.929 ms (0.9×) | 2.016 ms (1.0×) | 2.113 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.144 ms (1.3×) | 2.215 ms (0.9×) | 2.314 ms (0.9×) | 2.463 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 6.609 ms (1.2×) | 5.411 ms (1.0×) | 5.436 ms (1.0×) | 5.407 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 15.07 ms (1.4×) | 10.81 ms (1.0×) | 11.74 ms (1.1×) | 11.16 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 15.00 ms | 10.87 ms | 11.77 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 7.332 ms (1.4×) | 5.374 ms (1.0×) | 5.406 ms (1.0×) | 5.367 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 11.95 ms (1.3×) | 8.836 ms (1.0×) | 9.153 ms (1.0×) | 9.224 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 794.3 op/s (87.3%) | 968.1 op/s (106%) | 958.8 op/s (105%) | 909.9 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 466.4 op/s (98.6%) | 518.4 op/s (110%) | 496.0 op/s (105%) | 473.3 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 318.1 op/s (78.3%) | 451.5 op/s (111%) | 432.2 op/s (106%) | 406.0 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 151.3 op/s (81.8%) | 184.8 op/s (99.9%) | 184.0 op/s (99.5%) | 184.9 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 66.37 op/s (74.1%) | 92.53 op/s (103%) | 85.17 op/s (95.1%) | 89.61 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 66.66 op/s | 92.00 op/s | 84.97 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 136.4 op/s (73.2%) | 186.1 op/s (99.9%) | 185.0 op/s (99.3%) | 186.3 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 83.70 op/s (77.2%) | 113.2 op/s (104%) | 109.3 op/s (101%) | 108.4 op/s |

![1.12.2 Reorder (DFS-preorder) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-2-reorder-dfs-preorder-cost-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 95.36 ms (1.1×) | 79.17 ms (0.9×) | 84.36 ms (1.0×) | 87.61 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 414.3 ms | 315.0 ms | 336.1 ms | — |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 317.2 ms (1.0×) | 336.7 ms (1.1×) | 318.2 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 52.67 ms (1.0×) | 53.13 ms (1.0×) | 52.32 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 10.49 op/s (91.9%) | 12.63 op/s (111%) | 11.85 op/s (104%) | 11.41 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 2.414 op/s | 3.175 op/s | 2.975 op/s | — |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 3.153 op/s (100%) | 2.970 op/s (94.5%) | 3.143 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 18.99 op/s (99.3%) | 18.82 op/s (98.5%) | 19.11 op/s |

![1.12.3 Reorder (DFS-preorder) cost @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-3-reorder-dfs-preorder-cost-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4011.1 ms (1.3×) | 3263.5 ms (1.0×) | 3401.7 ms (1.1×) | 3194.6 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 10805 ms | 7613.8 ms | 7838.8 ms | — |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 7509.7 ms (1.0×) | 7782.3 ms (1.1×) | 7202.3 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 617.1 ms (1.1×) | 622.8 ms (1.1×) | 543.0 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.249 op/s (79.6%) | 0.306 op/s (97.9%) | 0.294 op/s (93.9%) | 0.313 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.093 op/s | 0.131 op/s | 0.128 op/s | — |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 0.133 op/s (95.9%) | 0.128 op/s (92.5%) | 0.139 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 1.620 op/s (88.0%) | 1.606 op/s (87.2%) | 1.842 op/s |

![1.12.4 Reorder (DFS-preorder) cost @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-4-reorder-dfs-preorder-cost-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 49550 ms (1.2×) | 41091 ms (1.0×) | 41896 ms (1.0×) | 40225 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.020 op/s (81.2%) | 0.024 op/s (97.9%) | 0.024 op/s (96.0%) | 0.025 op/s |

![1.12.5 Reorder (DFS-preorder) cost @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-5-reorder-dfs-preorder-cost-n-100-000-000.svg)

### 1.13 BiAdjacency (in-adjacency) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.022 ms (1.5×) | 0.022 ms (1.5×) | 0.015 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.018 ms (0.8×) | 0.022 ms (1.0×) | 0.023 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.016 ms (0.7×) | 0.023 ms (1.0×) | 0.023 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.058 ms (1.0×) | 0.058 ms (1.0×) | 0.057 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.546 ms (1.0×) | 0.548 ms (1.0×) | 0.544 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.544 ms | 0.547 ms | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.545 ms (1.0×) | 0.542 ms (1.0×) | 0.542 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.548 ms (1.0×) | 0.547 ms (1.0×) | 0.544 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 45 455 op/s (68.2%) | 45 455 op/s (68.2%) | 66 667 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 55 556 op/s (128%) | 45 455 op/s (105%) | 43 478 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 62 500 op/s (144%) | 43 478 op/s (100%) | 43 478 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 17 241 op/s (98.3%) | 17 241 op/s (98.3%) | 17 544 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 832 op/s (99.6%) | 1 825 op/s (99.3%) | 1 838 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 838 op/s | 1 828 op/s | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 835 op/s (99.4%) | 1 845 op/s (100%) | 1 845 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 825 op/s (99.3%) | 1 828 op/s (99.5%) | 1 838 op/s |

![1.13.1 BiAdjacency (in-adjacency) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-13-1-biadjacency-in-adjacency-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.185 ms (0.9×) | 0.204 ms (1.0×) | 0.203 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.238 ms (1.0×) | 0.236 ms (1.0×) | 0.236 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.612 ms (1.0×) | 0.613 ms (1.0×) | 0.606 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.837 ms (0.6×) | 1.423 ms (1.0×) | 1.408 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.148 ms (1.0×) | 7.932 ms (1.1×) | 7.385 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 7.216 ms | 7.744 ms | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.559 ms (1.0×) | 5.555 ms (1.0×) | 5.548 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.951 ms (0.9×) | 7.824 ms (1.0×) | 7.574 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 405 op/s (110%) | 4 902 op/s (99.5%) | 4 926 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 202 op/s (99.2%) | 4 237 op/s (100%) | 4 237 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 634 op/s (99.0%) | 1 631 op/s (98.9%) | 1 650 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 195 op/s (168%) | 702.7 op/s (98.9%) | 710.2 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 139.9 op/s (103%) | 126.1 op/s (93.1%) | 135.4 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 138.6 op/s | 129.1 op/s | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 179.9 op/s (99.8%) | 180.0 op/s (99.9%) | 180.2 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 143.9 op/s (109%) | 127.8 op/s (96.8%) | 132.0 op/s |

![1.13.2 BiAdjacency (in-adjacency) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-13-2-biadjacency-in-adjacency-cost-n-100-000.svg)

### 1.14 Dominators

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 0.168 ms (1.0×) | 0.165 ms |
| Irreducible | 0.178 ms (1.1×) | 0.164 ms |
| Ladder | 0.155 ms (1.0×) | 0.159 ms |
| Chain | 0.114 ms (1.0×) | 0.113 ms |
| Fan behind chain | 0.144 ms (1.0×) | 0.143 ms |
| Small methods | 0.201 ms (1.1×) | 0.184 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 5 952 op/s (98.2%) | 6 061 op/s |
| Irreducible | 5 618 op/s (92.1%) | 6 098 op/s |
| Ladder | 6 452 op/s (103%) | 6 289 op/s |
| Chain | 8 772 op/s (99.1%) | 8 850 op/s |
| Fan behind chain | 6 944 op/s (99.3%) | 6 993 op/s |
| Small methods | 4 975 op/s (91.5%) | 5 435 op/s |

![1.14.1 Dominators @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-14-1-dominators-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 3.606 ms (1.0×) | 3.546 ms |
| Irreducible | 3.848 ms (1.0×) | 3.771 ms |
| Ladder | 1.528 ms (1.0×) | 1.465 ms |
| Chain | 1.098 ms (1.0×) | 1.096 ms |
| Fan behind chain | 1.469 ms (1.0×) | 1.459 ms |
| Small methods | 2.946 ms (1.0×) | 2.900 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 277.3 op/s (98.3%) | 282.0 op/s |
| Irreducible | 259.9 op/s (98.0%) | 265.2 op/s |
| Ladder | 654.5 op/s (95.9%) | 682.6 op/s |
| Chain | 910.7 op/s (99.8%) | 912.4 op/s |
| Fan behind chain | 680.7 op/s (99.3%) | 685.4 op/s |
| Small methods | 339.4 op/s (98.4%) | 344.8 op/s |

![1.14.2 Dominators @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-14-2-dominators-n-100-000.svg)

### 1.15 Dominance frontiers

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 0.069 ms (1.0×) | 0.069 ms |
| Irreducible | 0.083 ms (1.0×) | 0.080 ms |
| Ladder | 45.91 ms (1.0×) | 45.03 ms |
| Chain | 0.037 ms (1.0×) | 0.036 ms |
| Fan behind chain | 105.8 ms (1.0×) | 103.3 ms |
| Small methods | 0.057 ms (1.0×) | 0.055 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 14 493 op/s (100%) | 14 493 op/s |
| Irreducible | 12 048 op/s (96.4%) | 12 500 op/s |
| Ladder | 21.78 op/s (98.1%) | 22.21 op/s |
| Chain | 27 027 op/s (97.3%) | 27 778 op/s |
| Fan behind chain | 9.452 op/s (97.6%) | 9.681 op/s |
| Small methods | 17 544 op/s (96.5%) | 18 182 op/s |

![1.15.1 Dominance frontiers @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-15-1-dominance-frontiers-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 1.991 ms (1.0×) | 1.957 ms |
| Irreducible | 2.347 ms (1.0×) | 2.310 ms |
| Chain | 0.368 ms (1.0×) | 0.358 ms |
| Small methods | 1.455 ms (1.0×) | 1.425 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 502.3 op/s (98.3%) | 511.0 op/s |
| Irreducible | 426.1 op/s (98.4%) | 432.9 op/s |
| Chain | 2 717 op/s (97.3%) | 2 793 op/s |
| Small methods | 687.3 op/s (97.9%) | 701.8 op/s |

![1.15.2 Dominance frontiers @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-15-2-dominance-frontiers-n-100-000.svg)

### 1.16 Iterated dominance frontier (build + one query per variable)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 30.88 ms (1.0×) | 30.80 ms |
| Irreducible | 42.61 ms (1.0×) | 41.67 ms |
| Ladder | 36.01 ms (1.0×) | 35.74 ms |
| Chain | 21.29 ms (1.0×) | 20.90 ms |
| Fan behind chain | 33.67 ms (1.0×) | 33.78 ms |
| Small methods | 0.432 ms (1.0×) | 0.435 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 32.38 op/s (99.7%) | 32.47 op/s |
| Irreducible | 23.47 op/s (97.8%) | 24.00 op/s |
| Ladder | 27.77 op/s (99.3%) | 27.98 op/s |
| Chain | 46.96 op/s (98.1%) | 47.85 op/s |
| Fan behind chain | 29.70 op/s (100%) | 29.61 op/s |
| Small methods | 2 315 op/s (101%) | 2 299 op/s |

![1.16.1 Iterated dominance frontier (build + one query per variable) @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-16-1-iterated-dominance-frontier-build-one-query-per-variable-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 438.1 ms (1.0×) | 433.1 ms |
| Irreducible | 572.2 ms (1.0×) | 557.4 ms |
| Ladder | 375.5 ms (1.0×) | 364.3 ms |
| Chain | 231.5 ms (1.0×) | 228.3 ms |
| Fan behind chain | 405.6 ms (1.0×) | 404.0 ms |
| Small methods | 4.535 ms (1.0×) | 4.420 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 2.283 op/s (98.9%) | 2.309 op/s |
| Irreducible | 1.748 op/s (97.4%) | 1.794 op/s |
| Ladder | 2.663 op/s (97.0%) | 2.745 op/s |
| Chain | 4.320 op/s (98.6%) | 4.380 op/s |
| Fan behind chain | 2.465 op/s (99.6%) | 2.475 op/s |
| Small methods | 220.5 op/s (97.5%) | 226.2 op/s |

![1.16.2 Iterated dominance frontier (build + one query per variable) @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-16-2-iterated-dominance-frontier-build-one-query-per-variable-n-100-000.svg)

### 1.17 Loops

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 0.400 ms (1.0×) | 0.395 ms |
| Irreducible | 0.420 ms (1.0×) | 0.422 ms |
| Ladder | 0.102 ms (1.0×) | 0.101 ms |
| Chain | 0.103 ms (1.0×) | 0.102 ms |
| Fan behind chain | 0.103 ms (1.0×) | 0.102 ms |
| Small methods | 0.465 ms (1.0×) | 0.464 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 2 500 op/s (98.8%) | 2 532 op/s |
| Irreducible | 2 381 op/s (100%) | 2 370 op/s |
| Ladder | 9 804 op/s (99.0%) | 9 901 op/s |
| Chain | 9 709 op/s (99.0%) | 9 804 op/s |
| Fan behind chain | 9 709 op/s (99.0%) | 9 804 op/s |
| Small methods | 2 151 op/s (99.8%) | 2 155 op/s |

![1.17.1 Loops @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-17-1-loops-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 5.543 ms (1.0×) | 5.751 ms |
| Irreducible | 7.337 ms (1.0×) | 7.272 ms |
| Ladder | 1.035 ms (1.0×) | 1.020 ms |
| Chain | 0.992 ms (1.0×) | 0.994 ms |
| Fan behind chain | 1.005 ms (1.0×) | 0.997 ms |
| Small methods | 4.997 ms (1.0×) | 4.844 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 180.4 op/s (104%) | 173.9 op/s |
| Irreducible | 136.3 op/s (99.1%) | 137.5 op/s |
| Ladder | 966.2 op/s (98.6%) | 980.4 op/s |
| Chain | 1 008 op/s (100%) | 1 006 op/s |
| Fan behind chain | 995.0 op/s (99.2%) | 1 003 op/s |
| Small methods | 200.1 op/s (96.9%) | 206.4 op/s |

![1.17.2 Loops @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-17-2-loops-n-100-000.svg)

### 1.18 Ancestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 0.065 ms (1.0×) | 0.065 ms |
| Irreducible | 0.067 ms (1.0×) | 0.066 ms |
| Ladder | 0.052 ms (1.2×) | 0.045 ms |
| Chain | 0.056 ms (1.0×) | 0.056 ms |
| Fan behind chain | 0.052 ms (1.0×) | 0.051 ms |
| Small methods | 0.058 ms (1.0×) | 0.057 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 15 385 op/s (100%) | 15 385 op/s |
| Irreducible | 14 925 op/s (98.5%) | 15 152 op/s |
| Ladder | 19 231 op/s (86.5%) | 22 222 op/s |
| Chain | 17 857 op/s (100%) | 17 857 op/s |
| Fan behind chain | 19 231 op/s (98.1%) | 19 608 op/s |
| Small methods | 17 241 op/s (98.3%) | 17 544 op/s |

![1.18.1 Ancestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-18-1-ancestry-of-the-dominator-tree-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 1.897 ms (1.1×) | 1.792 ms |
| Irreducible | 1.727 ms (1.0×) | 1.718 ms |
| Ladder | 0.504 ms (1.0×) | 0.488 ms |
| Chain | 0.559 ms (1.0×) | 0.553 ms |
| Fan behind chain | 0.543 ms (1.1×) | 0.490 ms |
| Small methods | 1.591 ms (1.0×) | 1.564 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 527.1 op/s (94.5%) | 558.0 op/s |
| Irreducible | 579.0 op/s (99.5%) | 582.1 op/s |
| Ladder | 1 984 op/s (96.8%) | 2 049 op/s |
| Chain | 1 789 op/s (98.9%) | 1 808 op/s |
| Fan behind chain | 1 842 op/s (90.2%) | 2 041 op/s |
| Small methods | 628.5 op/s (98.3%) | 639.4 op/s |

![1.18.2 Ancestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-18-2-ancestry-of-the-dominator-tree-n-100-000.svg)

### 1.19 PreorderAncestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 0.052 ms (1.0×) | 0.051 ms |
| Irreducible | 0.052 ms (1.0×) | 0.051 ms |
| Ladder | 0.045 ms (1.0×) | 0.044 ms |
| Chain | 0.046 ms (1.0×) | 0.046 ms |
| Fan behind chain | 0.044 ms (1.0×) | 0.045 ms |
| Small methods | 0.050 ms (1.0×) | 0.048 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 19 231 op/s (98.1%) | 19 608 op/s |
| Irreducible | 19 231 op/s (98.1%) | 19 608 op/s |
| Ladder | 22 222 op/s (97.8%) | 22 727 op/s |
| Chain | 21 739 op/s (100%) | 21 739 op/s |
| Fan behind chain | 22 727 op/s (102%) | 22 222 op/s |
| Small methods | 20 000 op/s (96.0%) | 20 833 op/s |

![1.19.1 PreorderAncestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-19-1-preorderancestry-of-the-dominator-tree-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 1.072 ms (1.0×) | 1.094 ms |
| Irreducible | 1.051 ms (1.0×) | 1.055 ms |
| Ladder | 0.378 ms (1.1×) | 0.346 ms |
| Chain | 0.426 ms (1.0×) | 0.411 ms |
| Fan behind chain | 0.376 ms (0.9×) | 0.401 ms |
| Small methods | 0.868 ms (1.0×) | 0.846 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|
| Structured | 932.8 op/s (102%) | 914.1 op/s |
| Irreducible | 951.5 op/s (100%) | 947.9 op/s |
| Ladder | 2 646 op/s (91.5%) | 2 890 op/s |
| Chain | 2 347 op/s (96.5%) | 2 433 op/s |
| Fan behind chain | 2 660 op/s (107%) | 2 494 op/s |
| Small methods | 1 152 op/s (97.5%) | 1 182 op/s |

![1.19.2 PreorderAncestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-19-2-preorderancestry-of-the-dominator-tree-n-100-000.svg)

## 2 Memory Footprint

Retained heap in bytes for a fully built graph. Lower is better. Baseline (= 100%): **Graphity 2.3.0**, or the newest version present in a table it is missing from.

### 2.1 Total Graph Size

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB | 1.2 MB | 1.2 MB | 1.2 MB | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Path<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 12.4 MB | 12.4 MB | 12.4 MB | 12.4 MB | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 16.0 MB (100%) | 16.0 MB (100%) | 16.0 MB (100%) | 16.0 MB |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 160 MB (100%) | 160 MB (100%) | 160 MB (100%) | 160 MB |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 1600 MB (100%) | 1600 MB (100%) | 1600 MB (100%) | 1600 MB |

![2.1 · Total Graph Size](fig/synthetic-ubuntu-ryzen-9950x-history/2-1-total-graph-size.svg)

### 2.2 Bytes per Element  `totalBytes / (Nodes + Edges)`

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 4 B | 4 B | 4 B | 4 B | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 4 B | 4 B | 4 B | 4 B | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |

![2.2 · Bytes per Element  `totalBytes / (Nodes + Edges)`](fig/synthetic-ubuntu-ryzen-9950x-history/2-2-bytes-per-element-totalbytes-nodes-edges.svg)

## 3 Allocations

Bytes allocated per algorithm invocation (JMH `-prof gc`). Lower is better. Baseline (= 100%): **Graphity 2.3.0**, or the newest version present in a table it is missing from.

### 3.1 Build

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB | 9.9 MB | 9.9 MB | 9.9 MB | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB |

![3.1.1 · Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-1-build-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 111 MB | 111 MB | 111 MB | 111 MB | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 534 kB (100.0%) | 534 kB (100.0%) | 534 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 283 kB (52.9%) | 544 kB (102%) | 534 kB (100.0%) | 534 kB (100.0%) | 534 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 283 kB (98.1%) | 288 kB (100.0%) | 288 kB (100.0%) | 288 kB (100.0%) | 288 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 250 kB (67.5%) | 356 kB (96.0%) | 371 kB (100.0%) | 371 kB (100.0%) | 371 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (49.5%) | 507 kB (102%) | 498 kB (100.0%) | 498 kB (100.0%) | 498 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 246 kB | 507 kB | 497 kB | 497 kB | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (49.3%) | 505 kB (102%) | 495 kB (100.0%) | 495 kB (100.0%) | 496 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (67.0%) | 350 kB (95.9%) | 364 kB (100.0%) | 364 kB (100.0%) | 364 kB |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 6.0 MB (122%) | 4.9 MB (100.0%) | 4.9 MB (100.0%) | 4.9 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (57.8%) | 4.9 MB (100%) | 4.9 MB (100.0%) | 4.9 MB (100.0%) | 4.9 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (101%) | 2.8 MB (100.0%) | 2.8 MB (100.0%) | 2.8 MB (100.0%) | 2.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.5 MB (71.1%) | 3.9 MB (110%) | 3.5 MB (100.0%) | 3.5 MB (100.0%) | 3.5 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (54.3%) | 5.6 MB (124%) | 4.5 MB (100.0%) | 4.5 MB (100.0%) | 4.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.5 MB | 5.6 MB | 4.5 MB | 4.5 MB | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (54.2%) | 5.6 MB (124%) | 4.5 MB (100.0%) | 4.5 MB (100.0%) | 4.5 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (70.6%) | 3.3 MB (96.7%) | 3.5 MB (100.0%) | 3.5 MB (100.0%) | 3.5 MB |

![3.2.2 · SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-2-scc-n-100-000.svg)

### 3.3 WCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 200 kB | 200 kB | 200 kB | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 2.0 MB | 2.0 MB | 2.0 MB | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |

![3.3.2 · WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-2-wcc-n-100-000.svg)

### 3.4 CCs

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 200 kB | 200 kB | 200 kB | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |

![3.4.1 · CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-1-ccs-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 2.0 MB | 2.0 MB | 2.0 MB | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |

![3.4.2 · CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-2-ccs-n-100-000.svg)

### 3.5 Condense

**Allocations @ 1k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | — | — | 23.2 kB (100.0%) | 23.2 kB (99.9%) | 23.2 kB |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | — | — | 96 B (99.0%) | 96 B (99.0%) | 97 B |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | — | — | 760 B (99.9%) | 760 B (99.9%) | 761 B |
| DAG GNP sparse<br><sub>1k nodes</sub> | — | — | 66.4 kB (100.0%) | 66.4 kB (100.0%) | 66.4 kB |

![3.5.1 · Condense @ n = 1 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-1-condense-n-1-000.svg)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | — | — | 250 kB (100.0%) | 250 kB (100.0%) | 250 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | — | — | 97 B (94.2%) | 97 B (94.2%) | 103 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | — | — | 7.2 kB (99.8%) | 7.2 kB (99.8%) | 7.2 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | — | — | 541 kB (100.0%) | 541 kB (100.0%) | 541 kB |

![3.5.2 · Condense @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-2-condense-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | — | 2.1 MB (100.0%) | 2.1 MB (100.0%) | 2.1 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | — | — | 107 B (52.2%) | 107 B (52.2%) | 205 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | — | — | 75.7 kB (99.8%) | 75.7 kB (99.8%) | 75.8 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | — | — | 5.9 MB (100.0%) | 5.9 MB (100.0%) | 5.9 MB |

![3.5.3 · Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-3-condense-n-100-000.svg)

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 297 kB (100.0%) | 297 kB (100.0%) | 297 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (14.4%) | 306 kB (103%) | 297 kB (100.0%) | 297 kB (100.0%) | 297 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB |
| Tree shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB |
| DAG GNP dense<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.9 kB |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 3.6 MB (143%) | 2.5 MB (100.0%) | 2.5 MB (100.0%) | 2.5 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 425 kB (16.9%) | 2.5 MB (101%) | 2.5 MB (100.0%) | 2.5 MB (100.0%) | 2.5 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB |
| Tree shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB |
| DAG GNP dense<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (99.9%) | 433 kB (99.9%) | 433 kB (99.9%) | 434 kB |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (99.9%) | 433 kB (99.9%) | 433 kB (99.9%) | 434 kB |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 257 kB (100.0%) | 257 kB (100.0%) | 257 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.1%) | 266 kB (104%) | 257 kB (100.0%) | 257 kB (100.0%) | 257 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (25.2%) | 10.9 kB (100.0%) | 10.9 kB (100.0%) | 10.9 kB (100.0%) | 10.9 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 228 kB (65.0%) | 336 kB (95.8%) | 351 kB (100.0%) | 351 kB (100.0%) | 351 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100.0%) | 4.1 MB (100.0%) | 4.1 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB | 4.1 MB | 4.1 MB | 4.1 MB | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (93.8%) | 4.1 MB (100%) | 4.1 MB (100.0%) | 4.1 MB (100.0%) | 4.1 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (96.9%) | 4.0 MB (99.6%) | 4.0 MB (100.0%) | 4.0 MB (100.0%) | 4.0 MB |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 3.2 MB (151%) | 2.1 MB (100.0%) | 2.1 MB (100.0%) | 2.1 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.2%) | 2.1 MB (101%) | 2.1 MB (100.0%) | 2.1 MB (100.0%) | 2.1 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 25.3 kB (75.7%) | 33.4 kB (99.8%) | 33.4 kB (99.8%) | 33.4 kB (99.8%) | 33.5 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.6 MB (71.3%) | 4.0 MB (110%) | 3.6 MB (100.0%) | 3.6 MB (100.0%) | 3.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB (100.0%) | 45.9 MB (100.0%) | 45.9 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 43.8 MB | 47.0 MB | 45.9 MB | 45.9 MB | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (95.4%) | 47.0 MB (102%) | 45.9 MB (100.0%) | 45.9 MB (100.0%) | 45.9 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (97.7%) | 44.7 MB (99.7%) | 44.8 MB (100.0%) | 44.8 MB (100.0%) | 44.8 MB |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 307 kB (100.0%) | 307 kB (100.0%) | 307 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (17.2%) | 316 kB (103%) | 307 kB (100.0%) | 307 kB (100.0%) | 307 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (86.6%) | 60.8 kB (100.0%) | 60.8 kB (100.0%) | 60.8 kB (100.0%) | 60.8 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 72.7 kB (37.2%) | 181 kB (92.4%) | 196 kB (100.0%) | 196 kB (100.0%) | 196 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (57.4%) | 606 kB (102%) | 596 kB (100.0%) | 596 kB (100.0%) | 596 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 342 kB | 606 kB | 596 kB | 596 kB | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (58.2%) | 617 kB (102%) | 607 kB (100.0%) | 607 kB (100.0%) | 608 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (74.2%) | 462 kB (96.9%) | 476 kB (100.0%) | 476 kB (100.0%) | 476 kB |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 3.7 MB (141%) | 2.6 MB (100.0%) | 2.6 MB (100.0%) | 2.6 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 525 kB (20.1%) | 2.6 MB (101%) | 2.6 MB (100.0%) | 2.6 MB (100.0%) | 2.6 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 525 kB (98.5%) | 533 kB (100.0%) | 533 kB (100.0%) | 533 kB (100.0%) | 533 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 725 kB (41.1%) | 2.1 MB (120%) | 1.8 MB (100.0%) | 1.8 MB (100.0%) | 1.8 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (62.1%) | 6.6 MB (120%) | 5.5 MB (100.0%) | 5.5 MB (100.0%) | 5.5 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 3.4 MB | 6.6 MB | 5.5 MB | 5.5 MB | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (62.8%) | 6.7 MB (119%) | 5.6 MB (100.0%) | 5.6 MB (100.0%) | 5.6 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (77.2%) | 4.5 MB (97.5%) | 4.6 MB (100.0%) | 4.6 MB (100.0%) | 4.6 MB |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-2-edgetypes-n-100-000.svg)

### 3.10 Dominators

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 371 kB (100.0%) | 371 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 371 kB (100.0%) | 371 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 486 kB (100.0%) | 486 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 566 kB (100.0%) | 566 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 486 kB (100.0%) | 486 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 475 kB (100.0%) | 475 kB |

![3.10.1 · Dominators @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-10-1-dominators-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 3.6 MB (100.0%) | 3.7 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 3.7 MB (100.0%) | 3.7 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 4.7 MB (100.0%) | 4.7 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 5.5 MB (100.0%) | 5.5 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 4.7 MB (100.0%) | 4.7 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 4.7 MB (100.0%) | 4.7 MB |

![3.10.2 · Dominators @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-10-2-dominators-n-100-000.svg)

### 3.11 Dominance frontiers

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 134 kB (100.0%) | 134 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 144 kB (100.0%) | 144 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 50.1 MB (100.0%) | 50.1 MB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 80.1 kB (100.0%) | 80.1 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 100 MB (100.0%) | 100 MB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 140 kB (100.0%) | 140 kB |

![3.11.1 · Dominance frontiers @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-11-1-dominance-frontiers-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 1.3 MB (100.0%) | 1.3 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 1.4 MB (100.0%) | 1.4 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 800 kB (100.0%) | 800 kB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 1.4 MB (100.0%) | 1.4 MB |

![3.11.2 · Dominance frontiers @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-11-2-dominance-frontiers-n-100-000.svg)

### 3.12 Iterated dominance frontier (build + one query per variable)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 25.4 MB (100.0%) | 25.4 MB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 28.9 MB (100.0%) | 28.9 MB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 53.8 MB (100.0%) | 53.8 MB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 21.4 MB (100.0%) | 21.4 MB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 65.4 MB (100.0%) | 65.4 MB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 604 kB (100.0%) | 604 kB |

![3.12.1 · Iterated dominance frontier (build + one query per variable) @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-12-1-iterated-dominance-frontier-build-one-query-per-variable-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 370 MB (100.0%) | 370 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 451 MB (100.0%) | 451 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 504 MB (100.0%) | 504 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 198 MB (100.0%) | 198 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 662 MB (100.0%) | 662 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 5.8 MB (100.0%) | 5.8 MB |

![3.12.2 · Iterated dominance frontier (build + one query per variable) @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-12-2-iterated-dominance-frontier-build-one-query-per-variable-n-100-000.svg)

### 3.13 Loops

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 772 kB (100.0%) | 772 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 812 kB (100.0%) | 812 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 675 kB (100.0%) | 675 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 735 kB (100.0%) | 735 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 675 kB (100.0%) | 675 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 897 kB (100.0%) | 897 kB |

![3.13.1 · Loops @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-13-1-loops-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 7.5 MB (100.0%) | 7.5 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 13.5 MB (100.0%) | 13.5 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 6.6 MB (100.0%) | 6.6 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 7.2 MB (100.0%) | 7.2 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 6.6 MB (100.0%) | 6.6 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 8.9 MB (100.0%) | 8.9 MB |

![3.13.2 · Loops @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-13-2-loops-n-100-000.svg)

### 3.14 Ancestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 297 kB (100.0%) | 297 kB |

![3.14.1 · Ancestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-14-1-ancestry-of-the-dominator-tree-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 2.9 MB (100.0%) | 2.9 MB |

![3.14.2 · Ancestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-14-2-ancestry-of-the-dominator-tree-n-100-000.svg)

### 3.15 PreorderAncestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 216 kB (100.0%) | 216 kB |

![3.15.1 · PreorderAncestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-15-1-preorderancestry-of-the-dominator-tree-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 |
|---|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 2.1 MB (100.0%) | 2.1 MB |

![3.15.2 · PreorderAncestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-15-2-preorderancestry-of-the-dominator-tree-n-100-000.svg)
