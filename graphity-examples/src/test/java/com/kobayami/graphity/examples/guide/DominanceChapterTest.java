package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graphs;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Dominance". */
class DominanceChapterTest {

  @Test
  void immediateDominators() {
    FindingDominators.Immediate immediate = FindingDominators.immediate();
    assertThat(immediate.immediateDominatorOfNext()).isEqualTo(2);
    assertThat(immediate.immediateDominatorOfExit()).isEqualTo(1);
    assertThat(immediate.exitReachable()).isTrue();
    // The green blocks of the picture after: next, body, header, entry.
    assertThat(immediate.dominatorsOfNext().toString()).isEqualTo("[4, 2, 1, 0]");
  }

  @Test
  void dominatorTree() {
    FindingDominators.Tree tree = FindingDominators.tree();
    assertThat(Graphs.adjacencyListOf(tree.tree())).isEqualTo("""
        0 -> [1]
        1 -> [2, 5]
        2 -> [3, 4]
        3 -> []
        4 -> []
        5 -> []
        """);
    assertThat(tree.bodyDominatesNext()).isTrue();
    assertThat(tree.thenDominatesNext()).isFalse();
    assertThat(tree.lowestCommonDominator()).isEqualTo(2);
  }

  @Test
  void postDominators() {
    FindingDominators.Post post = FindingDominators.post();
    assertThat(post.joinOfBody()).isEqualTo(4);
    assertThat(post.joinOfHeader()).isEqualTo(5);
    // header -> entry, header -> next, next -> body, next -> then, exit -> header
    assertThat(Graphs.adjacencyListOf(post.tree())).isEqualTo("""
        0 -> []
        1 -> [0, 4]
        2 -> []
        3 -> []
        4 -> [2, 3]
        5 -> [1]
        """);
  }

  @Test
  void twoEntries() {
    FindingDominators.TwoEntries two = FindingDominators.twoEntries();
    assertThat(two.immediateDominatorOfLoad()).isEqualTo(-1);
    assertThat(two.immediateDominatorOfParse()).isEqualTo(3);
    assertThat(two.immediateDominatorOfFormat()).isEqualTo(2);
    // main -> parseArgs, main -> run, run -> save, run -> show, run -> format, load -> parse,
    // load -> read, save -> write
    assertThat(Graphs.adjacencyListOf(two.forest())).isEqualTo("""
        0 -> [1, 2]
        1 -> []
        2 -> [4, 5, 7]
        3 -> [6, 8]
        4 -> [9]
        5 -> []
        6 -> []
        7 -> []
        8 -> []
        9 -> []
        """);
  }
}
