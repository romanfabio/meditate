# CLAUDE.md

Guidance for working on this repository.

## What this is

Meditate: a small, offline, Android-only meditation timer (Kotlin, Jetpack Compose, Room). MIT licensed, published on GitHub, installed by sideloading (not on the Play Store). Package/namespace: `io.github.romanfabio.meditate`. Version 1.0.0. `minSdk 31` (Android 12), `targetSdk 36`, `compileSdk 37`.

## Commands

Gradle must run on **JDK 17+**. If `java` on PATH is older, set `JAVA_HOME` (Android Studio's `jbr` works). `local.properties` (git-ignored) holds `sdk.dir`.

```bash
./gradlew testDebugUnitTest            # unit tests (engine, formatting)
./gradlew assembleDebug                # debug APK
./gradlew lintRelease assembleRelease  # lint + signed/minified release APK
```

Before declaring work done, run `lintRelease assembleRelease testDebugUnitTest`; all must pass. Lint is strict about API levels (minSdk 31 vs newer APIs) and Compose rules.

Windows note: use `gradlew.bat`. `adb` is at `%LOCALAPPDATA%\Android\Sdk\platform-tools`.

## Architecture

```
session/SessionEngine   pure Kotlin state machine (Idle, Countdown, Running, Paused, Finished); time comes from an injected clock
session/SessionManager  process-wide owner; runs a 200 ms tick loop, plays bells, toggles DND, saves sessions; exposes StateFlow
session/SessionService  foreground service (specialUse), wake lock, notification with Pause/Resume/Stop
audio/BellPlayer        MediaPlayer on USAGE_ALARM, audio focus, fade-out
dnd/DndController       Priority-only DND with persisted ownership, restore-on-start safety net
data/                   Room (sessions table), SessionRepository, Prefs (SharedPreferences)
ui/                     Compose screens: timer, calendar, about; no navigation library (state-based tabs)
MeditateApp             hand-rolled service locator (no DI library)
```

Rules that must keep holding:
- `SessionEngine` stays free of Android imports and is driven only by clock differences, never by counting ticks. Any behaviour change needs a unit test in `SessionEngineTest`.
- Recorded time = actual meditated time (no pauses, no countdown). Sessions under 10 s are not saved (`SessionRepository.MIN_RECORD_SECONDS`). Day bucket = start date, stored as ISO string in `localDate`.
- DND: never turn it off if the app did not turn it on, and only restore if the filter is still the one the app set.
- Manual Stop is silent; only natural completion plays the end bell.
- All user-visible strings live in `res/values/strings.xml` (English only for now).

## Dependency and licensing policy (important)

- The repository is MIT. Only permissive dependencies are acceptable: Apache-2.0, MIT, BSD-2/3-Clause, ISC.
- `app/build.gradle.kts` configures AboutLibraries with `strictMode = FAIL` and an allow-list, so a bad licence fails the build. Do not weaken it.
- No Google Play Services, Firebase, analytics, ads or any network library. The app deliberately declares no `INTERNET` permission; do not add one.
- Prefer the platform and AndroidX over new libraries. If a dependency is added, check its licence, add it to `gradle/libs.versions.toml`, rebuild, and confirm the About page list/README licence section is still accurate.
- Assets are original: sounds come from `tools/audio/generate_sounds.py`, icons are hand-written vectors. Do not add third-party images, fonts or audio.
- Test-only dependencies (JUnit, coroutines-test) are not shipped. `org.json` JSON-license artifacts must not be added, even for tests.

## Secrets and files that must never be committed

`keystore.properties`, `*.jks`/`*.keystore`, `local.properties`, APKs, build output, IDE folders. They are in `.gitignore`. The release keystore lives outside the repo; `keystore.properties` points to it. Do not print passwords.

## Git

- Repo-local identity must be `Fabio Roman <61688500+romanfabio@users.noreply.github.com>` for author and committer. Check with `git config user.name` / `user.email` before committing; never use another email.
- Branch `main`. Small focused commits with imperative messages.

## Gotchas learned the hard way

- AGP 9.x needs Gradle 9.9 (wrapper is pinned, with a checksum) and uses built-in Kotlin: do not apply `kotlin-android`. KSP is used for Room.
- Current AndroidX requires `compileSdk 37`; the SDK platform `android-37.0` must be installed (`sdkmanager "platforms/android-37.0"`).
- `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` exists only on API 34+; `SessionService` calls `startForeground` without a type below that.
- Use `LocalConfiguration.current.locales[0]` instead of `Locale.getDefault()` inside composables (lint `NonObservableLocale`).
- libsndfile's Vorbis encoder can overflow the stack on Windows when given one huge buffer; the generator writes in 4096-frame blocks.
- Foreground-service notification actions go through `SessionService.onStartCommand`; the notification is only re-posted when its content key changes (the chronometer ticks by itself).

## Testing on a device

Unit tests cannot cover DND, audio, foreground service or screen-off behaviour. After changes in `session/`, `audio/` or `dnd/`, check on a real phone: countdown and start bell, end bell with the screen off, DND on at start and off at end/stop, Pause/Stop from the notification, calendar totals for several sessions in one day. Starting a session on a connected phone turns DND on and rings the bell, so do it deliberately.

## Release checklist

1. Bump `versionCode`/`versionName` in `app/build.gradle.kts`.
2. `./gradlew lintRelease assembleRelease testDebugUnitTest`.
3. Verify the APK: `apksigner verify`, no `INTERNET` permission, licences on the About page.
4. Attach `app/build/outputs/apk/release/app-release.apk` to a GitHub release (do not commit APKs).
