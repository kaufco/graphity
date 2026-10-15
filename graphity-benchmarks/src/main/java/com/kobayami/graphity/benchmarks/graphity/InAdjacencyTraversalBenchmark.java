package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.SortedIntView;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Walks every in-adjacency list of a bi-adjacent graph through {@code inNodes(node)} and sums
 * the node IDs, with no algorithm on top - the counterpart of
 * {@link OutAdjacencyTraversalBenchmark} for the other direction. The in-adjacency is built in
 * the fixture, outside the measurement.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class InAdjacencyTraversalBenchmark {

  @Benchmark
  public long inAdjacencyTraversal(BiAdjacentGraphFixture fx) {
    var graph = fx.graph;
    long sum = 0;
    for (int node = 0, n = graph.nodeCount(); node < n; node++) {
      SortedIntView inNodes = graph.inNodes(node);
      for (int i = 0, size = inNodes.size(); i < size; i++) {
        sum += inNodes.getInt(i);
      }
    }
    return sum;
  }
}
