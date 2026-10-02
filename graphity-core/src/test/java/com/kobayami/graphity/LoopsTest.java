package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

class LoopsTest {

  private static BiAdjacentGraph bi(int n, int... edges) {
    return TestGraphs.of(n, edges).biAdjacentGraph();
  }

  @Test
  void naturalLoop() {
    // 0 -> 1 -> 2 -> 1, 2 -> 3
    var loops = Loops.of(bi(4, 0, 1, 1, 2, 2, 1, 2, 3), 0);
    assertThat(loops.loopCount()).isEqualTo(1);
    assertThat(loops.isReducible()).isTrue();
    assertThat(loops.headerOf(0)).isEqualTo(1);
    assertThat(loops.nodesOf(0).toIntArray()).containsExactly(1, 2);
    assertThat(loops.entriesOf(0).toIntArray()).containsExactly(1);
    assertThat(loops.parentOf(0)).isEqualTo(-1);
    assertThat(loops.innermostLoopOf(0)).isEqualTo(-1);
    assertThat(loops.innermostLoopOf(2)).isZero();
    assertThat(loops.innermostLoopOf(3)).isEqualTo(-1);
    assertThat(loops.loopDepthOf(2)).isEqualTo(1);
    assertThat(loops.loopDepthOf(3)).isZero();
  }

  @Test
  void nestedLoops() {
    // Outer loop 1..4 (back edge 4 -> 1), inner loop 2..3 (back edge 3 -> 2), exit 5.
    var loops = Loops.of(bi(6, 0, 1, 1, 2, 2, 3, 3, 2, 3, 4, 4, 1, 4, 5), 0);
    assertThat(loops.loopCount()).isEqualTo(2);
    assertThat(loops.headerOf(0)).isEqualTo(1);
    assertThat(loops.headerOf(1)).isEqualTo(2);
    assertThat(loops.parentOf(1)).isZero();
    assertThat(loops.nodesOf(0).toIntArray()).containsExactly(1, 4, 2, 3);
    assertThat(loops.nodesOf(1).toIntArray()).containsExactly(2, 3);
    assertThat(loops.isInLoop(3, 0)).isTrue();
    assertThat(loops.isInLoop(4, 1)).isFalse();
    assertThat(loops.loopDepthOf(3)).isEqualTo(2);
    assertThat(loops.loopDepthOf(4)).isEqualTo(1);
    assertThat(loops.isReducible()).isTrue();
  }

  @Test
  void irreducibleCycleIsOneLoopWithTwoEntries() {
    // 0 -> 1, 0 -> 2, 1 <-> 2. The search reaches 1 first, so 1 is the header.
    var loops = Loops.of(bi(3, 0, 1, 0, 2, 1, 2, 2, 1), 0);
    assertThat(loops.loopCount()).isEqualTo(1);
    assertThat(loops.headerOf(0)).isEqualTo(1);
    assertThat(loops.entriesOf(0).toIntArray()).containsExactly(1, 2);
    assertThat(loops.isReducible()).isFalse();
    assertThat(loops.loopDepthOf(1)).isEqualTo(1);
    assertThat(loops.loopDepthOf(2)).isEqualTo(1);
  }

  @Test
  void irreducibleLoopNestedInAReducibleOne() {
    // Outer loop headed by 1 (back edge 4 -> 1) around the cycle 2 <-> 3, entered at 2 and 3.
    var loops = Loops.of(bi(6, 0, 1, 1, 2, 1, 3, 2, 3, 3, 2, 3, 4, 4, 1, 4, 5), 0);
    assertThat(loops.loopCount()).isEqualTo(2);
    assertThat(loops.entriesOf(0).toIntArray()).containsExactly(1);
    assertThat(loops.entriesOf(1).toIntArray()).containsExactly(2, 3);
    assertThat(loops.isReducible()).isFalse();
  }

  @Test
  void selfLoopIsALoopOfItsOwn() {
    var loops = Loops.of(bi(3, 0, 1, 1, 1, 1, 2), 0);
    assertThat(loops.loopCount()).isEqualTo(1);
    assertThat(loops.headerOf(0)).isEqualTo(1);
    assertThat(loops.nodesOf(0).toIntArray()).containsExactly(1);
    assertThat(loops.entriesOf(0).toIntArray()).containsExactly(1);
  }

  @Test
  void graphEntriesCountAsLoopEntries() {
    // Entries 1 and 2 on the cycle 1 <-> 2: two entries, irreducible.
    var loops = Loops.of(bi(3, 1, 2, 2, 1), IntList.of(1, 2));
    assertThat(loops.loopCount()).isEqualTo(1);
    assertThat(loops.headerOf(0)).isEqualTo(1);
    assertThat(loops.entriesOf(0).toIntArray()).containsExactly(1, 2);
    assertThat(loops.isReducible()).isFalse();
    // With 1 as the only entry, the same cycle is a natural loop.
    assertThat(Loops.of(bi(3, 1, 2, 2, 1), 1).isReducible()).isTrue();
  }

  @Test
  void entryOrderDecidesTheHeaderOfAnIrreducibleLoop() {
    var graph = bi(3, 1, 2, 2, 1);
    assertThat(Loops.of(graph, IntList.of(2, 1)).headerOf(0)).isEqualTo(2);
  }

  @Test
  void unreachableCyclesAreNoLoops() {
    var loops = Loops.of(bi(4, 0, 1, 2, 3, 3, 2), 0);
    assertThat(loops.loopCount()).isZero();
    assertThat(loops.isReducible()).isTrue();
    assertThat(loops.innermostLoopOf(2)).isEqualTo(-1);
  }

  @Test
  void edgesFromUnreachableNodesEnterNoLoop() {
    // 3 -> 2 comes from a node no entry reaches and must not make 2 a second entry.
    var loops = Loops.of(bi(4, 0, 1, 1, 2, 2, 1, 3, 2), 0);
    assertThat(loops.loopCount()).isEqualTo(1);
    assertThat(loops.entriesOf(0).toIntArray()).containsExactly(1);
    assertThat(loops.isReducible()).isTrue();
  }

  @Test
  void emptyEntryListAndEmptyGraph() {
    assertThat(Loops.of(bi(2, 0, 1, 1, 0), IntList.of()).loopCount()).isZero();
    assertThat(Loops.of(TestGraphs.empty().biAdjacentGraph(), IntList.of()).loopCount()).isZero();
  }

  @Test
  void siblingLoopsShareAParent() {
    // Outer loop 1..5 (back edge 5 -> 1) around two sibling loops, 2 <-> 3 and 4 <-> 5:
    // 1 -> 2 -> 3 -> 2, 3 -> 4 -> 5 -> 4, 5 -> 1, 5 -> 6.
    var loops = Loops.of(bi(7, 0, 1, 1, 2, 2, 3, 3, 2, 3, 4, 4, 5, 5, 4, 5, 1, 5, 6), 0);
    assertThat(loops.loopCount()).isEqualTo(3);
    assertThat(loops.headerOf(0)).isEqualTo(1);
    assertThat(loops.parentOf(1)).isZero();
    assertThat(loops.parentOf(2)).isZero();
    assertThat(loops.nodesOf(0).toIntArray()).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  void viewsAreReadOnly() {
    var loops = Loops.of(bi(3, 0, 1, 1, 2, 2, 1), 0);
    assertThatThrownBy(() -> loops.nodesOf(0).set(0, 1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void outOfRangeArgumentsAreRejected() {
    var loops = Loops.of(bi(3, 0, 1, 1, 2, 2, 1), 0);
    assertThatThrownBy(() -> Loops.of(bi(2, 0, 1), 2))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> loops.innermostLoopOf(3))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> loops.isInLoop(0, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> loops.headerOf(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> loops.entriesOf(1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> loops.parentOf(1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> loops.nodesOf(1)).isInstanceOf(IndexOutOfBoundsException.class);
  }
}
