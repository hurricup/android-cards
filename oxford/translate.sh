#!/usr/bin/env bash
# Wrapper for translate.py -- fetches EN->RU translations into google_cache/.
# Safe to interrupt (Ctrl-C) and re-run: it resumes from the cache.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 not found -- install it first (sudo apt install python3)" >&2
  exit 1
fi

exec python3 "$HERE/translate.py" "$@"
