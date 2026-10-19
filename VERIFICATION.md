# Verification

Version: 1.0.0

- Compiled Java against Android SDK 35.
- Android resources and manifest compiled with official AAPT2.
- DEX produced with official D8; APK aligned and signature verified with Android SDK tools.
- Minimum Android version: API 28 (Android 9); target: API 34.
- Standard Gradle `assembleDebug lintDebug` passed (43 tasks; zero lint errors).
- Android 10 software-emulator interaction checks are in progress; not yet claimed as passed.

Physical-device behavior, OEM-specific Home selection and screen-time accuracy still require testing on the user's phone. Calendar and voice depend on the installed providers. No claim of complete parity with another product is made.
