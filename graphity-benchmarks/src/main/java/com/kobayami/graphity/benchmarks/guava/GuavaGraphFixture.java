package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.Graph;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * JMH state: a pre-built Guava graph, parameterized by (shape × nodeCount).
 * Built from a Graphity template, so all libraries see identical input.
 */
@State(Scope.Benchmark)
public class GuavaGraphFixture {

  @Param({"PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED", "CHAIN_OF_CLIQUES_SHUFFLED"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  @Param({"synthetic"})
  public String inputMode;

  @Param({""})
  public String datasetPath;

  public Graph<Integer> graph;

  @Setup(Level.Trial)
  public void setupGraph() {
    com.kobayami.graphity.Graph template =
        BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ false);
    graph = GuavaGraphs.fromGraphity(template);
  }
}
