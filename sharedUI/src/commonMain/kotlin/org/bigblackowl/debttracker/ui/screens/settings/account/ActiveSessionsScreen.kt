package org.bigblackowl.debttracker.ui.screens.settings.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.platform.AppPlatform
import org.bigblackowl.debttracker.domain.model.DeviceSession
import org.bigblackowl.debttracker.domain.model.formatDateTime
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.ConfirmDialog
import org.bigblackowl.debttracker.ui.components.SettingsDetailScaffold
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.button.TextButton
import org.bigblackowl.debttracker.ui.components.card.SemanticOutlinedCard
import org.koin.compose.viewmodel.koinViewModel

/** Settings → Active devices: session management + remote logout, reached from [SettingsScreen]'s account section. */
@Composable
fun ActiveSessionsScreen(
    onBack: () -> Unit,
    viewModel: ActiveSessionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ActiveSessionsEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ActiveSessionsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRevokeSession = { viewModel.onIntent(ActiveSessionsIntent.RevokeSession(it)) },
        onRevokeAllOthers = { viewModel.onIntent(ActiveSessionsIntent.RevokeAllOthers) },
    )
}

@Composable
private fun ActiveSessionsContent(
    state: ActiveSessionsState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRevokeSession: (String) -> Unit,
    onRevokeAllOthers: () -> Unit,
) {
    val strings = LocalStrings.current
    var pendingRevoke by remember { mutableStateOf<DeviceSession?>(null) }
    var showRevokeAllConfirm by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = strings.activeSessions.title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        if (state.isLoading) {
            CircularWavyProgressIndicator(modifier = Modifier.padding(Dimens.Spacing.lg))
        } else {
            SettingsSection(strings.activeSessions.title) {
                state.sessions.forEachIndexed { index, session ->
                    SettingsRow(
                        icon = session.platform.icon(),
                        title = session.deviceName,
                        subtitle = if (session.isCurrentDevice) {
                            strings.activeSessions.currentDevice
                        } else {
                            strings.activeSessions.lastActive(session.lastSeenAt.formatDateTime())
                        },
                        trailing = if (!session.isCurrentDevice) {
                            {
                                if (state.revokingId == session.id) {
                                    CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.IconSize.sm))
                                } else {
                                    TextButton(onClick = { pendingRevoke = session }) {
                                        Text(strings.activeSessions.logOut, color = MaterialTheme.debtAccentColors.debt)
                                    }
                                }
                            }
                        } else null,
                    )
                    if (index != state.sessions.lastIndex) SettingsRowDivider()
                }
            }

            if (state.sessions.count { !it.isCurrentDevice } > 0) {
                SemanticOutlinedCard(borderColor = MaterialTheme.colorScheme.primary) {
                    TextButton(
                        onClick = { showRevokeAllConfirm = true },
                        enabled = !state.isRevokingAll,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isRevokingAll) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.IconSize.sm))
                        } else {
                            Text(strings.activeSessions.logOutAllOthers, color = MaterialTheme.debtAccentColors.debt)
                        }
                    }
                }
            }
        }
    }

    pendingRevoke?.let { session ->
        ConfirmDialog(
            title = strings.activeSessions.revokeConfirmTitle,
            text = strings.activeSessions.revokeConfirmText(session.deviceName),
            confirmLabel = strings.activeSessions.logOut,
            onConfirm = {
                pendingRevoke = null
                onRevokeSession(session.id)
            },
            onDismiss = { pendingRevoke = null },
        )
    }

    if (showRevokeAllConfirm) {
        ConfirmDialog(
            title = strings.activeSessions.logOutAllOthersConfirmTitle,
            text = strings.activeSessions.logOutAllOthersConfirmText,
            confirmLabel = strings.activeSessions.logOutAllOthers,
            onConfirm = {
                showRevokeAllConfirm = false
                onRevokeAllOthers()
            },
            onDismiss = { showRevokeAllConfirm = false },
        )
    }
}

private fun AppPlatform.icon(): ImageVector = when (this) {
    AppPlatform.ANDROID -> Icons.Filled.PhoneAndroid
    AppPlatform.IOS -> Icons.Filled.PhoneIphone
    AppPlatform.DESKTOP -> Icons.Filled.Computer
    AppPlatform.WEB -> Icons.Filled.Public
}

private val PREVIEW_SESSIONS = listOf(
    DeviceSession(id = "s1", deviceName = "Pixel 8", platform = AppPlatform.ANDROID, lastSeenAt = kotlin.time.Instant.parse("2026-09-16T08:00:00Z"), isCurrentDevice = true),
    DeviceSession(id = "s2", deviceName = "MacBook Pro", platform = AppPlatform.DESKTOP, lastSeenAt = kotlin.time.Instant.parse("2026-09-15T20:00:00Z"), isCurrentDevice = false),
)

@Composable
private fun Preview(state: ActiveSessionsState) = ActiveSessionsContent(
    state = state,
    snackbarHostState = remember { SnackbarHostState() },
    onBack = {},
    onRevokeSession = {},
    onRevokeAllOthers = {},
)

@Preview
@Composable
private fun ActiveSessionsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ActiveSessionsState(isLoading = false, sessions = PREVIEW_SESSIONS))
}

@Preview
@Composable
private fun ActiveSessionsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(ActiveSessionsState(isLoading = false, sessions = PREVIEW_SESSIONS))
}

@Preview(device = DESKTOP)
@Composable
private fun ActiveSessionsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(ActiveSessionsState(isLoading = false, sessions = PREVIEW_SESSIONS))
}

@Preview(device = DESKTOP)
@Composable
private fun ActiveSessionsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) {
    Preview(ActiveSessionsState(isLoading = false, sessions = PREVIEW_SESSIONS))
}
