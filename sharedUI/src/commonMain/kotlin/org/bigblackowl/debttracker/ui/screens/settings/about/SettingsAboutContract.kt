package org.bigblackowl.debttracker.ui.screens.settings.about

import org.bigblackowl.debttracker.core.update.AppUpdateChecker
import org.bigblackowl.debttracker.core.update.AppUpdateInfo
import org.bigblackowl.debttracker.core.update.InAppUpdateLauncher

data class SettingsAboutState(
    val updateState: UpdateCheckState = UpdateCheckState.Idle,
)

/** Settings → About's own on-demand update check — independent of [org.bigblackowl.debttracker.ui.components.UpdateBanner]'s automatic on-launch check. */
sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    /** The check itself failed (offline, GitHub unreachable/rate-limited) — distinct from [UpToDate] so it isn't misreported as "no update". */
    data object CheckFailed : UpdateCheckState
    data class Available(val info: AppUpdateInfo) : UpdateCheckState
    data class Downloading(val info: AppUpdateInfo) : UpdateCheckState
    data class Failed(val info: AppUpdateInfo) : UpdateCheckState
}

sealed interface SettingsAboutIntent {
    data class CheckForInAppUpdate(val launcher: InAppUpdateLauncher) : SettingsAboutIntent
    data class CheckForUpdate(val checker: AppUpdateChecker) : SettingsAboutIntent
    data class DownloadUpdate(val checker: AppUpdateChecker, val info: AppUpdateInfo) : SettingsAboutIntent
}
