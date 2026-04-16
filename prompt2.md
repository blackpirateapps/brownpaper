# Role & Context
You are an expert Android developer specializing in Kotlin, Jetpack Compose, and modern Android architecture (MVI/MVVM). You are working on **BrownPaper**, an offline-first reading queue application. 

Your task is to implement a series of bug fixes, UX improvements, and feature enhancements. Read through the requirements carefully. Make targeted, idiomatic changes, and ensure that Jetpack Compose best practices (e.g., state hoisting, recomposition optimization) are followed.

# Objectives

## 1. Fix Sidebar Navigation State (Likes & Archive)
**Current Behavior:** When a user clicks "Likes" or "Archived" in the `BrownPaperDrawerContent` sidebar, the UI remains on the Home screen view instead of switching lists.
**Expected Behavior:** Clicking these options must update the current feed. 
**Implementation Details:**
- Investigate how `onSelectSource(ArticleListSource, Long?)` is handled in the host (likely `ShellViewModel` or the navigation graph).
- Ensure the selected `ArticleListSource` state is properly hoisted and passed down to `ArticleListViewModel` or triggers a navigation event to `BrownPaperRoutes.listRoute()`.
- The UI must react to this state change and display the corresponding filtered list of articles from the database.

## 2. Upgrade Article Text Extraction (Replace Jsoup Logic)
**Current Behavior:** The app uses `JsoupArticleParser.kt` with basic manual CSS selector fallbacks to extract article text, which often results in poor formatting and missed content.
**Expected Behavior:** Use a sophisticated, purpose-built library for article extraction (e.g., a Kotlin port of Mozilla's Readability like `net.dankito.readability4j:readability4j` or `Crux`).
**Implementation Details:**
- Add the necessary dependencies to `app/build.gradle.kts`.
- Refactor `JsoupArticleParser.kt` to use the new library to parse the raw HTML.
- The extracted text should preserve basic structural formatting (e.g., paragraphs, headers) so it can be rendered cleanly in the Reader view. Do not just return a flat string of text; maintain line breaks and paragraph spacing.

## 3. Refactor Search Bar UX
**Current Behavior:** The search bar in `ArticleListScreen.kt` is a static `OutlinedTextField` taking up permanent vertical space, and it does not lose focus when the user taps elsewhere on the screen.
**Expected Behavior:** The search bar should be a collapsible icon in the `TopAppBar`. When expanded, it should allow typing. Tapping anywhere outside the search bar should clear the keyboard focus.
**Implementation Details:**
- Move the search input out of the main column and into the `actions` block of the `TopAppBar` as a toggleable search icon (`Icons.Outlined.Search`).
- Use `AnimatedVisibility` to expand a `TextField` (or `SearchBar`) when the icon is clicked.
- To handle focus loss: use `LocalFocusManager.current.clearFocus()` combined with a `Modifier.pointerInput(Unit) { detectTapGestures { ... } }` on the parent `Scaffold` or `LazyColumn` to detect outside taps.

## 4. Integrate Custom Fonts for the Reader
**Current Behavior:** The app only provides the default system fonts (sans-serif, serif, monospace).
**Expected Behavior:** The user must be able to choose from a richer selection of high-quality reading fonts.
**Implementation Details:**
- Act as if we have downloaded at least 3-4 new external fonts (e.g., *Merriweather*, *Lora*, *Fira Sans*, *Inter*) and placed them in `app/src/main/res/font/`.
- Update the font configuration files and Compose `Typography` or `FontFamily` definitions to map to these `.ttf`/`.otf` resources.
- Update the `ReaderPreferences` data model and the Reader's settings UI to expose these new font choices to the user.

## 5. Enable Text Selection in Reader Screen
**Current Behavior:** Users cannot copy text while reading an article.
**Expected Behavior:** All article content on the Reader page must be selectable and copyable.
**Implementation Details:**
- Locate the composable responsible for rendering the article body in `ReaderScreen.kt`.
- Wrap the text components in a `SelectionContainer { ... }`.
- Ensure this does not break vertical scrolling.

# Constraints & Guidelines
- **Dependencies:** If adding new libraries (like Readability4J), provide the exact `implementation(...)` line for `build.gradle.kts`.
- **UI Consistency:** Rely on Material 3 components and existing theme tokens where applicable.
- **Provide Complete Code:** When providing updated files, show the exact changes required. Do not leave placeholder comments like `// ... rest of code`. If a file is large, you may provide just the updated class or composable function, but ensure the imports are included.

Please analyze these requirements and provide the step-by-step code changes required to implement them.