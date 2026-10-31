package com.kobayami.graphity.examples;

import com.kobayami.graphity.Graph;
import com.kobayami.graphity.GraphBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * The example domain: a small program, seen as graphs.
 * <p>
 * The program is a notebook on the command line. Its packages depend on each other, its
 * methods call each other, and each method has a control flow between its basic blocks.
 * This class gives six of these graphs, each with the names of its nodes:
 * <ul>
 *   <li>{@link #dependencies()} with {@link #packageNames()} - the packages and the
 *       packages they use; {@code model} and {@code storage} use each other, the one
 *       cycle.</li>
 *   <li>{@link #calls()} with {@link #methodNames()} - the methods and the methods they
 *       call, built from the {@link #callSites()}; {@code parse} and {@code format} call
 *       themselves. {@link #packageOf()} gives the package of each method.</li>
 *   <li>{@link #cfg()} with {@link #blockNames()} - the control flow of {@code load}, a
 *       loop with a branch in it, from the entry block to the exit block.</li>
 *   <li>{@link #cfgOfShow()} with {@link #blockNamesOfShow()} - the control flow of
 *       {@code show}, a loop over the lines of a note inside a loop over the notes.</li>
 *   <li>{@link #buildSteps()} with {@link #stepNames()} - the steps that build and
 *       publish the program, an edge from each step to each step that may start only
 *       after it.</li>
 *   <li>{@link #cfgOfParseArgs()} with {@link #blockNamesOfParseArgs()} - the control
 *       flow of {@code parseArgs}, a state machine whose loop is entered in either of its
 *       two states.</li>
 * </ul>
 * Node IDs follow the order of the names. The graphs never change: the comments of the
 * examples, and the Javadoc that shows them, depend on every node and edge.
 */
public final class SmallProgram {

  private static final String[] PACKAGES = {
      "app", "cli", "ui", "service", "model", "storage", "json", "util"};

  // A package, then the packages it uses.
  private static final String[][] USES = {
      {"app", "cli", "ui", "service"},
      {"cli", "util"},
      {"ui", "service", "model", "util"},
      {"service", "model", "storage"},
      {"model", "storage"},
      {"storage", "model", "json", "util"},
      {"json", "util"},
  };

  private static final String[] METHODS = {
      "main", "parseArgs", "run", "load", "save", "show", "parse", "format", "read",
      "write"};

  // A method, then the methods it calls, once per call site: run shows the notes before
  // and after saving them, and parse calls itself for nested arrays and for nested
  // objects.
  private static final String[][] CALL_SITES = {
      {"main", "parseArgs", "run"},
      {"run", "load", "show", "save", "show"},
      {"load", "read", "parse"},
      {"save", "format", "write"},
      {"show", "format"},
      {"parse", "parse", "parse"},
      {"format", "format"},
  };

  // The methods of each package, the package first. Packages without methods are left
  // out.
  private static final String[][] METHODS_OF_PACKAGE = {
      {"app", "main", "run"},
      {"cli", "parseArgs"},
      {"ui", "show"},
      {"service", "load", "save"},
      {"model", "parse", "format"},
      {"storage", "read", "write"},
  };

  private static final String[] BLOCKS = {
      "entry", "header", "body", "then", "next", "exit"};

  // The control flow of load, a block, then the blocks it may continue with:
  //
  //   List<Note> load(Path file) {
  //     List<String> lines = read(file);             // entry
  //     List<Note> notes = new ArrayList<>();
  //     for (int i = 0; i < lines.size(); i++) {     // header: i < lines.size()
  //       String line = lines.get(i);                // body
  //       if (!line.isBlank()) {
  //         notes.add(parse(line));                  // then
  //       }
  //     }                                            // next: i++
  //     return notes;                                // exit
  //   }
  private static final String[][] FLOW = {
      {"entry", "header"},
      {"header", "body", "exit"},
      {"body", "then", "next"},
      {"then", "next"},
      {"next", "header"},
  };

  private static final String[] SHOW_BLOCKS = {
      "entry", "nextNote", "format", "nextLine", "print", "exit"};

  // The control flow of show, a block, then the blocks it may continue with:
  //
  //   void show(List<Note> notes) {
  //     out.println(notes.size() + " notes");                  // entry
  //     for (Note note : notes) {                              // nextNote: another note?
  //       List<String> lines = format(note).lines().toList();  // format
  //       for (String line : lines) {                          // nextLine: another line?
  //         out.println("  " + line);                          // print
  //       }
  //     }
  //     out.flush();                                           // exit
  //   }
  private static final String[][] SHOW_FLOW = {
      {"entry", "nextNote"},
      {"nextNote", "format", "exit"},
      {"format", "nextLine"},
      {"nextLine", "print", "nextNote"},
      {"print", "nextLine"},
  };

  private static final String[] STEPS = {
      "generate", "compile", "lint", "compileTests", "docs", "test", "package", "sign",
      "publish"};

  // A step, then the steps that may start only after it.
  private static final String[][] BEFORE = {
      {"generate", "compile"},
      {"compile", "compileTests", "docs", "package"},
      {"lint", "package"},
      {"compileTests", "test"},
      {"docs", "publish"},
      {"test", "package"},
      {"package", "sign"},
      {"sign", "publish"},
  };

  private static final String[] PARSE_ARGS_BLOCKS = {"entry", "option", "value", "exit"};

  // The control flow of parseArgs, a block, then the blocks it may continue with.
  // parseArgs is generated from a state machine with two states that jump to each other;
  // it starts in the state "option" or, if the first argument is not an option, in the
  // state "value":
  //
  //   entry:   if (isOption(args[0])) goto option; else goto value;
  //   option:  read an option;  if (done) goto exit; else goto value;
  //   value:   read a value;    if (done) goto exit; else goto option;
  //   exit:    return the settings;
  private static final String[][] PARSE_ARGS_FLOW = {
      {"entry", "option", "value"},
      {"option", "value", "exit"},
      {"value", "option", "exit"},
  };

  private SmallProgram() {}

  /**
   * Returns the dependencies between the packages: an edge from each package to each
   * package it uses.
   *
   * @return 8 nodes, 14 edges
   */
  public static Graph dependencies() {
    return build(PACKAGES, USES);
  }

  /**
   * Returns the names of the packages, by node ID of {@link #dependencies()}.
   *
   * @return app, cli, ui, service, model, storage, json, util
   */
  public static String[] packageNames() {
    return PACKAGES.clone();
  }

  /**
   * Returns the calls between the methods: an edge from each method to each method it
   * calls, built with {@link GraphBuilder} from one edge per call site, which merges the
   * duplicates.
   *
   * @return 10 nodes, 12 edges
   */
  public static Graph calls() {
    return build(METHODS, CALL_SITES);
  }

  /**
   * Returns the call sites in source order, one per place in the source where a method
   * calls another: {@code run} calls {@code show} twice, and {@code parse} calls itself
   * twice.
   *
   * @return 14 call sites, as node IDs of {@link #calls()}
   */
  public static List<CallSite> callSites() {
    var sites = new ArrayList<CallSite>();
    for (String[] row : CALL_SITES) {
      int caller = nodeOf(METHODS, row[0]);
      for (int i = 1; i < row.length; i++) {
        sites.add(new CallSite(caller, nodeOf(METHODS, row[i])));
      }
    }
    return List.copyOf(sites);
  }

  /**
   * Returns the names of the methods, by node ID of {@link #calls()}.
   *
   * @return main, parseArgs, run, load, save, show, parse, format, read, write
   */
  public static String[] methodNames() {
    return METHODS.clone();
  }

  /**
   * Returns the package of each method, a node ID of {@link #dependencies()}, by node ID
   * of {@link #calls()}. Every call between two packages is a dependency between them.
   *
   * @return app for main and run, cli for parseArgs, ui for show, service for load and
   *         save, model for parse and format, storage for read and write
   */
  public static int[] packageOf() {
    int[] packageOf = new int[METHODS.length];
    for (String[] row : METHODS_OF_PACKAGE) {
      int pkg = nodeOf(PACKAGES, row[0]);
      for (int i = 1; i < row.length; i++) {
        packageOf[nodeOf(METHODS, row[i])] = pkg;
      }
    }
    return packageOf;
  }

  /**
   * Returns the control flow of the method {@code load} between its basic blocks.
   *
   * @return 6 nodes, 7 edges
   */
  public static Graph cfg() {
    return build(BLOCKS, FLOW);
  }

  /**
   * Returns the names of the basic blocks, by node ID of {@link #cfg()}.
   *
   * @return entry, header, body, then, next, exit
   */
  public static String[] blockNames() {
    return BLOCKS.clone();
  }

  /**
   * Returns the control flow of the method {@code show} between its basic blocks.
   *
   * @return 6 nodes, 7 edges
   */
  public static Graph cfgOfShow() {
    return build(SHOW_BLOCKS, SHOW_FLOW);
  }

  /**
   * Returns the names of the basic blocks of {@code show}, by node ID of
   * {@link #cfgOfShow()}.
   *
   * @return entry, nextNote, format, nextLine, print, exit
   */
  public static String[] blockNamesOfShow() {
    return SHOW_BLOCKS.clone();
  }

  /**
   * Returns the steps that build and publish the program: an edge {@code a -> b} if step
   * {@code b} may start only after step {@code a} has finished.
   *
   * @return 9 nodes, 10 edges
   */
  public static Graph buildSteps() {
    return build(STEPS, BEFORE);
  }

  /**
   * Returns the names of the build steps, by node ID of {@link #buildSteps()}.
   *
   * @return generate, compile, lint, compileTests, docs, test, package, sign, publish
   */
  public static String[] stepNames() {
    return STEPS.clone();
  }

  /**
   * Returns the control flow of the method {@code parseArgs} between its basic blocks.
   *
   * @return 4 nodes, 6 edges
   */
  public static Graph cfgOfParseArgs() {
    return build(PARSE_ARGS_BLOCKS, PARSE_ARGS_FLOW);
  }

  /**
   * Returns the names of the basic blocks of {@code parseArgs}, by node ID of
   * {@link #cfgOfParseArgs()}.
   *
   * @return entry, option, value, exit
   */
  public static String[] blockNamesOfParseArgs() {
    return PARSE_ARGS_BLOCKS.clone();
  }

  /**
   * Returns the node ID of the node named {@code name}.
   *
   * @param names the names of the nodes, by node ID
   * @param name  the name to look up
   * @return the node ID
   * @throws IllegalArgumentException if no node has this name
   */
  public static int nodeOf(String[] names, String name) {
    for (int node = 0; node < names.length; node++) {
      if (names[node].equals(name)) {
        return node;
      }
    }
    throw new IllegalArgumentException("no node is named " + name);
  }

  /**
   * A place in the source where one method calls another.
   *
   * @param caller the calling method, a node ID of {@link #calls()}
   * @param callee the called method, a node ID of {@link #calls()}
   */
  public record CallSite(int caller, int callee) {}

  private static Graph build(String[] names, String[][] rows) {
    var builder = new GraphBuilder();
    builder.addNodes(names.length);
    for (String[] row : rows) {
      int source = nodeOf(names, row[0]);
      for (int i = 1; i < row.length; i++) {
        builder.addEdge(source, nodeOf(names, row[i]));
      }
    }
    return builder.build();
  }
}
