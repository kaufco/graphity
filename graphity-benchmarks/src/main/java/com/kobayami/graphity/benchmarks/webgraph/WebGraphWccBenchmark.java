package com.kobayami.graphity.benchmarks.webgraph;

import it.unimi.dsi.webgraph.algo.ConnectedComponents;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Measures WebGraph's own connected-component algorithm ({@link ConnectedComponents}) — the
 * counterpart to Graphity's {@code Components.wccsOf}.
 *
 * <p>Single-threaded on purpose. {@link ConnectedComponents} is the only peer implementation in
 * this suite that takes a thread count, and letting it use every core would put a parallel
 * column next to sequential ones.
 *
 * <p>The graph it runs on is the symmetric closure built by
 * {@link WebGraphSymmetricGraphFixture}; that structure is what a WebGraph user has to build
 * before this algorithm is available at all, and {@link WebGraphSymmetrizeBenchmark} prices it.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WebGraphWccBenchmark {

  @Benchmark
  public ConnectedComponents wcc(WebGraphSymmetricGraphFixture fx) {
    return ConnectedComponents.compute(fx.graph, 1, null);
  }
}
