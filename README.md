# Meditate

A minimal, offline meditation timer for Android (Kotlin, Jetpack Compose).

- Timer with three wheels (HH : mm : ss). The duration you last started with is remembered (20 min on first launch).
- 5 second countdown, then a soft bowl-bell; three strikes when the session ends.
- Pause, resume or stop at any time; a notification with the same controls keeps working with the screen off.
- Optional Do Not Disturb ("Priority only") for the length of the session; the previous state is restored afterwards.
- Calendar with the days you meditated and the time spent; tap a day to see every session. Several sessions per day are summed.
- About page with the app's licence and the licences of all dependencies.
- No network access (the app does not even request the `INTERNET` permission), no analytics, no ads.

Requires Android 12 (API 31) or newer. Not intended for the Play Store; install it manually.

## How the details work

- **Recorded time** is the time actually meditated, excluding pauses and the countdown. Sessions stopped early are saved too (marked "Stopped early"); sessions under 10 seconds are discarded. A session that crosses midnight counts for the day it started.
- **Do Not Disturb** needs a one-time "Do Not Disturb access" grant in system settings; the app takes you there. If DND is already on when a session starts it is left untouched, and it is only switched off again if it is still in the state the app set.
- **Sounds** are played on the *alarm* audio stream so they are heard in DND. Use the volume keys while the app is open to adjust them.
- Stopping a session manually is silent (the end bell only rings when the timer reaches zero).

## Build

Prerequisites: Android SDK (platform 37, build-tools) and a **JDK 17+** (Android Studio's bundled JBR works).

```bash
# once: tell Gradle where the SDK is
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew testDebugUnitTest      # unit tests
./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew lintRelease assembleRelease
```

On Windows use `gradlew.bat`, and set `JAVA_HOME` to a JDK 17+ if your default `java` is older.

### Signed release APK

Create a keystore once (keep it somewhere safe and **back it up**: without it you cannot update an installed app in place):

```bash
keytool -genkeypair -v -keystore ~/.android/meditate-release.jks -alias meditate \
  -keyalg RSA -keysize 4096 -validity 36500
```

Then create `keystore.properties` in the repository root (it is git-ignored):

```properties
storeFile=/home/you/.android/meditate-release.jks
storePassword=...
keyAlias=meditate
keyPassword=...
```

`./gradlew assembleRelease` then produces a signed, minified `app/build/outputs/apk/release/app-release.apk`. Without `keystore.properties` the release APK is simply left unsigned.

### Install on a phone

Enable USB debugging, connect the phone, then:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Or copy the APK to the phone and open it (allow installs from that source).

## Sounds and icon

Everything is generated; nothing is taken from third parties.

- Bells: `tools/audio/generate_sounds.py` synthesises a singing-bowl style bell (inharmonic partials, slow beating, soft attack, light reverb). Regenerate with:

  ```bash
  pip install numpy soundfile
  python tools/audio/generate_sounds.py
  ```

  Tweak the constants at the top of the script (pitch, decay, reverb) and re-run to change the character. Output goes to `app/src/main/res/raw/`.
- Icon: hand-written adaptive vector drawables in `app/src/main/res/drawable/ic_launcher_*.xml` (with a monochrome layer for themed icons).

## Project layout

```
app/src/main/java/io/github/romanfabio/meditate/
  session/   state machine (SessionEngine), SessionManager, foreground SessionService
  audio/     BellPlayer
  dnd/       DndController
  data/      Room database, repository, preferences
  ui/        timer, calendar, about, theme
tools/audio/ sound generator
```

`SessionEngine` has no Android dependencies and is unit-tested with a fake clock.

## Licences

The app is MIT licensed, see [LICENSE](LICENSE). The build only allows dependencies under permissive licences (Apache-2.0, MIT, BSD, ISC): the [AboutLibraries](https://github.com/mikepenz/AboutLibraries) Gradle plugin (build time only, nothing from it ships in the app) reads the licence metadata of every dependency, fails the build on anything else, and generates the data shown on the About page. At the time of writing all runtime dependencies are Apache-2.0 (AndroidX, Jetpack Compose, Room, Kotlin and kotlinx.coroutines).
