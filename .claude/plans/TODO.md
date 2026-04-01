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

## Stats improvements
- Separate scoring window from retention window (e.g. score based on last week, but keep data for a month)
- Configurable scoring/retention windows as user settings
- Optional per-question stat display during exercise (configurable)

## Exercise session features
- Elapsed timer for the questionary
- Configurable session limits: by time or by number of questions
- Go back to previous answer to correct it (sometimes tap right instead of wrong and vice versa) — questionable, needs UX thought

## Statistics page
- Available after each question via button press and at the end of the lesson
- Share button to export stats as an image for messengers etc.

## Rich text in questions/answers
- XML markup tags for parts of question/answer text
- Tagged parts rendered differently (underline, color, etc.)
- Tap on tagged part shows a tooltip
