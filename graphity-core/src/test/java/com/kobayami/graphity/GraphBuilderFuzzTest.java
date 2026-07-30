package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Cross-validates the {@link GraphBuilder} convenience and batch methods against the primitives
 * they are sugar for. The oracle is always a second construction path that must yield an equal
 * graph, so it stays independent of how either path is implemented.
 * <p>
 * Covered: capacity hints, the fan-out and fan-in batch forms in both their {@code int[]} and
 * their {@link it.unimi.dsi.fastutil.ints.IntList} shape, the flat edge-pair form, the
 * {@code AndNodes} twins against a pre-allocated equivalent, strict mode against the default
 * mode, and the child/parent helpers against explicit node and edge calls.
 */
class GraphBuilderFuzzTest {

  private static final int ITERATIONS = 200;

  private record EdgeList(int nodeCount, int[] sources, int[] targets) {

    int size() {
      return sources.length;
    }

    /** Node count required when nodes are created on reference rather than up front. */
    int referencedNodeCount() {
      int max = -1;
      for (int i = 0; i < size(); i++) {
        max = Math.max(max, Math.max(sources[i], targets[i]));
      }
      return max + 1;
    }

    String describe() {
      return "n=%d, m=%d".formatted(nodeCount, size());
    }
  }

  @Test
  void capacityHintsDoNotChangeTheResult() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var edges = randomEdges(seed);
      var expected = edgeByEdge(edges);

      for (int hint : new int[] {0, edges.size() / 2, edges.size(), 2 * edges.size() + 8}) {
        var b = new GraphBuilder();
        b.ensureEdgeCapacity(hint);
        b.addNodes(edges.nodeCount());
        for (int i = 0; i < edges.size(); i++) {
          b.addEdge(edges.sources()[i], edges.targets()[i]);
        }
        assertThat(b.build())
            .as("capacity hint %d for %s", hint, edges.describe())
            .isEqualTo(expected);
      }
    }
  }

  @Test
  void capacityHintAppliedMidConstructionKeepsAllEdges() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var edges = randomEdges(seed);
      int half = edges.size() / 2;

      var b = new GraphBuilder();
      b.addNodes(edges.nodeCount());
      for (int i = 0; i < half; i++) {
        b.addEdge(edges.sources()[i], edges.targets()[i]);
      }
      b.ensureEdgeCapacity(edges.size());
      for (int i = half; i < edges.size(); i++) {
        b.addEdge(edges.sources()[i], edges.targets()[i]);
      }

      assertThat(b.build()).as("mid-construction hint for %s", edges.describe())
          .isEqualTo(edgeByEdge(edges));
    }
  }

  @Test
  void batchFormsMatchEdgeByEdge() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var edges = randomEdges(seed);
      var expected = edgeByEdge(edges);
      var bySource = group(edges, true, edges.nodeCount());
      var byTarget = group(edges, false, edges.nodeCount());

      var fanOutArray = new GraphBuilder();
      fanOutArray.addNodes(edges.nodeCount());
      for (int node = 0; node < edges.nodeCount(); node++) {
        fanOutArray.addEdges(node, bySource[node].toIntArray());
      }

      var fanOutList = new GraphBuilder();
      fanOutList.addNodes(edges.nodeCount());
      for (int node = 0; node < edges.nodeCount(); node++) {
        fanOutList.addEdges(node, bySource[node]);
      }

      var fanInArray = new GraphBuilder();
      fanInArray.addNodes(edges.nodeCount());
      for (int node = 0; node < edges.nodeCount(); node++) {
        fanInArray.addInEdges(node, byTarget[node].toIntArray());
      }

      var fanInList = new GraphBuilder();
      fanInList.addNodes(edges.nodeCount());
      for (int node = 0; node < edges.nodeCount(); node++) {
        fanInList.addInEdges(node, byTarget[node]);
      }

      var pairs = new GraphBuilder();
      pairs.addNodes(edges.nodeCount());
      pairs.addEdgePairs(interleave(edges));

      assertThat(fanOutArray.build()).as("fan-out from int[] for %s", edges.describe())
          .isEqualTo(expected);
      assertThat(fanOutList.build()).as("fan-out from IntList for %s", edges.describe())
          .isEqualTo(expected);
      assertThat(fanInArray.build()).as("fan-in from int[] for %s", edges.describe())
          .isEqualTo(expected);
      assertThat(fanInList.build()).as("fan-in from IntList for %s", edges.describe())
          .isEqualTo(expected);
      assertThat(pairs.build()).as("edge pairs for %s", edges.describe()).isEqualTo(expected);
    }
  }

  @Test
  void andNodesTwinsMatchPreallocatedConstruction() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var edges = randomEdges(seed);
      int referenced = edges.referencedNodeCount();

      var expected = new GraphBuilder();
      expected.addNodes(referenced);
      for (int i = 0; i < edges.size(); i++) {
        expected.addEdge(edges.sources()[i], edges.targets()[i]);
      }
      var reference = expected.build();

      var single = new GraphBuilder();
      for (int i = 0; i < edges.size(); i++) {
        single.addEdgeAndNodes(edges.sources()[i], edges.targets()[i]);
      }

      var pairs = new GraphBuilder();
      pairs.addEdgePairsAndNodes(interleave(edges));

      var bySource = group(edges, true, referenced);
      var fanOutArray = new GraphBuilder();
      var fanOutList = new GraphBuilder();
      for (int node = 0; node < referenced; node++) {
        fanOutArray.addEdgesAndNodes(node, bySource[node].toIntArray());
        fanOutList.addEdgesAndNodes(node, bySource[node]);
      }

      var byTarget = group(edges, false, referenced);
      var fanInArray = new GraphBuilder();
      var fanInList = new GraphBuilder();
      for (int node = 0; node < referenced; node++) {
        fanInArray.addInEdgesAndNodes(node, byTarget[node].toIntArray());
        fanInList.addInEdgesAndNodes(node, byTarget[node]);
      }

      assertThat(single.build()).as("addEdgeAndNodes for %s", edges.describe())
          .isEqualTo(reference);
      assertThat(pairs.build()).as("addEdgePairsAndNodes for %s", edges.describe())
          .isEqualTo(reference);
      assertThat(fanOutArray.build()).as("addEdgesAndNodes from int[] for %s", edges.describe())
          .isEqualTo(reference);
      assertThat(fanOutList.build()).as("addEdgesAndNodes from IntList for %s", edges.describe())
          .isEqualTo(reference);
      assertThat(fanInArray.build()).as("addInEdgesAndNodes from int[] for %s", edges.describe())
          .isEqualTo(reference);
      assertThat(fanInList.build()).as("addInEdgesAndNodes from IntList for %s", edges.describe())
          .isEqualTo(reference);
    }
  }

  @Test
  void strictModeAcceptsEveryValidConstruction() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var edges = randomEdges(seed);

      var b = new GraphBuilder();
      b.strict();
      b.addNodes(edges.nodeCount());
      for (int i = 0; i < edges.size(); i++) {
        b.addEdge(edges.sources()[i], edges.targets()[i]);
      }

      assertThat(b.build()).as("strict mode for %s", edges.describe())
          .isEqualTo(edgeByEdge(edges));
    }
  }

  @Test
  void pathsMatchExplicitEdges() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var rnd = new Random(seed * 977 + 13);
      int n = 1 + rnd.nextInt(40);
      int length = rnd.nextInt(12);
      var nodes = new int[length];
      for (int i = 0; i < length; i++) {
        nodes[i] = rnd.nextInt(n);
      }

      var explicitEdges = new GraphBuilder();
      explicitEdges.addNodes(n);
      for (int i = 1; i < length; i++) {
        explicitEdges.addEdge(nodes[i - 1], nodes[i]);
      }
      var reference = explicitEdges.build();

      var viaPath = new GraphBuilder();
      viaPath.addNodes(n);
      viaPath.addPath(nodes);

      var viaPathList = new GraphBuilder();
      viaPathList.addNodes(n);
      viaPathList.addPath(new IntArrayList(nodes));

      assertThat(viaPath.build()).as("addPath, seed %d", seed).isEqualTo(reference);
      assertThat(viaPathList.build()).as("addPath from IntList, seed %d", seed)
          .isEqualTo(reference);

      int referenced = 0;
      for (int node : nodes) {
        referenced = Math.max(referenced, node + 1);
      }
      var grownReference = new GraphBuilder();
      grownReference.addNodes(referenced);
      grownReference.addPath(nodes);

      var viaPathAndNodes = new GraphBuilder();
      viaPathAndNodes.addPathAndNodes(nodes);

      var viaPathAndNodesList = new GraphBuilder();
      viaPathAndNodesList.addPathAndNodes(new IntArrayList(nodes));

      assertThat(viaPathAndNodes.build()).as("addPathAndNodes, seed %d", seed)
          .isEqualTo(grownReference.build());
      assertThat(viaPathAndNodesList.build())
          .as("addPathAndNodes from IntList, seed %d", seed)
          .isEqualTo(grownReference.build());
    }
  }

  @Test
  void childAndParentHelpersMatchManualConstruction() {
    for (long seed = 0; seed < ITERATIONS; seed++) {
      var helpers = new GraphBuilder();
      var manual = new GraphBuilder();
      helpers.addNode();
      manual.addNode();

      var rnd = new Random(seed * 31 + 5);
      int rounds = 1 + rnd.nextInt(12);
      int nodeCount = 1;

      for (int round = 0; round < rounds; round++) {
        int anchor = rnd.nextInt(nodeCount);
        int count = rnd.nextInt(4);
        boolean downwards = rnd.nextBoolean();

        int viaHelper;
        int viaManual;
        if (count == 1 && downwards) {
          viaHelper = helpers.addChild(anchor);
          viaManual = manual.addNode();
          manual.addEdge(anchor, viaManual);
        } else if (count == 1) {
          viaHelper = helpers.addParent(anchor);
          viaManual = manual.addNode();
          manual.addEdge(viaManual, anchor);
        } else if (downwards) {
          viaHelper = helpers.addChildren(anchor, count);
          viaManual = manual.addNodes(count);
          for (int i = 0; i < count; i++) {
            manual.addEdge(anchor, viaManual + i);
          }
        } else {
          viaHelper = helpers.addParents(anchor, count);
          viaManual = manual.addNodes(count);
          for (int i = 0; i < count; i++) {
            manual.addEdge(viaManual + i, anchor);
          }
        }

        assertThat(viaHelper).as("first returned ID, seed %d round %d", seed, round)
            .isEqualTo(viaManual);
        nodeCount += count;
      }

      assertThat(helpers.build()).as("child/parent helpers, seed %d", seed)
          .isEqualTo(manual.build());
    }
  }

  // ---- helpers ----

  private static EdgeList randomEdges(long seed) {
    var rnd = new Random(seed * 1_000_003L + 17);
    int n = 1 + rnd.nextInt(60);
    int m = rnd.nextInt(3 * n);
    var sources = new int[m];
    var targets = new int[m];
    for (int i = 0; i < m; i++) {
      sources[i] = rnd.nextInt(n);
      targets[i] = rnd.nextInt(n);
    }
    return new EdgeList(n, sources, targets);
  }

  private static Graph edgeByEdge(EdgeList edges) {
    var b = new GraphBuilder();
    b.addNodes(edges.nodeCount());
    for (int i = 0; i < edges.size(); i++) {
      b.addEdge(edges.sources()[i], edges.targets()[i]);
    }
    return b.build();
  }

  /** Buckets the edge list by source (or by target), producing one list per node. */
  private static IntArrayList[] group(EdgeList edges, boolean bySource, int nodeCount) {
    var groups = new IntArrayList[nodeCount];
    for (int node = 0; node < nodeCount; node++) {
      groups[node] = new IntArrayList();
    }
    for (int i = 0; i < edges.size(); i++) {
      int key = bySource ? edges.sources()[i] : edges.targets()[i];
      int value = bySource ? edges.targets()[i] : edges.sources()[i];
      groups[key].add(value);
    }
    return groups;
  }

  private static int[] interleave(EdgeList edges) {
    var flat = new int[2 * edges.size()];
    for (int i = 0; i < edges.size(); i++) {
      flat[2 * i] = edges.sources()[i];
      flat[2 * i + 1] = edges.targets()[i];
    }
    return flat;
  }
}
