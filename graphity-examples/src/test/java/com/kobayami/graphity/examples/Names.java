package com.kobayami.graphity.examples;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;

/** The comments of the examples give node IDs with their names; the tests compare both. */
final class Names {

  private Names() {}

  static List<String> of(IntList nodes, String[] names) {
    return nodes.intStream().mapToObj(node -> names[node]).toList();
  }
}
