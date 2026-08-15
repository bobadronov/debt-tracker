package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable

/** A newer release found on GitHub — version string without the leading "v", plus where to get it. */
data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseUrl: String,
)

/** [totalBytes]/[bytesPerSecond] are null until known (e.g. no Content-Length header, or too early to average). */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val bytesPerSecond: Long?,
) {
    val fraction: Float? get() = totalBytes?.takeIf { it > 0 }?.let { bytesDownloaded.toFloat() / it }
}

/**
 * Checks GitHub Releases for a build newer than the one currently running, downloads its
 * installer, and launches it. Desktop-only (option (a) from the OTA discussion): Android/iOS
 * update via their app stores, and Web is always served at the latest build, so neither needs
 * this — see [appUpdateSupported].
 */
interface AppUpdateChecker {
    /**
     * null if already up to date or this platform/OS has no matching release asset. Throws on a
     * genuine check failure (offline, GitHub unreachable/rate-limited, bad response) — callers
     * must catch that themselves and surface it, since it's not the same as "no update".
     */
    suspend fun checkForUpdate(): AppUpdateInfo?

    /** Downloads [update]'s installer to a temp file, reporting [DownloadProgress] as it goes, and returns its path. */
    suspend fun download(update: AppUpdateInfo, onProgress: (DownloadProgress) -> Unit): String

    /**
     * Runs the installer at [filePath] and, on success, exits this process so it can replace this
     * app's files — the call never returns in that case. Throws instead of exiting if the install
     * itself fails, so callers can surface that rather than being left on a stuck "Downloading…" state.
     */
    suspend fun installAndExit(filePath: String)
}

/** true only on Desktop, where [rememberAppUpdateChecker] does real work — a no-op everywhere else. */
expect val appUpdateSupported: Boolean

@Composable
expect fun rememberAppUpdateChecker(): AppUpdateChecker
