# BrownPaper AI Handoff

## Project intent
BrownPaper is an offline-first Android "read later" app built in Kotlin with Jetpack Compose, Room, DataStore, Hilt, Coil, and Jsoup. The app saves article text and a hero image locally, supports Android share intents, and provides a native reader with adjustable typography and reader themes.

## Current architecture
- `app/src/main/java/com/blackpirateapps/brownpaper/data`
  - `local`: Room entities, cross refs, FTS table, DAO, database.
  - `parser`: Jsoup-based article extraction with lightweight readability heuristics.
  - `preferences`: DataStore-backed reader settings repository.
  - `repository`: repository implementation that owns list queries, ingestion, and article mutations.
- `app/src/main/java/com/blackpirateapps/brownpaper/domain`
  - Domain models, repository interfaces, and the add-article use case.
- `app/src/main/java/com/blackpirateapps/brownpaper/ui`
  - `navigation`: app shell, drawer routing, and NavHost.
  - `shell`: global ingestion state and drawer data.
  - `home`: searchable article lists for Home, Likes, Archived, tags, and folders.
  - `reader`: native reading screen, typography controls, tag/folder management, share/open/delete actions.
  - `components`: shared cards, dialogs, and drawer content.

## Important behavior
- `MainActivity` accepts `ACTION_SEND` intents with `text/plain` payloads and forwards shared URLs into the shell view model.
- URL ingestion is normalized, fetched with Jsoup, parsed into a title/body/hero image, then stored in Room.
- Search uses Room FTS4 over article title and extracted text. Drawer filters use the same repository query path to avoid divergent list behavior.
- Reader preferences are persisted in Preferences DataStore and applied only to the reader surface.

## Verification status
- Code-level validation was completed end to end.
- Focused unit tests were added for URL normalization and FTS query formatting.
- Full Gradle/Android compilation was not run locally because the environment did not include `java` or `gradle`.

## Known follow-up items
- Add a dedicated background worker if ingestion must survive process death; the current implementation follows the prompt’s coroutine-based ingestion requirement.
- Improve article extraction with a stronger readability pipeline if content quality across diverse publishers becomes an issue.
- Add instrumentation/UI tests once an Android toolchain is available locally or in CI.
- Consider adding app icons, screenshot tests, and release signing configuration before store distribution.

## CI/build notes
- GitHub Actions uses `actions/setup-java`, `android-actions/setup-android`, and `gradle/actions/setup-gradle`.
- The workflow builds `:app:assembleRelease` and uploads the resulting APK artifact from `app/build/outputs/apk/release`.

