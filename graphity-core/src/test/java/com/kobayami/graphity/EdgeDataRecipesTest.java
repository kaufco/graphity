package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import java.util.BitSet;
import org.junit.jupiter.api.Test;

/** The recipes for edge data in the tutorial, run as written and checked independently. */
class EdgeDataRecipesTest {

  private static double[] weightsOf(Graph graph) {
    var weights = new double[graph.edgeCount()];
    for (int e = 0; e < weights.length; e++) weights[e] = 1 + e % 7;
    return weights;
  }

  @Test
  void visitEveryEdgeWithItsData() {
    Graph graph = RandomGraphs.gnp(40, 0.1, 1);
    double[] weights = weightsOf(graph);

    double[] total = {0};
    graph.forEachEdge((source, target, edgeIndex) -> total[0] += weights[edgeIndex]);

    double expected = 0;
    for (double w : weights) expected += w;
    assertThat(total[0]).isEqualTo(expected);
  }

  @Test
  void sumTheWeightsBehindEachContractedEdgeAndCollectTheEdgesBetweenTwoGroups() {
    Graph graph = RandomGraphs.gnp(60, 0.03, 2);
    double[] weights = weightsOf(graph);
    Sccs partition = Components.sccsOf(graph);

    Graph condensed = partition.condense();
    EdgePartition provenance = Graphs.edgePartition(condensed, partition);
    double[] weightPerCondensedEdge = provenance.reduce(weights, 0.0, Double::sum);

    assertThat(partition.sccCount()).isGreaterThan(10);
    assertThat(condensed.edgeCount()).isGreaterThan(10);

    for (int g = 0; g < partition.sccCount(); g++) {
      for (int h = 0; h < partition.sccCount(); h++) {
        int condensedEdge = condensed.edgeIndexOf(g, h);                 // -1 if no edge runs from g to h
        SortedIntView between = condensedEdge < 0 ? SortedIntView.EMPTY : provenance.edgesOf(condensedEdge);
        double betweenWeight = 0;
        for (int i = 0; i < between.size(); i++) {
          betweenWeight += weights[between.getInt(i)];                 // between holds edge indices of graph
        }

        double sum = 0;
        int count = 0;
        for (int e = 0; e < graph.edgeCount(); e++) {
          if (g != h && partition.sccOf(graph.edgeSource(e)) == g
              && partition.sccOf(graph.edgeTarget(e)) == h) {
            sum += weights[e];
            count++;
          }
        }
        assertThat(between.size()).as("%d -> %d", g, h).isEqualTo(count);
        assertThat(betweenWeight).isEqualTo(sum);
        if (condensedEdge >= 0) assertThat(weightPerCondensedEdge[condensedEdge]).isEqualTo(sum);
        for (int i = 0; i < between.size(); i++) {
          int edgeIndex = between.getInt(i);
          assertThat(partition.sccOf(graph.edgeSource(edgeIndex))).isEqualTo(g);
          assertThat(partition.sccOf(graph.edgeTarget(edgeIndex))).isEqualTo(h);
        }
      }
    }
    // Edges inside one group are in no condensed edge.
    for (int e = 0; e < graph.edgeCount(); e++) {
      boolean inside =
          partition.sccOf(graph.edgeSource(e)) == partition.sccOf(graph.edgeTarget(e));
      assertThat(provenance.get(e) < 0).isEqualTo(inside);
    }
  }

  @Test
  void carryEdgeDataAcrossASubgraph() {
    Graph graph = RandomGraphs.gnp(50, 0.1, 3);
    double[] weights = weightsOf(graph);
    String[] names = new String[graph.nodeCount()];
    for (int v = 0; v < names.length; v++) names[v] = "v" + v;
    var selected = new BitSet();
    for (int v = 0; v < graph.nodeCount(); v += 2) selected.set(v);

    NodeMapping keep = NodeMapping.forSubgraph(selected);
    Graph sub = Graphs.remapped(graph, keep);
    EdgeMapping subToGraph = Graphs.edgeMapping(sub, graph, keep);
    double[] subWeights = subToGraph.gather(weights);
    String[] subNames = new String[sub.nodeCount()];
    keep.gather(names, subNames);

    sub.forEachEdge((source, target, edgeIndex) -> {
      int oldEdge = graph.edgeIndexOf(keep.get(source), keep.get(target));
      assertThat(subWeights[edgeIndex]).isEqualTo(weights[oldEdge]);
    });
    for (int v = 0; v < sub.nodeCount(); v++) {
      assertThat(subNames[v]).isEqualTo("v" + keep.get(v));
    }
  }

  @Test
  void carryEdgeDataAcrossATranspose() {
    Graph graph = RandomGraphs.gnp(30, 0.1, 4);
    double[] weights = weightsOf(graph);

    Graph transpose = BiAdjacentGraph.of(graph).transposed();
    EdgeMapping newToOld = Graphs.transposedEdgeMapping(graph);
    double[] transposedWeights = newToOld.gather(weights);

    transpose.forEachEdge((source, target, edgeIndex) -> assertThat(transposedWeights[edgeIndex])
        .isEqualTo(weights[graph.edgeIndexOf(target, source)]));
  }
}
