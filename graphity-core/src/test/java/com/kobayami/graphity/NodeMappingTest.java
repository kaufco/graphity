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
    assertThat(nm.mappedCount()).isEqualTo(3);
    assertThat(nm.isPartial()).isFalse();
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isZero();
    assertThat(nm.get(2)).isEqualTo(1);
  }

  @Test
  void ofPartialMappingMarksIsPartial() {
    var nm = NodeMapping.of(new int[] {2, -1, 0, -1});
    assertThat(nm.mappedCount()).isEqualTo(2);
    assertThat(nm.isPartial()).isTrue();
    assertThat(nm.isMapped(0)).isTrue();
    assertThat(nm.isMapped(1)).isFalse();
    assertThat(nm.isMapped(2)).isTrue();
    assertThat(nm.isMapped(3)).isFalse();
  }

  @Test
  void ofEmptyArrayCreatesEmptyMapping() {
    var nm = NodeMapping.of(new int[0]);
    assertThat(nm.mappedCount()).isZero();
    assertThat(nm.isPartial()).isFalse();
  }

  @Test
  void ofRejectsValuesBelowMinusOne() {
    assertThatThrownBy(() -> NodeMapping.of(new int[] {0, -2, 1}))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("-2");
  }

  @Test
  void ofAcceptsRepeatsAsTheCallersContract() {
    var nm = NodeMapping.of(new int[] {0, 0});
    assertThat(nm.mappedCount()).isEqualTo(2);
    assertThatThrownBy(() -> nm.inverted(1))
        .isInstanceOf(IllegalArgumentException.class);
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
  void getOutOfRangeThrows(int id) {
    var nm = NodeMapping.of(new int[] {10, 20, 30});
    assertThatThrownBy(() -> nm.get(id)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> nm.isMapped(id)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void indexCountIsTheSourceSpaceSizeIncludingUnmappedIds() {
    var nm = NodeMapping.of(new int[] {2, -1, 0, -1});
    assertThat(nm.indexCount()).isEqualTo(4);
    assertThat(nm.mappedCount()).isEqualTo(2);
    assertThat(NodeMapping.of(new int[0]).indexCount()).isZero();
  }

  @Test
  void forSubgraphFromBitSetMapsDenseToOriginal() {
    var selected = new BitSet();
    selected.set(2);
    selected.set(5);
    selected.set(7);
    var nm = NodeMapping.forSubgraph(selected);
    assertThat(nm.mappedCount()).isEqualTo(3);
    assertThat(nm.isPartial()).isFalse();
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isEqualTo(5);
    assertThat(nm.get(2)).isEqualTo(7);
  }

  @Test
  void forSubgraphFromEmptyBitSetCreatesEmptyMapping() {
    var nm = NodeMapping.forSubgraph(new BitSet());
    assertThat(nm.mappedCount()).isZero();
    assertThat(nm.isPartial()).isFalse();
  }

  @Test
  void forSubgraphFromUnsortedIntListSortsIds() {
    IntList unsorted = new IntArrayList(new int[] {5, 2, 7});
    var nm = NodeMapping.forSubgraph(unsorted);
    assertThat(nm.mappedCount()).isEqualTo(3);
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
  void forSubgraphRejectsDuplicatesInList() {
    IntList withDupes = new IntArrayList(new int[] {3, 1, 3});
    assertThatThrownBy(() -> NodeMapping.forSubgraph(withDupes))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("3");
  }

  @Test
  void forSubgraphRejectsANegativeIdInSortedIntView() {
    var withNegative = new SortedIntView(new int[] {-1, 2});
    assertThatThrownBy(() -> NodeMapping.forSubgraph(withNegative))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void forSubgraphRejectsNegativeIds() {
    assertThatThrownBy(() -> NodeMapping.forSubgraph(new IntArrayList(new int[] {2, -1})))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> NodeMapping.forSubgraph(2, -1))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void forSubgraphFromLooseArgumentsSortsIds() {
    var nm = NodeMapping.forSubgraph(5, 2, 7);
    assertThat(nm.mappedCount()).isEqualTo(3);
    assertThat(nm.isPartial()).isFalse();
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isEqualTo(5);
    assertThat(nm.get(2)).isEqualTo(7);
  }

  @Test
  void forSubgraphFromNoArgumentsCreatesEmptyMapping() {
    var nm = NodeMapping.forSubgraph();
    assertThat(nm.mappedCount()).isZero();
    assertThat(nm.isPartial()).isFalse();
  }

  @Test
  void forSubgraphCopiesInputArray() {
    int[] input = {5, 2, 7};
    var nm = NodeMapping.forSubgraph(input);
    assertThat(input).containsExactly(5, 2, 7);
    input[0] = 999;
    assertThat(nm.get(1)).isEqualTo(5);
  }

  @Test
  void forSubgraphArrayAndBitSetAgree() {
    var selected = new BitSet();
    selected.set(2);
    selected.set(5);
    selected.set(7);
    var fromBitSet = NodeMapping.forSubgraph(selected);
    var fromArray = NodeMapping.forSubgraph(7, 2, 5);
    assertThat(fromArray.mappedCount()).isEqualTo(fromBitSet.mappedCount());
    for (int id = 0; id < fromBitSet.mappedCount(); id++) {
      assertThat(fromArray.get(id)).isEqualTo(fromBitSet.get(id));
    }
  }

  @Test
  void forSubgraphRejectsDuplicatesInArray() {
    assertThatThrownBy(() -> NodeMapping.forSubgraph(3, 1, 3))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void forPermutationWrapsOrder() {
    IntList order = new IntArrayList(new int[] {2, 0, 1});
    var nm = NodeMapping.forPermutation(order);
    assertThat(nm.mappedCount()).isEqualTo(3);
    assertThat(nm.isPartial()).isFalse();
    assertThat(nm.get(0)).isEqualTo(2);
    assertThat(nm.get(1)).isZero();
    assertThat(nm.get(2)).isEqualTo(1);
  }

  @Test
  void forPermutationRejectsDuplicates() {
    IntList withDupes = new IntArrayList(new int[] {0, 1, 1});
    assertThatThrownBy(() -> NodeMapping.forPermutation(withDupes))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("1");
  }

  @Test
  void forPermutationRejectsOutOfRangeValue() {
    IntList outOfRange = new IntArrayList(new int[] {0, 3, 1});
    assertThatThrownBy(() -> NodeMapping.forPermutation(outOfRange))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("3");
  }

  @Test
  void forPermutationRejectsNegativeValue() {
    IntList withNegative = new IntArrayList(new int[] {0, -1, 1});
    assertThatThrownBy(() -> NodeMapping.forPermutation(withNegative))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void inverseOfFullPermutationIsInjective() {
    var nm = NodeMapping.of(new int[] {2, 0, 1});
    var inv = nm.inverted(3);
    assertThat(inv.get(0)).isEqualTo(1);
    assertThat(inv.get(1)).isEqualTo(2);
    assertThat(inv.get(2)).isZero();
    assertThat(inv.isPartial()).isFalse();
    assertThat(inv.mappedCount()).isEqualTo(3);
  }

  @Test
  void inverseOfPartialMappingIsPartial() {
    var nm = NodeMapping.of(new int[] {5, -1, 3});
    var inv = nm.inverted(7);
    assertThat(inv.mappedCount()).isEqualTo(2);
    assertThat(inv.indexCount()).isEqualTo(7);
    assertThat(inv.isPartial()).isTrue();
    assertThat(inv.get(3)).isEqualTo(2);
    assertThat(inv.get(5)).isZero();
    assertThat(inv.get(0)).isEqualTo(-1);
    assertThat(inv.get(4)).isEqualTo(-1);
    assertThat(inv.get(6)).isEqualTo(-1);
  }

  @Test
  void inverseOfSubgraphCoversTheWholeOldGraph() {
    // Path with 5 nodes, subgraph {0, 1}: the inverse maps all 5 old nodes, 2..4 to -1.
    var oldToNew = NodeMapping.forSubgraph(0, 1).inverted(5);
    assertThat(oldToNew.isPartial()).isTrue();
    assertThat(oldToNew.indexCount()).isEqualTo(5);
    assertThat(oldToNew.get(4)).isEqualTo(-1);
    assertThat(oldToNew.gather(new int[] {7, 8})).containsExactly(7, 8, 0, 0, 0);
    String[] oldNames = new String[5];
    oldToNew.gather(new String[] {"a", "b"}, oldNames);
    assertThat(oldNames).containsExactly("a", "b", null, null, null);
  }

  @Test
  void inverseRejectsATargetOutsideTheTargetSpace() {
    var nm = NodeMapping.forSubgraph(0, 4);
    assertThatThrownBy(() -> nm.inverted(4))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("4");
    assertThat(nm.inverted(5).indexCount()).isEqualTo(5);
  }

  @Test
  void inverseRejectsANegativeTargetCount() {
    var nm = NodeMapping.of(new int[0]);
    assertThatThrownBy(() -> nm.inverted(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void inverseOfNonInjectiveThrows() {
    var nm = NodeMapping.of(new int[] {1, 1, 2});
    assertThatThrownBy(() -> nm.inverted(3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("target 1");
  }

  @Test
  void inverseOfEmptyIsEmpty() {
    var nm = NodeMapping.of(new int[0]);
    var inv = nm.inverted(0);
    assertThat(inv.mappedCount()).isZero();
    assertThat(inv.indexCount()).isZero();
  }

  @Test
  void inverseOfAllMinusOneMapsNothing() {
    var nm = NodeMapping.of(new int[] {-1, -1, -1});
    var inv = nm.inverted(2);
    assertThat(inv.mappedCount()).isZero();
    assertThat(inv.indexCount()).isEqualTo(2);
    assertThat(inv.isPartial()).isTrue();
  }

  @Test
  void inverseRoundTripRestoresOriginal() {
    var nm = NodeMapping.of(new int[] {2, 0, 1});
    var roundtrip = nm.inverted(3).inverted(3);
    assertThat(roundtrip.get(0)).isEqualTo(2);
    assertThat(roundtrip.get(1)).isZero();
    assertThat(roundtrip.get(2)).isEqualTo(1);
  }

  @Test
  void doubleInversionKeepsTheSourceSpace() {
    var nm = NodeMapping.of(new int[] {-1, 0, -1});
    var roundtrip = nm.inverted(1).inverted(3);
    assertThat(roundtrip.indexCount()).isEqualTo(3);
    assertThat(roundtrip.gather(new int[] {5})).containsExactly(0, 5, 0);
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
  void composeRejectsInnerValueOutOfOuterRange() {
    var outer = NodeMapping.of(new int[] {10, 20});
    var inner = NodeMapping.of(new int[] {0, 5, 1});
    assertThatThrownBy(() -> outer.compose(inner))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("5");
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
    assertThat(nm.mappedCount()).isZero();
  }

  @Test
  void reindexSingleNodeGraphReturnsIdentity() {
    var builder = new GraphBuilder();
    builder.addNode();
    var nm = NodeMapping.reindex(builder.build());
    assertThat(nm.mappedCount()).isEqualTo(1);
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
    assertThat(nm.mappedCount()).isEqualTo(4);
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

  // --- gather ---

  @Test
  void gatherMovesValuesToTheNewNumbering() {
    // new -> old: 0 -> 2, 1 -> -1 (unmapped), 2 -> 0
    var m = NodeMapping.of(new int[] {2, -1, 0});
    assertThat(m.gather(new int[] {10, 11, 12})).containsExactly(12, 0, 10);
    assertThat(m.gather(new long[] {10, 11, 12})).containsExactly(12L, 0L, 10L);
    assertThat(m.gather(new double[] {1.5, 2.5, 3.5})).containsExactly(3.5, 0.0, 1.5);
    String[] newValues = {"x", "keep", "y"};
    m.gather(new String[] {"a", "b", "c"}, newValues);
    assertThat(newValues).containsExactly("c", "keep", "a");
  }

  @Test
  void gatherAlongASubgraph() {
    var newToOld = NodeMapping.forSubgraph(1, 3);
    assertThat(newToOld.gather(new int[] {0, 10, 20, 30})).containsExactly(10, 30);
  }

  @Test
  void gatherRejectsWrongArrays() {
    var m = NodeMapping.of(new int[] {2, 0});
    assertThatThrownBy(() -> m.gather(new int[] {1, 2}))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> m.gather(new String[] {"a", "b", "c"}, new String[3]))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void gatherRejectsTheSameArrayForValuesAndNewValues() {
    var swap = NodeMapping.forPermutation(new IntArrayList(new int[] {1, 0}));
    String[] values = {"a", "b"};
    assertThatThrownBy(() -> swap.gather(values, values))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(values).containsExactly("a", "b");
  }
}
