package com.kobayami.graphity;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * A binary format for the topology of a {@link Graph}: its two CSR arrays, as
 * {@link Graph#of(int[], int[])} takes them, behind a short header.
 * <p>
 * The layout, every field after the magic number a signed little-endian 32-bit integer in two's
 * complement:
 * <pre>
 * byte offset               size in bytes         field
 * 0                         4                     magic number, the ASCII bytes "GPHY"
 * 4                         4                     format version, 1
 * 8                         4                     nodeCount, 0 &lt;= nodeCount &lt;= 2^31 - 10
 * 12                        4                     edgeCount, 0 &lt;= edgeCount &lt;= 2^31 - 9
 * 16                        4 x (nodeCount + 1)   outAdjacencyStart
 * 16 + 4 x (nodeCount + 1)  4 x edgeCount         outAdjacency
 * </pre>
 * A graph takes exactly {@code 16 + 4 x (nodeCount + 1 + edgeCount)} bytes, without padding or
 * checksum. The bounds on the counts keep each array within the JDK's soft maximum array length,
 * {@code 2^31 - 9}, the largest array the library allocates. The arrays must satisfy the rules of
 * {@link Graph#of(int[], int[])}. Little-endian, so that consumers on common hardware - a
 * JavaScript {@code Int32Array} over the buffer, a NumPy array of type {@code <i4} - read the
 * arrays without conversion; the header keeps them 4-byte aligned. A reader rejects a version it
 * does not know rather than guessing.
 * <p>
 * The format holds topology only. Edge {@code e} of the read graph is edge {@code e} of the
 * written one, and node IDs are kept, so data per node or per edge stored separately in arrays
 * indexed by those IDs stays aligned with the graph.
 * <p>
 * Neither method closes the stream, and neither buffers beyond what it needs: wrap a file stream
 * in a {@link java.io.BufferedInputStream} or {@link java.io.BufferedOutputStream} only if other
 * data shares it in small pieces.
 *
 * <pre>{@code
 * try (var out = Files.newOutputStream(path)) {
 *     GraphFormat.write(graph, out);
 * }
 * Graph loaded;
 * try (var in = Files.newInputStream(path)) {
 *     loaded = GraphFormat.read(in);
 * }
 * }</pre>
 *
 * @see Graph#of(int[], int[])
 * @see Csr
 */
public final class GraphFormat {

  /** The format version this class writes and reads. */
  private static final int VERSION = 1;

  /** "GPHY" read as a little-endian integer. */
  private static final int MAGIC = 'G' | 'P' << 8 | 'H' << 16 | 'Y' << 24;

  private static final int HEADER_BYTES = 16;

  /** Ints per buffer when copying arrays to or from a stream. */
  private static final int CHUNK_INTS = 1 << 14;

  private GraphFormat() {}

  /**
   * Writes the topology of the graph to the stream.
   * <p>
   * A {@link BiAdjacentGraph} is written as its out-adjacency only; the in-adjacency is derived
   * again by {@link BiAdjacentGraph#of(Graph)} after reading.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> a 64 KiB buffer.
   *
   * @param graph the graph to write
   * @param out   the stream to write to; not closed
   * @throws IOException if writing to the stream fails
   */
  public static void write(Graph graph, OutputStream out) throws IOException {
    var buffer = ByteBuffer.allocate(Integer.BYTES * CHUNK_INTS).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(MAGIC).putInt(VERSION).putInt(graph.nodeCount()).putInt(graph.edgeCount());
    out.write(buffer.array(), 0, HEADER_BYTES);
    writeInts(Csr.outAdjacencyStart(graph), buffer, out);
    writeInts(Csr.outAdjacency(graph), buffer, out);
  }

  /**
   * Reads a graph written by {@link #write(Graph, OutputStream)}.
   * <p>
   * Reads exactly the bytes of one graph and leaves the stream after them. The arrays are checked
   * as by {@link Graph#of(int[], int[])}, but the graph keeps the arrays read, without the copy
   * {@code Graph.of} makes. Arrays grow with the data that actually arrives, so a header that
   * announces more than the stream holds fails at the end of the stream, not with an allocation
   * of the announced size.
   * <p>
   * <strong>Time:</strong> O(nodeCount + edgeCount).<br>
   * <strong>Memory:</strong> 4 x (nodeCount + 1 + edgeCount) bytes for the result, plus a
   * 64 KiB buffer. While an array grows, the old and the new one coexist: the peak is up to
   * twice the larger array, the price of not trusting the header.
   *
   * @param in the stream to read from; not closed
   * @return the graph
   * @throws EOFException if the stream ends before the graph does
   * @throws IOException if reading fails, the data does not start with the magic number of the
   *                     format, has a version other than 1, has a header with a count outside
   *                     the bounds of the layout, or does not form a valid graph; invalid arrays
   *                     carry the exception of {@link Graph#of(int[], int[])} as cause
   */
  public static Graph read(InputStream in) throws IOException {
    var buffer = ByteBuffer.allocate(Integer.BYTES * CHUNK_INTS).order(ByteOrder.LITTLE_ENDIAN);
    readFully(in, buffer.array(), HEADER_BYTES);
    int magic = buffer.getInt(0);
    if (magic != MAGIC) {
      throw new IOException("not a graph in Graphity's binary format: the magic number is "
          + Arrays.toString(Arrays.copyOf(buffer.array(), Integer.BYTES)) + " instead of \"GPHY\"");
    }
    int version = buffer.getInt(4);
    if (version != VERSION) {
      throw new IOException("unsupported format version " + version
          + "; this release reads version " + VERSION);
    }
    int nodeCount = buffer.getInt(8);
    int edgeCount = buffer.getInt(12);
    if (nodeCount < 0 || nodeCount > ArrayLimits.MAX_NODE_COUNT
        || edgeCount < 0 || edgeCount > ArrayLimits.MAX_ARRAY_LENGTH) {
      throw new IOException(
          "invalid header: nodeCount " + nodeCount + ", edgeCount " + edgeCount);
    }
    int[] start = readInts(in, nodeCount + 1, buffer);
    int[] adjacency = readInts(in, edgeCount, buffer);
    try {
      return Graph.ofOwned(start, adjacency);
    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
      throw new IOException("the stream does not hold a valid graph: " + e.getMessage(), e);
    }
  }

  private static void writeInts(int[] values, ByteBuffer buffer, OutputStream out)
      throws IOException {
    var ints = buffer.clear().asIntBuffer();
    for (int from = 0; from < values.length; from += CHUNK_INTS) {
      int count = Math.min(CHUNK_INTS, values.length - from);
      ints.clear();
      ints.put(values, from, count);
      out.write(buffer.array(), 0, Integer.BYTES * count);
    }
  }

  /**
   * Reads {@code count} little-endian ints, growing the result as data arrives instead of
   * trusting {@code count} for the allocation.
   */
  private static int[] readInts(InputStream in, int count, ByteBuffer buffer) throws IOException {
    var values = new int[Math.min(count, CHUNK_INTS)];
    var ints = buffer.clear().asIntBuffer();
    for (int from = 0; from < count; from += CHUNK_INTS) {
      int chunk = Math.min(CHUNK_INTS, count - from);
      readFully(in, buffer.array(), Integer.BYTES * chunk);
      if (from + chunk > values.length) {
        values = Arrays.copyOf(values, (int) Math.min(count, 2L * values.length));
      }
      ints.clear();
      ints.get(values, from, chunk);
    }
    return values;
  }

  private static void readFully(InputStream in, byte[] bytes, int length) throws IOException {
    int read = in.readNBytes(bytes, 0, length);
    if (read < length) {
      throw new EOFException("the stream ended inside a graph: " + read + " of " + length
          + " bytes of the last block");
    }
  }
}
