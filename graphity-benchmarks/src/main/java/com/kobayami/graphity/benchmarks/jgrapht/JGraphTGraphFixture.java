package com.kobayami.graphity.benchmarks.jgrapht;

import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * JMH state: a pre-built JGraphT graph, parameterized by (shape × nodeCount).
 *
 * <p>Constructed in {@link #setupGraph()} by generating a Graphity graph and
 * converting it via {@link JGraphTGraphs#fromGraphity}. Both libraries therefore
 * see structurally identical inputs.
 */
@State(Scope.Benchmark)
public class JGraphTGraphFixture {

  @Param({"PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED", "CHAIN_OF_CLIQUES_SHUFFLED"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  @Param({"synthetic"})
  public String inputMode;

  @Param({""})
  public String datasetPath;

  public Graph<Integer, DefaultEdge> graph;

  @Setup(Level.Trial)
  public void setupGraph() {
    com.kobayami.graphity.Graph template =
        BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ false);
    graph = JGraphTGraphs.fromGraphity(template);
  }
}
