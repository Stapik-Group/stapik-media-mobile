# Stapik Media (Android)

A read-only mobile companion to [Stapik Media](https://github.com/Stapik-Group/stapik-media), the desktop media consumption tracker. Written in Kotlin with Jetpack Compose, styled after the same retro old-school aesthetic as the desktop version and its sibling apps.

## Features

- **Category side screens** – swipe horizontally between six categories (Movies, Series, Cartoons, Books, Albums, Games), with left/right arrows on the tab bar as a swipe-more affordance
- **Entry details** – tap an entry to open its full details (director/author/performer/studio, genre, release date, platform, audiobook flag where relevant); system back or an edge swipe returns to the list
- **Year/month filtering** – narrow each category down to a consumption year, and optionally a specific month within it
- **Cloud sync (read-only)** – fetches the media log from the Stapik Cloud compatible self-hosted API
- **Offline cache** – automatically caches the last retrieved list, with a banner showing when you're looking at cached data and from when
- **Pull-to-refresh** – drag down to fetch the latest data on demand without losing scroll position
- **Themes** – Classic, Modern and Classic Pink, matching the desktop app, applied consistently across every screen
- **Encrypted connection config** – server URL and API key are encrypted with a key held in the Android Keystore before being stored on device; the API key is masked on screen with a show/hide toggle
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