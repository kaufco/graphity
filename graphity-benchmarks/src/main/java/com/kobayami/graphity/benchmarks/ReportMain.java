package com.kobayami.graphity.benchmarks;

import com.sun.management.OperatingSystemMXBean;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Report generator: aggregates JMH perf JSON, JMH alloc JSON, and JOL memory CSV into
 * a single Markdown report.
 *
 * <p>Invocation:
 * <pre>{@code
 * java -jar graphity-benchmarks.jar report \
 *      --perf  results/perf-quick-20260420-132417.json \
 *      --alloc results/alloc-quick-20260420-133502.json \
 *      --mem   results/mem-quick-20260420-134120.csv \
 *      --profile quick \
 *      --out   results/report-quick-20260420-134120.md
 * }</pre>
 *
 * <p>Missing input files produce a report with the corresponding section omitted (and
 * a short notice in its place) — useful when only one of the measurement phases was run.
 */
public final class ReportMain {

  private static final DateTimeFormatter TS_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Fixed shape order for row sorting — matches the {@code @Param} declarations in benchmarks.
   * Shuffled variants are listed immediately after their base shape so that side-by-side
   * comparison in the rendered tables is straightforward.
   */
  private static final List<String> SHAPE_ORDER = List.of(
      "PATH", "PATH_SHUFFLED",
      "TREE", "TREE_SHUFFLED",
      "GNP_SPARSE", "GNP_SPARSE_SHUFFLED",
      "GNP_DENSE", "GNP_DENSE_SHUFFLED",
      "CHAIN_OF_CLIQUES", "CHAIN_OF_CLIQUES_SHUFFLED",
      "DAG_GNP_SPARSE", "DAG_GNP_SPARSE_SHUFFLED",
      "DAG_GNP_DENSE", "DAG_GNP_DENSE_SHUFFLED");

  /** Canonical algorithm names (map from benchmark-class stem → displayed name). */
  private static final Map<String, String> ALGO_NAME = Map.ofEntries(
      Map.entry("Build", "Build"),
      Map.entry("Scc", "SCC"),
      Map.entry("Wcc", "WCC"),
      Map.entry("Ccs", "CCs"),
      Map.entry("TopOrder", "TopOrder"),
      Map.entry("TopOrderAndLevels", "TopOrderAndLevels"),
      Map.entry("BackEdges", "BackEdges"),
      Map.entry("EdgeTypes", "EdgeTypes"));

  /** Libraries in the preferred display order (Graphity first as the baseline). */
  private static final List<String> LIB_ORDER = List.of("graphity", "jgrapht", "guava", "webgraph");
  private static final Map<String, String> LIB_DISPLAY = Map.of(
      "graphity", "Graphity",
      "jgrapht", "JGraphT",
      "guava", "Guava",
      "webgraph", "WebGraph");

  private ReportMain() {}

  public static void run(String[] args) throws IOException {
    Path perfPath = null, allocPath = null, memPath = null, outPath = null;
    String profile = "unknown";

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--perf" -> perfPath = Path.of(args[++i]);
        case "--alloc" -> allocPath = Path.of(args[++i]);
        case "--mem" -> memPath = Path.of(args[++i]);
        case "--out" -> outPath = Path.of(args[++i]);
        case "--profile" -> profile = args[++i];
        case "-h", "--help" -> { printUsage(); return; }
        default -> { System.err.println("error: unknown arg: " + args[i]); printUsage(); System.exit(2); }
      }
    }

    List<PerfResult> perf = perfPath == null ? List.of() : loadPerf(perfPath);
    List<AllocResult> alloc = allocPath == null ? List.of() : loadAlloc(allocPath);
    List<MemResult> mem = memPath == null ? List.of() : loadMem(memPath);

    try (PrintStream out = outPath == null
        ? System.out
        : new PrintStream(Files.newOutputStream(outPath), false, StandardCharsets.UTF_8)) {
      emitReport(out, profile, perf, alloc, mem, perfPath, allocPath, memPath);
    }
    if (outPath != null) {
      System.err.println("Report written to " + outPath);
    }
  }

  // ======================================================================
  // Rendering
  // ======================================================================

  private static void emitReport(
      PrintStream out, String profile,
      List<PerfResult> perf, List<AllocResult> alloc, List<MemResult> mem,
      Path perfPath, Path allocPath, Path memPath) {

    emitHeader(out, profile);

    if (perf.isEmpty()) {
      out.println("## 1 · Performance\n\n*(no perf input — skipped)*\n");
    } else {
      emitPerfSection(out, perf);
    }

    if (mem.isEmpty()) {
      out.println("## 2 · Memory Footprint\n\n*(no memory input — skipped)*\n");
    } else {
      emitMemorySection(out, mem);
    }

    if (alloc.isEmpty()) {
      out.println("## 3 · Allocations\n\n*(no allocation input — skipped)*\n");
    } else {
      emitAllocSection(out, alloc);
    }

    if (!perf.isEmpty()) {
      emitPrecisionSection(out, perf);
    }

    emitSourcesFooter(out, perfPath, allocPath, memPath);
  }

  private static void emitHeader(PrintStream out, String profile) {
    out.println("# Graphity Benchmark Report");
    out.println();
    out.println("```");
    out.printf(Locale.ROOT, "Profile:      %s%n", profile);
    out.printf(Locale.ROOT, "Timestamp:    %s%n", ZonedDateTime.now().format(TS_FORMAT));
    out.printf(Locale.ROOT, "JDK:          %s %s (%s)%n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.version"),
        System.getProperty("java.vm.vendor"));

    String osName = System.getProperty("os.name");
    String osVersion = System.getProperty("os.version");
    String osArch = System.getProperty("os.arch");
    String distro = linuxDistro();
    if (distro != null) {
      out.printf(Locale.ROOT, "OS:           %s, kernel %s (%s)%n", distro, osVersion, osArch);
    } else {
      out.printf(Locale.ROOT, "OS:           %s %s (%s)%n", osName, osVersion, osArch);
    }

    String cpu = cpuModel();
    int cores = Runtime.getRuntime().availableProcessors();
    if (cpu != null) {
      out.printf(Locale.ROOT, "CPU:          %s (%d logical cores)%n", cpu, cores);
    } else {
      out.printf(Locale.ROOT, "CPU:          (%d logical cores)%n", cores);
    }

    try {
      OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
      long ramBytes = os.getTotalMemorySize();
      out.printf(Locale.ROOT, "RAM:          %.1f GiB%n", ramBytes / (1024.0 * 1024.0 * 1024.0));
    } catch (Throwable ignored) {
      // RAM line omitted if MXBean unavailable
    }
    out.println("```");
    out.println();
  }

  // ---- Section 1: Performance -----------------------------------------

  private static void emitPerfSection(PrintStream out, List<PerfResult> perf) {
    out.println("## 1 · Performance");
    out.println();

    // Group by algorithm, then by nodeCount.
    Map<String, List<PerfResult>> byAlgo = new LinkedHashMap<>();
    for (String canonical : List.of(
        "Build", "Scc", "Wcc", "Ccs", "TopOrder", "TopOrderAndLevels",
        "BackEdges", "EdgeTypes")) {
      byAlgo.put(canonical, new ArrayList<>());
    }
    for (PerfResult r : perf) byAlgo.computeIfAbsent(r.algoKey, k -> new ArrayList<>()).add(r);

    int subIdx = 0;
    for (Map.Entry<String, List<PerfResult>> e : byAlgo.entrySet()) {
      if (e.getValue().isEmpty()) continue;
      subIdx++;
      emitPerfAlgoSubsection(out, subIdx, e.getKey(), e.getValue());
    }
  }

  private static void emitPerfAlgoSubsection(PrintStream out, int idx, String algoKey, List<PerfResult> rows) {
    String pretty = ALGO_NAME.getOrDefault(algoKey, algoKey);

    // Group by nodeCount.
    Map<Integer, List<PerfResult>> byN = new LinkedHashMap<>();
    rows.stream()
        .sorted(Comparator.comparingInt((PerfResult r) -> r.nodeCount))
        .forEach(r -> byN.computeIfAbsent(r.nodeCount, k -> new ArrayList<>()).add(r));

    int n2Idx = 0;
    for (Map.Entry<Integer, List<PerfResult>> e : byN.entrySet()) {
      n2Idx++;
      int n = e.getKey();
      List<PerfResult> rs = e.getValue();
      out.printf("### 1.%d.%d · %s @ n = %s%n%n", idx, n2Idx, pretty, fmtInt(n));

      List<String> librariesPresent = librariesIn(rs);
      emitPerfTable(out, rs, librariesPresent);
      out.println();
    }
  }

  private static void emitPerfTable(PrintStream out, List<PerfResult> rows, List<String> libs) {
    StringBuilder hdr = new StringBuilder("| Shape |");
    StringBuilder sep = new StringBuilder("|---|");
    for (String lib : libs) {
      hdr.append(" ").append(LIB_DISPLAY.get(lib)).append(" |");
      sep.append("---:|");
    }
    out.println(hdr);
    out.println(sep);

    Map<String, Map<String, Double>> cells = new LinkedHashMap<>();
    for (PerfResult r : rows) {
      cells.computeIfAbsent(r.shape, k -> new LinkedHashMap<>()).put(r.library, r.score);
    }

    for (String shape : shapeOrderPresent(cells.keySet())) {
      Map<String, Double> byLib = cells.get(shape);
      StringBuilder line = new StringBuilder("| ").append(shape).append(" |");
      Double baseline = byLib.get("graphity");
      for (String lib : libs) {
        Double v = byLib.get(lib);
        line.append(" ").append(v == null ? "—" : withRatio(fmtMs(v) + " ms", v, baseline, lib)).append(" |");
      }
      out.println(line);
    }
  }

  /**
   * Appends a {@code " (ratio×)"} suffix to {@code formatted} when {@code lib} is a peer
   * (i.e. not {@code "graphity"}) and a meaningful ratio against {@code baseline} can be
   * computed. For the baseline library itself, or when the baseline is missing, the
   * pre-formatted value is returned unchanged.
   */
  private static String withRatio(String formatted, double value, Double baseline, String lib) {
    if (lib.equals("graphity") || baseline == null || baseline <= 0) return formatted;
    return formatted + " (" + fmtSpeedup(value / baseline) + ")";
  }

  // ---- Section 2: Memory ----------------------------------------------

  private static void emitMemorySection(PrintStream out, List<MemResult> mem) {
    out.println("## 2 · Memory Footprint");
    out.println();
    out.println(
        "> *Graphity's `Graph` and WebGraph's `BVGraph` both store out-adjacency only "
        + "(forward-directed). For algorithms that require O(1) reverse-edge lookups (e.g. "
        + "bidirectional BFS), Graphity offers a `BiAdjacentGraph` variant that additionally "
        + "stores in-adjacency; its footprint is roughly 2× the Graphity values shown below. "
        + "JGraphT's `DefaultDirectedGraph` and Guava's `MutableGraph` are bidirectional by "
        + "construction, so the strict apples-to-apples comparison against them would use "
        + "`BiAdjacentGraph` (double the Graphity numbers). Even then Graphity retains "
        + "a multi-× memory advantage over both peers. The WebGraph/Graphity comparison is "
        + "already apples-to-apples as shown.*");
    out.println();
    out.println(
        "> *Rows are indexed by `(Shape, Nodes, Edges)`. For Graphity, JGraphT and Guava the "
        + "footprint is determined purely by the counts and does not depend on topology, so their "
        + "numbers coincide across shapes with matching `(n, m)` — including every `*_SHUFFLED` "
        + "variant against its base. For WebGraph's `BVGraph` topology matters: its gap, reference "
        + "and interval codes collapse adjacency lists when node IDs are clustered in ID space "
        + "(e.g. `CHAIN_OF_CLIQUES`) and inflate back toward the information-theoretic floor when "
        + "IDs are randomly permuted. Compare each `*_SHUFFLED` row against its base shape to see "
        + "the effect.*");
    out.println();

    // Sort: (nodes asc, edges asc, shape-order, library order) so that every SHUFFLED variant
    // sits directly under its base shape when both share the same (n, m).
    List<MemResult> sorted = new ArrayList<>(mem);
    sorted.sort(Comparator
        .comparingInt((MemResult r) -> r.nodes)
        .thenComparingInt(r -> r.edges)
        .thenComparingInt(r -> shapeRank(r.shape))
        .thenComparing(r -> LIB_ORDER.indexOf(r.library)));

    // Pivot: each (shape,nodes,edges) gets one row with columns for each library.
    Map<String, Map<String, Long>> byPair = new LinkedHashMap<>();
    for (MemResult r : sorted) {
      String key = r.shape + "|" + r.nodes + "|" + r.edges;
      byPair.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(r.library, r.totalBytes);
    }
    List<String> libs = librariesInMem(mem);

    // ---- 2.1 Total Graph Size
    out.println("### 2.1 · Total Graph Size");
    out.println();
    {
      StringBuilder hdr = new StringBuilder("| Shape | Nodes | Edges | Edges/Node |");
      StringBuilder sep = new StringBuilder("|---|---:|---:|---:|");
      for (String lib : libs) { hdr.append(" ").append(LIB_DISPLAY.get(lib)).append(" |"); sep.append("---:|"); }
      out.println(hdr);
      out.println(sep);

      for (Map.Entry<String, Map<String, Long>> e : byPair.entrySet()) {
        String[] parts = e.getKey().split("\\|");
        String shape = parts[0];
        int n = Integer.parseInt(parts[1]);
        int m = Integer.parseInt(parts[2]);
        Map<String, Long> byLib = e.getValue();
        Long gBytes = byLib.get("graphity");
        Double baseline = gBytes == null ? null : (double) gBytes;

        StringBuilder line = new StringBuilder("| ")
            .append(shape).append(" | ")
            .append(fmtInt(n)).append(" | ").append(fmtInt(m)).append(" | ")
            .append(fmtDouble(edgesPerNode(n, m))).append(" |");
        for (String lib : libs) {
          Long v = byLib.get(lib);
          line.append(" ").append(v == null ? "—" : withRatio(fmtInt(v) + " B", (double) v, baseline, lib)).append(" |");
        }
        out.println(line);
      }
      out.println();
    }

    // ---- 2.2 Bytes per Element
    out.println("### 2.2 · Bytes per Element  `totalBytes / (Nodes + Edges)`");
    out.println();
    {
      StringBuilder hdr = new StringBuilder("| Shape | Nodes | Edges |");
      StringBuilder sep = new StringBuilder("|---|---:|---:|");
      for (String lib : libs) { hdr.append(" ").append(LIB_DISPLAY.get(lib)).append(" |"); sep.append("---:|"); }
      out.println(hdr);
      out.println(sep);

      for (Map.Entry<String, Map<String, Long>> e : byPair.entrySet()) {
        String[] parts = e.getKey().split("\\|");
        String shape = parts[0];
        int n = Integer.parseInt(parts[1]);
        int m = Integer.parseInt(parts[2]);
        long denom = (long) n + m;
        if (denom <= 0) continue;

        Map<String, Long> byLib = e.getValue();
        Long gTotal = byLib.get("graphity");
        Double baselineBpe = gTotal == null ? null : gTotal / (double) denom;

        StringBuilder line = new StringBuilder("| ")
            .append(shape).append(" | ")
            .append(fmtInt(n)).append(" | ").append(fmtInt(m)).append(" |");
        for (String lib : libs) {
          Long v = byLib.get(lib);
          if (v == null) { line.append(" — |"); continue; }
          double bpe = v / (double) denom;
          line.append(" ").append(withRatio(String.format(Locale.ROOT, "%.2f B", bpe), bpe, baselineBpe, lib)).append(" |");
        }
        out.println(line);
      }
      out.println();
    }
  }

  /**
   * Position of {@code shape} in {@link #SHAPE_ORDER}; shapes not listed there come last (but
   * remain in their mutual string order) so that custom shapes produced by user extensions
   * don't cause spurious collisions with {@code -1} indices.
   */
  private static int shapeRank(String shape) {
    int idx = SHAPE_ORDER.indexOf(shape);
    return idx < 0 ? SHAPE_ORDER.size() : idx;
  }

  // ---- Section 3: Allocations -----------------------------------------

  private static void emitAllocSection(PrintStream out, List<AllocResult> alloc) {
    out.println("## 3 · Allocations  (bytes allocated per algorithm invocation)");
    out.println();
    out.println(
        "> *Allocations measured in a separate JMH run with `-prof gc`, so the perf numbers "
        + "in §1 stay un-instrumented. Values below the JMH detection threshold (~16 B) are "
        + "shown as `~0`.*");
    out.println();

    // Group by algorithm, then nodeCount.
    Map<String, List<AllocResult>> byAlgo = new LinkedHashMap<>();
    for (String canonical : List.of(
        "Build", "Scc", "Wcc", "Ccs", "TopOrder", "TopOrderAndLevels",
        "BackEdges", "EdgeTypes")) {
      byAlgo.put(canonical, new ArrayList<>());
    }
    for (AllocResult r : alloc) byAlgo.computeIfAbsent(r.algoKey, k -> new ArrayList<>()).add(r);

    int subIdx = 0;
    for (Map.Entry<String, List<AllocResult>> e : byAlgo.entrySet()) {
      if (e.getValue().isEmpty()) continue;
      subIdx++;
      emitAllocAlgoSubsection(out, subIdx, e.getKey(), e.getValue());
    }
  }

  private static void emitAllocAlgoSubsection(PrintStream out, int idx, String algoKey, List<AllocResult> rows) {
    String pretty = ALGO_NAME.getOrDefault(algoKey, algoKey);

    Map<Integer, List<AllocResult>> byN = new LinkedHashMap<>();
    rows.stream()
        .sorted(Comparator.comparingInt((AllocResult r) -> r.nodeCount))
        .forEach(r -> byN.computeIfAbsent(r.nodeCount, k -> new ArrayList<>()).add(r));

    int n2Idx = 0;
    for (Map.Entry<Integer, List<AllocResult>> e : byN.entrySet()) {
      n2Idx++;
      int n = e.getKey();
      List<AllocResult> rs = e.getValue();
      out.printf("### 3.%d.%d · %s @ n = %s%n%n", idx, n2Idx, pretty, fmtInt(n));

      List<String> libs = librariesInAlloc(rs);
      emitAllocTable(out, rs, libs);
      out.println();
    }
  }

  private static void emitAllocTable(PrintStream out, List<AllocResult> rows, List<String> libs) {
    StringBuilder hdr = new StringBuilder("| Shape |");
    StringBuilder sep = new StringBuilder("|---|");
    for (String lib : libs) { hdr.append(" ").append(LIB_DISPLAY.get(lib)).append(" |"); sep.append("---:|"); }
    out.println(hdr);
    out.println(sep);

    Map<String, Map<String, Double>> cells = new LinkedHashMap<>();
    for (AllocResult r : rows) cells.computeIfAbsent(r.shape, k -> new LinkedHashMap<>()).put(r.library, r.bytesPerOp);

    for (String shape : shapeOrderPresent(cells.keySet())) {
      Map<String, Double> byLib = cells.get(shape);
      StringBuilder line = new StringBuilder("| ").append(shape).append(" |");
      Double baseline = byLib.get("graphity");
      for (String lib : libs) {
        Double v = byLib.get(lib);
        line.append(" ").append(v == null ? "—" : withRatio(fmtBytes(v) + " B", v, baseline, lib)).append(" |");
      }
      out.println(line);
    }
  }

  // ---- Section 4: Precision -------------------------------------------

  private static void emitPrecisionSection(PrintStream out, List<PerfResult> perf) {
    out.println("## 4 · Precision Summary");
    out.println();
    out.println(
        "> *All performance benchmarks sorted by relative error (`±error / score`) descending. "
        + "JMH reports `±error` as the 99.9% CI half-width. Memory measurements are single-trial "
        + "(no error bar). Allocation precision is identical to the perf precision shown here.*");
    out.println();

    out.println("| Benchmark | Shape | Nodes | Score [ms/op] | ±Error | Rel |");
    out.println("|---|---|---:|---:|---:|---:|");

    List<PerfResult> sorted = new ArrayList<>(perf);
    sorted.sort(Comparator.comparingDouble((PerfResult r) ->
        r.score == 0 ? Double.MAX_VALUE : -Math.abs(r.scoreError / r.score)));

    for (PerfResult r : sorted) {
      double rel = r.score == 0 ? 0 : Math.abs(r.scoreError / r.score) * 100.0;
      out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s | %.0f%% |%n",
          shortBenchmarkName(r.benchmarkFull),
          r.shape, fmtInt(r.nodeCount), fmtMs(r.score), fmtMs(r.scoreError), rel);
    }
    out.println();
  }

  // ---- Sources footer --------------------------------------------------

  private static void emitSourcesFooter(PrintStream out, Path perfPath, Path allocPath, Path memPath) {
    out.println("---");
    out.println();
    out.println("```");
    out.printf("Sources: %s%n", perfPath == null ? "(no perf input)" : perfPath.getFileName());
    out.printf("         %s%n", allocPath == null ? "(no alloc input)" : allocPath.getFileName());
    out.printf("         %s%n", memPath == null ? "(no mem input)" : memPath.getFileName());
    out.println("```");
  }

  // ======================================================================
  // Input parsing
  // ======================================================================

  static List<PerfResult> loadPerf(Path path) throws IOException {
    List<PerfResult> out = new ArrayList<>();
    for (Map<String, Object> e : parseJmhArray(path)) {
      String benchmark = (String) e.get("benchmark");
      double score = asDouble(getNested(e, "primaryMetric", "score"));
      double err = asDouble(getNested(e, "primaryMetric", "scoreError"));
      Map<String, Object> params = castMap(e.get("params"));
      String shape = params == null ? "?" : asStr(params.get("shape"));
      int nodeCount = params == null ? 0 : asInt(params.get("nodeCount"));

      Meta meta = parseBenchmarkName(benchmark);
      out.add(new PerfResult(benchmark, meta.library, meta.algoKey, shape, nodeCount, score, err));
    }
    return out;
  }

  static List<AllocResult> loadAlloc(Path path) throws IOException {
    List<AllocResult> out = new ArrayList<>();
    for (Map<String, Object> e : parseJmhArray(path)) {
      String benchmark = (String) e.get("benchmark");
      Map<String, Object> params = castMap(e.get("params"));
      String shape = params == null ? "?" : asStr(params.get("shape"));
      int nodeCount = params == null ? 0 : asInt(params.get("nodeCount"));

      Map<String, Object> sec = castMap(e.get("secondaryMetrics"));
      Double bytesPerOp = null;
      if (sec != null) {
        Object norm = sec.get("·gc.alloc.rate.norm");
        if (norm == null) norm = sec.get("gc.alloc.rate.norm");
        if (norm instanceof Map) {
          bytesPerOp = asDouble(((Map<?, ?>) norm).get("score"));
        }
      }
      if (bytesPerOp == null) continue;  // no gc metric recorded — skip silently

      Meta meta = parseBenchmarkName(benchmark);
      out.add(new AllocResult(benchmark, meta.library, meta.algoKey, shape, nodeCount, bytesPerOp));
    }
    return out;
  }

  static List<MemResult> loadMem(Path path) throws IOException {
    List<MemResult> out = new ArrayList<>();
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    for (int i = 1; i < lines.size(); i++) {  // skip header
      String line = lines.get(i).trim();
      if (line.isEmpty()) continue;
      String[] parts = line.split(",");
      if (parts.length < 5) continue;
      out.add(new MemResult(
          parts[0].trim(), parts[1].trim(),
          Integer.parseInt(parts[2].trim()),
          Integer.parseInt(parts[3].trim()),
          Long.parseLong(parts[4].trim())));
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> parseJmhArray(Path path) throws IOException {
    String text = Files.readString(path, StandardCharsets.UTF_8);
    Object parsed = new MiniJson(text).parse();
    if (!(parsed instanceof List<?> arr)) {
      throw new IOException("expected top-level JSON array in " + path);
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object e : arr) out.add((Map<String, Object>) e);
    return out;
  }

  // ======================================================================
  // Benchmark-name parsing
  // ======================================================================

  private static Meta parseBenchmarkName(String fullyQualified) {
    // e.g. "com.kobayami.graphity.benchmarks.graphity.SccBenchmark.scc"
    //   or "com.kobayami.graphity.benchmarks.jgrapht.JGraphTSccBenchmark.scc"
    //   or "com.kobayami.graphity.benchmarks.guava.GuavaTopOrderBenchmark.topOrder"
    int lastDot = fullyQualified.lastIndexOf('.');
    String classPart = fullyQualified.substring(0, lastDot);
    int classDot = classPart.lastIndexOf('.');
    String simple = classPart.substring(classDot + 1);
    String pkgPart = classPart.substring(0, classDot);
    int pkgDot = pkgPart.lastIndexOf('.');
    String library = pkgPart.substring(pkgDot + 1);

    // Strip library-specific class prefix to get the algorithm key.
    String stem = simple;
    if (library.equals("jgrapht") && stem.startsWith("JGraphT")) stem = stem.substring("JGraphT".length());
    if (library.equals("guava") && stem.startsWith("Guava")) stem = stem.substring("Guava".length());
    if (library.equals("webgraph") && stem.startsWith("WebGraph")) stem = stem.substring("WebGraph".length());
    if (stem.endsWith("Benchmark")) stem = stem.substring(0, stem.length() - "Benchmark".length());
    return new Meta(library, stem);
  }

  private static String shortBenchmarkName(String fqn) {
    int lastDot = fqn.lastIndexOf('.');
    int prevDot = fqn.lastIndexOf('.', lastDot - 1);
    return fqn.substring(prevDot + 1);
  }

  // ======================================================================
  // Formatting helpers
  // ======================================================================

  private static String fmtInt(long v) {
    // Use thin-space-like grouping via Locale.ROOT with a non-breaking space replacement.
    return String.format(Locale.ROOT, "%,d", v).replace(',', ' ');
  }

  private static String fmtDouble(double v) {
    return String.format(Locale.ROOT, "%.2f", v);
  }

  private static String fmtMs(double v) {
    if (Math.abs(v) >= 100) return String.format(Locale.ROOT, "%.1f", v);
    return String.format(Locale.ROOT, "%.3f", v);
  }

  private static String fmtSpeedup(double ratio) {
    if (ratio >= 100) return String.format(Locale.ROOT, "%.0f×", ratio);
    if (ratio >= 10) return String.format(Locale.ROOT, "%.1f×", ratio);
    return String.format(Locale.ROOT, "%.2f×", ratio);
  }

  private static String fmtBytes(double v) {
    if (v < 16) return "~0";
    return fmtInt((long) Math.round(v));
  }

  private static double edgesPerNode(int n, int m) {
    return n == 0 ? 0 : (double) m / n;
  }

  private static List<String> librariesIn(List<PerfResult> rows) {
    TreeSet<String> seen = new TreeSet<>(Comparator.comparingInt(LIB_ORDER::indexOf));
    for (PerfResult r : rows) seen.add(r.library);
    List<String> out = new ArrayList<>();
    for (String l : LIB_ORDER) if (seen.contains(l)) out.add(l);
    return out;
  }

  private static List<String> librariesInAlloc(List<AllocResult> rows) {
    TreeSet<String> seen = new TreeSet<>(Comparator.comparingInt(LIB_ORDER::indexOf));
    for (AllocResult r : rows) seen.add(r.library);
    List<String> out = new ArrayList<>();
    for (String l : LIB_ORDER) if (seen.contains(l)) out.add(l);
    return out;
  }

  private static List<String> librariesInMem(List<MemResult> rows) {
    TreeSet<String> seen = new TreeSet<>(Comparator.comparingInt(LIB_ORDER::indexOf));
    for (MemResult r : rows) seen.add(r.library);
    List<String> out = new ArrayList<>();
    for (String l : LIB_ORDER) if (seen.contains(l)) out.add(l);
    return out;
  }

  private static List<String> shapeOrderPresent(java.util.Set<String> shapes) {
    List<String> out = new ArrayList<>();
    for (String s : SHAPE_ORDER) if (shapes.contains(s)) out.add(s);
    for (String s : shapes) if (!SHAPE_ORDER.contains(s)) out.add(s);  // unknown shapes at end
    return out;
  }

  // ======================================================================
  // Env detection
  // ======================================================================

  private static String linuxDistro() {
    Path p = Path.of("/etc/os-release");
    if (!Files.isReadable(p)) return null;
    try {
      Properties props = new Properties();
      for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
        int eq = line.indexOf('=');
        if (eq < 0) continue;
        String key = line.substring(0, eq).trim();
        String val = line.substring(eq + 1).trim();
        if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length() - 1);
        props.setProperty(key, val);
      }
      String pretty = props.getProperty("PRETTY_NAME");
      return pretty != null ? pretty : props.getProperty("NAME");
    } catch (IOException e) {
      return null;
    }
  }

  private static String cpuModel() {
    Path p = Path.of("/proc/cpuinfo");
    if (!Files.isReadable(p)) return null;
    try {
      for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
        if (line.startsWith("model name")) {
          int colon = line.indexOf(':');
          return colon < 0 ? null : line.substring(colon + 1).trim();
        }
      }
    } catch (IOException ignored) {}
    return null;
  }

  // ======================================================================
  // JSON parsing helpers (cast & navigate)
  // ======================================================================

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Object o) {
    return o instanceof Map ? (Map<String, Object>) o : null;
  }

  private static Object getNested(Map<String, Object> m, String... keys) {
    Object cur = m;
    for (String k : keys) {
      if (!(cur instanceof Map)) return null;
      cur = ((Map<?, ?>) cur).get(k);
    }
    return cur;
  }

  private static double asDouble(Object o) {
    if (o == null) return Double.NaN;
    if (o instanceof Number n) return n.doubleValue();
    return Double.parseDouble(o.toString());
  }

  private static int asInt(Object o) {
    if (o == null) return 0;
    if (o instanceof Number n) return n.intValue();
    return Integer.parseInt(o.toString().trim());
  }

  private static String asStr(Object o) {
    return o == null ? null : o.toString();
  }

  // ======================================================================
  // Usage
  // ======================================================================

  private static void printUsage() {
    System.err.println("""
        Benchmark report generator

        Usage:
          java -jar graphity-benchmarks.jar report [options]

        Options:
          --perf   FILE       JMH performance JSON (from 'perf' run)
          --alloc  FILE       JMH allocation JSON (from 'alloc' run with -prof gc)
          --mem    FILE       Memory CSV (from 'mem' run)
          --profile NAME      Profile label for the report header (e.g. quick, full)
          --out    FILE       Output Markdown file (default: stdout)
          -h, --help          This help.

        Any subset of --perf / --alloc / --mem may be omitted; the corresponding
        section is then skipped in the report.
        """);
  }

  // ======================================================================
  // Data records
  // ======================================================================

  /** One row from the perf JSON. */
  record PerfResult(
      String benchmarkFull, String library, String algoKey,
      String shape, int nodeCount, double score, double scoreError) {}

  /** One row from the alloc JSON (only gc.alloc.rate.norm extracted). */
  record AllocResult(
      String benchmarkFull, String library, String algoKey,
      String shape, int nodeCount, double bytesPerOp) {}

  /** One row from the mem CSV. */
  record MemResult(String library, String shape, int nodes, int edges, long totalBytes) {}

  private record Meta(String library, String algoKey) {}

  // ======================================================================
  // Minimal JSON parser — handles objects, arrays, strings, numbers, bool, null.
  // Returns nested Map<String,Object> / List<Object> / String / Double / Boolean / null.
  // Good enough for JMH output; not a general-purpose JSON library.
  // ======================================================================

  static final class MiniJson {
    private final String src;
    private int pos;

    MiniJson(String src) { this.src = src; }

    Object parse() {
      skipWs();
      Object v = parseValue();
      skipWs();
      if (pos != src.length()) throw err("trailing content");
      return v;
    }

    private Object parseValue() {
      skipWs();
      if (pos >= src.length()) throw err("unexpected end");
      char c = src.charAt(pos);
      return switch (c) {
        case '{' -> parseObject();
        case '[' -> parseArray();
        case '"' -> parseString();
        case 't', 'f' -> parseBool();
        case 'n' -> parseNull();
        default -> parseNumber();
      };
    }

    private Map<String, Object> parseObject() {
      expect('{');
      Map<String, Object> out = new LinkedHashMap<>();
      skipWs();
      if (peek() == '}') { pos++; return out; }
      while (true) {
        skipWs();
        String key = parseString();
        skipWs();
        expect(':');
        Object val = parseValue();
        out.put(key, val);
        skipWs();
        char c = src.charAt(pos++);
        if (c == ',') continue;
        if (c == '}') return out;
        throw err("expected , or } after object entry");
      }
    }

    private List<Object> parseArray() {
      expect('[');
      List<Object> out = new ArrayList<>();
      skipWs();
      if (peek() == ']') { pos++; return out; }
      while (true) {
        Object val = parseValue();
        out.add(val);
        skipWs();
        char c = src.charAt(pos++);
        if (c == ',') continue;
        if (c == ']') return out;
        throw err("expected , or ] in array");
      }
    }

    private String parseString() {
      expect('"');
      StringBuilder sb = new StringBuilder();
      while (pos < src.length()) {
        char c = src.charAt(pos++);
        if (c == '"') return sb.toString();
        if (c == '\\') {
          if (pos >= src.length()) throw err("bad escape");
          char esc = src.charAt(pos++);
          switch (esc) {
            case '"', '\\', '/' -> sb.append(esc);
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'u' -> {
              if (pos + 4 > src.length()) throw err("bad \\u");
              sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
              pos += 4;
            }
            default -> throw err("bad escape: \\" + esc);
          }
        } else {
          sb.append(c);
        }
      }
      throw err("unterminated string");
    }

    private Object parseBool() {
      if (src.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
      if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
      throw err("bad bool");
    }

    private Object parseNull() {
      if (src.startsWith("null", pos)) { pos += 4; return null; }
      throw err("bad null");
    }

    private Object parseNumber() {
      int start = pos;
      if (peek() == '-') pos++;
      while (pos < src.length() && "0123456789.eE+-".indexOf(src.charAt(pos)) >= 0) pos++;
      return Double.parseDouble(src.substring(start, pos));
    }

    private void skipWs() {
      while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
      if (pos >= src.length()) throw err("unexpected end");
      return src.charAt(pos);
    }

    private void expect(char c) {
      if (pos >= src.length() || src.charAt(pos) != c) throw err("expected '" + c + "'");
      pos++;
    }

    private RuntimeException err(String msg) {
      return new IllegalStateException("JSON parse error at " + pos + ": " + msg);
    }
  }
}
