package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GraphTest {

  @Test
  void emptyGraphReportsZero() {
    var g = TestGraphs.empty();
    assertThat(g.nodeCount()).isZero();
    assertThat(g.edgeCount()).isZero();
    assertThat(g.isEmpty()).isTrue();
  }

  @Test
  void isolatedGraphHasNodesNoEdges() {
    var g = TestGraphs.isolated(5);
    assertThat(g.nodeCount()).isEqualTo(5);
    assertThat(g.edgeCount()).isZero();
    assertThat(g.isEmpty()).isFalse();
  }

  @Test
  void nodeAndEdgeCountsMatchGraphBuilder() {
    // 0->1, 0->2, 1->3, 2->3 (diamond).
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    assertThat(g.nodeCount()).isEqualTo(4);
    assertThat(g.edgeCount()).isEqualTo(4);
  }

  @Test
  void outDegreeReportsOutEdges() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    assertThat(g.outDegree(0)).isEqualTo(2);
    assertThat(g.outDegree(1)).isEqualTo(1);
    assertThat(g.outDegree(2)).isEqualTo(1);
    assertThat(g.outDegree(3)).isZero();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void outDegreeOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(4);
    assertThatThrownBy(() -> g.outDegree(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void outNodesReturnsSortedView() {
    var g = TestGraphs.of(4, 0, 3, 0, 1, 0, 2);
    assertThat(g.outNodes(0).toIntArray()).containsExactly(1, 2, 3);
  }

  @Test
  void outNodesForNodeWithoutOutEdgesIsEmpty() {
    var g = TestGraphs.of(2, 0, 1);
    assertThat(g.outNodes(1).isEmpty()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 1000})
  void outNodesOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(4);
    assertThatThrownBy(() -> g.outNodes(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void outEdgeStartAndEndFrameOutNodes() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    int s = g.outEdgeStart(0);
    int e = g.outEdgeEnd(0);
    assertThat(e - s).isEqualTo(g.outDegree(0));
    int[] targets = new int[e - s];
    for (int i = s; i < e; i++) targets[i - s] = g.edgeTarget(i);
    assertThat(targets).containsExactly(1, 2);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void outEdgeStartOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(3);
    assertThatThrownBy(() -> g.outEdgeStart(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> g.outEdgeEnd(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void edgeTargetReturnsTargetByEdgeIndex() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    assertThat(g.edgeTarget(0)).isEqualTo(1);
    assertThat(g.edgeTarget(1)).isEqualTo(2);
    assertThat(g.edgeTarget(2)).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void edgeTargetOutOfRangeThrows(int edgeIndex) {
    var g = TestGraphs.of(2, 0, 1, 1, 0, 0, 0);
    assertThatThrownBy(() -> g.edgeTarget(edgeIndex))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void edgeSourceInvertsEdgeTargetMapping() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    for (int e = 0; e < g.edgeCount(); e++) {
      int source = g.edgeSource(e);
      int target = g.edgeTarget(e);
      assertThat(g.hasOutNode(source, target)).isTrue();
      assertThat(e).isBetween(g.outEdgeStart(source), g.outEdgeEnd(source) - 1);
    }
  }

  @Test
  void edgeSourceReturnsCorrectSourceAcrossAllEdges() {
    var g = TestGraphs.of(5,
        0, 2, 0, 4,
        2, 3,
        3, 1, 3, 4);
    assertThat(g.edgeSource(0)).isZero();
    assertThat(g.edgeSource(1)).isZero();
    assertThat(g.edgeSource(2)).isEqualTo(2);
    assertThat(g.edgeSource(3)).isEqualTo(3);
    assertThat(g.edgeSource(4)).isEqualTo(3);
  }

  @Test
  void edgeSourceHandlesNodesWithNoOutEdges() {
    // Node 0 has no out-edges; edge 0 belongs to node 1.
    var g = TestGraphs.of(3, 1, 2);
    assertThat(g.edgeSource(0)).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 4, 100})
  void edgeSourceOutOfRangeThrows(int edgeIndex) {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2, 2, 0);
    assertThatThrownBy(() -> g.edgeSource(edgeIndex))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void hasOutNodeDetectsExistingEdges() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    assertThat(g.hasOutNode(0, 1)).isTrue();
    assertThat(g.hasOutNode(0, 2)).isTrue();
    assertThat(g.hasOutNode(1, 2)).isTrue();
  }

  @Test
  void edgeIndexOfFindsEdgesAndReportsMissingOnes() {
    // Edge indices: 0 -> 1 is 0, 0 -> 2 is 1, 1 -> 2 is 2, 2 -> 0 is 3.
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2, 2, 0);
    assertThat(g.edgeIndexOf(0, 1)).isZero();
    assertThat(g.edgeIndexOf(0, 2)).isEqualTo(1);
    assertThat(g.edgeIndexOf(1, 2)).isEqualTo(2);
    assertThat(g.edgeIndexOf(2, 0)).isEqualTo(3);
    assertThat(g.edgeIndexOf(1, 0)).isEqualTo(-1);
    assertThat(g.edgeIndexOf(0, 0)).isEqualTo(-1);
    assertThat(g.edgeIndexOf(0, 999)).isEqualTo(-1);
    assertThat(g.edgeIndexOf(0, -1)).isEqualTo(-1);
    assertThat(BiAdjacentGraph.of(g).edgeIndexOf(2, 0)).isEqualTo(3);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3})
  void edgeIndexOfRejectsSourceOutOfRange(int source) {
    var g = TestGraphs.of(3, 0, 1);
    assertThatThrownBy(() -> g.edgeIndexOf(source, 1))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void hasOutNodeRejectsMissingEdges() {
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    assertThat(g.hasOutNode(0, 0)).isFalse();
    assertThat(g.hasOutNode(1, 0)).isFalse();
    assertThat(g.hasOutNode(2, 0)).isFalse();
    assertThat(g.hasOutNode(0, 999)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 1000})
  void hasOutNodeOutOfRangeSourceThrows(int node) {
    var g = TestGraphs.isolated(3);
    assertThatThrownBy(() -> g.hasOutNode(node, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void containsSelfLoopDetectsSelfLoops() {
    var g = TestGraphs.of(3, 0, 0, 1, 2, 2, 2);
    assertThat(g.containsSelfLoop(0)).isTrue();
    assertThat(g.containsSelfLoop(1)).isFalse();
    assertThat(g.containsSelfLoop(2)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 1000})
  void containsSelfLoopOutOfRangeThrows(int node) {
    var g = TestGraphs.isolated(3);
    assertThatThrownBy(() -> g.containsSelfLoop(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void biAdjacentGraphComputesCorrectInEdges() {
    // Diamond 0->1, 0->2, 1->3, 2->3. In-edges: 1<-{0}, 2<-{0}, 3<-{1,2}.
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var bi = BiAdjacentGraph.of(g);
    assertThat(bi.inNodes(0).toIntArray()).isEmpty();
    assertThat(bi.inNodes(1).toIntArray()).containsExactly(0);
    assertThat(bi.inNodes(2).toIntArray()).containsExactly(0);
    assertThat(bi.inNodes(3).toIntArray()).containsExactly(1, 2);
  }

  @Test
  void biAdjacentGraphPreservesOutAdjacency() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var bi = BiAdjacentGraph.of(g);
    for (int i = 0; i < g.nodeCount(); i++) {
      assertThat(bi.outNodes(i).toIntArray()).containsExactly(g.outNodes(i).toIntArray());
    }
  }

  @Test
  void biAdjacentGraphOnEmptyGraphIsEmpty() {
    var bi = BiAdjacentGraph.of(TestGraphs.empty());
    assertThat(bi.nodeCount()).isZero();
    assertThat(bi.edgeCount()).isZero();
  }

  @Test
  void biAdjacentGraphInAdjacencyIsSorted() {
    // Node 3 receives edges from 0, 2, 1 — must end up sorted as [0, 1, 2].
    var g = TestGraphs.of(4, 0, 3, 1, 3, 2, 3);
    assertThat(BiAdjacentGraph.of(g).inNodes(3).toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void uniAdjacentGraphOnPlainGraphReturnsSelf() {
    var g = TestGraphs.path(3);
    assertThatObject(g.uniAdjacentGraph()).isSameAs(g);
  }

  @Test
  void equalsBasedOnStructure() {
    var a = TestGraphs.of(3, 0, 1, 1, 2);
    var b = TestGraphs.of(3, 0, 1, 1, 2);
    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equalsWithDifferentStructureFalse() {
    var a = TestGraphs.of(3, 0, 1, 1, 2);
    var b = TestGraphs.of(3, 0, 2, 1, 2);
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void equalsWithDifferentNodeCountFalse() {
    assertThat(TestGraphs.isolated(3)).isNotEqualTo(TestGraphs.isolated(4));
  }

  @Test
  void equalsIgnoresBiVsUniAdjacentType() {
    var uni = TestGraphs.of(3, 0, 1, 1, 2);
    var bi = BiAdjacentGraph.of(uni);
    assertThat(uni).isEqualTo(bi);
    assertThat(bi).isEqualTo(uni);
    assertThat(uni.hashCode()).isEqualTo(bi.hashCode());
  }

  @Test
  void equalsWithNullOrOtherTypeFalse() {
    var g = TestGraphs.path(3);
    assertThat(g.equals(null)).isFalse();
    assertThat(g.equals("not a graph")).isFalse();
  }

  @Test
  void equalsSameInstanceReturnsTrue() {
    var g = TestGraphs.path(3);
    assertThat(g.equals(g)).isTrue();
  }

  @Test
  void nodesStreamsEveryNodeInOrder() {
    assertThat(TestGraphs.path(4).nodes().toArray()).containsExactly(0, 1, 2, 3);
    assertThat(TestGraphs.empty().nodes().count()).isZero();
  }

  @Test
  void forEachEdgeVisitsEveryEdgeInEdgeIndexOrder() {
    for (long seed = 0; seed < 20; seed++) {
      Graph graph = RandomGraphs.gnp(30, 0.1, seed);
      var indices = new IntArrayList();
      graph.forEachEdge((source, target, edgeIndex) -> {
        assertThat(graph.edgeSource(edgeIndex)).isEqualTo(source);
        assertThat(graph.edgeTarget(edgeIndex)).isEqualTo(target);
        indices.add(edgeIndex);
      });
      assertThat(indices.toIntArray())
          .containsExactly(IntStream.range(0, graph.edgeCount()).toArray());
    }
  }

  @Test
  void forEachEdgeOnABiAdjacentGraphAndItsTransposeVisitsTheirOwnEdges() {
    var bi = BiAdjacentGraph.of(TestGraphs.of(3, 0, 1, 0, 2, 2, 1));
    for (BiAdjacentGraph graph : new BiAdjacentGraph[] {bi, bi.transposed()}) {
      var seen = new IntArrayList();
      graph.forEachEdge((source, target, edgeIndex) -> {
        assertThat(graph.edgeIndexOf(source, target)).isEqualTo(edgeIndex);
        seen.add(edgeIndex);
      });
      assertThat(seen.toIntArray()).containsExactly(0, 1, 2);
    }
    var transposedSources = new IntArrayList();
    bi.transposed().forEachEdge((source, target, edgeIndex) -> transposedSources.add(source));
    // Transposed: 1 -> 0, 1 -> 2, 2 -> 0.
    assertThat(transposedSources.toIntArray()).containsExactly(1, 1, 2);
  }

  @Test
  void forEachEdgeOnAGraphWithoutEdgesCallsNothing() {
    var count = new AtomicInteger();
    TestGraphs.isolated(3).forEachEdge((source, target, edgeIndex) -> count.incrementAndGet());
    assertThat(count.get()).isZero();
  }

  @Test
  void toStringListsSmallGraphsCompletely() {
    assertThat(TestGraphs.of(3, 0, 1, 0, 2, 2, 0).toString())
        .isEqualTo("Graph[nodes=3, edges=3] {0 -> [1, 2], 1 -> [], 2 -> [0]}");
    assertThat(TestGraphs.empty().toString()).isEqualTo("Graph[nodes=0, edges=0] {}");
  }

  @Test
  void toStringNamesTheBiAdjacentType() {
    assertThat(BiAdjacentGraph.of(TestGraphs.path(2)).toString())
        .isEqualTo("BiAdjacentGraph[nodes=2, edges=1] {0 -> [1], 1 -> []}");
  }

  @Test
  void toStringCutsAfterSixteenNodes() {
    String text = TestGraphs.isolated(17).toString();
    assertThat(text).startsWith("Graph[nodes=17, edges=0] {0 -> [], ")
        .endsWith("15 -> [], ...}")
        .doesNotContain("16 ->");
  }

  @Test
  void toStringListsExactlySixteenNodesWithoutAMarker() {
    assertThat(TestGraphs.isolated(16).toString()).endsWith("15 -> []}").doesNotContain("...");
  }

  @Test
  void toStringCutInTheLastListHasNoTrailingMarker() {
    // Nodes 0..8 with 7 out-nodes each use 63 of the budget; node 9 is the last node.
    var b = new GraphBuilder();
    b.addNodes(10);
    for (int v = 0; v < 9; v++) {
      for (int t = 0; t < 7; t++) b.addEdge(v, t);
    }
    b.addOutEdges(9, 0, 1);
    assertThat(b.build().toString()).endsWith("9 -> [0, ...]}");
  }

  @Test
  void toStringIsBoundedOnAHub() {
    var b = new GraphBuilder();
    b.addNodes(1_000_000);
    for (int v = 1; v < 1_000_000; v++) b.addEdge(0, v);
    String text = b.build().toString();
    assertThat(text).hasSizeLessThan(600)
        .startsWith("Graph[nodes=1000000, edges=999999] {0 -> [1, 2, ")
        .endsWith(", 64, ...], ...}");

    var high = new GraphBuilder();
    high.addNodes(1_000_000);
    for (int v = 1; v <= 100; v++) high.addEdge(0, 1_000_000 - v);
    assertThat(high.build().toString()).hasSizeLessThan(1024).endsWith(", ...], ...}");
  }

  @Test
  void toStringStopsWhenTheTargetBudgetRunsOutBetweenLists() {
    // 64 out-nodes on node 0 exhaust the budget; node 1 still has edges.
    var b = new GraphBuilder();
    b.addNodes(66);
    for (int v = 1; v <= 64; v++) b.addEdge(0, v);
    b.addEdge(1, 65);
    String text = b.build().toString();
    assertThat(text).contains("63, 64], 1 -> [...], ...}");
  }
}
