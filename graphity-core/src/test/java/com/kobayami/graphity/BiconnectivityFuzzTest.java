package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.JGraphtAdapter;
import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link Components#biconnectivityOf(Graph)} against JGraphT's
 * {@code BiconnectivityInspector} on the same graph read as undirected, and against invariants
 * that hold independently of any implementation.
 * <p>
 * Every graph is checked twice: once as generated, where an undirected edge is usually stored
 * in one direction only, and once symmetrized, where both directions are present. Without the
 * second form the decision to read {@code a → b} and {@code b → a} as one edge would go
 * untested.
 */
class BiconnectivityFuzzTest {

  private static final int ITERATIONS = 100;

  @Test
  void bridgesMatchJgrapht() {
    forEachGraph((description, graph) -> {
      var jgrapht = JGraphtAdapter.toUndirectedJgrapht(graph);
      var inspector = new BiconnectivityInspector<>(jgrapht);
      var expected = new LongOpenHashSet();
      for (DefaultEdge edge : inspector.getBridges()) {
        expected.add(pairKey(jgrapht.getEdgeSource(edge), jgrapht.getEdgeTarget(edge)));
      }
      var actual = bridgeKeys(Components.biconnectivityOf(graph));
      assertThat(actual).as("bridges for %s", description).isEqualTo(expected);
    });
  }

  @Test
  void articulationPointsMatchJgrapht() {
    forEachGraph((description, graph) -> {
      var inspector = new BiconnectivityInspector<>(JGraphtAdapter.toUndirectedJgrapht(graph));
      var expected = new TreeSet<Integer>(inspector.getCutpoints());
      var actual = new TreeSet<Integer>();
      var points = Components.biconnectivityOf(graph).articulationPoints();
      for (int i = 0; i < points.size(); i++) actual.add(points.getInt(i));
      assertThat(actual).as("articulation points for %s", description).isEqualTo(expected);
    });
  }

  @Test
  void blocksMatchJgrapht() {
    forEachGraph((description, graph) -> {
      var jgrapht = JGraphtAdapter.toUndirectedJgrapht(graph);
      var inspector = new BiconnectivityInspector<>(jgrapht);

      Set<Set<Long>> expected = new HashSet<>();
      for (var block : inspector.getBlocks()) {
        Set<Long> edges = new HashSet<>();
        for (DefaultEdge edge : block.edgeSet()) {
          edges.add(pairKey(jgrapht.getEdgeSource(edge), jgrapht.getEdgeTarget(edge)));
        }
        if (!edges.isEmpty()) expected.add(edges);
      }

      var blocks = Components.biconnectivityOf(graph).blocks();
      Set<Set<Long>> actual = new HashSet<>();
      for (int block = 0; block < blocks.groupCount(); block++) {
        Set<Long> edges = new HashSet<>();
        var edgeIndices = blocks.edgesOf(block);
        for (int i = 0; i < edgeIndices.size(); i++) {
          int edgeIndex = edgeIndices.getInt(i);
          edges.add(pairKey(graph.edgeSource(edgeIndex), graph.edgeTarget(edgeIndex)));
        }
        actual.add(edges);
      }
      assertThat(actual).as("blocks for %s", description).isEqualTo(expected);
    });
  }

  @Test
  void blocksCoverEveryNonSelfLoopEdgeExactlyOnce() {
    forEachGraph((description, graph) -> {
      var blocks = Components.biconnectivityOf(graph).blocks();
      int selfLoops = 0;
      for (int edgeIndex = 0; edgeIndex < graph.edgeCount(); edgeIndex++) {
        boolean selfLoop = graph.edgeSource(edgeIndex) == graph.edgeTarget(edgeIndex);
        if (selfLoop) {
          selfLoops++;
          assertThat(blocks.get(edgeIndex))
              .as("self-loop %d is unmapped for %s", edgeIndex, description)
              .isEqualTo(-1);
        } else {
          assertThat(blocks.get(edgeIndex))
              .as("edge %d belongs to a block for %s", edgeIndex, description)
              .isBetween(0, blocks.groupCount() - 1);
        }
      }
      assertThat(blocks.size())
          .as("mapped edge count for %s", description)
          .isEqualTo(graph.edgeCount() - selfLoops);
      assertThat(blocks.isPartial())
          .as("partial exactly with self-loops for %s", description)
          .isEqualTo(selfLoops > 0);

      int members = 0;
      for (int block = 0; block < blocks.groupCount(); block++) {
        members += blocks.sizeOf(block);
      }
      assertThat(members).as("block members sum for %s", description).isEqualTo(blocks.size());
    });
  }

  @Test
  void reciprocalPairsShareABlockAndBridgesStandAlone() {
    forEachGraph((description, graph) -> {
      var biconnectivity = Components.biconnectivityOf(graph);
      var blocks = biconnectivity.blocks();
      var biAdjacent = graph.biAdjacentGraph();

      for (int edgeIndex = 0; edgeIndex < graph.edgeCount(); edgeIndex++) {
        int source = graph.edgeSource(edgeIndex);
        int target = graph.edgeTarget(edgeIndex);
        if (source == target || !biAdjacent.hasOutNode(target, source)) continue;
        int reverse = reverseEdgeIndex(graph, edgeIndex);
        assertThat(blocks.get(reverse))
            .as("reciprocal pair %d/%d for %s", edgeIndex, reverse, description)
            .isEqualTo(blocks.get(edgeIndex));
      }

      var bridges = biconnectivity.bridges();
      for (int i = 0; i < bridges.count(); i++) {
        int source = bridges.sourceNodes().getInt(i);
        int target = bridges.targetNodes().getInt(i);
        assertThat(source).as("bridge normalization for %s", description).isLessThan(target);
        int edgeIndex = anyEdgeIndexBetween(graph, source, target);
        int block = blocks.get(edgeIndex);
        var edgeIndices = blocks.edgesOf(block);
        for (int j = 0; j < edgeIndices.size(); j++) {
          int other = edgeIndices.getInt(j);
          assertThat(pairKey(graph.edgeSource(other), graph.edgeTarget(other)))
              .as("bridge block holds no other edge for %s", description)
              .isEqualTo(pairKey(source, target));
        }
      }
    });
  }

  @Test
  void resultIsIdenticalForBiAdjacentInput() {
    forEachGraph((description, graph) -> {
      var fromUniAdjacent = Components.biconnectivityOf(graph);
      var fromBiAdjacent = Components.biconnectivityOf(graph.biAdjacentGraph());
      assertThat(bridgeKeys(fromBiAdjacent))
          .as("bridges for %s", description)
          .isEqualTo(bridgeKeys(fromUniAdjacent));
      assertThat(fromBiAdjacent.articulationPoints().toIntArray())
          .as("articulation points for %s", description)
          .containsExactly(fromUniAdjacent.articulationPoints().toIntArray());
      for (int edgeIndex = 0; edgeIndex < graph.edgeCount(); edgeIndex++) {
        assertThat(fromBiAdjacent.blocks().get(edgeIndex))
            .as("block of edge %d for %s", edgeIndex, description)
            .isEqualTo(fromUniAdjacent.blocks().get(edgeIndex));
      }
    });
  }

  /**
   * Guards the corpus itself: a fuzz run in which every graph happens to have no bridge, no
   * articulation point and no block larger than one edge would agree with any oracle.
   */
  @Test
  void theCorpusExercisesAllThreeResults() {
    int[] totals = new int[3];
    forEachGraph((description, graph) -> {
      var biconnectivity = Components.biconnectivityOf(graph);
      totals[0] += biconnectivity.bridges().count();
      totals[1] += biconnectivity.articulationPoints().size();
      var blocks = biconnectivity.blocks();
      for (int block = 0; block < blocks.groupCount(); block++) {
        if (blocks.sizeOf(block) > 2) totals[2]++;
      }
    });
    assertThat(totals[0]).as("bridges in the corpus").isGreaterThan(100);
    assertThat(totals[1]).as("articulation points in the corpus").isGreaterThan(100);
    assertThat(totals[2]).as("blocks of more than one undirected edge").isGreaterThan(100);
  }

  /** Runs the check on each random graph, once as generated and once with both directions. */
  private static void forEachGraph(GraphCheck check) {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      check.accept(labeled.describe(), labeled.graph());
      check.accept(labeled.describe() + ", symmetrized",
          RandomGraphs.symmetrized(labeled.graph()));
    }
  }

  private interface GraphCheck {
    void accept(String description, Graph graph);
  }

  private static LongOpenHashSet bridgeKeys(Biconnectivity biconnectivity) {
    var keys = new LongOpenHashSet();
    var bridges = biconnectivity.bridges();
    for (int i = 0; i < bridges.count(); i++) {
      keys.add(pairKey(bridges.sourceNodes().getInt(i), bridges.targetNodes().getInt(i)));
    }
    return keys;
  }

  private static long pairKey(int a, int b) {
    return ((long) Math.min(a, b) << 32) | Math.max(a, b);
  }

  private static int reverseEdgeIndex(Graph graph, int edgeIndex) {
    return edgeIndexOf(graph, graph.edgeTarget(edgeIndex), graph.edgeSource(edgeIndex));
  }

  /** Edge index of {@code source → target}, or of the reverse edge if only that one exists. */
  private static int anyEdgeIndexBetween(Graph graph, int source, int target) {
    int forward = edgeIndexOf(graph, source, target);
    return forward >= 0 ? forward : edgeIndexOf(graph, target, source);
  }

  /** Edge index of {@code source → target} via binary search, or {@code -1}. */
  private static int edgeIndexOf(Graph graph, int source, int target) {
    int position = graph.outNodes(source).indexOf(target);
    return position < 0 ? -1 : graph.outEdgeStart(source) + position;
  }
}
