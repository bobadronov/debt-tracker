package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.BuildConfig
import java.io.File
import kotlin.system.exitProcess

actual val appUpdateSupported: Boolean = true

/** Same repo release.bat tags (`vX.Y.Z`) and release.yml publishes to. */
private const val REPO = "bobadronov/debt-tracker"

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

private class DesktopAppUpdateChecker : AppUpdateChecker {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    override suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val assetExtension = currentAssetExtension() ?: return@withContext null

        // Deliberately not caught here — a network/GitHub failure must not look identical to
        // "no update available" (see AppUpdateChecker's KDoc); callers catch and surface it.
        val release = client.get("https://api.github.com/repos/$REPO/releases/latest") {
            header("Accept", "application/vnd.github+json")
        }.body<GitHubRelease>()

        val latestVersion = release.tagName.removePrefix("v")
        if (!isNewerVersion(latestVersion, BuildConfig.APP_VERSION)) return@withContext null

        val asset = release.assets.firstOrNull { it.name.endsWith(assetExtension) }
            ?: return@withContext null

        AppUpdateInfo(version = latestVersion, downloadUrl = asset.browserDownloadUrl, releaseUrl = release.htmlUrl)
    }

    override suspend fun download(update: AppUpdateInfo, onProgress: (Float?) -> Unit): String =
        withContext(Dispatchers.IO) {
            val target = File.createTempFile("debt-tracker-update-", currentAssetExtension())
            client.prepareGet(update.downloadUrl) {
                onDownload { sentBytes, contentLength ->
                    onProgress(if (contentLength != null && contentLength > 0) sentBytes.toFloat() / contentLength else null)
                }
            }.execute { response ->
                target.outputStream().use { out -> response.bodyAsChannel().copyTo(out) }
            }
            target.absolutePath
        }

    override suspend fun installAndExit(filePath: String): Unit = withContext(Dispatchers.IO) {
        // Captured before running the installer: for a jpackage-installed app this is the native
        // launcher's own exe, at the same path the (in-place) upgrade just wrote back to.
        val relaunchCommand = ProcessHandle.current().info().command().orElse(null)
        runInstallerSilently(File(filePath))
        relaunchCommand?.let { runCatching { ProcessBuilder(it).start() } }
        exitProcess(0)
    }

    /** Runs the platform installer with no wizard UI and waits for it to finish; throws if it fails. */
    private fun runInstallerSilently(installer: File) {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("win") -> listOf("msiexec", "/i", installer.absolutePath, "/passive", "/norestart")
            os.contains("mac") -> error("No macOS release exists to install")
            else -> listOf("pkexec", "dpkg", "-i", installer.absolutePath)
        }
        val exitCode = ProcessBuilder(command).start().waitFor()
        // 3010 = success, reboot required — shouldn't happen with /norestart, but treat it as success too.
        check(exitCode == 0 || exitCode == 3010) { "Installer exited with code $exitCode" }
    }
}

@Composable
actual fun rememberAppUpdateChecker(): AppUpdateChecker = remember { DesktopAppUpdateChecker() }

/** Matches release.yml's per-OS packaging jobs — no macOS release job exists today. */
private fun currentAssetExtension(): String? {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> ".msi"
        os.contains("mac") -> null
        else -> ".deb"
    }
}

/** Both versions are plain "major.minor.patch" (see version.properties) — no pre-release qualifiers to handle. */
private fun isNewerVersion(remote: String, local: String): Boolean {
    val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
    val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
        val r = remoteParts.getOrElse(i) { 0 }
        val l = localParts.getOrElse(i) { 0 }
        if (r != l) return r > l
    }
    return false
}
