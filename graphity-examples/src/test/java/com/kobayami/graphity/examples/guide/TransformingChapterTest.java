package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graphs;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Transforming graphs and carrying data along". */
class TransformingChapterTest {

  @Test
  void subgraphs() {
    TransformingGraphs.Subgraph subgraph = TransformingGraphs.subgraph();
    assertThat(Graphs.adjacencyListOf(subgraph.loading())).isEqualTo("""
        0 -> [1, 2]
        1 -> [1]
        2 -> []
        """);
    assertThat(subgraph.loadingNames()).containsExactly("load", "parse", "read");
    assertThat(Graphs.adjacencyListOf(subgraph.savingAndShowing())).isEqualTo("""
        0 -> [2, 3]
        1 -> [2]
        2 -> [2]
        3 -> []
        """);
  }

  @Test
  void renumbered() {
    TransformingGraphs.Renumbered renumbered = TransformingGraphs.renumbered();
    StringBuilder order = new StringBuilder();
    for (int i = 0; i < renumbered.searchOrder().indexCount(); i++) {
      order.append(renumbered.searchOrder().get(i)).append(' ');
    }
    assertThat(order.toString()).isEqualTo("0 1 2 3 6 8 4 7 9 5 ");
    assertThat(Graphs.adjacencyListOf(renumbered.inSearchOrder())).isEqualTo("""
        0 -> [1, 2]
        1 -> []
        2 -> [3, 6, 9]
        3 -> [4, 5]
        4 -> [4]
        5 -> []
        6 -> [7, 8]
        7 -> [7]
        8 -> []
        9 -> [7]
        """);
    StringBuilder runOrder = new StringBuilder();
    for (int i = 0; i < renumbered.runOrder().indexCount(); i++) {
      runOrder.append(renumbered.runOrder().get(i)).append(' ');
    }
    assertThat(runOrder.toString()).isEqualTo("2 0 1 4 3 5 6 7 8 ");
    assertThat(Graphs.adjacencyListOf(renumbered.inRunOrder())).startsWith("""
        0 -> [6]
        1 -> [2]
        2 -> [3, 4, 6]
        3 -> [8]
        4 -> [5]
        5 -> [6]
        6 -> [7]
        """);
    assertThat(Graphs.isDag(renumbered.inRunOrder())).isTrue();
  }

  @Test
  void nodeData() {
    TransformingGraphs.NodeData data = TransformingGraphs.nodeData();
    assertThat(data.loadingLines()).containsExactly(25, 60, 10);
    assertThat(data.newIdOfParse()).isEqualTo(1);
    assertThat(data.mainKept()).isFalse();
    assertThat(data.partial()).isTrue();
    assertThat(data.mappedCount()).isEqualTo(3);
    assertThat(data.indexCount()).isEqualTo(10);
    assertThat(data.secondToOriginal().indexCount()).isEqualTo(2);
    assertThat(data.secondToOriginal().get(0)).isEqualTo(6);
    assertThat(data.secondToOriginal().get(1)).isEqualTo(8);
    assertThat(data.calleeLines()).containsExactly(60, 10);
  }

  @Test
  void edgeData() {
    assertThat(TransformingGraphs.edgeData()).containsExactly(1, 1, 2);
  }

  @Test
  void transposed() {
    TransformingGraphs.Transposed transposed = TransformingGraphs.transposed();
    assertThat(Graphs.adjacencyListOf(transposed.calledBy())).isEqualTo("""
        0 -> []
        1 -> [0]
        2 -> [0]
        3 -> [2]
        4 -> [2]
        5 -> [2]
        6 -> [3, 6]
        7 -> [4, 5, 7]
        8 -> [3]
        9 -> [4]
        """);
    assertThat(transposed.transposedIndexOfLoadToRead()).isEqualTo(10);
    assertThat(transposed.callersOfFormat())
        .isEqualTo(List.of("save: 1", "show: 1", "format: 1"));
  }

  @Test
  void ownGroups() {
    TransformingGraphs.OwnGroups own = TransformingGraphs.ownGroups();
    assertThat(own.groupCount()).isEqualTo(8);
    assertThat(own.sizeOfService()).isEqualTo(2);
    assertThat(own.methodsOfService().toString()).isEqualTo("[3, 4]");
    assertThat(Graphs.adjacencyListOf(own.packageCalls())).isEqualTo("""
        0 -> [1, 2, 3]
        1 -> []
        2 -> [4]
        3 -> [4, 5]
        4 -> []
        5 -> []
        6 -> []
        7 -> []
        """);
  }

  @Test
  void combined() {
    TransformingGraphs.Combined combined = TransformingGraphs.combined();
    assertThat(combined.linesPerPackage()).containsExactly(32, 30, 15, 43, 100, 20, 0, 0);
    assertThat(combined.behind().toString()).isEqualTo("[5, 7]");
    assertThat(combined.sitesPerPackageCall()).containsExactly(1, 2, 2, 1, 2, 2);
  }
}
