package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.GraphDiff;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallProgram;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Graphs stored in a file, hashed and compared. */
final class StoringAndComparing {

  private StoringAndComparing() {}

  /** What the example reads after storing and loading. */
  record Stored(Graph loaded, boolean same, long size) {}

  /** Returns the packages after a round trip through {@code file}. */
  static Stored roundTrip(Path file) throws IOException {
    Graph dependencies = SmallProgram.dependencies();
    // @start region="writeRead"
    try (OutputStream out = Files.newOutputStream(file)) {
      dependencies.writeTo(out);
    }
    Graph loaded;
    try (InputStream in = Files.newInputStream(file)) {
      loaded = Graph.readFrom(in);
    }
    boolean same = loaded.equals(dependencies);   // true
    long size = Files.size(file);                 // 100 - bytes, for 8 nodes and 14 edges
    // @end
    return new Stored(loaded, same, size);
  }

  /** The next version of the packages: storage no longer uses model, ui uses storage. */
  static Graph nextVersion() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    int ui = SmallProgram.nodeOf(names, "ui");
    int model = SmallProgram.nodeOf(names, "model");
    int storage = SmallProgram.nodeOf(names, "storage");
    var builder = new GraphBuilder();
    builder.addNodes(dependencies.nodeCount());
    dependencies.forEachEdge((source, target, edge) -> {
      if (source != storage || target != model) {
        builder.addEdge(source, target);
      }
    });
    builder.addEdge(ui, storage);
    return builder.build();
  }

  /** Hashes compared, and the graph built a second time. */
  record Hashes(Graph rebuilt, boolean sameForEqualGraphs,
      boolean differentForNextVersion) {}

  /** Returns how the hashes of three graphs compare. */
  static Hashes hashes() {
    Graph dependencies = SmallProgram.dependencies();
    Graph nextVersion = nextVersion();
    // @start region="topologyHash"
    // The same graph built a second time, from its edges as pairs in another order, with
    // the pair {2, 4} given twice:
    int[][] uses = {
        {5, 7}, {2, 4}, {0, 3}, {6, 7}, {4, 5}, {2, 7}, {3, 5}, {1, 7}, {5, 4}, {0, 1},
        {2, 3}, {3, 4}, {5, 6}, {0, 2}, {2, 4}};
    var builder = new GraphBuilder();
    builder.addNodes(8);
    for (int[] pair : uses) {
      builder.addEdge(pair[0], pair[1]);
    }
    Graph rebuilt = builder.build();

    // The hash of dependencies, compared with those of the other two graphs:
    long hash = Graphs.topologyHash(dependencies);
    boolean sameForEqualGraphs = hash == Graphs.topologyHash(rebuilt);             // true
    boolean differentForNextVersion = hash != Graphs.topologyHash(nextVersion);    // true
    // @end
    return new Hashes(rebuilt, sameForEqualGraphs, differentForNextVersion);
  }

  /** Returns the edges added and removed between two versions of the packages. */
  static GraphDiff diff() {
    Graph dependencies = SmallProgram.dependencies();
    Graph nextVersion = nextVersion();
    // @start region="diff"
    GraphDiff diff = Graphs.diff(dependencies, nextVersion);
    Edges added = diff.addedEdges();       // ui -> storage, edge 6 of nextVersion
    Edges removed = diff.removedEdges();   // storage -> model, edge 10 of dependencies
    boolean unchanged = diff.isEmpty();    // false
    // @end
    return diff;
  }
}
