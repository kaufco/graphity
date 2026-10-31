package com.kobayami.graphity.examples.guide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.BiAdjacentGraph;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.examples.SmallFamily;
import com.kobayami.graphity.examples.SmallProgram;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The examples of the chapter "Building and reading graphs". */
class BuildingChapterTest {

  @Test
  void fromEdgesBuildsThePackagesOfTheSmallProgramAndMergesTheDuplicate() {
    BuildingFromEdges.Built built = BuildingFromEdges.dependencies();
    assertThat(built.nodeCount()).isEqualTo(8);
    assertThat(built.edgeCount()).isEqualTo(14);
    assertThat(built.dependencies()).isEqualTo(SmallProgram.dependencies());
  }

  @Test
  void fromObjectsBuildsTheSameGraphAndKeepsThePackageOfEachNode() {
    BuildingFromObjects.Built built = BuildingFromObjects.fromObjects();
    assertThat(built.dependencies()).isEqualTo(SmallProgram.dependencies());
    assertThat(built.packageOf()[5].name()).isEqualTo("storage");
    assertThat(built.usedByApp()).isEqualTo("ui");
    // The comment: an unknown name gives -1, which the builder rejects.
    var builder = new GraphBuilder();
    builder.addNodes(8);
    assertThatThrownBy(() -> builder.addEdge(0, -1))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void andNodesAddsTheNodesUpToTheHighestNodeId() {
    Graph graph = BuildingFromObjects.fromNumberedInput();
    assertThat(graph.nodeCount()).isEqualTo(8);
    assertThat(Graphs.adjacencyListOf(graph)).isEqualTo("""
        0 -> [3]
        1 -> []
        2 -> []
        3 -> [4]
        4 -> [0]
        5 -> []
        6 -> []
        7 -> [3]
        """);
  }

  @Test
  void addChildBuildsTheFamilyTreeOfTheDomain() {
    assertThat(BuildingTreesAndPaths.familyTree()).isEqualTo(SmallFamily.tree());
  }

  @Test
  void pathsAndBatchesBuildTheStepsOfTheDomain() {
    Graph steps = BuildingTreesAndPaths.steps();
    assertThat(Graphs.adjacencyListOf(steps)).isEqualTo("""
        0 -> [1]
        1 -> [3, 4, 6]
        2 -> [6]
        3 -> [5]
        4 -> [8]
        5 -> [6]
        6 -> [7]
        7 -> [8]
        8 -> []
        """);
    assertThat(steps).isEqualTo(SmallProgram.buildSteps());
  }

  @Test
  void addGraphPutsTheLibraryAfterTheProgram() {
    CombiningGraphs.Combined combined = CombiningGraphs.programWithLibrary();
    assertThat(combined.programOffset()).isEqualTo(0);
    assertThat(combined.libraryOffset()).isEqualTo(8);
    assertThat(combined.graph().nodeCount()).isEqualTo(11);
    assertThat(combined.graph().edgeCount()).isEqualTo(17);
    assertThat(combined.graph().outNodes(6).toString()).isEqualTo("[7, 8]");
    assertThat(combined.graph().outNodes(8).toString()).isEqualTo("[9, 10]");
    // The picture after: the program as it was, the library after it, and json -> api.
    String program = Graphs.adjacencyListOf(SmallProgram.dependencies());
    assertThat(Graphs.adjacencyListOf(combined.graph())).isEqualTo(
        program.replace("6 -> [7]", "6 -> [7, 8]") + "8 -> [9, 10]\n9 -> []\n10 -> []\n");
  }

  @Test
  void addGraphExtendsAGraph() {
    Graph next = CombiningGraphs.nextVersion();
    assertThat(next.nodeCount()).isEqualTo(8);
    assertThat(next.edgeCount()).isEqualTo(15);
    assertThat(next.hasOutNode(2, 5)).isTrue();
    // The text: ui -> storage gets the edge index 6, and ui -> util moves up from 6 to 7.
    assertThat(next.edgeIndexOf(2, 5)).isEqualTo(6);
    assertThat(SmallProgram.dependencies().edgeIndexOf(2, 7)).isEqualTo(6);
    assertThat(next.edgeIndexOf(2, 7)).isEqualTo(7);
  }

  @Test
  void dataOnNodesAndEdgesGivesTheCallsBetweenPackages() {
    KeepingData.Data data = KeepingData.callData();
    assertThat(data.packageOf()).hasSize(10);
    assertThat(Arrays.stream(data.siteCount()).sum()).isEqualTo(14);
    assertThat(data.lines()).isEqualTo(List.of(
        "main -> parseArgs: 1", "run -> load: 1", "run -> save: 1", "run -> show: 2",
        "load -> parse: 1", "load -> read: 1", "save -> format: 1", "save -> write: 1",
        "show -> format: 1"));
  }

  @Test
  void outNodesOfRun() {
    FindingNeighbors.Out out = FindingNeighbors.outNodes();
    assertThat(out.calledByRun().toString()).isEqualTo("[3, 4, 5]");
    assertThat(out.outDegree()).isEqualTo(3);
    assertThat(out.runCallsSave()).isTrue();
    assertThat(out.recursive()).isTrue();
  }

  @Test
  void inNodesOfFormat() {
    FindingNeighbors.In in = FindingNeighbors.inNodes();
    assertThat(in.callersOfFormat().toString()).isEqualTo("[4, 5, 7]");
    assertThat(in.inDegree()).isEqualTo(3);
    assertThat(in.saveCallsFormat()).isTrue();
    assertThat(in.neighborsOfSave().toString()).isEqualTo("[2, 7, 9]");
    assertThat(in.degreeOfSave()).isEqualTo(3);
    assertThat(in.mainIsRoot()).isTrue();
    // The text: degree counts edges, not neighbors - format has 3 neighbors but degree 4.
    BiAdjacentGraph both = BiAdjacentGraph.of(SmallProgram.calls());
    assertThat(both.neighbors(7).size()).isEqualTo(3);
    assertThat(both.degree(7)).isEqualTo(4);
  }

  @Test
  void sortedLists() {
    ReadingResultLists.Sorted sorted = ReadingResultLists.sortedLists();
    assertThat(sorted.size()).isEqualTo(3);
    assertThat(sorted.first()).isEqualTo(3);
    assertThat(sorted.last()).isEqualTo(5);
    assertThat(sorted.position()).isEqualTo(1);
    assertThat(sorted.hasFormat()).isFalse();
    assertThat(sorted.both().toString()).isEqualTo("[7, 9]");
    assertThat(sorted.copy()).containsExactly(3, 4, 5);
  }

  @Test
  void edgeLists() {
    assertThat(ReadingResultLists.edgeLists())
        .containsExactly("parse -> parse (edge 10)", "format -> format (edge 11)");
  }

  @Test
  void csrArraysMakeTheControlFlowOfLoad() {
    UsingCsrArrays.FromArrays fromArrays = UsingCsrArrays.load();
    assertThat(fromArrays.graph().nodeCount()).isEqualTo(6);
    assertThat(fromArrays.graph().edgeCount()).isEqualTo(7);
    assertThat(fromArrays.graph()).isEqualTo(SmallProgram.cfg());
    assertThat(fromArrays.start()).containsExactly(0, 1, 3, 5, 6, 7, 7);
    assertThat(fromArrays.adjacency()).containsExactly(1, 2, 5, 3, 4, 4, 1);
  }
}
