# BrownPaper AI Handoff

## Project intent
BrownPaper is an offline-first Android "read later" app built in Kotlin with Jetpack Compose, Room, DataStore, Hilt, Coil, WorkManager, OkHttp, and Readability4J. The app saves article text and a hero image locally, supports Android share intents, provides a native reader with adjustable typography and reader themes, and can optionally sync with wallabag.

## Current architecture
- `app/src/main/java/com/blackpirateapps/brownpaper/data`
  - `local`: Room entities, cross refs, FTS table, DAO, database.
  - `parser`: `Readability4J`-based article extraction for high-quality content retrieval.
  - `preferences`: DataStore-backed reader settings repository.
  - `repository`: repository implementation that owns list queries, ingestion, and article mutations.
  - `wallabag`: OkHttp API client, encrypted session storage, host/content mapping, OAuth token handling, two-way sync orchestration, and WorkManager scheduling.
- `app/src/main/java/com/blackpirateapps/brownpaper/domain`
  - Domain models, repository interfaces, the wallabag repository contract, and the add-article use case.
- `app/src/main/java/com/blackpirateapps/brownpaper/ui`
  - `navigation`: app shell, drawer routing, and NavHost.
  - `shell`: global ingestion state and drawer data.
  - `home`: Searchable article lists. `ArticleListViewModel` is reactive to `SavedStateHandle` changes, allowing seamless source switching (Inbox, Likes, Archived), and exposes quick article actions.
  - `reader`: Native reading screen with `SelectionContainer` for text copying. Supports custom fonts (Merriweather, Lora, Fira Sans, Inter) and article metadata.
  - `settings`: Backup/restore plus wallabag login, host switching, client credentials fallback, manual sync, and disconnect actions.
  - `components`: shared cards, dialogs, and drawer content.

## Important behavior
- **Navigation:** Sidebar selection (Likes, Archived, Tags, Folders) correctly updates the feed because `ArticleListViewModel` observes navigation arguments reactively.
- **Search:** The home screen search bar is now a collapsible icon in the `TopAppBar` with animated expansion and focus management.
- **Article cards:** Homepage cards only render media when an article has a hero image. Quick actions are available on each card for favorite, archive/unarchive, mark as read, and delete.
- **Reader metadata:** The reader header shows estimated reading time, word count, and the source domain. The domain opens the full original URL.
- **Extraction:** Replaced manual `Jsoup` heuristics with `Readability4J`. It extracts structured text (paragraphs/headers) which is then rendered as discrete blocks in the reader.
- **Typography:** Custom fonts are bundled in `app/src/main/res/font/` and integrated into the `ReaderPreferences` model and `ReaderScreen` settings.
- **wallabag login:** Settings starts with host, username, and password fields. The API client ID and secret live in an advanced section because wallabag OAuth requires them on normal v2 API installations. The password is never persisted.
- **wallabag sync:** Room is at schema version 4. Articles carry wallabag entry metadata and local/remote timestamps, `wallabag_sync_operations` records pending local updates, and `wallabag_delete_operations` records remote deletes after local deletion. Local saves, likes, archive changes, tag edits, and deletes queue sync work and schedule WorkManager.
- **Sync policy:** Sync pushes queued deletes before pulling remote entries, pulls paged wallabag entries with `detail=full`, links by URL before creating remote duplicates, pushes local unsynced articles, and patches starred/archived/tags.
- **Secret storage:** Wallabag session JSON is encrypted with an Android Keystore AES-GCM key before being stored in a dedicated DataStore file.

## Verification status
- Unit tests exist for URL normalization, FTS query formatting, wallabag host normalization, wallabag API request construction/token parsing, and wallabag content mapping.
- Verification was not run after the latest reader/homepage/delete-sync changes; the user asked to skip verification here.
- Custom fonts verified as present in the resource directory.

## Known follow-up items
- Add a Gradle wrapper so future agents can reliably run `./gradlew test`.
- Do a device/emulator pass for wallabag login and first sync against a real wallabag instance.
- Add instrumentation and Compose UI tests.
- Add launcher icons and release signing configuration.
