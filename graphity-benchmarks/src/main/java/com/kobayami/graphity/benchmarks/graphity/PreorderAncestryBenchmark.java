package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.PreorderAncestry;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/** {@code PreorderAncestry.of} on the precomputed ancestry of the dominator tree. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class PreorderAncestryBenchmark {

  @Benchmark
  public void preorderAncestry(ControlFlowFixture fx, Blackhole bh) {
    for (var ancestry : fx.dominatorAncestries) bh.consume(PreorderAncestry.of(ancestry));
  }
}
