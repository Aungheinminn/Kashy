# Kashy
Android app to track cash in and cash out

## Stack

Kotlin, Jetpack Compose (Material 3), Room. All data is stored on the device in a local SQLite database (`kashy.db`); there is no server. The only network use is the optional Google Drive upload in Settings.

## Building

No local Android toolchain is required.

- **CI** (`.github/workflows/ci.yml`) runs the unit tests and builds a debug APK on every push to `main` and on every pull request. The APK is uploaded as the `kashy-debug-apk` artifact on the run.
- **Release** (`.github/workflows/release.yml`) runs when a tag like `v0.2.0` is pushed. It builds `Kashy-0.2.0-android.apk` and publishes a GitHub Release with it. Running it by hand from the Actions tab builds the APK without publishing.

```sh
git tag v0.2.0
git push origin v0.2.0
```

Debug and release builds are both signed with the committed `app/debug.keystore`, so newer builds install over older ones without losing data.

## Google Drive setup

Uploading to Drive uses the `drive.file` scope, so Kashy only sees the files it creates. Google only hands out tokens to an app registered in a Cloud project:

1. In the [Google Cloud console](https://console.cloud.google.com/), create a project and enable the **Google Drive API**.
2. Set up the **OAuth consent screen** (External). While it is in Testing, add each Google account that should be able to connect as a test user.
3. Create an **OAuth client ID** of type **Android** with package name `com.aungheinmin.kashy` and SHA-1 `1C:36:BE:68:16:F1:76:31:F5:0F:DE:F6:5F:82:83:8A:05:53:19:31` (the committed `app/debug.keystore`).

No client ID goes into the app; Google matches the package name and signing key.
