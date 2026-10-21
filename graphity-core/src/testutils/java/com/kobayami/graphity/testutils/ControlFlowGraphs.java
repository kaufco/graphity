package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Random;

/**
 * Generators for graphs shaped like control flow, for the fuzz tests of the control-flow
 * analyses and for their benchmarks. In every graph, node 0 is the entry and every node is
 * reachable from it.
 * <p>
 * The shapes cover what the analyses are built for and where they are known to be expensive:
 * structured code with nested loops, the same with jumps that make it irreducible, ladders on
 * which dominance frontiers grow quadratically, deep straight-line chains, and the fan behind a
 * chain on which the iterative dominator algorithms and plain SEMI-NCA turn quadratic.
 */
public final class ControlFlowGraphs {

  private ControlFlowGraphs() {}

  /** A generated graph with its recipe, for reproducible failure messages. */
  public record LabeledCfg(String generator, int nodeCount, long seed, Graph graph) {
    public String describe() {
      return "gen=%s, n=%d, seed=%d".formatted(generator, nodeCount, seed);
    }
  }

  /**
   * Structured control flow, reducible throughout: starting from the edge {@code 0 -> 1}
   * (entry to exit), a random edge is replaced by a sequence, an if-then, an if-then-else, a
   * while loop or a do-while loop, until the graph has at least {@code targetNodes} nodes. Loop
   * bodies are edges themselves, so later replacements nest constructs inside loops.
   */
  public static Graph structured(int targetNodes, long seed) {
    var rnd = new Random(seed);
    var sources = new IntArrayList();
    var targets = new IntArrayList();
    sources.add(0);
    targets.add(1);
    int n = 2;
    while (n < targetNodes) {
      int e = rnd.nextInt(sources.size());
      int a = sources.getInt(e);
      int b = targets.getInt(e);
      int last = sources.size() - 1;
      sources.set(e, sources.getInt(last));
      targets.set(e, targets.getInt(last));
      sources.removeInt(last);
      targets.removeInt(last);
      switch (rnd.nextInt(5)) {
        case 0 -> { // sequence
          int x = n++;
          add(sources, targets, a, x, x, b);
        }
        case 1 -> { // if-then
          int cond = n++;
          int then = n++;
          add(sources, targets, a, cond, cond, then, cond, b, then, b);
        }
        case 2 -> { // if-then-else
          int cond = n++;
          int then = n++;
          int otherwise = n++;
          add(sources, targets, a, cond, cond, then, cond, otherwise, then, b, otherwise, b);
        }
        case 3 -> { // while: header tests, body returns to the header
          int header = n++;
          int body = n++;
          add(sources, targets, a, header, header, body, body, header, header, b);
        }
        default -> { // do-while: body first, then the test
          int body = n++;
          int cond = n++;
          add(sources, targets, a, body, body, cond, cond, body, cond, b);
        }
      }
    }
    return build(n, sources, targets);
  }

  /**
   * {@link #structured} with {@code jumps} random extra edges between non-entry nodes, the way
   * {@code goto}, generated code or decompiled bytecode cross structure. Most such edges enter
   * a loop somewhere other than its header and make the graph irreducible.
   */
  public static Graph irreducible(int targetNodes, int jumps, long seed) {
    var base = structured(targetNodes, seed);
    int n = base.nodeCount();
    var rnd = new Random(seed ^ 0x9E3779B97F4A7C15L);
    var b = copyOf(base);
    for (int i = 0; i < jumps; i++) {
      b.addEdge(1 + rnd.nextInt(n - 1), 1 + rnd.nextInt(n - 1));
    }
    return b.build();
  }

  /**
   * A ladder of {@code rungs} branches: a left rail {@code a_0 -> a_1 -> ...}, a right rail
   * {@code b_0 -> b_1 -> ...}, and a rung {@code a_i -> b_i} at every step. Every {@code b_i}
   * is in the dominance frontier of every {@code a_j} with {@code j <= i}, so the frontier
   * relation has about {@code rungs^2 / 2} pairs. Node 0 is {@code a_0}; both rails end in a
   * common exit.
   */
  public static Graph ladder(int rungs) {
    // a_i = i, b_i = rungs + i, exit = 2 * rungs.
    int exit = 2 * rungs;
    var b = new GraphBuilder();
    b.addNodes(2 * rungs + 1);
    for (int i = 0; i < rungs; i++) {
      b.addEdge(i, rungs + i);
      if (i + 1 < rungs) {
        b.addEdge(i, i + 1);
        b.addEdge(rungs + i, rungs + i + 1);
      }
    }
    if (rungs > 0) {
      b.addEdge(rungs - 1, exit);
      b.addEdge(2 * rungs - 1, exit);
    }
    return b.build();
  }

  /** A straight-line chain {@code 0 -> 1 -> ... -> n-1}: the deepest possible dominator tree. */
  public static Graph chain(int n) {
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i + 1 < n; i++) b.addEdge(i, i + 1);
    return b.build();
  }

  /**
   * A fan behind a chain: {@code 0 -> 1 -> ... -> chainLength}, then an edge from the end of the
   * chain and one directly from the entry to each of {@code fanWidth} further nodes. The
   * depth-first search reaches every fan node from the deep end of the chain, but its immediate
   * dominator is the entry, so an algorithm that walks up the dominator tree from the search
   * parent takes {@code chainLength} steps per fan node. Plain SEMI-NCA and the iterative
   * Cooper-Harvey-Kennedy algorithm are quadratic on this family.
   */
  public static Graph fanBehindChain(int chainLength, int fanWidth) {
    var b = new GraphBuilder();
    b.addNodes(1 + chainLength + fanWidth);
    for (int i = 0; i < chainLength; i++) b.addEdge(i, i + 1);
    for (int f = 0; f < fanWidth; f++) {
      int fan = 1 + chainLength + f;
      b.addEdge(chainLength, fan);
      b.addEdge(0, fan);
    }
    return b.build();
  }

  /**
   * Picks a control-flow graph of a random shape and size for fuzzing: mostly structured and
   * irreducible ones, with ladders, chains and fans mixed in. Sizes stay small enough for the
   * quadratic oracles.
   */
  public static LabeledCfg anyControlFlowGraph(long seed) {
    var rnd = new Random(seed);
    int n = 4 + rnd.nextInt(200);
    long innerSeed = rnd.nextLong();
    int choice = rnd.nextInt(8);
    return switch (choice) {
      case 0, 1, 2 -> new LabeledCfg("structured", n, seed, structured(n, innerSeed));
      case 3, 4 -> {
        int jumps = 1 + rnd.nextInt(Math.max(1, n / 10));
        yield new LabeledCfg(
            "irreducible(jumps=%d)".formatted(jumps), n, seed, irreducible(n, jumps, innerSeed));
      }
      case 5 -> new LabeledCfg("ladder", n, seed, ladder(n / 2));
      case 6 -> new LabeledCfg("chain", n, seed, chain(n));
      case 7 -> new LabeledCfg("fanBehindChain", n, seed, fanBehindChain(n / 2, n - n / 2 - 1));
      default -> throw new IllegalStateException();
    };
  }

  private static void add(IntArrayList sources, IntArrayList targets, int... pairs) {
    for (int i = 0; i < pairs.length; i += 2) {
      sources.add(pairs[i]);
      targets.add(pairs[i + 1]);
    }
  }

  private static Graph build(int n, IntArrayList sources, IntArrayList targets) {
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i < sources.size(); i++) b.addEdge(sources.getInt(i), targets.getInt(i));
    return b.build();
  }

  private static GraphBuilder copyOf(Graph graph) {
    var b = new GraphBuilder();
    b.addNodes(graph.nodeCount());
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) b.addEdge(u, outs.getInt(i));
    }
    return b;
  }
}
