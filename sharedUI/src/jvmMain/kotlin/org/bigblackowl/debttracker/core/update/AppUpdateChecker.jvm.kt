package org.bigblackowl.debttracker.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

private const val TAG = "AppUpdateChecker"

private class DesktopAppUpdateChecker : AppUpdateChecker {

    private val client = HttpClient(OkHttp) {
        // GitHub's release JSON has dozens of fields beyond the ones GitHubRelease/GitHubAsset
        // declare — without this, the default Json() rejects every response as an unknown key.
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    override suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        Napier.d(tag = TAG) {
            "checkForUpdate: local version = ${BuildConfig.APP_VERSION} (code ${BuildConfig.APP_VERSION_CODE})"
        }
        val assetExtension = currentAssetExtension() ?: return@withContext null

        // Deliberately not caught here — a network/GitHub failure must not look identical to
        // "no update available" (see AppUpdateChecker's KDoc); callers catch and surface it.
        val release = try {
            client.get("https://api.github.com/repos/$REPO/releases/latest") {
                header("Accept", "application/vnd.github+json")
            }.body<GitHubRelease>()
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "checkForUpdate: GitHub request failed" }
            throw e
        }

        val latestVersion = release.tagName.removePrefix("v")
        // Compare VERSION_CODE, not the "vX.Y.Z" name: the code is a plain integer that's
        // bumped +1 on every release (see version.properties / publishing.bat), so ">" on it
        // is unambiguous, while name ordering breaks the moment a hotfix or scheme change
        // makes it non-monotonic. version.properties is the single source of truth and lives
        // at the repo root, so read it straight from the released tag's tree.
        val latestVersionCode = try {
            client.get("https://raw.githubusercontent.com/$REPO/${release.tagName}/version.properties")
                .bodyAsText()
                .let { VERSION_CODE_REGEX.find(it)?.groupValues?.get(1)?.toInt() }
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "checkForUpdate: version.properties fetch failed" }
            throw e
        }
        if (latestVersionCode == null) {
            Napier.w(tag = TAG) { "checkForUpdate: no VERSION_CODE in ${release.tagName}'s version.properties" }
            return@withContext null
        }
        Napier.d(tag = TAG) {
            "checkForUpdate: latest GitHub tag = ${release.tagName} -> $latestVersion (code $latestVersionCode)"
        }
        if (latestVersionCode <= BuildConfig.APP_VERSION_CODE) {
            Napier.d(tag = TAG) { "checkForUpdate: no newer version available" }
            return@withContext null
        }

        val asset = release.assets.firstOrNull { it.name.endsWith(assetExtension) }
        if (asset == null) {
            Napier.w(tag = TAG) { "checkForUpdate: $latestVersion is newer but has no *$assetExtension asset" }
            return@withContext null
        }

        Napier.i(tag = TAG) { "checkForUpdate: update found -> $latestVersion, asset=${asset.name}" }
        AppUpdateInfo(version = latestVersion, downloadUrl = asset.browserDownloadUrl, releaseUrl = release.htmlUrl)
    }

    override suspend fun download(update: AppUpdateInfo, onProgress: (DownloadProgress) -> Unit): String =
        withContext(Dispatchers.IO) {
            Napier.d(tag = TAG) { "download: starting from ${update.downloadUrl}" }
            // FileKit.cacheDir instead of File.createTempFile: it's an app-scoped, already-existing
            // directory (see FileKit.init() in desktopApp's main()), and the timestamp keeps this
            // unique the same way createTempFile's random suffix did.
            val fileName = "debt-tracker-update-${System.currentTimeMillis()}${currentAssetExtension() ?: ".tmp"}"
            val target = PlatformFile(FileKit.cacheDir, fileName).file
            // Overall average since the download started, not an instantaneous per-chunk rate —
            // onDownload fires for every small buffer, so an instant delta would be too jumpy to show.
            val startedAtNanos = System.nanoTime()
            try {
                client.prepareGet(update.downloadUrl) {
                    // The client-level 15s requestTimeoutMillis covers checkForUpdate()'s small JSON
                    // request, but Ktor's HttpTimeout applies it to the WHOLE request lifetime — an
                    // 80-120MB installer would blow past 15s on any but the fastest connection, so
                    // this request needs its own much longer budget.
                    timeout { requestTimeoutMillis = 5 * 60_000 }
                    onDownload { sentBytes, contentLength ->
                        val elapsedSeconds = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0
                        onProgress(
                            DownloadProgress(
                                bytesDownloaded = sentBytes,
                                totalBytes = contentLength?.takeIf { it > 0 },
                                bytesPerSecond = if (elapsedSeconds > 0) (sentBytes / elapsedSeconds).toLong() else null,
                            )
                        )
                    }
                }.execute { response ->
                    target.outputStream().use { out -> response.bodyAsChannel().copyTo(out) }
                }
            } catch (e: Exception) {
                Napier.e(tag = TAG, throwable = e) { "download: failed" }
                throw e
            }
            Napier.i(tag = TAG) { "download: finished -> ${target.absolutePath} (${target.length()} bytes)" }
            target.absolutePath
        }

    override suspend fun installAndExit(filePath: String): Unit = withContext(Dispatchers.IO) {
        Napier.i(tag = TAG) { "installAndExit: scheduling installer for $filePath" }
        // Captured before exiting: for a jpackage-installed app this is the native launcher's own
        // exe, at the same path the (in-place) upgrade will write back to.
        val relaunchCommand = ProcessHandle.current().info().command().orElse(null)
        val pid = ProcessHandle.current().pid()
        try {
            // msiexec/dpkg upgrade this app's own exe/DLLs in place, which stay locked while this
            // JVM is running — installing now (then exiting) would fail or corrupt the install.
            // Instead hand off to a detached helper that waits for this process to exit, THEN
            // installs, THEN relaunches — and exit immediately so the locks are released.
            spawnDetachedInstaller(File(filePath), pid, relaunchCommand)
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "installAndExit: failed to schedule installer" }
            throw e
        }
        Napier.i(tag = TAG) { "installAndExit: installer scheduled, exiting so it can replace locked files" }
        exitProcess(0)
    }

    /**
     * Starts a detached process (survives this JVM exiting) that waits for [waitForPid] to die,
     * runs the platform installer with no wizard UI, then relaunches [relaunchCommand] if given.
     */
    private fun spawnDetachedInstaller(installer: File, waitForPid: Long, relaunchCommand: String?) {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("win") -> listOf(
                "powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
                "Wait-Process -Id $waitForPid -ErrorAction SilentlyContinue; " +
                    "Start-Process msiexec.exe -ArgumentList '/i','${installer.absolutePath}','/passive','/norestart' -Wait; " +
                    (relaunchCommand?.let { "Start-Process '$it'" } ?: ""),
            )
            os.contains("mac") -> error("No macOS release exists to install")
            else -> listOf(
                "sh", "-c",
                "while kill -0 $waitForPid 2>/dev/null; do sleep 1; done; " +
                    "pkexec dpkg -i '${installer.absolutePath}'; " +
                    (relaunchCommand?.let { "'$it' &" } ?: ""),
            )
        }
        ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
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

/** Pulls `VERSION_CODE=<int>` out of the repo-root version.properties file. */
private val VERSION_CODE_REGEX = Regex("""^\s*VERSION_CODE\s*=\s*(\d+)""", RegexOption.MULTILINE)
