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
 * DAG-restricted counterpart of {@link ReorderedGraphFixture}, for algorithms that require an
 * acyclic input (e.g. {@link com.kobayami.graphity.TopOrder#of}). DFS-preorder relabeling
 * preserves acyclicity, so the reordered graph remains a valid DAG.
 */
@State(Scope.Benchmark)
public class ReorderedDagFixture {

  @Param({"PATH", "TREE", "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"})
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
    Graph base = BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ true);
    graph = Graphs.dfsReordered(base);
  }
}
