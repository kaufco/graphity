package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import java.util.Random;

/**
 * Seed-based random graph generators for fuzzing / cross-validation tests.
 * <p>
 * Every generator takes a {@code long seed} and is fully deterministic given the seed.
 * All generators allow self-loops (the library supports them, so algorithms must too).
 * Parallel edges may be produced but are deduplicated by {@link GraphBuilder}.
 * <p>
 * Intended size range for fuzzing: ~100 to ~10_000 nodes. Smaller graphs are already
 * covered by hand-crafted {@link TestGraphs}; larger graphs belong in benchmarks.
 */
public final class RandomGraphs {

  private RandomGraphs() {}

  /** A generator invocation tagged with its source name, for reproducible failure messages. */
  public record LabeledGraph(String generator, int nodeCount, long seed, Graph graph) {
    public String describe() {
      return "gen=%s, n=%d, seed=%d".formatted(generator, nodeCount, seed);
    }
  }

  // ---- Basic generators (seed + explicit parameters) ----

  /**
   * Erdős–Rényi G(n, p): each of the {@code n * n} directed node pairs is added with
   * probability {@code edgeProb}. May include self-loops and back-and-forth edges.
   */
  public static Graph gnp(int n, double edgeProb, long seed) {
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int u = 0; u < n; u++) {
      for (int v = 0; v < n; v++) {
        if (rnd.nextDouble() < edgeProb) b.addEdge(u, v);
      }
    }
    return b.build();
  }

  /**
   * Erdős–Rényi G(n, m): {@code n} nodes, exactly {@code m} random directed edges
   * (with replacement — parallel edges possible, deduplicated on build). May include
   * self-loops.
   */
  public static Graph gnm(int n, int m, long seed) {
    if (n == 0) return new GraphBuilder().build();
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i < m; i++) {
      b.addEdge(rnd.nextInt(n), rnd.nextInt(n));
    }
    return b.build();
  }

  /**
   * Random out-tree rooted at node 0: for each node {@code i > 0}, pick a random parent
   * in {@code [0, i)}. Produces a connected tree with {@code n - 1} edges, no cycles,
   * no self-loops.
   */
  public static Graph randomTree(int n, long seed) {
    if (n == 0) return new GraphBuilder().build();
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 1; i < n; i++) {
      b.addEdge(rnd.nextInt(i), i);
    }
    return b.build();
  }

  /**
   * Random DAG: edges only go from lower node indices to higher ones.
   * Each of the {@code n*(n-1)/2} candidate forward edges is added with probability
   * {@code edgeProb}. No cycles, no self-loops.
   */
  public static Graph randomDag(int n, double edgeProb, long seed) {
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int u = 0; u < n; u++) {
      for (int v = u + 1; v < n; v++) {
        if (rnd.nextDouble() < edgeProb) b.addEdge(u, v);
      }
    }
    return b.build();
  }

  // ---- Randomized selection (size, density, generator) ----

  /** Node count uniformly in [10, 500]. */
  private static int pickNodeCount(Random rnd) {
    return 10 + rnd.nextInt(500 - 10 + 1);
  }

  /**
   * Edge density picked from a distribution that favors sparse graphs but still
   * exercises moderately dense ones. Returns a probability in (0, 0.3].
   * Very dense (≥30%) on 10k nodes = 100M edges and is stress-test territory.
   */
  private static double pickEdgeProb(Random rnd) {
    double[] choices = {0.001, 0.002, 0.005, 0.01, 0.05, 0.1};
    return choices[rnd.nextInt(choices.length)];
  }

  /**
   * Picks a random graph from any of the four base generators with randomized size
   * and density. Returns a labeled bundle so failing tests can print the full recipe.
   */
  public static LabeledGraph anyGraph(long seed) {
    var rnd = new Random(seed);
    int n = pickNodeCount(rnd);
    double p = pickEdgeProb(rnd);
    long innerSeed = rnd.nextLong();
    int choice = rnd.nextInt(4);
    return switch (choice) {
      case 0 -> new LabeledGraph("gnp(p=%.3f)".formatted(p), n, seed, gnp(n, p, innerSeed));
      case 1 -> {
        int m = (int) Math.min((long) Math.round(p * (double) n * n), Integer.MAX_VALUE);
        yield new LabeledGraph("gnm(m=%d)".formatted(m), n, seed, gnm(n, m, innerSeed));
      }
      case 2 -> new LabeledGraph("randomTree", n, seed, randomTree(n, innerSeed));
      case 3 -> new LabeledGraph("randomDag(p=%.3f)".formatted(p), n, seed, randomDag(n, p, innerSeed));
      default -> throw new IllegalStateException();
    };
  }

  /**
   * Picks a random DAG from {@code randomTree} or {@code randomDag}. Use for algorithms
   * that require acyclic input ({@link com.kobayami.graphity.TopOrder}, etc.).
   */
  public static LabeledGraph anyDag(long seed) {
    var rnd = new Random(seed);
    int n = pickNodeCount(rnd);
    double p = pickEdgeProb(rnd);
    long innerSeed = rnd.nextLong();
    int choice = rnd.nextInt(2);
    return switch (choice) {
      case 0 -> new LabeledGraph("randomTree", n, seed, randomTree(n, innerSeed));
      case 1 -> new LabeledGraph("randomDag(p=%.3f)".formatted(p), n, seed, randomDag(n, p, innerSeed));
      default -> throw new IllegalStateException();
    };
  }
}
