# Verification

Version: 1.0.0

- Standard Gradle `assembleDebug lintDebug` passed locally: zero lint errors.
- GitHub Actions build, lint and artifact upload passed: [successful run](https://github.com/marcuschelete/King/actions/runs/39180700888).
- SDK-only build script also passed using Android SDK 35, official AAPT2, D8, zipalign and apksigner.
- Published APK signature verified. Downloaded GitHub APK SHA-256 matches the locally signed file; see `downloads/SHA256SUMS`.
- Minimum Android version: API 28 (Android 9); target: API 34.
- Android 10 software emulator: APK installation, app launch, Home rendering, installed-app discovery and opening the ritual editor passed. Home and Apps screenshots were visually inspected.
- The extended UI script stopped at a case-sensitive test selector (`Cancel` versus Android's `CANCEL`). No app crash appeared in that run. The full interaction suite was not completed; focus, backups, permissions and routine-history behavior have not been verified end to end on a device.

Physical-device behavior, OEM-specific Home selection and screen-time accuracy still require testing on the user's phone. Calendar and voice depend on the installed providers. No claim of complete parity with another product is made.
