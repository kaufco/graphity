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
   * Random forest of out-trees: node 0 is always a root, every other node either starts a new
   * tree (with probability {@code rootProb}) or gets a random parent from {@code [0, i)}.
   * No cycles, no self-loops, every node has in-degree ≤ 1.
   */
  public static Graph randomForest(int n, double rootProb, long seed) {
    if (n == 0) return new GraphBuilder().build();
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 1; i < n; i++) {
      if (rnd.nextDouble() >= rootProb) b.addEdge(rnd.nextInt(i), i);
    }
    return b.build();
  }

  /**
   * Forest of long chains: every node's parent is its predecessor, except where a new tree
   * starts (with probability {@code rootProb}). Produces deep, thin trees - the shape that
   * makes path decomposition and per-query walks differ most.
   */
  public static Graph chainForest(int n, double rootProb, long seed) {
    if (n == 0) return new GraphBuilder().build();
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 1; i < n; i++) {
      if (rnd.nextDouble() >= rootProb) b.addEdge(i - 1, i);
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

  /**
   * Random bipartite graph: every node is assigned to one of two classes at random, and each
   * cross-class pair is joined with probability {@code edgeProb} in a random direction. No
   * self-loops, and no odd cycle can arise, so the result is always bipartite.
   */
  public static Graph randomBipartite(int n, double edgeProb, long seed) {
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    var secondClass = new boolean[n];
    for (int i = 0; i < n; i++) secondClass[i] = rnd.nextBoolean();
    for (int u = 0; u < n; u++) {
      for (int v = u + 1; v < n; v++) {
        if (secondClass[u] != secondClass[v] && rnd.nextDouble() < edgeProb) {
          if (rnd.nextBoolean()) b.addEdge(u, v);
          else b.addEdge(v, u);
        }
      }
    }
    return b.build();
  }

  /**
   * Returns a copy of the given graph with {@code extraEdges} random edges added, which is how a
   * graph is pushed just out of a shape class: one edge added to a tree closes an undirected
   * cycle, one added to a bipartite graph is likely to close an odd one. The added edges may
   * duplicate existing ones, reverse them, or be self-loops, so the cases where nothing changes
   * are covered too.
   */
  public static Graph withExtraEdges(Graph graph, int extraEdges, long seed) {
    int n = graph.nodeCount();
    var rnd = new Random(seed);
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int u = 0; u < n; u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) b.addEdge(u, outs.getInt(i));
    }
    for (int i = 0; i < extraEdges && n > 0; i++) {
      b.addEdge(rnd.nextInt(n), rnd.nextInt(n));
    }
    return b.build();
  }

  /**
   * Returns a copy of the given graph with every edge stored in both directions - the way an
   * undirected graph is usually represented. Self-loops are kept as they are.
   */
  public static Graph symmetrized(Graph graph) {
    var b = new GraphBuilder();
    b.addNodes(graph.nodeCount());
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        b.addEdge(u, v);
        b.addEdge(v, u);
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
   * Picks a random forest of out-trees, mixing bushy and chain-like shapes and single-tree
   * with multi-tree cases. Use for algorithms that require forest input.
   */
  public static LabeledGraph anyForest(long seed) {
    var rnd = new Random(seed);
    int n = pickNodeCount(rnd);
    long innerSeed = rnd.nextLong();
    double rootProb = new double[] {0.0, 0.01, 0.1}[rnd.nextInt(3)];
    int choice = rnd.nextInt(3);
    return switch (choice) {
      case 0 -> new LabeledGraph("randomTree", n, seed, randomTree(n, innerSeed));
      case 1 -> new LabeledGraph(
          "randomForest(rootProb=%.2f)".formatted(rootProb), n, seed,
          randomForest(n, rootProb, innerSeed));
      case 2 -> new LabeledGraph(
          "chainForest(rootProb=%.2f)".formatted(rootProb), n, seed,
          chainForest(n, rootProb, innerSeed));
      default -> throw new IllegalStateException();
    };
  }

  /**
   * Picks a graph from the shapes on which the undirected predicates actually discriminate:
   * bipartite graphs and graphs one edge away from bipartite, trees and forests stored in one or
   * in both directions, and each of those with a few random edges added. {@link #anyGraph} is
   * almost never a tree and almost never bipartite, so comparing against it alone would be
   * "false against false" throughout and would not test much.
   */
  public static LabeledGraph anyUndirectedShape(long seed) {
    var rnd = new Random(seed);
    int n = pickNodeCount(rnd);
    double p = pickEdgeProb(rnd);
    long innerSeed = rnd.nextLong();
    int choice = rnd.nextInt(9);
    return switch (choice) {
      case 0 -> new LabeledGraph(
          "randomBipartite(p=%.3f)".formatted(p), n, seed, randomBipartite(n, p, innerSeed));
      case 1 -> new LabeledGraph(
          "randomBipartite(p=%.3f)+3".formatted(p), n, seed,
          withExtraEdges(randomBipartite(n, p, innerSeed), 3, innerSeed));
      case 2 -> new LabeledGraph("randomTree", n, seed, randomTree(n, innerSeed));
      case 3 -> new LabeledGraph(
          "randomTree+1", n, seed, withExtraEdges(randomTree(n, innerSeed), 1, innerSeed));
      case 4 -> new LabeledGraph(
          "symmetrized(randomTree)", n, seed, symmetrized(randomTree(n, innerSeed)));
      case 5 -> new LabeledGraph(
          "randomForest(rootProb=0.05)", n, seed, randomForest(n, 0.05, innerSeed));
      case 6 -> new LabeledGraph(
          "symmetrized(randomForest(rootProb=0.05))", n, seed,
          symmetrized(randomForest(n, 0.05, innerSeed)));
      case 7 -> new LabeledGraph("gnm(m=n)", n, seed, gnm(n, n, innerSeed));
      // A symmetrically stored graph that does hold an undirected cycle. The symmetrized trees
      // above are all acyclic, so without this case nothing would distinguish "this pair is the
      // second direction of an edge I already counted" from "this pair closes a cycle".
      case 8 -> new LabeledGraph(
          "symmetrized(randomTree+1)", n, seed,
          symmetrized(withExtraEdges(randomTree(n, innerSeed), 1, innerSeed)));
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
