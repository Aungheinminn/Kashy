# Kashy
Android app to track cash in and cash out

## Stack

Kotlin, Jetpack Compose (Material 3), Room. All data is stored on the device in a local SQLite database (`kashy.db`); there is no server.

## Building

No local Android toolchain is required. Every push to `main` (and every pull request) runs the **Build APK** GitHub Actions workflow, which uploads `kashy-debug-apk` as a build artifact. Download it from the workflow run, unzip, and install the APK on your phone.

Builds are signed with the committed `app/debug.keystore`, so newer builds install over older ones without losing data.
