package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual val appUpdateSupported: Boolean = false

private object NoOpAppUpdateChecker : AppUpdateChecker {
    override suspend fun checkForUpdate(): AppUpdateInfo? = null
    override suspend fun download(update: AppUpdateInfo, onProgress: (DownloadProgress) -> Unit): String =
        error("iOS updates via the App Store, not self-update")
    override suspend fun installAndExit(filePath: String) = Unit
}

@Composable
actual fun rememberAppUpdateChecker(): AppUpdateChecker = remember { NoOpAppUpdateChecker }
