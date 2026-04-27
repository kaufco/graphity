package com.kobayami.graphity.testutils;

import com.kobayami.graphity.DfsTraverser;
import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * A {@link DfsTraverser} that records preorder and postorder visit sequences.
 * Recurses into all unvisited children via {@link DfsTraverser#visitChildren(int)}.
 */
public final class RecordingDfsTraverser extends DfsTraverser {

  public final IntArrayList preOrder = new IntArrayList();
  public final IntArrayList postOrder = new IntArrayList();

  public RecordingDfsTraverser(Graph graph) {
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
