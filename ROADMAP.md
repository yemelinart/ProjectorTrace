# Roadmap toward Stable 0.9

Updated 2026-09-22. Proposed work below is not shipped functionality or a promised delivery date. The current public development candidate is **0.9.0-rc.1**.

## Release validation — before Stable 0.9

- [ ] Record physical projector models, Android versions and remote-control layouts. Include a low-memory device and a 32-bit device if those remain supported.
- [ ] Run at least one uninterrupted 60-minute drawing session on each representative device, including image replacement, geometry, several filters, background/resume and screen sleep. Record crashes, freezes and observable input delays.
- [ ] Verify USB removal/reconnection, slow providers, missing files, denied/revoked/partial permissions, restart restoration, and the absence of a system file picker. Provide useful recovery text when operations fail.
- [ ] Exercise minimum-version decoding/blending and a 16 KB runtime; inspect 720p/1080p/4K/portrait layouts and focus with the real remote.
- [ ] Check every visible tool and reset command against what the UI describes. Verify unexpected filter failures are explained instead of silently falling back.
- [ ] Set up the permanent signing and update path, then repeat a representative smoke test with the exact signed candidate.
- [ ] Triage all reproducible release-blocking failures and publish a supported-device/test-results list.

These are acceptance checks, not a guarantee that software has no bugs. Store publication additionally requires the Play permissions/policy review and account-specific testing in [PLAY-READINESS.md](docs/PLAY-READINESS.md).

## Highest-value product improvements — proposals

### 1. Undo / Redo

Restore a previous alignment, crop or filter change without resetting an entire tool. Treat a held remote key as one edit so history remains useful. This is the first recommended enhancement. It is not implemented in RC1.

### 2. Named projects and calibration presets

The app currently has a single saved setup profile. Add multiple named projects containing the image reference, perspective, crop, palette and filters. Save projector/surface calibration separately so it can be reused with another image. Handle missing files explicitly and support project export/import. Proposed; no current multi-project claim.

### 3. Guided surface calibration

Walk the artist through placing the projected image on four physical canvas corners, then locking that geometry. An optional measured reference can support a scale grid; centimetres must never be claimed without calibration. Existing manual corner adjustment remains available.

### 4. Quick transfer from a phone

A local-network QR workflow could move a reference from phone to projector without a USB stick. This is a proposal: RC1 has no transfer server and no INTERNET permission. It would need an explicit session, local authentication, expiry and a privacy review before shipping.

### 5. A simpler start and reliable recovery

Offer a short remote-control walkthrough, an immediate Original/Adjusted preview, clearer loading/cancel/error messages, and tested recovery when USB disappears. Add accessible labels and Russian UI text. The current app has comparison modes; the proposal is to make them easier to reach, not invent a new comparison feature.

### 6. Export an outline or palette

Save the processed reference or palette for a sketchbook, printing or another device. Currently these are displayed in the projector workspace, not exported. Document source dimensions and avoid presenting extracted image colors as calibrated physical paint formulas.

## Product position

Keep the core useful without an account, subscription or advertising, consistent with the current free/open-source promise. Paid distribution/support is a separate product decision, not a feature shipped in this roadmap. Usability, dependable file opening and recoverable edits take priority over adding more filters.
