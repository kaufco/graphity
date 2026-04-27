package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;

class GraphBuilderTest {

  @Test
  void newBuilderBuildsEmptyGraph() {
    var graph = new GraphBuilder().build();
    assertThat(graph.nodeCount()).isZero();
    assertThat(graph.edgeCount()).isZero();
    assertThat(graph.isEmpty()).isTrue();
  }

  @Test
  void addNodeReturnsSequentialIds() {
    var b = new GraphBuilder();
    assertThat(b.addNode()).isZero();
    assertThat(b.addNode()).isEqualTo(1);
    assertThat(b.addNode()).isEqualTo(2);
    assertThat(b.build().nodeCount()).isEqualTo(3);
  }

  @Test
  void addNodesReturnsFirstAddedId() {
    var b = new GraphBuilder();
    b.addNode();
    int firstOfBatch = b.addNodes(3);
    assertThat(firstOfBatch).isEqualTo(1);
    assertThat(b.build().nodeCount()).isEqualTo(4);
  }

  @Test
  void addNodesWithZeroCountIsNoOp() {
    var b = new GraphBuilder();
    b.addNodes(2);
    int first = b.addNodes(0);
    assertThat(first).isEqualTo(2);
    assertThat(b.build().nodeCount()).isEqualTo(2);
  }

  @Test
  void addNodesWithNegativeCountThrows() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addNodes(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addEdgeWithInvalidSourceTriggersAssertion() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdge(2, 0))
        .isInstanceOf(AssertionError.class);
    assertThatThrownBy(() -> b.addEdge(-1, 0))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void addEdgeWithInvalidTargetTriggersAssertion() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdge(0, 5))
        .isInstanceOf(AssertionError.class);
    assertThatThrownBy(() -> b.addEdge(0, -1))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void addEdgesWithInvalidSourceTriggersAssertion() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdges(5, new IntArrayList(new int[] {0})))
        .isInstanceOf(AssertionError.class);
    assertThatThrownBy(() -> b.addEdges(-1, new IntArrayList(new int[] {0})))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void addEdgesWithInvalidTargetTriggersAssertion() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdges(0, new IntArrayList(new int[] {0, 99})))
        .isInstanceOf(AssertionError.class);
    assertThatThrownBy(() -> b.addEdges(0, new IntArrayList(new int[] {0, -1})))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void buildProducesSortedAdjacencyList() {
    var b = new GraphBuilder();
    b.addNodes(4);
    b.addEdge(0, 3);
    b.addEdge(0, 1);
    b.addEdge(0, 2);
    var graph = b.build();
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1, 2, 3);
  }

  @Test
  void buildDeduplicatesParallelEdges() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    b.addEdge(0, 1);
    b.addEdge(0, 1);
    var graph = b.build();
    assertThat(graph.edgeCount()).isEqualTo(1);
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1);
  }

  @Test
  void buildAllowsSelfLoops() {
    var b = new GraphBuilder();
    b.addNodes(1);
    b.addEdge(0, 0);
    var graph = b.build();
    assertThat(graph.edgeCount()).isEqualTo(1);
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(0);
    assertThat(graph.containsSelfLoop(0)).isTrue();
  }

  @Test
  void buildTrimsAdjacencyArrayAfterDedup() {
    var b = new GraphBuilder();
    b.addNodes(3);
    for (int i = 0; i < 100; i++) {
      b.addEdge(0, 1);
    }
    b.addEdge(0, 2);
    var graph = b.build();
    assertThat(graph.edgeCount()).isEqualTo(2);
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1, 2);
  }

  @Test
  void buildWithIsolatedNodesProducesNoEdges() {
    var graph = new GraphBuilder();
    graph.addNodes(5);
    var built = graph.build();
    assertThat(built.nodeCount()).isEqualTo(5);
    assertThat(built.edgeCount()).isZero();
    for (int i = 0; i < 5; i++) {
      assertThat(built.outNodes(i).toIntArray()).isEmpty();
    }
  }

  @Test
  void builderIsNotConsumedByBuild() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    var first = b.build();
    b.addNode();
    b.addEdge(1, 2);
    var second = b.build();
    assertThat(first.nodeCount()).isEqualTo(2);
    assertThat(first.edgeCount()).isEqualTo(1);
    assertThat(second.nodeCount()).isEqualTo(3);
    assertThat(second.edgeCount()).isEqualTo(2);
  }

  @Test
  void multipleBuildsAreIndependent() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    var first = b.build();
    var second = b.build();
    assertThat(first).isNotSameAs(second);
    assertThat(first).isEqualTo(second);
  }

  @Test
  void addEdgesAppendsAllTargets() {
    var b = new GraphBuilder();
    b.addNodes(4);
    b.addEdges(0, new IntArrayList(new int[] {3, 1, 2}));
    var graph = b.build();
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1, 2, 3);
  }

  @Test
  void addEdgesAcceptsSortedIntView() {
    var source = TestGraphs.path(4);
    var b = new GraphBuilder();
    b.addNodes(4);
    for (int n = 0; n < source.nodeCount(); n++) {
      b.addEdges(n, source.outNodes(n));
    }
    var copy = b.build();
    assertThat(copy).isEqualTo(source);
  }

  @Test
  void addGraphSingleImportPreservesFullTopology() {
    // Diamond with a self-loop: 0->0, 0->1, 0->2, 1->3, 2->3.
    var original = TestGraphs.of(4, 0, 0, 0, 1, 0, 2, 1, 3, 2, 3);
    var b = new GraphBuilder();
    int offset = b.addGraph(original);
    assertThat(offset).isZero();
    assertThat(b.build()).isEqualTo(original);
  }

  @Test
  void addGraphTwoImportsPreserveTopologyAtOffset() {
    // g1: diamond 0->1, 0->2, 1->3, 2->3 (4 nodes).
    var g1 = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    // g2: triangle 0->1, 0->2, 1->2 (3 nodes).
    var g2 = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    var b = new GraphBuilder();
    int off1 = b.addGraph(g1);
    int off2 = b.addGraph(g2);
    assertThat(off1).isZero();
    assertThat(off2).isEqualTo(4);
    // Expected merge: g1 edges unchanged + g2 edges shifted by 4.
    var expected = TestGraphs.of(7,
        0, 1, 0, 2, 1, 3, 2, 3,
        4, 5, 4, 6, 5, 6);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphAllowsBridgingEdgesBetweenImports() {
    var g1 = TestGraphs.of(3, 0, 1, 1, 2);
    var g2 = TestGraphs.of(3, 0, 1, 1, 2);
    var b = new GraphBuilder();
    int off1 = b.addGraph(g1);
    int off2 = b.addGraph(g2);
    b.addEdge(off1 + 2, off2 + 0);
    b.addEdge(off1 + 0, off2 + 2);
    var expected = TestGraphs.of(6,
        0, 1, 1, 2, 2, 3,
        3, 4, 4, 5,
        0, 5);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphWorksAfterManualEdges() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    int offset = b.addGraph(TestGraphs.of(3, 0, 1, 1, 2));
    assertThat(offset).isEqualTo(2);
    var expected = TestGraphs.of(5,
        0, 1,
        2, 3, 3, 4);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphWorksBeforeManualEdges() {
    var b = new GraphBuilder();
    int offset = b.addGraph(TestGraphs.of(3, 0, 1, 1, 2));
    assertThat(offset).isZero();
    b.addNode();
    b.addEdge(0, 3);
    b.addEdge(2, 3);
    var expected = TestGraphs.of(4,
        0, 1, 0, 3,
        1, 2,
        2, 3);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphDeduplicatesAgainstExistingEdges() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    b.addGraph(TestGraphs.of(2, 0, 1));
    var expected = TestGraphs.of(4,
        0, 1,
        2, 3);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphOnEmptyGraphIsNoOp() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    int offset = b.addGraph(TestGraphs.empty());
    assertThat(offset).isEqualTo(2);
    assertThat(b.build()).isEqualTo(TestGraphs.of(2, 0, 1));
  }
}
