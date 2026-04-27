#!/usr/bin/env bash
# Graphity benchmark orchestrator (JSON-matrix driven, fail-fast validation).
#
# Usage:
#   ./run-benchmarks.sh <subcommand> <profile-or-matrix.json>
#   subcommands: perf | alloc | mem | report | all  (typically 'all')
#   profiles:    quick, full, full-big-graph, dataset-quick, dataset-full
#                or a path to any JSON matrix file
#
# Run from anywhere — script resolves paths relative to its own location.
#
# Build prerequisite (from repo root, run once after code changes):
#   mvn -am -pl graphity-benchmarks package -DskipTests
#
# ─────────────────────────────────────────────────────────────────────────────
# Methodology note — variance at n ≥ 10⁶
# ─────────────────────────────────────────────────────────────────────────────
# At n = 1M and above, several benchmarks show ±error/score in the 50–97% range
# (observed in the n=1M runs of 2026-04-26). Root cause: GC pauses inside long
# iterations, not insufficient measurement time. Lengthening iterations makes it
# worse, not better — more allocation per iter accumulates more GC pressure.
#
# Recommended JMH flags for 1M+ profiles (apply before relying on absolute
# factors in any external communication):
#
#   -Xmx16G -XX:+UseZGC -XX:+AlwaysPreTouch -f 3 -wi 5 -w 3s -i 10 -r 5s
#
# Mitigations in order of increasing effect:
#   1. -f 3              — averages JIT-state lottery across forks
#   2. -Xmx16G           — pushes Major GCs out of the measurement window
#   3. -XX:+UseZGC       — sub-millisecond pauses, removes GC variance entirely
#   4. shorter iters     — many short iters (10 × 5s) instead of few long ones
#   5. taskset -c 0-15   — pin to one socket / fast cores
#   6. AlwaysPreTouch    — heap allocated at startup, smoother first iters
#
# These flags are NOT yet applied to any standard profile — that work belongs
# into the Release 1.1 benchmark round, which focuses on performance and memory
# investigations rather than new algorithms. Until then, treat n=1M factors as
# directional and quote ranges, not single numbers.
#
# Full discussion: Backlog.md → "Varianz-Reduktion bei n ≥ 10⁶".
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCH_DIR="${SCRIPT_DIR}"
JAR="${BENCH_DIR}/target/graphity-benchmarks.jar"
RESULTS="${BENCH_DIR}/results"
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

mkdir -p "$RESULTS"

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

ALL_SHAPES = {
    "PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
    "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
    "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED",
    "CHAIN_OF_CLIQUES_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"
}
GRAPH_FIXTURE_SHAPES = {
    "PATH", "TREE", "GNP_SPARSE", "GNP_DENSE", "CHAIN_OF_CLIQUES",
    "PATH_SHUFFLED", "TREE_SHUFFLED", "GNP_SPARSE_SHUFFLED", "GNP_DENSE_SHUFFLED",
    "CHAIN_OF_CLIQUES_SHUFFLED"
}
DAG_FIXTURE_SHAPES = {
    "PATH", "TREE", "DAG_GNP_SPARSE", "DAG_GNP_DENSE",
    "PATH_SHUFFLED", "TREE_SHUFFLED", "DAG_GNP_SPARSE_SHUFFLED", "DAG_GNP_DENSE_SHUFFLED"
}
SUPPORTED_LIBS = {"graphity", "jgrapht", "guava", "webgraph"}
SUPPORTED_ALGOS = {
    "build", "scc", "wcc", "ccs", "toporder", "toporderandlevels",
    "backedges", "edgetypes"
}
ALGO_SUPPORTED_LIBS = {
    "build": {"graphity", "jgrapht", "guava", "webgraph"},
    "scc": {"graphity", "jgrapht", "guava", "webgraph"},
    "toporder": {"graphity", "jgrapht", "guava", "webgraph"},
    "backedges": {"graphity", "jgrapht", "guava", "webgraph"},
    "edgetypes": {"graphity", "jgrapht", "guava", "webgraph"},
    "wcc": {"graphity"},
    "ccs": {"graphity"},
    "toporderandlevels": {"graphity"},
}
ALGO_ALLOWED_SHAPES = {
    "build": GRAPH_FIXTURE_SHAPES,
    "scc": GRAPH_FIXTURE_SHAPES,
    "backedges": GRAPH_FIXTURE_SHAPES,
    "edgetypes": GRAPH_FIXTURE_SHAPES,
    "toporder": DAG_FIXTURE_SHAPES,
    "wcc": GRAPH_FIXTURE_SHAPES,
    "ccs": GRAPH_FIXTURE_SHAPES,
    "toporderandlevels": DAG_FIXTURE_SHAPES,
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
print(f"{forks}\t{wi}\t{w}\t{it}\t{r}\t{stack}")
PY
}

matrix_cases_tsv() {
  local matrix_file="$1"
  local phase="$2"
  python3 - "$matrix_file" "$phase" <<'PY'
import json, os, re, sys
matrix_path = sys.argv[1]
data = json.loads(open(matrix_path, encoding="utf-8").read())
phase = sys.argv[2]
base_dir = os.path.dirname(os.path.abspath(matrix_path))

def resolve_dataset_path(raw):
    expanded = os.path.expanduser(os.path.expandvars(str(raw)))
    if not os.path.isabs(expanded):
        expanded = os.path.normpath(os.path.join(base_dir, expanded))
    return expanded

BENCHMARK_FQNS = {
    "build": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.BuildBenchmark.build",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTBuildBenchmark.build",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaBuildBenchmark.build",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphBuildBenchmark.build",
    },
    "scc": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.SccBenchmark.scc",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTSccBenchmark.scc",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaSccBenchmark.scc",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphSccBenchmark.scc",
    },
    "wcc": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.WccBenchmark.wcc",
    },
    "ccs": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.CcsBenchmark.ccs",
    },
    "toporder": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.TopOrderBenchmark.topOrder",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTTopOrderBenchmark.topOrder",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaTopOrderBenchmark.topOrder",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphTopOrderBenchmark.topOrder",
    },
    "toporderandlevels": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.TopOrderAndLevelsBenchmark.topOrderAndLevels",
    },
    "backedges": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.BackEdgesBenchmark.backEdges",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTBackEdgesBenchmark.backEdges",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaBackEdgesBenchmark.backEdges",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphBackEdgesBenchmark.backEdges",
    },
    "edgetypes": {
        "graphity": "com.kobayami.graphity.benchmarks.graphity.EdgeTypesBenchmark.edgeTypes",
        "jgrapht": "com.kobayami.graphity.benchmarks.jgrapht.JGraphTEdgeTypesBenchmark.edgeTypes",
        "guava": "com.kobayami.graphity.benchmarks.guava.GuavaEdgeTypesBenchmark.edgeTypes",
        "webgraph": "com.kobayami.graphity.benchmarks.webgraph.WebGraphEdgeTypesBenchmark.edgeTypes",
    },
}

cases = data.get("cases", [])
for c in cases:
    phases = c.get("phases", ["perf", "alloc"])
    if phase not in phases:
        continue
    name = c["name"]
    libs = c["libraries"]
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
    for algo in algos:
        fqns = [BENCHMARK_FQNS[algo][lib] for lib in libs]
        escaped = [re.escape(x) for x in fqns]
        bench_regex = "^(?:" + "|".join(escaped) + ")$"
        fields = [f"{name}:{algo}", bench_regex]
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

merge_jmh_json_arrays() {
  local out="$1"
  shift
  python3 - "$out" "$@" <<'PY'
import json, sys
out = sys.argv[1]
inputs = sys.argv[2:]
arr = []
for p in inputs:
    with open(p, encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, list):
        raise SystemExit(f"expected JSON array in {p}")
    arr.extend(data)
with open(out, "w", encoding="utf-8") as f:
    json.dump(arr, f, indent=2)
PY
}

matrix_mem_tsv() {
  local matrix_file="$1"
  python3 - "$matrix_file" <<'PY'
import json, sys
data = json.loads(open(sys.argv[1], encoding="utf-8").read())
mem = data.get("mem", {})
enabled = mem.get("enabled", True)
nodes = ",".join(str(x) for x in mem.get("nodes", []))
shapes = ",".join(str(x) for x in mem.get("shapes", []))
libraries = ",".join(str(x) for x in mem.get("libraries", []))
print(f"{str(bool(enabled)).lower()}\t{nodes}\t{shapes}\t{libraries}")
PY
}

matrix_validate_cases_tsv() {
  local matrix_file="$1"
  local phase="$2"
  python3 - "$matrix_file" "$phase" <<'PY'
import json, os, sys
matrix_path = sys.argv[1]
data = json.loads(open(matrix_path, encoding="utf-8").read())
phase = sys.argv[2]
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
    libs = ",".join(c["libraries"])
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
  IFS=$'\t' read -r _ _ _ _ _ jvm_stack <<< "$(matrix_jmh_defaults "$matrix_file")"
  banner "validate — semantic quick-fail (${phase})"
  local case_idx=0
  while IFS=$'\x1f' read -r case_name algo libs nodes shapes datasets; do
    [[ -z "$algo" ]] && continue
    case_idx=$((case_idx + 1))
    echo "[validate/$case_idx] case=${case_name} algo=${algo} libs=${libs}"
    if [[ -n "$datasets" ]]; then
      java "$jvm_stack" -jar "$JAR" validate \
        --algorithm "$algo" \
        --libraries "$libs" \
        --datasets "$datasets"
    else
      java "$jvm_stack" -jar "$JAR" validate \
        --algorithm "$algo" \
        --libraries "$libs" \
        --nodes "$nodes" \
        --shapes "$shapes"
    fi
  done < <(matrix_validate_cases_tsv "$matrix_file" "$phase")
}

run_jmh_phase() {
  local phase="$1" matrix_file="$2" profile="$3" ts="$4"
  local out="${RESULTS}/${phase}-${profile}-${ts}.json"
  local log="${out%.json}.log"
  local tmp_dir
  tmp_dir="$(mktemp -d)"
  local -a case_json_files=()
  local case_idx=0

  IFS=$'\t' read -r forks wi w it r jvm_stack <<< "$(matrix_jmh_defaults "$matrix_file")"

  : > "$log"
  while IFS=$'\t' read -r -a fields; do
    [[ ${#fields[@]} -lt 2 ]] && continue
    case_idx=$((case_idx + 1))
    local case_name="${fields[0]}"
    local bench_regex="${fields[1]}"
    local case_out="${tmp_dir}/${phase}-${case_idx}.json"
    local case_log="${tmp_dir}/${phase}-${case_idx}.log"

    echo "[$phase/$case_idx] case=${case_name}" | tee -a "$log"
    rm -f /tmp/jmh.lock

    local -a cmd=(java "$jvm_stack" -jar "$JAR" -f "$forks" -wi "$wi" -w "$w" -i "$it" -r "$r")
    if [[ "$phase" == "alloc" ]]; then
      cmd+=(-prof gc)
    fi
    for ((i = 2; i < ${#fields[@]}; i++)); do
      cmd+=(-p "${fields[i]}")
    done
    cmd+=("$bench_regex" -jvmArgsAppend "$jvm_stack" -rf json -rff "$case_out")

    "${cmd[@]}" 2>&1 | tee "$case_log"
    cat "$case_log" >> "$log"
    case_json_files+=("$case_out")
  done < <(matrix_cases_tsv "$matrix_file" "$phase")

  if [[ ${#case_json_files[@]} -eq 0 ]]; then
    echo "error: no JMH cases for phase '$phase' in matrix $matrix_file" >&2
    rm -rf "$tmp_dir"
    exit 2
  fi

  merge_jmh_json_arrays "$out" "${case_json_files[@]}"
  rm -rf "$tmp_dir"
  echo "$phase JSON → $out"
}

run_perf() {
  local matrix_file="$1" profile="$2" ts="$3"
  run_validate_phase "$matrix_file" "perf"
  banner "perf — ${profile}"
  run_jmh_phase "perf" "$matrix_file" "$profile" "$ts"
}

run_alloc() {
  local matrix_file="$1" profile="$2" ts="$3"
  run_validate_phase "$matrix_file" "alloc"
  banner "alloc — ${profile} (with -prof gc)"
  run_jmh_phase "alloc" "$matrix_file" "$profile" "$ts"
}

run_mem() {
  local matrix_file="$1" profile="$2" ts="$3"
  local out="${RESULTS}/mem-${profile}-${ts}.csv"
  IFS=$'\t' read -r mem_enabled mem_nodes mem_shapes mem_libraries <<< "$(matrix_mem_tsv "$matrix_file")"

  banner "mem — ${profile}"
  if [[ "$mem_enabled" != "true" ]]; then
    echo "mem disabled by matrix (${matrix_file})"
    return
  fi

  local -a mem_args=(mem -o "$out")
  if [[ -n "$mem_nodes" ]]; then
    mem_args+=(-n "$mem_nodes")
  fi
  if [[ -n "$mem_shapes" ]]; then
    mem_args+=(-s "$mem_shapes")
  fi
  if [[ -n "$mem_libraries" ]]; then
    mem_args+=(-l "$mem_libraries")
  fi

  java -jar "$JAR" "${mem_args[@]}"
  echo "mem CSV → $out"
}

run_report() {
  local profile="$1" ts="$2"
  local out="${RESULTS}/report-${profile}-${ts}.md"

  banner "report — aggregating latest artifacts for profile '$profile'"

  local perf_file alloc_file mem_file
  perf_file=$(ls -t "${RESULTS}"/perf-"${profile}"-*.json 2>/dev/null | head -1 || true)
  alloc_file=$(ls -t "${RESULTS}"/alloc-"${profile}"-*.json 2>/dev/null | head -1 || true)
  mem_file=$(ls -t "${RESULTS}"/mem-"${profile}"-*.csv 2>/dev/null | head -1 || true)

  local report_args=(report --profile "$profile" --out "$out")
  if [[ -n "$perf_file" ]]; then
    report_args+=(--perf "$perf_file")
    echo "  using perf:  $(basename "$perf_file")"
  else
    echo "  perf:  (missing)"
  fi
  if [[ -n "$alloc_file" ]]; then
    report_args+=(--alloc "$alloc_file")
    echo "  using alloc: $(basename "$alloc_file")"
  else
    echo "  alloc: (missing)"
  fi
  if [[ -n "$mem_file" ]]; then
    report_args+=(--mem "$mem_file")
    echo "  using mem:   $(basename "$mem_file")"
  else
    echo "  mem:   (missing)"
  fi

  java -jar "$JAR" "${report_args[@]}"
}

usage() {
  cat <<EOF
Usage: $(basename "$0") <subcommand> <profile-or-matrix.json>

Subcommands:
  perf    <profile>   JMH performance run (matrix-driven)      -> perf-*.json
  alloc   <profile>   JMH allocation run (-prof gc, matrix)    -> alloc-*.json
  mem     <profile>   JOL memory measurement (matrix-driven)   -> mem-*.csv
  report  <profile>   Aggregate latest artifacts to Markdown   → report-*.md
  all     <profile>   perf → alloc → mem → report, sequentially

Profiles (resolved as benchmark-matrix/<name>.json):
  quick
  full
  full-big-graph
  dataset-quick
  dataset-full

Examples:
  $(basename "$0") all quick
  $(basename "$0") perf full
  $(basename "$0") all dataset-quick
  $(basename "$0") all ./benchmark-matrix/full.json
EOF
}

subcmd="${1:-}"
profile_arg="${2:-}"
ts=$(date +%Y%m%d-%H%M%S)

case "$subcmd" in
  -h|--help|help|"")
    usage
    exit 0
    ;;
esac

if [[ -z "$profile_arg" ]]; then
  usage
  exit 2
fi

matrix_file="$(resolve_matrix_file "$profile_arg")"
validate_matrix "$matrix_file"
profile_name="$(matrix_profile_name "$matrix_file")"

case "$subcmd" in
  perf)   run_perf   "$matrix_file" "$profile_name" "$ts" ;;
  alloc)  run_alloc  "$matrix_file" "$profile_name" "$ts" ;;
  mem)    run_mem    "$matrix_file" "$profile_name" "$ts" ;;
  report) run_report "$profile_name" "$ts" ;;
  all)
    banner "Phase 1/4 — perf"
    run_perf "$matrix_file" "$profile_name" "$ts"
    banner "Phase 2/4 — alloc"
    run_alloc "$matrix_file" "$profile_name" "$ts"
    banner "Phase 3/4 — mem"
    run_mem "$matrix_file" "$profile_name" "$ts"
    banner "Phase 4/4 — report"
    run_report "$profile_name" "$ts"
    ;;
  *)
    echo "error: unknown subcommand '$subcmd'" >&2
    usage
    exit 2
    ;;
esac
