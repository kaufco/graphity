package com.kobayami.graphity;

/**
 * Benchmark-side access to the CSR arrays of a {@link Graph}, for the binary graph cache of the
 * benchmark suite. Lives in the library's package because the arrays and the constructor are
 * package-private; it is not part of the published library.
 */
public final class GraphArrays {

  private GraphArrays() {}

  /** The out-adjacency offsets, shared, not copied. */
  public static int[] outAdjacencyStart(Graph graph) {
    return graph.outAdjacencyStart;
  }

  /** The flat out-adjacency, shared, not copied. */
  public static int[] outAdjacency(Graph graph) {
    return graph.outAdjacency;
  }

  /** A graph over the given arrays, which must form a valid CSR layout; they are not copied. */
  public static Graph of(int[] outAdjacencyStart, int[] outAdjacency) {
    return new Graph(outAdjacencyStart, outAdjacency);
  }
}
