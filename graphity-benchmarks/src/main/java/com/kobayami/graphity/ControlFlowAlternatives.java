package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.Arrays;

/**
 * Alternative implementations of the control-flow analyses, for comparison benchmarks only.
 * <p>
 * They live in the package of the library so that they read the same CSR arrays and reuse the
 * same depth-first numbering as the implementations they are compared with; only the part that
 * differs is written out here. Each returns the immediate dominators by node ID, {@code -1} for
 * a root of the dominator forest and for unreachable nodes, exactly as
 * {@link Dominators#immediateDominatorOf(int)} reports them.
 */
public final class ControlFlowAlternatives {

  private static final int UNLINKED = -1;

  private ControlFlowAlternatives() {}

  /**
   * The library's algorithm with the predecessors rebuilt as lists over preorder numbers instead
   * of read from the in-adjacency: 4 x (nodeCount + edgeCount) bytes more, in exchange for
   * sequential access and no translation per edge. Returns the immediate dominators only, without
   * the reachability bit set of {@link Dominators}.
   */
  public static int[] lengauerTarjanPreorderLists(Graph graph, IntList entries) {
    var dfs = DfsPreorder.of(graph, entries, "ControlFlowAlternatives");
    var predecessors = predecessors(graph, dfs);
    int[] start = predecessors.start();
    int[] nodes = predecessors.nodes();
    int size = dfs.reachedCount + 1;
    int[] parent = dfs.parent;
    var semi = new int[size];
    var lanes = new int[2 * size];
    var idom = new int[size];
    var bucket = new int[size];
    Arrays.fill(bucket, -1);
    for (int v = 0; v < size; v++) {
      semi[v] = v;
      lanes[2 * v] = UNLINKED;
      lanes[2 * v + 1] = v;
    }
    for (int v = size - 1; v >= 1; v--) {
      int best = v;
      for (int i = start[v], end = start[v + 1]; i < end; i++) {
        int u = nodes[i];
        int candidate = u < v ? u : semi[eval(lanes, semi, u)];
        if (candidate < best) best = candidate;
      }
      int s = dfs.isEntry(v) ? 0 : best;
      semi[v] = s;
      idom[v] = bucket[s];
      bucket[s] = v;
      int p = parent[v];
      lanes[2 * v] = p;
      for (int w = bucket[p]; w >= 0; ) {
        int next = idom[w];
        int u = eval(lanes, semi, w);
        idom[w] = semi[u] < semi[w] ? u : p;
        w = next;
      }
      bucket[p] = -1;
    }
    for (int v = 1; v < size; v++) {
      if (idom[v] != semi[v]) idom[v] = idom[idom[v]];
    }
    return toNodeIds(graph.nodeCount(), dfs, idom);
  }

  /**
   * SEMI-NCA without jump pointers: the same semidominators, then a linear walk up the
   * dominator tree for each node. Quadratic on a fan behind a chain.
   */
  public static int[] semiNcaPlain(Graph graph, IntList entries) {
    var dfs = DfsPreorder.of(graph, entries, "ControlFlowAlternatives");
    int size = dfs.reachedCount + 1;
    int[] idom = dfs.parent;
    var semi = new int[size];
    var lanes = new int[2 * size];
    semidominators(graph, dfs, semi, lanes);
    for (int v = 1; v < size; v++) {
      int s = semi[v];
      int x = idom[v];
      while (x > s) x = idom[x];
      idom[v] = x;
    }
    return toNodeIds(graph.nodeCount(), dfs, idom);
  }

  /**
   * SEMI-NCA with skew-binary jump pointers (Myers) in the second phase: O(log depth) instead of
   * a linear walk per node, O(m log n) overall. Predecessor lists rebuilt in preorder.
   */
  public static int[] semiNcaJumps(Graph graph, IntList entries) {
    var dfs = DfsPreorder.of(graph, entries, "ControlFlowAlternatives");
    int size = dfs.reachedCount + 1;
    int[] idom = dfs.parent;
    var semi = new int[size];
    var lanes = new int[2 * size];
    semidominators(graph, dfs, semi, lanes);
    lanes[0] = 0;
    lanes[1] = 0;
    for (int v = 1; v < size; v++) {
      int s = semi[v];
      int x = idom[v];
      while (x > s) {
        int jump = lanes[2 * x + 1];
        x = jump > s ? jump : idom[x];
      }
      idom[v] = x;
      int depth = lanes[2 * x];
      int jump = lanes[2 * x + 1];
      int jumpDepth = lanes[2 * jump];
      int jumpJump = lanes[2 * jump + 1];
      lanes[2 * v] = depth + 1;
      lanes[2 * v + 1] = depth - jumpDepth == jumpDepth - lanes[2 * jumpJump] ? jumpJump : x;
    }
    return toNodeIds(graph.nodeCount(), dfs, idom);
  }

  /**
   * Cooper, Harvey and Kennedy's iterative algorithm over reverse postorder, intersecting the
   * dominators of the processed predecessors until nothing changes. Quadratic on a fan behind a
   * chain, and needs extra passes on irreducible graphs.
   */
  public static int[] cooperHarveyKennedy(Graph graph, IntList entries) {
    int n = graph.nodeCount();
    var bi = graph.biAdjacentGraph();
    int[] inStart = bi.inAdjacencyStart;
    int[] in = bi.inAdjacency;
    int[] out = graph.outAdjacencyStart;
    int[] adj = graph.outAdjacency;

    // Postorder numbers 1..R; the virtual root above the entries gets R + 1.
    var post = new int[n];
    var rpo = new int[n];
    var visited = new boolean[n];
    var frames = new int[2 * Math.max(16, Math.min(n, 1024))];
    int count = 0;
    for (int e = 0; e < entries.size(); e++) {
      int entry = entries.getInt(e);
      if (visited[entry]) continue;
      visited[entry] = true;
      frames[0] = entry;
      frames[1] = out[entry];
      int top = 0;
      outer:
      while (top >= 0) {
        int u = frames[2 * top];
        int i = frames[2 * top + 1];
        while (i < out[u + 1]) {
          int v = adj[i++];
          if (!visited[v]) {
            visited[v] = true;
            frames[2 * top + 1] = i;
            if (2 * (++top + 1) > frames.length) frames = Arrays.copyOf(frames, 2 * frames.length);
            frames[2 * top] = v;
            frames[2 * top + 1] = out[v];
            continue outer;
          }
        }
        post[u] = ++count;
        rpo[n - count] = u;
        top--;
      }
    }
    int root = count + 1;
    var isEntry = new boolean[n];
    for (int e = 0; e < entries.size(); e++) isEntry[entries.getInt(e)] = true;

    // idom by postorder number; 0 = not yet known.
    var idom = new int[root + 1];
    var nodeOfPost = new int[root + 1];
    for (int v = 0; v < n; v++) {
      if (post[v] > 0) nodeOfPost[post[v]] = v;
    }
    idom[root] = root;
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int k = n - count; k < n; k++) {
        int b = rpo[k];
        int newIdom = isEntry[b] ? root : 0;
        for (int i = inStart[b], end = inStart[b + 1]; i < end; i++) {
          int p = post[in[i]];
          if (p == 0 || idom[p] == 0) continue;
          newIdom = newIdom == 0 ? p : intersect(idom, p, newIdom);
        }
        if (idom[post[b]] != newIdom) {
          idom[post[b]] = newIdom;
          changed = true;
        }
      }
    }
    var result = new int[n];
    Arrays.fill(result, -1);
    for (int v = 0; v < n; v++) {
      if (post[v] > 0 && idom[post[v]] != root) result[v] = nodeOfPost[idom[post[v]]];
    }
    return result;
  }

  private static int intersect(int[] idom, int a, int b) {
    while (a != b) {
      while (a < b) a = idom[a];
      while (b < a) b = idom[b];
    }
    return a;
  }

  private static void semidominators(Graph graph, DfsPreorder dfs, int[] semi, int[] lanes) {
    int size = semi.length;
    int[] parent = dfs.parent;
    for (int v = 0; v < size; v++) {
      semi[v] = v;
      lanes[2 * v] = UNLINKED;
      lanes[2 * v + 1] = v;
    }
    var predecessors = predecessors(graph, dfs);
    int[] start = predecessors.start();
    int[] nodes = predecessors.nodes();
    for (int v = size - 1; v >= 1; v--) {
      int best = v;
      for (int i = start[v], end = start[v + 1]; i < end; i++) {
        int u = nodes[i];
        int candidate = u < v ? u : semi[eval(lanes, semi, u)];
        if (candidate < best) best = candidate;
      }
      semi[v] = dfs.isEntry(v) ? 0 : best;
      lanes[2 * v] = parent[v];
    }
  }

  /**
   * Predecessor lists in preorder numbers, for the reached nodes only: the predecessors of the
   * node numbered {@code v} are {@code nodes[start[v]..start[v + 1])}, each list ascending.
   * <p>
   * <strong>Memory:</strong> 4 x (reachedCount + reached edges) bytes.
   */
  private static Predecessors predecessors(Graph graph, DfsPreorder dfs) {
    int[] out = graph.outAdjacencyStart;
    int[] adj = graph.outAdjacency;
    int[] order = dfs.order;
    int[] number = dfs.preorderNumber;
    int size = dfs.reachedCount + 1;
    var start = new int[size + 1];
    for (int u = 1; u < size; u++) {
      int node = order[u];
      for (int i = out[node]; i < out[node + 1]; i++) {
        start[number[adj[i]]]++;
      }
    }
    int next = 0;
    for (int v = 0; v < size; v++) {
      int c = start[v];
      start[v] = next;
      next += c;
    }
    start[size] = next;
    var nodes = new int[next];
    // Scanning sources in preorder leaves every list ascending; start[v] doubles as write
    // pointer and is shifted back afterwards.
    for (int u = 1; u < size; u++) {
      int node = order[u];
      for (int i = out[node]; i < out[node + 1]; i++) {
        nodes[start[number[adj[i]]]++] = u;
      }
    }
    for (int v = size; v > 0; v--) {
      start[v] = start[v - 1];
    }
    start[0] = 0;
    return new Predecessors(start, nodes);
  }

  private record Predecessors(int[] start, int[] nodes) {}

  private static int eval(int[] lanes, int[] semi, int v) {
    int ancestor = lanes[2 * v];
    if (ancestor == UNLINKED) return v;
    if (lanes[2 * ancestor] != UNLINKED) compress(lanes, semi, v);
    return lanes[2 * v + 1];
  }

  private static void compress(int[] lanes, int[] semi, int v) {
    int below = UNLINKED;
    int x = v;
    while (lanes[2 * lanes[2 * x]] != UNLINKED) {
      int up = lanes[2 * x];
      lanes[2 * x] = below;
      below = x;
      x = up;
    }
    while (below != UNLINKED) {
      int y = below;
      below = lanes[2 * y];
      int xLabel = lanes[2 * x + 1];
      if (semi[xLabel] < semi[lanes[2 * y + 1]]) lanes[2 * y + 1] = xLabel;
      lanes[2 * y] = lanes[2 * x];
      x = y;
    }
  }

  private static int[] toNodeIds(int n, DfsPreorder dfs, int[] idom) {
    var result = new int[n];
    Arrays.fill(result, -1);
    for (int v = 1; v <= dfs.reachedCount; v++) {
      if (idom[v] != 0) result[dfs.order[v]] = dfs.order[idom[v]];
    }
    return result;
  }

  /**
   * {@link IteratedDominanceFrontier} with the dominator tree as child lists instead of in
   * preorder: 12 instead of 16 bytes per node, but a subtree is walked through an explicit stack
   * that checks every child against the walked set.
   */
  public static final class ChildListFrontier {

    private final Graph graph;
    private final int[] depth;
    private final int[] childStart;
    private final int[] children;

    public ChildListFrontier(Dominators dominators) {
      this.graph = dominators.graph;
      int[] parent = dominators.immediateDominator;
      var forest = ParentForest.childrenOf(parent);
      childStart = forest.start();
      children = forest.nodes();
      int n = parent.length;
      depth = new int[n];
      Arrays.fill(depth, -1);
      var queue = new int[n];
      int head = 0;
      int tail = 0;
      for (int node = 0; node < n; node++) {
        if (parent[node] < 0 && dominators.isReachable(node)) {
          depth[node] = 0;
          queue[tail++] = node;
        }
      }
      while (head < tail) {
        int node = queue[head++];
        for (int i = childStart[node], end = childStart[node + 1]; i < end; i++) {
          depth[children[i]] = depth[node] + 1;
          queue[tail++] = children[i];
        }
      }
    }

    public int[] frontierOf(IntList nodes) {
      int size = nodes.size();
      var given = new IntOpenHashSet(size);
      var heap = new int[2 * Math.max(4, size)];
      int heapSize = 0;
      for (int i = 0; i < size; i++) {
        int node = nodes.getInt(i);
        if (depth[node] < 0 || !given.add(node)) continue;
        heap = push(heap, heapSize++, depth[node], node);
      }
      int[] out = graph.outAdjacencyStart;
      int[] adj = graph.outAdjacency;
      var inFrontier = new IntOpenHashSet();
      var walked = new IntOpenHashSet();
      var result = new IntArrayList();
      var stack = new IntArrayList();
      while (heapSize > 0) {
        int rootDepth = heap[0];
        int root = heap[1];
        heapSize = pop(heap, heapSize);
        walked.add(root);
        stack.add(root);
        while (!stack.isEmpty()) {
          int x = stack.popInt();
          for (int e = out[x], last = out[x + 1]; e < last; e++) {
            int y = adj[e];
            int yDepth = depth[y];
            if (yDepth > rootDepth || !inFrontier.add(y)) continue;
            result.add(y);
            if (!given.contains(y)) heap = push(heap, heapSize++, yDepth, y);
          }
          for (int c = childStart[x], end = childStart[x + 1]; c < end; c++) {
            if (walked.add(children[c])) stack.add(children[c]);
          }
        }
      }
      int[] sorted = result.toIntArray();
      Arrays.sort(sorted);
      return sorted;
    }

    /** Same pair heap as in {@link IteratedDominanceFrontier}. */
    private static int[] push(int[] heap, int size, int key, int node) {
      if (2 * (size + 1) > heap.length) heap = Arrays.copyOf(heap, 2 * heap.length);
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
}
