package com.kobayami.graphity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobayami.graphity.testutils.TestGraphs;
import it.unimi.dsi.fastutil.ints.AbstractIntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class GraphBuilderTest {

  /** The node limit stated in the class Javadoc. */
  private static final int NODE_LIMIT = Integer.MAX_VALUE - 9;

  /** The edge limit stated in the class Javadoc. */
  private static final int EDGE_LIMIT = Integer.MAX_VALUE - 8;

  @Test
  void newBuilderBuildsEmptyGraph() {
    var graph = new GraphBuilder().build();
    assertThat(graph.nodeCount()).isZero();
    assertThat(graph.edgeCount()).isZero();
    assertThat(graph.isEmpty()).isTrue();
  }

  @Test
  void addNodeReturnsSequentialIds() {
    var b = new GraphBuilder();
    assertThat(b.addNode()).isZero();
    assertThat(b.addNode()).isEqualTo(1);
    assertThat(b.addNode()).isEqualTo(2);
    assertThat(b.build().nodeCount()).isEqualTo(3);
  }

  @Test
  void addNodesReturnsFirstAddedId() {
    var b = new GraphBuilder();
    b.addNode();
    int firstOfBatch = b.addNodes(3);
    assertThat(firstOfBatch).isEqualTo(1);
    assertThat(b.build().nodeCount()).isEqualTo(4);
  }

  @Test
  void addNodesWithZeroCountIsNoOp() {
    var b = new GraphBuilder();
    b.addNodes(2);
    int first = b.addNodes(0);
    assertThat(first).isEqualTo(2);
    assertThat(b.build().nodeCount()).isEqualTo(2);
  }

  @Test
  void addNodesWithNegativeCountThrows() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addNodes(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addEdgeWithInvalidSourceThrows() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdge(2, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdge(-1, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void addEdgeWithInvalidTargetThrows() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdge(0, 5))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdge(0, -1))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void addOutEdgesWithInvalidSourceThrows() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addOutEdges(5, new IntArrayList(new int[] {0})))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdges(-1, new IntArrayList(new int[] {0})))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void addOutEdgesWithInvalidTargetThrows() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addOutEdges(0, new IntArrayList(new int[] {0, 99})))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdges(0, new IntArrayList(new int[] {0, -1})))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void buildProducesSortedAdjacencyList() {
    var b = new GraphBuilder();
    b.addNodes(4);
    b.addEdge(0, 3);
    b.addEdge(0, 1);
    b.addEdge(0, 2);
    var graph = b.build();
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1, 2, 3);
  }

  @Test
  void buildDeduplicatesParallelEdges() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    b.addEdge(0, 1);
    b.addEdge(0, 1);
    var graph = b.build();
    assertThat(graph.edgeCount()).isEqualTo(1);
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1);
  }

  @Test
  void buildAllowsSelfLoops() {
    var b = new GraphBuilder();
    b.addNodes(1);
    b.addEdge(0, 0);
    var graph = b.build();
    assertThat(graph.edgeCount()).isEqualTo(1);
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(0);
    assertThat(graph.containsSelfLoop(0)).isTrue();
  }

  @Test
  void buildTrimsAdjacencyArrayAfterDedup() {
    var b = new GraphBuilder();
    b.addNodes(3);
    for (int i = 0; i < 100; i++) {
      b.addEdge(0, 1);
    }
    b.addEdge(0, 2);
    var graph = b.build();
    assertThat(graph.edgeCount()).isEqualTo(2);
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1, 2);
  }

  @Test
  void buildWithIsolatedNodesProducesNoEdges() {
    var graph = new GraphBuilder();
    graph.addNodes(5);
    var built = graph.build();
    assertThat(built.nodeCount()).isEqualTo(5);
    assertThat(built.edgeCount()).isZero();
    for (int i = 0; i < 5; i++) {
      assertThat(built.outNodes(i).toIntArray()).isEmpty();
    }
  }

  @Test
  void builderIsNotConsumedByBuild() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    var first = b.build();
    b.addNode();
    b.addEdge(1, 2);
    var second = b.build();
    assertThat(first.nodeCount()).isEqualTo(2);
    assertThat(first.edgeCount()).isEqualTo(1);
    assertThat(second.nodeCount()).isEqualTo(3);
    assertThat(second.edgeCount()).isEqualTo(2);
  }

  @Test
  void multipleBuildsAreIndependent() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    var first = b.build();
    var second = b.build();
    assertThat(first).isNotSameAs(second);
    assertThat(first).isEqualTo(second);
  }

  @Test
  void addOutEdgesAppendsAllTargets() {
    var b = new GraphBuilder();
    b.addNodes(4);
    b.addOutEdges(0, new IntArrayList(new int[] {3, 1, 2}));
    var graph = b.build();
    assertThat(graph.outNodes(0).toIntArray()).containsExactly(1, 2, 3);
  }

  @Test
  void addOutEdgesAcceptsSortedIntView() {
    var source = TestGraphs.path(4);
    var b = new GraphBuilder();
    b.addNodes(4);
    for (int n = 0; n < source.nodeCount(); n++) {
      b.addOutEdges(n, source.outNodes(n));
    }
    var copy = b.build();
    assertThat(copy).isEqualTo(source);
  }

  @Test
  void addGraphSingleImportPreservesFullTopology() {
    // Diamond with a self-loop: 0->0, 0->1, 0->2, 1->3, 2->3.
    var original = TestGraphs.of(4, 0, 0, 0, 1, 0, 2, 1, 3, 2, 3);
    var b = new GraphBuilder();
    int offset = b.addGraph(original);
    assertThat(offset).isZero();
    assertThat(b.build()).isEqualTo(original);
  }

  @Test
  void addGraphTwoImportsPreserveTopologyAtOffset() {
    // g1: diamond 0->1, 0->2, 1->3, 2->3 (4 nodes).
    var g1 = TestGraphs.of(4, 0, 1, 0, 2, 1, 3, 2, 3);
    // g2: triangle 0->1, 0->2, 1->2 (3 nodes).
    var g2 = TestGraphs.of(3, 0, 1, 0, 2, 1, 2);
    var b = new GraphBuilder();
    int off1 = b.addGraph(g1);
    int off2 = b.addGraph(g2);
    assertThat(off1).isZero();
    assertThat(off2).isEqualTo(4);
    // Expected merge: g1 edges unchanged + g2 edges shifted by 4.
    var expected = TestGraphs.of(7,
        0, 1, 0, 2, 1, 3, 2, 3,
        4, 5, 4, 6, 5, 6);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphAllowsBridgingEdgesBetweenImports() {
    var g1 = TestGraphs.of(3, 0, 1, 1, 2);
    var g2 = TestGraphs.of(3, 0, 1, 1, 2);
    var b = new GraphBuilder();
    int off1 = b.addGraph(g1);
    int off2 = b.addGraph(g2);
    b.addEdge(off1 + 2, off2 + 0);
    b.addEdge(off1 + 0, off2 + 2);
    var expected = TestGraphs.of(6,
        0, 1, 1, 2, 2, 3,
        3, 4, 4, 5,
        0, 5);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphWorksAfterManualEdges() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    int offset = b.addGraph(TestGraphs.of(3, 0, 1, 1, 2));
    assertThat(offset).isEqualTo(2);
    var expected = TestGraphs.of(5,
        0, 1,
        2, 3, 3, 4);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphWorksBeforeManualEdges() {
    var b = new GraphBuilder();
    int offset = b.addGraph(TestGraphs.of(3, 0, 1, 1, 2));
    assertThat(offset).isZero();
    b.addNode();
    b.addEdge(0, 3);
    b.addEdge(2, 3);
    var expected = TestGraphs.of(4,
        0, 1, 0, 3,
        1, 2,
        2, 3);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphDeduplicatesAgainstExistingEdges() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    b.addGraph(TestGraphs.of(2, 0, 1));
    var expected = TestGraphs.of(4,
        0, 1,
        2, 3);
    assertThat(b.build()).isEqualTo(expected);
  }

  @Test
  void addGraphOnEmptyGraphIsNoOp() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    int offset = b.addGraph(TestGraphs.empty());
    assertThat(offset).isEqualTo(2);
    assertThat(b.build()).isEqualTo(TestGraphs.of(2, 0, 1));
  }

  // ---- validation ----

  @Test
  void rejectsUnknownNodes() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdge(2, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdge(0, 2)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdge(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdges(0, new int[] {5})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdges(0, new int[] {5})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPath(0, 5)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgePairs(0, 5)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addChild(5)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addChildren(5, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addParent(5)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addParents(5, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdges(0, new IntArrayList(new int[] {5})))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdges(5, new int[] {0})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdges(5, new int[] {0})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdges(5, new IntArrayList(new int[] {0})))
        .isInstanceOf(IndexOutOfBoundsException.class);
    // No rejected call created a node or an edge.
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void rejectsNegativeIdsInEveryBatchForm() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addOutEdges(0, new int[] {-1})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdges(0, new IntArrayList(new int[] {-1})))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdges(0, new int[] {-1})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPath(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgePairs(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void leavesTheBuilderUnchangedWhenABatchIsRejected() {
    var b = new GraphBuilder();
    b.addNodes(3);
    b.addEdge(0, 1);
    assertThatThrownBy(() -> b.addOutEdges(0, new int[] {2, 99}))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPath(0, 2, 99)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgePairs(0, 2, 1, 99))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.build()).isEqualTo(TestGraphs.of(3, 0, 1));
  }

  @Test
  void rejectionMessageNamesNodeAndCount() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdge(0, 7))
        .hasMessageContaining("7")
        .hasMessageContaining("nodeCount=2");
  }

  @Test
  void acceptsValidReferences() {
    var b = new GraphBuilder();
    b.addNodes(3);
    b.addEdge(0, 1);
    b.addPath(1, 2);
    assertThat(b.build()).isEqualTo(TestGraphs.of(3, 0, 1, 1, 2));
  }

  @Test
  void rejectsUnknownNodeAfterImportingAGraph() {
    var b = new GraphBuilder();
    b.addGraph(TestGraphs.isolated(3));
    assertThatThrownBy(() -> b.addEdge(0, 3)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void andNodesMethodsStillCreateNodes() {
    var b = new GraphBuilder();
    b.addEdgeAndNodes(2, 0);
    assertThat(b.build()).isEqualTo(TestGraphs.of(3, 2, 0));
  }

  // ---- ensureEdgeCapacity ----

  @Test
  void ensureEdgeCapacityDoesNotChangeTheResult() {
    var hinted = new GraphBuilder();
    hinted.ensureEdgeCapacity(3);
    hinted.addNodes(3);
    hinted.addPath(0, 1, 2);
    assertThat(hinted.build()).isEqualTo(TestGraphs.of(3, 0, 1, 1, 2));
  }

  @Test
  void ensureEdgeCapacityMidConstructionPreservesEdges() {
    var b = new GraphBuilder();
    b.addNodes(3);
    b.addEdge(0, 1);
    b.ensureEdgeCapacity(1000);
    b.addEdge(1, 2);
    assertThat(b.build()).isEqualTo(TestGraphs.of(3, 0, 1, 1, 2));
  }

  @Test
  void ensureEdgeCapacityBelowCurrentSizeIsHarmless() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    b.ensureEdgeCapacity(0);
    assertThat(b.build()).isEqualTo(TestGraphs.of(2, 0, 1));
  }

  @Test
  void ensureEdgeCapacityPreventsReallocation() {
    var b = new GraphBuilder();
    b.ensureEdgeCapacity(1000);
    b.addNodes(2);
    int reserved = b.edgeCapacity();
    assertThat(reserved).isGreaterThanOrEqualTo(1000);
    for (int i = 0; i < 1000; i++) {
      b.addEdge(0, 1);
    }
    assertThat(b.edgeCapacity()).isEqualTo(reserved);
  }

  @Test
  void withoutAHintTheEdgeStorageGrows() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    int initial = b.edgeCapacity();
    for (int i = 0; i < 1000; i++) {
      b.addEdge(0, 1);
    }
    assertThat(b.edgeCapacity()).isGreaterThan(initial);
  }

  @Test
  void ensureEdgeCapacityRejectsNegative() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.ensureEdgeCapacity(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ensureEdgeCapacityRejectsMoreThanTheEdgeLimit() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.ensureEdgeCapacity(Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("edge limit");
    assertThatThrownBy(() -> b.ensureEdgeCapacity(EDGE_LIMIT + 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(b.edgeCapacity()).isZero();
  }

  @Test
  void ensureEdgeCapacityReservesExactly() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addOutEdges(0, new int[100]);
    b.ensureEdgeCapacity(101);
    assertThat(b.edgeCapacity()).isEqualTo(101);
  }

  // ---- growth of the edge storage under batch calls ----

  @Test
  void batchCallsInALoopGrowTheEdgeStorageGeometrically() {
    // 10 000 calls of two edges each: growing by half takes about 20 capacity changes, growing
    // to the exact size one per call.
    var targets = new IntArrayList(new int[] {1, 1});
    var path = TestGraphs.path(3);
    assertThat(capacityChanges(b -> b.addOutEdges(0, 1, 1))).isLessThanOrEqualTo(30);
    assertThat(capacityChanges(b -> b.addOutEdges(0, targets))).isLessThanOrEqualTo(30);
    assertThat(capacityChanges(b -> b.addInEdges(1, targets))).isLessThanOrEqualTo(30);
    assertThat(capacityChanges(b -> b.addPath(0, 1, 0))).isLessThanOrEqualTo(30);
    assertThat(capacityChanges(b -> b.addEdgePairs(0, 1, 1, 0))).isLessThanOrEqualTo(30);
    assertThat(capacityChanges(b -> b.addChildren(0, 2))).isLessThanOrEqualTo(30);
    assertThat(capacityChanges(b -> b.addParents(0, 2))).isLessThanOrEqualTo(30);
    assertThat(capacityChanges(b -> b.addGraph(path))).isLessThanOrEqualTo(30);
  }

  @Test
  void aBatchBeyondTheGrowthStepAllocatesExactlyOnce() {
    var imported = new GraphBuilder();
    imported.addGraph(TestGraphs.path(1001));
    assertThat(imported.edgeCapacity()).isEqualTo(1000);

    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdge(0, 1);
    b.addOutEdges(0, new int[1000]);
    assertThat(b.edgeCapacity()).isEqualTo(1001);
  }

  /** Counts how often 10 000 applications of {@code call} change the edge capacity. */
  private static int capacityChanges(Consumer<GraphBuilder> call) {
    var b = new GraphBuilder();
    b.addNodes(2);
    int capacity = b.edgeCapacity();
    int changes = 0;
    for (int i = 0; i < 10_000; i++) {
      call.accept(b);
      if (b.edgeCapacity() != capacity) {
        capacity = b.edgeCapacity();
        changes++;
      }
    }
    return changes;
  }

  // ---- addChild / addChildren / addParent / addParents ----

  @Test
  void addChildReturnsNewIdAndLinksIt() {
    var b = new GraphBuilder();
    int root = b.addNode();
    int child = b.addChild(root);
    assertThat(child).isEqualTo(1);
    assertThat(b.addChild(child)).isEqualTo(2);
    assertThat(b.build()).isEqualTo(TestGraphs.of(3, 0, 1, 1, 2));
  }

  @Test
  void addChildrenReturnsFirstIdAndLinksAll() {
    var b = new GraphBuilder();
    int root = b.addNode();
    int first = b.addChildren(root, 3);
    assertThat(first).isEqualTo(1);
    assertThat(b.build()).isEqualTo(TestGraphs.of(4, 0, 1, 0, 2, 0, 3));
  }

  @Test
  void addChildrenWithZeroCountAddsNothing() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThat(b.addChildren(0, 0)).isEqualTo(2);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void addChildrenWithNegativeCountThrows() {
    var b = new GraphBuilder();
    b.addNode();
    assertThatThrownBy(() -> b.addChildren(0, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addParentReturnsNewIdAndLinksIt() {
    var b = new GraphBuilder();
    int leaf = b.addNode();
    int parent = b.addParent(leaf);
    assertThat(parent).isEqualTo(1);
    assertThat(b.build()).isEqualTo(TestGraphs.of(2, 1, 0));
  }

  @Test
  void addParentsReturnsFirstIdAndLinksAll() {
    var b = new GraphBuilder();
    int join = b.addNode();
    int first = b.addParents(join, 2);
    assertThat(first).isEqualTo(1);
    assertThat(b.build()).isEqualTo(TestGraphs.of(3, 1, 0, 2, 0));
  }

  @Test
  void addParentsWithZeroCountAddsNothing() {
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThat(b.addParents(0, 0)).isEqualTo(2);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void addParentsWithNegativeCountThrows() {
    var b = new GraphBuilder();
    b.addNode();
    assertThatThrownBy(() -> b.addParents(0, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---- addPath ----

  @Test
  void addPathChainsExistingNodes() {
    var b = new GraphBuilder();
    b.addNodes(4);
    b.addPath(3, 1, 0);
    assertThat(b.build()).isEqualTo(TestGraphs.of(4, 3, 1, 1, 0));
  }

  @Test
  void addPathWithFewerThanTwoNodesAddsNoEdge() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addPath();
    b.addPath(1);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void addPathAllowsRepeatedNodes() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addPath(0, 1, 0);
    assertThat(b.build()).isEqualTo(TestGraphs.of(2, 0, 1, 1, 0));
  }

  // ---- addOutEdges(int[]) and addInEdges ----

  @Test
  void addOutEdgesArrayMatchesIntListOverload() {
    var viaArray = new GraphBuilder();
    viaArray.addNodes(4);
    viaArray.addOutEdges(0, new int[] {3, 1, 2});

    var viaList = new GraphBuilder();
    viaList.addNodes(4);
    viaList.addOutEdges(0, new IntArrayList(new int[] {3, 1, 2}));

    assertThat(viaArray.build()).isEqualTo(viaList.build());
  }

  @Test
  void addOutEdgesWithEmptyArrayAddsNothing() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addOutEdges(0, new int[0]);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void addInEdgesFansIn() {
    var b = new GraphBuilder();
    b.addNodes(3);
    b.addInEdges(0, new int[] {1, 2});
    assertThat(b.build()).isEqualTo(TestGraphs.inStar(3));
  }

  @Test
  void addInEdgesWithEmptyArrayAddsNothing() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addInEdges(0, new int[0]);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  // ---- parameter forms: loose arguments, int[], IntList ----

  @Test
  void theThreeParameterFormsAgreeForEveryBatchMethod() {
    assertFormsAgree("addOutEdges",
        (b, ids) -> b.addOutEdges(0, ids),
        (b, ids) -> b.addOutEdges(0, ids[0], ids[1], ids[2]),
        (b, ids) -> b.addOutEdges(0, new IntArrayList(ids)));
    assertFormsAgree("addInEdges",
        (b, ids) -> b.addInEdges(0, ids),
        (b, ids) -> b.addInEdges(0, ids[0], ids[1], ids[2]),
        (b, ids) -> b.addInEdges(0, new IntArrayList(ids)));
    assertFormsAgree("addOutEdgesAndNodes",
        (b, ids) -> b.addOutEdgesAndNodes(0, ids),
        (b, ids) -> b.addOutEdgesAndNodes(0, ids[0], ids[1], ids[2]),
        (b, ids) -> b.addOutEdgesAndNodes(0, new IntArrayList(ids)));
    assertFormsAgree("addInEdgesAndNodes",
        (b, ids) -> b.addInEdgesAndNodes(0, ids),
        (b, ids) -> b.addInEdgesAndNodes(0, ids[0], ids[1], ids[2]),
        (b, ids) -> b.addInEdgesAndNodes(0, new IntArrayList(ids)));
    assertFormsAgree("addPath",
        GraphBuilder::addPath,
        (b, ids) -> b.addPath(ids[0], ids[1], ids[2]),
        (b, ids) -> b.addPath(new IntArrayList(ids)));
    assertFormsAgree("addPathAndNodes",
        GraphBuilder::addPathAndNodes,
        (b, ids) -> b.addPathAndNodes(ids[0], ids[1], ids[2]),
        (b, ids) -> b.addPathAndNodes(new IntArrayList(ids)));
  }

  @Test
  void batchMethodsWithNoTargetsAddNothing() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addOutEdges(0);
    b.addInEdges(0);
    b.addOutEdges(0, IntLists.emptyList());
    b.addInEdges(0, IntLists.emptyList());
    b.addPath(IntLists.emptyList());
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void andNodesBatchMethodsWithNoTargetsStillCreateTheAnchor() {
    var b = new GraphBuilder();
    b.addOutEdgesAndNodes(2);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(3));

    var fanIn = new GraphBuilder();
    fanIn.addInEdgesAndNodes(2, IntLists.emptyList());
    assertThat(fanIn.build()).isEqualTo(TestGraphs.isolated(3));

    var path = new GraphBuilder();
    path.addPathAndNodes(IntLists.emptyList());
    assertThat(path.build().nodeCount()).isZero();
  }

  @Test
  void intListMethodsAcceptAdjacencyOfAnExistingGraph() {
    var source = TestGraphs.path(4);
    var copy = new GraphBuilder();
    copy.addNodes(4);
    for (int n = 0; n < source.nodeCount(); n++) {
      copy.addInEdges(n, BiAdjacentGraph.of(source).inNodes(n));
    }
    assertThat(copy.build()).isEqualTo(source);
  }

  @Test
  void intListArgumentsAreReadNotRetained() {
    var nodes = new IntArrayList(new int[] {1, 2});
    var b = new GraphBuilder();
    b.addNodes(4);
    b.addOutEdges(0, nodes);
    b.addInEdges(0, nodes);
    b.addPath(nodes);
    b.addOutEdgesAndNodes(3, nodes);
    b.addInEdgesAndNodes(3, nodes);
    b.addPathAndNodes(nodes);
    nodes.clear();
    nodes.add(3);
    assertThat(b.build())
        .isEqualTo(TestGraphs.of(4, 0, 1, 0, 2, 1, 0, 2, 0, 1, 2, 3, 1, 3, 2, 1, 3, 2, 3));
  }

  @Test
  void rejectsUnknownNodesInEveryIntListForm() {
    var b = new GraphBuilder();
    b.addNodes(2);
    var invalid = new IntArrayList(new int[] {0, 5});
    assertThatThrownBy(() -> b.addOutEdges(0, invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdges(0, invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPath(invalid)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void andNodesIntListMethodsRejectNegativeIdsAndAddNothing() {
    var b = new GraphBuilder();
    b.addEdgeAndNodes(0, 1);
    var invalid = new IntArrayList(new int[] {2, -1});
    assertThatThrownBy(() -> b.addOutEdgesAndNodes(0, invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdgesAndNodes(0, invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPathAndNodes(invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.build()).isEqualTo(TestGraphs.of(2, 0, 1));
  }

  @Test
  void andNodesIntListMethodsRejectIdsFarBeyondTheNodeLimit() {
    var b = new GraphBuilder();
    var invalid = new IntArrayList(new int[] {Integer.MAX_VALUE});
    assertThatThrownBy(() -> b.addOutEdgesAndNodes(0, invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdgesAndNodes(0, invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPathAndNodes(invalid))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.build().nodeCount()).isZero();
  }

  /**
   * Applies the same three node IDs through an {@code int[]}, through loose arguments and
   * through an {@link IntArrayList}, and asserts that all three built the same graph.
   * <p>
   * The first two arms reach the same method - what they establish is that the call form
   * compiles and that varargs wraps the arguments in the order given. Only the third arm
   * exercises a second implementation.
   */
  private static void assertFormsAgree(
      String method, BatchCall viaArray, BatchCall viaArguments, BatchCall viaList) {
    var reference = buildWith(viaArray);
    assertThat(buildWith(viaArguments)).as("%s from loose arguments", method)
        .isEqualTo(reference);
    assertThat(buildWith(viaList)).as("%s from IntList", method).isEqualTo(reference);
  }

  private static Graph buildWith(BatchCall call) {
    var b = new GraphBuilder();
    b.addNodes(4);
    call.apply(b, new int[] {3, 1, 2});
    return b.build();
  }

  @FunctionalInterface
  private interface BatchCall {
    void apply(GraphBuilder builder, int[] ids);
  }

  // ---- addEdgePairs ----

  @Test
  void addEdgePairsAddsOneEdgePerPair() {
    var b = new GraphBuilder();
    b.addNodes(4);
    b.addEdgePairs(0, 1, 2, 3);
    assertThat(b.build()).isEqualTo(TestGraphs.of(4, 0, 1, 2, 3));
  }

  @Test
  void addEdgePairsWithNoArgumentsAddsNothing() {
    var b = new GraphBuilder();
    b.addNodes(2);
    b.addEdgePairs();
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void addEdgePairsWithOddArgumentCountThrows() {
    var b = new GraphBuilder();
    b.addNodes(3);
    assertThatThrownBy(() -> b.addEdgePairs(0, 1, 2))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addEdgePairsAndNodesWithOddArgumentCountAddsNothing() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addEdgePairsAndNodes(0, 1, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(b.build().nodeCount()).isZero();
  }

  // ---- the AndNodes family ----

  @Test
  void addEdgeAndNodesCreatesEveryIdUpToTheHighest() {
    var b = new GraphBuilder();
    b.addEdgeAndNodes(7, 3);
    var graph = b.build();
    assertThat(graph.nodeCount()).isEqualTo(8);
    assertThat(graph.edgeCount()).isEqualTo(1);
    assertThat(graph.outNodes(7).toIntArray()).containsExactly(3);
  }

  @Test
  void andNodesMethodsNeverReduceTheNodeCount() {
    var b = new GraphBuilder();
    b.addNodes(10);
    b.addEdgeAndNodes(0, 1);
    b.addOutEdgesAndNodes(0, new int[] {2});
    b.addInEdgesAndNodes(0, new int[] {3});
    b.addPathAndNodes(4, 5);
    b.addEdgePairsAndNodes(6, 7);
    b.addEdgesAndNodes(BackEdges.of(TestGraphs.cycle(3)));
    assertThat(b.build().nodeCount()).isEqualTo(10);
  }

  @Test
  void addOutEdgesAndNodesMatchesPreallocatedVariant() {
    var grown = new GraphBuilder();
    grown.addOutEdgesAndNodes(4, new int[] {0, 9});

    var preallocated = new GraphBuilder();
    preallocated.addNodes(10);
    preallocated.addOutEdges(4, new int[] {0, 9});

    assertThat(grown.build()).isEqualTo(preallocated.build());
  }

  @Test
  void addInEdgesAndNodesMatchesPreallocatedVariant() {
    var grown = new GraphBuilder();
    grown.addInEdgesAndNodes(4, new int[] {0, 9});

    var preallocated = new GraphBuilder();
    preallocated.addNodes(10);
    preallocated.addInEdges(4, new int[] {0, 9});

    assertThat(grown.build()).isEqualTo(preallocated.build());
  }

  @Test
  void addPathAndNodesMatchesPreallocatedVariant() {
    var grown = new GraphBuilder();
    grown.addPathAndNodes(2, 5, 0);

    var preallocated = new GraphBuilder();
    preallocated.addNodes(6);
    preallocated.addPath(2, 5, 0);

    assertThat(grown.build()).isEqualTo(preallocated.build());
  }

  @Test
  void addPathAndNodesWithOneNodeStillCreatesIt() {
    var b = new GraphBuilder();
    b.addPathAndNodes(4);
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(5));
  }

  @Test
  void andNodesMethodsRejectNegativeIds() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addEdgeAndNodes(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgeAndNodes(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdgesAndNodes(0, new int[] {-1})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdgesAndNodes(0, new int[] {-1})).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPathAndNodes(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgePairsAndNodes(0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
    // Not reachable through results of a graph; checked on a hand-made list.
    var negative = Edges.wrap(new int[] {0}, new int[] {-1}, new int[1]);
    assertThatThrownBy(() -> b.addEdgesAndNodes(negative))
        .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void andNodesMethodsRejectIdsFarBeyondTheNodeLimit() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addEdgeAndNodes(0, Integer.MAX_VALUE))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPathAndNodes(Integer.MAX_VALUE))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.build().nodeCount()).isZero();
  }

  @Test
  void andNodesMethodsLeaveTheBuilderUnchangedWhenRejected() {
    var b = new GraphBuilder();
    b.addEdgeAndNodes(0, 1);
    assertThatThrownBy(() -> b.addOutEdgesAndNodes(0, new int[] {2, -1}))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPathAndNodes(0, 3, -1))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgesAndNodes(
        Edges.wrap(new int[] {0, 4}, new int[] {5, Integer.MAX_VALUE}, new int[2])))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.build()).isEqualTo(TestGraphs.of(2, 0, 1));
  }

  @Test
  void addChildrenReportsAnInvalidCountBeforeAnInvalidAnchor() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addChildren(99, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> b.addParents(99, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addEdgesTakesTheEdgesOfAResult() {
    // 0 -> 1 -> 2 -> 0, 2 -> 1: the back edges, as a graph of their own.
    Graph graph = TestGraphs.of(3, 0, 1, 1, 2, 2, 0, 2, 1);
    var back = BackEdges.of(graph);
    var b = new GraphBuilder();
    b.addNodes(graph.nodeCount());
    b.addEdges(back);
    Graph backGraph = b.build();
    assertThat(backGraph.edgeCount()).isEqualTo(back.count());
    for (int p = 0; p < back.count(); p++) {
      assertThat(backGraph.hasOutNode(back.sourceNodes().getInt(p), back.targetNodes().getInt(p)))
          .isTrue();
    }
  }

  @Test
  void addEdgesRejectsAMissingSourceAndAddsNothing() {
    var back = BackEdges.of(TestGraphs.cycle(3));   // 2 -> 0
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdges(back)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.build().edgeCount()).isZero();
  }

  @Test
  void addEdgesRejectsAMissingTargetAtALaterPositionAndAddsNothing() {
    var bridges = Components.biconnectivityOf(BiAdjacentGraph.of(TestGraphs.path(3))).bridges();
    assertThat(bridges.targetNodes().toIntArray()).containsExactly(1, 2);
    var b = new GraphBuilder();
    b.addNodes(2);
    assertThatThrownBy(() -> b.addEdges(bridges))
        .isInstanceOf(IndexOutOfBoundsException.class)
        .hasMessageContaining("target");
    assertThat(b.build()).isEqualTo(TestGraphs.isolated(2));
  }

  @Test
  void addEdgesAndNodesCreatesTheNodesItReferences() {
    var bridges = Components.biconnectivityOf(BiAdjacentGraph.of(TestGraphs.path(5))).bridges();
    var b = new GraphBuilder();
    b.addEdgesAndNodes(bridges);
    assertThat(b.build()).isEqualTo(TestGraphs.path(5));
  }

  @Test
  void addEdgesOfAnEmptyResultAddsNothing() {
    var b = new GraphBuilder();
    b.addEdges(BackEdges.of(TestGraphs.path(3)));
    b.addEdgesAndNodes(BackEdges.of(TestGraphs.path(3)));
    assertThat(b.build().nodeCount()).isZero();
  }

  // ---- node limit ----

  @Test
  void singleNodeMethodsFailAtTheNodeLimit() {
    var b = new GraphBuilder();
    b.addNodes(NODE_LIMIT);
    assertThatThrownBy(b::addNode).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> b.addChild(0)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> b.addParent(0)).isInstanceOf(IllegalStateException.class);
    assertThat(b.addNodes(0)).isEqualTo(NODE_LIMIT);
    assertThat(b.edgeCapacity()).isZero();
  }

  @Test
  void countMethodsRejectACountBeyondTheNodeLimit() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addNodes(Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
    b.addNodes(NODE_LIMIT - 2);
    assertThatThrownBy(() -> b.addNodes(3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("node limit");
    assertThatThrownBy(() -> b.addChildren(0, 3)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> b.addParents(0, 3)).isInstanceOf(IllegalArgumentException.class);
    assertThat(b.addNodes(0)).isEqualTo(NODE_LIMIT - 2);
    assertThat(b.edgeCapacity()).isZero();
    assertThat(b.addChildren(0, 2)).isEqualTo(NODE_LIMIT - 2);
    assertThat(b.addNodes(0)).isEqualTo(NODE_LIMIT);
  }

  @Test
  void addGraphRejectsAGraphBeyondTheNodeLimit() {
    var b = new GraphBuilder();
    b.addNodes(NODE_LIMIT - 2);
    assertThatThrownBy(() -> b.addGraph(TestGraphs.path(3)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(b.addNodes(0)).isEqualTo(NODE_LIMIT - 2);
    assertThat(b.edgeCapacity()).isZero();
    assertThat(b.addGraph(TestGraphs.path(2))).isEqualTo(NODE_LIMIT - 2);
    assertThat(b.addNodes(0)).isEqualTo(NODE_LIMIT);
  }

  @Test
  void andNodesMethodsRejectIdsAtTheNodeLimit() {
    var b = new GraphBuilder();
    assertThatThrownBy(() -> b.addEdgeAndNodes(0, NODE_LIMIT))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addOutEdgesAndNodes(NODE_LIMIT, new int[] {0}))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addInEdgesAndNodes(0, new IntArrayList(new int[] {NODE_LIMIT})))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addPathAndNodes(0, NODE_LIMIT))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgePairsAndNodes(NODE_LIMIT, 0))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> b.addEdgesAndNodes(
        Edges.wrap(new int[] {0}, new int[] {NODE_LIMIT}, new int[1])))
        .isInstanceOf(IndexOutOfBoundsException.class);
    assertThat(b.addNodes(0)).isZero();
    // The highest creatable ID fills the builder up to the limit.
    b.addEdgeAndNodes(0, NODE_LIMIT - 1);
    assertThat(b.addNodes(0)).isEqualTo(NODE_LIMIT);
    assertThatThrownBy(b::addNode).isInstanceOf(IllegalStateException.class);
  }

  // ---- edge limit ----

  @Test
  void countMethodsRejectEdgesBeyondTheEdgeLimitAndChangeNothing() {
    // Three edges plus NODE_LIMIT - 1 new ones are one more than the edge limit; the node count
    // would still fit.
    var b = new GraphBuilder();
    b.addNode();
    b.addOutEdges(0, 0, 0, 0);
    assertThatThrownBy(() -> b.addChildren(0, NODE_LIMIT - 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("edge limit");
    assertThatThrownBy(() -> b.addParents(0, NODE_LIMIT - 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("edge limit");
    assertThat(b.addNodes(0)).isEqualTo(1);
    assertThat(b.build()).isEqualTo(TestGraphs.of(1, 0, 0));
  }

  @Test
  void batchMethodsRejectEdgesBeyondTheEdgeLimitBeforeCreatingNodes() {
    // One edge plus EDGE_LIMIT new ones, from lists that hold no memory: the limit is reached
    // without allocating it. A path of k nodes has k - 1 edges.
    var b = new GraphBuilder();
    b.addEdgeAndNodes(0, 0);
    var zeros = zeros(EDGE_LIMIT);
    var path = zeros(EDGE_LIMIT + 1);
    assertThatThrownBy(() -> b.addOutEdges(0, zeros))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("edge limit");
    assertThatThrownBy(() -> b.addInEdges(0, zeros))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> b.addPath(path)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> b.addOutEdgesAndNodes(5, zeros))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> b.addInEdgesAndNodes(5, zeros))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> b.addPathAndNodes(path))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(b.addNodes(0)).isEqualTo(1);
    assertThat(b.build()).isEqualTo(TestGraphs.of(1, 0, 0));
  }

  @Test
  void buildRejectsMoreEdgesThanTheEdgeLimit() {
    // Only the single-edge methods get past the limit, and only with 16 GiB of edges, so the
    // check that build() runs is tested on its own.
    GraphBuilder.checkEdgeLimit(EDGE_LIMIT);
    assertThatThrownBy(() -> GraphBuilder.checkEdgeLimit(EDGE_LIMIT + 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage((EDGE_LIMIT + 1) + " edges added exceed the edge limit of " + EDGE_LIMIT);
    assertThatThrownBy(() -> GraphBuilder.checkEdgeLimit(Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** A list of {@code size} zeros backed by no array. */
  private static IntList zeros(int size) {
    return new AbstractIntList() {
      @Override
      public int getInt(int index) {
        return 0;
      }

      @Override
      public int size() {
        return size;
      }
    };
  }
}
