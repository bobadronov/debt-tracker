package org.bigblackowl.debttracker.ui.screens.settings

sealed interface SettingsIntent {
    data object SignOut : SettingsIntent
}
