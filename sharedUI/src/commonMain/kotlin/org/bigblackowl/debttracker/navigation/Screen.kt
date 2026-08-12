package org.bigblackowl.debttracker.navigation

/** Типізовані маршрути навігаційного графа (спек §6). */
sealed interface Screen {
    data object Splash : Screen
    data object Onboarding : Screen
    data object AccountOnboarding : Screen
    data object AuthGate : Screen
    data object Home : Screen
    data object AddEditDebtor : Screen
    data class DebtorDetail(val debtorId: String) : Screen
    data object AddEditCreditor : Screen
    data class CreditorDetail(val creditorId: String) : Screen
    data object Stats : Screen
    data object Settings : Screen
    data object Language : Screen
    data object AccountInfo : Screen
    data object EditAccount : Screen
    data object ActiveSessions : Screen
    data class Export(val debtorId: String? = null, val creditorId: String? = null) : Screen
    data class Auth(val isGate: Boolean = false) : Screen
}
