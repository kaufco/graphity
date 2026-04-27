package com.kobayami.graphity.benchmarks.guava;

import com.google.common.graph.Graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Direct port of Graphity's Tarjan-SCC algorithm
 * ({@code com.kobayami.graphity.Components.TarjanTraverser}) onto Guava's
 * {@link Graph} data model.
 *
 * <p>Algorithmically identical to Graphity: recursive DFS, index / lowlink bookkeeping,
 * single-pass SCC extraction from a stack. The difference lies <em>only</em> in the
 * data structures used by the algorithm:
 * <ul>
 *   <li>Graphity: primitive {@code int[]} for index/lowlink, {@link java.util.BitSet} for onStack,
 *       direct CSR array access for adjacency.</li>
 *   <li>This port: {@link HashMap}&lt;Integer,Integer&gt; for index/lowlink,
 *       {@link HashSet}&lt;Integer&gt; for onStack, Guava's {@link Graph#successors} for adjacency.</li>
 * </ul>
 * The resulting performance gap reflects the cost of Guava's object-based graph model,
 * not a weaker algorithm — a user who picked Guava and wrote their own SCC would end
 * up with code of this shape.
 */
public final class GuavaTarjanScc {

  private final Graph<Integer> graph;
  private final Map<Integer, Integer> index = new HashMap<>();
  private final Map<Integer, Integer> lowlink = new HashMap<>();
  private final Deque<Integer> stack = new ArrayDeque<>();
  private final Set<Integer> onStack = new HashSet<>();
  private final Map<Integer, Integer> nodeToScc = new HashMap<>();
  private int nextIndex;
  private int sccCount;

  public GuavaTarjanScc(Graph<Integer> graph) {
    this.graph = graph;
  }

  /** @return a map from node id to SCC id; caller gets SCC count via {@link #sccCount()}. */
  public Map<Integer, Integer> compute() {
    for (Integer v : graph.nodes()) {
      if (!index.containsKey(v)) {
        strongconnect(v);
      }
    }
    return nodeToScc;
  }

  public int sccCount() {
    return sccCount;
  }

  private void strongconnect(Integer v) {
    int idx = nextIndex++;
    index.put(v, idx);
    lowlink.put(v, idx);
    stack.push(v);
    onStack.add(v);

    for (Integer w : graph.successors(v)) {
      Integer wIdx = index.get(w);
      if (wIdx == null) {
        strongconnect(w);
        lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
      } else if (onStack.contains(w)) {
        lowlink.put(v, Math.min(lowlink.get(v), wIdx));
      }
    }

    if (lowlink.get(v).intValue() == index.get(v).intValue()) {
      Integer w;
      do {
        w = stack.pop();
        onStack.remove(w);
        nodeToScc.put(w, sccCount);
      } while (!w.equals(v));
      sccCount++;
    }
  }
}
