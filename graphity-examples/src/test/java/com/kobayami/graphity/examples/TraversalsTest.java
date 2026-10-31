package com.kobayami.graphity.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobayami.graphity.EdgeType;
import com.kobayami.graphity.Edges;
import com.kobayami.graphity.Graph;
import org.junit.jupiter.api.Test;

class TraversalsTest {

  @Test
  void leaveNodeGivesTheCalleesBeforeTheirCallers() {
    assertThat(Traversals.calleesFirst().toString()).isEqualTo("[1, 6, 8, 3, 7, 9, 4, 5, 2, 0]");
    assertThat(Names.of(Traversals.calleesFirst(), SmallProgram.methodNames())).containsExactly(
        "parseArgs", "parse", "read", "load", "format", "write", "save", "show", "run", "main");
  }

  @Test
  void visitEdgeStopsAtTheDependencyOfStorageOnModel() {
    Graph dependencies = SmallProgram.dependencies();
    String[] names = SmallProgram.packageNames();
    int edge = Traversals.firstCycleEdge();
    assertThat(edge).isEqualTo(10);
    assertThat(names[dependencies.edgeSource(edge)]).isEqualTo("storage");
    assertThat(names[dependencies.edgeTarget(edge)]).isEqualTo("model");
  }

  @Test
  void currentLevelStopsTwoCallsAwayFromMain() {
    assertThat(Traversals.withinTwoCalls().toString()).isEqualTo("[0, 1, 2, 3, 4, 5]");
    assertThat(Names.of(Traversals.withinTwoCalls(), SmallProgram.methodNames()))
        .containsExactly("main", "parseArgs", "run", "load", "save", "show");
  }

  @Test
  void edgeTypesFindTheBackEdgeOfTheLoopAndTheForwardEdgePastThen() {
    String[] names = SmallProgram.blockNames();
    Traversals.Classified classified = Traversals.edgeTypesOfLoad();
    assertThat(classified.loopEdge()).isEqualTo(EdgeType.BACK);
    Edges forward = classified.forwardEdges();
    assertThat(forward.count()).isEqualTo(1);
    assertThat(names[forward.sourceNodes().getInt(0)]).isEqualTo("body");
    assertThat(names[forward.targetNodes().getInt(0)]).isEqualTo("next");
  }

  @Test
  void pathToGivesTheShortestCallChainFromMainToParse() {
    Traversals.Chain chain = Traversals.callChainToParse();
    assertThat(chain.depth()).isEqualTo(3);
    assertThat(chain.chain().toString()).isEqualTo("[0, 2, 3, 6]");
    assertThat(Names.of(chain.chain(), SmallProgram.methodNames()))
        .containsExactly("main", "run", "load", "parse");
  }
}
