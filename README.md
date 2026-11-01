# KING Focus

A local-first Android launcher with translucent panels, customizable wallpaper and an offline RIVAL progress dashboard.

## Install

**[Download KING Focus 1.4.0 APK](https://github.com/marcuschelete/King/raw/refs/heads/main/downloads/KING-Focus-1.4.0.apk)**

Android 9 or newer and an up-to-date Android System WebView. Download the APK, open it and allow your browser/file manager to install it if Android asks. Install **over your existing KING app without uninstalling** to keep your data. Version code 8 uses the original KING signing certificate. Settings offers full JSON backup and restore.

Choose KING under Android Settings → Apps → Default apps → Home app, or use KING Settings. Switch back through the same menu at any time.

## What's new in 1.4

- **Home folders:** tap + New folder, name it, then choose apps. Tap the folder to expand its icon-only grid. Icons use the installed apps' real artwork in grayscale. Long-press a folder to rename, reorder, change apps or delete the folder. Long-press an app for its accessible name and actions. Existing home pins are preserved.
- **Alphabet navigation:** swipe up for Apps. Tap or drag the left A–Z/# rail to jump to a group; search stays at the top. Accented Latin names are grouped by their base letter.
- **Habit calendar:** the dashboard has a monthly dot calendar using the same commitments and completions as RIVAL. Select a habit or all habits, browse previous months and tap a day to update it. Manage habits in RIVAL. The unused Intention and Capture cards are removed; saved notes remain in Settings.
- **Quick finance:** Income, Expense and Accounts buttons open the relevant form directly. Add ABSA, RedotPay, Orange Money, MyZaka, Cash at hand or a custom account; choose its currency and opening balance/date.
- **Account ledger:** income, expenses, transaction fees and account transfers update balances. Cash is an account too. Edit/delete entries, archive accounts and export a ledger CSV. Transfers do not count as income; different currencies stay separate. Cross-currency transfers require the amount received. Account tracking is manual, with no bank connection or exchange-rate service.
- **Safe continuity:** older money snapshots remain under Earlier money records, separate from the new account ledger because their accounts are unknown. Full progress backups include the ledger. Existing tasks, pins, preferences and progress remain in place during an in-place upgrade.

## Navigation and appearance

Swipe **left** from Home for the dashboard; swipe **right** for the calendar agenda and focus; swipe **up** for Apps. Back returns Home. Settings also provides tap navigation. Long-press the clock for Settings.

Choose the alpine background, your own photo or solid background in Settings. Both native panels and the RIVAL dashboard use translucent glass styling. Long-press either bottom corner shortcut to assign any installed app; the defaults are phone and calculator. Distraction rules still apply when launching apps from folders or shortcuts.

Proof of progress → Raise your standard opens RIVAL: Today, Stats, Rival, Log and Money. It includes habits, streaks, progress graphs, body measurements, chess ratings, grades and other metrics. Timed launcher rituals remain separately available in Settings.

## Calendar and updates

Calendar permission is optional. The agenda reads all selected calendar sources available through Android's local calendar provider, including synced school/subscribed sources; it does not filter to events you created. Choose sources and a 1/3/7/14-day horizon. If a school calendar appears only on Google Calendar's website and is not synced to Android, KING cannot read it; source diagnostics help identify this. KING does not sync calendar accounts itself.

On app activation, enabled update checks run at most every six hours against this repository's HTTPS update feed. A newer release prompts you to download its APK; Android still asks you to install it. Settings provides a manual check and an automatic-check toggle. Offline features work without a connection. The APK has Internet permission for these checks; no analytics or ads are included.

## Limits and verification

Focus blocking applies inside this launcher, not system-wide. Timers have no background completion alarm. Usage statistics depend on Android/OEM event history. No work-profile discovery, Android widget hosting or icon-pack support is included. Backups are unencrypted and should be stored privately; Android cloud backup is disabled.

See [VERIFICATION.md](VERIFICATION.md) for exact checks. Native folder gestures, device installation, calendar-provider sync and real-device upgrade still need phone testing.

## Build and updates

Native Java launcher plus an offline React/TypeScript, Zustand and Recharts dashboard. Runtime dependency license notices are included in progress/THIRD_PARTY_NOTICES.txt. Standard build: JDK 17, Gradle 8.9, Android SDK 35:

```sh
cd android
gradle assembleDebug lintDebug
```

Build the progress assets before either Android build (Node 22.13+):

```sh
cd progress
npm ci
npm run build
node test-model.mjs
node test-finance.mjs
cd ..
```

The cross-platform signed SDK-only build is `python scripts/build-apk.py`, with `ANDROID_HOME`, `JAVA_HOME`, `KING_KEYSTORE` and `KING_SIGN_PASS` set. The signing secret and keystore stay outside the repository. It compiles, signs, verifies and runs gesture checks.

Or use the shell SDK-only build:

```sh
export ANDROID_HOME=/path/to/android-sdk
bash scripts/build-apk.sh
```

The SDK-only build requires Python 3, JDK 17, platform `android-35`, and build tools `35.0.0`. It writes `android/app/build/manual/KING-Focus.apk`. Without signing variables it produces a development-signed APK. GitHub Actions also produces a **development-signed** artifact; it is not an update for the distributed APK.

The APK under `downloads/` is signed with a private **KING** key. The private update bundle is kept separately and is **never committed**. For compatible updates, use that same keystore (`KING_KEYSTORE`) and its password (`KING_SIGN_PASS`) with `scripts/build-apk.sh`, and increment the version in both the Gradle file and SDK-only script. Never put the key/password in a GitHub issue, source file, or public artifact.

Export a backup before uninstalling or trying a differently signed development build. Android requires uninstalling when switching signing keys, which deletes app data.
