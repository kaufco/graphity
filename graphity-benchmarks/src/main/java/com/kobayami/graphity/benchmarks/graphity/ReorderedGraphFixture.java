package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Variant of {@link GraphFixture} whose graph is DFS-preorder reordered once per trial via
 * {@link Graphs#dfsReordered(Graph)}. The one-time reordering cost is excluded from the
 * measured algorithm (it is captured separately in {@link ReorderBenchmark}); the benchmarks
 * that use this fixture therefore measure algorithm throughput on a cache-friendly layout.
 *
 * <p>Kept as a dedicated {@code @State} class (rather than a parameter on {@link GraphFixture})
 * so the reordered and as-built runs execute in separate JMH invocations and cannot warm each
 * other up.
 */
@State(Scope.Benchmark)
public class ReorderedGraphFixture {

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
    Graph base = BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ false);
    graph = Graphs.dfsReordered(base);
  }
}
