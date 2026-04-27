package com.kobayami.graphity.benchmarks.generators;

/**
 * Topology family used to drive the benchmark parameter matrix.
 *
 * <p>Each shape exercises a different performance/memory characteristic:
 * <ul>
 *   <li>{@link #PATH} — maximally deep (longest chain), stresses recursion depth. Acyclic.</li>
 *   <li>{@link #TREE} — random out-tree, high fanout at root, varying depth. Acyclic.</li>
 *   <li>{@link #GNP_SPARSE} — random directed graph with avg degree ≈ 3, may contain cycles.</li>
 *   <li>{@link #GNP_DENSE} — random directed graph with avg degree ≈ 30, usually strongly
 *       connected for n &gt; a few hundred.</li>
 *   <li>{@link #CHAIN_OF_CLIQUES} — cliques of size ≤ 32 chained together, many mid-size SCCs.</li>
 *   <li>{@link #DAG_GNP_SPARSE} — acyclic variant of {@link #GNP_SPARSE}: edges (i, j) with i &lt; j.</li>
 *   <li>{@link #DAG_GNP_DENSE} — acyclic variant of {@link #GNP_DENSE}.</li>
 * </ul>
 *
 * <p>Each non-random shape also has a {@code *_SHUFFLED} variant that applies a random
 * permutation to node IDs after generation. The underlying topology is unchanged, but
 * structural ID-locality (contiguous cliques, {@code i → i+1} chains, etc.) is destroyed.
 * This is the realistic worst case for compression-based representations like WebGraph's
 * {@code BVGraph}, whose gap/interval codes shine when adjacency is clustered in ID space.
 *
 * <p>Benchmarks that require acyclic input (e.g. {@code TopOrder}) restrict their
 * {@code @Param} to the acyclic subset; general benchmarks use all shapes.
 */
public enum GraphShape {
  PATH,
  TREE,
  GNP_SPARSE,
  GNP_DENSE,
  CHAIN_OF_CLIQUES,
  DAG_GNP_SPARSE,
  DAG_GNP_DENSE,
  PATH_SHUFFLED,
  TREE_SHUFFLED,
  GNP_SPARSE_SHUFFLED,
  GNP_DENSE_SHUFFLED,
  CHAIN_OF_CLIQUES_SHUFFLED,
  DAG_GNP_SPARSE_SHUFFLED,
  DAG_GNP_DENSE_SHUFFLED
}
