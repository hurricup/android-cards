#!/usr/bin/env bash
# Wrapper for generate.py -- (re)builds per-level Oxford questionary XMLs from
# the translation cache into app/src/main/assets/xml/. Safe to re-run anytime.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 not found -- install it first (sudo apt install python3)" >&2
  exit 1
fi

exec python3 "$HERE/generate.py" "$@"
