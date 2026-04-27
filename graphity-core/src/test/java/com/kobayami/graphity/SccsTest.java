package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers Sccs-specific methods ({@link Sccs#containsCycle(int)} and
 * {@link Sccs#isStronglyConnected()}). Inherited partition behavior
 * (groupCount / get / nodesOf) is covered in {@link NodePartitionTest} and
 * {@link ComponentsTest}.
 */
class SccsTest {

  @Test
  void containsCycleTrueForMultiNodeScc() {
    // Single cycle {0, 1, 2}.
    var sccs = Components.sccsOf(TestGraphs.cycle(3));
    assertThat(sccs.groupCount()).isEqualTo(1);
    assertThat(sccs.containsCycle(0)).isTrue();
  }

  @Test
  void containsCycleTrueForSingletonWithSelfLoop() {
    var sccs = Components.sccsOf(TestGraphs.of(1, 0, 0));
    assertThat(sccs.containsCycle(0)).isTrue();
  }

  @Test
  void containsCycleFalseForSingletonWithoutSelfLoop() {
    // 0 -> 1: both are singleton SCCs, no cycles.
    var sccs = Components.sccsOf(TestGraphs.of(2, 0, 1));
    assertThat(sccs.containsCycle(sccs.get(0))).isFalse();
    assertThat(sccs.containsCycle(sccs.get(1))).isFalse();
  }

  @Test
  void containsCycleMixedComponents() {
    // 0 -> 0 (self-loop), 1 -> 2 (DAG), 3 -> 4 -> 3 (cycle).
    var g = TestGraphs.of(5, 0, 0, 1, 2, 3, 4, 4, 3);
    var sccs = Components.sccsOf(g);
    assertThat(sccs.containsCycle(sccs.get(0))).isTrue();
    assertThat(sccs.containsCycle(sccs.get(1))).isFalse();
    assertThat(sccs.containsCycle(sccs.get(2))).isFalse();
    assertThat(sccs.containsCycle(sccs.get(3))).isTrue();
    assertThat(sccs.containsCycle(sccs.get(4))).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 5, 1000})
  void containsCycleOutOfRangeThrows(int sccId) {
    var sccs = Components.sccsOf(TestGraphs.path(3));
    assertThatThrownBy(() -> sccs.containsCycle(sccId))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void isStronglyConnectedTrueForEmptyGraph() {
    assertThat(Components.sccsOf(TestGraphs.empty()).isStronglyConnected()).isTrue();
  }

  @Test
  void isStronglyConnectedTrueForSingleNode() {
    var sccs = Components.sccsOf(TestGraphs.isolated(1));
    assertThat(sccs.isStronglyConnected()).isTrue();
  }

  @Test
  void isStronglyConnectedTrueForPureCycle() {
    var sccs = Components.sccsOf(TestGraphs.cycle(4));
    assertThat(sccs.isStronglyConnected()).isTrue();
  }

  @Test
  void isStronglyConnectedFalseForDisconnectedNodes() {
    var sccs = Components.sccsOf(TestGraphs.isolated(3));
    assertThat(sccs.isStronglyConnected()).isFalse();
  }

  @Test
  void isStronglyConnectedFalseForOneWayChain() {
    // 0 -> 1 -> 2: three singleton SCCs.
    var sccs = Components.sccsOf(TestGraphs.path(3));
    assertThat(sccs.isStronglyConnected()).isFalse();
  }
}
