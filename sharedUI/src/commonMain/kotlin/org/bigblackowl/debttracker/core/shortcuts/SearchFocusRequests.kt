package org.bigblackowl.debttracker.core.shortcuts

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Місток між Desktop-хоткеєм Ctrl+F (спек §6, "Навігація") і полем пошуку на
 * поточному видимому списку — NavGraph не знає про FocusRequester конкретного
 * екрана, тому просто емітить подію, а DebtorListScreen/CreditorListScreen
 * підписуються й самі викликають requestFocus().
 */
class SearchFocusRequests {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun request() {
        _events.tryEmit(Unit)
    }
}
