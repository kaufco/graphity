package com.kobayami.graphity.benchmarks.guava;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Guava counterpart to {@link com.kobayami.graphity.benchmarks.graphity.TopOrderBenchmark}.
 *
 * <p>Runs {@link GuavaDfsTopOrder}, a direct port of Graphity's reverse-postorder
 * DFS topological sort onto Guava's data model.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class GuavaTopOrderBenchmark {

  @Benchmark
  public Integer[] topOrder(GuavaDagFixture fx) {
    return new GuavaDfsTopOrder(fx.graph).compute();
  }
}
