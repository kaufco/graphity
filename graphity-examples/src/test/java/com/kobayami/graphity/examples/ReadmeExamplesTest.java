package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Checks that every Java example in the README is a region of the examples in this module, so that
 * it compiles and does what its comments say. A marker {@code <!-- example: File.java#region -->}
 * names the region, or several separated by spaces, directly before the code block. The imports at
 * the top of a block must be imports of the named files; the rest equals the regions, without their
 * common indentation, separated by a blank line.
 */
class ReadmeExamplesTest {

  private static final Pattern BLOCK =
      Pattern.compile("(?:<!-- example: ([^>]*?) -->\\n)?```java\\n(.*?)```", Pattern.DOTALL);

  // A line with nothing but the start or the end of a region.
  private static final Pattern MARKUP =
      Pattern.compile("\\s*// @(start|end)(?: region=\"([^\"]*)\")?\\s*");

  @Test
  void everyJavaBlockOfTheReadmeIsARegionOfTheExamples() throws IOException {
    String readme = resource("README.md");
    Matcher block = BLOCK.matcher(readme);
    int count = 0;
    List<String> failures = new ArrayList<>();
    while (block.find()) {
      count++;
      String line = "README.md:" + (readme.substring(0, block.start()).split("\n", -1).length);
      if (block.group(1) == null) {
        failures.add(line + ": a Java block without an example marker");
        continue;
      }
      List<String> imports = new ArrayList<>();
      List<String> body = new ArrayList<>();
      for (String text : block.group(2).split("\n", -1)) {
        (text.startsWith("import ") ? imports : body).add(text);
      }
      List<String> regions = new ArrayList<>();
      List<String> files = new ArrayList<>();
      for (String reference : block.group(1).trim().split(" +")) {
        String[] parts = reference.split("#");
        String source = optionalResource("example-sources/com/kobayami/graphity/examples/" + parts[0]);
        String region = source == null ? null : region(source, parts[1]);
        if (source != null) {
          files.add(source);
        }
        if (region == null) {
          failures.add(line + ": no region " + reference);
        } else {
          regions.add(region);
        }
      }
      for (String text : imports) {
        if (files.stream().noneMatch(source -> source.contains("\n" + text + "\n"))) {
          failures.add(line + ": " + text + " is not an import of " + block.group(1));
        }
      }
      String expected = String.join("\n\n", regions);
      String actual = dedent(String.join("\n", body));
      if (regions.size() == block.group(1).trim().split(" +").length && !actual.equals(expected)) {
        failures.add(line + ": differs from " + block.group(1) + "\n--- README\n" + actual
            + "\n--- examples\n" + expected);
      }
    }
    assertThat(count).isPositive();
    assertThat(failures).isEmpty();
  }

  // The lines of a region, as the Javadoc tool shows it: from "// @start region="name"" to the
  // "// @end" that closes it, without the markup lines of regions nested in it and without their
  // common indentation. "// @end" closes the region started last, "// @end region="name"" the
  // region of that name.
  private static @Nullable String region(String source, String name) {
    List<String> open = new ArrayList<>();
    List<String> lines = new ArrayList<>();
    for (String line : source.split("\n", -1)) {
      Matcher markup = MARKUP.matcher(line);
      if (!markup.matches()) {
        if (open.contains(name)) {
          lines.add(line);
        }
        continue;
      }
      String region = markup.group(2);
      if (markup.group(1).equals("start")) {
        open.add(region == null ? "" : region);
      } else {
        String closed = region == null ? open.get(open.size() - 1) : region;
        open.remove(closed);
        if (closed.equals(name)) {
          return dedent(String.join("\n", lines));
        }
      }
    }
    return null;
  }

  private static String dedent(String text) {
    String[] lines = text.strip().isEmpty() ? new String[0] : text.split("\n", -1);
    int indent = Integer.MAX_VALUE;
    for (String line : lines) {
      if (!line.isBlank()) {
        indent = Math.min(indent, line.length() - line.stripLeading().length());
      }
    }
    var result = new StringBuilder();
    for (String line : lines) {
      result.append(line.isBlank() ? "" : line.substring(indent).stripTrailing()).append('\n');
    }
    return result.toString().strip();
  }

  private static String resource(String name) throws IOException {
    String text = optionalResource(name);
    assertThat(text).as(name).isNotNull();
    return text;
  }

  private static @Nullable String optionalResource(String name) throws IOException {
    try (InputStream in = ReadmeExamplesTest.class.getClassLoader().getResourceAsStream(name)) {
      return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
