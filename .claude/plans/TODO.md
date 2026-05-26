# Plans

## XML consistency tests
- Add tests to check XML questionary files for consistency
- Detect duplicate questions and answers within a questionary
- Armenian text validation: catch ւ used without preceding ո (accounting for possible ՞ in between)

## Template-based dynamic questionnaires
- Generate questions from templates at runtime, e.g. "I am in %city%", "I am in the %city%"
- Templates produce session-specific question sets with substituted values
- Use the template itself as the internal ID for persistent scoring

## Stats improvements
- Separate scoring window from retention window (e.g. score based on last week, but keep data for a month)
- Configurable scoring/retention windows as user settings
- Optional per-question stat display during exercise (configurable)

## Session question selection improvements
- Session size configurable: global config with per-questionary override
- Percentages (70% mistakes cap, 50/50 new/known split) configurable

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

## Rich text in questions/answers
- XML markup tags for parts of question/answer text
- Tagged parts rendered differently (underline, color, etc.)
- Tap on tagged part shows a tooltip
