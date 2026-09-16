package org.bigblackowl.debttracker.ui.screens.settings.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.notifications.rememberNotificationPermissionRequester
import org.bigblackowl.debttracker.core.notifications.rememberOpenNotificationSettings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.preview.DebtTrackerPreview
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.settings.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.settings.SettingsSection
import org.bigblackowl.debttracker.ui.components.settings.SettingsSwitchRow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings → Notifications — split out from the former single SettingsScreen. Signed-in users only
 * (notifications §7 are about mirrored debts in Supabase — with no account signed in, there's
 * nowhere to receive them from), so this page is unreachable from the hub for local-only accounts.
 */
@Composable
fun SettingsNotificationsScreen(
    onBack: () -> Unit,
    viewModel: SettingsNotificationsViewModel = koinViewModel(),
) {
    val settings = koinInject<AppSettings>()
    val notificationPermissionRequester = rememberNotificationPermissionRequester()
    val openNotificationSettings = rememberOpenNotificationSettings()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsNotificationsContent(
        state = state,
        notificationsEnabled = settings.notificationsEnabled,
        hideAmountsInNotifications = settings.hideAmountsInNotifications,
        onBack = onBack,
        onToggleNotifications = { viewModel.onIntent(SettingsNotificationsIntent.ToggleNotifications(it, notificationPermissionRequester)) },
        onToggleHideAmounts = { settings.hideAmountsInNotifications = it },
        onOpenNotificationSettings = openNotificationSettings,
    )
}

@Composable
private fun SettingsNotificationsContent(
    state: SettingsNotificationsState,
    notificationsEnabled: Boolean,
    hideAmountsInNotifications: Boolean,
    onBack: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleHideAmounts: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val strings = LocalStrings.current
    PlaceholderScreen(title = strings.settings.notifications, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.Spacing.xl),
            ) {
                SettingsSection(strings.settings.notifications) {
                    SettingsSwitchRow(
                        icon = if (notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                        title = strings.settings.notifications,
                        subtitle = if (notificationsEnabled && state.notificationsPermissionBlocked) strings.settings.notificationsBlocked else null,
                        onSubtitleClick = if (notificationsEnabled && state.notificationsPermissionBlocked) onOpenNotificationSettings else null,
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                    )
                    if (notificationsEnabled) {
                        SettingsRowDivider()
                        SettingsSwitchRow(
                            icon = if (hideAmountsInNotifications) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            title = strings.notificationBody.hideAmountsToggle,
                            checked = hideAmountsInNotifications,
                            onCheckedChange = onToggleHideAmounts,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Preview(state: SettingsNotificationsState, notificationsEnabled: Boolean = true, hideAmountsInNotifications: Boolean = false) =
    SettingsNotificationsContent(
        state = state,
        notificationsEnabled = notificationsEnabled,
        hideAmountsInNotifications = hideAmountsInNotifications,
        onBack = {},
        onToggleNotifications = {},
        onToggleHideAmounts = {},
        onOpenNotificationSettings = {},
    )

@Preview
@Composable
private fun SettingsNotificationsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { Preview(SettingsNotificationsState()) }

@Preview
@Composable
private fun SettingsNotificationsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { Preview(SettingsNotificationsState()) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsNotificationsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { Preview(SettingsNotificationsState()) }

@Preview(device = DESKTOP)
@Composable
private fun SettingsNotificationsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { Preview(SettingsNotificationsState()) }

@Preview
@Composable
private fun SettingsNotificationsScreenDisabledPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(SettingsNotificationsState(), notificationsEnabled = false)
}

@Preview
@Composable
private fun SettingsNotificationsScreenBlockedPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(SettingsNotificationsState(notificationsPermissionBlocked = true))
}

@Preview
@Composable
private fun SettingsNotificationsScreenHideAmountsPreview() = DebtTrackerPreview(darkTheme = false) {
    Preview(SettingsNotificationsState(), hideAmountsInNotifications = true)
}
