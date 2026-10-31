package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BuildingAndReadingTest {

  @Test
  void buildMergesTheCallSitesOfRunToShowAndOfParseToParse() {
    String[] names = SmallProgram.methodNames();
    List<SmallProgram.CallSite> callSites = SmallProgram.callSites();
    Graph calls = BuildingAndReading.callsFromCallSites();

    assertThat(calls).isEqualTo(SmallProgram.calls());
    assertThat(calls.nodeCount()).isEqualTo(names.length);
    assertThat(callSites).hasSize(14);
    assertThat(calls.edgeCount()).isEqualTo(12);

    Map<String, Integer> timesAdded = new HashMap<>();
    for (SmallProgram.CallSite site : callSites) {
      timesAdded.merge(names[site.caller()] + " -> " + names[site.callee()], 1, Integer::sum);
    }
    timesAdded.values().removeIf(times -> times == 1);
    assertThat(timesAdded)
        .containsOnly(Map.entry("run -> show", 2), Map.entry("parse -> parse", 2));
  }

  @Test
  void addGraphKeepsTheNodeIdsAndTheEdgeFromUiToStorageIsAdded() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    int ui = SmallProgram.nodeOf(names, "ui");
    int storage = SmallProgram.nodeOf(names, "storage");
    Graph nextVersion = BuildingAndReading.nextVersion();

    assertThat(dependencies.hasOutNode(ui, storage)).isFalse();
    assertThat(nextVersion.nodeCount()).isEqualTo(dependencies.nodeCount());
    assertThat(nextVersion.edgeCount()).isEqualTo(dependencies.edgeCount() + 1);
    dependencies.forEachEdge((source, target, edge) ->
        assertThat(nextVersion.hasOutNode(source, target)).isTrue());
    assertThat(nextVersion.hasOutNode(ui, storage)).isTrue();
  }

  @Test
  void loopCountsTheCallersOfEachMethod() {
    String[] names = SmallProgram.methodNames();
    int[] callerCount = BuildingAndReading.callerCounts();

    assertThat(callerCount).hasSize(names.length);
    for (int method = 0; method < names.length; method++) {
      int expected = switch (names[method]) {
        case "format" -> 3;
        case "parse" -> 2;
        case "main" -> 0;
        default -> 1;
      };
      assertThat(callerCount[method]).as(names[method]).isEqualTo(expected);
    }
  }

  @Test
  void edgesPrintTheRecursiveCallsWithTheirCallSites() {
    var bytes = new ByteArrayOutputStream();
    BuildingAndReading.recursiveCalls(new PrintStream(bytes, true, StandardCharsets.UTF_8));
    assertThat(bytes.toString(StandardCharsets.UTF_8).lines())
        .containsExactly("parse -> parse: 2", "format -> format: 1");
  }
}
