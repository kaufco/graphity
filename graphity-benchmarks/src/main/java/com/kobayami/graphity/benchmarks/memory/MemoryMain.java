package com.kobayami.graphity.benchmarks.memory;

import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import com.kobayami.graphity.benchmarks.guava.GuavaGraphs;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTGraphs;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphGraphs;
import org.openjdk.jol.info.GraphLayout;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Memory-footprint measurement via JOL ({@link GraphLayout#parseInstance}).
 *
 * <p>Completely independent of JMH — no warmup, no iterations, no profilers. Measures
 * {@code totalSize()} of the fully-built graph for each library and each
 * {@code (shape, nodeCount)} combination. The resulting CSV is consumed by
 * {@link com.kobayami.graphity.benchmarks.ReportMain} to produce the memory section of
 * the final report.
 *
 * <p>Output CSV columns: {@code library,shape,nodes,edges,totalBytes}. Duplicate
 * {@code (nodes, edges)} pairs from different shapes are preserved on purpose — they
 * document empirically that the footprint depends only on counts, not on topology.
 */
public final class MemoryMain {

  private static final int[] DEFAULT_NODE_COUNTS = {1_000, 10_000, 100_000};
  private static final List<GraphShape> DEFAULT_SHAPES = List.of(GraphShape.values());
  private static final List<String> DEFAULT_LIBRARIES = List.of("graphity", "jgrapht", "guava", "webgraph");

  private static final String HEADER = "library,shape,nodes,edges,totalBytes";

  private MemoryMain() {}

  public static void run(String[] args) throws Exception {
    Path output = null;
    int[] nodeCounts = DEFAULT_NODE_COUNTS;
    List<GraphShape> shapes = DEFAULT_SHAPES;
    Set<String> libraries = new LinkedHashSet<>(DEFAULT_LIBRARIES);

    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      switch (a) {
        case "-o", "--output" -> {
          if (i + 1 >= args.length) fatal(a + " requires a path argument");
          output = Path.of(args[++i]);
        }
        case "-n", "--nodes" -> {
          if (i + 1 >= args.length) fatal(a + " requires a comma-separated list");
          nodeCounts = parseIntList(args[++i]);
        }
        case "-s", "--shapes" -> {
          if (i + 1 >= args.length) fatal(a + " requires a comma-separated list");
          shapes = parseShapeList(args[++i]);
        }
        case "-l", "--libraries" -> {
          if (i + 1 >= args.length) fatal(a + " requires a comma-separated list");
          libraries = parseLibraryList(args[++i]);
        }
        case "-h", "--help" -> {
          printUsage();
          return;
        }
        default -> fatal("unknown argument: " + a);
      }
    }

    int total = nodeCounts.length * shapes.size() * libraries.size();
    int[] idx = {0};
    Path wgTmpRoot = WebGraphGraphs.createTempWorkspace();

    try (PrintStream out = output == null
        ? System.out
        : new PrintStream(Files.newOutputStream(output))) {
      out.println(HEADER);

      for (int n : nodeCounts) {
        for (GraphShape shape : shapes) {
          com.kobayami.graphity.Graph template = BenchmarkGraphs.generate(shape, n);
          int m = template.edgeCount();

          if (libraries.contains("graphity")) {
            measure(out, ++idx[0], total, "graphity", shape, n, m,
                () -> GraphLayout.parseInstance(template).totalSize());
          }
          if (libraries.contains("jgrapht")) {
            measure(out, ++idx[0], total, "jgrapht", shape, n, m,
                () -> GraphLayout.parseInstance(JGraphTGraphs.fromGraphity(template)).totalSize());
          }
          if (libraries.contains("guava")) {
            measure(out, ++idx[0], total, "guava", shape, n, m,
                () -> GraphLayout.parseInstance(GuavaGraphs.fromGraphity(template)).totalSize());
          }
          if (libraries.contains("webgraph")) {
            measure(out, ++idx[0], total, "webgraph", shape, n, m,
                () -> GraphLayout.parseInstance(WebGraphGraphs.fromGraphity(template, wgTmpRoot)).totalSize());
          }
        }
      }
    } finally {
      WebGraphGraphs.deleteTree(wgTmpRoot);
    }

    if (output != null) {
      System.err.printf(Locale.ROOT, "%nMemory measurement complete: %d rows -> %s%n", total, output);
    }
  }

  /** Runs one measurement, prints progress to stderr, emits one CSV line to {@code out}. */
  private static void measure(
      PrintStream out, int idx, int total, String library,
      GraphShape shape, int n, int m, LongSupplier bytes) {
    long t0 = System.nanoTime();
    long totalBytes = bytes.getAsLong();
    long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

    out.printf(Locale.ROOT, "%s,%s,%d,%d,%d%n", library, shape, n, m, totalBytes);
    out.flush();

    System.err.printf(
        Locale.ROOT,
        "[%3d/%3d] %-9s %-20s n=%-6d m=%-8d -> %,14d bytes (%d ms)%n",
        idx, total, library, shape, n, m, totalBytes, elapsedMs);
  }

  // ---- CLI helpers ----

  private static int[] parseIntList(String s) {
    String[] parts = s.split(",");
    int[] out = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      try {
        out[i] = Integer.parseInt(parts[i].trim());
      } catch (NumberFormatException e) {
        fatal("invalid integer in list: " + parts[i]);
      }
    }
    return out;
  }

  private static List<GraphShape> parseShapeList(String s) {
    List<GraphShape> out = new ArrayList<>();
    for (String part : s.split(",")) {
      try {
        out.add(GraphShape.valueOf(part.trim()));
      } catch (IllegalArgumentException e) {
        fatal("unknown shape: " + part + " (available: " + Arrays.toString(GraphShape.values()) + ")");
      }
    }
    return out;
  }

  private static Set<String> parseLibraryList(String s) {
    Set<String> out = new LinkedHashSet<>();
    for (String raw : s.split(",")) {
      String lib = raw.trim().toLowerCase(Locale.ROOT);
      if (lib.isEmpty()) continue;
      if (!DEFAULT_LIBRARIES.contains(lib)) {
        fatal("unknown library: " + raw + " (available: " + DEFAULT_LIBRARIES + ")");
      }
      out.add(lib);
    }
    if (out.isEmpty()) {
      fatal("libraries list must not be empty");
    }
    return out;
  }

  private static void fatal(String msg) {
    System.err.println("error: " + msg);
    printUsage();
    System.exit(2);
  }

  private static void printUsage() {
    System.err.println("""
        Memory-footprint measurement (JOL)

        Usage:
          java -jar graphity-benchmarks.jar mem [options]

        Options:
          -o, --output FILE       write CSV to FILE (default: stdout)
          -n, --nodes LIST        comma-separated node counts (default: 1000,10000,100000)
          -s, --shapes LIST       comma-separated GraphShape names (default: all)
          -l, --libraries LIST    comma-separated libraries (graphity,jgrapht,guava,webgraph)
          -h, --help              this help

        CSV columns:
          library,shape,nodes,edges,totalBytes

        Libraries measured: graphity, jgrapht, guava, webgraph (BVGraph).
        The graph is generated once per (shape, n) via the Graphity generator and
        converted for each peer library, so all four measurements are over the
        same structural graph.
        """);
  }

  @FunctionalInterface
  private interface LongSupplier {
    long getAsLong();
  }
}
