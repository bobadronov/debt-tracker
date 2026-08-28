package org.bigblackowl.debttracker.ui.screens.notifications

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.notifications.formatBody
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.NotificationType
import org.bigblackowl.debttracker.domain.model.formatDateTime
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.theme.debtAccentColors
import org.bigblackowl.debttracker.ui.components.SettingsDetailScaffold
import org.bigblackowl.debttracker.ui.components.SettingsRow
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.koin.compose.viewmodel.koinViewModel

/** Історія сповіщень про дзеркальні борги (спек §7) — доступна лише в Account+Sync (бейдж у Home top bar). */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToDebtor: (String) -> Unit,
    onNavigateToCreditor: (String) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationsEffect.NavigateToDebtor -> onNavigateToDebtor(effect.debtorId)
                is NotificationsEffect.NavigateToCreditor -> onNavigateToCreditor(effect.creditorId)
            }
        }
    }

    SettingsDetailScaffold(
        title = strings.notificationsTitle,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        when {
            state.isLoading -> CircularWavyProgressIndicator(modifier = Modifier.padding(Dimens.space16))
            state.notifications.isEmpty() -> Text(
                strings.notificationsEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Dimens.space24),
            )
            else -> {
                if (state.notifications.any { !it.isRead }) {
                    TextButton(
                        onClick = { viewModel.onIntent(NotificationsIntent.MarkAllRead) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.padding(end = Dimens.space8))
                        Text(strings.notificationsMarkAllRead)
                    }
                }
                SettingsSection(strings.notificationsTitle) {
                    state.notifications.forEachIndexed { index, notification ->
                        NotificationRow(
                            notification = notification,
                            onOpen = { viewModel.onIntent(NotificationsIntent.Open(notification)) },
                            onDelete = { viewModel.onIntent(NotificationsIntent.Delete(notification.id)) },
                        )
                        if (index != state.notifications.lastIndex) SettingsRowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onOpen: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    val accent = when (notification.type) {
        NotificationType.DEBTOR_LINKED, NotificationType.DEBT_TRANSACTION_ADDED -> MaterialTheme.debtAccentColors.debt
        NotificationType.CREDITOR_LINKED, NotificationType.CREDIT_TRANSACTION_ADDED -> MaterialTheme.debtAccentColors.repay
    }
    SettingsRow(
        icon = notification.type.icon(),
        title = notification.formatBody(strings),
        titleColor = if (notification.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        subtitle = notification.createdAt.formatDateTime(),
        iconTint = accent,
        iconContainerColor = accent.copy(alpha = 0.14f),
        onClick = onOpen,
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        },
    )
}

private fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.DEBTOR_LINKED, NotificationType.CREDITOR_LINKED -> Icons.Filled.Link
    NotificationType.DEBT_TRANSACTION_ADDED, NotificationType.CREDIT_TRANSACTION_ADDED -> Icons.Filled.SwapHoriz
}

@Composable
private fun NotificationsScreenPreviewContent() {
    NotificationsScreen(onBack = {}, onNavigateToDebtor = {}, onNavigateToCreditor = {})
}

@Preview
@Composable
private fun NotificationsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { NotificationsScreenPreviewContent() }

@Preview
@Composable
private fun NotificationsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { NotificationsScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun NotificationsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { NotificationsScreenPreviewContent() }
