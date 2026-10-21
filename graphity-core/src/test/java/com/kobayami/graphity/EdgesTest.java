package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.jupiter.api.Test;

class EdgesTest {

  @Test
  void constructorStoresAllThreeLists() {
    IntList src = IntLists.unmodifiable(new IntArrayList(new int[] {0, 1, 2}));
    IntList tgt = IntLists.unmodifiable(new IntArrayList(new int[] {3, 4, 5}));
    IntList idx = IntLists.unmodifiable(new IntArrayList(new int[] {6, 7, 8}));
    var edges = new Edges(src, tgt, idx);
    assertThatObject(edges.sourceNodes()).isSameAs(src);
    assertThatObject(edges.targetNodes()).isSameAs(tgt);
    assertThatObject(edges.edgeIndices()).isSameAs(idx);
  }

  @Test
  void countReturnsNumberOfEdges() {
    var edges = Edges.wrap(new int[] {0, 1, 2}, new int[] {3, 4, 5}, new int[] {0, 1, 2});
    assertThat(edges.count()).isEqualTo(3);
  }

  @Test
  void isEmptyOnEmptyListsTrue() {
    var edges = Edges.wrap(new int[0], new int[0], new int[0]);
    assertThat(edges.isEmpty()).isTrue();
    assertThat(edges.count()).isZero();
  }

  @Test
  void isEmptyOnNonEmptyListsFalse() {
    var edges = Edges.wrap(new int[] {0}, new int[] {1}, new int[] {0});
    assertThat(edges.isEmpty()).isFalse();
    assertThat(edges.count()).isEqualTo(1);
  }

  @Test
  void emptyFactoryReturnsEmptyInstance() {
    assertThat(Edges.empty().isEmpty()).isTrue();
    assertThat(Edges.empty().count()).isZero();
    assertThat(Edges.empty().sourceNodes().toIntArray()).isEmpty();
    assertThat(Edges.empty().targetNodes().toIntArray()).isEmpty();
    assertThat(Edges.empty().edgeIndices().toIntArray()).isEmpty();
  }

  @Test
  void emptyFactoryReturnsCachedInstance() {
    assertThatObject(Edges.empty()).isSameAs(Edges.empty());
  }

  @Test
  void allListsAreUnmodifiable() {
    var edges = Edges.wrap(new int[] {0, 1}, new int[] {2, 3}, new int[] {0, 1});
    assertThatThrownBy(() -> edges.sourceNodes().add(99))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> edges.targetNodes().add(99))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> edges.edgeIndices().add(99))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> Edges.empty().edgeIndices().add(99))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
