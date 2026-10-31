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
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GraphStreamTest {

  private static byte[] bytesOf(Graph graph) throws IOException {
    var out = new ByteArrayOutputStream();
    graph.writeTo(out);
    return out.toByteArray();
  }

  private static Graph readBack(byte[] bytes) throws IOException {
    return Graph.readFrom(new ByteArrayInputStream(bytes));
  }

  private static void assertSameTopology(Graph actual, Graph expected) {
    assertThat(Csr.outAdjacencyStart(actual)).isEqualTo(Csr.outAdjacencyStart(expected));
    assertThat(Csr.outAdjacency(actual)).isEqualTo(Csr.outAdjacency(expected));
  }

  /** The 8 header bytes of a graph with the given counts, and nothing after them. */
  private static byte[] header(int nodeCount, int edgeCount) {
    byte[] bytes = new byte[8];
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(0, nodeCount)
        .putInt(4, edgeCount);
    return bytes;
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

    assertThat(bytesOf(graph)).containsExactly(
        3, 0, 0, 0,   // nodeCount
        3, 0, 0, 0,   // edgeCount
        0, 0, 0, 0,   // outAdjacencyStart
        2, 0, 0, 0,
        2, 0, 0, 0,
        3, 0, 0, 0,
        1, 0, 0, 0,   // outAdjacency
        2, 0, 0, 0,
        0, 0, 0, 0);
  }

  @Test
  void writesLittleEndian() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.isolated(0x0102));

    assertThat(Arrays.copyOf(bytes, 8)).containsExactly(0x02, 0x01, 0, 0, 0, 0, 0, 0);
  }

  @Test
  void biAdjacentGraphIsWrittenAsItsOutAdjacency() throws IOException {
    Graph graph = RandomGraphs.gnp(30, 0.1, 3);

    assertThat(bytesOf(BiAdjacentGraph.of(graph))).isEqualTo(bytesOf(graph));
  }

  @Test
  void biAdjacentGraphIsReadBackAsAGraph() throws IOException {
    BiAdjacentGraph written = BiAdjacentGraph.of(RandomGraphs.gnp(30, 0.1, 3));

    Graph read = readBack(bytesOf(written));

    assertThat(read.getClass()).isEqualTo(Graph.class);
    assertSameTopology(read, written);
  }

  @Test
  void readFromCalledOnBiAdjacentGraphReturnsAGraph() throws IOException {
    BiAdjacentGraph written = BiAdjacentGraph.of(RandomGraphs.gnp(30, 0.1, 3));

    // The static method that BiAdjacentGraph inherits from Graph.
    Graph read = BiAdjacentGraph.readFrom(new ByteArrayInputStream(bytesOf(written)));

    assertThat(read.getClass()).isEqualTo(Graph.class);
    assertSameTopology(read, written);
  }

  @Test
  void readsExactlyOneGraphAndLeavesTheRest() throws IOException {
    var out = new ByteArrayOutputStream();
    Graph first = TestGraphs.path(4);
    Graph second = TestGraphs.cycle(3);
    first.writeTo(out);
    second.writeTo(out);
    out.write(42);
    var in = new ByteArrayInputStream(out.toByteArray());

    assertSameTopology(Graph.readFrom(in), first);
    assertSameTopology(Graph.readFrom(in), second);
    assertThat(in.read()).isEqualTo(42);
  }

  @Test
  void rejectsCountsOutsideTheirRange() throws IOException {
    int[][] cases = {{0, -1}, {0, -5}, {0, Integer.MAX_VALUE}, {4, -1}, {4, Integer.MIN_VALUE}};
    for (int[] c : cases) {
      byte[] bytes = bytesOf(TestGraphs.path(3));
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(c[0], c[1]);

      assertThatThrownBy(() -> readBack(bytes))
          .isExactlyInstanceOf(IOException.class)
          .hasMessageContaining("invalid header");
    }
  }

  @Test
  void rejectsCountsBeyondTheLargestArrayFromTheHeaderAlone() throws IOException {
    // A stream of the header only: a count the layout allows passes the header and then meets
    // the end of the stream; one beyond the largest array is rejected as an invalid header.
    int nodeLimit = Integer.MAX_VALUE - 9;
    int edgeLimit = Integer.MAX_VALUE - 8;
    assertThatThrownBy(() -> readBack(header(nodeLimit, 0))).isInstanceOf(EOFException.class);
    assertThatThrownBy(() -> readBack(header(0, edgeLimit))).isInstanceOf(EOFException.class);
    int[][] beyond = {{nodeLimit + 1, 0}, {Integer.MAX_VALUE - 1, 0}, {0, edgeLimit + 1}};
    for (int[] counts : beyond) {
      assertThatThrownBy(() -> readBack(header(counts[0], counts[1])))
          .isExactlyInstanceOf(IOException.class)
          .hasMessageContaining("invalid header");
    }
  }

  @Test
  void rejectsATruncatedStream() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.path(5));

    for (int length : new int[] {0, 3, 7, 8, 12, bytes.length - 1}) {
      byte[] truncated = Arrays.copyOf(bytes, length);
      assertThatThrownBy(() -> readBack(truncated)).isInstanceOf(EOFException.class);
    }
  }

  @Test
  void aHugeAnnouncedSizeFailsAtTheEndOfTheStreamNotWithAnAllocation() throws IOException {
    var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    // The largest node count and edge count the header allows.
    int[][] fieldAndCount = {{0, Integer.MAX_VALUE - 9}, {4, Integer.MAX_VALUE - 8}};
    for (int[] c : fieldAndCount) {
      byte[] bytes = bytesOf(TestGraphs.path(3));
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(c[0], c[1]);

      long before = threads.getCurrentThreadAllocatedBytes();
      assertThatThrownBy(() -> readBack(bytes)).isInstanceOf(EOFException.class);
      long allocated = threads.getCurrentThreadAllocatedBytes() - before;

      assertThat(allocated).isLessThan(16L << 20);
    }
  }

  @Test
  void rejectsArraysThatDoNotFormAGraph() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.of(2, 0, 1));
    // The only target, at byte 8 + 4 x 3, points past the last node.
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(8 + 12, 5);

    assertThatThrownBy(() -> readBack(bytes))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("valid graph")
        .hasCauseExactlyInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void rejectsAnEdgeCountThatDisagreesWithTheOffsets() throws IOException {
    // path(3) has the offsets 0, 1, 2, 2; announce and send one edge instead of two.
    byte[] bytes = bytesOf(TestGraphs.path(3));
    byte[] shortened = Arrays.copyOf(bytes, bytes.length - 4);
    ByteBuffer.wrap(shortened).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 1);

    assertThatThrownBy(() -> readBack(shortened))
        .isInstanceOf(IOException.class)
        .hasCauseExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsUnsortedListsWithTheCauseFromGraphOf() throws IOException {
    byte[] bytes = bytesOf(TestGraphs.of(3, 0, 1, 0, 2));
    // Swap the two targets of node 0, at bytes 8 + 4 x 4 and 8 + 4 x 5.
    var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(24, 2).putInt(28, 1);

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
    TestGraphs.path(3).writeTo(out);
    var in = new ByteArrayInputStream(out.toByteArray()) {
      @Override
      public void close() {
        closed[1] = true;
      }
    };
    Graph.readFrom(in);

    assertThat(closed).containsExactly(false, false);
  }
}
