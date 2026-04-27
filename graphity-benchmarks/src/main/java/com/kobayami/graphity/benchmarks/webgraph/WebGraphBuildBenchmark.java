package com.kobayami.graphity.benchmarks.webgraph;

import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * WebGraph counterpart to {@link com.kobayami.graphity.benchmarks.graphity.BuildBenchmark}.
 *
 * <p>BVGraph construction is inherently a 3-stage pipeline:
 * {@code ArrayListMutableGraph.addArc} → {@code BVGraph.store} (encoder) →
 * {@code BVGraph.load} (in-memory compressed representation). All three stages are
 * in the measurement because the result — a loaded BVGraph — is the only form in
 * which WebGraph lets you actually run queries or algorithms. The fair comparison
 * is therefore <em>"how much wall time to produce WebGraph's compressed in-memory
 * graph from a list of edges"</em>, vs Graphity's add-and-done.
 *
 * <p>Intermediate files are written to a single per-trial workspace and are
 * overwritten on each invocation; the workspace is deleted in {@link #teardown()}.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WebGraphBuildBenchmark {

  @Param({"PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
      "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED", "CHAIN_OF_CLIQUES_SHUFFLED"})
  public GraphShape shape;

  @Param({"1000", "10000", "100000"})
  public int nodeCount;

  private int[] sources;
  private int[] targets;
  private Path tmpRoot;
  private String basename;

  @Setup(Level.Trial)
  public void setup() throws IOException {
    com.kobayami.graphity.Graph template = BenchmarkGraphs.generate(shape, nodeCount);
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
    tmpRoot = WebGraphGraphs.createTempWorkspace();
    basename = tmpRoot.resolve("build").toString();
  }

  @TearDown(Level.Trial)
  public void teardown() {
    WebGraphGraphs.deleteTree(tmpRoot);
  }

  @Benchmark
  public ImmutableGraph build() throws IOException {
    ArrayListMutableGraph mutable = new ArrayListMutableGraph(nodeCount);
    int ec = sources.length;
    for (int i = 0; i < ec; i++) {
      mutable.addArc(sources[i], targets[i]);
    }
    BVGraph.store(mutable.immutableView(), basename);
    return BVGraph.load(basename);
  }
}
