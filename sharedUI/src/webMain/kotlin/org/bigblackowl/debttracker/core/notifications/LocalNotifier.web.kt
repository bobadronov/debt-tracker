package org.bigblackowl.debttracker.core.notifications

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.js

/**
 * Web: браузерний `Notification` API (Chrome/Edge/Firefox; Safari — обмежено). Той самий підхід
 * до js()/wasmJs()-interop, що й [org.bigblackowl.debttracker.core.qr.ContactQrFileDecoder.web.kt]
 * (`detectQrCodeBase64`) — один `js("""...""")`-сніпет, спільний для обох веб-таргетів.
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
private fun showNotificationJs(title: String, body: String): Unit = js(
    """
    (function () {
        if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return;
        try { new Notification(title, { body: body }); } catch (e) {}
    })()
    """
)

internal class WebLocalNotifier : LocalNotifier {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun requestPermission(): Boolean =
        runCatching { requestNotificationPermissionJs().await().toString() == "granted" }.getOrDefault(false)

    override fun notify(title: String, body: String) {
        runCatching { showNotificationJs(title, body) }
    }
}
