package com.kobayami.graphity.examples.guide;

import com.kobayami.graphity.Components;
import com.kobayami.graphity.Graph;
import com.kobayami.graphity.Sccs;
import com.kobayami.graphity.examples.SmallGrammar;
import java.util.ArrayList;
import java.util.List;

/** The rules of a grammar that use each other, found as strongly connected components. */
final class FindingMutualDependencies {

  private FindingMutualDependencies() {}

  /** The groups of rules that use each other, and the number of SCCs. */
  record Groups(int sccCount, List<List<String>> recursive) {}

  /** Whether two rules are in the SCC of the expressions. */
  record SameGroup(int expressions, boolean factorInIt, boolean assignmentInIt) {}

  /** Returns the groups of rules that use each other, by their names. */
  static Groups recursiveGroups() {
    Graph rules = SmallGrammar.rules();
    String[] names = SmallGrammar.ruleNames();
    // @start region="groups"
    // Group the rules into SCCs: rules in one SCC use each other, directly or through
    // others.
    Sccs sccs = Components.sccsOf(rules);
    int sccCount = sccs.sccCount();   // 6

    // Keep the SCCs with a cycle: those are the rules that use themselves, through each
    // other.
    List<List<String>> recursive = new ArrayList<>();
    for (int scc = 0; scc < sccs.sccCount(); scc++) {
      if (sccs.containsCycle(scc)) {
        recursive.add(
            sccs.nodesOf(scc).intStream().mapToObj(rule -> names[rule]).toList());
      }
    }
    // recursive: [[expression, term, factor], [statement, block, ifStatement]]
    // @end
    return new Groups(sccCount, recursive);
  }

  /** Returns whether two pairs of rules are in the same SCC. */
  static SameGroup sameGroup() {
    Sccs sccs = Components.sccsOf(SmallGrammar.rules());
    int factor = SmallGrammar.ruleOf("factor");
    int expression = SmallGrammar.ruleOf("expression");
    int assignment = SmallGrammar.ruleOf("assignment");
    // @start region="sameGroup"
    // Two rules reach each other if they are in the same SCC.
    int expressions = sccs.sccOf(expression);                         // 2
    boolean factorInIt = sccs.sccOf(factor) == expressions;           // true
    boolean assignmentInIt = sccs.sccOf(assignment) == expressions;   // false
    // assignment uses expression, but expression does not use assignment.
    // @end
    return new SameGroup(expressions, factorInIt, assignmentInIt);
  }
}
