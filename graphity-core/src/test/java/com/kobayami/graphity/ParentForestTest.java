package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ParentForestTest {

  @Test
  void groupIndicesListsEachGroupAscendingAndSkipsNegativeKeys() {
    var groups = ParentForest.groupIndices(new int[] {2, -1, 0, 2, -1, 0, 3}, 5);
    assertThat(groups.start()).containsExactly(0, 2, 2, 4, 5, 5);
    assertThat(groups.members()).containsExactly(2, 5, 0, 3, 6);
  }

  @Test
  void groupIndicesWithoutKeysOrIndices() {
    var noKeys = ParentForest.groupIndices(new int[] {-1, -1}, 0);
    assertThat(noKeys.start()).containsExactly(0);
    assertThat(noKeys.members()).isEmpty();
    var noIndices = ParentForest.groupIndices(new int[0], 3);
    assertThat(noIndices.start()).containsExactly(0, 0, 0, 0);
    assertThat(noIndices.members()).isEmpty();
  }

  @Test
  void groupIndicesMatchesAScanPerKey() {
    var random = new Random(7);
    for (int round = 0; round < 500; round++) {
      int keyCount = random.nextInt(12);
      var keyOf = new int[random.nextInt(40)];
      for (int i = 0; i < keyOf.length; i++) {
        keyOf[i] = random.nextInt(keyCount + 2) - 2;   // -2 and -1 are in no group
      }
      var groups = ParentForest.groupIndices(keyOf, keyCount);
      assertThat(groups.start()).hasSize(keyCount + 1);
      int at = 0;
      for (int key = 0; key < keyCount; key++) {
        assertThat(groups.start()[key]).isEqualTo(at);
        for (int i = 0; i < keyOf.length; i++) {
          if (keyOf[i] == key) assertThat(groups.members()[at++]).isEqualTo(i);
        }
      }
      assertThat(groups.start()[keyCount]).isEqualTo(at);
      assertThat(groups.members()).hasSize(at);
    }
  }

  @Test
  void childrenOfGroupsByParent() {
    var children = ParentForest.childrenOf(new int[] {-1, 0, 0, 1, -1, 4});
    assertThat(children.start()).containsExactly(0, 2, 3, 3, 3, 4, 4);
    assertThat(children.members()).containsExactly(1, 2, 3, 5);
  }

  @Test
  void preorderListsRootsAndChildrenAscendingWithSubtreeSizes() {
    int[] parent = {-1, 0, 0, 1, -1, 4};
    var preorderIndex = new int[6];
    var order = new int[6];
    var subtreeSize = new int[6];
    ParentForest.preorder(parent, preorderIndex, order, subtreeSize);
    assertThat(order).containsExactly(0, 1, 3, 2, 4, 5);
    assertThat(preorderIndex).containsExactly(0, 1, 3, 2, 4, 5);
    assertThat(subtreeSize).containsExactly(4, 2, 1, 1, 2, 1);
  }
}
