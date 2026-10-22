package org.bigblackowl.debttracker.ui.components.appbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bigblackowl.debttracker.ui.components.appbar.DesktopTitleBar.claim
import org.bigblackowl.debttracker.ui.components.appbar.DesktopTitleBar.claimed

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

    // Every live screen that routed content here, in order of first registration (newest last). The
    // newest one is shown; when it leaves (nav pop) the previous one takes the bar back even though
    // it never recomposed — so a screen that stays composed under a pushed one doesn't lose its
    // title / back button, and an outgoing screen's teardown never wipes an incoming one's bar.
    private val entries = LinkedHashMap<Any, Content>()

    /** Called once by the desktop shell (`main.kt`). */
    fun claim() {
        claimed = true
    }

    fun set(owner: Any, title: String?, back: (() -> Unit)?, actions: (@Composable RowScope.() -> Unit)?) {
        entries[owner] = Content(title, back, actions)
        publish()
    }

    /** Drops [owner]'s entry; the bar falls back to the newest remaining screen (or empty). */
    fun release(owner: Any) {
        if (entries.remove(owner) != null) publish()
    }

    private fun publish() {
        _content.value = entries.values.lastOrNull() ?: Content()
    }
}
