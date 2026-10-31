package com.kobayami.graphity;

import java.util.Arrays;

/**
 * Growth of the frame stacks of the iterative depth-first cores. A frame is two interleaved
 * ints, the node and its resume position in the adjacency array, so a stack of depth
 * {@code d} needs {@code 2 x d} ints.
 * <p>
 * Kept out of the cores because it runs once per doubling, not per edge; the cores themselves
 * stay duplicated (architecture §7.4).
 */
final class DfsFrames {

  /** Most frames a stack holds, 2^30 - 5: as many as fit into the largest array. */
  private static final int MAX_DEPTH = ArrayLimits.MAX_NODE_COUNT_TWO_INTS;

  /** The largest array, rounded down to whole frames. */
  private static final int MAX_LENGTH = 2 * MAX_DEPTH;

  private DfsFrames() {}

  /** Initial capacity: one frame per node, but at least 16 and at most 1024 frames. */
  static int[] initial(int nodeCount) {
    return new int[2 * Math.max(16, Math.min(nodeCount, 1024))];
  }

  /**
   * Doubles the stack, capped at one frame per node and at the array limit.
   *
   * @throws IllegalArgumentException if the search is deeper than an array can hold, which
   *                                  takes a path of more than 2^30 - 5 nodes
   */
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
