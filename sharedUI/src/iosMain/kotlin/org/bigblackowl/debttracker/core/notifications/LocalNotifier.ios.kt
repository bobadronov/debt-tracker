@file:OptIn(ExperimentalForeignApi::class)

package org.bigblackowl.debttracker.core.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** iOS: `UNUserNotificationCenter` — миттєве локальне сповіщення (`trigger = null` — доставляється одразу). */
internal class IosLocalNotifier : LocalNotifier {
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, _ ->
            if (continuation.isActive) continuation.resume(granted)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun notify(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = Uuid.random().toString(),
            content = content,
            trigger = null,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}
