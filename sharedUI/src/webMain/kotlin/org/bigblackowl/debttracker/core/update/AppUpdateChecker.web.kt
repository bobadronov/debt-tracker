package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual val appUpdateSupported: Boolean = false

private object NoOpAppUpdateChecker : AppUpdateChecker {
    override suspend fun checkForUpdate(): AppUpdateInfo? = null
    override suspend fun download(update: AppUpdateInfo, onProgress: (Float?) -> Unit): String =
        error("Web is always served at the latest build, not self-update")
    override fun installAndExit(filePath: String) = Unit
}

@Composable
actual fun rememberAppUpdateChecker(): AppUpdateChecker = remember { NoOpAppUpdateChecker }
