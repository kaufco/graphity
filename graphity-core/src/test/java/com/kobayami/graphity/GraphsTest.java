package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import org.junit.jupiter.api.Test;

class GraphsTest {

  // ---- Shape checks ----

  @Test
  void isDagTrueForEmptyGraph() {
    assertThat(Graphs.isDag(TestGraphs.empty())).isTrue();
  }

  @Test
  void isDagTrueForIsolatedNodes() {
    assertThat(Graphs.isDag(TestGraphs.isolated(5))).isTrue();
  }

  @Test
  void isDagTrueForPath() {
    assertThat(Graphs.isDag(TestGraphs.path(10))).isTrue();
  }

  @Test
  void isDagTrueForDiamond() {
    assertThat(Graphs.isDag(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3))).isTrue();
  }

  @Test
  void isDagFalseForCycle() {
    assertThat(Graphs.isDag(TestGraphs.cycle(3))).isFalse();
  }

  @Test
  void isDagFalseForSelfLoop() {
    assertThat(Graphs.isDag(TestGraphs.of(1, 0, 0))).isFalse();
  }

  @Test
  void isDagFalseForCycleBetweenOtherwiseDagComponents() {
    // DAG 0->1 plus cycle 2->3->2.
    assertThat(Graphs.isDag(TestGraphs.of(4, 0, 1, 2, 3, 3, 2))).isFalse();
  }

  @Test
  void isForestTrueForEmptyGraph() {
    assertThat(Graphs.isForest(TestGraphs.empty())).isTrue();
  }

  @Test
  void isForestTrueForIsolatedNodes() {
    assertThat(Graphs.isForest(TestGraphs.isolated(3))).isTrue();
  }

  @Test
  void isForestTrueForOutStar() {
    assertThat(Graphs.isForest(TestGraphs.outStar(5))).isTrue();
  }

  @Test
  void isForestTrueForPath() {
    assertThat(Graphs.isForest(TestGraphs.path(5))).isTrue();
  }

  @Test
  void isForestTrueForDisjointOutTrees() {
    // Two disjoint out-trees: {0->1, 0->2} and {3->4}.
    assertThat(Graphs.isForest(TestGraphs.of(5, 0, 1, 0, 2, 3, 4))).isTrue();
  }

  @Test
  void isForestFalseForDiamond() {
    // Diamond: node 3 has in-degree 2.
    assertThat(Graphs.isForest(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3))).isFalse();
  }

  @Test
  void isForestFalseForInStar() {
    // In-star: center node 0 has in-degree n-1.
    assertThat(Graphs.isForest(TestGraphs.inStar(4))).isFalse();
  }

  @Test
  void isOutTreeTrueForSingleNode() {
    assertThat(Graphs.isOutTree(TestGraphs.isolated(1))).isTrue();
  }

  @Test
  void isOutTreeTrueForPath() {
    assertThat(Graphs.isOutTree(TestGraphs.path(5))).isTrue();
  }

  @Test
  void isOutTreeTrueForOutStar() {
    assertThat(Graphs.isOutTree(TestGraphs.outStar(5))).isTrue();
  }

  @Test
  void isOutTreeFalseForDisjointOutTrees() {
    // Two roots: {0->1} and {2->3}.
    assertThat(Graphs.isOutTree(TestGraphs.of(4, 0, 1, 2, 3))).isFalse();
  }

  @Test
  void isOutTreeFalseForIsolatedMultipleNodes() {
    assertThat(Graphs.isOutTree(TestGraphs.isolated(3))).isFalse();
  }

  @Test
  void isPathTrueForEmptyGraph() {
    assertThat(Graphs.isPath(TestGraphs.empty())).isTrue();
  }

  @Test
  void isPathTrueForSingleNode() {
    assertThat(Graphs.isPath(TestGraphs.isolated(1))).isTrue();
  }

  @Test
  void isPathTrueForChain() {
    assertThat(Graphs.isPath(TestGraphs.path(5))).isTrue();
  }

  @Test
  void isPathFalseForOutStar() {
    // Center has out-degree 4 > 1.
    assertThat(Graphs.isPath(TestGraphs.outStar(5))).isFalse();
  }

  @Test
  void shapeHierarchyConsistencyForCycle() {
    // Cycle is none of DAG/forest/out-tree/path.
    var g = TestGraphs.cycle(3);
    assertThat(Graphs.isDag(g)).isFalse();
    assertThat(Graphs.isForest(g)).isFalse();
    assertThat(Graphs.isOutTree(g)).isFalse();
    assertThat(Graphs.isPath(g)).isFalse();
  }

  @Test
  void shapeHierarchyConsistencyForPath() {
    // Path is DAG, forest, out-tree, and path.
    var g = TestGraphs.path(4);
    assertThat(Graphs.isDag(g)).isTrue();
    assertThat(Graphs.isForest(g)).isTrue();
    assertThat(Graphs.isOutTree(g)).isTrue();
    assertThat(Graphs.isPath(g)).isTrue();
  }

  // ---- remapped ----

  @Test
  void remappedWithIdentityYieldsIsomorphicGraph() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    IntList identity = new IntArrayList(new int[] {0, 1, 2, 3});
    var remapped = Graphs.remapped(g, NodeMapping.forPermutation(identity));
    assertThat(remapped).isEqualTo(g);
  }

  @Test
  void remappedWithPermutationPreservesEdgeCount() {
    var g = TestGraphs.path(5);
    IntList perm = new IntArrayList(new int[] {4, 3, 2, 1, 0});
    var remapped = Graphs.remapped(g, NodeMapping.forPermutation(perm));
    assertThat(remapped.nodeCount()).isEqualTo(g.nodeCount());
    assertThat(remapped.edgeCount()).isEqualTo(g.edgeCount());
  }

  @Test
  void remappedWithReversePermutationReversesPath() {
    // Original path: 0 -> 1 -> 2. After permutation {2, 1, 0}, new node 0 is old node 2 etc.
    var g = TestGraphs.path(3);
    IntList perm = new IntArrayList(new int[] {2, 1, 0});
    var remapped = Graphs.remapped(g, NodeMapping.forPermutation(perm));
    // New edges: old 0->1 becomes new 2->1, old 1->2 becomes new 1->0.
    assertThat(remapped.outNodes(1).toIntArray()).containsExactly(0);
    assertThat(remapped.outNodes(2).toIntArray()).containsExactly(1);
    assertThat(remapped.outNodes(0).toIntArray()).isEmpty();
  }

  @Test
  void remappedWithSubgraphDropsEdgesToExcludedNodes() {
    // Original: 0 -> 1, 1 -> 2, 2 -> 3. Select {0, 1, 3}: edge 1->2 is dropped; edge 2->3 is dropped (source excluded).
    var g = TestGraphs.path(4);
    var selected = new BitSet();
    selected.set(0);
    selected.set(1);
    selected.set(3);
    var sub = Graphs.remapped(g, NodeMapping.forSubgraph(selected));
    assertThat(sub.nodeCount()).isEqualTo(3);
    // Only edge is the old 0->1, mapped to new 0->1.
    assertThat(sub.edgeCount()).isEqualTo(1);
    assertThat(sub.outNodes(0).toIntArray()).containsExactly(1);
  }

  @Test
  void remappedSubgraphWithTargetsBeyondSelectionRange() {
    // Path 0 -> 1 -> 2 -> 3. Select {0, 1}: max selected old index = 1, so the
    // inverse mapping's length is 2. Edge 1 -> 2 has oldTarget = 2 which is >= 2,
    // triggering the bounds-check in remapped. The edge must be dropped.
    var g = TestGraphs.path(4);
    var selected = new BitSet();
    selected.set(0);
    selected.set(1);
    var sub = Graphs.remapped(g, NodeMapping.forSubgraph(selected));
    assertThat(sub.nodeCount()).isEqualTo(2);
    assertThat(sub.edgeCount()).isEqualTo(1);
    assertThat(sub.outNodes(0).toIntArray()).containsExactly(1);
    assertThat(sub.outNodes(1).toIntArray()).isEmpty();
  }

  @Test
  void remappedEmptyMappingReturnsEmptyGraph() {
    var g = TestGraphs.path(3);
    var sub = Graphs.remapped(g, NodeMapping.forSubgraph(new BitSet()));
    assertThat(sub.nodeCount()).isZero();
    assertThat(sub.edgeCount()).isZero();
  }

  @Test
  void remappedPreservesSortedAdjacencyLists() {
    // Permute such that old adjacency ordering does NOT translate to sorted new ordering.
    // Original: 0 -> 1, 0 -> 2, 0 -> 3. Permutation {3, 1, 2, 0} (new 0 = old 3, new 1 = old 1, new 2 = old 2, new 3 = old 0).
    // Old 0's outs (1, 2, 3) map via inverse to new (1, 2, 0). After sorting: (0, 1, 2).
    var g = TestGraphs.outStar(4);
    IntList perm = new IntArrayList(new int[] {3, 1, 2, 0});
    var remapped = Graphs.remapped(g, NodeMapping.forPermutation(perm));
    // Old node 0 becomes new node 3. Its out-edges: 0->1 stays 3->1; 0->2 stays 3->2; 0->3 becomes 3->0.
    assertThat(remapped.outNodes(3).toIntArray()).containsExactly(0, 1, 2);
  }

  // ---- edgeMapping ----

  @Test
  void edgeMappingOfReversedPath() {
    var g = TestGraphs.path(3);
    IntList perm = new IntArrayList(new int[] {2, 1, 0});
    var nm = NodeMapping.forPermutation(perm);
    var newGraph = Graphs.remapped(g, nm);
    var em = Graphs.edgeMapping(newGraph, g, nm);
    assertThat(em.size()).isEqualTo(newGraph.edgeCount());
    // Every new edge index must map to a valid old edge index.
    for (int i = 0; i < em.size(); i++) {
      assertThat(em.get(i)).isBetween(0, g.edgeCount() - 1);
    }
  }

  @Test
  void edgeMappingRecordsHolesForEdgesNotInOldGraph() {
    // newGraph has a self-loop 0->0 that does not exist in oldGraph (a 2-node path).
    // With the identity node mapping, edgeMapping must record -1 for the missing edge
    // and set isPartial = true. size() returns the count of valid entries (0 here).
    var oldGraph = TestGraphs.path(2);
    var newGraph = TestGraphs.of(2, 0, 0);
    IntList identity = new IntArrayList(new int[] {0, 1});
    var em = Graphs.edgeMapping(newGraph, oldGraph, NodeMapping.forPermutation(identity));
    assertThat(em.get(0)).isEqualTo(-1);
    assertThat(em.size()).isZero();
    assertThat(em.isPartial()).isTrue();
  }

  @Test
  void edgeMappingEmptyGraphHasZeroSize() {
    var g = TestGraphs.empty();
    var nm = NodeMapping.of(new int[0]);
    var em = Graphs.edgeMapping(g, g, nm);
    assertThat(em.size()).isZero();
  }

  // ---- edgePartition ----

  @Test
  void edgePartitionOmitsIntraGroupEdges() {
    // Graph: 0->1 (intra), 0->2 (inter), 1->2 (inter). Partition: {0,1}/{2}.
    var g = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    var partition = new NodePartition(
        g,
        new int[] {0, 0, 1},
        new int[] {0, 1, 2},
        new int[] {0, 2, 3});
    var condensed = partition.condense();
    var ep = Graphs.edgePartition(condensed, g, partition);
    assertThat(ep.edgeCount()).isEqualTo(g.edgeCount());
    assertThat(ep.get(0)).isEqualTo(-1);
    assertThat(ep.get(1)).isZero();
    assertThat(ep.get(2)).isZero();
  }

  @Test
  void edgePartitionGroupsMembersSortedAscending() {
    // 0->2, 1->2, 0->3: all cross-group {0,1}/{2}/{3}. Two condensed edges: 0->1 from {0,1}->{2}, 0->2 from {0,1}->{3}.
    var g = TestGraphs.of(4, 0, 2, 0, 3, 1, 2);
    var partition = new NodePartition(
        g,
        new int[] {0, 0, 1, 2},
        new int[] {0, 1, 2, 3},
        new int[] {0, 2, 3, 4});
    var condensed = partition.condense();
    var ep = Graphs.edgePartition(condensed, g, partition);
    // Condensed edge to group 1 ({2}) collects original edges 0->2 (index 0 in g) and 1->2 (index 2 in g).
    // Edge order in g depends on node ordering: node 0's outs sorted = [2, 3] (edge indices 0 and 1), node 1's outs = [2] (edge 2).
    assertThat(ep.edgesOf(0).toIntArray()).containsExactly(0, 2);
    assertThat(ep.edgesOf(1).toIntArray()).containsExactly(1);
  }
}
