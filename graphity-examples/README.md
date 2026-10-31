# graphity-examples

This module holds the examples of the Graphity documentation: the Javadoc, the README and the
guide. The build compiles them against the current API, and the tests of this module run each
example and check what its comments claim, so an example in the documentation cannot go stale
unnoticed. The module is not published.

## The example domains

The examples work on three small domains seen as graphs, so that they build on each other:
`SmallProgram` is a small program with its packages, methods and control flow; `SmallGrammar` is
the grammar of a small language; and `SmallFamily` is a family tree.

The main graphs of `SmallProgram`, each with the names of its nodes, are:

- `dependencies()` and `packageNames()` - the packages of the program and the packages they use,
  with one cycle between `model` and `storage`;
- `calls()`, `callSites()`, `methodNames()` and `packageOf()` - the calls between its methods, two
  of which call themselves, and the package of each method;
- `cfg()` and `blockNames()` - the control flow of its method `load` between basic blocks: a loop
  with a branch in it.

Examples use variable names from the domain, such as `dependencies`, `calls`, `cfg` and `names`. A
comment on node IDs shows them as Java prints them, then their names:
`// cycle: [4, 5] - model and storage use each other`.

## How the Javadoc embeds an example

Each topic has a file in `com.kobayami.graphity.examples`. An example is a method that returns or
prints its result; the lines the Javadoc shows are a region of it:

```java
// @start region="transposed"
BiAdjacentGraph usedBy = BiAdjacentGraph.of(dependencies).transposed();
SortedIntView affected = Graphs.reachableFrom(usedBy, model);
// affected: [0, 2, 3, 4, 5] - app, ui, service, model and storage
// @end
```

The Javadoc of the published modules refers to the region by file and name:

```java
 * {@snippet file="com/kobayami/graphity/examples/Transposing.java" region="transposed"}
```

The parent POM passes `--snippet-path graphity-examples/src/main/java` to the Javadoc tool, for the
Javadoc jar of every module and for the aggregated site alike. A region that does not exist fails
the Javadoc build. Building Graphity needs JDK 25; the code, this module included, is compiled for Java 17.

A new example goes into the file of its topic, with a region name that says what it shows, and
a test in `src/test/java` that checks every claim of its comments. An idea is shown once: where the
Javadoc needs it in several places, those places embed the same region. Data that several examples
need belongs in one of the example domains: `SmallProgram`, `SmallGrammar` or `SmallFamily`.

The guide links the full example below each block. A region that the guide shows therefore has no
comment that only makes up for the code the excerpt leaves out.

## Line length

No line of the example sources is longer than 90 characters, counted with the indentation. That is
the maximum: the guide shows these sources at a standard width, and a longer line wraps there,
which spoils the formatting of the example. A line that is too long is shortened in one of three
ways:

- A comment line is wrapped, and the rest is joined with the next comment line, so that the comment
  reads as one paragraph. Every line starts with the comment marker: `//`, or `*` in Javadoc.
- A code line that is too long only because of its trailing comment: the comment moves to its own
  line directly above the code, with the same indentation. A result comment such as `// 3 -
  nextLine` moves the same way. Where it would land directly under another comment, a blank line
  keeps the two apart.
- A code line that is too long by itself is wrapped like the code around it: after `=`, after `(`
  or after a comma, with a continuation indented by 4 spaces.
