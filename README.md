# BrownPaper

BrownPaper is an offline-first read-later app for Android with optional wallabag sync. It saves article text and hero images locally, provides a focused native reader, and lets you organize, annotate, and sync your reading queue.

## Features

- Save links from Android's share sheet.
- Extract readable article content with Readability4J.
- Read saved articles offline with locally cached text and hero images.
- Customize the reader with bundled fonts, font weight, reader themes, and content width presets.
- Highlight passages, add notes, search annotations, and jump back to saved highlights.
- Search saved articles with local full-text search.
- Organize articles with favorites, read state, archive state, tags, and folders.
- Sync articles, tags, favorite/archive state, deletes, and annotations with a wallabag account.
- Store wallabag session data encrypted with Android Keystore.
- Backup and restore local app data.
- Use adaptive phone and tablet layouts built with Jetpack Compose.

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- Room, FTS, DataStore, WorkManager, and Hilt
- OkHttp, kotlinx.serialization, Jsoup, and Readability4J
- Coil for image loading
- JUnit unit tests

## Requirements

- JDK 17
- Android SDK with platform 36 installed
- Gradle 9.3.1 or compatible

This repository does not currently include a Gradle wrapper. Until one is added, use a locally installed `gradle` command. If a wrapper is added later, replace `gradle` with `./gradlew` in the commands below.

## Build

Clone the repository and build a debug APK:

```sh
git clone <repo-url>
cd brownpaper
gradle :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device or emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tests

Run the unit test suite:

```sh
gradle :app:testDebugUnitTest
```

Current unit coverage includes URL normalization, FTS query formatting, wallabag host normalization, wallabag API request construction/token parsing, and wallabag content mapping.

## Release Builds

Build a release APK:

```sh
gradle :app:assembleRelease
```

For a signed release build, provide the signing environment variables used by `app/build.gradle.kts`:

```sh
export CI_RELEASE_KEYSTORE_PATH=/path/to/release.jks
export CI_RELEASE_STORE_PASSWORD=...
export CI_RELEASE_KEY_ALIAS=...
export CI_RELEASE_KEY_PASSWORD=...
gradle :app:assembleRelease
```

The GitHub Actions release workflow expects these repository secrets:

- `CI_RELEASE_KEYSTORE_BASE64`
- `CI_RELEASE_STORE_PASSWORD`
- `CI_RELEASE_KEY_ALIAS`
- `CI_RELEASE_KEY_PASSWORD`

## Store Metadata

Fastlane metadata for IzzyOnDroid and F-Droid lives in:

```text
fastlane/metadata/android/en-US/
```

Add phone screenshots here:

```text
fastlane/metadata/android/en-US/images/phoneScreenshots/
```

Use sequential image names such as `1.png`, `2.png`, and `3.png`.

## License

BrownPaper is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
