package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;

/**
 * The third example domain: a family tree that records one parent per person, over four
 * generations.
 * <p>
 * Each person has the one parent the tree records, so the graph is a tree: an edge from
 * each parent to each child, rooted at {@code Ada}.
 * <pre>
 * Ada
 * +-- Ben
 * |   +-- Dora
 * |   |   +-- Greta
 * |   |   +-- Hugo
 * |   +-- Emil
 * +-- Cleo
 *     +-- Finn
 *         +-- Ida
 *         +-- Jonas
 * </pre>
 * Node IDs follow the order of the names, which is the order in which {@link #tree()}
 * adds the people, generation by generation. The graph never changes: the comments of the
 * examples, and the documentation that shows them, depend on every node and edge.
 */
public final class SmallFamily {

  private static final String[] NAMES = {
      "Ada", "Ben", "Cleo", "Dora", "Emil", "Finn", "Greta", "Hugo", "Ida", "Jonas"};

  // A person, then the person's parent.
  private static final String[][] PARENT = {
      {"Ben", "Ada"}, {"Cleo", "Ada"},
      {"Dora", "Ben"}, {"Emil", "Ben"}, {"Finn", "Cleo"},
      {"Greta", "Dora"}, {"Hugo", "Dora"}, {"Ida", "Finn"}, {"Jonas", "Finn"},
  };

  private SmallFamily() {}

  /**
   * Returns the family tree: an edge from each parent to each child.
   *
   * @return 10 nodes, 9 edges
   */
  public static Graph tree() {
    var builder = new GraphBuilder();
    builder.addNodes(NAMES.length);
    for (String[] row : PARENT) {
      builder.addEdge(personOf(row[1]), personOf(row[0]));
    }
    return builder.build();
  }

  /**
   * Returns the names of the people, by node ID of {@link #tree()}.
   *
   * @return Ada, Ben, Cleo, Dora, Emil, Finn, Greta, Hugo, Ida, Jonas
   */
  public static String[] names() {
    return NAMES.clone();
  }

  /**
   * Returns the node ID of the person named {@code name}.
   *
   * @param name the name of the person
   * @return the node ID
   * @throws IllegalArgumentException if nobody has this name
   */
  public static int personOf(String name) {
    return SmallProgram.nodeOf(NAMES, name);
  }
}
