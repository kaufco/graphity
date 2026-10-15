#!/usr/bin/env bash
# Graphity benchmark orchestrator (JSON-matrix driven, fail-fast validation).
#
# Usage:
#   ./run-benchmarks.sh <command> [args]
#   commands: clean | bench [version] | synthetic <lib|all> | realworld <lib|all>
#             | report [version] | ab <label> | compare <label-a> <label-b>
#   'bench [version]' runs synthetic → realworld → report in one go; the
#   individual commands do the same thing step by step.
#   See './run-benchmarks.sh --help' for the full workflow.
#
# Run from anywhere — script resolves paths relative to its own location.
#
# Build prerequisite (from repo root, run once after code changes):
#   mvn -am -pl graphity-benchmarks package -DskipTests
#
# ─────────────────────────────────────────────────────────────────────────────
# Methodology note — report runs versus A/B decisions
# ─────────────────────────────────────────────────────────────────────────────
# The report profiles (quick, full, big) measure with one fork and few iterations.
# That resolves the factors against the peer libraries, but not changes of a few
# percent: report sections whose code did not change deviate by 9–14 % at p90 and
# up to 28 % between runs, and at n ≥ 10⁶ GC pauses inside single iterations push
# confidence half-widths to 50–97 %.
#
# For a decision between two Graphity states use the A/B profile instead:
#
#   ./run-benchmarks.sh ab before --cases scc,toporder    # on the old state
#   ./run-benchmarks.sh ab after  --cases scc,toporder    # on the new state
#   ./run-benchmarks.sh compare before after
#
# benchmark-matrix/ab.json: Graphity only, PATH always among the shapes, 3 forks
# (averages the JIT lottery, the strongest single lever), 10 short iterations,
# -Xmx16g -XX:+UseZGC -XX:+AlwaysPreTouch against GC pauses. Each label lives in
# measurements/ab/<label>/ with the measured commit in meta.json; 'compare' prints
# means with half-widths and marks cases above 15 %. Methodology for any
# performance claim: internal/ops/benchmark-protokoll.md.
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCH_DIR="${SCRIPT_DIR}"
JAR="${BENCH_DIR}/target/graphity-benchmarks.jar"
MEASUREMENTS="${BENCH_DIR}/measurements"
MATRIX_DIR="${BENCH_DIR}/benchmark-matrix"

if [[ ! -f "$JAR" ]]; then
  echo "error: jar not found at $JAR" >&2
  echo "build it first (from repo root): mvn -am -pl graphity-benchmarks package -DskipTests" >&2
  exit 1
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "error: python3 not found (required for JSON matrix parsing)" >&2
  exit 1
fi

mkdir -p "$MEASUREMENTS"

banner() {
  local txt="$1"
  echo
  printf '═══ %s ' "$txt"
  printf '═%.0s' $(seq 1 $((70 - ${#txt})))
  echo
}

resolve_matrix_file() {
  local profile_or_path="$1"
  if [[ -f "$profile_or_path" ]]; then
    echo "$profile_or_path"
    return
  fi
  if [[ -f "${BENCH_DIR}/${profile_or_path}" ]]; then
    echo "${BENCH_DIR}/${profile_or_path}"
    return
  fi
  if [[ -f "${MATRIX_DIR}/${profile_or_path}.json" ]]; then
    echo "${MATRIX_DIR}/${profile_or_path}.json"
    return
  fi
  echo "error: cannot resolve matrix/profile '$profile_or_path'" >&2
  echo "tried: $profile_or_path, ${BENCH_DIR}/${profile_or_path}, ${MATRIX_DIR}/${profile_or_path}.json" >&2
  exit 2
}

matrix_profile_name() {
  local matrix_file="$1"
  python3 - "$matrix_file" <<'PY'
import json, pathlib, sys
path = pathlib.Path(sys.argv[1])
data = json.loads(path.read_text(encoding="utf-8"))
print(data.get("name") or path.stem)
PY
}

validate_matrix() {
  local matrix_file="$1"
  python3 - "$matrix_file" <<'PY'
import json, os, sys

# Shapes of ControlFlowFixture (ControlFlowShape), for the control-flow analyses only.
CONTROL_FLOW_SHAPES = {
    "STRUCTURED", "IRREDUCIBLE", "LADDER", "CHAIN", "FAN_BEHIND_CHAIN", "SMALL_METHODS"
}
ALL_SHAPES = {
    "PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
    "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
    "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED",
    "CHAIN_OF_CLIQUES_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"
} | CONTROL_FLOW_SHAPES
GRAPH_FIXTURE_SHAPES = {
    "PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
    "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED",
    "CHAIN_OF_CLIQUES_SHUFFLED"
}
DAG_FIXTURE_SHAPES = {
    "PATH", "TREE", "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
    "PATH_SHUFFLED", "TREE_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"
}
# "graphity-reordered" and "graphity-biadjacent" are pseudo-libraries: the same Graphity
# algorithm run on a transformed graph — a DFS-preorder reordered one (NodeMapping.reindex +
# Graphs.remapped) or a bi-adjacent one (Graph.biAdjacentGraph). Each shows up as a separate
# "Graphity (…)" column in the report. Alongside them sit the algorithms that measure the
# one-time cost of those structures: "reorder" and "biadjacency" for Graphity, "symmetrize" for
# WebGraph, whose ConnectedComponents needs a symmetric graph a BVGraph cannot provide.
SUPPORTED_LIBS = {"graphity", "graphity-reordered", "graphity-biadjacent", "jgrapht", "guava", "webgraph"}
SUPPORTED_ALGOS = {
    "build", "scc", "wcc", "ccs", "condense", "toporder", "toporderandlevels",
    "backedges", "edgetypes", "reorder", "biadjacency", "symmetrize",
    "dominators", "dominancefrontiers", "iterateddominancefrontier", "loops",
    "dominatorancestry", "preorderancestry"
}
# No peer offers dominators or loop nesting, so these are Graphity-only sections.
CONTROL_FLOW_ALGOS = {
    "dominators", "dominancefrontiers", "iterateddominancefrontier", "loops",
    "dominatorancestry", "preorderancestry"
}
ALGO_SUPPORTED_LIBS = {
    "build": {"graphity", "jgrapht", "guava", "webgraph"},
    "scc": {"graphity", "graphity-reordered", "jgrapht", "guava", "webgraph"},
    "toporder": {"graphity", "graphity-reordered", "jgrapht", "guava", "webgraph"},
    "backedges": {"graphity", "graphity-reordered", "jgrapht", "guava", "webgraph"},
    "edgetypes": {"graphity", "graphity-reordered", "jgrapht", "guava", "webgraph"},
    "wcc": {"graphity", "graphity-reordered", "jgrapht", "guava", "webgraph"},
    "ccs": {"graphity", "graphity-reordered"},
    "condense": {"graphity"},
    "toporderandlevels": {
        "graphity", "graphity-reordered", "graphity-biadjacent", "jgrapht", "guava", "webgraph"
    },
    "reorder": {"graphity"},
    "biadjacency": {"graphity"},
    "symmetrize": {"webgraph"},
    **{algo: {"graphity"} for algo in CONTROL_FLOW_ALGOS},
}
ALGO_ALLOWED_SHAPES = {
    **{algo: CONTROL_FLOW_SHAPES for algo in CONTROL_FLOW_ALGOS},
    "build": GRAPH_FIXTURE_SHAPES,
    "scc": GRAPH_FIXTURE_SHAPES,
    "backedges": GRAPH_FIXTURE_SHAPES,
    "edgetypes": GRAPH_FIXTURE_SHAPES,
    "toporder": DAG_FIXTURE_SHAPES,
    "wcc": GRAPH_FIXTURE_SHAPES,
    "ccs": GRAPH_FIXTURE_SHAPES,
    # CondenseFixture accepts cyclic and acyclic input alike: the group count, not
    # acyclicity, is the dimension that matters for condense().
    "condense": ALL_SHAPES,
    "toporderandlevels": DAG_FIXTURE_SHAPES,
    "reorder": GRAPH_FIXTURE_SHAPES,
    # In-adjacency and symmetric closure exist for cyclic graphs just as much as for acyclic
    # ones, so their construction cost is measured over the full shape set.
    "biadjacency": GRAPH_FIXTURE_SHAPES,
    "symmetrize": GRAPH_FIXTURE_SHAPES,
}
SUPPORTED_PHASES = {"perf", "alloc"}
DAG_ALGOS = {"toporder", "toporderandlevels"}

def fail(msg):
    raise SystemExit(f"matrix validation failed: {msg}")

path = sys.argv[1]
base_dir = os.path.dirname(os.path.abspath(path))
try:
    data = json.loads(open(path, encoding="utf-8").read())
except Exception as e:
    fail(f"cannot parse JSON '{path}': {e}")

def resolve_dataset_path(raw):
    expanded = os.path.expanduser(os.path.expandvars(raw))
    if "$" in expanded:
        fail(f"dataset path contains unresolved env var: {raw}")
    if not os.path.isabs(expanded):
        expanded = os.path.normpath(os.path.join(base_dir, expanded))
    return expanded

if not isinstance(data, dict):
    fail("top-level JSON must be an object")

jmh = data.get("jmh", {})
if not isinstance(jmh, dict):
    fail("'jmh' must be an object")
for key in ("forks", "warmup_iterations", "measurement_iterations"):
    v = jmh.get(key, 1)
    if not isinstance(v, int) or v < 0:
        fail(f"jmh.{key} must be a non-negative integer")
for key in ("warmup_time", "measurement_time", "jvm_stack"):
    v = jmh.get(key, "")
    if not isinstance(v, str) or not v.strip():
        fail(f"jmh.{key} must be a non-empty string")
heap = jmh.get("jvm_heap", "")
if heap != "" and (not isinstance(heap, str) or not heap.strip()):
    fail("jmh.jvm_heap must be a non-empty string when present")
extra = jmh.get("jvm_args", "")
if extra != "" and (not isinstance(extra, str) or not extra.strip()):
    fail("jmh.jvm_args must be a non-empty string when present")

cases = data.get("cases")
if not isinstance(cases, list) or not cases:
    fail("'cases' must be a non-empty array")

for i, c in enumerate(cases):
    if not isinstance(c, dict):
        fail(f"cases[{i}] must be an object")
    name = c.get("name")
    if not isinstance(name, str) or not name.strip():
        fail(f"cases[{i}].name must be a non-empty string")

    libs = c.get("libraries")
    if not isinstance(libs, list) or not libs:
        fail(f"cases[{i}].libraries must be a non-empty array")
    bad_libs = [x for x in libs if x not in SUPPORTED_LIBS]
    if bad_libs:
        fail(f"cases[{i}].libraries contains unsupported entries: {bad_libs}; supported={sorted(SUPPORTED_LIBS)}")

    algos = c.get("algorithms")
    if not isinstance(algos, list) or not algos:
        fail(f"cases[{i}].algorithms must be a non-empty array")
    bad_algos = [x for x in algos if x not in SUPPORTED_ALGOS]
    for algo in algos:
        unsupported = [lib for lib in libs if lib not in ALGO_SUPPORTED_LIBS[algo]]
        if unsupported:
            fail(f"cases[{i}] requests libraries {unsupported} for algo '{algo}', but supported are {sorted(ALGO_SUPPORTED_LIBS[algo])}")

    if bad_algos:
        fail(f"cases[{i}].algorithms contains unsupported entries: {bad_algos}; supported={sorted(SUPPORTED_ALGOS)}")

    phases = c.get("phases", ["perf", "alloc"])
    if not isinstance(phases, list) or not phases:
        fail(f"cases[{i}].phases must be a non-empty array if provided")
    bad_phases = [x for x in phases if x not in SUPPORTED_PHASES]
    if bad_phases:
        fail(f"cases[{i}].phases contains unsupported entries: {bad_phases}; supported={sorted(SUPPORTED_PHASES)}")

    params = c.get("params")
    dataset = c.get("dataset")
    if (params is None) == (dataset is None):
        fail(f"cases[{i}] must define exactly one of 'params' or 'dataset'")

    if params is not None:
        if not isinstance(params, dict):
            fail(f"cases[{i}].params must be an object")
        node_counts = params.get("nodeCount")
        if not isinstance(node_counts, list) or not node_counts:
            fail(f"cases[{i}].params.nodeCount must be a non-empty array")
        for n in node_counts:
            if not isinstance(n, int) or n <= 0:
                fail(f"cases[{i}].params.nodeCount contains invalid entry: {n}")

        shapes = params.get("shape")
        if not isinstance(shapes, list) or not shapes:
            fail(f"cases[{i}].params.shape must be a non-empty array")
        bad_shapes = [s for s in shapes if s not in ALL_SHAPES]
        if bad_shapes:
            fail(f"cases[{i}].params.shape contains unknown shapes: {bad_shapes}")
        for algo in algos:
            allowed = ALGO_ALLOWED_SHAPES[algo]
            wrong = [s for s in shapes if s not in allowed]
            if wrong:
                fail(f"cases[{i}] has unsupported shape(s) for algo '{algo}': {wrong}")
    else:
        if not isinstance(dataset, dict):
            fail(f"cases[{i}].dataset must be an object")
        if "build" in algos:
            fail(f"cases[{i}] dataset-mode does not support algorithm 'build'")
        files = dataset.get("files")
        if not isinstance(files, list) or not files:
            fail(f"cases[{i}].dataset.files must be a non-empty array")
        expanded_files = []
        for raw in files:
            if not isinstance(raw, str) or not raw.strip():
                fail(f"cases[{i}].dataset.files contains invalid path entry: {raw!r}")
            expanded = resolve_dataset_path(raw)
            if not os.path.isfile(expanded):
                fail(f"cases[{i}].dataset file does not exist: {raw} -> {expanded}")
            expanded_files.append(expanded)
        dag_flag = dataset.get("dag")
        if not isinstance(dag_flag, bool):
            fail(f"cases[{i}].dataset.dag must be a boolean")
        if any(a in DAG_ALGOS for a in algos) and not dag_flag:
            fail(f"cases[{i}] contains DAG-only algorithms {sorted(DAG_ALGOS.intersection(algos))} but dataset.dag=false")

mem = data.get("mem", {})
if not isinstance(mem, dict):
    fail("'mem' must be an object")
if bool(mem.get("enabled", True)):
    mem_nodes = mem.get("nodes")
    mem_shapes = mem.get("shapes")
    mem_libs = mem.get("libraries")
    if not isinstance(mem_nodes, list) or not mem_nodes:
        fail("mem.nodes must be a non-empty array when mem.enabled=true")
    for n in mem_nodes:
        if not isinstance(n, int) or n <= 0:
            fail(f"mem.nodes contains invalid entry: {n}")
    if not isinstance(mem_shapes, list) or not mem_shapes:
        fail("mem.shapes must be a non-empty array when mem.enabled=true")
    bad_mem_shapes = [s for s in mem_shapes if s not in ALL_SHAPES]
    if bad_mem_shapes:
        fail(f"mem.shapes contains unknown shapes: {bad_mem_shapes}")
    if not isinstance(mem_libs, list) or not mem_libs:
        fail("mem.libraries must be a non-empty array when mem.enabled=true")
    bad_mem_libs = [x for x in mem_libs if x not in SUPPORTED_LIBS]
    if bad_mem_libs:
        fail(f"mem.libraries contains unsupported entries: {bad_mem_libs}; supported={sorted(SUPPORTED_LIBS)}")
PY
}

matrix_jmh_defaults() {
  local matrix_file="$1"
  python3 - "$matrix_file" <<'PY'
import json, sys
data = json.loads(open(sys.argv[1], encoding="utf-8").read())
jmh = data.get("jmh", {})
forks = jmh.get("forks", 1)
wi = jmh.get("warmup_iterations", 2)
w = jmh.get("warmup_time", "500ms")
it = jmh.get("measurement_iterations", 5)
r = jmh.get("measurement_time", "500ms")
stack = jmh.get("jvm_stack", "-Xss16m")
heap = jmh.get("jvm_heap", "")
extra = jmh.get("jvm_args", "")
# Tab-separated for `read`; an empty field would collapse, so absent values become "-".
print(f"{forks}\t{wi}\t{w}\t{it}\t{r}\t{stack}\t{heap or '-'}\t{extra or '-'}")
PY
}

matrix_cases_tsv() {
  local matrix_file="$1"
  local phase="$2"
  local lib_filter="${3:-}"
  python3 - "$matrix_file" "$phase" "$lib_filter" <<'PY'
import json, os, re, sys
matrix_path = sys.argv[1]
data = json.loads(open(matrix_path, encoding="utf-8").read())
phase = sys.argv[2]
lib_filter = [x.strip() for x in sys.argv[3].split(",") if x.strip()] if len(sys.argv) > 3 else []
base_dir = os.path.dirname(os.path.abspath(matrix_path))

def resolve_dataset_path(raw):
    expanded = os.path.expanduser(os.path.expandvars(str(raw)))
    if not os.path.isabs(expanded):
        expanded = os.path.normpath(os.path.join(base_dir, expanded))
    return expanded

def slug(text):
    return re.sub(r"[^A-Za-z0-9.-]+", "-", str(text)).strip("-")

BENCHMARK_FQNS = {
    "build": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.BuildBenchmark.build",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTBuildBenchmark.build",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaBuildBenchmark.build",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphBuildBenchmark.build",
    },
    "scc": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.SccBenchmark.scc",
        "graphity-reordered": "com.kobayami.graphity.benchmarks.graphity.SccReorderedBenchmark.scc",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTSccBenchmark.scc",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaSccBenchmark.scc",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphSccBenchmark.scc",
    },
    "wcc": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.WccBenchmark.wcc",
        "graphity-reordered": "com.kobayami.graphity.benchmarks.graphity.WccReorderedBenchmark.wcc",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTWccBenchmark.wcc",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaWccBenchmark.wcc",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphWccBenchmark.wcc",
    },
    "ccs": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.CcsBenchmark.ccs",
        "graphity-reordered": "com.kobayami.graphity.benchmarks.graphity.CcsReorderedBenchmark.ccs",
    },
    "condense": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.CondenseBenchmark.condense",
    },
    "toporder": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.TopOrderBenchmark.topOrder",
        "graphity-reordered": "com.kobayami.graphity.benchmarks.graphity.TopOrderReorderedBenchmark.topOrder",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTTopOrderBenchmark.topOrder",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaTopOrderBenchmark.topOrder",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphTopOrderBenchmark.topOrder",
    },
    "toporderandlevels": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.TopOrderAndLevelsBenchmark.topOrderAndLevels",
        "graphity-reordered": "com.kobayami.graphity.benchmarks.graphity.TopOrderAndLevelsReorderedBenchmark.topOrderAndLevels",
        "graphity-biadjacent": "com.kobayami.graphity.benchmarks.graphity.TopOrderAndLevelsBiAdjacentBenchmark.topOrderAndLevels",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTTopOrderAndLevelsBenchmark.topOrderAndLevels",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaTopOrderAndLevelsBenchmark.topOrderAndLevels",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphTopOrderAndLevelsBenchmark.topOrderAndLevels",
    },
    "backedges": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.BackEdgesBenchmark.backEdges",
        "graphity-reordered": "com.kobayami.graphity.benchmarks.graphity.BackEdgesReorderedBenchmark.backEdges",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTBackEdgesBenchmark.backEdges",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaBackEdgesBenchmark.backEdges",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphBackEdgesBenchmark.backEdges",
    },
    "edgetypes": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.EdgeTypesBenchmark.edgeTypes",
        "graphity-reordered": "com.kobayami.graphity.benchmarks.graphity.EdgeTypesReorderedBenchmark.edgeTypes",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTEdgeTypesBenchmark.edgeTypes",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaEdgeTypesBenchmark.edgeTypes",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphEdgeTypesBenchmark.edgeTypes",
    },
    "reorder": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.ReorderBenchmark.reorder",
    },
    "biadjacency": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.BiAdjacencyBenchmark.biAdjacency",
    },
    "symmetrize": {
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphSymmetrizeBenchmark.symmetrize",
    },
    "dominators": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.DominatorsBenchmark.dominators",
    },
    "dominancefrontiers": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.DominanceFrontiersBenchmark.dominanceFrontiers",
    },
    "iterateddominancefrontier": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.IteratedDominanceFrontierBenchmark.iteratedDominanceFrontier",
    },
    "loops": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.LoopsBenchmark.loops",
    },
    "dominatorancestry": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.DominatorAncestryBenchmark.dominatorAncestry",
    },
    "preorderancestry": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.PreorderAncestryBenchmark.preorderAncestry",
    },
}

cases = data.get("cases", [])
for c in cases:
    phases = c.get("phases", ["perf", "alloc"])
    if phase not in phases:
        continue
    name = c["name"]
    libs = c["libraries"]
    if lib_filter:
        libs = [l for l in libs if l in lib_filter]
        if not libs:
            continue
    algos = c["algorithms"]
    if "params" in c:
        params = dict(c.get("params", {}))
    else:
        ds = c["dataset"]
        dataset_paths = [resolve_dataset_path(p) for p in ds.get("files", [])]
        params = {
            "inputMode": "dataset",
            "datasetPath": dataset_paths,
            # keep fixture param space singular in dataset mode (shape/nodeCount are ignored there)
            "nodeCount": [1000],
            "shape": ["PATH"],
        }
    # One TSV row per (case, algorithm) — libraries and param values stay
    # bundled into one JMH invocation via -p key=v1,v2,... exactly as before.
    # `case_key` is a stable, matrix-derived identifier (not a running
    # counter) so the same case always maps to the same output filename for a
    # given profile+library-filter. Actual resume no longer hinges on that
    # filename though: run_jmh_phase matches already-measured results by
    # (benchmark FQN, params) across ALL profiles (see compute_case_resume),
    # so a differently-named subset profile's data is reused too.
    # The libraries actually selected for this invocation (after --lib
    # filtering) are still part of the key so `realworld jgrapht` and
    # `realworld graphity` write to distinct files rather than clobbering
    # each other.
    libs_key = slug(",".join(sorted(libs)))
    for algo in algos:
        fqns = [BENCHMARK_FQNS[algo][lib] for lib in libs]
        escaped = [re.escape(x) for x in fqns]
        bench_regex = "^(?:" + "|".join(escaped) + ")$"
        case_key = f"{slug(name)}__{slug(algo)}__{libs_key}"
        # Field 4 (fqns) carries the exact benchmark FQNs this case expects, so
        # run_jmh_phase can compute cross-profile coverage per param-combo
        # (a combo is "done" only once every listed FQN has a result for it).
        fields = [f"{name}:{algo}", bench_regex, case_key, ",".join(fqns)]
        for k in sorted(params.keys()):
            v = params[k]
            if isinstance(v, list):
                vv = ",".join(str(x) for x in v)
            else:
                vv = str(v)
            fields.append(f"{k}={vv}")
        print("\t".join(fields))
PY
}

matrix_mem_tsv() {
  local matrix_file="$1"
  local lib_filter="${2:-}"
  python3 - "$matrix_file" "$lib_filter" <<'PY'
import json, sys
data = json.loads(open(sys.argv[1], encoding="utf-8").read())
lib_filter = [x.strip() for x in sys.argv[2].split(",") if x.strip()] if len(sys.argv) > 2 else []
mem = data.get("mem", {})
enabled = mem.get("enabled", True)
libs = list(mem.get("libraries", []))
if lib_filter:
    libs = [l for l in libs if l in lib_filter]
    if not libs:
        enabled = False
nodes = ",".join(str(x) for x in mem.get("nodes", []))
shapes = ",".join(str(x) for x in mem.get("shapes", []))
libraries = ",".join(str(x) for x in libs)
print(f"{str(bool(enabled)).lower()}\t{nodes}\t{shapes}\t{libraries}")
PY
}

matrix_validate_cases_tsv() {
  local matrix_file="$1"
  local phase="$2"
  python3 - "$matrix_file" "$phase" "${3:-}" <<'PY'
import json, os, sys
matrix_path = sys.argv[1]
data = json.loads(open(matrix_path, encoding="utf-8").read())
phase = sys.argv[2]
lib_filter = [x.strip() for x in sys.argv[3].split(",") if x.strip()] if len(sys.argv) > 3 else []
base_dir = os.path.dirname(os.path.abspath(matrix_path))
sep = "\x1f"

def resolve_dataset_path(raw):
    expanded = os.path.expanduser(os.path.expandvars(str(raw)))
    if not os.path.isabs(expanded):
        expanded = os.path.normpath(os.path.join(base_dir, expanded))
    return expanded
for c in data.get("cases", []):
    phases = c.get("phases", ["perf", "alloc"])
    if phase not in phases:
        continue
    name = c["name"]
    orig_libs = c["libraries"]
    if lib_filter:
        orig_libs = [l for l in orig_libs if l in lib_filter]
        if not orig_libs:
            continue
    # The pseudo-libraries run the identical Graphity algorithm on a transformed graph, so they
    # validate as plain "graphity" (dedup to avoid running it twice). The transforms themselves
    # are covered inside ValidateMain: the "toporderandlevels" case checks the bi-adjacent path
    # against the uni-adjacent one, and "wcc" checks WebGraph's symmetric closure against
    # Graphity's partition.
    mapped_libs = []
    for lib in orig_libs:
        canon = "graphity" if lib in ("graphity-reordered", "graphity-biadjacent") else lib
        if canon not in mapped_libs:
            mapped_libs.append(canon)
    libs = ",".join(mapped_libs)
    for algo in c["algorithms"]:
        if "params" in c:
            params = c["params"]
            nodes = ",".join(str(x) for x in params.get("nodeCount", []))
            shapes = ",".join(str(x) for x in params.get("shape", []))
            datasets = ""
        else:
            dataset = c["dataset"]
            nodes = ""
            shapes = ""
            datasets = ",".join(resolve_dataset_path(x) for x in dataset.get("files", []))
        print(sep.join([name, algo, libs, nodes, shapes, datasets]))
PY
}

run_validate_phase() {
  local matrix_file="$1"
  local phase="$2"
  IFS=$'\t' read -r _ _ _ _ _ jvm_stack jvm_heap _ <<< "$(matrix_jmh_defaults "$matrix_file")"
  [[ "$jvm_heap" == "-" ]] && jvm_heap=""
  banner "validate — semantic quick-fail (${phase})"
  local -a jbase=(java "$jvm_stack")
  [[ -n "$jvm_heap" ]] && jbase+=("$jvm_heap")
  jbase+=(-jar "$JAR" validate)
  local case_idx=0
  while IFS=$'\x1f' read -r case_name algo libs nodes shapes datasets; do
    [[ -z "$algo" ]] && continue
    if [[ -n "$CASE_FILTER" && ",${CASE_FILTER}," != *",${case_name},"* \
          && ",${CASE_FILTER}," != *",${case_name}:${algo},"* ]]; then
      continue
    fi
    case_idx=$((case_idx + 1))
    echo "[validate/$case_idx] case=${case_name} algo=${algo} libs=${libs}"
    if [[ -n "$datasets" ]]; then
      "${jbase[@]}" \
        --algorithm "$algo" \
        --libraries "$libs" \
        --datasets "$datasets"
    else
      "${jbase[@]}" \
        --algorithm "$algo" \
        --libraries "$libs" \
        --nodes "$nodes" \
        --shapes "$shapes"
    fi
  done < <(matrix_validate_cases_tsv "$matrix_file" "$phase" "$LIB_FILTER")
}

# library_versions -> JSON object: the version every library is measured at, plus the JDK and
# the measured state. Peers carry their Maven version from the parent POM; the Graphity variants
# carry the state from measured_state (commit, plus a hash of uncommitted and untracked changes).
# A cached result is reused only while its library's entry and the JDK still match
# (compute_case_resume). Changes to generators, fixtures or peer adapters do not show in a peer's
# version; after such a change, `clean --lib jgrapht,guava,webgraph` as well.
library_versions() {
  local state jdk
  state="$(measured_state)"
  jdk="$(java -XshowSettings:properties -version 2>&1 | awk -F'= ' '/^ *java\.version =/{print $2; exit}')"
  python3 - "$(cd "${BENCH_DIR}/.." && pwd)/pom.xml" "$state" "$jdk" <<'PY'
import json, re, sys
pom, state, jdk = sys.argv[1], json.loads(sys.argv[2]), sys.argv[3]
text = open(pom, encoding="utf-8").read()
def prop(name):
    m = re.search(rf"<{re.escape(name)}>([^<]+)</{re.escape(name)}>", text)
    return m.group(1).strip() if m else "unknown"
graphity = "git:" + state["revision"]
print(json.dumps({
    "graphity": graphity,
    "graphity-reordered": graphity,
    "graphity-biadjacent": graphity,
    "jgrapht": prop("jgrapht.version"),
    "guava": prop("guava.version"),
    "webgraph": prop("webgraph.version"),
    "jdk": jdk,
    "revision": state["revision"],
    "subject": state["subject"],
}))
PY
}

# Adds a "measuredAt" (ISO 8601, UTC) field and the "libraryVersion" of the measured library
# to every entry of a JMH JSON result array, in place. The report tooling (ReportMain.java's hand-rolled
# JSON parser) reads results into a generic map and only looks up the keys it
# knows about, so an extra field is silently ignored there — this is purely
# provenance data for anyone inspecting the raw JSON later, since the
# filename itself no longer carries a timestamp.
stamp_measured_at() {
  local json_file="$1"
  python3 - "$json_file" "$LIBRARY_VERSIONS" <<'PY'
import datetime, json, sys
path = sys.argv[1]
versions = json.loads(sys.argv[2])
def lib_of(fqn):
    # com.kobayami.graphity.benchmarks.<package>.<Class>.<method>
    parts = fqn.split(".")
    if len(parts) < 3:
        return ""
    pkg, cls = parts[-3], parts[-2]
    if pkg == "graphity":
        if cls.endswith("ReorderedBenchmark"):
            return "graphity-reordered"
        if cls.endswith("BiAdjacentBenchmark"):
            return "graphity-biadjacent"
        return "graphity"
    return pkg
with open(path, encoding="utf-8") as f:
    data = json.load(f)
now = datetime.datetime.now(datetime.timezone.utc).isoformat()
for entry in data:
    entry["measuredAt"] = now
    entry["libraryVersion"] = versions.get(lib_of(entry.get("benchmark", "")), "unknown")
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2)
PY
}

# compute_case_resume <reuse_dest.json> <phase> <fqns_csv> <param_spec...>
#
# Cross-profile resume for a single case. Scans EVERY `<phase>-*@*.json` in
# measurements/ (not just this profile's file) and decides, per param-combo
# (the JMH cross-product of the case's `-p key=v1,v2,...` values — one combo is
# one dataset in dataset mode, or one (shape,nodeCount) pair in fixture mode),
# whether it is already fully measured. A combo counts as done only once EVERY
# expected benchmark FQN of the case has a result for it, so a partially-run
# combo is re-measured rather than silently accepted.
#
# Writes the reusable entries (every current entry of an expected FQN, also in combos
# that are not complete) as a JSON array to <reuse_dest>. Prints to stdout:
#   STATS\t<total_combos>\t<run_combos>\t<reused_combos>
#   PARAM\t<key=csv>            (one per param key; ONLY the values still to run)
#   REGEX\t<regex>              (only the FQNs that miss a result in some combo)
# When <run_combos> is 0 there are no PARAM lines and the caller skips JMH and
# just materializes the reuse array as the case output.
compute_case_resume() {
  local reuse_dest="$1" phase="$2" fqns_csv="$3"; shift 3
  python3 - "$reuse_dest" "$MEASUREMENTS" "$phase" "$fqns_csv" "$LIBRARY_VERSIONS" "$@" <<'PY'
import glob, itertools, json, os, sys

reuse_dest = sys.argv[1]
results_dir = sys.argv[2]
phase = sys.argv[3]
fqns = [x for x in sys.argv[4].split(",") if x]
versions = json.loads(sys.argv[5])
specs = sys.argv[6:]
def lib_of(fqn):
    # com.kobayami.graphity.benchmarks.<package>.<Class>.<method>
    parts = fqn.split(".")
    if len(parts) < 3:
        return ""
    pkg, cls = parts[-3], parts[-2]
    if pkg == "graphity":
        if cls.endswith("ReorderedBenchmark"):
            return "graphity-reordered"
        if cls.endswith("BiAdjacentBenchmark"):
            return "graphity-biadjacent"
        return "graphity"
    return pkg

def current(entry):
    """A cached entry counts only if it was measured at today's library version and JDK. Without
    git the Graphity state is unknown, and an unknown state is never current."""
    lib = lib_of(entry.get("benchmark", ""))
    version = versions.get(lib)
    return (entry.get("libraryVersion") == version and "unknown-git" not in (version or "")
            and entry.get("jdkVersion") == versions.get("jdk"))

# Ordered param space: [(key, [values...]), ...] exactly as passed to `-p`.
param_space = []
for spec in specs:
    if "=" not in spec:
        continue
    key, raw = spec.split("=", 1)
    param_space.append((key, raw.split(",") if raw != "" else [""]))
keys = [k for k, _ in param_space]

# All combos in this case's param cross-product.
combos = [dict(zip(keys, vals)) for vals in itertools.product(*[v for _, v in param_space])] if param_space else [{}]

# Index existing results across ALL profiles for this phase.
# present[combo_key] -> set of benchmark FQNs; stored[(fqn, combo_key)] -> entry.
present, stored = {}, {}
for path in sorted(glob.glob(os.path.join(results_dir, f"{phase}-*@*.json")), key=os.path.getmtime):
    try:
        data = json.load(open(path, encoding="utf-8"))
    except Exception:
        continue
    if not isinstance(data, list):
        continue
    for entry in data:
        bm = entry.get("benchmark", "")
        p = entry.get("params") or {}
        if not all(k in p for k in keys) or not current(entry):
            continue
        combo_key = tuple((k, str(p[k])) for k in keys)
        present.setdefault(combo_key, set()).add(bm)
        stored[(bm, combo_key)] = entry  # mtime-sorted glob → newest wins

def combo_key_of(combo):
    return tuple((k, combo[k]) for k in keys)

covered, uncovered = [], []
for combo in combos:
    ck = combo_key_of(combo)
    have = present.get(ck, set())
    (covered if all(fqn in have for fqn in fqns) else uncovered).append(combo)

# Values still needed per key = those appearing in any uncovered combo.
run_values = {k: set() for k in keys}
for combo in uncovered:
    for k in keys:
        run_values[k].add(combo[k])

# Reuse every current entry, also from incomplete combos: when only Graphity is stale, the peer
# results of the same combos stay, and only the FQNs that miss a result are run (REGEX).
reuse_entries, missing_fqns = [], set()
for combo in combos:
    ck = combo_key_of(combo)
    for fqn in fqns:
        e = stored.get((fqn, ck))
        if e is not None:
            reuse_entries.append(e)
        else:
            missing_fqns.add(fqn)
json.dump(reuse_entries, open(reuse_dest, "w", encoding="utf-8"), indent=2)

if uncovered:
    # Keep original per-key value order, drop values no uncovered combo needs.
    filtered = [[v for v in vals if v in run_values[k]] for k, vals in param_space]
    run_combos = 1
    for f in filtered:
        run_combos *= len(f)
else:
    filtered = []
    run_combos = 0

total = len(combos)
reused = total - run_combos
print(f"STATS\t{total}\t{run_combos}\t{reused}")
if run_combos > 0:
    for (k, _), fvals in zip(param_space, filtered):
        print(f"PARAM\t{k}={','.join(fvals)}")
    import re
    run_fqns = [f for f in fqns if f in missing_fqns]
    print("REGEX\t^(?:" + "|".join(re.escape(f) for f in run_fqns) + ")$")
PY
}

# merge_case_result <dest.json> <base.json> [<overlay.json>]
# Combines two JMH result arrays, deduplicating by (benchmark, params); entries
# from <overlay> win over <base>. Used to fold freshly measured combos on top of
# reused ones so the case output is complete and self-contained.
merge_case_result() {
  local dest="$1" base="$2" overlay="${3:-}"
  python3 - "$dest" "$base" "$overlay" <<'PY'
import json, sys
dest, base, overlay = sys.argv[1], sys.argv[2], sys.argv[3]
merged, order = {}, []
for path in (base, overlay):
    if not path or not __import__("os").path.isfile(path):
        continue
    try:
        data = json.load(open(path, encoding="utf-8"))
    except Exception:
        continue
    if not isinstance(data, list):
        continue
    for entry in data:
        params = entry.get("params") or {}
        key = (entry.get("benchmark", ""), tuple(sorted((str(k), str(v)) for k, v in params.items())))
        if key not in merged:
            order.append(key)
        merged[key] = entry
json.dump([merged[k] for k in order], open(dest, "w", encoding="utf-8"), indent=2)
PY
}

run_jmh_phase() {
  local phase="$1" matrix_file="$2" profile="$3"
  # One JSON file per matrix case, written directly into measurements/ — the
  # same granularity as before this feature existed (one JMH invocation
  # covering all of a case's libraries/shapes/nodeCounts via
  # `-p key=v1,v2,...`). Filename: `<phase>-<profile>@<case_key>.json` — no
  # timestamp, no hash: it must be the SAME name on every invocation of the
  # same command, so a rerun of the same profile lands on the same file.
  #
  # Resume is CROSS-PROFILE and per param-combo, not per file. Before running a
  # case, compute_case_resume scans EVERY `<phase>-*@*.json` (all profiles) and
  # matches results by (benchmark FQN, params) — the same key `report` merges
  # on. A combo (one dataset in dataset mode, or one (shape,nodeCount) pair in
  # fixture mode) is "done" only once every FQN the case expects has a result
  # for it; JMH is then invoked with `-p` filtered to the combos still missing,
  # and the fresh results are merged on top of the reused ones (fresh wins) into
  # this profile's case file — so it is always complete and self-contained even
  # when part of the data came from another profile (e.g. a narrower subset
  # profile measured earlier). If nothing is missing the case is materialized
  # from the reused entries with no JMH run at all.
  #
  # A cached combo is reused only while every entry was measured at the current
  # library version and JDK (library_versions): a changed Graphity commit or
  # working tree re-measures the Graphity columns and keeps the peers. If you
  # change the matrix's `jmh` block (forks/warmup/measurement/stack/heap)
  # between two runs, `clean` first — nothing detects that the settings changed.
  local case_idx=0 n_run=0 n_cached=0
  [[ -z "$LIBRARY_VERSIONS" ]] && LIBRARY_VERSIONS="$(library_versions)"

  IFS=$'\t' read -r forks wi w it r jvm_stack jvm_heap jvm_args <<< "$(matrix_jmh_defaults "$matrix_file")"
  [[ "$jvm_heap" == "-" ]] && jvm_heap=""
  [[ "$jvm_args" == "-" ]] && jvm_args=""

  while IFS=$'\t' read -r -a fields; do
    [[ ${#fields[@]} -lt 4 ]] && continue
    local case_name="${fields[0]}"
    # case_name is "<case>:<algorithm>"; --cases accepts either form.
    if [[ -n "$CASE_FILTER" && ",${CASE_FILTER}," != *",${case_name%%:*},"* \
          && ",${CASE_FILTER}," != *",${case_name},"* ]]; then
      continue
    fi
    case_idx=$((case_idx + 1))
    local bench_regex="${fields[1]}"
    local case_key="${fields[2]}"
    local fqns_csv="${fields[3]}"
    local case_out="${MEASUREMENTS}/${phase}-${profile}@${case_key}.json"
    local case_log="${case_out%.json}.log"

    # Cross-profile resume: ask which param-combos of this case are already
    # measured (in ANY profile's JSON) and which still need to run. Reused
    # entries are captured so the case output stays complete and self-contained.
    local reuse_json="${case_out}.reuse.$$"
    local -a run_params=()
    local total_combos=0 run_combos=0 reused_combos=0 run_regex=""
    while IFS=$'\t' read -r tag rest; do
      case "$tag" in
        STATS) IFS=$'\t' read -r total_combos run_combos reused_combos <<< "$rest" ;;
        PARAM) run_params+=("$rest") ;;
        REGEX) run_regex="$rest" ;;
      esac
    done < <(compute_case_resume "$reuse_json" "$phase" "$fqns_csv" "${fields[@]:4}")

    if [[ "$total_combos" -eq 0 ]]; then
      rm -f "$reuse_json"
      echo "[$phase/$case_idx] FAILED: case=${case_name} — resume planning produced no combos (see stderr)" | tee "$case_log"
      FAILED_MEASUREMENTS+=("${phase}/${profile}/${case_name}")
      continue
    fi

    if [[ "$run_combos" -eq 0 ]]; then
      # Everything this case needs is already measured elsewhere — materialize
      # the reused entries under this profile's name; no JMH run.
      merge_case_result "$case_out" "$reuse_json"
      rm -f "$reuse_json"
      echo "[$phase/$case_idx] case=${case_name} — all ${total_combos} combo(s) reused from measurements/, skipping"
      n_cached=$((n_cached + 1))
      continue
    fi

    echo "[$phase/$case_idx] case=${case_name} — ${run_combos} combo(s) to run, ${reused_combos} reused" | tee "$case_log"
    rm -f /tmp/jmh.lock

    local -a cmd=(java "$jvm_stack")
    [[ -n "$jvm_heap" ]] && cmd+=("$jvm_heap")
    # jvm_args holds several flags separated by spaces; split them deliberately.
    # shellcheck disable=SC2206
    [[ -n "$jvm_args" ]] && cmd+=($jvm_args)
    cmd+=(-jar "$JAR" -f "$forks" -wi "$wi" -w "$w" -i "$it" -r "$r")
    if [[ "$phase" == "alloc" ]]; then
      cmd+=(-prof gc)
    fi
    local p
    for p in "${run_params[@]}"; do
      cmd+=(-p "$p")
    done
    local fork_args="$jvm_stack"
    [[ -n "$jvm_heap" ]] && fork_args="$fork_args $jvm_heap"
    [[ -n "$jvm_args" ]] && fork_args="$fork_args $jvm_args"
    local case_tmp="${case_out}.tmp.$$"
    cmd+=("${run_regex:-$bench_regex}" -jvmArgsAppend "$fork_args" -rf json -rff "$case_tmp")

    # Guarded call (not a bare pipeline statement): a crashing/OOM-killed
    # library must not abort the whole script via `set -e` — it should only
    # mark this one case as failed and let the rest keep running.
    if "${cmd[@]}" 2>&1 | tee -a "$case_log"; then
      if [[ -f "$case_tmp" ]]; then
        stamp_measured_at "$case_tmp"
        # Fold freshly measured combos on top of the reused ones (fresh wins).
        merge_case_result "$case_out" "$reuse_json" "$case_tmp"
        rm -f "$case_tmp"
        n_run=$((n_run + 1))
      else
        echo "[$phase/$case_idx] WARNING: case=${case_name} exited 0 but wrote no result — treating as failed" | tee -a "$case_log"
        FAILED_MEASUREMENTS+=("${phase}/${profile}/${case_name}")
      fi
    else
      rm -f "$case_tmp"
      echo "[$phase/$case_idx] FAILED: case=${case_name} (see ${case_log})" | tee -a "$case_log"
      FAILED_MEASUREMENTS+=("${phase}/${profile}/${case_name}")
    fi
    rm -f "$reuse_json"
  done < <(matrix_cases_tsv "$matrix_file" "$phase" "$LIB_FILTER")

  if [[ $((n_run + n_cached)) -eq 0 ]]; then
    echo "skip: no cases declare phase '$phase' in matrix $matrix_file (nothing to run)"
    return 0
  fi
  echo "$phase (${profile}): ${n_run} run, ${n_cached} already done → ${MEASUREMENTS}/${phase}-${profile}@*.json"
}

run_perf() {
  local matrix_file="$1" profile="$2"
  run_validate_phase "$matrix_file" "perf"
  banner "perf — ${profile}"
  run_jmh_phase "perf" "$matrix_file" "$profile"
}

run_alloc() {
  local matrix_file="$1" profile="$2"
  run_validate_phase "$matrix_file" "alloc"
  banner "alloc — ${profile} (with -prof gc)"
  run_jmh_phase "alloc" "$matrix_file" "$profile"
}

run_mem() {
  local matrix_file="$1" profile="$2"
  IFS=$'\t' read -r mem_enabled mem_nodes mem_shapes mem_libraries <<< "$(matrix_mem_tsv "$matrix_file" "$LIB_FILTER")"
  IFS=$'\t' read -r _ _ _ _ _ _ jvm_heap _ <<< "$(matrix_jmh_defaults "$matrix_file")"
  [[ "$jvm_heap" == "-" ]] && jvm_heap=""

  banner "mem — ${profile}"
  if [[ "$mem_enabled" != "true" ]]; then
    echo "mem disabled by matrix (${matrix_file})"
    return
  fi

  # Same idea as run_jmh_phase: a stable filename directly in measurements/,
  # written atomically. The mem CLI's own -o file is flushed row-by-row but
  # TRUNCATED on every invocation, so pointing two separate attempts at the
  # same path would destroy a crashed run's partial progress — write once,
  # atomically, and skip if the final file already exists. Keyed by the
  # actually-selected libraries so `synthetic graphity` and a later
  # `synthetic jgrapht` don't collide on the same file.
  local libs_key="${mem_libraries//,/-}"
  [[ -z "$libs_key" ]] && libs_key="none"
  local out="${MEASUREMENTS}/mem-${profile}@${libs_key}.csv"

  # A CSV is reused only while the versions it was measured at (written next to it) still match.
  [[ -z "$LIBRARY_VERSIONS" ]] && LIBRARY_VERSIONS="$(library_versions)"
  local versions_now
  versions_now="$(python3 -c 'import json, sys; v = json.loads(sys.argv[1]); print(json.dumps({l: v.get(l) for l in sys.argv[2].split(",") if l} | {"jdk": v.get("jdk")}, sort_keys=True))' "$LIBRARY_VERSIONS" "$mem_libraries")"
  if [[ -f "$out" && -f "${out}.versions" && "$versions_now" != *unknown-git* \
        && "$(cat "${out}.versions")" == "$versions_now" ]]; then
    echo "mem — already in measurements/ at the current versions, skipping (${out})"
    return
  fi
  rm -f "$out" "${out}.versions"

  local -a mem_args=(mem -o "${out}.tmp.$$")
  [[ -n "$mem_nodes" ]] && mem_args+=(-n "$mem_nodes")
  [[ -n "$mem_shapes" ]] && mem_args+=(-s "$mem_shapes")
  [[ -n "$mem_libraries" ]] && mem_args+=(-l "$mem_libraries")

  local -a mem_cmd=(java)
  [[ -n "$jvm_heap" ]] && mem_cmd+=("$jvm_heap")
  mem_cmd+=(-jar "$JAR" "${mem_args[@]}")

  if "${mem_cmd[@]}"; then
    mv -f "${out}.tmp.$$" "$out"
    printf '%s\n' "$versions_now" > "${out}.versions"
    echo "mem CSV → $out"
  else
    rm -f "${out}.tmp.$$"
    echo "mem FAILED (${profile})" >&2
    FAILED_MEASUREMENTS+=("mem/${profile}/${libs_key}")
  fi
}

# merge_json_across_profiles <phase> <dest.json> <profile1> [<profile2> ...]
# Merges JMH JSON arrays from multiple profiles into one, deduplicating by benchmark+params.
merge_json_across_profiles() {
  local phase="$1" dest="$2"
  shift 2
  python3 - "$dest" "$MEASUREMENTS" "$phase" "$@" <<'PY'
import glob, json, os, sys
dest = sys.argv[1]; results = sys.argv[2]; phase = sys.argv[3]; profiles = sys.argv[4:]
merged, order = {}, []
for profile in profiles:
    for path in sorted(glob.glob(os.path.join(results, f"{phase}-{profile}@*.json")), key=os.path.getmtime):
        try:
            data = json.load(open(path, encoding="utf-8"))
        except Exception:
            continue
        if not isinstance(data, list):
            continue
        for entry in data:
            params = entry.get("params") or {}
            key = (entry.get("benchmark",""), tuple(sorted((str(k), str(v)) for k,v in params.items())))
            if key not in merged:
                order.append(key)
            merged[key] = entry
if not order:
    sys.exit(1)
json.dump([merged[k] for k in order], open(dest, "w", encoding="utf-8"), indent=2)
print(f"{len(order)} benchmarks from profiles: {profiles}", file=sys.stderr)
PY
}

# merge_mem_csv_across_profiles <dest.csv> <profile1> [<profile2> ...]
# Concatenates JOL CSV files from multiple profiles (header from first file only).
merge_mem_csv_across_profiles() {
  local dest="$1"; shift
  local header_done=0
  for p in "$@"; do
    local f
    f=$(ls -t "${MEASUREMENTS}"/mem-"${p}"@*.csv 2>/dev/null | head -1 || true)
    [[ -z "$f" ]] && continue
    if [[ $header_done -eq 0 ]]; then
      cat "$f" > "$dest"
      header_done=1
    else
      tail -n +2 "$f" >> "$dest"
    fi
  done
  [[ $header_done -eq 1 ]]  # return true if at least one file found
}

# run_report_merged <kind> <out.md> <profile1> [<profile2> ...]
# Generates one unified report MD merging JSON from all given profiles.
run_report_merged() {
  local kind="$1" out="$2"
  shift 2
  local profiles=("$@")
  local merge_dir
  merge_dir=$(mktemp -d)
  local report_args=(report --profile "$kind" --out "$out")

  if merge_json_across_profiles perf "${merge_dir}/perf.json" "${profiles[@]}" 2>/dev/null; then
    report_args+=(--perf "${merge_dir}/perf.json")
    echo "  perf: merged profiles (${profiles[*]})"
  fi
  if merge_json_across_profiles alloc "${merge_dir}/alloc.json" "${profiles[@]}" 2>/dev/null; then
    report_args+=(--alloc "${merge_dir}/alloc.json")
    echo "  alloc: merged profiles (${profiles[*]})"
  fi
  if merge_mem_csv_across_profiles "${merge_dir}/mem.csv" "${profiles[@]}"; then
    report_args+=(--mem "${merge_dir}/mem.csv")
    echo "  mem: merged profiles (${profiles[*]})"
  fi

  java -jar "$JAR" "${report_args[@]}"
  rm -rf "$merge_dir"
}

# ─────────────────────────────────────────────────────────────────────────────
# High-level, task-oriented commands (clean → synthetic/realworld → report)
# ─────────────────────────────────────────────────────────────────────────────

OPS_SCRIPT="${BENCH_DIR}/times-to-ops.py"
REPORTS_DIR="${BENCH_DIR}/reports"
DEFAULT_SYNTHETIC_PROFILE="full"
DEFAULT_REALWORLD_PROFILE="realworld"
SUPPORTED_LIBS=" graphity graphity-reordered graphity-biadjacent jgrapht guava webgraph "
# LIB_FILTER is consumed by the engine functions (matrix_*_tsv); set per run below.
LIB_FILTER=""
# Accumulates "phase/profile/case" labels for any measurement that failed
# during this invocation (see run_jmh_phase / run_mem). Checked once at the
# very end of the script so a crashed/failed measurement never aborts the
# rest of the run via `set -e` — it's just reported, and re-running the same
# command retries only what's still missing.
FAILED_MEASUREMENTS=()
# Accumulates whole phases that were skipped because a precondition was missing
# (currently only: real-world datasets not downloaded). Reported in its own
# banner at the very end so a multi-hour `bench` run cannot silently produce a
# report that covers less than expected.
SKIPPED_PHASES=()

# Extract the project version from the parent pom.xml (strips -SNAPSHOT suffix).
read_project_version() {
  python3 - "${SCRIPT_DIR}/../pom.xml" <<'PY'
import re, sys
try:
    text = open(sys.argv[1], encoding="utf-8").read()
except FileNotFoundError:
    raise SystemExit(f"error: pom.xml not found at {sys.argv[1]}")
m = re.search(r'<version>([^<]+)</version>', text)
if not m:
    raise SystemExit("error: could not find <version> in pom.xml")
v = m.group(1).strip().removesuffix("-SNAPSHOT")
print(v)
PY
}

cmd_clean() {
  if [[ -n "$CLEAN_LIBS" ]]; then
    cmd_clean_libs "$CLEAN_LIBS"
    return
  fi
  banner "clean — removing run artifacts from measurements/"
  local n=0 f
  shopt -s nullglob
  for f in "${MEASUREMENTS}"/perf-*.json "${MEASUREMENTS}"/alloc-*.json "${MEASUREMENTS}"/mem-*.csv \
           "${MEASUREMENTS}"/mem-*.csv.versions \
           "${MEASUREMENTS}"/report-*.md "${MEASUREMENTS}"/*.log; do
    rm -f "$f"
    n=$((n + 1))
  done
  shopt -u nullglob
  echo "removed ${n} file(s) from ${MEASUREMENTS}/ (this is also the resumable-run cache — next run starts fully fresh)"
}

# cmd_clean_libs <lib,lib,...>: drops only the results of the named libraries from the JSON
# files of measurements/ and keeps everything else as resume cache. Memory CSVs are per run
# rather than per entry (run_mem skips an existing file), so a CSV with a row of a named library
# is removed as a whole; memory measurement takes minutes, not hours.
cmd_clean_libs() {
  local libs="$1"
  banner "clean — removing results of [${libs}] from measurements/, keeping the rest"
  python3 - "$MEASUREMENTS" "$libs" <<'PY'
import csv, glob, json, os, sys
results_dir, libs = sys.argv[1], set(x.strip() for x in sys.argv[2].split(",") if x.strip())
def lib_of(fqn):
    # com.kobayami.graphity.benchmarks.<package>.<Class>.<method>
    parts = fqn.split(".")
    if len(parts) < 3:
        return ""
    pkg, cls = parts[-3], parts[-2]
    if pkg == "graphity":
        if cls.endswith("ReorderedBenchmark"):
            return "graphity-reordered"
        if cls.endswith("BiAdjacentBenchmark"):
            return "graphity-biadjacent"
        return "graphity"
    return pkg
entries_removed = files_removed = 0
for path in sorted(glob.glob(os.path.join(results_dir, "perf-*.json"))
                   + glob.glob(os.path.join(results_dir, "alloc-*.json"))):
    try:
        data = json.load(open(path, encoding="utf-8"))
    except Exception:
        continue
    kept = [e for e in data if lib_of(e.get("benchmark", "")) not in libs]
    if len(kept) == len(data):
        continue
    entries_removed += len(data) - len(kept)
    if kept:
        json.dump(kept, open(path, "w", encoding="utf-8"), indent=2)
    else:
        os.remove(path)
        files_removed += 1
for path in sorted(glob.glob(os.path.join(results_dir, "mem-*.csv"))):
    with open(path, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    if any(r.get("library") in libs for r in rows):
        os.remove(path)
        if os.path.isfile(path + ".versions"):
            os.remove(path + ".versions")
        files_removed += 1
for path in glob.glob(os.path.join(results_dir, "report-*.md")):
    os.remove(path)
    files_removed += 1
print(f"removed {entries_removed} result(s) and {files_removed} file(s); the rest stays as resume cache")
PY
}

# run_kind <synthetic|realworld> <lib|all> <profile>
run_kind() {
  local kind="$1" lib="$2" profile="$3"
  if [[ -z "$lib" ]]; then
    echo "error: '$kind' requires a library name or 'all'" >&2
    echo "supported: graphity, graphity-reordered, graphity-biadjacent, jgrapht, guava, webgraph, all" >&2
    exit 2
  fi
  if [[ "$lib" == "all" ]]; then
    LIB_FILTER=""
  elif [[ "$SUPPORTED_LIBS" == *" $lib "* ]]; then
    LIB_FILTER="$lib"
    echo "library filter active: only [${lib}]"
  else
    echo "error: unknown library '$lib'" >&2
    echo "supported: graphity, graphity-reordered, graphity-biadjacent, jgrapht, guava, webgraph, all" >&2
    exit 2
  fi

  local matrix_file profile_name
  matrix_file="$(resolve_matrix_file "$profile")"
  validate_matrix "$matrix_file"
  check_case_filter "$matrix_file"
  snapshot_state
  profile_name="$(matrix_profile_name "$matrix_file")"

  banner "${kind} — profile '${profile_name}', libraries [${lib}]"
  run_perf  "$matrix_file" "$profile_name"
  run_alloc "$matrix_file" "$profile_name"
  run_mem   "$matrix_file" "$profile_name"
  echo
  echo "→ JSON written to ${MEASUREMENTS}/ (profile '${profile_name}')."
  echo "  Repeat per library as needed, then:  $(basename "$0") report"
}

list_result_profiles() {
  python3 - "$MEASUREMENTS" <<'PY'
import os, re, sys
d = sys.argv[1]
if not os.path.isdir(d):
    raise SystemExit(0)
pat = re.compile(r"^perf-(.+)@.+\.json$")
seen = []
for f in sorted(os.listdir(d)):
    m = pat.match(f)
    if m and m.group(1) not in seen:
        seen.append(m.group(1))
print("\n".join(seen))
PY
}

# report_meta <report.md> [machine-override] -> prints "<kind>\t<machine>"
report_meta() {
  python3 - "$1" "${2:-}" <<'PY'
import re, sys
text = open(sys.argv[1], encoding="utf-8").read()
override = sys.argv[2] if len(sys.argv) > 2 else ""

def field(name):
    m = re.search(rf"^{name}:\s*(.+)$", text, re.MULTILINE)
    return m.group(1).strip() if m else ""

profile = field("Profile")
kind = "realworld" if profile.lower().startswith(("dataset", "realworld")) else "synthetic"
if override:
    machine = override
else:
    os_raw = field("OS")
    os_first = (re.split(r"[\s,]+", os_raw)[0] or "unknown").lower()
    # "Mac OS X ..." → first token is "mac"; normalise to "macos"
    os_tok = "macos" if os_first == "mac" else os_first
    cpu = field("CPU").lower()
    if "ryzen" in cpu:
        m = re.search(r"([0-9]{4}[a-z]?)", cpu)
        cpu_tok = "ryzen-" + (m.group(1) if m else "cpu")
    elif re.search(r"\bm[0-9]", cpu) or "apple" in cpu:
        m = re.search(r"\b(m[0-9][a-z]*)", cpu)
        cpu_tok = m.group(1) if m else "apple"
    elif "aarch64" in os_raw.lower() or "arm" in cpu:
        # Apple Silicon: JVM may report only core count, not chip name
        cpu_tok = "apple"
    else:
        m = re.search(r"([a-z][a-z0-9]*)", cpu)  # letter-first to skip bare numbers
        cpu_tok = m.group(1) if m else "cpu"
    machine = f"{os_tok}-{cpu_tok}"
print(f"{kind}\t{machine}")
PY
}

cmd_report() {
  local version="${1:-}"
  local machine_override="${2:-}"

  local synth_promoted=0 real_promoted=0

  # Classify which profiles are in measurements/ and group by kind.
  local -a profiles=()
  while IFS= read -r line; do [[ -n "$line" ]] && profiles+=("$line"); done < <(list_result_profiles)

  if [[ ${#profiles[@]} -gt 0 ]]; then
    # Split profiles into synthetic vs realworld.
    local -a synth_profiles=() rw_profiles=()
    for p in "${profiles[@]}"; do
      if [[ "$p" == realworld* || "$p" == dataset* ]]; then
        rw_profiles+=("$p")
      else
        synth_profiles+=("$p")
      fi
    done

    # Step 1 – generate one merged report per kind (synthetic = full+big+…; realworld = realworld+…)
    banner "report — merging JSON artifacts into Markdown"
    local synth_md="" rw_md=""
    if [[ ${#synth_profiles[@]} -gt 0 ]]; then
      synth_md="${MEASUREMENTS}/report-synthetic-${ts}.md"
      echo "  synthetic profiles: ${synth_profiles[*]}"
      run_report_merged synthetic "$synth_md" "${synth_profiles[@]}"
    fi
    if [[ ${#rw_profiles[@]} -gt 0 ]]; then
      rw_md="${MEASUREMENTS}/report-realworld-${ts}.md"
      echo "  realworld profiles: ${rw_profiles[*]}"
      run_report_merged realworld "$rw_md" "${rw_profiles[@]}"
    fi

    # Step 2 – determine version
    if [[ -z "$version" ]]; then
      version="$(read_project_version)"
      echo "  version: ${version} (from pom.xml)"
    else
      echo "  version: ${version} (explicit)"
    fi

    # Step 3 – promote merged reports into reports/versions/
    banner "report — promoting results as version '${version}'"
    local machine
    if [[ -n "$synth_md" && -f "$synth_md" ]]; then
      IFS=$'\t' read -r _ machine < <(report_meta "$synth_md" "$machine_override")
      mkdir -p "${REPORTS_DIR}/versions"
      cp "$synth_md" "${REPORTS_DIR}/versions/${version}-synthetic-${machine}.md"
      echo "  synthetic → reports/versions/${version}-synthetic-${machine}.md"
      synth_promoted=1
    fi
    if [[ -n "$rw_md" && -f "$rw_md" ]]; then
      IFS=$'\t' read -r _ machine < <(report_meta "$rw_md" "$machine_override")
      mkdir -p "${REPORTS_DIR}/versions"
      cp "$rw_md" "${REPORTS_DIR}/versions/${version}-realworld-${machine}.md"
      echo "  realworld → reports/versions/${version}-realworld-${machine}.md"
      real_promoted=1
    fi
  else
    echo "(no perf-*.json in ${MEASUREMENTS}/ — skipping promote, regenerating summary from existing versions)"
    synth_promoted=1
    real_promoted=1
  fi

  # Step 4 – regenerate summary reports (one call covers all kinds)
  if [[ $synth_promoted -eq 1 || $real_promoted -eq 1 ]] && \
     compgen -G "${REPORTS_DIR}/versions/"*.md >/dev/null 2>&1; then
    banner "report — regenerating reports/summary/ (peers and history view)"
    python3 "$OPS_SCRIPT" \
      --source-dir "${REPORTS_DIR}/versions" \
      --output-dir "${REPORTS_DIR}/summary"
  fi
  echo
  echo "→ Done. Summary reports updated in ${REPORTS_DIR}/summary/"
}

cmd_bench() {
  local version="${1:-}"
  banner "bench — complete run: synthetic full + big + realworld + report (~3 h)"

  banner "bench [1/4] — synthetic full (all libraries, n = 10K / 100K)"
  run_kind synthetic all "$DEFAULT_SYNTHETIC_PROFILE"

  banner "bench [2/4] — synthetic big (Graphity + reordered, n = 1M / 10M / 100M)"
  run_kind synthetic all "big"

  local datasets_present=0
  if compgen -G "${BENCH_DIR}/datasets/*.txt" >/dev/null 2>&1 || \
     compgen -G "${BENCH_DIR}/datasets/snap/*.txt" >/dev/null 2>&1; then
    datasets_present=1
  fi

  if [[ $datasets_present -eq 1 ]]; then
    banner "bench [3/4] — realworld (all libraries)"
    run_kind realworld all "$DEFAULT_REALWORLD_PROFILE"
  else
    banner "bench [3/4] — realworld SKIPPED — no datasets found"
    echo "  No .txt edge lists under ${BENCH_DIR}/datasets/ (or datasets/snap/)."
    echo "  The SNAP datasets are gitignored and must be fetched once per machine:"
    echo "      ./datasets/download-datasets.sh"
    echo "  Without them this run produces synthetic results only."
    SKIPPED_PHASES+=("realworld — no datasets under ${BENCH_DIR}/datasets (run ./datasets/download-datasets.sh, then re-run)")
  fi

  # The report step belongs to the run: it merges measurements/ into
  # reports/versions/<version>-… and regenerates reports/summary/.
  banner "bench [4/4] — report"
  cmd_report "$version"
}

# measured_state -> JSON {"revision", "subject"}: the commit, plus a hash of uncommitted and
# untracked changes to the library, the benchmarks and the POMs ("<sha>+<hash>"). Results are
# attributed to this state; require_fresh_jar makes sure the jar was built from it.
measured_state() {
  python3 - "$(cd "${BENCH_DIR}/.." && pwd)" <<'PY'
import hashlib, json, subprocess, sys
repo = sys.argv[1]
paths = ["graphity-core", "graphity-benchmarks/src", "pom.xml", "graphity-benchmarks/pom.xml"]
def git(*args):
    return subprocess.run(["git", "-C", repo, *args], capture_output=True, check=True).stdout
try:
    sha = git("rev-parse", "HEAD").decode().strip()
    subject = git("log", "-1", "--format=%s").decode().strip()
    diff = git("diff", "HEAD", "--", *paths)
    untracked = [u for u in git("ls-files", "--others", "--exclude-standard", "-z", "--", *paths)
                 .split(b"\0") if u]
except (OSError, subprocess.CalledProcessError):
    print(json.dumps({"revision": "unknown-git", "subject": ""}))
    sys.exit(0)
digest = hashlib.sha1(diff)
for name in sorted(untracked):
    digest.update(name)
    with open(f"{repo}/{name.decode()}", "rb") as f:
        digest.update(f.read())
dirty = bool(diff) or bool(untracked)
print(json.dumps({"revision": sha + ("+" + digest.hexdigest()[:12] if dirty else ""),
                  "subject": subject}))
PY
}

# require_fresh_jar: refuses to measure with a jar older than the sources it is built from;
# otherwise the results of an old jar would be attributed to the current state.
require_fresh_jar() {
  python3 - "$JAR" "$(cd "${BENCH_DIR}/.." && pwd)" <<'PY'
import os, sys
jar, repo = sys.argv[1], sys.argv[2]
roots = ["graphity-core/src/main", "graphity-core/src/test/java/com/kobayami/graphity/testutils",
         "graphity-benchmarks/src/main"]
files = ["pom.xml", "graphity-core/pom.xml", "graphity-benchmarks/pom.xml"]
newest, newest_path = 0.0, ""
candidates = [os.path.join(repo, f) for f in files]
for root in roots:
    for d, _, names in os.walk(os.path.join(repo, root)):
        candidates.extend(os.path.join(d, n) for n in names)
for path in candidates:
    if os.path.isfile(path) and os.path.getmtime(path) > newest:
        newest, newest_path = os.path.getmtime(path), path
if newest > os.path.getmtime(jar):
    sys.exit(f"error: {os.path.relpath(jar, repo)} is older than {os.path.relpath(newest_path, repo)}; "
             "rebuild it first (from the repo root): mvn -am -pl graphity-benchmarks package -DskipTests")
PY
}

# snapshot_state: checks the jar and records the library versions once per invocation, at the
# start, so that every phase of a run - `bench` runs several - attributes its results to the state
# the jar was checked against. A source saved during a long run changes nothing for that run.
snapshot_state() {
  [[ -n "$LIBRARY_VERSIONS" ]] && return 0
  require_fresh_jar
  LIBRARY_VERSIONS="$(library_versions)"
}

# check_case_filter <matrix>: every name in --cases must be a case ("scc") or a case with its
# algorithm ("toporder:toporderandlevels") of the matrix; a typo would otherwise measure nothing.
check_case_filter() {
  [[ -z "$CASE_FILTER" ]] && return 0
  python3 - "$1" "$CASE_FILTER" <<'PY'
import json, sys
data = json.load(open(sys.argv[1], encoding="utf-8"))
known = set()
for c in data["cases"]:
    known.add(c["name"])
    known.update(f"{c['name']}:{a}" for a in c["algorithms"])
unknown = [x for x in sys.argv[2].split(",") if x and x not in known]
if unknown:
    sys.exit(f"error: --cases names unknown to {sys.argv[1]}: {', '.join(unknown)}; "
             f"cases are: {', '.join(c['name'] for c in data['cases'])}")
PY
}

# ab_write_meta <dir> <profile>
# Records what an A/B label measures: the state from measured_state, subject, JDK, CPU. Refuses
# to add to a label that holds measurements of another state - resume would mix two states. The
# same state, uncommitted or not, resumes.
ab_write_meta() {
  local dir="$1" profile="$2"
  local state java_version cpu
  # The snapshot of this invocation if there is one, so meta.json and the entries agree.
  state="${LIBRARY_VERSIONS:-$(measured_state)}"
  java_version="$(java -version 2>&1 | head -1)"
  cpu="$(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2- | sed 's/^ *//' || true)"
  [[ -z "$cpu" ]] && cpu="$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo unknown)"
  python3 - "$dir/meta.json" "$profile" "$state" "$java_version" "$cpu" <<'PY'
import datetime, json, os, sys
path, profile, java, cpu = sys.argv[1], sys.argv[2], sys.argv[4], sys.argv[5]
state = json.loads(sys.argv[3])
if os.path.isfile(path):
    old = json.load(open(path, encoding="utf-8"))
    if old.get("revision") != state["revision"] or state["revision"] == "unknown-git":
        raise SystemExit(
            f"error: {os.path.dirname(path)} already holds measurements of {old.get('revision', '?')}, "
            f"the current state is {state['revision']}; a resumed run would mix two states. "
            "Use a new label or delete the directory.")
meta = {
    "profile": profile,
    "revision": state["revision"],
    "subject": state["subject"],
    "java": java,
    "cpu": cpu,
    "startedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
}
json.dump(meta, open(path, "w", encoding="utf-8"), indent=2)
PY
}

# cmd_ab <label>: perf phase of the A/B profile into measurements/ab/<label>/, isolated from
# every other run - resume only ever reuses results of the same label.
cmd_ab() {
  local label="$1"
  if [[ ! "$label" =~ ^[A-Za-z0-9._-]+$ || "$label" =~ ^\.+$ ]]; then
    echo "error: 'ab' requires a label of letters, digits, '.', '_' or '-', e.g. 'before' or 'after'" >&2
    exit 2
  fi
  local matrix_file profile_name
  matrix_file="$(resolve_matrix_file "${PROFILE_OVERRIDE:-ab}")"
  validate_matrix "$matrix_file"
  check_case_filter "$matrix_file"
  snapshot_state
  profile_name="$(matrix_profile_name "$matrix_file")"
  MEASUREMENTS="${BENCH_DIR}/measurements/ab/${label}"
  mkdir -p "$MEASUREMENTS"
  ab_write_meta "$MEASUREMENTS" "$profile_name"
  LIB_FILTER=""
  banner "ab — label '${label}', profile '${profile_name}'${CASE_FILTER:+, cases [${CASE_FILTER}]}"
  run_perf "$matrix_file" "$profile_name"
  echo
  echo "→ JSON written to ${MEASUREMENTS}/. Measure the other state under a second label, then:"
  echo "  $(basename "$0") compare <label-a> <label-b>"
}

# cmd_compare <label-a> <label-b>: B against A per (benchmark, params), with error bars.
cmd_compare() {
  local a="$1" b="$2"
  if [[ -z "$a" || -z "$b" ]]; then
    echo "error: 'compare' requires two labels measured with 'ab'" >&2
    exit 2
  fi
  python3 - "${BENCH_DIR}/measurements/ab" "$a" "$b" <<'PY'
import glob, json, math, os, sys

root, label_a, label_b = sys.argv[1:]
MAX_HALF_WIDTH = 15.0  # percent of the score; above it a case does not resolve small effects

def load(label):
    d = os.path.join(root, label)
    if not os.path.isdir(d):
        raise SystemExit(f"error: no measurements for label '{label}' under {root}/")
    meta_path = os.path.join(d, "meta.json")
    meta = json.load(open(meta_path, encoding="utf-8")) if os.path.isfile(meta_path) else {}
    results = {}
    for path in sorted(glob.glob(os.path.join(d, "perf-*@*.json"))):
        for e in json.load(open(path, encoding="utf-8")):
            params = e.get("params") or {}
            key = (e["benchmark"], tuple(sorted((str(k), str(v)) for k, v in params.items())))
            m = e["primaryMetric"]
            # JMH writes a missing error as the string "NaN"; float() turns it into nan.
            results[key] = (float(m["score"]), float(m.get("scoreError", "nan")),
                            m.get("scoreUnit", ""))
    return meta, results

meta_a, res_a = load(label_a)
meta_b, res_b = load(label_b)

def describe(label, meta):
    commit, _, changes = meta.get("revision", "unknown").partition("+")
    state = f"`{commit[:12]}`" + (f" + uncommitted changes `{changes}`" if changes else "")
    return f"- **{label}**: {state} {meta.get('subject', '')} ({meta.get('java', '?')}, {meta.get('cpu', '?')})"

def half_width(score, err):
    return 100.0 * err / score if score and not math.isnan(err) else float("nan")

def fmt(x):
    if math.isnan(x):
        return "n/a"
    return (f"{x:.1f}" if abs(x) >= 100 else f"{x:.2f}" if abs(x) >= 10
            else f"{x:.3f}" if abs(x) >= 1 else f"{x:.4g}")

def pct(x):
    return "n/a" if math.isnan(x) else f"{fmt(x)} %"

print(f"# A/B comparison: {label_b} against {label_a}\n")
print(describe(label_a, meta_a))
print(describe(label_b, meta_b))
print()
print(f"Scores are JMH means with 99.9 % confidence half-widths. B/A below 1 means B is faster. "
      f"'noise' marks ratios whose distance from 1 is within the combined half-width; "
      f"'wide' marks cases with a half-width above {MAX_HALF_WIDTH:.0f} % on either side.\n")
print("| Benchmark | Params | A | ±A | B | ±B | B/A | ±B/A | |")
print("|---|---|---:|---:|---:|---:|---:|---:|---|")
keys = sorted(set(res_a) & set(res_b))
wide = noise = 0
for key in keys:
    sa, ea, unit = res_a[key]
    sb, eb, _ = res_b[key]
    ha, hb = half_width(sa, ea), half_width(sb, eb)
    ratio = sb / sa if sa else float("nan")
    hr = math.sqrt(ha * ha + hb * hb) if not (math.isnan(ha) or math.isnan(hb)) else float("nan")
    flags = []
    if not math.isnan(hr) and abs(ratio - 1.0) * 100.0 <= hr:
        flags.append("noise")
        noise += 1
    if (not math.isnan(ha) and ha > MAX_HALF_WIDTH) or (not math.isnan(hb) and hb > MAX_HALF_WIDTH):
        flags.append("wide")
        wide += 1
    bench = key[0].rsplit(".", 2)[-2] + "." + key[0].rsplit(".", 1)[-1]
    params = ", ".join(f"{k}={v}" for k, v in key[1])
    print(f"| {bench} | {params} | {fmt(sa)} {unit} | {pct(ha)} | {fmt(sb)} {unit} | {pct(hb)} "
          f"| {ratio:.3f} | {pct(hr)} | {' '.join(flags)} |")
only_a = len(set(res_a) - set(res_b))
only_b = len(set(res_b) - set(res_a))
print()
print(f"{len(keys)} case(s) compared, {noise} within noise, {wide} wider than {MAX_HALF_WIDTH:.0f} %."
      + (f" {only_a} only in {label_a}, {only_b} only in {label_b}." if only_a or only_b else ""))
PY
}

usage() {
  cat <<EOF
Usage: $(basename "$0") <command> [args]

Workflow — from a clean slate to committed reports with charts:

  0. ./datasets/download-datasets.sh          # once per machine: fetch the SNAP datasets
       (they are gitignored; without them the realworld phase is skipped)
  1. $(basename "$0") clean                   # wipe measurements/ (old JSON etc.)
       measurements/ doubles as the resume cache. Results are reused only while
       their library version and JDK match, so a new Graphity state is measured
       again on its own; 'clean --keep-peers' drops Graphity explicitly and keeps
       JGraphT, Guava and WebGraph. After a change to generators, fixtures or peer
       adapters, also 'clean --lib jgrapht,guava,webgraph': those do not show in a
       peer's version.
  2. $(basename "$0") bench [version]         # complete ~3h run: synthetic full + big + realworld + report
     — OR run individual steps:
     $(basename "$0") synthetic <lib|all>     # synthetic full (n = 10K / 100K, all libraries)
     $(basename "$0") synthetic <lib|all> --profile big   # large graphs (n = 1M+, Graphity only)
     $(basename "$0") realworld <lib|all>     # real-world datasets
     (repeat per library so one slow/crashing peer can't block the run)
  3. $(basename "$0") report [version]        # merge JSON → promote → regenerate summary reports

Commands:
  clean                          Delete run artifacts (JSON/CSV/MD/log) from measurements/.
  clean --lib <a,b,...>          Remove only the results of the named libraries; keep the rest as cache.
  clean --keep-peers             Same as --lib graphity,graphity-reordered,graphity-biadjacent.
  bench [version]                Run all phases in sequence: synthetic full → big → realworld → report.
                                 Skips realworld (loudly, and again in the closing summary) if
                                 datasets/ contains no .txt files. 'version' is passed to report.
  synthetic <lib|all>            perf+alloc+mem on synthetic graphs (default profile: ${DEFAULT_SYNTHETIC_PROFILE}).
  realworld <lib|all>            perf on real-world SNAP datasets   (default profile: ${DEFAULT_REALWORLD_PROFILE}).
  ab <label> [--cases a,b]       A/B measurement of the current state into measurements/ab/<label>/
                                 (profile 'ab', perf only, Graphity only). Resume reuses only results
                                 of the same label; a label refuses results of a second commit.
                                 --cases limits the run to the named cases of the matrix
                                 ('scc', or 'toporder:toporderandlevels' for one algorithm).
  compare <label-a> <label-b>    Markdown table of B against A per case: means, 99.9 % half-widths,
                                 B/A with combined half-width; flags 'noise' and 'wide' (> 15 %).
  report [version] [machine]     Merge JSON → promote to reports/versions/<version>-<kind>-<machine>.md →
                                 regenerate reports/summary/<kind>-<machine>-{peers,history}.md + chart SVGs.
                                 Version defaults to the value in pom.xml (SNAPSHOT stripped).
                                 Machine is auto-derived from OS/CPU; override if needed.

Libraries (for synthetic / realworld):
  all   graphity   graphity-reordered   graphity-biadjacent   jgrapht   guava   webgraph

Profiles (--profile <name>):
  Synthetic:    quick   — fast dev loop, n = 10K/100K, all libs, perf+alloc (~minutes)
                full    — full baseline, n = 10K/100K, all libs, perf+alloc (~hours) [default]
                big     — large graphs, n = 1M/10M/100M, Graphity+reordered only, perf (~hours)
                ab      — A/B decisions, Graphity only, 3 forks, n = 10K/100K/1M (used by 'ab')
  Real-world:   realworld       — all libs, all datasets, perf [default]
                realworld-quick — smoke run, one dataset per case (~minutes)

Examples:
  $(basename "$0") bench                             # complete run (~3h) + report, version from pom.xml
  $(basename "$0") bench 2.2.0                       # same, promoted as version 2.2.0
  $(basename "$0") clean && $(basename "$0") bench   # clean slate then full run
  $(basename "$0") synthetic all                     # full synthetic only
  $(basename "$0") synthetic all --profile big       # large-graph synthetic only
  $(basename "$0") realworld all                     # real-world only
  $(basename "$0") report                            # regenerate summary from existing versions
  $(basename "$0") report 2.0.0
  $(basename "$0") report 2.0.0 macos-m5
  $(basename "$0") ab before --cases scc && $(basename "$0") ab after --cases scc && $(basename "$0") compare before after
EOF
}

cmd="${1:-}"
ts=$(date +%Y%m%d-%H%M%S)

case "$cmd" in
  -h|--help|help|"") usage; exit 0 ;;
esac

shift || true

# Parse options (--profile/--matrix, --cases, and --lib/--keep-peers for clean; run commands
# use positionals).
PROFILE_OVERRIDE=""
CASE_FILTER=""
CLEAN_LIBS=""
LIBRARY_VERSIONS=""
POSARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile|--matrix)
      PROFILE_OVERRIDE="${2:-}"
      [[ -z "$PROFILE_OVERRIDE" ]] && { echo "error: $1 requires a value" >&2; exit 2; }
      shift 2 ;;
    --profile=*|--matrix=*)
      PROFILE_OVERRIDE="${1#*=}"; shift ;;
    --cases)
      CASE_FILTER="${2:-}"
      [[ -z "$CASE_FILTER" ]] && { echo "error: $1 requires a value" >&2; exit 2; }
      shift 2 ;;
    --cases=*)
      CASE_FILTER="${1#*=}"; shift ;;
    --lib)
      CLEAN_LIBS="${2:-}"
      [[ -z "$CLEAN_LIBS" ]] && { echo "error: $1 requires a value" >&2; exit 2; }
      shift 2 ;;
    --lib=*)
      CLEAN_LIBS="${1#*=}"; shift ;;
    --keep-peers)
      CLEAN_LIBS="graphity,graphity-reordered,graphity-biadjacent"; shift ;;
    -*)
      echo "error: unknown option '$1'" >&2; usage; exit 2 ;;
    *)
      POSARGS+=("$1"); shift ;;
  esac
done

if [[ -n "$CLEAN_LIBS" ]]; then
  if [[ "$cmd" != "clean" ]]; then
    echo "error: --lib and --keep-peers belong to 'clean'" >&2
    exit 2
  fi
  for lib in ${CLEAN_LIBS//,/ }; do
    if [[ "$SUPPORTED_LIBS" != *" $lib "* ]]; then
      echo "error: unknown library '$lib'; supported:${SUPPORTED_LIBS}" >&2
      exit 2
    fi
  done
fi

case "$cmd" in
  clean)     cmd_clean ;;
  bench)     cmd_bench "${POSARGS[0]:-}" ;;
  synthetic) run_kind synthetic "${POSARGS[0]:-}" "${PROFILE_OVERRIDE:-$DEFAULT_SYNTHETIC_PROFILE}" ;;
  realworld) run_kind realworld "${POSARGS[0]:-}" "${PROFILE_OVERRIDE:-$DEFAULT_REALWORLD_PROFILE}" ;;
  report)    cmd_report "${POSARGS[0]:-}" "${POSARGS[1]:-}" ;;
  ab)        cmd_ab "${POSARGS[0]:-}" ;;
  compare)   cmd_compare "${POSARGS[0]:-}" "${POSARGS[1]:-}" ;;
  *)
    echo "error: unknown command '$cmd'" >&2
    usage
    exit 2
    ;;
esac

if [[ ${#SKIPPED_PHASES[@]} -gt 0 ]]; then
  banner "skipped phases"
  echo "${#SKIPPED_PHASES[@]} phase(s) did not run — the report covers less than a full run:" >&2
  printf '  - %s\n' "${SKIPPED_PHASES[@]}" >&2
fi

if [[ ${#FAILED_MEASUREMENTS[@]} -gt 0 ]]; then
  banner "done, but with failures"
  echo "${#FAILED_MEASUREMENTS[@]} measurement(s) failed and were skipped (everything else succeeded and was kept):" >&2
  printf '  - %s\n' "${FAILED_MEASUREMENTS[@]}" >&2
  echo "Re-run the same command to retry only these — already-completed measurements are cached and skipped." >&2
  exit 1
fi
