package org.bigblackowl.debttracker.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [resolveIsDark] backs [AppTheme] and [rememberAppColorScheme] (desktop window chrome + toasts). */
class ThemeResolutionTest {

    @Test
    fun explicitDarkAlwaysWins() {
        assertTrue(resolveIsDark("dark", systemIsDark = false))
        assertTrue(resolveIsDark("dark", systemIsDark = true))
    }

    @Test
    fun explicitLightAlwaysWins() {
        assertFalse(resolveIsDark("light", systemIsDark = true))
        assertFalse(resolveIsDark("light", systemIsDark = false))
    }

    @Test
    fun anythingElseFollowsTheSystem() {
        assertEquals(true, resolveIsDark("system", systemIsDark = true))
        assertEquals(false, resolveIsDark("system", systemIsDark = false))
        // Unknown / empty preference also defers to the OS rather than guessing.
        assertEquals(true, resolveIsDark("", systemIsDark = true))
        assertEquals(false, resolveIsDark("auto", systemIsDark = false))
    }
}
