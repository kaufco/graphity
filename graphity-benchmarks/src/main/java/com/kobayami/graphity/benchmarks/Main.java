package com.kobayami.graphity.benchmarks;

import com.kobayami.graphity.benchmarks.memory.MemoryMain;

import java.util.Arrays;

/**
 * Entry point for the benchmark uber-jar. Dispatches to either JMH (performance) or
 * the JOL-based memory measurement based on the first argument.
 *
 * <pre>
 *   java -jar graphity-benchmarks.jar [JMH args...]    # performance benchmarks (default)
 *   java -jar graphity-benchmarks.jar mem [mem args...]# memory footprint (JOL)
 *   java -jar graphity-benchmarks.jar --help           # brief usage
 * </pre>
 *
 * <p>Any argument other than {@code mem} / {@code --help} (including none) is forwarded
 * verbatim to {@link org.openjdk.jmh.Main}. That way the full JMH CLI
 * ({@code -l}, {@code -f}, {@code -i}, {@code -p}, {@code -rf}, {@code -prof gc}, …)
 * remains available.
 */
public final class Main {

  private Main() {}

  public static void main(String[] args) throws Exception {
    if (args.length > 0 && args[0].equals("--help")) {
      printUsage();
      return;
    }
    if (args.length > 0 && args[0].equals("mem")) {
      MemoryMain.run(Arrays.copyOfRange(args, 1, args.length));
      return;
    }
    if (args.length > 0 && args[0].equals("report")) {
      ReportMain.run(Arrays.copyOfRange(args, 1, args.length));
      return;
    }
    if (args.length > 0 && args[0].equals("validate")) {
      ValidateMain.run(Arrays.copyOfRange(args, 1, args.length));
      return;
    }
    // Default: forward everything to JMH.
    org.openjdk.jmh.Main.main(args);
  }

  private static void printUsage() {
    System.out.println("""
        Graphity benchmark runner

        Usage:
          java -jar graphity-benchmarks.jar                       # run all JMH benchmarks
          java -jar graphity-benchmarks.jar [JMH args...]         # JMH with custom flags
          java -jar graphity-benchmarks.jar mem [options]         # memory footprint (JOL)
          java -jar graphity-benchmarks.jar report [options]      # aggregate → Markdown
          java -jar graphity-benchmarks.jar validate [options]    # semantic quick-fail checks
          java -jar graphity-benchmarks.jar --help                # this help

        Performance-tuning examples (forwarded to JMH):
          -l                                                      # list available benchmarks
          -f 1 -wi 2 -i 3                                         # fewer forks/iterations (quick)
          -rf json -rff results/perf.json                         # JSON export for later analysis
          -prof gc                                                # allocation rate per op
          -p nodeCount=1000                                       # restrict param values
          .*Scc.*                                                 # run only SCC benchmarks

        Memory measurement:
          Reports retained heap size (JOL) of the fully-built Graph object for each
          (shape, nodeCount) combination. Single-shot — no warmup, no iterations.
          Output: CSV (stdout by default; -o FILE to write to disk).
        """);
  }
}
