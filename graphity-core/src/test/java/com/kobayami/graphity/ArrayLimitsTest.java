package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * The limits derived from the largest array, against the thresholds the Javadoc states. A graph
 * at a limit takes gigabytes, so the checks themselves are exercised on small numbers.
 */
class ArrayLimitsTest {

  @Test
  void theLargestArrayIsTheJdkSoftMaximum() {
    assertThat(ArrayLimits.MAX_ARRAY_LENGTH).isEqualTo(Integer.MAX_VALUE - 8);
  }

  @Test
  void aGraphHoldsOneNodeFewerThanTheLargestArray() {
    // GraphBuilder, NodePartition.of and the header of Graph.readFrom: nodeCount + 1 offsets.
    int limit = ArrayLimits.MAX_NODE_COUNT;
    assertThat(limit).isEqualTo(Integer.MAX_VALUE - 9);
    assertThat(limit + 1).isEqualTo(ArrayLimits.MAX_ARRAY_LENGTH);
  }

  @Test
  void twoIntsPerNodeAllowTwoToTheThirtyMinusFiveNodes() {
    // sccsOf, isBipartite and the depth of a depth-first path.
    int limit = ArrayLimits.MAX_NODE_COUNT_TWO_INTS;
    assertThat(limit).isEqualTo((1 << 30) - 5);
    assertThat(2L * limit).isLessThanOrEqualTo(ArrayLimits.MAX_ARRAY_LENGTH);
    assertThat(2L * (limit + 1)).isGreaterThan(ArrayLimits.MAX_ARRAY_LENGTH);
  }

  @Test
  void theControlFlowAnalysesAllowOneNodeLessForTheVirtualRoot() {
    // Dominators, Loops and strongConnectivityOf.
    int limit = DfsPreorder.MAX_NODE_COUNT;
    assertThat(limit).isEqualTo((1 << 30) - 6);
    assertThat(2L * (limit + 1)).isLessThanOrEqualTo(ArrayLimits.MAX_ARRAY_LENGTH);
    assertThat(2L * (limit + 2)).isGreaterThan(ArrayLimits.MAX_ARRAY_LENGTH);
  }

  @Test
  void checkNodeCountRejectsOnlyCountsAboveTheLimit() {
    assertThatCode(() -> ArrayLimits.checkNodeCount(10, 10, "Some analysis"))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> ArrayLimits.checkNodeCount(11, 10, "Some analysis"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Some analysis keeps two ints per node in one array, counting a virtual"
            + " root if it adds one, and so requires nodeCount <= 10, but was 11");
  }
}
