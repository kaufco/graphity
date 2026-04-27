package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link Components} factory: {@code sccsOf}, {@code wccsOf}, {@code ccsOf}.
 * Inherited {@link NodePartition} behavior (groupCount / get / nodesOf / sizeOf / condense
 * structural correctness) is covered in {@link NodePartitionTest}; Sccs-specific methods
 * (containsCycle, isStronglyConnected) are covered in {@link SccsTest}. Here we verify:
 * <ul>
 *   <li>The factory builds the right partition for a given graph.</li>
 *   <li>SCC IDs follow the documented reverse-topological-order guarantee.</li>
 * </ul>
 */
class ComponentsTest {

  @Test
  void sccsOfEmptyGraphHasNoGroups() {
    var sccs = Components.sccsOf(TestGraphs.empty());
    assertThat(sccs.groupCount()).isZero();
  }

  @Test
  void sccsOfIsolatedNodesAreEachTheirOwnScc() {
    var sccs = Components.sccsOf(TestGraphs.isolated(4));
    assertThat(sccs.groupCount()).isEqualTo(4);
  }

  @Test
  void sccsOfPureCycleIsSingleScc() {
    var sccs = Components.sccsOf(TestGraphs.cycle(4));
    assertThat(sccs.groupCount()).isEqualTo(1);
    assertThat(sccs.nodesOf(0).toIntArray()).containsExactly(0, 1, 2, 3);
  }

  @Test
  void sccsOfDagHasOneSccPerNode() {
    // Diamond DAG 0->1, 0->2, 1->3, 2->3.
    var sccs = Components.sccsOf(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3));
    assertThat(sccs.groupCount()).isEqualTo(4);
    // Each node in its own SCC.
    for (int i = 0; i < 4; i++) {
      assertThat(sccs.sizeOf(sccs.get(i))).isEqualTo(1);
    }
  }

  @Test
  void sccsOfSelfLoopedNodeFormsSingletonScc() {
    var sccs = Components.sccsOf(TestGraphs.of(1, 0, 0));
    assertThat(sccs.groupCount()).isEqualTo(1);
    assertThat(sccs.nodesOf(0).toIntArray()).containsExactly(0);
  }

  @Test
  void sccsOfTwoDisjointSccsFindsBoth() {
    // Two disjoint cycles: {0,1,2} and {3,4,5}.
    var g = TestGraphs.of(6,
        0, 1, 1, 2, 2, 0,
        3, 4, 4, 5, 5, 3);
    var sccs = Components.sccsOf(g);
    assertThat(sccs.groupCount()).isEqualTo(2);
    assertThat(sccs.sizeOf(0)).isEqualTo(3);
    assertThat(sccs.sizeOf(1)).isEqualTo(3);
    assertThat(sccs.get(0)).isEqualTo(sccs.get(1)).isEqualTo(sccs.get(2));
    assertThat(sccs.get(3)).isEqualTo(sccs.get(4)).isEqualTo(sccs.get(5));
    assertThat(sccs.get(0)).isNotEqualTo(sccs.get(3));
  }

  @Test
  void sccsOfChainOfSingletonsAssignsReverseTopoIds() {
    // 0 -> 1 -> 2 -> 3: four singleton SCCs, linear component DAG.
    // Reverse-topo order: sink first. SCC of node 3 gets ID 0, node 2 → 1, node 1 → 2, node 0 → 3.
    var sccs = Components.sccsOf(TestGraphs.path(4));
    assertThat(sccs.get(3)).isZero();
    assertThat(sccs.get(2)).isEqualTo(1);
    assertThat(sccs.get(1)).isEqualTo(2);
    assertThat(sccs.get(0)).isEqualTo(3);
  }

  @Test
  void sccsOfTextbookGraphAssignsIdsInReverseTopoOrder() {
    // Textbook SCC example:
    //   0 -> 1
    //   1 -> 2, 1 -> 4
    //   2 -> 3, 3 -> 2       (cycle {2, 3})
    //   4 -> 5, 5 -> 6, 6 -> 4  (cycle {4, 5, 6})
    var g = TestGraphs.of(7,
        0, 1,
        1, 2, 1, 4,
        2, 3, 3, 2,
        4, 5, 5, 6, 6, 4);
    var sccs = Components.sccsOf(g);
    assertThat(sccs.groupCount()).isEqualTo(4);
    // Every cross-SCC edge must point from a higher SCC ID to a lower one.
    assertReverseTopoInvariant(g, sccs);
  }

  @Test
  void sccsOfMixedGraphMaintainsReverseTopoInvariant() {
    // A mix: cycle {0,1}, DAG 2 -> 0, 2 -> 3, 3 -> 4, cycle {5,6,7}, 4 -> 5.
    var g = TestGraphs.of(8,
        0, 1, 1, 0,
        2, 0, 2, 3,
        3, 4,
        4, 5,
        5, 6, 6, 7, 7, 5);
    var sccs = Components.sccsOf(g);
    assertThat(sccs.groupCount()).isEqualTo(5);
    assertReverseTopoInvariant(g, sccs);
  }

  @Test
  void sccsOfCondenseIsDag() {
    // Cyclic graph with two SCCs connected by bridge edge.
    var g = TestGraphs.of(4,
        0, 1, 1, 0,
        1, 2,
        2, 3, 3, 2);
    var sccs = Components.sccsOf(g);
    var condensed = sccs.condense();
    assertThat(condensed.nodeCount()).isEqualTo(sccs.groupCount());
    assertThat(Graphs.isDag(condensed)).isTrue();
  }

  @Test
  void wccsOfEmptyGraphHasNoGroups() {
    var wccs = Components.wccsOf(TestGraphs.empty());
    assertThat(wccs.groupCount()).isZero();
  }

  @Test
  void wccsOfIsolatedNodesAreEachOwnWcc() {
    var wccs = Components.wccsOf(TestGraphs.isolated(4));
    assertThat(wccs.groupCount()).isEqualTo(4);
  }

  @Test
  void wccsOfConnectedChainIsSingleWcc() {
    var wccs = Components.wccsOf(TestGraphs.path(5));
    assertThat(wccs.groupCount()).isEqualTo(1);
    assertThat(wccs.nodesOf(0).toIntArray()).containsExactly(0, 1, 2, 3, 4);
  }

  @Test
  void wccsOfIgnoresEdgeDirection() {
    // 0 -> 1, 2 -> 1: WCC merges {0, 1, 2}; SCCs would be three singletons.
    var g = TestGraphs.of(3, 0, 1, 2, 1);
    var wccs = Components.wccsOf(g);
    assertThat(wccs.groupCount()).isEqualTo(1);
    assertThat(wccs.nodesOf(0).toIntArray()).containsExactly(0, 1, 2);
  }

  @Test
  void wccsOfTwoDisjointComponents() {
    // {0, 1, 2} via 0->1, 1->2; {3, 4} via 3->4.
    var g = TestGraphs.of(5, 0, 1, 1, 2, 3, 4);
    var wccs = Components.wccsOf(g);
    assertThat(wccs.groupCount()).isEqualTo(2);
    int g1 = wccs.get(0);
    int g2 = wccs.get(3);
    assertThat(g1).isNotEqualTo(g2);
    assertThat(wccs.nodesOf(g1).toIntArray()).containsExactly(0, 1, 2);
    assertThat(wccs.nodesOf(g2).toIntArray()).containsExactly(3, 4);
  }

  @Test
  void wccsOfSelfLoopStandsAlone() {
    // {0} with self-loop, {1, 2} via 1->2. Two WCCs.
    var g = TestGraphs.of(3, 0, 0, 1, 2);
    var wccs = Components.wccsOf(g);
    assertThat(wccs.groupCount()).isEqualTo(2);
  }

  @Test
  void ccsOfMatchesWccsOf() {
    var g = TestGraphs.of(5, 0, 1, 1, 2, 3, 4);
    var ccs = Components.ccsOf(g);
    var wccs = Components.wccsOf(g);
    assertThat(ccs.groupCount()).isEqualTo(wccs.groupCount());
    for (int i = 0; i < g.nodeCount(); i++) {
      assertThat(ccs.get(i)).isEqualTo(wccs.get(i));
    }
  }

  /**
   * Verifies the Sccs API guarantee: for every edge u -> v in the original graph
   * where u and v are in different SCCs, {@code sccs.get(u) > sccs.get(v)}.
   */
  private static void assertReverseTopoInvariant(Graph graph, Sccs sccs) {
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        int suId = sccs.get(u);
        int svId = sccs.get(v);
        if (suId != svId) {
          assertThat(suId)
              .as("cross-SCC edge %d -> %d: sccs.get(%d)=%d must be > sccs.get(%d)=%d", u, v, u, suId, v, svId)
              .isGreaterThan(svId);
        }
      }
    }
  }
}
