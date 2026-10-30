# Release Status

**Status: Integration candidate — NOT production ready.**

This package contains the Android source, Compose UI, Room local database, Firebase authentication helpers, Firestore repository helpers/rules, a logo asset, and a GitHub Actions debug-build workflow.

## Confirmed by offline preflight
- Package/source files are present.
- XML resources and Android manifest parse successfully.
- Firestore rules include family membership checks and a default-deny catch-all.
- Gujarati, Hindi and English starter string resources are present.
- ZIP integrity and static checks can be run with `python PREFLIGHT_TESTS.py`.

## Still required before production
1. Implement and test Room ↔ Firestore bidirectional synchronization, including offline queue/retry, conflict handling and deletion propagation.
2. Replace Firebase placeholder values with the owner's Firebase project values; enable Email/Password and Phone providers and test SHA fingerprints.
3. Finish full-screen Gujarati/Hindi/English localization and wire a persistent language selector.
4. Test invitation/membership workflows and Firestore rules against the Firebase Emulator Suite or a dedicated test project.
5. Build `assembleDebug` in GitHub Actions, install the generated APK on a real device, and run multi-device acceptance tests.
6. Review privacy, backup/restore, data export, error handling and release signing before publishing.

Do not treat the presence of Firebase helper methods as proof that multi-device sync works. This package does not yet meet the user's confirmed requirements for full cloud sync and complete language switching.
