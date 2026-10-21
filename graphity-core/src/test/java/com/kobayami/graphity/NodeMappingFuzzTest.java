package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.RecursiveDfsOrder;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link NodeMapping#reindex(Graph)} against a plain recursive depth-first
 * search ({@link RecursiveDfsOrder}) as an independent oracle for the documented DFS-preorder semantics. The iterative reindex
 * traverser must agree node for node, not merely produce some permutation.
 */
class NodeMappingFuzzTest {

  private static final int ITERATIONS = 300;

  @Test
  void reindexOrderMatchesRecursivePreorder() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyGraph(seed);
      var g = labeled.graph();

      var recursive = new RecursiveDfsOrder(g);
      recursive.run();
      var nm = NodeMapping.reindex(g);

      int[] actual = new int[nm.size()];
      for (int newId = 0; newId < nm.size(); newId++) actual[newId] = nm.get(newId);

      assertThat(actual)
          .as("reindex must equal recursive DFS preorder for %s", labeled.describe())
          .isEqualTo(recursive.preOrder().toIntArray());
    }
  }
}
