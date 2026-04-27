package com.kobayami.graphity.benchmarks.webgraph;

import it.unimi.dsi.webgraph.algo.StronglyConnectedComponents;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * WebGraph counterpart to {@link com.kobayami.graphity.benchmarks.graphity.SccBenchmark}.
 *
 * <p>Runs {@link StronglyConnectedComponents#compute} — WebGraph's built-in Tarjan
 * implementation — over a loaded {@link it.unimi.dsi.webgraph.BVGraph}. Successor
 * decoding happens on the fly from the compressed bit stream, so this benchmarks
 * both the algorithm and WebGraph's decode overhead.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WebGraphSccBenchmark {

  @Benchmark
  public StronglyConnectedComponents scc(WebGraphGraphFixture fx) {
    return StronglyConnectedComponents.compute(fx.graph, false, null);
  }
}
