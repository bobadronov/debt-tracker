package org.bigblackowl.debttracker.core.shortcuts

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridge between the Desktop Ctrl+F hotkey (spec §6, "Navigation") and the search field on
 * the currently visible list — NavGraph doesn't know about a specific screen's FocusRequester,
 * so it simply emits an event, and DebtorListScreen/CreditorListScreen
 * subscribe and call requestFocus() themselves.
 */
class SearchFocusRequests {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun request() {
        _events.tryEmit(Unit)
    }
}
