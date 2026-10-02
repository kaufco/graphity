package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.NodeMapping;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle for the loop nesting forest, taken literally from its definition by strongly connected
 * components: the outermost loops are the components with an edge, the header of each is the
 * node that a depth-first search from the entries reaches first, and the nested loops are the
 * components of the rest once the header is taken out. Recursive over the nesting and
 * O(nodeCount x (nodeCount + edgeCount)); meant for graphs of a few hundred nodes.
 */
public final class NaiveLoops {

  /** One loop: its header, its body with nested loops, and the header of its enclosing loop. */
  public record Loop(int header, boolean[] body, int parentHeader) {}

  /** Loops keyed by header, {@code null} for a node that heads none. */
  public final Loop[] loopOfHeader;

  public final boolean[] reachable;

  /** Preorder position of each node in the search, {@code -1} if unreachable. */
  public final int[] preorder;

  private final Graph graph;
  private final IntList entries;

  public NaiveLoops(Graph graph, IntList entries) {
    this.graph = graph;
    this.entries = entries;
    int n = graph.nodeCount();
    preorder = searchOrder(graph, entries);
    reachable = new boolean[n];
    var all = new IntArrayList();
    for (int v = 0; v < n; v++) {
      if (preorder[v] >= 0) {
        reachable[v] = true;
        all.add(v);
      }
    }
    loopOfHeader = new Loop[n];
    decompose(all, -1);
  }

  /** Records the loops among {@code region}, all inside the loop headed by {@code parentHeader}. */
  private void decompose(IntList region, int parentHeader) {
    var mapping = NodeMapping.forSubgraph(region);
    var sub = Graphs.remapped(graph, mapping);
    var sccs = Components.sccsOf(sub);
    for (int c = 0; c < sccs.groupCount(); c++) {
      if (!sccs.containsCycle(c)) continue;
      var members = sccs.nodesOf(c);
      int header = -1;
      var body = new boolean[graph.nodeCount()];
      for (int i = 0; i < members.size(); i++) {
        int node = mapping.get(members.getInt(i));
        body[node] = true;
        if (header < 0 || preorder[node] < preorder[header]) header = node;
      }
      loopOfHeader[header] = new Loop(header, body, parentHeader);
      var rest = new IntArrayList();
      for (int i = 0; i < members.size(); i++) {
        int node = mapping.get(members.getInt(i));
        if (node != header) rest.add(node);
      }
      rest.sort(null);
      decompose(rest, header);
    }
  }

  /**
   * The entries of a loop: its nodes with a reachable predecessor outside it, and the entries of
   * the graph inside it.
   */
  public List<Integer> entriesOf(Loop loop) {
    int n = graph.nodeCount();
    var isGraphEntry = new boolean[n];
    for (int i = 0; i < entries.size(); i++) isGraphEntry[entries.getInt(i)] = true;
    var result = new ArrayList<Integer>();
    for (int x = 0; x < n; x++) {
      if (!loop.body()[x]) continue;
      boolean entered = isGraphEntry[x];
      for (int p = 0; p < n && !entered; p++) {
        if (reachable[p] && !loop.body()[p] && graph.hasOutNode(p, x)) entered = true;
      }
      if (entered) result.add(x);
    }
    return result;
  }

  /** Preorder of a depth-first search from the entries in list order, successors ascending. */
  private static int[] searchOrder(Graph graph, IntList entries) {
    int n = graph.nodeCount();
    var preorder = new int[n];
    java.util.Arrays.fill(preorder, -1);
    var nodeStack = new IntArrayList();
    var cursorStack = new IntArrayList();
    int count = 0;
    for (int e = 0; e < entries.size(); e++) {
      int entry = entries.getInt(e);
      if (preorder[entry] >= 0) continue;
      preorder[entry] = count++;
      nodeStack.add(entry);
      cursorStack.add(0);
      while (!nodeStack.isEmpty()) {
        int top = nodeStack.size() - 1;
        int u = nodeStack.getInt(top);
        int cursor = cursorStack.getInt(top);
        var outs = graph.outNodes(u);
        if (cursor == outs.size()) {
          nodeStack.removeInt(top);
          cursorStack.removeInt(top);
          continue;
        }
        cursorStack.set(top, cursor + 1);
        int v = outs.getInt(cursor);
        if (preorder[v] < 0) {
          preorder[v] = count++;
          nodeStack.add(v);
          cursorStack.add(0);
        }
      }
    }
    return preorder;
  }
}
