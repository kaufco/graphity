package com.kobayami.graphity.benchmarks.webgraph;

import it.unimi.dsi.webgraph.ImmutableGraph;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Measures the one-time cost of turning a {@link it.unimi.dsi.webgraph.BVGraph} into its
 * symmetric closure — the structure WebGraph's connected-component algorithm requires and a
 * BVGraph, storing out-adjacency only, cannot provide.
 *
 * <p>The WebGraph counterpart to {@code ReorderBenchmark} and {@code BiAdjacencyBenchmark}: an
 * up-front price that decides whether the algorithm behind it is worth reaching for. Each
 * invocation runs one full offline symmetrization plus the store and re-load that make the
 * result a compressed graph again.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WebGraphSymmetrizeBenchmark {

  @Benchmark
  public ImmutableGraph symmetrize(WebGraphGraphFixture fx) {
    return WebGraphGraphs.symmetrized(fx.graph, fx.tmpRoot);
  }
}
