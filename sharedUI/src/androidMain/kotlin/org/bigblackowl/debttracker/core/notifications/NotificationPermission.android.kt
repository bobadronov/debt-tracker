package org.bigblackowl.debttracker.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference

/**
 * Android 13+ (`POST_NOTIFICATIONS`) needs a runtime request through an `ActivityResultLauncher`.
 * The launcher is registered in composition; [request] rendezvous with its callback via a
 * [CompletableDeferred]. Pre-13 has no such permission — there the app-level notification switch
 * (`areNotificationsEnabled`) is the whole story.
 */
private class AndroidNotificationPermissionRequester(
    private val context: Context,
    private val launcher: ActivityResultLauncher<String>,
    private val pending: AtomicReference<CompletableDeferred<Boolean>?>,
) : NotificationPermissionRequester {

    override suspend fun request(): NotificationPermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationPermissionStatus.GRANTED
            } else {
                NotificationPermissionStatus.DENIED
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return NotificationPermissionStatus.GRANTED
        }
        val deferred = CompletableDeferred<Boolean>()
        // A prior in-flight request (user toggled twice fast) is abandoned rather than left hanging.
        pending.getAndSet(deferred)?.complete(false)
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return if (deferred.await()) NotificationPermissionStatus.GRANTED else NotificationPermissionStatus.DENIED
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester {
    val context = LocalContext.current
    val pending = remember { AtomicReference<CompletableDeferred<Boolean>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending.getAndSet(null)?.complete(granted)
    }
    return remember(context, launcher) {
        AndroidNotificationPermissionRequester(context, launcher, pending)
    }
}
