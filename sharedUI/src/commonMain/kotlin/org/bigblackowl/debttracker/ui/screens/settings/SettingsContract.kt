package org.bigblackowl.debttracker.ui.screens.settings

import org.bigblackowl.debttracker.core.security.BiometricAuthenticator
import org.bigblackowl.debttracker.core.update.AppUpdateChecker
import org.bigblackowl.debttracker.core.update.AppUpdateInfo
import org.bigblackowl.debttracker.core.update.InAppUpdateLauncher

data class SettingsState(
    val biometricHardwareAvailable: Boolean = false,
    val protectionConfirmError: String? = null,
    val deleteDone: Boolean = false,
    val deleteError: Boolean = false,
    val isCheckingInAppUpdate: Boolean = false,
    val updateState: UpdateCheckState = UpdateCheckState.Idle,
)

/** Settings' own on-demand update check — independent of [org.bigblackowl.debttracker.ui.components.UpdateBanner]'s automatic on-launch check. */
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

sealed interface SettingsIntent {
    data class CheckBiometricHardware(val authenticator: BiometricAuthenticator) : SettingsIntent
    /** Toggles protection when it's PIN-backed — Desktop (no biometric at all) or mobile with no biometric hardware/enrollment. */
    data class TogglePinProtection(val enabled: Boolean) : SettingsIntent
    data class SetupPinAndEnableProtection(val pin: String) : SettingsIntent
    data class EnableMobileProtection(val authenticator: BiometricAuthenticator) : SettingsIntent
    data object DisableMobileProtection : SettingsIntent
    data object SignOut : SettingsIntent
    data object DeleteAllData : SettingsIntent
    data class CheckForInAppUpdate(val launcher: InAppUpdateLauncher) : SettingsIntent
    data class CheckForUpdate(val checker: AppUpdateChecker) : SettingsIntent
    data class DownloadUpdate(val checker: AppUpdateChecker, val info: AppUpdateInfo) : SettingsIntent
}
