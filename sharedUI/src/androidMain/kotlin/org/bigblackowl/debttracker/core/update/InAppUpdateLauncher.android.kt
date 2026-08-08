package org.bigblackowl.debttracker.core.update

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

actual val inAppUpdateSupported: Boolean = true

/** Flexible updates download silently in the background; the caller just needs to know when [completeUpdate] becomes safe. */
private class PlayInAppUpdateLauncher(
    private val appUpdateManager: AppUpdateManager,
    private val updateFlowLauncher: ActivityResultLauncher<IntentSenderRequest>,
    private val scope: CoroutineScope,
) : InAppUpdateLauncher {

    private val _updateReadyToInstall = MutableStateFlow(false)
    override val updateReadyToInstall: StateFlow<Boolean> = _updateReadyToInstall.asStateFlow()

    init {
        appUpdateManager.registerListener(InstallStateUpdatedListener { state ->
            _updateReadyToInstall.value = state.installStatus() == InstallStatus.DOWNLOADED
        })
    }

    override fun checkForUpdate() {
        scope.launch {
            val info = runCatching { appUpdateManager.requestAppUpdateInfo() }.getOrNull() ?: return@launch
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> _updateReadyToInstall.value = true

                // An immediate update (resuming here means one was already in flight, e.g. app got backgrounded mid-update) — resume it.
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info, updateFlowLauncher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        )
                    }

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info, updateFlowLauncher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }
            }
        }
    }

    override fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
actual fun rememberInAppUpdateLauncher(): InAppUpdateLauncher {
    val activity = LocalContext.current as? ComponentActivity
    val scope = rememberCoroutineScope()

    // Always registered (Compose composable-call-order rule), even when there's no Activity
    // (e.g. @Preview rendering) — just unused in that case.
    val updateFlowLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    return remember(activity, updateFlowLauncher) {
        activity?.let { PlayInAppUpdateLauncher(AppUpdateManagerFactory.create(it), updateFlowLauncher, scope) }
            ?: NoopInAppUpdateLauncher
    }
}
