package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodeMapping;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Measures the one-time cost of DFS-preorder reordering ({@link NodeMapping#reindex(Graph)} plus
 * {@link Graphs#remapped(Graph, NodeMapping)}), i.e. the up-front price paid to obtain the
 * cache-friendly layout benchmarked by the {@code *ReorderedBenchmark} classes. Graphity-only;
 * reported in its own subsection.
 *
 * <p>The input graph comes from {@link GraphFixture} (built once per trial); each invocation
 * runs one full reindex + remap, so the score is the cost of transforming an existing graph
 * into its reordered copy.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class ReorderBenchmark {

  @Benchmark
  public Graph reorder(GraphFixture fx) {
    return Graphs.remapped(fx.graph, NodeMapping.reindex(fx.graph));
  }
}
