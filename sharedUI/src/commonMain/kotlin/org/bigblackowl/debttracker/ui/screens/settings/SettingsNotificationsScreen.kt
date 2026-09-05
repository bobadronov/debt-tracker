package org.bigblackowl.debttracker.ui.screens.settings

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.notifications.rememberNotificationPermissionRequester
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.theme.Dimens
import org.bigblackowl.debttracker.ui.components.PlaceholderScreen
import org.bigblackowl.debttracker.ui.components.SettingsRowDivider
import org.bigblackowl.debttracker.ui.components.SettingsSection
import org.bigblackowl.debttracker.ui.components.SettingsSwitchRow
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings → Сповіщення — виокремлено з колишнього єдиного SettingsScreen. Лише для залогінених
 * (сповіщення §7 стосуються дзеркальних боргів у Supabase — без входу в акаунт їх нема звідки
 * отримувати), тож ця сторінка недосяжна з хаба для local-only акаунтів.
 */
@Composable
fun SettingsNotificationsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings = koinInject<AppSettings>()
    val notificationPermissionRequester = rememberNotificationPermissionRequester()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    PlaceholderScreen(title = strings.settings.notifications, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(Dimens.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.space24),
            ) {
                SettingsSection(strings.settings.notifications) {
                    SettingsSwitchRow(
                        icon = if (settings.notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                        title = strings.settings.notifications,
                        subtitle = if (settings.notificationsEnabled && state.notificationsPermissionBlocked) strings.settings.notificationsBlocked else null,
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.onIntent(SettingsIntent.ToggleNotifications(it, notificationPermissionRequester)) },
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
