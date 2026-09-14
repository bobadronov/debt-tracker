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
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.SettingsSwitchRow
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
                        icon = if (settings.notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                        title = strings.settings.notifications,
                        subtitle = if (settings.notificationsEnabled && state.notificationsPermissionBlocked) strings.settings.notificationsBlocked else null,
                        onSubtitleClick = if (settings.notificationsEnabled && state.notificationsPermissionBlocked) openNotificationSettings else null,
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.onIntent(SettingsNotificationsIntent.ToggleNotifications(it, notificationPermissionRequester)) },
                    )
                    if (settings.notificationsEnabled) {
                        SettingsRowDivider()
                        SettingsSwitchRow(
                            icon = if (settings.hideAmountsInNotifications) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            title = strings.notificationBody.hideAmountsToggle,
                            checked = settings.hideAmountsInNotifications,
                            onCheckedChange = { settings.hideAmountsInNotifications = it },
                        )
                    }
                }
            }
        }
    }
}

// The @Preview functions render this rather than SettingsNotificationsScreen directly: the extra hop
// keeps the koinViewModel() call out of the previewed function's own body (matching SettingsScreen).
// The screen renders through SettingsNotificationsViewModel, backed by the fakes in preview/PreviewModule.kt.
@Composable
private fun SettingsNotificationsScreenPreviewContent() {
    SettingsNotificationsScreen(onBack = {})
}

@Preview
@Composable
private fun SettingsNotificationsScreenLightPhonePreview() = DebtTrackerPreview(darkTheme = false) { SettingsNotificationsScreenPreviewContent() }

@Preview
@Composable
private fun SettingsNotificationsScreenDarkPhonePreview() = DebtTrackerPreview(darkTheme = true) { SettingsNotificationsScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsNotificationsScreenLightDesktopPreview() = DebtTrackerPreview(darkTheme = false) { SettingsNotificationsScreenPreviewContent() }

@Preview(device = DESKTOP)
@Composable
private fun SettingsNotificationsScreenDarkDesktopPreview() = DebtTrackerPreview(darkTheme = true) { SettingsNotificationsScreenPreviewContent() }
