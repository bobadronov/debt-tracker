package org.bigblackowl.debttracker.core.shortcuts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bigblackowl.debttracker.core.shortcuts.HomeTabRequest.consume
import org.bigblackowl.debttracker.core.shortcuts.HomeTabRequest.pending
import org.bigblackowl.debttracker.core.shortcuts.HomeTabRequest.request

/**
 * Bridges an "open the app on a specific Home tab" request from *outside* Compose (the Android
 * home-screen widget tapping one of its two rows) into [org.bigblackowl.debttracker.ui.screens.home.HomeScreen].
 * The platform entry point ([org.bigblackowl.debttracker.androidApp.AppActivity] on Android — no
 * equivalent elsewhere) calls [request] with the target page index; `HomeScreen` collects
 * [pending] and scrolls its pager, then calls [consume] so it doesn't re-fire on the next
 * recomposition / config change.
 *
 * Plain [MutableStateFlow] (like [org.bigblackowl.debttracker.core.qr.ContactDeepLinks]) so a
 * request that lands before `HomeScreen` starts collecting — the OS delivers the launch intent
 * before `setContent {}` runs — isn't lost.
 */
object HomeTabRequest {
    /** Home pager page: 0 = debtors ("owed to me"), 1 = creditors ("I owe"). */
    const val TAB_DEBTORS = 0
    const val TAB_CREDITORS = 1

    private val _pending = MutableStateFlow<Int?>(null)
    val pending: StateFlow<Int?> = _pending.asStateFlow()

    fun request(tab: Int) {
        _pending.value = tab
    }

    fun consume() {
        _pending.value = null
    }
}
