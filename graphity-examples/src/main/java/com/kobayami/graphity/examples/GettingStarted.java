package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * The first program of the README: the dependencies between packages, and a cycle in
 * them.
 */
// @start region="program"
public class GettingStarted {

  public static void main(String[] args) {
    // @start region="firstResult"
    // The packages are the nodes 0..7, and uses[p] lists the packages that package p
    // uses.
    String[] names = {"app", "cli", "ui", "service", "model", "storage", "json", "util"};
    int[][] uses = {{1, 2, 3}, {7}, {3, 4, 7}, {4, 5}, {5}, {4, 6, 7}, {7}, {}};

    // Build the graph: an edge p -> q for every package q that p uses.
    var builder = new GraphBuilder();
    builder.addNodes(uses.length);
    for (int pkg = 0; pkg < uses.length; pkg++) {
      builder.addOutEdges(pkg, uses[pkg]);
    }
    Graph dependencies = builder.build();

    // Find a cycle.
    IntList cycle = Graphs.findCycle(dependencies);
    // cycle: [4, 5] - model and storage use each other
    // @end region="firstResult"

    // Print the cycle with the names of its packages.
    System.out.println(cycle.intStream().mapToObj(pkg -> names[pkg]).toList());
    // [model, storage]
  }
}
// @end region="program"
