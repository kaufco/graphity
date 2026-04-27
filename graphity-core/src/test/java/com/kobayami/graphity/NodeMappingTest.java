package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NodeMappingTest {

  @Test
  void ofFullMappingHasNoUnmappedEntries() {
    var nm = NodeMapping.of(new int[] {2, 0, 1});
    assertThat(nm.size()).isEqualTo(3);
    assertThat(nm.isPartial()).isFalse();
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isZero();
    assertThat(nm.get(2)).isEqualTo(1);
  }

  @Test
  void ofPartialMappingMarksIsPartial() {
    var nm = NodeMapping.of(new int[] {2, -1, 0, -1});
    assertThat(nm.size()).isEqualTo(2);
    assertThat(nm.isPartial()).isTrue();
    assertThat(nm.contains(0)).isTrue();
    assertThat(nm.contains(1)).isFalse();
    assertThat(nm.contains(2)).isTrue();
    assertThat(nm.contains(3)).isFalse();
  }

  @Test
  void ofEmptyArrayCreatesEmptyMapping() {
    var nm = NodeMapping.of(new int[0]);
    assertThat(nm.size()).isZero();
    assertThat(nm.isPartial()).isFalse();
  }

  @Test
  void ofCopiesInputArray() {
    int[] input = {1, 2, 3};
    var nm = NodeMapping.of(input);
    input[0] = 999;
    assertThat(nm.get(0)).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, -100, 3, 1000})
  void getOutOfRangeReturnsMinusOne(int id) {
    var nm = NodeMapping.of(new int[] {10, 20, 30});
    assertThat(nm.get(id)).isEqualTo(-1);
    assertThat(nm.contains(id)).isFalse();
  }

  @Test
  void forSubgraphFromBitSetMapsDenseToOriginal() {
    var selected = new BitSet();
    selected.set(2);
    selected.set(5);
    selected.set(7);
    var nm = NodeMapping.forSubgraph(selected);
    assertThat(nm.size()).isEqualTo(3);
    assertThat(nm.isPartial()).isFalse();
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isEqualTo(5);
    assertThat(nm.get(2)).isEqualTo(7);
  }

  @Test
  void forSubgraphFromEmptyBitSetCreatesEmptyMapping() {
    var nm = NodeMapping.forSubgraph(new BitSet());
    assertThat(nm.size()).isZero();
    assertThat(nm.isPartial()).isFalse();
  }

  @Test
  void forSubgraphFromUnsortedIntListSortsIds() {
    IntList unsorted = new IntArrayList(new int[] {5, 2, 7});
    var nm = NodeMapping.forSubgraph(unsorted);
    assertThat(nm.size()).isEqualTo(3);
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isEqualTo(5);
    assertThat(nm.get(2)).isEqualTo(7);
  }

  @Test
  void forSubgraphFromSortedIntViewSkipsSort() {
    IntList sorted = new SortedIntView(new int[] {2, 5, 7});
    var nm = NodeMapping.forSubgraph(sorted);
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isEqualTo(5);
    assertThat(nm.get(2)).isEqualTo(7);
  }

  @Test
  void forSubgraphDuplicatesInUnsortedListTriggerAssertion() {
    IntList withDupes = new IntArrayList(new int[] {3, 1, 3});
    assertThatThrownBy(() -> NodeMapping.forSubgraph(withDupes))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void forPermutationWrapsOrder() {
    IntList order = new IntArrayList(new int[] {2, 0, 1});
    var nm = NodeMapping.forPermutation(order);
    assertThat(nm.size()).isEqualTo(3);
    assertThat(nm.isPartial()).isFalse();
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isZero();
    assertThat(nm.get(2)).isEqualTo(1);
  }

  @Test
  void forPermutationInvalidPermutationTriggersAssertion() {
    IntList withDupes = new IntArrayList(new int[] {0, 1, 1});
    assertThatThrownBy(() -> NodeMapping.forPermutation(withDupes))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void forPermutationOutOfRangeValueTriggersAssertion() {
    IntList outOfRange = new IntArrayList(new int[] {0, 3, 1});
    assertThatThrownBy(() -> NodeMapping.forPermutation(outOfRange))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void forPermutationNegativeValueTriggersAssertion() {
    IntList withNegative = new IntArrayList(new int[] {0, -1, 1});
    assertThatThrownBy(() -> NodeMapping.forPermutation(withNegative))
        .isInstanceOf(AssertionError.class);
  }

  @Test
  void inverseOfFullPermutationIsInjective() {
    var nm = NodeMapping.of(new int[] {2, 0, 1});
    var inv = nm.inverse();
    assertThat(inv.get(0)).isEqualTo(1);
    assertThat(inv.get(1)).isEqualTo(2);
    assertThat(inv.get(2)).isZero();
    assertThat(inv.isPartial()).isFalse();
    assertThat(inv.size()).isEqualTo(3);
  }

  @Test
  void inverseOfPartialMappingIsPartial() {
    var nm = NodeMapping.of(new int[] {5, -1, 3});
    var inv = nm.inverse();
    assertThat(inv.size()).isEqualTo(2);
    assertThat(inv.isPartial()).isTrue();
    assertThat(inv.get(3)).isEqualTo(2);
    assertThat(inv.get(5)).isZero();
    assertThat(inv.get(0)).isEqualTo(-1);
    assertThat(inv.get(4)).isEqualTo(-1);
  }

  @Test
  void inverseOfNonInjectiveThrows() {
    var nm = NodeMapping.of(new int[] {1, 1, 2});
    assertThatThrownBy(nm::inverse)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("target 1");
  }

  @Test
  void inverseOfEmptyIsEmpty() {
    var nm = NodeMapping.of(new int[0]);
    var inv = nm.inverse();
    assertThat(inv.size()).isZero();
  }

  @Test
  void inverseOfAllMinusOneIsEmpty() {
    var nm = NodeMapping.of(new int[] {-1, -1, -1});
    var inv = nm.inverse();
    assertThat(inv.size()).isZero();
  }

  @Test
  void inverseRoundTripRestoresOriginal() {
    var nm = NodeMapping.of(new int[] {2, 0, 1});
    var roundtrip = nm.inverse().inverse();
    assertThat(roundtrip.get(0)).isEqualTo(2);
    assertThat(roundtrip.get(1)).isZero();
    assertThat(roundtrip.get(2)).isEqualTo(1);
  }

  @Test
  void composeChainsTwoMappings() {
    var outer = NodeMapping.of(new int[] {10, 20, 30});
    var inner = NodeMapping.of(new int[] {1, 0, 2});
    var composed = outer.compose(inner);
    assertThat(composed.get(0)).isEqualTo(20);
    assertThat(composed.get(1)).isEqualTo(10);
    assertThat(composed.get(2)).isEqualTo(30);
    assertThat(composed.isPartial()).isFalse();
  }

  @Test
  void composePropagatesUnmappedFromInner() {
    var outer = NodeMapping.of(new int[] {10, 20, 30});
    var inner = NodeMapping.of(new int[] {1, -1, 2});
    var composed = outer.compose(inner);
    assertThat(composed.get(0)).isEqualTo(20);
    assertThat(composed.get(1)).isEqualTo(-1);
    assertThat(composed.get(2)).isEqualTo(30);
    assertThat(composed.isPartial()).isTrue();
  }

  @Test
  void composePropagatesUnmappedFromOuter() {
    var outer = NodeMapping.of(new int[] {10, -1, 30});
    var inner = NodeMapping.of(new int[] {1, 0, 2});
    var composed = outer.compose(inner);
    assertThat(composed.get(0)).isEqualTo(-1);
    assertThat(composed.get(1)).isEqualTo(10);
    assertThat(composed.get(2)).isEqualTo(30);
    assertThat(composed.isPartial()).isTrue();
  }

  @Test
  void composeHandlesInnerValueOutOfOuterRange() {
    var outer = NodeMapping.of(new int[] {10, 20});
    var inner = NodeMapping.of(new int[] {0, 5, 1});
    var composed = outer.compose(inner);
    assertThat(composed.get(0)).isEqualTo(10);
    assertThat(composed.get(1)).isEqualTo(-1);
    assertThat(composed.get(2)).isEqualTo(20);
  }

  @Test
  void composeReturnsNodeMapping() {
    var outer = NodeMapping.of(new int[] {10, 20});
    var inner = NodeMapping.of(new int[] {0, 1});
    assertThatObject(outer.compose(inner)).isInstanceOf(NodeMapping.class);
  }

  @Test
  void reindexEmptyGraphReturnsEmptyMapping() {
    var graph = new GraphBuilder().build();
    var nm = NodeMapping.reindex(graph);
    assertThat(nm.size()).isZero();
  }

  @Test
  void reindexSingleNodeGraphReturnsIdentity() {
    var builder = new GraphBuilder();
    builder.addNode();
    var nm = NodeMapping.reindex(builder.build());
    assertThat(nm.size()).isEqualTo(1);
    assertThat(nm.get(0)).isZero();
  }

  @Test
  void reindexIsPermutationOfAllNodes() {
    // Graph: 0 -> 1 -> 2, 0 -> 3
    var builder = new GraphBuilder();
    builder.addNodes(4);
    builder.addEdge(0, 1);
    builder.addEdge(1, 2);
    builder.addEdge(0, 3);
    var nm = NodeMapping.reindex(builder.build());
    assertThat(nm.size()).isEqualTo(4);
    assertThat(nm.isPartial()).isFalse();
    int[] targets = new int[4];
    for (int i = 0; i < 4; i++) targets[i] = nm.get(i);
    assertThat(targets).containsExactlyInAnyOrder(0, 1, 2, 3);
  }

  @Test
  void reindexPlacesDfsChildrenAdjacent() {
    // Path 0 -> 1 -> 2 -> 3: DFS from 0 visits 0, 1, 2, 3 in order
    var builder = new GraphBuilder();
    builder.addNodes(4);
    builder.addEdge(0, 1);
    builder.addEdge(1, 2);
    builder.addEdge(2, 3);
    var nm = NodeMapping.reindex(builder.build());
    assertThat(nm.get(0)).isZero();
    assertThat(nm.get(1)).isEqualTo(1);
    assertThat(nm.get(2)).isEqualTo(2);
    assertThat(nm.get(3)).isEqualTo(3);
  }
}
