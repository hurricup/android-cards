# Plans

## Leitner tiered scheduler (major refactor — next big direction)
Replace the decay-weighted score + three-pile selection with a Leitner box system
(spaced repetition with increasing intervals). Research-backed; current algorithm is a rough proxy.

**Model**
- Per-card state = `(tier, lastAnswered, lastResult)`, stored explicitly per (leaf questionary id, question text). `lastResult` isn't needed for scheduling — kept for the pie / display / analytics. Keep the attempts log too for now (history/export).
- Tier 0 = unknown (never answered). Tiers ≥ 1 grow by a **configurable global multiplier**: `interval(tier) = round(baseDay × multiplier^(tier−1))`, base = 1 day, up to a max (~256 days / a max tier; exact numbers TBD from research).
  - Multiplier is a global setting, useful range ~1.2–5 (φ≈1.618 ≈ Fibonacci 1,2,3,5,8,13,…; 2 = doubling; 2.5 ≈ SM-2). **1 disables spacing** (all tiers = 1 day) — floor the setting above 1.
  - Because we store the **tier**, not the absolute interval, changing the multiplier recomputes every due date on the fly from `(tier, lastAnswered)` — no migration/rescale needed.
- On answer: correct → tier + 1 (capped at max); wrong → `max(1, tier − 1)`. A card leaves tier 0 on first answer and never returns; a wrong answer in tier 1 stays in tier 1.
- Due when `now − lastAnswered ≥ interval(tier)`.

**Session composition**
- Fill to session size: due cards **highest tier first** down to tier 1, then new (tier 0) to fill the rest.
- **No new cap** — pull extra new to fill the session; it self-throttles because due cards take priority (heavy review days leave no slack for new). Balance point tracks accuracy; early lumpiness smooths out.
- **Top-tier take limit** (and maybe per-tier limits): cap how many cards come from the top tier so sessions don't become all top-tier and starve lower tiers/new. Exact caps TBD with the interval numbers.

**Settings / UI**
- Drops the old decay knobs: halflife, max-age, mistakes-cap %, known-pool-factor.
- New knobs: global interval multiplier (~1.2–5), max tier, top-tier take limit (session size already exists).
- Pie chart buckets become tier 0 / tier 1 / tier 2+.

**Compatibility**
- Keying stays per leaf id, so composites (aggregate) and modes (direct/reverse/mixed, operate on leaf/variant ids) are unaffected.
- Migration: derive initial tier from the existing attempts log (nice-to-have, not critical); otherwise start everyone at tier 0.

**Phases**
1. Scheduler core: tiers, due calc, promote/demote, session fill with top-tier limit.
2. Settings + pie (tier 0/1/2+); remove obsolete decay settings.
3. Migration from the attempts log.

## Merge duplicate questions across composite leaves
- Within a leaf, same-text questions are merged (answers joined). Across composite leaves they are NOT — the same question (e.g. կам) in two leaves shows as two cards in a composite session.
- Constraint: stats must stay per leaf. So we can't merge-and-store at composite level, nor attribute a merged card to a single leaf.
- Plan: merge for display + fan-out on record.
  - Composite groups leaf questions by exact (text, answer); shows one card.
  - On answer, record the attempt to every owning leaf (same right/wrong).
  - Selection/classification of a merged card aggregates across owners: mistake if any owner > 0, new if all owners new, else known; lastAsked = most recent across owners.
- Model change: a merged Question carries multiple owner ids (questionaryIds: List<String>); StatsCoordinator.record/score/lastAsked/hasAttempts operate over the set. Localized to the coordinator.
- Open: key on exact (text, answer) (collapse true duplicates only, keep same-spelling-different-meaning separate) — leaning this way — vs. text-only (union answers).

## XML consistency tests
- Add tests to check XML questionary files for consistency
- Detect duplicate questions and answers within a questionary
- Armenian text validation: catch ւ used without preceding ո (accounting for possible ՞ in between)

## Template-based dynamic questionnaires
- Generate questions from templates at runtime, e.g. "I am in %city%", "I am in the %city%"
- Templates produce session-specific question sets with substituted values
- Use the template itself as the internal ID for persistent scoring

## Stats improvements
- Optional per-question stat display during exercise (configurable)
- (done) Configurable max-age setting; halflife derived from it (maxAge/4)
- (done) Configurable "recent window" for the trained/dimmed indicator

## Questionary mode (session composition)
- Not a separate synthetic questionary — composites already exist (e.g. +/−). Instead add a session "mode" option.
- Modes: normal (current three-pile algorithm), prioritize wrong, prioritize new, prioritize known.
- Selected from the same per-questionary popup menu as direct/reverse order and session size.
- Stats still counted against the respective source questionaries.

## Adjustable session size
- Make base session size a setting: global default + per-questionary override (same UX as the hard-questions % override).
- Currently only per-run Sprint/Default/Marathon options exist; Sprint/Marathon should derive from the configured base.
- (done) mistakes cap % configurable, global + per-questionary override.

## Mixed direct/reverse mode
- Direction is currently binary per questionary (direct or reverse), persisted.
- Add a "mixed" option that draws from both the direct and reverse variants in one session — trains production and comprehension together (needed: reverse-only practice makes speaking easier than understanding).
- Natural fit: mixed = composite of the questionary's own direct + reverse variants; stats stay per-direction.
- Selected from the same per-questionary menu as the reverse toggle (direct / reverse / mixed).

## Questionary options popup
- ~~"..." button on questionary opens a popup menu~~ — done
- ~~Session size options: Sprint / Default / Marathon~~ — done
- ~~Reverse mode toggle per questionary, persisted~~ — done
- For composite questionnaires: option to run sub-questionnaries separately
  - Opens main activity filtered to show only the sub-questionnaries (like a subfolder)

## Exercise session features
- Elapsed timer for the questionary
- Configurable session limits: by time
- Go back to previous answer to correct it (sometimes tap right instead of wrong and vice versa) — questionable, needs UX thought

## Statistics page
- Available after each question via button press and at the end of the lesson
- Share button to export stats as an image for messengers etc.

## Stats backup & sync
- ~~**Phase 1**: Manual import/export of stats to a file~~ — done (zip export/import via settings gear)
- **Phase 2**: Google Drive App Data folder for silent cloud backup
  - Sign in with Google once
  - Stats silently written to hidden app-specific Drive folder
  - On reinstall or new device: sign in, stats restored automatically
  - Enables cross-device sync

## Questionary management
- Remove bundled XML assets — clean install has only generated (math) questionnaires
- Questionnaires stored in app's internal storage as XML files
- Import/update: pick XML file from device, copies to internal storage. If id matches existing, replaces it.
- Remove: delete questionary from internal storage (with confirmation)
- Later: browse and download questionnaires directly from the GitHub data repo

## Session history
- Record each completed session: questionary id, start time, finish time, session size, correct/incorrect counts
- Persist as a log (JSON or similar)
- Usage TBD — could feed into progress charts, streaks, or export

## Multiple choice mode
- Alternative answer mode: instead of reveal, show answer options to pick from
- Find questions with lexically similar text (e.g. same root/prefix like verb conjugations)
- Use their answers as distractors alongside the correct one
- Similarity: Levenshtein distance, common prefix, or similar
- Distractors are plausible because they come from related questions

## Written answer mode
- User types answer into a text field instead of revealing it
- Answer checked automatically: correct/wrong with correct answer shown
- Good for kids — no self-assessment trust needed
- Results screen at the end of session is important (parent checks results)

## Answer mode system
- Three modes: **Reveal** (current), **Selection** (multiple choice), **Written** (type answer)
- Questionary can specify preferred mode in XML (e.g. `<mode>written</mode>`)
- User can override mode from the questionary context menu
- Default: reveal (current behavior)

## Questionary processors (variant generation)
- Global registry of processors
- Each processor is fed every questionary and returns a collection of derivative questions
- App merges the base questionary with all derivatives into the final question set
- Use case: an Armenian-verbs processor recognizes infinitives (սիրել) and produces:
  - Negation: չսիրել
  - Present tense conjugations: ես սիրում եմ, դու սիրում ես, ...
  - Negative present: ես չեմ սիրում, ...
  - Other tenses as needed
- Processors are not tied to specific questionnaires — they self-select based on content
- Each derivative question keeps a stable id (e.g. base + transformation key) for scoring continuity
- Easily extensible: register more processors for other languages or transformations
- Translation side (e.g. Russian) is harder than Armenian generation due to many inflected forms
  - **Initial approach**: questionary XML specifies all needed translation forms explicitly
  - **Later**: explore Russian morphology libraries / dictionaries (pymorphy3, OpenCorpora data, AOT.ru, lucene-analyzers-morfologik) to generate forms automatically

## Questionary groups
- Higher-level grouping of questionaries: Math, Armenian, etc.
- Only one group visible at a time; user switches between groups (UX TBD)
- Group declared in XML (e.g. `<group>Armenian</group>`)

## Levels
- Optional numeric level on questions, set by the questionary author
- Levels live within a group: user picks a group (e.g. Armenian) and a level (e.g. 4)
- App filters questions across all questionaries in the group to level ≤ N
- XML markup options to explore:
  - Per-question: `<question level="3">…</question>`
  - Block: `<level n="3"> <question>…</question> … </level>` grouping
- Levels apply to raw questions before the variant pipeline / processors

## Hide questionaries
- "Hide" item in the per-questionary dropdown menu — removes from the main list
- "Show hidden" toggle in the settings gear menu — when on, hidden ones reappear (dimmed) so they can be unhidden
- Hidden state persisted in SharedPreferences per questionary id

## Quicksearch
- Type a partial word, see matching questions and answers across all questionaries
- Sorted by relevancy (prefix match first, then substring, then fuzzy/Levenshtein)
- Useful for looking up a specific word/phrase without browsing
- UX TBD: dedicated search screen or inline search bar on main activity

## Rich text in questions/answers
- XML markup tags for parts of question/answer text
- Tagged parts rendered differently (underline, color, etc.)
- Tap on tagged part shows a tooltip
