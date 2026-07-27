package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Sccs;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * SCC on a DFS-preorder reordered graph. See {@link ReorderedGraphFixture}; results are
 * reported in the "Graphity (reordered)" column alongside {@link SccBenchmark}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class SccReorderedBenchmark {

  @Benchmark
  public Sccs scc(ReorderedGraphFixture fx) {
    return Components.sccsOf(fx.graph);
  }
}
