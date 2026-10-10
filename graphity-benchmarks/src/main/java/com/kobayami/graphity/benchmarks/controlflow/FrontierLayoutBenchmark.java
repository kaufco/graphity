package com.kobayami.graphity.benchmarks.controlflow;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.ControlFlowAlternatives;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.IteratedDominanceFrontier;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Random;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Comparison of two layouts for iterated dominance frontiers: the dominator tree in preorder with
 * subtree sizes (the library, 16 bytes per node) against the tree as child lists (12 bytes per
 * node). Each operation builds the structure and answers one query per simulated variable, as
 * SSA construction would. {@code backEdges} is the control group.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class FrontierLayoutBenchmark {

  /** Simulated variables per graph, each defined in one to four random blocks. */
  private static final int VARIABLES = 200;

  @Param({"STRUCTURED", "IRREDUCIBLE", "LADDER"})
  public ControlFlowShape shape;

  @Param({"10000", "100000"})
  public int nodeCount;

  private Graph graph;
  private Dominators dominators;
  private IntList[] definitions;

  @Setup(Level.Trial)
  public void setup() {
    graph = shape.generate(nodeCount, 42);
    dominators = Dominators.of(graph.biAdjacentGraph(), 0);
    var rnd = new Random(7);
    int n = graph.nodeCount();
    definitions = new IntList[VARIABLES];
    for (int v = 0; v < VARIABLES; v++) {
      var blocks = new IntArrayList();
      for (int i = 0, c = 1 + rnd.nextInt(4); i < c; i++) blocks.add(rnd.nextInt(n));
      definitions[v] = blocks;
    }
    var preorder = IteratedDominanceFrontier.of(dominators);
    var childLists = new ControlFlowAlternatives.ChildListFrontier(dominators);
    for (var blocks : definitions) {
      if (!Arrays.equals(preorder.frontierOf(blocks).toIntArray(), childLists.frontierOf(blocks))) {
        throw new IllegalStateException("the child-list layout disagrees with the library");
      }
    }
  }

  @Benchmark
  public void preorder(Blackhole bh) {
    var idf = IteratedDominanceFrontier.of(dominators);
    for (var blocks : definitions) bh.consume(idf.frontierOf(blocks));
  }

  @Benchmark
  public void childLists(Blackhole bh) {
    var idf = new ControlFlowAlternatives.ChildListFrontier(dominators);
    for (var blocks : definitions) bh.consume(idf.frontierOf(blocks));
  }

  @Benchmark
  public Edges backEdges() {
    return BackEdges.of(graph);
  }
}
