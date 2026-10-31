package com.kobayami.graphity.dot;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.NodePartition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import org.jspecify.annotations.Nullable;

/**
 * Writes a graph in Graphviz's DOT language, with labels, attributes and clusters taken from the
 * caller's data by node ID and edge index.
 *
 * <pre>{@code
 * DotWriter.of(graph)
 *     .nodeLabel(node -> names[node])
 *     .edgeLabel(edge -> String.valueOf(weights[edge]))
 *     .nodeAttributes((node, attributes) -> {
 *         if (node == entry) attributes.put("shape", "doublecircle");
 *     })
 *     .edgeAttributes((edge, attributes) -> {
 *         if (isBackEdge[edge]) attributes.put("color", "red");
 *     })
 *     .clusters(sccs, scc -> "SCC " + scc)
 *     .write(out);
 * }</pre>
 *
 * Nodes are named by their IDs. The output lists the clusters in group order with their nodes,
 * then the nodes in no cluster, then the edges in edge index order, so the same graph and hooks
 * always give the same text. A node appears only if it carries a label or attributes, or belongs
 * to a cluster, or has no written edge - DOT creates every other node from its edges.
 * <p>
 * Labels and attribute values are written as quoted strings: the writer escapes quotes and
 * backslashes, turns line breaks into DOT's {@code \n}, and writes NUL, which DOT cannot hold, as
 * U+FFFD. Graphviz decodes HTML entities such as {@code &lt;} in labels, so in the attributes
 * {@code label}, {@code xlabel}, {@code headlabel} and {@code taillabel} the writer also turns
 * {@code &} into {@code &amp;}, and a label shows exactly its text; other values, such as a
 * {@code URL}, keep their {@code &}. Attribute names must be identifiers -
 * a letter or underscore, then letters, digits or underscores - other than the DOT keywords
 * {@code node}, {@code edge}, {@code graph}, {@code digraph}, {@code subgraph} and
 * {@code strict}. A hook that returns {@code null} adds nothing.
 * <p>
 * A writer holds its configuration and may write any number of times. It is not safe for
 * concurrent configuration; the graph itself is only read. For Markdown, issues and
 * documentation, {@code MermaidWriter} in the module {@code graphity-mermaid} writes a Mermaid
 * flowchart.
 *
 * @see AttributeHook
 */
public final class DotWriter {

  private static final Set<String> KEYWORDS =
      Set.of("node", "edge", "graph", "digraph", "subgraph", "strict");

  /** The attributes whose values Graphviz reads as labels, decoding HTML entities. */
  private static final Set<String> LABEL_ATTRIBUTES =
      Set.of("label", "xlabel", "headlabel", "taillabel");

  private final Selection selection;
  private final Map<String, String> graphAttributes = new LinkedHashMap<>();
  private @Nullable IntFunction<@Nullable String> nodeLabel;
  private @Nullable IntFunction<@Nullable String> edgeLabel;
  private @Nullable AttributeHook nodeAttributes;
  private @Nullable AttributeHook edgeAttributes;

  private DotWriter(Graph graph) {
    this.selection = new Selection(graph);
  }

  /**
   * Returns a writer for the graph, with no labels, attributes, clusters or filter yet.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph to write
   * @return a new writer
   */
  public static DotWriter of(Graph graph) {
    return new DotWriter(graph);
  }

  /**
   * Sets a graph attribute, such as {@code rankdir} to {@code LR}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param name  the attribute name, an identifier other than a DOT keyword
   * @param value the value, escaped by the writer
   * @return this writer
   * @throws IllegalArgumentException if {@code name} is not an identifier or is a DOT keyword
   */
  public DotWriter graphAttribute(String name, String value) {
    graphAttributes.put(checkName(name), value);
    return this;
  }

  /**
   * Sets the label of each node, by node ID; {@code null} leaves a node unlabeled, and Graphviz
   * then shows its ID.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param label the label of a node
   * @return this writer
   */
  public DotWriter nodeLabel(IntFunction<@Nullable String> label) {
    this.nodeLabel = label;
    return this;
  }

  /**
   * Sets the label of each edge, by edge index; {@code null} leaves an edge unlabeled.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param label the label of an edge
   * @return this writer
   */
  public DotWriter edgeLabel(IntFunction<@Nullable String> label) {
    this.edgeLabel = label;
    return this;
  }

  /**
   * Sets a hook that adds DOT attributes to each node, by node ID - {@code shape},
   * {@code color}, {@code style} or anything else Graphviz reads. A {@code label} put here
   * replaces the one from {@link #nodeLabel(IntFunction)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param hook adds the attributes of one node
   * @return this writer
   */
  public DotWriter nodeAttributes(AttributeHook hook) {
    this.nodeAttributes = hook;
    return this;
  }

  /**
   * Sets a hook that adds DOT attributes to each edge, by edge index. A {@code label} put here
   * replaces the one from {@link #edgeLabel(IntFunction)}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param hook adds the attributes of one edge
   * @return this writer
   */
  public DotWriter edgeAttributes(AttributeHook hook) {
    this.edgeAttributes = hook;
    return this;
  }

  /**
   * Draws each group of the partition as a cluster, labeled by group ID; {@code null} leaves a
   * cluster unlabeled, also under a graph {@code label}, which Graphviz would otherwise repeat on
   * every cluster. Nodes in no group stay outside every cluster.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param partition the groups, over the nodes of this writer's graph - {@code Sccs} among them
   * @param label     the label of a cluster
   * @return this writer
   * @throws IllegalArgumentException if the partition covers a different number of nodes
   */
  public DotWriter clusters(NodePartition partition, IntFunction<@Nullable String> label) {
    selection.clusters(partition, label);
    return this;
  }

  /**
   * Writes only the nodes the filter accepts, and only the edges between them. Node IDs stay
   * those of the graph, so the hooks keep reading the same data.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param include whether to write a node
   * @return this writer
   */
  public DotWriter nodeFilter(IntPredicate include) {
    selection.nodeFilter = include;
    return this;
  }

  /**
   * Writes the undirected reading: a {@code graph} with {@code --} edges, each reciprocal pair
   * once, as its edge from the lower node ID, whose index the edge hooks receive.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return this writer
   */
  public DotWriter undirected() {
    selection.undirected = true;
    return this;
  }

  /**
   * Writes the graph to {@code out}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount) plus the hooks; in the undirected reading,
   * O(log outDegree) more per edge to find reciprocal pairs.
   *
   * @param out where to write; not closed
   * @throws IOException if {@code out} throws
   * @throws IllegalArgumentException if an attribute hook puts a name that is not an identifier
   *                                  or is a DOT keyword; what was written before stays in
   *                                  {@code out}
   */
  public void write(Appendable out) throws IOException {
    Graph graph = selection.graph;
    int n = graph.nodeCount();
    String arrow = selection.undirected ? " -- " : " -> ";
    out.append(selection.undirected ? "graph {\n" : "digraph {\n");
    for (var attribute : graphAttributes.entrySet()) {
      out.append("  ").append(attribute.getKey()).append('=');
      quote(attribute.getValue(), LABEL_ATTRIBUTES.contains(attribute.getKey()), out);
      out.append(";\n");
    }
    boolean graphLabeled = graphAttributes.containsKey("label");

    var hasEdge = new boolean[n];
    selection.forEachWrittenEdge((source, target, edgeIndex) -> {
      hasEdge[source] = true;
      hasEdge[target] = true;
    });

    var attributes = new LinkedHashMap<String, String>();
    Selection.Clusters clusters = selection.clusters;
    if (clusters != null) {
      NodePartition partition = clusters.partition();
      for (int group = 0; group < partition.groupCount(); group++) {
        var nodes = partition.nodesOf(group);
        boolean opened = false;
        for (int i = 0; i < nodes.size(); i++) {
          int node = nodes.getInt(i);
          if (!selection.isIncluded(node)) continue;
          if (!opened) {
            out.append("  subgraph cluster_").append(Integer.toString(group)).append(" {\n");
            String label = clusters.label().apply(group);
            if (label != null) {
              out.append("    label=");
              quote(label, true, out);
              out.append(";\n");
            } else if (graphLabeled) {
              out.append("    label=\"\";\n");
            }
            opened = true;
          }
          putNodeAttributes(node, attributes);
          writeNode(node, attributes, "    ", out);
        }
        if (opened) out.append("  }\n");
      }
    }
    for (int node = 0; node < n; node++) {
      if (!selection.isIncluded(node) || selection.clusterOf(node) >= 0) continue;
      putNodeAttributes(node, attributes);
      if (!attributes.isEmpty() || !hasEdge[node]) writeNode(node, attributes, "  ", out);
    }
    selection.forEachWrittenEdge((source, target, edgeIndex) -> {
      out.append("  ").append(Integer.toString(source)).append(arrow)
          .append(Integer.toString(target));
      putEdgeAttributes(edgeIndex, attributes);
      writeAttributes(attributes, out);
      out.append(";\n");
    });
    out.append("}\n");
  }

  /**
   * Returns the DOT text of the graph, as {@link #write(Appendable)} writes it.
   * <p>
   * <strong>Time:</strong> as for {@link #write(Appendable)}.
   *
   * @return the DOT text
   * @throws IllegalArgumentException as for {@link #write(Appendable)}
   */
  public String writeToString() {
    var sb = new StringBuilder();
    try {
      write(sb);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return sb.toString();
  }

  private static void writeNode(int node, Map<String, String> attributes, String indent,
      Appendable out) throws IOException {
    out.append(indent).append(Integer.toString(node));
    writeAttributes(attributes, out);
    out.append(";\n");
  }

  private void putNodeAttributes(int node, Map<String, String> attributes) {
    attributes.clear();
    String label = nodeLabel == null ? null : nodeLabel.apply(node);
    if (label != null) attributes.put("label", label);
    if (nodeAttributes != null) nodeAttributes.accept(node, attributes);
  }

  private void putEdgeAttributes(int edgeIndex, Map<String, String> attributes) {
    attributes.clear();
    String label = edgeLabel == null ? null : edgeLabel.apply(edgeIndex);
    if (label != null) attributes.put("label", label);
    if (edgeAttributes != null) edgeAttributes.accept(edgeIndex, attributes);
  }

  private static void writeAttributes(Map<String, String> attributes, Appendable out)
      throws IOException {
    boolean first = true;
    for (var attribute : attributes.entrySet()) {
      String name = checkName(attribute.getKey());
      out.append(first ? " [" : ", ").append(name).append('=');
      quote(attribute.getValue(), LABEL_ATTRIBUTES.contains(name), out);
      first = false;
    }
    if (!first) out.append(']');
  }

  private static String checkName(String name) {
    if (!Selection.isIdentifier(name)
        || KEYWORDS.contains(name.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("not a DOT attribute name: " + name);
    }
    return name;
  }

  /**
   * Writes {@code text} as a DOT quoted string that shows exactly that text, NUL aside, which DOT
   * cannot hold. For an attribute in which Graphviz decodes HTML entities, the labels,
   * {@code decodesEntities} turns {@code &} into {@code &amp;}.
   */
  private static void quote(String text, boolean decodesEntities, Appendable out)
      throws IOException {
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> {
          if (i + 1 >= text.length() || text.charAt(i + 1) != '\n') out.append("\\n");
        }
        case '&' -> out.append(decodesEntities ? "&amp;" : "&");
        case '\0' -> out.append('\uFFFD');
        default -> out.append(c);
      }
    }
    out.append('"');
  }
}
