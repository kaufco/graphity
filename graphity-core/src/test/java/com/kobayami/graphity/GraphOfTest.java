package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RandomGraphs;
import org.junit.jupiter.api.Test;

class GraphOfTest {

  @Test
  void buildsTheGraphTheArraysDescribe() {
    Graph graph = Graph.of(new int[] {0, 2, 2, 3}, new int[] {1, 2, 0});

    assertThat(graph.nodeCount()).isEqualTo(3);
    assertThat(graph.edgeCount()).isEqualTo(3);
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1, 2);
    assertThat(graph.outNodes(1).toIntArray()).isEmpty();
    assertThat(graph.outNodes(2).toIntArray()).containsExactly(0);
    assertThat(graph.edgeTarget(2)).isEqualTo(0);
  }

  @Test
  void emptyGraph() {
    Graph graph = Graph.of(new int[] {0}, new int[0]);

    assertThat(graph.nodeCount()).isZero();
    assertThat(graph.edgeCount()).isZero();
  }

  @Test
  void selfLoopsAreAllowed() {
    Graph graph = Graph.of(new int[] {0, 2, 3}, new int[] {0, 1, 1});

    assertThat(graph.outNodes(0).toIntArray()).containsExactly(0, 1);
    assertThat(graph.outNodes(1).toIntArray()).containsExactly(1);
  }

  @Test
  void copiesBothArrays() {
    int[] start = {0, 1, 1};
    int[] targets = {1};
    Graph graph = Graph.of(start, targets);

    start[1] = 0;
    targets[0] = 0;

    assertThat(Csr.outAdjacencyStart(graph)).isNotSameAs(start).containsExactly(0, 1, 1);
    assertThat(Csr.outAdjacency(graph)).isNotSameAs(targets).containsExactly(1);
  }

  @Test
  void roundTripsTheArraysOfAnyGraph() {
    for (long seed = 0; seed < 20; seed++) {
      Graph original = RandomGraphs.gnp(50, 0.08, seed);
      Graph copy = Graph.of(Csr.outAdjacencyStart(original), Csr.outAdjacency(original));

      assertThat(Csr.outAdjacencyStart(copy)).isEqualTo(Csr.outAdjacencyStart(original));
      assertThat(Csr.outAdjacency(copy)).isEqualTo(Csr.outAdjacency(original));
    }
  }

  @Test
  void rejectsEmptyOffsets() {
    assertThatThrownBy(() -> Graph.of(new int[0], new int[0]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }

  @Test
  void rejectsOffsetsNotStartingAtZero() {
    assertThatThrownBy(() -> Graph.of(new int[] {1, 1}, new int[] {0}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("outAdjacencyStart[0]");
  }

  @Test
  void rejectsOffsetsNotEndingAtTheTargetCount() {
    assertThatThrownBy(() -> Graph.of(new int[] {0, 1}, new int[] {0, 0}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("outAdjacency.length");
    assertThatThrownBy(() -> Graph.of(new int[] {0, 2}, new int[] {0}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsDecreasingOffsetsBeforeReadingPastTheTargets() {
    assertThatThrownBy(() -> Graph.of(new int[] {0, 5, 3}, new int[] {0, 1, 2}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("decreases");
  }

  @Test
  void rejectsTargetsOutOfRange() {
    assertThatThrownBy(() -> Graph.of(new int[] {0, 1, 1}, new int[] {2}))
        .isExactlyInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("out-node 2 of node 0");
    assertThatThrownBy(() -> Graph.of(new int[] {0, 1, 1}, new int[] {-1}))
        .isExactlyInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void rejectsUnsortedAdjacency() {
    assertThatThrownBy(() -> Graph.of(new int[] {0, 2, 2}, new int[] {1, 0}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("strictly ascending");
  }

  @Test
  void rejectsDuplicateTargets() {
    assertThatThrownBy(() -> Graph.of(new int[] {0, 2, 2}, new int[] {1, 1}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("strictly ascending");
  }

  @Test
  void sortednessIsCheckedPerListNotAcrossLists() {
    Graph graph = Graph.of(new int[] {0, 1, 2}, new int[] {1, 0});

    assertThat(graph.outNodes(1).toIntArray()).containsExactly(0);
  }
}
