# Android Cards

Flashcard learning app. Originally built for kids, now primarily used by the author to learn Armenian language. Also supports math practice and speech therapy exercises.

## Architecture

- **Kotlin + Jetpack Compose + Material 3**
- `compileSdk 35`, `minSdk 26`, `targetSdk 35`, Java 11
- Single module (`app`)

## Activities

- **MainActivity** — questionnaire selection screen. Lists all available questionnaires as buttons. Loads XML-based and generated questionnaires on start.
- **QuestionaryActivity** — card-by-card learning screen. Shows one question at a time. Answer is blurred until user taps it. User marks right/wrong. Progress bar at top shows green (correct), red (wrong), gray (remaining) with counts.

## Data Model

- **Question** (`model/Question.kt`) — `data class Question(text: String, answer: String? = null)`. Answer is optional.
- **Questionary** (`model/Questionary.kt`) — base class. Handles XML parsing from assets, caching by title in companion object map, Intent-based serialization (title as key). Questions are shuffled when passed to the learning screen.

## Questionnaire Sources

### XML Assets (`assets/xml/`)
Parsed via `XmlPullParser`. Each file follows structure: `<questionary><title>...</title><questions><question><text>...</text><answer>...</answer></question>...</questions></questionary>`.

Files: Armenian alphabet, numbers, words, grammar, classes; Russian vocabulary; speech therapy exercises.

### Generated (math operations in `model/impl/`)
- **Addition** ("Сложение") — 121 problems, 0-10 + 0-10
- **Subtraction** ("Вычитание") — 121 problems, ensures non-negative results
- **Multiplication** ("Умножение") — 121 problems, 0-10 × 0-10
- **Division** ("Деление") — 110 problems, clean division only (divisor 1-10)

All use lazy initialization. No answers provided in generated questions — student calculates mentally.

## UI Details

- Russian language UI
- Answer reveal: 16dp blur, tap to show
- Right/wrong buttons disabled until answer revealed
- Text beautification: `--` → `—`, `...` → `…`
- Dynamic color scheme on Android 12+

## Coding Directives

- Keep diffs clean and focused. Don't rename existing variables or refactor surrounding code unless directly required by the task.
- Don't add extra steps beyond what was asked.
- All user-facing strings in English.
- Prefer small atomic commits with one logical change each. Don't bundle unrelated changes.
