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
 * Bi-adjacent counterpart of {@link DagFixture}: the same acyclic graph, handed over as a
 * {@link BiAdjacentGraph}.
 *
 * <p>This is the only fixture in the suite that carries in-adjacency. Every other one builds a
 * uni-adjacent graph, so the branch in {@code TopOrderAndLevels.of} that reads in-degrees
 * straight from the in-adjacency offsets in O(nodeCount) — rather than scanning all edges in
 * O(nodeCount + edgeCount) — is reachable only through here.
 *
 * <p>The conversion happens once per trial and is therefore outside the measurement;
 * {@link BiAdjacencyBenchmark} prices it separately.
 */
@State(Scope.Benchmark)
public class BiAdjacentDagFixture {

  @Param({"PATH", "TREE", "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"})
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
    graph = BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ true)
        .biAdjacentGraph();
  }
}
