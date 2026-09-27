package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the iterative DFS cores.
 * <p>
 * On very deep graphs (DFS depth = n = 10^6) the internal DFS-based algorithms must not
 * overflow the JVM call stack. The former recursive traverser failed here even with an
 * enlarged {@code -Xss}; the iterative cores keep their stack on the heap.
 * <p>
 * These tests are only meaningful under the default JVM stack size - the {@code -Xss}
 * override was removed from {@code pom.xml} together with this change.
 */
class DeepGraphStackSafetyTest {

  private static final int DEEP = 1_000_000;

  @Test
  void deepPathDoesNotOverflowStack() {
    // 0 -> 1 -> ... -> DEEP-1: a single chain, DFS descends DEEP levels deep.
    Graph path = TestGraphs.path(DEEP);

    assertThatCode(() -> {
      Components.sccsOf(path);
      TopOrder.of(path);
      BackEdges.of(path);
      EdgeTypes.of(path);
      Graphs.isDag(path);
      NodeMapping.reindex(path);
    }).doesNotThrowAnyException();
  }

  @Test
  void deepAncestryDoesNotOverflowStack() {
    // A chain of DEEP nodes is one heavy path; construction and queries must stay iterative.
    Graph path = TestGraphs.path(DEEP);

    Ancestry ancestry = Ancestry.of(path);
    assertThat(ancestry.depthOf(DEEP - 1)).isEqualTo(DEEP - 1);
    assertThat(ancestry.lowestCommonAncestorOf(DEEP - 1, DEEP / 2)).isEqualTo(DEEP / 2);
    assertThat(ancestry.isAncestorOf(0, DEEP - 1)).isTrue();

    PreorderAncestry preorder = PreorderAncestry.of(ancestry);
    assertThat(preorder.subtreeSizeOf(0)).isEqualTo(DEEP);
    assertThat(preorder.preorderIndexOf(DEEP - 1)).isEqualTo(DEEP - 1);
    assertThat(preorder.isAncestorOf(DEEP / 2, DEEP - 1)).isTrue();
  }

  @Test
  void deepBiconnectivityDoesNotOverflowStack() {
    // Every edge of a path is a bridge and its own block, so the traversal descends DEEP levels
    // before the first block closes.
    Graph path = TestGraphs.path(DEEP);

    Biconnectivity biconnectivity = Components.biconnectivityOf(path);
    assertThat(biconnectivity.bridges().count()).isEqualTo(DEEP - 1);
    assertThat(biconnectivity.articulationPoints().size()).isEqualTo(DEEP - 2);
    assertThat(biconnectivity.blocks().groupCount()).isEqualTo(DEEP - 1);
  }

  @Test
  void deepCycleDoesNotOverflowStack() {
    // One giant SCC: DFS descends DEEP levels, then a single back-edge closes the cycle.
    // Stresses the deep sccStack pop (Tarjan) and the deep onStack unwind.
    Graph cycle = TestGraphs.cycle(DEEP);

    Sccs sccs = Components.sccsOf(cycle);
    assertThat(sccs.groupCount()).isEqualTo(1);
    assertThat(sccs.isStronglyConnected()).isTrue();

    assertThatCode(() -> {
      BackEdges.of(cycle);
      EdgeTypes.of(cycle);
      NodeMapping.reindex(cycle);
    }).doesNotThrowAnyException();

    assertThat(Graphs.isDag(cycle)).isFalse();

    // A cycle has no topological order: the documented IllegalArgumentException,
    // never a StackOverflowError.
    assertThatThrownBy(() -> TopOrder.of(cycle)).isInstanceOf(IllegalArgumentException.class);
  }
}
