package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Showing graphs": the exact text each writer produces. */
class ShowingChapterTest {

  @Test
  void text() {
    ShowingGraphs.Text text = ShowingGraphs.text();
    assertThat(text.full()).isEqualTo("""
        0 -> [1]
        1 -> [3, 4, 6]
        2 -> [6]
        3 -> [5]
        4 -> [8]
        5 -> [6]
        6 -> [7]
        7 -> [8]
        8 -> []
        """);
    assertThat(text.brief()).isEqualTo(
        "Graph[nodes=9, edges=10] {0 -> [1], 1 -> [3, 4, 6], 2 -> [6], 3 -> [5], 4 -> [8], 5 -> [6], 6 -> [7], 7 -> [8], 8 -> []}");
  }

  @Test
  void dot() throws IOException {
    assertThat(ShowingGraphs.dot()).isEqualTo("""
        digraph {
          rankdir="LR";
          0 [label="app"];
          1 [label="cli"];
          2 [label="ui"];
          3 [label="service"];
          4 [label="model"];
          5 [label="storage"];
          6 [label="json"];
          7 [label="util"];
          0 -> 1;
          0 -> 2;
          0 -> 3;
          1 -> 7;
          2 -> 3;
          2 -> 4;
          2 -> 7;
          3 -> 4;
          3 -> 5;
          4 -> 5;
          5 -> 4;
          5 -> 6;
          5 -> 7;
          6 -> 7;
        }
        """);
  }

  @Test
  void dotStyled() {
    assertThat(ShowingGraphs.dotStyled()).isEqualTo("""
        digraph {
          subgraph cluster_3 {
            label="SCC 3";
            4 [label="model"];
            5 [label="storage"];
          }
          0 [label="app"];
          1 [label="cli"];
          2 [label="ui"];
          3 [label="service"];
          6 [label="json"];
          0 -> 1;
          0 -> 2;
          0 -> 3;
          2 -> 3;
          2 -> 4;
          3 -> 4;
          3 -> 5;
          4 -> 5;
          5 -> 4 [style="dashed", color="red"];
          5 -> 6;
        }
        """);
  }

  @Test
  void mermaid() {
    assertThat(ShowingGraphs.mermaid()).isEqualTo("""
        flowchart LR
          n0["generate"]
          n1["compile"]
          n2["lint"]
          n3["compileTests"]
          n4["docs"]
          n5["test"]
          n6["package"]
          n7["sign"]
          n8(["publish"])
          n0 --> n1
          n1 --> n3
          n1 --> n4
          n1 --> n6
          n2 --> n6
          n3 --> n5
          n4 --> n8
          n5 --> n6
          n6 --> n7
          n7 --> n8
        """);
  }

  @Test
  void mermaidStyled() {
    assertThat(ShowingGraphs.mermaidStyled()).isEqualTo("""
        flowchart TD
          classDef focus stroke:#8b5cf6,stroke-width:3px
          subgraph c2["SCC 2"]
            n5["5#58; expression"]
            n6["6#58; term"]
            n7["7#58; factor"]
          end
          subgraph c4["SCC 4"]
            n1["1#58; statement"]
            n2["2#58; block"]
            n3["3#58; ifStatement"]
          end
          n0["0#58; program"]
          n4["4#58; assignment"]
          n8["8#58; name"]
          n9["9#58; number"]
          n0 --> n1
          n1 --> n2
          n1 --> n3
          n1 --> n4
          n2 --> n1
          n3 --> n1
          n3 --> n5
          n4 --> n5
          n4 --> n8
          n5 --> n6
          n6 --> n7
          n7 --> n5
          n7 --> n8
          n7 --> n9
          class n0 focus
          linkStyle 1 stroke:#f59e0b,stroke-width:3px
          linkStyle 2 stroke:#f59e0b,stroke-width:3px
          linkStyle 4 stroke:#f59e0b,stroke-width:3px
          linkStyle 5 stroke:#f59e0b,stroke-width:3px
          linkStyle 9 stroke:#f59e0b,stroke-width:3px
          linkStyle 10 stroke:#f59e0b,stroke-width:3px
          linkStyle 11 stroke:#f59e0b,stroke-width:3px
        """);
  }
}
