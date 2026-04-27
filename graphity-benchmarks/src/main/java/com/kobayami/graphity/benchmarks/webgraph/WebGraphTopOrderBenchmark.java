package com.kobayami.graphity.benchmarks.webgraph;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * WebGraph counterpart to {@link com.kobayami.graphity.benchmarks.graphity.TopOrderBenchmark}.
 *
 * <p>WebGraph does not ship a topological-sort utility, so this benchmark runs
 * {@link WebGraphDfsTopOrder}, a direct port of Graphity's reverse-postorder DFS
 * onto WebGraph's {@link it.unimi.dsi.webgraph.ImmutableGraph} data model. The
 * auxiliary data structures (primitive {@link java.util.BitSet}s and {@code int[]}s)
 * are the same as Graphity's, so the measurable delta is essentially the cost of
 * decoding successors from WebGraph's compressed bit stream vs reading a CSR array.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WebGraphTopOrderBenchmark {

  @Benchmark
  public int[] topOrder(WebGraphDagFixture fx) {
    return new WebGraphDfsTopOrder(fx.graph).compute();
  }
}
