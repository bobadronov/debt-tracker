package org.bigblackowl.debttracker.navigation

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [AppMenu] carries the app menu's visibility + navigation callbacks from the nav graph to the UI. */
class AppMenuTest {

    @AfterTest
    fun reset() = AppMenu.clear()

    @Test
    fun startsHidden() {
        assertFalse(AppMenu.state.value.visible)
    }

    @Test
    fun setPublishesVisibilityAndCallbacks() {
        var settingsOpened = 0
        AppMenu.set(AppMenu.State(visible = true, openSettings = { settingsOpened++ }))

        assertTrue(AppMenu.state.value.visible)
        AppMenu.state.value.openSettings()
        assertEquals(1, settingsOpened)
    }

    @Test
    fun clearHidesTheMenu() {
        AppMenu.set(AppMenu.State(visible = true))
        AppMenu.clear()
        assertFalse(AppMenu.state.value.visible)
        assertTrue(AppMenu.state.value.activeTargets.isEmpty())
    }

    @Test
    fun activeTargetsAreCarriedThrough() {
        AppMenu.set(
            AppMenu.State(
                visible = true,
                activeTargets = setOf(AppMenu.Target.Settings, AppMenu.Target.Stats),
            )
        )
        val active = AppMenu.state.value.activeTargets
        assertTrue(AppMenu.Target.Settings in active)
        assertFalse(AppMenu.Target.Notifications in active)
    }
}
