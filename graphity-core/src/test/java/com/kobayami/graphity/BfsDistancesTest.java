package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

class BfsDistancesTest {

  @Test
  void distancesAndPathsFromOneSource() {
    // 0 -> 1 -> 2 -> 3, 0 -> 2; node 4 unreachable.
    var g = TestGraphs.of(5, 0, 1, 1, 2, 2, 3, 0, 2);
    var d = BfsDistances.of(g, 0);
    assertThat(d.distanceOf(0)).isZero();
    assertThat(d.distanceOf(1)).isEqualTo(1);
    assertThat(d.distanceOf(2)).isEqualTo(1);
    assertThat(d.distanceOf(3)).isEqualTo(2);
    assertThat(d.distanceOf(4)).isEqualTo(-1);
    assertThat(d.isReachable(3)).isTrue();
    assertThat(d.isReachable(4)).isFalse();
    assertThat(d.pathTo(3).toIntArray()).containsExactly(0, 2, 3);
    assertThat(d.pathTo(0).toIntArray()).containsExactly(0);
    assertThat(d.pathTo(4).toIntArray()).isEmpty();
    assertThat(d.maxDistance()).isEqualTo(2);
  }

  @Test
  void severalSourcesGiveTheDistanceToTheNearest() {
    // Chain 0 -> 1 -> 2 -> 3 -> 4, sources 0 and 3.
    var g = TestGraphs.path(5);
    var d = BfsDistances.of(g, IntList.of(0, 3, 3));
    assertThat(d.distanceOf(2)).isEqualTo(2);
    assertThat(d.distanceOf(4)).isEqualTo(1);
    assertThat(d.pathTo(4).toIntArray()).containsExactly(3, 4);
    assertThat(d.maxDistance()).isEqualTo(2);
  }

  @Test
  void cyclesAndSelfLoopsAreHarmless() {
    var g = TestGraphs.of(3, 0, 0, 0, 1, 1, 2, 2, 0);
    var d = BfsDistances.of(g, 1);
    assertThat(d.distanceOf(0)).isEqualTo(2);
    assertThat(d.pathTo(0).toIntArray()).containsExactly(1, 2, 0);
  }

  @Test
  void emptySourcesAndEmptyGraph() {
    var d = BfsDistances.of(TestGraphs.of(2, 0, 1), IntList.of());
    assertThat(d.isReachable(0)).isFalse();
    assertThat(d.maxDistance()).isEqualTo(-1);
    assertThat(BfsDistances.of(TestGraphs.empty(), IntList.of()).maxDistance()).isEqualTo(-1);
  }

  @Test
  void pathIsReadOnly() {
    var d = BfsDistances.of(TestGraphs.path(3), 0);
    assertThatThrownBy(() -> d.pathTo(2).set(0, 1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void outOfRangeArgumentsAreRejected() {
    var g = TestGraphs.path(3);
    assertThatThrownBy(() -> BfsDistances.of(g, 3)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> BfsDistances.of(g, IntList.of(0, -1)))
        .isInstanceOf(IndexOutOfBoundsException.class);
    var d = BfsDistances.of(g, 0);
    assertThatThrownBy(() -> d.distanceOf(3)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> d.isReachable(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> d.pathTo(3)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  // --- Graphs.reachableFrom ---

  @Test
  void reachableFromReturnsTheSortedReachableSet() {
    var g = TestGraphs.of(6, 3, 1, 1, 5, 5, 3, 0, 2);
    assertThat(Graphs.reachableFrom(g, 3).toIntArray()).containsExactly(1, 3, 5);
    assertThat(Graphs.reachableFrom(g, IntList.of(4, 0, 4)).toIntArray()).containsExactly(0, 2, 4);
    assertThat(Graphs.reachableFrom(g, IntList.of()).toIntArray()).isEmpty();
    assertThat(Graphs.reachableFrom(TestGraphs.empty(), IntList.of()).toIntArray()).isEmpty();
  }

  @Test
  void reachableFromExtractsASubgraph() {
    // Two functions in one graph: 0 -> 1 -> 2 and 3 -> 4.
    var g = TestGraphs.of(5, 0, 1, 1, 2, 3, 4);
    var function = Graphs.remapped(g, NodeMapping.forSubgraph(Graphs.reachableFrom(g, 3)));
    assertThat(function.nodeCount()).isEqualTo(2);
    assertThat(function.hasOutNode(0, 1)).isTrue();
  }

  @Test
  void reachableFromRejectsOutOfRangeSources() {
    var g = TestGraphs.path(3);
    assertThatThrownBy(() -> Graphs.reachableFrom(g, 3))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> Graphs.reachableFrom(g, IntList.of(-1)))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }
}
