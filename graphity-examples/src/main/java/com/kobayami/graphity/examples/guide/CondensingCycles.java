package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Graphs;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.SortedIntView;
import com.kobayami.graphity.examples.SmallGrammar;
import java.util.stream.Collectors;

/**
 * The rules of a grammar, each group of rules that use each other condensed into one
 * node.
 */
final class CondensingCycles {

  private CondensingCycles() {}

  /**
   * The condensed graph of the rules, a name for each of its nodes, and what statement
   * uses.
   */
  record Condensed(Graph graph, boolean acyclic, String[] groupNames,
      SortedIntView usedByStatements) {}

  /** Returns the condensed graph of the rules and the names of its nodes. */
  static Condensed condense() {
    Graph rules = SmallGrammar.rules();
    String[] names = SmallGrammar.ruleNames();
    int statement = SmallGrammar.ruleOf("statement");
    // @start region="condense"
    // Condense each group of rules that use each other into one node.
    Sccs sccs = Components.sccsOf(rules);
    Graph condensed = sccs.condense();
    boolean acyclic = Graphs.isDag(condensed);   // true
    // condensed: 0 -> [], 1 -> [], 2 -> [0, 1], 3 -> [0, 2], 4 -> [2, 3], 5 -> [4]

    // Name each node of the condensed graph after the rules in it.
    String[] groupNames = new String[sccs.sccCount()];
    for (int scc = 0; scc < sccs.sccCount(); scc++) {
      groupNames[scc] = sccs.nodesOf(scc).intStream()
          .mapToObj(rule -> names[rule]).collect(Collectors.joining("/"));
    }
    // groupNames: [name, number, expression/term/factor, assignment,
    //              statement/block/ifStatement, program]

    // Which groups does the group of statement use? The out-nodes of its node.
    SortedIntView usedByStatements = condensed.outNodes(sccs.sccOf(statement));
    // usedByStatements: [2, 3] - expression/term/factor and assignment
    // @end
    return new Condensed(condensed, acyclic, groupNames, usedByStatements);
  }
}
