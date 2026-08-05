package com.kobayami.graphity.benchmarks.webgraph;

import com.kobayami.graphity.SortedIntView;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;

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

  /**
   * Arcs per batch during offline symmetrization. WebGraph allocates two {@code int[]} of this
   * size, so 4 M arcs cost 32 MB of scratch space and keep the batch count in the low hundreds
   * even for the largest dataset measured here.
   */
  private static final int SYMMETRIZE_BATCH_SIZE = 4 * 1024 * 1024;

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

  /**
   * Builds the symmetric closure of {@code g} as a {@link BVGraph} — the input WebGraph's
   * {@link it.unimi.dsi.webgraph.algo.ConnectedComponents} requires and cannot derive itself.
   *
   * <p>A BVGraph stores out-adjacency only, so the symmetrization has to materialize the
   * transpose. {@link Transform#symmetrizeOffline(ImmutableGraph, int, java.io.File)} does that
   * through external batches rather than in RAM, which is what makes the operation survive the
   * larger datasets. The result is then stored and re-loaded, exactly as
   * {@link #fromGraphity(com.kobayami.graphity.Graph, Path)} does for the plain graph: a union
   * view over a graph and its transpose would decode two bitstreams per successor list and is
   * not a representation a WebGraph user would keep.
   *
   * @param g the graph to symmetrize
   * @param tmpRoot workspace for the batch files and the stored graph; both are deleted before
   *     this method returns
   */
  public static ImmutableGraph symmetrized(ImmutableGraph g, Path tmpRoot) {
    try {
      Path dir = Files.createTempDirectory(tmpRoot, "sym-");
      try {
        ImmutableGraph symmetric =
            Transform.symmetrizeOffline(g, SYMMETRIZE_BATCH_SIZE, dir.toFile());
        String basename = dir.resolve("s").toString();
        BVGraph.store(symmetric, basename);
        return BVGraph.load(basename);
      } finally {
        deleteTree(dir);
      }
    } catch (IOException e) {
      throw new UncheckedBuildException("failed to symmetrize BVGraph", e);
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
