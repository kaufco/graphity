package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.Edges;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/** Back-edge detection on a DFS-preorder reordered graph. See {@link ReorderedGraphFixture}. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class BackEdgesReorderedBenchmark {

  @Benchmark
  public Edges backEdges(ReorderedGraphFixture fx) {
    return BackEdges.of(fx.graph);
  }
}
