# Repository Guidelines

## Project Structure

TruyenDex is a single-module Android novel reader. Java sources live in `app/src/main/java/com/example/truyendex/`. Put screen code in the matching `features/` package, such as `features/reader/` for chapter playback or `features/library/` for reading history. Keep shared theme and window helpers in `core/`. The `data/` package groups models, repositories, local storage, and remote service interfaces.

Under `app/src/main/res/`, use `layout/` for screen and list-item XML, `drawable/` for icons and backgrounds, and `values/` for strings, colors, and styles. Fragment destinations are defined in `navigation/nav_graph.xml`.

## Build, Test, and Run

Install Android SDK Platform 37. Run the Gradle wrapper from the repository root. In PowerShell, use these commands individually.

| Command | Purpose |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug` | Build the debug APK. |
| `.\gradlew.bat :app:lintDebug` | Run Android Lint on the debug variant. |
| `.\gradlew.bat :app:testDebugUnitTest` | Run local JVM tests. |
| `.\gradlew.bat :app:connectedDebugAndroidTest` | Run instrumentation tests on a connected device or emulator. |

To launch locally, select the `app` configuration in Android Studio, choose an API 24 or newer device, and click **Run**.

## Coding Style and Naming

Use Java 11 language features and four-space indentation. Use PascalCase for classes and lowerCamelCase for methods and fields. Follow existing role suffixes such as `ReaderActivity`, `LibraryFragment`, and `ChapterAdapter`. Name XML resources in lowercase snake_case, following patterns such as `activity_reader.xml` and `item_chapter.xml`.

## Testing Guidelines

Place JUnit 4 tests for plain Java behavior in `app/src/test/java/`. Place tests requiring Android context or Espresso UI interactions in `app/src/androidTest/java/`. Mirror the production package and end test class names in `Test`. Give each test method a descriptive name, following the existing `addition_isCorrect` pattern. The project defines no minimum coverage percentage.

## Commits and Pull Requests

Recent feature commits use short Vietnamese summaries describing the affected screen or behavior. Keep each commit focused. In a pull request, describe the user-visible change, link any related issue, and list the tests or device checks performed. Include screenshots for visual changes and identify the theme shown when changing light, dark, or sepia styling.
