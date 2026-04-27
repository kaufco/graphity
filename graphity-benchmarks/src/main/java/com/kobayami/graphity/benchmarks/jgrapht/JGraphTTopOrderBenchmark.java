package com.kobayami.graphity.benchmarks.jgrapht;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JGraphT counterpart to {@link com.kobayami.graphity.benchmarks.graphity.TopOrderBenchmark}.
 *
 * <p>Uses {@link TopologicalOrderIterator}, drained fully so the measurement
 * reflects the entire topological sort (not just iterator setup). The consumed
 * nodes are fed into a {@link Blackhole} to prevent dead-code elimination.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class JGraphTTopOrderBenchmark {

  @Benchmark
  public void topOrder(JGraphTDagFixture fx, Blackhole bh) {
    TopologicalOrderIterator<Integer, DefaultEdge> it = new TopologicalOrderIterator<>(fx.graph);
    while (it.hasNext()) {
      bh.consume(it.next());
    }
  }
}
