package org.bigblackowl.debttracker.core.notifications

import androidx.compose.runtime.Composable

/** Desktop: WinRT / D-Bus / fallback toasts need no runtime permission — [DesktopLocalNotifier.requestPermission] is always `true`. */
@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    rememberLocalNotifierPermissionRequester()
