package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Shared JMH state: a pre-built Graph, parameterized by (shape × nodeCount).
 *
 * <p>The graph is constructed once per trial in {@link #setupGraph()} — benchmarks
 * that operate on an existing graph (SCC, TopOrder, …) reference this fixture so
 * that construction cost is excluded from their measurement.
 *
 * <p>Construction itself is benchmarked separately in {@link BuildBenchmark}, where
 * we do NOT use this fixture (we rebuild inside the benchmark method).
 */
@State(Scope.Benchmark)
public class GraphFixture {

  @Param({"PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED", "CHAIN_OF_CLIQUES_SHUFFLED"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  @Param({"synthetic"})
  public String inputMode;

  @Param({""})
  public String datasetPath;

  public Graph graph;

  @Setup(Level.Trial)
  public void setupGraph() {
    graph = BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ false);
  }
}
