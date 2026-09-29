package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.ControlFlowGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

class DominatorsTest {

  private static int[] immediateDominators(Dominators d, int n) {
    var result = new int[n];
    for (int node = 0; node < n; node++) result[node] = d.immediateDominatorOf(node);
    return result;
  }

  private static BiAdjacentGraph bi(int n, int... edges) {
    return TestGraphs.of(n, edges).biAdjacentGraph();
  }

  @Test
  void diamondJoinsAtTheEntry() {
    // 0 -> {1, 2} -> 3
    var d = Dominators.of(bi(4, 0, 1, 0, 2, 1, 3, 2, 3), 0);
    assertThat(immediateDominators(d, 4)).containsExactly(-1, 0, 0, 0);
  }

  @Test
  void loopHeaderDominatesItsBody() {
    // 0 -> 1 -> 2 -> 1, 2 -> 3
    var d = Dominators.of(bi(4, 0, 1, 1, 2, 2, 1, 2, 3), 0);
    assertThat(immediateDominators(d, 4)).containsExactly(-1, 0, 1, 2);
  }

  @Test
  void irreducibleCycleIsDominatedOnlyByTheEntry() {
    // 0 -> 1, 0 -> 2, 1 <-> 2: a cycle entered at both nodes.
    var d = Dominators.of(bi(3, 0, 1, 0, 2, 1, 2, 2, 1), 0);
    assertThat(immediateDominators(d, 3)).containsExactly(-1, 0, 0);
  }

  @Test
  void textbookExample() {
    // Lengauer and Tarjan's example, nodes R=0, A..L = 1..12.
    int r = 0, a = 1, b = 2, c = 3, dd = 4, e = 5, f = 6, g = 7, h = 8, i = 9, j = 10, k = 11,
        l = 12;
    var graph = bi(13,
        r, a, r, b, r, c, a, dd, b, a, b, dd, b, e, c, f, c, g, dd, l, e, h, f, i, g, i, g, j,
        h, e, h, k, i, k, j, i, k, r, k, i, l, h);
    var d = Dominators.of(graph, r);
    assertThat(immediateDominators(d, 13))
        .containsExactly(-1, r, r, r, r, r, c, c, r, r, g, r, dd);
  }

  @Test
  void severalEntriesShareAVirtualRoot() {
    // Entries 0 and 3 both reach 1; only the virtual root dominates it.
    var d = Dominators.of(bi(4, 0, 1, 3, 1, 1, 2), IntList.of(0, 3));
    assertThat(immediateDominators(d, 4)).containsExactly(-1, -1, 1, -1);
  }

  @Test
  void anEntryReachableFromAnotherEntryIsStillARoot() {
    var d = Dominators.of(bi(3, 0, 1, 1, 2), IntList.of(0, 1));
    assertThat(immediateDominators(d, 3)).containsExactly(-1, -1, 1);
  }

  @Test
  void unreachableNodesHaveNoDominator() {
    var d = Dominators.of(bi(4, 0, 1, 2, 3), 0);
    assertThat(d.isReachable(1)).isTrue();
    assertThat(d.isReachable(2)).isFalse();
    assertThat(d.isReachable(3)).isFalse();
    assertThat(d.immediateDominatorOf(3)).isEqualTo(-1);
  }

  @Test
  void edgesFromUnreachableNodesAreIgnored() {
    // 2 -> 1 comes from a node no entry reaches and must not make 1 a join point.
    var d = Dominators.of(bi(4, 0, 1, 1, 3, 2, 1, 2, 3), 0);
    assertThat(immediateDominators(d, 4)).containsExactly(-1, 0, -1, 1);
  }

  @Test
  void selfLoopsChangeNothing() {
    var d = Dominators.of(bi(2, 0, 0, 0, 1, 1, 1), 0);
    assertThat(immediateDominators(d, 2)).containsExactly(-1, 0);
  }

  @Test
  void emptyEntryListLeavesEverythingUnreachable() {
    var d = Dominators.of(bi(2, 0, 1), IntList.of());
    assertThat(d.isReachable(0)).isFalse();
    assertThat(d.isReachable(1)).isFalse();
  }

  @Test
  void duplicateEntriesAreIgnored() {
    var graph = bi(3, 0, 1, 1, 2);
    var d = Dominators.of(graph, IntList.of(0, 0, 0));
    assertThat(immediateDominators(d, 3)).containsExactly(-1, 0, 1);
  }

  @Test
  void emptyGraph() {
    var d = Dominators.of(TestGraphs.empty().biAdjacentGraph(), IntList.of());
    assertThat(Graphs.dominatorTree(d).nodeCount()).isZero();
    assertThat(Graphs.dominanceFrontiers(d).nodeCount()).isZero();
  }

  @Test
  void outOfRangeArgumentsAreRejected() {
    var graph = bi(2, 0, 1);
    assertThatThrownBy(() -> Dominators.of(graph, 2)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> Dominators.of(graph, IntList.of(0, -1)))
        .isInstanceOf(IndexOutOfBoundsException.class);
    var d = Dominators.of(graph, 0);
    assertThatThrownBy(() -> d.immediateDominatorOf(2))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> d.isReachable(-1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void dominatorTreeHasAnEdgeFromEachImmediateDominator() {
    var d = Dominators.of(bi(4, 0, 1, 1, 2, 2, 1, 2, 3), 0);
    var tree = Graphs.dominatorTree(d);
    assertThat(tree.edgeCount()).isEqualTo(3);
    assertThat(tree.hasOutNode(0, 1)).isTrue();
    assertThat(tree.hasOutNode(1, 2)).isTrue();
    assertThat(tree.hasOutNode(2, 3)).isTrue();
    assertThat(Graphs.isTree(tree)).isTrue();
  }

  @Test
  void dominanceFrontiersOfALoop() {
    // 0 -> 1 -> 2 -> 1, 2 -> 3: the loop header is in its own frontier and in its body's.
    var d = Dominators.of(bi(4, 0, 1, 1, 2, 2, 1, 2, 3), 0);
    var df = Graphs.dominanceFrontiers(d);
    assertThat(df.outNodes(0).toIntArray()).isEmpty();
    assertThat(df.outNodes(1).toIntArray()).containsExactly(1);
    assertThat(df.outNodes(2).toIntArray()).containsExactly(1);
    assertThat(df.outNodes(3).toIntArray()).isEmpty();
  }

  @Test
  void dominanceFrontiersOfADiamond() {
    var d = Dominators.of(bi(4, 0, 1, 0, 2, 1, 3, 2, 3), 0);
    var df = Graphs.dominanceFrontiers(d);
    assertThat(df.outNodes(1).toIntArray()).containsExactly(3);
    assertThat(df.outNodes(2).toIntArray()).containsExactly(3);
    assertThat(df.edgeCount()).isEqualTo(2);
  }

  @Test
  void ladderFrontierGrowsQuadratically() {
    int rungs = 50;
    var df = Graphs.dominanceFrontiers(
        Dominators.of(ControlFlowGraphs.ladder(rungs).biAdjacentGraph(), 0));
    // b_i (= rungs + i) is in the frontier of every a_j with 1 <= j <= i, plus the exit.
    assertThat(df.edgeCount()).isGreaterThan(rungs * (rungs - 1) / 2);
  }

  @Test
  void postDominatorsAndControlDependence() {
    // if (0) { 1 } ; 2   -  0 -> 1, 0 -> 2, 1 -> 2, exit 2.
    var cfg = bi(3, 0, 1, 0, 2, 1, 2);
    var postDominators = Dominators.of(cfg.transposed(), 2);
    assertThat(postDominators.immediateDominatorOf(0)).isEqualTo(2);
    assertThat(postDominators.immediateDominatorOf(1)).isEqualTo(2);
    var controlDependence = Graphs.dominanceFrontiers(postDominators).transposed();
    assertThat(controlDependence.edgeCount()).isEqualTo(1);
    assertThat(controlDependence.hasOutNode(0, 1)).isTrue();
  }

  @Test
  void ancestryOfDominatorsAnswersDominance() {
    var d = Dominators.of(bi(5, 0, 1, 1, 2, 2, 1, 2, 3, 0, 4, 4, 3), 0);
    var index = Ancestry.of(d);
    assertThat(index.isAncestorOf(1, 2)).isTrue();
    assertThat(index.isAncestorOf(0, 3)).isTrue();
    assertThat(index.isAncestorOf(1, 3)).isFalse();
    assertThat(index.lowestCommonAncestorOf(2, 4)).isZero();
    assertThat(index.depthOf(2)).isEqualTo(2);
  }

  @Test
  void nearestCommonDominatorInADag() {
    // Roots 0 and 5; 0 -> 1 -> {2, 3}, 5 -> 3. The deepest node on every path to 2 and 3 from
    // a root: none, because 3 is also reached from 5.
    var dag = bi(6, 0, 1, 1, 2, 1, 3, 5, 3);
    var index = Ancestry.of(Dominators.of(dag, IntList.of(0, 5)));
    assertThat(index.lowestCommonAncestorOf(2, 3)).isEqualTo(-1);
    var single = Ancestry.of(Dominators.of(dag, 0));
    assertThat(single.lowestCommonAncestorOf(2, 3)).isEqualTo(1);
  }

  @Test
  void fanBehindAChainIsDominatedByTheEntry() {
    var graph = ControlFlowGraphs.fanBehindChain(1000, 1000).biAdjacentGraph();
    var d = Dominators.of(graph, 0);
    for (int fan = 1001; fan <= 2000; fan++) {
      assertThat(d.immediateDominatorOf(fan)).isZero();
    }
    assertThat(d.immediateDominatorOf(1000)).isEqualTo(999);
  }
}
