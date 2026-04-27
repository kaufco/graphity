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
import com.kobayami.graphity.benchmarks.generators.BenchmarkGraphs;
import com.kobayami.graphity.benchmarks.generators.GraphShape;
import com.kobayami.graphity.benchmarks.guava.GuavaBackEdges;
import com.kobayami.graphity.benchmarks.guava.GuavaDfsTopOrder;
import com.kobayami.graphity.benchmarks.guava.GuavaEdgeTypes;
import com.kobayami.graphity.benchmarks.guava.GuavaGraphs;
import com.kobayami.graphity.benchmarks.guava.GuavaTarjanScc;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTBackEdges;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTEdgeTypes;
import com.kobayami.graphity.benchmarks.jgrapht.JGraphTGraphs;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphBackEdges;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphDfsTopOrder;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphEdgeTypes;
import com.kobayami.graphity.benchmarks.webgraph.WebGraphGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.nio.file.Path;
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
      } else {
        List<String> datasetPaths = parseStringCsv(datasetsCsv);
        for (String datasetPath : datasetPaths) {
          com.kobayami.graphity.Graph template = BenchmarkGraphs.loadDataset(datasetPath);
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
      case "scc" -> validateScc(libraries, template, wgTmpRoot);
      case "wcc" -> validateWcc(template);
      case "ccs" -> validateCcs(template);
      case "toporder" -> validateTopOrder(libraries, template, wgTmpRoot);
      case "toporderandlevels" -> validateTopOrderAndLevels(template);
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

  private static void validateWcc(com.kobayami.graphity.Graph template) {
    NodePartition w = Components.wccsOf(template);
    String canonical = canonicalPartition(mappingFromNodePartition(w, template.nodeCount()));
    if (canonical.isBlank()) {
      fail("wcc validation produced empty canonical partition");
    }
  }

  private static void validateCcs(com.kobayami.graphity.Graph template) {
    NodePartition c = Components.ccsOf(template);
    String canonical = canonicalPartition(mappingFromNodePartition(c, template.nodeCount()));
    if (canonical.isBlank()) {
      fail("ccs validation produced empty canonical partition");
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

  private static void validateTopOrderAndLevels(com.kobayami.graphity.Graph template) {
    TopOrderAndLevels tol = TopOrderAndLevels.of(template);
    int[] order = tol.order().toIntArray();
    if (!isValidTopOrder(template, order)) {
      fail("invalid toporderandlevels.order");
    }
    if (tol.levelCount() < 0) {
      fail("invalid toporderandlevels.levelCount");
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
