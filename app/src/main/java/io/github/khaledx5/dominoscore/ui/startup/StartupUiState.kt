package io.github.khaledx5.dominoscore.ui.startup

/** Where app startup stands, from the launcher tap until the first screen can be shown. */
enum class StartupUiState {
    /** The loading screen plays its entrance while the database answers its first read. */
    Loading,

    /** Both are done: the bar fills and says ready for a short moment. */
    Finishing,

    /** The screens can be shown; the loading screen fades out over them. */
    Ready,
}
