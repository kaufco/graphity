package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NodePartitionTest {

  /**
   * Builds a NodePartition for: nodes 0..4 in 3 groups {0,3} / {1,4} / {2}.
   * Expects groupIds 0, 1, 2.
   */
  private static NodePartition threeGroupPartition(Graph graph) {
    int[] nodeToGroup = {0, 1, 2, 0, 1};
    int[] members = {0, 3, 1, 4, 2};
    int[] memberStart = {0, 2, 4, 5};
    return new NodePartition(graph, nodeToGroup, members, memberStart);
  }

  private static Graph emptyGraph() {
    return new GraphBuilder().build();
  }

  @Test
  void groupCountReportsNumberOfGroups() {
    var p = threeGroupPartition(emptyGraph());
    assertThat(p.groupCount()).isEqualTo(3);
  }

  @Test
  void indexCountReportsTotalNodes() {
    var p = threeGroupPartition(emptyGraph());
    assertThat(p.indexCount()).isEqualTo(5);
    assertThat(p.nodeCount()).isEqualTo(5);
  }

  @Test
  void getReturnsGroupIdForEachNode() {
    var p = threeGroupPartition(emptyGraph());
    assertThat(p.get(0)).isZero();
    assertThat(p.get(1)).isEqualTo(1);
    assertThat(p.get(2)).isEqualTo(2);
    assertThat(p.get(3)).isZero();
    assertThat(p.get(4)).isEqualTo(1);
  }

  @Test
  void isPartialIsFalseForPartition() {
    var p = threeGroupPartition(emptyGraph());
    assertThat(p.isPartial()).isFalse();
  }

  @Test
  void indicesOfReturnsSortedMembers() {
    var p = threeGroupPartition(emptyGraph());
    assertThat(p.indicesOf(0).toIntArray()).containsExactly(0, 3);
    assertThat(p.indicesOf(1).toIntArray()).containsExactly(1, 4);
    assertThat(p.indicesOf(2).toIntArray()).containsExactly(2);
  }

  @Test
  void nodesOfDelegatesToIndicesOf() {
    var p = threeGroupPartition(emptyGraph());
    assertThat(p.nodesOf(0).toIntArray()).containsExactly(0, 3);
    assertThat(p.nodesOf(1).toIntArray()).containsExactly(1, 4);
    assertThat(p.nodesOf(2).toIntArray()).containsExactly(2);
  }

  @Test
  void sizeOfReportsGroupSize() {
    var p = threeGroupPartition(emptyGraph());
    assertThat(p.sizeOf(0)).isEqualTo(2);
    assertThat(p.sizeOf(1)).isEqualTo(2);
    assertThat(p.sizeOf(2)).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void indicesOfOutOfRangeThrows(int groupId) {
    var p = threeGroupPartition(emptyGraph());
    assertThatThrownBy(() -> p.indicesOf(groupId))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void sizeOfOutOfRangeThrows(int groupId) {
    var p = threeGroupPartition(emptyGraph());
    assertThatThrownBy(() -> p.sizeOf(groupId))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void condenseEmptyPartitionReturnsEmptyGraph() {
    var p = new NodePartition(emptyGraph(), new int[0], new int[0], new int[] {0});
    var condensed = p.condense();
    assertThat(condensed.nodeCount()).isZero();
    assertThat(condensed.edgeCount()).isZero();
  }

  @Test
  void condenseSingleGroupHasNoEdges() {
    // Graph: 0 -> 1, 1 -> 0  — all nodes in single group.
    var builder = new GraphBuilder();
    builder.addNodes(2);
    builder.addEdge(0, 1);
    builder.addEdge(1, 0);
    var graph = builder.build();
    var p = new NodePartition(graph, new int[] {0, 0}, new int[] {0, 1}, new int[] {0, 2});
    var condensed = p.condense();
    assertThat(condensed.nodeCount()).isEqualTo(1);
    assertThat(condensed.edgeCount()).isZero();
  }

  @Test
  void condenseOmitsIntraGroupEdges() {
    // Graph: 0 -> 1 (intra), 0 -> 2 (inter), 1 -> 2 (inter)
    // Partition: {0,1} / {2}
    var builder = new GraphBuilder();
    builder.addNodes(3);
    builder.addEdge(0, 1);
    builder.addEdge(0, 2);
    builder.addEdge(1, 2);
    var graph = builder.build();
    var p = new NodePartition(graph, new int[] {0, 0, 1}, new int[] {0, 1, 2}, new int[] {0, 2, 3});
    var condensed = p.condense();
    assertThat(condensed.nodeCount()).isEqualTo(2);
    assertThat(condensed.edgeCount()).isEqualTo(1);
    assertThat(condensed.outNodes(0).toIntArray()).containsExactly(1);
    assertThat(condensed.outNodes(1).toIntArray()).isEmpty();
  }

  @Test
  void condenseDeduplicatesParallelInterGroupEdges() {
    // Graph: 0 -> 2, 1 -> 2 — both endpoints in group 0, target in group 1.
    // Expect only one edge 0 -> 1 in condensation.
    var builder = new GraphBuilder();
    builder.addNodes(3);
    builder.addEdge(0, 2);
    builder.addEdge(1, 2);
    var graph = builder.build();
    var p = new NodePartition(graph, new int[] {0, 0, 1}, new int[] {0, 1, 2}, new int[] {0, 2, 3});
    var condensed = p.condense();
    assertThat(condensed.edgeCount()).isEqualTo(1);
    assertThat(condensed.outNodes(0).toIntArray()).containsExactly(1);
  }

  @Test
  void condenseProducesSortedAdjacencyLists() {
    // Graph: 0 -> 3 (g0 -> g2), 0 -> 2 (g0 -> g1), 1 -> 2 (g0 -> g1 again; dedup'd).
    // Partition: {0,1} / {2} / {3}. Expect group 0's out-neighbors = [1, 2].
    var builder = new GraphBuilder();
    builder.addNodes(4);
    builder.addEdge(0, 3);
    builder.addEdge(0, 2);
    builder.addEdge(1, 2);
    var graph = builder.build();
    var p = new NodePartition(
        graph,
        new int[] {0, 0, 1, 2},
        new int[] {0, 1, 2, 3},
        new int[] {0, 2, 3, 4});
    var condensed = p.condense();
    assertThat(condensed.outNodes(0).toIntArray()).containsExactly(1, 2);
  }

  // --- of(Graph, int[], int) ---

  @Test
  void ofGroupsNodesAsGiven() {
    // 0 -> 1 -> 2 -> 3, 3 -> 0; groups {0, 1} and {2, 3}.
    var graph = TestGraphs.of(4, 0, 1, 1, 2, 2, 3, 3, 0);
    var p = NodePartition.of(graph, new int[] {0, 0, 1, 1}, 2);
    assertThat(p.groupCount()).isEqualTo(2);
    assertThat(p.nodeCount()).isEqualTo(4);
    assertThat(p.isPartial()).isFalse();
    assertThat(p.nodesOf(0).toIntArray()).containsExactly(0, 1);
    assertThat(p.nodesOf(1).toIntArray()).containsExactly(2, 3);
    var condensed = p.condense();
    assertThat(condensed.outNodes(0).toIntArray()).containsExactly(1);
    assertThat(condensed.outNodes(1).toIntArray()).containsExactly(0);
  }

  @Test
  void ofLeavesUngroupedNodesOutOfTheCondensation() {
    // 0 -> 1 -> 2, 0 -> 2; node 1 in no group.
    var graph = TestGraphs.of(3, 0, 1, 1, 2, 0, 2);
    var p = NodePartition.of(graph, new int[] {0, -1, 1}, 2);
    assertThat(p.isPartial()).isTrue();
    assertThat(p.size()).isEqualTo(2);
    assertThat(p.get(1)).isEqualTo(-1);
    assertThat(p.contains(1)).isFalse();
    var condensed = p.condense();
    assertThat(condensed.edgeCount()).isEqualTo(1);
    assertThat(condensed.hasOutNode(0, 1)).isTrue();
    // Edge indices: 0 -> 1 is 0, 0 -> 2 is 1, 1 -> 2 is 2.
    var provenance = Graphs.edgePartition(condensed, graph, p);
    assertThat(provenance.edgesOf(0).toIntArray()).containsExactly(1);
    assertThat(provenance.get(0)).isEqualTo(-1);
    assertThat(provenance.get(2)).isEqualTo(-1);
  }

  @Test
  void ofKeepsEmptyGroupsAsIsolatedNodes() {
    var graph = TestGraphs.of(2, 0, 1);
    var p = NodePartition.of(graph, new int[] {2, 0}, 4);
    assertThat(p.sizeOf(1)).isZero();
    assertThat(p.sizeOf(3)).isZero();
    var condensed = p.condense();
    assertThat(condensed.nodeCount()).isEqualTo(4);
    assertThat(condensed.edgeCount()).isEqualTo(1);
    assertThat(condensed.hasOutNode(2, 0)).isTrue();
  }

  @Test
  void ofCopiesTheArray() {
    var graph = TestGraphs.of(2, 0, 1);
    int[] groupOf = {0, 1};
    var p = NodePartition.of(graph, groupOf, 2);
    groupOf[0] = 1;
    assertThat(p.get(0)).isZero();
  }

  @Test
  void ofOnAnEmptyGraph() {
    var p = NodePartition.of(emptyGraph(), new int[0], 0);
    assertThat(p.groupCount()).isZero();
    assertThat(p.condense().nodeCount()).isZero();
    var withGroups = NodePartition.of(emptyGraph(), new int[0], 2);
    assertThat(withGroups.condense().nodeCount()).isEqualTo(2);
  }

  @Test
  void ofRejectsInvalidInput() {
    var graph = TestGraphs.of(2, 0, 1);
    assertThatThrownBy(() -> NodePartition.of(graph, new int[] {0}, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NodePartition.of(graph, new int[] {0, 1}, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NodePartition.of(graph, new int[] {0, -2}, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NodePartition.of(graph, new int[] {-1, -1}, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NodePartition.of(graph, new int[] {-1, -1}, Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
