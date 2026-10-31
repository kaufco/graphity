package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.ArrayList;
import java.util.List;

/**
 * The packages in an order in which each comes after the packages it uses, despite a
 * cycle.
 */
final class OrderingDespiteCycles {

  private OrderingDespiteCycles() {}

  /** The build order, and a comparison of two packages in it. */
  record Order(List<List<String>> buildOrder, boolean jsonBeforeUi) {}

  /** Returns the packages grouped by SCC, in the order of the SCC IDs. */
  static Order buildOrder() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    int json = SmallProgram.nodeOf(names, "json");
    int ui = SmallProgram.nodeOf(names, "ui");
    // @start region="buildOrder"
    // model and storage use each other.
    Sccs sccs = Components.sccsOf(dependencies);

    // The SCCs in the order of their IDs: each after the SCCs it uses.
    List<List<String>> buildOrder = new ArrayList<>();
    for (int scc = 0; scc < sccs.sccCount(); scc++) {
      buildOrder.add(sccs.nodesOf(scc).intStream().mapToObj(p -> names[p]).toList());
    }
    // buildOrder: [[util], [cli], [json], [model, storage], [service], [ui], [app]]

    // Which of two packages comes first? Compare their SCC IDs.
    boolean jsonBeforeUi = sccs.sccOf(json) < sccs.sccOf(ui);   // true
    // @end
    return new Order(buildOrder, jsonBeforeUi);
  }
}
