package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.ControlFlowGraphs;
import com.kobayami.graphity.testutils.NaiveDominators;
import com.kobayami.graphity.testutils.NaiveLoops;
import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link Loops} against {@link NaiveLoops}, which builds the forest literally
 * from its definition by recursive decomposition into strongly connected components - and so
 * also checks that Havlak's algorithm yields that definition.
 */
class LoopsFuzzTest {

  private static final int ITERATIONS = 150;

  @Test
  void forestMatchesTheComponentDefinition() {
    int loopsSeen = 0;
    int irreducibleSeen = 0;
    int nestedSeen = 0;
    for (long seed = 0; seed < ITERATIONS; seed++) {
      Graph graph;
      IntList entries;
      String describe;
      if (seed % 3 != 2) {
        var cfg = ControlFlowGraphs.anyControlFlowGraph(seed);
        graph = cfg.graph();
        entries = IntList.of(0);
        describe = cfg.describe();
      } else {
        var labeled = RandomGraphs.anyGraph(seed);
        graph = labeled.graph();
        var rnd = new Random(seed);
        var list = new IntArrayList();
        for (int i = 0, c = 1 + rnd.nextInt(3); i < c; i++) {
          list.add(rnd.nextInt(graph.nodeCount()));
        }
        entries = list;
        describe = labeled.describe() + ", entries=" + entries;
      }
      int n = graph.nodeCount();
      var oracle = new NaiveLoops(graph, entries);
      var loops = Loops.of(graph.biAdjacentGraph(), entries);

      int expectedCount = 0;
      boolean expectedReducible = true;
      var depth = new int[n];
      for (int h = 0; h < n; h++) {
        var loop = oracle.loopOfHeader[h];
        if (loop == null) continue;
        expectedCount++;
        int id = loops.innermostLoopOf(h);
        assertThat(id).as("loop of header %d, %s", h, describe).isGreaterThanOrEqualTo(0);
        assertThat(loops.headerOf(id)).as("header of loop %d, %s", id, describe).isEqualTo(h);

        var body = new IntArrayList();
        for (int x = 0; x < n; x++) {
          if (loop.body()[x]) {
            body.add(x);
            depth[x]++;
          }
          assertThat(loops.isInLoop(x, id))
              .as("%d in loop of %d, %s", x, h, describe)
              .isEqualTo(loop.body()[x]);
        }
        var nodes = loops.nodesOf(id).toIntArray();
        assertThat(nodes[0]).as("body of %d starts with its header, %s", h, describe).isEqualTo(h);
        Arrays.sort(nodes);
        assertThat(nodes).as("body of %d, %s", h, describe).isEqualTo(body.toIntArray());

        var entryList = oracle.entriesOf(loop);
        assertThat(loops.entriesOf(id).toIntArray())
            .as("entries of %d, %s", h, describe)
            .isEqualTo(entryList.stream().mapToInt(Integer::intValue).toArray());
        if (entryList.size() != 1) expectedReducible = false;

        int parentHeader = loop.parentHeader();
        int expectedParent = parentHeader < 0 ? -1 : loops.innermostLoopOf(parentHeader);
        assertThat(loops.parentOf(id)).as("parent of %d, %s", h, describe)
            .isEqualTo(expectedParent);
        assertThat(loops.parentOf(id)).isLessThan(id);
        if (parentHeader >= 0) nestedSeen++;
      }
      assertThat(loops.loopCount()).as("loop count, %s", describe).isEqualTo(expectedCount);
      assertThat(loops.isReducible()).as("reducible, %s", describe).isEqualTo(expectedReducible);
      for (int x = 0; x < n; x++) {
        assertThat(loops.loopDepthOf(x)).as("depth of %d, %s", x, describe).isEqualTo(depth[x]);
      }
      loopsSeen += expectedCount;
      if (!expectedReducible) irreducibleSeen++;
    }
    assertThat(loopsSeen).as("loops compared").isGreaterThan(1000);
    assertThat(irreducibleSeen).as("irreducible graphs compared").isGreaterThan(20);
    assertThat(nestedSeen).as("nested loops compared").isGreaterThan(300);
  }

  /**
   * Checks the promises of the Javadoc against the classic definitions, with dominance taken from
   * {@link NaiveDominators}: a graph is reducible if it becomes acyclic once its dominance back
   * edges ({@code u -> v} with {@code v} dominating {@code u}) are removed, and then every loop is
   * the natural loop of its header - the header plus every node that reaches the source of one of
   * its back edges without passing through the header.
   */
  @Test
  void reducibilityAndNaturalLoopsMatchTheClassicDefinitions() {
    int naturalLoopsCompared = 0;
    int irreducibleSeen = 0;
    for (long seed = 0; seed < ITERATIONS; seed++) {
      Graph graph;
      IntList entries;
      if (seed % 3 != 2) {
        graph = ControlFlowGraphs.anyControlFlowGraph(seed).graph();
        entries = IntList.of(0);
      } else {
        graph = RandomGraphs.anyGraph(seed).graph();
        var rnd = new Random(seed);
        entries = IntList.of(rnd.nextInt(graph.nodeCount()), rnd.nextInt(graph.nodeCount()));
      }
      int n = graph.nodeCount();
      var dominance = new NaiveDominators(graph, entries);
      var loops = Loops.of(graph.biAdjacentGraph(), entries);

      var forwardOnly = new GraphBuilder();
      forwardOnly.addNodes(n);
      for (int u = 0; u < n; u++) {
        if (!dominance.reachable[u]) continue;
        var outs = graph.outNodes(u);
        for (int i = 0; i < outs.size(); i++) {
          int v = outs.getInt(i);
          if (!dominance.dominates[v][u]) forwardOnly.addEdge(u, v);
        }
      }
      boolean reducible = Graphs.isDag(forwardOnly.build());
      assertThat(loops.isReducible()).as("reducible, seed %d", seed).isEqualTo(reducible);
      if (!reducible) {
        irreducibleSeen++;
        continue;
      }

      // Every target of a dominance back edge heads a loop, and every loop header is one.
      var backEdgeTargets = new TreeSet<Integer>();
      for (int u = 0; u < n; u++) {
        if (!dominance.reachable[u]) continue;
        var outs = graph.outNodes(u);
        for (int i = 0; i < outs.size(); i++) {
          if (dominance.dominates[outs.getInt(i)][u]) backEdgeTargets.add(outs.getInt(i));
        }
      }
      var headers = new TreeSet<Integer>();
      for (int id = 0; id < loops.loopCount(); id++) headers.add(loops.headerOf(id));
      assertThat(headers).as("loop headers, seed %d", seed).isEqualTo(backEdgeTargets);

      for (int id = 0; id < loops.loopCount(); id++) {
        naturalLoopsCompared++;
        int header = loops.headerOf(id);
        var natural = new boolean[n];
        natural[header] = true;
        var queue = new IntArrayList();
        for (int u = 0; u < n; u++) {
          if (dominance.reachable[u] && graph.hasOutNode(u, header)
              && dominance.dominates[header][u] && !natural[u]) {
            natural[u] = true;
            queue.add(u);
          }
        }
        while (!queue.isEmpty()) {
          int x = queue.popInt();
          for (int p = 0; p < n; p++) {
            if (dominance.reachable[p] && !natural[p] && graph.hasOutNode(p, x)) {
              natural[p] = true;
              queue.add(p);
            }
          }
        }
        var expected = new IntArrayList();
        for (int x = 0; x < n; x++) {
          if (natural[x]) expected.add(x);
        }
        var body = loops.nodesOf(id).toIntArray();
        Arrays.sort(body);
        assertThat(body).as("natural loop of %d, seed %d", header, seed)
            .isEqualTo(expected.toIntArray());
      }
    }
    assertThat(naturalLoopsCompared).as("natural loops compared").isGreaterThan(500);
    assertThat(irreducibleSeen).as("irreducible graphs compared").isGreaterThan(20);
  }
}
