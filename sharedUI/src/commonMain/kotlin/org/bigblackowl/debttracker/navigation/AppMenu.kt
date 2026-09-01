package org.bigblackowl.debttracker.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The always-available app menu (Notifications / QR / Stats / Settings). Rendered by the desktop
 * shell into the native title bar on every unlocked screen, and by `HomeScreen`'s top bar on phone
 * / web. [DebtTrackerNavGraph] binds the navigation callbacks and flips [State.visible] as the user
 * moves in and out of the locked / onboarding screens.
 */
object AppMenu {
    /** A navigation destination reachable from the app menu. */
    enum class Target { Notifications, Qr, Stats, Settings }

    class State internal constructor(
        val visible: Boolean = false,
        /** Targets already on the back stack — the menu hides their items (just go back instead). */
        val activeTargets: Set<Target> = emptySet(),
        val openNotifications: () -> Unit = {},
        val openQr: () -> Unit = {},
        val openStats: () -> Unit = {},
        val openSettings: () -> Unit = {},
        val addDebtor: () -> Unit = {},
        val addCreditor: () -> Unit = {},
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    internal fun set(state: State) {
        _state.value = state
    }

    internal fun clear() {
        _state.value = State()
    }
}
