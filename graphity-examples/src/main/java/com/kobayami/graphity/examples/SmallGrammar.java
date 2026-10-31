package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;

/**
 * The second example domain: the grammar of a small programming language, seen as a
 * graph.
 * <p>
 * The grammar has a rule for each construct of the language, written in EBNF (extended
 * Backus-Naur form):
 * <pre>
 * program     = { statement } ;
 * statement   = block | ifStatement | assignment ;
 * block       = "{" { statement } "}" ;
 * ifStatement = "if" "(" expression ")" statement ;
 * assignment  = name "=" expression ";" ;
 * expression  = term { "+" term } ;
 * term        = factor { "*" factor } ;
 * factor      = number | name | "(" expression ")" ;
 * </pre>
 * {@code name} and {@code number} are rules for single tokens and use no other rule.
 * {@link #rules()} gives an edge from each rule to each rule it uses, with the names in
 * {@link #ruleNames()}: statements nest in statements, expressions in expressions.
 * <p>
 * Node IDs follow the order of the names. The graph never changes: the comments of the
 * examples, and the documentation that shows them, depend on every node and edge.
 */
public final class SmallGrammar {

  private static final String[] RULES = {
      "program", "statement", "block", "ifStatement", "assignment",
      "expression", "term", "factor", "name", "number"};

  // A rule, then the rules it uses, in the order they appear in the rule.
  private static final String[][] USES = {
      {"program", "statement"},
      {"statement", "block", "ifStatement", "assignment"},
      {"block", "statement"},
      {"ifStatement", "expression", "statement"},
      {"assignment", "name", "expression"},
      {"expression", "term"},
      {"term", "factor"},
      {"factor", "number", "name", "expression"},
  };

  private SmallGrammar() {}

  /**
   * Returns the rules and the rules they use: an edge from each rule to each rule it
   * uses.
   *
   * @return 10 nodes, 14 edges
   */
  public static Graph rules() {
    var builder = new GraphBuilder();
    builder.addNodes(RULES.length);
    for (String[] row : USES) {
      int rule = ruleOf(row[0]);
      for (int i = 1; i < row.length; i++) {
        builder.addEdge(rule, ruleOf(row[i]));
      }
    }
    return builder.build();
  }

  /**
   * Returns the names of the rules, by node ID of {@link #rules()}.
   *
   * @return program, statement, block, ifStatement, assignment, expression, term, factor,
   *         name, number
   */
  public static String[] ruleNames() {
    return RULES.clone();
  }

  /**
   * Returns the node ID of the rule named {@code name}.
   *
   * @param name the name of the rule
   * @return the node ID
   * @throws IllegalArgumentException if no rule has this name
   */
  public static int ruleOf(String name) {
    return SmallProgram.nodeOf(RULES, name);
  }
}
