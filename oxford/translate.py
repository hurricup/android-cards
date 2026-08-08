#!/usr/bin/env python3
"""Fetch EN->RU translations for Oxford 5000 words from Google Translate.

Uses the unofficial translate_a/single endpoint (dt=bd returns the POS-grouped
dictionary block with multiple translations per part of speech). Undocumented
and against Google ToS -- for personal, one-time deck building only.

Design:
- Source: full-word.json (array of {value:{word,type,level,...}}).
  One request per UNIQUE word; the response already carries every POS.
- Order: by the word's lowest CEFR level, then alphabetically. This mirrors
  the deck merge rule (a word's level = the earliest level it appears at).
- Cache: one file per word under google_cache/, named with the URL-quoted
  word. A cached word is skipped, so the script is fully resumable -- just
  re-run it after an interruption or a ban.
- Politeness: random 1..5s sleep between real fetches (never on cache hits).
- Ban handling: HTTP 429 or a non-JSON anti-bot body triggers backoff that
  starts at 60s and doubles up to 30min, retrying the SAME word until it
  succeeds -- nothing is skipped.

Writes are atomic (temp + rename) so an interrupt can't corrupt a cache file.

Run from anywhere; paths resolve relative to this script.
"""

import json
import os
import random
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "full-word.json")
CACHE_DIR = os.path.join(HERE, "google_cache")

ENDPOINT = "https://translate.googleapis.com/translate_a/single"
USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
)

# CEFR order for the "lowest level first" sort. Unknown/blank sorts last.
LEVEL_ORDER = {"A1": 0, "A2": 1, "B1": 2, "B2": 3, "C1": 4, "C2": 5}
UNKNOWN_LEVEL = 99

MIN_SLEEP = 1.0
MAX_SLEEP = 5.0
BAN_SLEEP_START = 60.0
BAN_SLEEP_MAX = 30 * 60.0


def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def level_rank(level):
    return LEVEL_ORDER.get((level or "").strip(), UNKNOWN_LEVEL)


def load_words():
    """Return unique words ordered by lowest level, then alphabetically."""
    with open(SOURCE, encoding="utf-8") as f:
        entries = json.load(f)

    lowest = {}  # word -> best (lowest) level rank seen
    for e in entries:
        v = e["value"]
        w = v["word"]
        r = level_rank(v.get("level"))
        if w not in lowest or r < lowest[w]:
            lowest[w] = r

    return sorted(lowest, key=lambda w: (lowest[w], w))


def cache_path(word):
    return os.path.join(CACHE_DIR, urllib.parse.quote(word, safe="") + ".json")


def write_atomic(path, text):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        f.write(text)
    os.replace(tmp, path)


class Banned(Exception):
    """Raised when Google throttles us (429 or anti-bot HTML)."""


def fetch(word):
    """Fetch raw response body for one word. Raises Banned on throttling."""
    params = urllib.parse.urlencode(
        [
            ("client", "gtx"),
            ("sl", "en"),
            ("tl", "ru"),
            ("dt", "t"),
            ("dt", "bd"),
            ("q", word),
        ]
    )
    req = urllib.request.Request(
        f"{ENDPOINT}?{params}", headers={"User-Agent": USER_AGENT}
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            body = resp.read().decode("utf-8")
    except urllib.error.HTTPError as ex:
        if ex.code == 429:
            raise Banned("HTTP 429") from ex
        raise

    # A throttled response comes back as HTML, not JSON. Validate before caching.
    try:
        json.loads(body)
    except json.JSONDecodeError as ex:
        raise Banned("non-JSON response (anti-bot page)") from ex

    return body


def fetch_with_backoff(word):
    """Fetch, sleeping through bans (60s doubling up to 30min) until it works."""
    sleep = BAN_SLEEP_START
    while True:
        try:
            return fetch(word)
        except Banned as ex:
            log(f"  BANNED ({ex}); sleeping {int(sleep)}s before retry")
            time.sleep(sleep)
            sleep = min(sleep * 2, BAN_SLEEP_MAX)
        except urllib.error.URLError as ex:
            # Transient network hiccup: back off like a ban rather than crash.
            log(f"  network error ({ex}); sleeping {int(sleep)}s before retry")
            time.sleep(sleep)
            sleep = min(sleep * 2, BAN_SLEEP_MAX)


def main():
    os.makedirs(CACHE_DIR, exist_ok=True)
    words = load_words()
    total = len(words)
    log(f"loaded {total} unique words from {os.path.basename(SOURCE)}")

    fetched = 0
    for i, word in enumerate(words, 1):
        path = cache_path(word)
        if os.path.exists(path):
            log(f"{word!r} is done ({i} of {total}) [cached]")
            continue

        log(f"fetching {word!r} ...")
        body = fetch_with_backoff(word)
        write_atomic(path, body)
        fetched += 1
        log(f"{word!r} is done ({i} of {total})")

        nap = random.uniform(MIN_SLEEP, MAX_SLEEP)
        time.sleep(nap)

    log(f"done: {fetched} newly fetched, {total - fetched} already cached")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        log("interrupted; re-run to resume from cache")
        sys.exit(130)
