package com.kobayami.graphity.examples.guide;

import static com.kobayami.graphity.EdgeType.BACK;
import static com.kobayami.graphity.EdgeType.CROSS;
import static com.kobayami.graphity.EdgeType.FORWARD;
import static com.kobayami.graphity.EdgeType.TREE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.EdgeTypes;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.TopOrder;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Order and cycles". */
class OrderChapterTest {

  @Test
  void thePackagesHaveTheCycleOfModelAndStorage() {
    FindingCycles.Found found = FindingCycles.cycles();
    assertThat(found.dependenciesAcyclic()).isFalse();
    assertThat(found.cycle().toString()).isEqualTo("[4, 5]");
  }

  @Test
  void topologicalOrderOfTheSteps() {
    assertThat(SortingTopologically.runOrder()).containsExactly(
        "lint", "generate", "compile", "docs", "compileTests", "test", "package", "sign",
        "publish");
    assertThatThrownBy(() -> TopOrder.of(SmallProgram.dependencies()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void stagesOfTheSteps() {
    FindingLevels.Stages stages = FindingLevels.stages();
    assertThat(stages.levelOfTest()).isEqualTo(3);
    assertThat(stages.levelCount()).isEqualTo(7);
    assertThat(stages.stages()).containsExactly(
        List.of("generate", "lint"), List.of("compile"), List.of("compileTests", "docs"),
        List.of("test"), List.of("package"), List.of("sign"), List.of("publish"));
    // The text: no path connects docs (4), on level 2, and test (5), on level 3.
    assertThat(Graphs.reachableFrom(SmallProgram.buildSteps(), 4).contains(5)).isFalse();
    assertThat(Graphs.reachableFrom(SmallProgram.buildSteps(), 5).contains(4)).isFalse();
  }

  @Test
  void rootsOfTheSteps() {
    FindingRoots.Roots roots = FindingRoots.roots();
    assertThat(roots.roots().toString()).isEqualTo("[0, 2]");
    assertThat(roots.compileIsRoot()).isFalse();
  }

  @Test
  void edgeTypesOfThePackages() {
    ClassifyingEdges.Classified classified = ClassifyingEdges.classify();
    assertThat(classified.storageToModel()).isEqualTo(EdgeType.BACK);
    assertThat(classified.appToService()).isEqualTo(EdgeType.FORWARD);
    assertThat(edges(classified.crossEdges(), SmallProgram.packageNames()))
        .containsExactly("ui -> util", "storage -> util", "json -> util");
    // The picture after: the type of every edge, by edge index from 0 to 13.
    EdgeTypes types = classified.types();
    assertThat(SmallProgram.dependencies().edgeCount()).isEqualTo(14);
    assertThat(IntStream.range(0, 14).mapToObj(types::typeOf).toList()).containsExactly(
        TREE, TREE, FORWARD, TREE, TREE, FORWARD, CROSS, TREE, FORWARD, TREE, BACK, TREE, CROSS,
        CROSS);
    String[] names = SmallProgram.packageNames();
    assertThat(edges(Edges.of(types, TREE), names)).containsExactly(
        "app -> cli", "app -> ui", "cli -> util", "ui -> service", "service -> model",
        "model -> storage", "storage -> json");
    assertThat(edges(Edges.of(types, FORWARD), names))
        .containsExactly("app -> service", "ui -> model", "service -> storage");
    assertThat(edges(Edges.of(types, BACK), names)).containsExactly("storage -> model");
  }

  @Test
  void withoutBackEdgesThePackagesAreADag() {
    BreakingCycles.Broken broken = BreakingCycles.withoutBackEdges();
    assertThat(edges(broken.backEdges(), SmallProgram.packageNames()))
        .containsExactly("storage -> model");
    assertThat(broken.backEdges().edgeIndices().getInt(0)).isEqualTo(10);
    assertThat(broken.isDag()).isTrue();
    assertThat(broken.acyclic().edgeCount()).isEqualTo(13);
    assertThat(Graphs.isDag(broken.acyclic())).isTrue();
  }

  static List<String> edges(Edges edges, String[] names) {
    List<String> result = new ArrayList<>();
    for (int p = 0; p < edges.count(); p++) {
      result.add(names[edges.sourceNodes().getInt(p)] + " -> "
          + names[edges.targetNodes().getInt(p)]);
    }
    return result;
  }
}
