# AGENTS.md

Guidance for AI agents and contributors working on Lazy Durtles, an Android client for WaniKani.

## Project overview

- Android app, package `com.lazy_durtles.wk`, Kotlin and Java mixed.
- The app is migrating from the legacy View/Fragment/Activity UI to Jetpack Compose. Both coexist; the migration is in progress (see the git history: "Migrate to Jetpack Compose and Material Expressive 3 (WIP)").
- Compose code lives under `app/src/main/java/com/lazy_durtles/wk/ui/`. Legacy code lives under `activities/`, `fragments/`, `views/`, and `adapter/`.
- The legacy UI is the **behavioral reference**. When rewriting a legacy screen in Compose, its behavior, text, and validation rules are the spec.
- Data is stored with Room (`db/`). API access lives in `api/`. Session logic lives in `model/Session.java`.

## Building and running

- Build and install the debug variant with:
  ```
  JAVA_HOME=/usr/lib/jvm/temurin-21-jdk ./gradlew :app:installDebug --offline
  ```
  Gradle runs on JDK 21, while the app compiles to Java/Kotlin target 17.
- The line `e: Daemon compilation failed: null` is a Kotlin daemon fallback warning, not a build failure. Check for `BUILD SUCCESSFUL` and for lines starting with `e: ` that are real compile errors.
- The debug build uses application ID `com.lazy_durtles.wk.debug` (the `.debug` suffix).
- Before building from a fresh checkout, follow the "Preparing to build the code" section in `README.md` to supply `Identification.java` and `strings.xml`. Do not commit those files with real values.
- Tooling: compileSdk 34, minSdk 21, Compose BOM 2024.02.00, Material 3, Room 2.6.1, Lottie, Glide, coroutines.

## Obtaining IDE warnings and errors

Most editor warnings and errors that do not come from the Kotlin/Java compiler are Android Lint inspections (Android Studio runs the same checks in the editor). To obtain them from the command line:

```
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk ./gradlew :app:lintDebug --offline
```

- The report is written to `app/build/reports/lint-results-debug.txt` (also `.html` and `.xml`).
- Gradle lint currently skips some Compose lint checks with an `ObsoleteLintCustomCheck` warning, because the lint jars shipped with the Compose libraries use a newer Kotlin Analysis API than the lint embedded in the Android Gradle plugin. Those checks still run in the Android Studio editor, so the IDE can show warnings (for example `UnusedBoxWithConstraintsScope`) that the Gradle lint run does not list.
- Known lint false positive: `UnusedBoxWithConstraintsScope` ("BoxWithConstraints scope is not used") does not resolve implicit receiver access such as `maxWidth` inside `BoxWithConstraints` (issuetracker 379269842). When the constraints are genuinely used, make the receiver explicit (`this.maxWidth`, or `this@BoxWithConstraints.maxWidth` when nested inside another receiver such as `with(density)`) instead of replacing `BoxWithConstraints` with `Box`.
- `strings.xml` is the build-supplied identification file (see "Preparing to build the code" in `README.md`) and is swapped on fresh checkouts. Do not add UI strings to it; put them in a separate resource file such as `values/ui_strings.xml`.

## Code guidelines

### General
- Match the style of the surrounding code. Newer Compose files use Kotlin, named arguments, trailing `modifier: Modifier = Modifier` parameters, and 4-space indentation.
- Keep changes scoped. Don't refactor unrelated legacy code while migrating a screen.
- Don't add comments that restate the code. Comment only non-obvious behavior, such as why a workaround exists.
- Don't create summary or documentation markdown files unless the user asks for them.
- Don't commit or push unless the user asks.

### Compose
- Put reusable Compose screens in `ui/`, and shared pieces in `ui/common/` (currently empty) or next to the screen.
- Hoist state. Screens take callbacks such as `onFinishSession: () -> Unit` rather than navigating directly.
- Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`. Hardcode colors only when they come from the legacy design (for example the question-type hint colors below).
- Use `rememberCoroutineScope` and `withContext(Dispatchers.IO)` for database work. Never touch Room on the main thread.
- Use `Animatable` and `LaunchedEffect` for animations, and keep them tied to a trigger value so they restart cleanly.

### Session logic
- Drive the review/lesson flow only through the `Session` API: `chooseQuestion()`, `submit(matchingKanji)`, `submitDontKnow()`, `advance()`, and `getCurrentQuestion()`.
- `Session.advance()` returns early unless the current item has been answered. Calling `Session` methods out of order, or bypassing `submit` with direct `question.checkAnswer` calls, leaves the session stuck. Don't bypass it.
- Respect the `GlobalSettings` flags that the legacy UI respects: `AdvancedOther.getDontKnowButtonBehavior()`, `AdvancedOther.getShakeOnMatchingKanji()`, `Review.getCloseEnoughAction()`, `Review.getEnableAlternativesToast()`, and `getAutoPlay(sessionType)`.

### Text input
- Reading questions require kana. Romaji typed on the keyboard is converted by `util/PseudoIme`.
- Convert text only when `TextFieldValue.composition == null`. Converting during IME composition breaks input (the "race" that `SessionScreen.kt` addresses; the fix is not yet verified on device). Keep the `TextFieldValue` cursor at the end after conversion.
- Keep input at most 50 characters, as the legacy UI does.

### Audio
- Playback goes through `util/AudioUtil.playAudio(subject, lastMatchedAnswer)`. It checks the mute flag itself.
- Mute state is stored with `WkApplication.getDatabase().propertiesDao().getIsMuted()` and `setIsMuted()`. Use these, not a separate preference.
- Autoplay only for reading questions, and only once per item (`FloatingUiState.audioPlayed`).

### Database and API
- Use DAOs (`db/dao/`) for database access. Don't add raw SQL in UI code.
- Keep API models in `api/model/`. Don't leak them into UI state; map to `db/model` types where the rest of the app does.

## Design principles

1. **Behavior parity first.** A redesigned screen must keep the legacy behavior, text, validation, and edge cases. Redesign only the presentation.
2. **Clarity during review.** The subject character is the focal point. Progress, the question type, and the next action are visible at all times.
3. **Don't hide primary actions.** The Next action sits in a pinned bottom bar that is visible while the content scrolls.
4. **Question type by color.** Meaning and reading use the legacy hint colors so users recognize them:
   - Meaning chip: background `#C0C0C0` with light text (`wkDarkMeaningHintColor`).
   - Reading chip: background `#000000` with light text (`wkDarkReadingHintColor`).
   These colors come from `res/values/colors.xml` and `styles.xml`.
5. **Feedback is brief and non-blocking.** Correct answers get a short full-screen checkmark with a few green specks (about 0.6 s), without the primary color. Incorrect answers show the reason in a card. Feedback overlays must not intercept taps.
6. **Subtle motion.** Animations are short (roughly 50 to 800 ms), and every animation must have an end state that matches the static UI.
7. **Accessibility.** Give icon buttons a `contentDescription`. Keep touch targets at least 48 dp. Don't rely on color alone for correct and incorrect states; pair the color with an icon or text.
8. **Respect the theme.** Support light and dark themes through `LazyDurtlesTheme`.

## Testing and verification

- There are few automated tests. Verify UI changes on the emulator (`sdk_gphone64_x86_64`, Android 16) with `installDebug`, and test the affected flow by hand.
- For session changes, check at least: answer submission (including kana conversion), "Don't know", Next advancing, mute toggling, and a reading question with audio.
- Use `adb logcat` filtered by the `Session` tag to diagnose session logic problems.
- Known tool limitation: `adb input text` mangles Unicode. Use the clipboard or the emulator Japanese IME to enter kana.

## Things to avoid

- Don't commit `local.properties`, `.cache/`, `excalidraw.log`, or `Identification.java` with real values.
- Don't remove the legacy UI while migrating a screen until the Compose version has been verified against it.
- Don't change `FuzzyMatching` answer rules ("Woman", "tree", "say" special cases) without checking the legacy behavior first.
