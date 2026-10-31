package com.kobayami.graphity.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.testutils.TestGraphs;
import org.junit.jupiter.api.Test;

class MermaidWriterTest {

  @Test
  void writesAPlainFlowchart() {
    String text = MermaidWriter.of(TestGraphs.of(3, 0, 1, 0, 2, 2, 0)).writeToString();
    assertThat(text).isEqualTo("""
        flowchart TD
          n0
          n1
          n2
          n0 --> n1
          n0 --> n2
          n2 --> n0
        """);
  }

  @Test
  void writesLabelsShapesClassesAndEdgeStyles() {
    String[] names = {"entry", "loop", "exit"};
    String text = MermaidWriter.of(TestGraphs.of(3, 0, 1, 1, 1, 1, 2))
        .direction(MermaidWriter.Direction.LEFT_RIGHT)
        .nodeLabel(node -> names[node])
        .edgeLabel(edge -> edge == 1 ? "again" : null)
        .nodeShape(node -> node == 0 ? MermaidWriter.Shape.CIRCLE : null)
        .classDef("hot", "fill:#f96,stroke:#333")
        .nodeClass(node -> node == 1 ? "hot" : null)
        .edgeStyle(edge -> edge == 1 ? "stroke:red" : null)
        .writeToString();
    assertThat(text).isEqualTo("""
        flowchart LR
          classDef hot fill:#f96,stroke:#333
          n0(("entry"))
          n1["loop"]
          n2["exit"]
          n0 --> n1
          n1 -->|"again"| n1
          n1 --> n2
          class n1 hot
          linkStyle 1 stroke:red
        """);
  }

  @Test
  void edgeStylesFollowTheWrittenLinksWhenAFilterDropsEdges() {
    // 0 -> 1, 0 -> 2, 1 -> 2; without node 1 only 0 -> 2 (edge 1) remains, Mermaid's link 0.
    String text = MermaidWriter.of(TestGraphs.of(3, 0, 1, 0, 2, 1, 2))
        .nodeFilter(node -> node != 1)
        .edgeStyle(edge -> "stroke-width:" + (edge + 1) + "px")
        .writeToString();
    assertThat(text).isEqualTo("""
        flowchart TD
          n0
          n2
          n0 --> n2
          linkStyle 0 stroke-width:2px
        """);
  }

  @Test
  void escapesQuotesHashesAnglesAndLineBreaks() {
    String text = MermaidWriter.of(TestGraphs.isolated(1))
        .nodeLabel(node -> "a \"b\" #1 <c>\nd")
        .writeToString();
    assertThat(text).contains("n0[\"a #quot;b#quot; #35;1 #lt;c#gt;<br>d\"]");
  }

  @Test
  void writesSubgraphsAndTheUndirectedReading() {
    var graph = TestGraphs.of(3, 0, 1, 1, 0, 1, 2);
    var sccs = Components.sccsOf(graph);
    String text = MermaidWriter.of(graph)
        .undirected()
        .clusters(sccs, scc -> sccs.sizeOf(scc) > 1 ? "cycle" : null)
        .writeToString();
    int big = sccs.sccOf(0);
    assertThat(text).contains("  subgraph c" + big + "[\"cycle\"]\n    n0\n    n1\n  end\n");
    assertThat(text).contains("  subgraph c" + sccs.sccOf(2) + "\n    n2\n  end\n");
    assertThat(text).endsWith("  n0 --- n1\n  n1 --- n2\n");
  }

  @Test
  void rejectsInvalidClassNamesAndStyles() {
    var writer = MermaidWriter.of(TestGraphs.path(2));
    assertThatThrownBy(() -> writer.classDef("1st", "fill:red"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.classDef("ok", "fill:red; x"))
        .isInstanceOf(IllegalArgumentException.class);
    var badClass = MermaidWriter.of(TestGraphs.path(2)).nodeClass(node -> "a b");
    assertThatThrownBy(badClass::writeToString).isInstanceOf(IllegalArgumentException.class);
    var badStyle = MermaidWriter.of(TestGraphs.path(2)).edgeStyle(edge -> "a\nb");
    assertThatThrownBy(badStyle::writeToString).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anEmptyGraph() {
    assertThat(MermaidWriter.of(TestGraphs.empty()).writeToString()).isEqualTo("flowchart TD\n");
  }

  @Test
  void everyShapeHasItsBrackets() {
    var shapes = MermaidWriter.Shape.values();
    String text = MermaidWriter.of(TestGraphs.isolated(shapes.length))
        .nodeShape(node -> shapes[node])
        .writeToString();
    assertThat(text).isEqualTo("""
        flowchart TD
          n0
          n1("n1")
          n2(["n2"])
          n3[["n3"]]
          n4[("n4")]
          n5(("n5"))
          n6((("n6")))
          n7{"n7"}
          n8{{"n8"}}
        """);
  }

  @Test
  void undirectedLinksWithLabelsAndStylesCountOnlyWrittenLinks() {
    // 0 <-> 1 stored both ways (edges 0 and 1), 1 -> 2 (edge 2): two links are written.
    String text = MermaidWriter.of(TestGraphs.of(3, 0, 1, 1, 0, 1, 2))
        .undirected()
        .edgeLabel(edge -> "e" + edge)
        .edgeStyle(edge -> edge == 2 ? "stroke:blue" : null)
        .writeToString();
    assertThat(text).endsWith("""
          n0 ---|"e0"| n1
          n1 ---|"e2"| n2
          linkStyle 1 stroke:blue
        """);
  }

  @Test
  void filterAndClustersTogether() {
    var graph = TestGraphs.path(4);
    var partition = NodePartition.of(graph, new int[] {0, 0, 1, -1}, 2);
    String text = MermaidWriter.of(graph)
        .clusters(partition, group -> null)
        .nodeFilter(node -> node == 0 || node == 3)
        .nodeClass(node -> "c")
        .classDef("c", "fill:red")
        .writeToString();
    assertThat(text).isEqualTo("""
        flowchart TD
          classDef c fill:red
          subgraph c0
            n0
          end
          n3
          class n0 c
          class n3 c
        """);
  }

  @Test
  void escapesAmpersandsAndBackticks() {
    String text = MermaidWriter.of(TestGraphs.isolated(1))
        .nodeLabel(node -> "`**x**` &lt;")
        .writeToString();
    assertThat(text).contains("n0[\"#96;**x**#96; #38;lt;\"]");
  }

  @Test
  void rejectsReservedAndHyphenatedClassNamesEmptyStylesAndForeignPartitions() {
    var writer = MermaidWriter.of(TestGraphs.path(2));
    assertThatThrownBy(() -> writer.classDef("end", "fill:red"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.classDef("a--b", "fill:red"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.classDef("ok", " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> writer.clusters(Components.sccsOf(TestGraphs.path(3)), scc -> null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
