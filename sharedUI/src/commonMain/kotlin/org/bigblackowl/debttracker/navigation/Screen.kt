package org.bigblackowl.debttracker.navigation

import org.bigblackowl.debttracker.domain.model.ContactPrefill
import org.bigblackowl.debttracker.domain.model.DebtDirection

/** Типізовані маршрути навігаційного графа (спек §6). */
sealed interface Screen {
    data object Splash : Screen
    data object Onboarding : Screen
    data object AccountOnboarding : Screen
    data object AuthGate : Screen
    data object Home : Screen

    /** Крок вибору раніше введеного контакту перед формою «Додати запис». */
    data class ContactPicker(val direction: DebtDirection) : Screen

    /** Об'єднана форма «Додати запис» (боржник або кредитор — за [direction]). */
    data class AddEditContact(
        val direction: DebtDirection,
        val prefill: ContactPrefill? = null,
    ) : Screen

    data class DebtorDetail(val debtorId: String) : Screen
    data class CreditorDetail(val creditorId: String) : Screen
    data object Stats : Screen
    data object Settings : Screen
    data object Language : Screen
    data object AccountInfo : Screen
    data object EditAccount : Screen
    data object ActiveSessions : Screen
    data object QrHub : Screen
    data object Notifications : Screen
    data class Export(val debtorId: String? = null, val creditorId: String? = null) : Screen
    data class Auth(val isGate: Boolean = false) : Screen
}
