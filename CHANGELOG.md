# Changelog

## 0.9.0-rc.1 — 2026-09-22

**Candidate for Stable 0.9.** Not yet a stable declaration or Google Play production release. This is the first public 0.x version; 8.x were development labels. Android versionCode increases from 851 to 900, so the new name is not an Android downgrade. An in-place update still requires the same signing certificate.

This candidate packages the audited 8.5.1 changes. The version change itself does not introduce new creative tools.

### Fixed and improved

- Bound full-image and thumbnail decoding on both axes, including panoramas and tall scans; use a smaller limit on low-memory devices.
- Serialize CPU filter work and cancel obsolete computations during rapid remote adjustments.
- Avoid rebuilding outline pixels when only strength changes; prevent stale source/effect results from being displayed.
- Restore the last accessible image and correctly recognize access inherited from selected folders.
- Coalesce settings writes and preserve the latest settings when the activity stops.
- Make Back return through controls and then exit to the TV launcher.
- Respect Keep Screen Awake after focus/resume.
- Correct mirrored EXIF orientation; support image orientation on Android 6 and legacy overlay blending on Android 6–9.
- Bound repeated movement/rotation and directory traversal; make scanning cancellable.
- Reduce the fixed launch wait from 3 seconds to 350 milliseconds.
- Enable optimized release packaging; add an in-app privacy screen and a public policy.
- Add JVM/device regression tests and automated GitHub builds.

### Evidence and limits

The underlying 8.5.1 application logic passed 7 JVM tests, 6 device tests and manual Android 16 TV emulator checks, including a test-signed optimized release build. The 0.9 RC1 package is rebuilt with the new version metadata; JVM tests and lint are rerun. Do not represent the earlier device tests as a new physical-device certification. Lint has no errors; the audit documents remaining warnings. APK size is approximately 7.6 MB; download packaging size is not a runtime performance benchmark.

The downloadable RC uses a **test signing certificate**. The AAB is **unsigned** and intended for release preparation. Neither is a production-signed Play submission. No previously distributed key is replaced. The original v8.5 APK remains in its own release.

### Before Stable 0.9

Pass the device and sustained-session checks in [ROADMAP.md](ROADMAP.md), review failures and document supported devices. Prepare and verify an owner-controlled signing/update path. Google Play has additional [submission gates](docs/PLAY-READINESS.md).

## 8.5.1 — 2026-09-22

Engineering hardening candidate. See the [detailed audit](docs/AUDIT-2026-09-22.md).

## 8.5 — recovered original

Original Android TV projector utility, preserved APK and recovered open-source project. See [recovery notes](docs/RECOVERY.md). Website screenshots and the presentation PDF show this version unless otherwise labelled.
