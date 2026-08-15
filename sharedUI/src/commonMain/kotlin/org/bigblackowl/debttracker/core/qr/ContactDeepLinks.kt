package org.bigblackowl.debttracker.core.qr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges a `debttracker://contact?...` link opened from *outside* the app (a third-party QR
 * scanner, Google Lens, the stock camera app resolving the link via the OS) into the nav graph.
 * Platform entry points (`AppActivity.onCreate`/`onNewIntent` on Android, `onOpenURL` on iOS —
 * there's no equivalent for Desktop/Web, which never handle inbound OS links) call
 * [onIncomingLink] with the raw URI text; [org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph]
 * collects [pendingLink] and decodes it via [org.bigblackowl.debttracker.domain.model.ContactQrPayload].
 *
 * A plain [MutableStateFlow] rather than a one-shot event channel so a link that arrives before
 * Compose has started collecting (the common case: the OS delivers the launch intent before
 * `setContent { }` even runs) isn't lost — new collectors immediately see whatever's here. The
 * collector calls [consume] right after reading it so the same link doesn't get reprocessed on
 * the next recomposition/collector restart (e.g. after a config change).
 */
object ContactDeepLinks {
    private val _pendingLink = MutableStateFlow<String?>(null)
    val pendingLink: StateFlow<String?> = _pendingLink.asStateFlow()

    fun onIncomingLink(rawUri: String) {
        _pendingLink.value = rawUri
    }

    fun consume() {
        _pendingLink.value = null
    }
}
