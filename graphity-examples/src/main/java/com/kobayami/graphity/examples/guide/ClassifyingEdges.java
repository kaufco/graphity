package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.EdgeTypes;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.examples.SmallProgram;

/** The type of each edge in a depth-first search of the packages. */
final class ClassifyingEdges {

  private ClassifyingEdges() {}

  /** What the example computes and reads. */
  record Classified(EdgeTypes types, EdgeType storageToModel, EdgeType appToService,
      Edges crossEdges) {}

  /**
   * Returns the types of the edges of the packages, two of them read, and the cross
   * edges.
   */
  static Classified classify() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    int app = SmallProgram.nodeOf(names, "app");
    int service = SmallProgram.nodeOf(names, "service");
    int model = SmallProgram.nodeOf(names, "model");
    int storage = SmallProgram.nodeOf(names, "storage");
    // @start region="edgeTypes"
    EdgeTypes types = EdgeTypes.of(dependencies);
    EdgeType storageToModel =
        types.typeOf(dependencies.edgeIndexOf(storage, model));   // BACK - closes a cycle
    // FORWARD - also through ui
    EdgeType appToService =
        types.typeOf(dependencies.edgeIndexOf(app, service));

    // All edges of one type, as an edge list.
    Edges crossEdges = Edges.of(types, EdgeType.CROSS);
    // crossEdges: ui -> util, storage -> util, json -> util
    // @end
    return new Classified(types, storageToModel, appToService, crossEdges);
  }
}
