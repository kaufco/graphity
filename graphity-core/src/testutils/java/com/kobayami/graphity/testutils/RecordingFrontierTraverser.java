package com.kobayami.graphity.testutils;

import com.kobayami.graphity.FrontierTraverser;
import com.kobayami.graphity.Graph;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * A {@link FrontierTraverser} that records visit order, the level at which each node was
 * visited, and completed levels. Enqueues all unvisited children via
 * {@link FrontierTraverser#enqueueChildren(int)} (standard BFS behavior).
 */
public final class RecordingFrontierTraverser extends FrontierTraverser {

  public final IntArrayList visitOrder = new IntArrayList();
  public final IntArrayList levelAtVisit = new IntArrayList();
  public final IntArrayList completedLevels = new IntArrayList();

  public RecordingFrontierTraverser(Graph graph) {
    super(graph);
  }

  @Override
  protected void visitNode(int node) {
    visitOrder.add(node);
    levelAtVisit.add(currentLevel());
    enqueueChildren(node);
  }

  @Override
  protected void onLevelComplete(int level) {
    completedLevels.add(level);
  }

  public IntList visitOrder() {
    return visitOrder;
  }
}
