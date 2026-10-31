package com.kobayami.graphity.controlflow;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Csr;
import com.kobayami.graphity.Dominators;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.SortedIntView;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.Arrays;
import java.util.Objects;

/**
 * The loop nesting forest of a control-flow graph: its loops, how they nest and where each of
 * them is entered, irreducible loops included.
 * <p>
 * A loop has a header, through which it is normally entered, and a body: the header, the other
 * nodes of the loop and those of the loops nested in it. {@link #of(BiAdjacentGraph, int)} finds
 * the loops from the entry of the graph - here, the loop of a method {@code load} around its
 * block {@code then}, from its control flow {@code cfg} and its entry block {@code entry}:
 * {@snippet file="com/kobayami/graphity/examples/AnalyzingControlFlow.java" region="loops"}
 * The forest is defined on every graph, reducible or not, over the nodes that the entries reach:
 * <ul>
 *   <li>The outermost loops are the {@linkplain Sccs strongly connected components} that
 *       contain at least one edge.</li>
 *   <li>The header of a loop is the node of its component that a depth-first search from the
 *       entries reaches first. The search takes the entries in list order and the out-nodes of a
 *       node in ascending order.</li>
 *   <li>Without the edges that lead into the header from inside the component, the rest of it
 *       breaks up into the nested loops, by the same rule.</li>
 * </ul>
 * <p>
 * Uses Havlak's algorithm, without recursion. On a reducible graph, the loops are the natural
 * loops. An irreducible loop is a cycle that can be entered at more than one node, as
 * {@code goto}, bytecode or generated state machines produce it. It is one loop with several
 * {@linkplain #entriesOf(int) entries}, and which of them is the header depends on the order of
 * the search. An entry of the graph that lies in a loop is an entry of that loop. A node with a
 * self-loop is the header of a loop of its own, unless it already is the header of a larger one.
 * Nodes that no entry reaches are in no loop.
 * <p>
 * Loops have IDs {@code 0..loopCount()-1}, in preorder of the forest: a loop has a smaller ID than
 * the loops nested in it, which follow it as one range of IDs. Going through the IDs downwards
 * visits the inner loops first.
 * <p>
 * Like {@link Dominators}, the analysis follows edges against their direction and so takes a
 * {@link BiAdjacentGraph}; the same graph serves both. Instances are immutable and safe to share
 * between threads.
 * <p>
 * <strong>Memory:</strong> 4 x nodeCount bytes, plus 4 bytes per node in a loop, 24 bytes per
 * loop and 4 bytes per entry of a loop.
 */
public final class Loops {

  // Innermost loop of each node, -1 outside every loop.
  private final int[] innermostLoop;

  // Header node of each loop.
  private final int[] header;

  // Enclosing loop of each loop, -1 for an outermost loop.
  private final int[] parent;

  // One past the last loop nested in each loop: the loops in L are [L, subtreeEnd[L]).
  private final int[] subtreeEnd;

  // Number of loops containing each loop, itself included.
  private final int[] depth;

  // Start of each loop's own nodes in bodies; the body of L with its nested loops is
  // [bodyStart[L], bodyStart[subtreeEnd[L]]).
  private final int[] bodyStart;

  // All loop bodies in loop ID order, each loop's own nodes header first, then ascending.
  private final IntImmutableList bodies;

  // The entries of each loop in CSR layout: those of L are
  // entries[entryStart[L]..entryStart[L + 1] - 1], ascending.
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
   * Returns the loop nesting forest of {@code graph} with one entry. Equivalent to
   * {@link #of(BiAdjacentGraph, IntList) of(graph, IntList.of(entry))}.
   * <p>
   * <strong>Time:</strong> as for {@link #of(BiAdjacentGraph, IntList)}.<br>
   * <strong>Memory:</strong> as for {@link #of(BiAdjacentGraph, IntList)}.
   *
   * @param graph the control-flow graph
   * @param entry node ID in {@code 0..nodeCount-1}
   * @return the loop nesting forest
   * @throws IndexOutOfBoundsException if {@code entry} is out of range
   * @throws IllegalArgumentException if {@code graph} has more than 2<sup>30</sup> - 6 nodes
   */
  public static Loops of(BiAdjacentGraph graph, int entry) {
    return of(graph, IntLists.singleton(entry));
  }

  /**
   * Returns the loop nesting forest of {@code graph} with the given entries.
   * <p>
   * The entries work as in {@link Dominators#of(BiAdjacentGraph, IntList)}: an entry given twice
   * counts once, and with no entries, no node is reachable, so there is no loop. Their order
   * matters here: it decides which node of an irreducible loop the search reaches first, and so
   * which node is its header.
   * <p>
   * <strong>Time:</strong> O(entries.size() + (nodeCount + edgeCount) log nodeCount) on a reducible
   * graph. On an irreducible graph, an edge costs one more step for every loop it enters other
   * than through the header.<br>
   * <strong>Memory:</strong> the result, as given in the class documentation; while it runs,
   * about 45 x nodeCount + 4 x edgeCount bytes more, plus 28 bytes per loop and 12 bytes per
   * entry of a loop.
   *
   * @param graph   the control-flow graph
   * @param entries node IDs in {@code 0..nodeCount-1}, in the order the search takes them
   * @return the loop nesting forest
   * @throws IndexOutOfBoundsException if an entry is out of range
   * @throws IllegalArgumentException if {@code graph} has more than 2<sup>30</sup> - 6 nodes
   */
  public static Loops of(BiAdjacentGraph graph, IntList entries) {
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

    // In-nodes from the in-adjacency over node IDs, translated per edge through `number`
    // (0 = unreachable).
    int[] predStart = Csr.inAdjacencyStart(graph);
    int[] preds = Csr.inAdjacency(graph);

    // 2) Headers in reverse preorder, inner before outer. The body of w is the part of its
    //    search subtree that reaches w inside that subtree. Union-find collapses finished loops
    //    into their header; the edges that enter a collapsed region from outside wait in a
    //    linked list per region, so an enclosing header scans each of them once instead of
    //    rescanning every in-node of every node inside.
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
    int loopCount = 0;

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
      loopCount++;
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
    // The child lists and the walk are ParentForest.groupCounted and ParentForest.preorder of the
    // core, which a module cannot call; change them together.
    // mark and stack are free now: mark maps each header to its index, stack walks the forest.
    var loopIndex = mark;
    Arrays.fill(loopIndex, -1);
    var indexHeader = new int[loopCount];
    int k = 0;
    for (int v = 1; v < size; v++) {
      if (isHeader[v]) {
        loopIndex[v] = k;
        indexHeader[k++] = v;
      }
    }
    var childStart = new int[loopCount + 1];
    var indexParent = new int[loopCount];
    for (int i = 0; i < loopCount; i++) {
      int e = enclosing[indexHeader[i]];
      indexParent[i] = e == 0 ? -1 : loopIndex[e];
      if (indexParent[i] >= 0) childStart[indexParent[i]]++;
    }
    int nextChildStart = 0;
    for (int i = 0; i < loopCount; i++) {
      int count = childStart[i];
      childStart[i] = nextChildStart;
      nextChildStart += count;
    }
    childStart[loopCount] = nextChildStart;
    var children = new int[nextChildStart];
    // childStart[p] doubles as write pointer and is shifted back afterwards.
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
      header[id] = order[indexHeader[i]];
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
    int nextBodyStart = 0;
    for (int l = 0; l < loopCount; l++) {
      int count = bodyStart[l];
      bodyStart[l] = nextBodyStart;
      nextBodyStart += count;
    }
    bodyStart[loopCount] = nextBodyStart;
    var bodies = new int[nextBodyStart];
    var bodyWritePos = Arrays.copyOf(bodyStart, loopCount);
    for (int l = 0; l < loopCount; l++) bodies[bodyWritePos[l]++] = header[l];
    for (int node = 0; node < n; node++) {
      int l = innermostLoop[node];
      if (l >= 0 && header[l] != node) bodies[bodyWritePos[l]++] = node;
    }

    // 6) Entries: x enters every loop that contains x but not one of its in-nodes - a chain from
    //    the innermost loop of x upwards. Walking each chain only above the part already marked
    //    keeps this proportional to the result. A graph entry has the virtual root as in-node,
    //    which no loop contains.
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
    int nextEntryStart = 0;
    boolean reducible = true;
    for (int l = 0; l < loopCount; l++) {
      int count = entryStart[l];
      if (count != 1) reducible = false;
      entryStart[l] = nextEntryStart;
      nextEntryStart += count;
    }
    entryStart[loopCount] = nextEntryStart;
    var loopEntries = new int[nextEntryStart];
    var entryWritePos = Arrays.copyOf(entryStart, loopCount);
    for (int i = 0; i < entryLoops.size(); i++) {
      loopEntries[entryWritePos[entryLoops.getInt(i)]++] = entryNodes.getInt(i);
    }

    return new Loops(innermostLoop, header, parent, subtreeEnd, depth, bodyStart, bodies,
        entryStart, loopEntries, reducible);
  }

  // Whether the loop inner, -1 for none, is loop or nested in it.
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
   * @return loop count
   */
  public int loopCount() {
    return header.length;
  }

  /**
   * Returns whether the graph is reducible: whether every loop has its header as its only entry.
   * <p>
   * The loops of a reducible graph are its natural loops. A node counts only if the entries reach
   * it.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return {@code true} if no loop can be entered other than through its header
   */
  public boolean isReducible() {
    return reducible;
  }

  /**
   * Returns the innermost loop that contains {@code node}, or {@code -1} if it lies in no loop.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return loop ID, or {@code -1}
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int innermostLoopOf(int node) {
    Objects.checkIndex(node, innermostLoop.length);
    return innermostLoop[node];
  }

  /**
   * Returns the loop depth of {@code node}: the number of loops that contain it. A node in no loop
   * has loop depth {@code 0}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
   * @return loop depth
   * @throws IndexOutOfBoundsException if {@code node} is out of range
   */
  public int loopDepthOf(int node) {
    Objects.checkIndex(node, innermostLoop.length);
    int loop = innermostLoop[node];
    return loop < 0 ? 0 : depth[loop];
  }

  /**
   * Returns whether {@code node} lies in {@code loop}, directly or in a loop nested in it.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param node node ID in {@code 0..nodeCount-1}
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
   * Returns the entries of {@code loop}, in ascending order.
   * <p>
   * These are the nodes of the loop with an in-node outside the loop, and the entries of the graph
   * that lie in the loop. An in-node counts only if the entries of the graph reach it. A loop that
   * can only be entered through its header has the header as its only entry.
   * <p>
   * The result is a view of this forest's own array, not a copy.
   * <p>
   * <strong>Time:</strong> O(entriesOf(loop).size()).
   *
   * @param loop loop ID in {@code 0..loopCount()-1}
   * @return entry node IDs
   * @throws IndexOutOfBoundsException if {@code loop} is out of range
   */
  public SortedIntView entriesOf(int loop) {
    Objects.checkIndex(loop, header.length);
    return SortedIntView.viewOf(entries, entryStart[loop], entryStart[loop + 1]);
  }

  /**
   * Returns the loop that immediately encloses {@code loop}, or {@code -1} for an outermost loop.
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
   * Returns the body of {@code loop}: its nodes and those of the loops nested in it.
   * <p>
   * The nodes come in this order: first the header, then, in ascending order, the other nodes whose
   * innermost loop is {@code loop}, then the nodes of the nested loops in the same form, loop after
   * loop in ID order.
   * <p>
   * The result is an unmodifiable view of this forest's own array, not a copy. All bodies share
   * the array, so a node is stored once, however deeply its loop is nested.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param loop loop ID in {@code 0..loopCount()-1}
   * @return node IDs of the body
   * @throws IndexOutOfBoundsException if {@code loop} is out of range
   */
  public IntList bodyOf(int loop) {
    Objects.checkIndex(loop, header.length);
    return bodies.subList(bodyStart[loop], bodyStart[subtreeEnd[loop]]);
  }
}
