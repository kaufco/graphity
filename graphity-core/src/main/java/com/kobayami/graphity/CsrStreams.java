package com.kobayami.graphity;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

// The binary format of Graph.writeTo and Graph.readFrom, whose Javadoc is its specification.
final class CsrStreams {

  private static final int HEADER_BYTES = 8;

  // Ints per block: the 64 KiB buffer that Graph.writeTo and Graph.readFrom document.
  private static final int CHUNK_INTS = 1 << 14;

  private CsrStreams() {}

  static void write(Graph graph, OutputStream out) throws IOException {
    var buffer = ByteBuffer.allocate(Integer.BYTES * CHUNK_INTS).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(graph.nodeCount()).putInt(graph.edgeCount());
    out.write(buffer.array(), 0, HEADER_BYTES);
    writeInts(Csr.outAdjacencyStart(graph), buffer, out);
    writeInts(Csr.outAdjacency(graph), buffer, out);
  }

  static Graph read(InputStream in) throws IOException {
    var buffer = ByteBuffer.allocate(Integer.BYTES * CHUNK_INTS).order(ByteOrder.LITTLE_ENDIAN);
    readFully(in, buffer.array(), HEADER_BYTES);
    int nodeCount = buffer.getInt(0);
    int edgeCount = buffer.getInt(4);
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

  // Grows the result as data arrives instead of allocating count ints up front: count comes from
  // the header, and a header may announce more than the stream holds.
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
