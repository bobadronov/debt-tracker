package org.bigblackowl.debttracker.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.bigblackowl.debttracker.ui.components.button.Button
import org.bigblackowl.debttracker.ui.components.button.rememberOnceClick
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Drives real [Button] taps through Compose's test harness (not just a direct call to
 * [rememberOnceClick]) — this is what every screen's Save/Delete/Approve button now goes
 * through project-wide (spec: a pressed button ignores a repeat tap while it's still running).
 */
@OptIn(ExperimentalTestApi::class)
class GuardedButtonsTest {

    @Test
    fun `a single tap fires onClick once`() = runComposeUiTest {
        var clicks = 0
        setContent {
            Button(onClick = { clicks++ }) { Text("Go") }
        }

        onNodeWithText("Go").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `rapid repeat taps within the guard window fire onClick only once`() = runComposeUiTest {
        var clicks = 0
        setContent {
            Button(onClick = { clicks++ }) { Text("Go") }
        }

        val node = onNodeWithText("Go")
        node.performClick()
        node.performClick()
        node.performClick()

        assertEquals(1, clicks)
    }

}
