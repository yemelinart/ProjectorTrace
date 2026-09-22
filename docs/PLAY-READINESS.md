# Google Play release checklist — 8.5.1 candidate

This is a hardening candidate, not an approved Google Play release. Review date: 2026-09-22.

## Technical scope

Android TV / Google TV only (`android.software.leanback` is required). Android 6+ (minSdk 23), targetSdk 35. The app has no INTERNET permission, accounts, ads, analytics or backend. All image processing is local. Some user-selected document providers can fetch cloud files independently of this app.

For TV, Google's current new-app target requirement is API 34+, so target 35 meets that specific gate. Do not use this statement for a phone release, which has different requirements.

Release builds enable R8 code optimization and resource shrinking. `./gradlew bundleRelease` creates an **unsigned** AAB until an owner-controlled upload signing configuration is supplied. Never submit the debug APK or a debug-signed bundle to production. No permanent signing key has been created or committed by this audit.

## Gates before submission

- [ ] Choose/confirm the Play Console developer account, identity verification, public support email and developer details.
- [ ] Create and securely back up the upload key; enable Play App Signing; sign the candidate AAB. Keep all keys and passwords outside the repository.
- [ ] Resolve the photo permissions declaration. The TV library currently requests READ_MEDIA_IMAGES to browse local/USB-indexed images when no usable system picker exists. Document why frequent browsing is core, demonstrate system-picker limitations on supported TVs, and obtain approval. If this does not qualify, implement a picker-only distribution and validate it on real TVs before removing broad permissions. Do not silently remove the library from existing projector users.
- [ ] Confirm the privacy policy against the signed artifact, publish its public URL and provide it in Play Console. The in-app policy is in Settings → Privacy. Verify public support contact; GitHub issues are currently offered.
- [ ] Complete Data safety (currently no developer collection/sharing), content rating, target audience, ads declaration and app-access declaration. These are owner attestations, not automatically submitted by this repository.
- [ ] Test the optimized signed build with physical projectors, USB storage, several file-provider apps, permission denial/revocation, sleep/wake and Android 6/8/11/13/14/16 where supported. Exercise all filters with low RAM and large files for at least a sustained session.
- [ ] Check full five-way D-pad navigation, Back to launcher, focus restoration after file selection, 720p/1080p/4K readability and overscan. Confirm Keep Screen Awake use complies with TV ambient-mode review requirements for this projection workflow.
- [ ] Verify 16 KB runtime compatibility on a suitable device/emulator in addition to native ELF and package alignment checks. Check 32-bit hardware separately.
- [ ] Upload accurate TV banner/icon, current unaltered TV screenshots, store text and support contact. Avoid claiming no competing apps or guaranteed compatibility with every projector.
- [ ] Run Play pre-launch report, address any findings, then closed testing. For personal accounts created after 13 Nov 2023, the current rule is 12 opted-in testers continuously for 14 days before applying for production access.

## Official references

- [TV quality criteria](https://developer.android.com/docs/quality-guidelines/tv-app-quality)
- [Target API requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Photo/video permissions](https://support.google.com/googleplay/android-developer/answer/14115180?hl=en)
- [User data and privacy policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)
- [Personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)
