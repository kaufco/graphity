package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AncestryTest {

  /**
   * Standard test fixture, a tree of depth 3:
   * <pre>
   *        0
   *      /   \
   *     1     2
   *    / \     \
   *   3   4     5
   *      /
   *     6
   * </pre>
   */
  private static Ancestry tree() {
    return Ancestry.of(TestGraphs.of(7, 0, 1, 0, 2, 1, 3, 1, 4, 2, 5, 4, 6));
  }

  @Test
  void lowestCommonAncestorOfSiblingsIsTheirParent() {
    var a = tree();
    assertThat(a.lowestCommonAncestorOf(3, 4)).isEqualTo(1);
  }

  @Test
  void lowestCommonAncestorAcrossBranchesIsTheRoot() {
    var a = tree();
    assertThat(a.lowestCommonAncestorOf(6, 5)).isZero();
    assertThat(a.lowestCommonAncestorOf(3, 2)).isZero();
  }

  @Test
  void lowestCommonAncestorOfAncestorAndDescendantIsTheAncestor() {
    var a = tree();
    assertThat(a.lowestCommonAncestorOf(1, 6)).isEqualTo(1);
    assertThat(a.lowestCommonAncestorOf(6, 1)).isEqualTo(1);
  }

  @Test
  void lowestCommonAncestorOfNodeWithItselfIsTheNode() {
    var a = tree();
    for (int node = 0; node < 7; node++) {
      assertThat(a.lowestCommonAncestorOf(node, node)).isEqualTo(node);
    }
  }

  @Test
  void lowestCommonAncestorIsSymmetric() {
    var a = tree();
    for (int x = 0; x < 7; x++) {
      for (int y = 0; y < 7; y++) {
        assertThat(a.lowestCommonAncestorOf(x, y)).isEqualTo(a.lowestCommonAncestorOf(y, x));
      }
    }
  }

  @Test
  void lowestCommonAncestorWithTheRootIsTheRoot() {
    var a = tree();
    for (int node = 0; node < 7; node++) {
      assertThat(a.lowestCommonAncestorOf(0, node)).isZero();
    }
  }

  @Test
  void lowestCommonAncestorAcrossTreesIsMinusOne() {
    // Two separate trees: 0 -> 1 -> 2 and 3 -> 4.
    var a = Ancestry.of(TestGraphs.of(5, 0, 1, 1, 2, 3, 4));
    assertThat(a.lowestCommonAncestorOf(2, 4)).isEqualTo(-1);
    assertThat(a.lowestCommonAncestorOf(0, 3)).isEqualTo(-1);
  }

  @Test
  void lowestCommonAncestorOnAPathIsTheShallowerNode() {
    var a = Ancestry.of(TestGraphs.path(64));
    assertThat(a.lowestCommonAncestorOf(10, 40)).isEqualTo(10);
    assertThat(a.lowestCommonAncestorOf(63, 0)).isZero();
  }

  @Test
  void lowestCommonAncestorOnAStarIsTheCenter() {
    var a = Ancestry.of(TestGraphs.outStar(10));
    assertThat(a.lowestCommonAncestorOf(3, 7)).isZero();
  }

  @Test
  void isAncestorOfHoldsAlongTheRootPath() {
    var a = tree();
    assertThat(a.isAncestorOf(0, 6)).isTrue();
    assertThat(a.isAncestorOf(1, 6)).isTrue();
    assertThat(a.isAncestorOf(4, 6)).isTrue();
    assertThat(a.isAncestorOf(6, 6)).isTrue();
    assertThat(a.isAncestorOf(6, 4)).isFalse();
    assertThat(a.isAncestorOf(2, 6)).isFalse();
    assertThat(a.isAncestorOf(3, 4)).isFalse();
  }

  @Test
  void isAncestorOfIsFalseAcrossTrees() {
    var a = Ancestry.of(TestGraphs.of(5, 0, 1, 1, 2, 3, 4));
    assertThat(a.isAncestorOf(0, 4)).isFalse();
  }

  @Test
  void depthOfCountsEdgesFromTheRoot() {
    var a = tree();
    assertThat(a.depthOf(0)).isZero();
    assertThat(a.depthOf(1)).isEqualTo(1);
    assertThat(a.depthOf(5)).isEqualTo(2);
    assertThat(a.depthOf(6)).isEqualTo(3);
  }

  @Test
  void depthOfIsZeroForEveryTreeRoot() {
    var a = Ancestry.of(TestGraphs.of(5, 0, 1, 1, 2, 3, 4));
    assertThat(a.depthOf(0)).isZero();
    assertThat(a.depthOf(3)).isZero();
  }

  @Test
  void parentOfIsMinusOneForEveryTreeRoot() {
    var a = Ancestry.of(TestGraphs.of(5, 0, 1, 1, 2, 3, 4));
    assertThat(a.parentOf(0)).isEqualTo(-1);
    assertThat(a.parentOf(3)).isEqualTo(-1);
    assertThat(a.parentOf(4)).isEqualTo(3);
  }

  @Test
  void parentOfReportsTheTreeParent() {
    var a = tree();
    assertThat(a.parentOf(6)).isEqualTo(4);
    assertThat(a.parentOf(4)).isEqualTo(1);
    assertThat(a.parentOf(1)).isZero();
  }

  @Test
  void ofAcceptsTheEmptyGraph() {
    var a = Ancestry.of(TestGraphs.empty());
    assertThatThrownBy(() -> a.depthOf(0)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void ofAcceptsIsolatedNodes() {
    var a = Ancestry.of(TestGraphs.isolated(3));
    assertThat(a.lowestCommonAncestorOf(0, 0)).isZero();
    assertThat(a.lowestCommonAncestorOf(0, 1)).isEqualTo(-1);
    assertThat(a.depthOf(2)).isZero();
    assertThat(a.parentOf(2)).isEqualTo(-1);
  }

  @Test
  void ofRejectsANodeWithTwoParents() {
    // Diamond: node 3 has parents 1 and 2.
    var diamond = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    assertThatThrownBy(() -> Ancestry.of(diamond))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("node 3");
  }

  @Test
  void ofRejectsACycle() {
    var cycle = TestGraphs.cycle(4);
    assertThatThrownBy(() -> Ancestry.of(cycle))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");
  }

  @Test
  void ofNamesANodeOnTheCycleAndNotOneBelowIt() {
    // Node 0 hangs below the cycle 2 -> 3 -> 2, so it is unreachable from any root as well.
    var cycleWithATail = TestGraphs.of(4, 2, 0, 2, 3, 3, 2);
    assertThatThrownBy(() -> Ancestry.of(cycleWithATail))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching(".*node [23] lies on a cycle.*");
  }

  @Test
  void ofRejectsASelfLoop() {
    // A self-loop makes its node unreachable from any root, just like any other cycle.
    var selfLoop = TestGraphs.of(2, 0, 0, 0, 1);
    assertThatThrownBy(() -> Ancestry.of(selfLoop))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");
  }

  @Test
  void ofAcceptsAnInTreeForestViaTheTranspose() {
    // In-tree: 1 -> 0 and 2 -> 0. Transposed it becomes the out-star rooted at 0.
    var a = Ancestry.of(TestGraphs.inStar(3).transposed());
    assertThat(a.lowestCommonAncestorOf(1, 2)).isZero();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 7, 1000})
  void queriesOutOfRangeThrow(int node) {
    var a = tree();
    assertThatThrownBy(() -> a.lowestCommonAncestorOf(node, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.lowestCommonAncestorOf(0, node))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.isAncestorOf(node, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.depthOf(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> a.parentOf(node))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }
}
