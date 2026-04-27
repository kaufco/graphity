package com.kobayami.graphity.benchmarks.generators;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.SortedIntView;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic generators driving the benchmark parameter matrix.
 *
 * <p>All generators take an explicit {@code seed} so that every (shape, n, seed)
 * triple maps to exactly one graph — required for reproducible benchmark runs.
 *
 * <p>These mirror the logic of {@code RandomGraphs} used in fuzz tests, but live
 * in main scope because benchmarks are an application, not a test dependency.
 */
public final class BenchmarkGraphs {

  /** Default seed; constant so that benchmark runs are reproducible across invocations. */
  public static final long DEFAULT_SEED = 0x6A097F8A3E4D92C1L;

  /** Average degree used for {@link GraphShape#GNP_SPARSE}. */
  private static final int SPARSE_AVG_DEGREE = 3;

  /** Average degree used for {@link GraphShape#GNP_DENSE}. */
  private static final int DENSE_AVG_DEGREE = 30;

  /** Maximum clique size in {@link GraphShape#CHAIN_OF_CLIQUES}; caps per-clique memory. */
  private static final int MAX_CLIQUE_SIZE = 32;

  /**
   * Salt for the shuffle permutation seed, XORed with the base seed so that the
   * permutation is correlated with but distinct from the generator's edge randomness.
   */
  private static final long SHUFFLE_SEED_SALT = 0xA5A5_C3C3_5A5A_3C3CL;
  private static final Map<String, Graph> DATASET_CACHE = new ConcurrentHashMap<>();

  private BenchmarkGraphs() {}

  public static Graph generate(GraphShape shape, int n) {
    return generate(shape, n, DEFAULT_SEED);
  }

  public static Graph generate(GraphShape shape, int n, long seed) {
    if (n < 2) throw new IllegalArgumentException("n must be >= 2, got " + n);
    return switch (shape) {
      case PATH -> path(n);
      case TREE -> randomTree(n, seed);
      case GNP_SPARSE -> gnm(n, (long) SPARSE_AVG_DEGREE * n, seed, /*acyclic=*/ false);
      case GNP_DENSE -> gnm(n, (long) DENSE_AVG_DEGREE * n, seed, /*acyclic=*/ false);
      case CHAIN_OF_CLIQUES -> chainOfCliques(n, seed);
      case DAG_GNP_SPARSE -> gnm(n, (long) SPARSE_AVG_DEGREE * n, seed, /*acyclic=*/ true);
      case DAG_GNP_DENSE -> gnm(n, (long) DENSE_AVG_DEGREE * n, seed, /*acyclic=*/ true);
      case PATH_SHUFFLED -> shuffle(path(n), seed ^ SHUFFLE_SEED_SALT);
      case TREE_SHUFFLED -> shuffle(randomTree(n, seed), seed ^ SHUFFLE_SEED_SALT);
      case GNP_SPARSE_SHUFFLED ->
          shuffle(gnm(n, (long) SPARSE_AVG_DEGREE * n, seed, false), seed ^ SHUFFLE_SEED_SALT);
      case GNP_DENSE_SHUFFLED ->
          shuffle(gnm(n, (long) DENSE_AVG_DEGREE * n, seed, false), seed ^ SHUFFLE_SEED_SALT);
      case CHAIN_OF_CLIQUES_SHUFFLED -> shuffle(chainOfCliques(n, seed), seed ^ SHUFFLE_SEED_SALT);
      case DAG_GNP_SPARSE_SHUFFLED ->
          shuffle(gnm(n, (long) SPARSE_AVG_DEGREE * n, seed, true), seed ^ SHUFFLE_SEED_SALT);
      case DAG_GNP_DENSE_SHUFFLED ->
          shuffle(gnm(n, (long) DENSE_AVG_DEGREE * n, seed, true), seed ^ SHUFFLE_SEED_SALT);
    };
  }

  /**
   * Resolves a benchmark input according to the fixture mode.
   *
   * <p>{@code synthetic}: generate from ({@code shape}, {@code nodeCount}).
   * {@code dataset}: load edge-list from {@code datasetPath}.
   */
  public static Graph fromInput(
      GraphShape shape,
      int nodeCount,
      String inputMode,
      String datasetPath,
      boolean requireDag) {
    String mode = (inputMode == null || inputMode.isBlank()) ? "synthetic" : inputMode.trim().toLowerCase();
    Graph graph;
    graph = switch (mode) {
      case "synthetic" -> generate(shape, nodeCount);
      case "dataset" -> loadDataset(datasetPath);
      default -> throw new IllegalArgumentException("unsupported inputMode: " + inputMode);
    };
    if (requireDag && !Graphs.isDag(graph)) {
      throw new IllegalArgumentException("dataset graph is not a DAG: " + datasetPath);
    }
    return graph;
  }

  /**
   * Loads a dataset from normalized text edge-list format:
   * {@code <source> <target>} per line, optional {@code #}-comments.
   */
  public static Graph loadDataset(String datasetPath) {
    if (datasetPath == null || datasetPath.isBlank()) {
      throw new IllegalArgumentException("datasetPath must be non-empty for inputMode=dataset");
    }
    Path path = Path.of(datasetPath).toAbsolutePath().normalize();
    return DATASET_CACHE.computeIfAbsent(path.toString(), ignored -> readDataset(path));
  }

  private static Graph readDataset(Path path) {
    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("dataset file not found: " + path);
    }
    GraphBuilder builder = new GraphBuilder();
    Long2IntOpenHashMap idMap = new Long2IntOpenHashMap();
    idMap.defaultReturnValue(-1);

    try (BufferedReader reader = Files.newBufferedReader(path)) {
      String line;
      int lineNo = 0;
      while ((line = reader.readLine()) != null) {
        lineNo++;
        String s = line.trim();
        if (s.isEmpty() || s.startsWith("#")) {
          continue;
        }
        String[] parts = s.split("\\s+");
        if (parts.length != 2) {
          throw new IllegalArgumentException("invalid dataset line " + lineNo + " in " + path + ": " + line);
        }
        long sourceRaw;
        long targetRaw;
        try {
          sourceRaw = Long.parseLong(parts[0]);
          targetRaw = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("invalid numeric ids at line " + lineNo + " in " + path, e);
        }
        int source = getOrCreateId(builder, idMap, sourceRaw);
        int target = getOrCreateId(builder, idMap, targetRaw);
        builder.addEdge(source, target);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("failed to read dataset file: " + path, e);
    }

    if (idMap.isEmpty()) {
      throw new IllegalArgumentException("dataset contains no edges/nodes: " + path);
    }
    return builder.build();
  }

  private static int getOrCreateId(GraphBuilder builder, Long2IntOpenHashMap map, long externalId) {
    int existing = map.get(externalId);
    if (existing >= 0) {
      return existing;
    }
    int created = builder.addNode();
    map.put(externalId, created);
    return created;
  }

  /** Directed path 0 → 1 → … → n-1. */
  private static Graph path(int n) {
    GraphBuilder b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 0; i < n - 1; i++) {
      b.addEdge(i, i + 1);
    }
    return b.build();
  }

  /** Random out-tree rooted at 0: parent of node i is uniform in [0, i-1]. */
  private static Graph randomTree(int n, long seed) {
    SplittableRandom rnd = new SplittableRandom(seed);
    GraphBuilder b = new GraphBuilder();
    b.addNodes(n);
    for (int i = 1; i < n; i++) {
      int parent = rnd.nextInt(i);
      b.addEdge(parent, i);
    }
    return b.build();
  }

  /**
   * Gnm: {@code m} directed edges chosen uniformly at random (with replacement; duplicates
   * are deduplicated by the builder).
   *
   * <p>If {@code acyclic} is true, only edges (i, j) with i &lt; j are emitted — the resulting
   * graph is guaranteed to be a DAG (topological order = natural node order).
   * If false, self-loops and back edges are allowed (Graphity supports both).
   */
  private static Graph gnm(int n, long m, long seed, boolean acyclic) {
    long maxEdges = acyclic ? (long) n * (n - 1) / 2 : (long) n * n;
    if (m > maxEdges) m = maxEdges;
    SplittableRandom rnd = new SplittableRandom(seed);
    GraphBuilder b = new GraphBuilder();
    b.addNodes(n);
    long added = 0;
    while (added < m) {
      int a = rnd.nextInt(n);
      int c = rnd.nextInt(n);
      if (acyclic) {
        if (a == c) continue;
        int src = Math.min(a, c);
        int tgt = Math.max(a, c);
        b.addEdge(src, tgt);
      } else {
        b.addEdge(a, c);
      }
      added++;
    }
    return b.build();
  }

  /**
   * Cliques of size ≤ {@link #MAX_CLIQUE_SIZE}, chained by a single edge from the last
   * node of clique i to the first node of clique i+1. Produces many mid-size SCCs that
   * stress cycle-aware algorithms (SCC, back edges).
   */
  private static Graph chainOfCliques(int n, long seed) {
    int k = (int) Math.min(MAX_CLIQUE_SIZE, Math.max(2, Math.sqrt(n)));
    int numCliques = n / k;
    int remainder = n - numCliques * k;
    GraphBuilder b = new GraphBuilder();
    b.addNodes(n);
    int base = 0;
    int prevLast = -1;
    for (int c = 0; c < numCliques; c++) {
      int size = k + (c < remainder ? 1 : 0);
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
          if (i != j) b.addEdge(base + i, base + j);
        }
      }
      if (prevLast >= 0) {
        b.addEdge(prevLast, base);
      }
      prevLast = base + size - 1;
      base += size;
    }
    // Trailing nodes (when n is not divisible evenly) remain isolated — acceptable for a
    // benchmark fixture. The `seed` parameter is accepted for API uniformity; the shape
    // itself is deterministic modulo n.
    return b.build();
  }

  /**
   * Applies a random permutation to node IDs while preserving the graph's topology.
   *
   * <p>Concretely, a Fisher-Yates shuffle produces a permutation {@code perm[]} such that
   * {@code perm[i]} is the new ID of original node {@code i}. Every edge {@code (u, v)}
   * is re-emitted as {@code (perm[u], perm[v])}, so structural properties (reachability,
   * cycles, SCCs, degree distribution) are invariant — only the correspondence between
   * node ID and adjacency is randomized.
   *
   * <p>This is the realistic worst case for compression schemes like WebGraph's
   * {@code BVGraph} that exploit ID-locality (gap, reference and interval encoding).
   * Shuffled versions of {@link GraphShape#CHAIN_OF_CLIQUES} and {@link GraphShape#PATH}
   * in particular collapse BVGraph's compression back toward the information-theoretic
   * bound for random adjacency.
   */
  private static Graph shuffle(Graph src, long seed) {
    int n = src.nodeCount();
    int[] perm = new int[n];
    for (int i = 0; i < n; i++) perm[i] = i;
    SplittableRandom rnd = new SplittableRandom(seed);
    for (int i = n - 1; i > 0; i--) {
      int j = rnd.nextInt(i + 1);
      int tmp = perm[i];
      perm[i] = perm[j];
      perm[j] = tmp;
    }
    GraphBuilder b = new GraphBuilder();
    b.addNodes(n);
    for (int v = 0; v < n; v++) {
      SortedIntView outs = src.outNodes(v);
      int deg = outs.size();
      int mapped = perm[v];
      for (int i = 0; i < deg; i++) {
        b.addEdge(mapped, perm[outs.getInt(i)]);
      }
    }
    return b.build();
  }
}
