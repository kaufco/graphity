package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EdgesSortedPairsTest {

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
    long[] expected = keys.clone();
    Arrays.sort(expected);
    if (dropDuplicates) expected = Arrays.stream(expected).distinct().toArray();
    var edges = Edges.sortedPairs(pairs, count, nodeCount, dropDuplicates);
    long[] actual = new long[edges.count()];
    for (int i = 0; i < actual.length; i++) {
      actual[i] = (long) edges.sourceNodes().getInt(i) << 32 | edges.targetNodes().getInt(i);
    }
    assertThat(actual).as("iteration %d, dropDuplicates %s", iteration, dropDuplicates)
        .containsExactly(expected);
  }
}
