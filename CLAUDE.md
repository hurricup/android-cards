# Android Cards

Flashcard learning app. Originally built for kids, now primarily used by the author to learn Armenian language. Also supports math practice and speech therapy exercises.

## Architecture

- **Kotlin + Jetpack Compose + Material 3**
- `compileSdk 35`, `minSdk 26`, `targetSdk 35`, Java 11
- Single module (`app`)

## Activities

- **MainActivity** — questionnaire selection screen. Lists questionnaires as buttons with pie chart showing distribution (mistakes/known/new). Loads XML-based and generated questionnaires on start. Error handling with Toast for broken XML files.
- **QuestionaryActivity** — card-by-card learning screen. Shows one question at a time. Answer is blurred until user taps it. User marks right/wrong. Progress bar at top shows green (correct), red (wrong), gray (remaining) with counts. Back button confirmation dialog.

## Data Model

- **Question** (`model/Question.kt`) — `data class Question(text, answer? , questionaryId)`. Carries the id of its owning questionary so its stats are tracked independently of which questionary shows it.
- **Questionary** (`model/Questionary.kt`) — base class. Handles XML parsing from assets, caching by id in companion object map, Intent-based serialization (id as key). Each source yields a direct and a reverse variant (`id__reverse`).
- **ReferenceCompositeQuestionary** (`model/impl/ReferenceCompositeQuestionary.kt`) — aggregates questions from parts referenced by id, resolved lazily from the cache. Parts keep their own stats; missing refs are skipped and reference cycles are broken. Used for the math composites and for XML-declared composites.
- **Scheduling (Leitner):** `model/TierScheduler.kt` + `TierStore.kt` + `Tiers.kt`. Each question has a tier + last-answered time stored in `tiers/<id>.json` (per questionary id). Correct → next tier, wrong → one tier back (floored at 1); tier 0 = unknown/new. Session = due cards (highest tier first, top tier capped) then new to fill. Interval per tier = `multiplier^(tier-1)` days, multiplier configurable in the settings gear. Pie chart shows per-tier distribution.
- **Legacy stats** (`model/QuestionaryStats.kt`, `StatsCoordinator.kt`) — the old decay-weighted attempts log in `stats/<id>.json`. Kept only so the gear's "Import stat data (tiers)" can derive tiers from prior history; not used for live scheduling.

## Questionnaire Sources

### XML Assets (`assets/xml/`)
Parsed via `XmlPullParser`. Each file follows structure: `<questionary><title>...</title><questions><question><text>...</text><answer>...</answer></question>...</questions></questionary>`. Optional `<id>` overrides the stats key (defaults to title). Pipe (`|`) in text/answer expands to variant cross-product.

A `<questionary>` may instead declare a composite via `<questionaries><id>other-id</id>...</questionaries>` (mutually exclusive with `<questions>`) — it aggregates the referenced questionaries by id.

Files: Armenian alphabet, numbers, words, grammar, classes; Russian vocabulary; speech therapy exercises.

### Generated (math operations in `model/impl/`)
- **Multiplication** ("Умножение") — 2-10 core set with answers, plus seeded random examples with 0 and 1
- **Division** ("Деление") — reverse of multiplication with answers, includes division by zero (N/A)
- **Addition** ("Сложение") — 121 problems, 0-10 + 0-10
- **Subtraction** ("Вычитание") — 121 problems, ensures non-negative results

Exposed as two composites: "+/−" (addition + subtraction) and "×/÷" (multiplication + division). All use lazy initialization.

## UI Details

- English language UI
- Answer reveal: 16dp blur, tap to show
- Right/wrong buttons disabled until answer revealed
- Text beautification: `--` → `—`, `...` → `…`
- Dynamic color scheme on Android 12+

## Coding Directives

- Keep diffs clean and focused. Don't rename existing variables or refactor surrounding code unless directly required by the task.
- Don't add extra steps beyond what was asked.
- All user-facing strings in English.
- Prefer small atomic commits with one logical change each. Don't bundle unrelated changes.
