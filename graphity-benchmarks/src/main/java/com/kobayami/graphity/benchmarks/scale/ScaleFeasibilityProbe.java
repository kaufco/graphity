package com.kobayami.graphity.benchmarks.scale;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.EdgeTypes;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.TopOrder;
import com.kobayami.graphity.TopOrderAndLevels;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphBackEdges;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphDfsTopOrder;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphEdgeTypes;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphGraphs;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.algo.StronglyConnectedComponents;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * One-shot feasibility probe (NOT a benchmark): for each requested {@code n}, runs every
 * measurement once for Graphity and WebGraph and reports wall time + retained heap (after a
 * forced GC), or the failure ({@code OutOfMemoryError}, {@code StackOverflowError}, …).
 * Answers "can library X even complete measurement Y at n = 100M / 500M on this machine,
 * and how much time / memory does it take?".
 *
 * <p>Run <em>one</em> {@code n} per JVM so an OOM at large n does not lose smaller-n results:
 * <pre>{@code
 *   java -Xmx75g -cp ... ScaleFeasibilityProbe 100000000
 * }</pre>
 *
 * <p>Uses {@code GNP_SPARSE} (avg degree 3) for cyclic algorithms and {@code DAG_GNP_SPARSE}
 * for the topological ones — the lightest realistic shapes.
 */
public final class ScaleFeasibilityProbe {

  private ScaleFeasibilityProbe() {}

  private static Path tmpRoot;
  private static boolean doGraphity = true;
  private static boolean doWebgraph = true;

  public static void main(String[] args) throws Exception {
    int n = args.length > 0 ? parseInt(args[0]) : 100_000_000;
    if (args.length > 1) {
      String lib = args[1].toLowerCase(Locale.ROOT);
      doGraphity = lib.equals("graphity") || lib.equals("both");
      doWebgraph = lib.equals("webgraph") || lib.equals("both");
    }
    tmpRoot = WebGraphGraphs.createTempWorkspace();
    try {
      System.out.printf(Locale.ROOT,
          "# Scale feasibility probe (single run, GNP_SPARSE deg 3 / DAG_GNP_SPARSE)%n");
      System.out.printf(Locale.ROOT, "# maxHeap = %,d MB | libs: %s%s%n%n",
          Runtime.getRuntime().maxMemory() >> 20,
          doGraphity ? "graphity " : "", doWebgraph ? "webgraph" : "");
      probe(n);
    } finally {
      WebGraphGraphs.deleteTree(tmpRoot);
    }
  }

  private static int parseInt(String s) {
    return Integer.parseInt(s.replace("_", "").replace(",", ""));
  }

  private static void probe(int n) {
    System.out.printf(Locale.ROOT, "## n = %,d%n", n);
    System.out.println("| library | measurement | result | time | retained heap |");
    System.out.println("|---|---|---|---:|---:|");

    // ---- Graphity (iterative cores) ----
    Graph gSparse = doGraphity
        ? run("graphity", "build (GNP_SPARSE)", () -> BenchmarkGraphs.generate(GraphShape.GNP_SPARSE, n))
        : BenchmarkGraphs.generate(GraphShape.GNP_SPARSE, n);
    if (gSparse != null && doGraphity) {
      System.out.printf(Locale.ROOT, "| | | (m = %,d) | | |%n", gSparse.edgeCount());
      run("graphity", "scc", () -> Components.sccsOf(gSparse));
      run("graphity", "wcc", () -> Components.wccsOf(gSparse));
      run("graphity", "ccs", () -> Components.ccsOf(gSparse));
      run("graphity", "backedges", () -> BackEdges.of(gSparse));
      run("graphity", "edgetypes", () -> EdgeTypes.of(gSparse));
    }
    Graph gDag = doGraphity
        ? run("graphity", "build (DAG_GNP_SPARSE)", () -> BenchmarkGraphs.generate(GraphShape.DAG_GNP_SPARSE, n))
        : BenchmarkGraphs.generate(GraphShape.DAG_GNP_SPARSE, n);
    if (gDag != null && doGraphity) {
      run("graphity", "toporder", () -> TopOrder.of(gDag));
      run("graphity", "toporderandlevels", () -> TopOrderAndLevels.of(gDag));
    }
    if (!doWebgraph) {
      System.out.println();
      return;
    }

    // ---- WebGraph (BVGraph compressed; recursive DFS wrappers for backedges/edgetypes/toporder) ----
    final ImmutableGraph bv = gSparse == null ? null
        : run("webgraph", "build (BVGraph, GNP_SPARSE)", () -> WebGraphGraphs.fromGraphity(gSparse, tmpRoot));
    if (bv != null) {
      run("webgraph", "scc (library, iterative)", () -> StronglyConnectedComponents.compute(bv, false, null));
      run("webgraph", "backedges (recursive)", () -> WebGraphBackEdges.of(bv));
      run("webgraph", "edgetypes (recursive)", () -> WebGraphEdgeTypes.of(bv));
    }
    final ImmutableGraph bvDag = gDag == null ? null
        : run("webgraph", "build (BVGraph, DAG_GNP_SPARSE)", () -> WebGraphGraphs.fromGraphity(gDag, tmpRoot));
    if (bvDag != null) {
      run("webgraph", "toporder (recursive)", () -> new WebGraphDfsTopOrder(bvDag).compute());
    }
    System.out.println();
  }

  /** Runs {@code op} once, timing it and catching any failure. Returns the result or null. */
  private static <T> T run(String lib, String measurement, Supplier<T> op) {
    long t0 = System.nanoTime();
    try {
      T result = op.get();
      long ms = (System.nanoTime() - t0) / 1_000_000;
      System.out.printf(Locale.ROOT, "| %s | %s | OK | %,d ms | %,d MB |%n",
          lib, measurement, ms, retainedHeapMb());
      System.out.flush();
      return result;
    } catch (Throwable t) {
      long ms = (System.nanoTime() - t0) / 1_000_000;
      String type = t.getClass().getSimpleName();
      String msg = t.getMessage();
      System.out.printf(Locale.ROOT, "| %s | %s | **FAIL** %s%s | %,d ms | |%n",
          lib, measurement, type, msg == null ? "" : (": " + truncate(msg)), ms);
      System.out.flush();
      return null;
    }
  }

  /** Retained (live) heap after a forced GC — an indicative footprint measure. */
  private static long retainedHeapMb() {
    Runtime rt = Runtime.getRuntime();
    for (int i = 0; i < 3; i++) {
      System.gc();
      try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
    return (rt.totalMemory() - rt.freeMemory()) >> 20;
  }

  private static String truncate(String s) {
    s = s.replace('\n', ' ');
    return s.length() > 80 ? s.substring(0, 80) + "…" : s;
  }
}
