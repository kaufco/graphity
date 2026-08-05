#!/usr/bin/env python3
"""Generate the summary benchmark reports from versioned ``<version>-<kind>-<machine>.md`` files.

Each invocation writes two files per ``(kind, machine)`` found in the source directory:

* ``<kind>-<machine>-peers.md`` — the newest measured Graphity version, plus its variants on a
  DFS-preorder reordered and on a bi-adjacent graph, against JGraphT, Guava and WebGraph.
* ``<kind>-<machine>-history.md`` — every measured Graphity version, each with its own variant
  columns where they were measured, without peer libraries.

Both carry the chapters their source files have: performance (times in ms/op and throughput
in op/s), memory footprint and allocations, each normalised to the newest Graphity version
and illustrated by bar-chart SVGs under ``fig/<kind>-<machine>-<view>/``.

Conversion: ``op/s = 1000 / ms``.

Usage::

    # both views for every (kind, machine) under reports/versions/
    python3 times-to-ops.py

    # explicit directories
    python3 times-to-ops.py --source-dir reports/versions --output-dir reports/summary

    # single-file conversion (for ad-hoc use)
    python3 times-to-ops.py --single IN.md OUT.md
"""

from __future__ import annotations

import argparse
import html
import math
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path

MS_CELL = re.compile(
    r"(?P<ms>\d+(?:\.\d+)?)\s+ms(?P<ratio>\s+\([^)]+\))?"
)
MS_VALUE_RE = re.compile(r"^(?P<ms>\d+(?:\.\d+)?)\s+ms")
CHAPTER_HEADING = re.compile(r"^## (?P<num>\d+)\b[^\n]*$", re.MULTILINE)
FILENAME_RE = re.compile(
    r"^(?P<version>\d+\.\d+(?:\.\d+)?)-(?P<kind>synthetic|realworld)-(?P<machine>.+)\.md$"
)
SECTION_RE = re.compile(r"^### (?P<title>.+)$", re.MULTILINE)
HEADER_BLOCK_RE = re.compile(r"```\n(?P<body>.*?)\n```", re.DOTALL)
BYTES_VALUE_RE = re.compile(
    r"^(?:~0|(?P<num>\d{1,3}(?: \d{3})*(?:\.\d+)?|\d+(?:\.\d+)?)\s+B)"
)
# Section headings in a version report look like
#   "1.6.1 · TopOrder @ n = 10 000"     — synthetic, one block per node count
#   "1.8 · SCC — real-world graphs"     — dataset mode, no node count
#   "2.1 · Total Graph Size"            — memory
# The leading number is a running index over the algorithms that had data in
# that particular run, so it SHIFTS whenever an algorithm is added or absent
# (Condense entered the matrix after 2.0.0 and pushed TopOrder from 1.5 to 1.6).
# Matching sections across versions must therefore ignore the number and key on
# algorithm + variant + node count instead; the combined report then assigns its
# own numbers from the union of all versions.
SECTION_TITLE_RE = re.compile(r"^(?P<num>\d+(?:\.\d+)*)\s*·\s*(?P<rest>.+)$")
N_SUFFIX_RE = re.compile(r"\s*@\s*n\s*=\s*(?P<n>[\d\s]+)$")
VARIANT_SUFFIX_RE = re.compile(r"\s*—\s*(?P<variant>.+)$")
# Mirrors ReportMain.ALGO_ORDER, which fixes the algorithm order within a single
# version report. Algorithms not listed here (e.g. the memory sub-chapters) keep
# the relative order they have in the source files.
ALGO_ORDER = (
    "Build", "SCC", "WCC", "CCs", "Condense", "TopOrder", "TopOrderAndLevels",
    "BackEdges", "EdgeTypes", "Reorder (DFS-preorder) cost",
    "BiAdjacency (in-adjacency) cost", "Symmetrize (WebGraph) cost",
)

MISSING = {"—", "-", ""}
META_HEADERS = ("Nodes", "Edges", "Edges/Node")
# Graphity variants: the same algorithm on a different graph representation, each its own column
# next to the plain one. The suffix is what the version reports append to "Graphity <version>".
GRAPHITY_VARIANTS = ("Graphity (reordered)", "Graphity (bi-adjacent)")
VARIANT_LABELS = tuple(v.removeprefix("Graphity") for v in GRAPHITY_VARIANTS)
LIB_HEADERS = ("Graphity", *GRAPHITY_VARIANTS, "JGraphT", "Guava", "WebGraph")
# Columns taken from the newest version that measured them, in output order. The variants belong
# here because they are measured per run, not per release.
PEER_LIBS = (*GRAPHITY_VARIANTS, "JGraphT", "Guava", "WebGraph")

SERIES_COLORS = {
    "JGraphT": "#16a34a",
    "Guava": "#ca8a04",
    "WebGraph": "#9333ea",
}
GRAPHITY_COLORS = [
    "#2563eb",
    "#dc2626",
    "#0891b2",
    "#ea580c",
    "#4f46e5",
]
# The history view shows one column per version and variant, so a single colour per variant would
# make those bars indistinguishable. One ramp per variant, and within it one entry per version;
# the first entry of the reordered ramp is the colour the peer view has always used.
REORDERED_COLORS = [
    "#0d9488",
    "#db2777",
    "#65a30d",
    "#7c3aed",
    "#0369a1",
]
BIADJACENT_COLORS = [
    "#be123c",
    "#047857",
    "#92400e",
    "#155e75",
    "#374151",
]
VARIANT_COLORS = {
    " (reordered)": REORDERED_COLORS,
    " (bi-adjacent)": BIADJACENT_COLORS,
}
GRAPHITY_LABEL_RE = re.compile(
    r"^Graphity \d+(?:\.\d+)*(?P<variant> \((?:reordered|bi-adjacent)\))?$"
)


class View(Enum):
    """One of the two summary files written per ``(kind, machine)``."""

    PEERS = "peers"
    HISTORY = "history"


# ---------------------------------------------------------------------------
# Value formatters
# ---------------------------------------------------------------------------


def fmt_ms(ms: float) -> str:
    if ms >= 10000:
        return f"{ms:.0f} ms"
    if ms >= 1000:
        return f"{ms:.1f} ms"
    if ms >= 100:
        return f"{ms:.1f} ms"
    if ms >= 10:
        return f"{ms:.2f} ms"
    return f"{ms:.3f} ms"


def fmt_ops(ms: float) -> str:
    ops = 1000.0 / ms
    if ops >= 1000:
        return f"{ops:,.0f}".replace(",", " ")
    if ops >= 100:
        return f"{ops:.1f}"
    if ops >= 10:
        return f"{ops:.2f}"
    return f"{ops:.3f}"


def fmt_ops_value(ops: float) -> str:
    if ops >= 1000:
        return f"{ops:,.0f}".replace(",", " ")
    if ops >= 100:
        return f"{ops:.1f}"
    if ops >= 10:
        return f"{ops:.2f}"
    return f"{ops:.3f}"


def fmt_bytes_value(value: float) -> str:
    """Format a byte count using kB / MB for large values, B with decimals for small."""
    if value <= 0:
        return "~0"
    if value >= 1_000_000:
        v = value / 1_000_000
        return f"{v:.0f} MB" if v >= 100 else f"{v:.1f} MB"
    if value >= 1_000:
        v = value / 1_000
        return f"{v:.0f} kB" if v >= 100 else f"{v:.1f} kB"
    # Small values (bytes per element, small allocations) — keep decimal precision
    if abs(value - round(value)) < 0.005:
        return f"{int(round(value))} B"
    return f"{value:.2f} B"


def fmt_count(n: int) -> str:
    """Format a count with k / M / G suffix, rounding to 1 decimal (drops .0)."""
    def _fmt(v: float, suffix: str) -> str:
        r = round(v, 1)
        return f"{int(r)}{suffix}" if r == int(r) else f"{r}{suffix}"
    if n >= 1_000_000_000:
        return _fmt(n / 1_000_000_000, "G")
    if n >= 1_000_000:
        return _fmt(n / 1_000_000, "M")
    if n >= 1_000:
        return _fmt(n / 1_000, "k")
    return str(n)


_SHAPE_ACRONYMS = {"GNP", "DAG"}

def fmt_shape(shape: str) -> str:
    """Mixed-case shape name with spaces; SHUFFLED → shfld."""
    if shape.endswith("_SHUFFLED"):
        return fmt_shape(shape[:-9]) + " shfld."
    parts = shape.split("_")
    result = []
    for p in parts:
        if p in _SHAPE_ACRONYMS:
            result.append(p)
        elif p == "OF":
            result.append("of")
        else:
            result.append(p.lower())
    s = " ".join(result)
    return s[0].upper() + s[1:] if s else s


def fmt_pct_of_baseline(value: float, baseline: float) -> str:
    pct = 100.0 * value / baseline
    if pct >= 100:
        return f"{pct:.0f}%"
    return f"{pct:.1f}%"


def format_ops_cell(
    ms: float | None,
    baseline_ms: float | None,
    *,
    is_baseline: bool,
) -> str:
    if ms is None or ms <= 0:
        return "—"
    ops = 1000.0 / ms
    text = f"{fmt_ops_value(ops)} op/s"
    if is_baseline or baseline_ms is None or baseline_ms <= 0:
        return text
    pct = 100.0 * (1000.0 / ms) / (1000.0 / baseline_ms)
    return f"{text} ({fmt_pct_of_baseline(pct, 100.0)})"


def format_bytes_cell(
    value: float | None,
    baseline: float | None,
    *,
    is_baseline: bool,
) -> str:
    if value is None:
        return "—"
    text = fmt_bytes_value(value)
    if is_baseline or baseline is None or baseline <= 0:
        return text
    return f"{text} ({fmt_pct_of_baseline(value, baseline)})"


# ---------------------------------------------------------------------------
# Single-file conversion (--single mode)
# ---------------------------------------------------------------------------


def replace_ms_cell(match: re.Match[str]) -> str:
    ms = float(match.group("ms"))
    if ms <= 0:
        raise ValueError(f"non-positive time value: {match.group(0)!r}")
    ratio = match.group("ratio") or ""
    return f"{fmt_ops(ms)} op/s{ratio}"


def extract_chapter(text: str, chapter: int) -> str:
    headings = list(CHAPTER_HEADING.finditer(text))
    start = None
    end = len(text)
    for i, match in enumerate(headings):
        if int(match.group("num")) == chapter:
            start = match.start()
            if i + 1 < len(headings):
                end = headings[i + 1].start()
            break
    if start is None:
        raise ValueError(f"chapter {chapter} heading not found")
    return text[start:end]


def extract_preamble_and_chapter_1(text: str) -> str:
    for match in CHAPTER_HEADING.finditer(text):
        if int(match.group("num")) == 2:
            return text[: match.start()].rstrip() + "\n"
    raise ValueError("chapter 2 heading ('## 2 …') not found")


def transform_times_to_ops(text: str) -> str:
    return MS_CELL.sub(replace_ms_cell, extract_preamble_and_chapter_1(text))


def convert_times_file(src: Path, dst: Path) -> None:
    text = src.read_text(encoding="utf-8")
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(transform_times_to_ops(text), encoding="utf-8")


# ---------------------------------------------------------------------------
# Parsing helpers
# ---------------------------------------------------------------------------


def parse_version(version: str) -> tuple[int, ...]:
    return tuple(int(p) for p in version.split("."))


def parse_spaced_number(raw: str) -> float:
    return float(raw.replace(" ", ""))


def parse_ms_cell(cell: str) -> float | None:
    cell = cell.strip()
    if cell in MISSING:
        return None
    match = MS_VALUE_RE.match(cell)
    if match is None:
        raise ValueError(f"unrecognized ms cell: {cell!r}")
    return float(match.group("ms"))


def parse_bytes_cell(cell: str) -> float | None:
    cell = cell.strip()
    if cell in MISSING:
        return None
    match = BYTES_VALUE_RE.match(cell)
    if match is None:
        raise ValueError(f"unrecognized bytes cell: {cell!r}")
    if match.group("num") is None:
        return 0.0
    return parse_spaced_number(match.group("num"))


@dataclass(frozen=True)
class ReportRef:
    path: Path
    version: str
    kind: str   # "synthetic" or "realworld"
    machine: str

    @property
    def version_key(self) -> tuple[int, ...]:
        return parse_version(self.version)

    @property
    def label(self) -> str:
        return f"Graphity {self.version}"


@dataclass
class DataRow:
    shape: str
    meta: dict[str, str] = field(default_factory=dict)
    values: dict[str, float | None] = field(default_factory=dict)

    @property
    def key(self) -> str:
        nodes = self.meta.get("Nodes")
        return f"{self.shape}|{nodes}" if nodes else self.shape

    @property
    def chart_label(self) -> str:
        nodes = self.meta.get("Nodes")
        return f"{self.shape} n={nodes}" if nodes else self.shape


ROW_KEY_HEADERS = ("Shape", "Dataset")


@dataclass(frozen=True)
class SectionId:
    """Number-independent identity of a report section, stable across versions."""

    algo: str
    variant: str = ""
    n_value: int | None = None


@dataclass
class SectionTable:
    title: str
    meta_columns: list[str]
    lib_columns: list[str]
    rows: list[DataRow]
    key_column: str = "Shape"
    sid: SectionId = SectionId("")
    source_number: tuple[int, ...] = (999,)


def parse_section_id(title: str) -> SectionId:
    """Strip the (run-dependent) section number and split off variant / node count."""
    match = SECTION_TITLE_RE.match(title.strip())
    rest = (match.group("rest") if match else title).strip()

    n_value: int | None = None
    n_match = N_SUFFIX_RE.search(rest)
    if n_match:
        n_value = int(n_match.group("n").replace(" ", ""))
        rest = rest[: n_match.start()].rstrip()

    variant = ""
    variant_match = VARIANT_SUFFIX_RE.search(rest)
    if variant_match:
        variant = variant_match.group("variant").strip()
        rest = rest[: variant_match.start()].rstrip()

    return SectionId(algo=rest, variant=variant, n_value=n_value)


def parse_section_number(title: str) -> tuple[int, ...]:
    match = SECTION_TITLE_RE.match(title.strip())
    if match is None:
        return (999,)
    return tuple(int(p) for p in match.group("num").split("."))


def parse_report_filename(path: Path) -> ReportRef:
    match = FILENAME_RE.match(path.name)
    if match is None:
        raise ValueError(
            f"unexpected source filename (expected <version>-<kind>-<machine>.md): {path.name}"
        )
    return ReportRef(
        path=path,
        version=match.group("version"),
        kind=match.group("kind"),
        machine=match.group("machine"),
    )


def extract_header_block(text: str) -> str | None:
    match = HEADER_BLOCK_RE.search(text)
    return match.group("body") if match else None


def parse_sections(
    text: str,
    *,
    value_parser,
) -> list[SectionTable]:
    sections: list[SectionTable] = []
    matches = list(SECTION_RE.finditer(text))
    for i, match in enumerate(matches):
        title = match.group("title").strip()
        body = text[match.end() : matches[i + 1].start() if i + 1 < len(matches) else len(text)]
        table = _parse_metric_table(body, value_parser)
        if table is not None:
            table.title = title
            table.sid = parse_section_id(title)
            table.source_number = parse_section_number(title)
            sections.append(table)
    return sections


def _parse_metric_table(body: str, value_parser) -> SectionTable | None:
    lines = [ln.strip() for ln in body.splitlines() if ln.strip().startswith("|")]
    if len(lines) < 3:
        return None
    headers = [c.strip() for c in lines[0].strip("|").split("|")]
    if not headers or headers[0] not in ROW_KEY_HEADERS:
        return None

    meta_columns = [h for h in headers[1:] if h in META_HEADERS]
    lib_columns = [h for h in headers[1:] if h in LIB_HEADERS]
    if not lib_columns:
        return None

    header_index = {h: i for i, h in enumerate(headers)}
    rows: list[DataRow] = []
    for line in lines[2:]:
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) != len(headers):
            raise ValueError(f"table row column mismatch: {line}")
        shape = cells[0]
        meta = {h: cells[header_index[h]] for h in meta_columns}
        values: dict[str, float | None] = {}
        for lib in lib_columns:
            values[lib] = value_parser(cells[header_index[lib]])
        rows.append(DataRow(shape=shape, meta=meta, values=values))
    return SectionTable(
        title="",
        meta_columns=meta_columns,
        lib_columns=lib_columns,
        rows=rows,
        key_column=headers[0],
    )


def parse_times_perf_chapter(text: str) -> list[SectionTable]:
    try:
        return parse_sections(extract_chapter(text, 1), value_parser=parse_ms_cell)
    except ValueError:
        return []


def parse_bytes_chapter(text: str, chapter: int) -> list[SectionTable]:
    try:
        return parse_sections(extract_chapter(text, chapter), value_parser=parse_bytes_cell)
    except ValueError:
        return []


# ---------------------------------------------------------------------------
# Section grouping and renumbering
# ---------------------------------------------------------------------------


@dataclass
class SectionGroup:
    """One ``###`` block of the combined report: an algorithm across all node counts."""

    heading: str                          # e.g. "1.6 TopOrder"
    entries: list[tuple[SectionId, str]]  # (identity, display title) ordered by n


def _fmt_spaced_int(value: int) -> str:
    return f"{value:,}".replace(",", " ")


def _group_sort_key(
    group: tuple[str, str], source_number: tuple[int, ...]
) -> tuple:
    """Order groups: synthetic before variants, then by canonical algorithm order.

    Algorithms outside ``ALGO_ORDER`` (the memory sub-chapters) fall back to the
    lowest section number they had in any source file, which preserves their
    original relative order.
    """
    algo, variant = group
    try:
        algo_rank = ALGO_ORDER.index(algo)
    except ValueError:
        algo_rank = len(ALGO_ORDER)
    return (bool(variant), variant, algo_rank, source_number, algo)


def build_section_groups(
    sections_by_version: dict[str, list[SectionTable]],
    chapter: int,
) -> list[SectionGroup]:
    """Union the sections of all versions and renumber them for the combined report.

    Sections are collected by their number-independent :class:`SectionId`, so the
    same algorithm merges across versions even when the source files number it
    differently. The emitted numbers are derived from the union, which makes them
    independent of which algorithms a single run happened to cover.
    """
    ids_by_group: dict[tuple[str, str], list[SectionId]] = {}
    hint_by_group: dict[tuple[str, str], tuple[int, ...]] = {}
    for sections in sections_by_version.values():
        for section in sections:
            group = (section.sid.algo, section.sid.variant)
            ids = ids_by_group.setdefault(group, [])
            if section.sid not in ids:
                ids.append(section.sid)
            previous = hint_by_group.get(group)
            if previous is None or section.source_number < previous:
                hint_by_group[group] = section.source_number

    ordered_groups = sorted(
        ids_by_group, key=lambda g: _group_sort_key(g, hint_by_group[g])
    )

    groups: list[SectionGroup] = []
    for idx, group in enumerate(ordered_groups, start=1):
        algo, variant = group
        number = f"{chapter}.{idx}"
        variant_suffix = f" — {variant}" if variant else ""
        entries: list[tuple[SectionId, str]] = []
        sids = sorted(ids_by_group[group], key=lambda s: s.n_value or 0)
        for sub_idx, sid in enumerate(sids, start=1):
            if sid.n_value is None:
                entries.append((sid, f"{number} · {algo}{variant_suffix}"))
            else:
                entries.append((
                    sid,
                    f"{number}.{sub_idx} · {algo}{variant_suffix}"
                    f" @ n = {_fmt_spaced_int(sid.n_value)}",
                ))
        groups.append(
            SectionGroup(heading=f"{number} {algo}{variant_suffix}", entries=entries)
        )
    return groups


def find_section(
    sections: list[SectionTable], sid: SectionId
) -> SectionTable | None:
    return next((s for s in sections if s.sid == sid), None)


# ---------------------------------------------------------------------------
# Column plans
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Column:
    """One output column: its heading, the version report it comes from, and the column there."""

    label: str
    version: str
    source: str

    @property
    def is_version(self) -> bool:
        return self.source == "Graphity"


@dataclass
class RowFrame:
    """The row axis of one section: the union of the rows of all contributing versions."""

    keys: list[str] = field(default_factory=list)
    shapes: list[str] = field(default_factory=list)
    metas: list[dict[str, str]] = field(default_factory=list)
    meta_columns: list[str] = field(default_factory=list)
    key_column: str = "Shape"


def _table_has_source(section: SectionTable, source: str) -> bool:
    if source not in section.lib_columns:
        return False
    return any(row.values.get(source) is not None for row in section.rows)


def visible_sections(
    view: View, sections_by_version: dict[str, list[SectionTable]]
) -> dict[str, list[SectionTable]]:
    """Drops the sections a view cannot show, before section numbers are assigned.

    The history view has Graphity columns only, so a section that no Graphity column covers —
    the WebGraph symmetrization cost is the first such section in the suite — would come out as
    a heading and a Contents entry with no table under them. Filtering has to happen before
    numbering, or the remaining sections would be numbered with gaps.
    """
    if view is View.PEERS:
        return sections_by_version
    sources = ("Graphity", *GRAPHITY_VARIANTS)
    return {
        version: [s for s in sections if any(_table_has_source(s, src) for src in sources)]
        for version, sections in sections_by_version.items()
    }


def _variant_suffix(source: str) -> str:
    """``' (reordered)'`` for a Graphity variant source, empty for plain Graphity or a peer."""
    return source.removeprefix("Graphity") if source in GRAPHITY_VARIANTS else ""


def _section_has_data(
    sections: list[SectionTable], sid: SectionId, source: str
) -> bool:
    section = find_section(sections, sid)
    if section is None or source not in section.lib_columns:
        return False
    return any(row.values.get(source) is not None for row in section.rows)


def plan_columns(
    view: View,
    reports: list[ReportRef],
    sections_by_version: dict[str, list[SectionTable]],
    sid: SectionId,
) -> list[Column]:
    """Decide which version supplies which column of one section, in output order."""
    if view is View.HISTORY:
        columns = [Column(ref.label, ref.version, "Graphity") for ref in reports]
        for source in GRAPHITY_VARIANTS:
            columns += [
                Column(f"{ref.label}{_variant_suffix(source)}", ref.version, source)
                for ref in reports
                if _section_has_data(sections_by_version.get(ref.version, []), sid, source)
            ]
        return columns

    # Peer view: one Graphity column plus one column per peer, each from the newest version
    # that measured it. A release benchmarked without peers must not empty their columns.
    columns = []
    for source in ("Graphity", *PEER_LIBS):
        for ref in reversed(reports):
            if not _section_has_data(sections_by_version.get(ref.version, []), sid, source):
                continue
            label = (
                f"{ref.label}{_variant_suffix(source)}"
                if source == "Graphity" or source in GRAPHITY_VARIANTS
                else source
            )
            columns.append(Column(label, ref.version, source))
            break
    return columns


def collect_rows(
    columns: list[Column],
    reports: list[ReportRef],
    sections_by_version: dict[str, list[SectionTable]],
    sid: SectionId,
) -> RowFrame:
    """Union the rows of every version that contributes a column, oldest version first."""
    contributing = {column.version for column in columns}
    frame = RowFrame()
    for ref in reports:
        if ref.version not in contributing:
            continue
        section = find_section(sections_by_version.get(ref.version, []), sid)
        if section is None:
            continue
        frame.key_column = section.key_column
        if not frame.meta_columns:
            frame.meta_columns = section.meta_columns
        for row in section.rows:
            if row.key in frame.keys:
                continue
            frame.keys.append(row.key)
            frame.shapes.append(row.shape)
            frame.metas.append(row.meta)
    return frame


def build_matrix(
    columns: list[Column],
    sections_by_version: dict[str, list[SectionTable]],
    sid: SectionId,
    keys: list[str],
) -> dict[str, list[float | None]]:
    matrix: dict[str, list[float | None]] = {}
    for column in columns:
        section = find_section(sections_by_version.get(column.version, []), sid)
        by_key = (
            {} if section is None
            else {row.key: row.values.get(column.source) for row in section.rows}
        )
        matrix[column.label] = [by_key.get(key) for key in keys]
    return matrix


def baseline_label_of(
    columns: list[Column], matrix: dict[str, list[float | None]]
) -> str:
    """The newest Graphity column with data - the reference for all factors and percentages.

    A benchmark that only older releases measured leaves the newest version's column empty.
    Falling back to the newest version that measured the section keeps the factors, the
    percentages and the chart of that section referring to the same column.
    """
    versions = [column.label for column in columns if column.is_version]
    for label in reversed(versions):
        if any(value is not None for value in matrix.get(label, [])):
            return label
    return versions[-1] if versions else ""


# ---------------------------------------------------------------------------
# Section-title helpers
# ---------------------------------------------------------------------------


def _gh_anchor(text: str) -> str:
    """GitHub Markdown anchor from heading text."""
    text = text.lower()
    text = re.sub(r'[^\w\s-]', '', text)
    text = re.sub(r'\s+', '-', text.strip())
    return '#' + text


def _clean_heading(title: str) -> str:
    """Remove middle-dot separator '·' from section titles."""
    return re.sub(r'\s*·\s*', ' ', title).strip()


# Known SNAP dataset sizes for real-world subscripts
_DATASET_SIZES: dict[str, tuple[int, int]] = {
    "web-Google":       (875_713,   5_105_039),
    "wiki-Talk":        (2_394_385, 5_021_410),
    "soc-LiveJournal1": (4_847_571, 68_993_773),
    "cit-Patents":      (3_774_768, 16_518_948),
    "cit-HepPh":        (34_546,    421_578),
}


def _fmt_shape_cell(
    shape: str,
    n_value: int | None,
    shape_edges_lookup: dict[tuple[str, int], int],
    shape_edges_per_node: dict[str, float] | None = None,
) -> str:
    display = fmt_shape(shape)
    if n_value is not None:
        edges = shape_edges_lookup.get((shape, n_value))
        if edges is None and shape_edges_per_node:
            epr = shape_edges_per_node.get(shape)
            if epr is not None:
                edges = round(epr * n_value)
        if edges is not None:
            return f"{display}<br><sub>{fmt_count(n_value)} nodes, {fmt_count(edges)} edges</sub>"
    return display


def _fmt_dataset_cell(dataset: str) -> str:
    sizes = _DATASET_SIZES.get(dataset)
    if sizes:
        nodes, edges = sizes
        return f"{dataset}<br><sub>{fmt_count(nodes)} nodes, {fmt_count(edges)} edges</sub>"
    return dataset


# ---------------------------------------------------------------------------
# SVG charts
# ---------------------------------------------------------------------------


def assign_series_colors(labels: list[str]) -> dict[str, str]:
    """Map series labels to colours: Graphity versions, their variants, then peers."""
    colors: dict[str, str] = {}
    plain = 0
    variants: dict[str, int] = {}
    for label in labels:
        match = GRAPHITY_LABEL_RE.match(label)
        if match is None:
            colors[label] = SERIES_COLORS.get(label, "#64748b")
        elif suffix := match.group("variant"):
            ramp = VARIANT_COLORS[suffix]
            seen = variants.get(suffix, 0)
            colors[label] = ramp[seen % len(ramp)]
            variants[suffix] = seen + 1
        else:
            colors[label] = GRAPHITY_COLORS[plain % len(GRAPHITY_COLORS)]
            plain += 1
    return colors


def render_grouped_bar_svg(
    labels: list[str],
    series: list[tuple[str, list[float | None]]],
    baseline_label: str,
    *,
    log_scale: bool = False,
) -> str:
    n_groups = len(labels)
    n_series = len(series)
    if n_groups == 0 or n_series == 0:
        return ""

    pct_values = [v for _, vals in series for v in vals if v is not None and v > 0]
    if not pct_values:
        return ""

    if log_scale:
        y_min = _nice_log_floor(min(pct_values))
        y_max = _nice_log_ceil(max(pct_values))
        if y_min >= y_max:
            y_max = y_min * 10
        ticks = _log_y_ticks(y_min, y_max)
    else:
        y_min = 0.0
        y_max = _nice_ceiling(max(100.0, max(pct_values)) * 1.08)
        ticks = _y_ticks(y_max)

    left, right, top, bottom = 56, 24, 36, 96
    group_gap = 18
    bar_gap = 3
    bar_w = 16
    group_w = n_series * bar_w + (n_series - 1) * bar_gap
    plot_w = max(n_groups * (group_w + group_gap) - group_gap, 120)
    plot_h = 220
    width = left + plot_w + right
    height = top + plot_h + bottom
    legend_row_h = 16
    legend_h = n_series * legend_row_h + 4
    height += legend_h
    top += legend_h

    if log_scale:
        log_min = math.log10(y_min)
        log_max = math.log10(y_max)

        def y_of(pct: float) -> float:
            clamped = min(max(pct, y_min), y_max)
            t = (math.log10(clamped) - log_min) / (log_max - log_min)
            return top + plot_h - t * plot_h
    else:

        def y_of(pct: float) -> float:
            return top + plot_h - (pct / y_max) * plot_h

    parts: list[str] = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}" role="img">',
        "<style>"
        "text{font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,sans-serif;"
        "font-size:11px;fill:#334155}"
        ".axis{stroke:#94a3b8;stroke-width:1;fill:none}"
        ".grid{stroke:#e2e8f0;stroke-width:1}"
        ".ylabel{fill:#64748b;font-size:10px}"
        ".tick{fill:#64748b;font-size:10px}"
        ".xlabel{font-size:10px}"
        "</style>",
        f'<rect x="0" y="0" width="{width}" height="{height}" fill="#ffffff"/>',
    ]

    colors = assign_series_colors([label for label, _ in series])

    for i, (label, _) in enumerate(series):
        ly = (i + 1) * legend_row_h - 2
        color = colors[label]
        parts.append(
            f'<rect x="{left}" y="{ly - 9}" width="12" height="12" rx="2" fill="{color}"/>'
        )
        parts.append(f'<text x="{left + 16}" y="{ly}" class="tick">{html.escape(label)}</text>')

    for tick in ticks:
        y = y_of(tick)
        parts.append(
            f'<line class="grid" x1="{left}" y1="{y:.1f}" x2="{left + plot_w}" y2="{y:.1f}"/>'
        )
        parts.append(
            f'<text class="tick" x="{left - 8}" y="{y + 3:.1f}" text-anchor="end">'
            f"{_fmt_tick_pct(tick)}</text>"
        )

    parts.append(
        f'<line class="axis" x1="{left}" y1="{top}" x2="{left}" y2="{top + plot_h}"/>'
    )
    parts.append(
        f'<line class="axis" x1="{left}" y1="{top + plot_h}" '
        f'x2="{left + plot_w}" y2="{top + plot_h}"/>'
    )
    ylabel = f"% of {baseline_label}" + (" (log)" if log_scale else "")
    parts.append(
        f'<text class="ylabel" x="16" y="{top + plot_h / 2:.1f}" '
        f'text-anchor="middle" transform="rotate(-90 16 {top + plot_h / 2:.1f})">'
        f"{html.escape(ylabel)}</text>"
    )

    y_floor = top + plot_h if not log_scale else y_of(y_min)

    for gi, label in enumerate(labels):
        gx = left + gi * (group_w + group_gap)
        tx = gx + group_w / 2
        ty = top + plot_h + 12
        parts.append(
            f'<text class="xlabel" x="{tx:.1f}" y="{ty:.1f}" text-anchor="end" '
            f'transform="rotate(-40 {tx:.1f} {ty:.1f})">{html.escape(label)}</text>'
        )
        for si, (series_label, values) in enumerate(series):
            pct = values[gi]
            if pct is None or pct <= 0:
                continue
            color = colors[series_label]
            bx = gx + si * (bar_w + bar_gap)
            by = y_of(pct)
            bh = y_floor - by
            if 0 < bh < 1.5:
                bh = 1.5
                by = y_floor - bh
            parts.append(
                f'<rect x="{bx:.1f}" y="{by:.1f}" width="{bar_w}" height="{bh:.1f}" '
                f'fill="{color}" rx="1">'
                f"<title>{html.escape(series_label)} · {html.escape(label)}: "
                f"{_fmt_tick_pct(pct)}</title>"
                f"</rect>"
            )

    if y_min < 100 <= y_max:
        y100 = y_of(100.0)
        parts.append(
            f'<line x1="{left}" y1="{y100:.1f}" x2="{left + plot_w}" y2="{y100:.1f}" '
            f'stroke="#94a3b8" stroke-width="1" stroke-dasharray="4 3"/>'
        )
    parts.append("</svg>")
    return "\n".join(parts) + "\n"


def _nice_step(span: float, max_ticks: int = 10) -> float:
    if span <= 0:
        return 1.0
    target = span / max(max_ticks - 1, 1)
    exp = math.floor(math.log10(target))
    base = 10 ** exp
    for mult in (1.0, 2.0, 2.5, 5.0, 10.0, 20.0):
        step = mult * base
        n_ticks = int(math.ceil(span / step - 1e-12)) + 1
        if n_ticks <= max_ticks:
            return step
    return 10.0 * base


def _nice_ceiling(value: float, max_ticks: int = 10) -> float:
    if value <= 100:
        return 100.0
    if value <= 110:
        return 110.0
    if value <= 125:
        return 125.0
    step = _nice_step(value, max_ticks=max_ticks)
    return math.ceil(value / step - 1e-12) * step


def _nice_log_floor(value: float) -> float:
    if value <= 0:
        return 0.1
    return 10 ** math.floor(math.log10(value))


def _nice_log_ceil(value: float) -> float:
    if value <= 0:
        return 1.0
    return 10 ** math.ceil(math.log10(value))


def _y_ticks(y_max: float, max_ticks: int = 10) -> list[float]:
    if y_max <= 100:
        return [0.0, 25.0, 50.0, 75.0, 100.0]
    if y_max <= 110:
        return [0.0, 25.0, 50.0, 75.0, 100.0, 110.0]
    if y_max <= 125:
        return [0.0, 25.0, 50.0, 75.0, 100.0, 125.0]
    step = _nice_step(y_max, max_ticks=max_ticks)
    ticks: list[float] = []
    t = 0.0
    while t <= y_max + step * 1e-9 and len(ticks) < max_ticks:
        ticks.append(round(t, 10))
        t += step
    if ticks[-1] < y_max - 1e-9 and len(ticks) < max_ticks:
        ticks.append(round(y_max, 10))
    elif ticks[-1] < y_max - 1e-9:
        ticks[-1] = round(y_max, 10)
    return ticks


def _log_y_ticks(y_min: float, y_max: float) -> list[float]:
    ticks: list[float] = []
    exp = int(math.floor(math.log10(y_min)))
    end = int(math.ceil(math.log10(y_max)))
    for e in range(exp, end + 1):
        tick = 10.0 ** e
        if y_min <= tick <= y_max:
            ticks.append(tick)
    if 100.0 not in ticks and y_min <= 100.0 <= y_max:
        ticks.append(100.0)
        ticks.sort()
    return ticks or [y_min, y_max]


def _fmt_tick_pct(pct: float) -> str:
    if abs(pct) < 1e-12:
        return "0%"
    if pct >= 10:
        return f"{pct:.0f}%"
    if pct >= 1:
        return f"{pct:.1f}%"
    return f"{pct:.2f}%"


def section_fig_slug(title: str) -> str:
    slug = title.lower().replace("·", " ").replace("@", " ").replace("=", " ")
    slug = re.sub(r"[^a-z0-9]+", "-", slug)
    return slug.strip("-")


# ---------------------------------------------------------------------------
# Combined report builder
# ---------------------------------------------------------------------------


def _col_header(label: str) -> str:
    """Format a column header: break before a variant suffix to reduce column width."""
    for suffix in VARIANT_LABELS:
        if suffix in label:
            return label.replace(suffix, f"<br>{suffix.lstrip()}")
    return label


def _fmt_ms_factor(ms: float, baseline_ms: float | None, *, is_baseline: bool) -> str:
    """Format a Times cell: raw ms for the baseline, ms + factor for others."""
    if is_baseline or baseline_ms is None or baseline_ms <= 0:
        return fmt_ms(ms)
    factor = ms / baseline_ms
    if factor >= 100:
        return f"{fmt_ms(ms)} ({factor:.0f}×)"
    if factor >= 10:
        return f"{fmt_ms(ms)} ({factor:.0f}×)"
    return f"{fmt_ms(ms)} ({factor:.1f}×)"


def _build_perf_section(
    lines: list[str],
    *,
    key_column: str,
    all_column_labels: list[str],
    ms_matrix: dict[str, list[float | None]],
    chart_labels: list[str],
    row_shapes: list[str],
    row_metas: list[dict[str, str]],
    meta_columns: list[str],
    baseline_label: str,
    machine: str,
    machine_fig_dir: Path,
    fig_slug_base: str,
    n_value: int | None,
    shape_edges_lookup: dict[tuple[str, int], int],
    shape_edges_per_node: dict[str, float] | None = None,
) -> None:
    """Emit #### Times/Throughput tables and chart for one n-value block."""
    n = len(row_shapes)

    all_column_labels = [
        col for col in all_column_labels
        if any(v is not None for v in ms_matrix.get(col, []))
    ]
    if not all_column_labels:
        return

    header_cols = [*meta_columns, *(_col_header(c) for c in all_column_labels)]
    n_label = f" @ {fmt_count(n_value)} nodes" if n_value is not None else ""

    # ── Times table ──────────────────────────────────────────────────────────
    lines.append(f"#### Times{n_label} (ms/op, lower is better)")
    lines.append("")
    lines.append(f"| {key_column} | " + " | ".join(header_cols) + " |")
    lines.append("|---|" + "|".join(["---:" for _ in header_cols]) + "|")
    for i, shape in enumerate(row_shapes):
        meta = row_metas[i]
        baseline_ms = ms_matrix[baseline_label][i] if baseline_label in ms_matrix else None
        if key_column == "Dataset":
            shape_cell = _fmt_dataset_cell(shape)
        else:
            shape_cell = _fmt_shape_cell(shape, n_value, shape_edges_lookup, shape_edges_per_node)
        cells = [meta.get(h, "—") for h in meta_columns]
        for col in all_column_labels:
            ms = ms_matrix[col][i]
            cells.append("—" if ms is None else _fmt_ms_factor(ms, baseline_ms, is_baseline=(col == baseline_label)))
        lines.append("| " + shape_cell + " | " + " | ".join(cells) + " |")
    lines.append("")

    # ── Throughput table ──────────────────────────────────────────────────────
    lines.append(f"#### Throughput{n_label} (op/s, higher is better)")
    lines.append("")
    lines.append(f"| {key_column} | " + " | ".join(header_cols) + " |")
    lines.append("|---|" + "|".join(["---:" for _ in header_cols]) + "|")
    for i, shape in enumerate(row_shapes):
        meta = row_metas[i]
        baseline_ms = ms_matrix[baseline_label][i] if baseline_label in ms_matrix else None
        if key_column == "Dataset":
            shape_cell = _fmt_dataset_cell(shape)
        else:
            shape_cell = _fmt_shape_cell(shape, n_value, shape_edges_lookup, shape_edges_per_node)
        cells = [meta.get(h, "—") for h in meta_columns]
        for col in all_column_labels:
            ms = ms_matrix[col][i]
            cells.append(format_ops_cell(ms, baseline_ms, is_baseline=(col == baseline_label)))
        lines.append("| " + shape_cell + " | " + " | ".join(cells) + " |")
    lines.append("")

    # ── Chart ────────────────────────────────────────────────────────────────
    chart_series: list[tuple[str, list[float | None]]] = []
    for col in all_column_labels:
        pcts: list[float | None] = []
        for i in range(n):
            ms = ms_matrix[col][i]
            baseline_ms = ms_matrix[baseline_label][i] if baseline_label in ms_matrix else None
            if ms is None or ms <= 0 or baseline_ms is None or baseline_ms <= 0:
                pcts.append(None)
            else:
                pcts.append(100.0 * (1000.0 / ms) / (1000.0 / baseline_ms))
        if any(v is not None for v in pcts):
            chart_series.append((col, pcts))

    # A chart whose only series is the baseline is a row of bars at 100%. The peer view has one
    # such block per Graphity-only algorithm and per node count above the peers' reach.
    if any(label != baseline_label for label, _ in chart_series):
        svg = render_grouped_bar_svg(chart_labels, chart_series, baseline_label, log_scale=False)
        if svg:
            fig_name = f"{section_fig_slug(fig_slug_base)}.svg"
            (machine_fig_dir / fig_name).write_text(svg, encoding="utf-8")
            rel = f"fig/{machine}/{fig_name}"
            lines.append(f"![{_clean_heading(fig_slug_base)}]({rel})")
            lines.append("")


def _build_bytes_section(
    lines: list[str],
    *,
    title: str,
    key_column: str,
    all_column_labels: list[str],
    bytes_matrix: dict[str, list[float | None]],
    chart_labels: list[str],
    row_shapes: list[str],
    row_metas: list[dict[str, str]],
    meta_columns: list[str],
    baseline_label: str,
    machine: str,
    machine_fig_dir: Path,
    table_title: str = "",
    dual_charts: bool = True,
    show_heading: bool = True,
    log_scale: bool = False,
    n_value: int | None = None,
    shape_edges_lookup: dict[tuple[str, int], int] | None = None,
    shape_edges_per_node: dict[str, float] | None = None,
) -> None:
    """Emit bytes table and charts for one memory/allocations subsection."""
    n = len(row_shapes)

    if show_heading:
        lines.append(f"### {_clean_heading(title)}")
        lines.append("")

    if table_title:
        lines.append(f"**{table_title}**")
        lines.append("")

    # Fold Nodes/Edges/Edges-per-Node into shape-column subscript if present
    _NODE_EDGE_COLS = {"Nodes", "Edges", "Edges/Node"}
    fold = _NODE_EDGE_COLS & set(meta_columns)
    display_meta = [h for h in meta_columns if h not in _NODE_EDGE_COLS]

    header_cols = [*display_meta, *(_col_header(c) for c in all_column_labels)]
    lines.append(f"| {key_column} | " + " | ".join(header_cols) + " |")
    lines.append("|---|" + "|".join(["---:" for _ in header_cols]) + "|")

    abs_matrix: dict[str, list[float | None]] = {c: [] for c in all_column_labels}
    for i, shape in enumerate(row_shapes):
        meta = row_metas[i]
        baseline_val = bytes_matrix.get(baseline_label, [None] * n)[i]

        # Build shape cell with optional nodes/edges subscript
        shape_display = fmt_shape(shape)
        if fold:
            # ch2 memory: Nodes/Edges come from meta columns
            ns = meta.get("Nodes", "").replace(" ", "")
            es = meta.get("Edges", "").replace(" ", "")
            if ns.isdigit() and es.isdigit():
                sub = f"{fmt_count(int(ns))} nodes, {fmt_count(int(es))} edges"
                shape_cell = f"{shape_display}<br><sub>{sub}</sub>"
            else:
                shape_cell = shape_display
        elif n_value is not None:
            # ch3 alloc: use n_value + shape_edges_lookup
            edges = (shape_edges_lookup or {}).get((shape, n_value))
            if edges is None and shape_edges_per_node:
                epr = shape_edges_per_node.get(shape)
                if epr is not None:
                    edges = round(epr * n_value)
            if edges is not None:
                sub = f"{fmt_count(n_value)} nodes, {fmt_count(edges)} edges"
            else:
                sub = f"{fmt_count(n_value)} nodes"
            shape_cell = f"{shape_display}<br><sub>{sub}</sub>"
        else:
            shape_cell = shape_display

        cells = [meta.get(h, "—") for h in display_meta]
        for col in all_column_labels:
            val = bytes_matrix.get(col, [None] * n)[i]
            abs_matrix[col].append(val)
            cells.append(
                format_bytes_cell(val, baseline_val, is_baseline=(col == baseline_label))
            )
        lines.append("| " + shape_cell + " | " + " | ".join(cells) + " |")
    lines.append("")

    graphity_cols = [
        c for c in all_column_labels
        if c.startswith("Graphity ") and not any(s in c for s in VARIANT_LABELS)
    ]

    if dual_charts:
        # Chart A: Graphity versions + WebGraph
        league_cols = list(graphity_cols)
        if "WebGraph" in abs_matrix and any(v is not None for v in abs_matrix["WebGraph"]):
            league_cols.append("WebGraph")
        _emit_bytes_chart(
            lines,
            machine=machine,
            machine_fig_dir=machine_fig_dir,
            title=title,
            chart_labels=chart_labels,
            columns=league_cols,
            abs_matrix=abs_matrix,
            baseline_label=baseline_label,
            caption="Graphity & WebGraph",
            fig_suffix="-a-league",
            n=n,
        )
        # Chart B: all libraries
        has_heavy = any(
            c in abs_matrix and any(v is not None for v in abs_matrix[c])
            for c in ("JGraphT", "Guava")
        )
        if has_heavy:
            _emit_bytes_chart(
                lines,
                machine=machine,
                machine_fig_dir=machine_fig_dir,
                title=title,
                chart_labels=chart_labels,
                columns=all_column_labels,
                abs_matrix=abs_matrix,
                baseline_label=baseline_label,
                caption="All libraries",
                fig_suffix="-b-vs-graphity",
                n=n,
            )
    else:
        # Single chart, no caption text (issue 6: alloc has only one chart, no title)
        _emit_bytes_chart(
            lines,
            machine=machine,
            machine_fig_dir=machine_fig_dir,
            title=title,
            chart_labels=chart_labels,
            columns=all_column_labels,
            abs_matrix=abs_matrix,
            baseline_label=baseline_label,
            caption="",
            fig_suffix="",
            n=n,
            log_scale=log_scale,
        )


def _emit_bytes_chart(
    lines: list[str],
    *,
    machine: str,
    machine_fig_dir: Path,
    title: str,
    chart_labels: list[str],
    columns: list[str],
    abs_matrix: dict[str, list[float | None]],
    baseline_label: str,
    caption: str,
    fig_suffix: str,
    n: int,
    log_scale: bool = False,
) -> None:
    series: list[tuple[str, list[float | None]]] = []
    for col in columns:
        if col not in abs_matrix:
            continue
        vals = abs_matrix[col]
        baseline_vals = abs_matrix.get(baseline_label, [None] * n)
        pcts: list[float | None] = []
        for i in range(n):
            v = vals[i]
            b = baseline_vals[i]
            if v is None or b is None or b <= 0:
                pcts.append(None)
            else:
                pcts.append(100.0 * v / b)
        if any(p is not None for p in pcts):
            series.append((col, pcts))

    if not any(label != baseline_label for label, _ in series):
        return

    svg = render_grouped_bar_svg(chart_labels, series, baseline_label, log_scale=log_scale)
    if not svg:
        return
    fig_name = f"{section_fig_slug(title)}{fig_suffix}.svg"
    (machine_fig_dir / fig_name).write_text(svg, encoding="utf-8")
    rel = f"fig/{machine}/{fig_name}"
    if caption:
        lines.append(f"**{caption}**")
        lines.append("")
    lines.append(f"![{title}]({rel})")
    lines.append("")


def build_combined_for_machine(
    kind: str,
    machine: str,
    reports: list[ReportRef],
    output_dir: Path,
    view: View,
) -> str:
    reports = sorted(reports, key=lambda r: r.version_key)
    newest = reports[-1]

    fig_slug = f"{kind}-{machine}-{view.value}"
    fig_dir = output_dir / "fig"
    machine_fig_dir = fig_dir / fig_slug
    if machine_fig_dir.exists():
        for stale in machine_fig_dir.glob("*.svg"):
            stale.unlink()
    machine_fig_dir.mkdir(parents=True, exist_ok=True)

    # Parse all source files
    times_perf: dict[str, list[SectionTable]] = {}
    times_mem: dict[str, list[SectionTable]] = {}
    times_alloc: dict[str, list[SectionTable]] = {}
    header = ""

    for ref in reports:
        text = ref.path.read_text(encoding="utf-8")
        if ref.version == newest.version:
            header = extract_header_block(text) or ""
        times_perf[ref.version] = parse_times_perf_chapter(text)
        times_mem[ref.version] = parse_bytes_chapter(text, 2)
        times_alloc[ref.version] = parse_bytes_chapter(text, 3)

    visible_perf = visible_sections(view, times_perf)
    visible_mem = visible_sections(view, times_mem)
    visible_alloc = visible_sections(view, times_alloc)

    has_mem = any(visible_mem.values())
    has_alloc = any(visible_alloc.values())

    baseline_label = newest.label
    # A benchmark the newest version did not measure falls back to the newest one that did,
    # so the chapter intros cannot name a single baseline for every table.
    baseline_note = (
        f"**{baseline_label}**, or the newest version present in a table it is missing from"
    )

    # Group perf sections by algorithm, renumbered from the union of all versions
    perf_groups = build_section_groups(visible_perf, 1)
    mem_groups = build_section_groups({v: s for v, s in visible_mem.items() if s}, 2)
    alloc_groups = build_section_groups({v: s for v, s in visible_alloc.items() if s}, 3)

    # Build shape → edges lookup from memory data (for subscripts in perf tables)
    shape_edges_lookup: dict[tuple[str, int], int] = {}
    for v_secs in times_mem.values():
        for sec in v_secs:
            if "Nodes" in sec.meta_columns and "Edges" in sec.meta_columns:
                for row in sec.rows:
                    ns = row.meta.get("Nodes", "").replace(" ", "")
                    es = row.meta.get("Edges", "").replace(" ", "")
                    if ns.isdigit() and es.isdigit():
                        shape_edges_lookup[(row.shape, int(ns))] = int(es)
    # Derive edges-per-node ratio for extrapolating to n-values without direct data
    shape_edges_per_node: dict[str, float] = {}
    for (shape, nodes), edges in shape_edges_lookup.items():
        if nodes > 0 and shape not in shape_edges_per_node:
            shape_edges_per_node[shape] = edges / nodes

    # Build intro
    if view is View.PEERS:
        intro_lines: list[str] = [
            f"# Graphity vs. JGraphT, Guava and WebGraph — {kind} / {machine}",
            "",
            f"Graphity `{newest.version}` against the peer libraries. Peer numbers come from the "
            f"newest run that measured them, which need not be the run of the newest Graphity "
            f"version; every column states the version it belongs to. Blocks without a peer column "
            f"are Graphity-only benchmarks or graph sizes beyond the peers' reach — for the "
            f"comparison across Graphity versions see "
            f"[`{kind}-{machine}-{View.HISTORY.value}.md`]({kind}-{machine}-{View.HISTORY.value}.md).",
            "",
        ]
    else:
        intro_lines = [
            f"# Graphity Version History — {kind} / {machine}",
            "",
            "Benchmarked versions: " + ", ".join(f"`{r.version}`" for r in reports)
            + ". Peer libraries are left out on purpose: they are re-measured per run, so a "
            "moving peer column would say nothing about Graphity. For the comparison against "
            f"them see [`{kind}-{machine}-{View.PEERS.value}.md`]({kind}-{machine}-{View.PEERS.value}.md).",
            "",
        ]
    if header:
        intro_lines += ["```", header, "```", ""]

    lines: list[str] = intro_lines

    # ── Table of contents ────────────────────────────────────────────────────
    toc: list[str] = ["## Contents", ""]
    toc.append(f"- [1 Performance]({_gh_anchor('1 Performance')})")
    for group in perf_groups:
        toc.append(f"  - [{group.heading}]({_gh_anchor(group.heading)})")
    if has_mem:
        toc.append(f"- [2 Memory Footprint]({_gh_anchor('2 Memory Footprint')})")
        for group in mem_groups:
            toc.append(f"  - [{group.heading}]({_gh_anchor(group.heading)})")
    if has_alloc:
        toc.append(f"- [3 Allocations]({_gh_anchor('3 Allocations')})")
        for group in alloc_groups:
            toc.append(f"  - [{group.heading}]({_gh_anchor(group.heading)})")
    toc.append("")
    lines.extend(toc)

    # ── Chapter 1: Performance ───────────────────────────────────────────────
    lines.append("## 1 Performance")
    lines.append("")
    lines.append(
        f"Times (ms/op, lower is better) and throughput (op/s, higher is better) "
        f"for each algorithm and graph size. Non-baseline cells show the factor "
        f"relative to {baseline_note} (= 100% in the throughput table)."
    )
    lines.append("")

    for group in perf_groups:
        lines.append(f"### {group.heading}")
        lines.append("")

        for sid, title in group.entries:
            columns = plan_columns(view, reports, times_perf, sid)
            frame = collect_rows(columns, reports, times_perf, sid)
            if not frame.keys:
                continue

            ms_matrix = build_matrix(columns, times_perf, sid, frame.keys)

            if frame.key_column == "Dataset":
                chart_labels = list(frame.shapes)  # keep dataset names as-is
            else:
                chart_labels = [fmt_shape(shape) for shape in frame.shapes]

            _build_perf_section(
                lines,
                key_column=frame.key_column,
                all_column_labels=[column.label for column in columns],
                ms_matrix=ms_matrix,
                chart_labels=chart_labels,
                row_shapes=frame.shapes,
                row_metas=frame.metas,
                meta_columns=frame.meta_columns,
                baseline_label=baseline_label_of(columns, ms_matrix),
                machine=fig_slug,
                machine_fig_dir=machine_fig_dir,
                fig_slug_base=title,
                n_value=sid.n_value,
                shape_edges_lookup=shape_edges_lookup,
                shape_edges_per_node=shape_edges_per_node,
            )

    # ── Chapter 2: Memory ────────────────────────────────────────────────────
    if has_mem:
        lines.append("## 2 Memory Footprint")
        lines.append("")
        chart_note = (
            " Two charts per section: *Graphity & WebGraph* (same league) and *All libraries*."
            if view is View.PEERS else ""
        )
        lines.append(
            f"Retained heap in bytes for a fully built graph. Lower is better. "
            f"Baseline (= 100%): {baseline_note}.{chart_note}"
        )
        lines.append("")

        for group in mem_groups:
            for sid, title in group.entries:
                _append_bytes_comparison_section(
                    lines,
                    sid=sid,
                    title=title,
                    view=view,
                    reports=reports,
                    sections_by_version=times_mem,
                    machine=fig_slug,
                    machine_fig_dir=machine_fig_dir,
                    table_title="Memory footprint in bytes (lower is better)",
                    dual_charts=view is View.PEERS,
                )

    # ── Chapter 3: Allocations ───────────────────────────────────────────────
    if has_alloc:
        lines.append("## 3 Allocations")
        lines.append("")
        lines.append(
            f"Bytes allocated per algorithm invocation (JMH `-prof gc`). "
            f"Lower is better. Baseline (= 100%): {baseline_note}."
        )
        lines.append("")

        for group in alloc_groups:
            lines.append(f"### {group.heading}")
            lines.append("")
            for sid, title in group.entries:
                n_label = f" @ {fmt_count(sid.n_value)} nodes" if sid.n_value else ""
                _append_bytes_comparison_section(
                    lines,
                    sid=sid,
                    title=title,
                    view=view,
                    reports=reports,
                    sections_by_version=times_alloc,
                    machine=fig_slug,
                    machine_fig_dir=machine_fig_dir,
                    table_title=f"Allocations{n_label} in bytes (lower is better)",
                    dual_charts=False,
                    show_heading=False,
                    n_value=sid.n_value,
                    shape_edges_lookup=shape_edges_lookup,
                    shape_edges_per_node=shape_edges_per_node,
                )

    return "\n".join(lines).rstrip() + "\n"


def _append_bytes_comparison_section(
    lines: list[str],
    *,
    sid: SectionId,
    title: str,
    view: View,
    reports: list[ReportRef],
    sections_by_version: dict[str, list[SectionTable]],
    machine: str,
    machine_fig_dir: Path,
    table_title: str = "",
    dual_charts: bool = True,
    show_heading: bool = True,
    n_value: int | None = None,
    shape_edges_lookup: dict[tuple[str, int], int] | None = None,
    shape_edges_per_node: dict[str, float] | None = None,
) -> None:
    """Emit one bytes (memory/alloc) subsection with the columns the view asks for."""
    columns = plan_columns(view, reports, sections_by_version, sid)
    frame = collect_rows(columns, reports, sections_by_version, sid)
    if not frame.keys:
        return

    bytes_matrix = build_matrix(columns, sections_by_version, sid, frame.keys)

    chart_labels = []
    for shape, meta in zip(frame.shapes, frame.metas):
        shape_fmt = fmt_shape(shape)
        nodes_str = meta.get("Nodes", "").replace(" ", "")
        if nodes_str.isdigit():
            chart_labels.append(f"{shape_fmt} {fmt_count(int(nodes_str))}")
        else:
            chart_labels.append(shape_fmt)

    _build_bytes_section(
        lines,
        title=title,
        key_column=frame.key_column,
        all_column_labels=[column.label for column in columns],
        bytes_matrix=bytes_matrix,
        chart_labels=chart_labels,
        row_shapes=frame.shapes,
        row_metas=frame.metas,
        meta_columns=frame.meta_columns,
        baseline_label=baseline_label_of(columns, bytes_matrix),
        machine=machine,
        machine_fig_dir=machine_fig_dir,
        table_title=table_title,
        dual_charts=dual_charts,
        show_heading=show_heading,
        n_value=n_value,
        shape_edges_lookup=shape_edges_lookup,
        shape_edges_per_node=shape_edges_per_node,
    )


def generate_combined(
    source_dir: Path,
    output_dir: Path,
) -> list[Path]:
    by_kind_machine: dict[tuple[str, str], list[ReportRef]] = defaultdict(list)
    for path in sorted(source_dir.glob("*.md")):
        if not FILENAME_RE.match(path.name):
            # A typo in the version (e.g. "2.1.a-…") would otherwise drop a whole
            # run from the summary without a trace.
            print(
                f"warning: ignoring {path.name} — not <version>-<kind>-<machine>.md",
                file=sys.stderr,
            )
            continue
        ref = parse_report_filename(path)
        by_kind_machine[(ref.kind, ref.machine)].append(ref)

    if not by_kind_machine:
        raise FileNotFoundError(
            f"no <version>-<kind>-<machine>.md source files in {source_dir}"
        )

    written: list[Path] = []
    output_dir.mkdir(parents=True, exist_ok=True)

    for (kind, machine), refs in sorted(by_kind_machine.items()):
        versions = ", ".join(r.version for r in sorted(refs, key=lambda r: r.version_key))
        for view in View:
            name = f"{kind}-{machine}-{view.value}"
            dst = output_dir / f"{name}.md"
            dst.write_text(
                build_combined_for_machine(kind, machine, refs, output_dir, view),
                encoding="utf-8",
            )
            written.append(dst)
            fig_dir = output_dir / "fig" / name
            n_figs = len(list(fig_dir.glob("*.svg"))) if fig_dir.exists() else 0
            print(f"summary: {name}.md ← [{versions}] ({n_figs} figs)")

    return written


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    here = Path(__file__).resolve().parent
    # Default paths are relative to the script's location.
    # When the script lives at graphity-benchmarks/times-to-ops.py:
    #   source  → graphity-benchmarks/reports/versions/
    #   output  → graphity-benchmarks/reports/summary/
    default_source = here / "reports" / "versions"
    default_output = here / "reports" / "summary"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source-dir",
        type=Path,
        default=default_source,
        help="directory containing <version>-<kind>-<machine>.md source files "
             f"(default: {default_source})",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=default_output,
        help="directory to write the <kind>-<machine>-peers.md and -history.md summary files "
             f"plus their fig/ subdirectories (default: {default_output})",
    )
    parser.add_argument(
        "--single",
        nargs=2,
        metavar=("IN", "OUT"),
        type=Path,
        help="convert a single times Markdown file to an op/s file (chapter 1 only) "
             "and exit; ignores all directory options",
    )
    args = parser.parse_args(argv)

    try:
        if args.single:
            src, dst = args.single
            if not src.is_file():
                raise FileNotFoundError(f"times file not found: {src}")
            convert_times_file(src, dst)
            print(f"ops: {src} → {dst}")
            return 0

        if not args.source_dir.is_dir():
            raise FileNotFoundError(f"source directory not found: {args.source_dir}")
        generate_combined(args.source_dir, args.output_dir)

    except (FileNotFoundError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
