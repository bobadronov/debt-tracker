package org.bigblackowl.debttracker.ui.screens.settings.data

data class SettingsDataState(
    val deleteDone: Boolean = false,
    val deleteError: Boolean = false,
    val cacheCleared: Boolean = false,
    val cacheClearError: Boolean = false,
)

sealed interface SettingsDataIntent {
    data object DeleteAllData : SettingsDataIntent
    data object ClearAppCache : SettingsDataIntent
}
