package com.kobayami.graphity;

import java.util.Arrays;

// The growth of the frame stacks of the depth-first searches without recursion. A frame is two
// interleaved ints, the node and its resume position in the adjacency array, so a stack of depth
// d needs 2 x d ints. Only the growth is shared, as it runs once per doubling; each search keeps
// its own loop over the edges, which a shared loop would slow down.
final class DfsFrames {

  // The most frames a stack holds, 2^30 - 5: as many as fit into the largest array.
  private static final int MAX_DEPTH = ArrayLimits.MAX_NODE_COUNT_TWO_INTS;

  // The largest array, rounded down to whole frames.
  private static final int MAX_LENGTH = 2 * MAX_DEPTH;

  private DfsFrames() {}

  // One frame per node, so that the stack of a small graph never grows, but at least 16 and at
  // most 1024 frames: a large graph grows its stack only if a path is that deep.
  static int[] initial(int nodeCount) {
    return new int[2 * Math.max(16, Math.min(nodeCount, 1024))];
  }

  // Doubles the stack, capped at one frame per node and at the array limit. Throws
  // IllegalArgumentException if the search is deeper than an array can hold, which takes a path
  // of more than 2^30 - 5 nodes; the run methods and algorithms document it.
  static int[] grow(int[] frames, int nodeCount) {
    long wanted = Math.min(2L * frames.length, 2L * nodeCount);
    if (wanted > MAX_LENGTH) {
      if (frames.length >= MAX_LENGTH) {
        throw new IllegalArgumentException("a depth-first path is longer than " + MAX_DEPTH
            + " nodes, the most frames an array can hold");
      }
      wanted = MAX_LENGTH;
    }
    return Arrays.copyOf(frames, (int) wanted);
  }
}
