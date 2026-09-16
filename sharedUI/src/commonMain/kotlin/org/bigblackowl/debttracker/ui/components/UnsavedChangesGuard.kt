package org.bigblackowl.debttracker.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.bigblackowl.debttracker.core.i18n.LocalStrings
import org.bigblackowl.debttracker.ui.components.button.TextButton

/**
 * Single-slot back interceptor: [org.bigblackowl.debttracker.navigation.DebtTrackerNavGraph]'s
 * `back()` — which every exit path (the in-app back arrow, Android system back/gesture, desktop
 * predictive back) already funnels through — consults [tryIntercept] before popping. That single
 * choke point is what lets [UnsavedChangesGuard] guard every exit path from a screen without that
 * screen needing its own back-button wiring: it just registers/unregisters here.
 */
internal object BackInterceptor {
    private var onBackAttempt: (() -> Unit)? = null

    fun register(handler: () -> Unit) {
        onBackAttempt = handler
    }

    fun unregister(handler: () -> Unit) {
        if (onBackAttempt === handler) onBackAttempt = null
    }

    /** Returns true if a registered screen intercepted this attempt (e.g. to show a dialog)
     * instead of letting the caller pop the back stack. */
    fun tryIntercept(): Boolean {
        val handler = onBackAttempt ?: return false
        handler()
        return true
    }
}

/**
 * Drop into any data-entry screen's body. While [hasUnsavedChanges] is true, leaving the screen —
 * by any path, since [BackInterceptor] is the one choke point every one of them already goes
 * through — pops a Save/Discard/Cancel dialog instead of leaving immediately.
 *
 * [onSave] and [onDiscard] are left to the screen because "leave" means different things depending
 * on how saving works there: a screen that persists synchronously (no network) can just save then
 * call its own `onBack`; one that saves asynchronously (e.g. a network write that can fail
 * validation) should only fire the save action here and let its existing
 * effect-observer navigate away once a success effect actually arrives — exactly like tapping its
 * own Save button already does, so failures surface the same way instead of navigating away early.
 */
@Composable
fun UnsavedChangesGuard(hasUnsavedChanges: Boolean, onSave: () -> Unit, onDiscard: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val handler = remember { { showDialog = true } }

    DisposableEffect(hasUnsavedChanges) {
        if (hasUnsavedChanges) BackInterceptor.register(handler) else BackInterceptor.unregister(handler)
        onDispose { BackInterceptor.unregister(handler) }
    }

    if (showDialog) {
        val strings = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(strings.unsavedChangesTitle) },
            text = { Text(strings.unsavedChangesMessage) },
            confirmButton = {
                // Unregister explicitly rather than waiting for hasUnsavedChanges to flip false:
                // onSave()/onDiscard() call back into the guarded back()/popTo() right away (or,
                // for an async save, once its own success effect arrives) — without this, that call
                // finds the interceptor still registered and immediately re-shows this same dialog
                // instead of actually leaving.
                TextButton(onClick = { showDialog = false; BackInterceptor.unregister(handler); onSave() }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; BackInterceptor.unregister(handler); onDiscard() }) { Text(strings.discardChanges) }
            },
        )
    }
}
