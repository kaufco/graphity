package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.TopOrder;
import it.unimi.dsi.fastutil.ints.IntList;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Measures {@link TopOrder#of(com.kobayami.graphity.Graph)} on acyclic inputs only.
 *
 * <p>Uses {@link DagFixture} (restricted shape set) because TopOrder throws on cyclic input.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class TopOrderBenchmark {

  @Benchmark
  public IntList topOrder(DagFixture fx) {
    return TopOrder.of(fx.graph);
  }
}
