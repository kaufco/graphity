package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

/**
 * EdgeMapping inherits get/isMapped/mappedCount/isPartial/indexCount and the inverse/compose
 * helpers (invertArray, composeArrays, countValid) from IndexMapping. All of that is
 * exercised through NodeMappingTest.
 * <p>
 * This class therefore tests only what is EdgeMapping-specific: the covariant return
 * types of {@link EdgeMapping#inverted(int)} and {@link EdgeMapping#compose(EdgeMapping)}.
 * Integration with {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)} is covered
 * in GraphsTest.
 */
class EdgeMappingTest {

  @Test
  void inverseReturnsEdgeMapping() {
    var em = new EdgeMapping(new int[] {2, 0, 1}, 3);
    var inv = em.inverted(3);
    assertThatObject(inv).isInstanceOf(EdgeMapping.class);
    assertThat(inv.get(0)).isEqualTo(1);
    assertThat(inv.get(1)).isEqualTo(2);
    assertThat(inv.get(2)).isZero();
  }

  @Test
  void composeReturnsEdgeMapping() {
    var outer = new EdgeMapping(new int[] {10, 20, 30}, 3);
    var inner = new EdgeMapping(new int[] {1, 0, 2}, 3);
    var composed = outer.compose(inner);
    assertThatObject(composed).isInstanceOf(EdgeMapping.class);
    assertThat(composed.get(0)).isEqualTo(20);
    assertThat(composed.get(1)).isEqualTo(10);
    assertThat(composed.get(2)).isEqualTo(30);
  }

  @Test
  void inverseOfMappingWithHolesIsPartial() {
    // Source 1 has no target (-1), so one of the three targets has no source in the inverse.
    var em = new EdgeMapping(new int[] {2, -1, 0}, 2);
    var inv = em.inverted(3);
    assertThat(inv.isPartial()).isTrue();
    assertThat(inv.get(1)).isEqualTo(-1);
  }

  @Test
  void inverseCoversTheWholeTargetSpace() {
    // Path 0 -> 1 -> 2 -> 3 restricted to nodes {0, 1, 2}: the old edge 2 has no new edge.
    var graph = TestGraphs.path(4);
    var keep = NodeMapping.forSubgraph(0, 1, 2);
    var newToOld = Graphs.edgeMapping(Graphs.remapped(graph, keep), graph, keep);
    var oldToNew = newToOld.inverted(graph.edgeCount());
    assertThat(oldToNew.isPartial()).isTrue();
    assertThat(oldToNew.indexCount()).isEqualTo(3);
    assertThat(oldToNew.gather(new double[] {0.5, 1.5})).containsExactly(0.5, 1.5, 0.0);
    assertThatThrownBy(() -> newToOld.inverted(1))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void composeWithHolesPropagatesPartiality() {
    var outer = new EdgeMapping(new int[] {10, 20, 30}, 3);
    var inner = new EdgeMapping(new int[] {1, -1, 2}, 2);
    var composed = outer.compose(inner);
    assertThat(composed.isPartial()).isTrue();
    assertThat(composed.get(1)).isEqualTo(-1);
  }

  @Test
  void gatherMovesEdgeValuesToTheNewNumbering() {
    var em = new EdgeMapping(new int[] {2, -1, 1}, 2);
    assertThat(em.gather(new int[] {5, 6, 7})).containsExactly(7, 0, 6);
    assertThat(em.gather(new long[] {5, 6, 7})).containsExactly(7L, 0L, 6L);
    assertThat(em.gather(new double[] {0.5, 1.5, 2.5})).containsExactly(2.5, 0.0, 1.5);
    assertThatThrownBy(() -> em.gather(new int[] {5, 6}))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> em.gather(new Integer[3], new Integer[2]))
        .isInstanceOf(IllegalArgumentException.class);
    Integer[] same = new Integer[3];
    assertThatThrownBy(() -> em.gather(same, same))
        .isInstanceOf(IllegalArgumentException.class);
    Integer[] newValues = new Integer[3];
    em.gather(new Integer[] {5, 6, 7}, newValues);
    assertThat(newValues).containsExactly(7, null, 6);
  }
}
