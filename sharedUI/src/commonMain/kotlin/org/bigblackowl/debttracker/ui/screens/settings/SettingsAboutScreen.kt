package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.i18n.resolveFeedbackStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.core.platform.currentPlatform
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.update.InAppUpdateStatus
import org.bigblackowl.debttracker.core.update.appUpdateSupported
import org.bigblackowl.debttracker.core.update.inAppUpdateSupported
import org.bigblackowl.debttracker.core.update.rememberAppUpdateChecker
import org.bigblackowl.debttracker.core.update.rememberInAppUpdateLauncher
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings → Про застосунок (версія/оновлення, автор, фідбек) — виокремлено з колишнього
 * єдиного SettingsScreen.
 */
@Composable
fun SettingsAboutScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val settings = koinInject<AppSettings>()
    val uriHandler = LocalUriHandler.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateChecker = rememberAppUpdateChecker()
    val inAppUpdateLauncher = rememberInAppUpdateLauncher()
    val inAppUpdateReady by inAppUpdateLauncher.updateReadyToInstall.collectAsStateWithLifecycle()
    val inAppUpdateStatus by inAppUpdateLauncher.updateStatus.collectAsStateWithLifecycle()

    PlaceholderScreen(title = strings.settings.about, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space24),
            ) {
                SettingsSection(strings.settings.about) {
                    val versionLine = "${BuildConfig.APP_VERSION}-${BuildConfig.APP_VERSION_CODE}"
                    val versionSubtitle = if (currentPlatform == AppPlatform.ANDROID) {
                        when {
                            inAppUpdateReady -> strings.updateReadyToInstall
                            inAppUpdateStatus == InAppUpdateStatus.Checking -> "$versionLine · ${strings.settings.checkingForUpdates}"
                            inAppUpdateStatus == InAppUpdateStatus.UpToDate -> "$versionLine · ${strings.settings.upToDate}"
                            inAppUpdateStatus == InAppUpdateStatus.CheckFailed -> "$versionLine · ${strings.update.failed}"
                            inAppUpdateStatus == InAppUpdateStatus.Downloading -> strings.update.downloading
                            inAppUpdateStatus == InAppUpdateStatus.DownloadFailed -> strings.update.failed
                            else -> versionLine
                        }
                    } else {
                        when (val s = state.updateState) {
                            UpdateCheckState.Idle -> versionLine
                            UpdateCheckState.Checking -> "$versionLine · ${strings.settings.checkingForUpdates}"
                            UpdateCheckState.UpToDate -> "$versionLine · ${strings.settings.upToDate}"
                            UpdateCheckState.CheckFailed -> "$versionLine · ${strings.update.failed}"
                            is UpdateCheckState.Available -> strings.update.availableMessage(s.info.version)
                            is UpdateCheckState.Downloading -> strings.update.downloading
                            is UpdateCheckState.Failed -> strings.update.failed
                        }
                    }
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        title = strings.settings.aboutVersion,
                        subtitle = versionSubtitle,
                        trailing = if (currentPlatform == AppPlatform.ANDROID && inAppUpdateSupported) {
                            {
                                when {
                                    inAppUpdateReady -> IconButton(onClick = { inAppUpdateLauncher.completeUpdate() }) {
                                        Icon(Icons.Filled.Download, contentDescription = strings.updateRestartNow)
                                    }

                                    inAppUpdateStatus == InAppUpdateStatus.Checking || inAppUpdateStatus == InAppUpdateStatus.Downloading ->
                                        CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))

                                    else -> IconButton(onClick = { viewModel.onIntent(SettingsIntent.CheckForInAppUpdate(inAppUpdateLauncher)) }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = strings.settings.checkForUpdates)
                                    }
                                }
                            }
                        } else if (appUpdateSupported) {
                            {
                                when (val s = state.updateState) {
                                    UpdateCheckState.Checking, is UpdateCheckState.Downloading ->
                                        CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))

                                    is UpdateCheckState.Available -> IconButton(onClick = { viewModel.onIntent(SettingsIntent.DownloadUpdate(updateChecker, s.info)) }) {
                                        Icon(Icons.Filled.Download, contentDescription = strings.update.downloadInstall)
                                    }

                                    is UpdateCheckState.Failed -> IconButton(onClick = { viewModel.onIntent(SettingsIntent.DownloadUpdate(updateChecker, s.info)) }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = strings.update.retry)
                                    }

                                    UpdateCheckState.Idle, UpdateCheckState.UpToDate, UpdateCheckState.CheckFailed -> IconButton(onClick = { viewModel.onIntent(SettingsIntent.CheckForUpdate(updateChecker)) }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = strings.settings.checkForUpdates)
                                    }
                                }
                            }
                        } else null,
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = strings.settings.aboutAuthor,
                        subtitle = BuildConfig.APP_AUTHOR,
                    )
                    SettingsRowDivider()
                    // Opens the web feedback form (legal/feedback.html on GitHub Pages) in a browser;
                    // it POSTs to the submit-feedback Edge Function, which emails the maintainer.
                    // Its label lives outside Strings — that constructor is at the JVM 255-param limit.
                    val feedbackStrings = remember(settings.locale) { resolveFeedbackStrings(settings.locale) }
                    SettingsRow(
                        icon = Icons.Filled.Feedback,
                        title = feedbackStrings.title,
                        subtitle = feedbackStrings.subtitle,
                        onClick = { uriHandler.openUri(feedbackUrl(settings.locale, settings.theme)) },
                    )
                }
            }
        }
    }
}

private const val FEEDBACK_BASE_URL = "https://bobadronov.github.io/debt-tracker/feedback.html"

/**
 * Feedback-form URL carrying the app version/build/platform as query params, plus — each only when
 * it isn't left to the OS — the UI language (`lang`) and the light/dark theme (`theme`), so the web
 * page opens matching the app.
 */
private fun feedbackUrl(locale: String, theme: String): String = buildString {
    append(FEEDBACK_BASE_URL)
    append("?v=").append(BuildConfig.APP_VERSION)
    append("&build=").append(BuildConfig.APP_VERSION_CODE)
    append("&platform=").append(currentPlatform.name.lowercase())
    if (locale != "system") append("&lang=").append(locale)
    if (theme == "light" || theme == "dark") append("&theme=").append(theme)
}
