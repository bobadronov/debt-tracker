package org.bigblackowl.debttracker.navigation

import kotlinx.serialization.Serializable
import org.bigblackowl.debttracker.domain.model.ContactPrefill
import org.bigblackowl.debttracker.domain.model.DebtDirection

/**
 * Typed routes for the navigation graph (spec §6).
 *
 * `@Serializable` — so the whole back stack survives Activity recreation (system theme/locale
 * change, process memory reclaim). Without this the graph would restart from [Splash] and show
 * the lock screen (`AuthGate`) again on every recreation. See `BackStackSaver` in
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

    /** Step for picking a previously entered contact before the "Add record" form. */
    @Serializable
    data class ContactPicker(val direction: DebtDirection) : Screen

    /**
     * Unified "Add record" form (debtor or creditor — per [direction]).
     * [editId] != null → editing mode for an existing contact (no starting transaction).
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
    data object SettingsProtection : Screen

    @Serializable
    data object SettingsNotifications : Screen

    @Serializable
    data object SettingsData : Screen

    @Serializable
    data object SettingsAbout : Screen

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
