package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.Graph;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/** JMH state for Guava benchmarks restricted to acyclic graphs. */
@State(Scope.Benchmark)
public class GuavaDagFixture {

  @Param({"PATH", "TREE", "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"})
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
        BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ true);
    graph = GuavaGraphs.fromGraphity(template);
  }
}
