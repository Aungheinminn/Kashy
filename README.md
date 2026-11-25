# Kashy
Android app to track cash in and cash out

## Stack

Kotlin, Jetpack Compose (Material 3), Room. All data is stored on the device in a local SQLite database (`kashy.db`); there is no server.

## Building

No local Android toolchain is required.

- **CI** (`.github/workflows/ci.yml`) runs the unit tests and builds a debug APK on every push to `main` and on every pull request. The APK is uploaded as the `kashy-debug-apk` artifact on the run.
- **Release** (`.github/workflows/release.yml`) runs when a tag like `v0.2.0` is pushed. It builds `Kashy-0.2.0-android.apk` and publishes a GitHub Release with it. Running it by hand from the Actions tab builds the APK without publishing.

```sh
git tag v0.2.0
git push origin v0.2.0
```

Debug and release builds are both signed with the committed `app/debug.keystore`, so newer builds install over older ones without losing data.
