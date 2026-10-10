package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.IteratedDominanceFrontier;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Phi placement as SSA construction does it: build the structure once per graph from
 * precomputed dominators, then one {@code frontierOf} per simulated variable.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class IteratedDominanceFrontierBenchmark {

  @Benchmark
  public void iteratedDominanceFrontier(ControlFlowFixture fx, Blackhole bh) {
    for (int g = 0; g < fx.graphs.length; g++) {
      var idf = IteratedDominanceFrontier.of(fx.dominators[g]);
      for (var blocks : fx.definitions[g]) bh.consume(idf.frontierOf(blocks));
    }
  }
}
