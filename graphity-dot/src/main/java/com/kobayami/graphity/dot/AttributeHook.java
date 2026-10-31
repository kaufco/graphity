package com.kobayami.graphity.dot;

import java.util.Map;

/**
 * A hook that puts the attributes of one node or edge, by node ID or edge index, for
 * {@link DotWriter#nodeAttributes(AttributeHook)} and
 * {@link DotWriter#edgeAttributes(AttributeHook)}.
 * <p>
 * The attributes, such as {@code color} or {@code shape}, are those of DOT (the language of
 * Graphviz). The writer passes a map from attribute name to value. It holds the label from
 * {@link DotWriter#nodeLabel(java.util.function.IntFunction) nodeLabel} or
 * {@link DotWriter#edgeLabel(java.util.function.IntFunction) edgeLabel} as the attribute
 * {@code label}, if there is one, and nothing else; the hook can add attributes, and replace or
 * remove the label. The writer writes the attributes in the order they were put and escapes
 * their values; a name that is not an identifier or is a DOT keyword makes
 * {@link DotWriter#write(java.io.Appendable)} throw an {@link IllegalArgumentException}. The
 * writer reuses the map for every node and edge, so the hook must not keep it.
 */
@FunctionalInterface
public interface AttributeHook {

  /**
   * Puts the attributes of the node or edge {@code id} into {@code attributes}.
   *
   * @param id         node ID or edge index
   * @param attributes the attributes to write, by name
   */
  void accept(int id, Map<String, String> attributes);
}
