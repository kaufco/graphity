package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BackEdges;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.ArrayList;
import java.util.List;

/** The two kinds of lists that Graphity returns: sorted node lists and edge lists. */
final class ReadingResultLists {

  private ReadingResultLists() {}

  /** What the example reads from sorted lists. */
  record Sorted(int size, int first, int last, int position, boolean hasFormat,
      SortedIntView both, int[] copy) {}

  /** Returns what the example reads from the out-nodes of two methods. */
  static Sorted sortedLists() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    int run = SmallProgram.nodeOf(names, "run");
    int save = SmallProgram.nodeOf(names, "save");
    int show = SmallProgram.nodeOf(names, "show");
    int format = SmallProgram.nodeOf(names, "format");
    // @start region="sortedLists"
    SortedIntView calledByRun = calls.outNodes(run);    // [3, 4, 5] - load, save and show
    int size = calledByRun.size();                      // 3
    int first = calledByRun.first();                    // 3
    int last = calledByRun.last();                      // 5
    int position = calledByRun.indexOf(save);           // 1 - found by binary search
    boolean hasFormat = calledByRun.contains(format);   // false

    // Out-nodes come in ascending order and without duplicates, so they combine cheaply:
    // the methods that save or show call.
    SortedIntView both = calls.outNodes(save).union(calls.outNodes(show));
    // both: [7, 9] - format and write

    // [3, 4, 5] - a copy that may be changed
    int[] copy = calledByRun.toIntArray();
    // @end
    return new Sorted(size, first, last, position, hasFormat, both, copy);
  }

  /** Returns the recursive calls, read from an edge list. */
  static List<String> edgeLists() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    // @start region="edges"
    // An algorithm that returns edges returns an Edges: here the back edges of a search,
    // which are the recursive calls, since the only cycles of calls are methods that call
    // themselves.
    // Position p is the edge sourceNodes()[p] -> targetNodes()[p], and edgeIndices()[p]
    // is its edge index in calls.
    Edges recursive = BackEdges.of(calls);
    List<String> lines = new ArrayList<>();
    for (int p = 0; p < recursive.count(); p++) {
      int caller = recursive.sourceNodes().getInt(p);
      int callee = recursive.targetNodes().getInt(p);
      int edge = recursive.edgeIndices().getInt(p);
      lines.add(names[caller] + " -> " + names[callee] + " (edge " + edge + ")");
    }
    // lines: [parse -> parse (edge 10), format -> format (edge 11)]
    // @end
    return lines;
  }
}
