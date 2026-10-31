package com.kobayami.graphity.dot;

import java.util.Map;

/**
 * Adds format attributes for one node or edge, addressed by node ID or edge index - the hook of
 * {@link DotWriter#nodeAttributes(AttributeHook)} and {@link DotWriter#edgeAttributes(AttributeHook)}.
 * <p>
 * The map holds the label from {@code nodeLabel} or {@code edgeLabel}, if one is set, and nothing
 * else; the hook may add to it, replace the label or remove it. Entries are written in insertion
 * order, values escaped by the writer, and a name must be an identifier other than a DOT
 * keyword. The writer reuses the map for every element, so the hook must not keep it.
 *
 * @see DotWriter
 */
@FunctionalInterface
public interface AttributeHook {

  /**
   * Puts the attributes of one element.
   *
   * @param id         the node ID or edge index
   * @param attributes the attributes to fill, name to value
   */
  void accept(int id, Map<String, String> attributes);
}
