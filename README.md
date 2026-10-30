# Stapik Media (Android)

A read-only mobile companion to [Stapik Media](https://github.com/Stapik-Group/stapik-media), the desktop media consumption tracker. Written in Kotlin with Jetpack Compose, styled after the same retro old-school aesthetic as the desktop version and its sibling apps.

## Features

- **Category side screens** – swipe horizontally between six categories (Movies, Series, Cartoons, Books, Albums, Games), with left/right arrows on the tab bar as a swipe-more affordance
- **Entry details** – tap an entry to open its full details (director/author/performer/studio, genre, release date, platform, audiobook flag where relevant); system back or an edge swipe returns to the list
- **Year/month filtering** – narrow each category down to a consumption year, and optionally a specific month within it
- **Cloud sync (read-only)** – fetches the media log from the Stapik Cloud compatible self-hosted API through the shared sync engine of [stapik-common-mobile](https://github.com/Stapik-Group/stapik-common-mobile)
- **Offline copy** – the last synchronised log is kept on the device, encrypted with a key from the Android Keystore, with a banner showing when you're looking at stored data and from when
- **Sync status** – a banner explains why the data may be out of date (no network, rejected key, unreadable cloud data) and offers the matching action
- **Recovery** – if the copy on the device cannot be read, the app offers to discard it and download the log from the cloud again
- **Pull-to-refresh** – drag down to fetch the latest data on demand without losing scroll position
- **Themes** – Classic, Classic Pink, Modern, Dark and Neoclassic, matching the desktop app, applied consistently across every screen
- **Encrypted connection config** – server URL and API key are encrypted with a key held in the Android Keystore before being stored on device; the API key is masked on screen with a show/hide toggle
- **End-to-end encryption** – an optional passphrase reads cloud data that the desktop app encrypted (`stapik-e2e`)
- **Multilingual UI** – Polish, English and German, following the phone's system language
- **Retro aesthetic** – same raised-button, blue-navbar, grey-cell look as the desktop app

This app does not add, edit, or delete entries – all of that happens in the desktop app. It only reads and displays whatever is currently in the cloud.

### What's intentionally not here

Like other companion apps, this app has **no notifications** and **no home screen widget** — a deliberate scope decision for the media companion.

## Requirements

- Android 8.0 (API 26) or newer
- A running instance of the Stapik Cloud compatible sync API used by the desktop app (see [Stapik Media](https://github.com/Stapik-Group/stapik-media))

## Building

Requires [Android Studio](https://developer.android.com/studio) (Kotlin, Jetpack Compose).

The app uses the shared library [stapik-common-mobile](https://github.com/Stapik-Group/stapik-common-mobile) as a git submodule in `libs/stapik-common-mobile`, pinned to a release tag. Clone with `git clone --recurse-submodules`, or run `git submodule update --init` after a plain clone.

The library is pinned to `v0.3.1`. The release workflows in `.github/workflows` use the same tag, so change both together when updating.

The library is built together with the app, so it needs the Android SDK location too: either set `ANDROID_HOME`, or copy `local.properties` into `libs/stapik-common-mobile/`.

To work on the library next to the app, put its path in `~/.gradle/gradle.properties`:

```
stapikCommonMobileDir=/path/to/stapik-common-mobile
```

Build a debug APK with `./gradlew :app:assembleDebug`. Pull requests from `develop` to `master` are built and tested by GitHub Actions; a `v*` tag builds a signed release APK (secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
