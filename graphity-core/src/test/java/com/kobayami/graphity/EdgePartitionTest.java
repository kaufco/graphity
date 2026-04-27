package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * EdgePartition's CSR/get/size/inverse logic is structurally identical to NodePartition.
 * Here we focus on EdgePartition-specific aspects: partial mapping (intra-group edges
 * map to -1), edgeCount()/edgesOf() delegation, and inverse() throwing.
 * Integration with Graphs.edgePartition(...) is covered in GraphsTest.
 */
class EdgePartitionTest {

  /**
   * Builds an EdgePartition for 6 original edges, 3 condensed groups:
   *   original edge 0 -> condensed 0
   *   original edge 1 -> condensed 0
   *   original edge 2 -> unmapped (intra-group)
   *   original edge 3 -> condensed 1
   *   original edge 4 -> unmapped (intra-group)
   *   original edge 5 -> condensed 2
   */
  private static EdgePartition sampleEdgePartition() {
    int[] mapping = {0, 0, -1, 1, -1, 2};
    int[] members = {0, 1, 3, 5};
    int[] memberStart = {0, 2, 3, 4};
    return new EdgePartition(mapping, 4, true, members, memberStart);
  }

  @Test
  void groupCountReportsNumberOfCondensedEdges() {
    assertThat(sampleEdgePartition().groupCount()).isEqualTo(3);
  }

  @Test
  void indexCountReportsTotalOriginalEdges() {
    var p = sampleEdgePartition();
    assertThat(p.indexCount()).isEqualTo(6);
    assertThat(p.edgeCount()).isEqualTo(6);
  }

  @Test
  void getReturnsCondensedEdgeOrMinusOne() {
    var p = sampleEdgePartition();
    assertThat(p.get(0)).isZero();
    assertThat(p.get(1)).isZero();
    assertThat(p.get(2)).isEqualTo(-1);
    assertThat(p.get(3)).isEqualTo(1);
    assertThat(p.get(4)).isEqualTo(-1);
    assertThat(p.get(5)).isEqualTo(2);
  }

  @Test
  void isPartialTrueWhenIntraGroupEdgesPresent() {
    assertThat(sampleEdgePartition().isPartial()).isTrue();
  }

  @Test
  void indicesOfReturnsSortedOriginalEdgesPerCondensedEdge() {
    var p = sampleEdgePartition();
    assertThat(p.indicesOf(0).toIntArray()).containsExactly(0, 1);
    assertThat(p.indicesOf(1).toIntArray()).containsExactly(3);
    assertThat(p.indicesOf(2).toIntArray()).containsExactly(5);
  }

  @Test
  void edgesOfDelegatesToIndicesOf() {
    var p = sampleEdgePartition();
    assertThat(p.edgesOf(0).toIntArray()).containsExactly(0, 1);
    assertThat(p.edgesOf(1).toIntArray()).containsExactly(3);
    assertThat(p.edgesOf(2).toIntArray()).containsExactly(5);
  }

  @Test
  void sizeOfReportsEdgesPerGroup() {
    var p = sampleEdgePartition();
    assertThat(p.sizeOf(0)).isEqualTo(2);
    assertThat(p.sizeOf(1)).isEqualTo(1);
    assertThat(p.sizeOf(2)).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void indicesOfOutOfRangeThrows(int groupId) {
    assertThatThrownBy(() -> sampleEdgePartition().indicesOf(groupId))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void sizeOfOutOfRangeThrows(int groupId) {
    assertThatThrownBy(() -> sampleEdgePartition().sizeOf(groupId))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void inverseThrowsAlways() {
    assertThatThrownBy(() -> sampleEdgePartition().inverse())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("edgesOf");
  }
}
