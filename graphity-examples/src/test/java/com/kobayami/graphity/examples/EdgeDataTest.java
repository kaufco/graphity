package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class EdgeDataTest {

  @Test
  void edgeIndexOfCountsTwoCallSitesForRunToShowAndParseToParse() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int[] siteCount = EdgeData.callSitesPerEdge();
    assertThat(siteCount).hasSize(calls.edgeCount());
    calls.forEachEdge((caller, callee, edge) -> {
      String call = names[caller] + " -> " + names[callee];
      int expected = call.equals("run -> show") || call.equals("parse -> parse") ? 2 : 1;
      assertThat(siteCount[edge]).as(call).isEqualTo(expected);
    });
  }

  @Test
  void outEdgesPrintTheCalleesOfRunWithTheirCallSites() {
    var bytes = new ByteArrayOutputStream();
    EdgeData.calleesOfRun(new PrintStream(bytes, true, StandardCharsets.UTF_8));
    assertThat(bytes.toString(StandardCharsets.UTF_8).lines())
        .containsExactly("load: 1", "save: 1", "show: 2");
  }
}
