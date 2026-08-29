package org.bigblackowl.debttracker.core.notifications

import androidx.compose.runtime.Composable

/** Web: [WebLocalNotifier.requestPermission] already calls `Notification.requestPermission()`. */
@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    rememberLocalNotifierPermissionRequester()
