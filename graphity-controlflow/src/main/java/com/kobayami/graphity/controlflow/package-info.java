/**
 * Control-flow analyses on Graphity graphs: loops, dominance frontiers, control dependence and
 * phi placement.
 * <p>
 * The module {@code graphity-controlflow} builds on {@code graphity-core}: it takes a
 * {@link com.kobayami.graphity.BiAdjacentGraph} for the control-flow graph and
 * {@link com.kobayami.graphity.Dominators} from the core, and reads the graph through
 * {@link com.kobayami.graphity.Csr} like the core's own algorithms. It is released together with
 * the core, at the same version, and follows the core's null contract: nothing is {@code null}
 * unless its type is annotated {@code @Nullable}.
 *
 * <h2>Algorithms</h2>
 * <ul>
 *   <li>{@link Loops#of(com.kobayami.graphity.BiAdjacentGraph, int)} - the loop nesting forest,
 *       irreducible loops included (Havlak), with headers, entries, bodies and nesting
 *       depth.</li>
 *   <li>{@link ControlFlow#dominanceFrontiers(com.kobayami.graphity.Dominators)} - the dominance
 *       frontiers as a graph; on the post-dominators, transposed, control dependence.</li>
 *   <li>{@link IteratedDominanceFrontier#of(com.kobayami.graphity.Dominators)} - iterated
 *       dominance frontiers of node sets, where SSA construction places phi-nodes.</li>
 * </ul>
 *
 * <pre>{@code
 * BiAdjacentGraph cfg = BiAdjacentGraph.of(graph);
 * var loops = Loops.of(cfg, entry);
 * var dominators = Dominators.of(cfg, entry);
 * var phis = IteratedDominanceFrontier.of(dominators);
 * SortedIntView phiBlocks = phis.frontierOf(definitionBlocks);
 * }</pre>
 */
@NullMarked
package com.kobayami.graphity.controlflow;

import org.jspecify.annotations.NullMarked;
