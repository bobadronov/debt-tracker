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

    /**
     * Checks Play for an update and, if one exists, starts downloading it in the background (or
     * resumes an interrupted install). Safe to call repeatedly — a no-op when nothing changed.
     */
    fun checkForUpdate()

    /** Installs an update already downloaded ([updateReadyToInstall] must be true) and restarts the app. */
    fun completeUpdate()
}

/** true only on Android, where [rememberInAppUpdateLauncher] does real work. */
expect val inAppUpdateSupported: Boolean

@Composable
expect fun rememberInAppUpdateLauncher(): InAppUpdateLauncher

internal object NoopInAppUpdateLauncher : InAppUpdateLauncher {
    override val updateReadyToInstall: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override fun checkForUpdate() = Unit
    override fun completeUpdate() = Unit
}
