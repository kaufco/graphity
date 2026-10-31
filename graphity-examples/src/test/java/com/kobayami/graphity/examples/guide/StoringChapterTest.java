package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.GraphDiff;
import com.kobayami.graphity.examples.SmallProgram;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The examples of the chapter "Storing and comparing graphs". */
class StoringChapterTest {

  @Test
  void roundTrip(@TempDir Path dir) throws IOException {
    StoringAndComparing.Stored stored = StoringAndComparing.roundTrip(dir.resolve("deps.bin"));
    assertThat(stored.same()).isTrue();
    assertThat(stored.size()).isEqualTo(100);
  }

  @Test
  void hashes() {
    StoringAndComparing.Hashes hashes = StoringAndComparing.hashes();
    assertThat(hashes.rebuilt()).isEqualTo(SmallProgram.dependencies());
    assertThat(hashes.sameForEqualGraphs()).isTrue();
    assertThat(hashes.differentForNextVersion()).isTrue();
  }

  @Test
  void diff() {
    GraphDiff diff = StoringAndComparing.diff();
    String[] names = SmallProgram.packageNames();
    assertThat(OrderChapterTest.edges(diff.addedEdges(), names)).containsExactly("ui -> storage");
    assertThat(diff.addedEdges().edgeIndices().getInt(0)).isEqualTo(6);
    assertThat(OrderChapterTest.edges(diff.removedEdges(), names))
        .containsExactly("storage -> model");
    assertThat(diff.removedEdges().edgeIndices().getInt(0)).isEqualTo(10);
    assertThat(diff.isEmpty()).isFalse();
  }
}
