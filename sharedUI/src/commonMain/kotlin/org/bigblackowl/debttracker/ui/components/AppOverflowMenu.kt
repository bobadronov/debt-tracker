package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.navigation.AppMenu
import org.koin.compose.koinInject

/**
 * The ⋮ app menu — Notifications (with unread badge) / QR / Stats / Settings. Self-contained:
 * navigation comes from [AppMenu] (bound by the nav graph), the unread count and auth state from
 * DI. Renders nothing while [AppMenu] is hidden (lock / onboarding screens). Used both in the
 * desktop native title bar (every screen) and `HomeScreen`'s top bar on phone / web.
 */
@Composable
fun AppOverflowMenu(
    strings: Strings = LocalStrings.current,
    poller: NotificationsPoller = koinInject(),
    authRepository: AuthRepository = koinInject(),
) {
    val menu by AppMenu.state.collectAsState()
    if (!menu.visible) return

    val isAuthenticated by authRepository.isAuthenticated.collectAsState()
    val unread by poller.unreadCount.collectAsState()
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            if (isAuthenticated && unread > 0) {
                BadgedBox(badge = { Badge { Text(unread.toString()) } }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = strings.homeMenu)
                }
            } else {
                Icon(Icons.Filled.MoreVert, contentDescription = strings.homeMenu)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            // Items for screens already on the back stack are hidden — see AppMenu.activeTargets.
            if (isAuthenticated && AppMenu.Target.Notifications !in menu.activeTargets) {
                DropdownMenuItem(
                    text = { Text(strings.notificationsBell) },
                    leadingIcon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    trailingIcon = if (unread > 0) {
                        { Badge { Text(unread.toString()) } }
                    } else null,
                    onClick = { open = false; menu.openNotifications() },
                )
            }
            if (AppMenu.Target.Qr !in menu.activeTargets) {
                DropdownMenuItem(
                    text = { Text(strings.homeQr) },
                    leadingIcon = { Icon(Icons.Filled.QrCode, contentDescription = null) },
                    onClick = { open = false; menu.openQr() },
                )
            }
            if (AppMenu.Target.Stats !in menu.activeTargets) {
                DropdownMenuItem(
                    text = { Text(strings.homeStats) },
                    leadingIcon = { Icon(Icons.Filled.QueryStats, contentDescription = null) },
                    onClick = { open = false; menu.openStats() },
                )
            }
            if (AppMenu.Target.Settings !in menu.activeTargets) {
                DropdownMenuItem(
                    text = { Text(strings.homeSettings) },
                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = { open = false; menu.openSettings() },
                )
            }
        }
    }
}
