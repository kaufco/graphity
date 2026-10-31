/**
 * Analyses of control flow: loops, dominance frontiers, control dependence and the placement of
 * phi-nodes.
 * <p>
 * This package is the module {@code graphity-controlflow}, released with {@code graphity-core} at
 * the same version. Its analyses work on a control-flow graph, with the basic blocks of a method
 * as nodes and an edge wherever control can pass from one block to another:
 * <ul>
 *   <li>{@link Loops} - the loop nesting forest: every loop with its header, its body and its
 *       entries, and how the loops nest, irreducible loops included.</li>
 *   <li>{@link IteratedDominanceFrontier} - the blocks where SSA (static single assignment) form
 *       places the phi-nodes of a variable.</li>
 *   <li>{@link ControlFlow} - the dominance frontiers as a graph, and from them control
 *       dependence: which block decides whether another one runs.</li>
 * </ul>
 * <p>
 * The analyses follow edges against their direction, so they take a
 * {@link com.kobayami.graphity.BiAdjacentGraph}, or the {@link com.kobayami.graphity.Dominators}
 * computed on one; build it once and pass it to all of them. Here, the loop of a method
 * {@code load} around its block {@code then}, from its control flow {@code cfg} and its entry
 * block {@code entry}:
 * {@snippet file="com/kobayami/graphity/examples/AnalyzingControlFlow.java" region="loops"}
 * <p>
 * No parameter, return value or element in this package is {@code null} unless its type is
 * annotated {@code @Nullable}.
 */
@NullMarked
package com.kobayami.graphity.controlflow;

import org.jspecify.annotations.NullMarked;
