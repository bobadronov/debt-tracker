package org.bigblackowl.debttracker.core.notifications

import androidx.compose.runtime.Composable

/** Desktop: system-tray toasts need no permission — [DesktopLocalNotifier.requestPermission] is always `true`. */
@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    rememberLocalNotifierPermissionRequester()
