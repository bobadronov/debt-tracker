package org.bigblackowl.debttracker.navigation

import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.domain.model.ContactPrefill
import org.bigblackowl.debttracker.domain.model.DebtDirection

/**
 * Типізовані маршрути навігаційного графа (спек §6).
 *
 * `@Serializable` — щоб увесь back stack переживав перестворення Activity (зміна теми/локалі
 * системи, звільнення пам'яті процесу). Без цього граф перезапускався б зі [Splash] і повторно
 * показував екран блокування (`AuthGate`) при кожному перестворенні. Див. `BackStackSaver` у
 * [DebtTrackerNavGraph].
 */
@Serializable
sealed interface Screen {
    @Serializable
    data object Splash : Screen

    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object AccountOnboarding : Screen

    @Serializable
    data object AuthGate : Screen

    @Serializable
    data object Home : Screen

    /** Крок вибору раніше введеного контакту перед формою «Додати запис». */
    @Serializable
    data class ContactPicker(val direction: DebtDirection) : Screen

    /**
     * Об'єднана форма «Додати запис» (боржник або кредитор — за [direction]).
     * [editId] != null → режим редагування наявного контакту (без стартової транзакції).
     */
    @Serializable
    data class AddEditContact(
        val direction: DebtDirection,
        val prefill: ContactPrefill? = null,
        val editId: String? = null,
    ) : Screen

    @Serializable
    data class DebtorDetail(val debtorId: String) : Screen

    @Serializable
    data class CreditorDetail(val creditorId: String) : Screen

    @Serializable
    data object Stats : Screen

    @Serializable
    data object ExchangeRates : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Language : Screen

    @Serializable
    data object AccountInfo : Screen

    @Serializable
    data object EditAccount : Screen

    @Serializable
    data object ActiveSessions : Screen

    @Serializable
    data object QrHub : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data class Export(val debtorId: String? = null, val creditorId: String? = null) : Screen

    @Serializable
    data class Auth(val isGate: Boolean = false) : Screen
}
