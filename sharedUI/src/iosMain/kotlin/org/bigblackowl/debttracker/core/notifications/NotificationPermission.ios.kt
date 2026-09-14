package org.bigblackowl.debttracker.core.notifications

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/** iOS: [IosLocalNotifier.requestPermission] already drives `UNUserNotificationCenter.requestAuthorization`. */
@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    rememberLocalNotifierPermissionRequester()

/** iOS never re-prompts after a denial — the only way back is its own Settings app, via the standard `UIApplicationOpenSettingsURLString` deep link. */
@Composable
actual fun rememberOpenNotificationSettings(): () -> Unit = {
    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
        UIApplication.sharedApplication.openURL(it)
    }
}
