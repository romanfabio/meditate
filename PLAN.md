# Meditate — Implementation Plan

A minimal, offline, Android-only meditation timer. Kotlin + Jetpack Compose. MIT licensed.
Personal sideload only (no Play Store).

## 1. Decisions (from the Q&A)

| Topic | Decision |
|---|---|
| Package / namespace | `io.github.romanfabio.meditate` (corrected from `o.github...`) |
| Platform | Android only, `minSdk 31` (Android 12+), `targetSdk`/`compileSdk 36` |
| Language / UI | Kotlin, Jetpack Compose + Material 3 (single Activity, single module `app`) |
| DND level | **Priority only**, with a one-time "Do Not Disturb access" grant |
| Screen during session | Allowed to turn off; session runs in a foreground service |
| Timer input | Three scroll wheels (HH / mm / ss) |
| What is recorded | **Actual meditated time** (excludes pauses and the 5 s countdown); stopped sessions are saved too, sessions under 10 s are discarded |
| UI language | English only (all strings in `strings.xml` so translation is possible later) |
| Build | Local minified **release APK** signed with a locally generated keystore (kept out of git) |
| Git identity | `Fabio Roman <61688500+romanfabio@users.noreply.github.com>` as repo-local author **and** committer |

### Assumptions I made without asking (tell me if wrong)

1. "Last timer used in previous version" is read as "last duration the user *started* a session with". It is stored when Start is pressed; first launch defaults to 20:00.
2. The countdown is silent (visual only, 5 → 1). The "begin" bell rings when the countdown reaches 0. Sounds: *start bell* (single soft strike) and *end bell* (three strikes with a long decay).
3. A session that spans midnight is attributed to the day it started.
4. Calendar day cell shows the total time; tapping a day lists that day's individual sessions. No edit/delete of sessions (out of scope; easy to add later).
5. Copyright line in LICENSE: `Copyright (c) 2026 Fabio Roman`.
6. The app requests **no INTERNET permission**, has no analytics and no network code.
7. A session that is killed by the OS (process death, reboot) is lost, not recovered.
8. A small "Do Not Disturb during session" switch on the timer page (default on), so the feature can be skipped without granting access.
9. Theme: calm palette (deep indigo / warm sand), follows system light/dark. System font, so no font licences.

## 2. Features and behaviour

### Timer page (start destination)
- Wheels for HH (0–23), mm (0–59), ss (0–59). Start disabled when `00:00:00`.
- "Do Not Disturb during session" switch. If on but access is not granted, Start shows a short explanation dialog with a button to the system settings screen (`ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`); the user may also continue without DND.
- Start: asks for `POST_NOTIFICATIONS` on first use (Android 13+). Session proceeds even if denied.
- States shown on the same page:
  1. **Idle** – wheels + Start.
  2. **Countdown** – big `5…1`, Cancel button. Cancelling records nothing.
  3. **Running** – remaining time `HH:mm:ss`, progress ring, Pause and Stop.
  4. **Paused** – time frozen, Resume and Stop.
  5. **Finished** – brief summary ("Session complete — 20:00"), button back to Idle.
- Bottom navigation: **Timer · Calendar · About**.

### Calendar page
- Month grid (custom Compose, no library), previous/next month, today highlighted.
- Days with sessions are tinted (intensity by minutes) and show total time (`25m`, `1h 05m`).
- Tap a day → list of that day's sessions (start time, duration, "stopped early" marker) and the day total. Month total in the header.

### About page
- App name, version, short description.
- **App licence**: full MIT text (bundled from `LICENSE`).
- **Third-party licences**: list of every shipped dependency with name, version, licence; tap to expand the licence text.

### Audio
- Calm, synthesized singing-bowl / temple-bell sounds (see §5).
- Played with `MediaPlayer` using `USAGE_ALARM` so they are audible under DND (volume follows the alarm stream; volume keys control it while the app is open). Short transient audio focus with ducking so music is lowered, not stopped.

### Do Not Disturb
- On Start (beginning of countdown): save the current interruption filter; if it is `ALL`, set `INTERRUPTION_FILTER_PRIORITY`. If DND is already on, do not touch it.
- On finish or stop: restore the saved filter, **only** if the current filter is still the one we set (never clobber a change the user made meanwhile).
- The "we own DND" flag and previous filter are persisted, and reconciled on app start/service start, so a crash cannot leave DND stuck on.

## 3. Architecture

```
app/src/main/java/io/github/romanfabio/meditate/
  MainActivity.kt                 single Activity, NavigationBar, Compose host
  session/
    SessionState.kt               sealed state: Idle, Countdown, Running, Paused, Finished
    SessionEngine.kt              pure Kotlin state machine, injectable clock -> unit-testable
    SessionManager.kt             process singleton: exposes StateFlow, owns engine/audio/dnd/repo
    SessionService.kt             foreground service (type specialUse), notification, wake lock
  audio/BellPlayer.kt             MediaPlayer wrapper, audio focus
  dnd/DndController.kt            NotificationManager interruption filter + persistence
  data/
    SessionEntity.kt, SessionDao.kt, AppDatabase.kt   Room
    SessionRepository.kt          per-day aggregation
    Prefs.kt                      SharedPreferences: last duration, DND switch, DND ownership
  ui/
    theme/                        colors, typography, shapes
    timer/ TimerScreen.kt, WheelPicker.kt, SessionViews.kt
    calendar/ CalendarScreen.kt, MonthGrid.kt, DaySessionsSheet.kt
    about/ AboutScreen.kt, LicensesParser.kt
tools/audio/generate_sounds.py    reproducible sound generator (committed)
```

Key design points
- **Timing** uses `SystemClock.elapsedRealtime()` deltas, never tick counting, so it stays correct across doze/pauses. Running time = accumulated segments between resume/pause.
- **Service**: started with `startForegroundService` from the visible Activity (allowed on Android 12+). Foreground service type `specialUse` (justification property `meditation_timer`) — fine for sideloaded apps. `PARTIAL_WAKE_LOCK` held while running, with a timeout of remaining time plus margin, so the end bell is on time with the screen off.
- **Notification** (low-importance channel, ongoing): countdown chronometer, **Pause/Resume** and **Stop** actions, tap opens the app.
- **UI ↔ service**: same process; the Activity observes `SessionManager.state` (StateFlow), the service and notification actions send commands to the manager. Activity recreation (rotation, process stays alive) is therefore free.
- **Persistence**: Room table `sessions(id, startedAtEpochMs, localDate TEXT indexed, durationSeconds, plannedSeconds, completed)`. `localDate` is stored at save time so past days do not shift if the timezone changes. Last duration lives in SharedPreferences.
- **Manifest permissions**: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `ACCESS_NOTIFICATION_POLICY`. No others.

## 4. Dependencies and licensing

Policy: ship only Apache-2.0 / MIT / BSD / ISC-class licences. No Google Play Services, Firebase, or proprietary SDKs. Every runtime dependency is checked against this list.

Planned runtime dependencies (all Apache-2.0): Kotlin stdlib, kotlinx-coroutines-android, AndroidX core-ktx, activity-compose, lifecycle (runtime/viewmodel-compose), Compose BOM (ui, foundation, material3, material-icons-core), Room (runtime, ktx). Room compiler via KSP (build time only).
Test-only (not shipped): JUnit 4, kotlinx-coroutines-test.

Licence screen mechanism
- Use the **AboutLibraries Gradle plugin** (Apache-2.0) at build time only: it reads POM licence metadata of the resolved runtime classpath and emits a JSON resource. The app parses it with `org.json` (framework) and renders it with its own Compose UI — so **no extra runtime library**.
- Configure an allow-list of licences; the build fails if a dependency outside it appears.
- Fallback if the plugin is incompatible with the newest AGP: a hand-maintained JSON asset plus a Gradle check task that diffs it against the resolved classpath.
- Include the Apache-2.0 full text once and apply any `NOTICE` obligations found in the dependency set.
- Own assets: sounds and icon are original work, covered by the project's MIT licence (stated in README and About).

Versions: use the latest stable AGP, Gradle, Kotlin, Compose BOM at implementation time via a `gradle/libs.versions.toml` catalog; confirm they exist before pinning.

## 5. Generated assets

**Sounds** — `tools/audio/generate_sounds.py` (Python, numpy; `soundfile` for OGG Vorbis, falls back to 16-bit WAV if Vorbis is unavailable):
- Additive synthesis of a singing bowl / temple bell: inharmonic partials (≈ 1, 2.71, 5.2, 8.6 × fundamental ~ 220–330 Hz), each pair slightly detuned for the slow "beating/shimmer", soft attack (~15 ms), exponential decays of several seconds, gentle low-pass, light synthetic reverb (decaying-noise impulse response), normalized to about −9 dBFS to avoid loudness.
- Outputs `bell_start.ogg` (single strike, ~8 s tail) and `bell_end.ogg` (three strikes spaced ~4 s, longer tail) into `app/src/main/res/raw/`.
- I will check waveform sanity (peak, clipping, duration, spectrum) programmatically since I cannot listen to it; you should audition them and I can re-tune parameters.

**Icon** — hand-written adaptive icon (vector drawables): concentric rings / bowl-and-sunrise motif on indigo, plus a monochrome layer for themed icons.

## 6. Milestones (one or more commits each)

| # | Milestone | Done when |
|---|---|---|
| M0 | **Repo bootstrap**: `git init` (branch `main`), repo-local `user.name`/`user.email`, `.gitignore`, MIT `LICENSE`, README stub, this plan | `git log` shows author **and** committer as the required identity |
| M1 | **Gradle skeleton**: wrapper, version catalog, package `io.github.romanfabio.meditate`, theme, 3-tab navigation, empty screens | `assembleDebug` succeeds |
| M2 | **Assets**: sound generator + generated sounds, adaptive icon | Files exist, sanity checks pass |
| M3 | **Data layer**: Room, repository, prefs, per-day aggregation | Unit/instrumented tests pass |
| M4 | **Session engine**: state machine (injectable clock), `SessionManager`, `BellPlayer`, `DndController`, foreground service + notification | Engine unit tests cover countdown, pause/resume accuracy, stop, <10 s discard, completion |
| M5 | **Timer UI**: wheel picker, states, DND switch + permission flow, notification permission | Works end-to-end on device |
| M6 | **Calendar UI**: month grid, tinting, day detail, totals | Multiple sessions per day aggregate correctly |
| M7 | **About/licences** page via AboutLibraries | App and dependency licences visible |
| M8 | **Release & docs**: keystore generated locally (outside repo), signing config via gitignored `keystore.properties`, R8 minify, README (build, install via `adb`, audio regeneration, licence notes), final licence audit | `assembleRelease` yields signed APK; checklist below passes |

## 7. Tooling notes for this machine

- Android SDK found at `%LOCALAPPDATA%\Android\Sdk` (platforms 34–36.1, build-tools up to 37, platform-tools, emulator).
- System `java` is **1.8**, too old for current AGP. Gradle will run with the JDK bundled in Android Studio (`C:\Program Files\Android\Android Studio\jbr`), set through `JAVA_HOME` at invocation. No machine-specific paths are committed (`local.properties` is gitignored).
- `ffmpeg` is not installed, hence Python-based OGG encoding. Python needs `numpy` and `soundfile` (installed in a throwaway venv, not committed).

## 8. Verification

Automated: `gradlew testDebugUnitTest lintRelease assembleRelease`.

Manual checklist on your phone (these cannot be unit-tested):
1. Fresh install shows 00:20:00; after starting 00:07:30 and returning, the wheels show 00:07:30.
2. 5 s countdown → start bell → timer runs. Pause/Resume from the notification and from the app both work.
3. Lock the screen mid-session: end bell rings on time with the screen off.
4. DND turns on at Start and off at end **and** on Stop; a pre-existing DND setting is left untouched; denying the DND grant still lets sessions run.
5. Stopped session of ~1 min appears in the calendar with ~1 min; two sessions in one day sum correctly.
6. Bells audible with phone in Priority-only DND, and at a pleasant volume.
7. About page shows MIT text and dependency licences.

## 9. Risks / open points

- **Bell volume** on the alarm stream depends on your alarm volume; if it is too loud/quiet we can switch to the media stream or add a volume setting.
- **OEM battery policies** can be stricter than stock Android; foreground service + wake lock should cover it, to be confirmed on your device.
- **Release key**: back up the generated keystore, otherwise future updates will not install over the existing app (you would need to uninstall first, which also wipes the calendar data).
- **Sound quality** is judged by you; I can only verify technical sanity.
- The repo is initialised locally only; creating the GitHub remote and pushing is left to you.
