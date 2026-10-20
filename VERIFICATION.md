# Verification

## Version 1.2.0

- TypeScript checking and production dashboard bundle passed.
- SDK-only Java compilation, resource packaging, D8, alignment and APK signature verification passed.
- Version code 3; signing certificate SHA-256 matches v1.1: `dca922d2e205998600e238a0bfb7e923855c80f3b5b8d4d8cd004baa15a055ae`.
- All 13 gesture routing checks passed.
- Progress model checks passed: native storage adapter, habit scores/streaks and archival, editing, JSON backup round-trip, invalid backup preservation, and balance snapshot accounting.
- Browser QA: metric logging, habit completion, persistence after reload, and chart updates passed. Phone viewport (390 × 844) visually inspected.
- No Android device was connected. The actual WebView bridge, Android document pickers, installation and in-place upgrade have not been exercised on a device. Certificate compatibility is verified, but device runtime testing remains necessary.


## Version 1.1.0

- SDK-only build passed; APK signature verified.
- Signing certificate SHA-256 matches 1.0.0, permitting in-place updates. Version code increased to 2.
- 13 gesture policy checks passed, including scroll preservation, diagonal movement and long-press rejection.
- GitHub Actions compile, lint, gesture tests and artifact upload passed: [successful run](https://github.com/marcuschelete/King/actions/runs/39205510367).
- Downloaded 1.1.0 APK hash matches the local signed artifact; see `downloads/SHA256SUMS`.
- Android 10 software emulator booted, but installation of the **old 1.0.0 APK** timed out after 90 seconds during upgrade-test setup. The new UI and actual in-place upgrade were therefore not exercised on that emulator. No runtime or visual QA pass is claimed for 1.1.0; physical-device checking remains necessary.

## Version 1.0.0

- Standard Gradle `assembleDebug lintDebug` passed locally: zero lint errors.
- GitHub Actions build, lint and artifact upload passed: [successful run](https://github.com/marcuschelete/King/actions/runs/39180700888).
- SDK-only build script also passed using Android SDK 35, official AAPT2, D8, zipalign and apksigner.
- Published APK signature verified. Downloaded GitHub APK SHA-256 matches the locally signed file; see `downloads/SHA256SUMS`.
- Minimum Android version: API 28 (Android 9); target: API 34.
- Android 10 software emulator: APK installation, app launch, Home rendering, installed-app discovery and opening the ritual editor passed. Home and Apps screenshots were visually inspected.
- The extended UI script stopped at a case-sensitive test selector (`Cancel` versus Android's `CANCEL`). No app crash appeared in that run. The full interaction suite was not completed; focus, backups, permissions and routine-history behavior have not been verified end to end on a device.

Physical-device behavior, OEM-specific Home selection and screen-time accuracy still require testing on the user's phone. Calendar and voice depend on the installed providers. No claim of complete parity with another product is made.
