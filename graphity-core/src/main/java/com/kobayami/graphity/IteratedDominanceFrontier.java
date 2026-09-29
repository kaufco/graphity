package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.Arrays;
import java.util.Objects;

/**
 * Iterated dominance frontiers of node sets - the phi placement of SSA construction.
 * <p>
 * The iterated dominance frontier of a set of nodes is their dominance frontier, then the
 * frontier of the result, until nothing new is added. For SSA form, it is where a variable
 * defined in the given blocks needs a phi node. A compiler asks this once per variable, so the
 * type is built once per control-flow graph and then queried:
 *
 * <pre>{@code
 * var idf = IteratedDominanceFrontier.of(Dominators.of(cfg, entry));
 * for (var variable : variables) {
 *     SortedIntView phiBlocks = idf.frontierOf(definitionBlocks(variable));
 * }
 * }</pre>
 *
 * Queries follow Sreedhar and Gao: from the deepest given node upwards, walk its subtree of the
 * dominator tree and collect the targets of edges that leave the subtree no deeper than its
 * root. The frontier relation itself, which can grow quadratically, is never materialized, and a
 * query touches only the part of the graph it explores - it allocates no array sized by the
 * whole graph.
 * <p>
 * The dominator tree is held in preorder with subtree sizes, so walking a subtree is one linear
 * scan that skips already walked parts in a single step.
 * <p>
 * Immutable and safe to share across threads; every query allocates its own scratch space.
 * <p>
 * <strong>Memory:</strong> 16 x nodeCount bytes, plus the {@link Dominators} it reads.
 *
 * @see Dominators
 * @see Graphs#dominanceFrontiers(Dominators)
 */
public final class IteratedDominanceFrontier {

  private final Dominators dominators;

  /** Depth of each node in the dominator forest, {@code 0} for a root, {@code -1} if unreachable. */
  private final int[] depth;

  /** Position of each node in the preorder of the dominator forest. */
  private final int[] preorderIndex;

  /** Size of each node's subtree in the dominator forest, the node included. */
  private final int[] subtreeSize;

  /** Node at each preorder position. */
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
   * Prepares iterated-frontier queries on the given dominators.
   * <p>
   * Not cached - keep the result and query it as often as needed.
   * <p>
   * <strong>Time:</strong> O(nodeCount).<br>
   * <strong>Memory:</strong> 16 x nodeCount bytes for the result; 8 x nodeCount bytes
   * additional during computation.
   *
   * @param dominators the dominators of the control-flow graph
   * @return the query structure
   */
  public static IteratedDominanceFrontier of(Dominators dominators) {
    int[] parent = dominators.immediateDominator;
    int n = parent.length;
    var preorderIndex = new int[n];
    var order = new int[n];
    var subtreeSize = new int[n];
    ParentForest.preorder(
        parent, ParentForest.childrenOf(parent), preorderIndex, order, subtreeSize);

    // Preorder visits every parent before its children.
    var depth = new int[n];
    for (int i = 0; i < n; i++) {
      int node = order[i];
      int p = parent[node];
      if (p >= 0) {
        depth[node] = depth[p] + 1;
      } else {
        depth[node] = dominators.isReachable(node) ? 0 : -1;
      }
    }
    return new IteratedDominanceFrontier(dominators, depth, preorderIndex, subtreeSize, order);
  }

  /**
   * Returns the iterated dominance frontier of the given nodes.
   * <p>
   * The result may contain given nodes themselves, for instance a loop header that is both
   * defined in and reached through its own loop. Unreachable nodes in the input are ignored, and
   * duplicates are harmless. The result is newly allocated and sorted ascending.
   * <p>
   * <strong>Time:</strong> O(e + (s + k) log(s + k)), where e counts the nodes and out-edges the
   * query explores - at most nodeCount + edgeCount, usually far fewer -, s the distinct reachable
   * given nodes and k the result size. The logarithm is the priority queue by depth.<br>
   * <strong>Memory:</strong> O(explored nodes) during computation, plus 4 x k bytes for the
   * result.
   *
   * @param nodes the nodes, each in {@code 0..nodeCount()-1}
   * @return the iterated dominance frontier, sorted ascending
   * @throws IndexOutOfBoundsException if a node is out of range
   * @throws IllegalArgumentException if the query explores more nodes than its hash sets can
   *                                  hold, about 8 x 10^8
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

    int[] out = dominators.graph.outAdjacencyStart;
    int[] adj = dominators.graph.outAdjacency;
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
      walked.add(root);
      int position = preorderIndex[root];
      int end = position + subtreeSize[root];
      while (position < end) {
        int x = order[position];
        if (x != root && !walked.add(x)) {
          position += subtreeSize[x];
          continue;
        }
        for (int i = out[x], last = out[x + 1]; i < last; i++) {
          int y = adj[i];
          // Edges into the dominator subtree of x are deeper than the root and fall out here,
          // so only edges that leave the subtree remain.
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

  /**
   * Adds {@code (key, node)} to the binary max-heap of interleaved pairs holding {@code size}
   * entries, growing the array if needed, and returns the array. Every node enters at most once,
   * so the array never needs more than two slots per node of the graph, and the growth is capped
   * there instead of doubling past {@code Integer.MAX_VALUE}.
   */
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

  /** Removes the top of the heap holding {@code size} entries and returns the new size. */
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
