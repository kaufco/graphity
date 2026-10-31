package com.kobayami.graphity.controlflow;

import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.SortedIntView;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.Arrays;
import java.util.Objects;

/**
 * The iterated dominance frontiers of a control-flow graph, which tell where SSA (static single
 * assignment) form places phi-nodes.
 * <p>
 * In SSA form, every assignment to a variable creates a new version of it, and a phi-node merges
 * the versions where paths of control flow meet. The phi-nodes of a variable go into the iterated
 * dominance frontier of the blocks that assign it: the dominance frontier of these blocks, then
 * the dominance frontier of the result, and so on until nothing new is added. A compiler asks
 * this once per variable, so {@link #of(Dominators)} prepares the queries once per control-flow
 * graph, and {@link #frontierOf(IntList)} answers them. Here, for the loop variable {@code i} of
 * a method {@code load}, whose control flow {@code cfg} has the blocks {@code entry} and
 * {@code next}:
 * {@snippet file="com/kobayami/graphity/examples/AnalyzingControlFlow.java" region="frontierOf"}
 * Uses the algorithm of Sreedhar and Gao. A query walks the subtrees of the dominator tree below
 * the nodes it is given and the nodes it adds, so its cost depends on the part of the graph it
 * explores, not on the whole graph. It never builds the dominance frontiers themselves, which can
 * grow quadratically with the node count; {@link ControlFlow#dominanceFrontiers(Dominators)}
 * returns them as a graph.
 * <p>
 * Instances are immutable and safe to share between threads.
 * <p>
 * <strong>Memory:</strong> 16 x nodeCount bytes, plus the {@link Dominators} it was built from,
 * which stay reachable as long as it is held.
 */
public final class IteratedDominanceFrontier {

  private final Dominators dominators;

  // Depth of each node in the dominator forest, 0 for a root, -1 if unreachable.
  private final int[] depth;

  // Position of each node in the preorder of the dominator forest.
  private final int[] preorderIndex;

  // Size of each node's subtree in the dominator forest, the node included.
  private final int[] subtreeSize;

  // Node at each preorder position.
  private final int[] order;

  private IteratedDominanceFrontier(
      Dominators dominators, int[] depth, int[] preorderIndex, int[] subtreeSize, int[] order) {
    this.dominators = dominators;
    this.depth = depth;
    this.preorderIndex = preorderIndex;
    this.subtreeSize = subtreeSize;
    this.order = order;
  }

  /**
   * Returns the iterated dominance frontiers of the graph that {@code dominators} were computed
   * on, ready for queries.
   * <p>
   * They are prepared anew on every call, not cached: keep the result and query it for every
   * variable.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 16 x nodeCount bytes for the result; while it runs, 8 x nodeCount
   * bytes more.
   *
   * @param dominators the dominators of the control-flow graph
   * @return the iterated dominance frontiers
   */
  public static IteratedDominanceFrontier of(Dominators dominators) {
    Graph tree = Graphs.dominatorTree(dominators);
    int n = tree.nodeCount();
    int[] childStart = Csr.outAdjacencyStart(tree);
    int[] children = Csr.outAdjacency(tree);
    var preorderIndex = new int[n];
    var order = new int[n];
    var subtreeSize = new int[n];
    var depth = new int[n];

    // Canonical preorder - roots ascending, children ascending - as ParentForest.preorder in the
    // core computes it; change both together. Every node is pushed exactly once, so the stack fits
    // in n slots; the subtree-size array serves as the stack until it is filled below. Preorder
    // visits every parent before its children, so depth follows on the way.
    int[] stack = subtreeSize;
    int top = 0;
    for (int node = n - 1; node >= 0; node--) {
      if (dominators.immediateDominatorOf(node) < 0) {
        stack[top++] = node;
        depth[node] = dominators.isReachable(node) ? 0 : -1;
      }
    }
    int position = 0;
    while (top > 0) {
      int node = stack[--top];
      preorderIndex[node] = position;
      order[position++] = node;
      for (int i = childStart[node + 1] - 1; i >= childStart[node]; i--) {
        int child = children[i];
        depth[child] = depth[node] + 1;
        stack[top++] = child;
      }
    }

    // Reverse preorder visits every child before its parent.
    Arrays.fill(subtreeSize, 1);
    for (int i = n - 1; i >= 0; i--) {
      int node = order[i];
      int p = dominators.immediateDominatorOf(node);
      if (p >= 0) subtreeSize[p] += subtreeSize[node];
    }
    return new IteratedDominanceFrontier(dominators, depth, preorderIndex, subtreeSize, order);
  }

  /**
   * Returns the iterated dominance frontier of {@code nodes}, in ascending order.
   * <p>
   * The result can contain given nodes, such as the header of a natural loop, which is in its own
   * dominance frontier. Nodes that the entries do not reach are ignored, and so are duplicates.
   * <p>
   * <strong>Time:</strong> O(nodes.size() + e + (s + k) log(s + k)), where e is the number of
   * nodes and edges the query explores, at most nodeCount + edgeCount; s is the number of
   * distinct given nodes that the entries reach, and k the size of the result.<br>
   * <strong>Memory:</strong> 4 x k bytes for the result; while it runs, up to about 60 bytes per
   * node the query explores, and nothing sized by the whole graph.
   *
   * @param nodes node IDs in {@code 0..nodeCount-1}, such as the blocks that assign a variable
   * @return the iterated dominance frontier
   * @throws IndexOutOfBoundsException if a node is out of range
   * @throws IllegalArgumentException if the query explores more than about 8 x 10<sup>8</sup>
   *                                  nodes
   */
  public SortedIntView frontierOf(IntList nodes) {
    int n = depth.length;
    int size = nodes.size();
    // Sized by a capped guess and grown on demand, so that duplicates and unreachable nodes in a
    // long input cost no scratch space.
    int initial = Math.min(Math.max(4, size), 1024);
    var given = new IntOpenHashSet(initial);
    var heap = new int[2 * initial];
    int heapSize = 0;
    for (int i = 0; i < size; i++) {
      int node = nodes.getInt(i);
      Objects.checkIndex(node, n);
      if (depth[node] < 0 || !given.add(node)) continue;
      heap = push(heap, heapSize++, depth[node], node, n);
    }

    int[] out = Csr.outAdjacencyStart(dominators.graph());
    int[] adj = Csr.outAdjacency(dominators.graph());
    var inFrontier = new IntOpenHashSet();
    var walked = new IntOpenHashSet();
    var result = new IntArrayList();

    // Deepest root first. A subtree walked once need not be walked again for a shallower root:
    // every edge it could contribute leaves no deeper than the earlier root, and was taken then.
    // Walked parts are closed under descendants, so the scan skips each one in a single step.
    while (heapSize > 0) {
      int rootDepth = heap[0];
      int root = heap[1];
      heapSize = pop(heap, heapSize);
      int position = preorderIndex[root];
      int end = position + subtreeSize[root];
      while (position < end) {
        int x = order[position];
        if (!walked.add(x)) {
          position += subtreeSize[x];
          continue;
        }
        for (int i = out[x], last = out[x + 1]; i < last; i++) {
          int y = adj[i];
          // Targets deeper than the root lie strictly inside its subtree and fall out here; what
          // remains - the root itself or a node beyond its subtree - is in its frontier.
          int yDepth = depth[y];
          if (yDepth > rootDepth || !inFrontier.add(y)) continue;
          result.add(y);
          if (!given.contains(y)) heap = push(heap, heapSize++, yDepth, y, n);
        }
        position++;
      }
    }

    int[] sorted = result.toIntArray();
    Arrays.sort(sorted);
    return SortedIntView.viewOf(sorted, 0, sorted.length);
  }

  // Adds (key, node) to the binary max-heap of interleaved pairs holding size entries, growing
  // the array if needed, and returns the array. Every node enters at most once, so the array
  // never needs more than two slots per node of the graph, and the growth is capped there
  // instead of doubling past Integer.MAX_VALUE.
  private static int[] push(int[] heap, int size, int key, int node, int nodeCount) {
    if (2 * (size + 1) > heap.length) {
      heap = Arrays.copyOf(heap, (int) Math.min(2L * heap.length, 2L * nodeCount));
    }
    int i = size;
    while (i > 0) {
      int parent = (i - 1) >> 1;
      if (heap[2 * parent] >= key) break;
      heap[2 * i] = heap[2 * parent];
      heap[2 * i + 1] = heap[2 * parent + 1];
      i = parent;
    }
    heap[2 * i] = key;
    heap[2 * i + 1] = node;
    return heap;
  }

  // Removes the top of the heap holding size entries and returns the new size.
  private static int pop(int[] heap, int size) {
    int last = size - 1;
    int key = heap[2 * last];
    int node = heap[2 * last + 1];
    int i = 0;
    while (true) {
      int child = 2 * i + 1;
      if (child >= last) break;
      if (child + 1 < last && heap[2 * (child + 1)] > heap[2 * child]) child++;
      if (heap[2 * child] <= key) break;
      heap[2 * i] = heap[2 * child];
      heap[2 * i + 1] = heap[2 * child + 1];
      i = child;
    }
    heap[2 * i] = key;
    heap[2 * i + 1] = node;
    return last;
  }
}
