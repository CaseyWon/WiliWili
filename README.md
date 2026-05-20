# BiliMini

Personal-use Android MVP for browsing Bilibili with:

- embedded web sign-in
- feed browsing
- keyword search
- video detail pages
- embedded web playback
- basic profile lookup

## Tech Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- OkHttp
- kotlinx.serialization
- EncryptedSharedPreferences
- WebView

## Implemented

- `Feed`: uses Bilibili web-facing popular feed data
- `Search`: uses Bilibili web-facing video search data
- `Detail`: uses Bilibili web-facing video detail data
- `Sign in`: embedded `passport.bilibili.com` login page
- `Session storage`: saves cookie header securely
- `Playback`: opens the mobile Bilibili video page inside WebView
- `Profile`: loads basic user profile info from the signed-in session

## Not Implemented Yet

- comments
- danmaku rendering in a native player
- favorites management
- watch history sync UI
- downloads/offline cache
- messages
- live
- posting/uploading
- release signing

## Login Strategy

This build does not ask the user to type their Bilibili password into a custom native form.

Instead, it opens the official Bilibili login webpage in WebView, then captures the resulting
authenticated cookies after a successful sign-in flow. Those cookies are stored with
`EncryptedSharedPreferences` and reused for subsequent authenticated requests and WebView pages.

This is a safer MVP tradeoff than implementing direct credential handling in the app.

## Data Source Strategy

This MVP uses web-facing Bilibili endpoints/pages for:

- feed
- search
- video details
- current user profile

Playback uses the mobile Bilibili video page in WebView instead of a native extracted media stream.

## Known Risks

- Bilibili does not provide a clearly supported public Android-client API for a custom personal app
  matching this scope, so web-facing endpoints may change or break without notice.
- Embedded sign-in and cookie reuse can still be affected by account risk controls or future login
  flow changes.
- Playback is currently WebView-based, which is more robust for an MVP but less native than a
  dedicated Media3 player implementation.
- The generated APK is a debug APK for testing, not a release-signed store build.

## Build

From the project root:

```powershell
.\gradlew.bat assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Local Environment Notes

The project is configured against the Android SDK already available on this machine:

- platform: Android 36.1
- build tools: 36.1.0
- Java runtime: Android Studio JBR 21
