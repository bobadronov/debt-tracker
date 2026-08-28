package org.bigblackowl.debttracker.core.notifications

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.js

/**
 * Web: браузерний `Notification` API (Chrome/Edge/Firefox; Safari — обмежено). Той самий підхід
 * до js()/wasmJs()-interop, що й [org.bigblackowl.debttracker.core.qr.ContactQrFileDecoder.web.kt]
 * (`detectQrCodeBase64`) — один `js("""...""")`-сніпет, спільний для обох веб-таргетів.
 *
 * Тап по сповіщенню шле DOM-подію `debttracker:notification-click` (замість спроби передати
 * Kotlin-лямбду крізь `js()`), яку [WebLocalNotifier] слухає й пробрасує в [NotificationDeepLinks].
 */
@OptIn(ExperimentalWasmJsInterop::class)
private fun requestNotificationPermissionJs(): Promise<JsString> = js(
    """
    (function () {
        if (typeof Notification === 'undefined') return Promise.resolve('unsupported');
        if (Notification.permission === 'granted') return Promise.resolve('granted');
        if (Notification.permission === 'denied') return Promise.resolve('denied');
        return Notification.requestPermission();
    })()
    """
)

@OptIn(ExperimentalWasmJsInterop::class)
private fun showNotificationJs(title: String, body: String, deepLink: String): Unit = js(
    """
    (function () {
        if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return;
        try {
            var n = new Notification(title, { body: body });
            if (deepLink) {
                n.onclick = function () {
                    window.focus();
                    window.dispatchEvent(new CustomEvent('debttracker:notification-click', { detail: deepLink }));
                    n.close();
                };
            }
        } catch (e) {}
    })()
    """
)

@OptIn(ExperimentalWasmJsInterop::class)
private fun clickedDeepLink(event: Event): String? = js("event.detail")

internal class WebLocalNotifier : LocalNotifier {
    init {
        runCatching {
            window.addEventListener("debttracker:notification-click", { event ->
                clickedDeepLink(event)?.let { NotificationDeepLinks.onIncomingLink(it) }
            })
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun requestPermission(): Boolean =
        runCatching { requestNotificationPermissionJs().await().toString() == "granted" }.getOrDefault(false)

    override fun notify(title: String, body: String, deepLink: String?) {
        runCatching { showNotificationJs(title, body, deepLink ?: "") }
    }
}
