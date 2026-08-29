package org.bigblackowl.debttracker.core.notifications

import androidx.compose.runtime.Composable

/** iOS: [IosLocalNotifier.requestPermission] already drives `UNUserNotificationCenter.requestAuthorization`. */
@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    rememberLocalNotifierPermissionRequester()
