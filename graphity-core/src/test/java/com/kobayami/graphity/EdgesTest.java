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
  void constructorStoresBothLists() {
    IntList src = IntLists.unmodifiable(new IntArrayList(new int[] {0, 1, 2}));
    IntList tgt = IntLists.unmodifiable(new IntArrayList(new int[] {3, 4, 5}));
    var edges = new Edges(src, tgt);
    assertThatObject(edges.sourceNodes()).isSameAs(src);
    assertThatObject(edges.targetNodes()).isSameAs(tgt);
  }

  @Test
  void countReturnsNumberOfEdges() {
    var edges = new Edges(
        IntLists.unmodifiable(new IntArrayList(new int[] {0, 1, 2})),
        IntLists.unmodifiable(new IntArrayList(new int[] {3, 4, 5})));
    assertThat(edges.count()).isEqualTo(3);
  }

  @Test
  void isEmptyOnEmptyListsTrue() {
    var edges = new Edges(
        IntLists.unmodifiable(new IntArrayList()),
        IntLists.unmodifiable(new IntArrayList()));
    assertThat(edges.isEmpty()).isTrue();
    assertThat(edges.count()).isZero();
  }

  @Test
  void isEmptyOnNonEmptyListsFalse() {
    var edges = new Edges(
        IntLists.unmodifiable(new IntArrayList(new int[] {0})),
        IntLists.unmodifiable(new IntArrayList(new int[] {1})));
    assertThat(edges.isEmpty()).isFalse();
    assertThat(edges.count()).isEqualTo(1);
  }

  @Test
  void emptyFactoryReturnsEmptyInstance() {
    assertThat(Edges.empty().isEmpty()).isTrue();
    assertThat(Edges.empty().count()).isZero();
    assertThat(Edges.empty().sourceNodes().toIntArray()).isEmpty();
    assertThat(Edges.empty().targetNodes().toIntArray()).isEmpty();
  }

  @Test
  void emptyFactoryReturnsCachedInstance() {
    assertThatObject(Edges.empty()).isSameAs(Edges.empty());
  }

  @Test
  void sourceAndTargetNodesAreUnmodifiable() {
    var edges = new Edges(
        IntLists.unmodifiable(new IntArrayList(new int[] {0, 1})),
        IntLists.unmodifiable(new IntArrayList(new int[] {2, 3})));
    assertThatThrownBy(() -> edges.sourceNodes().add(99))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> edges.targetNodes().add(99))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
