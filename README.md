# KING Focus

A quiet, original Android home screen for intentional phone use. Monochrome, local-first, and designed around daily routines. Inspired by the functional idea of minimalist launchers; not affiliated with or a copy of Perful's mono. app, code, artwork, or brand.

## Install on your phone

**[Download KING Focus 1.1.0 APK](https://github.com/marcuschelete/King/raw/refs/heads/main/downloads/KING-Focus-1.1.0.apk)**

Android **9 or newer**, including Android 10. No Google Play Services required.

1. Open the APK download link on your phone. Download the **APK**, not this repository's source ZIP.
2. Open the downloaded file. If Android asks, temporarily allow your browser/file manager to **install unknown apps**, then install KING Focus. You can disable that permission again afterwards.
3. Open **KING Focus → ⚙ → Set KING as default Home app**, then select KING Focus.
4. **Swipe up** from Home to open Apps. Long-press your essentials and choose **Pin to home**. Mark distracting apps to add a five-second pause and an intention prompt.
5. **Swipe left** from Home for the dashboard: routines, tasks, notes, capture, calendar and usage. **Swipe right** from Home for Focus. Tap **Open routine** on the dashboard to edit rituals. Swipe horizontally back toward Home; Android Back also works.
6. Calendar and Usage Access are optional; enable only the features you want. Speech input uses your installed speech provider.

To switch back: **KING → ⚙ → Switch back to another launcher**, or Android **Settings → Apps → Default apps → Home app**. Menu wording varies by manufacturer. The original launcher is not removed.

## Version 1.1: quiet home, swipe navigation

Install this APK **over the existing app**, without uninstalling. It uses the original signing key and preserves preferences, pins, notes, tasks and routine history. Export a backup first if desired.

- Centered clock/date and smaller centered essentials; no branding banner, section headings or bottom tabs on Home.
- Five essentials on Home (the first five visible pins); additional apps stay searchable in the drawer. Long-press → Move to top changes the selection without deleting pins.
- Swipe **up**: app drawer. Swipe **left**: dashboard. Swipe **right**: focus.
- Dashboard and routine lists scroll normally. Swipe right from dashboard to Home; swipe left from focus to Home. Swipe down at the top of the app drawer to close it.
- Rounded monochrome dashboard cards; intention, routine history and capture moved off Home.
- Settings: tap **⚙** or long-press the clock. Settings also offers tap-based navigation for users who do not want gestures.
- Status bar hidden on first upgrade for a calmer home; it can be restored in Settings. Android's own navigation buttons remain available.

## What is included

| Area | KING implementation |
| --- | --- |
| Quiet home | Centered text-only essentials, searchable app drawer, pins, move-to-top, rename and hide/restore apps |
| Monochrome appearance | Midnight black / milk white, adjustable app font size and weight, right alignment, outlined letter icons with three stroke weights |
| Fullscreen and touch | Optional hidden status bar, matching navigation bar, optional subtle haptics, swipe between sections |
| Quick capture | Local explicit `todo:` / `note:` routing; other text becomes a note; voice transcription can be reviewed before saving |
| Tasks and notes | Create, edit, complete tasks, delete entries with confirmation; stored locally |
| Awareness | Today's screen-active minutes and unlock events, using optional Android Usage Access |
| Calendar | Next ongoing/upcoming event within seven days, with optional read-only permission |
| Daily rituals | Editable daily times, completion tracking, per-ritual streaks, next ritual on Home, midnight reset |
| Reflection | Editable intention and dated evening review |
| Focus | 25 / 50 / 90 minute sessions, task intention, persistent timer, completed-session count |
| Distraction friction | Five-second launch pause plus written purpose; distracting apps cannot be launched through KING during active focus |
| Data control | Export/restore JSON backup with validation; no analytics, ads, accounts, or app network permission |

## Honest limits

This is an independent first version, **not a pixel-perfect reproduction or verified complete feature parity** with mono. It uses original typography and letter icons rather than mono's proprietary artwork, pixel icon library, or animation system. Capture routing uses explicit prefixes, not AI or natural-language task inference.

- Focus controls are **launcher-only**. Android notifications, recent apps, links, other launchers and system settings can still open apps. There is no accessibility service, device-admin lock or system-wide blocking.
- Timers survive process restarts. They use monotonic time until reboot, then wall-clock time. Completion is recorded on the next visit; **there is no background alarm**. Ritual times are visual cues, not push reminders.
- Usage reporting depends on device/OEM event history. Screen-active minutes include the lock screen and are not the same metric as app foreground time.
- Calendar data is read from calendars already available on the phone. The app itself never syncs calendar accounts.
- Voice input requests offline recognition, but the chosen speech provider may use the internet. Typed capture is fully local.
- No work-profile app discovery, third-party Android widget hosting, notification filtering, or icon-pack support in v1.
- Backups are unencrypted. Store them privately. The app opts out of Android cloud backup.
- This is a sideloaded personal app, not a Play Store-reviewed release. See [verification](VERIFICATION.md) for what was actually tested.

## Build and updates

Native Java, Android platform widgets, no third-party app dependencies. Standard build: JDK 17, Gradle 8.9, Android SDK 35:

```sh
cd android
gradle assembleDebug lintDebug
```

Or use the SDK-only build:

```sh
export ANDROID_HOME=/path/to/android-sdk
bash scripts/build-apk.sh
```

The SDK-only build requires Python 3, JDK 17, platform `android-35`, and build tools `35.0.0`. It writes `android/app/build/manual/KING-Focus.apk`. Without signing variables it produces a development-signed APK. GitHub Actions also produces a **development-signed** artifact; it is not an update for the distributed APK.

The APK under `downloads/` is signed with a private **KING** key. The private update bundle is kept separately and is **never committed**. For compatible updates, use that same keystore (`KING_KEYSTORE`) and its password (`KING_SIGN_PASS`) with `scripts/build-apk.sh`, and increment the version in both the Gradle file and SDK-only script. Never put the key/password in a GitHub issue, source file, or public artifact.

Export a backup before uninstalling or trying a differently signed development build. Android requires uninstalling when switching signing keys, which deletes app data.
