package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graph;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Measures the one-time cost of {@link Graph#biAdjacentGraph()}, i.e. the up-front price of the
 * in-adjacency that the {@code *BiAdjacentBenchmark} classes run on. Graphity-only; reported in
 * its own subsection beside the reorder cost.
 *
 * <p>Without this number a faster bi-adjacent column would carry no price tag: building the
 * in-adjacency is itself O(nodeCount + edgeCount) and doubles the graph's footprint, so what
 * the report has to answer is after how many algorithm runs it pays for itself.
 *
 * <p>The input comes from {@link GraphFixture}, not from a DAG fixture: in-adjacency has nothing
 * to do with acyclicity, and the full shape set says more about the construction cost.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class BiAdjacencyBenchmark {

  @Benchmark
  public BiAdjacentGraph biAdjacency(GraphFixture fx) {
    return fx.graph.biAdjacentGraph();
  }
}
