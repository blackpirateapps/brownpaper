# BrownPaper AI Handoff

## Project intent
BrownPaper is an offline-first Android "read later" app built in Kotlin with Jetpack Compose, Room, DataStore, Hilt, Coil, and Readability4J. The app saves article text and a hero image locally, supports Android share intents, and provides a native reader with adjustable typography and reader themes.

## Current architecture
- `app/src/main/java/com/blackpirateapps/brownpaper/data`
  - `local`: Room entities, cross refs, FTS table, DAO, database.
  - `parser`: `Readability4J`-based article extraction for high-quality content retrieval.
  - `preferences`: DataStore-backed reader settings repository.
  - `repository`: repository implementation that owns list queries, ingestion, and article mutations.
- `app/src/main/java/com/blackpirateapps/brownpaper/domain`
  - Domain models, repository interfaces, and the add-article use case.
- `app/src/main/java/com/blackpirateapps/brownpaper/ui`
  - `navigation`: app shell, drawer routing, and NavHost.
  - `shell`: global ingestion state and drawer data.
  - `home`: Searchable article lists. `ArticleListViewModel` is reactive to `SavedStateHandle` changes, allowing seamless source switching (Inbox, Likes, Archived).
  - `reader`: Native reading screen with `SelectionContainer` for text copying. Supports custom fonts (Merriweather, Lora, Fira Sans, Inter).
  - `components`: shared cards, dialogs, and drawer content.

## Important behavior
- **Navigation:** Sidebar selection (Likes, Archived, Tags, Folders) correctly updates the feed because `ArticleListViewModel` observes navigation arguments reactively.
- **Search:** The home screen search bar is now a collapsible icon in the `TopAppBar` with animated expansion and focus management.
- **Extraction:** Replaced manual `Jsoup` heuristics with `Readability4J`. It extracts structured text (paragraphs/headers) which is then rendered as discrete blocks in the reader.
- **Typography:** Custom fonts are bundled in `app/src/main/res/font/` and integrated into the `ReaderPreferences` model and `ReaderScreen` settings.

## Verification status
- Code-level validation completed.
- Unit tests exist for URL normalization and FTS query formatting.
- Custom fonts verified as present in the resource directory.

## Known follow-up items
- Migrate ingestion to `WorkManager` for more robust background processing.
- Add instrumentation and Compose UI tests.
- Add launcher icons and release signing configuration.
