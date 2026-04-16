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
- Move ingestion to `WorkManager` if saves must survive app backgrounding or process death; the current implementation uses app-scoped coroutines, so an interrupted fetch can be lost.
- Improve article extraction with a stronger readability pipeline if text quality varies too much across publishers; the current `Jsoup` heuristics are intentionally lightweight.
- Add instrumentation and Compose UI tests once an Android toolchain is available locally or in CI; only small JVM utility tests were added in this pass.
- Add launcher icons, screenshot tests, and release signing configuration before store distribution; the project currently builds for CI, but release packaging still needs store-level polish.

## Build migration notes
- The project targets AGP `9.1.x`, which uses built-in Kotlin support.
- `org.jetbrains.kotlin.android` was removed from module and top-level build files per the AGP migration guide.
- `kotlin-kapt` was replaced with `com.android.legacy-kapt`, and `android.kotlinOptions {}` was migrated to `kotlin { compilerOptions {} }`.
- A Kotlin Gradle plugin classpath entry is declared in the top-level build so the Compose compiler plugin version stays aligned with the Kotlin compiler used by AGP built-in Kotlin.

## CI/build notes
- GitHub Actions uses `actions/setup-java`, `android-actions/setup-android`, and `gradle/actions/setup-gradle`.
- The workflow generates an ephemeral `PKCS12` release keystore at runtime with `keytool`, exports its credentials through `GITHUB_ENV`, and the app module consumes those environment variables to sign the `release` build.
- CI uses the same password for the keystore and key entry to avoid PKCS12 key-password mismatch failures during Android packaging.
- The workflow builds `:app:assembleRelease` and uploads the resulting APK artifact from `app/build/outputs/apk/release`.
