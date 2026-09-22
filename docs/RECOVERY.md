# Source recovery — 2026-09-22

Version: 8.5 (versionCode 850), package `com.projectortrace`.

The original Android Studio working directory was unavailable. Source files were reconstructed by replaying the successful code patches and literal version substitutions recorded in the original Codex development session, including patches embedded in shell calls. Historical shell commands were not executed wholesale. All recovered application source patches applied successfully.

The three branding PNGs and 48 menu icons were extracted unchanged from the preserved original v8.5 APK. The Gradle wrapper JAR was restored from the official Gradle v8.13.0 repository. Old incomplete function guides were excluded; the README documents the recovered version.

The original APK has SHA-256:

```text
2d0c34611c80c3b4a5a4c5014ab0fc483c5b6de8cec1653bdec16084ba1407af
```

The recovered source and original binary are separate artifacts. Building the source is checked independently; a byte-for-byte match is not claimed. No private signing keys, local SDK configuration, personal image library, conversation transcript or recovery working files are included.

## Original v8.5 changes

- Removed additional D-pad direction remapping from projection tools.
- Added the Image / White Blink mode alongside Image / Black and Blink Compare.

The physical projector was not used during recovery. Runtime behavior on a projector remains to be verified.

## Recovery build validation

`./gradlew :app:assembleDebug :app:lintDebug` passed on 2026-09-22 using Android Studio’s bundled JDK and Android SDK 35. Lint warnings remain; there were no blocking lint errors. No emulator or physical projector was started.

All six DEX files (`classes.dex` through `classes6.dex`) in the recovered debug build match the preserved original APK byte-for-byte. This confirms matching compiled code for this validation build; the entire signed APK is not claimed to be identical. Android lint reported 59 warnings and 6 hints, with no errors.
