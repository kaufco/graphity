package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.testutils.JGraphtAdapter;
import com.kobayami.graphity.testutils.RandomGraphs;
import java.util.Random;
import org.jgrapht.alg.lca.NaiveLCAFinder;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates {@link Ancestry} against two independent oracles. JGraphT's
 * {@code NaiveLCAFinder} solves the same problem by intersecting ancestor sets instead of
 * decomposing the tree into heavy paths, and a parent walk decides the ancestor relation
 * without any preprocessing at all.
 */
class AncestryFuzzTest {

  private static final int ITERATIONS = 60;
  private static final int QUERIES_PER_GRAPH = 200;

  @Test
  void lowestCommonAncestorMatchesJgrapht() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyForest(seed);
      var forest = labeled.graph();
      int n = forest.nodeCount();

      var ancestry = Ancestry.of(forest);
      var oracle = new NaiveLCAFinder<Integer, org.jgrapht.graph.DefaultEdge>(
          JGraphtAdapter.toJgrapht(forest));

      var rnd = new Random(seed * 31 + 1);
      for (int q = 0; q < QUERIES_PER_GRAPH; q++) {
        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n);
        Integer expected = oracle.getLCA(a, b);
        assertThat(ancestry.lowestCommonAncestorOf(a, b))
            .as("lca(%d, %d) for %s", a, b, labeled.describe())
            .isEqualTo(expected == null ? -1 : expected.intValue());
      }
    }
  }

  @Test
  void lowestCommonAncestorIsSymmetricAndReflexive() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyForest(seed);
      var forest = labeled.graph();
      int n = forest.nodeCount();
      var ancestry = Ancestry.of(forest);

      var rnd = new Random(seed * 31 + 2);
      for (int q = 0; q < QUERIES_PER_GRAPH; q++) {
        int a = rnd.nextInt(n);
        int b = rnd.nextInt(n);
        assertThat(ancestry.lowestCommonAncestorOf(a, b))
            .as("symmetry of lca(%d, %d) for %s", a, b, labeled.describe())
            .isEqualTo(ancestry.lowestCommonAncestorOf(b, a));
        assertThat(ancestry.lowestCommonAncestorOf(a, a))
            .as("reflexivity at %d for %s", a, labeled.describe())
            .isEqualTo(a);
        int root = rootOf(ancestry, a);
        assertThat(ancestry.lowestCommonAncestorOf(root, a))
            .as("lca of %d with its root %d for %s", a, root, labeled.describe())
            .isEqualTo(root);
      }
    }
  }

  @Test
  void isAncestorOfMatchesAParentWalk() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyForest(seed);
      var forest = labeled.graph();
      int n = forest.nodeCount();
      var ancestry = Ancestry.of(forest);

      var rnd = new Random(seed * 31 + 3);
      for (int q = 0; q < QUERIES_PER_GRAPH; q++) {
        int node = rnd.nextInt(n);
        int descendant = rnd.nextInt(n);
        assertThat(ancestry.isAncestorOf(node, descendant))
            .as("isAncestorOf(%d, %d) for %s", node, descendant, labeled.describe())
            .isEqualTo(reachesByWalkingUp(ancestry, node, descendant));
      }
    }
  }

  @Test
  void depthAndParentAgreeWithTheGraph() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var labeled = RandomGraphs.anyForest(seed);
      var forest = labeled.graph();
      var ancestry = Ancestry.of(forest);

      for (int node = 0; node < forest.nodeCount(); node++) {
        var children = forest.outNodes(node);
        for (int i = 0; i < children.size(); i++) {
          int child = children.getInt(i);
          assertThat(ancestry.parentOf(child))
              .as("parent of %d for %s", child, labeled.describe())
              .isEqualTo(node);
          assertThat(ancestry.depthOf(child))
              .as("depth of %d for %s", child, labeled.describe())
              .isEqualTo(ancestry.depthOf(node) + 1);
        }
      }
    }
  }

  /** Walks from {@code node} to the root of its tree, one parent at a time. */
  private static int rootOf(Ancestry ancestry, int node) {
    int root = node;
    for (int up = ancestry.parentOf(root); up >= 0; up = ancestry.parentOf(root)) {
      root = up;
    }
    return root;
  }

  /** Walks from {@code descendant} to its root, one parent at a time. */
  private static boolean reachesByWalkingUp(Ancestry ancestry, int node, int descendant) {
    for (int x = descendant; x >= 0; x = ancestry.parentOf(x)) {
      if (x == node) return true;
    }
    return false;
  }
}
