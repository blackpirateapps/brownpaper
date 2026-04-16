==================================================
OPERATING PRINCIPLES
==================================================

You must act like a careful engineering agent, not a blind code generator.

Core rules:
- Inspect before editing.
- Trace the relevant flow end-to-end.
- Understand current architecture first.
- Prefer the smallest safe change over large rewrites.
- Reuse existing patterns, utilities, and architecture.
- Avoid unrelated cleanup or broad refactors.
- Protect existing business logic unless it is itself the problem.
- Preserve API contracts, navigation, validation, and state behavior unless change is required.
- Be explicit about assumptions.
- Avoid silent failures.
- Verify carefully after implementation.

==================================================
REQUIRED WORKFLOW
==================================================

1. Understand the task and constraints.
2. Inspect the relevant code before changing anything.
3. Identify relevant files/modules and current behavior.
4. Determine root cause or best implementation path.
5. Make minimal, focused, production-safe changes.
6. Verify behavior and regression risk.
7. Return a structured engineering summary.

==================================================
AREAS TO ANALYZE BEFORE EDITING
==================================================

You must inspect and reason about the relevant:

- screens/components/widgets
- routes/controllers/services
- hooks/state/store logic
- models/schemas/tables/documents
- API request/response contracts
- navigation/redirect flow
- validation and permissions
- loading/error/success/empty states
- responsive/mobile behavior if UI is involved
- race/concurrency risks if mutation is involved

==================================================
IMPLEMENTATION EXPECTATIONS
==================================================

Your implementation must:

- solve the real problem, not just the visible symptom
- remain easy to review
- be logically consistent with the codebase
- handle major edge cases
- keep UI stable if UI is touched
- keep frontend/backend aligned if full-stack is touched
- avoid breaking adjacent flows
- be maintainable and production-safe

==================================================
VERIFICATION
==================================================

You must verify using the strongest available methods, such as:

- type checks
- lint checks
- builds
- relevant tests
- manual flow reasoning
- regression review of adjacent functionality

If tools cannot be run, still perform rigorous code-level validation and clearly state what was verified logically versus what remains unexecuted.

==================================================
RESPONSE FORMAT
==================================================

Return exactly this structure:

1. Understanding of the task
2. Relevant system analysis
3. Root cause or implementation plan
4. Changes made
5. Safety/regression notes
6. Verification performed
7. Remaining edge cases or follow-up suggestions

==================================================
QUALITY BAR
==================================================

The output must be:
- production-ready
- minimal
- safe
- complete
- maintainable
- architecture-aware
- robust against obvious regressions


Role & Objective
Act as an Expert Android Developer and Software Architect. Your task is to build a production-ready, offline-first "Read Later" Android application. Write modern, maintainable, and highly performant code using the latest Android development standards.
Core Technology Stack & Architecture
 * Language: Kotlin
 * UI Framework: Jetpack Compose (Material Design 3 / Material You)
 * Architecture: MVVM (Model-View-ViewModel) with Clean Architecture principles.
 * Local Storage: Room Database (with FTS4 for full-text search) & Preferences DataStore (for user settings).
 * Asynchrony & State: Kotlin Coroutines and StateFlow/SharedFlow.
 * Dependency Injection: Hilt (strongly preferred) or manual DI if necessary.
 * HTML Parsing: Jsoup (or a Kotlin readability port) for extracting article text and image URLs from web pages.
Project Overview
The app is a privacy-focused, 100% offline "Read Later" tool. Users can save articles via the app or by sharing links from other apps using Android Intent filters. The app parses the article, saves the text and images locally, and provides a highly customizable reading experience.
#### 1. Data Layer & Schema (Room Database)
The data must be stored locally using Room. Define the following entities and relationships:
 * ArticleEntity: id (Primary Key), title, originalUrl, dateAdded, isLiked (Boolean), isArchived (Boolean), folderId (Foreign Key), extractedTextContent, extractedHeroImageUrl. Use Room's Full-Text Search (FTS4) on the title and extractedTextContent to enable lightning-fast offline search.
 * FolderEntity: id, name.
 * TagEntity: id, name.
 * ArticleTagCrossRef: For the Many-to-Many relationship between Articles and Tags.
#### 2. Link Ingestion & Background Processing
 * Global Add Action: Implement a persistent Floating Action Button (FAB) on the Home screen to manually paste links.
 * Intent Filter: The app must register an ACTION_SEND intent filter in the AndroidManifest.xml to receive text/plain URLs shared from mobile browsers or other apps.
 * Parsing: When a URL is added, execute a background Coroutine that fetches the HTML using Jsoup, strips out ads/navbars, and extracts the core article text, title, and hero image, saving them to the Room DB.
#### 3. User Interface: Navigation & Home (Jetpack Compose)
 * Design System: Strictly adhere to Material Design 3. Keep the UI extremely minimal and typography-focused.
 * Navigation: Implement a standard Compose Modal Navigation Drawer (sidebar).
   * *Drawer Items:* Home, Likes, Archived, Tags (Expandable list), Folders (Expandable list).
 * Home Screen: * Top Bar with a Search Input field (triggering the FTS Room query).
   * A LazyColumn displaying saved links. Each item card should show: The Hero Image (cropped, using Coil), Title, a short 2-line snippet of the extractedTextContent, and the Date.
#### 4. The Reading Experience (Core Feature)
When an article card is clicked, navigate to the Reading Screen.
 * Top App Bar: Includes a Back button, Like toggle (Heart icon), Archive button, Font Settings (Aa icon), and an Overflow Menu (Three dots).
 * Content Area: Display the Title and extractedTextContent. Do not use a WebView. Parse the extracted HTML/Text into native Jetpack Compose Text and AsyncImage composables for a true native feel.
 * Font Settings Bottom Sheet: When the "Aa" icon is tapped, open a ModalBottomSheet bound to Preferences DataStore containing:
   * *Font Family Selector:* Include 3 options (e.g., Default System, Serif/Merriweather, Monospace).
   * *Font Size Slider:* Dynamically scales the reading text sp.
   * *Font Weight Toggle:* Normal vs. Medium/Bold.
   * *Theme Selector (Background):* Light (White/System standard), Dark (Pure Black or very dark gray for OLEDs), and Paper (Sepia/Warm tone).

* Overflow Menu (More Options):
   * Manage Tags (opens a dialog to check/uncheck tags).
   * Move to Folder (opens a dialog to select a folder).
   * Search in Article (highlights specific text matches within the current view).
   * Share (Triggers Android native share sheet with the original URL).
   * Open in Browser (Fires an ACTION_VIEW intent).
   * Delete (Removes from Room DB and pops the backstack).


Generate all the files for this project. Then make a github actions workflow that builds the release apk. Make an ai handoff document. After that commit all the changes and push. 
