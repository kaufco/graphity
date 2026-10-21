# Graphity tutorial

A walk through the data model and the API, from building a graph to carrying your own data through transformations. The [README](../README.md) is the overview; the [Javadoc](https://kaufco.github.io/graphity/) is the contract for every method, including its cost.

1. [The data model](#the-data-model)
2. [Building graphs](#building-graphs)
3. [Predecessors: `BiAdjacentGraph`](#predecessors-biadjacentgraph)
4. [Algorithm results and their follow-ups](#algorithm-results-and-their-follow-ups)
5. [Data on nodes and edges](#data-on-nodes-and-edges)
6. [Views, immutability and threads](#views-immutability-and-threads)
7. [Custom traversals](#custom-traversals)
8. [Algorithm reference](#algorithm-reference)

## The data model

**Compact, topology-only data model.** Graphs hold nodes as integer IDs and edges as integer pairs. No labels, no weights, no per-node objects. Constant 4 bytes per (node + edge) regardless of topology. Labels and weights live in user-owned parallel arrays indexed by node ID or edge ID:

```java
double[] weights = new double[graph.edgeCount()];
```

**Direction is an interpretation, not a type.** Edges are stored as directed pairs, but nothing commits a graph to one reading. Where the distinction matters, both variants are available — `Components.sccsOf` reads the graph as directed, `Components.ccsOf` as undirected. An AST that is a tree for one analysis and a dependency graph for the next needs no conversion and no second copy. Reading a graph as undirected collapses `a → b` and `b → a` into one edge and ignores self-loops, so a graph that stores its undirected edges symmetrically has the same components, bridges, articulation points and blocks as one that stores a single direction per edge; results that list edges name the edges the graph actually stores.

## Building graphs

`GraphBuilder` covers the recurring construction patterns directly instead of leaving them as boilerplate — top-down and bottom-up node allocation, fan-out and fan-in batches, paths, and flat edge lists:

```java
var b = new GraphBuilder();
b.ensureEdgeCapacity(1_000_000);        // no reallocation when the size is known

int entry = b.addNode();
int cond = b.addChild(entry);                       // adds a node, links entry -> it, returns its ID
int firstBranch = b.addChildren(cond, 2);           // two successors at once, ID of the first
b.addPath(firstBranch, firstBranch + 1);            // chains existing nodes
b.addInEdges(entry, firstBranch + 1);               // fan-in, the counterpart of addEdges
```

The fan-out, fan-in and path methods take their node IDs in whichever form you already have them — loose arguments, an `int[]`, or an `IntList`. The last one matters because it is what Graphity itself returns, so a result can go straight back into a builder without a copy:

```java
for (int n = 0; n < source.nodeCount(); n++) {
    b.addEdges(n, source.outNodes(n));  // SortedIntView, no intermediate array
}
b.addPath(TopOrder.of(source));         // any IntList works
```

Node IDs often come from somewhere else — block numbers in a control-flow graph, symbol IDs from a table — and then arrive in arbitrary order, frequently as a flat list of pairs. Every edge-adding method has an `AndNodes` twin that creates what it references, so no caller has to track the highest ID:

```java
b.addEdgeAndNodes(7, 3);                     // 8 nodes now exist
b.addEdgePairs(7, 5, 5, 3);                  // flat pair list: 7 -> 5, 5 -> 3
b.addEdgePairsAndNodes(9, 7, 3, 9);          // 9 -> 7, 3 -> 9; 10 nodes now exist
```

Every method that takes existing node IDs checks them and throws `IndexOutOfBoundsException` for one that does not exist; a batch that contains one adds none of its edges.

`build()` sorts each adjacency list and removes duplicate edges, so the order of insertion is not the order of edge indices; `graph.edgeIndexOf(source, target)` tells where an added edge ended up.

### From CSR arrays, and to and from files

A graph whose adjacency lists are already sorted and free of duplicates - computed elsewhere, or read from storage - does not need the builder. `Graph.of` takes the two CSR arrays, checks them in O(nodeCount + edgeCount) and copies them; the position of a target in `outAdjacency` becomes its edge index:

```java
// 0 -> 1, 0 -> 2, 2 -> 0
Graph graph = Graph.of(new int[] {0, 2, 2, 3}, new int[] {1, 2, 0});
```

`GraphFormat` stores exactly these two arrays behind a 16-byte header with a magic number and a format version, as little-endian 32-bit integers - the layout a JavaScript `Int32Array` reads without parsing. Node IDs and edge indices survive the round trip, so arrays of node or edge data saved next to the graph stay aligned with it:

```java
try (var out = Files.newOutputStream(path)) {
    GraphFormat.write(graph, out);
}
Graph loaded;
try (var in = Files.newInputStream(path)) {
    loaded = GraphFormat.read(in);
}
```

Both methods move the arrays in 64 KiB blocks, so a file stream needs no extra buffering, and neither closes the stream. A file written by a later format version is rejected with an `IOException` rather than misread.

## Predecessors: `BiAdjacentGraph`

**`BiAdjacentGraph` for O(1) bidirectional access.** Opt-in via `BiAdjacentGraph.of(graph)`. Doubles the storage to 8 B per (node + edge) and gives constant-time access in both directions — useful for Kahn's topological order, which then reads in-degrees in O(nodeCount) instead of scanning every edge, and for anything that walks predecessors, such as bidirectional BFS or ancestry queries. Connectivity is not among them: `Components.wccsOf` and `ccsOf` are Union-Find over the out-edges and need no in-adjacency. The control-flow analyses, which do walk predecessors, take a `BiAdjacentGraph` as input: the in-adjacency is built once, where the caller sees the cost, and shared by every analysis that needs it.

## Algorithm results and their follow-ups

**Algorithm results as first-class typed values.** Where other libraries hand you `Set<Set<V>>` or `Map<V, Integer>` and let you figure out the rest, Graphity returns a domain object with useful follow-ups (entries marked *controlflow* are in the module `graphity-controlflow`):

| Algorithm                  | Result          | Useful follow-ups                            |
|----------------------------|-----------------|----------------------------------------------|
| `Components.sccsOf(graph)` | `Sccs`          | `containsCycle()`, `condense()`, `nodesOf()` |
| `Components.wccsOf(graph)` | `NodePartition` | `nodesOf()`, `condense()`                    |
| `Components.biconnectivityOf(biAdjacentGraph)` | `Biconnectivity` | `bridges()`, `articulationPoints()`, `blocks()` |
| `Components.strongConnectivityOf(biAdjacentGraph)` | `StrongConnectivity` | `strongBridges()`, `strongArticulationPoints()` |
| `EdgeTypes.of(graph)`      | `EdgeTypes`     | `typeOf(edgeIndex)`, `Edges.of(edgeTypes, TREE)` |
| `BackEdges.of(graph)`      | `Edges`         | `sourceNodes()`, `targetNodes()`, `edgeIndices()` |
| `Ancestry.of(forest)`      | `Ancestry`      | `lowestCommonAncestorOf(a, b)`, `depthOf()`  |
| `PreorderAncestry.of(ancestry)` | `PreorderAncestry` | `isAncestorOf()` in O(1), `subtreeOf(node)`, `subtreeSizeOf()` |
| `Dominators.of(cfg, entry)` | `Dominators` | `immediateDominatorOf()`, `Graphs.dominatorTree()`, `ControlFlow.dominanceFrontiers()` (*controlflow*), `Ancestry.of(dominators)` |
| `Loops.of(cfg, entry)` (*controlflow*) | `Loops`         | `innermostLoopOf()`, `nodesOf(loop)`, `entriesOf(loop)`, `isReducible()` |
| `BfsDistances.of(graph, source)` | `BfsDistances` | `distanceOf()`, `pathTo(node)`, `maxDistance()` |
| `NodeMapping.of(...)`      | `NodeMapping`   | `inverted()`, `compose(other)`, `gather(values)` |

These types compose. `sccs.condense()` returns a `Graph` plus a `NodeMapping` from original nodes to SCC IDs that you can run further algorithms on; `nodeMapping.compose(other)` chains transformations and applies the result to your own metadata arrays in one step.

## Data on nodes and edges

**Edge indices as the bridge to user metadata.** Edges have stable CSR slots, their edge indices. You hold edge-attached data in a parallel array of length `graph.edgeCount()`. When a transformation reorders or filters edges (subgraph, condensation, remap, transpose), Graphity returns the corresponding `EdgeMapping` so you can reindex your metadata array in one pass, and a partition folds the data of merged nodes or edges into one value per group:

```java
Graph transpose = BiAdjacentGraph.of(graph).transposed();        // node IDs kept, edge indices not
EdgeMapping newToOld = Graphs.transposedEdgeMapping(graph);      // one linear pass, total
double[] transposedWeights = newToOld.gather(weights);           // transposedWeights[e] = weights[newToOld.get(e)]

Graph condensed = partition.condense();                          // partition: e.g. Components.sccsOf(graph)
EdgePartition provenance = Graphs.edgePartition(condensed, graph, partition);
double[] weightPerCondensedEdge = provenance.reduce(weights, 0.0, Double::sum);
```

For a single edge instead of a full pass, `BiAdjacentGraph.transposedEdgeIndex(edgeIndex)` answers the same question in the other direction.

## Views, immutability and threads

**Zero-copy views.** Adjacency lists, partition members, edge lists — wherever the underlying CSR already holds the data in the right form, the API hands back a view, not a copy. No allocation on hot loops.

**Immutable graphs, safe to share across threads.** Once built, a `Graph` never changes. The same instance can be passed to multiple algorithms or threads without locking or defensive copying. Mutation happens exclusively through transformations (`subgraph`, `remap`, `condense`) that produce new graphs. The immutability is what makes the zero-copy result types and shared-graph parallelism safe.

**Raw CSR access for algorithm code.** Algorithms of your own that loop over all edges can read the CSR arrays directly through `Csr` - `Csr.outAdjacencyStart(graph)`, `Csr.outAdjacency(graph)` and their in-adjacency counterparts return the graph's own arrays, the same access the built-in algorithms use. This is the one place where immutability rests on a contract: the arrays are read-only by contract, and writing to them breaks the graph for everyone who holds it.

**Iterative algorithms — no recursion, no stack ceiling.** Every DFS-based algorithm (`Sccs`, `TopOrder`, `BackEdges`, `EdgeTypes`, the shape predicates, node reindexing) runs on an explicit heap-allocated stack, not the JVM call stack. They complete on arbitrarily deep graphs — verified at a DFS depth of 1M — under the default `-Xss`, with zero JVM tuning. This is a real differentiator: JGraphT, Guava, and WebGraph (built for billion-node web crawls) all use recursive DFS. On a 4.8M-node graph like `soc-LiveJournal1`, JGraphT's and WebGraph's SCC overflow the default stack and only complete once `-Xss` is raised to 256–512 MB (WebGraph's own SCC Javadoc advises setting "a large stack size"); on deeper topologies they fail regardless of `-Xss`. Graphity just runs.

## Custom traversals

For custom traversals, subclass `IterativeDfsTraverser` — it keeps its DFS stack on the heap, so it handles arbitrarily deep graphs without any `-Xss` tuning. Override `enterNode(int)` for preorder and `leaveNode(int)` for postorder; return `false` from `enterNode` to prune a subtree. `IterativeDfsTraverser`, `IterativeDfsEdgeTraverser` and `FrontierTraverser` also let any hook call `stop()` to end the whole traversal. For edge-centric algorithms that must look at every edge, such as finding a cycle, subclass `IterativeDfsEdgeTraverser`: its `visitEdge` hook reports each edge with the state of its target (unvisited, on the current path, finished) and returns whether to descend, skip the edge or stop. Multiple `run()` calls accumulate visited state, so two-phase walks compose without external bookkeeping.

To start from the root nodes of a `BiAdjacentGraph`, pass them in: `traverser.runFromNodes(Graphs.rootNodes(biAdjacentGraph))`. Inside any hook, the `BiAdjacentGraph` you hold gives access to predecessors.

### From recursion to hooks

A recursive depth-first search puts work before the children, after each child, and between children into one method, with the call stack holding the state. The traversers have no recursion, so that state moves into arrays and the work into hooks:

| Recursive pattern | With the traversers |
|---|---|
| Work before the children (preorder) | `enterNode(node)` |
| Work after all children (postorder) | `leaveNode(node)` |
| Combine a child's result into its parent | record the parent in `visitEdge` when descending, add to it in the child's `leaveNode` |
| Pass a value down (depth, path length) | compute the child's value from the source's in `visitEdge` when descending |
| Work between two children | `visitEdge` of the next child, before it descends - it also fires before the first child and for edges to visited targets, so filter on `UNVISITED` and on whether a child was seen already |
| Skip a subtree | return `false` from `enterNode`, or `SKIP` from `visitEdge` |

Subtree sizes, the classic example of combining child results, look like this:

```java
class SubtreeSizes extends IterativeDfsEdgeTraverser {
    final int[] parent;
    final int[] size;

    SubtreeSizes(Graph tree) {
        super(tree);
        parent = new int[tree.nodeCount()];
        size = new int[tree.nodeCount()];
        Arrays.fill(parent, -1);
    }

    @Override protected EdgeAction visitEdge(int source, int target, int edgeIndex, TargetState state) {
        if (state == TargetState.UNVISITED) parent[target] = source;   // about to descend
        return EdgeAction.DESCEND;
    }

    @Override protected void leaveNode(int node) {
        size[node]++;                                                   // the node itself
        if (parent[node] >= 0) size[parent[node]] += size[node];       // pass the total up
    }
}

var sizes = new SubtreeSizes(tree);
sizes.run();
```

Depth works the same way in the other direction, set when descending: `if (state == TargetState.UNVISITED) depth[target] = depth[source] + 1;` in `visitEdge`. The results match the recursive version, also on a path of a million nodes, where recursion overflows.

## Algorithm reference

Graphity ships the following algorithms out of the box. All of them return typed result values; entry points live on the algorithm class itself, not on the graph. The rows marked *controlflow* are in the module `graphity-controlflow`, package `com.kobayami.graphity.controlflow`; everything else is in `graphity-core`.

| Algorithm                                                       | Entry point                   |
|-----------------------------------------------------------------|-------------------------------|
| Strongly connected components (Tarjan)                          | `Components.sccsOf(graph)`    |
| Weakly connected components                                     | `Components.wccsOf(graph)`    |
| Connected components (undirected interpretation)                | `Components.ccsOf(graph)`     |
| Bridges, articulation points, biconnected components            | `Components.biconnectivityOf(biAdjacentGraph)` |
| Strong bridges, strong articulation points (directed)           | `Components.strongConnectivityOf(biAdjacentGraph)` |
| Breadth-first distances and shortest paths, one or several sources | `BfsDistances.of(graph, sources)` |
| Reachable nodes, sorted (input for subgraph extraction)         | `Graphs.reachableFrom(graph, sources)` |
| Topological order (DFS reverse postorder)                       | `TopOrder.of(graph)`          |
| Topological order with levels (Kahn's)                          | `TopOrderAndLevels.of(graph)` |
| DFS edge classification (TREE / BACK / FORWARD / CROSS)         | `EdgeTypes.of(graph)`         |
| Back edges                                                      | `BackEdges.of(graph)`         |
| Lowest common ancestor (trees and forests)                      | `Ancestry.of(forest)`         |
| Constant-time ancestor test and subtree views (trees and forests) | `PreorderAncestry.of(ancestry)` |
| Dominators, one or several entries (Lengauer-Tarjan)            | `Dominators.of(biAdjacentGraph, entries)` |
| Dominator tree                                                  | `Graphs.dominatorTree(dominators)` |
| Post-dominators                                                 | `Dominators.of(cfg.transposed(), exits)` |
| Dominance frontiers (*controlflow*)                             | `ControlFlow.dominanceFrontiers(dominators)` |
| Control dependence (*controlflow*)                              | `BiAdjacentGraph.of(ControlFlow.dominanceFrontiers(postDominators)).transposed()` |
| Iterated dominance frontiers, phi placement, Sreedhar-Gao (*controlflow*) | `IteratedDominanceFrontier.of(dominators)` |
| Loop nesting forest, irreducible loops included, Havlak (*controlflow*) | `Loops.of(biAdjacentGraph, entries)` |
| Shape predicates (`isDag`, `isForest`, `isTree`, `isPath`, `isStronglyConnected`) | `Graphs.isDag(graph)` |
| Shape predicates, undirected interpretation (`isBipartite`, `isUndirectedTree`, `isUndirectedForest`, `isWeaklyConnected`) | `Graphs.isBipartite(graph)` |
| Condensation DAG                                                | `sccs.condense()`             |
| Quotient graph by a partition of your choosing                  | `NodePartition.of(graph, groupOf, groupCount).condense()` |
| Subgraph extraction and node remapping                          | `Graphs.remapped(...)`        |
