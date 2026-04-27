package com.kobayami.graphity.benchmarks.webgraph;

import com.kobayami.graphity.SortedIntView;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Conversion helpers between a Graphity {@link com.kobayami.graphity.Graph} and a
 * {@link BVGraph} (the canonical WebGraph compressed immutable format).
 *
 * <p>Unlike {@link ArrayListMutableGraph}, which is an uncompressed mutable scaffold,
 * {@link BVGraph} keeps the adjacency data compressed in RAM (γ/δ/ζ-coded bit streams
 * with gap, interval and reference compression). Successor lookups decode on the fly,
 * so a loaded BVGraph is both the actual benchmark subject and an empirical measure
 * of WebGraph's in-memory compression rate.
 *
 * <p>BVGraph construction is intrinsically a two-phase pipeline:
 * <ol>
 *   <li>Build an {@link ImmutableGraph} view of the edges (here: via
 *       {@link ArrayListMutableGraph}).</li>
 *   <li>Run the BV encoder over that view ({@link BVGraph#store(ImmutableGraph, CharSequence)}),
 *       which emits {@code .graph}, {@code .offsets} and {@code .properties} files under
 *       a chosen basename.</li>
 *   <li>Re-load those files via {@link BVGraph#load(CharSequence)} — this produces the
 *       in-memory compressed representation we actually want to benchmark. After load,
 *       the files are no longer referenced and can be deleted.</li>
 * </ol>
 *
 * <p>All peer-library fixtures use a Graphity graph as the source of truth and convert
 * from it, guaranteeing byte-identical graph structure across libraries.
 */
public final class WebGraphGraphs {

  private WebGraphGraphs() {}

  /**
   * Builds a {@link BVGraph} structurally equivalent to the given Graphity graph.
   * A unique sub-directory of {@code tmpRoot} is used for the encoder's intermediate
   * files; those files are deleted immediately after load, so the returned graph does
   * not keep any file handle.
   */
  public static ImmutableGraph fromGraphity(com.kobayami.graphity.Graph g, Path tmpRoot) {
    try {
      Path dir = Files.createTempDirectory(tmpRoot, "bv-");
      try {
        int n = g.nodeCount();
        ArrayListMutableGraph mutable = new ArrayListMutableGraph(n);
        for (int v = 0; v < n; v++) {
          SortedIntView outs = g.outNodes(v);
          int deg = outs.size();
          for (int i = 0; i < deg; i++) {
            mutable.addArc(v, outs.getInt(i));
          }
        }
        String basename = dir.resolve("g").toString();
        BVGraph.store(mutable.immutableView(), basename);
        ImmutableGraph loaded = BVGraph.load(basename);
        return loaded;
      } finally {
        deleteTree(dir);
      }
    } catch (IOException e) {
      throw new UncheckedBuildException("failed to build BVGraph", e);
    }
  }

  /** Creates and returns a fresh workspace directory for BV intermediate files. */
  public static Path createTempWorkspace() throws IOException {
    return Files.createTempDirectory("graphity-webgraph-");
  }

  /** Recursively deletes {@code root}, swallowing any IOException (best-effort). */
  public static void deleteTree(Path root) {
    if (root == null || !Files.exists(root)) return;
    try (Stream<Path> walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
      });
    } catch (IOException ignored) {
      // best-effort cleanup — nothing actionable if it fails
    }
  }

  /** Thrown instead of {@link IOException} so fixtures can propagate cleanly. */
  public static final class UncheckedBuildException extends RuntimeException {
    public UncheckedBuildException(String msg, Throwable cause) { super(msg, cause); }
  }
}
