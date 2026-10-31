package com.kobayami.graphity.controlflow;

import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Objects;

// The depth-first preorder of the nodes reachable from a list of entries, the first step of
// Loops. A copy of DfsPreorder in graphity-core, the first step of Dominators, as a module cannot
// see that class; change both together.
//
// Preorder numbers start at 1. Number 0 is a virtual root above all entries, so that several
// entries need no special case downstream: every entry the search starts from is a child of the
// virtual root, and isEntry marks every entry, also one that the search reached from an earlier
// entry. Out-nodes are visited in ascending order and entries in list order. Without recursion;
// the frame stack lives on the heap.
final class DfsPreorder {

  // The largest supported node count, 2^30 - 6: Dominators holds two ints per preorder number,
  // the virtual root included, in one array, so 2 x (MAX_NODE_COUNT + 1) has to stay within the
  // largest array. Loops takes the same limit, so that it accepts the graphs that Dominators
  // accepts.
  static final int MAX_NODE_COUNT = ControlFlow.MAX_ARRAY_LENGTH / 2 - 1;

  // The number of nodes reachable from the entries.
  final int reachedCount;

  // The node with each preorder number; order[0] == -1 for the virtual root.
  final int[] order;

  // The preorder number of each node; 0 for a node not reachable from any entry.
  final int[] preorderNumber;

  // The preorder number of the parent of each node in the search tree; 0 below the virtual root.
  final int[] parent;

  // A bit set over preorder numbers, set for every entry.
  private final long[] entryBits;

  private DfsPreorder(
      int reachedCount, int[] order, int[] preorderNumber, int[] parent, long[] entryBits) {
    this.reachedCount = reachedCount;
    this.order = order;
    this.preorderNumber = preorderNumber;
    this.parent = parent;
    this.entryBits = entryBits;
  }

  // Runs the search; analysis names the caller in the message of the node limit. Throws
  // IndexOutOfBoundsException for an entry out of range and IllegalArgumentException above
  // MAX_NODE_COUNT nodes.
  static DfsPreorder of(Graph graph, IntList entries, String analysis) {
    int n = graph.nodeCount();
    checkNodeCount(n, analysis);
    int entryCount = entries.size();
    for (int i = 0; i < entryCount; i++) {
      Objects.checkIndex(entries.getInt(i), n);
    }
    int[] out = Csr.outAdjacencyStart(graph);
    int[] adj = Csr.outAdjacency(graph);

    var preorderNumber = new int[n];
    var order = new int[n + 1];
    var parent = new int[n + 1];
    order[0] = -1;
    parent[0] = -1;
    // A frame is (node, resume cursor), interleaved, so both reads of a frame hit one cache line.
    var frames = new int[2 * Math.max(16, Math.min(n, 1024))];
    int count = 0;

    for (int e = 0; e < entryCount; e++) {
      int entry = entries.getInt(e);
      if (preorderNumber[entry] != 0) continue;
      preorderNumber[entry] = ++count;
      order[count] = entry;
      parent[count] = 0;
      frames[0] = entry;
      frames[1] = out[entry];
      int top = 0;

      outer:
      while (top >= 0) {
        int u = frames[2 * top];
        int i = frames[2 * top + 1];
        int end = out[u + 1];
        while (i < end) {
          int v = adj[i++];
          if (preorderNumber[v] == 0) {
            frames[2 * top + 1] = i;
            preorderNumber[v] = ++count;
            order[count] = v;
            parent[count] = preorderNumber[u];
            if (2 * (++top + 1) > frames.length) {
              // At most one frame per node, so 2 x nodeCount suffices and cannot overflow.
              frames = Arrays.copyOf(frames, (int) Math.min(2L * frames.length, 2L * n));
            }
            frames[2 * top] = v;
            frames[2 * top + 1] = out[v];
            continue outer;
          }
        }
        top--;
      }
    }

    var entryBits = new long[(count >> 6) + 1];
    for (int e = 0; e < entryCount; e++) {
      int number = preorderNumber[entries.getInt(e)];
      entryBits[number >> 6] |= 1L << number;
    }
    return new DfsPreorder(count, order, preorderNumber, parent, entryBits);
  }

  // The message names the reason of the limit, which lies in Dominators, not in the caller.
  static void checkNodeCount(int nodeCount, String analysis) {
    if (nodeCount > MAX_NODE_COUNT) {
      throw new IllegalArgumentException(analysis + " takes the node limit of Dominators, which"
          + " keeps two ints per node in one array, counting its virtual root: nodeCount must be"
          + " <= " + MAX_NODE_COUNT + ", but was " + nodeCount);
    }
  }

  // Whether the node with this preorder number is one of the entries, also if the search reached
  // it from an earlier entry.
  boolean isEntry(int number) {
    return (entryBits[number >> 6] & (1L << number)) != 0;
  }
}
