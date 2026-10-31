# SJ & CO.DIT Office Manager

Android app for office administration with a Marathi-friendly mobile interface.

## Current features
- Home dashboard and quick access
- Tea, coffee and other daily expense entries
- Employee attendance records
- Employee list
- Summary reports
- CSV export
- Offline storage on the current device using browser local storage

## Build an APK
1. Open the **Actions** tab in this repository.
2. Select **Build Android APK**.
3. Tap **Run workflow** on the `main` branch.
4. When the run succeeds, open that run and download the `SJ-CO-DIT-OfficeManager-debug-apk` artifact.

The artifact is a debug APK, not a Play Store release. Data currently stays on the device; cloud sync and backup are not implemented.

## Project
Native Android WebView shell with the UI in `app/src/main/assets/index.html`.
