package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Graph;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoringTest {

  @Test
  void loadGivesTheGraphWithItsEdgeIndicesAndTheNames(@TempDir Path dir) throws IOException {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    Path file = dir.resolve("calls.bin");

    Storing.store(calls, names, file);
    Storing.Named loaded = Storing.load(file);

    assertThat(loaded.graph()).isEqualTo(calls);
    assertThat(Csr.outAdjacency(loaded.graph())).isEqualTo(Csr.outAdjacency(calls));
    assertThat(loaded.names()).containsExactly(names);
  }
}
