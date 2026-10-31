package com.kobayami.graphity;

// The largest array the library allocates, and the limits derived from it.
//
// One rule for all of them: the JDK's soft maximum array length, Integer.MAX_VALUE - 8, which
// every JVM can allocate given the heap. HotSpot's own limit lies a few elements higher and
// depends on the object header layout, so a limit taken from it would not hold on every JVM.
// graphity-controlflow cannot see this class and repeats the numbers it needs, each with a note
// that it follows this one.
final class ArrayLimits {

  // The JDK's soft maximum array length.
  static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

  // Largest node count of a graph, Integer.MAX_VALUE - 9: its nodeCount + 1 node offsets fill the
  // largest array. Also the node limit of GraphBuilder, the group limit of NodePartition.of and
  // the bound on the node count that Graph.readFrom accepts from a header.
  static final int MAX_NODE_COUNT = MAX_ARRAY_LENGTH - 1;

  // Largest node count for an array of two ints per node, 2^30 - 5: the lanes of Tarjan's
  // algorithm, the double cover of Graphs.isBipartite and the frames of a depth-first path through
  // every node.
  static final int MAX_NODE_COUNT_TWO_INTS = MAX_ARRAY_LENGTH / 2;

  private ArrayLimits() {}

  // Rejects a node count above maxNodeCount with an IllegalArgumentException that names the
  // algorithm, for the algorithms that keep two ints per node in one array, counting a virtual
  // root as a node if the algorithm adds one.
  static void checkNodeCount(int nodeCount, int maxNodeCount, String algorithm) {
    if (nodeCount > maxNodeCount) {
      throw new IllegalArgumentException(algorithm + " keeps two ints per node in one array,"
          + " counting a virtual root if it adds one, and so requires nodeCount <= "
          + maxNodeCount + ", but was " + nodeCount);
    }
  }
}
