package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.TopOrder;
import com.kobayami.graphity.examples.SmallProgram;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;

/** The build steps in an order that respects every edge. */
final class SortingTopologically {

  private SortingTopologically() {}

  /** Returns the build steps in topological order, by name. */
  static List<String> runOrder() {
    Graph steps = SmallProgram.buildSteps();
    String[] names = SmallProgram.stepNames();
    // @start region="topOrder"
    IntList order = TopOrder.of(steps);   // [2, 0, 1, 4, 3, 5, 6, 7, 8]
    List<String> runOrder = order.intStream().mapToObj(step -> names[step]).toList();
    // runOrder: [lint, generate, compile, docs, compileTests, test, package, sign,
    //            publish]
    // @end
    return runOrder;
  }
}
