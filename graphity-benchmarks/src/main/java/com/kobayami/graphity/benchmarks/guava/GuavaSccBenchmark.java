package com.kobayami.graphity.benchmarks.guava;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Guava counterpart to {@link com.kobayami.graphity.benchmarks.graphity.SccBenchmark}.
 *
 * <p>Runs {@link GuavaTarjanScc}, which is a direct port of Graphity's Tarjan SCC
 * algorithm onto Guava's data model. The performance difference reflects the data
 * model overhead, not a different algorithm.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class GuavaSccBenchmark {

  @Benchmark
  public Map<Integer, Integer> scc(GuavaGraphFixture fx) {
    return new GuavaTarjanScc(fx.graph).compute();
  }
}
