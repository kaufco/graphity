package com.kobayami.graphity.benchmarks.graphity;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
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

import java.util.concurrent.TimeUnit;

/**
 * Measures {@link GraphBuilder#build()} throughput given a fixed edge list.
 *
 * <p>Edge arrays are prepared in {@link #setup()} so the measurement captures only
 * the builder's work: {@code addNodes}, repeated {@code addEdge}, and the sort/dedup
 * pass in {@code build()}. Random graph generation is excluded.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class BuildBenchmark {

  @Param({"PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED", "CHAIN_OF_CLIQUES_SHUFFLED"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  private int[] sources;
  private int[] targets;

  @Setup(Level.Trial)
  public void setup() {
    Graph template = BenchmarkGraphs.generate(shape, nodeCount);
    int edgeCount = template.edgeCount();
    sources = new int[edgeCount];
    targets = new int[edgeCount];
    int idx = 0;
    for (int v = 0; v < template.nodeCount(); v++) {
      SortedIntView outs = template.outNodes(v);
      int deg = outs.size();
      for (int i = 0; i < deg; i++) {
        sources[idx] = v;
        targets[idx] = outs.getInt(i);
        idx++;
      }
    }
  }

  @Benchmark
  public Graph build() {
    GraphBuilder b = new GraphBuilder();
    b.addNodes(nodeCount);
    int ec = sources.length;
    for (int i = 0; i < ec; i++) {
      b.addEdge(sources[i], targets[i]);
    }
    return b.build();
  }
}
