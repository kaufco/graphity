package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SortedIntViewTest {

  @Test
  void constructorFullArrayCoversWholeArray() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7});
    assertThat(view.size()).isEqualTo(4);
    assertThat(view.toIntArray()).containsExactly(1, 3, 5, 7);
  }

  @Test
  void constructorRangeSubsetCoversRange() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7, 9}, 1, 4);
    assertThat(view.size()).isEqualTo(3);
    assertThat(view.toIntArray()).containsExactly(3, 5, 7);
  }

  @Test
  void constructorEmptyRangeIsEmpty() {
    var view = new SortedIntView(new int[] {1, 2, 3}, 2, 2);
    assertThat(view.size()).isZero();
    assertThat(view.isEmpty()).isTrue();
  }

  @Test
  void constructorNegativeFromIndexThrows() {
    assertThatThrownBy(() -> new SortedIntView(new int[] {1, 2, 3}, -1, 2))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void constructorToIndexBeyondLengthThrows() {
    assertThatThrownBy(() -> new SortedIntView(new int[] {1, 2, 3}, 0, 4))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void constructorFromGreaterThanToThrows() {
    assertThatThrownBy(() -> new SortedIntView(new int[] {1, 2, 3}, 2, 1))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void emptyConstantHasSizeZero() {
    assertThat(SortedIntView.EMPTY.size()).isZero();
    assertThat(SortedIntView.EMPTY.isEmpty()).isTrue();
  }

  @Test
  void getIntValidIndexReturnsElement() {
    var view = new SortedIntView(new int[] {10, 20, 30, 40}, 1, 4);
    assertThat(view.getInt(0)).isEqualTo(20);
    assertThat(view.getInt(1)).isEqualTo(30);
    assertThat(view.getInt(2)).isEqualTo(40);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 100})
  void getIntOutOfBoundsThrows(int index) {
    var view = new SortedIntView(new int[] {1, 2, 3});
    assertThatThrownBy(() -> view.getInt(index))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void firstReturnsSmallestElement() {
    var view = new SortedIntView(new int[] {5, 10, 15});
    assertThat(view.first()).isEqualTo(5);
  }

  @Test
  void lastReturnsLargestElement() {
    var view = new SortedIntView(new int[] {5, 10, 15});
    assertThat(view.last()).isEqualTo(15);
  }

  @Test
  void firstOnEmptyThrows() {
    assertThatThrownBy(() -> SortedIntView.EMPTY.first())
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void lastOnEmptyThrows() {
    assertThatThrownBy(() -> SortedIntView.EMPTY.last())
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void firstOnSubViewRespectsRange() {
    var view = new SortedIntView(new int[] {1, 2, 3, 4, 5}, 2, 5);
    assertThat(view.first()).isEqualTo(3);
    assertThat(view.last()).isEqualTo(5);
  }

  @ParameterizedTest
  @CsvSource({
      "1, 0",
      "3, 1",
      "5, 2",
      "7, 3",
      "9, 4",
      "0, -1",
      "2, -1",
      "4, -1",
      "10, -1",
  })
  void indexOfOnFullView(int value, int expectedIndex) {
    var view = new SortedIntView(new int[] {1, 3, 5, 7, 9});
    assertThat(view.indexOf(value)).isEqualTo(expectedIndex);
  }

  @Test
  void indexOfOnSubViewReturnsViewRelativeIndex() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7, 9}, 2, 5);
    assertThat(view.indexOf(5)).isEqualTo(0);
    assertThat(view.indexOf(7)).isEqualTo(1);
    assertThat(view.indexOf(9)).isEqualTo(2);
    assertThat(view.indexOf(3)).isEqualTo(-1);
    assertThat(view.indexOf(1)).isEqualTo(-1);
  }

  @Test
  void lastIndexOfBehavesLikeIndexOf() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7, 9});
    assertThat(view.lastIndexOf(5)).isEqualTo(view.indexOf(5));
    assertThat(view.lastIndexOf(4)).isEqualTo(view.indexOf(4));
  }

  @Test
  void containsInheritedFromIntListWorks() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThat(view.contains(3)).isTrue();
    assertThat(view.contains(4)).isFalse();
  }

  @Test
  void indexOfInRangeReturnsAbsoluteIndex() {
    int[] array = {1, 3, 5, 7, 9};
    assertThat(SortedIntView.indexOfInRange(array, 2, 5, 5)).isEqualTo(2);
    assertThat(SortedIntView.indexOfInRange(array, 2, 5, 7)).isEqualTo(3);
    assertThat(SortedIntView.indexOfInRange(array, 2, 5, 3)).isEqualTo(-1);
  }

  @Test
  void containsInRangeDetectsMembership() {
    int[] array = {1, 3, 5, 7, 9};
    assertThat(SortedIntView.containsInRange(array, 2, 5, 7)).isTrue();
    assertThat(SortedIntView.containsInRange(array, 2, 5, 3)).isFalse();
  }

  @Test
  void toIntArrayEmptyReturnsEmptyArray() {
    assertThat(SortedIntView.EMPTY.toIntArray()).isEmpty();
  }

  @Test
  void toIntArraySubViewReturnsCopyOfRange() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7, 9}, 1, 4);
    assertThat(view.toIntArray()).containsExactly(3, 5, 7);
  }

  @Test
  void toIntArrayReturnsIndependentCopy() {
    int[] backing = {1, 3, 5};
    var view = new SortedIntView(backing);
    int[] copy = view.toIntArray();
    copy[0] = 999;
    assertThat(backing[0]).isEqualTo(1);
  }

  @Test
  void toArrayNullDestAllocatesNewArray() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThat(view.toArray((int[]) null)).containsExactly(1, 3, 5);
  }

  @Test
  void toArrayTooSmallDestAllocatesNewArray() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    int[] dest = new int[2];
    int[] result = view.toArray(dest);
    assertThat(result).isNotSameAs(dest);
    assertThat(result).containsExactly(1, 3, 5);
  }

  @Test
  void toArrayExactSizeDestReusesDest() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    int[] dest = new int[3];
    int[] result = view.toArray(dest);
    assertThat(result).isSameAs(dest);
    assertThat(dest).containsExactly(1, 3, 5);
  }

  @Test
  void toArrayLargerDestReusesDest() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    int[] dest = new int[] {9, 9, 9, 9, 9};
    int[] result = view.toArray(dest);
    assertThat(result).isSameAs(dest);
    assertThat(dest).startsWith(1, 3, 5);
  }

  @Test
  void getElementsCopiesCorrectRange() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7, 9});
    int[] dest = new int[4];
    view.getElements(1, dest, 0, 3);
    assertThat(dest).containsExactly(3, 5, 7, 0);
  }

  @Test
  void getElementsInvalidSrcRangeThrows() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    int[] dest = new int[3];
    assertThatThrownBy(() -> view.getElements(0, dest, 0, 5))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void getElementsInvalidDestRangeThrows() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    int[] dest = new int[2];
    assertThatThrownBy(() -> view.getElements(0, dest, 0, 3))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void forEachVisitsAllInOrder() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7}, 1, 4);
    var collected = new IntArrayList();
    view.forEach((int v) -> collected.add(v));
    assertThat(collected.toIntArray()).containsExactly(3, 5, 7);
  }

  @Test
  void forEachInRangeVisitsAllInOrder() {
    var collected = new IntArrayList();
    SortedIntView.forEachInRange(new int[] {1, 3, 5, 7, 9}, 1, 4, collected::add);
    assertThat(collected.toIntArray()).containsExactly(3, 5, 7);
  }

  @Test
  void listIteratorForwardTraversal() {
    var view = new SortedIntView(new int[] {10, 20, 30});
    var it = view.listIterator(0);
    assertThat(it.hasNext()).isTrue();
    assertThat(it.nextInt()).isEqualTo(10);
    assertThat(it.nextInt()).isEqualTo(20);
    assertThat(it.nextInt()).isEqualTo(30);
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void listIteratorForwardPastEndThrows() {
    var view = new SortedIntView(new int[] {10, 20});
    var it = view.listIterator(2);
    assertThatThrownBy(it::nextInt).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void listIteratorBackwardTraversal() {
    var view = new SortedIntView(new int[] {10, 20, 30});
    var it = view.listIterator(3);
    assertThat(it.hasPrevious()).isTrue();
    assertThat(it.previousInt()).isEqualTo(30);
    assertThat(it.previousInt()).isEqualTo(20);
    assertThat(it.previousInt()).isEqualTo(10);
    assertThat(it.hasPrevious()).isFalse();
  }

  @Test
  void listIteratorBackwardFromStartThrows() {
    var view = new SortedIntView(new int[] {10, 20});
    var it = view.listIterator(0);
    assertThatThrownBy(it::previousInt).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void listIteratorInvalidStartIndexThrows() {
    var view = new SortedIntView(new int[] {10, 20});
    assertThatThrownBy(() -> view.listIterator(-1))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> view.listIterator(3))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void listIteratorReportsPositions() {
    var view = new SortedIntView(new int[] {10, 20, 30});
    var it = view.listIterator(1);
    assertThat(it.nextIndex()).isEqualTo(1);
    assertThat(it.previousIndex()).isEqualTo(0);
    it.nextInt();
    assertThat(it.nextIndex()).isEqualTo(2);
    assertThat(it.previousIndex()).isEqualTo(1);
  }

  @Test
  void spliteratorHasExpectedCharacteristics() {
    var sp = new SortedIntView(new int[] {1, 3, 5}).spliterator();
    assertThat(sp.hasCharacteristics(Spliterator.ORDERED)).isTrue();
    assertThat(sp.hasCharacteristics(Spliterator.SIZED)).isTrue();
    assertThat(sp.hasCharacteristics(Spliterator.SUBSIZED)).isTrue();
    assertThat(sp.hasCharacteristics(Spliterator.DISTINCT)).isTrue();
    assertThat(sp.hasCharacteristics(Spliterator.IMMUTABLE)).isTrue();
    assertThat(sp.hasCharacteristics(Spliterator.NONNULL)).isTrue();
    // SORTED is intentionally not advertised, see spliterator() javadoc.
    assertThat(sp.hasCharacteristics(Spliterator.SORTED)).isFalse();
  }

  @Test
  void spliteratorReportsExactSize() {
    var sp = new SortedIntView(new int[] {1, 3, 5, 7}, 1, 4).spliterator();
    assertThat(sp.estimateSize()).isEqualTo(3);
  }

  @Test
  void spliteratorEnablesStreamSum() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7});
    var sum = new AtomicInteger();
    view.intStream().forEach(sum::addAndGet);
    assertThat(sum.get()).isEqualTo(16);
  }

  @Test
  void subListFullRangeReturnsSameInstance() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThatObject(view.subList(0, 3)).isSameAs(view);
  }

  @Test
  void subListEmptyRangeReturnsEmpty() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThatObject(view.subList(1, 1)).isSameAs(SortedIntView.EMPTY);
  }

  @Test
  void subListPartialRangeCreatesSubView() {
    var view = new SortedIntView(new int[] {1, 3, 5, 7, 9});
    var sub = view.subList(1, 4);
    assertThat(sub.toIntArray()).containsExactly(3, 5, 7);
  }

  @Test
  void subListOutOfBoundsThrows() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThatThrownBy(() -> view.subList(-1, 2))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> view.subList(0, 4))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> view.subList(2, 1))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void viewOfEmptyRangeReturnsEmpty() {
    assertThatObject(SortedIntView.viewOf(new int[] {1, 3, 5}, 2, 2))
        .isSameAs(SortedIntView.EMPTY);
  }

  @Test
  void viewOfNonEmptyRangeCreatesView() {
    var view = SortedIntView.viewOf(new int[] {1, 3, 5, 7}, 1, 3);
    assertThat(view.toIntArray()).containsExactly(3, 5);
  }

  @Test
  void viewOfInvalidRangeThrows() {
    assertThatThrownBy(() -> SortedIntView.viewOf(new int[] {1, 3}, 0, 5))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void cloneReturnsSameInstance() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThatObject(view.clone()).isSameAs(view);
  }

  @Test
  void equalsSameInstanceTrue() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThat(view.equals(view)).isTrue();
  }

  @Test
  void equalsSameContentDifferentBackingArrayTrue() {
    var a = new SortedIntView(new int[] {1, 3, 5});
    var b = new SortedIntView(new int[] {1, 3, 5});
    assertThatObject(a).isEqualTo(b);
  }

  @Test
  void equalsSameContentViaSubViewTrue() {
    var full = new SortedIntView(new int[] {0, 1, 3, 5, 9}, 1, 4);
    var direct = new SortedIntView(new int[] {1, 3, 5});
    assertThatObject(full).isEqualTo(direct);
  }

  @Test
  void equalsDifferentSizeFalse() {
    var a = new SortedIntView(new int[] {1, 3, 5});
    var b = new SortedIntView(new int[] {1, 3});
    assertThatObject(a).isNotEqualTo(b);
  }

  @Test
  void equalsSameSizeDifferentContentFalse() {
    var a = new SortedIntView(new int[] {1, 3, 5});
    var b = new SortedIntView(new int[] {1, 3, 6});
    assertThatObject(a).isNotEqualTo(b);
  }

  @Test
  void equalsWithEquivalentIntListTrue() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    IntList list = new IntArrayList(new int[] {1, 3, 5});
    assertThat(view.equals(list)).isTrue();
    assertThatObject(view).isEqualTo(list);
  }

  @Test
  void equalsWithDifferentIntListFalse() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    IntList list = new IntArrayList(new int[] {1, 3, 6});
    assertThatObject(view).isNotEqualTo(list);
  }

  @Test
  void equalsWithNullFalse() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThat(view.equals(null)).isFalse();
  }

  @Test
  void equalsWithUnrelatedTypeFalse() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    assertThat(view.equals("not a list")).isFalse();
  }

  @Test
  void equalsWithObjectTypedNullFalse() {
    // Statically typing the argument as Object forces dispatch to equals(Object) instead
    // of the more specific equals(SortedIntView) overload, exercising the case-null arm.
    var view = new SortedIntView(new int[] {1, 3, 5});
    Object nullObj = null;
    assertThat(view.equals(nullObj)).isFalse();
  }

  @Test
  void equalsIntListOverloadWithSameReferenceTrue() {
    // Cast to IntList to hit equals(IntList) directly and exercise its self-reference short-circuit.
    var view = new SortedIntView(new int[] {1, 3, 5});
    IntList asIntList = view;
    assertThat(view.equals(asIntList)).isTrue();
  }

  @Test
  void equalsIntListOverloadWithNullFalse() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    IntList nullList = null;
    assertThat(view.equals(nullList)).isFalse();
  }

  @Test
  void equalsIntListOverloadWithDifferentSizeFalse() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    IntList shorter = new IntArrayList(new int[] {1, 3});
    assertThat(view.equals(shorter)).isFalse();
  }

  @Test
  void subListFromZeroToLessThanSizeCreatesSubView() {
    // Exercises the short-circuit path where fromIndex == 0 but toIndex != size(),
    // so the "return this" fast path must not fire.
    var view = new SortedIntView(new int[] {1, 3, 5});
    var sub = view.subList(0, 2);
    assertThatObject(sub).isNotSameAs(view);
    assertThat(sub.toIntArray()).containsExactly(1, 3);
  }

  @Test
  void hashCodeMatchesIntListContract() {
    var view = new SortedIntView(new int[] {1, 3, 5});
    IntList list = new IntArrayList(new int[] {1, 3, 5});
    assertThat(view.hashCode()).isEqualTo(list.hashCode());
  }

  @Test
  void hashCodeConsistentWithEquals() {
    var a = new SortedIntView(new int[] {1, 3, 5, 7, 9}, 1, 4);
    var b = new SortedIntView(new int[] {3, 5, 7});
    assertThatObject(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void unionBothEmptyReturnsEmpty() {
    var result = SortedIntView.EMPTY.union(SortedIntView.EMPTY);
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void unionThisEmptyReturnsOther() {
    var other = new SortedIntView(new int[] {1, 3, 5});
    assertThatObject(SortedIntView.EMPTY.union(other)).isSameAs(other);
  }

  @Test
  void unionOtherEmptyReturnsThis() {
    var self = new SortedIntView(new int[] {1, 3, 5});
    assertThatObject(self.union(SortedIntView.EMPTY)).isSameAs(self);
  }

  @Test
  void unionDisjointRangesMergesAndSorts() {
    var a = new SortedIntView(new int[] {1, 3, 5});
    var b = new SortedIntView(new int[] {2, 4, 6});
    assertThat(a.union(b).toIntArray()).containsExactly(1, 2, 3, 4, 5, 6);
  }

  @Test
  void unionOverlappingDeduplicates() {
    var a = new SortedIntView(new int[] {1, 2, 3, 4});
    var b = new SortedIntView(new int[] {3, 4, 5, 6});
    assertThat(a.union(b).toIntArray()).containsExactly(1, 2, 3, 4, 5, 6);
  }

  @Test
  void unionIdenticalReturnsSameContent() {
    var a = new SortedIntView(new int[] {1, 3, 5});
    var b = new SortedIntView(new int[] {1, 3, 5});
    assertThat(a.union(b).toIntArray()).containsExactly(1, 3, 5);
  }

  @Test
  void unionSubsetReturnsLargerSet() {
    var a = new SortedIntView(new int[] {1, 2, 3, 4, 5});
    var b = new SortedIntView(new int[] {2, 4});
    assertThat(a.union(b).toIntArray()).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  void staticUnionWithOffsetRangesDeduplicates() {
    int[] a = {0, 1, 3, 5, 9};
    int[] b = {0, 3, 4, 5, 9};
    int[] result = SortedIntView.union(a, 1, 4, b, 1, 4);
    assertThat(result).containsExactly(1, 3, 4, 5);
  }
}
