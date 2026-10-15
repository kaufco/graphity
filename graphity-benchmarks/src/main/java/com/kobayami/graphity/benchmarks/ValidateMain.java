package com.kobayami.graphity.benchmarks;

import com.google.common.graph.Graph;
import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.EdgeTypes;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodePartition;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.TopOrder;
import com.kobayami.graphity.TopOrderAndLevels;
import com.kobayami.graphity.benchmarks.common.BenchEdgePairs;
import com.kobayami.graphity.benchmarks.controlflow.ControlFlowShape;
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import com.kobayami.graphity.benchmarks.guava.GuavaBackEdges;
import com.kobayami.graphity.benchmarks.guava.GuavaDfsTopOrder;
import com.kobayami.graphity.benchmarks.guava.GuavaEdgeTypes;
import com.kobayami.graphity.benchmarks.guava.GuavaGraphs;
import com.kobayami.graphity.benchmarks.guava.GuavaTarjanScc;
import com.kobayami.graphity.benchmarks.guava.GuavaTopOrderAndLevels;
import com.kobayami.graphity.benchmarks.guava.GuavaUnionFindWcc;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTBackEdges;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTEdgeTypes;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTGraphs;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTTopOrderAndLevels;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphBackEdges;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphDfsTopOrder;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphEdgeTypes;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphGraphs;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphTopOrderAndLevels;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.algo.ConnectedComponents;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Quick-fail semantic validation for benchmark matrix cases.
 */
public final class ValidateMain {

  /**
   * Algorithms that only accept a DAG. For those, a dataset is loaded without its self-loops, so
   * that validation sees the same graph the JMH fixtures measure — there, the equivalent switch is
   * the {@code requireDag} flag of the {@code *DagFixture} classes. Keep this in sync with
   * {@code DAG_ALGOS} in run-benchmarks.sh, which rejects a matrix case that pairs one of these
   * algorithms with {@code dataset.dag=false}.
   */
  private static final Set<String> DAG_ONLY_ALGORITHMS = Set.of("toporder", "toporderandlevels");

  /**
   * Algorithms measured on {@link ControlFlowShape} graphs. They are Graphity-only, so there
   * is no peer output to cross-validate; generating the fixture graphs is the smoke test.
   * Keep this in sync with {@code CONTROL_FLOW_ALGOS} in run-benchmarks.sh.
   */
  private static final Set<String> CONTROL_FLOW_ALGORITHMS = Set.of(
      "dominators",
      "dominancefrontiers",
      "iterateddominancefrontier",
      "loops",
      "dominatorancestry",
      "preorderancestry");

  private ValidateMain() {}

  public static void run(String[] args) throws Exception {
    String algorithm = null;
    String librariesCsv = null;
    String nodesCsv = null;
    String shapesCsv = null;
    String datasetsCsv = null;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--algorithm" -> algorithm = args[++i];
        case "--libraries" -> librariesCsv = args[++i];
        case "--nodes" -> nodesCsv = args[++i];
        case "--shapes" -> shapesCsv = args[++i];
        case "--datasets" -> datasetsCsv = args[++i];
        case "-h", "--help" -> {
          printUsage();
          return;
        }
        default -> fail("unknown argument: " + args[i]);
      }
    }

    if (isBlank(algorithm) || isBlank(librariesCsv)) {
      fail("missing required args: --algorithm --libraries");
    }

    List<String> libraries = parseStringCsv(librariesCsv);
    boolean hasSynthetic = !isBlank(nodesCsv) || !isBlank(shapesCsv);
    boolean hasDatasets = !isBlank(datasetsCsv);
    if (hasSynthetic == hasDatasets) {
      fail("use either (--nodes and --shapes) OR --datasets");
    }

    Path wgTmpRoot = WebGraphGraphs.createTempWorkspace();
    try {
      if (hasSynthetic) {
        if (isBlank(nodesCsv) || isBlank(shapesCsv)) {
          fail("synthetic mode requires both --nodes and --shapes");
        }
        int[] nodeCounts = parseIntCsv(nodesCsv);
        if (CONTROL_FLOW_ALGORITHMS.contains(algorithm)) {
          List<ControlFlowShape> shapes = parseControlFlowShapes(shapesCsv);
          for (int n : nodeCounts) {
            for (ControlFlowShape shape : shapes) {
              // Graphity-only; correctness is covered by the control-flow unit tests.
              shape.generateAll(n, 42);
            }
          }
          System.err.printf(Locale.ROOT,
              "validate ok: algorithm=%s libraries=%s nodes=%s shapes=%s%n",
              algorithm, libraries, Arrays.toString(nodeCounts), shapes);
        } else {
          List<GraphShape> shapes = parseShapes(shapesCsv);
          for (int n : nodeCounts) {
            for (GraphShape shape : shapes) {
              com.kobayami.graphity.Graph template = BenchmarkGraphs.generate(shape, n);
              validateCase(algorithm, libraries, shape, n, template, wgTmpRoot);
            }
          }
          System.err.printf(Locale.ROOT,
              "validate ok: algorithm=%s libraries=%s nodes=%s shapes=%s%n",
              algorithm, libraries, Arrays.toString(nodeCounts), shapes);
        }
      } else {
        List<String> datasetPaths = parseStringCsv(datasetsCsv);
        boolean dropSelfLoops = DAG_ONLY_ALGORITHMS.contains(algorithm);
        for (String datasetPath : datasetPaths) {
          com.kobayami.graphity.Graph template = BenchmarkGraphs.loadDataset(datasetPath, dropSelfLoops);
          validateCase(algorithm, libraries, null, template.nodeCount(), template, wgTmpRoot);
        }
        System.err.printf(Locale.ROOT,
            "validate ok: algorithm=%s libraries=%s datasets=%s%n",
            algorithm, libraries, new LinkedHashSet<>(datasetPaths));
      }
    } finally {
      WebGraphGraphs.deleteTree(wgTmpRoot);
    }
  }

  private static void validateCase(
      String algorithm,
      List<String> libraries,
      GraphShape shape,
      int nodeCount,
      com.kobayami.graphity.Graph template,
      Path wgTmpRoot) throws Exception {
    switch (algorithm) {
      case "build" -> {
        // No semantic output to cross-validate here.
      }
      case "reorder" -> {
        // Pure structural transform; correctness of reindex + remapped is covered by the
        // NodeMapping/Graphs unit tests. Nothing to cross-validate here.
      }
      case "biadjacency" -> {
        // Pure structural transform. Its correctness is covered where it matters: the
        // "toporderandlevels" case below runs Kahn over the bi-adjacent graph as well and
        // requires the same levels as the uni-adjacent path.
      }
      case "outtraversal", "intraversal" -> {
        // Sums node IDs over the adjacency lists; nothing to cross-validate.
      }
      case "symmetrize" -> {
        // Same: a wrong symmetric closure would make WebGraph's components disagree with
        // Graphity's under the "wcc" case, which is where the symmetrization is exercised.
      }
      case "scc" -> validateScc(libraries, template, wgTmpRoot);
      case "wcc" -> validateWcc(libraries, template, wgTmpRoot);
      case "ccs" -> validateCcs(template);
      case "condense" -> validateCondense(template);
      case "toporder" -> validateTopOrder(libraries, template, wgTmpRoot);
      case "toporderandlevels" -> validateTopOrderAndLevels(libraries, template, wgTmpRoot);
      case "backedges" -> validateBackEdges(libraries, template, wgTmpRoot);
      case "edgetypes" -> validateEdgeTypes(libraries, template, wgTmpRoot);
      default -> fail("unsupported algorithm: " + algorithm);
    }
  }

  private static void validateScc(List<String> libraries, com.kobayami.graphity.Graph template, Path wgTmpRoot) throws Exception {
    Map<String, String> canonicalByLib = new HashMap<>();
    for (String lib : libraries) {
      switch (lib) {
        case "graphity" -> canonicalByLib.put(lib, canonicalPartition(mappingFromNodePartition(Components.sccsOf(template), template.nodeCount())));
        case "jgrapht" -> {
          var g = JGraphTGraphs.fromGraphity(template);
          var scc = new org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector<>(g).stronglyConnectedSets();
          canonicalByLib.put(lib, canonicalPartition(mappingFromSetPartition(scc, template.nodeCount())));
        }
        case "guava" -> {
          Graph<Integer> g = GuavaGraphs.fromGraphity(template);
          Map<Integer, Integer> m = new GuavaTarjanScc(g).compute();
          canonicalByLib.put(lib, canonicalPartition(mappingFromMap(m, template.nodeCount())));
        }
        case "webgraph" -> {
          ImmutableGraph g = WebGraphGraphs.fromGraphity(template, wgTmpRoot);
          canonicalByLib.put(lib, canonicalPartition(webGraphTarjanMapping(g)));
        }
        default -> fail("unsupported library for scc validation: " + lib);
      }
    }
    ensureAllEqual("scc", canonicalByLib);
  }

  /**
   * Cross-validates the weakly connected components of every requested library. The three
   * implementations are Union-Find, JGraphT's {@code ConnectivityInspector} and a breadth-first
   * visit over the symmetric closure — a shared wrong answer would take three independent errors
   * pointing the same way.
   *
   * <p>Group IDs are not comparable across libraries (WebGraph numbers by visit round, Graphity
   * by order of discovery), so each partition is canonicalised into sorted member lists first.
   */
  private static void validateWcc(List<String> libraries, com.kobayami.graphity.Graph template, Path wgTmpRoot) throws Exception {
    Map<String, String> canonicalByLib = new HashMap<>();
    for (String lib : libraries) {
      switch (lib) {
        case "graphity" -> recordWcc(canonicalByLib, lib, template,
            mappingFromNodePartition(Components.wccsOf(template), template.nodeCount()));
        case "jgrapht" -> {
          var g = JGraphTGraphs.fromGraphity(template);
          var sets = new org.jgrapht.alg.connectivity.ConnectivityInspector<>(g).connectedSets();
          recordWcc(canonicalByLib, lib, template, mappingFromSetPartition(sets, template.nodeCount()));
        }
        case "guava" -> {
          Graph<Integer> g = GuavaGraphs.fromGraphity(template);
          Map<Integer, Integer> m = new GuavaUnionFindWcc(g).compute();
          recordWcc(canonicalByLib, lib, template, mappingFromMap(m, template.nodeCount()));
        }
        case "webgraph" -> {
          ImmutableGraph symmetric =
              WebGraphGraphs.symmetrized(WebGraphGraphs.fromGraphity(template, wgTmpRoot), wgTmpRoot);
          recordWcc(canonicalByLib, lib, template, ConnectedComponents.compute(symmetric, 1, null).component);
        }
        default -> fail("unsupported library for wcc validation: " + lib);
      }
    }
    ensureAllEqual("wcc", canonicalByLib);
  }

  /**
   * Checks one library's WCC mapping against the graph's edges, then keeps it for the
   * cross-comparison. A case that lists a single library still gets this check.
   */
  private static void recordWcc(
      Map<String, String> canonicalByLib,
      String label,
      com.kobayami.graphity.Graph graph,
      int[] mapping) {
    int n = graph.nodeCount();
    if (mapping.length != n) {
      fail("wcc[" + label + "] returned " + mapping.length + " group IDs for " + n + " nodes");
    }
    for (int source = 0; source < n; source++) {
      SortedIntView outs = graph.outNodes(source);
      for (int i = 0; i < outs.size(); i++) {
        int target = outs.getInt(i);
        if (mapping[source] != mapping[target]) {
          fail("wcc[" + label + "] puts the adjacent nodes " + source + " and " + target
              + " into different groups");
        }
      }
    }
    canonicalByLib.put(label, canonicalPartition(mapping));
  }

  private static void validateCcs(com.kobayami.graphity.Graph template) {
    NodePartition c = Components.ccsOf(template);
    String canonical = canonicalPartition(mappingFromNodePartition(c, template.nodeCount()));
    if (canonical.isBlank()) {
      fail("ccs validation produced empty canonical partition");
    }
  }

  private static void validateCondense(com.kobayami.graphity.Graph template) {
    NodePartition sccs = Components.sccsOf(template);
    com.kobayami.graphity.Graph condensed = sccs.condense();
    if (condensed.nodeCount() != sccs.groupCount()) {
      fail("condense produced " + condensed.nodeCount()
          + " nodes for " + sccs.groupCount() + " groups");
    }
    // The condensation of the SCCs is a DAG by construction; a broken dedup or a stale
    // "seen" reset would show up here before the benchmark reports a number.
    if (!Graphs.isDag(condensed)) {
      fail("condensation of SCCs is not a DAG");
    }
  }

  private static void validateTopOrder(List<String> libraries, com.kobayami.graphity.Graph template, Path wgTmpRoot) throws Exception {
    for (String lib : libraries) {
      int[] order;
      switch (lib) {
        case "graphity" -> order = TopOrder.of(template).toIntArray();
        case "jgrapht" -> {
          var g = JGraphTGraphs.fromGraphity(template);
          TopologicalOrderIterator<Integer, DefaultEdge> it = new TopologicalOrderIterator<>(g);
          int[] out = new int[template.nodeCount()];
          int i = 0;
          while (it.hasNext()) out[i++] = it.next();
          order = out;
        }
        case "guava" -> {
          var g = GuavaGraphs.fromGraphity(template);
          Integer[] out = new GuavaDfsTopOrder(g).compute();
          order = Arrays.stream(out).mapToInt(Integer::intValue).toArray();
        }
        case "webgraph" -> {
          ImmutableGraph g = WebGraphGraphs.fromGraphity(template, wgTmpRoot);
          order = new WebGraphDfsTopOrder(g).compute();
        }
        default -> throw new IllegalStateException("unsupported library for toporder: " + lib);
      }
      if (!isValidTopOrder(template, order)) {
        fail("invalid toporder for lib=" + lib);
      }
    }
  }

  /**
   * Cross-validates Kahn's algorithm over every requested library, plus Graphity's bi-adjacent
   * path against its uni-adjacent one.
   *
   * <p>The topological order is not unique, so identical orders cannot be required — each one is
   * only checked against the edges of the graph. The level assignment
   * {@code level(v) = max(level(predecessors)) + 1} is a function of the graph alone, which makes
   * the level vectors the sharp condition here.
   */
  private static void validateTopOrderAndLevels(List<String> libraries, com.kobayami.graphity.Graph template, Path wgTmpRoot) throws Exception {
    Map<String, int[]> levelsByLib = new LinkedHashMap<>();
    for (String lib : libraries) {
      switch (lib) {
        case "graphity" -> {
          TopOrderAndLevels uni = TopOrderAndLevels.of(template);
          recordLevels(levelsByLib, lib, template, uni.order().toIntArray(), uni.levels().toIntArray());
          // The bi-adjacent branch reads in-degrees from the in-adjacency offsets instead of
          // scanning all edges. It is a second code path and its own report column, so it is
          // checked rather than assumed.
          TopOrderAndLevels bi = TopOrderAndLevels.of(template.biAdjacentGraph());
          recordLevels(levelsByLib, lib + "/bi-adjacent", template, bi.order().toIntArray(), bi.levels().toIntArray());
        }
        case "jgrapht" -> {
          JGraphTTopOrderAndLevels r = JGraphTTopOrderAndLevels.of(JGraphTGraphs.fromGraphity(template));
          recordLevels(levelsByLib, lib, template, r.order(), r.levels());
        }
        case "guava" -> {
          GuavaTopOrderAndLevels r = GuavaTopOrderAndLevels.of(GuavaGraphs.fromGraphity(template));
          recordLevels(levelsByLib, lib, template, r.order(), r.levels());
        }
        case "webgraph" -> {
          WebGraphTopOrderAndLevels r =
              WebGraphTopOrderAndLevels.of(WebGraphGraphs.fromGraphity(template, wgTmpRoot));
          recordLevels(levelsByLib, lib, template, r.order(), r.levels());
        }
        default -> fail("unsupported library for toporderandlevels validation: " + lib);
      }
    }
    ensureAllLevelsEqual(levelsByLib);
  }

  /** Checks one library's Kahn result on its own, then keeps its levels for the cross-comparison. */
  private static void recordLevels(
      Map<String, int[]> levelsByLib,
      String label,
      com.kobayami.graphity.Graph graph,
      int[] order,
      int[] levels) {
    if (!isValidTopOrder(graph, order)) {
      fail("invalid toporderandlevels.order for lib=" + label);
    }
    if (levels.length != graph.nodeCount()) {
      fail("toporderandlevels[" + label + "] returned " + levels.length
          + " levels for " + graph.nodeCount() + " nodes");
    }
    int n = graph.nodeCount();
    for (int source = 0; source < n; source++) {
      SortedIntView outs = graph.outNodes(source);
      for (int i = 0; i < outs.size(); i++) {
        int target = outs.getInt(i);
        if (levels[target] <= levels[source]) {
          fail("toporderandlevels[" + label + "] has level " + levels[target] + " for " + target
              + " but level " + levels[source] + " for its predecessor " + source);
        }
      }
    }
    levelsByLib.put(label, levels);
  }

  private static void ensureAllLevelsEqual(Map<String, int[]> levelsByLib) {
    String referenceLabel = null;
    int[] reference = null;
    for (Map.Entry<String, int[]> e : levelsByLib.entrySet()) {
      if (reference == null) {
        referenceLabel = e.getKey();
        reference = e.getValue();
        continue;
      }
      int at = Arrays.mismatch(reference, e.getValue());
      if (at >= 0) {
        fail("toporderandlevels level mismatch between " + referenceLabel + " and " + e.getKey()
            + " at node " + at);
      }
    }
  }

  private static void validateBackEdges(List<String> libraries, com.kobayami.graphity.Graph template, Path wgTmpRoot) throws Exception {
    for (String lib : libraries) {
      LongSet set = switch (lib) {
        case "graphity" -> toLongSet(BackEdges.of(template));
        case "jgrapht" -> JGraphTBackEdges.of(JGraphTGraphs.fromGraphity(template)).asLongSet();
        case "guava" -> GuavaBackEdges.of(GuavaGraphs.fromGraphity(template)).asLongSet();
        case "webgraph" -> WebGraphBackEdges.of(WebGraphGraphs.fromGraphity(template, wgTmpRoot)).asLongSet();
        default -> throw new IllegalStateException("unsupported library for backedges: " + lib);
      };
      validateBackEdgeSet(template, set, lib);
    }
  }

  private static void validateEdgeTypes(List<String> libraries, com.kobayami.graphity.Graph template, Path wgTmpRoot) throws Exception {
    for (String lib : libraries) {
      Long2ByteOpenHashMap typeMap = switch (lib) {
        case "graphity" -> graphityTypeMap(template);
        case "jgrapht" -> JGraphTEdgeTypes.of(JGraphTGraphs.fromGraphity(template)).asTypeMap();
        case "guava" -> GuavaEdgeTypes.of(GuavaGraphs.fromGraphity(template)).asTypeMap();
        case "webgraph" -> WebGraphEdgeTypes.of(WebGraphGraphs.fromGraphity(template, wgTmpRoot)).asTypeMap();
        default -> throw new IllegalStateException("unsupported library for edgetypes: " + lib);
      };
      validateTypeMap(template, typeMap, lib);
      LongSet backFromTypes = new LongOpenHashSet();
      for (Long2ByteMap.Entry e : typeMap.long2ByteEntrySet()) {
        if (e.getByteValue() == (byte) EdgeType.BACK.ordinal()) {
          backFromTypes.add(e.getLongKey());
        }
      }
      validateBackEdgeSet(template, backFromTypes, lib + "/edgetypes");
      LongSet backDirect = switch (lib) {
        case "graphity" -> toLongSet(BackEdges.of(template));
        case "jgrapht" -> JGraphTBackEdges.of(JGraphTGraphs.fromGraphity(template)).asLongSet();
        case "guava" -> GuavaBackEdges.of(GuavaGraphs.fromGraphity(template)).asLongSet();
        case "webgraph" -> WebGraphBackEdges.of(WebGraphGraphs.fromGraphity(template, wgTmpRoot)).asLongSet();
        default -> throw new IllegalStateException("unsupported library for edge/back consistency: " + lib);
      };
      if (!backFromTypes.equals(backDirect)) {
        fail("edgetypes/backedges mismatch for lib=" + lib
            + " backFromTypes=" + backFromTypes.size()
            + " backDirect=" + backDirect.size());
      }
    }
  }

  private static Long2ByteOpenHashMap graphityTypeMap(com.kobayami.graphity.Graph graph) {
    EdgeTypes et = EdgeTypes.of(graph);
    Long2ByteOpenHashMap out = new Long2ByteOpenHashMap(graph.edgeCount());
    int n = graph.nodeCount();
    for (int node = 0; node < n; node++) {
      int start = graph.outEdgeStart(node);
      int end = graph.outEdgeEnd(node);
      for (int edgeIndex = start; edgeIndex < end; edgeIndex++) {
        int target = graph.edgeTarget(edgeIndex);
        long edge = BenchEdgePairs.encode(node, target);
        out.put(edge, (byte) et.typeOf(edgeIndex).ordinal());
      }
    }
    return out;
  }

  private static void validateTypeMap(com.kobayami.graphity.Graph graph, Long2ByteOpenHashMap typeMap, String lib) {
    if (typeMap.size() != graph.edgeCount()) {
      fail("edgetypes[" + lib + "] does not classify every edge: got " + typeMap.size() + " expected " + graph.edgeCount());
    }
    int n = graph.nodeCount();
    for (int node = 0; node < n; node++) {
      SortedIntView outs = graph.outNodes(node);
      for (int i = 0; i < outs.size(); i++) {
        int target = outs.getInt(i);
        long edge = BenchEdgePairs.encode(node, target);
        if (!typeMap.containsKey(edge)) {
          fail("edgetypes[" + lib + "] missing edge " + node + "->" + target);
        }
        byte t = typeMap.get(edge);
        if (t < 0 || t >= EdgeType.values().length) {
          fail("edgetypes[" + lib + "] contains invalid type ordinal " + t);
        }
      }
    }
  }

  private static void validateBackEdgeSet(com.kobayami.graphity.Graph graph, LongSet backEdges, String lib) {
    for (long edge : backEdges) {
      int source = (int) (edge >>> 32);
      int target = (int) edge;
      if (source < 0 || source >= graph.nodeCount() || target < 0 || target >= graph.nodeCount()) {
        fail("backedges[" + lib + "] contains out-of-range edge " + source + "->" + target);
      }
      if (!graph.hasOutNode(source, target)) {
        fail("backedges[" + lib + "] contains non-existing edge " + source + "->" + target);
      }
    }

    GraphBuilder b = new GraphBuilder();
    b.addNodes(graph.nodeCount());
    for (int source = 0; source < graph.nodeCount(); source++) {
      SortedIntView outs = graph.outNodes(source);
      for (int i = 0; i < outs.size(); i++) {
        int target = outs.getInt(i);
        long enc = BenchEdgePairs.encode(source, target);
        if (!backEdges.contains(enc)) {
          b.addEdge(source, target);
        }
      }
    }
    if (!Graphs.isDag(b.build())) {
      fail("backedges[" + lib + "] does not break all cycles");
    }
  }

  private static boolean isValidTopOrder(com.kobayami.graphity.Graph graph, int[] order) {
    int n = graph.nodeCount();
    if (order.length != n) return false;
    int[] pos = new int[n];
    Arrays.fill(pos, -1);
    for (int i = 0; i < n; i++) {
      int node = order[i];
      if (node < 0 || node >= n || pos[node] != -1) return false;
      pos[node] = i;
    }
    for (int u = 0; u < n; u++) {
      SortedIntView outs = graph.outNodes(u);
      for (int i = 0; i < outs.size(); i++) {
        int v = outs.getInt(i);
        if (pos[u] >= pos[v]) return false;
      }
    }
    return true;
  }

  private static String canonicalPartition(int[] nodeToGroup) {
    Map<Integer, IntArrayList> groups = new HashMap<>();
    for (int node = 0; node < nodeToGroup.length; node++) {
      groups.computeIfAbsent(nodeToGroup[node], k -> new IntArrayList()).add(node);
    }
    List<String> pieces = new ArrayList<>();
    for (IntArrayList members : groups.values()) {
      int[] sorted = members.toIntArray();
      Arrays.sort(sorted);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < sorted.length; i++) {
        if (i > 0) sb.append(',');
        sb.append(sorted[i]);
      }
      pieces.add(sb.toString());
    }
    pieces.sort(Comparator.naturalOrder());
    return String.join("|", pieces);
  }

  private static int[] mappingFromNodePartition(NodePartition p, int n) {
    int[] out = new int[n];
    for (int i = 0; i < n; i++) out[i] = p.get(i);
    return out;
  }

  private static int[] mappingFromSetPartition(List<? extends Set<Integer>> components, int n) {
    int[] out = new int[n];
    Arrays.fill(out, -1);
    for (int group = 0; group < components.size(); group++) {
      for (int node : components.get(group)) {
        out[node] = group;
      }
    }
    for (int v : out) if (v < 0) fail("set partition misses at least one node");
    return out;
  }

  private static int[] mappingFromMap(Map<Integer, Integer> m, int n) {
    int[] out = new int[n];
    Arrays.fill(out, -1);
    for (Map.Entry<Integer, Integer> e : m.entrySet()) {
      out[e.getKey()] = e.getValue();
    }
    for (int v : out) if (v < 0) fail("map partition misses at least one node");
    return out;
  }

  private static int[] webGraphTarjanMapping(ImmutableGraph graph) {
    int n = graph.numNodes();
    int[] index = new int[n];
    int[] lowlink = new int[n];
    int[] component = new int[n];
    Arrays.fill(index, -1);
    Arrays.fill(component, -1);

    IntArrayList stack = new IntArrayList();
    boolean[] onStack = new boolean[n];
    int[] nextIndex = {0};
    int[] nextComponent = {0};

    for (int node = 0; node < n; node++) {
      if (index[node] < 0) {
        webGraphStrongConnect(node, graph, index, lowlink, component, stack, onStack, nextIndex, nextComponent);
      }
    }
    return component;
  }

  private static void webGraphStrongConnect(
      int node,
      ImmutableGraph graph,
      int[] index,
      int[] lowlink,
      int[] component,
      IntArrayList stack,
      boolean[] onStack,
      int[] nextIndex,
      int[] nextComponent) {
    index[node] = nextIndex[0];
    lowlink[node] = nextIndex[0];
    nextIndex[0]++;
    stack.push(node);
    onStack[node] = true;

    LazyIntIterator it = graph.successors(node);
    int target;
    while ((target = it.nextInt()) != -1) {
      if (index[target] < 0) {
        webGraphStrongConnect(target, graph, index, lowlink, component, stack, onStack, nextIndex, nextComponent);
        lowlink[node] = Math.min(lowlink[node], lowlink[target]);
      } else if (onStack[target]) {
        lowlink[node] = Math.min(lowlink[node], index[target]);
      }
    }

    if (lowlink[node] == index[node]) {
      int w;
      do {
        w = stack.popInt();
        onStack[w] = false;
        component[w] = nextComponent[0];
      } while (w != node);
      nextComponent[0]++;
    }
  }

  private static LongSet toLongSet(Edges edges) {
    LongOpenHashSet out = new LongOpenHashSet(edges.count());
    IntList src = edges.sourceNodes();
    IntList tgt = edges.targetNodes();
    for (int i = 0; i < edges.count(); i++) {
      out.add(BenchEdgePairs.encode(src.getInt(i), tgt.getInt(i)));
    }
    return out;
  }

  private static void ensureAllEqual(String label, Map<String, String> byLib) {
    if (byLib.size() <= 1) return;
    String expected = null;
    for (Map.Entry<String, String> e : byLib.entrySet()) {
      if (expected == null) expected = e.getValue();
      else if (!Objects.equals(expected, e.getValue())) {
        fail(label + " mismatch between libraries: " + byLib);
      }
    }
  }

  private static List<String> parseStringCsv(String csv) {
    List<String> out = new ArrayList<>();
    for (String part : csv.split(",")) {
      String v = part.trim();
      if (!v.isEmpty()) out.add(v);
    }
    if (out.isEmpty()) fail("empty CSV list");
    return out;
  }

  private static int[] parseIntCsv(String csv) {
    List<String> items = parseStringCsv(csv);
    int[] out = new int[items.size()];
    for (int i = 0; i < items.size(); i++) {
      out[i] = Integer.parseInt(items.get(i));
      if (out[i] <= 0) fail("node count must be > 0");
    }
    return out;
  }

  private static List<GraphShape> parseShapes(String csv) {
    List<String> items = parseStringCsv(csv);
    List<GraphShape> out = new ArrayList<>();
    for (String item : items) out.add(GraphShape.valueOf(item));
    return out;
  }

  private static List<ControlFlowShape> parseControlFlowShapes(String csv) {
    List<String> items = parseStringCsv(csv);
    List<ControlFlowShape> out = new ArrayList<>();
    for (String item : items) out.add(ControlFlowShape.valueOf(item));
    return out;
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static void fail(String msg) {
    throw new IllegalArgumentException(msg);
  }

  private static void printUsage() {
    System.err.println("""
        Benchmark validation runner

        Usage:
          java -jar graphity-benchmarks.jar validate \\
              --algorithm <algo> \\
              --libraries <csv> \\
              (--nodes <csv> --shapes <csv> | --datasets <csv>)
        """);
  }
}
