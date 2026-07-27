package com.kobayami.graphity.benchmarks.compression;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodeMapping;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Locale;

/**
 * Offline analysis of adjacency-list compressibility (no JMH, no timing).
 * <p>
 * For each benchmark shape and each node ordering (as-built / DFS-preorder / BFS), this tool
 * decomposes every node's <em>sorted</em> out-adjacency into maximal runs of consecutive node
 * IDs and reports, per edge, how many bits several candidate encoders would need:
 * <ul>
 *   <li><b>CSR</b> — the current representation: a flat {@code int} per edge (32 bits).</li>
 *   <li><b>FixedBits</b> — fixed-width absolute bit-packing to {@code ceil(log2 n)} bits.
 *       Keeps O(1) random access; the universal, run-independent baseline.</li>
 *   <li><b>VarintGap</b> — byte-aligned LEB128 on ascending gaps (sequential decode).</li>
 *   <li><b>GammaGap</b> — Elias-γ on ascending gaps (bit-aligned, the theoretical yardstick).</li>
 *   <li><b>SeqA</b> — Variant A: run-length "sequence list", 5 bytes/run
 *       (1-byte length ≤255 + 4-byte absolute start).</li>
 *   <li><b>SeqB</b> — Variant B: run-length packed into one {@code int}/run
 *       (30-bit start + 2-bit length, length ≤4).</li>
 * </ul>
 * Bits/edge shown as an average over all edges; ratio is relative to CSR (lower = smaller).
 * Only the edge payload is counted — the per-node offset array (4 B/node) is identical for all
 * encoders and deliberately excluded (see discussion: nodes stay the integration point for
 * mappings/metadata and are not the compression target).
 *
 * <pre>{@code
 * java -cp graphity-benchmarks.jar \
 *   com.kobayami.graphity.benchmarks.compression.AdjacencyCompressionAnalysis [n]
 * }</pre>
 */
public final class AdjacencyCompressionAnalysis {

  private AdjacencyCompressionAnalysis() {}

  /** Shapes worth analyzing: the natural topologies (their *_SHUFFLED twins reorder identically). */
  private static final GraphShape[] SHAPES = {
      GraphShape.PATH,
      GraphShape.TREE,
      GraphShape.GNP_SPARSE,
      GraphShape.GNP_DENSE,
      GraphShape.CHAIN_OF_CLIQUES,
      GraphShape.DAG_GNP_SPARSE,
      GraphShape.DAG_GNP_DENSE,
  };

  private enum Ordering { AS_BUILT, DFS_PREORDER, BFS }

  public static void main(String[] args) {
    // A numeric arg => synthetic shapes at that n; otherwise treat each arg as a dataset path.
    if (args.length > 0 && !args[0].chars().allMatch(Character::isDigit)) {
      for (String path : args) {
        Graph base = BenchmarkGraphs.loadDataset(path);
        report(path.substring(path.lastIndexOf('/') + 1), base);
      }
      return;
    }
    int n = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
    System.out.printf(Locale.ROOT, "# Adjacency compression analysis (synthetic, n = %,d)%n%n", n);
    printLegend();
    for (GraphShape shape : SHAPES) {
      report(shape.toString(), BenchmarkGraphs.generate(shape, n));
    }
  }

  private static void printLegend() {
    System.out.println("Edge payload only (per-node offset array excluded). Bits/edge, ratio vs CSR.");
    System.out.println("FixedBits = ceil(log2 nodeCount) bits/edge (run-independent, keeps O(1)).");
    System.out.println();
  }

  private static void report(String title, Graph base) {
    int fixed = ceilLog2(base.nodeCount());
    System.out.printf(Locale.ROOT, "## %s  (n = %,d, m = %,d, avg deg = %.2f, FixedBits = %d)%n%n",
        title, base.nodeCount(), base.edgeCount(),
        base.edgeCount() / (double) base.nodeCount(), fixed);
    System.out.printf(Locale.ROOT, "| Ordering | runs/node | avg run | %%edges in runs>=2 | "
        + "CSR | FixedBits | VarintGap | GammaGap | SeqA (5B) | SeqB (int) |%n");
    System.out.println("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|");
    for (Ordering ordering : Ordering.values()) {
      analyzeAndPrint(ordering, reorder(base, ordering), fixed);
    }
    System.out.println();
  }

  private static Graph reorder(Graph base, Ordering ordering) {
    return switch (ordering) {
      case AS_BUILT -> base;
      case DFS_PREORDER -> Graphs.remapped(base, NodeMapping.reindex(base));
      case BFS -> Graphs.remapped(base, NodeMapping.forPermutation(bfsOrder(base)));
    };
  }

  /** Returns {@code order[newId] = oldId} in BFS visitation order over the whole forest. */
  private static IntArrayList bfsOrder(Graph g) {
    int n = g.nodeCount();
    int[] order = new int[n];
    boolean[] seen = new boolean[n];
    int[] queue = new int[n];
    int pos = 0;
    for (int root = 0; root < n; root++) {
      if (seen[root]) continue;
      int head = 0, tail = 0;
      seen[root] = true;
      queue[tail++] = root;
      while (head < tail) {
        int u = queue[head++];
        order[pos++] = u;
        SortedIntView outs = g.outNodes(u);
        for (int i = 0, d = outs.size(); i < d; i++) {
          int v = outs.getInt(i);
          if (!seen[v]) {
            seen[v] = true;
            queue[tail++] = v;
          }
        }
      }
    }
    return IntArrayList.wrap(order);
  }

  private static void analyzeAndPrint(Ordering ordering, Graph g, int fixedBits) {
    int n = g.nodeCount();
    long m = g.edgeCount();
    if (m == 0) {
      System.out.printf(Locale.ROOT, "| %s | – | – | – | – | – | – | – | – | – |%n", ordering);
      return;
    }

    long runCount = 0;              // maximal consecutive runs
    long edgesInLongRuns = 0;       // edges belonging to runs of length >= 2
    long varintBits = 0;
    long gammaBits = 0;
    long seqRecordsA = 0;           // 5-byte records (length cap 255)
    long seqRecordsB = 0;           // 4-byte records (length cap 4)

    for (int u = 0; u < n; u++) {
      SortedIntView outs = g.outNodes(u);
      int d = outs.size();
      if (d == 0) continue;

      long prev = -1;               // for gap coding; first gap = value + 1
      int runLen = 0;
      long runPrev = Long.MIN_VALUE;
      for (int i = 0; i < d; i++) {
        long v = outs.getInt(i);

        long gap = v - prev;        // >= 1 for a strictly ascending, deduplicated list
        prev = v;
        varintBits += leb128Bytes(gap) * 8L;
        gammaBits += gammaBits(gap);

        if (v == runPrev + 1) {
          runLen++;
        } else {
          if (runLen > 0) {
            runCount++;
            if (runLen >= 2) edgesInLongRuns += runLen;
            seqRecordsA += ceilDiv(runLen, 255);
            seqRecordsB += ceilDiv(runLen, 4);
          }
          runLen = 1;
        }
        runPrev = v;
      }
      // close the final run of this node
      runCount++;
      if (runLen >= 2) edgesInLongRuns += runLen;
      seqRecordsA += ceilDiv(runLen, 255);
      seqRecordsB += ceilDiv(runLen, 4);
    }

    double csr = 32.0;
    double fixed = fixedBits;
    double varint = varintBits / (double) m;
    double gamma = gammaBits / (double) m;
    double seqA = seqRecordsA * 40.0 / m;   // 5 bytes = 40 bits per record
    double seqB = seqRecordsB * 32.0 / m;   // 1 int  = 32 bits per record

    System.out.printf(Locale.ROOT,
        "| %s | %.2f | %.2f | %.1f%% | %s | %s | %s | %s | %s | %s |%n",
        ordering,
        runCount / (double) n,
        m / (double) runCount,
        100.0 * edgesInLongRuns / m,
        cell(csr, csr),
        cell(fixed, csr),
        cell(varint, csr),
        cell(gamma, csr),
        cell(seqA, csr),
        cell(seqB, csr));
  }

  private static String cell(double bitsPerEdge, double csr) {
    return String.format(Locale.ROOT, "%.1f (%.0f%%)", bitsPerEdge, 100.0 * bitsPerEdge / csr);
  }

  // ---- codec size helpers ----

  /** LEB128 byte count for a positive value. */
  private static int leb128Bytes(long value) {
    int bytes = 1;
    long v = value >>> 7;
    while (v != 0) {
      bytes++;
      v >>>= 7;
    }
    return bytes;
  }

  /** Elias-γ bit count for x >= 1: 2*floor(log2 x) + 1. */
  private static int gammaBits(long x) {
    int floorLog2 = 63 - Long.numberOfLeadingZeros(x);
    return 2 * floorLog2 + 1;
  }

  private static int ceilLog2(int n) {
    if (n <= 1) return 1;
    return 32 - Integer.numberOfLeadingZeros(n - 1);
  }

  private static long ceilDiv(long a, long b) {
    return (a + b - 1) / b;
  }
}
