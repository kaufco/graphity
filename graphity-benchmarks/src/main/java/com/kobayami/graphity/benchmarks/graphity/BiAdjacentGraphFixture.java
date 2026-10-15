package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Bi-adjacent counterpart of {@link GraphFixture}: the same graph, handed over as a
 * {@link BiAdjacentGraph}. The conversion happens once per trial, outside the measurement;
 * {@link BiAdjacencyBenchmark} prices it separately.
 */
@State(Scope.Benchmark)
public class BiAdjacentGraphFixture {

  @Param({"PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED", "CHAIN_OF_CLIQUES_SHUFFLED"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  @Param({"synthetic"})
  public String inputMode;

  @Param({""})
  public String datasetPath;

  public BiAdjacentGraph graph;

  @Setup(Level.Trial)
  public void setupGraph() {
    graph = BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ false)
        .biAdjacentGraph();
  }
}
