package com.kobayami.graphity.benchmarks.webgraph;

import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import it.unimi.dsi.webgraph.ImmutableGraph;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.nio.file.Path;

/** JMH state for WebGraph benchmarks restricted to acyclic graphs. */
@State(Scope.Benchmark)
public class WebGraphDagFixture {

  @Param({"PATH", "TREE", "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  @Param({"synthetic"})
  public String inputMode;

  @Param({""})
  public String datasetPath;

  public ImmutableGraph graph;
  private Path tmpRoot;

  @Setup(Level.Trial)
  public void setupGraph() throws IOException {
    com.kobayami.graphity.Graph template =
        BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ true);
    tmpRoot = WebGraphGraphs.createTempWorkspace();
    graph = WebGraphGraphs.fromGraphity(template, tmpRoot);
  }

  @TearDown(Level.Trial)
  public void teardown() {
    WebGraphGraphs.deleteTree(tmpRoot);
  }
}
