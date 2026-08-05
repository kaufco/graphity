#!/usr/bin/env python3
"""Unit tests for the summary generator: ``python3 test-times-to-ops.py``.

The CI builds Java and runs no Python, so these tests are meant to be run by hand after a
change to ``times-to-ops.py``. They use the standard library only.

The fixtures are two miniature version reports whose column sets differ on purpose: one
section has peers, one has a reordered variant in both versions, one has neither, and one
exists in the older version alone. That covers every branch of the column plan. The memory
chapter repeats the split: one section with peers, one Graphity-only.
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import re
import sys
import tempfile
import unittest
from pathlib import Path


def _load_generator():
    """Import the generator despite the hyphens in its file name."""
    path = Path(__file__).resolve().with_name("times-to-ops.py")
    spec = importlib.util.spec_from_file_location("times_to_ops", path)
    module = importlib.util.module_from_spec(spec)
    # @dataclass resolves annotations through sys.modules, so register before executing.
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


gen = _load_generator()

HEADER = """# Graphity Benchmark Report

```
Profile:      synthetic
JDK:          OpenJDK 64-Bit Server VM 25 (test)
```
"""

OLD_REPORT = HEADER + """
## 1 · Performance

### 1.1.1 · Build @ n = 1 000

| Shape | Graphity | JGraphT |
|---|---:|---:|
| PATH | 1.000 ms | 4.000 ms (4.00×) |
| TREE | 2.000 ms | 6.000 ms (3.00×) |

### 1.2.1 · SCC @ n = 1 000

| Shape | Graphity | Graphity (reordered) |
|---|---:|---:|
| PATH | 2.000 ms | 1.500 ms (0.75×) |
| TREE | 4.000 ms | 3.000 ms (0.75×) |

### 1.3.1 · WCC @ n = 1 000

| Shape | Graphity |
|---|---:|
| PATH | 0.500 ms |
| TREE | 0.800 ms |

### 1.4.1 · Legacy @ n = 1 000

| Shape | Graphity | JGraphT |
|---|---:|---:|
| PATH | 3.000 ms | 9.000 ms (3.00×) |

## 2 · Memory Footprint

### 2.1 · Total Graph Size

| Shape | Nodes | Edges | Graphity | WebGraph |
|---|---:|---:|---:|---:|
| PATH | 1 000 | 999 | 8 000 B | 2 000 B (0.25×) |
| TREE | 1 000 | 999 | 8 000 B | 3 000 B (0.38×) |

### 2.2 · Condensation Size

| Shape | Nodes | Edges | Graphity |
|---|---:|---:|---:|
| PATH | 1 000 | 999 | 4 000 B |
| TREE | 1 000 | 999 | 5 000 B |
"""

NEW_REPORT = HEADER + """
## 1 · Performance

### 1.1.1 · Build @ n = 1 000

| Shape | Graphity | JGraphT | WebGraph |
|---|---:|---:|---:|
| PATH | 0.900 ms | 4.100 ms (4.56×) | 7.000 ms (7.78×) |
| TREE | 1.800 ms | 6.100 ms (3.39×) | 9.000 ms (5.00×) |

### 1.2.1 · SCC @ n = 1 000

| Shape | Graphity | Graphity (reordered) |
|---|---:|---:|
| PATH | 1.900 ms | 1.400 ms (0.74×) |
| TREE | 3.800 ms | 2.900 ms (0.76×) |

### 1.3.1 · WCC @ n = 1 000

| Shape | Graphity |
|---|---:|
| PATH | 0.450 ms |
| TREE | 0.750 ms |

## 2 · Memory Footprint

### 2.1 · Total Graph Size

| Shape | Nodes | Edges | Graphity | JGraphT | Guava | WebGraph |
|---|---:|---:|---:|---:|---:|---:|
| PATH | 1 000 | 999 | 8 000 B | 400 000 B (50.0×) | 270 000 B (33.8×) | 2 000 B (0.25×) |
| TREE | 1 000 | 999 | 8 000 B | 400 000 B (50.0×) | 270 000 B (33.8×) | 3 000 B (0.38×) |

### 2.2 · Condensation Size

| Shape | Nodes | Edges | Graphity |
|---|---:|---:|---:|
| PATH | 1 000 | 999 | 3 800 B |
| TREE | 1 000 | 999 | 4 800 B |
"""

IMAGE_RE = re.compile(r"^!\[[^\]]*\]\((?P<path>[^)]+)\)$")


def table_columns(text: str, heading: str) -> list[str]:
    """Column labels of the first table below a heading, without the row-key column."""
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line == heading)
    for line in lines[start + 1:]:
        if line.startswith("|"):
            cells = [c.strip().replace("<br>", " ") for c in line.strip("|").split("|")]
            return cells[1:]
        if line.startswith("## ") or (line.startswith("### ") and line != heading):
            break
    raise AssertionError(f"no table below {heading!r}")


def table_rows(text: str, heading: str) -> list[list[str]]:
    """Data rows of the first table below a heading."""
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line == heading)
    rows: list[list[str]] = []
    seen_table = False
    for line in lines[start + 1:]:
        if line.startswith("|"):
            seen_table = True
            cells = [c.strip() for c in line.strip("|").split("|")]
            if set("".join(cells)) <= set("-: "):
                continue
            rows.append(cells)
        elif seen_table:
            break
    return rows[1:]


def images_of(text: str, heading: str) -> list[str]:
    """Chart links between a heading and the next heading of the same or a higher level."""
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line == heading)
    found: list[str] = []
    for line in lines[start + 1:]:
        if line.startswith("## ") or (line.startswith("### ") and line != heading):
            break
        match = IMAGE_RE.match(line)
        if match:
            found.append(match.group("path"))
    return found


class SummaryViewTest(unittest.TestCase):
    """The two views over one pair of version reports."""

    @classmethod
    def setUpClass(cls):
        cls._tmp = tempfile.TemporaryDirectory()
        root = Path(cls._tmp.name)
        cls.source_dir = root / "versions"
        cls.output_dir = root / "summary"
        cls.source_dir.mkdir()
        (cls.source_dir / "1.0-synthetic-testbox.md").write_text(OLD_REPORT, encoding="utf-8")
        (cls.source_dir / "2.0.0-synthetic-testbox.md").write_text(NEW_REPORT, encoding="utf-8")
        with contextlib.redirect_stdout(io.StringIO()):
            cls.written = gen.generate_combined(cls.source_dir, cls.output_dir)
        cls.peers = (cls.output_dir / "synthetic-testbox-peers.md").read_text(encoding="utf-8")
        cls.history = (cls.output_dir / "synthetic-testbox-history.md").read_text(encoding="utf-8")

    @classmethod
    def tearDownClass(cls):
        cls._tmp.cleanup()

    def test_both_views_and_their_chart_directories_are_written(self):
        self.assertEqual(
            [path.name for path in self.written],
            ["synthetic-testbox-peers.md", "synthetic-testbox-history.md"],
        )
        for view in ("peers", "history"):
            fig_dir = self.output_dir / "fig" / f"synthetic-testbox-{view}"
            self.assertTrue(fig_dir.is_dir(), f"missing chart directory for {view}")
            self.assertTrue(list(fig_dir.glob("*.svg")), f"no charts written for {view}")

    def test_peer_view_shows_only_the_newest_version(self):
        columns = table_columns(self.peers, "### 1.1 Build")
        self.assertEqual(columns, ["Graphity 2.0.0", "JGraphT", "WebGraph"])

    def test_history_view_shows_every_version_and_no_peer(self):
        columns = table_columns(self.history, "### 1.1 Build")
        self.assertEqual(columns, ["Graphity 1.0", "Graphity 2.0.0"])

    def test_history_view_measures_against_the_newest_version(self):
        # The newest column carries no factor; the older one is expressed relative to it,
        # not the other way round.
        row = table_rows(self.history, "### 1.1 Build")[0]
        self.assertEqual(row[1], "1.000 ms (1.1×)")
        self.assertEqual(row[2], "0.900 ms")

    def test_history_view_shows_a_reordered_column_per_version(self):
        columns = table_columns(self.history, "### 1.2 SCC")
        self.assertEqual(
            columns,
            [
                "Graphity 1.0",
                "Graphity 2.0.0",
                "Graphity 1.0 (reordered)",
                "Graphity 2.0.0 (reordered)",
            ],
        )

    def test_history_view_omits_reordered_columns_nothing_measured(self):
        # The bytes tables print every column they are handed, dashes included, so only the
        # column plan can keep an unmeasured reordered variant out of the memory chapter.
        self.assertEqual(
            table_columns(self.history, "### 2.1 Total Graph Size"),
            ["Graphity 1.0", "Graphity 2.0.0"],
        )

    def test_peer_view_keeps_a_section_without_peer_data(self):
        columns = table_columns(self.peers, "### 1.2 SCC")
        self.assertEqual(columns, ["Graphity 2.0.0", "Graphity 2.0.0 (reordered)"])

    def test_peer_view_falls_back_to_the_version_that_measured_the_section(self):
        # Only 1.0 measured "Legacy", so its column carries that version and is the baseline
        # the peer factor refers to.
        self.assertEqual(table_columns(self.peers, "### 1.4 Legacy"), ["Graphity 1.0", "JGraphT"])
        row = table_rows(self.peers, "### 1.4 Legacy")[0]
        self.assertEqual(row[1], "3.000 ms")
        self.assertEqual(row[2], "9.000 ms (3.0×)")

    def test_chart_is_omitted_where_only_the_baseline_has_data(self):
        self.assertEqual(images_of(self.peers, "### 1.3 WCC"), [])
        self.assertTrue(images_of(self.history, "### 1.3 WCC"))

    def test_memory_chart_is_omitted_where_only_the_baseline_has_data(self):
        heading = "### 2.2 Condensation Size"
        self.assertEqual(table_columns(self.peers, heading), ["Graphity 2.0.0"])
        self.assertEqual(images_of(self.peers, heading), [])
        self.assertTrue(images_of(self.history, heading))

    def test_memory_chapter_pairs_its_charts_in_the_peer_view_only(self):
        self.assertIn("**Graphity & WebGraph**", self.peers)
        self.assertIn("**All libraries**", self.peers)
        self.assertNotIn("**Graphity & WebGraph**", self.history)
        self.assertNotIn("**All libraries**", self.history)

    def test_every_linked_chart_exists(self):
        for name, text in (("peers", self.peers), ("history", self.history)):
            links = [
                match.group("path")
                for match in map(IMAGE_RE.match, text.splitlines())
                if match
            ]
            self.assertTrue(links, f"no charts linked from the {name} view")
            for link in links:
                self.assertTrue(
                    (self.output_dir / link).is_file(), f"dangling chart link: {link}"
                )

    def test_the_two_views_link_to_each_other(self):
        self.assertIn("synthetic-testbox-history.md", self.peers)
        self.assertIn("synthetic-testbox-peers.md", self.history)


DROPPED_BENCHMARK_REPORTS = {
    # Three versions, the last of which stopped measuring "Legacy": there the baseline has to
    # fall back to 1.5, the newest version that still measured it.
    "1.0": """
## 1 · Performance

### 1.1.1 · Build @ n = 1 000

| Shape | Graphity |
|---|---:|
| PATH | 1.200 ms |

### 1.4.1 · Legacy @ n = 1 000

| Shape | Graphity |
|---|---:|
| PATH | 4.000 ms |
""",
    "1.5": """
## 1 · Performance

### 1.1.1 · Build @ n = 1 000

| Shape | Graphity |
|---|---:|
| PATH | 1.100 ms |

### 1.4.1 · Legacy @ n = 1 000

| Shape | Graphity |
|---|---:|
| PATH | 2.000 ms |
""",
    "2.0.0": """
## 1 · Performance

### 1.1.1 · Build @ n = 1 000

| Shape | Graphity |
|---|---:|
| PATH | 1.000 ms |
""",
}


class BaselineFallbackTest(unittest.TestCase):
    """A section the newest version no longer measures keeps a baseline among its own columns."""

    @classmethod
    def setUpClass(cls):
        cls._tmp = tempfile.TemporaryDirectory()
        root = Path(cls._tmp.name)
        cls.output_dir = root / "summary"
        source_dir = root / "versions"
        source_dir.mkdir()
        for version, body in DROPPED_BENCHMARK_REPORTS.items():
            (source_dir / f"{version}-synthetic-testbox.md").write_text(
                HEADER + body, encoding="utf-8"
            )
        with contextlib.redirect_stdout(io.StringIO()):
            gen.generate_combined(source_dir, cls.output_dir)
        cls.history = (cls.output_dir / "synthetic-testbox-history.md").read_text(encoding="utf-8")
        cls.peers = (cls.output_dir / "synthetic-testbox-peers.md").read_text(encoding="utf-8")

    @classmethod
    def tearDownClass(cls):
        cls._tmp.cleanup()

    def test_history_view_falls_back_to_the_newest_version_with_data(self):
        heading = "### 1.2 Legacy"
        self.assertEqual(table_columns(self.history, heading), ["Graphity 1.0", "Graphity 1.5"])
        row = table_rows(self.history, heading)[0]
        self.assertEqual(row[1], "4.000 ms (2.0×)")
        self.assertEqual(row[2], "2.000 ms")

    def test_peer_view_labels_the_section_with_the_version_that_measured_it(self):
        self.assertEqual(table_columns(self.peers, "### 1.2 Legacy"), ["Graphity 1.5"])


class SeriesColorTest(unittest.TestCase):
    """Chart colours: versions, reordered variants and peers must be distinguishable."""

    def test_each_version_and_each_reordered_variant_gets_its_own_colour(self):
        labels = [
            "Graphity 1.0",
            "Graphity 2.0.0",
            "Graphity 1.0 (reordered)",
            "Graphity 2.0.0 (reordered)",
            "JGraphT",
        ]
        colors = gen.assign_series_colors(labels)
        self.assertEqual(len(set(colors.values())), len(labels))

    def test_the_single_reordered_column_keeps_the_established_colour(self):
        colors = gen.assign_series_colors(["Graphity 2.0.0", "Graphity 2.0.0 (reordered)"])
        self.assertEqual(colors["Graphity 2.0.0 (reordered)"], gen.REORDERED_COLORS[0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
