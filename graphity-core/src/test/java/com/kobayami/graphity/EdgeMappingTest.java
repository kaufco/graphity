package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

import org.junit.jupiter.api.Test;

/**
 * EdgeMapping inherits get/contains/size/isPartial and the inverse/compose helpers
 * (computeInverse, computeCompose, countValid) from IndexMapping. All of that is
 * exercised through NodeMappingTest.
 * <p>
 * This class therefore tests only what is EdgeMapping-specific: the covariant return
 * types of {@link EdgeMapping#inverse()} and {@link EdgeMapping#compose(EdgeMapping)}.
 * Integration with {@link Graphs#edgeMapping(Graph, Graph, NodeMapping)} is covered
 * in GraphsTest.
 */
class EdgeMappingTest {

  @Test
  void inverseReturnsEdgeMapping() {
    var em = new EdgeMapping(new int[] {2, 0, 1}, 3, false);
    var inv = em.inverse();
    assertThatObject(inv).isInstanceOf(EdgeMapping.class);
    assertThat(inv.get(0)).isEqualTo(1);
    assertThat(inv.get(1)).isEqualTo(2);
    assertThat(inv.get(2)).isZero();
  }

  @Test
  void composeReturnsEdgeMapping() {
    var outer = new EdgeMapping(new int[] {10, 20, 30}, 3, false);
    var inner = new EdgeMapping(new int[] {1, 0, 2}, 3, false);
    var composed = outer.compose(inner);
    assertThatObject(composed).isInstanceOf(EdgeMapping.class);
    assertThat(composed.get(0)).isEqualTo(20);
    assertThat(composed.get(1)).isEqualTo(10);
    assertThat(composed.get(2)).isEqualTo(30);
  }

  @Test
  void inverseOfMappingWithHolesIsPartial() {
    // Source 1 has no target (-1). Inverse has length max(0,2)+1 = 3 with only two valid entries.
    var em = new EdgeMapping(new int[] {2, -1, 0}, 2, true);
    var inv = em.inverse();
    assertThat(inv.isPartial()).isTrue();
  }

  @Test
  void composeWithHolesPropagatesPartiality() {
    var outer = new EdgeMapping(new int[] {10, 20, 30}, 3, false);
    var inner = new EdgeMapping(new int[] {1, -1, 2}, 2, true);
    var composed = outer.compose(inner);
    assertThat(composed.isPartial()).isTrue();
    assertThat(composed.get(1)).isEqualTo(-1);
  }
}
