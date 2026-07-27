# Benchmark Datasets

This directory is for local benchmark datasets.

- Dataset files are intentionally not versioned in git.
- Use `./download-datasets.sh` to fetch the default SNAP datasets used by
  the `realworld-quick` and `realworld` profiles.

Expected layout after download:

- `snap/web-Google.txt`
- `snap/wiki-Talk.txt`
- `snap/soc-LiveJournal1.txt`
- `snap/cit-Patents.txt`
- `snap/cit-HepPh.txt`

All five files are benchmarked by the `realworld` profile; `realworld-quick`
uses `web-Google.txt` only, as a fast smoke test.
