# Plans

## XML consistency tests
- Add tests to check XML questionary files for consistency
- Detect duplicate questions and answers within a questionary

## Template-based dynamic questionnaires
- Generate questions from templates at runtime, e.g. "I am in %city%", "I am in the %city%"
- Templates produce session-specific question sets with substituted values
- Use the template itself as the internal ID for persistent scoring

## Composite questionnaires
- Ability to compose multiple questionnaires into one
- User can select several questionnaires and run them as a single combined session

## Questionary health metric on main screen
- Show per-questionary stats next to each title: mistakes / known / new percentages
- Uses the same three-pile classification as session selection (score > 0 / score ≤ 0 / no attempts)
- Coverage naturally drops as stats decay and get pruned — signals need to practice
- Display as compact bar or three numbers

## Stats improvements
- Separate scoring window from retention window (e.g. score based on last week, but keep data for a month)
- Configurable scoring/retention windows as user settings
- Optional per-question stat display during exercise (configurable)

## Session question selection algorithm
- Session size: default 100 (or all questions if fewer). Global config with per-questionary override (later).
- Split all questions into 3 piles based on stats:
  - **Mistakes**: positive score (recently wrong)
  - **New**: no stats at all
  - **Known**: zero or negative score, sorted by last asked date ascending (oldest first)
- Take up to 70% of session size from mistakes (sorted by score descending — worst first)
- Split remaining slots 50/50 between new (random) and known (oldest first)
- If one pile can't fill its half, the other takes the leftover slots
- Shuffle the final list
- Track last asked date per question in stats (alongside attempt log)
- Percentages (70% mistakes cap, 50/50 new/known split) hardcoded for now, configurable later

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

## Rich text in questions/answers
- XML markup tags for parts of question/answer text
- Tagged parts rendered differently (underline, color, etc.)
- Tap on tagged part shows a tooltip
