package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Storing a graph in a file together with the data on its nodes, and loading both again.
 */
final class Storing {

  private Storing() {}

  /** A graph with the names of its nodes. */
  record Named(Graph graph, String[] names) {}

  /** Writes {@code calls} to {@code file}, followed by the names of the methods. */
  static void store(Graph calls, String[] names, Path file) throws IOException {
    // @start region="writeTo"
    try (var out =
        new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
      calls.writeTo(out);
      for (String name : names) {
        out.writeUTF(name);
      }
    }
    // @end
  }

  /** Reads what {@link #store} wrote. */
  static Named load(Path file) throws IOException {
    // @start region="readFrom"
    Graph calls;
    String[] names;
    try (var in =
        new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
      calls = Graph.readFrom(in);    // equal to the graph written, edge indices included
      names = new String[calls.nodeCount()];
      for (int method = 0; method < names.length; method++) {
        names[method] = in.readUTF();
      }
    }
    // @end
    return new Named(calls, names);
  }
}
