package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.TestGraphs;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EdgesSortedEdgesTest {

  /** Fills the slots past {@code count}, as a growing list leaves them; never a valid node. */
  private static final int JUNK = 1_000_000;

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

  @Test
  void readsOnlyTheFirstCountEntriesAndLeavesTheArraysUntouched() {
    // 0 -> 1 (edge 0), 0 -> 2 (1), 1 -> 2 (2), 2 -> 0 (3), 3 -> 1 (4).
    var graph = TestGraphs.of(4, 0, 1, 0, 2, 1, 2, 2, 0, 3, 1);
    int[][] mixed = {{2, 0, 3, 0, 1}, {0, 2, 1, 1, 2}, {3, -1, 4, 0, -1}};
    int[][] ascending = {{0, 0, 1, 2, 3}, {1, 2, 2, 0, 1}, {-1, 1, 2, -1, 4}};
    int[][] descending = {{3, 2, 1, 0, 0}, {1, 0, 2, 2, 1}, {4, 3, -1, 1, 0}};
    for (int[][] input : new int[][][] {mixed, ascending, descending}) {
      int[] sources = withJunkTail(input[0]);
      int[] targets = withJunkTail(input[1]);
      int[] edgeIndices = withJunkTail(input[2]);
      int[] sourcesBefore = sources.clone();
      int[] targetsBefore = targets.clone();
      int[] edgeIndicesBefore = edgeIndices.clone();

      var edges = Edges.sortedEdges(graph, sources, targets, edgeIndices, 5, false);

      assertThat(edges.sourceNodes().toIntArray()).containsExactly(0, 0, 1, 2, 3);
      assertThat(edges.targetNodes().toIntArray()).containsExactly(1, 2, 2, 0, 1);
      assertThat(edges.edgeIndices().toIntArray()).containsExactly(0, 1, 2, 3, 4);
      assertThat(sources).containsExactly(sourcesBefore);
      assertThat(targets).containsExactly(targetsBefore);
      assertThat(edgeIndices).containsExactly(edgeIndicesBefore);
    }
  }

  @Test
  void dropsDuplicatesFromParallelArrays() {
    // 0 -> 1 (edge 0), 1 -> 0 (1), 1 -> 2 (2).
    var graph = TestGraphs.of(3, 0, 1, 1, 0, 1, 2);
    int[] sources = withJunkTail(new int[] {1, 0, 1, 1, 0});
    int[] targets = withJunkTail(new int[] {2, 1, 0, 2, 1});
    int[] edgeIndices = withJunkTail(new int[] {-1, 0, 1, 2, -1});

    var edges = Edges.sortedEdges(graph, sources, targets, edgeIndices, 5, true);

    assertThat(edges.sourceNodes().toIntArray()).containsExactly(0, 1, 1);
    assertThat(edges.targetNodes().toIntArray()).containsExactly(1, 0, 2);
    assertThat(edges.edgeIndices().toIntArray()).containsExactly(0, 1, 2);
  }

  private static int[] withJunkTail(int[] values) {
    int[] array = Arrays.copyOf(values, values.length + 3);
    Arrays.fill(array, values.length, array.length, JUNK);
    return array;
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
    int[] sources = new int[count];
    int[] targets = new int[count];
    int[] edgeIndices = new int[count];
    for (int i = 0; i < count; i++) {
      sources[i] = pairs[2 * i];
      targets[i] = pairs[2 * i + 1];
      edgeIndices[i] = iteration % 2 == 0 ? graph.edgeIndexOf(sources[i], targets[i]) : -1;
    }
    var edges = Edges.sortedEdges(graph, sources, targets, edgeIndices, count, dropDuplicates);
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
