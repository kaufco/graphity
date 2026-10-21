package com.kobayami.graphity.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DotWriterTest {

  @Test
  void writesAPlainGraph() {
    // 0 -> 1, 0 -> 2, 2 -> 0, and 3 alone.
    String dot = DotWriter.of(TestGraphs.of(4, 0, 1, 0, 2, 2, 0)).writeToString();
    assertThat(dot).isEqualTo("""
        digraph {
          3;
          0 -> 1;
          0 -> 2;
          2 -> 0;
        }
        """);
  }

  @Test
  void writesLabelsAndAttributesFromTheHooks() {
    String[] names = {"entry", "loop", "exit"};
    double[] weights = {1.5, 2.0, 0.25};
    String dot = DotWriter.of(TestGraphs.of(3, 0, 1, 1, 1, 1, 2))
        .graphAttribute("rankdir", "LR")
        .nodeLabel(node -> names[node])
        .edgeLabel(edge -> String.valueOf(weights[edge]))
        .nodeAttributes((node, attributes) -> {
          if (node == 0) attributes.put("shape", "doublecircle");
        })
        .edgeAttributes((edge, attributes) -> {
          if (edge == 1) attributes.put("color", "red");
        })
        .writeToString();
    assertThat(dot).isEqualTo("""
        digraph {
          rankdir="LR";
          0 [label="entry", shape="doublecircle"];
          1 [label="loop"];
          2 [label="exit"];
          0 -> 1 [label="1.5"];
          1 -> 1 [label="2.0", color="red"];
          1 -> 2 [label="0.25"];
        }
        """);
  }

  @Test
  void escapesQuotesBackslashesAndLineBreaks() {
    String dot = DotWriter.of(TestGraphs.isolated(1))
        .nodeLabel(node -> "say \"hi\"\\\nbye")
        .writeToString();
    assertThat(dot).contains("0 [label=\"say \\\"hi\\\"\\\\\\nbye\"];");
  }

  @Test
  void writesClustersInGroupOrderWithTheirLabels() {
    // 0 <-> 1 is one SCC, 2 and 3 are trivial; 1 -> 2 -> 3.
    var graph = TestGraphs.of(4, 0, 1, 1, 0, 1, 2, 2, 3);
    var sccs = Components.sccsOf(graph);
    String dot = DotWriter.of(graph)
        .clusters(sccs, scc -> sccs.sizeOf(scc) > 1 ? "SCC " + scc : null)
        .writeToString();
    int bigScc = sccs.sccOf(0);
    assertThat(dot).contains("  subgraph cluster_" + bigScc + " {\n    label=\"SCC " + bigScc
        + "\";\n    0;\n    1;\n  }\n");
    assertThat(dot).contains("  subgraph cluster_" + sccs.sccOf(3) + " {\n    3;\n  }\n");
    assertThat(dot).endsWith("  0 -> 1;\n  1 -> 0;\n  1 -> 2;\n  2 -> 3;\n}\n");
  }

  @Test
  void nodesInNoGroupStayOutsideTheClusters() {
    var graph = TestGraphs.path(3);
    var partition = NodePartition.of(graph, new int[] {0, -1, 0}, 1);
    String dot = DotWriter.of(graph).clusters(partition, group -> null).writeToString();
    assertThat(dot).isEqualTo("""
        digraph {
          subgraph cluster_0 {
            0;
            2;
          }
          0 -> 1;
          1 -> 2;
        }
        """);
  }

  @Test
  void theNodeFilterDropsNodesAndTheirEdgesButKeepsTheIds() {
    String dot = DotWriter.of(TestGraphs.path(4))
        .nodeFilter(node -> node != 1)
        .nodeLabel(node -> "v" + node)
        .writeToString();
    assertThat(dot).isEqualTo("""
        digraph {
          0 [label="v0"];
          2 [label="v2"];
          3 [label="v3"];
          2 -> 3;
        }
        """);
  }

  @Test
  void theUndirectedReadingWritesEachReciprocalPairOnce() {
    // 0 <-> 1 stored both ways, 2 -> 1 stored from the higher node only, 2 -> 2 a self-loop.
    var graph = TestGraphs.of(3, 0, 1, 1, 0, 2, 1, 2, 2);
    int[] seen = new int[4];
    String dot = DotWriter.of(graph)
        .undirected()
        .edgeLabel(edge -> {
          seen[edge]++;
          return null;
        })
        .writeToString();
    assertThat(dot).isEqualTo("""
        graph {
          0 -- 1;
          2 -- 1;
          2 -- 2;
        }
        """);
    assertThat(seen).containsExactly(1, 0, 1, 1);
  }

  @Test
  void writingTwiceGivesTheSameText() throws IOException {
    var graph = RandomGraphs.gnp(40, 0.1, 5);
    var writer = DotWriter.of(graph).nodeLabel(node -> "n" + node);
    var first = new StringBuilder();
    writer.write(first);
    assertThat(writer.writeToString()).isEqualTo(first.toString());
    assertThat(first.toString().lines().filter(line -> line.contains(" -> ")).count())
        .isEqualTo(graph.edgeCount());
  }

  @Test
  void rejectsInvalidAttributeNamesAndForeignPartitions() {
    assertThatThrownBy(() -> DotWriter.of(TestGraphs.path(2)).graphAttribute("rank dir", "LR"))
        .isInstanceOf(IllegalArgumentException.class);
    var writer = DotWriter.of(TestGraphs.path(2))
        .nodeAttributes((node, attributes) -> attributes.put("bad-name", "x"));
    assertThatThrownBy(writer::writeToString).isInstanceOf(IllegalArgumentException.class);
    var other = Components.sccsOf(TestGraphs.path(3));
    assertThatThrownBy(() -> DotWriter.of(TestGraphs.path(2)).clusters(other, scc -> null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anEmptyGraph() {
    assertThat(DotWriter.of(TestGraphs.empty()).writeToString()).isEqualTo("digraph {\n}\n");
  }

  @Test
  void filterAndClustersTogether() {
    // Groups {0, 1} and {2}; the filter drops 2, so its cluster disappears, and drops 1.
    var graph = TestGraphs.path(4);
    var partition = NodePartition.of(graph, new int[] {0, 0, 1, -1}, 2);
    String dot = DotWriter.of(graph)
        .clusters(partition, group -> "g" + group)
        .nodeFilter(node -> node == 0 || node == 3)
        .writeToString();
    assertThat(dot).isEqualTo("""
        digraph {
          subgraph cluster_0 {
            label="g0";
            0;
          }
          3;
        }
        """);
  }

  @Test
  void nullValuesAreSkippedAndAttributesReplaceTheLabel() {
    String dot = DotWriter.of(TestGraphs.path(2))
        .edgeLabel(edge -> "plain")
        .edgeAttributes((edge, attributes) -> {
          attributes.put("label", "replaced");
          attributes.put("color", null);
        })
        .writeToString();
    assertThat(dot).contains("  0 -> 1 [label=\"replaced\"];\n");
  }

  @Test
  void nodeHooksRunOncePerWrittenNode() {
    var graph = TestGraphs.of(3, 0, 1);
    var partition = NodePartition.of(graph, new int[] {0, -1, -1}, 1);
    int[] labels = new int[3];
    int[] hooks = new int[3];
    DotWriter.of(graph)
        .clusters(partition, group -> null)
        .nodeLabel(node -> {
          labels[node]++;
          return "v" + node;
        })
        .nodeAttributes((node, attributes) -> hooks[node]++)
        .writeToString();
    assertThat(labels).containsExactly(1, 1, 1);
    assertThat(hooks).containsExactly(1, 1, 1);
  }

  @Test
  void rejectsDotKeywordsAsAttributeNames() {
    for (String keyword : new String[] {"node", "Edge", "graph", "DIGRAPH", "subgraph", "strict"}) {
      assertThatThrownBy(() -> DotWriter.of(TestGraphs.path(2)).graphAttribute(keyword, "x"))
          .as(keyword).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void aLoneCarriageReturnIsALineBreak() {
    String dot = DotWriter.of(TestGraphs.isolated(1)).nodeLabel(node -> "a\rb\r\nc").writeToString();
    assertThat(dot).contains("label=\"a\\nb\\nc\"");
  }
}
