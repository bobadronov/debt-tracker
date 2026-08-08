package org.bigblackowl.debttracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.launch
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.update.AppUpdateInfo
import org.bigblackowl.debttracker.core.update.appUpdateSupported
import org.bigblackowl.debttracker.core.update.rememberAppUpdateChecker
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
fun UpdateBanner() {
    if (!appUpdateSupported) return

    val strings = LocalStrings.current
    val updateChecker = rememberAppUpdateChecker()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateBannerState>(UpdateBannerState.Hidden) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val update = updateChecker.checkForUpdate() ?: return@LaunchedEffect
        state = UpdateBannerState.Available(update)
    }

    val visibleState = state
    AnimatedVisibility(visible = visibleState != UpdateBannerState.Hidden && !dismissed) {
        Box(modifier = Modifier.fillMaxWidth().padding(Dimens.space16), contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.space8),
            ) {
                Column(modifier = Modifier.padding(Dimens.space16)) {
                    when (val s = visibleState) {
                        is UpdateBannerState.Hidden -> Unit

                        is UpdateBannerState.Available -> {
                            Text(strings.updateAvailableTitle, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(Dimens.space4))
                            Text(
                                strings.updateAvailableMessage(s.info.version),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Dimens.space8))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { dismissed = true }) { Text(strings.updateLater) }
                                TextButton(onClick = {
                                    scope.launch {
                                        state = UpdateBannerState.Downloading(s.info, null)
                                        runCatching {
                                            updateChecker.download(s.info) { progress ->
                                                state = UpdateBannerState.Downloading(s.info, progress)
                                            }
                                        }.onSuccess { path ->
                                            updateChecker.installAndExit(path)
                                        }.onFailure {
                                            state = UpdateBannerState.Failed(s.info)
                                        }
                                    }
                                }) { Text(strings.updateDownloadInstall) }
                            }
                        }

                        is UpdateBannerState.Downloading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))
                                Spacer(Modifier.width(Dimens.space12))
                                Text(strings.updateDownloading, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (s.progress != null) {
                                Spacer(Modifier.height(Dimens.space8))
                                LinearWavyProgressIndicator(
                                    progress = { s.progress },
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
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { dismissed = true }) { Text(strings.updateLater) }
                                TextButton(onClick = { state = UpdateBannerState.Available(s.info) }) {
                                    Text(strings.updateRetry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
