@file:OptIn(ExperimentalForeignApi::class)

package org.bigblackowl.debttracker.core.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionAlert
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val USER_INFO_DEEP_LINK = "deepLink"

/** iOS: `UNUserNotificationCenter` — миттєве локальне сповіщення (`trigger = null` — доставляється одразу). */
internal class IosLocalNotifier : LocalNotifier {
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    // Retained for the lifetime of this (Koin) singleton so notification taps keep routing.
    private val clickDelegate = NotificationClickDelegate()

    init {
        center.setDelegate(clickDelegate)
    }

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, _ ->
            if (continuation.isActive) continuation.resume(granted)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun notify(title: String, body: String, deepLink: String?) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            if (deepLink != null) setUserInfo(mapOf<Any?, Any?>(USER_INFO_DEEP_LINK to deepLink))
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = Uuid.random().toString(),
            content = content,
            trigger = null,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}

/**
 * Routes a tapped notification's `userInfo["deepLink"]` into [NotificationDeepLinks], and keeps
 * notifications visible while the app is foregrounded (iOS suppresses them by default without a
 * delegate) so they stay tappable.
 */
private class NotificationClickDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val link = didReceiveNotificationResponse.notification.request.content.userInfo[USER_INFO_DEEP_LINK] as? String
        if (link != null) NotificationDeepLinks.onIncomingLink(link)
        withCompletionHandler()
    }

    @Suppress("DEPRECATION") // Alert/Sound cover iOS < 14 too; Banner/List are 14+ only
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(UNNotificationPresentationOptionAlert or UNNotificationPresentationOptionSound)
    }
}
