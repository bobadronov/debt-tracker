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
        assertEquals(strings.settingsTitle, Screen.Settings.windowTitle(strings))
        assertEquals(strings.statsTitle, Screen.Stats.windowTitle(strings))
        assertEquals(strings.notificationsTitle, Screen.Notifications.windowTitle(strings))
        assertEquals(strings.exportTitle, Screen.Export().windowTitle(strings))
        assertEquals(
            strings.addEditDebtorTitleNew,
            Screen.AddEditContact(DebtDirection.DEBTOR).windowTitle(strings),
        )
        assertEquals(
            strings.addEditCreditorTitleNew,
            Screen.AddEditContact(DebtDirection.CREDITOR).windowTitle(strings),
        )
    }
}
