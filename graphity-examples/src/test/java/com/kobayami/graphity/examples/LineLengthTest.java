package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checks that no line of the examples is longer than 90 characters, counted with its indentation.
 * Longer lines wrap in the guide at its standard width; see the README of this module.
 */
class LineLengthTest {

  private static final int MAX_LINE = 90;

  @Test
  void noLineOfTheExamplesIsLongerThanTheLimit() throws IOException, URISyntaxException {
    URL root = LineLengthTest.class.getClassLoader().getResource("example-sources");
    assertThat(root).isNotNull();
    List<Path> sources;
    try (Stream<Path> files = Files.walk(Path.of(root.toURI()))) {
      sources = files.filter(file -> file.toString().endsWith(".java")).toList();
    }
    assertThat(sources).isNotEmpty();
    List<String> failures = new ArrayList<>();
    for (Path source : sources) {
      List<String> lines = Files.readAllLines(source);
      for (int i = 0; i < lines.size(); i++) {
        if (lines.get(i).length() > MAX_LINE) {
          failures.add(source + ":" + (i + 1));
        }
      }
    }
    assertThat(failures).isEmpty();
  }
}
