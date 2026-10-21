package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.RandomGraphs;
import com.kobayami.graphity.testutils.TestGraphs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GraphFormatTest {

  private static byte[] bytesOf(Graph graph) throws IOException {
    var out = new ByteArrayOutputStream();
    GraphFormat.write(graph, out);
    return out.toByteArray();
  }

  private static Graph readBack(byte[] bytes) throws IOException {
    return GraphFormat.read(new ByteArrayInputStream(bytes));
  }

  private static void assertSameTopology(Graph actual, Graph expected) {
    assertThat(Csr.outAdjacencyStart(actual)).isEqualTo(Csr.outAdjacencyStart(expected));
    assertThat(Csr.outAdjacency(actual)).isEqualTo(Csr.outAdjacency(expected));
  }

  @Test
  void roundTripsRandomGraphs() throws IOException {
    for (long seed = 0; seed < 20; seed++) {
      Graph graph = RandomGraphs.gnp(60, 0.07, seed);
      assertSameTopology(readBack(bytesOf(graph)), graph);
    }
  }

  @Test
  void roundTripsTheEmptyGraph() throws IOException {
    Graph read = readBack(bytesOf(TestGraphs.empty()));

    assertThat(read.nodeCount()).isZero();
    assertThat(read.edgeCount()).isZero();
  }

  @Test
  void roundTripsArraysLargerThanOneBuffer() throws IOException {
    Graph graph = RandomGraphs.gnm(40_000, 100_000, 7);
    assertSameTopology(readBack(bytesOf(graph)), graph);
  }

  @Test
  void roundTripsArraysAtTheBufferBoundaries() throws IOException {
    int chunk = 1 << 14;
    for (int count : new int[] {chunk - 1, chunk, chunk + 1, 2 * chunk, 2 * chunk + 1}) {
      Graph edges = TestGraphs.path(count + 1);
      assertThat(edges.edgeCount()).isEqualTo(count);
      assertSameTopology(readBack(bytesOf(edges)), edges);

      Graph offsets = TestGraphs.isolated(count - 1);
      assertSameTopology(readBack(bytesOf(offsets)), offsets);
    }
  }

  @Test
  void writesTheDocumentedLayout() throws IOException {
    Graph graph = TestGraphs.of(3, 0, 1, 0, 2, 2, 0);
    var buffer = ByteBuffer.wrap(bytesOf(graph)).order(ByteOrder.LITTLE_ENDIAN);

    assertThat(buffer.remaining()).isEqualTo(16 + 4 * (3 + 1) + 4 * 3);
    assertThat(new String(buffer.array(), 0, 4, StandardCharsets.US_ASCII))
        .isEqualTo("GPHY");
    assertThat(buffer.getInt(4)).isEqualTo(1);
    assertThat(buffer.getInt(8)).isEqualTo(3);
    assertThat(buffer.getInt(12)).isEqualTo(3);
    int[] ints = new int[(buffer.remaining() - 16) / 4];
    buffer.position(16);
    buffer.asIntBuffer().get(ints);
    assertThat(ints).containsExactly(0, 2, 2, 3, 1, 2, 0);
  }

  @Test
  void biAdjacentGraphIsWrittenAsItsOutAdjacency() throws IOException {
    Graph graph = RandomGraphs.gnp(30, 0.1, 3);

    assertThat(bytesOf(BiAdjacentGraph.of(graph))).isEqualTo(bytesOf(graph));
  }

  @Test
  void readsExactlyOneGraphAndLeavesTheRest() throws IOException {
    var out = new ByteArrayOutputStream();
    Graph first = TestGraphs.path(4);
    Graph second = TestGraphs.cycle(3);
    GraphFormat.write(first, out);
    GraphFormat.write(second, out);
    out.write(42);
    var in = new ByteArrayInputStream(out.toByteArray());

    assertSameTopology(GraphFormat.read(in), first);
    assertSameTopology(GraphFormat.read(in), second);
    assertThat(in.read()).isEqualTo(42);
  }

  @Test
  void rejectsAWrongMagicNumber() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.path(3));
    bytes[0] = 'X';

    assertThatThrownBy(() -> readBack(bytes))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("magic");
  }

  @Test
  void rejectsAnUnknownVersion() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.path(3));
    bytes[4] = 2;

    assertThatThrownBy(() -> readBack(bytes))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("version 2");
  }

  @Test
  void rejectsCountsOutsideTheirRange() throws IOException {
    int[][] cases = {{8, -1}, {8, -5}, {8, Integer.MAX_VALUE}, {12, -1}, {12, Integer.MIN_VALUE}};
    for (int[] c : cases) {
      byte[] bytes = bytesOf(TestGraphs.path(3));
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(c[0], c[1]);

      assertThatThrownBy(() -> readBack(bytes))
          .isExactlyInstanceOf(IOException.class)
          .hasMessageContaining("invalid header");
    }
  }

  @Test
  void rejectsATruncatedStream() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.path(5));

    for (int length : new int[] {0, 3, 16, 20, bytes.length - 1}) {
      byte[] truncated = Arrays.copyOf(bytes, length);
      assertThatThrownBy(() -> readBack(truncated)).isInstanceOf(EOFException.class);
    }
  }

  @Test
  void aHugeAnnouncedSizeFailsAtTheEndOfTheStreamNotWithAnAllocation() throws IOException {
    var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    for (int field : new int[] {8, 12}) {
      byte[] bytes = bytesOf(TestGraphs.path(3));
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(field, Integer.MAX_VALUE - 8);

      long before = threads.getCurrentThreadAllocatedBytes();
      assertThatThrownBy(() -> readBack(bytes)).isInstanceOf(EOFException.class);
      long allocated = threads.getCurrentThreadAllocatedBytes() - before;

      assertThat(allocated).isLessThan(16L << 20);
    }
  }

  @Test
  void rejectsArraysThatDoNotFormAGraph() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.of(2, 0, 1));
    // The only target, at byte 16 + 4 x 3, points past the last node.
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(16 + 12, 5);

    assertThatThrownBy(() -> readBack(bytes))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("valid graph")
        .hasCauseExactlyInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void rejectsUnsortedListsWithTheCauseFromGraphOf() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.of(3, 0, 1, 0, 2));
    // Swap the two targets of node 0, at bytes 16 + 4 x 4 and 16 + 4 x 5.
    var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(32, 2).putInt(36, 1);

    assertThatThrownBy(() -> readBack(bytes))
        .isInstanceOf(IOException.class)
        .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void doesNotCloseTheStreams() throws IOException {
    var closed = new boolean[2];
    var out = new ByteArrayOutputStream() {
      @Override
      public void close() {
        closed[0] = true;
      }
    };
    GraphFormat.write(TestGraphs.path(3), out);
    var in = new ByteArrayInputStream(out.toByteArray()) {
      @Override
      public void close() {
        closed[1] = true;
      }
    };
    GraphFormat.read(in);

    assertThat(closed).containsExactly(false, false);
  }
}
