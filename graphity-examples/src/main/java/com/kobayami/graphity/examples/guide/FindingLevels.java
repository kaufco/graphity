package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.TopOrderAndLevels;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.ArrayList;
import java.util.List;

/** The build steps in stages: the steps of one stage can run at the same time. */
final class FindingLevels {

  private FindingLevels() {}

  /** The stages and two values read on the way. */
  record Stages(List<List<String>> stages, int levelOfTest, int levelCount) {}

  /** Returns the build steps grouped by level. */
  static Stages stages() {
    Graph steps = SmallProgram.buildSteps();
    String[] names = SmallProgram.stepNames();
    int test = SmallProgram.nodeOf(names, "test");
    // @start region="levels"
    TopOrderAndLevels levels = TopOrderAndLevels.of(steps);
    // 3 - three steps run before it, one after the other
    int levelOfTest = levels.levelOf(test);
    int levelCount = levels.levelCount();     // 7

    // One stage per level: the steps of a stage have no edge between them.
    List<List<String>> stages = new ArrayList<>();
    for (int level = 0; level < levelCount; level++) {
      stages.add(new ArrayList<>());
    }
    for (int step : levels.order()) {
      stages.get(levels.levelOf(step)).add(names[step]);
    }
    // stages: [[generate, lint], [compile], [compileTests, docs], [test], [package],
    //          [sign], [publish]]
    // @end
    return new Stages(stages, levelOfTest, levelCount);
  }
}
