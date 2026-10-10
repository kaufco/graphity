package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.Ancestry;
import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.benchmarks.controlflow.ControlFlowShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Random;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Shared JMH state for the control-flow analyses: one graph of a {@link ControlFlowShape}, or for
 * {@code SMALL_METHODS} many small ones, entry node 0. Inputs that a benchmark does not measure -
 * the in-adjacency the analyses take, the dominators for the frontier and ancestry benchmarks,
 * the definition blocks of the simulated variables - are prepared once per trial.
 */
@State(Scope.Benchmark)
public class ControlFlowFixture {

  @Param({"STRUCTURED", "IRREDUCIBLE", "LADDER", "CHAIN", "FAN_BEHIND_CHAIN", "SMALL_METHODS"})
  public ControlFlowShape shape;

  @Param({"10000", "100000"})
  public int nodeCount;

  public BiAdjacentGraph[] graphs;
  public Dominators[] dominators;
  public Ancestry[] dominatorAncestries;

  /** Per graph, the definition blocks of its simulated variables, about one per 50 nodes. */
  public IntList[][] definitions;

  @Setup(Level.Trial)
  public void setup() {
    Graph[] generated = shape.generateAll(nodeCount, 42);
    graphs = new BiAdjacentGraph[generated.length];
    for (int g = 0; g < generated.length; g++) graphs[g] = generated[g].biAdjacentGraph();
    dominators = new Dominators[graphs.length];
    dominatorAncestries = new Ancestry[graphs.length];
    definitions = new IntList[graphs.length][];
    var rnd = new Random(7);
    for (int g = 0; g < graphs.length; g++) {
      int n = graphs[g].nodeCount();
      dominators[g] = Dominators.of(graphs[g], 0);
      dominatorAncestries[g] = Ancestry.of(dominators[g]);
      int variables = Math.min(200, Math.max(1, n / 50));
      definitions[g] = new IntList[variables];
      for (int v = 0; v < variables; v++) {
        var blocks = new IntArrayList();
        for (int i = 0, c = 1 + rnd.nextInt(4); i < c; i++) blocks.add(rnd.nextInt(n));
        definitions[g][v] = blocks;
      }
    }
  }
}
