package com.kobayami.graphity.testutils;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.IterativeDfsTraverser;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * An {@link IterativeDfsTraverser} that records preorder and postorder visit sequences.
 * The stack-safe counterpart to {@link RecordingDfsTraverser}, used to cross-validate that
 * both traversers discover nodes in the same order.
 */
public final class RecordingIterativeDfsTraverser extends IterativeDfsTraverser {

  public final IntArrayList preOrder = new IntArrayList();
  public final IntArrayList postOrder = new IntArrayList();

  public RecordingIterativeDfsTraverser(Graph graph) {
    super(graph);
  }

  @Override
  protected boolean enterNode(int node) {
    preOrder.add(node);
    return true;
  }

  @Override
  protected void leaveNode(int node) {
    postOrder.add(node);
  }

  public IntList preOrder() {
    return preOrder;
  }

  public IntList postOrder() {
    return postOrder;
  }
}
