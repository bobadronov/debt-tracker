package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Android updates via Play's in-app update flow (see InAppUpdateLauncher.android.kt /
// inAppUpdateSupported), not this GitHub-Releases self-download — a Play-installed APK can't
// sideload a self-downloaded update over itself anyway (Play Protect blocks it).
actual val appUpdateSupported: Boolean = false

private object NoOpAppUpdateChecker : AppUpdateChecker {
    override suspend fun checkForUpdate(): AppUpdateInfo? = null
    override suspend fun download(update: AppUpdateInfo, onProgress: (Float?) -> Unit): String =
        error("Android updates via the Play Store, not self-update")
    override suspend fun installAndExit(filePath: String) = Unit
}

@Composable
actual fun rememberAppUpdateChecker(): AppUpdateChecker = remember { NoOpAppUpdateChecker }
