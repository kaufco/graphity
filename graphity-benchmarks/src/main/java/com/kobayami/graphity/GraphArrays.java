package com.kobayami.graphity;

/**
 * Benchmark-side construction of a {@link Graph} from CSR arrays without validation or copy, for
 * the binary graph cache of the benchmark suite. Lives in the library's package because the
 * constructor is package-private; it is not part of the published library. Reading the arrays
 * goes through {@link Csr}.
 */
public final class GraphArrays {

  private GraphArrays() {}

  /** A graph over the given arrays, which must form a valid CSR layout; they are not copied. */
  public static Graph of(int[] outAdjacencyStart, int[] outAdjacency) {
    return new Graph(outAdjacencyStart, outAdjacency);
  }
}
