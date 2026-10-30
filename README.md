# Stapik Media (Android)

A mobile edition of [Stapik Media](https://github.com/Stapik-Group/stapik-media), the desktop media consumption tracker. Browse, add, edit and delete entries on the phone, with or without a server. Written in Kotlin with Jetpack Compose, styled after the same retro old-school aesthetic as the desktop version and its sibling apps.

## Features

### Browsing

- **Category side screens** – swipe horizontally between six categories (Movies, Series, Cartoons, Books, Albums, Games), with left/right arrows on the tab bar as a swipe-more affordance
- **Entry details** – tap an entry to open its full details (director/author/performer/studio, genre, release date, platform, audiobook flag, rating and note where relevant); system back or an edge swipe returns to the list
- **Search, filters and sorting** – title search, a consumption year and month filter, and sorting by title, release date or consumption date; the chosen order is remembered
- **Yearly archives** – entries consumed in earlier years live in separate cloud archives, as on the desktop; they are downloaded when you pick the year in the filter

### Editing

- **Add, edit, delete** – a form per category with the release date at day, month or year precision, the month of consumption, a rating from 1 to 10 and a note; deleting offers an undo
- **Desktop rules** – the same duplicate rule (category, release year and title ignoring case), the same year ranges and the same refusal to write into a cloud archive that has not been downloaded
- **Nothing is lost on the way** – unknown fields of an entry and entries this version cannot read are written back untouched
- **Works without a server** – with no connection the list is kept and edited on the device only; on the first connection you choose between using the cloud data and sending the local list
- **Reliable sync** – every change is stored on the device first and sent to the cloud in the background, retried when the network returns

### Backups and exchange

- **Backup file** – export the whole list to a `stapik-archive` ZIP and restore from one; the format is the desktop's, so a backup travels between phone and desktop in both directions
- **Safety copies** – before every restore or CSV import the current list is saved as an encrypted safety copy (the last five are kept) and can be restored from Settings
- **CSV** – export and import with the same columns as the desktop; the import shows a preview with new entries, duplicates, invalid rows and entries of archives that are not downloaded

### Sync and security

- **Cloud sync** – synchronises with the Stapik Cloud compatible self-hosted API through the shared sync engine of [stapik-common-mobile](https://github.com/Stapik-Group/stapik-common-mobile); when both the device and the cloud changed a document, the cloud version wins and the local one is kept as a conflict copy
- **Read-only keys** – if the API key only allows reading, editing is switched off
- **Offline copy** – the last synchronised list is kept on the device, encrypted with a key from the Android Keystore, with a banner showing when you are looking at stored data and from when
- **Sync status** – a banner explains why the data may be out of date (no network, rejected key, unreadable cloud data) and offers the matching action
- **Recovery** – if the copy on the device cannot be read, the app offers to discard it and download the list from the cloud again
- **Refresh** – the stored copy appears at once while the latest data is fetched; drag down to refresh on demand, and the list also refreshes when you return to the app after a minute
- **Encrypted connection config** – server URL and API key are encrypted with a key held in the Android Keystore; the API key is masked on screen with a show/hide toggle
- **End-to-end encryption** – an optional passphrase reads and writes cloud data encrypted the way the desktop app does it (`stapik-e2e`)
- **Not in Android backups** – the data directory is excluded from Android's cloud backup and device transfer; use the backup file instead

### Look and language

- **Themes** – Classic, Classic Pink, Modern, Dark and Neoclassic, matching the desktop app, applied consistently across every screen
- **Multilingual UI** – Polish, English and German, following the phone's system language; dates follow the device locale

### What's intentionally not here

This app has **no notifications** and **no home screen widget** — a deliberate scope decision for the media app. There is no PDF export, and the conflict copies kept by the sync engine are not browsable on the phone.

## Data model

- The main document holds the entries consumed in the current year or later; each earlier year is a separate document stored in the cloud as a partition named after the year (`2024`) and on the device as `media.json@2024.doc`.
- An entry has a 16-character id, a category, a title, a release date with precision, the month it was consumed, category-specific details, an optional rating and a note.
- The documents on the device are encrypted with a key from the Android Keystore and live in the app's private data directory.

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
