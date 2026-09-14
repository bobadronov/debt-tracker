package org.bigblackowl.debttracker.core.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.koinInject

/** Result of asking the OS for permission to post notifications (Settings → Preferences → Notifications). */
enum class NotificationPermissionStatus {
    /** Notifications may be shown — either the user granted them, or the platform needs no permission (Desktop). */
    GRANTED,

    /** The user has denied notifications (or a previous denial means the OS won't prompt again) — the app can't show them until they're enabled in system settings. */
    DENIED,
}

/**
 * Shows the OS "allow notifications?" prompt when the current platform needs and permits one, and
 * reports the outcome. Factory is `@Composable` rather than a plain constructor because Android's
 * runtime-permission request needs an `ActivityResultLauncher` registered in composition — the same
 * reason [org.bigblackowl.debttracker.core.security.rememberBiometricAuthenticator] is a composable.
 */
fun interface NotificationPermissionRequester {
    /**
     * Returns [NotificationPermissionStatus.GRANTED] immediately when permission already exists (or
     * isn't a concept on this platform); otherwise shows the OS prompt where possible and awaits the
     * user's choice.
     */
    suspend fun request(): NotificationPermissionStatus
}

@Composable
expect fun rememberNotificationPermissionRequester(): NotificationPermissionRequester

/**
 * Jumps to the OS's notification settings for this app, for when [NotificationPermissionRequester]
 * came back [NotificationPermissionStatus.DENIED] and re-requesting in-app can't do anything more
 * (Android/iOS won't show their own prompt again after a denial). A no-op where no such deep link
 * exists (Desktop needs no permission; browsers give web pages no way to open their own settings UI).
 */
@Composable
expect fun rememberOpenNotificationSettings(): () -> Unit

/**
 * Shared actual body for platforms where [LocalNotifier.requestPermission] already shows the OS
 * prompt itself (iOS, Web) or needs no permission at all (Desktop) — only Android has to go through
 * an `ActivityResultLauncher` instead.
 */
@Composable
internal fun rememberLocalNotifierPermissionRequester(): NotificationPermissionRequester {
    val notifier = koinInject<LocalNotifier>()
    return remember(notifier) {
        NotificationPermissionRequester {
            if (notifier.requestPermission()) NotificationPermissionStatus.GRANTED
            else NotificationPermissionStatus.DENIED
        }
    }
}
