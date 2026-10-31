package com.kobayami.graphity.mermaid;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.NodePartition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import org.jspecify.annotations.Nullable;

/**
 * A writer of Mermaid flowcharts that labels and styles a graph with the caller's data by node ID
 * and edge index. Mermaid flowcharts render in Markdown.
 * <p>
 * {@link #of(Graph)} returns a writer for a graph, the methods that configure it return the writer
 * again, and {@link #write(Appendable)} or {@link #writeToString()} write the text. The graph holds
 * no data, so labels, shapes, classes and styles come from hooks that receive a node ID or an edge
 * index and read the caller's arrays. Here, the packages of a small program get their names from
 * {@code names}. The strongly connected components {@code sccs} tell which packages are in a
 * cycle, and these are drawn in red:
 * {@snippet file="com/kobayami/graphity/examples/WritingMermaid.java" region="nodeClass"}
 * Nodes are named by their node IDs after an {@code n}, such as {@code n4}. The text lists the
 * class definitions, the clusters in group order with their nodes, then the nodes in no cluster,
 * then the edges in edge index order, the classes of the nodes and the edge styles, so the same
 * graph and hooks always give the same text. Mermaid numbers the edges in the order they are
 * written; the writer translates edge indices into these numbers for {@code linkStyle}, also when
 * a filter leaves edges out.
 * <p>
 * Labels are written as quoted strings: the writer turns quotes, {@code #}, {@code %}, {@code &},
 * {@code :}, {@code <}, {@code >} and backticks into Mermaid entity codes and line breaks into
 * {@code <br>}, so that no label reads as HTML, as a Markdown string or as a directive. For the
 * same reason, whitespace right after the word {@code direction} becomes an entity code, and an
 * empty label, which Mermaid rejects, is written as one space.
 * <p>
 * Mermaid's renderer still changes some labels. Mermaid 11 applies Markdown emphasis and
 * backslash escapes to every label, so {@code 2*3*4} loses its asterisks; Mermaid 12 shows a
 * backslash before {@code n} as a line break. Both read <code>&para;&szlig;</code> and
 * <code>&#xFB02;&deg;</code>, which Mermaid uses internally for entity codes, as {@code ;} and
 * {@code &}. Both also turn {@code fa:fa-}<i>name</i> into an icon in a node label with a line
 * break. With {@code htmlLabels} set to {@code false}, Mermaid shows some entity codes as written,
 * such as {@code &#58;} for {@code :}.
 * <p>
 * Class names must be identifiers - an ASCII letter or underscore, then ASCII letters, digits or
 * underscores - other than a Mermaid keyword: {@code style}, {@code linkStyle},
 * {@code interpolate}, {@code classDef}, {@code class}, {@code href}, {@code click}, {@code call},
 * {@code graph}, {@code flowchart}, {@code subgraph}, {@code end}, {@code _self}, {@code _blank},
 * {@code _parent} and {@code _top}. Keywords are case-sensitive, so {@code End} is a valid class
 * name. Styles are written as given and must not be blank or contain a line break or a semicolon.
 * <p>
 * Mermaid renders graphs of a few hundred nodes well; for larger ones, {@code DotWriter} in the
 * module {@code graphity-dot} writes DOT (the language of Graphviz). A writer keeps its
 * configuration and can write any number of times.
 * <p>
 * Instances are mutable and not safe to share between threads.
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

  // The words Mermaid's flowchart lexer reads as keywords, never as a class name.
  private static final Set<String> KEYWORDS = Set.of("style", "linkStyle", "interpolate",
      "classDef", "class", "href", "click", "call", "graph", "flowchart", "subgraph", "end",
      "_self", "_blank", "_parent", "_top");

  // The word that starts a direction statement; whitespace after it is encoded in labels.
  private static final String DIRECTION_WORD = "direction";

  private final Selection selection;
  private Direction direction = Direction.TOP_DOWN;
  private final Map<String, String> classDefs = new LinkedHashMap<>();
  private @Nullable IntFunction<@Nullable String> nodeLabel;
  private @Nullable IntFunction<@Nullable String> edgeLabel;
  private @Nullable IntFunction<@Nullable Shape> nodeShape;
  private @Nullable IntFunction<@Nullable String> nodeClass;
  private @Nullable IntFunction<@Nullable String> edgeStyle;

  private MermaidWriter(Graph graph) {
    this.selection = new Selection(graph);
  }

  /**
   * Returns a writer for {@code graph}, top down, with no labels, shapes, classes, clusters or
   * filter yet.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param graph the graph to write
   * @return the writer
   */
  public static MermaidWriter of(Graph graph) {
    return new MermaidWriter(graph);
  }

  /**
   * Sets the direction of the flowchart; without a call, it is {@link Direction#TOP_DOWN}.
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
   * Sets the label of each node, by node ID; {@code null} shows the name of the node, such as
   * {@code n4}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param label called with a node ID, returns the label of the node or {@code null}
   * @return this writer
   */
  public MermaidWriter nodeLabel(IntFunction<@Nullable String> label) {
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
  public MermaidWriter edgeLabel(IntFunction<@Nullable String> label) {
    this.edgeLabel = label;
    return this;
  }

  /**
   * Sets the shape of each node, by node ID; {@code null} draws a rectangle.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param shape called with a node ID, returns the shape of the node or {@code null}
   * @return this writer
   */
  public MermaidWriter nodeShape(IntFunction<@Nullable Shape> shape) {
    this.nodeShape = shape;
    return this;
  }

  /**
   * Defines a class for {@link #nodeClass(IntFunction)} to assign, written as
   * {@code classDef name style}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param name  class name, an identifier other than a Mermaid keyword
   * @param style style of the class, such as {@code fill:#f96,stroke:#333}
   * @return this writer
   * @throws IllegalArgumentException if {@code name} is not an identifier or is a Mermaid keyword,
   *                                  or {@code style} is blank or contains a line break or a
   *                                  semicolon
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
   * @param className called with a node ID, returns the name of the class of the node, an
   *                  identifier other than a Mermaid keyword, or {@code null}
   * @return this writer
   */
  public MermaidWriter nodeClass(IntFunction<@Nullable String> className) {
    this.nodeClass = className;
    return this;
  }

  /**
   * Sets the style of each edge, by edge index, written as a {@code linkStyle} statement;
   * {@code null} leaves an edge in the default style.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param style called with an edge index, returns the style of the edge, such as
   *              {@code stroke:red,stroke-width:2px}, or {@code null}
   * @return this writer
   */
  public MermaidWriter edgeStyle(IntFunction<@Nullable String> style) {
    this.edgeStyle = style;
    return this;
  }

  /**
   * Draws each group of {@code partition} as a cluster (a Mermaid {@code subgraph}), with a label
   * by group ID.
   * <p>
   * Nodes in no group stay outside every cluster, and a group without a node to write has no
   * cluster. A label of {@code null} shows the name of the cluster: {@code c} and the group ID,
   * such as {@code c2}.
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
  public MermaidWriter clusters(NodePartition partition, IntFunction<@Nullable String> label) {
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
  public MermaidWriter nodeFilter(IntPredicate include) {
    selection.nodeFilter = include;
    return this;
  }

  /**
   * Switches to the undirected reading of the graph: edges without arrowheads.
   * <p>
   * The edges {@code a -> b} and {@code b -> a} become one, written as the edge from the lower
   * node ID; the edge hooks receive its edge index.
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
   * Writes the graph to {@code out}, as the text of a Mermaid flowchart.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount + groupCount), plus the hooks, with groupCount
   * the number of groups of the partition drawn as clusters; in the undirected reading,
   * O(log outDegree(target)) more per edge.<br>
   * <strong>Memory:</strong> up to twice the size of the {@code linkStyle} lines while it runs, as
   * they are written last.
   *
   * @param out where the text goes; not closed
   * @throws IOException if writing to {@code out} fails
   * @throws IllegalArgumentException if a hook returns a class name that is not an identifier or
   *                                  is a Mermaid keyword, or an edge style that is blank or
   *                                  contains a line break or a semicolon; what was written before
   *                                  stays in {@code out}
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
          if (!selection.isIncluded(node)) continue;
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
      if (selection.isIncluded(node) && selection.clusterOf(node) < 0) writeNode(node, "  ", out);
    }

    String link = selection.undirected ? " ---" : " -->";
    var styles = new StringBuilder();
    int[] ordinal = {0};
    selection.forEachWrittenEdge((source, target, edgeIndex) -> {
      out.append("  n").append(Integer.toString(source)).append(link);
      String label = edgeLabel == null ? null : edgeLabel.apply(edgeIndex);
      if (label != null) {
        out.append('|');
        quote(label, out);
        out.append('|');
      }
      out.append(" n").append(Integer.toString(target)).append('\n');
      String style = edgeStyle == null ? null : edgeStyle.apply(edgeIndex);
      if (style != null) {
        styles.append("  linkStyle ").append(ordinal[0]).append(' ').append(checkStyle(style))
            .append('\n');
      }
      ordinal[0]++;
    });

    if (nodeClass != null) {
      for (int node = 0; node < n; node++) {
        if (!selection.isIncluded(node)) continue;
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
   * Returns the text of the Mermaid flowchart, as {@link #write(Appendable)} writes it.
   * <p>
   * <strong>Time:</strong> as for {@link #write(Appendable)}.<br>
   * <strong>Memory:</strong> the text for the result; while it runs, up to twice as much again,
   * and the {@code linkStyle} lines as for {@link #write(Appendable)}.
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
    if (!Selection.isIdentifier(name) || KEYWORDS.contains(name)) {
      throw new IllegalArgumentException("not a Mermaid class name: " + name);
    }
    return name;
  }

  private static String checkStyle(String style) {
    if (style.isBlank() || style.indexOf('\n') >= 0 || style.indexOf('\r') >= 0
        || style.indexOf(';') >= 0) {
      throw new IllegalArgumentException("a Mermaid style must not be blank, nor contain a line "
          + "break or semicolon: " + style);
    }
    return style;
  }

  // Writes text as a Mermaid quoted string.
  //
  // Mermaid applies regular expressions to the whole text before parsing, strings included: %%
  // starts a directive or a comment, and on a line with style and : the last ; is dropped, which
  // ends an entity code. Its lexer takes any line with direction, whitespace and a direction code
  // for a direction statement. So %, : and whitespace right after direction are written as entity
  // codes too, and an empty text, which Mermaid rejects, as one space.
  private static void quote(String text, Appendable out) throws IOException {
    if (text.isEmpty()) {
      out.append("\" \"");
      return;
    }
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("#quot;");
        case '#' -> out.append("#35;");
        case '%' -> out.append("#37;");
        case '&' -> out.append("#38;");
        case ':' -> out.append("#58;");
        case '`' -> out.append("#96;");
        case '<' -> out.append("#lt;");
        case '>' -> out.append("#gt;");
        case '\n' -> out.append("<br>");
        case '\r' -> {
          if (i + 1 >= text.length() || text.charAt(i + 1) != '\n') out.append("<br>");
        }
        default -> {
          if (isSpace(c) && text.startsWith(DIRECTION_WORD, i - DIRECTION_WORD.length())) {
            out.append('#').append(Integer.toString(c)).append(';');
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }

  // Whether c is whitespace, at least everything JavaScript's \s matches.
  private static boolean isSpace(char c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\uFEFF';
  }
}
