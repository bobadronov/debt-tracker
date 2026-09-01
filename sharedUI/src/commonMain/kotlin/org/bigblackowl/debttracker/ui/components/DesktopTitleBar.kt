package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges a screen's `TopAppBar` content into the desktop app's native OS title bar. The desktop
 * shell ([claim] from `main.kt`) takes over rendering; [BackTopAppBar] then feeds its title / back
 * button / action icons here and draws nothing itself. A plain global object (like
 * [org.bigblackowl.debttracker.navigation.CurrentScreen]) rather than a `CompositionLocal`, because
 * the Tao title bar and the window body are separate `ComposeScene`s and locals don't cross them —
 * a [StateFlow] observed with `collectAsState` does.
 *
 * Untouched (and [claimed] stays `false`) on phone / web, where top bars render normally.
 */
object DesktopTitleBar {

    /** Title / back handler / action icons the visible screen has routed into the native title bar. */
    class Content internal constructor(
        val title: String? = null,
        val back: (() -> Unit)? = null,
        val actions: (@Composable RowScope.() -> Unit)? = null,
    )

    /** Set once from `main.kt` before the first composition — a plain flag is enough. */
    var claimed: Boolean = false
        private set

    private val _content = MutableStateFlow(Content())
    val content: StateFlow<Content> = _content.asStateFlow()

    // During a nav transition the outgoing and incoming screens are both composed; the incoming
    // one takes ownership, so the outgoing one's teardown must not wipe the bar it no longer owns.
    private var owner: Any? = null

    /** Called once by the desktop shell (`main.kt`). */
    fun claim() {
        claimed = true
    }

    fun set(owner: Any, title: String?, back: (() -> Unit)?, actions: (@Composable RowScope.() -> Unit)?) {
        this.owner = owner
        _content.value = Content(title, back, actions)
    }

    /** Clears only if [owner] is still the current owner (no-op once another screen has taken over). */
    fun release(owner: Any) {
        if (this.owner === owner) {
            this.owner = null
            _content.value = Content()
        }
    }
}
