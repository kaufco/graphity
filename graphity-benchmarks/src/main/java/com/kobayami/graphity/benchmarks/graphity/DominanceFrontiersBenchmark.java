package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.controlflow.ControlFlow;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/** {@code ControlFlow.dominanceFrontiers} on precomputed dominators. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class DominanceFrontiersBenchmark {

  @Benchmark
  public void dominanceFrontiers(ControlFlowFixture fx, Blackhole bh) {
    for (var dominators : fx.dominators) bh.consume(ControlFlow.dominanceFrontiers(dominators));
  }
}
