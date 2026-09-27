package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

class PreorderAncestryTest {

  /**
   * Same fixture as {@code AncestryTest}, a tree of depth 3:
   * <pre>
   *        0
   *      /   \
   *     1     2
   *    / \     \
   *   3   4     5
   *      /
   *     6
   * </pre>
   * Preorder: 0, 1, 3, 4, 6, 2, 5.
   */
  private static PreorderAncestry tree() {
    return PreorderAncestry.of(Ancestry.of(TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 4, 6)));
  }

  @Test
  void preorderVisitsChildrenInAscendingOrder() {
    assertThat(tree().preorder().toIntArray()).containsExactly(0, 1, 3, 4, 6, 2, 5);
  }

  @Test
  void preorderIndexIsThePositionInPreorder() {
    var a = tree();
    for (int i = 0; i < 7; i++) {
      assertThat(a.preorderIndexOf(a.preorder().getInt(i))).isEqualTo(i);
    }
  }

  @Test
  void subtreeSizeCountsTheNodeAndAllDescendants() {
    var a = tree();
    assertThat(a.subtreeSizeOf(0)).isEqualTo(7);
    assertThat(a.subtreeSizeOf(1)).isEqualTo(4);
    assertThat(a.subtreeSizeOf(4)).isEqualTo(2);
    assertThat(a.subtreeSizeOf(5)).isEqualTo(1);
  }

  @Test
  void subtreeStartsWithTheNodeAndFollowsPreorder() {
    var a = tree();
    assertThat(a.subtreeOf(1).toIntArray()).containsExactly(1, 3, 4, 6);
    assertThat(a.subtreeOf(2).toIntArray()).containsExactly(2, 5);
    assertThat(a.subtreeOf(6).toIntArray()).containsExactly(6);
  }

  @Test
  void isAncestorOfIsAnIntervalTest() {
    var a = tree();
    assertThat(a.isAncestorOf(0, 6)).isTrue();
    assertThat(a.isAncestorOf(1, 6)).isTrue();
    assertThat(a.isAncestorOf(4, 6)).isTrue();
    assertThat(a.isAncestorOf(6, 6)).isTrue();
    assertThat(a.isAncestorOf(2, 6)).isFalse();
    assertThat(a.isAncestorOf(6, 4)).isFalse();
    assertThat(a.isAncestorOf(3, 4)).isFalse();
  }

  @Test
  void inheritedQueriesStillWork() {
    var a = tree();
    assertThat(a.lowestCommonAncestorOf(3, 6)).isEqualTo(1);
    assertThat(a.depthOf(6)).isEqualTo(3);
    assertThat(a.parentOf(6)).isEqualTo(4);
  }

  @Test
  void isAncestorOfThroughTheBaseTypeUsesTheIntervalTest() {
    Ancestry a = tree();
    assertThat(a.isAncestorOf(1, 6)).isTrue();
    assertThat(a.isAncestorOf(2, 6)).isFalse();
  }

  @Test
  void forestRootsComeInAscendingOrder() {
    // Two trees, 3 -> {0, 4} and 1 -> 2; the roots are 1 and 3.
    var a = PreorderAncestry.of(Ancestry.of(TestGraphs.of(5, 3, 0, 3, 4, 1, 2)));
    assertThat(a.preorder().toIntArray()).containsExactly(1, 2, 3, 0, 4);
    assertThat(a.isAncestorOf(1, 4)).isFalse();
    assertThat(a.isAncestorOf(3, 4)).isTrue();
  }

  @Test
  void isolatedNodesAreSingletonTrees() {
    var a = PreorderAncestry.of(Ancestry.of(TestGraphs.isolated(3)));
    assertThat(a.preorder().toIntArray()).containsExactly(0, 1, 2);
    assertThat(a.subtreeSizeOf(1)).isEqualTo(1);
    assertThat(a.isAncestorOf(0, 1)).isFalse();
  }

  @Test
  void emptyForest() {
    var a = PreorderAncestry.of(Ancestry.of(TestGraphs.empty()));
    assertThat(a.preorder().toIntArray()).isEmpty();
  }

  @Test
  void ofAPreorderAncestryReturnsItUnchanged() {
    var a = tree();
    assertThat(PreorderAncestry.of(a)).isSameAs(a);
  }

  @Test
  void viewsAreReadOnly() {
    var a = tree();
    assertThatThrownBy(() -> a.preorder().set(0, 1))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> a.subtreeOf(1).removeInt(0))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void outOfRangeNodesAreRejected() {
    var a = tree();
    assertThatThrownBy(() -> a.isAncestorOf(7, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.isAncestorOf(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.subtreeSizeOf(7)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.subtreeOf(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.preorderIndexOf(7)).isInstanceOf(IndexOutOfBoundsException.class);
  }
}
