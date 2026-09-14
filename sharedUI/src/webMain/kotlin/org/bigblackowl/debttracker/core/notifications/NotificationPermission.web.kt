package org.bigblackowl.debttracker.core.notifications

import androidx.compose.runtime.Composable

/** Web: [WebLocalNotifier.requestPermission] already calls `Notification.requestPermission()`. */
@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    rememberLocalNotifierPermissionRequester()

/** No JS API lets a page open its own browser's notification settings — once denied, only the user, in the browser's own UI, can undo it. */
@Composable
actual fun rememberOpenNotificationSettings(): () -> Unit = {}
