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
  - `shell`: global ingestion state, drawer data, and wallabag sync action state.
  - `home`: Searchable article lists. `ArticleListViewModel` is reactive to `SavedStateHandle` changes, allowing seamless source switching (Inbox, Likes, Read, Archived, Tags, Folders), and exposes quick article actions.
  - `reader`: Native reading screen with custom annotation-aware text rendering. Supports custom fonts (Merriweather, Lora, Fira Sans, Inter), explicit reader themes, dropdown font weights, article metadata, inline highlights, notes, and an annotation list.
  - `settings`: Backup/restore plus wallabag login, host switching, client credentials fallback, manual sync, and disconnect actions.
  - `components`: shared cards, dialogs, and drawer content.

## Important behavior
- **Navigation:** Sidebar selection (Likes, Read, Archived, Videos, Tags, Folders) correctly updates the feed because `ArticleListViewModel` observes navigation arguments reactively. A wallabag sync action appears in the drawer when an account is connected.
- **Search:** The home screen search bar is now a collapsible icon in the `TopAppBar` with animated expansion and focus management.
- **Article cards:** Homepage cards only render media when an article has a hero image. Quick actions are available on each card for favorite, archive/unarchive, mark as read, and delete. Read/archived cards are visually muted, and tag feeds include read articles instead of filtering them out.
- **Reader metadata:** The reader header shows reader-theme-aware icon pills for estimated reading time, word count, and the source domain. The domain opens the full original URL.
- **Annotations:** Articles support offline-first annotations with highlight color, quote, note text, edit/delete, copy quote, search within annotations, tap-to-edit highlights, and jump-to-highlight from the annotation sheet. Reader highlights use the reader palette rather than the app theme.
- **Extraction:** Replaced manual `Jsoup` heuristics with `Readability4J`. It extracts structured text (paragraphs/headers) which is then rendered as discrete blocks in the reader.
- **Typography:** Custom fonts are bundled in `app/src/main/res/font/` and integrated into the `ReaderPreferences` model and `ReaderScreen` settings. Merriweather, Lora, and Inter were replaced with real Google Fonts TTFs after previous files were HTML placeholders. Reader font weight is now Light/Regular/Bold instead of a boolean toggle.
- **wallabag login:** Settings starts with host, username, and password fields. The API client ID and secret live in an advanced section because wallabag OAuth requires them on normal v2 API installations. The password is never persisted.
- **wallabag sync:** Room is at schema version 5. Articles carry wallabag entry metadata and local/remote timestamps, `wallabag_sync_operations` records pending local updates, `wallabag_delete_operations` records remote entry deletes after local deletion, and `wallabag_annotation_sync_operations` records pending annotation creates/updates/deletes. Local saves, likes, archive changes, tag edits, deletes, and annotation changes queue sync work and schedule WorkManager.
- **Sync policy:** Sync pushes queued deletes before pulling remote entries, pulls paged wallabag entries with `detail=full`, links by URL before creating remote duplicates, pushes local unsynced articles, patches starred/archived/tags, and pulls/pushes annotations for wallabag-linked entries. Annotation sync uses `GET/POST/PUT/DELETE /api/annotations`, preserves wallabag range JSON, and keeps BrownPaper annotation colors local because wallabag does not expose color in the documented annotation payload.
- **Secret storage:** Wallabag session JSON is encrypted with an Android Keystore AES-GCM key before being stored in a dedicated DataStore file.

## Verification status
- Unit tests exist for URL normalization, FTS query formatting, wallabag host normalization, wallabag API request construction/token parsing, and wallabag content mapping.
- Unit tests were extended for wallabag annotation JSON request construction and wrapped annotation list parsing.
- Verification was not run after the latest annotation implementation because the user explicitly requested skipping verification.
- Custom fonts are present as real TTF files in the resource directory.

## Known follow-up items
- Add a Gradle wrapper so future agents can reliably run `./gradlew test`.
- Do a device/emulator pass for wallabag login and first sync against a real wallabag instance.
- Add instrumentation and Compose UI tests.
- Add launcher icons and release signing configuration.
