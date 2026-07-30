package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * JMH state holding a pre-computed {@link NodePartition}, for benchmarking
 * {@link NodePartition#condense()} in isolation.
 *
 * <p>Both the graph and the SCC partition are built in {@link #setupPartition()} so that
 * neither construction nor Tarjan is part of the measurement — {@code condense()} is
 * typically orders of magnitude cheaper than the partition it operates on, so including
 * either would hide it entirely.
 *
 * <p>Shape selection targets the group-count dimension rather than the node-count
 * dimension, because {@code condense()} scales with the number of groups:
 * <ul>
 *   <li>{@link GraphShape#DAG_GNP_SPARSE} — acyclic, so every SCC is a singleton:
 *       {@code groupCount == nodeCount}, and group targets spread over the whole ID range.
 *       Maximum group count reachable for a given {@code nodeCount}.</li>
 *   <li>{@link GraphShape#PATH} — also all singletons, but each group has exactly one
 *       target, and that target is the adjacent group ID. Separates "many groups" from
 *       "many edges per group".</li>
 *   <li>{@link GraphShape#CHAIN_OF_CLIQUES} — mid-size SCCs, so {@code groupCount} is
 *       roughly {@code nodeCount / 32}: same node count, two orders of magnitude fewer
 *       groups.</li>
 *   <li>{@link GraphShape#GNP_DENSE} — usually one giant SCC, so {@code groupCount} is
 *       near 1 and {@code condense()} has almost nothing to do. Control shape.</li>
 * </ul>
 */
@State(Scope.Benchmark)
public class CondenseFixture {

  @Param({"PATH", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES", "DAG_GNP_SPARSE"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  @Param({"synthetic"})
  public String inputMode;

  @Param({""})
  public String datasetPath;

  public NodePartition partition;

  @Setup(Level.Trial)
  public void setupPartition() {
    Graph graph =
        BenchmarkGraphs.fromInput(shape, nodeCount, inputMode, datasetPath, /*requireDag=*/ false);
    partition = Components.sccsOf(graph);
  }
}
