package com.kobayami.graphity.benchmarks.compression;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodeMapping;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Indicative micro-benchmark (manual timing, not JMH) isolating the effect of node-ID
 * <em>ordering</em> on the real traversal cores — the random {@code state[v]}/{@code node[v]}
 * access is the actual bottleneck, and ordering is the lever that changes its cache locality.
 * <p>
 * Same topology, four ID layouts:
 * <ul>
 *   <li><b>natural</b> — as generated (already locality-friendly for these shapes).</li>
 *   <li><b>shuffled</b> — random permutation (realistic worst case / poorly-ordered input).</li>
 *   <li><b>shuffled+DFS</b> — shuffled, then {@link NodeMapping#reindex} (DFS preorder).</li>
 *   <li><b>shuffled+BFS</b> — shuffled, then BFS reindex.</li>
 * </ul>
 * Workloads are the real public algorithms {@link Components#sccsOf} (Tarjan; {@code long[n]}
 * per-node state, the heaviest random access) and {@link BackEdges#of}.
 *
 * <pre>{@code
 * java -cp graphity-benchmarks.jar \
 *   com.kobayami.graphity.benchmarks.compression.AdjacencyReorderBenchmark
 * }</pre>
 */
public final class AdjacencyReorderBenchmark {

  private AdjacencyReorderBenchmark() {}

  private static final int WARMUP = 4;
  private static final int MEASURE = 8;

  public static void main(String[] args) {
    System.out.println("# Reordering-effect micro-benchmark (manual timing, indicative)");
    System.out.println("ms/op = min over " + MEASURE + " iters. Lower is better.");
    System.out.println();

    run(GraphShape.GNP_SPARSE, GraphShape.GNP_SPARSE_SHUFFLED, 2_000_000);
    run(GraphShape.GNP_DENSE, GraphShape.GNP_DENSE_SHUFFLED, 500_000);
  }

  private static void run(GraphShape natural, GraphShape shuffled, int n) {
    Graph gNatural = BenchmarkGraphs.generate(natural, n);
    Graph gShuffled = BenchmarkGraphs.generate(shuffled, n);
    Graph gDfs = Graphs.remapped(gShuffled, NodeMapping.reindex(gShuffled));
    Graph gBfs = Graphs.remapped(gShuffled, NodeMapping.forPermutation(bfsOrder(gShuffled)));

    System.out.printf(Locale.ROOT, "## %s  (n = %,d, m = %,d, avg deg = %.1f)%n%n",
        natural, gNatural.nodeCount(), gNatural.edgeCount(),
        gNatural.edgeCount() / (double) gNatural.nodeCount());
    System.out.println("| Workload | natural | shuffled | shuffled+DFS | shuffled+BFS |");
    System.out.println("|---|---:|---:|---:|---:|");

    timeRow("SCC (Tarjan)", g -> Components.sccsOf(g), gNatural, gShuffled, gDfs, gBfs);
    timeRow("BackEdges", g -> BackEdges.of(g), gNatural, gShuffled, gDfs, gBfs);
    System.out.println();
  }

  private static void timeRow(String label, Consumer<Graph> op,
                              Graph natural, Graph shuffled, Graph dfs, Graph bfs) {
    double tN = bestMs(op, natural);
    double tS = bestMs(op, shuffled);
    double tD = bestMs(op, dfs);
    double tB = bestMs(op, bfs);
    System.out.printf(Locale.ROOT, "| %s | %.2f | %.2f (%.0f%%) | %.2f (%.0f%%) | %.2f (%.0f%%) |%n",
        label, tN, tS, 100 * tS / tN, tD, 100 * tD / tN, tB, 100 * tB / tN);
  }

  private static final Object[] SINK = new Object[1];

  private static double bestMs(Consumer<Graph> op, Graph g) {
    for (int i = 0; i < WARMUP; i++) op.accept(g);
    long best = Long.MAX_VALUE;
    for (int i = 0; i < MEASURE; i++) {
      long t0 = System.nanoTime();
      op.accept(g);
      long dt = System.nanoTime() - t0;
      SINK[0] = g;  // keep the consumer result path live
      if (dt < best) best = dt;
    }
    return best / 1e6;
  }

  /** {@code order[newId] = oldId} in BFS visitation order over the whole forest. */
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
}
