package org.bigblackowl.debttracker.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.domain.model.DeviceSession
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.BackButton
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.viewmodel.koinViewModel

/** Settings → Active devices: session management + remote logout, reached from [SettingsScreen]'s account section. */
@Composable
fun ActiveSessionsScreen(
    onBack: () -> Unit,
    viewModel: ActiveSessionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRevoke by remember { mutableStateOf<DeviceSession?>(null) }
    var showRevokeAllConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ActiveSessionsEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.activeSessionsTitle) },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth).padding(Dimens.space16),
                verticalArrangement = Arrangement.spacedBy(Dimens.space16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isLoading) {
                    CircularWavyProgressIndicator(modifier = Modifier.padding(Dimens.space16))
                } else {
                    SettingsSection(strings.activeSessionsTitle) {
                        state.sessions.forEachIndexed { index, session ->
                            SettingsRow(
                                icon = session.platform.icon(),
                                title = session.deviceName,
                                subtitle = if (session.isCurrentDevice) {
                                    strings.activeSessionsCurrentDevice
                                } else {
                                    strings.activeSessionsLastActive(
                                        session.lastSeenAt.toLocalDateTime(TimeZone.currentSystemDefault()).let { dt ->
                                            "${dt.date} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
                                        }
                                    )
                                },
                                trailing = if (!session.isCurrentDevice) {
                                    {
                                        if (state.revokingId == session.id) {
                                            CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))
                                        } else {
                                            TextButton(onClick = { pendingRevoke = session }) {
                                                Text(strings.activeSessionsLogOut, color = MaterialTheme.debtAccentColors.debt)
                                            }
                                        }
                                    }
                                } else null,
                            )
                            if (index != state.sessions.lastIndex) SettingsRowDivider()
                        }
                    }

                    if (state.sessions.count { !it.isCurrentDevice } > 0) {
                        OutlinedCard(
                            shape = RoundedCornerShape(Dimens.space16),
                            border = BorderStroke(Dimens.space2, color = MaterialTheme.colorScheme.primary),
                            ) {
                        TextButton(
                            onClick = { showRevokeAllConfirm = true },
                            enabled = !state.isRevokingAll,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isRevokingAll) {
                                CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space20))
                            } else {
                                Text(strings.activeSessionsLogOutAllOthers, color = MaterialTheme.debtAccentColors.debt)
                            }
                        }}
                    }
                }
            }
        }
    }

    pendingRevoke?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingRevoke = null },
            title = { Text(strings.activeSessionsRevokeConfirmTitle) },
            text = { Text(strings.activeSessionsRevokeConfirmText(session.deviceName)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRevoke = null
                    viewModel.onIntent(ActiveSessionsIntent.RevokeSession(session.id))
                }) { Text(strings.activeSessionsLogOut) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevoke = null }) { Text(strings.cancel) }
            },
        )
    }

    if (showRevokeAllConfirm) {
        AlertDialog(
            onDismissRequest = { showRevokeAllConfirm = false },
            title = { Text(strings.activeSessionsLogOutAllOthersConfirmTitle) },
            text = { Text(strings.activeSessionsLogOutAllOthersConfirmText) },
            confirmButton = {
                TextButton(onClick = {
                    showRevokeAllConfirm = false
                    viewModel.onIntent(ActiveSessionsIntent.RevokeAllOthers)
                }) { Text(strings.activeSessionsLogOutAllOthers) }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeAllConfirm = false }) { Text(strings.cancel) }
            },
        )
    }
}

private fun AppPlatform.icon(): ImageVector = when (this) {
    AppPlatform.ANDROID -> Icons.Filled.PhoneAndroid
    AppPlatform.IOS -> Icons.Filled.PhoneIphone
    AppPlatform.DESKTOP -> Icons.Filled.Computer
    AppPlatform.WEB -> Icons.Filled.Public
}

@Preview
@Composable
private fun ActiveSessionsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    ActiveSessionsScreen(onBack = {})
}

@Preview
@Composable
private fun ActiveSessionsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    ActiveSessionsScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ActiveSessionsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    ActiveSessionsScreen(onBack = {})
}

@Preview(device = DESKTOP)
@Composable
private fun ActiveSessionsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    ActiveSessionsScreen(onBack = {})
}
