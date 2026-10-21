package com.kobayami.graphity.io;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.NodePartition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import org.jspecify.annotations.Nullable;

/**
 * Writes a graph as a Mermaid flowchart, with labels, shapes, classes and link styles taken from
 * the caller's data by node ID and edge index.
 *
 * <pre>{@code
 * MermaidWriter.of(graph)
 *     .direction(MermaidWriter.Direction.LEFT_RIGHT)
 *     .nodeLabel(node -> names[node])
 *     .nodeShape(node -> node == entry ? MermaidWriter.Shape.CIRCLE : MermaidWriter.Shape.RECTANGLE)
 *     .classDef("hot", "fill:#f96")
 *     .nodeClass(node -> isHot[node] ? "hot" : null)
 *     .linkStyle(edge -> isBackEdge[edge] ? "stroke:red" : null)
 *     .clusters(sccs, scc -> "SCC " + scc)
 *     .write(out);
 * }</pre>
 *
 * Node {@code v} is named {@code n}<i>v</i>. The output lists the class definitions, the
 * clusters in group order with their nodes, then the nodes in no cluster, then the links in edge
 * index order, the classes and the link styles, so the same graph and hooks always give the same
 * text. Mermaid numbers links in the order they are written; the writer translates edge indices
 * to those numbers for {@code linkStyle}, also when a filter leaves edges out.
 * <p>
 * Labels are written as quoted strings: the writer turns quotes, {@code #}, {@code &},
 * {@code <}, {@code >} and backticks into Mermaid entity codes and line breaks into {@code <br>},
 * so no label reads as HTML or as a Markdown string. Class names must be identifiers - a letter
 * or underscore, then letters, digits or underscores - other than {@code end}; style definitions
 * are passed through as Mermaid reads them and must be non-empty, without line breaks or
 * semicolons. A hook that returns {@code null} adds nothing.
 * <p>
 * Mermaid renders graphs of a few hundred nodes well; for larger ones, {@link DotWriter}. A
 * writer holds its configuration and may write any number of times. It is not safe for
 * concurrent configuration; the graph itself is only read.
 *
 * @see DotWriter
 */
public final class MermaidWriter {

  /** The direction of a flowchart. */
  public enum Direction {
    /** Top to bottom, {@code TD}. */
    TOP_DOWN("TD"),
    /** Bottom to top, {@code BT}. */
    BOTTOM_UP("BT"),
    /** Left to right, {@code LR}. */
    LEFT_RIGHT("LR"),
    /** Right to left, {@code RL}. */
    RIGHT_LEFT("RL");

    private final String code;

    Direction(String code) {
      this.code = code;
    }
  }

  /** The shape of a node, with the brackets Mermaid draws it with. */
  public enum Shape {
    /** A rectangle, {@code [...]}. */
    RECTANGLE("[", "]"),
    /** A rectangle with rounded corners, {@code (...)}. */
    ROUNDED("(", ")"),
    /** A stadium, {@code ([...])}. */
    STADIUM("([", "])"),
    /** A subroutine box, {@code [[...]]}. */
    SUBROUTINE("[[", "]]"),
    /** A cylinder, {@code [(...)]}. */
    CYLINDER("[(", ")]"),
    /** A circle, {@code ((...))}. */
    CIRCLE("((", "))"),
    /** A double circle, {@code (((...)))}. */
    DOUBLE_CIRCLE("(((", ")))"),
    /** A rhombus, {@code {...}}. */
    RHOMBUS("{", "}"),
    /** A hexagon, {@code {{...}}}. */
    HEXAGON("{{", "}}");

    private final String open;
    private final String close;

    Shape(String open, String close) {
      this.open = open;
      this.close = close;
    }
  }

  private final Selection selection;
  private Direction direction = Direction.TOP_DOWN;
  private final Map<String, String> classDefs = new LinkedHashMap<>();
  private @Nullable IntFunction<@Nullable String> nodeLabel;
  private @Nullable IntFunction<@Nullable String> edgeLabel;
  private @Nullable IntFunction<@Nullable Shape> nodeShape;
  private @Nullable IntFunction<@Nullable String> nodeClass;
  private @Nullable IntFunction<@Nullable String> linkStyle;

  private MermaidWriter(Graph graph) {
    this.selection = new Selection(graph);
  }

  /**
   * Returns a writer for the graph, top down, with no labels, shapes, classes, clusters or filter
   * yet.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph to write
   * @return a new writer
   */
  public static MermaidWriter of(Graph graph) {
    return new MermaidWriter(graph);
  }

  /**
   * Sets the direction of the flowchart.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param direction the direction
   * @return this writer
   */
  public MermaidWriter direction(Direction direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Sets the label of each node, by node ID; {@code null} shows the node's name
   * {@code n}<i>v</i>.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param label the label of a node
   * @return this writer
   */
  public MermaidWriter nodeLabel(IntFunction<@Nullable String> label) {
    this.nodeLabel = label;
    return this;
  }

  /**
   * Sets the label of each link, by edge index; {@code null} leaves a link unlabeled.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param label the label of an edge
   * @return this writer
   */
  public MermaidWriter edgeLabel(IntFunction<@Nullable String> label) {
    this.edgeLabel = label;
    return this;
  }

  /**
   * Sets the shape of each node, by node ID; {@code null} is a rectangle.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param shape the shape of a node
   * @return this writer
   */
  public MermaidWriter nodeShape(IntFunction<@Nullable Shape> shape) {
    this.nodeShape = shape;
    return this;
  }

  /**
   * Defines a class that {@link #nodeClass(IntFunction)} can assign, written as
   * {@code classDef name style}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param name  the class name, an identifier other than {@code end}
   * @param style the style definition, such as {@code fill:#f96,stroke:#333}
   * @return this writer
   * @throws IllegalArgumentException if {@code name} is not such an identifier, or {@code style}
   *                                  is empty or contains a line break or semicolon
   */
  public MermaidWriter classDef(String name, String style) {
    classDefs.put(checkClass(name), checkStyle(style));
    return this;
  }

  /**
   * Sets the class of each node, by node ID; {@code null} assigns none.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param className the class name of a node, an identifier
   * @return this writer
   */
  public MermaidWriter nodeClass(IntFunction<@Nullable String> className) {
    this.nodeClass = className;
    return this;
  }

  /**
   * Sets the style of each link, by edge index, written as {@code linkStyle}; {@code null}
   * leaves a link as it is.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param style the style of an edge, such as {@code stroke:red,stroke-width:2px}
   * @return this writer
   */
  public MermaidWriter linkStyle(IntFunction<@Nullable String> style) {
    this.linkStyle = style;
    return this;
  }

  /**
   * Draws each group of the partition as a subgraph, labeled by group ID; {@code null} shows the
   * subgraph's name {@code c}<i>group</i>. Nodes in no group stay outside every subgraph.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param partition the groups, over the nodes of this writer's graph - {@code Sccs} among them
   * @param label     the label of a subgraph
   * @return this writer
   * @throws IllegalArgumentException if the partition covers a different number of nodes
   */
  public MermaidWriter clusters(NodePartition partition, IntFunction<@Nullable String> label) {
    if (partition.nodeCount() != selection.graph.nodeCount()) {
      throw new IllegalArgumentException("the partition covers " + partition.nodeCount()
          + " nodes, the graph has " + selection.graph.nodeCount());
    }
    selection.clusters = new Selection.Clusters(partition, label);
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
  public MermaidWriter nodeFilter(IntPredicate include) {
    selection.nodeFilter = include;
    return this;
  }

  /**
   * Writes the undirected reading: links without arrowheads, each reciprocal pair once, as its
   * edge from the lower node ID, whose index the edge hooks receive.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return this writer
   */
  public MermaidWriter undirected() {
    selection.undirected = true;
    return this;
  }

  /**
   * Writes the flowchart to {@code out}.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount) plus the hooks; in the undirected reading,
   * O(log outDegree) more per edge to find reciprocal pairs.
   *
   * @param out where to write; not closed
   * @throws IOException if {@code out} throws
   * @throws IllegalArgumentException if a hook returns a class name that is not an identifier or
   *                                  is {@code end}, or an empty link style or one with a line
   *                                  break or semicolon; what was written before stays in
   *                                  {@code out}
   */
  public void write(Appendable out) throws IOException {
    Graph graph = selection.graph;
    int n = graph.nodeCount();
    out.append("flowchart ").append(direction.code).append('\n');
    for (var classDef : classDefs.entrySet()) {
      out.append("  classDef ").append(classDef.getKey()).append(' ').append(classDef.getValue())
          .append('\n');
    }

    Selection.Clusters clusters = selection.clusters;
    if (clusters != null) {
      NodePartition partition = clusters.partition();
      for (int group = 0; group < partition.groupCount(); group++) {
        var nodes = partition.nodesOf(group);
        boolean opened = false;
        for (int i = 0; i < nodes.size(); i++) {
          int node = nodes.getInt(i);
          if (!selection.includes(node)) continue;
          if (!opened) {
            out.append("  subgraph c").append(Integer.toString(group));
            String label = clusters.label().apply(group);
            if (label != null) {
              out.append('[');
              quote(label, out);
              out.append(']');
            }
            out.append('\n');
            opened = true;
          }
          writeNode(node, "    ", out);
        }
        if (opened) out.append("  end\n");
      }
    }
    for (int node = 0; node < n; node++) {
      if (selection.includes(node) && selection.clusterOf(node) < 0) writeNode(node, "  ", out);
    }

    String link = selection.undirected ? " --- " : " --> ";
    var styles = new StringBuilder();
    int[] ordinal = {0};
    selection.forEachWrittenEdge((source, target, edgeIndex) -> {
      out.append("  n").append(Integer.toString(source));
      String label = edgeLabel == null ? null : edgeLabel.apply(edgeIndex);
      if (label == null) {
        out.append(link);
      } else {
        out.append(link, 0, link.length() - 1).append('|');
        quote(label, out);
        out.append("| ");
      }
      out.append('n').append(Integer.toString(target)).append('\n');
      String style = linkStyle == null ? null : linkStyle.apply(edgeIndex);
      if (style != null) {
        styles.append("  linkStyle ").append(ordinal[0]).append(' ').append(checkStyle(style))
            .append('\n');
      }
      ordinal[0]++;
    });

    if (nodeClass != null) {
      for (int node = 0; node < n; node++) {
        if (!selection.includes(node)) continue;
        String className = nodeClass.apply(node);
        if (className != null) {
          out.append("  class n").append(Integer.toString(node)).append(' ')
              .append(checkClass(className)).append('\n');
        }
      }
    }
    out.append(styles);
  }

  /**
   * Returns the Mermaid text of the graph, as {@link #write(Appendable)} writes it.
   * <p>
   * <strong>Time:</strong> as for {@link #write(Appendable)}.
   *
   * @return the Mermaid text
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

  private void writeNode(int node, String indent, Appendable out) throws IOException {
    out.append(indent).append('n').append(Integer.toString(node));
    String label = nodeLabel == null ? null : nodeLabel.apply(node);
    Shape shape = nodeShape == null ? null : nodeShape.apply(node);
    if (label != null || (shape != null && shape != Shape.RECTANGLE)) {
      Shape drawn = shape == null ? Shape.RECTANGLE : shape;
      out.append(drawn.open);
      quote(label == null ? "n" + node : label, out);
      out.append(drawn.close);
    }
    out.append('\n');
  }

  private static String checkClass(String name) {
    if (!Selection.isIdentifier(name) || name.equalsIgnoreCase("end")) {
      throw new IllegalArgumentException("not a Mermaid class name: " + name);
    }
    return name;
  }

  private static String checkStyle(String style) {
    if (style.isBlank() || style.indexOf('\n') >= 0 || style.indexOf('\r') >= 0
        || style.indexOf(';') >= 0) {
      throw new IllegalArgumentException("a Mermaid style must be non-empty, without a line break "
          + "or semicolon: " + style);
    }
    return style;
  }

  /** Writes {@code text} as a Mermaid quoted string that shows exactly that text. */
  static void quote(String text, Appendable out) throws IOException {
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("#quot;");
        case '#' -> out.append("#35;");
        case '&' -> out.append("#38;");
        case '`' -> out.append("#96;");
        case '<' -> out.append("#lt;");
        case '>' -> out.append("#gt;");
        case '\n' -> out.append("<br>");
        case '\r' -> {
          if (i + 1 >= text.length() || text.charAt(i + 1) != '\n') out.append("<br>");
        }
        default -> out.append(c);
      }
    }
    out.append('"');
  }
}
