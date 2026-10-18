# Graphity Version History — synthetic / ubuntu-ryzen-9950x

Benchmarked versions: `1.0`, `2.0.0`, `2.1.0`, `2.2.0`, `2.3.0`, `2.4.0`. Peer libraries are left out on purpose: they are re-measured per run, so a moving peer column would say nothing about Graphity. For the comparison against them see [`synthetic-ubuntu-ryzen-9950x-peers.md`](synthetic-ubuntu-ryzen-9950x-peers.md).

Versions `1.0`, `2.0.0`, `2.1.0`, `2.2.0` report the mean over all measurement windows; the later ones a near-best window (`Estimate` below), which comes out a few percent faster for unchanged code.

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

Times (ms/op, lower is better) and throughput (op/s, higher is better) for each algorithm and graph size. Non-baseline cells show the factor relative to **Graphity 2.4.0**, or the newest version present in a table it is missing from (= 100% in the throughput table).

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

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.014 ms (1.0×) | 0.014 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.014 ms (1.0×) | 0.014 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.010 ms (1.0×) | 0.010 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.017 ms (1.0×) | 0.017 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.048 ms (1.0×) | 0.047 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.051 ms (1.0×) | 0.051 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.053 ms (1.0×) | 0.053 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s (100%) | 71 429 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s (100%) | 71 429 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 100 000 op/s (100%) | 100 000 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 58 824 op/s (100%) | 58 824 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 20 833 op/s (97.9%) | 21 277 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 19 608 op/s (100%) | 19 608 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 18 868 op/s (100%) | 18 868 op/s |

![1.2.1 Out-adjacency traversal (outNodes, no algorithm) @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-1-out-adjacency-traversal-outnodes-no-algorithm-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.169 ms (1.0×) | 0.170 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.170 ms (1.0×) | 0.169 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.404 ms (1.0×) | 0.405 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.658 ms (1.0×) | 0.653 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.134 ms (1.0×) | 1.130 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 0.517 ms (1.0×) | 0.518 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 0.514 ms (1.0×) | 0.518 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 917 op/s (101%) | 5 882 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 5 882 op/s (99.4%) | 5 917 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2 475 op/s (100%) | 2 469 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 520 op/s (99.2%) | 1 531 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 881.8 op/s (99.6%) | 885.0 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1 934 op/s (100%) | 1 931 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1 946 op/s (101%) | 1 931 op/s |

![1.2.2 Out-adjacency traversal (outNodes, no algorithm) @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-2-2-out-adjacency-traversal-outnodes-no-algorithm-n-100-000.svg)

### 1.3 In-adjacency traversal (inNodes, no algorithm)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.014 ms (1.0×) | 0.014 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.014 ms (1.0×) | 0.014 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.014 ms (1.0×) | 0.014 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.017 ms (1.0×) | 0.017 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.050 ms (1.0×) | 0.050 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.054 ms (1.0×) | 0.054 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.056 ms (1.0×) | 0.056 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s (100%) | 71 429 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s (100%) | 71 429 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 71 429 op/s (100%) | 71 429 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 58 824 op/s (100%) | 58 824 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 20 000 op/s (100%) | 20 000 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 18 519 op/s (100%) | 18 519 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 17 857 op/s (100%) | 17 857 op/s |

![1.3.1 In-adjacency traversal (inNodes, no algorithm) @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-1-in-adjacency-traversal-innodes-no-algorithm-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.170 ms (1.0×) | 0.171 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.171 ms (1.0×) | 0.171 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.170 ms (1.0×) | 0.171 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.669 ms (1.0×) | 0.662 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.238 ms (1.0×) | 1.186 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 0.538 ms (1.0×) | 0.541 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 0.540 ms (1.0×) | 0.541 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 882 op/s (101%) | 5 848 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 5 848 op/s (100%) | 5 848 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 5 882 op/s (101%) | 5 848 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 495 op/s (99.0%) | 1 511 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 807.8 op/s (95.8%) | 843.2 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1 859 op/s (101%) | 1 848 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 1 852 op/s (100%) | 1 848 op/s |

![1.3.2 In-adjacency traversal (inNodes, no algorithm) @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-3-2-in-adjacency-traversal-innodes-no-algorithm-n-100-000.svg)

### 1.4 SCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.106 ms (1.0×) | 0.113 ms (1.0×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) | 0.109 ms | 0.112 ms (1.0×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) | 0.106 ms (1.0×) | 0.110 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.129 ms (0.9×) | 0.134 ms (1.0×) | 0.134 ms (1.0×) | 0.135 ms (1.0×) | 0.134 ms (1.0×) | 0.138 ms | 0.108 ms (0.8×) | 0.105 ms (0.8×) | 0.106 ms (0.8×) | 0.104 ms (0.8×) | 0.111 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.271 ms (2.4×) | 0.107 ms (1.0×) | 0.104 ms (0.9×) | 0.105 ms (0.9×) | 0.103 ms (0.9×) | 0.112 ms | 0.095 ms (0.8×) | 0.086 ms (0.8×) | 0.086 ms (0.8×) | 0.084 ms (0.8×) | 0.101 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.245 ms (1.0×) | 0.239 ms (1.0×) | 0.242 ms (1.0×) | 0.245 ms (1.0×) | 0.232 ms (1.0×) | 0.241 ms | 0.111 ms (0.5×) | 0.112 ms (0.5×) | 0.112 ms (0.5×) | 0.110 ms (0.5×) | 0.121 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms (1.3×) | 0.539 ms (1.0×) | 0.559 ms (1.0×) | 0.550 ms (1.0×) | 0.533 ms (1.0×) | 0.559 ms | 0.378 ms (0.7×) | 0.381 ms (0.7×) | 0.386 ms (0.7×) | 0.400 ms (0.7×) | 0.415 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.730 ms | 0.531 ms | 0.539 ms | 0.576 ms | — | — | 0.384 ms | 0.373 ms | 0.379 ms | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.563 ms (1.8×) | 0.260 ms (0.8×) | 0.261 ms (0.8×) | 0.261 ms (0.8×) | 0.260 ms (0.8×) | 0.310 ms | 0.259 ms (0.8×) | 0.260 ms (0.8×) | 0.266 ms (0.9×) | 0.260 ms (0.8×) | 0.302 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.6×) | 0.338 ms (0.8×) | 0.348 ms (0.9×) | 0.349 ms (0.9×) | 0.342 ms (0.9×) | 0.401 ms | 0.250 ms (0.6×) | 0.265 ms (0.7×) | 0.264 ms (0.7×) | 0.255 ms (0.6×) | 0.302 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 9 434 op/s (103%) | 8 850 op/s (96.5%) | 9 434 op/s (103%) | 9 434 op/s (103%) | 9 434 op/s (103%) | 9 174 op/s | 8 929 op/s (97.3%) | 9 434 op/s (103%) | 9 434 op/s (103%) | 9 434 op/s (103%) | 9 091 op/s (99.1%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 7 752 op/s (107%) | 7 463 op/s (103%) | 7 463 op/s (103%) | 7 407 op/s (102%) | 7 463 op/s (103%) | 7 246 op/s | 9 259 op/s (128%) | 9 524 op/s (131%) | 9 434 op/s (130%) | 9 615 op/s (133%) | 9 009 op/s (124%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 3 690 op/s (41.3%) | 9 346 op/s (105%) | 9 615 op/s (108%) | 9 524 op/s (107%) | 9 709 op/s (109%) | 8 929 op/s | 10 526 op/s (118%) | 11 628 op/s (130%) | 11 628 op/s (130%) | 11 905 op/s (133%) | 9 901 op/s (111%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 082 op/s (98.4%) | 4 184 op/s (101%) | 4 132 op/s (99.6%) | 4 082 op/s (98.4%) | 4 310 op/s (104%) | 4 149 op/s | 9 009 op/s (217%) | 8 929 op/s (215%) | 8 929 op/s (215%) | 9 091 op/s (219%) | 8 264 op/s (199%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s (76.6%) | 1 855 op/s (104%) | 1 789 op/s (100%) | 1 818 op/s (102%) | 1 876 op/s (105%) | 1 789 op/s | 2 646 op/s (148%) | 2 625 op/s (147%) | 2 591 op/s (145%) | 2 500 op/s (140%) | 2 410 op/s (135%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 370 op/s | 1 883 op/s | 1 855 op/s | 1 736 op/s | — | — | 2 604 op/s | 2 681 op/s | 2 639 op/s | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 776 op/s (55.1%) | 3 846 op/s (119%) | 3 831 op/s (119%) | 3 831 op/s (119%) | 3 846 op/s (119%) | 3 226 op/s | 3 861 op/s (120%) | 3 846 op/s (119%) | 3 759 op/s (117%) | 3 846 op/s (119%) | 3 311 op/s (103%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (62.8%) | 2 959 op/s (119%) | 2 874 op/s (115%) | 2 865 op/s (115%) | 2 924 op/s (117%) | 2 494 op/s | 4 000 op/s (160%) | 3 774 op/s (151%) | 3 788 op/s (152%) | 3 922 op/s (157%) | 3 311 op/s (133%) |

![1.4.1 SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-1-scc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 1.130 ms (1.0×) | 1.030 ms (0.9×) | 1.021 ms (0.9×) | 1.118 ms (1.0×) | 1.158 ms | 1.117 ms (1.0×) | 1.025 ms (0.9×) | 1.030 ms (0.9×) | 1.027 ms (0.9×) | 1.060 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.415 ms (1.0×) | 2.192 ms (0.9×) | 2.146 ms (0.9×) | 2.236 ms (0.9×) | 2.279 ms (0.9×) | 2.451 ms | 1.038 ms (0.4×) | 1.035 ms (0.4×) | 1.052 ms (0.4×) | 1.040 ms (0.4×) | 1.091 ms (0.4×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 11.69 ms (5.4×) | 2.240 ms (1.0×) | 2.122 ms (1.0×) | 2.153 ms (1.0×) | 2.159 ms (1.0×) | 2.161 ms | 1.413 ms (0.7×) | 1.305 ms (0.6×) | 1.303 ms (0.6×) | 1.285 ms (0.6×) | 1.354 ms (0.6×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.872 ms (1.1×) | 4.329 ms (1.0×) | 4.447 ms (1.0×) | 4.617 ms (1.1×) | 4.400 ms (1.0×) | 4.389 ms | 2.247 ms (0.5×) | 2.265 ms (0.5×) | 2.220 ms (0.5×) | 2.152 ms (0.5×) | 2.246 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.957 ms (1.1×) | 7.389 ms (0.8×) | 7.397 ms (0.8×) | 8.010 ms (0.9×) | 7.751 ms (0.8×) | 9.265 ms | 4.475 ms (0.5×) | 4.431 ms (0.5×) | 4.693 ms (0.5×) | 4.474 ms (0.5×) | 4.946 ms (0.5×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 7.595 ms | 7.548 ms | 8.094 ms | — | — | 4.423 ms | 4.537 ms | 4.517 ms | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.702 ms (1.9×) | 2.623 ms (0.9×) | 2.606 ms (0.9×) | 2.509 ms (0.8×) | 2.614 ms (0.9×) | 3.058 ms | 2.625 ms (0.9×) | 2.598 ms (0.8×) | 2.645 ms (0.9×) | 2.616 ms (0.9×) | 2.936 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 7.381 ms (1.2×) | 4.697 ms (0.7×) | 4.820 ms (0.8×) | 4.904 ms (0.8×) | 5.033 ms (0.8×) | 6.351 ms | 2.542 ms (0.4×) | 2.596 ms (0.4×) | 2.789 ms (0.4×) | 2.732 ms (0.4×) | 3.040 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 885.0 op/s (102%) | 970.9 op/s (112%) | 979.4 op/s (113%) | 894.5 op/s (104%) | 863.6 op/s | 895.3 op/s (104%) | 975.6 op/s (113%) | 970.9 op/s (112%) | 973.7 op/s (113%) | 943.4 op/s (109%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 414.1 op/s (101%) | 456.2 op/s (112%) | 466.0 op/s (114%) | 447.2 op/s (110%) | 438.8 op/s (108%) | 408.0 op/s | 963.4 op/s (236%) | 966.2 op/s (237%) | 950.6 op/s (233%) | 961.5 op/s (236%) | 916.6 op/s (225%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 85.56 op/s (18.5%) | 446.4 op/s (96.5%) | 471.3 op/s (102%) | 464.5 op/s (100%) | 463.2 op/s (100%) | 462.7 op/s | 707.7 op/s (153%) | 766.3 op/s (166%) | 767.5 op/s (166%) | 778.2 op/s (168%) | 738.6 op/s (160%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 205.3 op/s (90.1%) | 231.0 op/s (101%) | 224.9 op/s (98.7%) | 216.6 op/s (95.1%) | 227.3 op/s (99.8%) | 227.8 op/s | 445.0 op/s (195%) | 441.5 op/s (194%) | 450.5 op/s (198%) | 464.7 op/s (204%) | 445.2 op/s (195%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 100.4 op/s (93.1%) | 135.3 op/s (125%) | 135.2 op/s (125%) | 124.8 op/s (116%) | 129.0 op/s (120%) | 107.9 op/s | 223.5 op/s (207%) | 225.7 op/s (209%) | 213.1 op/s (197%) | 223.5 op/s (207%) | 202.2 op/s (187%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | — | 131.7 op/s | 132.5 op/s | 123.5 op/s | — | — | 226.1 op/s | 220.4 op/s | 221.4 op/s | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 175.4 op/s (53.6%) | 381.2 op/s (117%) | 383.7 op/s (117%) | 398.6 op/s (122%) | 382.6 op/s (117%) | 327.0 op/s | 381.0 op/s (116%) | 384.9 op/s (118%) | 378.1 op/s (116%) | 382.3 op/s (117%) | 340.6 op/s (104%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 135.5 op/s (86.0%) | 212.9 op/s (135%) | 207.5 op/s (132%) | 203.9 op/s (130%) | 198.7 op/s (126%) | 157.5 op/s | 393.4 op/s (250%) | 385.2 op/s (245%) | 358.6 op/s (228%) | 366.0 op/s (232%) | 328.9 op/s (209%) |

![1.4.2 SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-2-scc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 77.49 ms (0.7×) | 76.73 ms (0.7×) | 86.77 ms (0.7×) | 83.52 ms (0.7×) | 117.5 ms | 26.75 ms (0.2×) | 26.04 ms (0.2×) | 25.87 ms (0.2×) | 26.19 ms (0.2×) | 31.28 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 238.7 ms (0.9×) | 233.4 ms (0.9×) | 246.1 ms (0.9×) | 239.7 ms (0.9×) | 267.9 ms | — | 61.63 ms (0.2×) | 61.93 ms (0.2×) | 61.88 ms (0.2×) | 70.19 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 232.9 ms | 229.7 ms | 248.1 ms | — | — | 66.11 ms | 62.45 ms | 62.57 ms | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.36 ms (0.8×) | 26.24 ms (0.9×) | 26.51 ms (0.9×) | 26.41 ms (0.9×) | 30.70 ms | — | 26.27 ms (0.9×) | 26.54 ms (0.9×) | 26.47 ms (0.9×) | 31.18 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 12.91 op/s (152%) | 13.03 op/s (153%) | 11.52 op/s (135%) | 11.97 op/s (141%) | 8.511 op/s | 37.38 op/s (439%) | 38.41 op/s (451%) | 38.65 op/s (454%) | 38.19 op/s (449%) | 31.97 op/s (376%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.189 op/s (112%) | 4.284 op/s (115%) | 4.063 op/s (109%) | 4.172 op/s (112%) | 3.733 op/s | — | 16.23 op/s (435%) | 16.15 op/s (433%) | 16.16 op/s (433%) | 14.25 op/s (382%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.294 op/s | 4.354 op/s | 4.031 op/s | — | — | 15.13 op/s | 16.01 op/s | 15.98 op/s | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 39.43 op/s (121%) | 38.10 op/s (117%) | 37.72 op/s (116%) | 37.87 op/s (116%) | 32.58 op/s | — | 38.06 op/s (117%) | 37.67 op/s (116%) | 37.78 op/s (116%) | 32.07 op/s (98.5%) |

![1.4.3 SCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-3-scc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 3085.1 ms (1.0×) | 3051.6 ms (1.0×) | 3120.3 ms (1.0×) | 3055.4 ms (1.0×) | 3155.9 ms | 739.3 ms (0.2×) | 700.8 ms (0.2×) | 757.2 ms (0.2×) | 716.1 ms (0.2×) | 773.9 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 6301.4 ms (0.9×) | 6518.8 ms (1.0×) | 6783.8 ms (1.0×) | 6885.2 ms (1.0×) | 6771.3 ms | — | 2978.2 ms (0.4×) | 3038.0 ms (0.4×) | 2873.4 ms (0.4×) | 3192.5 ms (0.5×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 6297.6 ms | 6583.3 ms | 6754.9 ms | — | — | 2740.8 ms | 2700.6 ms | 3578.0 ms | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 268.4 ms (0.8×) | 275.9 ms (0.9×) | 256.3 ms (0.8×) | 283.3 ms (0.9×) | 318.4 ms | — | 271.5 ms (0.9×) | 274.0 ms (0.9×) | 281.5 ms (0.9×) | 322.3 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.324 op/s (102%) | 0.328 op/s (103%) | 0.320 op/s (101%) | 0.327 op/s (103%) | 0.317 op/s | 1.353 op/s (427%) | 1.427 op/s (450%) | 1.321 op/s (417%) | 1.396 op/s (441%) | 1.292 op/s (408%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s (107%) | 0.153 op/s (104%) | 0.147 op/s (99.8%) | 0.145 op/s (98.3%) | 0.148 op/s | — | 0.336 op/s (227%) | 0.329 op/s (223%) | 0.348 op/s (236%) | 0.313 op/s (212%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.159 op/s | 0.152 op/s | 0.148 op/s | — | — | 0.365 op/s | 0.370 op/s | 0.279 op/s | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.726 op/s (119%) | 3.625 op/s (115%) | 3.902 op/s (124%) | 3.530 op/s (112%) | 3.141 op/s | — | 3.683 op/s (117%) | 3.650 op/s (116%) | 3.552 op/s (113%) | 3.103 op/s (98.8%) |

![1.4.4 SCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-4-scc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 36814 ms (1.0×) | 36265 ms (1.0×) | 36545 ms (1.0×) | 36155 ms (1.0×) | 37032 ms | 9963.5 ms (0.3×) | 9839.3 ms (0.3×) | 9918.4 ms (0.3×) | 9842.1 ms (0.3×) | 10158 ms (0.3×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.027 op/s (101%) | 0.028 op/s (102%) | 0.027 op/s (101%) | 0.028 op/s (102%) | 0.027 op/s | 0.100 op/s (372%) | 0.102 op/s (376%) | 0.101 op/s (373%) | 0.102 op/s (376%) | 0.098 op/s (365%) |

![1.4.5 SCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-4-5-scc-n-100-000-000.svg)

### 1.5 WCC

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (1.0×) | 0.036 ms (0.9×) | 0.037 ms (1.0×) | 0.036 ms (0.9×) | 0.040 ms (1.1×) | 0.038 ms | 0.042 ms (1.1×) | 0.040 ms (1.1×) | 0.042 ms (1.1×) | 0.036 ms (0.9×) | 0.037 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.046 ms (1.0×) | 0.046 ms (1.0×) | 0.053 ms (1.1×) | 0.046 ms (1.0×) | 0.056 ms (1.2×) | 0.047 ms | 0.042 ms (0.9×) | 0.042 ms (0.9×) | 0.042 ms (0.9×) | 0.042 ms (0.9×) | 0.042 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.038 ms (0.9×) | 0.038 ms (0.9×) | 0.038 ms (0.9×) | 0.038 ms (0.9×) | 0.038 ms (0.9×) | 0.044 ms | 0.038 ms (0.9×) | 0.044 ms (1.0×) | 0.039 ms (0.9×) | 0.039 ms (0.9×) | 0.040 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.095 ms (1.1×) | 0.083 ms (1.0×) | 0.097 ms (1.1×) | 0.102 ms (1.2×) | 0.095 ms (1.1×) | 0.087 ms | 0.086 ms (1.0×) | 0.082 ms (0.9×) | 0.081 ms (0.9×) | 0.089 ms (1.0×) | 0.083 ms (1.0×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.530 ms (1.0×) | 0.524 ms (1.0×) | 0.523 ms (1.0×) | 0.531 ms (1.0×) | 0.513 ms (1.0×) | 0.516 ms | 0.524 ms (1.0×) | 0.520 ms (1.0×) | 0.537 ms (1.0×) | 0.512 ms (1.0×) | 0.515 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.525 ms | 0.524 ms | 0.525 ms | 0.528 ms | — | — | 0.527 ms | 0.521 ms | 0.525 ms | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.579 ms (1.0×) | 0.581 ms (1.0×) | 0.572 ms (1.0×) | 0.572 ms | 0.578 ms (1.0×) | 0.578 ms (1.0×) | 0.589 ms (1.0×) | 0.571 ms (1.0×) | 0.573 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.640 ms (1.0×) | 0.640 ms (1.0×) | 0.641 ms (1.0×) | 0.648 ms (1.0×) | 0.633 ms (1.0×) | 0.632 ms | 0.581 ms (0.9×) | 0.614 ms (1.0×) | 0.620 ms (1.0×) | 0.572 ms (0.9×) | 0.610 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (103%) | 27 778 op/s (106%) | 27 027 op/s (103%) | 27 778 op/s (106%) | 25 000 op/s (95.0%) | 26 316 op/s | 23 810 op/s (90.5%) | 25 000 op/s (95.0%) | 23 810 op/s (90.5%) | 27 778 op/s (106%) | 27 027 op/s (103%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 21 739 op/s (102%) | 21 739 op/s (102%) | 18 868 op/s (88.7%) | 21 739 op/s (102%) | 17 857 op/s (83.9%) | 21 277 op/s | 23 810 op/s (112%) | 23 810 op/s (112%) | 23 810 op/s (112%) | 23 810 op/s (112%) | 23 810 op/s (112%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 26 316 op/s (116%) | 26 316 op/s (116%) | 26 316 op/s (116%) | 26 316 op/s (116%) | 26 316 op/s (116%) | 22 727 op/s | 26 316 op/s (116%) | 22 727 op/s (100%) | 25 641 op/s (113%) | 25 641 op/s (113%) | 25 000 op/s (110%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 10 526 op/s (91.6%) | 12 048 op/s (105%) | 10 309 op/s (89.7%) | 9 804 op/s (85.3%) | 10 526 op/s (91.6%) | 11 494 op/s | 11 628 op/s (101%) | 12 195 op/s (106%) | 12 346 op/s (107%) | 11 236 op/s (97.8%) | 12 048 op/s (105%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 887 op/s (97.4%) | 1 908 op/s (98.5%) | 1 912 op/s (98.7%) | 1 883 op/s (97.2%) | 1 949 op/s (101%) | 1 938 op/s | 1 908 op/s (98.5%) | 1 923 op/s (99.2%) | 1 862 op/s (96.1%) | 1 953 op/s (101%) | 1 942 op/s (100%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 905 op/s | 1 908 op/s | 1 905 op/s | 1 894 op/s | — | — | 1 898 op/s | 1 919 op/s | 1 905 op/s | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 730 op/s (99.0%) | 1 730 op/s (99.0%) | 1 727 op/s (98.8%) | 1 721 op/s (98.5%) | 1 748 op/s (100%) | 1 748 op/s | 1 730 op/s (99.0%) | 1 730 op/s (99.0%) | 1 698 op/s (97.1%) | 1 751 op/s (100%) | 1 745 op/s (99.8%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 562 op/s (98.8%) | 1 562 op/s (98.8%) | 1 560 op/s (98.6%) | 1 543 op/s (97.5%) | 1 580 op/s (99.8%) | 1 582 op/s | 1 721 op/s (109%) | 1 629 op/s (103%) | 1 613 op/s (102%) | 1 748 op/s (110%) | 1 639 op/s (104%) |

![1.5.1 WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-1-wcc-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.393 ms (1.0×) | 0.398 ms (1.0×) | 0.406 ms (1.0×) | 0.395 ms (1.0×) | 0.395 ms (1.0×) | 0.406 ms | 0.400 ms (1.0×) | 0.352 ms (0.9×) | 0.357 ms (0.9×) | 0.400 ms (1.0×) | 0.405 ms (1.0×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.228 ms (1.0×) | 1.232 ms (1.0×) | 1.291 ms (1.0×) | 1.228 ms (1.0×) | 1.233 ms | 0.390 ms (0.3×) | 0.347 ms (0.3×) | 0.356 ms (0.3×) | 0.392 ms (0.3×) | 0.401 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.661 ms (1.0×) | 0.636 ms (0.9×) | 0.669 ms (1.0×) | 0.665 ms (1.0×) | 0.632 ms (0.9×) | 0.677 ms | 0.732 ms (1.1×) | 0.770 ms (1.1×) | 0.732 ms (1.1×) | 0.713 ms (1.1×) | 0.733 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.769 ms (1.0×) | 1.767 ms (1.0×) | 1.794 ms (1.0×) | 1.800 ms (1.0×) | 1.772 ms (1.0×) | 1.767 ms | 1.550 ms (0.9×) | 1.561 ms (0.9×) | 1.572 ms (0.9×) | 1.525 ms (0.9×) | 1.566 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.082 ms (1.0×) | 6.090 ms (1.0×) | 6.096 ms (1.0×) | 6.096 ms (1.0×) | 6.078 ms (1.0×) | 6.139 ms | 6.069 ms (1.0×) | 6.054 ms (1.0×) | 6.263 ms (1.0×) | 6.004 ms (1.0×) | 6.131 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.163 ms | 6.091 ms | 6.088 ms | 6.158 ms | — | — | 6.163 ms | 6.118 ms | 6.194 ms | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.858 ms (1.0×) | 6.000 ms (1.0×) | 5.778 ms (1.0×) | 6.479 ms (1.1×) | 5.791 ms (1.0×) | 5.724 ms | 5.927 ms (1.0×) | 5.764 ms (1.0×) | 6.018 ms (1.1×) | 5.672 ms (1.0×) | 5.744 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.955 ms (1.0×) | 5.953 ms (1.0×) | 5.988 ms (1.0×) | 6.054 ms (1.0×) | 5.883 ms (1.0×) | 6.045 ms | 6.021 ms (1.0×) | 6.133 ms (1.0×) | 5.936 ms (1.0×) | 5.888 ms (1.0×) | 6.146 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 545 op/s (103%) | 2 513 op/s (102%) | 2 463 op/s (100%) | 2 532 op/s (103%) | 2 532 op/s (103%) | 2 463 op/s | 2 500 op/s (102%) | 2 841 op/s (115%) | 2 801 op/s (114%) | 2 500 op/s (102%) | 2 469 op/s (100%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (100%) | 814.3 op/s (100%) | 811.7 op/s (100%) | 774.6 op/s (95.5%) | 814.3 op/s (100%) | 811.0 op/s | 2 564 op/s (316%) | 2 882 op/s (355%) | 2 809 op/s (346%) | 2 551 op/s (315%) | 2 494 op/s (307%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 513 op/s (102%) | 1 572 op/s (106%) | 1 495 op/s (101%) | 1 504 op/s (102%) | 1 582 op/s (107%) | 1 477 op/s | 1 366 op/s (92.5%) | 1 299 op/s (87.9%) | 1 366 op/s (92.5%) | 1 403 op/s (95.0%) | 1 364 op/s (92.4%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 565.3 op/s (99.9%) | 565.9 op/s (100%) | 557.4 op/s (98.5%) | 555.6 op/s (98.2%) | 564.3 op/s (99.7%) | 565.9 op/s | 645.2 op/s (114%) | 640.6 op/s (113%) | 636.1 op/s (112%) | 655.7 op/s (116%) | 638.6 op/s (113%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 164.4 op/s (101%) | 164.2 op/s (101%) | 164.0 op/s (101%) | 164.0 op/s (101%) | 164.5 op/s (101%) | 162.9 op/s | 164.8 op/s (101%) | 165.2 op/s (101%) | 159.7 op/s (98.0%) | 166.6 op/s (102%) | 163.1 op/s (100%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.3 op/s | 164.2 op/s | 164.3 op/s | 162.4 op/s | — | — | 162.3 op/s | 163.5 op/s | 161.4 op/s | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 170.7 op/s (97.7%) | 166.7 op/s (95.4%) | 173.1 op/s (99.1%) | 154.3 op/s (88.3%) | 172.7 op/s (98.8%) | 174.7 op/s | 168.7 op/s (96.6%) | 173.5 op/s (99.3%) | 166.2 op/s (95.1%) | 176.3 op/s (101%) | 174.1 op/s (99.7%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 167.9 op/s (102%) | 168.0 op/s (102%) | 167.0 op/s (101%) | 165.2 op/s (99.9%) | 170.0 op/s (103%) | 165.4 op/s | 166.1 op/s (100%) | 163.1 op/s (98.6%) | 168.5 op/s (102%) | 169.8 op/s (103%) | 162.7 op/s (98.4%) |

![1.5.2 WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-2-wcc-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 21.74 ms (1.0×) | 21.57 ms (1.0×) | 19.97 ms (0.9×) | 21.40 ms (1.0×) | 21.28 ms | 17.26 ms (0.8×) | 16.24 ms (0.8×) | 16.39 ms (0.8×) | 17.45 ms (0.8×) | 19.21 ms (0.9×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 65.50 ms (1.0×) | 78.43 ms (1.2×) | 67.53 ms (1.0×) | 67.67 ms (1.0×) | 67.51 ms | — | 66.27 ms (1.0×) | 69.74 ms (1.0×) | 66.93 ms (1.0×) | 67.42 ms (1.0×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 70.47 ms | 66.99 ms | 68.44 ms | — | — | 69.61 ms | 66.64 ms | 68.78 ms | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.95 ms (1.0×) | 51.65 ms (1.0×) | 50.92 ms (1.0×) | 50.41 ms (1.0×) | 49.98 ms | — | 50.10 ms (1.0×) | 50.64 ms (1.0×) | 50.15 ms (1.0×) | 50.46 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 46.00 op/s (97.9%) | 46.37 op/s (98.7%) | 50.08 op/s (107%) | 46.73 op/s (99.4%) | 46.99 op/s | 57.94 op/s (123%) | 61.58 op/s (131%) | 61.02 op/s (130%) | 57.29 op/s (122%) | 52.06 op/s (111%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 15.27 op/s (103%) | 12.75 op/s (86.1%) | 14.81 op/s (100.0%) | 14.78 op/s (99.8%) | 14.81 op/s | — | 15.09 op/s (102%) | 14.34 op/s (96.8%) | 14.94 op/s (101%) | 14.83 op/s (100%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.19 op/s | 14.93 op/s | 14.61 op/s | — | — | 14.37 op/s | 15.01 op/s | 14.54 op/s | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.63 op/s (98.1%) | 19.36 op/s (96.8%) | 19.64 op/s (98.2%) | 19.84 op/s (99.2%) | 20.01 op/s | — | 19.96 op/s (99.8%) | 19.75 op/s (98.7%) | 19.94 op/s (99.7%) | 19.82 op/s (99.1%) |

![1.5.3 WCC @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-3-wcc-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 615.6 ms (1.0×) | 600.5 ms (1.0×) | 569.5 ms (0.9×) | 590.8 ms (1.0×) | 606.7 ms | 410.6 ms (0.7×) | 378.4 ms (0.6×) | 447.4 ms (0.7×) | 406.0 ms (0.7×) | 414.5 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4094.7 ms (1.3×) | 4106.6 ms (1.3×) | 4580.5 ms (1.4×) | 3441.2 ms (1.1×) | 3273.0 ms | — | 3774.2 ms (1.2×) | 4418.3 ms (1.3×) | 3251.8 ms (1.0×) | 3301.6 ms (1.0×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3899.0 ms | 3919.9 ms | 3734.0 ms | — | — | 3844.9 ms | 3828.2 ms | 4399.7 ms | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 754.7 ms (1.4×) | 516.1 ms (1.0×) | 594.1 ms (1.1×) | 518.9 ms (1.0×) | 520.9 ms | — | 516.0 ms (1.0×) | 561.6 ms (1.1×) | 521.1 ms (1.0×) | 522.3 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.624 op/s (98.6%) | 1.665 op/s (101%) | 1.756 op/s (107%) | 1.693 op/s (103%) | 1.648 op/s | 2.435 op/s (148%) | 2.643 op/s (160%) | 2.235 op/s (136%) | 2.463 op/s (149%) | 2.413 op/s (146%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.244 op/s (79.9%) | 0.244 op/s (79.7%) | 0.218 op/s (71.5%) | 0.291 op/s (95.1%) | 0.306 op/s | — | 0.265 op/s (86.7%) | 0.226 op/s (74.1%) | 0.308 op/s (101%) | 0.303 op/s (99.1%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s | 0.255 op/s | 0.268 op/s | — | — | 0.260 op/s | 0.261 op/s | 0.227 op/s | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.325 op/s (69.0%) | 1.938 op/s (101%) | 1.683 op/s (87.7%) | 1.927 op/s (100%) | 1.920 op/s | — | 1.938 op/s (101%) | 1.781 op/s (92.8%) | 1.919 op/s (100.0%) | 1.915 op/s (99.7%) |

![1.5.4 WCC @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-4-wcc-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8231.6 ms (0.9×) | 9442.8 ms (1.1×) | 9600.9 ms (1.1×) | 8807.1 ms (1.0×) | 8761.4 ms | 6807.7 ms (0.8×) | 6812.3 ms (0.8×) | 6010.4 ms (0.7×) | 7245.4 ms (0.8×) | 7238.6 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.121 op/s (106%) | 0.106 op/s (92.8%) | 0.104 op/s (91.3%) | 0.114 op/s (99.5%) | 0.114 op/s | 0.147 op/s (129%) | 0.147 op/s (129%) | 0.166 op/s (146%) | 0.138 op/s (121%) | 0.138 op/s (121%) |

![1.5.5 WCC @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-5-5-wcc-n-100-000-000.svg)

### 1.6 CCs

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.037 ms (1.0×) | 0.042 ms (1.1×) | 0.042 ms (1.1×) | 0.043 ms (1.2×) | 0.042 ms (1.1×) | 0.037 ms | 0.036 ms (1.0×) | 0.042 ms (1.1×) | 0.037 ms (1.0×) | 0.036 ms (1.0×) | 0.038 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.053 ms (1.1×) | 0.053 ms (1.1×) | 0.045 ms (1.0×) | 0.058 ms (1.2×) | 0.045 ms (1.0×) | 0.047 ms | 0.036 ms (0.8×) | 0.042 ms (0.9×) | 0.036 ms (0.8×) | 0.041 ms (0.9×) | 0.043 ms (0.9×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.044 ms (1.1×) | 0.038 ms (1.0×) | 0.042 ms (1.1×) | 0.039 ms (1.0×) | 0.044 ms (1.1×) | 0.039 ms | 0.043 ms (1.1×) | 0.038 ms (1.0×) | 0.044 ms (1.1×) | 0.038 ms (1.0×) | 0.040 ms (1.0×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.106 ms (1.2×) | 0.090 ms (1.1×) | 0.094 ms (1.1×) | 0.110 ms (1.3×) | 0.082 ms (1.0×) | 0.085 ms | 0.088 ms (1.0×) | 0.087 ms (1.0×) | 0.087 ms (1.0×) | 0.089 ms (1.0×) | 0.092 ms (1.1×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.518 ms (1.0×) | 0.526 ms (1.0×) | 0.524 ms (1.0×) | 0.530 ms (1.0×) | 0.516 ms (1.0×) | 0.517 ms | 0.520 ms (1.0×) | 0.527 ms (1.0×) | 0.530 ms (1.0×) | 0.512 ms (1.0×) | 0.516 ms (1.0×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.522 ms | 0.524 ms | 0.524 ms | 0.535 ms | — | — | 0.534 ms | 0.521 ms | 0.535 ms | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.586 ms (1.0×) | 0.580 ms (1.0×) | 0.588 ms (1.0×) | 0.586 ms (1.0×) | 0.582 ms (1.0×) | 0.572 ms | 0.584 ms (1.0×) | 0.588 ms (1.0×) | 0.556 ms (1.0×) | 0.569 ms (1.0×) | 0.573 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.639 ms (1.0×) | 0.640 ms (1.0×) | 0.643 ms (1.0×) | 0.648 ms (1.0×) | 0.632 ms (1.0×) | 0.633 ms | 0.625 ms (1.0×) | 0.611 ms (1.0×) | 0.625 ms (1.0×) | 0.607 ms (1.0×) | 0.615 ms (1.0×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 27 027 op/s (100%) | 23 810 op/s (88.1%) | 23 810 op/s (88.1%) | 23 256 op/s (86.0%) | 23 810 op/s (88.1%) | 27 027 op/s | 27 778 op/s (103%) | 23 810 op/s (88.1%) | 27 027 op/s (100%) | 27 778 op/s (103%) | 26 316 op/s (97.4%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 868 op/s (88.7%) | 18 868 op/s (88.7%) | 22 222 op/s (104%) | 17 241 op/s (81.0%) | 22 222 op/s (104%) | 21 277 op/s | 27 778 op/s (131%) | 23 810 op/s (112%) | 27 778 op/s (131%) | 24 390 op/s (115%) | 23 256 op/s (109%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 22 727 op/s (88.6%) | 26 316 op/s (103%) | 23 810 op/s (92.9%) | 25 641 op/s (100%) | 22 727 op/s (88.6%) | 25 641 op/s | 23 256 op/s (90.7%) | 26 316 op/s (103%) | 22 727 op/s (88.6%) | 26 316 op/s (103%) | 25 000 op/s (97.5%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 9 434 op/s (80.2%) | 11 111 op/s (94.4%) | 10 638 op/s (90.4%) | 9 091 op/s (77.3%) | 12 195 op/s (104%) | 11 765 op/s | 11 364 op/s (96.6%) | 11 494 op/s (97.7%) | 11 494 op/s (97.7%) | 11 236 op/s (95.5%) | 10 870 op/s (92.4%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 931 op/s (99.8%) | 1 901 op/s (98.3%) | 1 908 op/s (98.7%) | 1 887 op/s (97.5%) | 1 938 op/s (100%) | 1 934 op/s | 1 923 op/s (99.4%) | 1 898 op/s (98.1%) | 1 887 op/s (97.5%) | 1 953 op/s (101%) | 1 938 op/s (100%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 916 op/s | 1 908 op/s | 1 908 op/s | 1 869 op/s | — | — | 1 873 op/s | 1 919 op/s | 1 869 op/s | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 706 op/s (97.6%) | 1 724 op/s (98.6%) | 1 701 op/s (97.3%) | 1 706 op/s (97.6%) | 1 718 op/s (98.3%) | 1 748 op/s | 1 712 op/s (97.9%) | 1 701 op/s (97.3%) | 1 799 op/s (103%) | 1 757 op/s (101%) | 1 745 op/s (99.8%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 565 op/s (99.1%) | 1 562 op/s (98.9%) | 1 555 op/s (98.4%) | 1 543 op/s (97.7%) | 1 582 op/s (100%) | 1 580 op/s | 1 600 op/s (101%) | 1 637 op/s (104%) | 1 600 op/s (101%) | 1 647 op/s (104%) | 1 626 op/s (103%) |

![1.6.1 CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-1-ccs-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.396 ms (1.0×) | 0.403 ms (1.0×) | 0.394 ms (1.0×) | 0.398 ms (1.0×) | 0.359 ms (0.9×) | 0.405 ms | 0.392 ms (1.0×) | 0.394 ms (1.0×) | 0.399 ms (1.0×) | 0.359 ms (0.9×) | 0.370 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.229 ms (1.0×) | 1.326 ms (1.1×) | 1.232 ms (1.0×) | 1.276 ms (1.0×) | 1.314 ms (1.1×) | 1.230 ms | 0.369 ms (0.3×) | 0.365 ms (0.3×) | 0.403 ms (0.3×) | 0.362 ms (0.3×) | 0.401 ms (0.3×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.665 ms (1.0×) | 0.649 ms (1.0×) | 0.664 ms (1.0×) | 0.644 ms (1.0×) | 0.664 ms (1.0×) | 0.648 ms | 0.728 ms (1.1×) | 0.751 ms (1.2×) | 0.720 ms (1.1×) | 0.747 ms (1.2×) | 0.737 ms (1.1×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.762 ms (1.0×) | 1.769 ms (1.0×) | 1.763 ms (1.0×) | 1.836 ms (1.0×) | 1.756 ms (1.0×) | 1.783 ms | 1.501 ms (0.8×) | 1.542 ms (0.9×) | 1.515 ms (0.8×) | 1.559 ms (0.9×) | 1.536 ms (0.9×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 6.173 ms (1.0×) | 6.095 ms (1.0×) | 6.117 ms (1.0×) | 6.177 ms (1.0×) | 6.012 ms (1.0×) | 6.197 ms | 6.057 ms (1.0×) | 6.066 ms (1.0×) | 6.129 ms (1.0×) | 5.998 ms (1.0×) | 6.198 ms (1.0×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 6.157 ms | 6.186 ms | 6.175 ms | 6.303 ms | — | — | 6.029 ms | 6.032 ms | 6.203 ms | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.806 ms (1.0×) | 5.871 ms (1.0×) | 5.816 ms (1.0×) | 5.949 ms (1.0×) | 5.678 ms (1.0×) | 5.753 ms | 5.856 ms (1.0×) | 5.781 ms (1.0×) | 5.961 ms (1.0×) | 5.683 ms (1.0×) | 5.732 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 5.999 ms (1.0×) | 5.949 ms (1.0×) | 5.935 ms (1.0×) | 6.078 ms (1.0×) | 5.926 ms (1.0×) | 6.050 ms | 6.055 ms (1.0×) | 6.115 ms (1.0×) | 6.133 ms (1.0×) | 6.068 ms (1.0×) | 5.782 ms (1.0×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2 525 op/s (102%) | 2 481 op/s (100%) | 2 538 op/s (103%) | 2 513 op/s (102%) | 2 786 op/s (113%) | 2 469 op/s | 2 551 op/s (103%) | 2 538 op/s (103%) | 2 506 op/s (102%) | 2 786 op/s (113%) | 2 703 op/s (109%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 813.7 op/s (100%) | 754.1 op/s (92.8%) | 811.7 op/s (99.8%) | 783.7 op/s (96.4%) | 761.0 op/s (93.6%) | 813.0 op/s | 2 710 op/s (333%) | 2 740 op/s (337%) | 2 481 op/s (305%) | 2 762 op/s (340%) | 2 494 op/s (307%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 504 op/s (97.4%) | 1 541 op/s (99.8%) | 1 506 op/s (97.6%) | 1 553 op/s (101%) | 1 506 op/s (97.6%) | 1 543 op/s | 1 374 op/s (89.0%) | 1 332 op/s (86.3%) | 1 389 op/s (90.0%) | 1 339 op/s (86.7%) | 1 357 op/s (87.9%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 567.5 op/s (101%) | 565.3 op/s (101%) | 567.2 op/s (101%) | 544.7 op/s (97.1%) | 569.5 op/s (102%) | 560.9 op/s | 666.2 op/s (119%) | 648.5 op/s (116%) | 660.1 op/s (118%) | 641.4 op/s (114%) | 651.0 op/s (116%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 162.0 op/s (100%) | 164.1 op/s (102%) | 163.5 op/s (101%) | 161.9 op/s (100%) | 166.3 op/s (103%) | 161.4 op/s | 165.1 op/s (102%) | 164.9 op/s (102%) | 163.2 op/s (101%) | 166.7 op/s (103%) | 161.3 op/s (100.0%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 162.4 op/s | 161.7 op/s | 161.9 op/s | 158.7 op/s | — | — | 165.9 op/s | 165.8 op/s | 161.2 op/s | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 172.2 op/s (99.1%) | 170.3 op/s (98.0%) | 171.9 op/s (98.9%) | 168.1 op/s (96.7%) | 176.1 op/s (101%) | 173.8 op/s | 170.8 op/s (98.2%) | 173.0 op/s (99.5%) | 167.8 op/s (96.5%) | 176.0 op/s (101%) | 174.5 op/s (100%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 166.7 op/s (101%) | 168.1 op/s (102%) | 168.5 op/s (102%) | 164.5 op/s (99.5%) | 168.7 op/s (102%) | 165.3 op/s | 165.2 op/s (99.9%) | 163.5 op/s (98.9%) | 163.1 op/s (98.6%) | 164.8 op/s (99.7%) | 173.0 op/s (105%) |

![1.6.2 CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-2-ccs-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 19.83 ms (0.9×) | 19.65 ms (0.9×) | 20.46 ms (1.0×) | 22.02 ms (1.0×) | 21.33 ms | 17.28 ms (0.8×) | 17.28 ms (0.8×) | 17.11 ms (0.8×) | 16.34 ms (0.8×) | 17.79 ms (0.8×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 79.11 ms (1.0×) | 67.00 ms (0.8×) | 67.57 ms (0.8×) | 68.75 ms (0.8×) | 81.85 ms | — | 69.08 ms (0.8×) | 67.67 ms (0.8×) | 64.90 ms (0.8×) | 66.53 ms (0.8×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 67.63 ms | 66.92 ms | 71.04 ms | — | — | 66.60 ms | 67.00 ms | 69.03 ms | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 50.35 ms (1.0×) | 50.47 ms (1.0×) | 50.58 ms (1.0×) | 49.23 ms (1.0×) | 50.09 ms | — | 50.11 ms (1.0×) | 50.61 ms (1.0×) | 50.53 ms (1.0×) | 49.68 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 50.44 op/s (108%) | 50.88 op/s (109%) | 48.87 op/s (104%) | 45.41 op/s (96.9%) | 46.87 op/s | 57.86 op/s (123%) | 57.86 op/s (123%) | 58.46 op/s (125%) | 61.19 op/s (131%) | 56.21 op/s (120%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 12.64 op/s (103%) | 14.93 op/s (122%) | 14.80 op/s (121%) | 14.55 op/s (119%) | 12.22 op/s | — | 14.48 op/s (118%) | 14.78 op/s (121%) | 15.41 op/s (126%) | 15.03 op/s (123%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 14.79 op/s | 14.94 op/s | 14.08 op/s | — | — | 15.02 op/s | 14.92 op/s | 14.49 op/s | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 19.86 op/s (99.5%) | 19.82 op/s (99.3%) | 19.77 op/s (99.0%) | 20.31 op/s (102%) | 19.96 op/s | — | 19.96 op/s (100.0%) | 19.76 op/s (99.0%) | 19.79 op/s (99.1%) | 20.13 op/s (101%) |

![1.6.3 CCs @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-3-ccs-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 576.6 ms (1.0×) | 602.9 ms (1.0×) | 615.4 ms (1.0×) | 589.4 ms (1.0×) | 606.4 ms | 410.4 ms (0.7×) | 409.6 ms (0.7×) | 445.8 ms (0.7×) | 405.6 ms (0.7×) | 414.4 ms (0.7×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4122.2 ms (1.2×) | 4102.7 ms (1.2×) | 4620.4 ms (1.4×) | 3362.6 ms (1.0×) | 3338.5 ms | — | 3844.3 ms (1.2×) | 4435.6 ms (1.3×) | 3218.0 ms (1.0×) | 3229.5 ms (1.0×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3909.4 ms | 3884.4 ms | 4689.5 ms | — | — | 4002.5 ms | 3959.5 ms | 4352.3 ms | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 569.9 ms (1.1×) | 550.5 ms (1.1×) | 598.6 ms (1.1×) | 518.4 ms (1.0×) | 522.7 ms | — | 595.1 ms (1.1×) | 557.8 ms (1.1×) | 517.2 ms (1.0×) | 521.5 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1.734 op/s (105%) | 1.659 op/s (101%) | 1.625 op/s (98.5%) | 1.697 op/s (103%) | 1.649 op/s | 2.437 op/s (148%) | 2.441 op/s (148%) | 2.243 op/s (136%) | 2.465 op/s (150%) | 2.413 op/s (146%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.243 op/s (81.0%) | 0.244 op/s (81.4%) | 0.216 op/s (72.3%) | 0.297 op/s (99.3%) | 0.300 op/s | — | 0.260 op/s (86.8%) | 0.225 op/s (75.3%) | 0.311 op/s (104%) | 0.310 op/s (103%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.256 op/s | 0.257 op/s | 0.213 op/s | — | — | 0.250 op/s | 0.253 op/s | 0.230 op/s | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.755 op/s (91.7%) | 1.817 op/s (95.0%) | 1.671 op/s (87.3%) | 1.929 op/s (101%) | 1.913 op/s | — | 1.680 op/s (87.8%) | 1.793 op/s (93.7%) | 1.933 op/s (101%) | 1.918 op/s (100%) |

![1.6.4 CCs @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-4-ccs-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 8479.6 ms (1.0×) | 8563.2 ms (1.0×) | 8594.8 ms (1.0×) | 8738.0 ms (1.0×) | 8837.8 ms | 6840.6 ms (0.8×) | 6788.3 ms (0.8×) | 6004.0 ms (0.7×) | 7211.5 ms (0.8×) | 7216.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.118 op/s (104%) | 0.117 op/s (103%) | 0.116 op/s (103%) | 0.114 op/s (101%) | 0.113 op/s | 0.146 op/s (129%) | 0.147 op/s (130%) | 0.167 op/s (147%) | 0.139 op/s (123%) | 0.139 op/s (122%) |

![1.6.5 CCs @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-6-5-ccs-n-100-000-000.svg)

### 1.7 Condense

#### Times @ 1k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 0.011 ms (1.1×) | 0.011 ms (1.1×) | 0.010 ms (1.0×) | 0.010 ms |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 0.007 ms (1.0×) | 0.008 ms (1.1×) | 0.007 ms (1.0×) | 0.007 ms |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 0.024 ms (1.1×) | 0.024 ms (1.1×) | 0.021 ms (1.0×) | 0.022 ms |
| DAG GNP sparse | 0.017 ms (1.0×) | 0.017 ms (1.0×) | 0.016 ms (0.9×) | 0.017 ms |

#### Throughput @ 1k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | 90 909 op/s (90.9%) | 90 909 op/s (90.9%) | 100 000 op/s (100%) | 100 000 op/s |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | 142 857 op/s (100%) | 125 000 op/s (87.5%) | 142 857 op/s (100%) | 142 857 op/s |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | 41 667 op/s (91.7%) | 41 667 op/s (91.7%) | 47 619 op/s (105%) | 45 455 op/s |
| DAG GNP sparse | 58 824 op/s (100%) | 58 824 op/s (100%) | 62 500 op/s (106%) | 58 824 op/s |

![1.7.1 Condense @ n = 1 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-1-condense-n-1-000.svg)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.080 ms (1.0×) | 0.080 ms (1.0×) | 0.080 ms (1.0×) | 0.081 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.099 ms (1.0×) | 0.092 ms (0.9×) | 0.098 ms (1.0×) | 0.098 ms |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.182 ms (0.9×) | 0.185 ms (0.9×) | 0.200 ms (1.0×) | 0.201 ms |
| DAG GNP sparse | 0.256 ms (1.0×) | 0.261 ms (1.0×) | 0.261 ms (1.0×) | 0.261 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 12 500 op/s (101%) | 12 500 op/s (101%) | 12 500 op/s (101%) | 12 346 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 10 101 op/s (99.0%) | 10 870 op/s (107%) | 10 204 op/s (100%) | 10 204 op/s |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 5 495 op/s (110%) | 5 405 op/s (109%) | 5 000 op/s (100%) | 4 975 op/s |
| DAG GNP sparse | 3 906 op/s (102%) | 3 831 op/s (100%) | 3 831 op/s (100%) | 3 831 op/s |

![1.7.2 Condense @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-2-condense-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.772 ms (1.0×) | 0.768 ms (1.0×) | 0.764 ms (1.0×) | 0.758 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 1.523 ms (0.9×) | 1.591 ms (1.0×) | 1.572 ms (1.0×) | 1.642 ms |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 1.722 ms (0.8×) | 1.852 ms (0.9×) | 1.908 ms (0.9×) | 2.038 ms |
| DAG GNP sparse | 3.542 ms (1.0×) | 3.641 ms (1.0×) | 3.590 ms (1.0×) | 3.603 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 295 op/s (98.2%) | 1 302 op/s (98.7%) | 1 309 op/s (99.2%) | 1 319 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 656.6 op/s (108%) | 628.5 op/s (103%) | 636.1 op/s (104%) | 609.0 op/s |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 580.7 op/s (118%) | 540.0 op/s (110%) | 524.1 op/s (107%) | 490.7 op/s |
| DAG GNP sparse | 282.3 op/s (102%) | 274.6 op/s (99.0%) | 278.6 op/s (100%) | 277.5 op/s |

![1.7.3 Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-7-3-condense-n-100-000.svg)

### 1.8 TopOrder

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.1×) | 0.058 ms (0.9×) | 0.067 ms (1.1×) | 0.063 ms (1.0×) | 0.062 ms | 0.067 ms (1.1×) | 0.066 ms (1.1×) | 0.067 ms (1.1×) | 0.066 ms (1.1×) | 0.054 ms (0.9×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.099 ms (1.5×) | 0.072 ms (1.1×) | 0.070 ms (1.1×) | 0.070 ms (1.1×) | 0.070 ms (1.1×) | 0.066 ms | 0.056 ms (0.8×) | 0.052 ms (0.8×) | 0.052 ms (0.8×) | 0.052 ms (0.8×) | 0.055 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.198 ms (3.4×) | 0.044 ms (0.8×) | 0.056 ms (1.0×) | 0.049 ms (0.8×) | 0.055 ms (0.9×) | 0.058 ms | 0.039 ms (0.7×) | 0.053 ms (0.9×) | 0.037 ms (0.6×) | 0.044 ms (0.8×) | 0.051 ms (0.9×) |
| Tree shfld. | 0.088 ms (1.7×) | 0.052 ms (1.0×) | 0.050 ms (0.9×) | 0.050 ms (0.9×) | 0.050 ms (0.9×) | 0.053 ms | 0.047 ms (0.9×) | 0.047 ms (0.9×) | 0.047 ms (0.9×) | 0.046 ms (0.9×) | 0.048 ms (0.9×) |
| DAG GNP sparse | 0.279 ms (3.6×) | 0.070 ms (0.9×) | 0.084 ms (1.1×) | 0.074 ms (1.0×) | 0.078 ms (1.0×) | 0.077 ms | 0.061 ms (0.8×) | 0.067 ms (0.9×) | 0.064 ms (0.8×) | 0.064 ms (0.8×) | 0.067 ms (0.9×) |
| DAG GNP dense | 0.448 ms (1.3×) | 0.337 ms (1.0×) | 0.324 ms (1.0×) | 0.324 ms (1.0×) | 0.326 ms (1.0×) | 0.336 ms | 0.245 ms (0.7×) | 0.243 ms (0.7×) | 0.245 ms (0.7×) | 0.239 ms (0.7×) | 0.256 ms (0.8×) |
| DAG GNP dense shfld. | 0.560 ms (1.5×) | 0.387 ms (1.0×) | 0.371 ms (1.0×) | 0.366 ms (1.0×) | 0.382 ms (1.0×) | 0.378 ms | 0.244 ms (0.6×) | 0.233 ms (0.6×) | 0.246 ms (0.7×) | 0.243 ms (0.6×) | 0.267 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (107%) | 14 925 op/s (92.5%) | 17 241 op/s (107%) | 14 925 op/s (92.5%) | 15 873 op/s (98.4%) | 16 129 op/s | 14 925 op/s (92.5%) | 15 152 op/s (93.9%) | 14 925 op/s (92.5%) | 15 152 op/s (93.9%) | 18 519 op/s (115%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 10 101 op/s (66.7%) | 13 889 op/s (91.7%) | 14 286 op/s (94.3%) | 14 286 op/s (94.3%) | 14 286 op/s (94.3%) | 15 152 op/s | 17 857 op/s (118%) | 19 231 op/s (127%) | 19 231 op/s (127%) | 19 231 op/s (127%) | 18 182 op/s (120%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 051 op/s (29.3%) | 22 727 op/s (132%) | 17 857 op/s (104%) | 20 408 op/s (118%) | 18 182 op/s (105%) | 17 241 op/s | 25 641 op/s (149%) | 18 868 op/s (109%) | 27 027 op/s (157%) | 22 727 op/s (132%) | 19 608 op/s (114%) |
| Tree shfld. | 11 364 op/s (60.2%) | 19 231 op/s (102%) | 20 000 op/s (106%) | 20 000 op/s (106%) | 20 000 op/s (106%) | 18 868 op/s | 21 277 op/s (113%) | 21 277 op/s (113%) | 21 277 op/s (113%) | 21 739 op/s (115%) | 20 833 op/s (110%) |
| DAG GNP sparse | 3 584 op/s (27.6%) | 14 286 op/s (110%) | 11 905 op/s (91.7%) | 13 514 op/s (104%) | 12 821 op/s (98.7%) | 12 987 op/s | 16 393 op/s (126%) | 14 925 op/s (115%) | 15 625 op/s (120%) | 15 625 op/s (120%) | 14 925 op/s (115%) |
| DAG GNP dense | 2 232 op/s (75.0%) | 2 967 op/s (99.7%) | 3 086 op/s (104%) | 3 086 op/s (104%) | 3 067 op/s (103%) | 2 976 op/s | 4 082 op/s (137%) | 4 115 op/s (138%) | 4 082 op/s (137%) | 4 184 op/s (141%) | 3 906 op/s (131%) |
| DAG GNP dense shfld. | 1 786 op/s (67.5%) | 2 584 op/s (97.7%) | 2 695 op/s (102%) | 2 732 op/s (103%) | 2 618 op/s (99.0%) | 2 646 op/s | 4 098 op/s (155%) | 4 292 op/s (162%) | 4 065 op/s (154%) | 4 115 op/s (156%) | 3 745 op/s (142%) |

![1.8.1 TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-1-toporder-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.539 ms (1.0×) | 0.656 ms (1.3×) | 0.632 ms (1.2×) | 0.632 ms (1.2×) | 0.631 ms (1.2×) | 0.524 ms | 0.662 ms (1.3×) | 0.629 ms (1.2×) | 0.628 ms (1.2×) | 0.631 ms (1.2×) | 0.580 ms (1.1×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.287 ms (1.2×) | 1.051 ms (1.0×) | 0.964 ms (0.9×) | 1.068 ms (1.0×) | 1.080 ms (1.0×) | 1.101 ms | 0.566 ms (0.5×) | 0.521 ms (0.5×) | 0.528 ms (0.5×) | 0.520 ms (0.5×) | 0.665 ms (0.6×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.963 ms (8.4×) | 1.311 ms (1.1×) | 1.440 ms (1.2×) | 1.426 ms (1.2×) | 1.175 ms (1.0×) | 1.191 ms | 1.023 ms (0.9×) | 1.142 ms (1.0×) | 1.182 ms (1.0×) | 1.128 ms (0.9×) | 0.924 ms (0.8×) |
| Tree shfld. | 5.091 ms (3.9×) | 1.487 ms (1.1×) | 1.475 ms (1.1×) | 1.479 ms (1.1×) | 1.473 ms (1.1×) | 1.303 ms | 1.212 ms (0.9×) | 1.242 ms (1.0×) | 1.255 ms (1.0×) | 1.236 ms (0.9×) | 1.099 ms (0.8×) |
| DAG GNP sparse | 9.521 ms (4.4×) | 2.251 ms (1.0×) | 2.201 ms (1.0×) | 2.242 ms (1.0×) | 2.269 ms (1.0×) | 2.185 ms | 1.480 ms (0.7×) | 1.470 ms (0.7×) | 1.499 ms (0.7×) | 1.442 ms (0.7×) | 1.394 ms (0.6×) |
| DAG GNP dense | 6.487 ms (1.3×) | 4.533 ms (0.9×) | 4.333 ms (0.8×) | 4.515 ms (0.9×) | 4.475 ms (0.9×) | 5.141 ms | 3.208 ms (0.6×) | 3.086 ms (0.6×) | 3.127 ms (0.6×) | 3.041 ms (0.6×) | 3.204 ms (0.6×) |
| DAG GNP dense shfld. | 8.053 ms (1.4×) | 5.303 ms (0.9×) | 5.127 ms (0.9×) | 5.063 ms (0.9×) | 5.321 ms (0.9×) | 5.836 ms | 3.254 ms (0.6×) | 3.085 ms (0.5×) | 3.173 ms (0.5×) | 3.130 ms (0.5×) | 3.309 ms (0.6×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 855 op/s (97.2%) | 1 524 op/s (79.9%) | 1 582 op/s (82.9%) | 1 582 op/s (82.9%) | 1 585 op/s (83.0%) | 1 908 op/s | 1 511 op/s (79.2%) | 1 590 op/s (83.3%) | 1 592 op/s (83.4%) | 1 585 op/s (83.0%) | 1 724 op/s (90.3%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 777.0 op/s (85.5%) | 951.5 op/s (105%) | 1 037 op/s (114%) | 936.3 op/s (103%) | 925.9 op/s (102%) | 908.3 op/s | 1 767 op/s (195%) | 1 919 op/s (211%) | 1 894 op/s (209%) | 1 923 op/s (212%) | 1 504 op/s (166%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 100.4 op/s (12.0%) | 762.8 op/s (90.8%) | 694.4 op/s (82.7%) | 701.3 op/s (83.5%) | 851.1 op/s (101%) | 839.6 op/s | 977.5 op/s (116%) | 875.7 op/s (104%) | 846.0 op/s (101%) | 886.5 op/s (106%) | 1 082 op/s (129%) |
| Tree shfld. | 196.4 op/s (25.6%) | 672.5 op/s (87.6%) | 678.0 op/s (88.3%) | 676.1 op/s (88.1%) | 678.9 op/s (88.5%) | 767.5 op/s | 825.1 op/s (108%) | 805.2 op/s (105%) | 796.8 op/s (104%) | 809.1 op/s (105%) | 909.9 op/s (119%) |
| DAG GNP sparse | 105.0 op/s (22.9%) | 444.2 op/s (97.1%) | 454.3 op/s (99.3%) | 446.0 op/s (97.5%) | 440.7 op/s (96.3%) | 457.7 op/s | 675.7 op/s (148%) | 680.3 op/s (149%) | 667.1 op/s (146%) | 693.5 op/s (152%) | 717.4 op/s (157%) |
| DAG GNP dense | 154.2 op/s (79.3%) | 220.6 op/s (113%) | 230.8 op/s (119%) | 221.5 op/s (114%) | 223.5 op/s (115%) | 194.5 op/s | 311.7 op/s (160%) | 324.0 op/s (167%) | 319.8 op/s (164%) | 328.8 op/s (169%) | 312.1 op/s (160%) |
| DAG GNP dense shfld. | 124.2 op/s (72.5%) | 188.6 op/s (110%) | 195.0 op/s (114%) | 197.5 op/s (115%) | 187.9 op/s (110%) | 171.4 op/s | 307.3 op/s (179%) | 324.1 op/s (189%) | 315.2 op/s (184%) | 319.5 op/s (186%) | 302.2 op/s (176%) |

![1.8.2 TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-2-toporder-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 30.49 ms (0.8×) | 30.46 ms (0.8×) | 31.70 ms (0.8×) | 33.89 ms (0.8×) | 40.37 ms | 15.46 ms (0.4×) | 14.97 ms (0.4×) | 15.34 ms (0.4×) | 14.76 ms (0.4×) | 14.15 ms (0.4×) |
| DAG GNP dense | 103.7 ms (0.9×) | 103.6 ms (0.9×) | 106.4 ms (0.9×) | 104.2 ms (0.9×) | 119.5 ms | — | 37.35 ms (0.3×) | 38.21 ms (0.3×) | 38.09 ms (0.3×) | 39.13 ms (0.3×) |
| DAG GNP dense shfld. | 176.2 ms (0.9×) | 173.5 ms (0.9×) | 177.6 ms (0.9×) | 178.7 ms (0.9×) | 197.0 ms | 39.69 ms (0.2×) | 38.60 ms (0.2×) | 39.11 ms (0.2×) | 38.93 ms (0.2×) | 42.15 ms (0.2×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 32.80 op/s (132%) | 32.83 op/s (133%) | 31.55 op/s (127%) | 29.51 op/s (119%) | 24.77 op/s | 64.68 op/s (261%) | 66.82 op/s (270%) | 65.18 op/s (263%) | 67.77 op/s (274%) | 70.69 op/s (285%) |
| DAG GNP dense | 9.643 op/s (115%) | 9.653 op/s (115%) | 9.398 op/s (112%) | 9.597 op/s (115%) | 8.368 op/s | — | 26.77 op/s (320%) | 26.17 op/s (313%) | 26.25 op/s (314%) | 25.56 op/s (305%) |
| DAG GNP dense shfld. | 5.675 op/s (112%) | 5.764 op/s (114%) | 5.631 op/s (111%) | 5.596 op/s (110%) | 5.076 op/s | 25.20 op/s (496%) | 25.90 op/s (510%) | 25.57 op/s (504%) | 25.69 op/s (506%) | 23.73 op/s (467%) |

![1.8.3 TopOrder @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-3-toporder-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1170.9 ms (1.0×) | 1145.1 ms (1.0×) | 1241.1 ms (1.1×) | 1133.7 ms (1.0×) | 1145.6 ms | 160.8 ms (0.1×) | 156.7 ms (0.1×) | 163.7 ms (0.1×) | 154.0 ms (0.1×) | 157.0 ms (0.1×) |
| DAG GNP dense | 1966.4 ms (1.0×) | 1890.5 ms (1.0×) | 2005.5 ms (1.0×) | 1935.8 ms (1.0×) | 1964.4 ms | — | 450.2 ms (0.2×) | 466.3 ms (0.2×) | 466.2 ms (0.2×) | 515.7 ms (0.3×) |
| DAG GNP dense shfld. | 2823.5 ms (1.0×) | 2805.5 ms (1.0×) | 2927.1 ms (1.1×) | 2829.4 ms (1.0×) | 2712.5 ms | 414.3 ms (0.2×) | 405.1 ms (0.1×) | 418.1 ms (0.2×) | 429.6 ms (0.2×) | 420.8 ms (0.2×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.854 op/s (97.8%) | 0.873 op/s (100%) | 0.806 op/s (92.3%) | 0.882 op/s (101%) | 0.873 op/s | 6.219 op/s (712%) | 6.382 op/s (731%) | 6.109 op/s (700%) | 6.494 op/s (744%) | 6.369 op/s (730%) |
| DAG GNP dense | 0.509 op/s (99.9%) | 0.529 op/s (104%) | 0.499 op/s (98.0%) | 0.517 op/s (101%) | 0.509 op/s | — | 2.221 op/s (436%) | 2.145 op/s (421%) | 2.145 op/s (421%) | 1.939 op/s (381%) |
| DAG GNP dense shfld. | 0.354 op/s (96.1%) | 0.356 op/s (96.7%) | 0.342 op/s (92.7%) | 0.353 op/s (95.9%) | 0.369 op/s | 2.414 op/s (655%) | 2.469 op/s (670%) | 2.392 op/s (649%) | 2.328 op/s (631%) | 2.376 op/s (645%) |

![1.8.4 TopOrder @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-4-toporder-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 17135 ms (1.0×) | 17065 ms (1.0×) | 18079 ms (1.1×) | 16836 ms (1.0×) | 16683 ms | 1810.4 ms (0.1×) | 1759.8 ms (0.1×) | 1855.1 ms (0.1×) | 1845.1 ms (0.1×) | 1719.5 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.058 op/s (97.4%) | 0.059 op/s (97.8%) | 0.055 op/s (92.3%) | 0.059 op/s (99.1%) | 0.060 op/s | 0.552 op/s (921%) | 0.568 op/s (948%) | 0.539 op/s (899%) | 0.542 op/s (904%) | 0.582 op/s (970%) |

![1.8.5 TopOrder @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-8-5-toporder-n-100-000-000.svg)

### 1.9 TopOrderAndLevels

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.040 ms (1.0×) | 0.039 ms (1.0×) | 0.040 ms (1.0×) | 0.040 ms | 0.037 ms (0.9×) | 0.040 ms (1.0×) | 0.039 ms (1.0×) | 0.039 ms (1.0×) | 0.040 ms (1.0×) | 0.041 ms (1.0×) | 0.041 ms (1.0×) | 0.040 ms (1.0×) | 0.041 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.055 ms (1.0×) | 0.056 ms (1.0×) | 0.056 ms (1.0×) | 0.057 ms (1.0×) | 0.056 ms (1.0×) | 0.056 ms | 0.038 ms (0.7×) | 0.040 ms (0.7×) | 0.040 ms (0.7×) | 0.039 ms (0.7×) | 0.040 ms (0.7×) | 0.054 ms (1.0×) | 0.055 ms (1.0×) | 0.054 ms (1.0×) | 0.055 ms (1.0×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.034 ms (1.0×) | 0.035 ms (1.0×) | 0.031 ms (0.9×) | 0.035 ms | 0.030 ms (0.9×) | 0.027 ms (0.8×) | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.028 ms (0.8×) | 0.028 ms (0.8×) | 0.029 ms (0.8×) | 0.028 ms (0.8×) | 0.030 ms (0.9×) |
| Tree shfld. | 0.034 ms (1.0×) | 0.031 ms (0.9×) | 0.034 ms (1.0×) | 0.032 ms (0.9×) | 0.031 ms (0.9×) | 0.035 ms | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.031 ms (0.9×) | 0.029 ms (0.8×) | 0.030 ms (0.9×) | 0.030 ms (0.9×) | 0.029 ms (0.8×) | 0.031 ms (0.9×) |
| DAG GNP sparse | 0.052 ms (1.0×) | 0.057 ms (1.1×) | 0.052 ms (1.0×) | 0.061 ms (1.2×) | 0.052 ms (1.0×) | 0.053 ms | 0.050 ms (0.9×) | 0.054 ms (1.0×) | 0.054 ms (1.0×) | 0.054 ms (1.0×) | 0.055 ms (1.0×) | 0.042 ms (0.8×) | 0.045 ms (0.8×) | 0.042 ms (0.8×) | 0.043 ms (0.8×) |
| DAG GNP dense | 0.364 ms (1.0×) | 0.358 ms (1.0×) | 0.355 ms (1.0×) | 0.364 ms (1.0×) | 0.350 ms (1.0×) | 0.357 ms | 0.326 ms (0.9×) | 0.324 ms (0.9×) | 0.334 ms (0.9×) | 0.320 ms (0.9×) | 0.323 ms (0.9×) | 0.240 ms (0.7×) | 0.249 ms (0.7×) | 0.241 ms (0.7×) | 0.238 ms (0.7×) |
| DAG GNP dense shfld. | 0.393 ms (1.0×) | 0.396 ms (1.0×) | 0.400 ms (1.0×) | 0.404 ms (1.0×) | 0.395 ms (1.0×) | 0.390 ms | 0.323 ms (0.8×) | 0.321 ms (0.8×) | 0.328 ms (0.8×) | 0.312 ms (0.8×) | 0.314 ms (0.8×) | 0.289 ms (0.7×) | 0.295 ms (0.8×) | 0.280 ms (0.7×) | 0.286 ms (0.7×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 25 641 op/s (103%) | 25 641 op/s (103%) | 25 000 op/s (100%) | 25 641 op/s (103%) | 25 000 op/s (100%) | 25 000 op/s | 27 027 op/s (108%) | 25 000 op/s (100%) | 25 641 op/s (103%) | 25 641 op/s (103%) | 25 000 op/s (100%) | 24 390 op/s (97.6%) | 24 390 op/s (97.6%) | 25 000 op/s (100%) | 24 390 op/s (97.6%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 18 182 op/s (102%) | 17 857 op/s (100%) | 17 857 op/s (100%) | 17 544 op/s (98.2%) | 17 857 op/s (100%) | 17 857 op/s | 26 316 op/s (147%) | 25 000 op/s (140%) | 25 000 op/s (140%) | 25 641 op/s (144%) | 25 000 op/s (140%) | 18 519 op/s (104%) | 18 182 op/s (102%) | 18 519 op/s (104%) | 18 182 op/s (102%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 32 258 op/s (113%) | 32 258 op/s (113%) | 29 412 op/s (103%) | 28 571 op/s (100%) | 32 258 op/s (113%) | 28 571 op/s | 33 333 op/s (117%) | 37 037 op/s (130%) | 32 258 op/s (113%) | 32 258 op/s (113%) | 35 714 op/s (125%) | 35 714 op/s (125%) | 34 483 op/s (121%) | 35 714 op/s (125%) | 33 333 op/s (117%) |
| Tree shfld. | 29 412 op/s (103%) | 32 258 op/s (113%) | 29 412 op/s (103%) | 31 250 op/s (109%) | 32 258 op/s (113%) | 28 571 op/s | 32 258 op/s (113%) | 32 258 op/s (113%) | 32 258 op/s (113%) | 32 258 op/s (113%) | 34 483 op/s (121%) | 33 333 op/s (117%) | 33 333 op/s (117%) | 34 483 op/s (121%) | 32 258 op/s (113%) |
| DAG GNP sparse | 19 231 op/s (102%) | 17 544 op/s (93.0%) | 19 231 op/s (102%) | 16 393 op/s (86.9%) | 19 231 op/s (102%) | 18 868 op/s | 20 000 op/s (106%) | 18 519 op/s (98.1%) | 18 519 op/s (98.1%) | 18 519 op/s (98.1%) | 18 182 op/s (96.4%) | 23 810 op/s (126%) | 22 222 op/s (118%) | 23 810 op/s (126%) | 23 256 op/s (123%) |
| DAG GNP dense | 2 747 op/s (98.1%) | 2 793 op/s (99.7%) | 2 817 op/s (101%) | 2 747 op/s (98.1%) | 2 857 op/s (102%) | 2 801 op/s | 3 067 op/s (110%) | 3 086 op/s (110%) | 2 994 op/s (107%) | 3 125 op/s (112%) | 3 096 op/s (111%) | 4 167 op/s (149%) | 4 016 op/s (143%) | 4 149 op/s (148%) | 4 202 op/s (150%) |
| DAG GNP dense shfld. | 2 545 op/s (99.2%) | 2 525 op/s (98.5%) | 2 500 op/s (97.5%) | 2 475 op/s (96.5%) | 2 532 op/s (98.7%) | 2 564 op/s | 3 096 op/s (121%) | 3 115 op/s (121%) | 3 049 op/s (119%) | 3 205 op/s (125%) | 3 185 op/s (124%) | 3 460 op/s (135%) | 3 390 op/s (132%) | 3 571 op/s (139%) | 3 497 op/s (136%) |

![1.9.1 TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-1-toporderandlevels-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.562 ms (1.0×) | 0.456 ms (0.8×) | 0.457 ms (0.8×) | 0.462 ms (0.8×) | 0.452 ms (0.8×) | 0.567 ms | 0.560 ms (1.0×) | 0.458 ms (0.8×) | 0.571 ms (1.0×) | 0.547 ms (1.0×) | 0.458 ms (0.8×) | 0.348 ms (0.6×) | 0.360 ms (0.6×) | 0.370 ms (0.7×) | 0.359 ms (0.6×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.463 ms (1.0×) | 1.378 ms (0.9×) | 1.459 ms (1.0×) | 1.424 ms (1.0×) | 1.314 ms (0.9×) | 1.454 ms | 0.559 ms (0.4×) | 0.564 ms (0.4×) | 0.565 ms (0.4×) | 0.450 ms (0.3×) | 0.457 ms (0.3×) | 1.253 ms (0.9×) | 1.335 ms (0.9×) | 1.280 ms (0.9×) | 1.346 ms (0.9×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.934 ms (1.0×) | 0.929 ms (1.0×) | 0.927 ms (1.0×) | 0.947 ms (1.0×) | 0.926 ms (1.0×) | 0.936 ms | 0.863 ms (0.9×) | 0.782 ms (0.8×) | 0.845 ms (0.9×) | 0.783 ms (0.8×) | 0.838 ms (0.9×) | 0.839 ms (0.9×) | 0.852 ms (0.9×) | 0.849 ms (0.9×) | 0.869 ms (0.9×) |
| Tree shfld. | 1.048 ms (1.1×) | 0.945 ms (1.0×) | 1.032 ms (1.1×) | 0.954 ms (1.0×) | 0.951 ms (1.0×) | 0.962 ms | 0.829 ms (0.9×) | 0.891 ms (0.9×) | 0.893 ms (0.9×) | 0.835 ms (0.9×) | 0.839 ms (0.9×) | 0.884 ms (0.9×) | 0.896 ms (0.9×) | 0.917 ms (1.0×) | 0.872 ms (0.9×) |
| DAG GNP sparse | 2.167 ms (1.0×) | 2.211 ms (1.0×) | 2.159 ms (1.0×) | 2.261 ms (1.0×) | 2.153 ms (1.0×) | 2.210 ms | 1.974 ms (0.9×) | 1.962 ms (0.9×) | 1.985 ms (0.9×) | 1.936 ms (0.9×) | 1.940 ms (0.9×) | 1.989 ms (0.9×) | 1.981 ms (0.9×) | 1.931 ms (0.9×) | 1.938 ms (0.9×) |
| DAG GNP dense | 5.160 ms (0.9×) | 5.093 ms (0.9×) | 5.070 ms (0.9×) | 5.364 ms (1.0×) | 5.212 ms (0.9×) | 5.634 ms | 4.574 ms (0.8×) | 4.551 ms (0.8×) | 4.696 ms (0.8×) | 4.618 ms (0.8×) | 5.018 ms (0.9×) | 3.777 ms (0.7×) | 4.360 ms (0.8×) | 3.651 ms (0.6×) | 4.212 ms (0.7×) |
| DAG GNP dense shfld. | 5.877 ms (0.9×) | 5.806 ms (0.9×) | 5.763 ms (0.9×) | 5.866 ms (0.9×) | 6.018 ms (0.9×) | 6.536 ms | 4.496 ms (0.7×) | 4.496 ms (0.7×) | 4.690 ms (0.7×) | 4.535 ms (0.7×) | 4.979 ms (0.8×) | 4.487 ms (0.7×) | 4.509 ms (0.7×) | 4.514 ms (0.7×) | 5.049 ms (0.8×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 779 op/s (101%) | 2 193 op/s (124%) | 2 188 op/s (124%) | 2 165 op/s (123%) | 2 212 op/s (125%) | 1 764 op/s | 1 786 op/s (101%) | 2 183 op/s (124%) | 1 751 op/s (99.3%) | 1 828 op/s (104%) | 2 183 op/s (124%) | 2 874 op/s (163%) | 2 778 op/s (158%) | 2 703 op/s (153%) | 2 786 op/s (158%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 683.5 op/s (99.4%) | 725.7 op/s (106%) | 685.4 op/s (99.7%) | 702.2 op/s (102%) | 761.0 op/s (111%) | 687.8 op/s | 1 789 op/s (260%) | 1 773 op/s (258%) | 1 770 op/s (257%) | 2 222 op/s (323%) | 2 188 op/s (318%) | 798.1 op/s (116%) | 749.1 op/s (109%) | 781.2 op/s (114%) | 742.9 op/s (108%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 071 op/s (100%) | 1 076 op/s (101%) | 1 079 op/s (101%) | 1 056 op/s (98.8%) | 1 080 op/s (101%) | 1 068 op/s | 1 159 op/s (108%) | 1 279 op/s (120%) | 1 183 op/s (111%) | 1 277 op/s (120%) | 1 193 op/s (112%) | 1 192 op/s (112%) | 1 174 op/s (110%) | 1 178 op/s (110%) | 1 151 op/s (108%) |
| Tree shfld. | 954.2 op/s (91.8%) | 1 058 op/s (102%) | 969.0 op/s (93.2%) | 1 048 op/s (101%) | 1 052 op/s (101%) | 1 040 op/s | 1 206 op/s (116%) | 1 122 op/s (108%) | 1 120 op/s (108%) | 1 198 op/s (115%) | 1 192 op/s (115%) | 1 131 op/s (109%) | 1 116 op/s (107%) | 1 091 op/s (105%) | 1 147 op/s (110%) |
| DAG GNP sparse | 461.5 op/s (102%) | 452.3 op/s (100.0%) | 463.2 op/s (102%) | 442.3 op/s (97.7%) | 464.5 op/s (103%) | 452.5 op/s | 506.6 op/s (112%) | 509.7 op/s (113%) | 503.8 op/s (111%) | 516.5 op/s (114%) | 515.5 op/s (114%) | 502.8 op/s (111%) | 504.8 op/s (112%) | 517.9 op/s (114%) | 516.0 op/s (114%) |
| DAG GNP dense | 193.8 op/s (109%) | 196.3 op/s (111%) | 197.2 op/s (111%) | 186.4 op/s (105%) | 191.9 op/s (108%) | 177.5 op/s | 218.6 op/s (123%) | 219.7 op/s (124%) | 212.9 op/s (120%) | 216.5 op/s (122%) | 199.3 op/s (112%) | 264.8 op/s (149%) | 229.4 op/s (129%) | 273.9 op/s (154%) | 237.4 op/s (134%) |
| DAG GNP dense shfld. | 170.2 op/s (111%) | 172.2 op/s (113%) | 173.5 op/s (113%) | 170.5 op/s (111%) | 166.2 op/s (109%) | 153.0 op/s | 222.4 op/s (145%) | 222.4 op/s (145%) | 213.2 op/s (139%) | 220.5 op/s (144%) | 200.8 op/s (131%) | 222.9 op/s (146%) | 221.8 op/s (145%) | 221.5 op/s (145%) | 198.1 op/s (129%) |

![1.9.2 TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-2-toporderandlevels-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 29.60 ms (0.8×) | 29.09 ms (0.8×) | 32.07 ms (0.8×) | 31.72 ms (0.8×) | 38.38 ms | 26.85 ms (0.7×) | 25.49 ms (0.7×) | 27.96 ms (0.7×) | 28.43 ms (0.7×) | 36.68 ms (1.0×) |
| DAG GNP dense | 112.1 ms (0.9×) | 113.1 ms (0.9×) | 112.9 ms (0.9×) | 112.5 ms (0.9×) | 129.6 ms | — | 92.44 ms (0.7×) | 95.81 ms (0.7×) | 93.99 ms (0.7×) | 113.3 ms (0.9×) |
| DAG GNP dense shfld. | 191.1 ms (0.9×) | 190.3 ms (0.9×) | 206.4 ms (1.0×) | 199.4 ms (1.0×) | 209.4 ms | 89.45 ms (0.4×) | 92.20 ms (0.4×) | 96.05 ms (0.5×) | 92.20 ms (0.4×) | 110.4 ms (0.5×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 33.78 op/s (130%) | 34.38 op/s (132%) | 31.18 op/s (120%) | 31.53 op/s (121%) | 26.06 op/s | 37.24 op/s (143%) | 39.23 op/s (151%) | 35.76 op/s (137%) | 35.18 op/s (135%) | 27.26 op/s (105%) |
| DAG GNP dense | 8.921 op/s (116%) | 8.842 op/s (115%) | 8.857 op/s (115%) | 8.889 op/s (115%) | 7.716 op/s | — | 10.82 op/s (140%) | 10.44 op/s (135%) | 10.64 op/s (138%) | 8.826 op/s (114%) |
| DAG GNP dense shfld. | 5.233 op/s (110%) | 5.255 op/s (110%) | 4.845 op/s (101%) | 5.015 op/s (105%) | 4.776 op/s | 11.18 op/s (234%) | 10.85 op/s (227%) | 10.41 op/s (218%) | 10.85 op/s (227%) | 9.058 op/s (190%) |

![1.9.3 TopOrderAndLevels @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-3-toporderandlevels-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 856.2 ms (1.0×) | 853.6 ms (1.0×) | 877.4 ms (1.0×) | 847.7 ms (1.0×) | 860.1 ms | 751.9 ms (0.9×) | 759.4 ms (0.9×) | 772.4 ms (0.9×) | 752.4 ms (0.9×) | 770.8 ms (0.9×) |
| DAG GNP dense | 3085.9 ms (1.0×) | 2971.3 ms (1.0×) | 3289.1 ms (1.1×) | 3082.4 ms (1.0×) | 3105.4 ms | — | 2553.0 ms (0.8×) | 3050.5 ms (1.0×) | 2699.6 ms (0.9×) | 2757.7 ms (0.9×) |
| DAG GNP dense shfld. | 4398.5 ms (1.0×) | 4386.9 ms (1.0×) | 4671.3 ms (1.1×) | 4329.4 ms (1.0×) | 4418.6 ms | 2631.0 ms (0.6×) | 2623.6 ms (0.6×) | 2667.8 ms (0.6×) | 2707.5 ms (0.6×) | 2608.4 ms (0.6×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 1.168 op/s (100%) | 1.172 op/s (101%) | 1.140 op/s (98.0%) | 1.180 op/s (101%) | 1.163 op/s | 1.330 op/s (114%) | 1.317 op/s (113%) | 1.295 op/s (111%) | 1.329 op/s (114%) | 1.297 op/s (112%) |
| DAG GNP dense | 0.324 op/s (101%) | 0.337 op/s (105%) | 0.304 op/s (94.4%) | 0.324 op/s (101%) | 0.322 op/s | — | 0.392 op/s (122%) | 0.328 op/s (102%) | 0.370 op/s (115%) | 0.363 op/s (113%) |
| DAG GNP dense shfld. | 0.227 op/s (100%) | 0.228 op/s (101%) | 0.214 op/s (94.6%) | 0.231 op/s (102%) | 0.226 op/s | 0.380 op/s (168%) | 0.381 op/s (168%) | 0.375 op/s (166%) | 0.369 op/s (163%) | 0.383 op/s (169%) |

![1.9.4 TopOrderAndLevels @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-4-toporderandlevels-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 11384 ms (1.0×) | 11412 ms (1.0×) | 11575 ms (1.0×) | 11214 ms (1.0×) | 11304 ms | 9441.1 ms (0.8×) | 9419.7 ms (0.8×) | 9530.7 ms (0.8×) | 9499.8 ms (0.8×) | 9506.7 ms (0.8×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| DAG GNP sparse | 0.088 op/s (99.3%) | 0.088 op/s (99.1%) | 0.086 op/s (97.7%) | 0.089 op/s (101%) | 0.088 op/s | 0.106 op/s (120%) | 0.106 op/s (120%) | 0.105 op/s (119%) | 0.105 op/s (119%) | 0.105 op/s (119%) |

![1.9.5 TopOrderAndLevels @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-9-5-toporderandlevels-n-100-000-000.svg)

### 1.10 BackEdges

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.047 ms (1.0×) | 0.061 ms (1.2×) | 0.060 ms (1.2×) | 0.052 ms (1.1×) | 0.059 ms (1.2×) | 0.049 ms | 0.061 ms (1.2×) | 0.060 ms (1.2×) | 0.061 ms (1.2×) | 0.059 ms (1.2×) | 0.050 ms (1.0×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.058 ms (0.9×) | 0.067 ms (1.1×) | 0.065 ms (1.0×) | 0.065 ms (1.0×) | 0.065 ms (1.0×) | 0.062 ms | 0.051 ms (0.8×) | 0.050 ms (0.8×) | 0.051 ms (0.8×) | 0.051 ms (0.8×) | 0.051 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.223 ms (5.3×) | 0.049 ms (1.2×) | 0.049 ms (1.2×) | 0.049 ms (1.2×) | 0.048 ms (1.1×) | 0.042 ms | 0.048 ms (1.1×) | 0.046 ms (1.1×) | 0.047 ms (1.1×) | 0.041 ms (1.0×) | 0.038 ms (0.9×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.207 ms (1.1×) | 0.119 ms (0.6×) | 0.164 ms (0.9×) | 0.143 ms (0.8×) | 0.165 ms (0.9×) | 0.190 ms | 0.090 ms (0.5×) | 0.087 ms (0.5×) | 0.093 ms (0.5×) | 0.085 ms (0.4×) | 0.094 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.797 ms (1.0×) | 0.776 ms (1.0×) | 0.771 ms (1.0×) | 0.775 ms (1.0×) | 0.781 ms (1.0×) | 0.806 ms | 0.647 ms (0.8×) | 0.665 ms (0.8×) | 0.658 ms (0.8×) | 0.656 ms (0.8×) | 0.690 ms (0.9×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.811 ms | 0.779 ms | 0.780 ms | 0.776 ms | — | — | 0.669 ms | 0.652 ms | 0.662 ms | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.512 ms (0.8×) | 0.592 ms (1.0×) | 0.582 ms (0.9×) | 0.582 ms (0.9×) | 0.583 ms (0.9×) | 0.616 ms | 0.592 ms (1.0×) | 0.580 ms (0.9×) | 0.596 ms (1.0×) | 0.589 ms (1.0×) | 0.617 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.619 ms (0.9×) | 0.644 ms (1.0×) | 0.642 ms (1.0×) | 0.657 ms (1.0×) | 0.642 ms (1.0×) | 0.656 ms | 0.588 ms (0.9×) | 0.634 ms (1.0×) | 0.588 ms (0.9×) | 0.582 ms (0.9×) | 0.600 ms (0.9×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 21 277 op/s (104%) | 16 393 op/s (80.3%) | 16 667 op/s (81.7%) | 19 231 op/s (94.2%) | 16 949 op/s (83.1%) | 20 408 op/s | 16 393 op/s (80.3%) | 16 667 op/s (81.7%) | 16 393 op/s (80.3%) | 16 949 op/s (83.1%) | 20 000 op/s (98.0%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 17 241 op/s (107%) | 14 925 op/s (92.5%) | 15 385 op/s (95.4%) | 15 385 op/s (95.4%) | 15 385 op/s (95.4%) | 16 129 op/s | 19 608 op/s (122%) | 20 000 op/s (124%) | 19 608 op/s (122%) | 19 608 op/s (122%) | 19 608 op/s (122%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 484 op/s (18.8%) | 20 408 op/s (85.7%) | 20 408 op/s (85.7%) | 20 408 op/s (85.7%) | 20 833 op/s (87.5%) | 23 810 op/s | 20 833 op/s (87.5%) | 21 739 op/s (91.3%) | 21 277 op/s (89.4%) | 24 390 op/s (102%) | 26 316 op/s (111%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 831 op/s (91.8%) | 8 403 op/s (160%) | 6 098 op/s (116%) | 6 993 op/s (133%) | 6 061 op/s (115%) | 5 263 op/s | 11 111 op/s (211%) | 11 494 op/s (218%) | 10 753 op/s (204%) | 11 765 op/s (224%) | 10 638 op/s (202%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 255 op/s (101%) | 1 289 op/s (104%) | 1 297 op/s (105%) | 1 290 op/s (104%) | 1 280 op/s (103%) | 1 241 op/s | 1 546 op/s (125%) | 1 504 op/s (121%) | 1 520 op/s (122%) | 1 524 op/s (123%) | 1 449 op/s (117%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 233 op/s | 1 284 op/s | 1 282 op/s | 1 289 op/s | — | — | 1 495 op/s | 1 534 op/s | 1 511 op/s | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 953 op/s (120%) | 1 689 op/s (104%) | 1 718 op/s (106%) | 1 718 op/s (106%) | 1 715 op/s (106%) | 1 623 op/s | 1 689 op/s (104%) | 1 724 op/s (106%) | 1 678 op/s (103%) | 1 698 op/s (105%) | 1 621 op/s (99.8%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 616 op/s (106%) | 1 553 op/s (102%) | 1 558 op/s (102%) | 1 522 op/s (99.8%) | 1 558 op/s (102%) | 1 524 op/s | 1 701 op/s (112%) | 1 577 op/s (103%) | 1 701 op/s (112%) | 1 718 op/s (113%) | 1 667 op/s (109%) |

![1.10.1 BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-1-backedges-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.507 ms (1.1×) | 0.606 ms (1.3×) | 0.563 ms (1.2×) | 0.568 ms (1.2×) | 0.557 ms (1.2×) | 0.478 ms | 0.605 ms (1.3×) | 0.556 ms (1.2×) | 0.569 ms (1.2×) | 0.558 ms (1.2×) | 0.454 ms (0.9×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.351 ms (1.3×) | 0.945 ms (0.9×) | 1.063 ms (1.0×) | 0.994 ms (1.0×) | 1.042 ms (1.0×) | 1.027 ms | 0.510 ms (0.5×) | 0.513 ms (0.5×) | 0.506 ms (0.5×) | 0.503 ms (0.5×) | 0.524 ms (0.5×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 9.871 ms (8.5×) | 1.327 ms (1.1×) | 1.271 ms (1.1×) | 1.295 ms (1.1×) | 1.315 ms (1.1×) | 1.166 ms | 1.083 ms (0.9×) | 0.872 ms (0.7×) | 1.035 ms (0.9×) | 1.038 ms (0.9×) | 0.903 ms (0.8×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4.691 ms (1.4×) | 3.177 ms (1.0×) | 3.086 ms (1.0×) | 3.124 ms (1.0×) | 3.047 ms (0.9×) | 3.246 ms | 1.962 ms (0.6×) | 1.890 ms (0.6×) | 1.895 ms (0.6×) | 1.962 ms (0.6×) | 1.952 ms (0.6×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 17.38 ms (1.2×) | 12.91 ms (0.9×) | 12.72 ms (0.9×) | 13.51 ms (0.9×) | 13.08 ms (0.9×) | 14.30 ms | 7.303 ms (0.5×) | 7.055 ms (0.5×) | 7.279 ms (0.5×) | 7.220 ms (0.5×) | 7.507 ms (0.5×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 17.38 ms | 12.63 ms | 12.71 ms | 13.88 ms | — | — | 7.353 ms | 7.081 ms | 7.242 ms | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 8.563 ms (1.3×) | 6.365 ms (1.0×) | 6.191 ms (1.0×) | 6.179 ms (0.9×) | 6.134 ms (0.9×) | 6.510 ms | 6.292 ms (1.0×) | 6.108 ms (0.9×) | 6.220 ms (1.0×) | 6.154 ms (0.9×) | 6.525 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 13.28 ms (1.1×) | 11.15 ms (0.9×) | 11.15 ms (0.9×) | 12.63 ms (1.0×) | 11.61 ms (0.9×) | 12.52 ms | 6.545 ms (0.5×) | 6.298 ms (0.5×) | 6.482 ms (0.5×) | 6.395 ms (0.5×) | 6.633 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 972 op/s (94.3%) | 1 650 op/s (78.9%) | 1 776 op/s (84.9%) | 1 761 op/s (84.2%) | 1 795 op/s (85.8%) | 2 092 op/s | 1 653 op/s (79.0%) | 1 799 op/s (86.0%) | 1 757 op/s (84.0%) | 1 792 op/s (85.7%) | 2 203 op/s (105%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 740.2 op/s (76.0%) | 1 058 op/s (109%) | 940.7 op/s (96.6%) | 1 006 op/s (103%) | 959.7 op/s (98.6%) | 973.7 op/s | 1 961 op/s (201%) | 1 949 op/s (200%) | 1 976 op/s (203%) | 1 988 op/s (204%) | 1 908 op/s (196%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 101.3 op/s (11.8%) | 753.6 op/s (87.9%) | 786.8 op/s (91.7%) | 772.2 op/s (90.0%) | 760.5 op/s (88.7%) | 857.6 op/s | 923.4 op/s (108%) | 1 147 op/s (134%) | 966.2 op/s (113%) | 963.4 op/s (112%) | 1 107 op/s (129%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 213.2 op/s (69.2%) | 314.8 op/s (102%) | 324.0 op/s (105%) | 320.1 op/s (104%) | 328.2 op/s (107%) | 308.1 op/s | 509.7 op/s (165%) | 529.1 op/s (172%) | 527.7 op/s (171%) | 509.7 op/s (165%) | 512.3 op/s (166%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s (82.3%) | 77.44 op/s (111%) | 78.61 op/s (112%) | 74.05 op/s (106%) | 76.43 op/s (109%) | 69.93 op/s | 136.9 op/s (196%) | 141.7 op/s (203%) | 137.4 op/s (196%) | 138.5 op/s (198%) | 133.2 op/s (191%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 57.53 op/s | 79.20 op/s | 78.71 op/s | 72.07 op/s | — | — | 136.0 op/s | 141.2 op/s | 138.1 op/s | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 116.8 op/s (76.0%) | 157.1 op/s (102%) | 161.5 op/s (105%) | 161.8 op/s (105%) | 163.0 op/s (106%) | 153.6 op/s | 158.9 op/s (103%) | 163.7 op/s (107%) | 160.8 op/s (105%) | 162.5 op/s (106%) | 153.3 op/s (99.8%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 75.31 op/s (94.3%) | 89.65 op/s (112%) | 89.69 op/s (112%) | 79.16 op/s (99.1%) | 86.14 op/s (108%) | 79.90 op/s | 152.8 op/s (191%) | 158.8 op/s (199%) | 154.3 op/s (193%) | 156.4 op/s (196%) | 150.8 op/s (189%) |

![1.10.2 BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-2-backedges-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 49.47 ms (0.7×) | 45.23 ms (0.7×) | 49.41 ms (0.7×) | 53.11 ms (0.8×) | 68.33 ms | 20.62 ms (0.3×) | 19.54 ms (0.3×) | 19.64 ms (0.3×) | 21.00 ms (0.3×) | 20.93 ms (0.3×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 240.2 ms (0.9×) | 242.3 ms (0.9×) | 251.1 ms (1.0×) | 247.6 ms (1.0×) | 258.1 ms | — | 92.69 ms (0.4×) | 93.71 ms (0.4×) | 89.72 ms (0.3×) | 104.2 ms (0.4×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 241.4 ms | 240.9 ms | 253.4 ms | — | — | 94.11 ms | 90.99 ms | 92.78 ms | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 75.32 ms (0.9×) | 76.46 ms (1.0×) | 77.08 ms (1.0×) | 74.96 ms (0.9×) | 79.86 ms | — | 76.53 ms (1.0×) | 77.78 ms (1.0×) | 76.19 ms (1.0×) | 79.00 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 20.22 op/s (138%) | 22.11 op/s (151%) | 20.24 op/s (138%) | 18.83 op/s (129%) | 14.63 op/s | 48.50 op/s (331%) | 51.17 op/s (350%) | 50.91 op/s (348%) | 47.63 op/s (325%) | 47.77 op/s (326%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.163 op/s (107%) | 4.127 op/s (107%) | 3.982 op/s (103%) | 4.039 op/s (104%) | 3.874 op/s | — | 10.79 op/s (278%) | 10.67 op/s (275%) | 11.15 op/s (288%) | 9.597 op/s (248%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.143 op/s | 4.151 op/s | 3.946 op/s | — | — | 10.63 op/s | 10.99 op/s | 10.78 op/s | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 13.28 op/s (106%) | 13.08 op/s (104%) | 12.97 op/s (104%) | 13.34 op/s (107%) | 12.52 op/s | — | 13.07 op/s (104%) | 12.86 op/s (103%) | 13.12 op/s (105%) | 12.66 op/s (101%) |

![1.10.3 BackEdges @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-3-backedges-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 1984.9 ms (1.0×) | 1952.3 ms (1.0×) | 2052.4 ms (1.0×) | 2025.5 ms (1.0×) | 1996.7 ms | 254.0 ms (0.1×) | 244.4 ms (0.1×) | 244.8 ms (0.1×) | 246.6 ms (0.1×) | 261.7 ms (0.1×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 3708.4 ms (1.0×) | 3722.5 ms (1.0×) | 3843.7 ms (1.0×) | 3661.0 ms (1.0×) | 3711.0 ms | — | 1223.7 ms (0.3×) | 1022.2 ms (0.3×) | 927.2 ms (0.2×) | 976.8 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 3672.0 ms | 3874.8 ms | 3989.1 ms | — | — | 930.6 ms | 1124.4 ms | 1135.8 ms | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 815.0 ms (1.2×) | 839.7 ms (1.2×) | 838.2 ms (1.2×) | 692.1 ms (1.0×) | 689.9 ms | — | 797.1 ms (1.2×) | 802.5 ms (1.2×) | 689.2 ms (1.0×) | 709.6 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.504 op/s (101%) | 0.512 op/s (102%) | 0.487 op/s (97.3%) | 0.494 op/s (98.6%) | 0.501 op/s | 3.937 op/s (786%) | 4.092 op/s (817%) | 4.085 op/s (816%) | 4.055 op/s (810%) | 3.821 op/s (763%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.270 op/s (100%) | 0.269 op/s (99.7%) | 0.260 op/s (96.5%) | 0.273 op/s (101%) | 0.269 op/s | — | 0.817 op/s (303%) | 0.978 op/s (363%) | 1.079 op/s (400%) | 1.024 op/s (380%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.272 op/s | 0.258 op/s | 0.251 op/s | — | — | 1.075 op/s | 0.889 op/s | 0.880 op/s | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 1.227 op/s (84.7%) | 1.191 op/s (82.2%) | 1.193 op/s (82.3%) | 1.445 op/s (99.7%) | 1.449 op/s | — | 1.255 op/s (86.6%) | 1.246 op/s (86.0%) | 1.451 op/s (100%) | 1.409 op/s (97.2%) |

![1.10.4 BackEdges @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-4-backedges-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 27110 ms (1.0×) | 26787 ms (1.0×) | 28157 ms (1.0×) | 27738 ms (1.0×) | 27875 ms | 2988.7 ms (0.1×) | 2799.7 ms (0.1×) | 3364.5 ms (0.1×) | 3076.0 ms (0.1×) | 3042.7 ms (0.1×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.037 op/s (103%) | 0.037 op/s (104%) | 0.036 op/s (99.0%) | 0.036 op/s (100%) | 0.036 op/s | 0.335 op/s (933%) | 0.357 op/s (996%) | 0.297 op/s (829%) | 0.325 op/s (906%) | 0.329 op/s (916%) |

![1.10.5 BackEdges @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-10-5-backedges-n-100-000-000.svg)

### 1.11 EdgeTypes

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.051 ms (0.7×) | 0.064 ms (0.9×) | 0.063 ms (0.8×) | 0.064 ms (0.9×) | 0.064 ms (0.9×) | 0.075 ms | 0.064 ms (0.9×) | 0.064 ms (0.9×) | 0.065 ms (0.9×) | 0.064 ms (0.9×) | 0.066 ms (0.9×) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.066 ms (0.8×) | 0.077 ms (0.9×) | 0.077 ms (0.9×) | 0.077 ms (0.9×) | 0.078 ms (0.9×) | 0.083 ms | 0.059 ms (0.7×) | 0.060 ms (0.7×) | 0.062 ms (0.7×) | 0.060 ms (0.7×) | 0.069 ms (0.8×) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.196 ms (3.9×) | 0.053 ms (1.1×) | 0.053 ms (1.1×) | 0.053 ms (1.1×) | 0.052 ms (1.0×) | 0.050 ms | 0.046 ms (0.9×) | 0.046 ms (0.9×) | 0.047 ms (0.9×) | 0.046 ms (0.9×) | 0.040 ms (0.8×) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.222 ms (1.2×) | 0.183 ms (1.0×) | 0.186 ms (1.0×) | 0.182 ms (1.0×) | 0.166 ms (0.9×) | 0.189 ms | 0.093 ms (0.5×) | 0.094 ms (0.5×) | 0.090 ms (0.5×) | 0.078 ms (0.4×) | 0.086 ms (0.5×) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.653 ms (1.2×) | 0.511 ms (1.0×) | 0.530 ms (1.0×) | 0.533 ms (1.0×) | 0.531 ms (1.0×) | 0.525 ms | 0.373 ms (0.7×) | 0.377 ms (0.7×) | 0.369 ms (0.7×) | 0.384 ms (0.7×) | 0.381 ms (0.7×) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.658 ms | 0.514 ms | 0.525 ms | 0.528 ms | — | — | 0.372 ms | 0.380 ms | 0.382 ms | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.375 ms (1.3×) | 0.282 ms (1.0×) | 0.296 ms (1.1×) | 0.296 ms (1.1×) | 0.293 ms (1.0×) | 0.280 ms | 0.285 ms (1.0×) | 0.297 ms (1.1×) | 0.303 ms (1.1×) | 0.293 ms (1.0×) | 0.282 ms (1.0×) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.499 ms (1.3×) | 0.383 ms (1.0×) | 0.394 ms (1.0×) | 0.402 ms (1.0×) | 0.394 ms (1.0×) | 0.392 ms | 0.276 ms (0.7×) | 0.297 ms (0.8×) | 0.300 ms (0.8×) | 0.295 ms (0.8×) | 0.304 ms (0.8×) |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 19 608 op/s (147%) | 15 625 op/s (117%) | 15 873 op/s (119%) | 15 625 op/s (117%) | 15 625 op/s (117%) | 13 333 op/s | 15 625 op/s (117%) | 15 625 op/s (117%) | 15 385 op/s (115%) | 15 625 op/s (117%) | 15 152 op/s (114%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 15 152 op/s (126%) | 12 987 op/s (108%) | 12 987 op/s (108%) | 12 987 op/s (108%) | 12 821 op/s (106%) | 12 048 op/s | 16 949 op/s (141%) | 16 667 op/s (138%) | 16 129 op/s (134%) | 16 667 op/s (138%) | 14 493 op/s (120%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 102 op/s (25.5%) | 18 868 op/s (94.3%) | 18 868 op/s (94.3%) | 18 868 op/s (94.3%) | 19 231 op/s (96.2%) | 20 000 op/s | 21 739 op/s (109%) | 21 739 op/s (109%) | 21 277 op/s (106%) | 21 739 op/s (109%) | 25 000 op/s (125%) |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 505 op/s (85.1%) | 5 464 op/s (103%) | 5 376 op/s (102%) | 5 495 op/s (104%) | 6 024 op/s (114%) | 5 291 op/s | 10 753 op/s (203%) | 10 638 op/s (201%) | 11 111 op/s (210%) | 12 821 op/s (242%) | 11 628 op/s (220%) |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 531 op/s (80.4%) | 1 957 op/s (103%) | 1 887 op/s (99.1%) | 1 876 op/s (98.5%) | 1 883 op/s (98.9%) | 1 905 op/s | 2 681 op/s (141%) | 2 653 op/s (139%) | 2 710 op/s (142%) | 2 604 op/s (137%) | 2 625 op/s (138%) |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 520 op/s | 1 946 op/s | 1 905 op/s | 1 894 op/s | — | — | 2 688 op/s | 2 632 op/s | 2 618 op/s | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 2 667 op/s (74.7%) | 3 546 op/s (99.3%) | 3 378 op/s (94.6%) | 3 378 op/s (94.6%) | 3 413 op/s (95.6%) | 3 571 op/s | 3 509 op/s (98.2%) | 3 367 op/s (94.3%) | 3 300 op/s (92.4%) | 3 413 op/s (95.6%) | 3 546 op/s (99.3%) |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 2 004 op/s (78.6%) | 2 611 op/s (102%) | 2 538 op/s (99.5%) | 2 488 op/s (97.5%) | 2 538 op/s (99.5%) | 2 551 op/s | 3 623 op/s (142%) | 3 367 op/s (132%) | 3 333 op/s (131%) | 3 390 op/s (133%) | 3 289 op/s (129%) |

![1.11.1 EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-1-edgetypes-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.554 ms (0.9×) | 0.647 ms (1.0×) | 0.607 ms (1.0×) | 0.603 ms (1.0×) | 0.595 ms (0.9×) | 0.629 ms | 0.642 ms (1.0×) | 0.592 ms (0.9×) | 0.605 ms (1.0×) | 0.604 ms (1.0×) | 0.698 ms (1.1×) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.563 ms (1.0×) | 1.365 ms (0.9×) | 1.408 ms (0.9×) | 1.430 ms (1.0×) | 1.386 ms (0.9×) | 1.500 ms | 0.615 ms (0.4×) | 0.629 ms (0.4×) | 0.634 ms (0.4×) | 0.623 ms (0.4×) | 0.643 ms (0.4×) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 10.29 ms (7.5×) | 1.288 ms (0.9×) | 1.480 ms (1.1×) | 1.497 ms (1.1×) | 1.439 ms (1.0×) | 1.375 ms | 1.022 ms (0.7×) | 1.066 ms (0.8×) | 1.071 ms (0.8×) | 1.044 ms (0.8×) | 0.871 ms (0.6×) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 5.006 ms (1.4×) | 3.626 ms (1.0×) | 3.587 ms (1.0×) | 3.696 ms (1.0×) | 3.580 ms (1.0×) | 3.558 ms | 1.957 ms (0.6×) | 1.853 ms (0.5×) | 1.962 ms (0.6×) | 1.830 ms (0.5×) | 1.840 ms (0.5×) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 9.351 ms (1.1×) | 7.009 ms (0.9×) | 6.926 ms (0.8×) | 7.537 ms (0.9×) | 7.214 ms (0.9×) | 8.165 ms | 4.296 ms (0.5×) | 4.197 ms (0.5×) | 4.320 ms (0.5×) | 4.159 ms (0.5×) | 4.314 ms (0.5×) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 9.400 ms | 7.103 ms | 6.963 ms | 7.584 ms | — | — | 4.185 ms | 4.193 ms | 4.299 ms | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4.372 ms (1.7×) | 2.812 ms (1.1×) | 2.706 ms (1.0×) | 2.696 ms (1.0×) | 2.701 ms (1.0×) | 2.616 ms | 2.815 ms (1.1×) | 2.689 ms (1.0×) | 2.754 ms (1.1×) | 2.642 ms (1.0×) | 2.615 ms (1.0×) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.766 ms (1.0×) | 5.303 ms (0.8×) | 5.599 ms (0.8×) | 6.171 ms (0.9×) | 5.809 ms (0.9×) | 6.794 ms | 3.053 ms (0.4×) | 2.982 ms (0.4×) | 2.973 ms (0.4×) | 2.965 ms (0.4×) | 3.402 ms (0.5×) |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1 805 op/s (114%) | 1 546 op/s (97.2%) | 1 647 op/s (104%) | 1 658 op/s (104%) | 1 681 op/s (106%) | 1 590 op/s | 1 558 op/s (98.0%) | 1 689 op/s (106%) | 1 653 op/s (104%) | 1 656 op/s (104%) | 1 433 op/s (90.1%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 639.8 op/s (96.0%) | 732.6 op/s (110%) | 710.2 op/s (107%) | 699.3 op/s (105%) | 721.5 op/s (108%) | 666.7 op/s | 1 626 op/s (244%) | 1 590 op/s (238%) | 1 577 op/s (237%) | 1 605 op/s (241%) | 1 555 op/s (233%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 97.17 op/s (13.4%) | 776.4 op/s (107%) | 675.7 op/s (92.9%) | 668.0 op/s (91.9%) | 694.9 op/s (95.6%) | 727.3 op/s | 978.5 op/s (135%) | 938.1 op/s (129%) | 933.7 op/s (128%) | 957.9 op/s (132%) | 1 148 op/s (158%) |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 199.8 op/s (71.1%) | 275.8 op/s (98.1%) | 278.8 op/s (99.2%) | 270.6 op/s (96.3%) | 279.3 op/s (99.4%) | 281.1 op/s | 511.0 op/s (182%) | 539.7 op/s (192%) | 509.7 op/s (181%) | 546.4 op/s (194%) | 543.5 op/s (193%) |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 106.9 op/s (87.3%) | 142.7 op/s (116%) | 144.4 op/s (118%) | 132.7 op/s (108%) | 138.6 op/s (113%) | 122.5 op/s | 232.8 op/s (190%) | 238.3 op/s (195%) | 231.5 op/s (189%) | 240.4 op/s (196%) | 231.8 op/s (189%) |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 106.4 op/s | 140.8 op/s | 143.6 op/s | 131.9 op/s | — | — | 238.9 op/s | 238.5 op/s | 232.6 op/s | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 228.7 op/s (59.8%) | 355.6 op/s (93.0%) | 369.5 op/s (96.7%) | 370.9 op/s (97.0%) | 370.2 op/s (96.9%) | 382.3 op/s | 355.2 op/s (92.9%) | 371.9 op/s (97.3%) | 363.1 op/s (95.0%) | 378.5 op/s (99.0%) | 382.4 op/s (100%) |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 147.8 op/s (100%) | 188.6 op/s (128%) | 178.6 op/s (121%) | 162.0 op/s (110%) | 172.1 op/s (117%) | 147.2 op/s | 327.5 op/s (223%) | 335.3 op/s (228%) | 336.4 op/s (229%) | 337.3 op/s (229%) | 293.9 op/s (200%) |

![1.11.2 EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-2-edgetypes-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 60.41 ms (0.6×) | 58.24 ms (0.6×) | 69.55 ms (0.7×) | 67.35 ms (0.7×) | 103.5 ms | 23.47 ms (0.2×) | 20.58 ms (0.2×) | 22.69 ms (0.2×) | 21.38 ms (0.2×) | 23.44 ms (0.2×) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 235.5 ms (0.9×) | 238.8 ms (0.9×) | 252.1 ms (1.0×) | 241.3 ms (0.9×) | 261.1 ms | — | 60.05 ms (0.2×) | 60.84 ms (0.2×) | 61.23 ms (0.2×) | 67.87 ms (0.3×) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 231.9 ms | 235.5 ms | 252.0 ms | — | — | 60.28 ms | 60.09 ms | 60.77 ms | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 25.97 ms (0.9×) | 26.59 ms (0.9×) | 26.76 ms (1.0×) | 26.94 ms (1.0×) | 28.06 ms | — | 26.68 ms (1.0×) | 26.80 ms (1.0×) | 26.71 ms (1.0×) | 27.32 ms (1.0×) |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 16.55 op/s (171%) | 17.17 op/s (178%) | 14.38 op/s (149%) | 14.85 op/s (154%) | 9.662 op/s | 42.60 op/s (441%) | 48.59 op/s (503%) | 44.08 op/s (456%) | 46.77 op/s (484%) | 42.66 op/s (441%) |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | 4.246 op/s (111%) | 4.188 op/s (109%) | 3.967 op/s (104%) | 4.144 op/s (108%) | 3.830 op/s | — | 16.65 op/s (435%) | 16.44 op/s (429%) | 16.33 op/s (426%) | 14.73 op/s (385%) |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 4.312 op/s | 4.246 op/s | 3.968 op/s | — | — | 16.59 op/s | 16.64 op/s | 16.46 op/s | — | — |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | 38.51 op/s (108%) | 37.62 op/s (106%) | 37.37 op/s (105%) | 37.12 op/s (104%) | 35.64 op/s | — | 37.48 op/s (105%) | 37.32 op/s (105%) | 37.44 op/s (105%) | 36.60 op/s (103%) |

![1.11.3 EdgeTypes @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-3-edgetypes-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 2629.9 ms (1.0×) | 2492.1 ms (1.0×) | 2557.2 ms (1.0×) | 2500.9 ms (1.0×) | 2524.1 ms | 422.5 ms (0.2×) | 414.7 ms (0.2×) | 447.1 ms (0.2×) | 411.7 ms (0.2×) | 425.6 ms (0.2×) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 4515.8 ms (1.0×) | 4552.4 ms (1.0×) | 4861.4 ms (1.1×) | 4606.3 ms (1.0×) | 4540.2 ms | — | 1515.5 ms (0.3×) | 1624.0 ms (0.4×) | 1435.0 ms (0.3×) | 1449.5 ms (0.3×) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 4526.0 ms | 4492.7 ms | 4853.4 ms | — | — | 1383.2 ms | 1393.9 ms | 1557.3 ms | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 276.6 ms (1.0×) | 281.8 ms (1.0×) | 336.4 ms (1.2×) | 278.7 ms (1.0×) | 281.0 ms | — | 275.9 ms (1.0×) | 284.6 ms (1.0×) | 279.7 ms (1.0×) | 282.8 ms (1.0×) |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.380 op/s (96.0%) | 0.401 op/s (101%) | 0.391 op/s (98.7%) | 0.400 op/s (101%) | 0.396 op/s | 2.367 op/s (597%) | 2.411 op/s (609%) | 2.237 op/s (565%) | 2.429 op/s (613%) | 2.350 op/s (593%) |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s (101%) | 0.220 op/s (99.7%) | 0.206 op/s (93.4%) | 0.217 op/s (98.6%) | 0.220 op/s | — | 0.660 op/s (300%) | 0.616 op/s (280%) | 0.697 op/s (316%) | 0.690 op/s (313%) |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.221 op/s | 0.223 op/s | 0.206 op/s | — | — | 0.723 op/s | 0.717 op/s | 0.642 op/s | — | — |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | 3.615 op/s (102%) | 3.549 op/s (99.7%) | 2.973 op/s (83.5%) | 3.588 op/s (101%) | 3.559 op/s | — | 3.625 op/s (102%) | 3.514 op/s (98.7%) | 3.575 op/s (100%) | 3.536 op/s (99.4%) |

![1.11.4 EdgeTypes @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-4-edgetypes-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 31792 ms (1.0×) | 31282 ms (1.0×) | 32754 ms (1.0×) | 31640 ms (1.0×) | 31522 ms | 7181.5 ms (0.2×) | 6992.4 ms (0.2×) | 7210.5 ms (0.2×) | 6770.4 ms (0.2×) | 6892.8 ms (0.2×) |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.0.0<br>(reordered) | Graphity 2.1.0<br>(reordered) | Graphity 2.2.0<br>(reordered) | Graphity 2.3.0<br>(reordered) | Graphity 2.4.0<br>(reordered) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.031 op/s (99.2%) | 0.032 op/s (101%) | 0.031 op/s (96.2%) | 0.032 op/s (99.6%) | 0.032 op/s | 0.139 op/s (439%) | 0.143 op/s (451%) | 0.139 op/s (437%) | 0.148 op/s (466%) | 0.145 op/s (457%) |

![1.11.5 EdgeTypes @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-11-5-edgetypes-n-100-000-000.svg)

### 1.12 Reorder (DFS-preorder) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.121 ms (1.1×) | 0.110 ms (1.0×) | 0.110 ms (1.0×) | 0.112 ms (1.0×) | 0.108 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.145 ms (1.1×) | 0.133 ms (1.0×) | 0.130 ms (1.0×) | 0.124 ms (0.9×) | 0.135 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.185 ms (2.0×) | 0.087 ms (0.9×) | 0.090 ms (1.0×) | 0.084 ms (0.9×) | 0.093 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.483 ms (1.3×) | 0.386 ms (1.0×) | 0.385 ms (1.0×) | 0.380 ms (1.0×) | 0.373 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.127 ms (1.4×) | 0.821 ms (1.0×) | 0.818 ms (1.0×) | 0.811 ms (1.0×) | 0.827 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.114 ms | 0.812 ms | 0.817 ms | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.715 ms (1.4×) | 0.532 ms (1.0×) | 0.533 ms (1.0×) | 0.519 ms (1.0×) | 0.525 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.867 ms (1.4×) | 0.612 ms (1.0×) | 0.618 ms (1.0×) | 0.614 ms (1.0×) | 0.631 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 8 264 op/s (89.3%) | 9 091 op/s (98.2%) | 9 091 op/s (98.2%) | 8 929 op/s (96.4%) | 9 259 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 6 897 op/s (93.1%) | 7 519 op/s (102%) | 7 692 op/s (104%) | 8 065 op/s (109%) | 7 407 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 5 405 op/s (50.3%) | 11 494 op/s (107%) | 11 111 op/s (103%) | 11 905 op/s (111%) | 10 753 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 2 070 op/s (77.2%) | 2 591 op/s (96.6%) | 2 597 op/s (96.9%) | 2 632 op/s (98.2%) | 2 681 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 887.3 op/s (73.4%) | 1 218 op/s (101%) | 1 222 op/s (101%) | 1 233 op/s (102%) | 1 209 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 897.7 op/s | 1 232 op/s | 1 224 op/s | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 399 op/s (73.4%) | 1 880 op/s (98.7%) | 1 876 op/s (98.5%) | 1 927 op/s (101%) | 1 905 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 153 op/s (72.8%) | 1 634 op/s (103%) | 1 618 op/s (102%) | 1 629 op/s (103%) | 1 585 op/s |

![1.12.1 Reorder (DFS-preorder) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-1-reorder-dfs-preorder-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.259 ms (1.1×) | 1.033 ms (0.9×) | 1.043 ms (0.9×) | 1.099 ms (1.0×) | 1.113 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.144 ms (1.0×) | 1.929 ms (0.9×) | 2.016 ms (1.0×) | 2.113 ms (1.0×) | 2.096 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.144 ms (1.3×) | 2.215 ms (0.9×) | 2.314 ms (0.9×) | 2.463 ms (1.0×) | 2.501 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 6.609 ms (1.2×) | 5.411 ms (1.0×) | 5.436 ms (1.0×) | 5.407 ms (1.0×) | 5.441 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 15.07 ms (1.3×) | 10.81 ms (0.9×) | 11.74 ms (1.0×) | 11.16 ms (0.9×) | 12.03 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 15.00 ms | 10.87 ms | 11.77 ms | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 7.332 ms (1.3×) | 5.374 ms (1.0×) | 5.406 ms (1.0×) | 5.367 ms (1.0×) | 5.510 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 11.95 ms (1.2×) | 8.836 ms (0.9×) | 9.153 ms (0.9×) | 9.224 ms (0.9×) | 10.30 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 794.3 op/s (88.4%) | 968.1 op/s (108%) | 958.8 op/s (107%) | 909.9 op/s (101%) | 898.5 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 466.4 op/s (97.8%) | 518.4 op/s (109%) | 496.0 op/s (104%) | 473.3 op/s (99.2%) | 477.1 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 318.1 op/s (79.5%) | 451.5 op/s (113%) | 432.2 op/s (108%) | 406.0 op/s (102%) | 399.8 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 151.3 op/s (82.3%) | 184.8 op/s (101%) | 184.0 op/s (100%) | 184.9 op/s (101%) | 183.8 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 66.37 op/s (79.8%) | 92.53 op/s (111%) | 85.17 op/s (102%) | 89.61 op/s (108%) | 83.13 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 66.66 op/s | 92.00 op/s | 84.97 op/s | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 136.4 op/s (75.2%) | 186.1 op/s (103%) | 185.0 op/s (102%) | 186.3 op/s (103%) | 181.5 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 83.70 op/s (86.2%) | 113.2 op/s (117%) | 109.3 op/s (113%) | 108.4 op/s (112%) | 97.06 op/s |

![1.12.2 Reorder (DFS-preorder) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-2-reorder-dfs-preorder-cost-n-100-000.svg)

#### Times @ 1M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 95.36 ms (0.7×) | 79.17 ms (0.6×) | 84.36 ms (0.7×) | 87.61 ms (0.7×) | 127.2 ms |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 414.3 ms | 315.0 ms | 336.1 ms | — | — |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 317.2 ms (1.0×) | 336.7 ms (1.0×) | 318.2 ms (1.0×) | 328.2 ms |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 52.67 ms (1.0×) | 53.13 ms (1.0×) | 52.32 ms (1.0×) | 54.61 ms |

#### Throughput @ 1M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | 10.49 op/s (133%) | 12.63 op/s (161%) | 11.85 op/s (151%) | 11.41 op/s (145%) | 7.862 op/s |
| GNP dense shfld.<br><sub>1M nodes, 30M edges</sub> | 2.414 op/s | 3.175 op/s | 2.975 op/s | — | — |
| GNP dense<br><sub>1M nodes, 30M edges</sub> | — | 3.153 op/s (103%) | 2.970 op/s (97.5%) | 3.143 op/s (103%) | 3.047 op/s |
| Chain of cliques<br><sub>1M nodes, 31.1M edges</sub> | — | 18.99 op/s (104%) | 18.82 op/s (103%) | 19.11 op/s (104%) | 18.31 op/s |

![1.12.3 Reorder (DFS-preorder) cost @ n = 1 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-3-reorder-dfs-preorder-cost-n-1-000-000.svg)

#### Times @ 10M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 4011.1 ms (1.2×) | 3263.5 ms (1.0×) | 3401.7 ms (1.1×) | 3194.6 ms (1.0×) | 3228.1 ms |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 10805 ms | 7613.8 ms | 7838.8 ms | — | — |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 7509.7 ms (1.0×) | 7782.3 ms (1.1×) | 7202.3 ms (1.0×) | 7233.0 ms |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 617.1 ms (1.1×) | 622.8 ms (1.1×) | 543.0 ms (1.0×) | 548.8 ms |

#### Throughput @ 10M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | 0.249 op/s (80.5%) | 0.306 op/s (98.9%) | 0.294 op/s (94.9%) | 0.313 op/s (101%) | 0.310 op/s |
| GNP dense shfld.<br><sub>10M nodes, 299.6M edges</sub> | 0.093 op/s | 0.131 op/s | 0.128 op/s | — | — |
| GNP dense<br><sub>10M nodes, 299.6M edges</sub> | — | 0.133 op/s (96.3%) | 0.128 op/s (92.9%) | 0.139 op/s (100%) | 0.138 op/s |
| Chain of cliques<br><sub>10M nodes, 310.8M edges</sub> | — | 1.620 op/s (88.9%) | 1.606 op/s (88.1%) | 1.842 op/s (101%) | 1.822 op/s |

![1.12.4 Reorder (DFS-preorder) cost @ n = 10 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-4-reorder-dfs-preorder-cost-n-10-000-000.svg)

#### Times @ 100M nodes (ms/op, lower is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 49550 ms (1.2×) | 41091 ms (1.0×) | 41896 ms (1.0×) | 40225 ms (1.0×) | 40274 ms |

#### Throughput @ 100M nodes (op/s, higher is better)

| Shape | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | 0.020 op/s (81.3%) | 0.024 op/s (98.0%) | 0.024 op/s (96.1%) | 0.025 op/s (100%) | 0.025 op/s |

![1.12.5 Reorder (DFS-preorder) cost @ n = 100 000 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-12-5-reorder-dfs-preorder-cost-n-100-000-000.svg)

### 1.13 BiAdjacency (in-adjacency) cost

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 0.022 ms (1.0×) | 0.022 ms (1.0×) | 0.015 ms (0.7×) | 0.022 ms |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 0.018 ms (0.8×) | 0.022 ms (1.0×) | 0.023 ms (1.0×) | 0.023 ms |
| Tree<br><sub>10k nodes, 10k edges</sub> | 0.016 ms (0.7×) | 0.023 ms (1.0×) | 0.023 ms (1.0×) | 0.024 ms |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 0.058 ms (1.0×) | 0.058 ms (1.0×) | 0.057 ms (1.0×) | 0.059 ms |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 0.546 ms (1.0×) | 0.548 ms (1.0×) | 0.544 ms (1.0×) | 0.551 ms |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 0.544 ms | 0.547 ms | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 0.545 ms (1.0×) | 0.542 ms (1.0×) | 0.542 ms (1.0×) | 0.547 ms |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 0.548 ms (1.0×) | 0.547 ms (1.0×) | 0.544 ms (1.0×) | 0.551 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 45 455 op/s (100%) | 45 455 op/s (100%) | 66 667 op/s (147%) | 45 455 op/s |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 55 556 op/s (128%) | 45 455 op/s (105%) | 43 478 op/s (100%) | 43 478 op/s |
| Tree<br><sub>10k nodes, 10k edges</sub> | 62 500 op/s (150%) | 43 478 op/s (104%) | 43 478 op/s (104%) | 41 667 op/s |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 17 241 op/s (102%) | 17 241 op/s (102%) | 17 544 op/s (104%) | 16 949 op/s |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1 832 op/s (101%) | 1 825 op/s (101%) | 1 838 op/s (101%) | 1 815 op/s |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1 838 op/s | 1 828 op/s | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1 835 op/s (100%) | 1 845 op/s (101%) | 1 845 op/s (101%) | 1 828 op/s |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1 825 op/s (101%) | 1 828 op/s (101%) | 1 838 op/s (101%) | 1 815 op/s |

![1.13.1 BiAdjacency (in-adjacency) cost @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-13-1-biadjacency-in-adjacency-cost-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 0.185 ms (0.9×) | 0.204 ms (1.0×) | 0.203 ms (1.0×) | 0.206 ms |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 0.238 ms (1.0×) | 0.236 ms (1.0×) | 0.236 ms (1.0×) | 0.241 ms |
| Tree<br><sub>100k nodes, 100k edges</sub> | 0.612 ms (1.0×) | 0.613 ms (1.0×) | 0.606 ms (1.0×) | 0.618 ms |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 0.837 ms (0.6×) | 1.423 ms (1.0×) | 1.408 ms (1.0×) | 1.418 ms |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 7.148 ms (0.9×) | 7.932 ms (1.0×) | 7.385 ms (0.9×) | 7.949 ms |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 7.216 ms | 7.744 ms | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 5.559 ms (1.2×) | 5.555 ms (1.2×) | 5.548 ms (1.2×) | 4.611 ms |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 6.951 ms (0.9×) | 7.824 ms (1.0×) | 7.574 ms (0.9×) | 8.096 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 5 405 op/s (111%) | 4 902 op/s (101%) | 4 926 op/s (101%) | 4 854 op/s |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 202 op/s (101%) | 4 237 op/s (102%) | 4 237 op/s (102%) | 4 149 op/s |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1 634 op/s (101%) | 1 631 op/s (101%) | 1 650 op/s (102%) | 1 618 op/s |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1 195 op/s (169%) | 702.7 op/s (99.6%) | 710.2 op/s (101%) | 705.2 op/s |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 139.9 op/s (111%) | 126.1 op/s (100%) | 135.4 op/s (108%) | 125.8 op/s |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 138.6 op/s | 129.1 op/s | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 179.9 op/s (82.9%) | 180.0 op/s (83.0%) | 180.2 op/s (83.1%) | 216.9 op/s |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 143.9 op/s (116%) | 127.8 op/s (103%) | 132.0 op/s (107%) | 123.5 op/s |

![1.13.2 BiAdjacency (in-adjacency) cost @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-13-2-biadjacency-in-adjacency-cost-n-100-000.svg)

### 1.14 Dominators

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 0.168 ms (0.9×) | 0.165 ms (0.9×) | 0.177 ms |
| Irreducible | 0.178 ms (1.0×) | 0.164 ms (0.9×) | 0.174 ms |
| Ladder | 0.155 ms (1.0×) | 0.159 ms (1.0×) | 0.157 ms |
| Chain | 0.114 ms (1.0×) | 0.113 ms (1.0×) | 0.117 ms |
| Fan behind chain | 0.144 ms (1.0×) | 0.143 ms (1.0×) | 0.143 ms |
| Small methods | 0.201 ms (1.1×) | 0.184 ms (1.0×) | 0.183 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 5 952 op/s (105%) | 6 061 op/s (107%) | 5 650 op/s |
| Irreducible | 5 618 op/s (97.8%) | 6 098 op/s (106%) | 5 747 op/s |
| Ladder | 6 452 op/s (101%) | 6 289 op/s (98.7%) | 6 369 op/s |
| Chain | 8 772 op/s (103%) | 8 850 op/s (104%) | 8 547 op/s |
| Fan behind chain | 6 944 op/s (99.3%) | 6 993 op/s (100%) | 6 993 op/s |
| Small methods | 4 975 op/s (91.0%) | 5 435 op/s (99.5%) | 5 464 op/s |

![1.14.1 Dominators @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-14-1-dominators-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 3.606 ms (1.0×) | 3.546 ms (1.0×) | 3.727 ms |
| Irreducible | 3.848 ms (1.0×) | 3.771 ms (0.9×) | 4.013 ms |
| Ladder | 1.528 ms (1.0×) | 1.465 ms (1.0×) | 1.540 ms |
| Chain | 1.098 ms (1.0×) | 1.096 ms (1.0×) | 1.119 ms |
| Fan behind chain | 1.469 ms (1.0×) | 1.459 ms (1.0×) | 1.414 ms |
| Small methods | 2.946 ms (1.0×) | 2.900 ms (1.0×) | 2.925 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 277.3 op/s (103%) | 282.0 op/s (105%) | 268.3 op/s |
| Irreducible | 259.9 op/s (104%) | 265.2 op/s (106%) | 249.2 op/s |
| Ladder | 654.5 op/s (101%) | 682.6 op/s (105%) | 649.4 op/s |
| Chain | 910.7 op/s (102%) | 912.4 op/s (102%) | 893.7 op/s |
| Fan behind chain | 680.7 op/s (96.3%) | 685.4 op/s (96.9%) | 707.2 op/s |
| Small methods | 339.4 op/s (99.3%) | 344.8 op/s (101%) | 341.9 op/s |

![1.14.2 Dominators @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-14-2-dominators-n-100-000.svg)

### 1.15 Dominance frontiers

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 0.069 ms (1.0×) | 0.069 ms (1.0×) | 0.069 ms |
| Irreducible | 0.083 ms (1.0×) | 0.080 ms (1.0×) | 0.081 ms |
| Ladder | 45.91 ms (1.0×) | 45.03 ms (1.0×) | 45.78 ms |
| Chain | 0.037 ms (1.0×) | 0.036 ms (1.0×) | 0.037 ms |
| Fan behind chain | 105.8 ms (1.0×) | 103.3 ms (1.0×) | 107.1 ms |
| Small methods | 0.057 ms (1.0×) | 0.055 ms (1.0×) | 0.056 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 14 493 op/s (100%) | 14 493 op/s (100%) | 14 493 op/s |
| Irreducible | 12 048 op/s (97.6%) | 12 500 op/s (101%) | 12 346 op/s |
| Ladder | 21.78 op/s (99.7%) | 22.21 op/s (102%) | 21.84 op/s |
| Chain | 27 027 op/s (100%) | 27 778 op/s (103%) | 27 027 op/s |
| Fan behind chain | 9.452 op/s (101%) | 9.681 op/s (104%) | 9.337 op/s |
| Small methods | 17 544 op/s (98.2%) | 18 182 op/s (102%) | 17 857 op/s |

![1.15.1 Dominance frontiers @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-15-1-dominance-frontiers-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 1.991 ms (1.0×) | 1.957 ms (1.0×) | 1.980 ms |
| Irreducible | 2.347 ms (1.0×) | 2.310 ms (1.0×) | 2.293 ms |
| Chain | 0.368 ms (1.0×) | 0.358 ms (1.0×) | 0.368 ms |
| Small methods | 1.455 ms (1.0×) | 1.425 ms (1.0×) | 1.424 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 502.3 op/s (99.4%) | 511.0 op/s (101%) | 505.1 op/s |
| Irreducible | 426.1 op/s (97.7%) | 432.9 op/s (99.3%) | 436.1 op/s |
| Chain | 2 717 op/s (100%) | 2 793 op/s (103%) | 2 717 op/s |
| Small methods | 687.3 op/s (97.9%) | 701.8 op/s (99.9%) | 702.2 op/s |

![1.15.2 Dominance frontiers @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-15-2-dominance-frontiers-n-100-000.svg)

### 1.16 Iterated dominance frontier (build + one query per variable)

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 30.88 ms (1.0×) | 30.80 ms (1.0×) | 30.64 ms |
| Irreducible | 42.61 ms (1.0×) | 41.67 ms (1.0×) | 41.00 ms |
| Ladder | 36.01 ms (1.0×) | 35.74 ms (1.0×) | 36.06 ms |
| Chain | 21.29 ms (1.0×) | 20.90 ms (1.0×) | 21.12 ms |
| Fan behind chain | 33.67 ms (1.0×) | 33.78 ms (1.0×) | 33.82 ms |
| Small methods | 0.432 ms (1.0×) | 0.435 ms (1.0×) | 0.427 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 32.38 op/s (99.2%) | 32.47 op/s (99.5%) | 32.64 op/s |
| Irreducible | 23.47 op/s (96.2%) | 24.00 op/s (98.4%) | 24.39 op/s |
| Ladder | 27.77 op/s (100%) | 27.98 op/s (101%) | 27.73 op/s |
| Chain | 46.96 op/s (99.2%) | 47.85 op/s (101%) | 47.34 op/s |
| Fan behind chain | 29.70 op/s (100%) | 29.61 op/s (100%) | 29.56 op/s |
| Small methods | 2 315 op/s (98.8%) | 2 299 op/s (98.2%) | 2 342 op/s |

![1.16.1 Iterated dominance frontier (build + one query per variable) @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-16-1-iterated-dominance-frontier-build-one-query-per-variable-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 438.1 ms (1.0×) | 433.1 ms (1.0×) | 432.0 ms |
| Irreducible | 572.2 ms (1.0×) | 557.4 ms (1.0×) | 559.8 ms |
| Ladder | 375.5 ms (1.0×) | 364.3 ms (1.0×) | 369.3 ms |
| Chain | 231.5 ms (1.0×) | 228.3 ms (1.0×) | 234.4 ms |
| Fan behind chain | 405.6 ms (1.0×) | 404.0 ms (1.0×) | 402.3 ms |
| Small methods | 4.535 ms (1.0×) | 4.420 ms (1.0×) | 4.475 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 2.283 op/s (98.6%) | 2.309 op/s (99.7%) | 2.315 op/s |
| Irreducible | 1.748 op/s (97.8%) | 1.794 op/s (100%) | 1.786 op/s |
| Ladder | 2.663 op/s (98.3%) | 2.745 op/s (101%) | 2.708 op/s |
| Chain | 4.320 op/s (101%) | 4.380 op/s (103%) | 4.266 op/s |
| Fan behind chain | 2.465 op/s (99.2%) | 2.475 op/s (99.6%) | 2.486 op/s |
| Small methods | 220.5 op/s (98.7%) | 226.2 op/s (101%) | 223.5 op/s |

![1.16.2 Iterated dominance frontier (build + one query per variable) @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-16-2-iterated-dominance-frontier-build-one-query-per-variable-n-100-000.svg)

### 1.17 Loops

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 0.400 ms (1.0×) | 0.395 ms (1.0×) | 0.400 ms |
| Irreducible | 0.420 ms (1.0×) | 0.422 ms (1.0×) | 0.423 ms |
| Ladder | 0.102 ms (1.0×) | 0.101 ms (0.9×) | 0.107 ms |
| Chain | 0.103 ms (1.0×) | 0.102 ms (1.0×) | 0.105 ms |
| Fan behind chain | 0.103 ms (1.0×) | 0.102 ms (1.0×) | 0.105 ms |
| Small methods | 0.465 ms (1.0×) | 0.464 ms (1.0×) | 0.461 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 2 500 op/s (100%) | 2 532 op/s (101%) | 2 500 op/s |
| Irreducible | 2 381 op/s (101%) | 2 370 op/s (100%) | 2 364 op/s |
| Ladder | 9 804 op/s (105%) | 9 901 op/s (106%) | 9 346 op/s |
| Chain | 9 709 op/s (102%) | 9 804 op/s (103%) | 9 524 op/s |
| Fan behind chain | 9 709 op/s (102%) | 9 804 op/s (103%) | 9 524 op/s |
| Small methods | 2 151 op/s (99.1%) | 2 155 op/s (99.4%) | 2 169 op/s |

![1.17.1 Loops @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-17-1-loops-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 5.543 ms (0.9×) | 5.751 ms (1.0×) | 5.993 ms |
| Irreducible | 7.337 ms (1.0×) | 7.272 ms (1.0×) | 7.556 ms |
| Ladder | 1.035 ms (1.0×) | 1.020 ms (1.0×) | 1.063 ms |
| Chain | 0.992 ms (1.0×) | 0.994 ms (1.0×) | 1.025 ms |
| Fan behind chain | 1.005 ms (1.0×) | 0.997 ms (1.0×) | 1.029 ms |
| Small methods | 4.997 ms (1.0×) | 4.844 ms (1.0×) | 4.950 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 180.4 op/s (108%) | 173.9 op/s (104%) | 166.9 op/s |
| Irreducible | 136.3 op/s (103%) | 137.5 op/s (104%) | 132.3 op/s |
| Ladder | 966.2 op/s (103%) | 980.4 op/s (104%) | 940.7 op/s |
| Chain | 1 008 op/s (103%) | 1 006 op/s (103%) | 975.6 op/s |
| Fan behind chain | 995.0 op/s (102%) | 1 003 op/s (103%) | 971.8 op/s |
| Small methods | 200.1 op/s (99.1%) | 206.4 op/s (102%) | 202.0 op/s |

![1.17.2 Loops @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-17-2-loops-n-100-000.svg)

### 1.18 Ancestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 0.065 ms (1.1×) | 0.065 ms (1.1×) | 0.058 ms |
| Irreducible | 0.067 ms (1.0×) | 0.066 ms (1.0×) | 0.068 ms |
| Ladder | 0.052 ms (1.0×) | 0.045 ms (0.8×) | 0.053 ms |
| Chain | 0.056 ms (1.1×) | 0.056 ms (1.1×) | 0.049 ms |
| Fan behind chain | 0.052 ms (1.0×) | 0.051 ms (1.0×) | 0.052 ms |
| Small methods | 0.058 ms (1.0×) | 0.057 ms (1.0×) | 0.059 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 15 385 op/s (89.2%) | 15 385 op/s (89.2%) | 17 241 op/s |
| Irreducible | 14 925 op/s (101%) | 15 152 op/s (103%) | 14 706 op/s |
| Ladder | 19 231 op/s (102%) | 22 222 op/s (118%) | 18 868 op/s |
| Chain | 17 857 op/s (87.5%) | 17 857 op/s (87.5%) | 20 408 op/s |
| Fan behind chain | 19 231 op/s (100%) | 19 608 op/s (102%) | 19 231 op/s |
| Small methods | 17 241 op/s (102%) | 17 544 op/s (104%) | 16 949 op/s |

![1.18.1 Ancestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-18-1-ancestry-of-the-dominator-tree-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 1.897 ms (1.0×) | 1.792 ms (1.0×) | 1.816 ms |
| Irreducible | 1.727 ms (1.0×) | 1.718 ms (0.9×) | 1.814 ms |
| Ladder | 0.504 ms (1.0×) | 0.488 ms (1.0×) | 0.506 ms |
| Chain | 0.559 ms (1.0×) | 0.553 ms (1.0×) | 0.566 ms |
| Fan behind chain | 0.543 ms (1.1×) | 0.490 ms (1.0×) | 0.513 ms |
| Small methods | 1.591 ms (1.0×) | 1.564 ms (1.0×) | 1.577 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 527.1 op/s (95.7%) | 558.0 op/s (101%) | 550.7 op/s |
| Irreducible | 579.0 op/s (105%) | 582.1 op/s (106%) | 551.3 op/s |
| Ladder | 1 984 op/s (100%) | 2 049 op/s (104%) | 1 976 op/s |
| Chain | 1 789 op/s (101%) | 1 808 op/s (102%) | 1 767 op/s |
| Fan behind chain | 1 842 op/s (94.5%) | 2 041 op/s (105%) | 1 949 op/s |
| Small methods | 628.5 op/s (99.1%) | 639.4 op/s (101%) | 634.1 op/s |

![1.18.2 Ancestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-18-2-ancestry-of-the-dominator-tree-n-100-000.svg)

### 1.19 PreorderAncestry of the dominator tree

#### Times @ 10k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 0.052 ms (1.0×) | 0.051 ms (1.0×) | 0.053 ms |
| Irreducible | 0.052 ms (1.0×) | 0.051 ms (1.0×) | 0.053 ms |
| Ladder | 0.045 ms (1.0×) | 0.044 ms (1.0×) | 0.045 ms |
| Chain | 0.046 ms (1.0×) | 0.046 ms (1.0×) | 0.047 ms |
| Fan behind chain | 0.044 ms (1.0×) | 0.045 ms (1.0×) | 0.046 ms |
| Small methods | 0.050 ms (1.0×) | 0.048 ms (1.0×) | 0.049 ms |

#### Throughput @ 10k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 19 231 op/s (102%) | 19 608 op/s (104%) | 18 868 op/s |
| Irreducible | 19 231 op/s (102%) | 19 608 op/s (104%) | 18 868 op/s |
| Ladder | 22 222 op/s (100%) | 22 727 op/s (102%) | 22 222 op/s |
| Chain | 21 739 op/s (102%) | 21 739 op/s (102%) | 21 277 op/s |
| Fan behind chain | 22 727 op/s (105%) | 22 222 op/s (102%) | 21 739 op/s |
| Small methods | 20 000 op/s (98.0%) | 20 833 op/s (102%) | 20 408 op/s |

![1.19.1 PreorderAncestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-19-1-preorderancestry-of-the-dominator-tree-n-10-000.svg)

#### Times @ 100k nodes (ms/op, lower is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 1.072 ms (1.0×) | 1.094 ms (1.0×) | 1.079 ms |
| Irreducible | 1.051 ms (0.9×) | 1.055 ms (0.9×) | 1.168 ms |
| Ladder | 0.378 ms (1.0×) | 0.346 ms (1.0×) | 0.364 ms |
| Chain | 0.426 ms (1.0×) | 0.411 ms (1.0×) | 0.424 ms |
| Fan behind chain | 0.376 ms (1.0×) | 0.401 ms (1.1×) | 0.363 ms |
| Small methods | 0.868 ms (1.0×) | 0.846 ms (1.0×) | 0.858 ms |

#### Throughput @ 100k nodes (op/s, higher is better)

| Shape | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|
| Structured | 932.8 op/s (101%) | 914.1 op/s (98.6%) | 926.8 op/s |
| Irreducible | 951.5 op/s (111%) | 947.9 op/s (111%) | 856.2 op/s |
| Ladder | 2 646 op/s (96.3%) | 2 890 op/s (105%) | 2 747 op/s |
| Chain | 2 347 op/s (99.5%) | 2 433 op/s (103%) | 2 358 op/s |
| Fan behind chain | 2 660 op/s (96.5%) | 2 494 op/s (90.5%) | 2 755 op/s |
| Small methods | 1 152 op/s (98.8%) | 1 182 op/s (101%) | 1 166 op/s |

![1.19.2 PreorderAncestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/1-19-2-preorderancestry-of-the-dominator-tree-n-100-000.svg)

## 2 Memory Footprint

Retained heap in bytes for a fully built graph. Lower is better. Baseline (= 100%): **Graphity 2.4.0**, or the newest version present in a table it is missing from.

### 2.1 Total Graph Size

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB (100%) | 80.1 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB (100%) | 160 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB (100%) | 1.2 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 1.2 MB | 1.2 MB | 1.2 MB | 1.2 MB | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB (100%) | 1.3 MB |
| Path<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB (100%) | 800 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB (100%) | 1.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB (100%) | 12.4 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 12.4 MB | 12.4 MB | 12.4 MB | 12.4 MB | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB (100%) | 12.8 MB |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 16.0 MB (100%) | 16.0 MB (100%) | 16.0 MB (100%) | 16.0 MB (100%) | 16.0 MB |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 160 MB (100%) | 160 MB (100%) | 160 MB (100%) | 160 MB (100%) | 160 MB |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 1600 MB (100%) | 1600 MB (100%) | 1600 MB (100%) | 1600 MB (100%) | 1600 MB |

![2.1 · Total Graph Size](fig/synthetic-ubuntu-ryzen-9950x-history/2-1-total-graph-size.svg)

### 2.2 Bytes per Element  `totalBytes / (Nodes + Edges)`

**Memory footprint in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>10k nodes, 10k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 4 B | 4 B | 4 B | 4 B | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Tree<br><sub>100k nodes, 100k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 4 B | 4 B | 4 B | 4 B | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>1M nodes, 3M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>10M nodes, 30M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |
| GNP sparse<br><sub>100M nodes, 300M edges</sub> | — | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B (100%) | 4 B |

![2.2 · Bytes per Element  `totalBytes / (Nodes + Edges)`](fig/synthetic-ubuntu-ryzen-9950x-history/2-2-bytes-per-element-totalbytes-nodes-edges.svg)

## 3 Allocations

Bytes allocated per algorithm invocation (JMH `-prof gc`). Lower is better. Baseline (= 100%): **Graphity 2.4.0**, or the newest version present in a table it is missing from.

### 3.1 Build

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB | — |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB | — |
| Tree<br><sub>10k nodes, 10k edges</sub> | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB (100.0%) | 458 kB | — |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB (100.0%) | 960 kB | — |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB (100.0%) | 9.9 MB | — |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 9.9 MB | 9.9 MB | 9.9 MB | 9.9 MB | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB | — |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB (100.0%) | 10.0 MB | — |

![3.1.1 · Build @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-1-build-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB | — |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB | — |
| Tree<br><sub>100k nodes, 100k edges</sub> | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB (100.0%) | 3.8 MB | — |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB (100.0%) | 10.6 MB | — |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB (100.0%) | 111 MB | — |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 111 MB | 111 MB | 111 MB | 111 MB | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB | — |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB (100.0%) | 112 MB | — |

![3.1.2 · Build @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-1-2-build-n-100-000.svg)

### 3.2 SCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 283 kB (58.5%) | 544 kB (113%) | 534 kB (111%) | 534 kB (111%) | 534 kB (111%) | 483 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 283 kB (58.5%) | 544 kB (113%) | 534 kB (111%) | 534 kB (111%) | 534 kB (111%) | 483 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 283 kB (98.1%) | 288 kB (100.0%) | 288 kB (100.0%) | 288 kB (100.0%) | 288 kB (100.0%) | 288 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 250 kB (67.5%) | 356 kB (96.0%) | 371 kB (100.0%) | 371 kB (100.0%) | 371 kB (100%) | 371 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 246 kB (55.1%) | 507 kB (114%) | 498 kB (111%) | 498 kB (111%) | 498 kB (111%) | 446 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 246 kB | 507 kB | 497 kB | 497 kB | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (54.9%) | 505 kB (114%) | 495 kB (111%) | 495 kB (111%) | 496 kB (111%) | 444 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 244 kB (67.0%) | 350 kB (95.9%) | 364 kB (100.0%) | 364 kB (100.0%) | 364 kB (100.0%) | 364 kB |

![3.2.1 · SCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-1-scc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | 6.0 MB (129%) | 4.9 MB (105%) | 4.9 MB (105%) | 4.9 MB (105%) | 4.6 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (60.9%) | 4.9 MB (106%) | 4.9 MB (105%) | 4.9 MB (105%) | 4.9 MB (105%) | 4.6 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.8 MB (101%) | 2.8 MB (100.0%) | 2.8 MB (100.0%) | 2.8 MB (100.0%) | 2.8 MB (100%) | 2.8 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.5 MB (71.1%) | 3.9 MB (110%) | 3.5 MB (100.0%) | 3.5 MB (100.0%) | 3.5 MB (100.0%) | 3.5 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.5 MB (57.5%) | 5.6 MB (131%) | 4.5 MB (106%) | 4.5 MB (106%) | 4.5 MB (106%) | 4.3 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.5 MB | 5.6 MB | 4.5 MB | 4.5 MB | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (57.3%) | 5.6 MB (131%) | 4.5 MB (106%) | 4.5 MB (106%) | 4.5 MB (106%) | 4.3 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.4 MB (70.6%) | 3.3 MB (96.7%) | 3.5 MB (100.0%) | 3.5 MB (100.0%) | 3.5 MB (100.0%) | 3.5 MB |

![3.2.2 · SCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-2-2-scc-n-100-000.svg)

### 3.3 WCC

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 200 kB | 200 kB | 200 kB | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |

![3.3.1 · WCC @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-1-wcc-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 2.0 MB | 2.0 MB | 2.0 MB | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |

![3.3.2 · WCC @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-3-2-wcc-n-100-000.svg)

### 3.4 CCs

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 200 kB | 200 kB | 200 kB | 200 kB | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB (100.0%) | 200 kB |

![3.4.1 · CCs @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-1-ccs-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 2.0 MB | 2.0 MB | 2.0 MB | 2.0 MB | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |

![3.4.2 · CCs @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-4-2-ccs-n-100-000.svg)

### 3.5 Condense

**Allocations @ 1k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>1k nodes, 1k edges</sub> | — | — | 23.2 kB (100%) | 23.2 kB (100.0%) | 23.2 kB (100%) | 23.2 kB |
| GNP dense<br><sub>1k nodes, 30k edges</sub> | — | — | 96 B (99.0%) | 96 B (99.0%) | 97 B (100%) | 97 B |
| Chain of cliques<br><sub>1k nodes, 31.1k edges</sub> | — | — | 760 B (99.9%) | 760 B (99.9%) | 761 B (100%) | 761 B |
| DAG GNP sparse<br><sub>1k nodes</sub> | — | — | 66.4 kB (100.0%) | 66.4 kB (100.0%) | 66.4 kB (100%) | 66.4 kB |

![3.5.1 · Condense @ n = 1 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-1-condense-n-1-000.svg)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | — | — | 250 kB (100.0%) | 250 kB (100.0%) | 250 kB (100%) | 250 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | — | — | 97 B (94.2%) | 97 B (94.2%) | 103 B (100%) | 103 B |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | — | — | 7.2 kB (99.8%) | 7.2 kB (99.8%) | 7.2 kB (100%) | 7.2 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | — | — | 541 kB (100.0%) | 541 kB (100.0%) | 541 kB (100.0%) | 541 kB |

![3.5.2 · Condense @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-2-condense-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | — | — | 2.1 MB (100.0%) | 2.1 MB (100.0%) | 2.1 MB (100%) | 2.1 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | — | — | 107 B (49.3%) | 107 B (49.3%) | 205 B (94.5%) | 217 B |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | — | — | 75.7 kB (99.8%) | 75.7 kB (99.8%) | 75.8 kB (100.0%) | 75.9 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | — | — | 5.9 MB (100.0%) | 5.9 MB (100.0%) | 5.9 MB (100.0%) | 5.9 MB |

![3.5.3 · Condense @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-5-3-condense-n-100-000.svg)

### 3.6 TopOrder

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (17.4%) | 306 kB (125%) | 297 kB (121%) | 297 kB (121%) | 297 kB (121%) | 246 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (17.4%) | 306 kB (125%) | 297 kB (121%) | 297 kB (121%) | 297 kB (121%) | 246 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 42.7 kB (84.1%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| Tree shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.1%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP sparse<br><sub>10k nodes</sub> | 42.7 kB (84.1%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB (100%) | 50.8 kB |
| DAG GNP dense<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 42.7 kB (84.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.8 kB (100.0%) | 50.9 kB (100%) | 50.9 kB |

![3.6.1 · TopOrder @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-1-toporder-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 425 kB (18.8%) | 3.6 MB (159%) | 2.5 MB (111%) | 2.5 MB (111%) | 2.5 MB (111%) | 2.3 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 425 kB (18.8%) | 2.5 MB (112%) | 2.5 MB (111%) | 2.5 MB (111%) | 2.5 MB (111%) | 2.3 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB |
| Tree shfld.<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100%) | 433 kB |
| DAG GNP sparse<br><sub>100k nodes</sub> | 425 kB (98.1%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB (100.0%) | 433 kB |
| DAG GNP dense<br><sub>100k nodes</sub> | 425 kB (98.0%) | 433 kB (99.9%) | 433 kB (99.9%) | 433 kB (99.9%) | 434 kB (100.0%) | 434 kB |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 425 kB (98.0%) | 433 kB (99.9%) | 433 kB (99.9%) | 433 kB (99.9%) | 434 kB (100.0%) | 434 kB |

![3.6.2 · TopOrder @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-6-2-toporder-n-100-000.svg)

### 3.7 TopOrderAndLevels

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB (100%) |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB (100%) |
| Tree<br><sub>10k nodes, 10k edges</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB (100%) |
| Tree shfld.<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB (100%) |
| DAG GNP sparse<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) |
| DAG GNP dense<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) |
| DAG GNP dense shfld.<br><sub>10k nodes</sub> | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100%) | 160 kB | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) | 160 kB (100.0%) |

![3.7.1 · TopOrderAndLevels @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-1-toporderandlevels-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 | Graphity 2.1.0<br>(bi-adjacent) | Graphity 2.2.0<br>(bi-adjacent) | Graphity 2.3.0<br>(bi-adjacent) | Graphity 2.4.0<br>(bi-adjacent) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| Tree<br><sub>100k nodes, 100k edges</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| Tree shfld.<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP sparse<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP dense<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |
| DAG GNP dense shfld.<br><sub>100k nodes</sub> | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) | 1.6 MB (100.0%) |

![3.7.2 · TopOrderAndLevels @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-7-2-toporderandlevels-n-100-000.svg)

### 3.8 BackEdges

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.3%) | 266 kB (130%) | 257 kB (125%) | 257 kB (125%) | 257 kB (125%) | 206 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (1.3%) | 266 kB (130%) | 257 kB (125%) | 257 kB (125%) | 257 kB (125%) | 206 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 2.7 kB (25.3%) | 10.9 kB (100%) | 10.9 kB (100%) | 10.9 kB (100%) | 10.9 kB (100%) | 10.9 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 228 kB (65.0%) | 336 kB (95.8%) | 351 kB (100.0%) | 351 kB (100.0%) | 351 kB (100%) | 351 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB (95.0%) | 4.1 MB (101%) | 4.1 MB (101%) | 4.1 MB (101%) | 4.1 MB (101%) | 4.0 MB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 3.8 MB | 4.1 MB | 4.1 MB | 4.1 MB | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (95.0%) | 4.1 MB (101%) | 4.1 MB (101%) | 4.1 MB (101%) | 4.1 MB (101%) | 4.0 MB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 3.8 MB (96.9%) | 4.0 MB (99.6%) | 4.0 MB (100.0%) | 4.0 MB (100.0%) | 4.0 MB (100.0%) | 4.0 MB |

![3.8.1 · BackEdges @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-1-backedges-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.4%) | 3.2 MB (171%) | 2.1 MB (113%) | 2.1 MB (113%) | 2.1 MB (113%) | 1.9 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 25.2 kB (1.4%) | 2.1 MB (114%) | 2.1 MB (113%) | 2.1 MB (113%) | 2.1 MB (113%) | 1.9 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 25.3 kB (75.7%) | 33.4 kB (99.8%) | 33.4 kB (99.8%) | 33.4 kB (99.8%) | 33.5 kB (100%) | 33.5 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 2.6 MB (71.3%) | 4.0 MB (110%) | 3.6 MB (100.0%) | 3.6 MB (100.0%) | 3.6 MB (100.0%) | 3.6 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 43.8 MB (96.0%) | 47.0 MB (103%) | 45.9 MB (101%) | 45.9 MB (101%) | 45.9 MB (101%) | 45.6 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 43.8 MB | 47.0 MB | 45.9 MB | 45.9 MB | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (96.0%) | 47.0 MB (103%) | 45.9 MB (101%) | 45.9 MB (101%) | 45.9 MB (101%) | 45.6 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 43.8 MB (97.7%) | 44.7 MB (99.7%) | 44.8 MB (100.0%) | 44.8 MB (100.0%) | 44.8 MB (100.0%) | 44.8 MB |

![3.8.2 · BackEdges @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-8-2-backedges-n-100-000.svg)

### 3.9 EdgeTypes

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (20.6%) | 316 kB (124%) | 307 kB (120%) | 307 kB (120%) | 307 kB (120%) | 256 kB |
| Path shfld.<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (20.6%) | 316 kB (124%) | 307 kB (120%) | 307 kB (120%) | 307 kB (120%) | 256 kB |
| Tree<br><sub>10k nodes, 10k edges</sub> | 52.7 kB (86.6%) | 60.8 kB (100.0%) | 60.8 kB (100.0%) | 60.8 kB (100.0%) | 60.8 kB (100%) | 60.8 kB |
| GNP sparse<br><sub>10k nodes, 30k edges</sub> | 72.7 kB (37.2%) | 181 kB (92.4%) | 196 kB (100.0%) | 196 kB (100.0%) | 196 kB (100.0%) | 196 kB |
| GNP dense<br><sub>10k nodes, 299.6k edges</sub> | 342 kB (62.8%) | 606 kB (111%) | 596 kB (109%) | 596 kB (109%) | 596 kB (109%) | 545 kB |
| GNP dense shfld.<br><sub>10k nodes, 299.6k edges</sub> | 342 kB | 606 kB | 596 kB | 596 kB | — | — |
| Chain of cliques<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (63.5%) | 617 kB (111%) | 607 kB (109%) | 607 kB (109%) | 608 kB (109%) | 556 kB |
| Chain of cliques shfld.<br><sub>10k nodes, 310.8k edges</sub> | 354 kB (74.2%) | 462 kB (96.9%) | 476 kB (100.0%) | 476 kB (100.0%) | 476 kB (100%) | 476 kB |

![3.9.1 · EdgeTypes @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-1-edgetypes-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Path<br><sub>100k nodes, 100k edges</sub> | 525 kB (22.2%) | 3.7 MB (156%) | 2.6 MB (111%) | 2.6 MB (111%) | 2.6 MB (111%) | 2.4 MB |
| Path shfld.<br><sub>100k nodes, 100k edges</sub> | 525 kB (22.2%) | 2.6 MB (111%) | 2.6 MB (111%) | 2.6 MB (111%) | 2.6 MB (111%) | 2.4 MB |
| Tree<br><sub>100k nodes, 100k edges</sub> | 525 kB (98.5%) | 533 kB (100.0%) | 533 kB (100.0%) | 533 kB (100.0%) | 533 kB (100%) | 533 kB |
| GNP sparse<br><sub>100k nodes, 300k edges</sub> | 725 kB (41.1%) | 2.1 MB (120%) | 1.8 MB (100.0%) | 1.8 MB (100.0%) | 1.8 MB (100.0%) | 1.8 MB |
| GNP dense<br><sub>100k nodes, 3M edges</sub> | 3.4 MB (65.0%) | 6.6 MB (125%) | 5.5 MB (105%) | 5.5 MB (105%) | 5.5 MB (105%) | 5.3 MB |
| GNP dense shfld.<br><sub>100k nodes, 3M edges</sub> | 3.4 MB | 6.6 MB | 5.5 MB | 5.5 MB | — | — |
| Chain of cliques<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (65.7%) | 6.7 MB (125%) | 5.6 MB (105%) | 5.6 MB (105%) | 5.6 MB (105%) | 5.4 MB |
| Chain of cliques shfld.<br><sub>100k nodes, 3.1M edges</sub> | 3.5 MB (77.2%) | 4.5 MB (97.5%) | 4.6 MB (100.0%) | 4.6 MB (100.0%) | 4.6 MB (100.0%) | 4.6 MB |

![3.9.2 · EdgeTypes @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-9-2-edgetypes-n-100-000.svg)

### 3.10 Dominators

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 371 kB (100.0%) | 371 kB (100%) | 371 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 371 kB (100.0%) | 371 kB (100%) | 371 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 486 kB (100.0%) | 486 kB (100%) | 486 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 566 kB (100.0%) | 566 kB (100%) | 566 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 486 kB (100.0%) | 486 kB (100%) | 486 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 475 kB (100.0%) | 475 kB (100.0%) | 475 kB |

![3.10.1 · Dominators @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-10-1-dominators-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 3.6 MB (100.0%) | 3.7 MB (100.0%) | 3.7 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 3.7 MB (100.0%) | 3.7 MB (100.0%) | 3.7 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 4.7 MB (100.0%) | 4.7 MB (100.0%) | 4.7 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 5.5 MB (100.0%) | 5.5 MB (100.0%) | 5.5 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 4.7 MB (100.0%) | 4.7 MB (100%) | 4.7 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 4.7 MB (100.0%) | 4.7 MB (100.0%) | 4.7 MB |

![3.10.2 · Dominators @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-10-2-dominators-n-100-000.svg)

### 3.11 Dominance frontiers

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 134 kB (100.0%) | 134 kB (100%) | 134 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 144 kB (100.0%) | 144 kB (100%) | 144 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 50.1 MB (100.0%) | 50.1 MB (100%) | 50.1 MB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 80.1 kB (100.0%) | 80.1 kB (100%) | 80.1 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 100 MB (100.0%) | 100 MB (100%) | 100 MB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 140 kB (100.0%) | 140 kB (100%) | 140 kB |

![3.11.1 · Dominance frontiers @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-11-1-dominance-frontiers-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 1.3 MB (100.0%) | 1.3 MB (100%) | 1.3 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 1.4 MB (100.0%) | 1.4 MB (100%) | 1.4 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 800 kB (100.0%) | 800 kB (100.0%) | 800 kB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 1.4 MB (100.0%) | 1.4 MB (100%) | 1.4 MB |

![3.11.2 · Dominance frontiers @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-11-2-dominance-frontiers-n-100-000.svg)

### 3.12 Iterated dominance frontier (build + one query per variable)

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 25.4 MB (100.0%) | 25.4 MB (100%) | 25.4 MB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 28.9 MB (100.0%) | 28.9 MB (100%) | 28.9 MB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 53.8 MB (100.0%) | 53.8 MB (100%) | 53.8 MB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 21.4 MB (100.0%) | 21.4 MB (100%) | 21.4 MB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 65.4 MB (100.0%) | 65.4 MB (100%) | 65.4 MB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 604 kB (100.0%) | 604 kB (100.0%) | 604 kB |

![3.12.1 · Iterated dominance frontier (build + one query per variable) @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-12-1-iterated-dominance-frontier-build-one-query-per-variable-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 370 MB (100.0%) | 370 MB (100%) | 370 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 451 MB (100.0%) | 451 MB (100%) | 451 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 504 MB (100.0%) | 504 MB (100%) | 504 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 198 MB (100.0%) | 198 MB (100%) | 198 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 662 MB (100.0%) | 662 MB (100%) | 662 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 5.8 MB (100.0%) | 5.8 MB (100.0%) | 5.8 MB |

![3.12.2 · Iterated dominance frontier (build + one query per variable) @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-12-2-iterated-dominance-frontier-build-one-query-per-variable-n-100-000.svg)

### 3.13 Loops

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 772 kB (100.0%) | 772 kB (100.0%) | 772 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 812 kB (100.0%) | 812 kB (100%) | 812 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 675 kB (100.0%) | 675 kB (100%) | 675 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 735 kB (100.0%) | 735 kB (100%) | 735 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 675 kB (100.0%) | 675 kB (100.0%) | 675 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 897 kB (100.0%) | 897 kB (100%) | 897 kB |

![3.13.1 · Loops @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-13-1-loops-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 7.5 MB (100.0%) | 7.5 MB (100.0%) | 7.5 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 13.5 MB (100.0%) | 13.5 MB (100.0%) | 13.5 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 6.6 MB (100.0%) | 6.6 MB (100.0%) | 6.6 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 7.2 MB (100.0%) | 7.2 MB (100.0%) | 7.2 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 6.6 MB (100.0%) | 6.6 MB (100.0%) | 6.6 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 8.9 MB (100.0%) | 8.9 MB (100.0%) | 8.9 MB |

![3.13.2 · Loops @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-13-2-loops-n-100-000.svg)

### 3.14 Ancestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB (100%) | 280 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB (100%) | 280 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB (100%) | 280 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB (100%) | 280 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 280 kB (100.0%) | 280 kB (100%) | 280 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 297 kB (100.0%) | 297 kB (100%) | 297 kB |

![3.14.1 · Ancestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-14-1-ancestry-of-the-dominator-tree-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB (100%) | 2.8 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB (100.0%) | 2.8 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB (100.0%) | 2.8 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB (100%) | 2.8 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 2.8 MB (100.0%) | 2.8 MB (100%) | 2.8 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 2.9 MB (100.0%) | 2.9 MB (100%) | 2.9 MB |

![3.14.2 · Ancestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-14-2-ancestry-of-the-dominator-tree-n-100-000.svg)

### 3.15 PreorderAncestry of the dominator tree

**Allocations @ 10k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Irreducible<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Ladder<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Chain<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Fan behind chain<br><sub>10k nodes</sub> | — | — | — | 200 kB (100.0%) | 200 kB (100%) | 200 kB |
| Small methods<br><sub>10k nodes</sub> | — | — | — | 216 kB (100.0%) | 216 kB (100.0%) | 216 kB |

![3.15.1 · PreorderAncestry of the dominator tree @ n = 10 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-15-1-preorderancestry-of-the-dominator-tree-n-10-000.svg)

**Allocations @ 100k nodes in bytes (lower is better)**

| Shape | Graphity 1.0 | Graphity 2.0.0 | Graphity 2.1.0 | Graphity 2.2.0 | Graphity 2.3.0 | Graphity 2.4.0 |
|---|---:|---:|---:|---:|---:|---:|
| Structured<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Irreducible<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Ladder<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Chain<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB (100.0%) | 2.0 MB |
| Fan behind chain<br><sub>100k nodes</sub> | — | — | — | 2.0 MB (100.0%) | 2.0 MB (100%) | 2.0 MB |
| Small methods<br><sub>100k nodes</sub> | — | — | — | 2.1 MB (100.0%) | 2.1 MB (100%) | 2.1 MB |

![3.15.2 · PreorderAncestry of the dominator tree @ n = 100 000](fig/synthetic-ubuntu-ryzen-9950x-history/3-15-2-preorderancestry-of-the-dominator-tree-n-100-000.svg)
