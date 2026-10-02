package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Objects;

/**
 * The loop nesting forest of a control-flow graph, irreducible loops included.
 * <p>
 * A loop has a header, through which it is normally entered, and a body; it may sit inside
 * another loop. The forest is defined on every graph, reducible or not:
 * <ul>
 *   <li>The outermost loops are the strongly connected components that contain at least one
 *       edge.</li>
 *   <li>The header of a loop is the node of its component that a depth-first search from the
 *       graph's entries reaches first. Entries are searched in list order and successors in
 *       ascending order.</li>
 *   <li>Without the edges that lead into the header from inside the component, the rest of it
 *       breaks up into the nested loops, by the same rule.</li>
 * </ul>
 * This is Havlak's loop nesting forest. On a reducible graph its loops are exactly the natural
 * loops. An irreducible cycle - one that can be entered at more than one node, as {@code goto},
 * bytecode or generated state machines produce - appears as one loop with several
 * {@linkplain #entriesOf(int) entries}, and which of them is the header depends on the node
 * numbering. An entry of the graph that lies in a loop counts as an entry of that loop. A node
 * with a self-loop is a loop of its own; unreachable nodes are in no loop.
 * <p>
 * Loops have IDs in {@code 0..loopCount()-1}, numbered in preorder of the forest: an enclosing
 * loop has a smaller ID than the loops inside it, so iterating IDs downwards visits inner loops
 * first, and the loops nested in {@code L}, {@code L} included, occupy a contiguous ID range.
 * {@link #isInLoop(int, int)} is a range test on {@link #innermostLoopOf(int)}, and
 * {@link #nodesOf(int)} is a slice of one shared array.
 * <p>
 * Like {@link Dominators}, the analysis reads predecessors and takes a {@link BiAdjacentGraph};
 * the same graph serves both.
 *
 * <pre>{@code
 * var loops = Loops.of(cfg, entry);
 * for (int loop = loops.loopCount() - 1; loop >= 0; loop--) {   // inner loops first
 *     int header = loops.headerOf(loop);
 *     IntList body = loops.nodesOf(loop);                       // nested loops included
 * }
 * }</pre>
 *
 * <strong>Memory:</strong> 4 x nodeCount bytes, plus 4 bytes per node that lies in a loop,
 * 24 bytes per loop and 4 bytes per loop entry.
 *
 * @see Sccs
 * @see Dominators
 */
public final class Loops {

  /** Innermost loop of each node, {@code -1} outside every loop. */
  private final int[] innermostLoop;

  /** Header node of each loop. */
  private final int[] header;

  /** Enclosing loop of each loop, {@code -1} at the top level. */
  private final int[] parent;

  /** One past the last loop nested in each loop: the loops in {@code L} are {@code [L, end[L])}. */
  private final int[] subtreeEnd;

  /** Number of loops containing each loop, itself included. */
  private final int[] depth;

  /**
   * Start of each loop's own nodes in {@link #bodies}; the body of {@code L} with its nested
   * loops is {@code [bodyStart[L], bodyStart[subtreeEnd[L]])}.
   */
  private final int[] bodyStart;

  /** All loop bodies in loop-ID order, each loop's own nodes header first, then ascending. */
  private final IntImmutableList bodies;

  /** Entries of each loop as CSR offsets into {@link #entries}, each list ascending. */
  private final int[] entryStart;

  private final int[] entries;

  private final boolean reducible;

  private Loops(int[] innermostLoop, int[] header, int[] parent, int[] subtreeEnd, int[] depth,
      int[] bodyStart, int[] bodies, int[] entryStart, int[] entries, boolean reducible) {
    this.innermostLoop = innermostLoop;
    this.header = header;
    this.parent = parent;
    this.subtreeEnd = subtreeEnd;
    this.depth = depth;
    this.bodyStart = bodyStart;
    this.bodies = new IntImmutableList(bodies);
    this.entryStart = entryStart;
    this.entries = entries;
    this.reducible = reducible;
  }

  /**
   * Computes the loop nesting forest of a graph with a single entry.
   * <p>
   * Equivalent to {@link #of(BiAdjacentGraph, IntList)} with a one-element list.
   * <p>
   * <strong>Time:</strong> as for {@link #of(BiAdjacentGraph, IntList)}.<br>
   * <strong>Memory:</strong> as for {@link #of(BiAdjacentGraph, IntList)}.
   *
   * @param graph the control-flow graph
   * @param entry the entry node ID in {@code 0..nodeCount()-1}
   * @return the loop nesting forest
   * @throws IndexOutOfBoundsException if {@code entry} is out of range
   * @throws IllegalArgumentException if {@code graph.nodeCount() >= 2^30 - 2}
   */
  public static Loops of(BiAdjacentGraph graph, int entry) {
    return of(graph, IntLists.singleton(entry));
  }

  /**
   * Computes the loop nesting forest of a graph with the given entries.
   * <p>
   * Entries follow the conventions of {@link Dominators#of(BiAdjacentGraph, IntList)}: an empty
   * list leaves every node unreachable, duplicates are ignored. Their order matters here,
   * because it decides which node of an irreducible loop the search reaches first. The
   * predecessors are read from the in-adjacency of the graph. Construction is iterative:
   * Havlak's algorithm with union-find, keeping the edges that enter a collapsed loop in linked
   * lists instead of copying them to each enclosing header.
   * <p>
   * <strong>Time:</strong> O((nodeCount + edgeCount) x log nodeCount) on a reducible graph. On
   * an irreducible graph, an edge additionally costs one step for every loop it enters other
   * than through the header.<br>
   * <strong>Memory:</strong> as given in the class description for the result; about
   * 48 x nodeCount + 4 x edgeCount bytes additional during computation.
   *
   * @param graph   the control-flow graph
   * @param entries the entry node IDs, each in {@code 0..nodeCount()-1}
   * @return the loop nesting forest
   * @throws IndexOutOfBoundsException if an entry is out of range
   * @throws IllegalArgumentException if {@code graph.nodeCount() >= 2^30 - 2}
   */
  public static Loops of(BiAdjacentGraph graph, IntList entries) {
    Objects.requireNonNull(graph, "graph");
    Objects.requireNonNull(entries, "entries");
    int n = graph.nodeCount();
    var dfs = DfsPreorder.of(graph, entries, "Loops");
    int size = dfs.reachedCount + 1;
    int[] order = dfs.order;
    int[] number = dfs.preorderNumber;

    // 1) Last preorder number in each search subtree: v's subtree is [v, last[v]].
    var last = new int[size];
    for (int v = 0; v < size; v++) last[v] = v;
    for (int v = size - 1; v >= 1; v--) {
      int p = dfs.parent[v];
      if (p > 0 && last[v] > last[p]) last[p] = last[v];
    }

    // Predecessors from the in-adjacency over node IDs, translated per edge through `number`
    // (0 = unreachable).
    int[] predStart = graph.inAdjacencyStart;
    int[] preds = graph.inAdjacency;

    // 2) Headers in reverse preorder, inner before outer. The body of w is the part of its
    //    search subtree that reaches w inside that subtree. Union-find collapses finished loops
    //    into their header; the edges that enter a collapsed region from outside wait in a
    //    linked list per region, so an enclosing header scans each of them once instead of
    //    rescanning every predecessor of every node inside.
    var uf = new int[size];
    for (int v = 0; v < size; v++) uf[v] = v;
    var enclosing = new int[size];
    var mark = new int[size];
    var pendingHead = new int[size];
    var pendingTail = new int[size];
    Arrays.fill(pendingHead, -1);
    Arrays.fill(pendingTail, -1);
    var pendingNext = new int[preds.length];
    var stack = new int[size];
    var body = new int[size];
    var isHeader = new boolean[size];
    int headerCount = 0;

    for (int w = size - 1; w >= 1; w--) {
      int lastW = last[w];
      boolean selfLoop = false;
      int top = 0;
      int node = order[w];
      for (int i = predStart[node], end = predStart[node + 1]; i < end; i++) {
        int v = number[preds[i]];
        if (v == 0) continue;
        if (v >= w && v <= lastW) {
          if (v == w) {
            selfLoop = true;
          } else {
            int r = find(uf, v);
            if (mark[r] != w) {
              mark[r] = w;
              stack[top++] = r;
            }
          }
        } else {
          append(pendingHead, pendingTail, pendingNext, w, i);
        }
      }
      if (top == 0 && !selfLoop) continue;

      isHeader[w] = true;
      headerCount++;
      int bodyCount = 0;
      while (top > 0) {
        int x = stack[--top];
        body[bodyCount++] = x;
        int i = pendingHead[x];
        pendingHead[x] = -1;
        pendingTail[x] = -1;
        while (i >= 0) {
          int next = pendingNext[i];
          int r = find(uf, number[preds[i]]);
          if (r >= w && r <= lastW) {
            if (r != w && mark[r] != w) {
              mark[r] = w;
              stack[top++] = r;
            }
          } else {
            append(pendingHead, pendingTail, pendingNext, w, i);
          }
          i = next;
        }
      }
      for (int b = 0; b < bodyCount; b++) {
        uf[body[b]] = w;
        enclosing[body[b]] = w;
      }
    }

    // 3) Loop IDs in preorder of the forest; siblings by the preorder of their headers.
    int loopCount = headerCount;
    var loopIndex = mark;
    Arrays.fill(loopIndex, -1);
    var headers = new int[loopCount];
    int k = 0;
    for (int v = 1; v < size; v++) {
      if (isHeader[v]) {
        loopIndex[v] = k;
        headers[k++] = v;
      }
    }
    var childStart = new int[loopCount + 1];
    var indexParent = new int[loopCount];
    for (int i = 0; i < loopCount; i++) {
      int e = enclosing[headers[i]];
      indexParent[i] = e == 0 ? -1 : loopIndex[e];
      if (indexParent[i] >= 0) childStart[indexParent[i]]++;
    }
    int next = 0;
    for (int i = 0; i < loopCount; i++) {
      int count = childStart[i];
      childStart[i] = next;
      next += count;
    }
    childStart[loopCount] = next;
    var children = new int[next];
    for (int i = 0; i < loopCount; i++) {
      if (indexParent[i] >= 0) children[childStart[indexParent[i]]++] = i;
    }
    for (int i = loopCount; i > 0; i--) childStart[i] = childStart[i - 1];
    childStart[0] = 0;

    var idOfIndex = new int[loopCount];
    var header = new int[loopCount];
    var parent = new int[loopCount];
    var depth = new int[loopCount];
    var subtreeEnd = new int[loopCount];
    int id = 0;
    int top = 0;
    for (int i = loopCount - 1; i >= 0; i--) {
      if (indexParent[i] < 0) stack[top++] = i;
    }
    while (top > 0) {
      int i = stack[--top];
      idOfIndex[i] = id;
      header[id] = order[headers[i]];
      parent[id] = indexParent[i] < 0 ? -1 : idOfIndex[indexParent[i]];
      depth[id] = parent[id] < 0 ? 1 : depth[parent[id]] + 1;
      id++;
      for (int c = childStart[i + 1] - 1; c >= childStart[i]; c--) stack[top++] = children[c];
    }
    for (int l = 0; l < loopCount; l++) subtreeEnd[l] = l + 1;
    for (int l = loopCount - 1; l >= 0; l--) {
      if (parent[l] >= 0 && subtreeEnd[l] > subtreeEnd[parent[l]]) {
        subtreeEnd[parent[l]] = subtreeEnd[l];
      }
    }

    // 4) Innermost loop per node: its own loop for a header, else the loop that collapsed it.
    var innermostLoop = new int[n];
    Arrays.fill(innermostLoop, -1);
    for (int v = 1; v < size; v++) {
      int h = isHeader[v] ? v : enclosing[v];
      if (h != 0) innermostLoop[order[v]] = idOfIndex[loopIndex[h]];
    }

    // 5) Bodies: each loop's own nodes, header first, then ascending; loops in ID order, so the
    //    nested loops of L follow its own nodes and the whole body is one slice.
    var bodyStart = new int[loopCount + 1];
    for (int node = 0; node < n; node++) {
      if (innermostLoop[node] >= 0) bodyStart[innermostLoop[node]]++;
    }
    next = 0;
    for (int l = 0; l < loopCount; l++) {
      int count = bodyStart[l];
      bodyStart[l] = next;
      next += count;
    }
    bodyStart[loopCount] = next;
    var bodies = new int[next];
    var write = Arrays.copyOf(bodyStart, loopCount);
    for (int l = 0; l < loopCount; l++) bodies[write[l]++] = header[l];
    for (int node = 0; node < n; node++) {
      int l = innermostLoop[node];
      if (l >= 0 && header[l] != node) bodies[write[l]++] = node;
    }

    // 6) Entries: x enters every loop that contains x but not one of its predecessors - a chain
    //    from the innermost loop of x upwards. Walking each chain only above the part already
    //    marked keeps this proportional to the result. A graph entry has the virtual root as
    //    predecessor, which no loop contains.
    var entryLoops = new IntArrayList();
    var entryNodes = new IntArrayList();
    for (int x = 0; x < n; x++) {
      int lx = innermostLoop[x];
      if (lx < 0) continue;
      int v = number[x];
      int marked = -1;
      if (dfs.isEntry(v)) {
        for (int l = lx; l >= 0; l = parent[l]) {
          entryLoops.add(l);
          entryNodes.add(x);
        }
        continue;
      }
      for (int i = predStart[x], end = predStart[x + 1]; i < end; i++) {
        int y = preds[i];
        if (number[y] == 0) continue;
        int ly = innermostLoop[y];
        int l;
        if (marked < 0) {
          l = lx;
        } else if (contains(subtreeEnd, marked, ly)) {
          continue;
        } else {
          l = parent[marked];
        }
        while (l >= 0 && !contains(subtreeEnd, l, ly)) {
          entryLoops.add(l);
          entryNodes.add(x);
          marked = l;
          l = parent[l];
        }
      }
    }
    var entryStart = new int[loopCount + 1];
    for (int i = 0; i < entryLoops.size(); i++) entryStart[entryLoops.getInt(i)]++;
    next = 0;
    boolean reducible = true;
    for (int l = 0; l < loopCount; l++) {
      int count = entryStart[l];
      if (count != 1) reducible = false;
      entryStart[l] = next;
      next += count;
    }
    entryStart[loopCount] = next;
    var loopEntries = new int[next];
    write = Arrays.copyOf(entryStart, loopCount);
    for (int i = 0; i < entryLoops.size(); i++) {
      loopEntries[write[entryLoops.getInt(i)]++] = entryNodes.getInt(i);
    }

    return new Loops(innermostLoop, header, parent, subtreeEnd, depth, bodyStart, bodies,
        entryStart, loopEntries, reducible);
  }

  /** Whether the loop {@code inner}, {@code -1} for none, is {@code loop} or nested in it. */
  private static boolean contains(int[] subtreeEnd, int loop, int inner) {
    return inner >= loop && inner < subtreeEnd[loop];
  }

  private static int find(int[] uf, int x) {
    int root = x;
    while (uf[root] != root) root = uf[root];
    while (uf[x] != root) {
      int up = uf[x];
      uf[x] = root;
      x = up;
    }
    return root;
  }

  private static void append(int[] head, int[] tail, int[] next, int list, int item) {
    next[item] = -1;
    if (tail[list] < 0) {
      head[list] = item;
    } else {
      next[tail[list]] = item;
    }
    tail[list] = item;
  }

  /**
   * Returns the number of loops.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return number of loops
   */
  public int loopCount() {
    return header.length;
  }

  /**
   * Returns whether every loop has exactly one entry, its header. Then the graph is reducible
   * and the loops are its natural loops.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if no loop can be entered other than through its header
   */
  public boolean isReducible() {
    return reducible;
  }

  /**
   * Returns the innermost loop containing {@code node}, or {@code -1} if it lies in no loop.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return loop ID, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int innermostLoopOf(int node) {
    Objects.checkIndex(node, innermostLoop.length);
    return innermostLoop[node];
  }

  /**
   * Returns the number of loops containing {@code node}, {@code 0} outside every loop.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @return nesting depth
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int loopDepthOf(int node) {
    Objects.checkIndex(node, innermostLoop.length);
    int loop = innermostLoop[node];
    return loop < 0 ? 0 : depth[loop];
  }

  /**
   * Returns whether {@code node} lies in {@code loop}, including its nested loops.
   * <p>
   * <strong>Time:</strong> O(1) - a range test on loop IDs.
   *
   * @param node node ID in {@code 0..nodeCount()-1}
   * @param loop loop ID in {@code 0..loopCount()-1}
   * @return {@code true} if the node is in the loop
   * @throws IndexOutOfBoundsException if {@code node} or {@code loop} is out of range
   */
  public boolean isInLoop(int node, int loop) {
    Objects.checkIndex(node, innermostLoop.length);
    Objects.checkIndex(loop, header.length);
    return contains(subtreeEnd, loop, innermostLoop[node]);
  }

  /**
   * Returns the header of {@code loop}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param loop loop ID in {@code 0..loopCount()-1}
   * @return header node ID
   * @throws IndexOutOfBoundsException if {@code loop} is out of range
   */
  public int headerOf(int loop) {
    Objects.checkIndex(loop, header.length);
    return header[loop];
  }

  /**
   * Returns the entries of {@code loop}: the nodes of the loop with a reachable predecessor
   * outside it, and the entries of the graph that lie in it. Exactly {@code {headerOf(loop)}}
   * for a loop that can only be entered through its header.
   * <p>
   * The list is a zero-copy, read-only view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param loop loop ID in {@code 0..loopCount()-1}
   * @return the entry nodes, sorted ascending
   * @throws IndexOutOfBoundsException if {@code loop} is out of range
   */
  public SortedIntView entriesOf(int loop) {
    Objects.checkIndex(loop, header.length);
    return SortedIntView.viewOf(entries, entryStart[loop], entryStart[loop + 1]);
  }

  /**
   * Returns the loop that immediately encloses {@code loop}, or {@code -1} at the top level.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param loop loop ID in {@code 0..loopCount()-1}
   * @return enclosing loop ID, smaller than {@code loop}, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code loop} is out of range
   */
  public int parentOf(int loop) {
    Objects.checkIndex(loop, header.length);
    return parent[loop];
  }

  /**
   * Returns the body of {@code loop}, nested loops included: the header first, then the nodes
   * whose innermost loop is {@code loop} in ascending order, then the bodies of the nested loops
   * in loop-ID order.
   * <p>
   * The list is a zero-copy, read-only view. All bodies share one array, so they cost 4 bytes
   * per node in a loop, not one copy per nesting level.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param loop loop ID in {@code 0..loopCount()-1}
   * @return the nodes of the loop
   * @throws IndexOutOfBoundsException if {@code loop} is out of range
   */
  public IntList nodesOf(int loop) {
    Objects.checkIndex(loop, header.length);
    return bodies.subList(bodyStart[loop], bodyStart[subtreeEnd[loop]]);
  }
}
