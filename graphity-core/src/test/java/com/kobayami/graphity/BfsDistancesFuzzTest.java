package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link BfsDistances} and {@link Graphs#reachableFrom} against a textbook
 * breadth-first search over {@link Graph#outNodes(int)} on random graphs with up to three
 * random sources, duplicates and the empty list included. A returned path is checked edge by
 * edge, not compared with one particular shortest path.
 */
class BfsDistancesFuzzTest {

  private static final int ITERATIONS = 150;

  @Test
  void distancesPathsAndReachabilityMatchTheOracle() {
    int pathsChecked = 0;
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      Graph g = labeled.graph();
      int n = g.nodeCount();
      var rnd = new Random(seed * 31 + 3);
      var sources = new IntArrayList();
      int count = rnd.nextInt(8) == 0 ? 0 : 1 + rnd.nextInt(3);
      for (int i = 0; i < count; i++) sources.add(rnd.nextInt(n));
      String describe = labeled.describe() + ", sources=" + sources;

      int[] expected = naiveDistances(g, sources);
      var d = BfsDistances.of(g, sources);
      int max = -1;
      var reachable = new IntArrayList();
      for (int v = 0; v < n; v++) {
        assertThat(d.distanceOf(v)).as("distance of %d, %s", v, describe).isEqualTo(expected[v]);
        max = Math.max(max, expected[v]);
        if (expected[v] >= 0) reachable.add(v);
        var path = d.pathTo(v);
        if (expected[v] < 0) {
          assertThat(path.toIntArray()).isEmpty();
          continue;
        }
        assertThat(path.toIntArray()).hasSize(expected[v] + 1);
        assertThat(sources.toIntArray()).contains(path.getInt(0));
        assertThat(path.getInt(path.size() - 1)).isEqualTo(v);
        for (int i = 0; i + 1 < path.size(); i++) {
          assertThat(g.hasOutNode(path.getInt(i), path.getInt(i + 1)))
              .as("path edge %d, %s", i, describe).isTrue();
        }
        if (expected[v] > 1) pathsChecked++;
      }
      assertThat(d.maxDistance()).as("max distance, %s", describe).isEqualTo(max);
      assertThat(Graphs.reachableFrom(g, sources).toIntArray())
          .as("reachable set, %s", describe)
          .containsExactly(reachable.toIntArray());
    }
    assertThat(pathsChecked).as("paths with more than one edge").isGreaterThan(2000);
  }

  private static int[] naiveDistances(Graph g, IntArrayList sources) {
    var dist = new int[g.nodeCount()];
    Arrays.fill(dist, -1);
    var queue = new ArrayDeque<Integer>();
    for (int s : sources) {
      if (dist[s] < 0) {
        dist[s] = 0;
        queue.add(s);
      }
    }
    while (!queue.isEmpty()) {
      int u = queue.poll();
      for (int v : g.outNodes(u)) {
        if (dist[v] < 0) {
          dist[v] = dist[u] + 1;
          queue.add(v);
        }
      }
    }
    return dist;
  }
}
