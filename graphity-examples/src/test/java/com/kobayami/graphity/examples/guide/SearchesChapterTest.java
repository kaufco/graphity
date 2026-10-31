package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.examples.SmallProgram;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Writing searches of your own". */
class SearchesChapterTest {

  @Test
  void descendantsBottomUp() {
    assertThat(WritingSearches.descendants()).containsExactly(9, 4, 3, 2, 0, 2, 0, 0, 0, 0);
  }

  @Test
  void withoutStorage() {
    WritingSearches.WithoutStorage search = WritingSearches.withoutStorage();
    assertThat(search.reached()).containsExactly(
        "main", "parseArgs", "run", "load", "parse", "save", "format", "show");
    assertThat(search.recursive()).containsExactly("parse -> parse", "format -> format");
    // The pictures: the grey frame of the package storage holds read and write.
    String[] names = SmallProgram.methodNames();
    int[] packageOf = SmallProgram.packageOf();
    int storage = SmallProgram.nodeOf(SmallProgram.packageNames(), "storage");
    assertThat(IntStream.range(0, names.length).filter(method -> packageOf[method] == storage)
        .mapToObj(method -> names[method])).containsExactly("read", "write");
  }

  @Test
  void byDepth() {
    assertThat(WritingSearches.byDepth()).containsExactly(
        List.of("main"), List.of("parseArgs", "run"), List.of("load", "save", "show"),
        List.of("parse", "read", "format", "write"));
  }

  @Test
  void fromArrays() {
    WritingSearches.FromArrays fromArrays = WritingSearches.fromArrays();
    assertThat(fromArrays.sitesIn()).containsExactly(2, 0, 4, 2, 2, 1, 2, 1, 0, 0);
    assertThat(fromArrays.callerCount()).containsExactly(0, 1, 1, 1, 1, 1, 2, 3, 1, 1);
    assertThat(fromArrays.sitesFromOthers()).containsExactly(0, 1, 1, 1, 1, 2, 1, 2, 1, 1);
  }
}
