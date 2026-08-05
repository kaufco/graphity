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

/**
 * Kahn's algorithm on a bi-adjacent graph, against {@link TopOrderAndLevelsBenchmark} on the
 * uni-adjacent one. Reported as the {@code Graphity (bi-adjacent)} column; the cost of the
 * in-adjacency itself is {@link BiAdjacencyBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class TopOrderAndLevelsBiAdjacentBenchmark {

  @Benchmark
  public TopOrderAndLevels topOrderAndLevels(BiAdjacentDagFixture fx) {
    return TopOrderAndLevels.of(fx.graph);
  }
}
