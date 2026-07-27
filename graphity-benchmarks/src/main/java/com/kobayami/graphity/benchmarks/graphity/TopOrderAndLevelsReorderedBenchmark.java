package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.TopOrderAndLevels;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/** TopOrderAndLevels on a DFS-preorder reordered DAG. See {@link ReorderedDagFixture}. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class TopOrderAndLevelsReorderedBenchmark {

  @Benchmark
  public TopOrderAndLevels topOrderAndLevels(ReorderedDagFixture fx) {
    return TopOrderAndLevels.of(fx.graph);
  }
}
