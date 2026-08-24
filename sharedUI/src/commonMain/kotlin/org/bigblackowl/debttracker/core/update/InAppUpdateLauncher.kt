package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play's own in-app update flow (flexible: Play downloads the update in the background,
 * the app just prompts the user to restart once it's ready) — distinct from [AppUpdateChecker],
 * which is the GitHub-Releases-based self-update used on Desktop. Only meaningful for a build
 * installed via Play Store, so Android-only; a no-op everywhere else (see [inAppUpdateSupported]).
 */
interface InAppUpdateLauncher {
    /** true once a flexible update has finished downloading and is ready to install via [completeUpdate]. */
    val updateReadyToInstall: StateFlow<Boolean>

    /** Coarse progress of the last [checkForUpdate] call, so Settings can show *why* nothing happened yet. */
    val updateStatus: StateFlow<InAppUpdateStatus>

    /**
     * Checks Play for an update and, if one exists, starts downloading it in the background (or
     * resumes an interrupted install). Safe to call repeatedly — a no-op when nothing changed.
     */
    fun checkForUpdate()

    /** Installs an update already downloaded ([updateReadyToInstall] must be true) and restarts the app. */
    fun completeUpdate()
}

/** Mirrors [org.bigblackowl.debttracker.ui.screens.settings.UpdateCheckState] for the Play-driven flow, which has no "available but not downloading yet" step — a flexible update starts downloading as soon as it's found. */
sealed interface InAppUpdateStatus {
    data object Idle : InAppUpdateStatus
    data object Checking : InAppUpdateStatus
    data object UpToDate : InAppUpdateStatus
    /** requestAppUpdateInfo() itself failed (offline, Play Services unavailable) — distinct from [UpToDate]. */
    data object CheckFailed : InAppUpdateStatus
    data object Downloading : InAppUpdateStatus
    data object DownloadFailed : InAppUpdateStatus
}

/** true only on Android, where [rememberInAppUpdateLauncher] does real work. */
expect val inAppUpdateSupported: Boolean

@Composable
expect fun rememberInAppUpdateLauncher(): InAppUpdateLauncher

internal object NoopInAppUpdateLauncher : InAppUpdateLauncher {
    override val updateReadyToInstall: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val updateStatus: StateFlow<InAppUpdateStatus> = MutableStateFlow<InAppUpdateStatus>(InAppUpdateStatus.Idle).asStateFlow()
    override fun checkForUpdate() = Unit
    override fun completeUpdate() = Unit
}
