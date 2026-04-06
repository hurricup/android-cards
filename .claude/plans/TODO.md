# Plans

## XML consistency tests
- Add tests to check XML questionary files for consistency
- Detect duplicate questions and answers within a questionary

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
- "..." button or long press on questionary button opens a popup menu
- Session size options: default, x2, x0.5 questions
- For composite questionnaires: option to run sub-questionnaries separately
  - Opens main activity filtered to show only the sub-questionnaries (like a subfolder)
- Both features can share the same popup

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

## Rich text in questions/answers
- XML markup tags for parts of question/answer text
- Tagged parts rendered differently (underline, color, etc.)
- Tap on tagged part shows a tooltip
