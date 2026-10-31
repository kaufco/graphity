package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Ancestry;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.PreorderAncestry;
import com.kobayami.graphity.examples.SmallFamily;
import it.unimi.dsi.fastutil.ints.IntList;

/** Parents, generations, ancestors and descendants in the family tree. */
final class ReadingAncestry {

  private ReadingAncestry() {}

  /** Parents and depths. */
  record Parents(int parentOfGreta, int generationOfGreta, int parentOfAda,
      int[] generations) {}

  /** Lowest common ancestors. */
  record Common(int gretaAndIda, int gretaAndEmil, int hugoAndGreta, int doraAndHugo) {}

  /** Ancestor tests. */
  record Tests(boolean benIsAncestor, boolean cleoIsAncestor, boolean hugoIsAncestor,
      boolean fastBenIsAncestor) {}

  /** Subtrees. */
  record Subtrees(IntList subtreeOfBen, int subtreeSizeOfBen, IntList preorder,
      int positionOfCleo) {}

  /**
   * Returns the parent and generation of Greta, the parent of Ada and every generation.
   */
  static Parents parents() {
    Graph family = SmallFamily.tree();
    int ada = SmallFamily.personOf("Ada");
    int greta = SmallFamily.personOf("Greta");
    // @start region="parentOf"
    Ancestry ancestry = Ancestry.of(family);
    int parent = ancestry.parentOf(greta);         // 3 - Dora
    int generation = ancestry.depthOf(greta);      // 3 - three generations below Ada
    int parentOfAda = ancestry.parentOf(ada);      // -1 - Ada is the root

    // The generation of every person, by node ID.
    int[] generations = new int[family.nodeCount()];
    for (int person = 0; person < generations.length; person++) {
      generations[person] = ancestry.depthOf(person);
    }
    // generations: [0, 1, 1, 2, 2, 2, 3, 3, 3, 3] - Ada in 0, Greta, Hugo, Ida and
    // Jonas in 3
    // @end
    return new Parents(parent, generation, parentOfAda, generations);
  }

  /** Returns the lowest common ancestors of four pairs. */
  static Common common() {
    Graph family = SmallFamily.tree();
    int dora = SmallFamily.personOf("Dora");
    int emil = SmallFamily.personOf("Emil");
    int greta = SmallFamily.personOf("Greta");
    int hugo = SmallFamily.personOf("Hugo");
    int ida = SmallFamily.personOf("Ida");
    // @start region="lowestCommonAncestor"
    Ancestry ancestry = Ancestry.of(family);
    int commonOfSecondCousins = ancestry.lowestCommonAncestorOf(greta, ida);   // 0 - Ada
    int commonOfUncleAndNiece = ancestry.lowestCommonAncestorOf(greta, emil);  // 1 - Ben
    int commonOfSiblings = ancestry.lowestCommonAncestorOf(hugo, greta);       // 3 - Dora
    // 3 - Dora herself
    int commonOfMotherAndSon = ancestry.lowestCommonAncestorOf(dora, hugo);
    // @end
    return new Common(commonOfSecondCousins, commonOfUncleAndNiece, commonOfSiblings,
        commonOfMotherAndSon);
  }

  /** Returns some ancestor tests, with Ancestry and with PreorderAncestry. */
  static Tests tests() {
    Graph family = SmallFamily.tree();
    int ben = SmallFamily.personOf("Ben");
    int cleo = SmallFamily.personOf("Cleo");
    int hugo = SmallFamily.personOf("Hugo");
    // @start region="isAncestorOf"
    Ancestry ancestry = Ancestry.of(family);
    // true - his grandfather
    boolean benIsAncestor = ancestry.isAncestorOf(ben, hugo);
    // false - his great-aunt
    boolean cleoIsAncestor = ancestry.isAncestorOf(cleo, hugo);
    // true - his own ancestor
    boolean hugoIsAncestor = ancestry.isAncestorOf(hugo, hugo);

    // For many tests, the index with the preorder answers each one in constant time.
    PreorderAncestry index = PreorderAncestry.of(ancestry);
    boolean fastBenIsAncestor = index.isAncestorOf(ben, hugo);   // true
    // @end
    return new Tests(benIsAncestor, cleoIsAncestor, hugoIsAncestor, fastBenIsAncestor);
  }

  /** Returns the subtree of Ben and the preorder of the family. */
  static Subtrees subtrees() {
    Graph family = SmallFamily.tree();
    int ben = SmallFamily.personOf("Ben");
    int cleo = SmallFamily.personOf("Cleo");
    // @start region="subtreeOf"
    PreorderAncestry index = PreorderAncestry.of(Ancestry.of(family));
    // [1, 3, 6, 7, 4] - Ben, Dora, Greta, Hugo, Emil
    IntList subtree = index.subtreeOf(ben);
    int size = index.subtreeSizeOf(ben);      // 5 - Ben included

    // The whole family in preorder, and where Cleo stands in it.
    IntList order = index.preorder();
    // order: [0, 1, 3, 6, 7, 4, 2, 5, 8, 9] - Ada, then the subtree of Ben, then that of
    // Cleo

    int positionOfCleo = index.preorderIndexOf(cleo);   // 6 - after Ada and Ben's subtree
    // @end
    return new Subtrees(subtree, size, order, positionOfCleo);
  }
}
