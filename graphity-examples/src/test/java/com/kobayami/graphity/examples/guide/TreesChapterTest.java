package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graphs;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Trees and hierarchies". */
class TreesChapterTest {

  @Test
  void shapes() {
    CheckingTrees.Shapes shapes = CheckingTrees.shapes();
    assertThat(shapes.familyTree()).isTrue();
    assertThat(shapes.familyPath()).isFalse();
    assertThat(Graphs.adjacencyListOf(shapes.line())).isEqualTo("""
        0 -> [1]
        1 -> [2]
        2 -> [3]
        3 -> []
        """);
    assertThat(shapes.lineTree()).isTrue();
    assertThat(shapes.linePath()).isTrue();
    assertThat(shapes.dependenciesForest()).isFalse();
  }

  @Test
  void parentsAndGenerations() {
    ReadingAncestry.Parents parents = ReadingAncestry.parents();
    assertThat(parents.parentOfGreta()).isEqualTo(3);
    assertThat(parents.generationOfGreta()).isEqualTo(3);
    assertThat(parents.parentOfAda()).isEqualTo(-1);
    assertThat(parents.generations()).containsExactly(0, 1, 1, 2, 2, 2, 3, 3, 3, 3);
  }

  @Test
  void lowestCommonAncestors() {
    ReadingAncestry.Common common = ReadingAncestry.common();
    assertThat(common.gretaAndIda()).isEqualTo(0);
    assertThat(common.gretaAndEmil()).isEqualTo(1);
    assertThat(common.hugoAndGreta()).isEqualTo(3);
    assertThat(common.doraAndHugo()).isEqualTo(3);
  }

  @Test
  void ancestorTests() {
    ReadingAncestry.Tests tests = ReadingAncestry.tests();
    assertThat(tests.benIsAncestor()).isTrue();
    assertThat(tests.cleoIsAncestor()).isFalse();
    assertThat(tests.hugoIsAncestor()).isTrue();
    assertThat(tests.fastBenIsAncestor()).isTrue();
  }

  @Test
  void subtrees() {
    ReadingAncestry.Subtrees subtrees = ReadingAncestry.subtrees();
    assertThat(subtrees.subtreeOfBen().toString()).isEqualTo("[1, 3, 6, 7, 4]");
    assertThat(subtrees.subtreeSizeOfBen()).isEqualTo(5);
    assertThat(subtrees.preorder().toString()).isEqualTo("[0, 1, 3, 6, 7, 4, 2, 5, 8, 9]");
    assertThat(subtrees.positionOfCleo()).isEqualTo(6);
  }
}
