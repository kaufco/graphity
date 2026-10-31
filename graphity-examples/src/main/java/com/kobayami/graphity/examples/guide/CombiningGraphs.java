package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.examples.SmallProgram;

/** Two graphs combined into one, and a graph extended by an edge. */
final class CombiningGraphs {

  private CombiningGraphs() {}

  /** The combined graph and the offsets of the two graphs in it. */
  record Combined(Graph graph, int programOffset, int libraryOffset) {}

  /** Returns the packages of the program and of a library, combined into one graph. */
  static Combined programWithLibrary() {
    Graph dependencies = SmallProgram.dependencies();
    // @start region="combine"
    // library is a JSON library of three packages - api uses parser and writer - that the
    // program's package json (6) uses.
    var libraryBuilder = new GraphBuilder();
    libraryBuilder.addNodes(3);
    libraryBuilder.addOutEdges(0, 1, 2);                 // api -> parser, api -> writer
    Graph library = libraryBuilder.build();

    // Put both into one graph; the nodes of each graph keep their order, after an offset.
    var builder = new GraphBuilder();
    int programOffset = builder.addGraph(dependencies);  // 0 - the program keeps 0 to 7
    // 8 - the library becomes 8 to 10
    int libraryOffset = builder.addGraph(library);
    builder.addEdge(programOffset + 6, libraryOffset);   // json -> api
    Graph combined = builder.build();
    // combined: 11 nodes, 17 edges
    // @end
    return new Combined(combined, programOffset, libraryOffset);
  }

  /**
   * Returns the dependencies of a next version of the program, in which ui also uses
   * storage.
   */
  static Graph nextVersion() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    int ui = SmallProgram.nodeOf(names, "ui");
    int storage = SmallProgram.nodeOf(names, "storage");
    // @start region="extend"
    // A graph never changes: to add an edge, start a builder with it and build a new
    // graph.
    var builder = new GraphBuilder();
    builder.addGraph(dependencies);   // into an empty builder: the node IDs stay the same
    builder.addEdge(ui, storage);
    Graph nextVersion = builder.build();
    // nextVersion: 8 nodes, 15 edges
    // @end
    return nextVersion;
  }
}
