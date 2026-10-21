package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EdgesSortedEdgesTest {

  @Test
  void ordersRandomAscendingAndDescendingInputAlike() {
    var random = new Random(5);
    for (int iteration = 0; iteration < 500; iteration++) {
      int nodeCount = 1 + random.nextInt(20);
      int count = random.nextInt(40);
      long[] keys = new long[count];
      for (int i = 0; i < count; i++) {
        keys[i] = (long) random.nextInt(nodeCount) << 32 | random.nextInt(nodeCount);
      }
      switch (iteration % 3) {
        case 1 -> Arrays.sort(keys);
        case 2 -> {
          Arrays.sort(keys);
          for (int i = 0, j = count - 1; i < j; i++, j--) {
            long swap = keys[i];
            keys[i] = keys[j];
            keys[j] = swap;
          }
        }
        default -> { }
      }
      for (boolean dropDuplicates : new boolean[] {false, true}) {
        assertSorted(keys, nodeCount, dropDuplicates, iteration);
      }
    }
  }

  private static void assertSorted(
      long[] keys, int nodeCount, boolean dropDuplicates, int iteration) {
    int count = keys.length;
    int[] pairs = new int[2 * count];
    for (int i = 0; i < count; i++) {
      pairs[2 * i] = (int) (keys[i] >>> 32);
      pairs[2 * i + 1] = (int) keys[i];
    }
    var builder = new GraphBuilder();
    builder.addNodes(nodeCount);
    builder.addEdgePairs(pairs);
    Graph graph = builder.build();
    long[] expected = keys.clone();
    Arrays.sort(expected);
    if (dropDuplicates) expected = Arrays.stream(expected).distinct().toArray();
    // Half the iterations pass the edge indices, half leave them to be looked up.
    int[] triples = new int[3 * count];
    for (int i = 0; i < count; i++) {
      triples[3 * i] = pairs[2 * i];
      triples[3 * i + 1] = pairs[2 * i + 1];
      triples[3 * i + 2] =
          iteration % 2 == 0 ? graph.edgeIndexOf(pairs[2 * i], pairs[2 * i + 1]) : -1;
    }
    var edges = Edges.sortedEdges(graph, triples, count, dropDuplicates);
    long[] actual = new long[edges.count()];
    for (int p = 0; p < actual.length; p++) {
      int source = edges.sourceNodes().getInt(p);
      int target = edges.targetNodes().getInt(p);
      actual[p] = (long) source << 32 | target;
      assertThat(edges.edgeIndices().getInt(p)).isEqualTo(graph.edgeIndexOf(source, target));
    }
    assertThat(actual).as("iteration %d, dropDuplicates %s", iteration, dropDuplicates)
        .containsExactly(expected);
  }
}
