package com.kobayami.graphity.benchmarks.jgrapht;

import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JGraphT counterpart to {@link com.kobayami.graphity.benchmarks.graphity.SccBenchmark}.
 *
 * <p>Uses {@link KosarajuStrongConnectivityInspector}, which is JGraphT's standard
 * DFS-based SCC implementation (two-pass Kosaraju's algorithm, matching Graphity's
 * internal approach for a fair comparison).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class JGraphTSccBenchmark {

  @Benchmark
  public List<Set<Integer>> scc(JGraphTGraphFixture fx) {
    return new KosarajuStrongConnectivityInspector<>(fx.graph).stronglyConnectedSets();
  }
}
