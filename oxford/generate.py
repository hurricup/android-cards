#!/usr/bin/env python3
"""Generate per-level Oxford questionary XMLs from the translation cache.

Reads Oxford words from full-word.json and their translations from
google_cache/ (populated by translate.py), then writes one questionary file
per CEFR level into the app assets. Re-runnable: only cached words are
included, so re-running after more words are fetched just refreshes the files.

Card model (as discussed):
- A plain-verb sense becomes its own card, displayed "to <word>", so it is
  distinguishable from the same spelling used as a noun/adjective/etc.
- Every other part of speech merges into a single "<word>" card.
  A word therefore yields at most two cards.
- Translations are Google's POS-grouped alternatives, filtered to drop the
  long low-frequency junk tail (see pick_terms), then joined with '|' as
  answer variants -- the format the app already uses.

Placement: each card goes to the file for the LOWEST CEFR level among the
Oxford entries that feed it (verb entries for the verb card, the rest for the
merged card). Questions are sorted alphabetically by word (the "to " prefix
does not affect ordering); the merged card precedes the verb card.

Output: app/src/main/assets/xml/oxford_<level>.xml
  id    = oxford-<level>       (e.g. oxford-a1)
  title = Oxford (<LEVEL>)     (e.g. Oxford (A1))
"""

import glob
import json
import os
import urllib.parse
from collections import defaultdict
from xml.sax.saxutils import escape

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "full-word.json")
CACHE_DIR = os.path.join(HERE, "google_cache")
OUT_DIR = os.path.join(HERE, "..", "app", "src", "main", "assets", "xml")

LEVELS = ["A1", "A2", "B1", "B2", "C1"]  # blank/unknown levels are skipped
LEVEL_RANK = {lv: i for i, lv in enumerate(LEVELS)}

# Translation filtering. Google returns a POS group as a frequency-ranked list;
# the tail is noise ("шесть взяток" for book). Keep the top term always, plus
# any others above the score thresholds, capped per POS and per card.
# Scores are not normalized across words (a group's top ranges from ~0.0001 to
# 1.0), so RELATIVE_THRESHOLD -- a fraction of THIS group's top -- is what
# actually trims same-POS near-duplicates like книжка next to книга.
SCORE_THRESHOLD = 0.01
RELATIVE_THRESHOLD = 0.1
MAX_PER_POS = 1
MAX_PER_CARD = 6

VERB_POS = "verb"

# Words whose only Oxford part of speech is one of these are dropped: they
# carry no learnable meaning and Google returns junk (e.g. "a" -> "один").
FUNCTION_POS = {"indefinite article", "definite article", "determiner"}


def level_rank(level):
    return LEVEL_RANK.get((level or "").strip())


def load_oxford():
    """word -> {'verb': [levels...], 'other': [levels...]} as CEFR ranks.

    Words that are exclusively function words (see FUNCTION_POS) are omitted.
    """
    with open(SOURCE, encoding="utf-8") as f:
        entries = json.load(f)

    types = defaultdict(set)
    words = defaultdict(lambda: {"verb": [], "other": []})
    for e in entries:
        v = e["value"]
        rank = level_rank(v.get("level"))
        if rank is None:
            continue
        types[v["word"]].add(v.get("type"))
        bucket = "verb" if v.get("type") == VERB_POS else "other"
        words[v["word"]][bucket].append(rank)

    return {w: b for w, b in words.items() if types[w] - FUNCTION_POS}


def load_translations(word):
    """Parse the cached Google response into {pos: [(term, score), ...]}.

    Returns (groups, main). `groups` is POS-keyed and frequency-ordered;
    `main` is the single top-line translation used as a fallback.
    """
    path = os.path.join(CACHE_DIR, urllib.parse.quote(word, safe="") + ".json")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as f:
        data = json.load(f)

    main = None
    if data and data[0] and data[0][0]:
        main = data[0][0][0]

    groups = {}
    dictionary = data[1] if len(data) > 1 and data[1] else []
    for grp in dictionary:
        pos = grp[0]
        scored = grp[2] if len(grp) > 2 and grp[2] else []
        pairs = []
        for t in scored:
            term = t[0]
            score = t[3] if len(t) > 3 and t[3] is not None else 0.0
            pairs.append((term, score))
        if pairs:
            groups[pos] = pairs
    return groups, main


def pick_terms(pairs):
    """Top term always, plus others clearing both thresholds, capped per POS."""
    top = pairs[0][1]
    floor = max(SCORE_THRESHOLD, top * RELATIVE_THRESHOLD)
    out = [pairs[0][0]]
    for term, score in pairs[1:]:
        if len(out) >= MAX_PER_POS:
            break
        if score >= floor:
            out.append(term)
    return out


def card_answer(pos_names, groups, main, word):
    """Merge picked terms of the given POS groups into a '|'-joined answer."""
    scored = []
    for pos in pos_names:
        if pos in groups:
            for term in pick_terms(groups[pos]):
                # keep the group's own frequency for cross-POS ordering
                score = next(s for t, s in groups[pos] if t == term)
                scored.append((term, score))
    if not scored and main:
        scored = [(main, 0.0)]

    seen, terms = set(), []
    for term, _ in sorted(scored, key=lambda x: -x[1]):
        low = term.lower()
        if low == word.lower() or low in seen:  # drop echoes/dupes
            continue
        seen.add(low)
        terms.append(term)
        if len(terms) >= MAX_PER_CARD:
            break
    return "|".join(terms)


def build_cards():
    """Return {level_rank: [(word, display_text, answer), ...]}."""
    oxford = load_oxford()
    by_level = defaultdict(list)

    for word in oxford:
        parsed = load_translations(word)
        if parsed is None:
            continue  # not translated yet
        groups, main = parsed
        buckets = oxford[word]
        word_min = min(buckets["verb"] + buckets["other"])

        # The verb/non-verb split is driven by Google's translations, not by
        # Oxford's POS tags: whenever a verb sense exists it becomes its own
        # "to <word>" card, so verb meanings never mix into the noun card (e.g.
        # mouse -> мышь vs. to mouse -> выслеживать). Oxford only sets levels.
        nonverb_pos = [p for p in groups if p != VERB_POS]
        has_verb_sense = VERB_POS in groups

        # Merged non-verb card: when Google has non-verb senses, or Oxford
        # lists the word as non-verb and Google offered nothing verb-y.
        if nonverb_pos or (buckets["other"] and not has_verb_sense):
            answer = card_answer(nonverb_pos, groups, main, word)
            if answer:
                level = min(buckets["other"]) if buckets["other"] else word_min
                by_level[level].append((word, word, answer))

        # Verb card: when Google has a verb sense, or Oxford tags it a verb.
        if has_verb_sense or buckets["verb"]:
            answer = card_answer([VERB_POS], groups, main, word)
            if answer:
                level = min(buckets["verb"]) if buckets["verb"] else word_min
                by_level[level].append((word, f"to {word}", answer))

    return by_level


def render(level, cards):
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        "<questionary>",
        f"    <id>oxford-{level.lower()}</id>",
        f"    <title>Oxford ({level})</title>",
        "    <questions>",
    ]
    for _word, text, answer in cards:
        lines += [
            "        <question>",
            f"            <text>{escape(text)}</text>",
            f"            <answer>{escape(answer)}</answer>",
            "        </question>",
        ]
    lines += ["    </questions>", "</questionary>", ""]
    return "\n".join(lines)


def main():
    by_level = build_cards()
    os.makedirs(OUT_DIR, exist_ok=True)
    for level in LEVELS:
        cards = sorted(by_level.get(LEVEL_RANK[level], []),
                       key=lambda c: (c[0].lower(), c[1] != c[0]))
        path = os.path.join(OUT_DIR, f"oxford_{level.lower()}.xml")
        with open(path, "w", encoding="utf-8") as f:
            f.write(render(level, cards))
        print(f"oxford_{level.lower()}.xml: {len(cards)} questions")


if __name__ == "__main__":
    main()
