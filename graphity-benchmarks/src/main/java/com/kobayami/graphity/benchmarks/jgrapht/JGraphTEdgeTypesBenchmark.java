package com.kobayami.graphity.benchmarks.jgrapht;

import com.kobayami.graphity.benchmarks.common.BenchTypedEdges;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class JGraphTEdgeTypesBenchmark {

  @Benchmark
  public BenchTypedEdges edgeTypes(JGraphTGraphFixture fx) {
    return JGraphTEdgeTypes.of(fx.graph);
  }
}
