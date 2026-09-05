package org.bigblackowl.debttracker.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bigblackowl.debttracker.core.i18n.Strings
import org.bigblackowl.debttracker.domain.model.DebtDirection

/**
 * [DebtTrackerNavGraph] publishes the top of its back stack here so the desktop window's Material
 * title bar (`main.kt`) can name the current screen. Built like [org.bigblackowl.debttracker.core.qr.ContactDeepLinks]
 * — a plain conflated [MutableStateFlow], since the nav graph owns the stack and the desktop shell
 * only needs to observe it. `null` until the first screen shows / after the graph leaves composition.
 */
object CurrentScreen {
    private val _screen = MutableStateFlow<Screen?>(null)
    val screen: StateFlow<Screen?> = _screen.asStateFlow()

    internal fun set(screen: Screen?) {
        _screen.value = screen
    }
}

/**
 * Label for the desktop window title bar. Detail screens fall back to a generic noun — the entity's
 * own name isn't known from the route alone. Pre-unlock screens and Home keep the app name.
 */
fun Screen.windowTitle(strings: Strings): String = when (this) {
    Screen.Splash, Screen.Onboarding, Screen.AccountOnboarding, Screen.Home -> strings.appName
    Screen.AuthGate -> strings.authGate.title
    is Screen.Auth -> if (isGate) strings.authGate.title else strings.appName
    is Screen.ContactPicker -> strings.contactPicker.title
    is Screen.AddEditContact -> when (direction) {
        DebtDirection.DEBTOR ->
            if (editId != null) strings.addEditDebtor.titleEdit else strings.addEditDebtor.titleNew
        DebtDirection.CREDITOR ->
            if (editId != null) strings.addEditCreditor.titleEdit else strings.addEditCreditor.titleNew
    }
    is Screen.DebtorDetail -> strings.debtorDetail.titleFallback
    is Screen.CreditorDetail -> strings.creditorDetail.titleFallback
    Screen.Stats -> strings.stats.title
    Screen.ExchangeRates -> strings.exchangeRates.menuTitle
    Screen.Settings -> strings.settings.title
    Screen.SettingsProtection -> strings.settings.protection
    Screen.SettingsNotifications -> strings.settings.notifications
    Screen.SettingsPreferences -> strings.settings.preferences
    Screen.SettingsData -> strings.settings.data
    Screen.SettingsAbout -> strings.settings.about
    Screen.Language -> strings.settings.language
    Screen.AccountInfo -> strings.settings.account
    Screen.EditAccount -> strings.editAccountTitle
    Screen.ActiveSessions -> strings.activeSessions.title
    Screen.QrHub -> strings.qr.home
    Screen.Notifications -> strings.notifications.title
    is Screen.Export -> strings.export.title
}
