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

- **Question** (`model/Question.kt`) — `data class Question(text: String, answer: String? = null)`. Answer is optional.
- **Questionary** (`model/Questionary.kt`) — base class. Handles XML parsing from assets, caching by title in companion object map, Intent-based serialization (title as key).
- **CompositeQuestionary** (`model/impl/CompositeQuestionary.kt`) — combines questions from multiple questionnaires under one title.
- **QuestionaryStats** (`model/QuestionaryStats.kt`) — persistent per-question answer history with decay-weighted scoring. Drives session composition (three-pile algorithm: mistakes/new/known) and distribution stats for pie charts. JSON storage per questionary.

## Questionnaire Sources

### XML Assets (`assets/xml/`)
Parsed via `XmlPullParser`. Each file follows structure: `<questionary><title>...</title><questions><question><text>...</text><answer>...</answer></question>...</questions></questionary>`.

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
