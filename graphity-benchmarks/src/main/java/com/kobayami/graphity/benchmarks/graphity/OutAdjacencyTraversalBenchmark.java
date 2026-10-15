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
 * Walks every out-adjacency list through {@code outNodes(node)} and sums the node IDs, with no
 * algorithm on top. Isolates the cost of the access path, so that a change to it can be judged
 * apart from the algorithms built on it. Graphity-only; {@link InAdjacencyTraversalBenchmark}
 * is the counterpart over {@code inNodes}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class OutAdjacencyTraversalBenchmark {

  @Benchmark
  public long outAdjacencyTraversal(GraphFixture fx) {
    var graph = fx.graph;
    long sum = 0;
    for (int node = 0, n = graph.nodeCount(); node < n; node++) {
      SortedIntView outNodes = graph.outNodes(node);
      for (int i = 0, size = outNodes.size(); i < size; i++) {
        sum += outNodes.getInt(i);
      }
    }
    return sum;
  }
}
