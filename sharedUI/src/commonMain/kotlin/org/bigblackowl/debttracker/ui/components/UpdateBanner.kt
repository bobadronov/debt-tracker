package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.update.AppUpdateInfo
import org.bigblackowl.debttracker.core.update.appUpdateSupported
import org.bigblackowl.debttracker.core.update.rememberAppUpdateChecker
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors

private sealed interface UpdateBannerState {
    data object Hidden : UpdateBannerState
    data class Available(val info: AppUpdateInfo) : UpdateBannerState
    data class Downloading(val info: AppUpdateInfo, val progress: Float?) : UpdateBannerState
    data class Failed(val info: AppUpdateInfo) : UpdateBannerState
}

/**
 * Self-update banner (option (a) from the OTA discussion): silently checks GitHub Releases once
 * on app start, and — if a newer build exists — offers to download its installer and relaunch
 * into it, closing this process so the installer can replace the running app's files. No-op
 * everywhere except Desktop ([appUpdateSupported]).
 */
@Composable
fun BoxScope.UpdateBanner() {
    if (!appUpdateSupported) return
    val updateChecker = rememberAppUpdateChecker()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateBannerState>(UpdateBannerState.Hidden) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val update = updateChecker.checkForUpdate() ?: return@LaunchedEffect
        state = UpdateBannerState.Available(update)
    }

    val visibleState = state
    AnimatedVisibility(
        visible = visibleState != UpdateBannerState.Hidden && !dismissed, modifier = Modifier.align(
            Alignment.BottomCenter
        )
    ) {
        UpdateBannerCard(
            state = visibleState,
            onDismiss = { dismissed = true },
            onDownload = { info ->
                scope.launch {
                    state = UpdateBannerState.Downloading(info, null)
                    runCatching {
                        updateChecker.download(info) { progress ->
                            state = UpdateBannerState.Downloading(info, progress)
                        }
                    }.onSuccess { path ->
                        updateChecker.installAndExit(path)
                    }.onFailure {
                        state = UpdateBannerState.Failed(info)
                    }
                }
            },
            onRetry = { info -> state = UpdateBannerState.Available(info) },
        )
    }
}

/** The card's visuals, pulled out of [UpdateBanner] so @Preview can render each [UpdateBannerState] without a real [rememberAppUpdateChecker]. */
@Composable
private fun UpdateBannerCard(
    state: UpdateBannerState,
    onDismiss: () -> Unit,
    onDownload: (AppUpdateInfo) -> Unit,
    onRetry: (AppUpdateInfo) -> Unit,
) {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier.fillMaxWidth().padding(Dimens.space16),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.space8),
        ) {
            Column(modifier = Modifier.padding(Dimens.space16)) {
                when (state) {
                    is UpdateBannerState.Hidden -> Unit

                    is UpdateBannerState.Available -> {
                        Text(
                            strings.updateAvailableTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(Dimens.space4))
                        Text(
                            strings.updateAvailableMessage(state.info.version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Dimens.space8))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = onDismiss) { Text(strings.updateLater) }
                            TextButton(onClick = { onDownload(state.info) }) { Text(strings.updateDownloadInstall) }
                        }
                    }

                    is UpdateBannerState.Downloading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))
                            Spacer(Modifier.width(Dimens.space12))
                            Text(
                                strings.updateDownloading,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (state.progress != null) {
                            Spacer(Modifier.height(Dimens.space8))
                            LinearWavyProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    is UpdateBannerState.Failed -> {
                        Text(
                            strings.updateFailed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.debtAccentColors.debt,
                        )
                        Spacer(Modifier.height(Dimens.space8))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) { Text(strings.updateLater) }
                            TextButton(onClick = { onRetry(state.info) }) { Text(strings.updateRetry) }
                        }
                    }
                }
            }
        }
    }
}

private val previewUpdateInfo = AppUpdateInfo(
    version = "1.2.0",
    downloadUrl = "https://example.com/debt-tracker-1.2.0.msi",
    releaseUrl = "https://example.com/releases/1.2.0",
)

@Preview
@Composable
private fun UpdateBannerLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    UpdateBannerCard(
        UpdateBannerState.Available(previewUpdateInfo),
        onDismiss = {},
        onDownload = {},
        onRetry = {})
}

@Preview
@Composable
private fun UpdateBannerDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    UpdateBannerCard(
        UpdateBannerState.Available(previewUpdateInfo),
        onDismiss = {},
        onDownload = {},
        onRetry = {})
}

@Preview(device = DESKTOP)
@Composable
private fun UpdateBannerLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    UpdateBannerCard(
        UpdateBannerState.Available(previewUpdateInfo),
        onDismiss = {},
        onDownload = {},
        onRetry = {})
}

@Preview(device = DESKTOP)
@Composable
private fun UpdateBannerDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    UpdateBannerCard(
        UpdateBannerState.Available(previewUpdateInfo),
        onDismiss = {},
        onDownload = {},
        onRetry = {})
}
