package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Biconnectivity;
import com.kobayami.graphity.Components;
import com.kobayami.graphity.EdgePartition;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.ArrayList;
import java.util.List;

/**
 * The bridges and articulation points of the calls between methods, read as undirected:
 * the single calls and methods whose removal splits a component; and the blocks of the
 * calls.
 */
final class FindingBridges {

  private FindingBridges() {}

  /** The bridges and articulation points. */
  record Splitters(Edges bridges, SortedIntView articulationPoints) {}

  /** The blocks, and the edges in no block. */
  record Blocks(int blockCount, List<String> largeBlock, List<String> inNoBlock) {}

  /** Returns the bridges and articulation points of the calls, read as undirected. */
  static Splitters splitters() {
    Graph calls = SmallProgram.calls();
    // @start region="biconnectivity"
    // The analysis reads the edges as undirected and follows them both ways, so it takes
    // a BiAdjacentGraph.
    Biconnectivity biconnectivity =
        Components.biconnectivityOf(BiAdjacentGraph.of(calls));
    Edges bridges = biconnectivity.bridges();
    // bridges: main -> parseArgs, main -> run, run -> load, load -> parse, load -> read,
    //          save -> write
    SortedIntView articulationPoints = biconnectivity.articulationPoints();
    // articulationPoints: [0, 2, 3, 4] - main, run, load and save
    // @end
    return new Splitters(bridges, articulationPoints);
  }

  /**
   * Returns the blocks of the calls: their number, the edges of the large block, those in
   * none.
   */
  static Blocks blocks() {
    Graph calls = SmallProgram.calls();
    String[] names = SmallProgram.methodNames();
    // @start region="blocks"
    // The blocks come from the same analysis as the bridges.
    Biconnectivity biconnectivity =
        Components.biconnectivityOf(BiAdjacentGraph.of(calls));
    EdgePartition blocks = biconnectivity.blocks();
    // 7 - six blocks of one edge, one of four edges
    int blockCount = blocks.groupCount();

    // The edges other than self-loops that lie on a cycle, read as undirected: those of
    // the blocks of three edges or more. A bridge is a block of one edge, or of two if it
    // is stored in both directions.
    List<String> largeBlock = new ArrayList<>();
    for (int block = 0; block < blockCount; block++) {
      if (blocks.sizeOf(block) > 2) {
        for (int edge : blocks.edgesOf(block)) {
          largeBlock.add(
              names[calls.edgeSource(edge)] + " -> " + names[calls.edgeTarget(edge)]);
        }
      }
    }
    // largeBlock: [run -> save, run -> show, save -> format, show -> format]
    // @end
    List<String> inNoBlock = new ArrayList<>();
    for (int edge = 0; edge < calls.edgeCount(); edge++) {
      if (blocks.get(edge) == -1) {
        inNoBlock.add(
            names[calls.edgeSource(edge)] + " -> " + names[calls.edgeTarget(edge)]);
      }
    }
    return new Blocks(blockCount, largeBlock, inNoBlock);
  }
}
