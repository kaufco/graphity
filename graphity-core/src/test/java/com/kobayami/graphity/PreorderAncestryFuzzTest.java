package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.RandomGraphs;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link PreorderAncestry} against {@link Ancestry}, whose ancestor test goes
 * through a lowest-common-ancestor query instead of preorder ranges, and against a recursive
 * preorder walk of the forest itself.
 */
class PreorderAncestryFuzzTest {

  private static final int ITERATIONS = 60;
  private static final int QUERIES_PER_GRAPH = 300;

  @Test
  void isAncestorOfMatchesTheLowestCommonAncestorTest() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyForest(seed);
      int n = labeled.graph().nodeCount();
      var ancestry = Ancestry.of(labeled.graph());
      var preorder = PreorderAncestry.of(ancestry);

      var rnd = new Random(seed * 31 + 4);
      for (int q = 0; q < QUERIES_PER_GRAPH; q++) {
        int node = rnd.nextInt(n);
        int descendant = rnd.nextBoolean() ? rnd.nextInt(n) : someDescendant(ancestry, node, rnd);
        assertThat(preorder.isAncestorOf(node, descendant))
            .as("isAncestorOf(%d, %d) for %s", node, descendant, labeled.describe())
            .isEqualTo(ancestry.lowestCommonAncestorOf(node, descendant) == node);
      }
    }
  }

  @Test
  void preorderAndSubtreesMatchAWalkOfTheForest() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyForest(seed);
      var forest = labeled.graph();
      int n = forest.nodeCount();
      var ancestry = Ancestry.of(forest);
      var preorder = PreorderAncestry.of(ancestry);

      var expected = new IntArrayList(n);
      for (int node = 0; node < n; node++) {
        if (ancestry.parentOf(node) < 0) walk(forest, node, expected);
      }
      assertThat(preorder.preorder().toIntArray())
          .as("preorder for %s", labeled.describe())
          .isEqualTo(expected.toIntArray());

      for (int node = 0; node < n; node++) {
        var subtree = new IntArrayList();
        walk(forest, node, subtree);
        assertThat(preorder.subtreeOf(node).toIntArray())
            .as("subtree of %d for %s", node, labeled.describe())
            .isEqualTo(subtree.toIntArray());
        assertThat(preorder.subtreeSizeOf(node)).isEqualTo(subtree.size());
        assertThat(preorder.preorder().getInt(preorder.preorderIndexOf(node))).isEqualTo(node);
      }
    }
  }

  /** Preorder walk over the forest's own adjacency, children in their stored ascending order. */
  private static void walk(Graph forest, int root, IntArrayList out) {
    var stack = new IntArrayList();
    stack.add(root);
    while (!stack.isEmpty()) {
      int node = stack.popInt();
      out.add(node);
      var children = forest.outNodes(node);
      for (int i = children.size() - 1; i >= 0; i--) {
        stack.add(children.getInt(i));
      }
    }
  }

  /** A random node on the path from {@code node} down, found by walking up from random nodes. */
  private static int someDescendant(Ancestry ancestry, int node, Random rnd) {
    int n = ancestry.parent.length;
    for (int attempt = 0; attempt < 20; attempt++) {
      int candidate = rnd.nextInt(n);
      for (int x = candidate; x >= 0; x = ancestry.parentOf(x)) {
        if (x == node) return candidate;
      }
    }
    return node;
  }
}
