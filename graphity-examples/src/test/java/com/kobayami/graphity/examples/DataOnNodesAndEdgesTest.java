package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.examples.DataOnNodesAndEdges.CallData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataOnNodesAndEdgesTest {

  private static final CallData DATA = DataOnNodesAndEdges.callData();

  @Test
  void methodsHoldTheNameAndThePackageOfEachMethodByNodeId() {
    String[] packageNames = SmallProgram.packageNames();
    int[] packageOf = SmallProgram.packageOf();
    assertThat(DATA.names()).containsExactly(SmallProgram.methodNames());
    assertThat(DATA.packages()).hasSize(packageOf.length);
    for (int node = 0; node < packageOf.length; node++) {
      assertThat(DATA.packages()[node]).as(DATA.names()[node])
          .isEqualTo(packageNames[packageOf[node]]);
    }
  }

  @Test
  void siteCountIsTwoForRunToShowAndParseToParse() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    assertThat(DATA.siteCount()).hasSize(calls.edgeCount());
    calls.forEachEdge((caller, callee, edge) -> {
      String call = names[caller] + " -> " + names[callee];
      int expected = call.equals("run -> show") || call.equals("parse -> parse") ? 2 : 1;
      assertThat(DATA.siteCount()[edge]).as(call).isEqualTo(expected);
    });
  }

  @Test
  void theSubgraphOfLoadTakesTheMethodsAndTheCallSitesAlong() {
    Graph loading = DATA.loading();
    String[] names = DATA.loadingNames();
    assertThat(names).containsExactly("load", "parse", "read");
    assertThat(MappingsTest.edgesByName(loading, names))
        .containsExactly("load -> parse", "load -> read", "parse -> parse");
    assertThat(DATA.loadingSiteCount()).hasSize(loading.edgeCount());
    List<String> counted = new ArrayList<>();
    loading.forEachEdge((caller, callee, edge) -> counted.add(
        names[caller] + " -> " + names[callee] + ": " + DATA.loadingSiteCount()[edge]));
    assertThat(counted).containsExactlyInAnyOrder(
        "load -> parse: 1", "load -> read: 1", "parse -> parse: 2");
  }
}
