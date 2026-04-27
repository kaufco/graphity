#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SNAP_DIR="${SCRIPT_DIR}/snap"
mkdir -p "${SNAP_DIR}"

download_snap() {
  local name="$1"
  local url="$2"
  local gz="${SNAP_DIR}/${name}.txt.gz"
  local out="${SNAP_DIR}/${name}.txt"

  if [[ -f "${out}" ]]; then
    echo "skip ${name}: already exists at ${out}"
    return
  fi

  echo "download ${name} ..."
  curl -fL --retry 3 --retry-delay 2 -o "${gz}" "${url}"
  gzip -dc "${gz}" > "${out}"
  rm -f "${gz}"
  echo "ok ${name} -> ${out}"
}

download_snap "web-Google" "https://snap.stanford.edu/data/web-Google.txt.gz"
download_snap "wiki-Talk" "https://snap.stanford.edu/data/wiki-Talk.txt.gz"
download_snap "soc-LiveJournal1" "https://snap.stanford.edu/data/soc-LiveJournal1.txt.gz"
download_snap "cit-Patents" "https://snap.stanford.edu/data/cit-Patents.txt.gz"
download_snap "cit-HepPh" "https://snap.stanford.edu/data/cit-HepPh.txt.gz"

echo
echo "Done. Dataset files are in:"
echo "  ${SNAP_DIR}"
echo
echo "You can now run:"
echo "  cd \"$(cd "${SCRIPT_DIR}/.." && pwd)\""
echo "  ./run-benchmarks.sh perf dataset-quick"
