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
  void isForestTrueForDisjointTrees() {
    // Two disjoint trees: {0->1, 0->2} and {3->4}.
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
  void isTreeTrueForSingleNode() {
    assertThat(Graphs.isTree(TestGraphs.isolated(1))).isTrue();
  }

  @Test
  void isTreeTrueForPath() {
    assertThat(Graphs.isTree(TestGraphs.path(5))).isTrue();
  }

  @Test
  void isTreeTrueForOutStar() {
    assertThat(Graphs.isTree(TestGraphs.outStar(5))).isTrue();
  }

  @Test
  void isTreeFalseForDisjointTrees() {
    // Two roots: {0->1} and {2->3}.
    assertThat(Graphs.isTree(TestGraphs.of(4, 0, 1, 2, 3))).isFalse();
  }

  @Test
  void isTreeFalseForIsolatedMultipleNodes() {
    assertThat(Graphs.isTree(TestGraphs.isolated(3))).isFalse();
  }

  @Test
  void isTreeFalseForInStar() {
    // Edges point toward the center, so there is no root with in-degree 0 except the leaves.
    assertThat(Graphs.isTree(TestGraphs.inStar(4))).isFalse();
  }

  @Test
  @SuppressWarnings("deprecation")
  void isOutTreeAnswersLikeIsTree() {
    for (var g : new Graph[] {
        TestGraphs.empty(), TestGraphs.isolated(1), TestGraphs.isolated(3),
        TestGraphs.path(5), TestGraphs.outStar(5), TestGraphs.inStar(4),
        TestGraphs.cycle(3), TestGraphs.of(4, 0, 1, 2, 3)}) {
      assertThat(Graphs.isOutTree(g)).isEqualTo(Graphs.isTree(g));
    }
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
    // Cycle is none of DAG/forest/tree/path.
    var g = TestGraphs.cycle(3);
    assertThat(Graphs.isDag(g)).isFalse();
    assertThat(Graphs.isForest(g)).isFalse();
    assertThat(Graphs.isTree(g)).isFalse();
    assertThat(Graphs.isPath(g)).isFalse();
  }

  @Test
  void shapeHierarchyConsistencyForPath() {
    // Path is DAG, forest, tree, and path.
    var g = TestGraphs.path(4);
    assertThat(Graphs.isDag(g)).isTrue();
    assertThat(Graphs.isForest(g)).isTrue();
    assertThat(Graphs.isTree(g)).isTrue();
    assertThat(Graphs.isPath(g)).isTrue();
  }

  // ---- Undirected shape checks ----

  @Test
  void isBipartiteTrueForEmptyGraph() {
    assertThat(Graphs.isBipartite(TestGraphs.empty())).isTrue();
  }

  @Test
  void isBipartiteTrueForIsolatedNodes() {
    assertThat(Graphs.isBipartite(TestGraphs.isolated(3))).isTrue();
  }

  @Test
  void isBipartiteTrueForPath() {
    assertThat(Graphs.isBipartite(TestGraphs.path(5))).isTrue();
  }

  @Test
  void isBipartiteTrueForStars() {
    // A star is bipartite either way round: center in one class, leaves in the other.
    assertThat(Graphs.isBipartite(TestGraphs.outStar(5))).isTrue();
    assertThat(Graphs.isBipartite(TestGraphs.inStar(5))).isTrue();
  }

  @Test
  void isBipartiteTrueForEvenCycle() {
    assertThat(Graphs.isBipartite(TestGraphs.cycle(4))).isTrue();
  }

  @Test
  void isBipartiteFalseForOddCycle() {
    assertThat(Graphs.isBipartite(TestGraphs.cycle(3))).isFalse();
  }

  @Test
  void isBipartiteTrueForReciprocalPair() {
    // 0->1 and 1->0 are one undirected edge, not a 2-cycle - and a 2-cycle would be even anyway.
    assertThat(Graphs.isBipartite(TestGraphs.of(2, 0, 1, 1, 0))).isTrue();
  }

  @Test
  void isBipartiteFalseForSymmetricallyStoredOddCycle() {
    // The triangle stored in both directions: collapsing the pairs still leaves an odd cycle.
    var g = TestGraphs.of(3, 0, 1, 1, 0, 1, 2, 2, 1, 0, 2, 2, 0);
    assertThat(Graphs.isBipartite(g)).isFalse();
  }

  @Test
  void isBipartiteFalseForSelfLoop() {
    // The one place where a self-loop is not ignored: a node adjacent to itself has no class.
    assertThat(Graphs.isBipartite(TestGraphs.of(1, 0, 0))).isFalse();
    assertThat(Graphs.isBipartite(TestGraphs.of(3, 0, 1, 1, 2, 2, 2))).isFalse();
  }

  @Test
  void isUndirectedTreeTrueForEmptyGraph() {
    assertThat(Graphs.isUndirectedTree(TestGraphs.empty())).isTrue();
  }

  @Test
  void isUndirectedTreeTrueForSingleNode() {
    assertThat(Graphs.isUndirectedTree(TestGraphs.isolated(1))).isTrue();
  }

  @Test
  void isUndirectedTreeTrueForPath() {
    assertThat(Graphs.isUndirectedTree(TestGraphs.path(5))).isTrue();
  }

  @Test
  void isUndirectedTreeTrueForInStar() {
    // Edges point toward the center, so this is no directed tree - undirected it is a star.
    var g = TestGraphs.inStar(4);
    assertThat(Graphs.isTree(g)).isFalse();
    assertThat(Graphs.isUndirectedTree(g)).isTrue();
  }

  @Test
  void isUndirectedTreeTrueForMixedDirections() {
    // 0->1 and 2->1: a DAG, but node 1 has two parents, so neither forest nor tree. Undirected
    // it is three nodes joined by two edges.
    var g = TestGraphs.of(3, 0, 1, 2, 1);
    assertThat(Graphs.isDag(g)).isTrue();
    assertThat(Graphs.isForest(g)).isFalse();
    assertThat(Graphs.isUndirectedTree(g)).isTrue();
  }

  @Test
  void isUndirectedTreeTrueForSymmetricallyStoredPath() {
    // Every edge in both directions: no DAG at all, yet the same tree when read as undirected.
    var g = TestGraphs.of(3, 0, 1, 1, 0, 1, 2, 2, 1);
    assertThat(Graphs.isDag(g)).isFalse();
    assertThat(Graphs.isUndirectedTree(g)).isTrue();
  }

  @Test
  void isUndirectedTreeTrueForReciprocalPair() {
    assertThat(Graphs.isUndirectedTree(TestGraphs.of(2, 0, 1, 1, 0))).isTrue();
  }

  @Test
  void isUndirectedTreeIgnoresSelfLoops() {
    // A self-loop on every node of a path leaves the path a tree.
    var g = TestGraphs.of(3, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2);
    assertThat(Graphs.isUndirectedTree(g)).isTrue();
  }

  @Test
  void isUndirectedTreeFalseForDisconnectedGraph() {
    assertThat(Graphs.isUndirectedTree(TestGraphs.isolated(3))).isFalse();
    assertThat(Graphs.isUndirectedTree(TestGraphs.of(4, 0, 1, 2, 3))).isFalse();
  }

  @Test
  void isUndirectedTreeFalseForCycle() {
    assertThat(Graphs.isUndirectedTree(TestGraphs.cycle(3))).isFalse();
  }

  @Test
  void isUndirectedTreeFalseForSymmetricallyStoredCycle() {
    // The triangle in both directions. Every edge here is a reciprocal pair, so a check that
    // discounted pairs without asking whether it has seen this one before would count three
    // edges as two and report a tree.
    var g = TestGraphs.of(3, 0, 1, 1, 0, 1, 2, 2, 1, 0, 2, 2, 0);
    assertThat(Graphs.isUndirectedTree(g)).isFalse();
    assertThat(Graphs.isUndirectedForest(g)).isFalse();
  }

  @Test
  void isUndirectedTreeTrueAtTheEdgeBound() {
    // A symmetrically stored path with a self-loop on every node holds exactly
    // 2 x (nodeCount - 1) + nodeCount = 3 x nodeCount - 2 edges: the most a tree can store, and
    // therefore the largest edge count the O(1) rejection must still let through.
    var g = TestGraphs.of(4,
        0, 0, 0, 1, 1, 0, 1, 1, 1, 2, 2, 1, 2, 2, 2, 3, 3, 2, 3, 3);
    assertThat(g.edgeCount()).isEqualTo(3 * g.nodeCount() - 2);
    assertThat(Graphs.isUndirectedTree(g)).isTrue();
  }

  @Test
  void isUndirectedTreeFalseForDiamond() {
    // Connected with n - 1 = 3 edges? No: four edges, and undirected they close a cycle.
    assertThat(Graphs.isUndirectedTree(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3))).isFalse();
  }

  @Test
  void isUndirectedForestTrueForEmptyGraph() {
    assertThat(Graphs.isUndirectedForest(TestGraphs.empty())).isTrue();
  }

  @Test
  void isUndirectedForestTrueForIsolatedNodes() {
    assertThat(Graphs.isUndirectedForest(TestGraphs.isolated(3))).isTrue();
  }

  @Test
  void isUndirectedForestTrueForDisjointTrees() {
    assertThat(Graphs.isUndirectedForest(TestGraphs.of(5, 0, 1, 0, 2, 3, 4))).isTrue();
  }

  @Test
  void isUndirectedForestTrueForInStar() {
    var g = TestGraphs.inStar(4);
    assertThat(Graphs.isForest(g)).isFalse();
    assertThat(Graphs.isUndirectedForest(g)).isTrue();
  }

  @Test
  void isUndirectedForestTrueForSelfLoopsOnly() {
    assertThat(Graphs.isUndirectedForest(TestGraphs.of(2, 0, 0, 1, 1))).isTrue();
  }

  @Test
  void isUndirectedForestFalseForCycle() {
    assertThat(Graphs.isUndirectedForest(TestGraphs.cycle(3))).isFalse();
  }

  @Test
  void isUndirectedForestFalseForDiamond() {
    assertThat(Graphs.isUndirectedForest(TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3))).isFalse();
  }

  @Test
  void isUndirectedForestFalseForCliqueRejectedByEdgeBound() {
    // 5 nodes, 20 edges - above 3 x nodeCount - 2, so this is decided without reading an edge.
    assertThat(Graphs.isUndirectedForest(TestGraphs.clique(5))).isFalse();
    assertThat(Graphs.isUndirectedTree(TestGraphs.clique(5))).isFalse();
  }

  @Test
  void isWeaklyConnectedTrueForEmptyGraph() {
    assertThat(Graphs.isWeaklyConnected(TestGraphs.empty())).isTrue();
  }

  @Test
  void isWeaklyConnectedTrueForSingleNode() {
    assertThat(Graphs.isWeaklyConnected(TestGraphs.isolated(1))).isTrue();
  }

  @Test
  void isWeaklyConnectedTrueForPathAndStars() {
    assertThat(Graphs.isWeaklyConnected(TestGraphs.path(5))).isTrue();
    assertThat(Graphs.isWeaklyConnected(TestGraphs.outStar(5))).isTrue();
    assertThat(Graphs.isWeaklyConnected(TestGraphs.inStar(5))).isTrue();
  }

  @Test
  void isWeaklyConnectedTrueForCycle() {
    assertThat(Graphs.isWeaklyConnected(TestGraphs.cycle(3))).isTrue();
  }

  @Test
  void isWeaklyConnectedFalseForIsolatedNodes() {
    assertThat(Graphs.isWeaklyConnected(TestGraphs.isolated(3))).isFalse();
  }

  @Test
  void isWeaklyConnectedFalseForTwoComponents() {
    assertThat(Graphs.isWeaklyConnected(TestGraphs.of(4, 0, 1, 2, 3))).isFalse();
  }

  @Test
  void isWeaklyConnectedIgnoresSelfLoops() {
    // Self-loops connect nothing, so two nodes carrying one each stay two components.
    assertThat(Graphs.isWeaklyConnected(TestGraphs.of(2, 0, 0, 1, 1))).isFalse();
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

  // ---- dfsReordered ----

  @Test
  @SuppressWarnings("deprecation")
  void dfsReorderedMatchesRemappedWithReindexMapping() {
    // Diamond + tail: 0 -> 1, 0 -> 2, 1 -> 3, 2 -> 3, 3 -> 4, 3 -> 5.
    var g = TestGraphs.of(6, 0, 1, 0, 2, 1, 3, 2, 3, 3, 4, 3, 5);
    var expected = Graphs.remapped(g, NodeMapping.reindex(g));
    assertThat(Graphs.dfsReordered(g)).isEqualTo(expected);
  }

  @Test
  @SuppressWarnings("deprecation")
  void dfsReorderedOnEmptyGraphIsEmpty() {
    var reordered = Graphs.dfsReordered(TestGraphs.empty());
    assertThat(reordered.nodeCount()).isZero();
    assertThat(reordered.edgeCount()).isZero();
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

  // ---- transposedEdgeMapping ----

  @Test
  void transposedEdgeMappingRelabelsEdges() {
    // 0->2 is edge 0, 1->0 is edge 1. In the transpose 0->1 comes first, so the two swap.
    var g = TestGraphs.of(3, 1, 0, 0, 2);
    var em = Graphs.transposedEdgeMapping(g);
    assertThat(em.get(0)).isEqualTo(1);
    assertThat(em.get(1)).isZero();
  }

  @Test
  void transposedEdgeMappingIsTotal() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3, 3, 3);
    var em = Graphs.transposedEdgeMapping(g);
    assertThat(em.size()).isEqualTo(g.edgeCount());
    assertThat(em.isPartial()).isFalse();
  }

  @Test
  void transposedEdgeMappingIdentifiesTheSwappedEdge() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var transposed = g.transposed();
    var em = Graphs.transposedEdgeMapping(g);
    for (int q = 0; q < transposed.edgeCount(); q++) {
      int p = em.get(q);
      assertThat(g.edgeSource(p)).isEqualTo(transposed.edgeTarget(q));
      assertThat(g.edgeTarget(p)).isEqualTo(transposed.edgeSource(q));
    }
  }

  @Test
  void transposedEdgeMappingInverseEqualsTransposedEdgeIndex() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3, 0, 0);
    var inverse = Graphs.transposedEdgeMapping(g).inverse();
    var biAdjacent = g.biAdjacentGraph();
    for (int p = 0; p < g.edgeCount(); p++) {
      assertThat(inverse.get(p)).isEqualTo(biAdjacent.transposedEdgeIndex(p));
    }
  }

  @Test
  void transposedEdgeMappingAgreesOnUniAndBiAdjacentInput() {
    var g = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    var fromUniAdjacent = Graphs.transposedEdgeMapping(g);
    var fromBiAdjacent = Graphs.transposedEdgeMapping(g.biAdjacentGraph());
    for (int q = 0; q < g.edgeCount(); q++) {
      assertThat(fromBiAdjacent.get(q)).isEqualTo(fromUniAdjacent.get(q));
    }
  }

  @Test
  void transposedEdgeMappingOfEmptyGraphHasZeroSize() {
    var em = Graphs.transposedEdgeMapping(TestGraphs.empty());
    assertThat(em.size()).isZero();
    assertThat(em.isPartial()).isFalse();
  }

  @Test
  void transposedEdgeMappingOfIsolatedNodesHasZeroSize() {
    assertThat(Graphs.transposedEdgeMapping(TestGraphs.isolated(5)).size()).isZero();
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
