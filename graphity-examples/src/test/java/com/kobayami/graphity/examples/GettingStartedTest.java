package com.kobayami.graphity.examples;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GettingStartedTest {

  @Test
  void programPrintsTheCycleOfModelAndStorage() {
    PrintStream original = System.out;
    var bytes = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
      GettingStarted.main(new String[0]);
    } finally {
      System.setOut(original);
    }
    assertThat(bytes.toString(StandardCharsets.UTF_8).lines()).containsExactly("[model, storage]");
  }

  // The program keeps its data to itself, so the source must hold the data of SmallProgram: the
  // examples that continue the program in the README work on SmallProgram.
  @Test
  void programBuildsTheDependenciesOfSmallProgram() throws IOException {
    Graph dependencies = SmallProgram.dependencies();
    String names = Arrays.stream(SmallProgram.packageNames())
        .map(name -> '"' + name + '"')
        .collect(joining(", ", "String[] names = {", "};"));
    String uses = IntStream.range(0, dependencies.nodeCount())
        .mapToObj(pkg -> dependencies.outNodes(pkg).intStream()
            .mapToObj(Integer::toString)
            .collect(joining(", ", "{", "}")))
        .collect(joining(", ", "int[][] uses = {", "};"));
    assertThat(source("GettingStarted.java")).contains(names, uses);
  }

  @Test
  void theCycleIsTheNodes4And5ModelAndStorage() {
    assertThat(Graphs.findCycle(SmallProgram.dependencies()).toString()).isEqualTo("[4, 5]");
    assertThat(SmallProgram.packageNames()[4]).isEqualTo("model");
    assertThat(SmallProgram.packageNames()[5]).isEqualTo("storage");
  }

  private static String source(String file) throws IOException {
    try (InputStream in = GettingStartedTest.class.getClassLoader()
        .getResourceAsStream("example-sources/com/kobayami/graphity/examples/" + file)) {
      assertThat(in).as(file).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
