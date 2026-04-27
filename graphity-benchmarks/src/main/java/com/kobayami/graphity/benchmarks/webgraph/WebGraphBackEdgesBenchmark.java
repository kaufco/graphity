package com.kobayami.graphity.benchmarks.webgraph;

import com.kobayami.graphity.benchmarks.common.BenchEdgePairs;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WebGraphBackEdgesBenchmark {

  @Benchmark
  public BenchEdgePairs backEdges(WebGraphGraphFixture fx, Blackhole bh) {
    BenchEdgePairs result = WebGraphBackEdges.of(fx.graph);
    bh.consume(result.count());
    return result;
  }
}
