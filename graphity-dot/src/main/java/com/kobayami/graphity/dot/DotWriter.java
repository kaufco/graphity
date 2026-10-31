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
 * A writer of DOT (the language of Graphviz) that labels and styles a graph with the caller's data
 * by node ID and edge index.
 * <p>
 * {@link #of(Graph)} returns a writer for a graph, the methods that configure it return the writer
 * again, and {@link #write(Appendable)} or {@link #writeToString()} write the text. The graph holds
 * no data, so labels and attributes come from hooks that receive a node ID or an edge index and
 * read the caller's arrays. Here, the packages of a small program get their names from
 * {@code names}. The strongly connected components {@code sccs} tell which packages are in a
 * cycle, and these are drawn in red:
 * {@snippet file="com/kobayami/graphity/examples/WritingDot.java" region="nodeAttributes"}
 * Nodes are named by their node IDs. The text lists the graph attributes, the clusters in group
 * order with their nodes, then the nodes in no cluster, then the edges in edge index order, so the
 * same graph and hooks always give the same text. A node outside the clusters is listed only if it
 * has a label or attributes, or no edge in the text; Graphviz creates the other nodes from their
 * edges.
 * <p>
 * Labels and attribute values are written as quoted strings: the writer escapes quotes and
 * backslashes and turns line breaks into DOT's {@code \n}. Graphviz decodes HTML entities such as
 * {@code &lt;} in the attributes {@code label}, {@code xlabel}, {@code headlabel} and
 * {@code taillabel}. In these, the writer also turns {@code &} into {@code &amp;}, so that a label
 * shows exactly its text. Other values, such as a {@code URL}, keep their {@code &}. NUL, which
 * DOT cannot hold, is written as U+FFFD.
 * <p>
 * Attribute names must be identifiers - an ASCII letter or underscore, then ASCII letters, digits
 * or underscores - other than the DOT keywords {@code node}, {@code edge}, {@code graph},
 * {@code digraph}, {@code subgraph} and {@code strict}, in any letter case.
 * <p>
 * A writer keeps its configuration and can write any number of times. For Markdown and
 * documentation, {@code MermaidWriter} in the module {@code graphity-mermaid} writes a Mermaid
 * flowchart.
 * <p>
 * Instances are mutable and not safe to share between threads.
 */
public final class DotWriter {

  private static final Set<String> KEYWORDS =
      Set.of("node", "edge", "graph", "digraph", "subgraph", "strict");

  // The attributes whose values Graphviz reads as labels, decoding HTML entities.
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
   * Returns a writer for {@code graph}, with no labels, attributes, clusters or filter yet.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph to write
   * @return the writer
   */
  public static DotWriter of(Graph graph) {
    return new DotWriter(graph);
  }

  /**
   * Sets an attribute of the graph, such as {@code rankdir} to {@code LR}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param name  attribute name, an identifier other than a DOT keyword
   * @param value attribute value
   * @return this writer
   * @throws IllegalArgumentException if {@code name} is not an identifier or is a DOT keyword
   */
  public DotWriter graphAttribute(String name, String value) {
    graphAttributes.put(checkName(name), value);
    return this;
  }

  /**
   * Sets the label of each node, by node ID; {@code null} leaves a node unlabeled, and Graphviz
   * then shows its node ID.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param label called with a node ID, returns the label of the node or {@code null}
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
   * @param label called with an edge index, returns the label of the edge or {@code null}
   * @return this writer
   */
  public DotWriter edgeLabel(IntFunction<@Nullable String> label) {
    this.edgeLabel = label;
    return this;
  }

  /**
   * Sets the hook that adds DOT attributes, such as {@code shape}, {@code color} or {@code style},
   * to each node, by node ID.
   * <p>
   * The hook receives the label from {@link #nodeLabel(IntFunction)} as the attribute
   * {@code label}, which it can replace or remove.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param hook called with each node that is written, puts its attributes
   * @return this writer
   */
  public DotWriter nodeAttributes(AttributeHook hook) {
    this.nodeAttributes = hook;
    return this;
  }

  /**
   * Sets the hook that adds DOT attributes, such as {@code color} or {@code style}, to each edge,
   * by edge index.
   * <p>
   * The hook receives the label from {@link #edgeLabel(IntFunction)} as the attribute
   * {@code label}, which it can replace or remove.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param hook called with each edge that is written, puts its attributes
   * @return this writer
   */
  public DotWriter edgeAttributes(AttributeHook hook) {
    this.edgeAttributes = hook;
    return this;
  }

  /**
   * Draws each group of {@code partition} as a cluster, with a label by group ID.
   * <p>
   * Nodes in no group stay outside every cluster, and a group without a node to write has no
   * cluster. A label of {@code null} leaves a cluster unlabeled, also when the graph has a
   * {@code label}, which Graphviz would otherwise repeat on every cluster.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param partition the groups, over the nodes of the graph, such as an
   *                  {@link com.kobayami.graphity.Sccs}
   * @param label     called with a group ID, returns the label of its cluster or {@code null}
   * @return this writer
   * @throws IllegalArgumentException if {@code partition} has a different node count than the
   *                                  graph
   */
  public DotWriter clusters(NodePartition partition, IntFunction<@Nullable String> label) {
    selection.setClusters(partition, label);
    return this;
  }

  /**
   * Restricts the text to the nodes that {@code include} accepts and the edges between them.
   * <p>
   * Node IDs stay those of the graph, so the hooks keep reading the same arrays.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param include called with a node ID, returns whether to write the node
   * @return this writer
   */
  public DotWriter nodeFilter(IntPredicate include) {
    selection.nodeFilter = include;
    return this;
  }

  /**
   * Switches to the undirected reading of the graph: a {@code graph} with {@code --} edges instead
   * of a {@code digraph}.
   * <p>
   * The edges {@code a -> b} and {@code b -> a} become one, written as the edge from the lower
   * node ID; the edge hooks receive its edge index.
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
   * Writes the graph to {@code out}, as DOT text.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + groupCount), plus the hooks, with groupCount
   * the number of groups of the partition drawn as clusters; in the undirected reading,
   * O(log outDegree(target)) more per edge.<br>
   * <strong>Memory:</strong> nodeCount bytes while it runs.
   *
   * @param out where the text goes; not closed
   * @throws IOException if writing to {@code out} fails
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
   * <strong>Time:</strong> as for {@link #write(Appendable)}.<br>
   * <strong>Memory:</strong> the text for the result; while it runs, up to twice as much again
   * and nodeCount bytes more.
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

  // Writes text as a DOT quoted string that shows exactly that text, NUL aside, which DOT cannot
  // hold. For an attribute in which Graphviz decodes HTML entities, the labels, decodesEntities
  // turns & into &amp;.
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
