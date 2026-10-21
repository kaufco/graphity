package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.TestGraphs;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CsrTest {

  private static final Graph GRAPH = TestGraphs.of(4, 0, 1, 0, 2, 1, 2, 2, 0, 3, 3);

  @Test
  void outArraysMatchTheEdgeIndexMethods() {
    int[] start = Csr.outAdjacencyStart(GRAPH);
    int[] targets = Csr.outAdjacency(GRAPH);

    assertThat(start).hasSize(GRAPH.nodeCount() + 1);
    assertThat(targets).hasSize(GRAPH.edgeCount());
    for (int v = 0; v < GRAPH.nodeCount(); v++) {
      assertThat(start[v]).isEqualTo(GRAPH.outEdgeStart(v));
      assertThat(start[v + 1]).isEqualTo(GRAPH.outEdgeEnd(v));
    }
    for (int e = 0; e < GRAPH.edgeCount(); e++) {
      assertThat(targets[e]).isEqualTo(GRAPH.edgeTarget(e));
    }
  }

  @Test
  void inArraysMatchTheInNodes() {
    BiAdjacentGraph graph = BiAdjacentGraph.of(GRAPH);
    int[] start = Csr.inAdjacencyStart(graph);
    int[] sources = Csr.inAdjacency(graph);

    assertThat(start).hasSize(graph.nodeCount() + 1);
    assertThat(sources).hasSize(graph.edgeCount());
    for (int v = 0; v < graph.nodeCount(); v++) {
      int[] expected = graph.inNodes(v).toIntArray();
      int[] actual = Arrays.copyOfRange(sources, start[v], start[v + 1]);
      assertThat(actual).containsExactly(expected);
    }
  }

  @Test
  void returnsTheGraphsOwnArraysWithoutCopying() {
    int[] outStart = {0, 1, 1, 2};
    int[] out = {2, 0};
    int[] inStart = {0, 1, 1, 2};
    int[] in = {2, 0};
    BiAdjacentGraph graph = new BiAdjacentGraph(outStart, out, inStart, in);

    assertThat(Csr.outAdjacencyStart(graph)).isSameAs(outStart);
    assertThat(Csr.outAdjacency(graph)).isSameAs(out);
    assertThat(Csr.inAdjacencyStart(graph)).isSameAs(inStart);
    assertThat(Csr.inAdjacency(graph)).isSameAs(in);
  }

  @Test
  void nodeWithoutEdgesInTheMiddleHasAnEmptyRange() {
    BiAdjacentGraph graph = BiAdjacentGraph.of(TestGraphs.of(3, 0, 2, 2, 0));

    assertThat(Csr.outAdjacencyStart(graph)).containsExactly(0, 1, 1, 2);
    assertThat(Csr.outAdjacency(graph)).containsExactly(2, 0);
    assertThat(Csr.inAdjacencyStart(graph)).containsExactly(0, 1, 1, 2);
    assertThat(Csr.inAdjacency(graph)).containsExactly(2, 0);
  }

  @Test
  void transposedViewSwapsTheArrays() {
    BiAdjacentGraph graph = BiAdjacentGraph.of(GRAPH);
    BiAdjacentGraph transposed = graph.transposed();

    assertThat(Csr.outAdjacencyStart(transposed)).isSameAs(Csr.inAdjacencyStart(graph));
    assertThat(Csr.outAdjacency(transposed)).isSameAs(Csr.inAdjacency(graph));
    assertThat(Csr.inAdjacencyStart(transposed)).isSameAs(Csr.outAdjacencyStart(graph));
    assertThat(Csr.inAdjacency(transposed)).isSameAs(Csr.outAdjacency(graph));
  }

  @Test
  void emptyGraphHasOneOffset() {
    assertThat(Csr.outAdjacencyStart(TestGraphs.empty())).containsExactly(0);
    assertThat(Csr.outAdjacency(TestGraphs.empty())).isEmpty();
  }
}
