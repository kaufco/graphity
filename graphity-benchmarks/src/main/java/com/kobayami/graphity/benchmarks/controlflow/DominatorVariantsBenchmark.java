package com.kobayami.graphity.benchmarks.controlflow;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.ControlFlowAlternatives;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Comparison of dominator algorithms and predecessor sources, for the decisions recorded with
 * the control-flow analyses: Lengauer-Tarjan (the library, on the in-adjacency of a
 * {@code BiAdjacentGraph}) against the same algorithm on predecessor lists rebuilt in preorder,
 * SEMI-NCA with and without jump pointers, and Cooper-Harvey-Kennedy. The in-adjacency is built
 * in the setup, outside the measurement. {@code backEdges} touches no changed code and serves as
 * the control group for machine drift.
 * <p>
 * Not part of the report matrix. The setup checks once that all variants agree.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class DominatorVariantsBenchmark {

  @Param({"STRUCTURED", "IRREDUCIBLE", "CHAIN"})
  public ControlFlowShape shape;

  @Param({"10000", "1000000"})
  public int nodeCount;

  private Graph graph;
  private BiAdjacentGraph biGraph;
  private final IntList entries = IntList.of(0);

  @Setup(Level.Trial)
  public void setup() {
    graph = shape.generate(nodeCount, 42);
    biGraph = graph.biAdjacentGraph();
    int n = graph.nodeCount();
    int[] expected = immediateDominators(Dominators.of(biGraph, entries), n);
    check("preorder lists", expected,
        ControlFlowAlternatives.lengauerTarjanPreorderLists(graph, entries));
    check("SEMI-NCA with jump pointers", expected,
        ControlFlowAlternatives.semiNcaJumps(graph, entries));
    check("plain SEMI-NCA", expected, ControlFlowAlternatives.semiNcaPlain(graph, entries));
    check("Cooper-Harvey-Kennedy", expected,
        ControlFlowAlternatives.cooperHarveyKennedy(graph, entries));
  }

  @Benchmark
  public Dominators lengauerTarjan() {
    return Dominators.of(biGraph, entries);
  }

  @Benchmark
  public int[] lengauerTarjanPreorderLists() {
    return ControlFlowAlternatives.lengauerTarjanPreorderLists(graph, entries);
  }

  @Benchmark
  public int[] semiNcaJumps() {
    return ControlFlowAlternatives.semiNcaJumps(graph, entries);
  }

  @Benchmark
  public int[] semiNcaPlain() {
    return ControlFlowAlternatives.semiNcaPlain(graph, entries);
  }

  @Benchmark
  public int[] cooperHarveyKennedy() {
    return ControlFlowAlternatives.cooperHarveyKennedy(graph, entries);
  }

  @Benchmark
  public Edges backEdges() {
    return BackEdges.of(graph);
  }

  static int[] immediateDominators(Dominators dominators, int n) {
    var result = new int[n];
    for (int i = 0; i < n; i++) result[i] = dominators.immediateDominatorOf(i);
    return result;
  }

  static void check(String variant, int[] expected, int[] actual) {
    if (!Arrays.equals(expected, actual)) {
      throw new IllegalStateException(variant + " disagrees with the library");
    }
  }
}
