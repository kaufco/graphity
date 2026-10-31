package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Objects;

// Argument checks shared by several classes of the package.
final class Checks {

  private Checks() {}

  // Checks every node of a list of start nodes, sources or entries before any work begins: the
  // check of each IntList form of a traversal or search. Throws IndexOutOfBoundsException for a node
  // outside 0..nodeCount-1.
  static void checkNodes(IntList nodes, int nodeCount) {
    for (int i = 0, size = nodes.size(); i < size; i++) {
      Objects.checkIndex(nodes.getInt(i), nodeCount);
    }
  }
}
