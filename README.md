# Projector Trace

[![Projector Trace presentation](site/assets/social-cover.png)](https://yemelinart.github.io/ProjectorTrace/)

[Explore the visual website](https://yemelinart.github.io/ProjectorTrace/) · [Presentation PDF](https://yemelinart.github.io/ProjectorTrace/Projector-Trace-Presentation.pdf)

An Android TV / Google TV app for artists who project reference images onto a canvas. Position, scale, rotate and adjust an image using the projector's remote — no touchscreen required.

**By S. Yemelin · 0.9 RC1 — candidate for Stable 0.9 · Kotlin + Jetpack Compose**

[Download test APK](https://github.com/yemelinart/ProjectorTrace/releases/tag/v0.9.0-rc.1) · [Русский](README.ru.md)

## Toward Stable 0.9

The current candidate is **0.9.0-rc.1**, packaging the audited 8.5.1 improvements. It is available for testing and is not yet declared stable or approved for Google Play. Development labels 8.x are being replaced by public 0.x versioning; the Android update code increases to 900. The original v8.5 release remains archived.

[Visual release journal](https://yemelinart.github.io/ProjectorTrace/releases.html) · [Changelog](CHANGELOG.md) · [Roadmap](ROADMAP.md) · [Audit and tests](docs/AUDIT-2026-09-22.md) · [Play checklist](docs/PLAY-READINESS.md)

## Features

- Open images from the gallery or a compatible Android file picker.
- Move, zoom, rotate, flip and fit the reference image.
- Adjust perspective with corner distortion and crop the projected image.
- Use grids, rulers, a center cross, canvas frames and corner guides.
- Adjust brightness, contrast, opacity, threshold, outlines, blur and other tracing filters.
- Display a color palette; save and load a setup profile; lock adjustments.
- Rotate the working scene for different projector orientations.
- Blink between image/black or image/white, or compare filtered, original, outline and black-and-white views.

## Install

Download `ProjectorTrace-V8.5.apk` from Releases and transfer it to your Android TV / Google TV projector. Open it with a file manager and allow that app to install unknown apps if Android asks.

Android 6.0 (API 23) or later is required. The app is designed for a D-pad remote and the Android TV environment. File browsing depends on the file picker available on your device.

The RC1 APK contains optimized release code signed with a **test certificate**. The accompanying AAB is **unsigned**. These are not production Play artifacts. The original v8.5 APK is preserved in its own release. Updating an installed APK requires the same signing certificate; if Android rejects the update, do not uninstall an existing working version without preserving your setup. No signing keys are included in this repository.

## Basic use

1. Open **Image → Gallery** or **Browse Files** and select a reference image.
2. Choose a tool from **Transform**, **Guides** or **Filter**.
3. Use the D-pad to adjust it. OK confirms or cycles a tool's selected element, depending on the mode.
4. Back closes the current controls and eventually exits to the TV launcher. Menu toggles controls; Right opens them from the canvas. Menu navigation uses Up/Down, Right to enter and Left to go back.
5. Configure orientation and Blink in **Settings**. When Blink is enabled and the controls are closed, OK switches the selected view.

## Build from source

Open this repository folder in Android Studio. Use JDK 17 or a compatible Android Studio bundled JDK, and install Android SDK Platform 35. Android Studio normally creates `local.properties` for your SDK; command-line users can set `ANDROID_HOME` instead.

```sh
./gradlew :app:assembleDebug
./gradlew :app:lintDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest # with a connected test device
./gradlew :app:bundleRelease # unsigned until upload signing is configured
```

On Windows, use `gradlew.bat`. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The project uses Gradle 8.13, Android Gradle Plugin 8.13.2 and Kotlin 2.0.21. The first build needs internet access to download dependencies.

## Project structure

- `MainActivity.kt`: activity and fullscreen setup.
- `ui/TraceScreen.kt`: projection, remote input, image processing and settings.
- `ui/TraceOverlay.kt`: menus and on-screen controls.
- `model/TransformState.kt`: transformation state and tool modes.

These Kotlin files are under `app/src/main/java/com/projectortrace/`.

## Recovery and verification

The original working folder was unavailable in September 2026. The Kotlin and Gradle sources were recovered from the original Codex development history through version 8.5. Original PNG resources were recovered from the preserved v8.5 APK. See [recovery notes](docs/RECOVERY.md).

The recovered project builds successfully and passes Android lint with warnings. All six compiled DEX files in the validation build match the original APK byte-for-byte.

The original APK is distributed separately from the recovered source. Do not assume a new build is byte-for-byte identical to that APK. Physical-projector behavior, including rotated D-pad controls, should be checked on the target device.

## Contributing and license

Issues and pull requests are welcome. For a bug report, include your projector model, Android version, app version, projector orientation and the steps to reproduce it. Run the build and Android lint before submitting changes.

Project source is available under the [MIT License](LICENSE). AndroidX, Kotlin and Gradle retain their own licenses; see [third-party notes](THIRD_PARTY_NOTICES.md).
