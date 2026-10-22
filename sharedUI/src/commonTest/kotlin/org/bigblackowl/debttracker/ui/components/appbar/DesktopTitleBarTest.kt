package org.bigblackowl.debttracker.ui.components.appbar

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** [DesktopTitleBar] carries a screen's routed `TopAppBar` content to the desktop native title bar. */
class DesktopTitleBarTest {

    private val owner = Any()

    @AfterTest
    fun reset() = DesktopTitleBar.release(owner)

    @Test
    fun setPublishesTitleAndBack() {
        val back = {}
        DesktopTitleBar.set(owner = owner, title = "Settings", back = back, actions = null)

        val content = DesktopTitleBar.content.value
        assertSame(back, content.back)
        assertNull(content.actions)
        assertTrue(content.title == "Settings")
    }

    @Test
    fun releaseByCurrentOwnerResetsEverything() {
        DesktopTitleBar.set(owner, "Export", {}, {})
        DesktopTitleBar.release(owner)

        val content = DesktopTitleBar.content.value
        assertNull(content.title)
        assertNull(content.back)
        assertNull(content.actions)
    }

    @Test
    fun releaseByAStaleOwnerIsANoOp() {
        val current = Any()
        DesktopTitleBar.set(owner, "Old", null, null)
        DesktopTitleBar.set(current, "Home", null, null)
        DesktopTitleBar.release(owner) // the outgoing screen during a nav transition

        assertTrue(DesktopTitleBar.content.value.title == "Home")
        DesktopTitleBar.release(current)
    }

    @Test
    fun poppingRestoresThePreviousScreen() {
        val under = Any()
        val back = {}
        DesktopTitleBar.set(under, "Contact", back, null)
        DesktopTitleBar.set(owner, "Edit", {}, null)
        DesktopTitleBar.release(owner) // popped; `under` never recomposed

        assertTrue(DesktopTitleBar.content.value.title == "Contact")
        assertSame(back, DesktopTitleBar.content.value.back)
        DesktopTitleBar.release(under)
    }

    @Test
    fun claimIsSticky() {
        DesktopTitleBar.claim()
        assertTrue(DesktopTitleBar.claimed)
    }
}
