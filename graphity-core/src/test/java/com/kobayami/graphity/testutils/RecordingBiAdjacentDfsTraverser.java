package com.kobayami.graphity.testutils;

import com.kobayami.graphity.BiAdjacentDfsTraverser;
import com.kobayami.graphity.BiAdjacentGraph;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * A {@link BiAdjacentDfsTraverser} that records preorder and postorder visit sequences.
 * Recurses into all unvisited children via {@link BiAdjacentDfsTraverser#visitChildren(int)}.
 */
public final class RecordingBiAdjacentDfsTraverser extends BiAdjacentDfsTraverser {

  public final IntArrayList preOrder = new IntArrayList();
  public final IntArrayList postOrder = new IntArrayList();

  public RecordingBiAdjacentDfsTraverser(BiAdjacentGraph graph) {
    super(graph);
  }

  @Override
  protected void visitNode(int node) {
    preOrder.add(node);
    visitChildren(node);
    postOrder.add(node);
  }

  public IntList preOrder() {
    return preOrder;
  }

  public IntList postOrder() {
    return postOrder;
  }
}
