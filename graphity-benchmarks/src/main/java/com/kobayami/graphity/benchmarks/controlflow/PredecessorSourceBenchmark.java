package com.kobayami.graphity.benchmarks.controlflow;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.ControlFlowAlternatives;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntList;
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
 * Dominators starting from a plain {@code Graph}: either the predecessors rebuilt as lists in
 * preorder, or {@code biAdjacentGraph()} built inside the measurement and handed to
 * {@code Dominators.of} - what a caller holding a plain graph pays, since the analysis takes a
 * {@code BiAdjacentGraph}. Both allocate 4 x (nodeCount + edgeCount) bytes for the predecessors;
 * the question is which access pattern is cheaper overall. {@code backEdges} is the control
 * group.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class PredecessorSourceBenchmark {

  @Param({"STRUCTURED", "IRREDUCIBLE", "CHAIN"})
  public ControlFlowShape shape;

  @Param({"10000", "1000000"})
  public int nodeCount;

  private Graph graph;
  private final IntList entries = IntList.of(0);

  @Setup(Level.Trial)
  public void setup() {
    graph = shape.generate(nodeCount, 42);
    int n = graph.nodeCount();
    DominatorVariantsBenchmark.check("preorder lists",
        DominatorVariantsBenchmark.immediateDominators(
            Dominators.of(graph.biAdjacentGraph(), entries), n),
        ControlFlowAlternatives.lengauerTarjanPreorderLists(graph, entries));
  }

  @Benchmark
  public int[] preorderLists() {
    return ControlFlowAlternatives.lengauerTarjanPreorderLists(graph, entries);
  }

  @Benchmark
  public Dominators viaBiAdjacentGraph() {
    return Dominators.of(graph.biAdjacentGraph(), entries);
  }

  @Benchmark
  public Edges backEdges() {
    return BackEdges.of(graph);
  }
}
