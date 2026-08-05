package com.kobayami.graphity.benchmarks.jgrapht;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
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
 * Measures JGraphT's own weakly-connected-component algorithm
 * ({@link ConnectivityInspector}) — the counterpart to Graphity's {@code Components.wccsOf}.
 *
 * <p>{@link ConnectivityInspector} reads a directed graph as undirected, which is exactly
 * Graphity's WCC semantics. No extra structure is needed: a JGraphT graph keeps incoming
 * edges alongside outgoing ones.
 *
 * <p>{@code connectedSets()} is what forces the computation; the inspector itself is lazy.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class JGraphTWccBenchmark {

  @Benchmark
  public List<Set<Integer>> wcc(JGraphTGraphFixture fx) {
    return new ConnectivityInspector<Integer, DefaultEdge>(fx.graph).connectedSets();
  }
}
