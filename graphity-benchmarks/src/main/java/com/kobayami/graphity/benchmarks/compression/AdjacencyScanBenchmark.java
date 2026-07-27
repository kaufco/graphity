package com.kobayami.graphity.benchmarks.compression;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;

import java.util.Locale;

/**
 * Indicative micro-benchmark (manual timing, not JMH) probing the core hypothesis:
 * does a smaller adjacency representation buy throughput via better cache behaviour,
 * despite the extra decode work?
 * <p>
 * Compares three edge stores built from the same graph:
 * <ul>
 *   <li><b>CSR</b> — raw {@code int[]} (32 bits/edge), the current representation.</li>
 *   <li><b>Packed</b> — fixed-width bit-packing to {@code b = ceil(log2 nodeCount)} bits.
 *       Bit width, mask and shifts are computed at runtime (never hard-wired).</li>
 *   <li><b>SeqB</b> — run-length "sequence list", one {@code int} per run
 *       (30-bit start + 2-bit length, runs &gt; 4 split).</li>
 * </ul>
 * Two workloads over all edges:
 * <ol>
 *   <li><b>sum</b> — accumulate all target IDs. Isolates sequential read/decode throughput.</li>
 *   <li><b>gather</b> — for every edge target {@code v}, flip 2 bits in a packed {@code state[]}
 *       array (16 nodes/int). Mirrors the memory pattern of the 6 DFS cores (random access
 *       to per-node state on every edge).</li>
 * </ol>
 * Reports ns/edge (min over measured iterations) and the edge-store size in bytes.
 * Manual timing is only indicative; a JMH port is the follow-up if the signal is promising.
 *
 * <pre>{@code
 * java -cp graphity-benchmarks.jar \
 *   com.kobayami.graphity.benchmarks.compression.AdjacencyScanBenchmark [n]
 * }</pre>
 */
public final class AdjacencyScanBenchmark {

  private AdjacencyScanBenchmark() {}

  private static final GraphShape[] SHAPES = {
      GraphShape.GNP_SPARSE,
      GraphShape.GNP_DENSE,
      GraphShape.CHAIN_OF_CLIQUES,
  };

  private static final int WARMUP = 6;
  private static final int MEASURE = 12;

  public static void main(String[] args) {
    int n = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
    System.out.printf(Locale.ROOT, "# Adjacency scan micro-benchmark (n = %,d)%n", n);
    System.out.println("Manual timing, indicative only. ns/edge = min over " + MEASURE + " iters.");
    System.out.println();

    for (GraphShape shape : SHAPES) {
      Graph g = BenchmarkGraphs.generate(shape, n);
      int nodes = g.nodeCount();
      long m = g.edgeCount();

      int[] csrStart = new int[nodes + 1];
      int[] csrAdj = new int[(int) m];
      flatten(g, csrStart, csrAdj);

      Packed packed = Packed.build(nodes, csrStart, csrAdj);
      SeqB seqB = SeqB.build(nodes, csrStart, csrAdj);
      int[] state = new int[(nodes >> 4) + 1];

      System.out.printf(Locale.ROOT, "## %s  (n = %,d, m = %,d, avg deg = %.1f, b = %d)%n%n",
          shape, nodes, m, m / (double) nodes, packed.bits);
      System.out.printf(Locale.ROOT, "| Store | size (MB) | %% of CSR | sum ns/edge | gather ns/edge |%n");
      System.out.println("|---|---:|---:|---:|---:|");

      long csrBytes = 4L * m;
      long packedBytes = (long) packed.words.length * 8;
      long seqBytes = (long) seqB.records.length * 4;

      row("CSR", csrBytes, csrBytes, m,
          () -> sumCsr(csrStart, csrAdj),
          () -> gatherCsr(csrStart, csrAdj, state));
      row("Packed", packedBytes, csrBytes, m,
          () -> sumPacked(packed),
          () -> gatherPacked(packed, state));
      row("SeqB", seqBytes, csrBytes, m,
          () -> sumSeqB(seqB),
          () -> gatherSeqB(seqB, state));
      System.out.println();
    }
  }

  private static void row(String name, long bytes, long csrBytes, long m,
                          LongOp sum, LongOp gather) {
    double sumNs = bestNsPerEdge(sum, m);
    double gatherNs = bestNsPerEdge(gather, m);
    System.out.printf(Locale.ROOT, "| %s | %.1f | %.0f%% | %.3f | %.3f |%n",
        name, bytes / 1e6, 100.0 * bytes / csrBytes, sumNs, gatherNs);
  }

  @FunctionalInterface
  private interface LongOp {
    long run();
  }

  private static long SINK;

  private static double bestNsPerEdge(LongOp op, long edges) {
    for (int i = 0; i < WARMUP; i++) SINK ^= op.run();
    long best = Long.MAX_VALUE;
    for (int i = 0; i < MEASURE; i++) {
      long t0 = System.nanoTime();
      SINK ^= op.run();
      long dt = System.nanoTime() - t0;
      if (dt < best) best = dt;
    }
    return best / (double) edges;
  }

  private static void flatten(Graph g, int[] start, int[] adj) {
    int nodes = g.nodeCount();
    int pos = 0;
    for (int u = 0; u < nodes; u++) {
      start[u] = pos;
      SortedIntView outs = g.outNodes(u);
      for (int i = 0, d = outs.size(); i < d; i++) {
        adj[pos++] = outs.getInt(i);
      }
    }
    start[nodes] = pos;
  }

  // ---- CSR workloads ----

  private static long sumCsr(int[] start, int[] adj) {
    long s = 0;
    int end = start[start.length - 1];
    for (int i = 0; i < end; i++) s += adj[i];
    return s;
  }

  private static long gatherCsr(int[] start, int[] adj, int[] state) {
    java.util.Arrays.fill(state, 0);
    int end = start[start.length - 1];
    for (int i = 0; i < end; i++) {
      int v = adj[i];
      state[v >> 4] ^= 1 << ((v & 0xF) << 1);
    }
    long s = 0;
    for (int w : state) s += w;
    return s;
  }

  // ---- Packed workloads ----

  private static long sumPacked(Packed p) {
    long s = 0;
    long m = p.edgeCount;
    for (long k = 0; k < m; k++) s += p.read(k);
    return s;
  }

  private static long gatherPacked(Packed p, int[] state) {
    java.util.Arrays.fill(state, 0);
    long m = p.edgeCount;
    for (long k = 0; k < m; k++) {
      int v = (int) p.read(k);
      state[v >> 4] ^= 1 << ((v & 0xF) << 1);
    }
    long s = 0;
    for (int w : state) s += w;
    return s;
  }

  // ---- SeqB workloads ----

  private static long sumSeqB(SeqB q) {
    long s = 0;
    int[] rec = q.records;
    for (int r = 0; r < rec.length; r++) {
      int packed = rec[r];
      int start = packed >>> 2;
      int count = (packed & 3) + 1;
      for (int j = 0; j < count; j++) s += start + j;
    }
    return s;
  }

  private static long gatherSeqB(SeqB q, int[] state) {
    java.util.Arrays.fill(state, 0);
    int[] rec = q.records;
    for (int r = 0; r < rec.length; r++) {
      int packed = rec[r];
      int start = packed >>> 2;
      int count = (packed & 3) + 1;
      for (int j = 0; j < count; j++) {
        int v = start + j;
        state[v >> 4] ^= 1 << ((v & 0xF) << 1);
      }
    }
    long s = 0;
    for (int w : state) s += w;
    return s;
  }

  // ---- Fixed-width bit-packed adjacency (dynamic width) ----

  static final class Packed {
    final long[] words;
    final int bits;      // bits per node ID, = ceil(log2 nodeCount)
    final long mask;     // (1L << bits) - 1
    final long edgeCount;

    private Packed(long[] words, int bits, long edgeCount) {
      this.words = words;
      this.bits = bits;
      this.mask = (bits == 64) ? -1L : ((1L << bits) - 1);
      this.edgeCount = edgeCount;
    }

    static Packed build(int nodeCount, int[] start, int[] adj) {
      int bits = Math.max(1, 32 - Integer.numberOfLeadingZeros(Math.max(1, nodeCount - 1)));
      long m = adj.length;
      // +1 word of padding so a read at the last edge can safely touch words[w+1].
      long totalBits = m * bits;
      long[] words = new long[(int) (totalBits >>> 6) + 2];
      long k = 0;
      for (int i = 0; i < adj.length; i++) {
        long value = adj[i] & 0xFFFFFFFFL;
        long bitPos = k * bits;
        int w = (int) (bitPos >>> 6);
        int off = (int) (bitPos & 63);
        words[w] |= value << off;
        if (off + bits > 64) {
          words[w + 1] |= value >>> (64 - off);
        }
        k++;
      }
      return new Packed(words, bits, m);
    }

    long read(long k) {
      long bitPos = k * bits;
      int w = (int) (bitPos >>> 6);
      int off = (int) (bitPos & 63);
      long lo = words[w] >>> off;
      long hi = (off + bits <= 64) ? 0L : (words[w + 1] << (64 - off));
      return (lo | hi) & mask;
    }
  }

  // ---- Run-length "sequence list", one int per run (30-bit start + 2-bit len, len<=4) ----

  static final class SeqB {
    final int[] records;

    private SeqB(int[] records) {
      this.records = records;
    }

    static SeqB build(int nodeCount, int[] start, int[] adj) {
      // Pass 1: count records.
      int recCount = 0;
      for (int u = 0; u < nodeCount; u++) {
        recCount += countRecords(start[u], start[u + 1], adj);
      }
      int[] rec = new int[recCount];
      int p = 0;
      for (int u = 0; u < nodeCount; u++) {
        p = writeRecords(start[u], start[u + 1], adj, rec, p);
      }
      return new SeqB(rec);
    }

    private static int countRecords(int from, int to, int[] adj) {
      int recs = 0;
      int i = from;
      while (i < to) {
        int runStart = adj[i];
        int len = 1;
        i++;
        while (i < to && adj[i] == runStart + len) {
          len++;
          i++;
        }
        recs += (len + 3) / 4;  // runs > 4 split into ceil(len/4) records
      }
      return recs;
    }

    private static int writeRecords(int from, int to, int[] adj, int[] rec, int p) {
      int i = from;
      while (i < to) {
        int runStart = adj[i];
        int len = 1;
        i++;
        while (i < to && adj[i] == runStart + len) {
          len++;
          i++;
        }
        int emitted = 0;
        while (emitted < len) {
          int chunk = Math.min(4, len - emitted);
          rec[p++] = ((runStart + emitted) << 2) | (chunk - 1);
          emitted += chunk;
        }
      }
      return p;
    }
  }
}
