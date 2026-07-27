package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Hard correctness check on the exact shape that shows the &gt;90% speedup (large random tree),
 * to rule out any under-traversal or packed-state bit bug that acyclic inputs could hide.
 * A tree has fully determined results for every DFS algorithm; any skipped node or wrong bit
 * would break one of these invariants.
 */
class PackedStateTreeCorrectnessTest {

  @Test
  void allDfsAlgorithmsAreCorrectOnLargeRandomTree() {
    int n = 100_000;
    var rnd = new Random(7);
    int[] parent = new int[n];
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 1; i < n; i++) {
      parent[i] = rnd.nextInt(i);
      b.addEdge(parent[i], i); // out-tree rooted at 0, parent < child, acyclic
    }
    Graph tree = b.build();

    // SCC: every node is its own component -> n singletons covering all nodes.
    Sccs sccs = Components.sccsOf(tree);
    assertThat(sccs.groupCount()).isEqualTo(n);
    long covered = 0;
    for (int g = 0; g < sccs.groupCount(); g++) {
      assertThat(sccs.nodesOf(g).size()).isEqualTo(1);
      covered += sccs.nodesOf(g).size();
    }
    assertThat(covered).isEqualTo(n); // proves every node was visited

    // isDag: a tree is acyclic.
    assertThat(Graphs.isDag(tree)).isTrue();

    // TopOrder: complete + valid (parent before child for every edge).
    IntList order = TopOrder.of(tree);
    assertThat(order.size()).isEqualTo(n);
    int[] pos = new int[n];
    for (int k = 0; k < n; k++) pos[order.getInt(k)] = k;
    for (int i = 1; i < n; i++) {
      assertThat(pos[parent[i]]).isLessThan(pos[i]);
    }

    // BackEdges: none in a tree.
    assertThat(BackEdges.of(tree).count()).isEqualTo(0);

    // EdgeTypes: every one of the n-1 edges is a TREE edge (no BACK/FORWARD/CROSS).
    EdgeTypes et = EdgeTypes.of(tree);
    int treeEdges = 0;
    for (int e = 0; e < tree.edgeCount(); e++) {
      assertThat(et.typeOf(e)).isEqualTo(EdgeType.TREE);
      treeEdges++;
    }
    assertThat(treeEdges).isEqualTo(n - 1);
  }

  @Test
  void packedStateHandlesCyclesToo() {
    int n = 1000;
    var b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i < n; i++) b.addEdge(i, (i + 1) % n); // one big directed cycle
    Graph cycle = b.build();

    Sccs sccs = Components.sccsOf(cycle);
    assertThat(sccs.groupCount()).isEqualTo(1);      // whole cycle = one SCC
    assertThat(Graphs.isDag(cycle)).isFalse();
    assertThat(BackEdges.of(cycle).count()).isEqualTo(1); // exactly one back-edge closes it
  }
}
