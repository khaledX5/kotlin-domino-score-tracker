package io.github.khaledx5.dominoscore.ui.startup

/** Where app startup stands, from the launcher tap until the first screen can be shown. */
enum class StartupUiState {
    /** The system splash still covers the window. */
    SystemSplash,

    /** The database answered its first read, so the screens can be shown. */
    Ready,
}
