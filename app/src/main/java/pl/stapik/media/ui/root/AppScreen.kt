package pl.stapik.media.ui.root

sealed interface AppScreen {
    data object Media : AppScreen
    data object Settings : AppScreen
}
