package com.kobayami.graphity.benchmarks.generators;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphArrays;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Input graphs as binary CSR files, so that every forked benchmark JVM loads its graph instead of
 * generating or parsing it again. Enabled by the system property {@value #PROPERTY} (the cache
 * directory); without it every call produces the graph afresh.
 *
 * <p>A file holds a little-endian header (magic, offset count, edge count) followed by the two
 * arrays. The loaded graph is identical to the produced one, array lengths included.
 *
 * <p>The file name carries a hash of the key and of the class files that produce the graph, so a
 * changed generator, builder or reordering is never served from a file written by the old code.
 * Writing a graph removes the files of the same label written by other code states.
 */
public final class GraphCache {

  public static final String PROPERTY = "graphity.bench.graphCache";

  private static final int MAGIC = 0x47435352;  // "GCSR"
  private static final int CHUNK_BYTES = 4 << 20;
  private static final String CORE_PREFIX = "com/kobayami/graphity/";
  private static final String BENCHMARKS_PREFIX = "com/kobayami/graphity/benchmarks/";
  private static final String GENERATORS_PREFIX = "com/kobayami/graphity/benchmarks/generators/";

  /** The code that generates and parses graphs: the generators and the builder. */
  static final Predicate<String> PRODUCING_CODE =
      name -> name.startsWith(GENERATORS_PREFIX) || name.startsWith(CORE_PREFIX + "GraphBuilder");

  /** Reordering may touch any class of the library, so a reordered graph depends on all of it. */
  static final Predicate<String> REORDERING_CODE = PRODUCING_CODE.or(
      name -> name.startsWith(CORE_PREFIX) && !name.startsWith(BENCHMARKS_PREFIX));

  private static final Map<Predicate<String>, String> FINGERPRINTS =
      Collections.synchronizedMap(new IdentityHashMap<>());

  private GraphCache() {}

  /**
   * Returns the cached graph for {@code key}, producing and writing it on a miss.
   *
   * @param label readable part of the file name, unique per key
   * @param key everything the graph depends on besides the code
   * @param code the class files the graph depends on
   */
  static Graph get(String label, String key, Predicate<String> code, Supplier<Graph> producer) {
    String dir = System.getProperty(PROPERTY);
    if (dir == null || dir.isBlank()) {
      return producer.get();
    }
    String safeLabel = label.replaceAll("[^A-Za-z0-9._-]+", "_");
    String hash = sha256Hex((key + "|" + fingerprint(code)).getBytes(StandardCharsets.UTF_8));
    Path file = Path.of(dir).resolve(safeLabel + "-" + hash.substring(0, 16) + ".csr");
    if (Files.isRegularFile(file)) {
      try {
        return read(file);
      } catch (IOException | RuntimeException e) {
        System.err.println("graph cache: unreadable " + file + " (" + e + "), regenerating");
      }
    }
    Graph graph = producer.get();
    try {
      Files.createDirectories(file.getParent());
      write(file, graph);
      removeOtherStates(file.getParent(), safeLabel, file);
    } catch (IOException e) {
      throw new UncheckedIOException("graph cache: cannot write " + file, e);
    }
    return graph;
  }

  private static Graph read(Path file) throws IOException {
    try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
      ByteBuffer header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
      readFully(ch, header);
      header.flip();
      if (header.getInt() != MAGIC) {
        throw new IOException("not a graph cache file");
      }
      int[] start = new int[header.getInt()];
      int[] adjacency = new int[header.getInt()];
      ByteBuffer buf = ByteBuffer.allocateDirect(CHUNK_BYTES).order(ByteOrder.LITTLE_ENDIAN);
      readInts(ch, buf, start);
      readInts(ch, buf, adjacency);
      if (ch.position() != ch.size() || start.length == 0 || start[start.length - 1] != adjacency.length) {
        throw new IOException("truncated or inconsistent graph cache file");
      }
      return GraphArrays.of(start, adjacency);
    }
  }

  private static void write(Path file, Graph graph) throws IOException {
    int[] start = GraphArrays.outAdjacencyStart(graph);
    int[] adjacency = GraphArrays.outAdjacency(graph);
    Path tmp = file.resolveSibling(file.getFileName() + ".tmp" + ProcessHandle.current().pid());
    try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING)) {
      ByteBuffer header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
      header.putInt(MAGIC).putInt(start.length).putInt(adjacency.length).flip();
      writeFully(ch, header);
      ByteBuffer buf = ByteBuffer.allocateDirect(CHUNK_BYTES).order(ByteOrder.LITTLE_ENDIAN);
      writeInts(ch, buf, start);
      writeInts(ch, buf, adjacency);
    } catch (IOException e) {
      Files.deleteIfExists(tmp);
      throw e;
    }
    Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }

  private static void removeOtherStates(Path dir, String label, Path keep) throws IOException {
    try (DirectoryStream<Path> stale = Files.newDirectoryStream(dir, label + "-????????????????.csr")) {
      for (Path p : stale) {
        if (!p.equals(keep)) {
          Files.deleteIfExists(p);
        }
      }
    }
  }

  private static void readInts(FileChannel ch, ByteBuffer buf, int[] target) throws IOException {
    IntBuffer ints = buf.clear().asIntBuffer();
    for (int off = 0; off < target.length; ) {
      int n = Math.min(target.length - off, CHUNK_BYTES / Integer.BYTES);
      buf.clear().limit(n * Integer.BYTES);
      readFully(ch, buf);
      ints.clear();
      ints.get(target, off, n);
      off += n;
    }
  }

  private static void writeInts(FileChannel ch, ByteBuffer buf, int[] source) throws IOException {
    IntBuffer ints = buf.clear().asIntBuffer();
    for (int off = 0; off < source.length; ) {
      int n = Math.min(source.length - off, CHUNK_BYTES / Integer.BYTES);
      ints.clear();
      ints.put(source, off, n);
      buf.clear().limit(n * Integer.BYTES);
      writeFully(ch, buf);
      off += n;
    }
  }

  private static void readFully(FileChannel ch, ByteBuffer buf) throws IOException {
    while (buf.hasRemaining()) {
      if (ch.read(buf) < 0) {
        throw new IOException("unexpected end of graph cache file");
      }
    }
  }

  private static void writeFully(FileChannel ch, ByteBuffer buf) throws IOException {
    while (buf.hasRemaining()) {
      ch.write(buf);
    }
  }

  /** Hash over the names and bytes of the matching class files, from the jar or class directory. */
  private static String fingerprint(Predicate<String> code) {
    return FINGERPRINTS.computeIfAbsent(code, ignored -> {
      Map<String, byte[]> classes = new TreeMap<>();
      for (Class<?> anchor : new Class<?>[] {Graph.class, GraphCache.class}) {
        collectClasses(anchor, code, classes);
      }
      try {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
          sha.update(e.getKey().getBytes(StandardCharsets.UTF_8));
          sha.update(e.getValue());
        }
        return HexFormat.of().formatHex(sha.digest());
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  private static void collectClasses(Class<?> anchor, Predicate<String> code, Map<String, byte[]> into) {
    try {
      Path source = Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (Files.isDirectory(source)) {
        try (Stream<Path> files = Files.walk(source)) {
          for (Path p : (Iterable<Path>) files::iterator) {
            String name = source.relativize(p).toString().replace('\\', '/');
            if (name.endsWith(".class") && code.test(name)) {
              into.put(name, Files.readAllBytes(p));
            }
          }
        }
      } else {
        try (JarFile jar = new JarFile(source.toFile())) {
          for (JarEntry entry : Collections.list(jar.entries())) {
            String name = entry.getName();
            if (name.endsWith(".class") && code.test(name)) {
              into.put(name, jar.getInputStream(entry).readAllBytes());
            }
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
