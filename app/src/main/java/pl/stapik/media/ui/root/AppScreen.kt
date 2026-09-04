package pl.stapik.media.ui.root

/** Manual sealed navigation, matching stapikcalendar-android (no Navigation Compose). */
sealed interface AppScreen {
    data object Media : AppScreen
    data object Connect : AppScreen
    data object About : AppScreen
}
