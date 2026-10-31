package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartitionsTest {

  @Test
  void ofCondensesTheCallsToTheCallsBetweenPackages() {
    Graph packageCalls = Partitions.packageCalls();
    String[] packageNames = SmallProgram.packageNames();
    assertThat(packageCalls.nodeCount()).isEqualTo(packageNames.length);
    assertThat(MappingsTest.edgesByName(packageCalls, packageNames)).containsExactly(
        "app -> cli", "app -> ui", "app -> service",
        "ui -> model",
        "service -> model", "service -> storage");
  }

  @Test
  void edgesOfListsTheCallsFromServiceToModel() {
    String[] names = SmallProgram.methodNames();
    Graph calls = SmallProgram.calls();
    assertThat(Partitions.callsFromServiceToModel().toString()).isEqualTo("[5, 7]");
    List<String> behind = new ArrayList<>();
    Partitions.callsFromServiceToModel().forEach((int edge) ->
        behind.add(names[calls.edgeSource(edge)] + " -> " + names[calls.edgeTarget(edge)]));
    assertThat(behind).containsExactly("load -> parse", "save -> format");
  }

  @Test
  void reduceSumsTheCallSitesPerCallBetweenPackages() {
    Graph packageCalls = Partitions.packageCalls();
    String[] packageNames = SmallProgram.packageNames();
    int[] packageSiteCount = Partitions.callSitesPerPackageCall();
    assertThat(packageSiteCount).hasSize(packageCalls.edgeCount());
    List<String> counted = new ArrayList<>();
    packageCalls.forEachEdge((source, target, edge) -> counted.add(
        packageNames[source] + " -> " + packageNames[target] + ": " + packageSiteCount[edge]));
    assertThat(counted).containsExactlyInAnyOrder(
        "app -> ui: 2", "app -> service: 2", "service -> model: 2", "service -> storage: 2",
        "app -> cli: 1", "ui -> model: 1");
  }
}
