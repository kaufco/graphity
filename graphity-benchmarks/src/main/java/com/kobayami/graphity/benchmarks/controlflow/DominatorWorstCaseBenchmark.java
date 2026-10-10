package com.kobayami.graphity.benchmarks.controlflow;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.ControlFlowAlternatives;
import com.kobayami.graphity.Dominators;
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
 * Growth on the fan behind a chain, the input family on which plain SEMI-NCA and the iterative
 * algorithm are quadratic. A tenfold larger graph should cost about ten times as much with
 * Lengauer-Tarjan (the library) or SEMI-NCA with jump pointers, and about a hundred times as
 * much without. Single-shot, because the quadratic variants take seconds at the largest size.
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class DominatorWorstCaseBenchmark {

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  private BiAdjacentGraph graph;
  private final IntList entries = IntList.of(0);

  @Setup(Level.Trial)
  public void setup() {
    graph = ControlFlowShape.FAN_BEHIND_CHAIN.generate(nodeCount, 0).biAdjacentGraph();
  }

  @Benchmark
  public Dominators lengauerTarjan() {
    return Dominators.of(graph, entries);
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
}
