# Stapik Media (Android)

A read-only mobile companion to [Stapik Media](https://github.com/Stapik-Group/stapik-media),
the desktop media tracker. Kotlin + Jetpack Compose, same retro aesthetic and
Stapik Cloud sync.

## What this is

Categories (Movies, Series, Cartoons, Books, Albums, Games) are side screens,
swiped between via a `HorizontalPager` — the same idea as swiping between
weekdays in the planner companion, applied to categories instead of days.
Each category screen is a plain infinite-scroll list, filterable by
consumption year and, once a year is picked, month.

Read-only, same as the other two companions: no add/edit/delete, no
notifications, no home screen widget.

## Status: initial scaffold, not yet built/run

This is a first pass generated from the desktop app's actual source
(`src/core/model`, `MediaEntrySerializer.cpp`, `Storage.cpp`, and the three
`style-*.css` files on `main`), not from scratch — but it has **not been
compiled or run** in an actual Android environment yet. Known gaps before
this is real:

- [ ] Build and run in Android Studio, fix whatever doesn't compile
  (dependency versions in `app/build.gradle.kts` are current as of
  this writing but unverified against each other)
- [ ] `RetroBevel.kt`'s Classic bevel border is a simplified placeholder
  (same border weight on all sides) — the desktop's raised-bevel look
  is asymmetric (light top/left, dark bottom/right); needs a proper
  `drawBehind`/`Canvas` implementation, ideally ported from whatever
  stapikcalendar-android's `retroBevel` actually does under the hood
- [ ] No offline-cache "stale data" banner yet (calendar's Etap B has one;
  `MediaUiState.Success.isStale` is already plumbed through, just not
  surfaced in the UI)
- [ ] No pull-to-refresh
- [ ] Theme switching UI (Settings menu) not built — `AppTheme.CLASSIC` is
  hardcoded in `MainActivity`; no persistence of the user's choice
- [ ] Gradle wrapper files (`gradlew`, `gradle/wrapper/*`) not included —
  generate with `gradle wrapper` once opened in Android Studio
- [ ] Launcher icon is a placeholder reference (`@mipmap/ic_launcher`) —
  no actual icon asset included
- [ ] No automated tests

## Requirements

- Android 8.0 (API 26) or newer
- A running [Stapik Cloud](https://github.com/Stapik-Group/stapik-cloud)
  instance, same one stapik-media itself syncs to
