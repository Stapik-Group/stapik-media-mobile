package pl.stapik.media.ui.root

sealed interface AppScreen {
    data object Media : AppScreen
    data object Connect : AppScreen
    data object Theme : AppScreen
    data object About : AppScreen
}