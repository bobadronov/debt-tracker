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
    Screen.AuthGate -> strings.authGateTitle
    is Screen.Auth -> if (isGate) strings.authGateTitle else strings.appName
    is Screen.ContactPicker -> strings.contactPickerTitle
    is Screen.AddEditContact -> when (direction) {
        DebtDirection.DEBTOR ->
            if (editId != null) strings.addEditDebtorTitleEdit else strings.addEditDebtorTitleNew
        DebtDirection.CREDITOR ->
            if (editId != null) strings.addEditCreditorTitleEdit else strings.addEditCreditorTitleNew
    }
    is Screen.DebtorDetail -> strings.debtorDetailTitleFallback
    is Screen.CreditorDetail -> strings.creditorDetailTitleFallback
    Screen.Stats -> strings.statsTitle
    Screen.ExchangeRates -> strings.exchangeRates.menuTitle
    Screen.Settings -> strings.settingsTitle
    Screen.Language -> strings.settingsLanguage
    Screen.AccountInfo -> strings.settingsAccount
    Screen.EditAccount -> strings.editAccountTitle
    Screen.ActiveSessions -> strings.activeSessionsTitle
    Screen.QrHub -> strings.homeQr
    Screen.Notifications -> strings.notificationsTitle
    is Screen.Export -> strings.exportTitle
}
