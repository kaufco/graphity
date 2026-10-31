package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.BfsDistances;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Reachability and distances". */
class ReachabilityChapterTest {

  @Test
  void reachableFromLoadAndFromLoadOrSave() {
    FindingReachable.Forward forward = FindingReachable.forward();
    assertThat(forward.fromLoad().toString()).isEqualTo("[3, 6, 8]");
    assertThat(forward.fromLoadOrSave().toString()).isEqualTo("[3, 4, 6, 7, 8, 9]");
  }

  @Test
  void methodsThatMayEndUpCallingFormat() {
    assertThat(FindingReachable.callersOfFormat().toString()).isEqualTo("[0, 2, 4, 5, 7]");
  }

  @Test
  void shortestChainFromMainToWrite() {
    ShortestPaths.FromMain fromMain = ShortestPaths.fromMain();
    assertThat(fromMain.distanceToWrite()).isEqualTo(3);
    assertThat(fromMain.pathToWrite().toString()).isEqualTo("[0, 2, 4, 9]");
    assertThat(fromMain.reachesParseArgs()).isTrue();
  }

  @Test
  void everyMethodIsAtMostThreeCallsAwayFromMain() {
    assertThat(ShortestPaths.farthestCall()).isEqualTo(3);
  }

  @Test
  void nearestOfLoadAndSave() {
    ShortestPaths.FromTwo fromTwo = ShortestPaths.fromTwo();
    assertThat(fromTwo.distanceToFormat()).isEqualTo(1);
    assertThat(fromTwo.pathToFormat().toString()).isEqualTo("[4, 7]");
    assertThat(fromTwo.reachesMain()).isFalse();
    // The picture after: the reached methods and the edges of the shortest paths to them.
    BfsDistances fromEither = fromTwo.fromEither();
    String[] names = SmallProgram.methodNames();
    assertThat(fromEither.pathTo(SmallProgram.nodeOf(names, "parse")).toString())
        .isEqualTo("[3, 6]");
    assertThat(fromEither.pathTo(SmallProgram.nodeOf(names, "read")).toString())
        .isEqualTo("[3, 8]");
    assertThat(fromEither.pathTo(SmallProgram.nodeOf(names, "write")).toString())
        .isEqualTo("[4, 9]");
    assertThat(IntStream.range(0, names.length).filter(fromEither::isReachable).boxed())
        .containsExactly(3, 4, 6, 7, 8, 9);
  }

  @Test
  void closureOfTheCalls() {
    TransitiveClosure.Read read = TransitiveClosure.closure();
    assertThat(read.edgeCount()).isEqualTo(23);
    assertThat(read.mainReachesWrite()).isTrue();
    assertThat(read.fromRun().toString()).isEqualTo("[3, 4, 5, 6, 7, 8, 9]");
    assertThat(read.parseOnCycle()).isTrue();
    assertThat(read.loadOnCycle()).isFalse();
    // The picture after: the calls and the edges the closure adds.
    assertThat(Graphs.adjacencyListOf(read.closure())).isEqualTo("""
        0 -> [1, 2, 3, 4, 5, 6, 7, 8, 9]
        1 -> []
        2 -> [3, 4, 5, 6, 7, 8, 9]
        3 -> [6, 8]
        4 -> [7, 9]
        5 -> [7]
        6 -> [6]
        7 -> [7]
        8 -> []
        9 -> []
        """);
  }

  @Test
  void reductionOfTheStepsLeavesOutCompileToPackage() {
    TransitiveReduction.Reduced reduced = TransitiveReduction.reduction();
    assertThat(reduced.edgeCount()).isEqualTo(9);
    assertThat(reduced.leftOut().sourceNodes().toString()).isEqualTo("[1]");
    assertThat(reduced.leftOut().targetNodes().toString()).isEqualTo("[6]");
    assertThat(reduced.leftOut().edgeIndices().toString()).isEqualTo("[3]");
    String[] names = SmallProgram.stepNames();
    int compile = SmallProgram.nodeOf(names, "compile");
    int pkg = SmallProgram.nodeOf(names, "package");
    assertThat(reduced.reduction().hasOutNode(compile, pkg)).isFalse();
    assertThat(SmallProgram.buildSteps().hasOutNode(compile, pkg)).isTrue();
    // The picture after: the steps without compile -> package.
    String steps = Graphs.adjacencyListOf(SmallProgram.buildSteps());
    assertThat(Graphs.adjacencyListOf(reduced.reduction()))
        .isEqualTo(steps.replace("1 -> [3, 4, 6]", "1 -> [3, 4]"));
  }
}
