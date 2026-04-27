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

/**
 * JMH state: a pre-built {@link it.unimi.dsi.webgraph.BVGraph} (WebGraph's compressed
 * in-memory format), parameterised by (shape × nodeCount). Built once per trial from a
 * Graphity template so every library sees structurally identical input.
 */
@State(Scope.Benchmark)
public class WebGraphGraphFixture {

  @Param({"PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED", "CHAIN_OF_CLIQUES_SHUFFLED"})
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
        BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ false);
    tmpRoot = WebGraphGraphs.createTempWorkspace();
    graph = WebGraphGraphs.fromGraphity(template, tmpRoot);
  }

  @TearDown(Level.Trial)
  public void teardown() {
    WebGraphGraphs.deleteTree(tmpRoot);
  }
}
