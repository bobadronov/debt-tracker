package org.bigblackowl.debttracker.ui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

private val ClickGuardWindow: Duration = 500.milliseconds

/**
 * Wraps [onClick] so a tap fires the action once, and any further tap within [window] is
 * silently ignored. Most `onClick` handlers in this app just fire-and-forget dispatch into a
 * ViewModel's `launch {}` (a [org.bigblackowl.debttracker.ui.screens] MVI intent) and return
 * immediately — there's no synchronous "still running" signal the UI could key off, so a fast
 * double-tap would otherwise dispatch the same intent twice (double-submit, double-navigation,
 * duplicate transaction, ...). This is the guard behind every wrapper below; reach for it
 * directly only for a bespoke clickable (e.g. `Modifier.clickable`) that isn't one of them.
 */
@Composable
fun rememberOnceClick(window: Duration = ClickGuardWindow, onClick: () -> Unit): () -> Unit {
    var lastAcceptedAt by remember { mutableStateOf<Instant?>(null) }
    return click@{
        val now = Clock.System.now()
        val last = lastAcceptedAt
        if (last != null && now - last < window) return@click
        lastAcceptedAt = now
        onClick()
    }
}