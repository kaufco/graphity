package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.JGraphtAdapter;
import com.kobayami.graphity.testutils.RandomGraphs;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link Components#sccsOf} and {@link Components#wccsOf} against
 * JGraphT. For every random graph, the two libraries must produce the same partition.
 */
class ComponentsFuzzTest {

  private static final int ITERATIONS = 100;
  private static final long BASE_SEED = 0xC0FFEEL;

  @Test
  void sccsAgreeWithJgrapht() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      var jg = JGraphtAdapter.toJgrapht(g);

      var ourSccs = Components.sccsOf(g);
      var ourPartition = collectPartition(ourSccs.groupCount(), ourSccs::nodesOf, g.nodeCount());
      var theirPartition = collectJgraphtPartition(
          new KosarajuStrongConnectivityInspector<>(jg).stronglyConnectedSets());

      assertThat(ourPartition)
          .as("SCC partition mismatch for %s", labeled.describe())
          .isEqualTo(theirPartition);

      assertReverseTopoInvariant(labeled, g, ourSccs);
    }
  }

  @Test
  void wccsAgreeWithJgrapht() {
    for (int i = 0; i < ITERATIONS; i++) {
      var labeled = RandomGraphs.anyGraph(BASE_SEED + i);
      var g = labeled.graph();
      var jg = JGraphtAdapter.toJgrapht(g);

      NodePartition ourWccs = Components.wccsOf(g);
      var ourPartition = collectPartition(ourWccs.groupCount(), ourWccs::nodesOf, g.nodeCount());
      var theirPartition = collectJgraphtPartition(
          new ConnectivityInspector<>(jg).connectedSets());

      assertThat(ourPartition)
          .as("WCC partition mismatch for %s", labeled.describe())
          .isEqualTo(theirPartition);
    }
  }

  private static Set<Set<Integer>> collectPartition(
      int groupCount,
      IntFunction<SortedIntView> nodesOf,
      int nodeCount) {
    var partition = new HashSet<Set<Integer>>();
    for (int g = 0; g < groupCount; g++) {
      var group = new HashSet<Integer>();
      var nodes = nodesOf.apply(g);
      for (int i = 0; i < nodes.size(); i++) group.add(nodes.getInt(i));
      partition.add(group);
    }
    int total = partition.stream().mapToInt(Set::size).sum();
    assertThat(total).isEqualTo(nodeCount);
    return partition;
  }

  private static Set<Set<Integer>> collectJgraphtPartition(
      java.util.List<Set<Integer>> components) {
    var result = new HashSet<Set<Integer>>();
    for (var component : components) result.add(new HashSet<>(component));
    return result;
  }

  private static void assertReverseTopoInvariant(
      RandomGraphs.LabeledGraph labeled, Graph graph, Sccs sccs) {
    for (int u = 0; u < graph.nodeCount(); u++) {
      var outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        int su = sccs.get(u);
        int sv = sccs.get(v);
        if (su != sv) {
          assertThat(su)
              .as("reverse-topo invariant violated for %s: edge %d -> %d, sccId(%d)=%d, sccId(%d)=%d",
                  labeled.describe(), u, v, u, su, v, sv)
              .isGreaterThan(sv);
        }
      }
    }
  }
}
