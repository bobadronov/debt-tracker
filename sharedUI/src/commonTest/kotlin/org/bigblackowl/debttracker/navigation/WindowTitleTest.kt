package org.bigblackowl.debttracker.navigation

import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.domain.model.DebtDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [Screen.windowTitle] feeds the desktop Material title bar (`main.kt`). */
class WindowTitleTest {

    private val strings = resolveStrings("en")

    private val everyScreen = listOf(
        Screen.Splash, Screen.Onboarding, Screen.AccountOnboarding, Screen.AuthGate, Screen.Home,
        Screen.ContactPicker(DebtDirection.DEBTOR),
        Screen.AddEditContact(DebtDirection.DEBTOR), Screen.AddEditContact(DebtDirection.CREDITOR),
        Screen.DebtorDetail("d"), Screen.CreditorDetail("c"),
        Screen.Stats, Screen.Settings, Screen.Language, Screen.AccountInfo, Screen.EditAccount,
        Screen.ActiveSessions, Screen.QrHub, Screen.Notifications,
        Screen.Export(), Screen.Auth(isGate = true), Screen.Auth(isGate = false),
    )

    @Test
    fun everyScreenResolvesToANonBlankTitle() {
        everyScreen.forEach {
            assertTrue(it.windowTitle(strings).isNotBlank(), "blank window title for $it")
        }
    }

    @Test
    fun titleReflectsTheScreen() {
        assertEquals(strings.appName, Screen.Home.windowTitle(strings))
        assertEquals(strings.appName, Screen.Splash.windowTitle(strings))
        assertEquals(strings.settings.title, Screen.Settings.windowTitle(strings))
        assertEquals(strings.stats.title, Screen.Stats.windowTitle(strings))
        assertEquals(strings.notifications.title, Screen.Notifications.windowTitle(strings))
        assertEquals(strings.export.title, Screen.Export().windowTitle(strings))
        assertEquals(
            strings.addEditDebtor.titleNew,
            Screen.AddEditContact(DebtDirection.DEBTOR).windowTitle(strings),
        )
        assertEquals(
            strings.addEditCreditor.titleNew,
            Screen.AddEditContact(DebtDirection.CREDITOR).windowTitle(strings),
        )
    }
}
